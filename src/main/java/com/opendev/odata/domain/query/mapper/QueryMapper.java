package com.opendev.odata.domain.query.mapper;


import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface QueryMapper {

    @Insert("INSERT INTO tdx_query (title, query) VALUES (#{title}, #{query})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertQuery(@Param("title") String title, @Param("query") String query);

    @Select("SELECT * FROM tdx_query WHERE id = #{id}")
    Map<String, Object> findQueryById(@Param("id") Long id);

    @Select("SELECT * FROM tdx_query")
    List<Map<String, Object>> findAllQueries();

    @Update("UPDATE tdx_query SET title = #{title}, query = #{query} WHERE id = #{id}")
    void updateQuery(@Param("id") Long id, @Param("title") String title, @Param("query") String query);

    @Delete("DELETE FROM tdx_query WHERE id = #{id}")
    void deleteQuery(@Param("id") Long id);
}