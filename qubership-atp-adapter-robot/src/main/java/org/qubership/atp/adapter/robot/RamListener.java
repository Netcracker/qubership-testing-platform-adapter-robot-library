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

package org.qubership.atp.adapter.robot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import org.qubership.atp.adapter.common.AtpRamAdapter;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.adapters.AtpReceiverRamAdapter;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.adapter.common.utils.Config;
import org.qubership.atp.adapter.common.utils.ExecutionRequestHelper;
import org.qubership.atp.adapter.common.ws.StartRunRequest;
import org.qubership.atp.adapter.robot.utils.ScreenShotHelper;

public class RamListener implements ListenerInterface {

    private static final Logger log = LoggerFactory.getLogger(RamListener.class);
    private ArrayList<Object> messagesCount;
    private String paBotPoolId = null;
    private String outPutDir = null;
    private AtpRamAdapter atpRamAdapter;
    private String testRunId;
    private String executionRequestUuid;
    private Stack<String> sectionIds;
    private Map<String, Integer> sectionsCounter;
    private String currentSectionId = "";

    private String atpProjectName;
    private String atpTestPlanName;
    private String recipients;
    private String suiteName;

    private TestRunContext context;

    private String executionRequestName;

    /**
     * Listener for RobotFramework allow logging into Atp Ram.
     */
    public RamListener() {
        init();
    }

    private void init() {
        atpRamAdapter = new AtpReceiverRamAdapter();
        Config cfg = Config.getConfig();
        atpProjectName = cfg.getProperty("atp.project",
                cfg.getProperty("atp.project", "Default"));
        atpTestPlanName = cfg.getProperty("atp.project.testplan",
                cfg.getProperty("atp.project.testplan", "Default"));
        recipients = cfg.getProperty("atp.project.recipients", "");
        executionRequestName = ExecutionRequestHelper.generateRequestName();
    }

    @Override
    public void startSuite(String name, Map attributes) {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");
            engine.eval("from robot.libraries.BuiltIn import BuiltIn");
            engine.eval("PabotPoolId =  BuiltIn().get_variable_value('${PABOTEXECUTIONPOOLID}') ");
            engine.eval("OutPutDir =  BuiltIn().get_variable_value('${OUTPUT_DIR}')");

            paBotPoolId = (String) engine.get("PabotPoolId");
            outPutDir = (String) engine.get("OutPutDir");
            Config.getConfig().setProperty("atp.test_suite", name);
            suiteName = name;
        } catch (ScriptException se) {
            log.error("Error inn Start Suite", se);
        }
    }

    @Override
    public void endSuite(String name, Map attributes) {
    }

    @Override
    public void startTest(String name, Map attributes) {
        sectionIds = new Stack<>();
        sectionsCounter = new HashMap<>();

        String serverAlias = Config.getConfig().getProperty("server.alias", "server");
        String serverUrl = Config.getConfig().getProperty(serverAlias + ".host", "");
        recipients = Config.getConfig().getProperty("atp.project.recipients", "");
        context = TestRunContextHolder.getContext(name);
        StartRunRequest request = StartRunRequest.getRequestBuilder()
                .setProjectName(atpProjectName)
                .setTestPlanName(atpTestPlanName)
                .setTestSuiteName(suiteName)
                .setTestCaseName(name)
                .setTestRunName(name + "_" + LocalDateTime.now().toString())
                .setExecutionRequestName(executionRequestName)
                .setQaHost(serverUrl)
                .setSolutionBuild(ExecutionRequestHelper.getSolutionBuild(serverUrl))
                .setMailList(recipients)
                .build();
        context = atpRamAdapter.startAtpRun(request, context);
        testRunId = context.getTestRunId();
        executionRequestUuid = context.getExecutionRequestId();
        currentSectionId = "";
        sectionIds.push(testRunId);

        log.debug("Start test: " + context.getTestRunId());
    }

    @Override
    public void endTest(String name, Map attributes) {
        atpRamAdapter.stopAtpRun(context.getTestRunId());
    }

    @Override
    public void startKeyword(String name, Map attributes) {
        String kwname = (String) attributes.get("kwname");
        kwname = StringEscapeUtils.escapeHtml3(kwname);
        messagesCount = new ArrayList<>();
        incrementCounter();
        context = atpRamAdapter.openSection(kwname, name, RamConstants.PASSED);
        incrementCounter();
    }

    private void incrementCounter() {
        if (!sectionIds.isEmpty()) {
            String sectionId = context.getCurrentSectionId();
            if (sectionsCounter.get(sectionId) != null) {
                int newValue = sectionsCounter.get(sectionId) + 1;
                sectionsCounter.put(sectionId, newValue);
            } else {
                sectionsCounter.put(sectionId, 0);
            }
        }
    }

    @Override
    public void endKeyword(String name, Map attributes) {
        int sectionCounter = sectionsCounter.get(context.getCurrentSectionId());
        if (sectionCounter < 1) {
            printMessage("Log Message", attributes);
        }
        atpRamAdapter.closeSection();
    }

    private int getCounter() {
        if (!sectionIds.isEmpty()) {
            String currentSection = sectionIds.peek();
            if (sectionsCounter.get(currentSection) != null) {
                return sectionsCounter.get(currentSection);
            }
        }
        return 0;
    }

    @Override
    public void logMessage(Map message) throws IOException {
        ScreenShotHelper helper = new ScreenShotHelper();
        String messageText = (String) message.get("message");
        String fileName = helper.extractImage(messageText);
        if (!Strings.isNullOrEmpty(fileName)) {
            File screenShot = helper.getScreenshotFile(fileName, paBotPoolId, outPutDir);
            String b64image = Base64.getEncoder().encodeToString(Files.readAllBytes(screenShot.toPath()));
            message.put("screenshot_name", fileName);
            message.put("screenshot_file", b64image);
        }
        try {
            incrementCounter();
            printMessage("Log Message", message);
        } catch (Exception e) {
            log.error("Error in LogMessage", e);
        }

    }


    @Override
    public void message(Map message) {
    }

    @Override
    public void outputFile(String path) {
    }

    @Override
    public void logFile(String path) {
    }

    @Override
    public void reportFile(String path) {
    }

    @Override
    public void debugFile(String path) {

    }

    private void printMessage(String name, Map attributes) {
        try {
            String status;
            if (attributes.get("status") != null) {
                status = (String) attributes.get("status");
            } else {
                status = (String) attributes.get("level");
            }

            String message = Strings.nullToEmpty((String) attributes.get("message"));

            String atpStatus;
            if (STATUS_PASS.equalsIgnoreCase(status) || "INFO".equalsIgnoreCase(status)) {
                atpStatus = "PASSED";
            } else {
                atpStatus = "FAILED";
            }
            atpRamAdapter.message(context.getCurrentSectionId(), name, message, atpStatus, attributes);
        } catch (Exception e) {
            log.error("Error in PrintMessage", e);
        }
    }

    @Override
    public void close() {
        atpRamAdapter.sendRamReportImmediately(executionRequestUuid);
    }
}
