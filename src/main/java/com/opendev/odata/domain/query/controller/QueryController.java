package com.opendev.odata.domain.query.controller;


import com.opendev.odata.domain.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/queries")
@RequiredArgsConstructor
public class QueryController {


    private final QueryService queryService;

    @PostMapping
    public void saveQuery(@RequestBody Map<String, String> payload) {
        queryService.saveQuery(payload.get("title"), payload.get("query"));
    }

    @GetMapping("/{id}")
    public Map<String, Object> getQuery(@PathVariable Long id) {
        return queryService.getQueryById(id);
    }

    @PutMapping("/{id}")
    public void updateQuery(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        queryService.updateQuery(id, payload.get("title"), payload.get("query"));
    }

    @DeleteMapping("/{id}")
    public void deleteQuery(@PathVariable Long id) {
        queryService.deleteQuery(id);
    }
}