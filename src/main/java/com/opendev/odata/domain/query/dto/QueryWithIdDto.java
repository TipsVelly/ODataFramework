package com.opendev.odata.domain.query.dto;

import com.opendev.odata.domain.query.entity.TdxQuery;
import com.opendev.odata.domain.query.entity.TdxQueryParam;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class QueryWithIdDto {
    private Long id;
    private String title;
    private String query;
    private List<QueryParamDto> parameters;

    public static QueryWithIdDto fromEntity(TdxQuery tdxQuery) {
        QueryWithIdDto queryWithIdDtoDTO = new QueryWithIdDto();
        queryWithIdDtoDTO.setId(tdxQuery.getId());
        queryWithIdDtoDTO.setTitle(tdxQuery.getTitle());
        queryWithIdDtoDTO.setQuery(tdxQuery.getQuery());

        List<TdxQueryParam> params = tdxQuery.getTdxQueryParams();
        queryWithIdDtoDTO.setParameters(params.stream()
                .map(QueryParamDto::fromEntity)
                .collect(Collectors.toList()));

        return queryWithIdDtoDTO;
    }
}
