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

package org.qubership.atp.adapter.common.adapters;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.adapter.common.AtpRamAdapter;
import org.qubership.atp.adapter.common.adapters.providers.RamAdapterProvider;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.common.utils.Config;
import org.qubership.atp.adapter.common.utils.ExecutionRequestHelper;
import org.qubership.atp.adapter.common.ws.StartRunRequest;
import org.qubership.atp.ram.models.MetaInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import net.sf.json.JSONObject;

public class AtpStandaloneRamAdapterTest {

    private AtpRamAdapter adapter;

    private AtpRamAdapter initAdapter() {
        Config.getConfig().setProperty("ram.adapter.type", "standalone");
        if (this.adapter == null) {
            String testRunName = "TestRunName";
            this.adapter = RamAdapterProvider.getNewAdapter(testRunName);
        }
        return this.adapter;
    }

    //Input params
    String projectName = "Project";
    String testPlanName = "Test Plan";
    UUID environmentId = UUID.fromString("b26f2624-c3c9-49ce-8961-ab1288010fc3");

    public TestRunContext createContextForTestRun(String erName, String testRunName, String suiteName) {
        AtpCompaund compaund = new AtpCompaund();
        UUID testRunId = UUID.randomUUID();
        UUID erId = UUID.randomUUID();
        UUID testScopeId = UUID.randomUUID();

        TestRunContext ram2Context = TestRunContextHolder.getContext(testRunName);
        ram2Context.setTestRunId(testRunId.toString());
        ram2Context.setProjectName(projectName);
        ram2Context.setTestPlanName(testPlanName);
        ram2Context.setAtpTestRunId(testRunName);
        ram2Context.setExecutionRequestName(erName);
        ram2Context.setTestRunName(testRunName);
        ram2Context.setAtpExecutionRequestId(erId.toString());
        ram2Context.setTestSuiteName(suiteName);
        ram2Context.setTestCaseName(testRunName);
        ram2Context.setCompoundAndUpdateCompoundStatuses(compaund);
        ram2Context.setTestScopeId(testScopeId.toString());
        ram2Context.setEnvironmentId(environmentId.toString());
//        ram2Context.setTestPlanId(testPlanId.toString());
//        ram2Context.setProjectId(projectId.toString());

        return ram2Context;
    }

    private StartRunRequest createRequestByContext(TestRunContext context) {
        return StartRunRequest.getRequestBuilder()
                .setProjectId(UUID.fromString(context.getProjectId()))
                .setTestPlanId(UUID.fromString(context.getTestPlanId()))
                .setProjectName(context.getProjectName())
                .setTestPlanName(context.getTestPlanName())
                .setTestCaseName(context.getTestCaseName())
                .setTestSuiteName(context.getTestSuiteName())
                .setExecutionRequestName(context.getExecutionRequestName())
                .setTestRunName(context.getTestRunName())
                .setSolutionBuild(ExecutionRequestHelper.getSolutionBuild(context.getQaHost()))
                .setQaHost(context.getQaHost())
                .setStartDate(ExecutionRequestHelper.getCurrentTimestamp())
                .setExecutor(ExecutionRequestHelper.getOsUser())
                .setTaHost(ExecutionRequestHelper.getHostName())
                .setAtpExecutionRequestId(UUID.fromString(context.getAtpExecutionRequestId()))
                .setTestScopeId(UUID.fromString(context.getTestScopeId()))
                .setEnvironmentId(UUID.fromString(context.getEnvironmentId()))
                .build();
    }

    @Data
    static public class MessageTree extends Message {
        List<MessageTree> child;
    }

    @Data
    static public class Scenario {
        MetaInfo metaInfo;
        String testCaseId;
        List<MessageTree> trees;
    }

    // The following method is commented (@Test annotation is commented) since 07/08/2020 - just since class creation
    //    @Test
    public void logsToNewRam() throws IOException {
        /* Is replaced from:
            String jsonRequest = new String(
                Files.readAllBytes(Paths.get("./src/test/resources", "logRecordsTree.json")));
            due to Stored_Absolute_Path_Traversal and Input_Path_Not_Canonicalized vulnerabilities
         */
        String jsonRequest = "{ \"testCaseId\": \"257794f0-8c6e-4e01-a95d-bbebf6218a31\",\n"
                + "  \"metaInfo\": {\n"
                + "    \"scenarioHashSum\": \"ac458c1b88d799676536b4e673f83360\"\n"
                + "  },\n"
                + "  \"trees\": [\n"
                + "    {\n"
                + "      \"logRecordId\": null,\n"
                + "      \"parentRecordId\": null,\n"
                + "      \"name\": \"section 1\",\n"
                + "      \"message\": null,\n"
                + "      \"testingStatus\": \"Unknown\",\n"
                + "      \"metaInfo\": {\n"
                + "        \"scenarioId\": \"654de161-3ca5-44c3-bbd7-33263665fe8a\",\n"
                + "        \"scenarioHashSum\": \"a6d1f65664a59a753c8524f7d8f04d2a\",\n"
                + "        \"line\": 1\n"
                + "      },\n"
                + "      \"configInfo\": null,\n"
                + "      \"child\": [\n"
                + "        {\n"
                + "          \"logRecordId\": null,\n"
                + "          \"parentRecordId\": null,\n"
                + "          \"name\": \"message 1\",\n"
                + "          \"message\": \"there is a body for message 1\",\n"
                + "          \"testingStatus\": \"Unknown\",\n"
                + "          \"metaInfo\": null,\n"
                + "          \"configInfo\": null,\n"
                + "          \"child\": []\n"
                + "        },\n"
                + "        {\n"
                + "          \"logRecordId\": null,\n"
                + "          \"parentRecordId\": null,\n"
                + "          \"name\": \"message 2\",\n"
                + "          \"message\": \"there is a body for message 2\",\n"
                + "          \"testingStatus\": \"Unknown\",\n"
                + "          \"metaInfo\": null,\n"
                + "          \"configInfo\": null,\n"
                + "          \"child\": []\n"
                + "        },\n"
                + "        {\n"
                + "          \"logRecordId\": null,\n"
                + "          \"parentRecordId\": null,\n"
                + "          \"name\": \"section 2\",\n"
                + "          \"message\": null,\n"
                + "          \"testingStatus\": \"Unknown\",\n"
                + "          \"metaInfo\": {\n"
                + "            \"scenarioId\": \"b4a9af89-3dc2-4daa-95b5-65e60f079458\",\n"
                + "            \"scenarioHashSum\": \"scenarioIdHashSum_section2metainfo\",\n"
                + "            \"line\": 2\n"
                + "          },\n"
                + "          \"configInfo\": null,\n"
                + "          \"child\": [\n"
                + "            {\n"
                + "              \"logRecordId\": null,\n"
                + "              \"parentRecordId\": null,\n"
                + "              \"name\": \"message 1 from section 2\",\n"
                + "              \"message\": \"there is a body for message 1 from section 2\",\n"
                + "              \"testingStatus\": \"Unknown\",\n"
                + "              \"metaInfo\": null,\n"
                + "              \"configInfo\": null,\n"
                + "              \"child\": []\n"
                + "            },\n"
                + "            {\n"
                + "              \"logRecordId\": null,\n"
                + "              \"parentRecordId\": null,\n"
                + "              \"name\": \"message 2 from section 2\",\n"
                + "              \"message\": \"there is a body for message 2 from section 2\",\n"
                + "              \"testingStatus\": \"Unknown\",\n"
                + "              \"metaInfo\": null,\n"
                + "              \"configInfo\": null,\n"
                + "              \"child\": []\n"
                + "            }\n"
                + "          ]\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ]\n"
                + "}\n";

        Scenario scenario = new ObjectMapper().readValue(jsonRequest, Scenario.class);

        initAdapter();

        String erName = "Execution Req - " + System.currentTimeMillis();
        String testRunName = "Test - " + System.currentTimeMillis();
        String testSuiteName = "Test Suite ";

        TestRunContext testRun1Context = createContextForTestRun(erName, testRunName, testSuiteName);
        StartRunRequest request1 = createRequestByContext(testRun1Context);
        request1.setMetaInfo(scenario.getMetaInfo());
        request1.setTestCaseId(UUID.fromString(scenario.getTestCaseId()));
        outputContextInLog("before start", testRun1Context);
        adapter.startAtpRun(request1, testRun1Context);
        {
            outputContextInLog("after start", testRun1Context);
            logScenario(scenario.trees);
        }
        String testRunId = testRun1Context.getTestRunId();
        adapter.stopAtpRun(testRunId);
    }

    private void logScenario(List<MessageTree> trees) {
        for (MessageTree tree : trees) {
            boolean isSection = tree.child != null && !tree.child.isEmpty();
            JSONObject pureMessageJson = new ObjectMapper().convertValue(tree, JSONObject.class);
            pureMessageJson.remove("child");
            Message pureMessage = new ObjectMapper().convertValue(pureMessageJson, Message.class);
            if (isSection) {
                adapter.openSection(pureMessage);
                logScenario(tree.child);
                adapter.closeSection();
            } else {
                adapter.message(pureMessage);
            }
        }
    }

    private void outputContextInLog(String stage, TestRunContext testRun1Context) {
        System.out.println(stage);
        System.out.println("\tTestRunId: " + testRun1Context.getTestRunId());
        System.out.println("\tExecutionRequestId: " + testRun1Context.getExecutionRequestId());
    }


//TEST Run
//POST: /api/executor/testruns/create
//POST: /api/executor/testruns/stop
//POST: /api/executor/testruns/updateOrCreate
//POST: /api/executor/testruns/upload/{id}/requestResponse


//Log Record
//POST: /api/executor/logrecords/findOrCreate
//POST: /api/executor/logrecords/upload/{id}/requestResponse
//POST: /api/executor/logrecords/upload/{id}/stream

// Config Info
//POST: /api/executor/configinfo/saveConfigInfo
}
