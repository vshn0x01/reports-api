package com.reports.api.dto;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

public class ReportRunCreateRequest {
    @Valid
    private List<FilterItem> filters = new ArrayList<>();

    public List<FilterItem> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterItem> filters) {
        this.filters = filters == null ? new ArrayList<>() : filters;
    }
}
