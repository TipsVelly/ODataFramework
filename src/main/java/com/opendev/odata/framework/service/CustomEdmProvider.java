package com.opendev.odata.framework.service;

import com.opendev.odata.domain.query.entity.TdxQuery;
import com.opendev.odata.domain.query.repository.QueryParamRepository;
import com.opendev.odata.domain.query.repository.QueryRepository;
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
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomEdmProvider extends CsdlAbstractEdmProvider {

	private final DatabaseMetadataService databaseMetadataService;
	private final EdmProviderMapper edmProviderMapper;
	private final CustomJpaRepository  customJpaRepository;
	private final QueryRepository queryRepository;
	private final QueryParamRepository queryParamRepository;

	private static final FullQualifiedName ACTION_RESET = new FullQualifiedName("OData.framework", "ResetDemo");
	private static final FullQualifiedName FUNCTION_CALCULATE_VAT = new FullQualifiedName("OData.framework", "CalculateVAT");


	@Override
	public List<CsdlSchema> getSchemas() throws ODataException {
		List<CsdlSchema> schemas = new ArrayList<>();
		CsdlSchema schema = new CsdlSchema();
		schema.setNamespace("OData.framework");

		// 데이터베이스에서 테이블 스키마 정보 조회
		List<TableSchemaDTO> tableSchemas = databaseMetadataService.getTableSchemas();
		List<CsdlEntityType> entityTypes = new ArrayList<>();

		// 각 TableSchemaDTO에 대해서 CsdlEntityType 생성
		for (TableSchemaDTO tableSchema : tableSchemas) {
			entityTypes.add(createCsdlEntityType(tableSchema));
		}
		List<CsdlAction> actions = loadDynamicActions();
		List<CsdlFunction> functions = loadDynamicFunctions();

		schema.setEntityTypes(entityTypes);

		schema.setActions(actions);
		schema.setFunctions(functions);



		schema.setEntityContainer(createEntityContainer(tableSchemas));

		schemas.add(schema);
		return schemas;
	}

	private Map<String, CsdlEntityType> getEntityTypes() {
		Map<String, CsdlEntityType> entityTypes = new HashMap<>();
		// 데이터베이스에서 테이블 스키마 정보 조회
		List<TableSchemaDTO> tableSchemas = databaseMetadataService.getTableSchemas();
		for (TableSchemaDTO tableSchema : tableSchemas) {
			entityTypes.put(tableSchema.getTableName(), createCsdlEntityType(tableSchema));
		}
		return entityTypes;
	}


	private CsdlEntityType createCsdlEntityType(TableSchemaDTO tableSchema) {
		List<CsdlProperty> csdlProperties = new ArrayList<>();
		CsdlPropertyRef propertyRef = new CsdlPropertyRef();
		propertyRef.setName("ID"); // 'ID'를 기본 키로 가정합니다.

		// ID 속성을 생성하고, Nullable=false로 설정하여 null 값을 허용하지 않도록 합니다.
		CsdlProperty idProperty = new CsdlProperty()
				.setName("ID")
				.setType(EdmPrimitiveTypeKind.Int16.getFullQualifiedName()) // 유형을 Edm.Int32로 설정합니다.
				.setNullable(false); // Nullable=false로 설정합니다.
		csdlProperties.add(idProperty);

		// 기타 모든 컬럼을 속성으로 추가합니다.
		tableSchema.getColumns().stream()
				.filter(column -> !column.getColumnName().equals("ID")) // 'ID' 컬럼은 이미 추가되었으므로 제외합니다.
				.forEach(column -> csdlProperties.add(new CsdlProperty()
						.setName(column.getColumnName())
						.setType(convertPostgresTypeToEdmType(column.getColumnType()))
						.setNullable(true))); // 기타 속성에 대한 Nullable 설정은 필요에 따라 조정할 수 있습니다.

		// CsdlEntityType 인스턴스를 생성하고 설정된 속성과 키를 할당합니다.
		return new CsdlEntityType()
				.setName(tableSchema.getTableName())
				.setProperties(csdlProperties)
				.setKey(Collections.singletonList(propertyRef));
	}

	private Map<String, CsdlEntitySet> getEntitySets() {
		Map<String, CsdlEntitySet> entitySets = new HashMap<>();
		// 데이터베이스에서 테이블 스키마 정보 조회
		List<TableSchemaDTO> tableSchemas = databaseMetadataService.getTableSchemas();
		for (TableSchemaDTO tableSchema : tableSchemas) {
			String entitySetName = tableSchema.getTableName() + "s";
			entitySets.put(entitySetName, createCsdlEntitySet(entitySetName, new FullQualifiedName("OData.framework", tableSchema.getTableName())));
		}
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

		// 엔티티 세트 추가
		container.setEntitySets(new ArrayList<>(getEntitySets().values()));

		// 동적 액션 및 펑션 로드
		List<CsdlAction> actions = loadDynamicActions();
		List<CsdlFunction> functions = loadDynamicFunctions();

		// 동적 액션 임포트 추가
		List<CsdlActionImport> actionImports = actions.stream()
				.map(action -> new CsdlActionImport()
						.setName(action.getName())
						.setAction(new FullQualifiedName("OData.framework", action.getName())))
				.collect(Collectors.toList());
		container.setActionImports(actionImports);

		// 동적 펑션 임포트 추가
		List<CsdlFunctionImport> functionImports = functions.stream()
				.map(function -> new CsdlFunctionImport()
						.setName(function.getName())
						.setFunction(new FullQualifiedName("OData.framework", function.getName()))
						.setIncludeInServiceDocument(true))
				.collect(Collectors.toList());
		container.setFunctionImports(functionImports);

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
		if (postgresType == null || postgresType.trim().isEmpty()) {
			// 입력된 타입이 없거나 공백만 있는 경우 기본 타입으로 처리
			return EdmPrimitiveTypeKind.String.getFullQualifiedName();
		}

		// Normalize the input and extract the base type
		postgresType = postgresType.toLowerCase().trim();
		String[] parts = postgresType.split("\\s+"); // Split on whitespace
		if (parts.length > 0) {
			postgresType = parts[0]; // Consider only the first part as the type
		}

		// Extract the base type without precision/scale for types like "decimal(10, 2)"
		int parenIndex = postgresType.indexOf('(');
		if (parenIndex != -1) {
			postgresType = postgresType.substring(0, parenIndex);
		}

		if (postgresType.startsWith("varchar") || postgresType.startsWith("char") || postgresType.equals("text")) {
			return EdmPrimitiveTypeKind.String.getFullQualifiedName();
		}

		switch (postgresType) {
			case "serial":
			case "smallserial":
			case "integer":
			case "int":
			case "smallint":
				return EdmPrimitiveTypeKind.Int32.getFullQualifiedName();
			case "bigint":
			case "bigserial":
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
			case "date":
				return EdmPrimitiveTypeKind.Date.getFullQualifiedName();
			case "time":
				return EdmPrimitiveTypeKind.TimeOfDay.getFullQualifiedName();
			case "timestamp":
			case "timestamp with time zone":
				return EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName();
			case "bytea":
				return EdmPrimitiveTypeKind.Binary.getFullQualifiedName();
			case "uuid":
				return EdmPrimitiveTypeKind.Guid.getFullQualifiedName();
			case "interval":
				return EdmPrimitiveTypeKind.Duration.getFullQualifiedName();
			case "json":
			case "jsonb":
				return EdmPrimitiveTypeKind.String.getFullQualifiedName(); // JSON is treated as string
			case "xml":
				return EdmPrimitiveTypeKind.String.getFullQualifiedName(); // XML is also treated as string
			case "point":
			case "line":
			case "lseg":
			case "box":
			case "path":
			case "polygon":
			case "circle":
				return EdmPrimitiveTypeKind.Geometry.getFullQualifiedName(); // Geometric types as Geometry
			case "cidr":
			case "inet":
			case "macaddr":
			case "macaddr8":
				return EdmPrimitiveTypeKind.String.getFullQualifiedName(); // Network addresses as string
			case "bit":
			case "bit varying":
				return EdmPrimitiveTypeKind.Binary.getFullQualifiedName(); // Bit strings as binary
			case "tsvector":
			case "tsquery":
				return EdmPrimitiveTypeKind.String.getFullQualifiedName(); // Text search types as string
			default:
				throw new IllegalArgumentException("Unsupported PostgreSQL type: " + postgresType);
		}
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

		// 동적으로 생성된 액션과 펑션을 사용하여 액션 임포트와 펑션 임포트 리스트를 생성
		List<CsdlActionImport> actionImports = loadDynamicActions().stream()
				.map(action -> new CsdlActionImport()
						.setName(action.getName())
						.setAction(new FullQualifiedName("OData.framework", action.getName())))
				.collect(Collectors.toList());
		List<CsdlFunctionImport> functionImports = loadDynamicFunctions().stream()
				.map(function -> new CsdlFunctionImport()
						.setName(function.getName())
						.setFunction(new FullQualifiedName("OData.framework", function.getName()))
						.setIncludeInServiceDocument(true))
				.collect(Collectors.toList());

		// 설정된 액션 임포트와 펑션 임포트를 컨테이너에 추가
		container.setActionImports(actionImports);
		container.setFunctionImports(functionImports);

		return container;
	}

	private CsdlAction defineResetAction() {
		return new CsdlAction().setName("ResetDemo").setBound(false).setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName()));
	}


	private CsdlActionImport defineResetActionImport() {
		return new CsdlActionImport()
				.setName("ResetDemo")
				.setAction(ACTION_RESET);
	}
	@Override
	public List<CsdlAction> getActions(FullQualifiedName actionName) {
		// 데이터베이스에서 로드된 액션을 반환
		return queryRepository.findAll().stream()
				.filter(query -> new FullQualifiedName("OData.framework", query.getOdataQueryName()).equals(actionName))
				.map(this::convertToCsdlAction)
				.collect(Collectors.toList());
	}

	@Override
	public List<CsdlFunction> getFunctions(FullQualifiedName functionName) {
		// 데이터베이스에서 로드된 펑션을 반환
		return queryRepository.findAll().stream()
				.filter(query -> new FullQualifiedName("OData.framework", query.getOdataQueryName()).equals(functionName))
				.map(this::convertToCsdlFunction)
				.collect(Collectors.toList());
	}

	@Override
	public CsdlActionImport getActionImport(FullQualifiedName entityContainer, String actionImportName) {
		// 동적으로 생성된 액션 임포트를 반환
		Optional<CsdlAction> action = loadDynamicActions().stream()
				.filter(a -> a.getName().equals(actionImportName))
				.findFirst();
		return action.map(a -> new CsdlActionImport()
						.setName(actionImportName)
						.setAction(new FullQualifiedName("OData.framework", actionImportName)))
				.orElse(null);
	}

	@Override
	public CsdlFunctionImport getFunctionImport(FullQualifiedName entityContainer, String functionImportName) {
		// 동적으로 생성된 펑션 임포트를 반환
		Optional<CsdlFunction> function = loadDynamicFunctions().stream()
				.filter(f -> f.getName().equals(functionImportName))
				.findFirst();
		return function.map(f -> new CsdlFunctionImport()
						.setName(functionImportName)
						.setFunction(new FullQualifiedName("OData.framework", functionImportName))
						.setIncludeInServiceDocument(true))
				.orElse(null);
	}


	public CsdlFunction defineCalculateVATFunction() {
		CsdlParameter netPrice = new CsdlParameter().setName("NetPrice").setType(EdmPrimitiveTypeKind.Decimal.getFullQualifiedName()).setNullable(false);
		CsdlParameter country = new CsdlParameter().setName("Country").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setNullable(false);

		CsdlReturnType returnType = new CsdlReturnType().setType(EdmPrimitiveTypeKind.Decimal.getFullQualifiedName());

		return new CsdlFunction()
				.setName("CalculateVAT")
				.setParameters(Arrays.asList(netPrice, country))
				.setReturnType(returnType);
	}

	private List<CsdlAction> loadDynamicActions() {
		return queryRepository.findAll().stream()
				.filter(query -> query.getHttpRequest().equals("POST")) // Assuming POST for Actions
				.map(this::convertToCsdlAction)
				.collect(Collectors.toList());
	}

	private CsdlAction convertToCsdlAction(TdxQuery query) {
		return new CsdlAction()
				.setName(query.getOdataQueryName())
				.setBound(false)
				.setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));
	}

	private List<CsdlFunction> loadDynamicFunctions() {
		return queryRepository.findAll().stream()
				.filter(query -> "GET".equals(query.getHttpRequest()))
				.map(this::convertToCsdlFunction)
				.collect(Collectors.toList());
	}

	private CsdlFunction convertToCsdlFunction(TdxQuery query) {
		List<CsdlParameter> parameters = queryParamRepository.findByTdxQuery(query).stream()
				.map(param -> new CsdlParameter().setName(param.getParameter()).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()))
				.collect(Collectors.toList());

		return new CsdlFunction()
				.setName(query.getOdataQueryName())
				.setParameters(parameters)
				.setReturnType(new CsdlReturnType().setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));
	}

}
