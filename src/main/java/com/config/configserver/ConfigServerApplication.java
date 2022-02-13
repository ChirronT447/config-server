package com.config.configserver;

import com.config.configserver.service.ConfigLoadingService;
import com.config.configserver.service.ConfigDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.config.configserver.service.ConfigLoadingService.APPLICATION;

@EnableSwagger2
@SpringBootApplication
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2)
				.select()
				.apis(RequestHandlerSelectors.any())
				.paths(PathSelectors.any())
				.build();
	}
}

@Service
class ConfigService {

	private final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);
	private final ConfigLoadingService configLoadingService;
	private final ConfigDatabaseService configDatabaseService;

	public ConfigService(final ConfigLoadingService configLoadingService, final ConfigDatabaseService configDatabaseService) {
		this.configLoadingService = configLoadingService;
		this.configDatabaseService = configDatabaseService;
	}

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
		final List<Path> applications = configLoadingService.fetchApplications();
		for(Path app : applications) {
			final Map<String, Path> profiles = configLoadingService.fetchProfiles(app);  // Map of profile : app config file
			try {
				final Map<String, String> baseProperties = configLoadingService.loadPropertiesFromPath(profiles.get(APPLICATION));
				profiles.remove(APPLICATION);
				for(Map.Entry<String, Path> profile : profiles.entrySet()) { // For each profile
					final Map<String, String> combinedProperties = configLoadingService.combineProperties(baseProperties, profile.getValue());
					for(String property : combinedProperties.keySet()) { // For each property
						boolean result = configDatabaseService.insertIntoDatabase(
								app.getFileName().toString(),       // The Application name
								profile.getKey(),                   // Profile name
								property,                           // Property
								combinedProperties.get(property)    // Value of specified property
						);
						LOGGER.info("Insert succeeded: {}", result);
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

}