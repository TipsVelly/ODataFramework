package com.opendev.odata.framework.service;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.PrimitiveCollectionProcessor;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.processor.PrimitiveValueProcessor;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class CustomPrimitiveProcessor implements PrimitiveProcessor, PrimitiveValueProcessor, PrimitiveCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    @Override
    public void init(OData oData, ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
        // Retrieve the entity set and key from the first URI part
        UriResourceEntitySet entitySetResource = (UriResourceEntitySet) uriInfo.getUriResourceParts().get(0);
        List<UriParameter> keyPredicates = entitySetResource.getKeyPredicates();

        // Retrieve the target property from the second URI part
        UriResourcePrimitiveProperty uriProperty = (UriResourcePrimitiveProperty) uriInfo.getUriResourceParts().get(1);
        EdmPrimitiveType propertyType = (EdmPrimitiveType) uriProperty.getProperty().getType();

        // Find the entity based on the key
        Entity entity = findEntityByKey(entitySetResource.getEntitySet().getName(), keyPredicates);
        if (entity == null) {
            throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        // Find the property within the entity
        Property property = entity.getProperty(uriProperty.getProperty().getName());
        if (property == null) {
            throw new ODataApplicationException("Property not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        // Serialize the property
        ODataSerializer serializer = this.odata.createSerializer(responseFormat);
        PrimitiveSerializerOptions options = PrimitiveSerializerOptions.with().build();
        SerializerResult serializerResult = serializer.primitive(serviceMetadata, propertyType, property, options);

        // Configure the response
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader("Content-Type", responseFormat.toContentTypeString());
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