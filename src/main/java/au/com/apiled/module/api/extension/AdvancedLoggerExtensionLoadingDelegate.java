package au.com.apiled.module.api.extension;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.ExtensionConstants;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.apiled.module.api.config.AdvancedLogger;

public class AdvancedLoggerExtensionLoadingDelegate implements ExtensionLoadingDelegate {

	protected transient Logger loggerSF = LoggerFactory.getLogger(AdvancedLoggerExtensionLoadingDelegate.class);

	static final String DEFAULT_LOG_LEVEL = "INFO";

	@Override
	public void accept(ExtensionDeclarer extensionDeclarer, ExtensionLoadingContext context) {

		loggerSF.info("AdvancedLoggerExtensionLoadingDelegate.accept - logger");

		final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault()
				.createTypeLoader(AdvancedLoggerExtensionLoadingDelegate.class.getClassLoader());

		extensionDeclarer.named(AdvancedLogger.MODULE_DESCRIPTION).describedAs(AdvancedLogger.MODULE_DESCRIPTION)
				.withCategory(COMMUNITY).onVersion(AdvancedLogger.MODULE_VERSION)
				.supportingJavaVersions(ExtensionConstants.ALL_SUPPORTED_JAVA_VERSIONS)
				.fromVendor(AdvancedLogger.MODULE_VENDOR)
				.withXmlDsl(XmlDslModel.builder().setPrefix(AdvancedLogger.PREFIX)
						.setNamespace(AdvancedLogger.NAMESPACE).setSchemaVersion(AdvancedLogger.MODULE_VERSION)
						.setXsdFileName(AdvancedLogger.MODULE_XSD_FILE_NAME)
						.setSchemaLocation(AdvancedLogger.MODULE_SCHEMA_LOCATION).build());

		declareLogger(extensionDeclarer, typeLoader);
	}

	private void declareLogger(ExtensionDeclarer extensionDeclarer, ClassTypeLoader typeLoader) {

		loggerSF.info("AdvancedLoggerExtensionLoadingDelegate.declareLogger - logger");

		OperationDeclarer logger = extensionDeclarer.withOperation("logger")
				.describedAs("Performs logging using an expression that determines what should be logged."
						+ "This logger creates an internal variable with all the attributes. "
						+ "Variable name: advancedLoggerBaseAttributes");

		logger.withOutput().ofType(typeLoader.load(void.class));
		logger.withOutputAttributes().ofType(typeLoader.load(void.class));

		loggerSF.info("AdvancedLoggerExtensionLoadingDelegate.declareLogger - message");

		logger.onDefaultParameterGroup().withOptionalParameter("message").ofType(typeLoader.load(String.class))
				.describedAs(
						"Message that will be logged. Embedded expressions can be used to extract value from the current message. "
								+ "If no message is specified then the current message is used.");

		loggerSF.info("AdvancedLoggerExtensionLoadingDelegate.declareLogger - level");

		logger.onDefaultParameterGroup().withOptionalParameter("level").defaultingTo(DEFAULT_LOG_LEVEL)
				.ofType(BaseTypeBuilder.create(JAVA).stringType().enumOf("ERROR", "WARN", "INFO", "DEBUG", "TRACE")
						.build())
				.withExpressionSupport(NOT_SUPPORTED)
				.describedAs("The logging level to be used. Default is " + DEFAULT_LOG_LEVEL + ".");

		loggerSF.info("AdvancedLoggerExtensionLoadingDelegate.declareLogger - action");

		logger.onDefaultParameterGroup().withRequiredParameter("action")
				.ofType(BaseTypeBuilder.create(JAVA).stringType()
						.enumOf("NONE", "CREATE", "UPDATE", "DELETE", "RETRIEVE", "EXCEPTION", "OTHER").build())
				.withExpressionSupport(NOT_SUPPORTED).describedAs("The action for the statistics");

		loggerSF.info("AdvancedLoggerExtensionLoadingDelegate.declareLogger - event");

		logger.onDefaultParameterGroup().withRequiredParameter("event").ofType(typeLoader.load(String.class))
				.withExpressionSupport(NOT_SUPPORTED).describedAs("Sets the statistics event - any name");

		loggerSF.info("AdvancedLoggerExtensionLoadingDelegate.declareLogger - category");

		logger.onDefaultParameterGroup().withOptionalParameter("category").ofType(typeLoader.load(String.class))
				.withExpressionSupport(NOT_SUPPORTED).describedAs("Sets the log category.");

		loggerSF.info("AdvancedLoggerExtensionLoadingDelegate.declareLogger - tag");

		logger.onDefaultParameterGroup().withRequiredParameter("tag")
				.ofType(BaseTypeBuilder.create(JAVA).stringType().enumOf("MAIN", "NONE", "START", "END").build())
				.withExpressionSupport(NOT_SUPPORTED).describedAs("The transaction state.");

		loggerSF.info("AdvancedLoggerExtensionLoadingDelegate.declareLogger - isSubflow");

		logger.onDefaultParameterGroup().withOptionalParameter("isSubflow").defaultingTo(false)
				.ofType(BaseTypeBuilder.create(JAVA).booleanType().defaultValue("false").build())
				.withExpressionSupport(NOT_SUPPORTED)
				.describedAs("It allows to add start and end time to subflows only, separated from the main flow");

		loggerSF.info("AdvancedLoggerExtensionLoadingDelegate.declareLogger - customProperties");
		logger.onDefaultParameterGroup().withOptionalParameter("customProperties")
				.ofType(typeLoader.load(java.util.Map.class)).describedAs("Custom Properties.");

	}
}
