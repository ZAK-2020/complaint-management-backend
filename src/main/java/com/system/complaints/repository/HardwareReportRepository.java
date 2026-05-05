package com.system.complaints.repository;

import com.system.complaints.model.HardwareReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HardwareReportRepository extends JpaRepository<HardwareReport, Long> {
    // Retrieve all reports belonging to a specific HardwareLog by its ID
    List<HardwareReport> findByHardwareLogId(Long hardwareLogId);

    @Query("""
            SELECT DISTINCT r.hardwareLog.complaintLog.complaintId
            FROM HardwareReport r
            WHERE r.hardwareLog.complaintLog.complaintId IN :complaintIds
            """)
    List<String> findComplaintIdsWithReports(@Param("complaintIds") List<String> complaintIds);

    @Query("SELECT DISTINCT r.createdBy, r.hardwareLog.complaintLog.complaintId FROM HardwareReport r WHERE DATE(r.createdAt) = CURRENT_DATE")
    List<Object[]> findComplaintIdsWithReportsByAllUsersToday();

}
