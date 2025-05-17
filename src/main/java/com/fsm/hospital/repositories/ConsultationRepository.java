package com.fsm.hospital.repositories;

import com.fsm.hospital.entities.Consultation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationRepository extends JpaRepository<Consultation,Long> {
}
