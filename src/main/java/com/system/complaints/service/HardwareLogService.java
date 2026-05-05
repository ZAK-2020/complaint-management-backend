package com.system.complaints.service;

import com.system.complaints.model.ComplaintHistory;
import com.system.complaints.model.ComplaintLog;
import com.system.complaints.model.HardwareLog;
import com.system.complaints.model.HardwareReport;
import com.system.complaints.repository.ComplaintHistoryRepository;
import com.system.complaints.repository.ComplaintLogRepository;
import com.system.complaints.repository.HardwareLogRepository;
import com.system.complaints.repository.HardwareReportRepository;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class HardwareLogService {

    @Autowired
    private HardwareLogRepository hardwareLogRepository;

    @Autowired
    private ComplaintLogRepository complaintLogRepository;

    @Autowired
    private ComplaintHistoryRepository complaintHistoryRepository;

    @Autowired
    private HardwareReportRepository hardwareReportRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // NEW: publish domain-change events so a listener can broadcast AFTER_COMMIT
    @Autowired
    private ApplicationEventPublisher events;

    /** Simple event type to signal “hardware domain changed”. */
    public static class HardwareDomainChangedEvent {}

    /** Fire an event (to be picked by a @TransactionalEventListener AFTER_COMMIT). */
    private void notifyHardwareChanged() {
        events.publishEvent(new HardwareDomainChangedEvent());
    }

    // Helper to get the logged-in username
    private String getLoggedInUsername() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymousUser";
        }
        Object principal = authentication.getPrincipal();
        return (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
    }

    // Save hardware changes to ComplaintHistory
    public void saveHardwareHistory(String complaintId, String fieldName, String oldValue, String newValue, String reason) {
        ComplaintHistory history = new ComplaintHistory();
        history.setComplaintId(complaintId);
        history.setFieldName(fieldName);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangeDate(new Timestamp(System.currentTimeMillis()));
        history.setChangedBy(getLoggedInUsername());
        history.setReasonForChange(reason);

        ComplaintLog complaintLog = complaintLogRepository.findByComplaintId(complaintId)
                .orElseThrow(() -> new RuntimeException("ComplaintLog not found: " + complaintId));
        history.setLoggedBy(complaintLog.getLoggedBy());

        complaintHistoryRepository.save(history);
    }

    // Generic field update helper
    private void updateField(Supplier<String> getter, Consumer<String> setter, Object newValue,
                             String complaintId, String fieldName, String reason) {
        String oldVal = getter.get();
        String newVal = (newValue != null) ? newValue.toString() : null;

        if (!Objects.equals(oldVal, newVal)) {
            setter.accept(newVal);
            saveHardwareHistory(
                    complaintId,
                    fieldName,
                    oldVal,
                    newVal,
                    reason
            );
        }
    }

    // Date field update helper
    private void updateDateField(Supplier<Date> getter, Consumer<Date> setter, Object newValue,
                                 String complaintId, String fieldName, String reason) {
        Date oldVal = getter.get();
        Date newVal = parseDate(newValue);

        if (!Objects.equals(oldVal, newVal)) {
            setter.accept(newVal);
            saveHardwareHistory(
                    complaintId,
                    fieldName,
                    (oldVal != null) ? oldVal.toString() : null,
                    (newVal != null) ? newVal.toString() : null,
                    reason
            );
        }
    }

    /**
     * Update a HardwareLog by Complaint ID with history tracking.
     * Note: The case for "report" has been removed because multiple reports are now handled separately.
     */
    @Transactional
    public HardwareLog updateHardwareLogByComplaintId(String complaintId, Map<String, Object> updates) {
        HardwareLog hardwareLog = getHardwareLogByComplaintId(complaintId);

        updates.forEach((key, value) -> {
            if (value != null) {
                switch (key) {
                    case "dispatchInwardDate":
                        updateDateField(hardwareLog::getDispatchInwardDate, hardwareLog::setDispatchInwardDate, value,
                                complaintId, "hardware.dispatchInwardDate", "Dispatch inward date updated");
                        break;
                    case "receivedInwardDate":
                        updateDateField(hardwareLog::getReceivedInwardDate, hardwareLog::setReceivedInwardDate, value,
                                complaintId, "hardware.receivedInwardDate", "Received inward date updated");
                        break;
                    case "dispatchOutwardDate":
                        updateDateField(hardwareLog::getDispatchOutwardDate, hardwareLog::setDispatchOutwardDate, value,
                                complaintId, "hardware.dispatchOutwardDate", "Dispatch outward date updated");
                        break;
                    case "receivedOutwardDate":
                        updateDateField(hardwareLog::getReceivedOutwardDate, hardwareLog::setReceivedOutwardDate, value,
                                complaintId, "hardware.receivedOutwardDate", "Received outward date updated");
                        break;

                    case "receivingCnNumber":
                        updateField(hardwareLog::getReceivingCnNumber, hardwareLog::setReceivingCnNumber, value,
                                complaintId, "hardware.receivingCnNumber", "Receiving CN number updated");
                        break;
                    case "dispatchCnNumber":
                        updateField(hardwareLog::getDispatchCnNumber, hardwareLog::setDispatchCnNumber, value,
                                complaintId, "hardware.dispatchCnNumber", "Dispatch CN number updated");
                        break;
                    case "hOkDate":
                        updateDateField(hardwareLog::getHOkDate, hardwareLog::setHOkDate, value,
                                complaintId, "hardware.hOkDate", "Hardware OK date updated");
                        break;
                    case "outOfStockDate":
                        updateDateField(
                                hardwareLog::getOutOfStockDate,
                                hardwareLog::setOutOfStockDate,
                                value,
                                complaintId,
                                "hardware.outOfStockDate",
                                "Out Of Stock date updated"
                        );
                        break;
                    case "remarks":
                        updateField(hardwareLog::getRemarks, hardwareLog::setRemarks, value,
                                complaintId, "hardware.remarks", "Remarks updated");
                        break;
                    case "equipmentDescription":
                        updateField(hardwareLog::getEquipmentDescription, hardwareLog::setEquipmentDescription, value,
                                complaintId, "hardware.equipmentDescription", "Equipment description updated");
                        break;
                    case "problem":
                        updateField(hardwareLog::getProblem, hardwareLog::setProblem, value,
                                complaintId, "hardware.problem", "Problem description updated");
                        break;

                    case "labStatus":
                        updateField(hardwareLog::getLabStatus, hardwareLog::setLabStatus, value,
                                complaintId, "hardware.labStatus", "Lab status updated");
                        break;
                    case "courierStatus":
                        updateField(hardwareLog::getCourierStatus, hardwareLog::setCourierStatus, value,
                                complaintId, "hardware.courierStatus", "Courier status updated");
                        break;

                    case "extraHardware":
                        updateField(hardwareLog::getExtraHardware, hardwareLog::setExtraHardware, value,
                                complaintId, "hardware.extraHardware", "Extra hardware updated");
                        break;
                    case "labEngineer":
                        updateField(hardwareLog::getLabEngineer, hardwareLog::setLabEngineer, value,
                                complaintId, "hardware.labEngineer", "Lab engineer assigned");
                        break;
                    case "done":
                        Boolean oldDone = hardwareLog.getDone();
                        Boolean newDone = (value instanceof Boolean) ? (Boolean) value : Boolean.valueOf(value.toString());
                        if (!Objects.equals(oldDone, newDone)) {
                            hardwareLog.setDone(newDone);
                            saveHardwareHistory(
                                    complaintId,
                                    "hardware.done",
                                    (oldDone != null ? oldDone.toString() : null),
                                    (newDone != null ? newDone.toString() : null),
                                    "Done status updated"
                            );
                        }
                        break;

                    default:
                        throw new IllegalArgumentException("Unexpected field: " + key);
                }
            }
        });

        HardwareLog saved = hardwareLogRepository.save(hardwareLog);
        // NEW: notify after mutation
        notifyHardwareChanged();
        return saved;
    }

    /**
     * Save a new HardwareLog.
     */
    @Transactional
    public HardwareLog saveHardwareLog(HardwareLog hardwareLog) {
        validateHardwareLog(hardwareLog);
        HardwareLog saved = hardwareLogRepository.save(hardwareLog);
        // NEW: notify after mutation
        notifyHardwareChanged();
        return saved;
    }

    /**
     * Retrieve a HardwareLog by Complaint ID.
     */
    public HardwareLog getHardwareLogByComplaintId(String complaintId) {
        ComplaintLog complaintLog = fetchComplaintLogByComplaintId(complaintId);
        return hardwareLogRepository.findByComplaintLog(complaintLog)
                .orElseThrow(() -> new RuntimeException("HardwareLog not found for complaintId: " + complaintId));
    }

    /**
     * Validate a HardwareLog object.
     */
    private void validateHardwareLog(HardwareLog hardwareLog) {
        if (hardwareLog.getDispatchCnNumber() == null || hardwareLog.getDispatchCnNumber().isEmpty()) {
            throw new IllegalArgumentException("Dispatch CN Number cannot be empty");
        }
    }

    /**
     * Parse an Object into a java.sql.Date.
     */
    private Date parseDate(Object value) {
        try {
            return Date.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid date format. Expected format: yyyy-MM-dd", e);
        }
    }

    /**
     * Fetch a ComplaintLog by complaintId.
     */
    private ComplaintLog fetchComplaintLogByComplaintId(String complaintId) {
        return complaintLogRepository.findByComplaintId(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found with complaintId: " + complaintId));
    }

    @Transactional
    public void createHardwareLogIfHardwarePicked(String complaintId) {
        ComplaintLog complaintLog = fetchComplaintLogByComplaintId(complaintId);
        List<String> validStatuses = Arrays.asList("Hardware Picked", "FOC", "Quotation");

        if (validStatuses.contains(complaintLog.getComplaintStatus())) {
            boolean isNew = false;
            HardwareLog hardwareLog;

            Optional<HardwareLog> existingLog = hardwareLogRepository.findByComplaintLog(complaintLog);
            if (existingLog.isEmpty()) {
                hardwareLog = new HardwareLog();
                hardwareLog.setComplaintLog(complaintLog);
                isNew = true;
            } else {
                hardwareLog = existingLog.get();
            }

            // Check if status is "Hardware Picked" and city is "Karachi"
            if ("Hardware Picked".equals(complaintLog.getComplaintStatus())
                    && "Karachi".equalsIgnoreCase(complaintLog.getCity())) {
                String oldCourierStatus = hardwareLog.getCourierStatus();
                if (!"Received Inward".equals(oldCourierStatus)) {
                    hardwareLog.setCourierStatus("Received Inward");

                    // Log the change in history
                    saveHardwareHistory(
                            complaintId,
                            "hardware.courierStatus",
                            oldCourierStatus,
                            "Received Inward",
                            "Automatically set due to Hardware Picked status and Karachi city"
                    );
                }
            }

            // Save the hardware log if new or updated
            if (isNew || hardwareLog.getCourierStatus() != null) {
                hardwareLogRepository.save(hardwareLog);
                // NEW: notify after mutation
                notifyHardwareChanged();
            }
        }
    }

    // NEW METHODS FOR MULTIPLE REPORTS

    /**
     * Add a new HardwareReport to the HardwareLog identified by complaintId.
     */
    @Transactional
    public HardwareReport addReportToHardwareLog(String complaintId, String reportContent, String createdBy) {
        HardwareLog hardwareLog = getHardwareLogByComplaintId(complaintId);

        // Create a new HardwareReport instance
        HardwareReport report = new HardwareReport(reportContent, createdBy, hardwareLog);
        // Use helper method to maintain the bidirectional relation
        hardwareLog.addReport(report);
        // Persist the changes (cascade will save the report as well)
        hardwareLogRepository.save(hardwareLog);

        // NEW: notify after mutation
        notifyHardwareChanged();

        return report;
    }

    /**
     * Retrieve all HardwareReports for the HardwareLog associated with the given complaintId.
     */
    public List<HardwareReport> getReportsByComplaintId(String complaintId) {
        Optional<ComplaintLog> complaintLogOpt = complaintLogRepository.findByComplaintId(complaintId);
        if (complaintLogOpt.isPresent()) {
            Optional<HardwareLog> hardwareLogOpt = hardwareLogRepository.findByComplaintLog(complaintLogOpt.get());
            if (hardwareLogOpt.isPresent()) {
                // Convert Set to List for return
                return new ArrayList<>(hardwareLogOpt.get().getReports());
            }
        }
        return Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> getReportAvailabilityMap(List<String> complaintIds) {
        if (complaintIds == null || complaintIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> sanitizedComplaintIds = complaintIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .toList();

        if (sanitizedComplaintIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> complaintIdsWithReports = new HashSet<>(
                hardwareReportRepository.findComplaintIdsWithReports(sanitizedComplaintIds)
        );

        Map<String, Boolean> availabilityMap = new LinkedHashMap<>();
        for (String complaintId : sanitizedComplaintIds) {
            availabilityMap.put(complaintId, complaintIdsWithReports.contains(complaintId));
        }

        return availabilityMap;
    }

    public List<Map<String, Object>> getTodayHardwareDispatchDetail() {
        Date today = Date.valueOf(LocalDate.now());

        List<HardwareLog> logs = hardwareLogRepository.findAllByDispatchOutwardDate(today);
        List<Map<String, Object>> result = new ArrayList<>();
        int srNo = 1;
        for (HardwareLog hl : logs) {
            ComplaintLog cl = hl.getComplaintLog();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("SNo", srNo++);
            row.put("Bank", cl.getBankName());
            row.put("Code", cl.getBranchCode());
            row.put("BranchName", cl.getBranchName());
            row.put("City", cl.getCity());
            row.put("Engineer", cl.getVisitorName());
            row.put("Dispatch", hl.getDispatchOutwardDate());
            row.put("CN", hl.getDispatchCnNumber());
            row.put("EquipmentDetail", hl.getEquipmentDescription());
            result.add(row);
        }
        return result;
    }

    public List<HardwareLog> getAllHardwareLogsAssignedTo(String username) {
        return hardwareLogRepository.findByLabEngineerIgnoreCase(username);
    }

    public HardwareLog getHardwareLogById(Long id) {
        return hardwareLogRepository.findById(id).orElse(null);
    }

    @Transactional
    public HardwareLog saveHardwareLogWithoutValidation(HardwareLog log) {
        // Just save, skip validation
        HardwareLog saved = hardwareLogRepository.save(log);
        // NEW: notify after mutation
        notifyHardwareChanged();
        return saved;
    }

    public List<HardwareLog> getAllHardwareLogsWithParts() {
        return hardwareLogRepository.findAllWithParts();
    }

    public List<HardwareLog> getHardwareLogsWithPartsAssignedTo(String username) {
        return hardwareLogRepository.findAllWithPartsAndReportsAssignedTo(username);
    }

    public Page<HardwareLog> searchHardwareLogs(
            String bankName,
            String city,
            String complaintStatus,
            List<String> courierStatus,
            String branchName,
            String branchCode,
            String equipment,
            String complaintId,
            String cnNumber,
            String dispatchInwardDate,
            String receivedInwardDate,
            String dispatchOutwardDate,
            String receivedOutwardDate,
            String hardwarePickedDate,
            String hardwareOKDate,
            String workedTodayByUser,
            String labEngineer,
            Boolean done,
            Boolean onlyFOCApproved,
            Pageable pageable
    ) {
        Specification<HardwareLog> spec = Specification.where(null);

        // --- ComplaintLog fields ---
        if (bankName != null && !bankName.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("bankName")), "%" + bankName.toLowerCase() + "%"));
        }
        if (city != null && !city.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("city")), "%" + city.toLowerCase() + "%"));
        }
        if (branchName != null && !branchName.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("branchName")), "%" + branchName.toLowerCase() + "%"));
        }
        if (branchCode != null && !branchCode.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("branchCode")), "%" + branchCode.toLowerCase() + "%"));
        }
        if (complaintId != null && !complaintId.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("complaintId")), "%" + complaintId.toLowerCase() + "%"));
        }
        if (complaintStatus != null && !complaintStatus.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("complaintStatus")), "%" + complaintStatus.toLowerCase() + "%"));
        }

        // --- HardwareLog fields ---
        if (courierStatus != null && !courierStatus.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("courierStatus").in(courierStatus));
        }
        if (equipment != null && !equipment.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("equipmentDescription")), "%" + equipment.toLowerCase() + "%"));
        }
        if (cnNumber != null && !cnNumber.isEmpty()) {
            // Will match either CN number field
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("dispatchCnNumber")), "%" + cnNumber.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("receivingCnNumber")), "%" + cnNumber.toLowerCase() + "%")
            ));
        }
        if (workedTodayByUser != null && !workedTodayByUser.isEmpty()) {
            // If you have a 'workedTodayByUser' field (doesn't exist in your model, but if you add one)
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("workedTodayByUser")), "%" + workedTodayByUser.toLowerCase() + "%"));
        }
        if (labEngineer != null && !labEngineer.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("labEngineer")), "%" + labEngineer.toLowerCase() + "%"));
        }
        if (done != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("done"), done));
        }

        // Only FOC/Approved/Hardware Ready from ComplaintLog
        if (onlyFOCApproved != null && onlyFOCApproved) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.equal(cb.lower(root.join("complaintLog").get("complaintStatus")), "foc"),
                            cb.equal(cb.lower(root.join("complaintLog").get("complaintStatus")), "approved"),
                            cb.equal(cb.lower(root.join("complaintLog").get("complaintStatus")), "hardware ready")
                    ));
        }

        // --- Date Filters (java.sql.Date.valueOf) ---
        if (dispatchInwardDate != null && !dispatchInwardDate.isEmpty()) {
            try {
                Date date = Date.valueOf(dispatchInwardDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("dispatchInwardDate"), date));
            } catch (Exception e) {}
        }
        if (receivedInwardDate != null && !receivedInwardDate.isEmpty()) {
            try {
                Date date = Date.valueOf(receivedInwardDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("receivedInwardDate"), date));
            } catch (Exception e) {}
        }
        if (dispatchOutwardDate != null && !dispatchOutwardDate.isEmpty()) {
            try {
                Date date = Date.valueOf(dispatchOutwardDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("dispatchOutwardDate"), date));
            } catch (Exception e) {}
        }
        if (receivedOutwardDate != null && !receivedOutwardDate.isEmpty()) {
            try {
                Date date = Date.valueOf(receivedOutwardDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("receivedOutwardDate"), date));
            } catch (Exception e) {}
        }
        // For hardwarePickedDate, you might want from ComplaintLog:
        if (hardwarePickedDate != null && !hardwarePickedDate.isEmpty()) {
            try {
                Date date = Date.valueOf(hardwarePickedDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.join("complaintLog").get("hardwarePickedDate"), date));
            } catch (Exception e) {}
        }
        if (hardwareOKDate != null && !hardwareOKDate.isEmpty()) {
            try {
                Date date = Date.valueOf(hardwareOKDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("hOkDate"), date));
            } catch (Exception e) {}
        }

        return hardwareLogRepository.findAll(spec, pageable);
    }

    public Page<HardwareLog> searchLabHardwareLogs(
            String bankName,
            String city,
            String complaintStatus,
            List<String> courierStatus,
            String branchName,
            String branchCode,
            String equipment,
            String complaintId,
            String cnNumber,
            String dispatchInwardDate,
            String receivedInwardDate,
            String dispatchOutwardDate,
            String receivedOutwardDate,
            String hardwarePickedDate,
            String hardwareOKDate,
            String workedTodayByUser,
            String labEngineer,
            Boolean done,
            Boolean onlyFOCApproved,
            Pageable pageable
    ) {
        Specification<HardwareLog> spec = Specification.where(null);

        // --- Regular Filters (Same as /paginated) ---
        if (bankName != null && !bankName.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("bankName")), "%" + bankName.toLowerCase() + "%"));
        }
        if (city != null && !city.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("city")), "%" + city.toLowerCase() + "%"));
        }
        if (branchName != null && !branchName.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("branchName")), "%" + branchName.toLowerCase() + "%"));
        }
        if (branchCode != null && !branchCode.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("branchCode")), "%" + branchCode.toLowerCase() + "%"));
        }
        if (complaintId != null && !complaintId.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("complaintId")), "%" + complaintId.toLowerCase() + "%"));
        }
        if (complaintStatus != null && !complaintStatus.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.join("complaintLog").get("complaintStatus")), "%" + complaintStatus.toLowerCase() + "%"));
        }
        if (courierStatus != null && !courierStatus.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("courierStatus").in(courierStatus));
        }
        if (equipment != null && !equipment.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("equipmentDescription")), "%" + equipment.toLowerCase() + "%"));
        }
        if (cnNumber != null && !cnNumber.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("dispatchCnNumber")), "%" + cnNumber.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("receivingCnNumber")), "%" + cnNumber.toLowerCase() + "%")
            ));
        }
        if (workedTodayByUser != null && !workedTodayByUser.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                // Subquery to find HardwareLog IDs with a report by this user today
                jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<HardwareReport> reportRoot = subquery.from(HardwareReport.class);
                subquery.select(reportRoot.get("hardwareLog").get("id"));
                subquery.where(
                        cb.equal(reportRoot.get("hardwareLog"), root),
                        cb.equal(cb.lower(reportRoot.get("createdBy")), workedTodayByUser.toLowerCase()),
                        cb.equal(
                                cb.function("date", Date.class, reportRoot.get("createdAt")),
                                java.sql.Date.valueOf(LocalDate.now())
                        )
                );
                return cb.exists(subquery);
            });
        }

        if (labEngineer != null && !labEngineer.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("labEngineer")), "%" + labEngineer.toLowerCase() + "%"));
        }
        if (done != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("done"), done));
        }
        if (onlyFOCApproved != null && onlyFOCApproved) {
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.equal(cb.lower(root.get("courierStatus")), "hardware ready"),
                    cb.or(
                            cb.equal(cb.lower(root.join("complaintLog").get("complaintStatus")), "foc"),
                            cb.equal(cb.lower(root.join("complaintLog").get("complaintStatus")), "approved"),
                            cb.equal(cb.lower(root.join("complaintLog").get("complaintStatus")), "pre approved")
                    )
            ));
        }

        if (dispatchInwardDate != null && !dispatchInwardDate.isEmpty()) {
            try {
                Date date = Date.valueOf(dispatchInwardDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("dispatchInwardDate"), date));
            } catch (Exception e) {}
        }
        if (receivedInwardDate != null && !receivedInwardDate.isEmpty()) {
            try {
                Date date = Date.valueOf(receivedInwardDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("receivedInwardDate"), date));
            } catch (Exception e) {}
        }
        if (dispatchOutwardDate != null && !dispatchOutwardDate.isEmpty()) {
            try {
                Date date = Date.valueOf(dispatchOutwardDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("dispatchOutwardDate"), date));
            } catch (Exception e) {}
        }
        if (receivedOutwardDate != null && !receivedOutwardDate.isEmpty()) {
            try {
                Date date = Date.valueOf(receivedOutwardDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("receivedOutwardDate"), date));
            } catch (Exception e) {}
        }
        if (hardwarePickedDate != null && !hardwarePickedDate.isEmpty()) {
            try {
                Date date = Date.valueOf(hardwarePickedDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.join("complaintLog").get("hardwarePickedDate"), date));
            } catch (Exception e) {}
        }
        if (hardwareOKDate != null && !hardwareOKDate.isEmpty()) {
            try {
                Date date = Date.valueOf(hardwareOKDate);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("hOkDate"), date));
            } catch (Exception e) {}
        }

        // --- LAB PAGE SPECIAL LOGIC ---
        List<String> allowedStatuses = Arrays.asList(
                "dispatch inward",
                "received inward",
                "hardware ready",
                "additional counter",
                "dispatch outward",
                "observation",
                "out of stock"
        );

        spec = spec.and((root, query, cb) -> {
            Expression<String> cityExpr = cb.lower(root.join("complaintLog").get("city"));
            Expression<String> courierStatusExpr = cb.lower(root.get("courierStatus"));
            Expression<String> complaintStatusExpr = cb.lower(root.join("complaintLog").get("complaintStatus"));
            Path<Boolean> dcGenerated = root.join("complaintLog").get("dcGenerated");

            Predicate notClosed = cb.and(
                    cb.notEqual(complaintStatusExpr, "closed"),
                    cb.notEqual(complaintStatusExpr, "pending for closed")
            );

            Predicate karachiReceivedOutward = cb.and(
                    cb.equal(cityExpr, "karachi"),
                    cb.equal(courierStatusExpr, "received outward"),
                    cb.equal(complaintStatusExpr, "approved"),
                    cb.isFalse(dcGenerated)
            );

            Predicate dispatchOutward = cb.and(
                    cb.equal(courierStatusExpr, "dispatch outward"),
                    cb.equal(complaintStatusExpr, "approved"),
                    cb.isFalse(dcGenerated)
            );

            Predicate allowedStatus = courierStatusExpr.in(allowedStatuses);

            // Exclude "Received Outward" for non-Karachi
            Predicate nonKarachiReceivedOutward = cb.and(
                    cb.notEqual(cityExpr, "karachi"),
                    cb.equal(courierStatusExpr, "received outward")
            );

            return cb.and(
                    notClosed,
                    allowedStatus,
                    cb.or(
                            karachiReceivedOutward,
                            dispatchOutward,
                            cb.and(
                                    cb.notEqual(courierStatusExpr, "received outward"),
                                    cb.notEqual(courierStatusExpr, "dispatch outward")
                            )
                    ),
                    cb.not(nonKarachiReceivedOutward)
            );
        });

        return hardwareLogRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCourierStatusCounts() {
        List<Object[]> rows = hardwareLogRepository.getCourierStatusCountsWithHardwareReadyBreakdown();
        Map<String, Object> out = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String status = (String) r[0];
            Long count = (Long) r[1];

            if ("hardware ready".equals(status)) {
                Long hardwareReadyApprovedFoc = (Long) r[2];
                Long hardwareReadyWaitApproval = (Long) r[3];

                Map<String, Long> counts = new LinkedHashMap<>();
                counts.put("total", count);
                counts.put("hardwareReadyApprovedFoc", hardwareReadyApprovedFoc);
                counts.put("hardwareReadyWaitApproval", hardwareReadyWaitApproval);

                out.put(status, counts);
            } else {
                out.put(status, Map.of("total", count));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, Long>> getTrendsPerDate() {
        List<Object[]> rows = hardwareLogRepository.getAllTrendsPerDate();

        // Outer key = date string, inner key = type ("hardwareReady", "dispatchInward"...)
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();

        for (Object[] r : rows) {
            String type = (String) r[0];       // alias "type" from the query
            String date = r[1].toString();     // e.g. "2025-09-22"
            Long count = ((Number) r[2]).longValue();

            out.computeIfAbsent(date, k -> new LinkedHashMap<>())
                    .put(type, count);
        }
        return out;
    }

    /** Broadcast courier status counts to WebSocket subscribers. */
    public void broadcastCourierStatusCounts() {
        Map<String, Object> counts = getCourierStatusCounts();
        messagingTemplate.convertAndSend("/topic/courier-status", counts);
    }

    /** Broadcast trends-per-date data to WebSocket subscribers. */
    public void broadcastTrendsPerDate() {
        Map<String, Map<String, Long>> trends = getTrendsPerDate();
        messagingTemplate.convertAndSend("/topic/courier-trends", trends);
    }
}
