package com.system.complaints.service;

import com.system.complaints.model.HardwarePart;
import com.system.complaints.model.ComplaintLog;
import com.system.complaints.repository.HardwarePartRepository;
import com.system.complaints.repository.ComplaintLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class HardwarePartService {

    @Autowired
    private HardwarePartRepository hardwarePartRepository;

    @Autowired
    private ComplaintLogRepository complaintLogRepository;

    private HardwarePart findRequiredPart(Long partId) {
        return hardwarePartRepository.findById(partId)
                .orElseThrow(() -> new NoSuchElementException("HardwarePart not found with id: " + partId));
    }

    /**
     * Add a HardwarePart to a ComplaintLog.
     */
    public HardwarePart addHardwarePart(Long complaintLogId, HardwarePart hardwarePart) {
        ComplaintLog complaintLog = complaintLogRepository.findById(complaintLogId)
                .orElseThrow(() -> new RuntimeException("ComplaintLog not found with id: " + complaintLogId));
        hardwarePart.setComplaintLog(complaintLog);
        hardwarePart.setStatus("pending");
        hardwarePart.setAssignedAt(LocalDateTime.now());
        return hardwarePartRepository.save(hardwarePart);
    }

    /**
     * Get all HardwareParts for a ComplaintLog.
     */
    public List<HardwarePart> getHardwarePartsByComplaintLog(Long complaintLogId) {
        return hardwarePartRepository.findByComplaintLogId(complaintLogId);
    }

    /**
     * Get a HardwarePart by id.
     */
    public Optional<HardwarePart> getHardwarePartById(Long partId) {
        return hardwarePartRepository.findById(partId);
    }

    /**
     * Update a HardwarePart.
     */
    public HardwarePart updateHardwarePart(Long partId, HardwarePart updatedPart) {
        HardwarePart part = findRequiredPart(partId);

        String oldEngineer = part.getAssignedEngineer();

        part.setHardwareName(updatedPart.getHardwareName());
        part.setAvailableWithEngineer(updatedPart.getAvailableWithEngineer());
        part.setRepaired(updatedPart.getRepaired());
        part.setAssignedEngineer(updatedPart.getAssignedEngineer());
        part.setNotRepairable(updatedPart.getNotRepairable());

        // Optionally update timestamps for status changes
        if (updatedPart.getStatus() != null && !updatedPart.getStatus().equals(part.getStatus())) {
            part.setStatus(updatedPart.getStatus());
            LocalDateTime now = LocalDateTime.now();
            switch (updatedPart.getStatus()) {
                case "pending":
                    part.setAssignedAt(now);
                    break;
                case "accepted":
                    part.setAcceptedAt(now);
                    break;
                case "repaired":
                    part.setRepairedAt(now);
                    break;
                case "not_repairable":
                    part.setNotRepairableAt(now);
                    break;
                // Add more cases if you have other statuses
            }
        }

        HardwarePart savedPart = hardwarePartRepository.save(part);

        // Example: Simple engineer change log (optional)
        if ((oldEngineer == null && part.getAssignedEngineer() != null) ||
                (oldEngineer != null && !oldEngineer.equals(part.getAssignedEngineer()))) {
            System.out.println("Assigned engineer changed for hardware part [id=" + part.getId()
                    + "]: from '" + oldEngineer + "' to '" + part.getAssignedEngineer() + "'");
            // You can replace this with your own logging mechanism or service call
        }

        return savedPart;
    }

    public HardwarePart markPartRepaired(Long partId) {
        HardwarePart part = findRequiredPart(partId);
        part.setStatus("repaired");
        part.setRepaired(true);
        part.setRepairedAt(LocalDateTime.now());
        return hardwarePartRepository.save(part);
    }

    public HardwarePart markPartNotRepairable(Long partId) {
        HardwarePart part = findRequiredPart(partId);
        part.setStatus("not_repairable");
        part.setNotRepairableAt(LocalDateTime.now());
        part.setNotRepairable(true);
        return hardwarePartRepository.save(part);
    }

    public void deleteHardwarePart(Long partId) {
        if (!hardwarePartRepository.existsById(partId)) {
            throw new NoSuchElementException("HardwarePart not found with id: " + partId);
        }
        hardwarePartRepository.deleteById(partId);
    }

}

