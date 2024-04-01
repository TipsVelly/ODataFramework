package com.opendev.odata.framework.service;

import com.opendev.odata.domain.table.dto.TableSchemaDTO;
import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CustomEdmProvider extends CsdlAbstractEdmProvider {

	private Map<String, TableSchemaDTO> dynamicTables = new HashMap<>();

	@Override
	public List<CsdlSchema> getSchemas() throws ODataException {
		List<CsdlSchema> schemas = new ArrayList<>();
		CsdlSchema schema = new CsdlSchema();
		schema.setNamespace("OData.framework");

		schema.setEntityTypes(new ArrayList<>(getEntityTypes().values()));

		CsdlEntityContainer container = new CsdlEntityContainer();
		container.setName("Container");
		container.setEntitySets(new ArrayList<>(getEntitySets().values()));

		schema.setEntityContainer(container);
		schemas.add(schema);
		return schemas;
	}

	private Map<String, CsdlEntityType> getEntityTypes() {
		Map<String, CsdlEntityType> entityTypes = new HashMap<>();
		dynamicTables.forEach((tableName, tableSchema) -> entityTypes.put(tableName, createCsdlEntityType(tableSchema)));
		return entityTypes;
	}

	private CsdlEntityType createCsdlEntityType(TableSchemaDTO tableSchema) {
		List<CsdlProperty> csdlProperties = new ArrayList<>();
		CsdlPropertyRef propertyRef = new CsdlPropertyRef();
		propertyRef.setName("ID"); // Assuming 'ID' as the primary key

		tableSchema.getColumns().forEach(column -> csdlProperties.add(new CsdlProperty()
				.setName(column.getColumnName())
				.setType(convertJavaTypeToEdmType(column.getColumnType()))));

		return new CsdlEntityType().setName(tableSchema.getTableName())
				.setProperties(csdlProperties)
				.setKey(Collections.singletonList(propertyRef));
	}

	private Map<String, CsdlEntitySet> getEntitySets() {
		Map<String, CsdlEntitySet> entitySets = new HashMap<>();
		dynamicTables.forEach((tableName, tableSchema) -> entitySets.put(tableName + "s", createCsdlEntitySet(tableName + "s", new FullQualifiedName("OData.framework", tableName))));
		return entitySets;
	}

	public CsdlEntitySet createCsdlEntitySet(String entitySetName, FullQualifiedName entityTypeFQN) {
		return new CsdlEntitySet().setName(entitySetName).setType(entityTypeFQN);
	}

	@Override
	public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
		// Logic to return a specific EntitySet based on name
		return getEntitySets().get(entitySetName);
	}

	@Override
	public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
		// Logic to return a specific EntityType based on name
		return getEntityTypes().get(entityTypeName.getName());
	}

	@Override
	public CsdlEntityContainer getEntityContainer() throws ODataException {
		CsdlEntityContainer container = new CsdlEntityContainer();
		container.setName("Container");
		container.setEntitySets(new ArrayList<>(getEntitySets().values()));
		return container;
	}

	@Override
	public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
		if (entityContainerName == null || "Container".equals(entityContainerName.getName())) {
			return new CsdlEntityContainerInfo().setContainerName(new FullQualifiedName("OData.framework", "Container"));
		}
		return null;
	}

	public void registerTable(TableSchemaDTO tableSchema) {
		dynamicTables.put(tableSchema.getTableName(), tableSchema);
	}

	private FullQualifiedName convertJavaTypeToEdmType(String javaType) {
		switch (javaType) {
			case "Integer":
				return EdmPrimitiveTypeKind.Int32.getFullQualifiedName();
			case "String":
				return EdmPrimitiveTypeKind.String.getFullQualifiedName();
			default:
				throw new IllegalArgumentException("Unsupported type: " + javaType);
		}
	}
}
