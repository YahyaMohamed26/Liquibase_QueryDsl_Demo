package com.meetup.dynamicorm.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Getter
public class EntityRequest {
    private List<String> fields = new ArrayList<>();
    private List<LinkedHashMap<String, Object>> predicateSet = new ArrayList<>();
    private LinkedHashMap<String, Object> saveEntityRequest = new LinkedHashMap<>();
}
