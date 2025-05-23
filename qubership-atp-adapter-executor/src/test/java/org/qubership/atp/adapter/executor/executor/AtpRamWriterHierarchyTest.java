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

package org.qubership.atp.adapter.executor.executor;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.qubership.atp.adapter.common.adapters.AtpKafkaRamAdapter;
import org.qubership.atp.adapter.common.adapters.providers.RamAdapterProvider;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.common.kafka.client.KafkaConfigurator;
import org.qubership.atp.adapter.common.kafka.pool.KafkaPoolManagementService;

import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.adapter.report.WebReportItem;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KafkaPoolManagementService.class, AtpKafkaRamAdapter.class, RamAdapterProvider.class,
        TestRunContextHolder.class})
@PowerMockIgnore("javax.management.*")
public class AtpRamWriterHierarchyTest {
    @Spy
    private org.qubership.atp.adapter.executor.executor.AtpRamWriter writer = new AtpRamWriter();
    @Mock
    private KafkaProducer producer;
    @Mock
    private KafkaConfigurator kafkaConfigurator;
    private AtpKafkaRamAdapter adapter;
    private Stack<LogRecord> expectedHierarchyLogRecords = new Stack<>();

    private static final UUID rootSectionId = UUID.randomUUID();
    private static final String rootSectionName = "Root Section";
    private static final UUID section1Id = UUID.randomUUID();
    private static final String section1Name = "Section1";
    private static final UUID logRecord11Id = UUID.randomUUID();
    private static final String logRecord11Name = "LogRecord11";
    private static final UUID logRecord12Id = UUID.randomUUID();
    private static final String logRecord12Name = "LogRecord12";
    private static final UUID section2Id = UUID.randomUUID();
    private static final String section2Name = "Section2";
    private static final UUID section21Id = UUID.randomUUID();
    private static final String section21Name = "Section21";
    private static final UUID logRecord211Id = UUID.randomUUID();
    private static final String logRecord211Name = "LogRecord211";
    private static final UUID logRecord212Id = UUID.randomUUID();
    private static final String logRecord212Name = "LogRecord212";
    private static final UUID rootLogRecordId = UUID.randomUUID();
    private static final String rootLogRecordName = "Root LogRecord";

    private TestRunContext setUp(boolean stepIsLast) throws Exception {
        TestRunContext testRunContext = Utils.createTestRunContext(false);
        testRunContext.setCompoundAndUpdateCompoundStatuses(Utils.createAtpCompaund(stepIsLast));
        whenNew(KafkaProducer.class).withAnyArguments().thenReturn(producer);
        whenNew(KafkaConfigurator.class).withAnyArguments().thenReturn(kafkaConfigurator);

        adapter = Mockito.spy(AtpKafkaRamAdapter.class);
        adapter.setContext(testRunContext);

        mockStatic(RamAdapterProvider.class);
        when(RamAdapterProvider.getNewAdapter(anyString())).thenReturn(adapter);

        mockStatic(TestRunContextHolder.class);
        when(TestRunContextHolder.getContext(anyString())).thenReturn(testRunContext);

        createExpectedHierarchyLogRecords();
        return testRunContext;
    }

    /**
     * Compound (FAILED)
     * ---Step1 (FAILED)
     * ------LogRecord1 (FAILED)
     */
    @Test
    public void testHierarchyLogRecords_CheckCompoundStatus_CompoundStatusIsFailed() throws Exception {
        Stack<LogRecord> result = executeWithTwoSteps();

        Assert.assertEquals(3, result.size());

        LogRecord compound = result.pop();
        boolean compoundIdEqualsExpectedId = compound.getUuid().equals(Utils.compaundId);
        boolean compoundStatusEqualsExpectedStatus =
                compound.getTestingStatus().getName().equals(TestingStatuses.FAILED.getName());
        Assert.assertTrue(String.format("Compound [%s] with status FAILED is expected last in the stack. AR: id [%s], "
                        + "status [%s]", Utils.compaundId, compound.getUuid(), compound.getTestingStatus()),
                compoundIdEqualsExpectedId && compoundStatusEqualsExpectedStatus);

        LogRecord step = result.pop();
        Assert.assertTrue(String.format("Step [%s] with parent [%s] and status FAILED expected after compound. AR: id "
                        + "[%s], "
                        + "parentId [%s]", Utils.stepId, Utils.compaundId, step.getUuid(), step.getParentRecordId()),
                verifyLogRecord(Utils.stepId, Utils.compaundId, TestingStatuses.FAILED, step));
    }

    /**
     * Compound (FAILED)
     * ---Step (FAILED)
     * ------RootSection (FAILED)
     * ---------Section1 (PASSED)
     * ------------LogRecord11 (PASSED)
     * ------------LogRecord12 (PASSED)
     * ---------Section2 (FAILED)
     * ------------Section21 (FAILED)
     * ---------------LogRecord211 (PASSED)
     * ---------------LogRecord211 (FAILED)
     * ---------RootLogRecord (PASSED)
     */
    @Test
    public void testHierarchyLogRecords_CheckParentAndStatus_InResultStackLogRecordsAreInCorrectOrderAndWithCorrectStatuses()
            throws Exception {
        Stack<LogRecord> result = execute();

        Assert.assertEquals(11, result.size());
        LogRecord compound = result.pop();
        boolean parentCompoundIsNull = Objects.isNull(compound.getParentRecordId());
        boolean compoundIdEqualsExpectedId = compound.getUuid().equals(Utils.compaundId);
        boolean compoundStatusEqualsExpectedStatus =
                compound.getTestingStatus().getName().equals(TestingStatuses.FAILED.getName());
        Assert.assertTrue(String.format("Compound [%s] with parent [%s] is expected last in the stack. AR: id [%s], "
                        + "parentId [%s]", Utils.compaundId, null, compound.getUuid(), compound.getParentRecordId()),
                parentCompoundIsNull && compoundIdEqualsExpectedId && compoundStatusEqualsExpectedStatus);

        LogRecord step = result.pop();
        Assert.assertTrue(String.format("Step [%s] with parent [%s] expected after compound. AR: id [%s], "
                        + "parentId [%s]", Utils.stepId, Utils.compaundId, step.getUuid(), step.getParentRecordId()),
                verifyLogRecord(Utils.stepId, Utils.compaundId, TestingStatuses.FAILED, step));

        LogRecord rootSection = result.pop();
        Assert.assertTrue(String.format("Root section [%s] with parent [%s] expected after Step. AR: id [%s], "
                        + "parentId [%s]", rootSectionId, Utils.stepId, rootSection.getUuid(),
                rootSection.getParentRecordId()), verifyLogRecord(rootSectionId, Utils.stepId, TestingStatuses.FAILED,
                rootSection));

        LogRecord rootLogRecord = result.pop();
        Assert.assertTrue(String.format("Root Log Record [%s] with parent [%s] expected last in the Root Section. "
                        + "AR: id [%s], parentId [%s]", rootLogRecordId, rootSectionId, rootLogRecord.getUuid(),
                rootLogRecord.getParentRecordId()), verifyLogRecord(rootLogRecordId, rootSectionId,
                TestingStatuses.PASSED, rootLogRecord));

        LogRecord section2 = result.pop();
        Assert.assertTrue(String.format("Section2 [%s] with parent [%s] expected in the Root Section. AR: id [%s], "
                        + "parentId [%s]", section2Id, rootSectionId, section2.getUuid(), section2.getParentRecordId()),
                verifyLogRecord(section2Id, rootSectionId, TestingStatuses.FAILED, section2));

        LogRecord section21 = result.pop();
        Assert.assertTrue(String.format("Section21 [%s] with parent [%s] expected in the Section2. "
                        + "AR: id [%s], parentId [%s]", section21Id, section2Id, section21.getUuid(),
                section21.getParentRecordId()), verifyLogRecord(section21Id, section2Id, TestingStatuses.FAILED,
                section21));

        LogRecord logRecord212 = result.pop();
        Assert.assertTrue(String.format("Log Record 212 [%s] with parent [%s] expected last in the Section21. "
                        + "AR: id [%s], parentId [%s]", logRecord212Id, section21Id, logRecord212.getUuid(),
                logRecord212.getParentRecordId()), verifyLogRecord(logRecord212Id, section21Id,
                TestingStatuses.FAILED, logRecord212));

        LogRecord logRecord211 = result.pop();
        Assert.assertTrue(String.format("Log Record 211 [%s] with parent [%s] expected in the Section21. "
                        + "AR: id [%s], parentId [%s]", logRecord211Id, section21Id, logRecord211.getUuid(),
                logRecord211.getParentRecordId()), verifyLogRecord(logRecord211Id, section21Id,
                TestingStatuses.PASSED, logRecord211));

        LogRecord section1 = result.pop();
        Assert.assertTrue(String.format("Section1 [%s] with parent [%s] expected first in the Root Section. "
                        + "AR: id [%s], parentId [%s]", section1Id, rootSectionId, section1.getUuid(),
                section1.getParentRecordId()), verifyLogRecord(section1Id, rootSectionId, TestingStatuses.PASSED,
                section1));

        LogRecord logRecord12 = result.pop();
        Assert.assertTrue(String.format("Log Record 12 [%s] with parent [%s] expected last in the Section1. "
                        + "AR: id [%s], parentId [%s]", logRecord12Id, section1Id, logRecord12.getUuid(),
                logRecord12.getParentRecordId()), verifyLogRecord(logRecord12Id, section1Id,
                TestingStatuses.PASSED, logRecord12));

        LogRecord logRecord11 = result.pop();
        Assert.assertTrue(String.format("Log Record 11 [%s] with parent [%s] expected in the Section1. "
                        + "AR: id [%s], parentId [%s]", logRecord11Id, section1Id, logRecord11.getUuid(),
                logRecord11.getParentRecordId()), verifyLogRecord(logRecord11Id, section1Id, TestingStatuses.PASSED,
                logRecord11));
    }

    private Stack<LogRecord> execute() throws Exception {
        TestRunContext testRunContext = setUp(true);
        Stack<LogRecord> result = new Stack<>();
        createAnswers(result, testRunContext);
        PowerMockito.doReturn(testRunContext).when(adapter).startAtpRun(any(), any());
        writer.openLog(testRunContext.getTestRunId());

        openSection(rootSectionName, "", rootSectionId.toString());

        openSection(section1Name, "", section1Id.toString());
        message(logRecord11Name, "message", logRecord11Id.toString(), TestingStatuses.PASSED);
        message(logRecord12Name, "message", logRecord12Id.toString(), TestingStatuses.PASSED);
        writer.closeSection(new WebReportItem.CloseSection());

        openSection(section2Name, "", section2Id.toString());
        openSection(section21Name, "", section21Id.toString());
        message(logRecord211Name, "message", logRecord211Id.toString(), TestingStatuses.PASSED);
        message(logRecord212Name, "message", logRecord212Id.toString(), TestingStatuses.FAILED);
        writer.closeSection(new WebReportItem.CloseSection());
        writer.closeSection(new WebReportItem.CloseSection());

        message(rootLogRecordName, "message", rootLogRecordId.toString(), TestingStatuses.PASSED);
        writer.closeSection(new WebReportItem.CloseSection());

        return result;
    }

    private Stack<LogRecord> executeWithTwoSteps() throws Exception {
        TestRunContext testRunContext = setUp(false);
        Stack<LogRecord> result = new Stack<>();
        createAnswers(result, testRunContext);

        PowerMockito.doReturn(testRunContext).when(adapter).startAtpRun(any(), any());
        writer.openLog(testRunContext.getTestRunId());

        message(logRecord11Name, "message", logRecord11Id.toString(), TestingStatuses.FAILED);

        return result;
    }

    private void createAnswers(Stack<LogRecord> result, TestRunContext context) {
        AtpCompaund currentCompound = context.getAtpCompaund();
        do {
            LogRecord logRecord = new LogRecord();
            logRecord.setName(currentCompound.getSectionName());
            logRecord.setUuid(UUID.fromString(currentCompound.getSectionId()));
            logRecord.setTestingStatus(currentCompound.getTestingStatuses());
            AtpCompaund parent = currentCompound.getParentSection();
            logRecord.setParentRecordId(parent == null ? null : UUID.fromString(parent.getSectionId()));

            result.push(logRecord);
            currentCompound = parent;
        } while (currentCompound != null);
        Answer<TestRunContext> answer = invocationOnMock -> {
            LogRecord logRecord = invocationOnMock.getArgument(0);
            String logRecordName = logRecord.getName();
            result.removeIf(step -> step.getName().equals(logRecordName));
            result.push(logRecord);
            return writer.getAdapter().getContext();
        };
        Answer<TestRunContext> answerFotUpdateStatus = invocationOnMock -> {
            String uuid = invocationOnMock.getArgument(0);
            String status = invocationOnMock.getArgument(1);
            Optional<LogRecord> optionalLogRecord =
                    result.stream().filter(step -> step.getUuid().toString().equals(uuid)).findFirst();
            LogRecord logRecord;
            if (optionalLogRecord.isPresent()) {
                logRecord = optionalLogRecord.get();
                result.remove(logRecord);
            } else {
                logRecord = new LogRecord();
                logRecord.setUuid(UUID.fromString(uuid));
            }
            logRecord.setTestingStatus(TestingStatuses.findByValue(status));
            result.push(logRecord);
            return writer.getAdapter().getContext();
        };
        PowerMockito.doAnswer(answer).when(adapter).sendLogRecord(any(LogRecord.class));
        PowerMockito.doAnswer(answerFotUpdateStatus).when(adapter).updateTestingStatus(anyString(), anyString());
    }

    private boolean verifyLogRecord(UUID expectedId, UUID expectedParentId,
                                    TestingStatuses status, LogRecord resultLogRecord) {
        return resultLogRecord.getUuid().equals(expectedId)
                && resultLogRecord.getParentRecordId().equals(expectedParentId)
                && status.getName().equals(resultLogRecord.getTestingStatus().getName());
    }

    private void createExpectedHierarchyLogRecords() {
        LogRecord compaund = Utils.createLogRecord(Utils.compaundName, Utils.compaundId, null);
        expectedHierarchyLogRecords.push(compaund);

        LogRecord step = Utils.createLogRecord(Utils.stepName, Utils.stepId, Utils.compaundId);
        expectedHierarchyLogRecords.push(step);

        LogRecord rootSection = Utils.createLogRecord(rootSectionName, rootSectionId, Utils.stepId);
        expectedHierarchyLogRecords.push(rootSection);

        LogRecord section1 = Utils.createLogRecord(section1Name, section1Id, rootSectionId);
        expectedHierarchyLogRecords.push(section1);
        LogRecord logRecord11 = Utils.createLogRecord(logRecord11Name, logRecord11Id, section1Id);
        expectedHierarchyLogRecords.push(logRecord11);
        LogRecord logRecord12 = Utils.createLogRecord(logRecord12Name, logRecord12Id, section1Id);
        expectedHierarchyLogRecords.push(logRecord12);

        LogRecord section2 = Utils.createLogRecord(section2Name, section2Id, rootSectionId);
        expectedHierarchyLogRecords.push(section2);
        LogRecord section21 = Utils.createLogRecord(section21Name, section21Id, section2Id);
        expectedHierarchyLogRecords.push(section21);
        LogRecord logRecord211 = Utils.createLogRecord(logRecord211Name, logRecord211Id, section21Id);
        expectedHierarchyLogRecords.push(logRecord211);
        LogRecord logRecord212 = Utils.createLogRecord(logRecord212Name, logRecord212Id, section21Id);
        expectedHierarchyLogRecords.push(logRecord212);

        LogRecord rootLogRecord = Utils.createLogRecord(rootLogRecordName, rootLogRecordId, rootSectionId);
        expectedHierarchyLogRecords.push(rootLogRecord);
    }

    private void openSection(String name, String message, String id) {
        Message messageBean = new Message();
        messageBean.setName(name);
        messageBean.setMessage(message);
        messageBean.setUuid(id);
        messageBean.setTestingStatus(TestingStatuses.UNKNOWN.toString());
        writer.openSection(messageBean);
    }

    private void message(String name, String message, String id, TestingStatuses status) {
        Message messageBean = new Message();
        messageBean.setName(name);
        messageBean.setMessage(message);
        messageBean.setUuid(id);
        messageBean.setTestingStatus(status.toString());
        if (!isEmpty(writer.getContext().getSections())) {
            messageBean.setParentRecordId(writer.getContext().getSections().peek().getUuid().toString());
        }
        writer.message(messageBean);
    }
}
