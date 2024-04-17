package com.opendev.odata.domain.query.controller;

import com.opendev.odata.domain.query.dto.QueryDto;
import com.opendev.odata.domain.query.dto.QueryWithIdDto;
import com.opendev.odata.domain.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public ResponseEntity<?> saveQuery(@RequestBody QueryDto queryDTO) {
        queryService.saveQuery(queryDTO);
        return new ResponseEntity<>("Query created successfully", HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QueryWithIdDto> getQuery(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getQueryById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuery(@PathVariable Long id, @RequestBody QueryWithIdDto queryWithIdDto) {
        try {
            queryService.updateQuery(id, queryWithIdDto);
            return ResponseEntity.ok("Query updated successfully");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Query not found");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuery(@PathVariable Long id) {
        try {
            queryService.deleteQuery(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Query not found");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<QueryWithIdDto>> getAllQueries() {
        return ResponseEntity.ok(queryService.getAllQueries());
    }
}
