package com.meetup.liquibase.domain.model;

import lombok.Data;

import java.util.LinkedHashMap;

@Data
public class QueryDsl extends LinkedHashMap<String, Object> {

    public static final String BOOL = "bool";
    public static final String MUST = "must";
    public static final String SHOULD = "should";
    public static final String MUST_NOT = "must_not";
    public static final String EXISTS = "exists";
    public static final String NOT_EXISTS = "not_exists";
}
