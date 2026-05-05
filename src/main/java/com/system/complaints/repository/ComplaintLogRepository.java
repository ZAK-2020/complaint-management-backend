package com.system.complaints.repository;

import com.system.complaints.dto.ComplaintBranchGroupProjection;
import com.system.complaints.model.ComplaintLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintLogRepository extends JpaRepository<ComplaintLog, Long>,JpaSpecificationExecutor<ComplaintLog> {



    // -------------------------------------------------------------------------
    // Basic queries
    // -------------------------------------------------------------------------

    Page<ComplaintLog> findAllByOrderByIdDesc(Pageable pageable);

    // NEW: For Open and sub-statuses
    Page<ComplaintLog> findByComplaintStatusInOrderByIdDesc(List<String> statuses, Pageable pageable);

    // NEW: For single status (Closed, Approved, etc)
    Page<ComplaintLog> findByComplaintStatusOrderByIdDesc(String status, Pageable pageable);
    // In ComplaintLogRepository
    List<ComplaintLog> findByComplaintIdIn(List<String> complaintIds);

    // Find complaints by visitor's ID
    List<ComplaintLog> findByComplaintStatusAndScheduleDateBefore(String complaintStatus, Date date);

    // Find a complaint by its complaintId
    Optional<ComplaintLog> findByComplaintId(String complaintId);

    // Find similar complaints within 15 days of closedDate
    @Query("SELECT c FROM ComplaintLog c WHERE " +
            "c.bankName = :bankName AND " +
            "c.branchCode = :branchCode AND " +
            "c.branchName = :branchName AND " +
            "c.complaintType = :complaintType AND " +
            "c.closedDate IS NOT NULL AND " +
            ":startDate <= c.date AND c.date <= :endDate")
    List<ComplaintLog> findSimilarComplaintsWithin15Days(
            @Param("bankName") String bankName,
            @Param("branchCode") String branchCode,
            @Param("branchName") String branchName,
            @Param("complaintType") String complaintType,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );


    // -------------------------------------------------------------------------
    // Today's Open / Closed
    // -------------------------------------------------------------------------

    // Get today's open complaints count
    @Query(value = "SELECT COUNT(*) AS today_open_complaints " +
            "FROM complaints_log " +
            "WHERE DATE(date) = CURDATE()",
            nativeQuery = true)
    int getTodaysOpenComplaints();

    // Get today's closed complaints count
    @Query(value = "SELECT COUNT(*) AS today_closed_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Closed' " +
            "AND closed_date IS NOT NULL " +
            "AND DATE(closed_date) = CURDATE()",
            nativeQuery = true)
    int getTodaysClosedComplaints();

    // Get city-wise open complaints for today
    @Query(value = "SELECT CASE " +
            "           WHEN city IN ('Karachi', 'Lahore', 'Islamabad', 'Peshawar', 'Hyderabad', " +
            "                            'Quetta', 'Sukkur', 'Sadiqabad', 'Bahawalpur', 'Multan', " +
            "                            'Sahiwal', 'Jhang', 'Faisalabad', 'Sargodha', 'Sialkot', " +
            "                            'Jhelum', 'Abbottabad') THEN city " +
            "           ELSE 'Others' " +
            "       END AS city_group, " +
            "       COUNT(*) AS today_open_complaints " +
            "FROM complaints_log " +
            "WHERE DATE(date) = CURDATE() " +
            "GROUP BY city_group",
            nativeQuery = true)
    List<Object[]> getCityWiseTodaysOpenComplaints();

    // Get city-wise closed complaints for today
    @Query(value = "SELECT CASE " +
            "           WHEN city IN ('Karachi', 'Lahore', 'Islamabad', 'Peshawar', 'Hyderabad', " +
            "                            'Quetta', 'Sukkur', 'Sadiqabad', 'Bahawalpur', 'Multan', " +
            "                            'Sahiwal', 'Jhang', 'Faisalabad', 'Sargodha', 'Sialkot', " +
            "                            'Jhelum', 'Abbottabad') THEN city " +
            "           ELSE 'Others' " +
            "       END AS city_group, " +
            "       COUNT(*) AS today_closed_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Closed' " +
            "AND closed_date IS NOT NULL " +
            "AND DATE(closed_date) = CURDATE() " +
            "GROUP BY city_group",
            nativeQuery = true)
    List<Object[]> getCityWiseTodaysClosedComplaints();

    // Get bank-wise open complaints for today
    @Query(value = "SELECT bank_name AS bank, COUNT(*) AS today_open_complaints " +
            "FROM complaints_log " +
            "WHERE DATE(date) = CURDATE() " +
            "GROUP BY bank_name",
            nativeQuery = true)
    List<Object[]> getBankWiseTodaysOpenComplaints();

    // Get bank-wise closed complaints for today
    @Query(value = "SELECT bank_name AS bank, COUNT(*) AS today_closed_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Closed' " +
            "AND closed_date IS NOT NULL " +
            "AND DATE(closed_date) = CURDATE() " +
            "GROUP BY bank_name",
            nativeQuery = true)
    List<Object[]> getBankWiseTodaysClosedComplaints();


    // -------------------------------------------------------------------------
    // All-Time (or Unfiltered by Date) Open / Closed
    // -------------------------------------------------------------------------
// Count complaints with specific statuses
    @Query("SELECT COUNT(c) FROM ComplaintLog c WHERE c.complaintStatus IN :statuses")
    int countByComplaintStatusIn(@Param("statuses") List<String> statuses);

    // Count complaints with a specific status
    @Query("SELECT COUNT(c) FROM ComplaintLog c WHERE c.complaintStatus = :status")
    int countByComplaintStatus(@Param("status") String status);
    // Count complaints with status "Wait For Approval"
    @Query("SELECT COUNT(c) FROM ComplaintLog c WHERE c.complaintStatus = 'Wait For Approval'")
    int countByComplaintStatusWaitForApproval();

    // Count complaints with status "Approved"
    @Query("SELECT COUNT(c) FROM ComplaintLog c WHERE c.complaintStatus = 'Approved'")
    int countByComplaintStatusApproved();
    /**
     * City-wise open complaints for all time
     */
    @Query(value = "SELECT CASE " +
            "           WHEN city IN ('Karachi', 'Lahore', 'Islamabad', 'Peshawar', 'Hyderabad', " +
            "                            'Quetta', 'Sukkur', 'Sadiqabad', 'Bahawalpur', 'Multan', " +
            "                            'Sahiwal', 'Jhang', 'Faisalabad', 'Sargodha', 'Sialkot', " +
            "                            'Jhelum', 'Abbottabad') THEN city " +
            "           ELSE 'Others' " +
            "       END AS city_group, " +
            "       COUNT(*) AS total_open_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Open' " +  // or = 'Open' if you prefer
            "GROUP BY city_group",
            nativeQuery = true)
    List<Object[]> getCityWiseAllOpenComplaints();
    /**
     * City-wise Visit Schedule complaints for all time
     */
    @Query(value = "SELECT CASE " +
            "           WHEN city IN ('Karachi', 'Lahore', 'Islamabad', 'Peshawar', 'Hyderabad', " +
            "                            'Quetta', 'Sukkur', 'Sadiqabad', 'Bahawalpur', 'Multan', " +
            "                            'Sahiwal', 'Jhang', 'Faisalabad', 'Sargodha', 'Sialkot', " +
            "                            'Jhelum', 'Abbottabad') THEN city " +
            "           ELSE 'Others' " +
            "       END AS city_group, " +
            "       COUNT(*) AS total_visit_schedule_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Visit Schedule' " +  //  = 'Visit Schedule'
            "GROUP BY city_group",
            nativeQuery = true)
    List<Object[]> getCityWiseAllVisitScheduleComplaints();
    /**
     * City-wise Quotation complaints for all time
     */
    @Query(value = "SELECT CASE " +
            "           WHEN city IN ('Karachi', 'Lahore', 'Islamabad', 'Peshawar', 'Hyderabad', " +
            "                            'Quetta', 'Sukkur', 'Sadiqabad', 'Bahawalpur', 'Multan', " +
            "                            'Sahiwal', 'Jhang', 'Faisalabad', 'Sargodha', 'Sialkot', " +
            "                            'Jhelum', 'Abbottabad') THEN city " +
            "           ELSE 'Others' " +
            "       END AS city_group, " +
            "       COUNT(*) AS total_quotation_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Quotation' " +  //  = 'Quotation'
            "GROUP BY city_group",
            nativeQuery = true)
    List<Object[]> getCityWiseAllQuotationComplaints();
    /**
     * City-wise hardware Picked complaints for all time
     */
    @Query(value = "SELECT CASE " +
            "           WHEN city IN ('Karachi', 'Lahore', 'Islamabad', 'Peshawar', 'Hyderabad', " +
            "                            'Quetta', 'Sukkur', 'Sadiqabad', 'Bahawalpur', 'Multan', " +
            "                            'Sahiwal', 'Jhang', 'Faisalabad', 'Sargodha', 'Sialkot', " +
            "                            'Jhelum', 'Abbottabad') THEN city " +
            "           ELSE 'Others' " +
            "       END AS city_group, " +
            "       COUNT(*) AS total_hardware_picked_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Hardware Picked' " +  // or = 'Open' if you prefer
            "GROUP BY city_group",
            nativeQuery = true)
    List<Object[]> getCityWiseAllHardwarePickedComplaints();
    /**
     * City-wise Approved complaints for all time
     */
    @Query(value = "SELECT CASE " +
            "           WHEN city IN ('Karachi', 'Lahore', 'Islamabad', 'Peshawar', 'Hyderabad', " +
            "                            'Quetta', 'Sukkur', 'Sadiqabad', 'Bahawalpur', 'Multan', " +
            "                            'Sahiwal', 'Jhang', 'Faisalabad', 'Sargodha', 'Sialkot', " +
            "                            'Jhelum', 'Abbottabad') THEN city " +
            "           ELSE 'Others' " +
            "       END AS city_group, " +
            "       COUNT(*) AS total_approved_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Approved' " +  // or = 'Approved' if you prefer
            "GROUP BY city_group",
            nativeQuery = true)
    List<Object[]> getCityWiseAllApprovedComplaints();
    /**
     * City-wise FOC complaints for all time
     */
    @Query(value = "SELECT CASE " +
            "           WHEN city IN ('Karachi', 'Lahore', 'Islamabad', 'Peshawar', 'Hyderabad', " +
            "                            'Quetta', 'Sukkur', 'Sadiqabad', 'Bahawalpur', 'Multan', " +
            "                            'Sahiwal', 'Jhang', 'Faisalabad', 'Sargodha', 'Sialkot', " +
            "                            'Jhelum', 'Abbottabad') THEN city " +
            "           ELSE 'Others' " +
            "       END AS city_group, " +
            "       COUNT(*) AS total_foc_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'FOC' " +  // or = 'FOC'
            "GROUP BY city_group",
            nativeQuery = true)
    List<Object[]> getCityWiseAllFocComplaints();
    /**
     * City-wise Wait For Approval complaints for all time
     */
    @Query(value = "SELECT CASE " +
            "           WHEN city IN ('Karachi', 'Lahore', 'Islamabad', 'Peshawar', 'Hyderabad', " +
            "                            'Quetta', 'Sukkur', 'Sadiqabad', 'Bahawalpur', 'Multan', " +
            "                            'Sahiwal', 'Jhang', 'Faisalabad', 'Sargodha', 'Sialkot', " +
            "                            'Jhelum', 'Abbottabad') THEN city " +
            "           ELSE 'Others' " +
            "       END AS city_group, " +
            "       COUNT(*) AS total_wait_for_approval_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Wait For Approval' " +  // or = 'Wait For Approval' if you prefer
            "GROUP BY city_group",
            nativeQuery = true)
    List<Object[]> getCityWiseAllWaitForApprovalComplaints();

    /**
     * City-wise closed complaints for all time
     */
    @Query(value = "SELECT CASE " +
            "           WHEN city IN ('Karachi', 'Lahore', 'Islamabad', 'Peshawar', 'Hyderabad', " +
            "                            'Quetta', 'Sukkur', 'Sadiqabad', 'Bahawalpur', 'Multan', " +
            "                            'Sahiwal', 'Jhang', 'Faisalabad', 'Sargodha', 'Sialkot', " +
            "                            'Jhelum', 'Abbottabad') THEN city " +
            "           ELSE 'Others' " +
            "       END AS city_group, " +
            "       COUNT(*) AS total_closed_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Closed' " +
            "AND closed_date IS NOT NULL " +
            "GROUP BY city_group",
            nativeQuery = true)
    List<Object[]> getCityWiseAllClosedComplaints();

    /**
     * City-wise open complaints for all time
     */
    @Query(value = "SELECT bank_name AS bank, " +
            "SUM(CASE WHEN complaint_status = 'Open' THEN 1 ELSE 0 END) + " +
            "SUM(CASE WHEN complaint_status = 'Visit Schedule' THEN 1 ELSE 0 END) AS total_open_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status IN ('Open') " +
            "GROUP BY bank_name",
            nativeQuery = true)
    List<Object[]> getBankWiseAllOpenComplaints();


    /**
     * Bank-wise closed complaints for all time
     */
    @Query(value = "SELECT bank_name AS bank, COUNT(*) AS total_closed_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Closed' " +
            "AND closed_date IS NOT NULL " +
            "GROUP BY bank_name",
            nativeQuery = true)
    List<Object[]> getBankWiseAllClosedComplaints();
    /**
     * Bank-wise Hardware Picked complaints for all time
     */
    @Query(value = "SELECT bank_name AS bank, COUNT(*) AS total_hardware_picked_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Hardware Picked' " +
            "GROUP BY bank_name",
            nativeQuery = true)
    List<Object[]> getBankWiseAllHardwarePickedComplaints();

    /**
     * Bank-wise Approved complaints for all time
     */
    @Query(value = "SELECT bank_name AS bank, COUNT(*) AS total_approved_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Approved' " +
            "GROUP BY bank_name",
            nativeQuery = true)
    List<Object[]> getBankWiseAllApprovedComplaints();

    /**
     * Bank-wise Wait For Approval complaints for all time
     */
    @Query(value = "SELECT bank_name AS bank, COUNT(*) AS total_wait_for_approval_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Wait For Approval' " +
            "GROUP BY bank_name",
            nativeQuery = true)
    List<Object[]> getBankWiseAllWaitForApprovalComplaints();
    /**
     * Bank-wise Visit Schedule complaints for all time
     */
    @Query(value = "SELECT bank_name AS bank, COUNT(*) AS total_visit_schedule_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Visit Schedule' " +
            "GROUP BY bank_name",
            nativeQuery = true)
    List<Object[]> getBankWiseVisitScheduleComplaints();
    @Query(value = "SELECT bank_name AS bank, COUNT(*) AS total_quotation_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'Quotation' " +
            "GROUP BY bank_name",
            nativeQuery = true)
    List<Object[]> getBankWiseQuotationComplaints();
    @Query(value = "SELECT bank_name AS bank, COUNT(*) AS total_foc_complaints " +
            "FROM complaints_log " +
            "WHERE complaint_status = 'FOC' " +
            "GROUP BY bank_name",
            nativeQuery = true)
    List<Object[]> getBankWiseFocComplaints();

    // For single status, all time
    @Query(value = "SELECT COUNT(*) FROM complaints_log WHERE complaint_status = :status", nativeQuery = true)
    int countAllTimeByStatus(@Param("status") String status);

    // For single status, today only (by complaint logged date)
    @Query(value = "SELECT COUNT(*) FROM complaints_log WHERE DATE(date) = CURDATE() AND complaint_status = :status", nativeQuery = true)
    int countTodayByStatus(@Param("status") String status);

    // For all complaints, all time
    @Query(value = "SELECT COUNT(*) FROM complaints_log", nativeQuery = true)
    int countAllComplaints();

    // For all complaints registered today (by complaint logged date)
    @Query(value = "SELECT COUNT(*) FROM complaints_log WHERE DATE(date) = CURDATE()", nativeQuery = true)
    int countTodaysRegistered();

    // For multiple statuses, all time
    @Query(value = "SELECT COUNT(*) FROM complaints_log WHERE complaint_status IN (:statuses)", nativeQuery = true)
    int countAllTimeByStatuses(@Param("statuses") List<String> statuses);

    // For multiple statuses, today only (by complaint logged date)
    @Query(value = "SELECT COUNT(*) FROM complaints_log WHERE DATE(date) = CURDATE() AND complaint_status IN (:statuses)", nativeQuery = true)
    int countTodayByStatuses(@Param("statuses") List<String> statuses);

    // === New Methods for Correct Daily Closed Counts ===

    // Count of complaints opened today AND closed today (same-day closed)
    @Query(
            value = "SELECT COUNT(*) FROM complaints_log WHERE complaint_status = 'Closed' AND DATE(date) = CURDATE() AND DATE(closed_date) = CURDATE()",
            nativeQuery = true
    )
    int countSameDayClosed();

    // Count of complaints closed today (regardless of open date)
    @Query(
            value = "SELECT COUNT(*) FROM complaints_log WHERE complaint_status = 'Closed' AND DATE(closed_date) = CURDATE()",
            nativeQuery = true
    )
    int countClosedToday();

    @Query(value = """
            SELECT
                COALESCE(SUM(CASE WHEN cl.date = CURDATE() THEN 1 ELSE 0 END), 0) AS today_logged,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'closed'
                     AND cl.date = CURDATE()
                     AND cl.closed_date = CURDATE()
                    THEN 1 ELSE 0 END), 0) AS same_day_closed,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'hardware picked'
                     AND cl.date = CURDATE()
                     AND cl.hardware_picked_date = CURDATE()
                    THEN 1 ELSE 0 END), 0) AS hardware_picked_today,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'wait for approval'
                     AND cl.date = CURDATE()
                     AND cl.quotation_date = CURDATE()
                    THEN 1 ELSE 0 END), 0) AS wait_for_approval_today,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'foc'
                     AND cl.date = CURDATE()
                     AND cl.foc_date = CURDATE()
                    THEN 1 ELSE 0 END), 0) AS foc_today,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'approved'
                     AND cl.date = CURDATE()
                     AND cl.approved_date = CURDATE()
                    THEN 1 ELSE 0 END), 0) AS approved_today,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'open'
                     AND cl.date < CURDATE()
                    THEN 1 ELSE 0 END), 0) AS old_logged,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'closed'
                     AND cl.closed_date = CURDATE()
                     AND (cl.date IS NULL OR cl.date <> CURDATE())
                    THEN 1 ELSE 0 END), 0) AS old_closed,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'hardware picked'
                     AND cl.hardware_picked_date = CURDATE()
                     AND (cl.date IS NULL OR cl.date <> CURDATE())
                    THEN 1 ELSE 0 END), 0) AS old_hardware_picked,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'approved'
                     AND cl.approved_date = CURDATE()
                     AND (cl.date IS NULL OR cl.date <> CURDATE())
                    THEN 1 ELSE 0 END), 0) AS old_approved,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'wait for approval'
                     AND cl.quotation_date = CURDATE()
                     AND (cl.date IS NULL OR cl.date <> CURDATE())
                    THEN 1 ELSE 0 END), 0) AS old_wait_for_approval,
                COALESCE(SUM(CASE
                    WHEN LOWER(TRIM(cl.complaint_status)) = 'foc'
                     AND cl.foc_date = CURDATE()
                     AND (cl.date IS NULL OR cl.date <> CURDATE())
                    THEN 1 ELSE 0 END), 0) AS old_foc
            FROM complaints_log cl
            """, nativeQuery = true)
    Object[] getComplaintSummarySnapshot();

    boolean existsByBankNameIgnoreCaseAndBranchCodeAndComplaintStatusNot(
            String bankName, String branchCode, String complaintStatus);

    @Query(
            value = "SELECT cl.bank_name AS bankName, cl.branch_code AS branchCode, cl.branch_name AS branchName " +
                    "FROM complaints_log cl " +
                    "WHERE (:bankName IS NULL OR LOWER(cl.bank_name) LIKE %:bankName%) " +
                    "AND (:branchCode IS NULL OR cl.branch_code = :branchCode) " +
                    "AND (:city IS NULL OR LOWER(cl.city) LIKE %:city%) " +
                    "AND (:status IS NULL OR LOWER(cl.complaint_status) = :status) " +
                    "AND (:dateFrom IS NULL OR cl.date >= :dateFrom) " +
                    "AND (:dateTo IS NULL OR cl.date <= :dateTo) " +
                    // Add any other filters you want here...
                    "GROUP BY cl.bank_name, cl.branch_code, cl.branch_name " +
                    "ORDER BY MAX(cl.date) DESC",
            countQuery = "SELECT COUNT(DISTINCT CONCAT(cl.bank_name, '_', cl.branch_code)) " +
                    "FROM complaints_log cl " +
                    "WHERE (:bankName IS NULL OR LOWER(cl.bank_name) LIKE %:bankName%) " +
                    "AND (:branchCode IS NULL OR cl.branch_code = :branchCode) " +
                    "AND (:city IS NULL OR LOWER(cl.city) LIKE %:city%) " +
                    "AND (:status IS NULL OR LOWER(cl.complaint_status) = :status) " +
                    "AND (:dateFrom IS NULL OR cl.date >= :dateFrom) " +
                    "AND (:dateTo IS NULL OR cl.date <= :dateTo)"
            // Add same filters as above!
            ,
            nativeQuery = true
    )
    Page<ComplaintBranchGroupProjection> findBranchGroupsPage(
            @Param("bankName") String bankName,
            @Param("branchCode") String branchCode,
            @Param("city") String city,
            @Param("status") String status,
            @Param("dateFrom") java.sql.Date dateFrom,
            @Param("dateTo") java.sql.Date dateTo,
            Pageable pageable
    );
    List<ComplaintLog> findByBankNameIgnoreCaseAndBranchCodeIgnoreCase(String bankName, String branchCode);

    List<ComplaintLog> findByBankNameIgnoreCaseAndBranchCodeAndComplaintStatus(
            String bankName, String branchCode, String complaintStatus
    );
}
