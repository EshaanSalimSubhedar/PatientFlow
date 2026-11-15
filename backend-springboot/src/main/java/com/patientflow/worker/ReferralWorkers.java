package com.patientflow.worker;
import io.camunda.spring.client.annotation.JobWorker;
import io.camunda.spring.client.annotation.Variable;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class ReferralWorkers {
 @JobWorker(type="fetch-patient-vitals")
 public Map<String,Object> fetchVitals(@Variable String referralId, @Variable Integer age) {
  // Synthetic demo values only. Never use these thresholds for real clinical decisions.
  int pulse = age != null && age > 60 ? 108 : 82;
  boolean critical = pulse >= 120;
  return Map.of("pulse",pulse,"oxygenSaturation",98,"criticalResult",critical,"vitalsSource","MOCK_FHIR");
 }
 @JobWorker(type="manual-vitals-review")
 public Map<String,Object> manualVitals(){return Map.of("criticalResult",false,"manualReviewRequired",true);}
 @JobWorker(type="emergency-clinical-escalation")
 public Map<String,Object> emergency(@Variable String referralId){return Map.of("escalationStatus","ALERT_RAISED","escalationReference","ESC-"+referralId.substring(0,Math.min(8,referralId.length())));}
 @JobWorker(type="specialist-review")
 public Map<String,Object> specialist(@Variable String specialist, @Variable String referralId){
  return Map.of("specialist",specialist,"reviewStatus","PENDING_SPECIALIST_CONFIRMATION","referralId",referralId);
 }
 @JobWorker(type="specialist-sla-escalation")
 public Map<String,Object> sla(){return Map.of("slaStatus","OVERDUE_ESCALATED");}
 @JobWorker(type="validate-patient-consent")
 public Map<String,Object> consent(@Variable Boolean consentGiven){
  if(!Boolean.TRUE.equals(consentGiven)) return Map.of("consentStatus","CONSENT_REQUIRED");
  return Map.of("consentStatus","CONSENT_CONFIRMED");
 }
 @JobWorker(type="sync-ehr-record")
 public Map<String,Object> ehr(@Variable String referralId){return Map.of("ehrSyncStatus","MOCK_SYNC_COMPLETE","ehrReference","EHR-"+referralId);}
}
