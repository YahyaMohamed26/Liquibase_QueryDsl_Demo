package com.meetup.dynamicorm.controller;

import com.meetup.dynamicorm.model.SaveEntityRequest;
import com.meetup.dynamicorm.service.QueryDslService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;

@RestController
@RequestMapping("save")
@RequiredArgsConstructor
public class SaveController {
	private final QueryDslService queryDslService;

	@PostMapping(value = "/{tableName}")
	public SaveEntityRequest save(@PathVariable("tableName") String tableName, @RequestBody SaveEntityRequest saveEntityRequest) throws Exception {
		return queryDslService.saveEntityToTable(tableName, saveEntityRequest);
	}

}
