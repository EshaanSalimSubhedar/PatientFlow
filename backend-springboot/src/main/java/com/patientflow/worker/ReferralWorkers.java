package com.patientflow.worker;

import io.camunda.client.exception.CamundaError;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReferralWorkers {
    @JobWorker(type = "fetch-patient-vitals")
    public Map<String, Object> fetchVitals(
        @Variable String referralId,
        @Variable Integer age,
        @Variable Boolean simulateVitalsUnavailable,
        @Variable Boolean simulateCriticalResult
    ) {
        if (Boolean.TRUE.equals(simulateVitalsUnavailable)) {
            throw CamundaError.bpmnError("VITALS_UNAVAILABLE", "Synthetic FHIR vitals endpoint is unavailable for this demo referral");
        }
        int safeAge = age == null ? 0 : age;
        int pulse = safeAge > 60 ? 108 : 82;
        boolean critical = Boolean.TRUE.equals(simulateCriticalResult);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pulse", pulse);
        result.put("oxygenSaturation", 98);
        result.put("criticalResult", critical);
        result.put("vitalsSource", "MOCK_FHIR");
        result.put("vitalsReference", "VITALS-" + referralId);
        result.put("triageDisclaimer", "Synthetic academic simulation; not for clinical use");
        return result;
    }

    @JobWorker(type = "manual-vitals-review")
    public Map<String, Object> manualVitals() {
        return Map.of(
            "criticalResult", false,
            "manualReviewRequired", true,
            "manualReviewStatus", "SIMULATED_REVIEW_COMPLETE"
        );
    }

    @JobWorker(type = "emergency-clinical-escalation")
    public Map<String, Object> emergency(@Variable String referralId) {
        return Map.of(
            "escalationStatus", "ALERT_RAISED",
            "escalationReference", "ESC-" + shortId(referralId),
            "escalationMode", "SIMULATED"
        );
    }

    @JobWorker(type = "specialist-consultation")
    public Map<String, Object> specialist(@Variable String specialist, @Variable String referralId) {
        Map<String, Object> specialistResult = new LinkedHashMap<>();
        specialistResult.put("specialist", specialist);
        specialistResult.put("reviewStatus", "PENDING_SPECIALIST_CONFIRMATION");
        specialistResult.put("referralId", referralId);
        specialistResult.put("mode", "SIMULATED");
        return Map.of("specialistResult", specialistResult);
    }

    @JobWorker(type = "diagnostic-report-collection")
    public Map<String, Object> diagnostic(@Variable String diagnosticType, @Variable String referralId) {
        Map<String, Object> diagnosticResult = new LinkedHashMap<>();
        diagnosticResult.put("diagnosticType", diagnosticType);
        diagnosticResult.put("reportStatus", "MOCK_REPORT_COLLECTED");
        diagnosticResult.put("reportReference", "RPT-" + shortId(referralId) + "-" + safeCode(diagnosticType));
        diagnosticResult.put("mode", "SIMULATED");
        return Map.of("diagnosticResult", diagnosticResult);
    }

    @JobWorker(type = "specialist-sla-escalation")
    public Map<String, Object> sla(@Variable String referralId) {
        return Map.of(
            "slaStatus", "OVERDUE_ESCALATED",
            "slaEscalationReference", "SLA-" + shortId(referralId),
            "slaEscalationMode", "SIMULATED"
        );
    }

    @JobWorker(type = "validate-patient-consent")
    public Map<String, Object> consent(@Variable Boolean consentGiven) {
        if (!Boolean.TRUE.equals(consentGiven)) {
            throw CamundaError.bpmnError("CONSENT_NOT_GIVEN", "Patient consent is required before EHR synchronization");
        }
        return Map.of("consentStatus", "CONSENT_CONFIRMED");
    }

    @JobWorker(type = "sync-ehr-record")
    public Map<String, Object> ehr(@Variable String referralId) {
        return Map.of(
            "ehrSyncStatus", "MOCK_SYNC_COMPLETE",
            "ehrReference", "EHR-" + referralId,
            "ehrSyncMode", "SIMULATED"
        );
    }

    private String shortId(String value) {
        if (value == null || value.isBlank()) return "UNKNOWN";
        return value.substring(0, Math.min(8, value.length()));
    }

    private String safeCode(String value) {
        if (value == null || value.isBlank()) return "UNKNOWN";
        return value.toUpperCase().replaceAll("[^A-Z0-9]+", "-");
    }
}
