package com.system.complaints.repository;

import com.system.complaints.dto.VisitorSummaryDTO;
import com.system.complaints.model.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    // existing method - untouched
    Visitor findByName(String name);

    // NEW method (case-insensitive)
    Optional<Visitor> findFirstByNameIgnoreCase(String name);

    List<Visitor> findAllByOrderByNameAsc();

    List<Visitor> findByCityIgnoreCaseOrderByNameAsc(String city);

    @Query("SELECT new com.system.complaints.dto.VisitorSummaryDTO(v.id, v.name, v.city) FROM Visitor v ORDER BY v.name ASC")
    List<VisitorSummaryDTO> findAllSummaries();

    @Query("SELECT new com.system.complaints.dto.VisitorSummaryDTO(v.id, v.name, v.city) FROM Visitor v WHERE LOWER(v.city) = LOWER(:city) ORDER BY v.name ASC")
    List<VisitorSummaryDTO> findSummariesByCity(@Param("city") String city);
}
