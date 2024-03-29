package com.opendev.odata.domain.table.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class TableSchemaDTO {
    private String tableName;
    private List<ColumnDTO> columns;

}
