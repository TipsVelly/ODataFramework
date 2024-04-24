package com.opendev.odata.framework.service;

import com.opendev.odata.framework.mapper.CustomJpaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomEntityProcessor implements EntityProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    private final CustomJpaRepository customJpaRepository;

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    private final EntityManager entityManager;



    // 기존의 readEntity 메소드
    @Override
    public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) {
        try {
            UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
            EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
            String tableName = getTableNameFromEdmEntitySet(edmEntitySet);
            Long entityId = Long.parseLong(uriResourceEntitySet.getKeyPredicates().get(0).getText());

            Map<String, Object> entityData = customJpaRepository.findById(tableName, entityId);
            if (entityData == null) {
                response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
                return;
            }

            Entity entity = convertToEdmEntity(entityData, edmEntitySet);
            serializeAndSendEntity(entity, edmEntitySet, response, responseFormat);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) {
        try {
            String tableName = "";
            EdmEntitySet edmEntitySet = null;

            // UriInfo에서 EdmEntityType와 EdmEntitySet 추출
            if (!uriInfo.getUriResourceParts().isEmpty() && uriInfo.getUriResourceParts().get(0) instanceof UriResourceEntitySet) {
                UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
                edmEntitySet = uriResourceEntitySet.getEntitySet();
                tableName = getTableNameFromEdmEntitySet(edmEntitySet); // 복수형에서 단수형으로 변환하는 로직 포함
            }

            if (edmEntitySet == null) {
                throw new ODataApplicationException("요청에서 엔티티 세트를 찾을 수 없습니다.", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
            }

            InputStream requestInputStream = request.getBody();
            ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
            DeserializerResult result = deserializer.entity(requestInputStream, edmEntitySet.getEntityType());
            Entity requestEntity = result.getEntity();

            Map<String, Object> entityMap = entityToMap(requestEntity);
            Long createdEntityId = customJpaRepository.create(tableName, entityMap);

            Entity createdEntity = new Entity();
            createdEntity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, createdEntityId));
            entityMap.forEach((key, value) -> createdEntity.addProperty(new Property(null, key, ValueType.PRIMITIVE, value)));

            // edmEntityType 대신 edmEntitySet을 사용하여 메서드 호출을 수정
            serializeAndSendEntity(createdEntity, edmEntitySet, response, responseFormat);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }





    @Override
    public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) {
        try {
            // Extract table name and entity ID from UriInfo
            UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
            EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
            String tableName = getTableNameFromEdmEntitySet(edmEntitySet);
            Long entityId = Long.parseLong(uriResourceEntitySet.getKeyPredicates().get(0).getText());

            // Deserialize the incoming request to an Entity
            InputStream requestInputStream = request.getBody();
            ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
            DeserializerResult result = deserializer.entity(requestInputStream, edmEntitySet.getEntityType());
            Entity requestEntity = result.getEntity();

            // Convert Entity to map for database update
            Map<String, Object> entityMap = entityToMap(requestEntity);

            // Update the entity in the database
            // Note: You need to implement this method in your customJpaRepository
            customJpaRepository.update(tableName, entityId, entityMap);

            // Respond with updated entity
            // Note: You might want to fetch the updated entity from the database and serialize it for the response
            response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }


    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo) {
        try {
            UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
            EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
            String tableName = getTableNameFromEdmEntitySet(edmEntitySet);
            Long entityId = Long.parseLong(uriResourceEntitySet.getKeyPredicates().get(0).getText());

            // Delete the entity from the database
            customJpaRepository.delete(tableName, entityId);

            // Set response status code
            response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    private String toPascalCase(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }
        // 첫 글자만 대문자로 변환
        return columnName.substring(0, 1).toUpperCase() + columnName.substring(1).toLowerCase();
    }


    // 데이터베이스 컬럼을 EDM 프로퍼티로 변환하는 메소드
    private Entity convertToEdmEntity(Map<String, Object> entityData, EdmEntitySet edmEntitySet) {
        Entity entity = new Entity();
        entityData.forEach((key, value) -> {
            // 'id' 컬럼은 'ID'로 변환
            String edmPropertyName = "id".equals(key) ? "ID" : toPascalCase(key);
            entity.addProperty(new Property(null, edmPropertyName, ValueType.PRIMITIVE, value));
        });
        // ID 값을 Long 타입으로 변환하여 createId 메서드 호출
        Object idValue = entityData.get("id");
        if (idValue != null) {
            Long idLongValue = Long.parseLong(idValue.toString()); // String을 Long으로 변환
            entity.setId(createId(edmEntitySet.getName(), idLongValue));
        }
        return entity;
    }

    // 엔티티 세트 이름에서 's' 제거하는 메소드 (복수형 -> 단수형 변환)
    private String getTableNameFromEdmEntitySet(EdmEntitySet edmEntitySet) {
        String tableName = edmEntitySet.getName();
        if (tableName.endsWith("s")) {
            tableName = tableName.substring(0, tableName.length() - 1);
        }
        return tableName;
    }

    // 엔티티 직렬화 및 응답 전송 메소드
    private void serializeAndSendEntity(Entity entity, EdmEntitySet edmEntitySet, ODataResponse response, ContentType responseFormat) throws SerializerException {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        ODataSerializer serializer = odata.createSerializer(responseFormat);
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();
        EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();
        SerializerResult serializedContent = serializer.entity(serviceMetadata, edmEntityType, entity, options);

        response.setContent(serializedContent.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    private URI createId(String entitySetName, Long id) {
        try {
            return new URI(entitySetName + "(" + id + ")");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to create ID for entity: " + entitySetName, e);
        }
    }

    private Map<String, Object> entityToMap(Entity entity) {
        Map<String, Object> entityMap = new HashMap<>();
        for (Property property : entity.getProperties()) {
            String propName = property.getName();
            Object propValue = property.getValue();
            //데이터베이스 컬럼 이름으로 변환 필요 시 아래 주석 해제
            String dbColumnName = toDatabaseColumnName(propName);
            entityMap.put(dbColumnName, propValue);

        }
        return entityMap;
    }
    // 속성 이름을 데이터베이스 컬럼 이름으로 변환하는 메소드 (여기서는 모두 소문자로)
    private String toDatabaseColumnName(String propName) {
        return propName.toLowerCase();
    }

}
