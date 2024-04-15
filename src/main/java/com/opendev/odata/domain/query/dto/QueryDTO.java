package com.opendev.odata.domain.query.dto;

import com.opendev.odata.domain.query.entity.TdxQuery;
import com.opendev.odata.domain.query.entity.TdxQueryParam;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class QueryDTO {
    private Long id;
    private String title;
    private String query;
    private List<QueryParamDTO> parameters;

    public static QueryDTO fromEntity(TdxQuery tdxQuery) {
        QueryDTO queryDTO = new QueryDTO();
        queryDTO.setId(tdxQuery.getId());
        queryDTO.setTitle(tdxQuery.getTitle());
        queryDTO.setQuery(tdxQuery.getQuery());

        List<TdxQueryParam> params = tdxQuery.getTdxQueryParams();
        queryDTO.setParameters(params.stream()
                .map(QueryParamDTO::fromEntity)
                .collect(Collectors.toList()));

        return queryDTO;
    }
}