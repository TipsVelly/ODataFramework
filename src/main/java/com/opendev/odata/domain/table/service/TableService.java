package com.opendev.odata.domain.table.service;


import com.opendev.odata.domain.table.dto.ColumnDTO;
import com.opendev.odata.domain.table.dto.TableSchemaDTO;
import com.opendev.odata.framework.service.CustomEdmProvider;
import com.opendev.odata.framework.service.CustomEntityCollectionProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TableService {
    private final CustomEntityCollectionProcessor customEntityCollectionProcessor;
    private final CustomEdmProvider customEdmProvider;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void createTableAndRegisterOData(TableSchemaDTO tableSchema) {
        createTable(tableSchema);
        registerTableInOData(tableSchema.getTableName());
    }

    private void createTable(TableSchemaDTO tableSchema) {
        StringBuilder queryBuilder = new StringBuilder("CREATE TABLE " + tableSchema.getTableName() + " (");
        for (int i = 0; i < tableSchema.getColumns().size(); i++) {
            ColumnDTO column = tableSchema.getColumns().get(i);
            String columnType = column.getColumnType();

            // MySQL에서 varchar 타입을 사용할 때는 길이를 지정해야 합니다.
            // 여기서는 varchar 타입에 대해 기본 길이로 255를 설정합니다.
            if ("varchar".equalsIgnoreCase(columnType)) {
                columnType += "(255)"; // varchar 타입의 기본 길이를 255로 설정
            }

            queryBuilder.append(column.getColumnName()).append(" ").append(columnType);
            if (i < tableSchema.getColumns().size() - 1) {
                queryBuilder.append(", ");
            }
        }
        queryBuilder.append(")");

        Query query = entityManager.createNativeQuery(queryBuilder.toString());
        query.executeUpdate();
    }


    private void registerTableInOData(String tableName) {
        TableSchemaDTO tableSchema = fetchTableSchemaFromDatabase(tableName);
        customEdmProvider.registerTable(tableSchema);
    }

    public TableSchemaDTO fetchTableSchemaFromDatabase(String tableName) {
        // 예시로, 간단한 테이블 스키마 정보를 하드코딩합니다. 실제로는 데이터베이스 메타데이터를 조회하는 로직이 필요합니다.
        // 테이블을 명시하는 테이블과 테이블의 컬럼정보를 명시하는 테이블에서 데이터를 가져와야함
        TableSchemaDTO schema = new TableSchemaDTO();
        schema.setTableName(tableName);
        List<ColumnDTO> columns = new ArrayList<>();
        columns.add(new ColumnDTO("ID", "Integer"));
        columns.add(new ColumnDTO("Name", "String"));
        columns.add(new ColumnDTO("Description", "String"));
        schema.setColumns(columns);
        return schema;
    }



}