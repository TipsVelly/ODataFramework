package com.opendev.odata.domain.table.service;


import com.opendev.odata.domain.table.dto.ColumnDTO;
import com.opendev.odata.domain.table.dto.TableSchemaDTO;
import com.opendev.odata.domain.table.mapper.TableMapper;
import com.opendev.odata.framework.service.CustomEdmProvider;
import com.opendev.odata.framework.service.CustomEntityCollectionProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TableService {
    private final CustomEntityCollectionProcessor customEntityCollectionProcessor;
    private final CustomEdmProvider customEdmProvider;
    private final TableMapper tableMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void createTableAndRegisterOData(TableSchemaDTO tableSchema) {
        String createTableSql = generateCreateTableSql(tableSchema); // SQL 생성 로직
        tableMapper.createTable(createTableSql);
        registerTableInOData(tableSchema);
    }


    private String generateCreateTableSql(TableSchemaDTO tableSchema) {
        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        sql.append(tableSchema.getTableName());
        sql.append(" (");
        List<ColumnDTO> columns = tableSchema.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            ColumnDTO column = columns.get(i);
            sql.append(column.getColumnName());
            sql.append(" ");
            sql.append(column.getColumnType());

            // VARCHAR 타입에 대한 길이 설정이 필요한 경우 여기에 추가
            if ("varchar".equalsIgnoreCase(column.getColumnType())) {
                sql.append("(255)"); // 예를 들어, varchar의 기본 길이를 255로 설정
            }

            // 마지막 열이 아니라면 콤마 추가
            if (i < columns.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(");");
        return sql.toString();
    }


    private void registerTableInOData(TableSchemaDTO tableSchema) {
        customEdmProvider.registerTable(tableSchema);
    }





}