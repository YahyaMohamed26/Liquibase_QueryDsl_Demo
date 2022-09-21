package com.meetup.dynamicorm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetup.dynamicorm.model.SaveEntityRequest;
import com.meetup.dynamicorm.model.SearchEntityRequest;
import com.meetup.liquibase.domain.model.EntityRequest;
import com.querydsl.core.Tuple;
import com.querydsl.core.dml.StoreClause;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathMetadataFactory;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.spring.SpringConnectionProvider;
import com.querydsl.sql.spring.SpringExceptionTranslator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedCaseInsensitiveMap;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryDslService {

	private final DataSource dataSource;
	private final NameGeneratorService nameGeneratorService;

	@Transactional(readOnly = true)
	public List<LinkedHashMap<String, Object>> searchEntity(String tableName, SearchEntityRequest entityRequest) throws Exception {
		SpringConnectionProvider connectionProvider = new SpringConnectionProvider(dataSource);
		com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(new PostgreSQLTemplates());
		configuration.setExceptionTranslator(new SpringExceptionTranslator());

		SQLQueryFactory queryFactory = new SQLQueryFactory(configuration, connectionProvider);

		RelationalPath<Object> relationalPath = new RelationalPathBase<Object>(Object.class, nameGeneratorService.getTableAliasName(tableName), "public", tableName);

		LinkedCaseInsensitiveMap<Expression> expressions = new LinkedCaseInsensitiveMap<>();

		PathMetadata metadata = PathMetadataFactory.forVariable(nameGeneratorService.getTableAliasName(tableName));
		PathBuilder pathBuilder = new PathBuilder<>(Object.class, metadata);

		for (String fieldName : entityRequest.getFields()) {
			if (fieldName.equals("id")) {
				expressions.put(fieldName, pathBuilder.getNumber(fieldName, Long.class));

			} else {
				expressions.put(fieldName, pathBuilder.getString(fieldName));
			}
		}

		List<Predicate> predicateList = new ArrayList<>();


		/**
		 *     "predicateSet": [
		 *         {
		 *             "ge": {
		 *                 "field": "id",
		 *                 "value": 1
		 *             }
		 *         },
		 *         {
		 *             "lt": {
		 *                 "field": "id",
		 *                 "value": 5
		 *             }
		 *         }
		 *     ]
		 */
		for (LinkedHashMap<String, Object> entityRequestPredicate : entityRequest.getPredicateSet()) {
			String operation = entityRequestPredicate.keySet().stream().findFirst().get();
			LinkedHashMap<String, Object> predicateMap = (LinkedHashMap) entityRequestPredicate.get(operation);
			String fieldName = (String) predicateMap.get("field");
			Object value = predicateMap.get("value");

			Expression expression = expressions.get(fieldName);
            Method operationMethod = null;
			for (Method m : expression.getClass().getMethods()) {
				Class<?>[] parameterTypes = m.getParameterTypes();
				if (m.getName().equals(operation) && !Expression.class.isAssignableFrom(parameterTypes[0])) {
                    operationMethod = m;
				}
			}

            if (operationMethod == null) {
                throw new UnsupportedOperationException("unsupported operation : " + operation);
            }

            try {
                predicateList.add((Predicate) operationMethod.invoke(expression, value));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

		}

		SQLQuery<Tuple> sqlQuery = queryFactory
				.select(expressions.values().toArray(new Expression[0]))
				.from(relationalPath)
				.where(predicateList.toArray(new Predicate[0]));

        log.info(sqlQuery.toString());

		ArrayList<LinkedHashMap<String, Object>> result = new ArrayList<>();

		for (Tuple t : sqlQuery.fetch()) {
			LinkedHashMap<String, Object> entityMap = new LinkedHashMap<>();
			for (Map.Entry<String, Expression> expressionEntry : expressions.entrySet()) {
				String fieldName = expressionEntry.getKey();
				entityMap.put(fieldName, t.get(expressionEntry.getValue()));
			}
			result.add(entityMap);
		}

		return result;
	}

	/**
	 *    INSERT INTO book(id, title)
	 *    VALUES(1, 'ABC');
	 *
	 *
	 *    UPDATE book
	 *    SET title = 'ABC'
	 *    WHERE id = 10;
	 *
	 * @param tableName
	 * @param saveEntityRequest
	 * @return
	 */
	@Transactional
	public SaveEntityRequest saveEntityToTable(String tableName, SaveEntityRequest saveEntityRequest) {

		SpringConnectionProvider connectionProvider = new SpringConnectionProvider(dataSource);
		com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(new PostgreSQLTemplates());
		configuration.setExceptionTranslator(new SpringExceptionTranslator());

		PathMetadata metadata = PathMetadataFactory.forVariable(nameGeneratorService.getTableAliasName(tableName));
		PathBuilder pathBuilder = new PathBuilder<>(Object.class, metadata);
		SQLQueryFactory sqlQueryFactory = new SQLQueryFactory(configuration, connectionProvider);

		RelationalPath<Object> relationalPath = new RelationalPathBase<Object>(Object.class, nameGeneratorService.getTableAliasName(tableName), "public", tableName);
		StoreClause<?> storeSqlClause;

		if (Objects.isNull(saveEntityRequest.get("id"))) {
			Long idNextSequenceValue = sqlQueryFactory.select(SQLExpressions.nextval(nameGeneratorService.getSequenceName(tableName))).fetchOne();

			saveEntityRequest.put("id", idNextSequenceValue);
			storeSqlClause = sqlQueryFactory.insert(relationalPath);
		} else {
			storeSqlClause = sqlQueryFactory.update(relationalPath)
					.where(pathBuilder.getNumber("id", Long.class).eq(saveEntityRequest.get("id")));
		}


		for (Object fieldName : saveEntityRequest.keySet()) {
			if (fieldName.equals("id")) {
				storeSqlClause.set(pathBuilder.getNumber("id", Long.class), saveEntityRequest.get(fieldName));
			} else {
				storeSqlClause.set((Path) pathBuilder.getString(fieldName.toString()), saveEntityRequest.get(fieldName));
			}
		}

		log.info(storeSqlClause.toString());

		storeSqlClause.execute();

		return saveEntityRequest;
	}

}
