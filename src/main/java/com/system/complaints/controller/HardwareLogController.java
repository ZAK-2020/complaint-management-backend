package com.system.complaints.controller;

import com.system.complaints.model.HardwareLog;
import com.system.complaints.model.ComplaintLog;
import com.system.complaints.model.HardwareReport;
import com.system.complaints.repository.HardwareReportRepository;
import com.system.complaints.service.HardwareLogService;
import com.system.complaints.service.ComplaintLogService;
import com.system.complaints.model.HardwarePart;
import com.system.complaints.service.HardwarePartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/hardware-logs")
public class HardwareLogController {

    @Autowired
    private HardwareLogService hardwareLogService;

    @Autowired
    private ComplaintLogService complaintLogService;

    @Autowired
    private HardwareReportRepository hardwareReportRepository;

    @Autowired
    private HardwarePartService hardwarePartService;

    /**
     * Create a new HardwareLog for a specific complaint.
     */
    @PostMapping("/{complaintId}")
    public ResponseEntity<HardwareLog> addHardwareLog(
            @PathVariable String complaintId,
            @RequestBody HardwareLog hardwareLog) {

        // Fetch the complaint log using the complaintId
        ComplaintLog complaintLog = complaintLogService.getComplaintByComplaintId(complaintId);

        // List of valid statuses for which we should create a hardware log
        List<String> validStatuses = Arrays.asList("Hardware Picked", "FOC", "Quotation");

        // Check if the complaint status is valid
        if (!validStatuses.contains(complaintLog.getComplaintStatus())) {
            return ResponseEntity.badRequest().body(null);
        }

        // Associate the hardware log with the complaint
        hardwareLog.setComplaintLog(complaintLog);

        // Save the hardware log
        HardwareLog savedLog = hardwareLogService.saveHardwareLog(hardwareLog);

        // NOTE: saveHardwareLog already notifies via service, no need to broadcast here.
        return ResponseEntity.ok(savedLog);
    }

    /**
     * Update a HardwareLog by Complaint ID.
     */
    @PutMapping("/{complaintId}")
    public ResponseEntity<HardwareLog> updateHardwareLog(
            @PathVariable String complaintId,
            @RequestBody Map<String, Object> updates) {
        try {
            // Service method updates and publishes change itself
            HardwareLog updatedLog = hardwareLogService.updateHardwareLogByComplaintId(complaintId, updates);
            return ResponseEntity.ok(updatedLog);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // NEW ENDPOINTS FOR MULTIPLE REPORTS

    /**
     * Endpoint to add a new HardwareReport for the complaint identified by complaintId.
     * POST /hardware-logs/{complaintId}/reports
     */
    @PostMapping("/{complaintId}/reports")
    public ResponseEntity<HardwareReport> addReportToHardwareLog(
            @PathVariable String complaintId,
            @RequestBody Map<String, String> payload) {
        String content = payload.get("content");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = authentication.getName();

        // Service method saves + notifies
        HardwareReport report = hardwareLogService.addReportToHardwareLog(complaintId, content, createdBy);
        return ResponseEntity.ok(report);
    }

    /**
     * Endpoint to retrieve all HardwareReports for the complaint identified by complaintId.
     * GET /hardware-logs/{complaintId}/reports
     */
    @GetMapping("/{complaintId}/reports")
    public ResponseEntity<List<HardwareReport>> getReportsByComplaintId(@PathVariable String complaintId) {
        List<HardwareReport> reports = hardwareLogService.getReportsByComplaintId(complaintId);
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/reports/availability")
    public ResponseEntity<Map<String, Boolean>> getReportAvailability(@RequestBody List<String> complaintIds) {
        Map<String, Boolean> availabilityMap = hardwareLogService.getReportAvailabilityMap(complaintIds);
        return ResponseEntity.ok(availabilityMap);
    }

    @GetMapping("/hardware-dispatch-detail")
    public ResponseEntity<List<Map<String, Object>>> getTodayHardwareDispatchDetail() {
        List<Map<String, Object>> detail = hardwareLogService.getTodayHardwareDispatchDetail();
        return ResponseEntity.ok(detail);
    }

    // GET /hardware-logs/assigned/{username}
    @GetMapping("/assigned/{username}")
    public ResponseEntity<List<HardwareLog>> getHardwareLogsAssignedToEngineer(
            @PathVariable String username
    ) {
        List<HardwareLog> logs = hardwareLogService.getAllHardwareLogsAssignedTo(username);
        return ResponseEntity.ok(logs);
    }

    @PatchMapping("/{hardwareLogId}/done")
    public ResponseEntity<?> markHardwareLogAsDone(
            @PathVariable Long hardwareLogId,
            @RequestBody Map<String, Boolean> payload
    ) {
        Boolean done = payload.getOrDefault("done", true);

        HardwareLog log = hardwareLogService.getHardwareLogById(hardwareLogId);
        if (log == null) {
            return ResponseEntity.notFound().build();
        }

        Boolean oldDone = log.getDone(); // Capture old value

        log.setDone(done);
        // Service method saves + notifies
        hardwareLogService.saveHardwareLogWithoutValidation(log);

        // Directly update history from controller:
        hardwareLogService.saveHardwareHistory(
                log.getComplaintLog().getComplaintId(),
                "hardware.done",
                oldDone != null ? oldDone.toString() : null,
                done != null ? done.toString() : null,
                done ? "Marked as done " : "Marked as not done"
        );

        return ResponseEntity.ok(log);
    }

    @GetMapping("/lab-user/reports-today")
    public ResponseEntity<Map<String, List<String>>> getComplaintsWithReportsByAllUsersToday() {
        List<Object[]> results = hardwareReportRepository.findComplaintIdsWithReportsByAllUsersToday();
        Map<String, List<String>> userToComplaints = new HashMap<>();
        for (Object[] row : results) {
            String user = (String) row[0];
            String complaintId = (String) row[1];
            userToComplaints.computeIfAbsent(user, k -> new ArrayList<>()).add(complaintId);
        }
        return ResponseEntity.ok(userToComplaints);
    }

    // ------------------------
    // Hardware Parts endpoints
    // ------------------------

    // Add a hardware part to a complaint log
    // POST /hardware-logs/{complaintId}/parts
    @PostMapping("/{complaintId}/parts")
    public ResponseEntity<HardwarePart> addHardwarePartToComplaint(
            @PathVariable Long complaintId,
            @RequestBody HardwarePart hardwarePart) {
        HardwarePart created = hardwarePartService.addHardwarePart(complaintId, hardwarePart);

        // Parts mutations live in HardwarePartService, so broadcast here
        hardwareLogService.broadcastCourierStatusCounts();
        hardwareLogService.broadcastTrendsPerDate();

        return ResponseEntity.ok(created);
    }

    // Get all hardware parts for a complaint log
    // GET /hardware-logs/{complaintId}/parts
    @GetMapping("/{complaintId}/parts")
    public ResponseEntity<List<HardwarePart>> getHardwarePartsForComplaint(
            @PathVariable Long complaintId) {
        List<HardwarePart> parts = hardwarePartService.getHardwarePartsByComplaintLog(complaintId);
        return ResponseEntity.ok(parts);
    }

    // Update a hardware part (PUT /hardware-logs/{complaintId}/parts/{partId})
    @PutMapping("/{complaintId}/parts/{partId}")
    public ResponseEntity<HardwarePart> updateHardwarePart(
            @PathVariable Long complaintId,
            @PathVariable Long partId,
            @RequestBody HardwarePart hardwarePart) {
        try {
            HardwarePart updated = hardwarePartService.updateHardwarePart(partId, hardwarePart);

            // Broadcast after mutation
            hardwareLogService.broadcastCourierStatusCounts();
            hardwareLogService.broadcastTrendsPerDate();

            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // Delete a hardware part
    @DeleteMapping("/{complaintId}/parts/{partId}")
    public ResponseEntity<Void> deleteHardwarePart(
            @PathVariable Long complaintId,
            @PathVariable Long partId) {
        try {
            hardwarePartService.deleteHardwarePart(partId);

            // Broadcast after mutation
            hardwareLogService.broadcastCourierStatusCounts();
            hardwareLogService.broadcastTrendsPerDate();

            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all HardwareLogs where at least one hardware part is assigned to the given engineer username.
     * GET /hardware-logs/parts-assigned/{username}
     */
    @GetMapping("/parts-assigned/{username}")
    public ResponseEntity<List<HardwareLog>> getHardwareLogsWithPartsAssignedToEngineer(
            @PathVariable String username) {

        List<HardwareLog> filteredLogs = hardwareLogService.getHardwareLogsWithPartsAssignedTo(username);
        return ResponseEntity.ok(filteredLogs);
    }

    // Mark a hardware part as repaired
    @PatchMapping("/parts/{partId}/repaired")
    public ResponseEntity<HardwarePart> markHardwarePartRepaired(@PathVariable Long partId) {
        try {
            HardwarePart updated = hardwarePartService.markPartRepaired(partId);

            // Broadcast after mutation
            hardwareLogService.broadcastCourierStatusCounts();
            hardwareLogService.broadcastTrendsPerDate();

            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // Mark a hardware part as not repairable
    @PatchMapping("/parts/{partId}/not-repairable")
    public ResponseEntity<HardwarePart> markHardwarePartNotRepairable(@PathVariable Long partId) {
        try {
            HardwarePart updated = hardwarePartService.markPartNotRepairable(partId);

            // Broadcast after mutation
            hardwareLogService.broadcastCourierStatusCounts();
            hardwareLogService.broadcastTrendsPerDate();

            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<HardwareLog>> getPaginatedHardwareLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,

            // --- Common filters ---
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String complaintStatus,
            @RequestParam(required = false) List<String> courierStatus,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String complaintId,
            @RequestParam(required = false) String cnNumber,

            // --- Incoming/Outgoing/Lab Hardware specific ---
            @RequestParam(required = false) String dispatchInwardDate,
            @RequestParam(required = false) String receivedInwardDate,
            @RequestParam(required = false) String dispatchOutwardDate,
            @RequestParam(required = false) String receivedOutwardDate,
            @RequestParam(required = false) String hardwarePickedDate,
            @RequestParam(required = false) String hardwareOKDate,
            @RequestParam(required = false) String workedTodayByUser,
            @RequestParam(required = false) String labEngineer,
            @RequestParam(required = false) Boolean done,
            @RequestParam(required = false, name = "onlyFOCApproved") Boolean onlyFOCApproved
    ) {
        try {
            Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<HardwareLog> pageResult = hardwareLogService.searchHardwareLogs(
                    bankName, city, complaintStatus, courierStatus, branchName, branchCode, equipment, complaintId, cnNumber,
                    dispatchInwardDate, receivedInwardDate, dispatchOutwardDate, receivedOutwardDate, hardwarePickedDate,
                    hardwareOKDate, workedTodayByUser, labEngineer, done, onlyFOCApproved, pageable
            );

            return ResponseEntity.ok(pageResult);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Page.empty());
        }
    }

    @GetMapping("/lab/paginated")
    public ResponseEntity<Page<HardwareLog>> getLabPaginatedHardwareLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,

            // --- Same filters as regular endpoint! ---
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String complaintStatus,
            @RequestParam(required = false) List<String> courierStatus,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String complaintId,
            @RequestParam(required = false) String cnNumber,
            @RequestParam(required = false) String dispatchInwardDate,
            @RequestParam(required = false) String receivedInwardDate,
            @RequestParam(required = false) String dispatchOutwardDate,
            @RequestParam(required = false) String receivedOutwardDate,
            @RequestParam(required = false) String hardwarePickedDate,
            @RequestParam(required = false) String hardwareOKDate,
            @RequestParam(required = false) String workedTodayByUser,
            @RequestParam(required = false) String labEngineer,
            @RequestParam(required = false) Boolean done,
            @RequestParam(required = false, name = "onlyFOCApproved") Boolean onlyFOCApproved
    ) {
        try {
            Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<HardwareLog> pageResult = hardwareLogService.searchLabHardwareLogs(
                    bankName, city, complaintStatus, courierStatus, branchName, branchCode, equipment, complaintId, cnNumber,
                    dispatchInwardDate, receivedInwardDate, dispatchOutwardDate, receivedOutwardDate, hardwarePickedDate,
                    hardwareOKDate, workedTodayByUser, labEngineer, done, onlyFOCApproved, pageable
            );

            return ResponseEntity.ok(pageResult);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Page.empty());
        }
    }

    // Read-only aggregates
    @GetMapping("/courier-status-counts")
    public ResponseEntity<Map<String, Object>> getCourierStatusCounts() {
        Map<String, Object> counts = hardwareLogService.getCourierStatusCounts();
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/trends-per-date")
    public ResponseEntity<Map<String, Map<String, Long>>> getTrendsPerDate() {
        return ResponseEntity.ok(hardwareLogService.getTrendsPerDate());
    }
}
