package com.opendev.odata.domain.query.dto;

import lombok.Data;

import java.util.List;

@Data
public class QueryDto {
    private String title;
    private String query;
    private String httpRequest;
    private String odataQueryName;
    private List<QueryParamDto> parameters;
}