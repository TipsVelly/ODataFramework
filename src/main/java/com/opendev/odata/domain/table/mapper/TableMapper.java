package com.opendev.odata.domain.table.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TableMapper {
    void createTable(String createTableSql);
}
