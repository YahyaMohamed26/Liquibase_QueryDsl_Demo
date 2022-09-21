package com.meetup.dynamicorm.controller;

import com.meetup.dynamicorm.model.EntityRequest;
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
@RequestMapping("save")
@RequiredArgsConstructor
public class SaveController {
	private final QueryDslService queryDslService;

	@PostMapping(value = "/{tableName}")
	public Object save(@PathVariable("tableName") String tableName, @RequestBody LinkedHashMap<String, Object> entitySaveRequest) throws Exception {
		return queryDslService.saveEntityToTable(tableName, entitySaveRequest);
	}

}
