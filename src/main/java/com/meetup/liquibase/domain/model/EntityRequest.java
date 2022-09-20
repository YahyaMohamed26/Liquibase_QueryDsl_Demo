package com.meetup.liquibase.domain.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Getter
public class EntityRequest {
    public List<String> fields = new ArrayList<>();
    private LinkedHashMap<String, LinkedHashMap<String, Object>> predicateSet = new LinkedHashMap<>();
    private LinkedHashMap<String, Object> saveEntityRequest = new LinkedHashMap<>();
}
