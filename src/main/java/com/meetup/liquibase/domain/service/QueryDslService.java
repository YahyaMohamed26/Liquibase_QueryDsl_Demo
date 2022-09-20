package com.meetup.liquibase.domain.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QueryDslService {

    private final DataSource dataSource;

    @Transactional
    public ArrayList<String> searchEntity(String tableName, EntityRequest entityRequest) {
        SpringConnectionProvider connectionProvider = new SpringConnectionProvider(dataSource);
        com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(new PostgreSQLTemplates());
        configuration.setExceptionTranslator(new SpringExceptionTranslator());

        SQLQueryFactory queryFactory = new SQLQueryFactory(configuration, connectionProvider);

        RelationalPath<Object> relationalPath = new RelationalPathBase<Object>(Object.class, tableName, "public", tableName);

        LinkedCaseInsensitiveMap<Expression> expressions = new LinkedCaseInsensitiveMap<>();

        PathMetadata metadata = PathMetadataFactory.forVariable(tableName);
        PathBuilder pathBuilder = new PathBuilder<>(Object.class, metadata);

        for (String fieldName : entityRequest.getFields()) {
            if (fieldName.equals("id")) {
                expressions.put(fieldName, pathBuilder.getNumber(fieldName, Long.class));

            } else {
                expressions.put(fieldName, pathBuilder.getString(fieldName));

            }
        }

        Set<Predicate> predicateSet = new HashSet<>();

        String operation = Arrays.stream(entityRequest.getPredicateSet().keySet().stream().toArray()).findFirst().get().toString();

        for (String s : expressions.keySet()) {
            if (entityRequest.getPredicateSet().get(operation).get("field").equals(s)) {
                for (Method m : expressions.get(s).getClass().getMethods()) {
                    Class<?>[] parameterTypes = m.getParameterTypes();
                    if (m.getName().equals(Arrays.stream(entityRequest.getPredicateSet().keySet().toArray()).findFirst().get()) && !Expression.class.isAssignableFrom(parameterTypes[0])) {
                        try {
                            predicateSet.add((Predicate) m.invoke(expressions.get(s), entityRequest.getPredicateSet().get(operation).get("value")));
                            break;
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                }
            }
        }

        SQLQuery<Tuple> sqlQuery = queryFactory
                .select(expressions.values().toArray(new Expression[0]))
                .from(relationalPath)
                .where(predicateSet.toArray(new Predicate[0]));

        ArrayList<String> result = new ArrayList<>();

        for (Tuple t : sqlQuery.fetch()) {
            result.add(t.toString());
        }

        System.out.println(sqlQuery);

        return result;
    }

    @Transactional
    public Object saveEntityToTable(String tableName, EntityRequest entityRequest) {

        SpringConnectionProvider connectionProvider = new SpringConnectionProvider(dataSource);
        com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(new PostgreSQLTemplates());
        configuration.setExceptionTranslator(new SpringExceptionTranslator());

        SQLQueryFactory sqlQueryFactory = new SQLQueryFactory(configuration, connectionProvider);

        Long id = sqlQueryFactory.select(SQLExpressions.nextval(tableName + "_id_seq")).fetchOne();
        RelationalPath<Object> relationalPath = new RelationalPathBase<Object>(Object.class, tableName, "public", tableName);
        StoreClause<?> storeSqlClause = sqlQueryFactory.insert(relationalPath);

        LinkedHashMap<String, Object> saveEntityRequest = entityRequest.getSaveEntityRequest();
        saveEntityRequest.put("id", id);

        PathMetadata metadata = PathMetadataFactory.forVariable(tableName);
        PathBuilder pathBuilder = new PathBuilder<>(Object.class, metadata);

        for (Object fieldName : saveEntityRequest.keySet()) {
            if (fieldName.equals("id")) {
                storeSqlClause.set(pathBuilder.getNumber("id", Long.class), saveEntityRequest.get(fieldName));
            } else  {
                storeSqlClause.set((Path)pathBuilder.getString(fieldName.toString()), saveEntityRequest.get(fieldName));
            }
        }
        storeSqlClause.execute();

        return saveEntityRequest;
    }
}