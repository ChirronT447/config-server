package com.config.configserver.service;

import com.config.configserver.database.Row;
import com.config.configserver.exception.ResultNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.Optional;

import static com.config.configserver.service.SqlStatements.SINGLE_INSERT;

@Service
public class ConfigDatabaseService {

    private final Logger LOGGER = LoggerFactory.getLogger(ConfigDatabaseService.class);
    private final String url;
    private final String usr;
    private final String pwd;

    public ConfigDatabaseService(
            @Value("${spring.datasource.url}")      final String url,
            @Value("${spring.datasource.username}") final String usr,
            @Value("${spring.datasource.password}") final String pwd
    ) {
        this.url = url;
        this.usr = usr;
        this.pwd = pwd;
    }

    // Fetch config by executing SQL against database as
    public String fetchConfiguration(String sql, String... params) throws SQLException, ResultNotFoundException {
        final PreparedStatement preparedStatement = setSqlParameters(sql, params);
        final Optional<String> result = executeSqlAndFetchResult(preparedStatement);
        if (result.isPresent()) {
            LOGGER.info("Result found for key [{}]: ", result);
            return result.get();
        } else {
            final String key = String.join("/", params);
            LOGGER.info("No result found for key [{}]: ", key);
            throw new ResultNotFoundException(key);
        }
    }

    // Does what it says
    public boolean insertIntoDatabase(
            final String appName,
            final String profile,
            final String prop_key,
            final String value
    ) throws SQLException {
        final Row row = new Row(appName, profile, prop_key, value, "latest");
        LOGGER.info("Inserting --> " + row);
        String[] params = {appName, profile, prop_key, value, "latest"};
        try(final Connection conn = DriverManager.getConnection(url, usr, pwd)) {
            final PreparedStatement preparedStatement = conn.prepareStatement(SINGLE_INSERT);
            for(int i = 0; i < params.length; i++) {
                preparedStatement.setString(i + 1, params[i]);
            }
            return preparedStatement.execute();
        }
    }

    // Set parameters into the SQL statement
    private PreparedStatement setSqlParameters(String sql, String... params) throws SQLException {
        try(final Connection conn = DriverManager.getConnection(url, usr, pwd)) {
            final PreparedStatement preparedStatement = conn.prepareStatement(sql);
            for(int i = 1, j = 0; i < params.length; i++, j++) {
                preparedStatement.setString(i, params[j]);
            }
            return preparedStatement;
        }
    }

    // Execute SQL and return result; closing ResultSet and PreparedStatement
    private Optional<String> executeSqlAndFetchResult(PreparedStatement sql) throws SQLException {
        final ResultSet rs = sql.executeQuery();
        if(rs.next()) {
            final String result = rs.getString(1);
            rs.close();
            sql.close();
            return Optional.of(result);
        } else {
            rs.close();
            sql.close();
            return Optional.empty();
        }
    }

}
