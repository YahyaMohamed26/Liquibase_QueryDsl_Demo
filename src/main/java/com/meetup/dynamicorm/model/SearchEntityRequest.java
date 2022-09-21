package com.meetup.dynamicorm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchEntityRequest {
    private List<String> fields = new ArrayList<>();
    private List<LinkedHashMap<String, Object>> predicateSet = new ArrayList<>();
}
