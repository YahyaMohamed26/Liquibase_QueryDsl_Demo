package com.meetup.dynamicorm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetup.dynamicorm.model.EntityRequest;
import com.querydsl.core.Tuple;
import com.querydsl.core.dml.StoreClause;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathMetadataFactory;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.H2Templates;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryDslService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<LinkedHashMap<String, Object>> searchEntity(String tableName, EntityRequest entityRequest) throws Exception {
        SpringConnectionProvider connectionProvider = new SpringConnectionProvider(dataSource);
        com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(new PostgreSQLTemplates());
        configuration.setExceptionTranslator(new SpringExceptionTranslator());

        SQLQueryFactory queryFactory = new SQLQueryFactory(configuration, connectionProvider);

        String sqlTableVariableName = "_" + tableName;
        RelationalPath<Object> relationalPath = new RelationalPathBase<Object>(Object.class, sqlTableVariableName, "public", tableName);

        LinkedCaseInsensitiveMap<Expression> expressions = new LinkedCaseInsensitiveMap<>();

        PathMetadata metadata = PathMetadataFactory.forVariable(sqlTableVariableName);
        PathBuilder pathBuilder = new PathBuilder<>(Object.class, metadata);

        for (String fieldName : entityRequest.getFields()) {
            if (fieldName.equals("id")) {
                expressions.put(fieldName, pathBuilder.getNumber(fieldName, Long.class));

            } else {
                expressions.put(fieldName, pathBuilder.getString(fieldName));
            }
        }

        Set<Predicate> predicateSet = new HashSet<>();


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
            for (Method m : expression.getClass().getMethods()) {
                Class<?>[] parameterTypes = m.getParameterTypes();
                if (m.getName().equals(operation) && !Expression.class.isAssignableFrom(parameterTypes[0])) {
                    try {
                        predicateSet.add((Predicate) m.invoke(expression, value));
                        break;
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }

        }

        SQLQuery<Tuple> sqlQuery = queryFactory
                .select(expressions.values().toArray(new Expression[0]))
                .from(relationalPath)
                .where(predicateSet.toArray(new Predicate[0]));

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

    @Transactional
    public Object saveEntityToTable(String tableName, LinkedHashMap<String, Object> entitySaveRequest) {

        SpringConnectionProvider connectionProvider = new SpringConnectionProvider(dataSource);
        com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(new PostgreSQLTemplates());
        configuration.setExceptionTranslator(new SpringExceptionTranslator());

        PathMetadata metadata = PathMetadataFactory.forVariable(tableName);
        PathBuilder pathBuilder = new PathBuilder<>(Object.class, metadata);
        SQLQueryFactory sqlQueryFactory = new SQLQueryFactory(configuration, connectionProvider);

        Long id = sqlQueryFactory.select(SQLExpressions.nextval(tableName + "_id_seq")).fetchOne();
        RelationalPath<Object> relationalPath = new RelationalPathBase<Object>(Object.class, tableName, "public", tableName);
        StoreClause<?> storeSqlClause;

        if (Objects.isNull(entitySaveRequest.get("id"))) {
            entitySaveRequest.put("id", id);
            storeSqlClause = sqlQueryFactory.insert(relationalPath);
        } else {
            storeSqlClause = sqlQueryFactory.update(relationalPath).where(pathBuilder.getNumber("id", Long.class).eq(entitySaveRequest.get("id")));
        }


        for (Object fieldName : entitySaveRequest.keySet()) {
            if (fieldName.equals("id")) {
                storeSqlClause.set(pathBuilder.getNumber("id", Long.class), entitySaveRequest.get(fieldName));
            } else  {
                storeSqlClause.set((Path)pathBuilder.getString(fieldName.toString()), entitySaveRequest.get(fieldName));
            }
        }
        storeSqlClause.execute();

        return entitySaveRequest;
    }
}
