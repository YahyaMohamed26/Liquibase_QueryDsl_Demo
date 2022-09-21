package com.meetup.dynamicorm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetup.dynamicorm.model.DbTableDto;
import com.meetup.dynamicorm.model.EntityRequest;
import com.meetup.dynamicorm.service.LiquibaseService;
import com.meetup.dynamicorm.service.QueryDslService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("query")
@RequiredArgsConstructor
public class QueryController {
	private final ObjectMapper objectMapper;
	private final QueryDslService queryDslService;

	@PostMapping(value = "/{tableName}")
	public List<LinkedHashMap<String, Object>> query(@PathVariable("tableName") String tableName, @RequestBody EntityRequest entityRequest) throws Exception {

		return queryDslService.searchEntity(tableName, entityRequest);
	}

}
