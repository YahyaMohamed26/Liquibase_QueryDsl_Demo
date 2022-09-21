package com.meetup.dynamicorm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetup.dynamicorm.model.DbTableDto;
import com.meetup.dynamicorm.service.LiquibaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("database-schema")
@RequiredArgsConstructor
public class DatabaseSchemaController {
	private final LiquibaseService liquibaseService;

	@PostMapping(value = "/publish-changes")
	public String publishChanges(@RequestBody List<DbTableDto> dbTabelList) throws Exception {
		return liquibaseService.updateRemoteDatabaseFromLocalChanges(dbTabelList);
	}

}
