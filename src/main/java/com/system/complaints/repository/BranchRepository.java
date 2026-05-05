package com.system.complaints.repository;

import com.system.complaints.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findAllByOrderByBankAscBranchCodeAscBranchNameAsc();

    List<Branch> findByBankIgnoreCaseOrderByBranchCodeAscBranchNameAsc(String bank);
}
