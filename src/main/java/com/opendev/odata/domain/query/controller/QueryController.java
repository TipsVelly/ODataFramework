package com.opendev.odata.domain.query.controller;


import com.opendev.odata.domain.query.dto.QueryDTO;
import com.opendev.odata.domain.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public void saveQuery(@RequestBody QueryDTO queryDTO) {
        queryService.saveQuery(queryDTO);
    }

    @GetMapping("/{id}")
    public QueryDTO getQuery(@PathVariable Long id) {
        return queryService.getQueryById(id);
    }

    @PutMapping("/{id}")
    public void updateQuery(@PathVariable Long id, @RequestBody QueryDTO queryDTO) {
        queryDTO.setId(id);
        queryService.updateQuery(queryDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteQuery(@PathVariable Long id) {
        queryService.deleteQuery(id);
    }
}