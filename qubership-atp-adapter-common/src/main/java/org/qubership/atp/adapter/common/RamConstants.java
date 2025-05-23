/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.qubership.atp.adapter.common;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface RamConstants {

    String NULL_VALUE = "null";
    String UNKNOWN = "UNKNOWN";
    String PASSED = "PASSED";
    String DEFAULT_ADAPTER_TYPE = "receiver";
    String RECEIVER_ADAPTER_TYPE = "receiver";
    String IMPORTER_ADAPTER_TYPE = "importer";
    String KAFKA_ADAPTER_TYPE = "kafka";
    String STANDALONE_ADAPTER_TYPE = "standalone";
    String DEFAULT_MESSAGE_TOPIC_NAME = "messages";
    String DEFAULT_LR_TOPIC_PARTITIONS_NUMBER = "1";
    String DEFAULT_LR_TOPIC_REPLICATION_FACTOR = "1";
    String DEFAULT_BROWSER_LOG_TOPIC_NAME = "logrecord_browser_log_topic";
    String DEFAULT_BROWSER_LOG_TOPIC_PARTITIONS_NUMBER = "1";
    String DEFAULT_BROWSER_LOG_TOPIC_REPLICATION_FACTOR = "1";
    String DEFAULT_CONSOLE_LOG_TOPIC_NAME = "logrecord_scripts_topic";
    String DEFAULT_CONSOLE_LOG_TOPIC_PARTITIONS_NUMBER = "1";
    String DEFAULT_CONSOLE_LOG_TOPIC_REPLICATION_FACTOR = "1";
    String DEFAULT_LR_CONTEXT_TOPIC_NAME = "logrecord_context_topic";
    String DEFAULT_LR_CONTEXT_TOPIC_PARTITIONS_NUMBER = "1";
    String DEFAULT_LR_CONTEXT_TOPIC_REPLICATION_FACTOR = "1";
    String DEFAULT_LR_STEP_CONTEXT_TOPIC_NAME = "logrecord_step_context_topic";
    String DEFAULT_LR_STEP_CONTEXT_TOPIC_PARTITIONS_NUMBER = "1";
    String DEFAULT_LR_STEP_CONTEXT_TOPIC_REPLICATION_FACTOR = "1";
    String DEFAULT_LR_MESSAGE_PARAMETERS_TOPIC_NAME = "logrecord_message_parameters_topic";
    String DEFAULT_LR_MESSAGE_PARAMETERS_TOPIC_PARTITIONS_NUMBER = "1";
    String DEFAULT_LR_MESSAGE_PARAMETERS_TOPIC_REPLICATION_FACTOR = "1";
    String DEFAULT_ENV_INFO_TOPIC_NAME = "envInfo";
    String DEFAULT_ENV_INFO_UPDATE_TOPIC_NAME = "envInfoSetToolsInfo";
    String DEFAULT_TOOLS_INFO_TOPIC_NAME = "toolsInfo";
    String DEFAULT_DETAILS_TOPIC_NAME = "details";
    String DEFAULT_CONFIG_FILES_TOPIC_NAME = "configinfo";
    String DEFAULT_BOOTSTRAP_SERVERS = "kafka:9092";
    int DEFAULT_LOGRECORD_BATCH_SIZE = 50;
    Long DEFAULT_LOGRECORD_BATCHING_TIMEOUT = 10000L;
    int LOGRECORD_BATCHING_SENDER_TASK_TIMEOUT = 3;//hours
    String UUID = "uuid";
    String UPDATE_TESTING_STATUSES = "updTestingStatus";
    String KAFKA_PRODUCERS_POOL_MAX_TOTAL_PER_KEY = "kafka.producers.pool.maxTotalPerKey";
    String KAFKA_PRODUCERS_POOL_MAX_WAIT_MILLIS = "kafka.producers.pool.maxWaitMillis";

    String MESSAGE_TOPIC_NAME_KEY = "kafka.topic.name";
    String LR_TOPIC_PARTITIONS_NUMBER = "kafka.logrecord.topic.partitions.number";
    String LR_TOPIC_REPLICATION_FACTOR = "kafka.logrecord.topic.replication.factor";
    String LR_CONTEXT_TOPIC_NAME_KEY = "kafka.logrecord.context.topic.name";
    String LR_CONTEXT_TOPIC_PARTITIONS_NUMBER = "kafka.logrecord.context.topic.partitions.number";
    String LR_CONTEXT_TOPIC_REPLICATION_FACTOR = "kafka.logrecord.context.topic.replication.factor";

    String BROWSER_LOG_TOPIC_NAME_KEY = "kafka.logrecord.browser.logs.topic.name";
    String BROWSER_LOG_TOPIC_PARTITION_NUMBER = "kafka.logrecord.browser.logs.partitions.number";
    String BROWSER_LOG_TOPIC_REPLICATION_FACTOR = "kafka.logrecord.browser.logs.replication.factor";

    String CONSOLE_LOG_TOPIC_NAME_KEY = "kafka.logrecord.scripts.topic.name";
    String CONSOLE_LOG_TOPIC_PARTITION_NUMBER = "kafka.logrecord.scripts.partitions.number";
    String CONSOLE_LOG_TOPIC_REPLICATION_FACTOR = "kafka.logrecord.scripts.replication.factor";

    String LR_STEP_CONTEXT_TOPIC_NAME_KEY = "kafka.logrecord.step.context.topic.name";
    String LR_STEP_CONTEXT_TOPIC_PARTITIONS_NUMBER = "kafka.logrecord.step.context.topic.partitions.number";
    String LR_STEP_CONTEXT_TOPIC_REPLICATION_FACTOR = "kafka.logrecord.step.context.topic.replication.factor";
    String LR_MESSAGE_PARAMETERS_TOPIC_NAME_KEY = "kafka.logrecord.message.parameters.topic.name";
    String LR_MESSAGE_PARAMETERS_TOPIC_PARTITIONS_NUMBER = "kafka.logrecord.message.parameters.topic.partitions.number";
    String LR_MESSAGE_PARAMETERS_TOPIC_REPLICATION_FACTOR =
            "kafka.logrecord.message.parameters.topic.replication.factor";
    String KAFKA_CONFIG_FILES_TOPIC_NAME_KEY = "kafka.configFiles.topic.name";
    String KAFKA_ENV_INFO_TOPIC_NAME_KEY = "kafka.envInfo.topic.name";
    String KAFKA_ENV_INFO_UPDATE_TOPIC_NAME_KEY = "kafka.envInfo.update.topic.name";
    String KAFKA_TOOLS_INFO_TOPIC_NAME_KEY = "kafka.toolsInfo.topic.name";
    String KAFKA_DETAILS_TOPIC_NAME_KEY = "kafka.details.topic.name";
    int DEFAULT_MAX_REQUEST_SIZE = 15728640;
    String COMPRESSION_TYPE = "lz4";
    String ACTION_PARAMETER_VALUE_SIZE_LIMIT_TO_TRIM_CHARS = "atp.adapter.action-parameter-size-limit-to-trim.chars";
    int DEFAULT_ACTION_PARAMETER_VALUE_SIZE_LIMIT_TO_TRIM_CHARS = 256;
    String ADAPTER_TYPE_KEY = "ram.adapter.type";
    String BUSINESS_IDS_KEYS_KEY = "atp.logging.business.keys";
    String DEFAULT_ER_NAME = "Default ER";
    String TEST_RUN_ID_KEY = "testRunId";
    String RECORD_ID_KEY = "recordId";
    String ATP_RAM_URL_KEY = "atp.ram.url";
    String ATP_RAM_IMPORTER_URL_KEY = "atp.ram.importer.url";
    String ATP_RAM_RECEIVER_URL_KEY = "atp.ram.receiver.url";
    String ATP_RAM_IMPORTER_LOGRECORD_BATCH_SIZE_KEY = "atp.ram.importer.logrecord.batch.size";
    String ATP_RAM_IMPORTER_LOGRECORD_BATCH_TIMEOUT_KEY = "atp.ram.importer.logrecord.batch.timeout";
    String ATP_LOGGER_URL_KEY = "atp.logger.url";
    String LOG_RECORD_ID_KEY = "id";
    String PARENT_RECORD_ID_KEY = "parentId";
    String IS_SECTION_KEY = "isSection";
    String IS_COMPAUND_KEY = "isCompaund";
    String NAME_KEY = "name";
    String MESSAGE_KEY = "message";
    String TYPE_ACTION_KEY = "type";
    String DATA_KEY = "data";
    String CATEGORY_KEY = "category";
    String TESTING_STATUS_KEY = "testingStatus";
    String LOG_COLLECTOR_DATA_KEY = "logCollectorData";
    String START_DATE_KEY = "startDate";
    String FINISH_DATE_KEY = "finishDate";
    String EXECUTION_REQUEST_ID_KEY = "executionRequestId";
    String EXECUTION_REQUEST_UUID_KEY = "executionRequestUuid";
    String PROJECT_ID_KEY = "projectId";
    String TEST_PLAN_ID_KEY = "testPlanId";
    String RECIPIENTS_KEY = "recipients";
    String SCREENSHOT_NAME_KEY = "screenshot_name";
    String SCREENSHOT_FILE_KEY = "screenshot_file";
    String ATTACHMENT_STREAM_KEY = "attachment_stream";
    String SCREENSHOT_TYPE_KEY = "screenshot_type";
    String SCREENSHOT_SOURCE_KEY = "screenshot_source";
    String SCREENSHOT_EXTERNAL_SOURCE_KEY = "screenshot_external_source";
    String URL_TO_BROWSER_OR_LOGS_KEY = "urlToBrowserOrLogs";
    String EXECUTION_STATUS_KEY = "executionStatus";
    String ATP_EXECUTION_REQUEST_ID_KEY = "atpExecutionRequestId";
    String IS_PASSED_KEY = "isPassed";
    String SERVER = "server";
    String FILE_METADATA = "file_metadata";

    String UUID_PLACEHOLDER = "{" + UUID + "}";
    String TESTING_STATUS_PLACEHOLDER = "{" + TESTING_STATUS_KEY + "}";
    String UUID_KEY = "uuid";
    String CONFIG_INFO_ID_KEY = "configInfoId";
    String STATISTIC = "statistic";
    String REPORT_LABELS_PARAMS = "reportLabelParams";

    String API_PATH = "/api";
    String V1_PATH = "/v1";
    String RAM_EXECUTOR_PATH = API_PATH + "/executor";
    String RAM_LOGGING_PATH = API_PATH + "/logging";
    String RAM_RECEIVER_API_PATH = "/api/v1/ram-receiver";
    String TEST_RUNS_PATH = "/testruns";
    String TEST_RUN_PATH = "/testrun";
    String LOG_RECORDS_PATH = "/logrecords";
    String ATTACHMENT_PATH = "/attachment";
    String LOG_RECORD_PATH = "/logrecord";
    String LOGGING_LOG_RECORDS_PATH = "/logRecords";
    String EXECUTION_REQUESTS_PATH = "/executionrequests";
    String ER_PATH = "/er";
    String DETAILS_PATH = "/details";
    String BULK_PATH = "/bulk";
    String DELAYED_PATH = "/delayed";
    String CREATE_PATH = "/create";
    String CREATE_BROWSER_CONSOLE_LOG = "/createBrowserConsoleLog";
    String PATCH_PATH = "/patch";
    String SAVE_PATH = "/save";
    String STOP_PATH = "/stop";
    String FINISH_PATH = "/finish";
    String UPLOAD_PATH = "/upload";
    String STREAM_PATH = "/stream";
    String UPDATE_EXECUTION_STATUS_PATH = "/updateExecutionStatus";
    String UPDATE_TESTING_STATUS = "updateTestingStatus";
    String UPDATE_CONTEXT_VARIABLES_PATH = "/updateContextVariables";
    String UPDATE_STEP_CONTEXT_VARIABLES_PATH = "/updateStepContextVariables";
    String UPDATE_MESSAGE_PARAMETERS_PATH = "/updateMessageParameters";
    String CONTEXT_VARIABLES_PATH = "/contextVariables";
    String STEP_CONTEXT_VARIABLES_PATH = "/stepContextVariables";
    String TOOLS_INFO_PATH = "/toolsInfo";
    String FIND_OR_CREATE_PATH = "/findOrCreate";
    String UPDATE_OR_CREATE_PATH = "/updateOrCreate";
    String UPDATE_PATH = "/update";
    String UUID_PLACEHOLDER_PATH = "/" + UUID_PLACEHOLDER;
    String UPDATE_TESTING_STATUSES_PATH = "/" + UPDATE_TESTING_STATUSES;
    String UPDATE_TESTING_STATUS_PATH = "/" + UPDATE_TESTING_STATUS;
    String ENVIRONMENTS_INFO_PATH = "/environmentsInfo";
    String TESTING_STATUS_PLACEHOLDER_PATh = "/" + TESTING_STATUS_PLACEHOLDER;
    String STATISTIC_PATH = "/" + STATISTIC;
    String REPORT_LABELS_PARAMS_PATH = "/" + REPORT_LABELS_PARAMS;
    String BROWSER_LOGS_PATH = "/createBrowserConsoleLog";

    String TOOL_CONFIG_INFO_PATH = "/toolConfigInfo";
    String SAVE_CONFIGS_PATH = "/saveConfigInfo";

    String ARRAY_PARAMETERS_KEY = "steps";
    String PARAMETER_NAME_KEY = "stepName";
    String TEMPLATE_ID_KEY = "templateId";
    String ER_KEY = "highlightedEr";
    String AR_KEY = "highlightedAr";
    String BV_STATUS_KEY = "compareResult";
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    String UPD_EXECUTION_STATUS_PATH = "/updExecutionStatus";
    String CONTENT_TYPE = "image/png";
    String UTF8_CHARSET = "UTF-8";

    String SUBJECT_KEY = "subject";
}
