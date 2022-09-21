package com.meetup.dynamicorm.controller;

import com.meetup.dynamicorm.model.SearchEntityRequest;
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
public class SearchController {
	private final QueryDslService queryDslService;

	@PostMapping(value = "/{tableName}")
	public List<LinkedHashMap<String, Object>> query(@PathVariable("tableName") String tableName, @RequestBody SearchEntityRequest searchEntityRequest) throws Exception {
		return queryDslService.searchEntity(tableName, searchEntityRequest);
	}

}
