package com.opendev.odata.domain.query.repository;

import com.opendev.odata.domain.query.entity.TdxQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRepository extends JpaRepository<TdxQuery, Long> {
    @Query("SELECT q FROM TdxQuery q WHERE q.odataQueryName = :odataQueryName")
    TdxQuery findByODataQueryName(@Param("odataQueryName") String odataQueryName);
}