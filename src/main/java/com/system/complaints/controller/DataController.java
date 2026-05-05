package com.system.complaints.controller;

import com.system.complaints.dto.VisitorSummaryDTO;
import com.system.complaints.model.Bank;
import com.system.complaints.model.Branch;
import com.system.complaints.model.City;
import com.system.complaints.model.ComplaintType;
import com.system.complaints.model.Status;
import com.system.complaints.model.UserType;
import com.system.complaints.repository.UserRepository;
import com.system.complaints.service.BankService;
import com.system.complaints.service.BranchService;
import com.system.complaints.service.CityService;
import com.system.complaints.service.ComplaintTypeService;
import com.system.complaints.service.StatusService;
import com.system.complaints.service.VisitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/data")
public class DataController {

    @Autowired
    private BankService bankService;

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private CityService cityService;

    @Autowired
    private StatusService statusService;

    @Autowired
    private ComplaintTypeService complaintTypeService;

    @Autowired
    private BranchService branchService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/banks")
    public List<Bank> getAllBanks() {
        return bankService.getAllBanks();
    }

    @GetMapping("/visitors")
    public List<VisitorSummaryDTO> getAllVisitors(@RequestParam(required = false) String city) {
        return visitorService.getVisitorSummariesByCity(city);
    }

    @GetMapping("/cities")
    public List<City> getAllCities() {
        return cityService.getAllCities();
    }

    @GetMapping("/complaint-types")
    public List<ComplaintType> getAllComplaintTypes() {
        return complaintTypeService.getAllComplaintTypes();
    }

    @GetMapping("/statuses")
    public List<Status> getAllStatuses() {
        return statusService.getAllStatuses();
    }

    @GetMapping("/branches")
    public List<Branch> getAllBranches(@RequestParam(required = false) String bank) {
        return branchService.getBranchesByBank(bank);
    }

    @GetMapping("/lab-engineers")
    public List<Map<String, Object>> getLabEngineers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getUserType() == UserType.LAB_USER)
                .map(user -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
