# Qubership Testing Platform Adapter-Robot Library

## Purpose

This library is used to send reports about tests execution results from so called 'TA Tools' (Qubership Testing Platform services performing test scenarios execution) into Qubership Testing Platform RAM service.
It performs this sending via adapters of various types (see below) by means of composing 'log messages' of special format, which contain all related information.

## Local build

In IntelliJ IDEA, one can select 'github' Profile in Maven Settings menu on the right, then expand Lifecycle dropdown of qubership-atp-adapter-robot-aggregator module, then select 'clean' and 'install' options and click 'Run Maven Build' green arrow button on the top.

Or, one can execute the command:
```bash
mvn -P github clean install
```

## How to add dependency into a service
```xml
    <!-- Change version number if necessary -->
    <dependency>
        <groupId>org.qubership.atp.adapter.robot</groupId>
        <artifactId>qubership-atp-adapter-common</artifactId>
        <version>4.5.71-SNAPSHOT</version>
    </dependency>
```

## Adapter selection
### The current version implements three adapters for ram2
- receiver: uses ram-receiver
- kafka: uses kafka connect
- standalone: uses atp-ram

### You can select the one you need with java option ram.adapter.type
```text
-Dram.adapter.type=receiver
or
-Dram.adapter.type=kafka
or
-Dram.adapter.type=standalone
```
Default value
```properties
ram.adapter.type=receiver
```

### If you use kafka adapter then you need set the bootstrap.server java option
```bash
-Dbootstrap.servers=kafka:9092
-Dkafka.topic.name=messages
```
Default value
```properties
bootstrap.servers=kafka:9092
kafka.topic.name=messages
```
### Special builds
There should be special built virtual machine(s) for building of docker images with robot-framework^
 - repository-path/atp/robot-framework/vm-for-atp-robot-centos:v1   - this is centos official VM from internal artifactory repository
 - repository-path/atp/robot-framework/vm-for-atp-robot:v1     - this is ppodgorsek/robot-framework image from docker hub

Versions numbering
- Even version (1.0.0, 1.0.2, etc.) use for ethalon publication
- Odd version (1.0.1, 1.0.3, etc.) use for adapter publication

### Logging business IDs
Default list of business IDs:
```text
userId,projectId,executionRequestId,testRunId,bvTestRunId,bvTestCaseId,environmentId,
systemId,subscriberId,tsgSessionId,svpSessionId,dataSetId,dataSetListId,attributeId,
itfLiteRequestId,reportType,itfSessionId,itfContextId,callChainId
```
Parameter to set business IDs:
```properties
atp.logging.business.keys=userId,projectId
```
