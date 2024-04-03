package com.opendev.odata.framework.service;

import com.opendev.odata.domain.table.dto.ColumnDTO;
import com.opendev.odata.domain.table.dto.TableSchemaDTO;
import com.opendev.odata.framework.mapper.CustomJpaRepository;
import com.opendev.odata.framework.mapper.EdmProviderMapper;
import com.opendev.odata.global.dynamic.service.DatabaseMetadataService;
import lombok.RequiredArgsConstructor;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.*;
import org.apache.olingo.commons.api.ex.ODataException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CustomEdmProvider extends CsdlAbstractEdmProvider {

	private final DatabaseMetadataService databaseMetadataService;
	private final EdmProviderMapper edmProviderMapper;
	private final CustomJpaRepository  customJpaRepository;

	private Map<String, TableSchemaDTO> dynamicTables = new HashMap<>();

	@Override
	public List<CsdlSchema> getSchemas() throws ODataException {
		List<CsdlSchema> schemas = new ArrayList<>();
		CsdlSchema schema = new CsdlSchema();
		schema.setNamespace("OData.framework");

		// 엔티티 타입 설정 set
		List<TableSchemaDTO> tableSchemas = databaseMetadataService.getTableSchemas();
		schema.setEntityTypes(createEntityTypes(tableSchemas));

		//엔티티 컨테이너 set
		CsdlEntityContainer container = createEntityContainer(tableSchemas);
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
				.setType(convertPostgresTypeToEdmType(column.getColumnType()))));

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

	@Transactional
	public void registerTable(TableSchemaDTO tableSchema) {
		// tdx_table에 테이블 정보 저장
		edmProviderMapper.insertTable(tableSchema.getTableName(), "Table description"); // 간단한 설명 추가

		// tdx_table에서 방금 삽입한 테이블의 ID 조회 (예제를 단순화하기 위해 생략)
		Map<String, Object>  map = customJpaRepository.findByName( "tdx_table", tableSchema.getTableName());
		Long id = (long) (Integer) map.get("id");
		// 각 컬럼 정보를 tdx_column에 저장
		tableSchema.getColumns().forEach(column -> {
			edmProviderMapper.insertColumn(id, column.getColumnName(), column.getColumnType());
		});

	}


	private FullQualifiedName convertPostgresTypeToEdmType(String postgresType) {
		postgresType = postgresType.toLowerCase();

		// Extract the base type without precision/scale for types like "decimal(10, 2)".
		int parenIndex = postgresType.indexOf('(');
		if (parenIndex != -1) {
			postgresType = postgresType.substring(0, parenIndex);
		}


		if (postgresType.startsWith("varchar") || postgresType.startsWith("char") || postgresType.equals("text")) {
			return EdmPrimitiveTypeKind.String.getFullQualifiedName();
		}

		switch (postgresType) {
			case "serial":
			case "integer":
			case "int":
			case "smallint":
				return EdmPrimitiveTypeKind.Int16.getFullQualifiedName();
			case "bigint":
				return EdmPrimitiveTypeKind.Int64.getFullQualifiedName();
			case "boolean":
				return EdmPrimitiveTypeKind.Boolean.getFullQualifiedName();
			case "numeric":
			case "decimal":
				return EdmPrimitiveTypeKind.Decimal.getFullQualifiedName();
			case "real":
				return EdmPrimitiveTypeKind.Single.getFullQualifiedName();
			case "double precision":
				return EdmPrimitiveTypeKind.Double.getFullQualifiedName();
			case "char":
			case "varchar":
			case "text":
				return EdmPrimitiveTypeKind.String.getFullQualifiedName();
			case "date":
				return EdmPrimitiveTypeKind.Date.getFullQualifiedName();
			case "time":
				return EdmPrimitiveTypeKind.TimeOfDay.getFullQualifiedName();
			case "timestamp":
			case "timestamp without time zone":
				return EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName();
			case "bytea":
				return EdmPrimitiveTypeKind.Binary.getFullQualifiedName();
			case "uuid":
				return EdmPrimitiveTypeKind.Guid.getFullQualifiedName();
			// PostgreSQL의 배열 유형, JSON 유형 등과 같은 더 많은 유형에 대한 매핑이 필요할 수 있습니다.
			// 해당 유형에 맞는 EDM 유형으로 매핑을 추가하세요.
			default:
				throw new IllegalArgumentException("Unsupported PostgreSQL type: " + postgresType);
		}
	}

	private List<CsdlEntityType> createEntityTypes(List<TableSchemaDTO> tableSchemas) {
		List<CsdlEntityType> entityTypes = new ArrayList<>();
		for (TableSchemaDTO tableSchema : tableSchemas) {
			List<CsdlProperty> csdlProperties = new ArrayList<>();
			for (ColumnDTO column : tableSchema.getColumns()) {
				CsdlProperty property = new CsdlProperty()
						.setName(column.getColumnName())
						.setType(convertPostgresTypeToEdmType(column.getColumnType()));
				csdlProperties.add(property);
			}

			CsdlEntityType entityType = new CsdlEntityType()
					.setName(tableSchema.getTableName())
					.setProperties(csdlProperties)
					.setKey(Arrays.asList(new CsdlPropertyRef().setName("ID"))); // ID를 기본 키로 가정
			entityTypes.add(entityType);
		}
		return entityTypes;
	}

	private CsdlEntityContainer createEntityContainer(List<TableSchemaDTO> tableSchemas) {
		CsdlEntityContainer container = new CsdlEntityContainer().setName("Container");
		List<CsdlEntitySet> entitySets = new ArrayList<>();
		for (TableSchemaDTO tableSchema : tableSchemas) {
			CsdlEntitySet entitySet = new CsdlEntitySet()
					.setName(tableSchema.getTableName() + "s") // 복수형을 가정
					.setType(new FullQualifiedName("OData.framework", tableSchema.getTableName()));
			entitySets.add(entitySet);
		}
		container.setEntitySets(entitySets);
		return container;
	}
}
