package au.com.apiled.module.processor.config;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildMapConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromMapEntryType;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import au.com.apiled.module.api.config.AdvancedLogger;
import au.com.apiled.module.api.processor.AdvancedLoggerProcessor;

/**
 * Provider for {@code ComponentBuildingDefinition}s to parse TEST module
 * configuration.
 *
 * @since 4.0
 */
public class AdvancedLoggerComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

	private static final String LOGGER = "logger";

	private ComponentBuildingDefinition.Builder baseDefinition;

	public AdvancedLoggerComponentBuildingDefinitionProvider() {
	}

	@Override
	public void init() {
		baseDefinition = new ComponentBuildingDefinition.Builder()
				.withNamespace(AdvancedLogger.PREFIX);
	}

	@Override
	public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
		List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();

		componentBuildingDefinitions
				.add(baseDefinition.withIdentifier(LOGGER).withTypeDefinition(fromType(AdvancedLoggerProcessor.class))
						.withSetterParameterDefinition("tag", fromSimpleParameter("tag").build())
						.withSetterParameterDefinition("message", fromSimpleParameter("message").build())
						.withSetterParameterDefinition("category", fromSimpleParameter("category").build())
						.withSetterParameterDefinition("level", fromSimpleParameter("level").build())
						.withSetterParameterDefinition("isSubflow", fromSimpleParameter("isSubflow").build())
						
						.withSetterParameterDefinition("customProperties", fromChildMapConfiguration(String.class, String.class)
								 .withWrapperIdentifier("custom-properties").build())
						.withSetterParameterDefinition("event", fromSimpleParameter("event").build())
						.withSetterParameterDefinition("action", fromSimpleParameter("action").build())

						.build());

	    componentBuildingDefinitions.add(baseDefinition.withIdentifier("custom-properties")
	            .withTypeDefinition(fromType(HashMap.class)).build());
	    componentBuildingDefinitions.add(baseDefinition.withIdentifier("custom-property")
	            .withTypeDefinition(fromMapEntryType(String.class, String.class))
	            .build());

		return componentBuildingDefinitions;
	}

}
