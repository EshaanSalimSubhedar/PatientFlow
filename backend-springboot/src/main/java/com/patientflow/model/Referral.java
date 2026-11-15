package com.patientflow.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
public class Referral {
 @Id private String id = UUID.randomUUID().toString();
 private String patientName;
 private Integer age;
 @Column(length=2000) private String reason;
 @Column(length=1000) private String specialists;
 private Boolean consentGiven;
 private String status = "SUBMITTED";
 private Instant createdAt = Instant.now();
 public Referral() {}
 public String getId(){return id;} public String getPatientName(){return patientName;}
 public void setPatientName(String v){patientName=v;} public Integer getAge(){return age;}
 public void setAge(Integer v){age=v;} public String getReason(){return reason;}
 public void setReason(String v){reason=v;} public String getSpecialists(){return specialists;}
 public void setSpecialists(String v){specialists=v;} public Boolean getConsentGiven(){return consentGiven;}
 public void setConsentGiven(Boolean v){consentGiven=v;} public String getStatus(){return status;}
 public void setStatus(String v){status=v;} public Instant getCreatedAt(){return createdAt;}
}
