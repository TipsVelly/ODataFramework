package com.opendev.odata.framework.service;

import com.opendev.odata.domain.query.entity.TdxQuery;
import com.opendev.odata.domain.query.entity.TdxQueryParam;
import com.opendev.odata.domain.query.repository.QueryParamRepository;
import com.opendev.odata.domain.query.repository.QueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.PrimitiveCollectionProcessor;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.processor.PrimitiveValueProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

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


    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        UriResourceFunction uriResourceFunction = extractFunctionFromUri(uriInfo);
        TdxQuery tdxQuery = queryRepository.findByODataQueryName(uriResourceFunction.getFunction().getName());

        if (tdxQuery == null) {
            throw new ODataApplicationException("Function not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }


        List<Map<String, Object>> result = executeQuery(resolveAndExecuteQuery(uriResourceFunction.getParameters(), tdxQuery));
        serializeResult(result, response, responseFormat);
    }

    private UriResourceFunction extractFunctionFromUri(UriInfo uriInfo) throws ODataApplicationException {
        UriResource firstSegment = uriInfo.getUriResourceParts().get(0);
        if (!(firstSegment instanceof UriResourceFunction)) {
            throw new ODataApplicationException("Invalid function call",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
        return (UriResourceFunction) firstSegment;
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

    private void serializeResult(List<Map<String, Object>> result, ODataResponse response, ContentType responseFormat) throws SerializerException {
        EntityCollection entityCollection = new EntityCollection();
        result.forEach(row -> {
            Entity entity = new Entity();
            row.forEach((key, value) -> entity.addProperty(new Property(null, key, ValueType.PRIMITIVE, value)));
            entityCollection.getEntities().add(entity);
        });
        final String id = "test";
        // Static definition of EdmEntityType and ContextURL
        ContextURL contextURL = ContextURL.with().entitySetOrSingletonOrType("products").build();
        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id).contextURL(contextURL).build();

        EdmEntityType edmEntityType = serviceMetadata.getEdm().getEntityType(new FullQualifiedName("OData.framework", "products"));

        ODataSerializer serializer = odata.createSerializer(responseFormat);
        SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entityCollection, opts);

        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }




    @Override
    public void updatePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        throw new ODataApplicationException("Update operation not supported for primitive properties", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    @Override
    public void deletePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo) throws ODataApplicationException, ODataLibraryException {
        throw new ODataApplicationException("Delete operation not supported for primitive properties", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
    }

    private Entity findEntityByKey(String entitySetName, List<UriParameter> keyPredicates) {
        // This method should implement the logic to retrieve an entity based on the key predicates provided
        // For the sake of example, this is a stub and needs actual implementation
        System.out.println("findEntityByKey: method execute");
        return null;
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