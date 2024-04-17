package com.opendev.odata.domain.query.dto;

import lombok.Data;

import java.util.List;

@Data
public class QueryDto {
    private String title;
    private String query;
    private List<QueryParamDto> parameters;
}