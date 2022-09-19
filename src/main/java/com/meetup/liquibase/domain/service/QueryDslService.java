package com.meetup.liquibase.domain.service;

import com.meetup.liquibase.domain.model.Entity;
import com.meetup.liquibase.domain.model.EntityRequest;
import com.meetup.liquibase.domain.model.QEntity;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryDslService {

    @PersistenceContext
    private EntityManager entityManager;

    public static final QEntity entity = new QEntity("entity");


    public String searchEntity(String tableName, EntityRequest entityRequest) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QEntity entity = QEntity.entity;
        Entity e = queryFactory.selectFrom(entity).where(entity.name.eq("ertugrul")).fetchOne();
        return e.getName();
    }

    public String saveEntityToTable(String tableName, EntityRequest entityRequest) {

        return null;
    }

}
