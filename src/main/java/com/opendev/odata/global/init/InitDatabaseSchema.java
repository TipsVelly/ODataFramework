package com.opendev.odata.global.init;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Configuration
@RequiredArgsConstructor
public class InitDatabaseSchema {

    @Autowired
    private InitDatabaseSchema self;


    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    @Order(3)
    public ApplicationRunner initdb() {
        return args -> {
            try {
                self.createTableDefinitions();
                self.createColumnDefinitions();
            }catch (Exception e){
                e.printStackTrace();
            }
        };
    }

    @Transactional
    @SneakyThrows(RuntimeException.class)
    public void createTableDefinitions() {
        if (tableNotExists("tdx_table")) {
            entityManager.createNativeQuery(
                    "CREATE TABLE tdx_table (" +
                            "table_id SERIAL PRIMARY KEY, " +
                            "table_name VARCHAR(255) NOT NULL, " +
                            "table_description TEXT" +
                            ")").executeUpdate();
        }
    }
    @Transactional
    @SneakyThrows(RuntimeException.class)
    public void createColumnDefinitions() {
        if (tableNotExists("tdx_column")) {
            entityManager.createNativeQuery(
                    "CREATE TABLE tdx_column (" +
                            "column_id SERIAL PRIMARY KEY, " +
                            "table_id INT NOT NULL REFERENCES tdx_table(table_id), " +
                            "column_name VARCHAR(255) NOT NULL, " +
                            "column_description TEXT, " +
                            "column_type VARCHAR(255) NOT NULL" +
                            ")").executeUpdate();
        }
    }
    @Transactional
    public boolean tableNotExists(String tableName) {
        try {
            String query = "SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = :tableName)";
            Boolean exists = (Boolean) entityManager.createNativeQuery(query)
                    .setParameter("tableName", tableName)
                    .getSingleResult();
            return !exists;
        } catch (Exception e) {
            // 로그를 통해 예외 정보를 확인
            e.printStackTrace();
            return false;
        }
    }

}