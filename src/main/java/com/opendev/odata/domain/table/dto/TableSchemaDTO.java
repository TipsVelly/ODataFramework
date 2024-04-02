package com.opendev.odata.domain.table.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
public class TableSchemaDTO {
    private String tableName;
    private List<ColumnDTO> columns;

}
