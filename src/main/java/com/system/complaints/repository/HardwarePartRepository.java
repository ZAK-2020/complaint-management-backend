package com.system.complaints.repository;

import com.system.complaints.model.HardwarePart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HardwarePartRepository extends JpaRepository<HardwarePart, Long> {
    List<HardwarePart> findByComplaintLogId(Long complaintLogId);
}
