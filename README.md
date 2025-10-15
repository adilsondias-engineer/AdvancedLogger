# Advanced Logger for Mule 4

A powerful Mule 4 custom logger module that provides advanced logging capabilities including transaction tracking, correlation ID propagation, automatic timing measurement, and custom properties support.

## Features

- **Correlation ID Tracking**: Automatically track and propagate correlation IDs across flows and applications
- **Transaction Timing**: Measure transaction execution time automatically with START/END tags
- **Subflow Timing**: Track subflow execution times independently
- **Custom Properties**: Add custom key-value properties for enhanced debugging and monitoring
- **Cross-Application Support**: Propagate correlation IDs via HTTP headers and JMS message properties
- **Multiple Log Levels**: Support for TRACE, DEBUG, INFO, WARN, and ERROR log levels
- **Flexible Tagging**: Use START, END, NONE, or MAIN tags to control logging behavior
## Distributed Systems Concepts
- Correlation ID propagation across microservices
- Transaction tracing in distributed architectures
- Circuit breaker patterns
- Eventual consistency handling
  
## TODO Performance Optimizations
- Asynchronous logging to avoid blocking main thread
- Circular buffer for log messages
- Zero-allocation string formatting
- Configurable batching for throughput optimization
  
## Table of Contents

- [Installation](#installation)
- [Building from Source](#building-from-source)
- [Configuration](#configuration)
- [Usage](#usage)
  - [Basic Logging](#basic-logging)
  - [Transaction Timing](#transaction-timing)
  - [Subflow Timing](#subflow-timing)
  - [Custom Properties](#custom-properties)
  - [HTTP Integration](#http-integration)
  - [JMS Integration](#jms-integration)
- [Attributes](#attributes)
- [Examples](#examples)
- [License](#license)

## Installation

### Maven Dependency

Add the following dependency to your Mule application's `pom.xml`:

```xml
<dependency>
    <groupId>au.com.apiled.module.plugins</groupId>
    <artifactId>advanced-logger</artifactId>
    <version>1.0.2-SNAPSHOT</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Namespace Configuration

Add the Advanced Logger namespace to your Mule configuration XML:

```xml
<mule xmlns:advanced-logger="http://www.api-led.com.au/modules"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="
          http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd
          http://www.api-led.com.au/modules http://www.api-led.com.au/modules/advanced-logger-modules.xsd">

    <!-- Your flows here -->

</mule>
```

## Building from Source

If you want to build the Advanced Logger module from source, follow these steps:

### Prerequisites

Before building, ensure you have the following installed:

- **Java 17** or higher
- **Apache Maven 3.9+**
- **Git**

### Clone the Repository

Clone the project from GitHub:

```bash
git clone https://github.com/your-organization/AdvancedLogger.git
cd AdvancedLogger
```

### Build the Project

Build the project using Maven:

```bash
mvn clean install
```

This command will:

1. Clean any previous build artifacts
2. Compile the source code
3. Run tests
4. Package the module as a Mule plugin
5. Install the artifact to your local Maven repository

### Build Output

After a successful build, you'll find the compiled module in the `target` directory:

```
target/advanced-logger-1.0.2-SNAPSHOT-mule-plugin.jar
```

### Skip Tests (Optional)

If you want to skip running tests during the build:

```bash
mvn clean install -DskipTests
```

### Using the Locally Built Module

Once built and installed to your local Maven repository, you can use it in your Mule applications by adding the dependency to your project's `pom.xml`:

```xml
<dependency>
    <groupId>au.com.apiled.module.plugins</groupId>
    <artifactId>advanced-logger</artifactId>
    <version>1.0.2-SNAPSHOT</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Verify Installation

To verify the module is installed in your local Maven repository, check:

```bash
# Windows
%USERPROFILE%\.m2\repository\au\com\apiled\module\plugins\advanced-logger\1.0.2-SNAPSHOT\

# Linux/Mac
~/.m2/repository/au/com/apiled/module/plugins/advanced-logger/1.0.2-SNAPSHOT/
```

### Build with Code Coverage

To generate code coverage reports using JaCoCo:

```bash
mvn clean install
```

Coverage reports will be available at:

```
target/site/jacoco/index.html
```

## Configuration

### Requirements

- Mule Runtime 4.9.8 or higher
- Java 17

## Usage

### Basic Logging

The simplest usage with a category and log level:

```xml
<advanced-logger:logger
    tag="NONE"
    level="DEBUG"
    category="my.app.logger"
    doc:name="Basic Logger"/>
```

### Transaction Timing

Automatically measure transaction execution time by using START and END tags:

```xml
<flow name="myFlow">
    <!-- Start timing -->
    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        category="my.app.start"
        doc:name="Start Logger"/>

    <!-- Your business logic here -->
    <set-payload value='#["Processing..."]'/>

    <!-- End timing - automatically calculates duration -->
    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        category="my.app.end"
        message="#[payload]"
        doc:name="End Logger"/>
</flow>
```

The logger automatically tracks:

- Start timestamp
- End timestamp
- Time taken (in milliseconds)
- Transaction ID

### Subflow Timing

Track subflow execution independently by setting `isSubflow` to true:

```xml
<sub-flow name="mySubFlow">
    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        isSubflow="true"
        category="my.app.subflow.start"
        doc:name="Subflow Start"/>

    <!-- Subflow logic -->

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        isSubflow="true"
        category="my.app.subflow.end"
        doc:name="Subflow End"/>
</sub-flow>
```

### Custom Properties

Add custom key-value properties for enhanced logging and debugging:

```xml
<advanced-logger:logger
    tag="START"
    level="DEBUG"
    category="my.app.logger">
    <advanced-logger:custom-properties>
        <advanced-logger:custom-property
            key="orderId"
            value="#[payload.orderId]"/>
        <advanced-logger:custom-property
            key="customerId"
            value="#[vars.customerId]"/>
        <advanced-logger:custom-property
            key="region"
            value="US-WEST"/>
    </advanced-logger:custom-properties>
</advanced-logger:logger>
```

Custom properties are:

- Stored in the `advancedLoggerBaseAttributes` variable
- Propagated throughout the flow
- Available in all subsequent logger calls
- Support DataWeave expressions

### HTTP Integration

The logger automatically extracts and propagates correlation IDs from HTTP headers.

#### Incoming HTTP Request

```xml
<flow name="httpListenerFlow">
    <http:listener
        config-ref="HTTP_Listener_config"
        path="/api/orders">
        <http:response>
            <http:headers><![CDATA[#[output application/java
---
{
    "xtransaction-id": vars.advancedLoggerBaseAttributes.transactionId
}]]]></http:headers>
        </http:response>
    </http:listener>

    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        category="api.orders.start"/>

    <!-- Process request -->

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        category="api.orders.end"/>
</flow>
```

The logger looks for the `xtransaction-id` header. If present, it uses that ID; otherwise, it generates a new correlation ID.

#### Outgoing HTTP Request

Propagate the correlation ID to downstream services:

```xml
<http:request
    method="POST"
    config-ref="HTTP_Request_config"
    path="/downstream/api">
    <http:headers><![CDATA[#[output application/java
---
{
    "xtransaction-id": vars.advancedLoggerBaseAttributes.transactionId
}]]]></http:headers>
</http:request>
```

### JMS Integration

The logger supports correlation ID propagation via JMS message properties.

#### Publishing to JMS

```xml
<jms:publish
    config-ref="JMS_Config"
    destination="my.queue">
    <jms:message>
        <jms:properties><![CDATA[#[{
            "xtransaction_id": vars.advancedLoggerBaseAttributes.transactionId
        }]]]></jms:properties>
    </jms:message>
</jms:publish>
```

#### Consuming from JMS

```xml
<flow name="jmsListenerFlow">
    <jms:listener
        config-ref="JMS_Config"
        destination="my.queue"/>

    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        category="jms.consumer.start">
        <advanced-logger:custom-properties>
            <advanced-logger:custom-property
                key="messageId"
                value="#[attributes.messageId]"/>
        </advanced-logger:custom-properties>
    </advanced-logger:logger>

    <!-- Process message -->

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        category="jms.consumer.end"/>
</flow>
```

The logger automatically extracts the `xtransaction_id` property from JMS messages.

## Attributes

### Logger Component Attributes

| Attribute   | Type    | Required | Default | Description                                       |
| ----------- | ------- | -------- | ------- | ------------------------------------------------- |
| `tag`       | String  | No       | NONE    | Controls logging behavior: START, END, NONE, MAIN |
| `level`     | String  | No       | INFO    | Log level: TRACE, DEBUG, INFO, WARN, ERROR        |
| `category`  | String  | Yes      | -       | Logger category for filtering and organization    |
| `message`   | String  | No       | -       | Custom message (supports DataWeave expressions)   |
| `isSubflow` | Boolean | No       | false   | Set to true when logging in subflows              |
| `event`     | String  | No       | -       | Custom event name                                 |
| `action`    | String  | No       | -       | Custom action name                                |

### Tag Values

- **START**: Marks the beginning of a transaction or subflow, captures start timestamp
- **END**: Marks the end of a transaction or subflow, calculates time taken
- **NONE**: General logging without timing
- **MAIN**: Special tag for main flow initialization

### AdvancedLoggerBaseAttributes Variable

The logger stores tracking data in a flow variable named `advancedLoggerBaseAttributes` with the following structure:

```java
{
    transactionId: String,      // Correlation ID
    start: Date,                // Start timestamp
    end: Date,                  // End timestamp
    timeTaken: Long,            // Duration in milliseconds
    customProperties: Map,      // Custom key-value pairs
    event: String,              // Event name
    action: String,             // Action name
    startSubflow: Date,         // Subflow start timestamp
    endSubflow: Date            // Subflow end timestamp
}
```

Access these attributes using DataWeave:

```
#[vars.advancedLoggerBaseAttributes.transactionId]
#[vars.advancedLoggerBaseAttributes.timeTaken]
#[vars.advancedLoggerBaseAttributes.customProperties.orderId]
```

## Examples

### Example 1: Simple Flow with Timing

```xml
<flow name="orderProcessingFlow">
    <http:listener config-ref="HTTP_Listener_config" path="/orders"/>

    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        category="order.processing.start"/>

    <set-variable
        variableName="orderId"
        value="#[payload.orderId]"/>

    <advanced-logger:logger
        tag="NONE"
        level="DEBUG"
        category="order.validation"
        message="Validating order">
        <advanced-logger:custom-properties>
            <advanced-logger:custom-property
                key="orderId"
                value="#[vars.orderId]"/>
        </advanced-logger:custom-properties>
    </advanced-logger:logger>

    <!-- Business logic -->

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        category="order.processing.end"
        message="Order processed successfully"/>
</flow>
```

### Example 2: Multi-Flow Transaction Tracking

```xml
<!-- Flow 1: Main API -->
<flow name="mainApiFlow">
    <http:listener config-ref="HTTP_Listener_config" path="/api/process">
        <http:response>
            <http:headers><![CDATA[#[output application/java
---
{
    "xtransaction-id": vars.advancedLoggerBaseAttributes.transactionId
}]]]></http:headers>
        </http:response>
    </http:listener>

    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        category="main.api.start"/>

    <!-- Call downstream service -->
    <http:request
        method="POST"
        config-ref="HTTP_Request_config"
        path="/downstream">
        <http:headers><![CDATA[#[output application/java
---
{
    "xtransaction-id": vars.advancedLoggerBaseAttributes.transactionId
}]]]></http:headers>
    </http:request>

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        category="main.api.end"/>
</flow>

<!-- Flow 2: Downstream Service -->
<flow name="downstreamServiceFlow">
    <http:listener config-ref="HTTP_Listener_config" path="/downstream"/>

    <!-- Uses same transaction ID from header -->
    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        category="downstream.start"/>

    <!-- Process -->

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        category="downstream.end"/>
</flow>
```

### Example 3: JMS Message Processing

```xml
<flow name="jmsPublisherFlow">
    <http:listener config-ref="HTTP_Listener_config" path="/publish"/>

    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        category="jms.publisher.start">
        <advanced-logger:custom-properties>
            <advanced-logger:custom-property
                key="messageType"
                value="ORDER_CREATED"/>
        </advanced-logger:custom-properties>
    </advanced-logger:logger>

    <jms:publish config-ref="JMS_Config" destination="orders.queue">
        <jms:message>
            <jms:properties><![CDATA[#[{
                "xtransaction_id": vars.advancedLoggerBaseAttributes.transactionId
            }]]]></jms:properties>
        </jms:message>
    </jms:publish>

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        category="jms.publisher.end"/>
</flow>

<flow name="jmsConsumerFlow">
    <jms:listener config-ref="JMS_Config" destination="orders.queue"/>

    <!-- Automatically picks up transaction ID from JMS properties -->
    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        category="jms.consumer.start">
        <advanced-logger:custom-properties>
            <advanced-logger:custom-property
                key="jmsMessageId"
                value="#[attributes.messageId]"/>
        </advanced-logger:custom-properties>
    </advanced-logger:logger>

    <!-- Process message -->

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        category="jms.consumer.end"
        message="Message processed"/>
</flow>
```

### Example 4: Subflow Timing

```xml
<flow name="mainFlow">
    <http:listener config-ref="HTTP_Listener_config" path="/process"/>

    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        category="main.flow.start"/>

    <flow-ref name="dataValidationSubflow"/>
    <flow-ref name="dataTransformationSubflow"/>

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        category="main.flow.end"/>
</flow>

<sub-flow name="dataValidationSubflow">
    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        isSubflow="true"
        category="validation.subflow.start"/>

    <!-- Validation logic -->

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        isSubflow="true"
        category="validation.subflow.end"
        message="Validation completed"/>
</sub-flow>

<sub-flow name="dataTransformationSubflow">
    <advanced-logger:logger
        tag="START"
        level="DEBUG"
        isSubflow="true"
        category="transform.subflow.start"/>

    <!-- Transformation logic -->

    <advanced-logger:logger
        tag="END"
        level="DEBUG"
        isSubflow="true"
        category="transform.subflow.end"
        message="Transformation completed"/>
</sub-flow>
```

## Log Output Format

### With DEBUG or TRACE Level

```
logger.start  transactionId=1234-5678-90ab-cdef start=2025-01-15T10:30:45.123 end=null  customProperties={orderId=12345, customerId=67890}  message=Processing order
 variables={orderId=12345, customerId=67890}
 MuleMessage=org.mule.runtime.core.internal.message.DefaultMessageBuilder$MessageImplementation@abc123
 payload={"order": "data"}
```

### With INFO, WARN, or ERROR Level

```
logger.start  transactionId=1234-5678-90ab-cdef start=2025-01-15T10:30:45.123 end=2025-01-15T10:30:46.456 timeTaken=1333ms  customProperties={orderId=12345}
```

## Best Practices

1. **Use Meaningful Categories**: Choose descriptive category names that help with log filtering and organization

   ```xml
   category="order.api.create.start"
   ```

2. **Always Pair START and END**: For accurate timing, always use START and END tags in pairs

3. **Propagate Transaction IDs**: Always pass correlation IDs when calling external services

4. **Use Custom Properties**: Add relevant business context to logs for better debugging

5. **Set Appropriate Log Levels**:

   - `ERROR`: For errors and exceptions
   - `WARN`: For warnings
   - `INFO`: For important business events (production default)
   - `DEBUG`: For detailed debugging information
   - `TRACE`: For very detailed trace information

6. **Subflow Timing**: Set `isSubflow="true"` when logging in subflows to track them independently

## Troubleshooting

### Transaction ID Not Propagating

Ensure you're passing the transaction ID in HTTP headers or JMS properties:

- HTTP: Use header name `xtransaction-id`
- JMS: Use property name `xtransaction_id`

### Timing Not Calculated

Make sure:

- You have both START and END tags
- The END tag is reached in the flow
- You're accessing the correct variable: `vars.advancedLoggerBaseAttributes.timeTaken`

### Custom Properties Not Appearing

Verify:

- Custom properties are defined before the END tag
- DataWeave expressions are valid
- Variables referenced in expressions exist

## License

Copyright (c) 2025 API-Led Pty Ltd

## Support

For issues, questions, or contributions, please contact API-Led Pty Ltd.
https://www.api-led.com.au

---

**Version**: 1.0.2-SNAPSHOT
**Vendor**: API-Led Pty Ltd
**Mule Version**: 4.9.8+
**Java Version**: 17
