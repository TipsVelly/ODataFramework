package com.opendev.odata.global.init;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
                            "id SERIAL PRIMARY KEY, " +
                            "name VARCHAR(255) NOT NULL, " +
                            "description TEXT" +
                            ")").executeUpdate();
        }
    }
    @Transactional
    @SneakyThrows(RuntimeException.class)
    public void createColumnDefinitions() {
        if (tableNotExists("tdx_column")) {
            entityManager.createNativeQuery(
                    "CREATE TABLE tdx_column (" +
                            "id SERIAL PRIMARY KEY, " +
                            "table_id INT NOT NULL REFERENCES tdx_table(table_id), " +
                            "name VARCHAR(255) NOT NULL, " +
                            "type VARCHAR(255) NOT NULL" +
                            ")").executeUpdate();
        }
    }
    @Transactional
    public boolean tableNotExists(String tableName) {
        try {
            // 파라미터 처리 불가로 인해 불가피하게 1 사용
            String query = "SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = ?1)";
            Boolean exists = (Boolean) entityManager.createNativeQuery(query)
                    .setParameter(1, tableName)
                    .getSingleResult();
            return !exists;
        } catch (Exception e) {
            log.error("테이블 존재 여부 확인 중 오류 발생: {}", tableName, e);
            return false;
        }
    }

}