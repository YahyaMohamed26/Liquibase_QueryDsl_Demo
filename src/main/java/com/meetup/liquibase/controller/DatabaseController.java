package com.meetup.liquibase.controller;

import com.meetup.liquibase.domain.model.EntityRequest;
import com.meetup.liquibase.domain.service.LiquibaseService;
import com.meetup.liquibase.domain.service.QueryDslService;

import liquibase.exception.LiquibaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/database")
@RequiredArgsConstructor
@Slf4j
public class DatabaseController {

    private final LiquibaseService liquibaseService;
    private final QueryDslService queryDslService;

    @GetMapping("/publish-changes")
    public String publishChanges(@RequestPart(value = "file") MultipartFile file) throws LiquibaseException, IOException {
        return liquibaseService.updateDatabase(file);
    }


    @PostMapping("/{table_name}/search")
    public ArrayList<String> searchEntity(@PathVariable("table_name") String tableName,
                                          @RequestBody EntityRequest entityRequest) {
        return queryDslService.searchEntity(tableName, entityRequest);
    }

    @PostMapping("/{table_name}/save")
    public Object saveEntityToTable(@PathVariable("table_name") String tableName,
                                    @RequestBody EntityRequest entityRequest) {
        return queryDslService.saveEntityToTable(tableName, entityRequest);
    }
}
