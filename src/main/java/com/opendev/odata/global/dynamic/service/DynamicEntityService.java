package com.opendev.odata.global.dynamic.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

@Service
public class DynamicEntityService {

    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;

    public DynamicEntityService(JdbcTemplate jdbcTemplate, EntityManager entityManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
    }

    // CREATE
    public void create(String tableName, Object... parameters) {
        String sql = "INSERT INTO " + tableName + " VALUES (?)"; // 적절한 SQL 쿼리로 대체
        jdbcTemplate.update(sql, parameters);
    }

    // READ
    public List<Object[]> read(String tableName, String whereClause, Object... parameters) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + whereClause; // 적절한 조건 추가
        Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < parameters.length; i++) {
            query.setParameter(i + 1, parameters[i]);
        }
        return query.getResultList();
    }

    // UPDATE
    public void update(String tableName, String setClause, String whereClause, Object... parameters) {
        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereClause; // 적절한 SQL 쿼리로 대체
        jdbcTemplate.update(sql, parameters);
    }

    // DELETE
    public void delete(String tableName, String whereClause, Object... parameters) {
        String sql = "DELETE FROM " + tableName + " WHERE " + whereClause; // 적절한 조건 추가
        jdbcTemplate.update(sql, parameters);
    }
}