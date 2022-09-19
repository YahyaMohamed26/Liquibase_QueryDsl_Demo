package com.meetup.liquibase.controller;

import com.meetup.liquibase.domain.model.Entity;
import com.meetup.liquibase.domain.model.EntityRequest;
import com.meetup.liquibase.domain.service.LiquibaseService;
import com.meetup.liquibase.domain.service.QueryDslService;
import com.querydsl.core.Tuple;
import liquibase.exception.LiquibaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/database/")
@RequiredArgsConstructor
@Slf4j
public class DatabaseController {

    private final LiquibaseService liquibaseService;
    private final QueryDslService queryDslService;

    @GetMapping("/publish-changes")
    public String publishChanges() throws LiquibaseException, IOException {
        return liquibaseService.updateRemoteDatabaseFromLocalChanges();
    }

    @GetMapping("/{table_name}/search")
    public String searchEntity(@PathVariable("table_name") String tableName,
                               @RequestBody EntityRequest entityRequest) {
        return queryDslService.searchEntity(tableName, entityRequest);
    }

    @PostMapping("/{table_name}/save")
    public String saveEntityToTable(@PathVariable("table_name") String tableName,
                                    @RequestBody EntityRequest entityRequest) {
        return queryDslService.saveEntityToTable(tableName, entityRequest);
    }
}
