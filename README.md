# Adapter selection
## The current version implements three adapters for ram2 
- receiver: used ram-receiver
- kafka: used kafka connect
- standalone: used atp-ram

## You can select the one you need with java option ram.adapter.type
```
-Dram.adapter.type=receiver
-Dram.adapter.type=kafka
-Dram.adapter.type=standalone
```
Default value
```
ram.adapter.type=receiver
```

## If you used kafka adapter then you need set the bootstrap.server java option
```
-Dbootstrap.servers=kafka:9092
-Dkafka.topic.name=messages
```
Default value
```
bootstrap.servers=kafka:9092
kafka.topic.name=messages
```
##
There should be special built virtual machine(s) for building of docker images with robot-framework^
repository-path/atp/robot-framework/vm-for-atp-robot-centos:v1   - this centos official VM from internal artifactory repository
repository-path/atp/robot-framework/vm-for-atp-robot:v1     - this ppodgorsek/robot-framework image from docker hub 

Even version (1.0.0, 1.0.2, etc) use for ethalon publication
Odd version (1.0.1, 1.0.3, etc) use for adapter publication

## Logging business ids
Default list of business ids:
```
userId,projectId,executionRequestId,testRunId,bvTestRunId,bvTestCaseId,environmentId,
systemId,subscriberId,tsgSessionId,svpSessionId,dataSetId,dataSetListId,attributeId,
itfLiteRequestId,reportType,itfSessionId,itfContextId,callChainId
```
Parameter for setting business  ids:
```
atp.logging.business.keys=userId,projectId
```
