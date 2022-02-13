package com.config.configserver.api;

import com.config.configserver.service.ConfigDatabaseService;
import com.config.configserver.exception.ResultNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

import static com.config.configserver.service.SqlStatements.*;

/**
 * Key: application/environment/key
 * eg. aService/dev/otherServiceURI
 *     aService/qa/otherServiceURI
 */
@RestController
public class ConfigController {

    private final ConfigDatabaseService configDatabaseService;

    public ConfigController(final ConfigDatabaseService configDatabaseService) {
        this.configDatabaseService = configDatabaseService;
    }

    /**
     * Return all config across all environments (as defined by profiles) for this application
     *  Key: config/{application}
     * @param application Application or Service
     * @return ResponseEntity<String>
     */
    @GetMapping(value = "/config/{application}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchConfig(
            @PathVariable final String application
    ) {
        try {
            return ResponseEntity.ok(configDatabaseService.fetchConfiguration(APP_SQL, application));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body("Please check parameters");
        } catch (ResultNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Return all config in the provided environment (profile) for this application
     *  Key: config/{application}/{profile}
     * @param application Application or Service
     * @param profile Environment (Spring Profile)
     * @return ResponseEntity<String>
     */
    @GetMapping(value = "/config/{application}/{profile}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchConfig(
            @PathVariable final String application,
            @PathVariable final String profile
    ) {
        try {
            return ResponseEntity.ok(configDatabaseService.fetchConfiguration(APP_PROFILE_SQL, application, profile));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body("Please check parameters");
        } catch (ResultNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }


    /**
     * Return all config in the provided environment (profile) for this application
     *  Key: config/{application}/{profile}
     * @param application Application or Service
     * @param profile Environment (Spring Profile)
     * @param key Property Key
     * @return ResponseEntity<String>
     */
    @GetMapping(value = "/config/{application}/{profile}/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fetchConfig(
            @PathVariable final String application,
            @PathVariable final String profile,
            @PathVariable final String key
    ) {
        try {
            return ResponseEntity.ok(configDatabaseService.fetchConfiguration(APP_PROFILE_KEY_SQL, application, profile, key));
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body("Please check parameters");
        } catch (ResultNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

}
