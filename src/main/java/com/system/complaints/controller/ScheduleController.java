package com.system.complaints.controller;

import com.system.complaints.dto.ScheduleDTO;
import com.system.complaints.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.util.List;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @GetMapping("/by-range")
    public ResponseEntity<List<ScheduleDTO>> getSchedulesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate end) {
        List<ScheduleDTO> dtos = scheduleService.findScheduleDTOsByRange(Date.valueOf(start), Date.valueOf(end));
        return ResponseEntity.ok(dtos);
    }
}
