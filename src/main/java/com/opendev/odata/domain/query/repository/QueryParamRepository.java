package com.opendev.odata.domain.query.repository;

import com.opendev.odata.domain.query.entity.TdxQuery;
import com.opendev.odata.domain.query.entity.TdxQueryParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryParamRepository extends JpaRepository<TdxQueryParam, Long> {
    List<TdxQueryParam> findByTdxQuery(TdxQuery query);
}
