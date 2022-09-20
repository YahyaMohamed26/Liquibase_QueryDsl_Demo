package com.meetup.liquibase.domain.service;

import com.meetup.liquibase.domain.model.EntityRequest;
import com.querydsl.core.Tuple;
import com.querydsl.core.dml.StoreClause;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathMetadataFactory;
import com.querydsl.core.types.Predicate;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.SimpleExpression;

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
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.object.SqlQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedCaseInsensitiveMap;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryDslService {

    @PersistenceContext
    private EntityManager entityManager;
    private final DataSource dataSource;
    // private final SQLQueryFactory sqlQueryFactory;

    @Transactional
    public String searchEntity(String tableName, EntityRequest entityRequest) {
        Connection connection = DataSourceUtils.getConnection(dataSource);

        SpringConnectionProvider connectionProvider = new SpringConnectionProvider(dataSource);
        com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(new PostgreSQLTemplates());
        configuration.setExceptionTranslator(new SpringExceptionTranslator());

        SQLQueryFactory queryFactory = new SQLQueryFactory(configuration, connectionProvider);

        RelationalPath<Object> relationalPath = new RelationalPathBase<Object>(Object.class, "person", "person", "person");

        LinkedCaseInsensitiveMap<Expression> expressions = new LinkedCaseInsensitiveMap<>();


        PathMetadata metadata = PathMetadataFactory.forVariable("person");
        PathBuilder pathBuilder = new PathBuilder<>(Object.class, metadata);

        for (Object s : entityRequest.getFields().values()) {
            if (s.equals("id")) {
                expressions.put(s.toString(), pathBuilder.getNumber("id", Long.class));

            } else if (s.equals("lastname") || s.equals("firstname") || s.equals("state")) {
                expressions.put(s.toString(), pathBuilder.getString(s.toString()));

            }
        }

        Set<Predicate> predicateSet = new HashSet<>();

        for (String s : expressions.keySet()) {
            if (entityRequest.getPredicateSet().get("eq").get("field").equals(s)) {
                for (Method m : expressions.get(s).getClass().getMethods()) {
                    if (m.getName().equals(Arrays.stream(entityRequest.getPredicateSet().keySet().toArray()).findFirst().get())) {
                        try {
                            predicateSet.add((Predicate) m.invoke(expressions.get(s), entityRequest.getPredicateSet().get("eq").get("value")));
                            break;
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                }
            }
        }

        //SQLQuery sqlQuery = new SQLQuery(connection, PostgreSQLTemplates.DEFAULT);
        SQLQuery<Tuple> sqlQuery = queryFactory
                .select(expressions.values().toArray(new Expression[0]))
                .from(relationalPath)
                .where(predicateSet.toArray(new Predicate[0]));
        sqlQuery.fetchOne();
        return "";
    }

    public String saveEntityToTable(String tableName, EntityRequest entityRequest) {

        Connection connection = DataSourceUtils.getConnection(dataSource);

        SpringConnectionProvider connectionProvider = new SpringConnectionProvider(dataSource);
        com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(new PostgreSQLTemplates());
        configuration.setExceptionTranslator(new SpringExceptionTranslator());

        SQLQueryFactory sqlQueryFactory = new SQLQueryFactory(configuration, connectionProvider);

        Long id = sqlQueryFactory.select(SQLExpressions.nextval(tableName + "_id_seq")).fetchOne();
        RelationalPath<Object> relationalPath = new RelationalPathBase<Object>(Object.class, "person", "person", "person");
        StoreClause<?> storeSqlClause;

        storeSqlClause = sqlQueryFactory.insert(relationalPath);

        LinkedHashMap<String, Object> saveEntityRequest = entityRequest.getFields();
        saveEntityRequest.put("id", id);

        PathMetadata metadata = PathMetadataFactory.forVariable("person");
        PathBuilder pathBuilder = new PathBuilder<>(Object.class, metadata);

        for (Object fieldName : entityRequest.getFields().keySet()) {
            if (fieldName.equals("id")) {
                storeSqlClause.set(pathBuilder.getNumber("id", Long.class), saveEntityRequest.get(fieldName));
            } else  {
                storeSqlClause.set(pathBuilder.getString(fieldName.toString()), (Expression<String>) saveEntityRequest.get(fieldName));
            }
        }

        storeSqlClause.execute();

        return searchEntity(tableName, entityRequest);
    }
}