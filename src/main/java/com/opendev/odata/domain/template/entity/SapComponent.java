package com.opendev.odata.domain.template.entity;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;

import static javax.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Builder
@Getter
public class SapComponent {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

    private String description;

    private String openComponent;

    private String closeComponent;

    private Integer level;

    private Integer seq;

    @ManyToOne
    @JoinColumn(name = "sap_template_id")
    private SapTemplate sapTemplate;

}
