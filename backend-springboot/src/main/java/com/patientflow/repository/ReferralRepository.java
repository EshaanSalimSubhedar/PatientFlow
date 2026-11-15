package com.patientflow.repository;
import com.patientflow.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReferralRepository extends JpaRepository<Referral,String> {}
