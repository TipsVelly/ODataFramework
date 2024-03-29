package com.opendev.odata.framework.service;

import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;

@Component
@RequiredArgsConstructor
public class CustomeEntityProcessor implements EntityProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    private final EntityManager entityManager;

    @Override
    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) {
        // 기존 구현 유지
        System.out.println("엔티티 읽기 로직 구현 ");
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) {
        // 엔티티 생성 로직 구현
        System.out.println("엔티티 생성 로직 구현 ");
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) {
        // 엔티티 업데이트 로직 구현
        System.out.println("엔티티 업데이트 로직 구현 ");
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo) {
        // 엔티티 삭제 로직 구현
        System.out.println("엔티티 삭제 로직 구현 ");
    }


}
