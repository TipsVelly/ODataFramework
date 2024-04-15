package com.opendev.odata.domain.query.dto;


import com.opendev.odata.domain.query.entity.TdxQueryParam;
import lombok.Data;

@Data
public class QueryParamDTO {
    private String parameter;
    private String attribute;

    public static QueryParamDTO fromEntity(TdxQueryParam tdxQueryParam) {
        QueryParamDTO paramDTO = new QueryParamDTO();
        paramDTO.setParameter(tdxQueryParam.getParameter());
        paramDTO.setAttribute(tdxQueryParam.getAttribute());
        return paramDTO;
    }
}