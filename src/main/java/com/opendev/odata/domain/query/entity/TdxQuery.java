package com.opendev.odata.domain.query.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Builder
@Getter
public class TdxQuery {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String query;

    @OneToMany(mappedBy = "tdxQuery", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<TdxQueryParam> tdxQueryParams = new ArrayList<>();

    private String httpRequest;

    private String odataQueryName;


    // 비즈니스 로직으로 상태 변경
    public void updateTitleAndQuery(String title, String query, String httpRequest, String odataQueryName) {
        this.title = title;
        this.query = query;
        this.httpRequest = httpRequest;
        this.odataQueryName = odataQueryName;

    }

    public void updateParameters(List<TdxQueryParam> newParams) {
        this.tdxQueryParams.clear();
        for (TdxQueryParam param : newParams) {
            param.assignToQuery(this);
            this.tdxQueryParams.add(param);
        }
    }

}
