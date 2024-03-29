package com.opendev.odata.domain.table.controller;

import com.opendev.odata.domain.table.dto.TableSchemaDTO;
import com.opendev.odata.domain.table.service.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TableController {

    @Autowired
    private TableService tableService;

    @PostMapping("/createTableAndRegisterOData")
    public void createTableAndRegisterOData(@RequestBody TableSchemaDTO tableSchema) {
        tableService.createTableAndRegisterOData(tableSchema);
    }
}