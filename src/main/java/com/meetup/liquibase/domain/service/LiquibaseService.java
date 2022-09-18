package com.meetup.liquibase.domain.service;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;

import static org.postgresql.core.ConnectionFactory.openConnection;
@Service
@RequiredArgsConstructor
@Slf4j
public class LiquibaseService {
    private final DataSource dataSource;

    public String updateDatabase(MultipartFile file) throws LiquibaseException, IOException {
        write(file, Path.of("/Users/yelnouby/Desktop/Liquibase_QueryDsl_Demo/src/main/resources/db/changelog"));

        ResourceAccessor accessor = new FileSystemResourceAccessor(new File("/Users/yelnouby/Desktop/Liquibase_QueryDsl_Demo/src/main/resources/db/changelog"));

        Connection connection = DataSourceUtils.getConnection(dataSource);

        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

        Liquibase liquibase = new liquibase.Liquibase("/Users/yelnouby/Desktop/Liquibase_QueryDsl_Demo/src/main/resources/db/changelog/changelog.json", accessor, database);

        liquibase.update(new Contexts(), new LabelExpression());

        return "updated";
    }

    public void write(MultipartFile file, Path dir) throws IOException {
        Path filepath = Paths.get(dir.toString(), file.getOriginalFilename());

        try (OutputStream os = Files.newOutputStream(filepath)) {
            os.write(file.getBytes());
        }
    }
}
