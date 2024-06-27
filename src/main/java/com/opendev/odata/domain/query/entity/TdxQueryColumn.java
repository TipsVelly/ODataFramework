package com.opendev.odata.domain.query.entity;


import lombok.*;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Builder
@Getter
public class TdxQueryColumn {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tdx_query_id", nullable = false)
    private TdxQuery tdxQuery;

    private String columnName;
    private String columnType;

    private String sapui5ViewType;
    private String width;
    private String alignment;
    private String hidden;

    public void assignToQuery(TdxQuery query) {
        this.tdxQuery = query;
    }
}