package com.meetup.liquibase.domain.model;

import com.querydsl.core.types.Predicate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EntityRequest {
    String tableName;
    QueryDsl queryDsl = new QueryDsl();
    public List<String> fields = new ArrayList<>();
    public Set<Predicate> predicateSet = new LinkedHashSet<>();

}
