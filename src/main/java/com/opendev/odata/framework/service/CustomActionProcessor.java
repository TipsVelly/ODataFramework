package com.opendev.odata.framework.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opendev.odata.domain.query.entity.TdxQuery;
import com.opendev.odata.domain.query.entity.TdxQueryParam;
import com.opendev.odata.domain.query.repository.QueryParamRepository;
import com.opendev.odata.domain.query.repository.QueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.ActionEntityCollectionProcessor;
import org.apache.olingo.server.api.processor.ActionEntityProcessor;
import org.apache.olingo.server.api.processor.ActionPrimitiveProcessor;
import org.apache.olingo.server.api.processor.ActionVoidProcessor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceAction;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomActionProcessor implements ActionVoidProcessor, ActionEntityCollectionProcessor, ActionEntityProcessor, ActionPrimitiveProcessor {
    private OData odata;
    private ServiceMetadata serviceMetadata;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QueryRepository queryRepository;
    private final QueryParamRepository queryParamRepository;

    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }
    @Override
    public void processActionPrimitive(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        try {
            String actionName = extractActionName(uriInfo);
            String httpMethod = oDataRequest.getMethod().name();
            TdxQuery tdxQuery = queryRepository.findByODataQueryName(actionName);

            if (tdxQuery == null) {
                throw new ODataApplicationException("Action not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
            }

            Map<String, Object> actionParameters = deserializeJsonToMap(oDataRequest);

            // `id` 필드를 제거하여 PostgreSQL이 자동 증가 값을 생성하게 함
            actionParameters.remove("id");

            String sqlQuery = buildSqlQueryWithParams(tdxQuery, actionParameters);

            executeSqlQuery(sqlQuery, actionParameters);

            // JSON 응답 생성
            Property resultProperty = new Property(null, "Result", ValueType.PRIMITIVE, "Success");
            ByteArrayInputStream jsonResponse = serializeToJSON(actionName, resultProperty);

            oDataResponse.setContent(jsonResponse);
            oDataResponse.setStatusCode(HttpStatusCode.OK.getStatusCode());
            oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        } catch (Exception e) {
            throw new ODataApplicationException("Error processing action: " + e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
        }
    }

    private ByteArrayInputStream serializeToJSON(String actionName, Property property) {
        JsonObjectBuilder rootBuilder = Json.createObjectBuilder();
        rootBuilder.add("@odata.context", "$metadata#" + actionName);
        rootBuilder.add(property.getName(), property.getValue().toString());

        StringWriter stringWriter = new StringWriter();
        Json.createWriter(stringWriter).write(rootBuilder.build());

        return new ByteArrayInputStream(stringWriter.toString().getBytes());
    }

    private String extractActionName(UriInfo uriInfo) throws ODataApplicationException {
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        if (resourceParts.isEmpty() || !(resourceParts.get(0) instanceof UriResourceAction)) {
            throw new ODataApplicationException("Expected an action but found none",
                    HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
        }
        return ((UriResourceAction) resourceParts.get(0)).getAction().getName();
    }

    private Map<String, Object> deserializeJsonToMap(ODataRequest oDataRequest) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(oDataRequest.getBody());
        return mapper.convertValue(jsonNode, Map.class);
    }

    private String buildSqlQueryWithParams(TdxQuery tdxQuery, Map<String, Object> actionParameters) {
        List<TdxQueryParam> queryParams = queryParamRepository.findByTdxQuery(tdxQuery);
        String query = tdxQuery.getQuery();

        for (TdxQueryParam param : queryParams) {
            String paramName = param.getParameter().replace(":", "");
            query = query.replace(":" + paramName, ":" + paramName);
        }
        return query;
    }

    private void executeSqlQuery(String query, Map<String, Object> params) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        params.forEach(paramSource::addValue);

        jdbcTemplate.update(query, paramSource);
    }

    @Override
    public void processActionEntityCollection(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        System.out.println("CustomActionProcessor.processActionEntityCollection()");
    }

    @Override
    public void processActionEntity(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        System.out.println("CustomActionProcessor.processActionEntity()");
    }

    @Override
    public void processActionVoid(ODataRequest oDataRequest, ODataResponse oDataResponse, UriInfo uriInfo, ContentType contentType) throws ODataApplicationException, ODataLibraryException {
        System.out.println("CustomActionProcessor.processActionVoid()");
    }
}
