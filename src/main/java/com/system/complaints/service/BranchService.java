package com.system.complaints.service;

import com.system.complaints.model.Branch;
import com.system.complaints.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BranchService {

    @Autowired
    private BranchRepository branchRepository;

    public List<Branch> getAllBranches() {
        return branchRepository.findAllByOrderByBankAscBranchCodeAscBranchNameAsc();
    }

    public List<Branch> getBranchesByBank(String bank) {
        if (bank == null || bank.trim().isEmpty()) {
            return getAllBranches();
        }
        return branchRepository.findByBankIgnoreCaseOrderByBranchCodeAscBranchNameAsc(bank.trim());
    }

    public Branch saveBranch(Branch branch) {
        return branchRepository.save(branch);
    }

    public void deleteBranch(Long id) {
        branchRepository.deleteById(id);
    }
}
