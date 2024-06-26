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
public class TdxQueryParam {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tdx_query_id", nullable = false)
    private TdxQuery tdxQuery;

    private String parameter;

    private String attribute;

    private String value;
    public void assignToQuery(TdxQuery query) {
        this.tdxQuery = query;
    }

}
