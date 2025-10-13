package au.com.apiled.module.api.processor;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.core.api.util.StreamingUtils.withCursoredEvent;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extensions.jms.api.message.JmsAttributes;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.meta.JavaVersion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.apiled.module.api.component.AdvancedLoggerBaseAttributes;

@JavaVersionSupport({ JavaVersion.JAVA_8, JavaVersion.JAVA_11, JavaVersion.JAVA_17 })
public class AdvancedLoggerProcessor extends AbstractComponent implements Processor, Initialisable {
	private static final String BLOCKING_CATEGORIES_PROPERTY = System.getProperty("com.mule.logging.blockingCategories",
			"");

	private static final Set<String> BLOCKING_CATEGORIES = new HashSet<>(
			Arrays.asList(BLOCKING_CATEGORIES_PROPERTY.split(",")));

	private volatile ReactiveProcessor.ProcessingType processingType;

	private transient ClassLoader loggerExecutionClassloader;

	protected transient Logger logger;

	// UI fields
	protected String tag = "NONE";
	protected String message;
	protected String category;
	protected String level = "INFO";
	protected String event;
	protected String action;
	protected HashMap<String, String> customProperties;
	protected AdvancedLoggerBaseAttributes advancedLoggerBaseAttributes;
	protected boolean isSubflow = false;
	protected boolean pushErrorNotification = false;
	protected String domainName = "${cloudhub.domain}";

	public boolean isSubflow() {
		return isSubflow;
	}

	public void setIsSubflow(boolean isSubflow) {
		this.isSubflow = isSubflow;
	}

	public HashMap<String, String> getCustomProperties() {
		return customProperties;
	}

	public void setCustomProperties(HashMap<String, String> customProperties) {
		this.customProperties = customProperties;
	}

	public String getTag() {
		return tag;
	}

	public String getMessage() {
		return message;
	}

	public String getCategory() {
		return category;
	}

	public String getLevel() {
		return level;
	}

	public String getEvent() {
		return event;
	}

	public String getAction() {
		return action;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	// others
	ExtendedExpressionManager expressionManager;

	public AdvancedLoggerBaseAttributes getAdvancedLoggerBaseAttributes() {
		return advancedLoggerBaseAttributes;
	}

	public void setAdvancedLoggerBaseAttributes(AdvancedLoggerBaseAttributes att) {
		advancedLoggerBaseAttributes = att;
	}

	private SimpleDateFormat sdf = new SimpleDateFormat();

	public void initialise() throws InitialisationException {
		sdf.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
		initLogger();
		initProcessingTypeIfPossible();
	}

	protected void initLogger() {
		this.loggerExecutionClassloader = Thread.currentThread().getContextClassLoader();
		if (category != null) {
			logger = LoggerFactory.getLogger(category);
		} else {
			logger = LoggerFactory.getLogger(AdvancedLoggerProcessor.class);
			category = "";
		}

	}

	protected void initProcessingTypeIfPossible() {
		if (getBlockingCategories().size() == 1 && getBlockingCategories().contains("")) {
			this.processingType = ReactiveProcessor.ProcessingType.CPU_LITE;
		} else if (getBlockingCategories().contains("*")) {
			this.processingType = ReactiveProcessor.ProcessingType.BLOCKING;
		}
	}

	public ReactiveProcessor.ProcessingType getProcessingType() {
		if (this.processingType == null)
			synchronized (this) {
				if (this.processingType == null)
					this.processingType = isBlocking(this.category)
							? ReactiveProcessor.ProcessingType.BLOCKING
							: ReactiveProcessor.ProcessingType.CPU_LITE;
			}
		return this.processingType;
	}

	private boolean isBlocking(String category) {
		return getBlockingCategories().stream().anyMatch(blockingCategory -> (blockingCategory.equals(category)
				|| (category != null && category.startsWith(blockingCategory + "."))));
	}

	public CoreEvent process(CoreEvent event) throws MuleException {

		return processIt(event);
	}

	protected CoreEvent processIt(CoreEvent event) {
		if (this.loggerExecutionClassloader != null) {
			return ClassUtils.withContextClassLoader(this.loggerExecutionClassloader, () -> doLog(event));
		} else {
			return doLog(event);
		}
	}

	// -----
	private CoreEvent doLog(CoreEvent coreEvent) {

		if (tag.equals("MAIN")) {

			AdvancedLoggerBaseAttributes advancedLoggerBaseAttributesTmp = new AdvancedLoggerBaseAttributes();

			advancedLoggerBaseAttributesTmp.setStart(new Date());
			advancedLoggerBaseAttributesTmp.setTransactionId(coreEvent.getCorrelationId());

			if (coreEvent.getMessage().getAttributes() != null) {
				if (coreEvent.getMessage().getAttributes().getValue() instanceof HttpRequestAttributes) {
					HttpRequestAttributes httpA = (HttpRequestAttributes) coreEvent.getMessage().getAttributes()
							.getValue();
					if (httpA.getHeaders().get("advanced-logger-transaction-id") != null
							&& httpA.getHeaders().get("advanced-logger-ransaction-id").length() > 0) {
						advancedLoggerBaseAttributesTmp
								.setTransactionId(httpA.getHeaders().get("advanced-logger-transaction-id"));
					}
				}
				if (coreEvent.getMessage().getAttributes().getValue() instanceof JmsAttributes) {
					JmsAttributes jmsA = (JmsAttributes) coreEvent.getMessage().getAttributes().getValue();

					if (jmsA.getProperties().getUserProperties().get("advanced_logger_transaction_id") != null
							&& ((String) jmsA.getProperties().getUserProperties().get("advanced_logger_transaction_id"))
									.length() > 0) {
						advancedLoggerBaseAttributesTmp.setTransactionId(
								((String) jmsA.getProperties().getUserProperties()
										.get("advanced_logger_transaction_id")));
					}
				}
			}

			if (customProperties != null && !customProperties.isEmpty()) {
				advancedLoggerBaseAttributesTmp = processCustomProperties(advancedLoggerBaseAttributesTmp,
						customProperties, expressionManager, coreEvent);
			}
			advancedLoggerBaseAttributesTmp.setAction(action);
			advancedLoggerBaseAttributesTmp.setEvent(getEvent());

			advancedLoggerBaseAttributes = advancedLoggerBaseAttributesTmp;

		} else if (tag.equals("START")) {

			AdvancedLoggerBaseAttributes advancedLoggerBaseAttributesTmp = new AdvancedLoggerBaseAttributes();

			// grab existent one if exists
			if (coreEvent.getVariables().containsKey("advancedLoggerBaseAttributes")) {
				advancedLoggerBaseAttributesTmp = (AdvancedLoggerBaseAttributes) coreEvent.getVariables()
						.get("advancedLoggerBaseAttributes").getValue();
			}

			if (!isSubflow) {

				advancedLoggerBaseAttributesTmp.setStart(new Date());
				advancedLoggerBaseAttributesTmp.setTransactionId(coreEvent.getCorrelationId());

				if (coreEvent.getMessage().getAttributes() != null) {
					if (coreEvent.getMessage().getAttributes().getValue() instanceof HttpRequestAttributes) {
						HttpRequestAttributes httpA = (HttpRequestAttributes) coreEvent.getMessage().getAttributes()
								.getValue();
						if (httpA.getHeaders().get("xtransaction-id") != null
								&& httpA.getHeaders().get("xtransaction-id").length() > 0) {
							advancedLoggerBaseAttributesTmp.setTransactionId(httpA.getHeaders().get("xtransaction-id"));
						}
					}
					if (coreEvent.getMessage().getAttributes().getValue() instanceof JmsAttributes) {
						JmsAttributes jmsA = (JmsAttributes) coreEvent.getMessage().getAttributes().getValue();

						if (jmsA.getProperties().getUserProperties().get("xtransaction_id") != null
								&& ((String) jmsA.getProperties().getUserProperties().get("xtransaction_id"))
										.length() > 0) {
							advancedLoggerBaseAttributesTmp.setTransactionId(
									((String) jmsA.getProperties().getUserProperties().get("xtransaction_id")));
						}
					}
				}

			} else {
				// grab existent one if exists
				if (coreEvent.getVariables().containsKey("advancedLoggerBaseAttributes")) {
					advancedLoggerBaseAttributesTmp = (AdvancedLoggerBaseAttributes) coreEvent.getVariables()
							.get("advancedLoggerBaseAttributes").getValue();
				}

				advancedLoggerBaseAttributesTmp.setStartSubflow(new Date());
			}

			if (customProperties != null && !customProperties.isEmpty()) {
				advancedLoggerBaseAttributesTmp = processCustomProperties(advancedLoggerBaseAttributesTmp,
						customProperties, expressionManager, coreEvent);
			}
			advancedLoggerBaseAttributesTmp.setAction(action);
			advancedLoggerBaseAttributesTmp.setEvent(getEvent());

			advancedLoggerBaseAttributes = advancedLoggerBaseAttributesTmp;

		} else if (tag.equals("END")) {

			AdvancedLoggerBaseAttributes advancedLoggerBaseAttributesTmp;
			if (coreEvent.getVariables().containsKey("advancedLoggerBaseAttributes")) {
				advancedLoggerBaseAttributesTmp = (AdvancedLoggerBaseAttributes) coreEvent.getVariables()
						.get("advancedLoggerBaseAttributes").getValue();
			} else {
				advancedLoggerBaseAttributesTmp = new AdvancedLoggerBaseAttributes();
				if (!isSubflow) {
					advancedLoggerBaseAttributesTmp.setStart(new Date());
				} else {
					advancedLoggerBaseAttributesTmp.setStartSubflow(new Date());
				}
				advancedLoggerBaseAttributesTmp.setTransactionId(coreEvent.getCorrelationId());
			}
			if (!isSubflow) {
				advancedLoggerBaseAttributesTmp.setEnd(new Date());

				advancedLoggerBaseAttributes = advancedLoggerBaseAttributesTmp;

				long timeTaken = (advancedLoggerBaseAttributesTmp.getEnd().getTime()
						- advancedLoggerBaseAttributesTmp.getStart().getTime());
				advancedLoggerBaseAttributesTmp.setTimeTaken(timeTaken);

			} else {
				advancedLoggerBaseAttributesTmp.setEndSubflow(new Date());
				advancedLoggerBaseAttributes = advancedLoggerBaseAttributesTmp;

				long timeTaken = (advancedLoggerBaseAttributesTmp.getEndSubflow().getTime()
						- advancedLoggerBaseAttributesTmp.getStartSubflow().getTime());
				advancedLoggerBaseAttributesTmp.setTimeTaken(timeTaken);
			}
			if (customProperties != null && !customProperties.isEmpty()) {
				advancedLoggerBaseAttributes = processCustomProperties(advancedLoggerBaseAttributes, customProperties,
						expressionManager,
						coreEvent);
			}

		} else if (tag.equals("NONE")) {

			if (coreEvent.getVariables().containsKey("advancedLoggerBaseAttributes")) {
				advancedLoggerBaseAttributes = (AdvancedLoggerBaseAttributes) coreEvent.getVariables()
						.get("advancedLoggerBaseAttributes").getValue();

				if (customProperties != null && !customProperties.isEmpty()) {

					advancedLoggerBaseAttributes = processCustomProperties(advancedLoggerBaseAttributes,
							customProperties, expressionManager,
							coreEvent);
				}
			}

		}

		if (advancedLoggerBaseAttributes != null) {
			CoreEvent newEvent = CoreEvent.builder(coreEvent)
					.addVariable("advancedLoggerBaseAttributes", advancedLoggerBaseAttributes)
					.build();

			try {
				return withCursoredEvent(newEvent, cursored -> {

					log(cursored);

					return newEvent;
				});
			} catch (MuleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			CoreEvent newEvent = CoreEvent.builder(coreEvent).build();

			try {
				return withCursoredEvent(newEvent, cursored -> {

					log(cursored);

					return newEvent;
				});
			} catch (MuleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return coreEvent;
	}

	protected void log(CoreEvent coreEvent) {

		String endDate = "";

		if (advancedLoggerBaseAttributes != null && advancedLoggerBaseAttributes.getEnd() != null && tag.equals("END")
				&& !isSubflow) {
			endDate = sdf.format(advancedLoggerBaseAttributes.getEnd()) + " timeTaken="
					+ advancedLoggerBaseAttributes.getTimeTaken()
					+ "ms  ";
		} else {
			endDate = null;
		}
		String endDateSubflow = "";
		if (advancedLoggerBaseAttributes != null && advancedLoggerBaseAttributes.getEndSubflow() != null
				&& tag.equals("END") && isSubflow) {
			endDateSubflow = sdf.format(advancedLoggerBaseAttributes.getEndSubflow()) + " timeTaken="
					+ advancedLoggerBaseAttributes.getTimeTaken() + "ms  ";
		} else {
			endDateSubflow = null;
		}

		HashMap variables = new HashMap();

		if ((level.equals("DEBUG") && logger.isDebugEnabled()) || logger.isDebugEnabled() && !logger.isTraceEnabled()) {

			level = "DEBUG";
			Configurator.setLevel(logger.getName(), Level.DEBUG);

		} else if ((level.equals("DEBUG") || level.equals("INFO") || level.equals("TRACE")) && !logger.isDebugEnabled()
				&& !logger.isTraceEnabled() && logger.isInfoEnabled()) {

			level = "INFO";
			Configurator.setLevel(logger.getName(), Level.INFO);

		} else if ((level.equals("TRACE") && logger.isTraceEnabled()) || logger.isTraceEnabled()) {

			level = "TRACE";
			Configurator.setLevel(logger.getName(), Level.TRACE);

		} else if (level.equals("WARN") || (logger.isWarnEnabled() && !logger.isInfoEnabled())) {

			level = "WARN";
			Configurator.setLevel(logger.getName(), Level.WARN);

		} else if (level.equals("ERROR") || (logger.isErrorEnabled() && !logger.isInfoEnabled())) {

			level = "ERROR";
			Configurator.setLevel(logger.getName(), Level.ERROR);
		}

		if (event == null) {
			logWithLevel(null, coreEvent, variables, endDate, endDateSubflow);

		} else {

			coreEvent.getVariables().forEach((k, v) -> {
				variables.put(k, v.getValue());
			});

			if (StringUtils.isEmpty(message)) {
				logWithLevel(coreEvent, coreEvent, variables, endDate, endDateSubflow);
			} else {

				LogLevel logLevel = LogLevel.valueOf(level);

				if (advancedLoggerBaseAttributes != null) {

					if (!tag.equals("END") && (level.equals("DEBUG") || level.equals("TRACE") || logger.isDebugEnabled()
							|| logger.isTraceEnabled())) {

						logLevel.log(logger,
								category + "  transactionId=" + advancedLoggerBaseAttributes.getTransactionId()
										+ " start="
										+ sdf.format(isSubflow ? advancedLoggerBaseAttributes.getStartSubflow()
												: advancedLoggerBaseAttributes.getStart())
										+ " end=" + (isSubflow ? endDateSubflow : endDate) + "  customProperties="
										+ advancedLoggerBaseAttributes.getCustomProperties().toString() + "  message="
										+ expressionManager.parseLogTemplate(message, coreEvent, getLocation(),
												NULL_BINDING_CONTEXT)
										+ " \r\n variables=" + variables + " \r\n MuleMessage= "
										+ coreEvent.getMessage() + "\r\n payload= "
										+ expressionManager.parseLogTemplate("#[payload]", coreEvent, getLocation(),
												NULL_BINDING_CONTEXT));

					} else if (!tag.equals("END") && !level.equals("DEBUG") && !level.equals("TRACE")
							&& !logger.isDebugEnabled() && !logger.isTraceEnabled()) {

						logLevel.log(logger,
								category + "  transactionId=" + advancedLoggerBaseAttributes.getTransactionId()
										+ " start="
										+ sdf.format(isSubflow ? advancedLoggerBaseAttributes.getStartSubflow()
												: advancedLoggerBaseAttributes.getStart())
										+ " end=" + (isSubflow ? endDateSubflow : endDate) + " customProperties="
										+ advancedLoggerBaseAttributes.getCustomProperties().toString());

					} else if (tag.equals("END") && (level.equals("DEBUG") || level.equals("TRACE")
							|| logger.isDebugEnabled() || logger.isTraceEnabled())) {

						logLevel.log(logger,
								category + "  transactionId=" + advancedLoggerBaseAttributes.getTransactionId()
										+ " start="
										+ sdf.format(isSubflow ? advancedLoggerBaseAttributes.getStartSubflow()
												: advancedLoggerBaseAttributes.getStart())
										+ " end=" + (isSubflow ? endDateSubflow : endDate) + "  customProperties="
										+ advancedLoggerBaseAttributes.getCustomProperties().toString() + "  message="
										+ expressionManager.parseLogTemplate(message, coreEvent, getLocation(),
												NULL_BINDING_CONTEXT)
										+ " \r\n variables=" + variables + " \r\n MuleMessage= "
										+ coreEvent.getMessage() + "\r\n payload= "
										+ expressionManager.parseLogTemplate("#[payload]", coreEvent, getLocation(),
												NULL_BINDING_CONTEXT));

					} else if (tag.equals("END") && !level.equals("DEBUG") && !level.equals("TRACE")
							&& !logger.isDebugEnabled() && !logger.isTraceEnabled()) {

						logLevel.log(logger,
								category + "  transactionId=" + advancedLoggerBaseAttributes.getTransactionId()
										+ " start="
										+ sdf.format(isSubflow ? advancedLoggerBaseAttributes.getStartSubflow()
												: advancedLoggerBaseAttributes.getStart())
										+ " end=" + (isSubflow ? endDateSubflow : endDate) + "  customProperties="
										+ advancedLoggerBaseAttributes.getCustomProperties().toString());
					}

				} else {
					if (!tag.equals("END") && (level.equals("DEBUG") || level.equals("TRACE") || logger.isDebugEnabled()
							|| logger.isTraceEnabled())) {

						logLevel.log(logger,
								category + "  "
										+ expressionManager.parseLogTemplate(message, coreEvent, getLocation(),
												NULL_BINDING_CONTEXT)
										+ " \r\n variables=" + variables + " \r\n MuleMessage= "
										+ coreEvent.getMessage() + "\r\n payload= "
										+ expressionManager.parseLogTemplate("#[payload]", coreEvent, getLocation(),
												NULL_BINDING_CONTEXT));

					} else if (!tag.equals("END") && !level.equals("DEBUG") && !level.equals("TRACE")
							&& !logger.isDebugEnabled() && !logger.isTraceEnabled()) {

						logLevel.log(logger, category);

					} else if (tag.equals("END") && (level.equals("DEBUG") || level.equals("TRACE")
							|| logger.isDebugEnabled() || logger.isTraceEnabled())) {

						logLevel.log(logger,
								category + " "
										+ expressionManager.parseLogTemplate(message, coreEvent, getLocation(),
												NULL_BINDING_CONTEXT)
										+ " \r\n variables=" + variables + " \r\n MuleMessage= "
										+ coreEvent.getMessage() + "\r\n payload= "
										+ expressionManager.parseLogTemplate("#[payload]", coreEvent, getLocation(),
												NULL_BINDING_CONTEXT));

					} else if (tag.equals("END") && !level.equals("DEBUG") && !level.equals("TRACE")
							&& !logger.isDebugEnabled() && !logger.isTraceEnabled()) {

						logLevel.log(logger, category);
					}

				}
			}
		}
	}

	protected void logWithLevel(Object object, CoreEvent event, HashMap variables, String endDate,
			String endDateSubflow) {
		LogLevel logLevel = LogLevel.valueOf(level);

		if (advancedLoggerBaseAttributes != null) {
			if (!tag.equals("END") && (level.equals("DEBUG") || level.equals("TRACE") || logger.isDebugEnabled()
					|| logger.isTraceEnabled())) {

				logLevel.log(logger, category + "  transactionId=" + advancedLoggerBaseAttributes.getTransactionId()
						+ " start="
						+ sdf.format(isSubflow ? advancedLoggerBaseAttributes.getStartSubflow()
								: advancedLoggerBaseAttributes.getStart())
						+ " end= " + (isSubflow ? endDateSubflow : endDate) + "  customProperties="
						+ advancedLoggerBaseAttributes.getCustomProperties().toString() + " \r\n variables=" + variables
						+ " \r\n MuleMessage= " + event.getMessage() + "\r\n payload= "
						+ expressionManager.parseLogTemplate("#[payload]", event, getLocation(), NULL_BINDING_CONTEXT));

			} else if (!tag.equals("END") && !level.equals("DEBUG") && !level.equals("TRACE")
					&& !logger.isDebugEnabled() && !logger.isTraceEnabled()) {

				logLevel.log(logger,
						category + "  transactionId=" + advancedLoggerBaseAttributes.getTransactionId() + " start="
								+ sdf.format(
										isSubflow ? advancedLoggerBaseAttributes.getStartSubflow()
												: advancedLoggerBaseAttributes.getStart())
								+ " end=" + (isSubflow ? endDateSubflow : endDate) + "  customProperties="
								+ advancedLoggerBaseAttributes.getCustomProperties().toString());

			} else if (tag.equals("END") && (level.equals("DEBUG") || level.equals("TRACE") || logger.isDebugEnabled()
					|| logger.isTraceEnabled())) {

				logLevel.log(logger, category + "  transactionId=" + advancedLoggerBaseAttributes.getTransactionId()
						+ " start="
						+ sdf.format(isSubflow ? advancedLoggerBaseAttributes.getStartSubflow()
								: advancedLoggerBaseAttributes.getStart())
						+ " end=" + (isSubflow ? endDateSubflow : endDate) + "  customProperties="
						+ advancedLoggerBaseAttributes.getCustomProperties().toString() + " \r\n variables=" + variables
						+ " \r\n MuleMessage= " + event.getMessage() + "\r\n payload= "
						+ expressionManager.parseLogTemplate("#[payload]", event, getLocation(), NULL_BINDING_CONTEXT));
			} else if (tag.equals("END") && !level.equals("DEBUG") && !level.equals("TRACE") && !logger.isDebugEnabled()
					&& !logger.isTraceEnabled()) {

				logLevel.log(logger,
						category + "  transactionId=" + advancedLoggerBaseAttributes.getTransactionId() + " start="
								+ sdf.format(
										isSubflow ? advancedLoggerBaseAttributes.getStartSubflow()
												: advancedLoggerBaseAttributes.getStart())
								+ " end=" + (isSubflow ? endDateSubflow : endDate) + "  customProperties="
								+ advancedLoggerBaseAttributes.getCustomProperties().toString());
			}
		} else {
			if (!tag.equals("END") && (level.equals("DEBUG") || level.equals("TRACE") || logger.isDebugEnabled()
					|| logger.isTraceEnabled())) {

				logLevel.log(logger, category + " \r\n variables=" + variables + " \r\n MuleMessage= "
						+ event.getMessage() + "\r\n payload= "
						+ expressionManager.parseLogTemplate("#[payload]", event, getLocation(), NULL_BINDING_CONTEXT));

			} else if (!tag.equals("END") && !level.equals("DEBUG") && !level.equals("TRACE")
					&& !logger.isDebugEnabled() && !logger.isTraceEnabled()) {

				logLevel.log(logger, category);

			} else if (tag.equals("END") && (level.equals("DEBUG") || level.equals("TRACE") || logger.isDebugEnabled()
					|| logger.isTraceEnabled())) {

				logLevel.log(logger, category + " \r\n variables=" + variables + " \r\n MuleMessage= "
						+ event.getMessage() + "\r\n payload= "
						+ expressionManager.parseLogTemplate("#[payload]", event, getLocation(), NULL_BINDING_CONTEXT));
			} else if (tag.equals("END") && !level.equals("DEBUG") && !level.equals("TRACE") && !logger.isDebugEnabled()
					&& !logger.isTraceEnabled()) {

				logLevel.log(logger, category);
			}
		}
	}

	@Inject
	public void setExpressionManager(ExtendedExpressionManager expressionManager) {
		this.expressionManager = expressionManager;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public void setLevel(String level) {
		this.level = level.toUpperCase();
	}

	public void setTag(String tag) {
		this.tag = tag.toUpperCase();
	}

	private AdvancedLoggerBaseAttributes processCustomProperties(
			AdvancedLoggerBaseAttributes advancedLoggerBaseAttributes,
			HashMap<String, String> _customProperties, ExtendedExpressionManager expressionManager, CoreEvent event) {

		for (java.util.Map.Entry<String, String> entry : _customProperties.entrySet()) {
			if (advancedLoggerBaseAttributes.getCustomProperties().containsKey(entry.getKey())) {
				advancedLoggerBaseAttributes.getCustomProperties().replace(entry.getKey(), expressionManager
						.parseLogTemplate(entry.getValue(), event, getLocation(), NULL_BINDING_CONTEXT));
			} else {
				advancedLoggerBaseAttributes.getCustomProperties().put(entry.getKey(), expressionManager
						.parseLogTemplate(entry.getValue(), event, getLocation(), NULL_BINDING_CONTEXT));
			}
		}

		return advancedLoggerBaseAttributes;
	}

	public enum LogLevel {
		ERROR {

			@Override
			public void log(Logger logger, Object object) {
				logger.error(object == null ? null : object.toString());
			}

			@Override
			public boolean isEnabled(Logger logger) {
				return logger.isErrorEnabled();
			}
		},
		WARN {

			@Override
			public void log(Logger logger, Object object) {
				logger.warn(object == null ? null : object.toString());
			}

			@Override
			public boolean isEnabled(Logger logger) {
				return logger.isWarnEnabled();
			}
		},
		INFO {

			@Override
			public void log(Logger logger, Object object) {
				logger.info(object == null ? null : object.toString());
			}

			@Override
			public boolean isEnabled(Logger logger) {
				return logger.isInfoEnabled();
			}
		},
		DEBUG {

			@Override
			public void log(Logger logger, Object object) {
				logger.debug(object == null ? null : object.toString());
			}

			@Override
			public boolean isEnabled(Logger logger) {
				return logger.isDebugEnabled();
			}
		},
		TRACE {

			@Override
			public void log(Logger logger, Object object) {
				logger.trace(object == null ? null : object.toString());
			}

			@Override
			public boolean isEnabled(Logger logger) {
				return logger.isTraceEnabled();
			}
		};

		public abstract void log(Logger logger, Object object);

		public abstract boolean isEnabled(Logger logger);
	}

	protected Set<String> getBlockingCategories() {
		return BLOCKING_CATEGORIES;
	}
}
