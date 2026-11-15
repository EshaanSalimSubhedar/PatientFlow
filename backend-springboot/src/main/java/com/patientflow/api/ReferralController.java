package com.patientflow.api;
import com.patientflow.model.Referral;
import com.patientflow.repository.ReferralRepository;
import io.camunda.client.CamundaClient;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api")
@CrossOrigin(origins="*")
public class ReferralController {
 private final ReferralRepository repository;
 private final Optional<CamundaClient> camunda;
 public ReferralController(ReferralRepository repository, Optional<CamundaClient> camunda) {
  this.repository=repository; this.camunda=camunda;
 }
 @PostMapping("/referrals")
 public ResponseEntity<?> create(@RequestBody Map<String,Object> body) {
  Referral r=new Referral();
  r.setPatientName(String.valueOf(body.getOrDefault("patientName","")));
  r.setAge(Integer.valueOf(String.valueOf(body.getOrDefault("age","0"))));
  r.setReason(String.valueOf(body.getOrDefault("reason","")));
  Object specs=body.getOrDefault("specialists", List.of("Cardiology","Radiology","Dietetics"));
  r.setSpecialists(specs instanceof List<?> l ? String.join(",",l.stream().map(String::valueOf).toList()) : String.valueOf(specs));
  r.setConsentGiven(Boolean.parseBoolean(String.valueOf(body.getOrDefault("consentGiven",false))));
  r.setStatus("SUBMITTED");
  repository.save(r);
  Map<String,Object> vars=new HashMap<>();
  vars.put("referralId",r.getId()); vars.put("patientName",r.getPatientName()); vars.put("age",r.getAge());
  vars.put("reason",r.getReason()); vars.put("specialists",Arrays.asList(r.getSpecialists().split(",")));
  vars.put("consentGiven",r.getConsentGiven());
  boolean started=false;
  try {
   if(camunda.isPresent()) {
    camunda.get().newCreateInstanceCommand().bpmnProcessId("patient-referral-triage").latestVersion().variables(vars).send().join();
    started=true; r.setStatus("WORKFLOW_STARTED"); repository.save(r);
   }
  } catch(Exception ignored) { r.setStatus("SAVED_ENGINE_NOT_CONNECTED"); repository.save(r); }
  return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("referral",r,"workflowStarted",started));
 }
 @GetMapping("/referrals") public List<Referral> all(){return repository.findAll();}
 @GetMapping("/referrals/{id}") public ResponseEntity<Referral> get(@PathVariable String id){
  return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
 }
 @GetMapping("/health") public Map<String,String> health(){return Map.of("status","UP","service","PatientFlow API");}
}
