package com.opendev.odata.domain.table.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ColumnDTO {
    private String columnName;
    private String columnType;
}
