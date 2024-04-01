package com.opendev.odata.global.dynamic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
public class DatabaseMetadataService {

    @PersistenceContext
    private EntityManager entityManager;
    @Transactional
    public List<String> getAllTableNames() {
        // PostgreSQL의 시스템 카탈로그에서 테이블 이름 조회
        String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
        return entityManager.createNativeQuery(query).getResultList();
    }
}