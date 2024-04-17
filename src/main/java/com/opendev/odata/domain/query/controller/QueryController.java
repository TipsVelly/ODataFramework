package com.opendev.odata.domain.query.controller;


import com.opendev.odata.domain.query.dto.QueryDto;
import com.opendev.odata.domain.query.dto.QueryWithIdDto;
import com.opendev.odata.domain.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public void saveQuery(@RequestBody QueryDto queryDTO) {
        queryService.saveQuery(queryDTO);

    }

    @GetMapping("/{id}")
    public ResponseEntity<QueryWithIdDto> getQuery(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getQueryById(id));
    }

    @PutMapping("/{id}")
    public void updateQuery(@PathVariable Long id, @RequestBody QueryWithIdDto queryWithIdDto) {
        queryService.updateQuery(id, queryWithIdDto);
    }

    @DeleteMapping("/{id}")
    public void deleteQuery(@PathVariable Long id) {
        queryService.deleteQuery(id);
    }

    @GetMapping("/all")
    public ResponseEntity<List<QueryWithIdDto>> getAllQueries() {
        return ResponseEntity.ok(queryService.getAllQueries());
    }
}