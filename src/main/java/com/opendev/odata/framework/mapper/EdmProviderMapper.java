package com.opendev.odata.framework.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EdmProviderMapper {
    void insertTable(@Param("tableName") String tableName, @Param("tableDescription") String tableDescription);
    void insertColumn(@Param("tableId") Long tableId, @Param("columnName") String columnName, @Param("columnType") String columnType);
}
