package com.meetup.liquibase.domain.model;

import com.querydsl.core.types.Predicate;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class EntityRequest {
    private String tableName;
    private LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
    private LinkedHashMap<String, LinkedHashMap<String, Object>> predicateSet = new LinkedHashMap<>();
    private LinkedHashMap<String, Object> saveEntityRequest = new LinkedHashMap<>();
}
