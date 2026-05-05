package com.system.complaints.repository;

import com.system.complaints.model.ComplaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {

    /**
     * Find all complaint history entries for a specific complaint ID.
     *
     * @param complaintId The ID of the complaint.
     * @return A list of ComplaintHistory objects.
     */
    List<ComplaintHistory> findByComplaintId(String complaintId);

    /**
     * Find all complaint history entries for a specific complaint ID, ordered by change date.
     *
     * @param complaintId The ID of the complaint.
     * @return A list of ComplaintHistory objects ordered by change date in ascending order.
     */
    List<ComplaintHistory> findByComplaintIdOrderByChangeDateAsc(String complaintId);

    /**
     * Find all complaint history entries for a specific complaint ID, ordered by change date descending.
     *
     * @param complaintId The ID of the complaint.
     * @return A list of ComplaintHistory objects ordered by change date in descending order.
     */
    List<ComplaintHistory> findByComplaintIdOrderByChangeDateDesc(String complaintId);

    List<ComplaintHistory> findByComplaintIdInAndFieldNameOrderByChangeDateAsc(List<String> complaintIds, String fieldName);

    List<ComplaintHistory> findTop20ByOrderByChangeDateDesc();

    List<ComplaintHistory> findTop20ByComplaintIdOrderByChangeDateDesc(String complaintId);

}
