package com.opendev.odata.domain.query.service;


import com.opendev.odata.domain.query.dto.QueryDto;
import com.opendev.odata.domain.query.dto.QueryParamDto;
import com.opendev.odata.domain.query.dto.QueryWithIdDto;
import com.opendev.odata.domain.query.entity.TdxQuery;
import com.opendev.odata.domain.query.entity.TdxQueryParam;
import com.opendev.odata.domain.query.repository.QueryParamRepository;
import com.opendev.odata.domain.query.repository.QueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class QueryService {

    private final QueryRepository queryRepository;
    private final QueryParamRepository  queryParamRepository;

    public void saveQuery(QueryDto queryDto) {
        TdxQuery tdxQuery = TdxQuery.builder()
                .title(queryDto.getTitle())
                .query(queryDto.getQuery())
                .build();
        TdxQuery savedQuery = queryRepository.save(tdxQuery);

        List<TdxQueryParam> params = queryDto.getParameters().stream()
                .map(paramDTO -> TdxQueryParam.builder()
                        .parameter(paramDTO.getParameter())
                        .attribute(paramDTO.getAttribute())
                        .tdxQuery(savedQuery)
                        .build())
                .collect(Collectors.toList());

        queryParamRepository.saveAll(params);
    }

    public QueryWithIdDto getQueryById(Long id) {
        TdxQuery tdxQuery = queryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Query not found"));

        return QueryWithIdDto.fromEntity(tdxQuery);
    }

//    public void updateQuery(QueryUpdateDto queryUpdateDto) {
//        TdxQuery existingQuery = queryRepository.findById(queryUpdateDto.getId())
//                .orElseThrow(() -> new RuntimeException("Query not found"));
//
//        TdxQuery updatedQuery = TdxQuery.builder()
//                .id(existingQuery.getId())
//                .title(queryUpdateDto.getTitle())
//                .query(queryUpdateDto.getQuery())
//                .tdxQueryParams(updateQueryParams(existingQuery, queryUpdateDto.getParameters()))
//                .build();
//
//        queryRepository.save(updatedQuery);
//    }

    private List<TdxQueryParam> updateQueryParams(TdxQuery existingQuery, QueryDto queryDTO) {
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

    public List<QueryWithIdDto> getAllQueries() {
        List<TdxQuery> allQueries = queryRepository.findAll();
        return allQueries.stream()
                .map(QueryWithIdDto::fromEntity)
                .collect(Collectors.toList());
    }

    public void updateQuery(Long id, QueryWithIdDto queryWithIdDto) {
        TdxQuery tdxQuery = queryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("TdxQuery not found. ID: " + id));

        tdxQuery.updateTitleAndQuery(queryWithIdDto.getTitle(), queryWithIdDto.getQuery());

        List<TdxQueryParam> newParams = queryWithIdDto.getParameters().stream()
                .map(QueryParamDto::toEntity)
                .collect(Collectors.toList());
        tdxQuery.updateParameters(newParams);

        queryRepository.save(tdxQuery);
    }

}