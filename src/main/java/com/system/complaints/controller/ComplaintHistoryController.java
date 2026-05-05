package com.system.complaints.controller;

import com.system.complaints.model.ComplaintHistory;
import com.system.complaints.service.ComplaintHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/history")
public class ComplaintHistoryController {

    @Autowired
    private ComplaintHistoryService complaintHistoryService;

    @GetMapping("/latest/20")
    public ResponseEntity<List<ComplaintHistory>> getLast20Activities() {
        try {
            return ResponseEntity.ok(complaintHistoryService.getLast20Activities());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
