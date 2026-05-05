package com.system.complaints.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "remarks_updates")  // Table name for remarks
public class RemarksUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "complaint_id", nullable = false)
    private ComplaintLog complaintLog;

    @Column(name = "remarks", nullable = false)
    private String remarks;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "logged_by", nullable = true)  // Allow null for loggedBy field
    private String loggedBy;

    private static final ZoneId KARACHI_ZONE = ZoneId.of("Asia/Karachi");

    public RemarksUpdate() {
        this.timestamp = LocalDateTime.now(KARACHI_ZONE);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ComplaintLog getComplaintLog() {
        return complaintLog;
    }

    public void setComplaintLog(ComplaintLog complaintLog) {
        this.complaintLog = complaintLog;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(String loggedBy) {
        this.loggedBy = loggedBy;
    }
}
