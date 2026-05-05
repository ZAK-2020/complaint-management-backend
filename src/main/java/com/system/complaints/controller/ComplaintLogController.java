package com.system.complaints.controller;

import com.system.complaints.dto.BulkVisitScheduleRequest;
import com.system.complaints.dto.BulkVisitScheduleRequest.Entry;
import com.system.complaints.dto.BulkVisitScheduleResult;
import com.system.complaints.dto.ComplaintBranchGroupDTO;
import com.system.complaints.model.ComplaintLog;
import com.system.complaints.model.RemarksUpdate;
import com.system.complaints.repository.PendingForClosedLogRepository;
import com.system.complaints.service.BulkVisitSchedulingService;
import com.system.complaints.service.ComplaintLogService;
import com.system.complaints.service.RemarksUpdateService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/complaints")
public class ComplaintLogController {

    private static final int MAX_COMPLAINT_PAGE_SIZE = 100;

    @Autowired
    private ComplaintLogService complaintLogService;

    @Autowired
    private RemarksUpdateService remarksUpdateService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private BulkVisitSchedulingService bulkVisitSchedulingService;

    @Autowired
    private PendingForClosedLogRepository pendingForClosedLogRepository;

    @PostMapping("/log")
    public ResponseEntity<ComplaintLog> logComplaint(@RequestBody ComplaintLog complaintLog) {
        try {
            ComplaintLog savedLog = complaintLogService.saveComplaintLog(complaintLog);
            messagingTemplate.convertAndSend("/topic/paginated-by-status",
                    Map.of("complaintId", savedLog.getComplaintId(), "action", "created"));
            return ResponseEntity.ok(savedLog);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/{id}/remarks")
    public ResponseEntity<RemarksUpdate> addRemarksUpdate(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        try {
            String remarks = body.get("remarks");
            return ResponseEntity.ok(remarksUpdateService.addRemarksUpdate(id, remarks));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{id}/remarks/latest")
    public ResponseEntity<RemarksUpdate> getLatestRemarks(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(remarksUpdateService.getLatestRemarks(id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{id}/remarks/history")
    public ResponseEntity<List<RemarksUpdate>> getRemarksHistory(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(remarksUpdateService.getRemarksHistory(id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/remarks/history/batch")
    public ResponseEntity<Map<Long, List<RemarksUpdate>>> getRemarksHistoryBatch(
            @RequestBody List<Long> complaintIds) {
        try {
            return ResponseEntity.ok(remarksUpdateService.getRemarksHistoryBatch(complaintIds));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/remarks/counts")
    public ResponseEntity<Map<Long, Long>> getRemarksCounts(@RequestBody List<Long> complaintIds) {
        try {
            return ResponseEntity.ok(remarksUpdateService.getRemarksCountsBatch(complaintIds));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/engineers/history/batch")
    public ResponseEntity<Map<String, String>> getEngineerHistoryBatch(@RequestBody List<String> complaintIds) {
        try {
            return ResponseEntity.ok(complaintLogService.getEngineerHistoryBatch(complaintIds));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComplaintLog> updateComplaintLog(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        try {
            return complaintLogService.updateComplaintLogFields(id, updates)
                    .map(log -> {
                        messagingTemplate.convertAndSend("/topic/paginated-by-status",
                                Map.of("complaintId", log.getComplaintId(), "action", "updated"));
                        return ResponseEntity.ok(log);
                    })
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/todays-complaints")
    public ResponseEntity<Map<String, Integer>> getTodaysComplaintMetrics() {
        try {
            return ResponseEntity.ok(complaintLogService.getTodaysComplaintMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/city-wise-todays-metrics")
    public ResponseEntity<Map<String, Map<String, Integer>>> getCityWiseTodaysMetrics() {
        try {
            return ResponseEntity.ok(complaintLogService.getCityWiseTodaysComplaintMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/bank-wise-todays-metrics")
    public ResponseEntity<Map<String, Map<String, Integer>>> getBankWiseTodaysMetrics() {
        try {
            return ResponseEntity.ok(complaintLogService.getBankWiseTodaysComplaintMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/city-wise-all-metrics")
    public ResponseEntity<Map<String, Map<String, Integer>>> getCityWiseAllComplaintMetrics() {
        try {
            return ResponseEntity.ok(complaintLogService.getCityWiseAllComplaintMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/bank-wise-all-metrics")
    public ResponseEntity<Map<String, Map<String, Integer>>> getBankWiseAllComplaintMetrics() {
        try {
            return ResponseEntity.ok(complaintLogService.getBankWiseAllComplaintMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/overall-metrics")
    public ResponseEntity<Map<String, Integer>> getOverallComplaintMetrics() {
        try {
            return ResponseEntity.ok(complaintLogService.getOverallComplaintMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{complaintId}/history")
    public ResponseEntity<Map<String, Object>> getComplaintFullHistory(@PathVariable String complaintId) {
        try {
            return ResponseEntity.ok(complaintLogService.getFullComplaintHistory(complaintId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/complaints-summary")
    public ResponseEntity<Map<String, Object>> getComplaintDashboardSummary() {
        return ResponseEntity.ok(complaintLogService.getComplaintDashboardSummary());
    }

    @GetMapping("/city-wise-summary")
    public ResponseEntity<Map<String, Object>> getCityWiseSummary() {
        return ResponseEntity.ok(complaintLogService.getCityWiseEngineerSummary());
    }

    @PatchMapping("/{id}/dc-generated")
    public ResponseEntity<?> updateDcGenerated(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload
    ) {
        Boolean dcGenerated = payload.get("dcGenerated");
        try {
            ComplaintLog complaintLog = complaintLogService.getComplaintById(id);
            Boolean oldVal = complaintLog.getDcGenerated();
            complaintLog.setDcGenerated(dcGenerated);
            complaintLogService.updateOnlyDcGenerated(complaintLog);
            complaintLogService.saveComplaintHistory(
                    complaintLog.getComplaintId(),
                    "dcGenerated",
                    oldVal == null ? "null" : oldVal.toString(),
                    dcGenerated == null ? "null" : dcGenerated.toString(),
                    "DC Generated"
            );
            return ResponseEntity.ok(complaintLog);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to update dcGenerated");
        }
    }

    @PatchMapping("/{id}/in-pool")
    public ResponseEntity<?> updateIsMarkedInPool(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload
    ) {
        Boolean markedInPool = payload.get("markedInPool");
        try {
            ComplaintLog complaintLog = complaintLogService.getComplaintById(id);
            Boolean oldVal = complaintLog.getMarkedInPool();
            complaintLog.setMarkedInPool(markedInPool);
            complaintLogService.updateOnlyMarkedInPool(complaintLog);
            complaintLogService.saveComplaintHistory(
                    complaintLog.getComplaintId(),
                    "isMarkedInPool",
                    oldVal == null ? "null" : oldVal.toString(),
                    markedInPool == null ? "null" : markedInPool.toString(),
                    "Marked In Pool"
            );
            if (Boolean.TRUE.equals(markedInPool)) {
                complaintLogService.unassignComplaintVisitor(complaintLog.getComplaintId());
            }
            return ResponseEntity.ok(complaintLog);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to update MarkedInPool");
        }
    }

    @GetMapping("/dashboard-counts")
    public ResponseEntity<Map<String, Map<String, Integer>>> getDashboardCounts() {
        try {
            return ResponseEntity.ok(complaintLogService.getDashboardCounts());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/paginated-by-status")
    public ResponseEntity<?> getPaginatedComplaintsByStatus(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) String engineerName,
            @RequestParam(required = false) List<String> city,
            @RequestParam(required = false) String complaintStatus,
            @RequestParam(required = false) String subStatus,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String approvedDateFrom,
            @RequestParam(required = false) String approvedDateTo,
            @RequestParam(required = false) String closedDateFrom,
            @RequestParam(required = false) String closedDateTo,
            @RequestParam(required = false) String quotationDateFrom,
            @RequestParam(required = false) String quotationDateTo,
            @RequestParam(required = false) String pendingForClosedDateFrom,
            @RequestParam(required = false) String pendingForClosedDateTo,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String approvedDate,
            @RequestParam(required = false) String closedDate,
            @RequestParam(required = false) String pendingForClosedDate,
            @RequestParam(required = false) String quotationDate,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String inPool,
            @RequestParam(required = false) Boolean hasReport,
            @RequestParam(required = false) String reportType
    ) {
        try {
            int safePage = Math.max(page, 0);
            int safeSize = Math.min(Math.max(size, 1), MAX_COMPLAINT_PAGE_SIZE);
            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "date", "id"));
            Page<ComplaintBranchGroupDTO> resultPage =
                    "Open".equalsIgnoreCase(status)
                            ? complaintLogService.searchOpenComplaintsWithBranchFiltering(
                            status, bankName, branchCode, branchName, engineerName, city,
                            complaintStatus, subStatus,
                            dateFrom, dateTo, approvedDateFrom, approvedDateTo,
                            closedDateFrom, closedDateTo, quotationDateFrom, quotationDateTo,
                            pendingForClosedDateFrom, pendingForClosedDateTo,
                            date, approvedDate, closedDate, pendingForClosedDate, quotationDate,
                            priority, inPool, hasReport, reportType, pageable)
                            : complaintLogService.searchComplaints(
                            status, bankName, branchCode, branchName, engineerName, city,
                            complaintStatus, subStatus,
                            dateFrom, dateTo, approvedDateFrom, approvedDateTo,
                            closedDateFrom, closedDateTo, quotationDateFrom, quotationDateTo,
                            pendingForClosedDateFrom, pendingForClosedDateTo,
                            date, approvedDate, closedDate, pendingForClosedDate, quotationDate,
                            priority, inPool, hasReport, reportType, pageable);

            long complaintsBeforePage = 0L;
            RequestAttributes ra = RequestContextHolder.getRequestAttributes();
            if (ra != null) {
                Object attr = ra.getAttribute("complaintsBeforePage", RequestAttributes.SCOPE_REQUEST);
                if (attr instanceof Long l) complaintsBeforePage = l;
                else if (attr instanceof Integer i) complaintsBeforePage = i.longValue();
            }

            return ResponseEntity.ok()
                    .header("X-Complaints-Before-Page", String.valueOf(complaintsBeforePage))
                    .header("Access-Control-Expose-Headers", "X-Complaints-Before-Page")
                    .body(resultPage);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Page.empty());
        }
    }

    @GetMapping("/duplicate")
    public ResponseEntity<Boolean> checkDuplicate(
            @RequestParam String bankName,
            @RequestParam String branchCode
    ) {
        return ResponseEntity.ok(complaintLogService.existsOpenComplaint(bankName, branchCode));
    }

    @GetMapping("/by-id")
    public ResponseEntity<ComplaintLog> getComplaintByComplaintId(@RequestParam String complaintId) {
        try {
            ComplaintLog log = complaintLogService.getComplaintByComplaintId(complaintId);
            return (log != null) ? ResponseEntity.ok(log) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/bulk-visit-schedule/upload")
    public ResponseEntity<Map<String, Object>> bulkVisitScheduleUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String scheduleDate,
            @RequestParam(required = false) Long visitorId,
            @RequestParam(required = false) String visitorName
    ) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is required"));
            }

            String filename = Optional.ofNullable(file.getOriginalFilename())
                    .orElse("")
                    .toLowerCase(Locale.ROOT);

            List<BulkVisitScheduleRequest.Entry> entries;

            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                entries = parseExcelToEntries(file.getInputStream());
            } else if (filename.endsWith(".csv")) {
                entries = parseCsvToEntries(file.getInputStream());
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Unsupported file type. Please upload .xlsx, .xls, or .csv"
                ));
            }

            if (entries.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No valid rows found in file."));
            }

            BulkVisitScheduleRequest dto = new BulkVisitScheduleRequest();
            dto.setEntries(entries);
            dto.setScheduleDate(scheduleDate);
            dto.setVisitorId(visitorId);
            dto.setVisitorName(visitorName);

            BulkVisitScheduleResult result = bulkVisitSchedulingService.bulkVisitSchedule(dto);

            messagingTemplate.convertAndSend("/topic/paginated-by-status",
                    Map.of("action", "bulkVisitSchedule"));

            return ResponseEntity.ok(Map.of(
                    "updatedCount", result.getUpdatedCount(),
                    "skipped", result.getSkipped()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }

    /** Parse CSV with headers: bankName,branchCode (case-insensitive; allows minor variants) */
    private List<Entry> parseCsvToEntries(InputStream in) throws IOException {
        final int MAX_ROWS = 50_000; // simple safety cap
        List<Entry> out = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) return out;

            List<String> cols = Arrays.stream(header.split(",", -1))
                    .map(s -> s.trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toList());

            int bankIdx    = firstIndex(cols, "bankname", "bank");
            int codeIdx    = firstIndex(cols, "branchcode", "branch code", "branch_code");
            int visitorIdx = firstIndex(cols, "visitorname", "visitor name", "visitor_name");
            int schedIdx   = firstIndex(cols, "scheduledate", "schedule date", "schedule_date");

            if (bankIdx < 0 || codeIdx < 0) {
                throw new IllegalArgumentException(
                        "CSV must have headers: bankName, branchCode (optional: visitorName, scheduleDate)"
                );
            }

            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (++count > MAX_ROWS) break;
                if (line.isBlank()) continue;

                String[] parts = line.split(",", -1);
                if (parts.length <= Math.max(bankIdx, codeIdx)) continue;

                String bankName     = parts[bankIdx].trim();
                String branchCode   = parts[codeIdx].trim();
                String visitorName  = (visitorIdx >= 0 && visitorIdx < parts.length)
                        ? parts[visitorIdx].trim()
                        : null;
                String scheduleDate = (schedIdx >= 0 && schedIdx < parts.length)
                        ? parts[schedIdx].trim()
                        : null;

                if (bankName.isEmpty() || branchCode.isEmpty()) continue;

                Entry e = new Entry();
                e.setBankName(bankName);
                e.setBranchCode(branchCode);
                e.setVisitorName(visitorName);
                e.setScheduleDate(scheduleDate); // ✅ per-row schedule date
                out.add(e);
            }
        }
        return out;
    }

    private List<BulkVisitScheduleRequest.Entry> parseExcelToEntries(InputStream inputStream) throws IOException {
        List<BulkVisitScheduleRequest.Entry> entries = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return entries;

            Row header = sheet.getRow(0);
            if (header == null) return entries;

            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : header) {
                String key = cell.getStringCellValue()
                        .trim()
                        .toLowerCase(Locale.ROOT);
                headerMap.put(key, cell.getColumnIndex());
            }

            int bankIdx    = getColumnIndex(headerMap, "bankname", "bank");
            int branchIdx  = getColumnIndex(headerMap, "branchcode", "branch code", "branch_code");
            int visitorIdx = getColumnIndex(headerMap, "visitorname", "engineername", "visitor name");
            int schedIdx   = getColumnIndex(headerMap,
                    "scheduledate",
                    "schedule date",
                    "schedule_date",
                    "schedule date (yyyy-mm-dd)");

            if (bankIdx < 0 || branchIdx < 0) {
                throw new IllegalArgumentException("Excel must have columns: bankName, branchCode");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String bankName     = getStringCellValue(row, bankIdx);
                String branchCode   = getStringCellValue(row, branchIdx);
                String visitorName  = (visitorIdx >= 0) ? getStringCellValue(row, visitorIdx) : null;
                String scheduleDate = (schedIdx  >= 0) ? getStringCellValue(row, schedIdx)  : null;

                if (bankName == null) bankName = "";
                if (branchCode == null) branchCode = "";

                if (bankName.isEmpty() || branchCode.isEmpty()) continue;

                BulkVisitScheduleRequest.Entry entry = new BulkVisitScheduleRequest.Entry();
                entry.setBankName(bankName);
                entry.setBranchCode(branchCode);
                entry.setVisitorName(visitorName);
                entry.setScheduleDate(scheduleDate); // ✅ per-row schedule date
                entries.add(entry);
            }
        }

        return entries;
    }

    private String getStringCellValue(Row row, int index) {
        if (index < 0) return "";
        Cell cell = row.getCell(index);
        if (cell == null) return "";

        // Detect Excel date cells
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            java.util.Date utilDate = cell.getDateCellValue();
            // Convert util.Date → yyyy-MM-dd string
            return new SimpleDateFormat("yyyy-MM-dd").format(utilDate);
        }

        // Normal string
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }

        // Numeric but NOT date (branch codes, etc.)
        if (cell.getCellType() == CellType.NUMERIC) {
            double d = cell.getNumericCellValue();
            if (d == (long) d) return String.valueOf((long) d);   // integer
            return String.valueOf(d);
        }

        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }

        return cell.toString().trim();
    }



    private int getColumnIndex(Map<String, Integer> headerMap, String... possibleNames) {
        for (String name : possibleNames) {
            Integer idx = headerMap.get(name.toLowerCase(Locale.ROOT));
            if (idx != null) return idx;
        }
        return -1;
    }


    private int firstIndex(List<String> cols, String... candidates) {
        for (String c : candidates) {
            int i = cols.indexOf(c);
            if (i >= 0) return i;
        }
        return -1;
    }

    @PostMapping("/pending-for-closed/count-by-source")
    public ResponseEntity<Map<String, Object>> countPendingForClosedBySource(
            @RequestBody List<String> complaintIds) {

        // Get distinct complaintIds for each source
        List<String> mobileIds = pendingForClosedLogRepository
                .getDistinctComplaintIdsBySource("MOBILE", complaintIds);

        List<String> portalIds = pendingForClosedLogRepository
                .getDistinctComplaintIdsBySource("PORTAL", complaintIds);

        // Build response
        Map<String, Object> response = new HashMap<>();

        Map<String, Object> mobileData = new HashMap<>();
        mobileData.put("count", mobileIds.size());
        mobileData.put("ids", mobileIds);

        Map<String, Object> portalData = new HashMap<>();
        portalData.put("count", portalIds.size());
        portalData.put("ids", portalIds);

        response.put("mobile", mobileData);
        response.put("portal", portalData);

        return ResponseEntity.ok(response);
    }


}
