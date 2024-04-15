package com.opendev.odata.domain.query.repository;

import com.opendev.odata.domain.query.entity.TdxQueryParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryParamRepository extends JpaRepository<TdxQueryParam, Long> {
}
