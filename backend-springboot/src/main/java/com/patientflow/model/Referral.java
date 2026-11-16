package com.patientflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referrals")
public class Referral {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String patientName;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(length = 1000)
    private String specialists;

    @Column(nullable = false)
    private Boolean consentGiven;

    @Column(nullable = false)
    private String status;

    @Column(length = 64)
    private String processInstanceKey;

    @Column(length = 32)
    private String workflowState;

    @Column
    private Boolean workflowIncident = false;

    private Instant workflowStatusCheckedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void initialize() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "SUBMITTED";
        if (workflowIncident == null) workflowIncident = false;
    }

    public String getId() { return id; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSpecialists() { return specialists; }
    public void setSpecialists(String specialists) { this.specialists = specialists; }
    public Boolean getConsentGiven() { return consentGiven; }
    public void setConsentGiven(Boolean consentGiven) { this.consentGiven = consentGiven; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProcessInstanceKey() { return processInstanceKey; }
    public void setProcessInstanceKey(String processInstanceKey) { this.processInstanceKey = processInstanceKey; }
    public String getWorkflowState() { return workflowState; }
    public void setWorkflowState(String workflowState) { this.workflowState = workflowState; }
    public Boolean getWorkflowIncident() { return workflowIncident; }
    public void setWorkflowIncident(Boolean workflowIncident) { this.workflowIncident = workflowIncident; }
    public Instant getWorkflowStatusCheckedAt() { return workflowStatusCheckedAt; }
    public void setWorkflowStatusCheckedAt(Instant workflowStatusCheckedAt) { this.workflowStatusCheckedAt = workflowStatusCheckedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
