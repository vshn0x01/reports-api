package com.reports.api.dto;

import java.util.List;

public record FilterOptionsResponse(
        List<FilterOption> sbu,
        List<FilterOption> zone,
        List<FilterOption> cluster,
        List<FilterOption> region,
        List<FilterOption> unit,
        List<FilterOption> branch
) {
}
