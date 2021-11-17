package com.config.configserver.service;

import com.config.configserver.database.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConfigLoadingService {

    private final Logger LOGGER = LoggerFactory.getLogger(ConfigLoadingService.class);
    public static final String LOCATION = "configurations/";
    public static final String APPLICATION = "application";

    /**
     // 1) Folder name is the APPLICATION field
     // 2) Files loaded as ClassPathResource
     // 3) Properties = PropertiesLoaderUtils.loadProperties(resource)
     // Base file loaded then each file added
     // Save to database:
     //  APPLICATION = folder name
     //  PROFILE = Word after "-"
     //  LABEL = "latest"
     //  PROP_KEY = Map key
     //  VALUE = Corresponding value
     */
    @EventListener(ContextRefreshedEvent.class) // After Bean creation, before server starts
    public void doEverything() {
        LOGGER.info("###########################################################");
        LOGGER.info("###################### LOADING ############################");
        LOGGER.info("###########################################################");
        final List<Path> applications = fetchApplications();
        for(Path app : applications) {
            final Map<String, Path> profiles = fetchProfiles(app);  // Map of profile : app config file
            try {
                final Map<String, String> baseProperties = loadPropertiesFromPath(profiles.get(APPLICATION));
                profiles.remove(APPLICATION);
                for(Map.Entry<String, Path> profile : profiles.entrySet()) { // For each profile
                    final Map<String, String> combinedProperties = combineProperties(baseProperties, profile.getValue());
                    for(String property : combinedProperties.keySet()) { // For each property
                        insertIntoDatabase(
                                app.getFileName().toString(),       // The Application name
                                profile.getKey(),                   // Profile name
                                property,                           // Property
                                combinedProperties.get(property)    // Value of specified property
                        );
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error while loading config ...: ", e);
            }
        }
        LOGGER.info("###########################################################");
        LOGGER.info("###################### COMPLETE ###########################");
        LOGGER.info("###########################################################");
    }

    // Combine base properties (eg. application.yml) with profile specific properties (eg. application-dev.yml)
    private Map<String, String> combineProperties(Map<String, String> baseProperties, Path path) throws IOException {
        Map<String, String> profileProperties = loadPropertiesFromPath(path);
        Map<String, String> result =  new HashMap<>(baseProperties);
        result.putAll(profileProperties);   // Merge: duplicate keys are overwritten
        return result;
    }

    // Load contents of Path (must be on classpath) into a Map
    private Map<String, String> loadPropertiesFromPath(Path path) throws IOException {
        Map<String,String> propertyMap = new HashMap<>();
        switch(path.toString().split("\\.")[1]) {
            case "properties"   -> loadDotPropertiesFile(path, propertyMap);
            case "yml"          -> loadDotYamlFile(path, propertyMap);
            default -> throw new IllegalArgumentException("Unable to load unfamiliar file type");
        }
        return propertyMap;
    }

    private Map<String, String> loadDotPropertiesFile(Path path, Map<String, String> propertyMap) throws IOException {
        Resource resource = new FileSystemResource(path);
        Properties props = PropertiesLoaderUtils.loadProperties(resource);
        for(Object key : props.keySet()) {
            propertyMap.put(key.toString(), props.getProperty(key.toString()));
        }
        return propertyMap;
    }

    // YamlPropertiesFactoryBean will load YAML as Properties (YamlMapFactoryBean will load YAML as a Map).
    private Map<String, String> loadDotYamlFile(Path path, Map<String, String> propertyMap) {
        Resource resource = new FileSystemResource(path);
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(resource);
        Properties properties = yamlPropertiesFactoryBean.getObject();
        for(Object key : properties.keySet()) {
            propertyMap.put(key.toString(), properties.get(key).toString());
        }
        return propertyMap;
    }

    // Does what it says
    private void insertIntoDatabase(
            final String appName,
            final String profile,
            final String prop_key,
            final String value
    ) {
        Row row = new Row(appName, profile, prop_key, value, "latest");
        LOGGER.info("--> " + row);
        // save(row);
    }

    private Map<String, Path> fetchProfiles(final Path application) {
        final List<Path> files;
        try {
            files = Files.list(application)             // Get all app config files
                    .filter(e -> !Files.isDirectory(e)) // Return no directories
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Unable to load contents of {} due to IOException: ", application, e);
            return Map.of();
        }

        return files.stream().collect(Collectors.toMap(file -> {
            final String name = file.getFileName().toString();
            if(name.contains("-") && name.contains(".")) {
                return name.substring(name.indexOf("-") + 1, name.indexOf("."));
            } else {
                LOGGER.warn("Base properties file {} found under {}", name, application);
                return APPLICATION;
            }
        }, Function.identity()));
    }

    private List<Path> fetchApplications() {
        final Path dir = Paths.get(LOCATION);       // Get configurations/
        try {
            return Files.list(dir)                  // Stream with full directory listing
                    .filter(Files::isDirectory)     // Return only directories
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Unable to load contents of {} due to IOException: ", LOCATION, e);
            return List.of();
        }
    }

}
