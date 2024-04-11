package com.opendev.odata.framework.service;

import com.opendev.odata.framework.mapper.CustomJpaRepository;
import com.opendev.odata.global.dynamic.service.DatabaseMetadataService;
import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.data.*;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Subash
 * @since 2/28/2021
 */
@Component
@RequiredArgsConstructor
public class CustomEntityCollectionProcessor implements EntityCollectionProcessor {

	private OData odata;
	private ServiceMetadata serviceMetadata;

	private final EntityManager entityManager;

	private final DatabaseMetadataService databaseMetadataService;

	private final CustomJpaRepository customJpaRepository;

	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}
	@Override
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

		// 1st we have retrieve the requested EntitySet from the uriInfo object
		// (representation of the parsed service URI)
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the
																									// first segment is
																									// the EntitySet
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// 2nd: fetch the data from backend for this requested EntitySetName
		// it has to be delivered as EntitySet object
		EntityCollection entitySet = getData(edmEntitySet);

		// 3rd: create a serializer based on the requested format (json)
		ODataSerializer serializer = odata.createSerializer(responseFormat);

		// 4th: Now serialize the content: transform from the EntitySet object to
		// InputStream
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

		final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
		EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl)
				.build();
		SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet,
				opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}

	private EntityCollection getData(EdmEntitySet edmEntitySet) throws ODataApplicationException {
		EntityCollection collection = new EntityCollection();

		String originalTableName = edmEntitySet.getName();
		String actualTableName = originalTableName.endsWith("s") ? originalTableName.substring(0, originalTableName.length() - 1) : originalTableName;

		List<Map<String, Object>> results = customJpaRepository.findAll(actualTableName);

		for (Map<String, Object> row : results) {
			Entity entity = new Entity();

			// 'ID' 속성을 명시적으로 설정합니다.
			Object idValue = row.get("id");
			if (idValue != null) {
				entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, idValue));
				entity.setId(createId(edmEntitySet.getName(), idValue.toString()));
			} else {
				throw new ODataApplicationException("ID is null for some entities", HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.getDefault());
			}

			// 나머지 속성들을 동적으로 추가합니다.
			row.forEach((columnName, value) -> {
				if (!"id".equalsIgnoreCase(columnName)) { // 'id' 컬럼은 이미 처리했습니다.
					String propertyName = toPascalCase(columnName);
					entity.addProperty(new Property(null, propertyName, ValueType.PRIMITIVE, value));
				}
			});

			collection.getEntities().add(entity);
		}

		return collection;
	}


	private String toPascalCase(String columnName) {
		if (columnName == null || columnName.isEmpty()) {
			return columnName;
		}
		// 첫 글자만 대문자로 변환
		return columnName.substring(0, 1).toUpperCase() + columnName.substring(1).toLowerCase();
	}


	private URI createId(String entitySetName, Object id) {
	    try {
	        return new URI(entitySetName + "(" + id + ")");
	    } catch (URISyntaxException e) {
	        throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
	    }
	}


}
