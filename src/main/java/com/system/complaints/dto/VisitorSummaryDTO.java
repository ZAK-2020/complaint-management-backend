package com.system.complaints.dto;

public class VisitorSummaryDTO {
    private final Long id;
    private final String name;
    private final String city;

    public VisitorSummaryDTO(Long id, String name, String city) {
        this.id = id;
        this.name = name;
        this.city = city;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }
}
