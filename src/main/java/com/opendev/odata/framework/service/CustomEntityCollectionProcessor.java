package com.opendev.odata.framework.service;

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

	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

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

		// EdmEntitySet의 이름에서 's'를 제거하여 실제 테이블 이름을 유추합니다.
		String actualTableName = edmEntitySet.getName();
		if (actualTableName.endsWith("s")) {
			actualTableName = actualTableName.substring(0, actualTableName.length() - 1);
		}

		try {
			// 수정된 테이블 이름을 사용하여 데이터베이스 쿼리를 실행합니다.
			List<Object[]> results = entityManager.createNativeQuery("SELECT * FROM " + actualTableName).getResultList();
			for (Object[] row : results) {
				Entity entity = new Entity();

				// 각 열에 대한 데이터를 Entity의 프로퍼티로 추가합니다.
				// 열 인덱스는 결과 세트의 구조에 따라 조정되어야 합니다.
				entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, row[0]));
				entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, row[1]));
				entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE, row[2]));

				// ID를 기반으로 Entity의 ID를 생성합니다.
				entity.setId(createId(edmEntitySet.getName(), row[0]));

				// EntityCollection의 리스트에 Entity를 추가합니다.
				collection.getEntities().add(entity);
			}
		} catch (Exception e) {
			// 쿼리 실행 중 오류가 발생한 경우, ODataApplicationException을 던집니다.
			throw new ODataApplicationException("Error fetching data from database",
					HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.getDefault());
		}
		return collection;
	}



	private URI createId(String entitySetName, Object id) {
	    try {
	        return new URI(entitySetName + "(" + id + ")");
	    } catch (URISyntaxException e) {
	        throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
	    }
	}


}
