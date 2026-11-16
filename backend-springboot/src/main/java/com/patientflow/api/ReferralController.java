package com.patientflow.api;

import com.patientflow.model.Referral;
import com.patientflow.repository.ReferralRepository;
import io.camunda.client.CamundaClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ReferralController {
    private static final Logger log = LoggerFactory.getLogger(ReferralController.class);
    private final ReferralRepository repository;
    private final Optional<CamundaClient> camundaClient;
    private final RestClient camundaRestClient;

    public ReferralController(
        ReferralRepository repository,
        Optional<CamundaClient> camundaClient,
        @Value("${camunda.client.rest-address:http://localhost:8081}") String camundaRestAddress
    ) {
        this.repository = repository;
        this.camundaClient = camundaClient;
        this.camundaRestClient = RestClient.builder().baseUrl(camundaRestAddress).build();
    }

    @PostMapping("/referrals")
    public ResponseEntity<?> create(@Valid @RequestBody ReferralRequest request) {
        Referral referral = new Referral();
        referral.setPatientName(request.patientName().trim());
        referral.setAge(request.age());
        referral.setReason(request.reason().trim());
        List<String> specialists = cleanList(request.specialists(), List.of("Cardiology", "Radiology", "Dietetics"));
        referral.setSpecialists(String.join(",", specialists));
        referral.setConsentGiven(request.consentGiven());
        referral.setStatus("SUBMITTED");
        repository.save(referral);

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("referralId", referral.getId());
        variables.put("patientName", referral.getPatientName());
        variables.put("age", referral.getAge());
        variables.put("reason", referral.getReason());
        variables.put("specialists", specialists);
        variables.put("diagnosticTypes", cleanList(request.diagnosticTypes(), List.of("Blood Test", "Imaging", "ECG")));
        variables.put("consentGiven", referral.getConsentGiven());
        variables.put("simulateVitalsUnavailable", Boolean.TRUE.equals(request.simulateVitalsUnavailable()));
        variables.put("simulateCriticalResult", Boolean.TRUE.equals(request.simulateCriticalResult()));

        boolean workflowStarted = false;
        String workflowMessage;
        try {
            if (camundaClient.isEmpty()) {
                referral.setStatus("ENGINE_UNAVAILABLE");
                workflowMessage = "Referral saved, but the Camunda client is not configured";
            } else {
                var processInstance = camundaClient.get().newCreateInstanceCommand()
                    .bpmnProcessId("patient-referral-triage")
                    .latestVersion()
                    .variables(variables)
                    .send()
                    .join();
                referral.setProcessInstanceKey(String.valueOf(processInstance.getProcessInstanceKey()));
                referral.setStatus("ACTIVE");
                referral.setWorkflowState("ACTIVE");
                workflowStarted = true;
                workflowMessage = "Referral submitted and workflow started";
            }
        } catch (Exception exception) {
            log.error("Could not start PatientFlow process for referral {}", referral.getId(), exception);
            referral.setStatus("WORKFLOW_START_FAILED");
            workflowMessage = "Referral saved, but Camunda could not start the workflow. Check backend logs and Camunda connectivity.";
        }
        repository.save(referral);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("referral", referral);
        response.put("workflowStarted", workflowStarted);
        response.put("message", workflowMessage);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/referrals")
    public List<Referral> all() {
        List<Referral> referrals = repository.findAll();
        referrals.forEach(this::refreshWorkflowStatus);
        return referrals;
    }

    @GetMapping("/referrals/{id}")
    public ResponseEntity<Referral> get(@PathVariable String id) {
        return repository.findById(id).map(referral -> {
            refreshWorkflowStatus(referral);
            return ResponseEntity.ok(referral);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "PatientFlow API");
    }

    private void refreshWorkflowStatus(Referral referral) {
        String processInstanceKey = referral.getProcessInstanceKey();
        if (processInstanceKey == null || processInstanceKey.isBlank()) return;
        try {
            Map<?, ?> process = camundaRestClient.get()
                .uri("/v2/process-instances/{key}", processInstanceKey)
                .retrieve()
                .body(Map.class);
            if (process == null) return;
            Object stateValue = process.get("state");
            String state = stateValue == null ? "UNKNOWN" : String.valueOf(stateValue);
            boolean incident = Boolean.TRUE.equals(process.get("hasIncident"));
            referral.setWorkflowState(state);
            referral.setWorkflowIncident(incident);
            referral.setWorkflowStatusCheckedAt(Instant.now());
            if (incident) referral.setStatus("INCIDENT");
            else if ("ACTIVE".equals(state)) referral.setStatus("ACTIVE");
            else if ("COMPLETED".equals(state)) referral.setStatus("COMPLETED");
            else if ("TERMINATED".equals(state)) referral.setStatus("TERMINATED");
            else if ("SUSPENDED".equals(state)) referral.setStatus("SUSPENDED");
            repository.save(referral);
        } catch (RestClientException exception) {
            log.debug("Camunda status unavailable for process instance {}: {}", processInstanceKey, exception.getMessage());
        } catch (Exception exception) {
            log.warn("Could not update workflow status for referral {}", referral.getId(), exception);
        }
    }

    private List<String> cleanList(List<String> values, List<String> defaults) {
        if (values == null || values.isEmpty()) return defaults;
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) cleaned.add(value.trim());
        }
        return cleaned.isEmpty() ? defaults : cleaned;
    }

    public record ReferralRequest(
        @NotBlank String patientName,
        @NotNull @Min(0) Integer age,
        @NotBlank String reason,
        List<String> specialists,
        List<String> diagnosticTypes,
        Boolean consentGiven,
        Boolean simulateVitalsUnavailable,
        Boolean simulateCriticalResult
    ) {
        public ReferralRequest {
            if (consentGiven == null) consentGiven = false;
        }
    }
}
