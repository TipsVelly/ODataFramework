package com.opendev.odata.global.dynamic.service;

import com.opendev.odata.domain.table.dto.ColumnDTO;
import com.opendev.odata.domain.table.dto.TableSchemaDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class DatabaseMetadataService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public List<TableSchemaDTO> getTableSchemas() {
        // 테이블과 컬럼 정보를 한 번에 가져오는 네이티브 쿼리
        List<Object[]> results = entityManager.createNativeQuery(
                        "SELECT t.id, t.name, c.name as columnName, c.type " +
                                "FROM tdx_table t " +
                                "LEFT JOIN tdx_column c ON t.id = c.table_id " +
                                "ORDER BY t.id, c.id")
                .getResultList();

        Map<String, TableSchemaDTO> schemaMap = new LinkedHashMap<>();

        for (Object[] result : results) {
            String tableName = (String) result[1];
            String columnName = (String) result[2];
            String columnType = (String) result[3];

            TableSchemaDTO tableSchema = schemaMap.computeIfAbsent(tableName, k -> new TableSchemaDTO(tableName, new ArrayList<>()));

            if (columnName != null && columnType != null) {
                tableSchema.getColumns().add(new ColumnDTO(columnName, columnType));
            }
        }

        return new ArrayList<>(schemaMap.values());
    }

}