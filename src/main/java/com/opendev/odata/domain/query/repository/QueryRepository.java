package com.opendev.odata.domain.query.repository;

import com.opendev.odata.domain.query.entity.TdxQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRepository extends JpaRepository<TdxQuery, Long> {
}