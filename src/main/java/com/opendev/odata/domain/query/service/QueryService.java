package com.opendev.odata.domain.query.service;


import com.opendev.odata.domain.query.dto.QueryDTO;
import com.opendev.odata.domain.query.entity.TdxQuery;
import com.opendev.odata.domain.query.entity.TdxQueryParam;
import com.opendev.odata.domain.query.repository.QueryParamRepository;
import com.opendev.odata.domain.query.repository.QueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class QueryService {

    private final QueryRepository queryRepository;
    private final QueryParamRepository  queryParamRepository;

    public void saveQuery(QueryDTO queryDTO) {
        TdxQuery tdxQuery = TdxQuery.builder()
                .title(queryDTO.getTitle())
                .query(queryDTO.getQuery())
                .build();

        List<TdxQueryParam> params = queryDTO.getParameters().stream()
                .map(paramDTO -> TdxQueryParam.builder()
                        .parameter(paramDTO.getParameter())
                        .attribute(paramDTO.getAttribute())
                        .tdxQuery(tdxQuery)
                        .build())
                .collect(Collectors.toList());

        //tdxQuery.setTdxQueryParams(params);
        queryRepository.save(tdxQuery);
    }

    public QueryDTO getQueryById(Long id) {
        TdxQuery tdxQuery = queryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found"));

        return QueryDTO.fromEntity(tdxQuery);
    }

    public void updateQuery(QueryDTO queryDTO) {
        TdxQuery existingQuery = queryRepository.findById(queryDTO.getId())
                .orElseThrow(() -> new RuntimeException("Query not found"));

        TdxQuery updatedQuery = TdxQuery.builder()
                .id(existingQuery.getId())
                .title(queryDTO.getTitle())
                .query(queryDTO.getQuery())
                .tdxQueryParams(updateQueryParams(existingQuery, queryDTO))
                .build();

        queryRepository.save(updatedQuery);
    }

    private List<TdxQueryParam> updateQueryParams(TdxQuery existingQuery, QueryDTO queryDTO) {
        List<TdxQueryParam> existingParams = existingQuery.getTdxQueryParams();
        List<TdxQueryParam> newParams = queryDTO.getParameters().stream()
                .map(paramDTO -> TdxQueryParam.builder()
                        .parameter(paramDTO.getParameter())
                        .attribute(paramDTO.getAttribute())
                        .tdxQuery(existingQuery)
                        .build())
                .collect(Collectors.toList());

        return newParams;
    }

    public void deleteQuery(Long id) {
        queryRepository.deleteById(id);
    }
}