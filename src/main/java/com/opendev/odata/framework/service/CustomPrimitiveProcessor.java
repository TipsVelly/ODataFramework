package com.opendev.odata.framework.service;

import com.opendev.odata.domain.query.entity.TdxQuery;
import com.opendev.odata.domain.query.entity.TdxQueryParam;
import com.opendev.odata.domain.query.repository.QueryParamRepository;
import com.opendev.odata.domain.query.repository.QueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.PrimitiveCollectionProcessor;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.processor.PrimitiveValueProcessor;
import org.apache.olingo.server.api.uri.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class CustomPrimitiveProcessor implements PrimitiveProcessor, PrimitiveValueProcessor, PrimitiveCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    private final QueryRepository queryRepository;

    private final QueryParamRepository queryParamRepository;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final CustomEdmProvider customEdmProvider;


    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        try {
            UriResourceFunction uriResourceFunction = extractFunctionFromUri(uriInfo);
            TdxQuery tdxQuery = queryRepository.findByODataQueryName(uriResourceFunction.getFunction().getName());

            if (tdxQuery == null) {
                throw new ODataApplicationException("Function not found",
                        HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
            }

            List<Map<String, Object>> result = executeQuery(resolveAndExecuteQuery(uriResourceFunction.getParameters(), tdxQuery));
            EntityCollection entityCollection = new EntityCollection();

            for (Map<String, Object> row : result) {
                Entity entity = new Entity();
                populateEntityWithProperties(entity, row);
                entityCollection.getEntities().add(entity);
            }
            serializeResult(entityCollection, response, responseFormat, uriInfo);




        } catch (Exception e) {
            throw new ODataApplicationException("Error processing request: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }
    }
    private void serializeResult(EntityCollection entityCollection, ODataResponse response, ContentType responseFormat, UriInfo uriInfo) throws ODataLibraryException, ODataApplicationException {
        if (entityCollection == null || entityCollection.getEntities().isEmpty()) {
            throw new ODataApplicationException("Entity collection is null or empty",
                    HttpStatusCode.NO_CONTENT.getStatusCode(), Locale.ENGLISH);
        }
        try {
            UriResource uriResource = uriInfo.getUriResourceParts().get(0);
            String entitySetOrFunctionName = getEntitySetOrFunctionName(uriResource);

            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            for (Entity entity : entityCollection.getEntities()) {
                JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
                for (Property property : entity.getProperties()) {
                    jsonObjectBuilder.add(property.getName(), property.getValue() == null ? "" : property.getValue().toString()); // Null safety check
                }
                jsonArrayBuilder.add(jsonObjectBuilder);
            }

            StringWriter stringWriter = new StringWriter();

            try (JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
                jsonWriter.writeArray(jsonArrayBuilder.build());
            }

            byte[] jsonBytes = stringWriter.toString().getBytes(StandardCharsets.UTF_8);
            response.setContent(new ByteArrayInputStream(jsonBytes));
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        } catch (Exception e) {
            throw new ODataApplicationException("Serialization error: " + e.getMessage(),
                    HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
        }
    }

    private String getEntitySetOrFunctionName(UriResource uriResource) throws ODataApplicationException {
        if (uriResource instanceof UriResourceEntitySet) {
            return ((UriResourceEntitySet) uriResource).getEntitySet().getName();
        } else if (uriResource instanceof UriResourceFunction) {
            return ((UriResourceFunction) uriResource).getFunctionImport().getName();
        } else {
            throw new ODataApplicationException("Invalid URI segment type",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
    }

    private UriResourceFunction extractFunctionFromUri(UriInfo uriInfo) throws ODataApplicationException {
        UriResource firstSegment = uriInfo.getUriResourceParts().get(0);
        if (firstSegment instanceof UriResourceFunction) {
            return (UriResourceFunction) firstSegment;
        } else {
            throw new ODataApplicationException("Expected a function call but found " + firstSegment.getClass().getSimpleName(),
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
    }


    private String resolveAndExecuteQuery(List<UriParameter> uriParameters, TdxQuery tdxQuery) {
        // 매핑된 쿼리 파라미터를 추출하여 Map으로 변환
        Map<String, String> queryParamMap = tdxQuery.getTdxQueryParams().stream()
                .collect(Collectors.toMap(
                        param -> param.getParameter().replace(":", ""), // URI 파라미터에서 ':' 제거
                        TdxQueryParam::getAttribute, // DB에 매핑된 실제 속성 이름
                        (oldValue, newValue) -> oldValue // 중복 키가 있는 경우 이전 값을 유지
                ));

        String resolvedQuery = tdxQuery.getQuery();
        // URI 파라미터를 반복하여 쿼리 문자열 내의 해당 파라미터를 실제 값으로 치환
        for (UriParameter uriParam : uriParameters) {
            String paramName = uriParam.getName();
            if (queryParamMap.containsKey(paramName)) {
                resolvedQuery = resolvedQuery.replace(":" + paramName, uriParam.getText());
            } else {
                System.out.println("Parameter not found in queryParamMap: " + paramName);
            }
        }
        return resolvedQuery;
    }



    public List<Map<String, Object>> executeQuery(String query) {
        // Execute the query directly without explicitly passing parameters
        return jdbcTemplate.queryForList(query, new MapSqlParameterSource());
    }
    private void populateEntityWithProperties(Entity entity, Map<String, Object> row) throws ODataApplicationException {
        // ID 속성 설정
        Object idValue = row.get("id");
        if (idValue != null) {
            entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, idValue));
            entity.setId(createId("products", idValue.toString()));
        } else {
            throw new ODataApplicationException("ID is null for some entities", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
        }

        // 나머지 속성들을 동적으로 추가
        row.forEach((columnName, value) -> {
            if (!"id".equalsIgnoreCase(columnName)) { // 'id' 컬럼은 이미 처리
                String propertyName = toPascalCase(columnName);
                entity.addProperty(new Property(null, propertyName, ValueType.PRIMITIVE, value));
            }
        });
    }

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase();
    }

    private URI createId(String entitySetName, String idValue) {
        try {
            return new URI(entitySetName + "(" + idValue + ")");
        } catch (Exception e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }


    @Override
    public void updatePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        throw new ODataApplicationException("Update operation not supported for primitive properties", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public void deletePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {
        throw new ODataApplicationException("Delete operation not supported for primitive properties", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }
    @Override
    public void readPrimitiveCollection(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType) throws ODataApplicationException, ODataLibraryException {
        System.out.println("readPrimitiveCollection: method execute");
    }

    @Override
    public void updatePrimitiveCollection(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType, ContentType contentType1) throws ODataApplicationException, ODataLibraryException {
        System.out.println("updatePrimitiveCollection: method execute");
    }

    @Override
    public void deletePrimitiveCollection(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {
        System.out.println("deletePrimitiveCollection: method execute");
    }

    @Override
    public void readPrimitiveValue(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType) throws ODataApplicationException, ODataLibraryException {
        System.out.println("readPrimitiveValue: method execute");
    }

    @Override
    public void updatePrimitiveValue(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType, ContentType contentType1) throws ODataApplicationException, ODataLibraryException {
        System.out.println("updatePrimitiveValue: method execute");
    }

    @Override
    public void deletePrimitiveValue(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {
        System.out.println("deletePrimitiveValue: method execute");
    }
}