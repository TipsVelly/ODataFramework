package com.opendev.odata.framework.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CustomJpaRepository {

    // ID로 객체 찾기, 테이블 이름 동적으로 처리
    Map<String, Object> findById(@Param("tableName") String tableName, @Param("id") Long id);

    Map<String, Object> findByName(@Param("tableName") String tableName, @Param("name") String name);

    // ID로 존재 여부 확인, 테이블 이름 동적으로 처리
    boolean existsById(@Param("tableName") String tableName, @Param("id") Long id);

    // 모든 객체 조회, 테이블 이름 동적으로 처리
    List<Map<String, Object>> findAll(@Param("tableName") String tableName);

    // ID 목록으로 객체 조회, 테이블 이름 동적으로 처리
    List<Map<String, Object>> findAllById(@Param("tableName") String tableName, @Param("ids") List<Long> ids);

    // 객체 개수 세기, 테이블 이름 동적으로 처리
    long count(@Param("tableName") String tableName);

    // ID로 객체 삭제, 테이블 이름 동적으로 처리
    void deleteById(@Param("tableName") String tableName, @Param("id") Long id);

    // 객체 삭제, 테이블 이름 동적으로 처리
    void delete(@Param("tableName") String tableName, @Param("id") Long id);

    // 모든 객체 삭제, 테이블 이름 동적으로 처리
    void deleteAll(@Param("tableName") String tableName);
}

