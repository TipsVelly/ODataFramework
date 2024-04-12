package com.opendev.odata.domain.query.service;


import com.opendev.odata.domain.query.mapper.QueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class QueryService {


    private final QueryMapper queryMapper;

    public void saveQuery(String title, String query) {
        queryMapper.insertQuery(title, query);
    }

    public Map<String, Object> getQueryById(Long id) {
        return queryMapper.findQueryById(id);
    }

    public void updateQuery(Long id, String title, String query) {
        queryMapper.updateQuery(id, title, query);
    }

    public void deleteQuery(Long id) {
        queryMapper.deleteQuery(id);
    }
}