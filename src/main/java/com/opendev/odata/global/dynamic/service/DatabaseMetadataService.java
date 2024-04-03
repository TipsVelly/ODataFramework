package com.opendev.odata.global.dynamic.service;

import com.opendev.odata.domain.table.dto.ColumnDTO;
import com.opendev.odata.domain.table.dto.TableSchemaDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;


@Service
public class DatabaseMetadataService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public List<TableSchemaDTO> getTableSchemas() {
        List<TableSchemaDTO> tableSchemas = new ArrayList<>();

        // tdx_table 테이블에서 모든 테이블 정보를 조회하는 네이티브 SQL 쿼리
        List<Object[]> tables = entityManager.createNativeQuery(
                        "SELECT t.id, t.name FROM tdx_table t")
                .getResultList();

        for (Object[] table : tables) {
            Integer tableId = (Integer) table[0];
            String tableName = (String) table[1];

            // tdx_column 테이블에서 현재 테이블의 컬럼 정보를 조회하는 네이티브 SQL 쿼리
            List<Object[]> columnsData = entityManager.createNativeQuery(
                            "SELECT c.name, c.type FROM tdx_column c WHERE c.table_id = ?1")
                    .setParameter(1, tableId)
                    .getResultList();

            List<ColumnDTO> columns = new ArrayList<>();
            for (Object[] columnData : columnsData) {
                String columnName = (String) columnData[0];
                String columnType = (String) columnData[1];
                columns.add(new ColumnDTO(columnName, columnType));
            }

            tableSchemas.add(new TableSchemaDTO(tableName, columns));
        }

        return tableSchemas;
    }
}