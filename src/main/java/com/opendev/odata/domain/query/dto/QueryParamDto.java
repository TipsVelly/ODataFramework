package com.opendev.odata.domain.query.dto;


import com.opendev.odata.domain.query.entity.TdxQueryParam;
import lombok.Data;

@Data
public class QueryParamDto {
    private String parameter;
    private String attribute;

    public static QueryParamDto fromEntity(TdxQueryParam tdxQueryParam) {
        QueryParamDto paramDTO = new QueryParamDto();
        paramDTO.setParameter(tdxQueryParam.getParameter());
        paramDTO.setAttribute(tdxQueryParam.getAttribute());
        return paramDTO;
    }

    // DTO to Entity conversion
    public static TdxQueryParam toEntity(QueryParamDto dto) {
        return TdxQueryParam.builder()
                .parameter(dto.getParameter())
                .attribute(dto.getAttribute())
                .build();
    }
}