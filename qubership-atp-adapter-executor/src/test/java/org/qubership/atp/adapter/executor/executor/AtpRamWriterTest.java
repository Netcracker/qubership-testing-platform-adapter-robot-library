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

import static org.qubership.atp.adapter.executor.executor.Utils.createAtpCompaund;
import static org.qubership.atp.adapter.executor.executor.Utils.createTestRunContext;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

//import org.qubership.atp.adapter.wd.shell.elements.common.Page;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.adapters.AtpReceiverRamAdapter;
import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.context.TestRunContextHolder;
import org.qubership.atp.adapter.common.entities.Message;

import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.adapter.report.WebReportItem;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TestRunContextHolder.class})
public class AtpRamWriterTest {
    private org.qubership.atp.adapter.executor.executor.AtpRamWriter writer = Mockito.spy(new AtpRamWriter());
    private AtpReceiverRamAdapter atpReceiverRamAdapter = Mockito.mock(AtpReceiverRamAdapter.class);

    @Before
    public void setUp() {
        Mockito.when(writer.getAdapter()).thenReturn(atpReceiverRamAdapter);
    }

    @Test
    public void writeParentSections_StepIsInCompaund_OpenSectionIsCalledWithParamIsCompaundForCompaund() {
        AtpCompaund atpCompaund = createAtpCompaund(true);
        Mockito.when(writer.writeParentSections(any())).thenCallRealMethod();

        writer.writeParentSections(atpCompaund);

        Message message1 = new Message(Utils.compaundId.toString(), "", Utils.compaundName, "",
                TestingStatuses.UNKNOWN.toString(), String.valueOf(TypeAction.COMPOUND), false);
        Mockito.verify(atpReceiverRamAdapter)
                .openCompoundSection(message1, false);

        Message message2 = new Message(Utils.stepId.toString(), Utils.compaundId.toString(), Utils.stepName, "",
                TestingStatuses.UNKNOWN.toString(), String.valueOf(TypeAction.TECHNICAL), false);
        message2.setLastInSection(true);
        Mockito.verify(atpReceiverRamAdapter)
                .openCompoundSection(message2, true);
    }

    @Test
    public void openSection_openSectionOnlyWithTitle_sectionIsOpenedUnderCompound() {
        setMocks(createTestRunContext(true));
        writer.openLog("any");
        writer.getContext().setCompoundAndUpdateCompoundStatuses(createAtpCompaund(true));

        writer.openSection(new WebReportItem.OpenSection("Section under AtpCompound", null, null));

        Message section = new Message();
        section.setName("Section under AtpCompound");
        section.setMessage("");
        section.setTestingStatus(TestingStatuses.UNKNOWN.toString());
        section.setSection(true);

        /*Mockito.verify(atpReceiverRamAdapter)
                .openSection("Step", "", "1", logRecordUuid.toString(), RamConstants.PASSED);*/
        Mockito.verify(atpReceiverRamAdapter).openSection(section);
    }


    @Test
    public void openSection_openSectionOnlyWithTitle_rootSectionIsOpened() {
        setMocks(createTestRunContext(false));
        writer.openLog("any");
        writer.openSection(new WebReportItem.OpenSection("Section without AtpCompound", null, null));

        Message section = new Message();
        section.setName("Section without AtpCompound");
        section.setMessage("");
        section.setTestingStatus(TestingStatuses.UNKNOWN.toString());
        section.setSection(true);

        Mockito.verify(atpReceiverRamAdapter, times(0))
                .openSection(Utils.stepName, "", Utils.compaundId.toString(),
                        Utils.logRecordUuid.toString(), RamConstants.PASSED);
        Mockito.verify(atpReceiverRamAdapter).openSection(section);
    }

//    @Test
//    public void deletingScreenshot_callDelete_screenshotDeleted() throws Exception {
//        setMocks(createTestRunContext(true));
//        File screenFile = new File("screen-thread-50-1660904215745.png");
//        screenFile.createNewFile();
//        Page mock = mockPage();
//        writer.message("title", Level.INFO,"message",mock,null,null);
//        assertFalse("Error during file deletion",screenFile.exists());
//    }

//    private Page mockPage() {
//        String exampleSource = "<html><body><a href='http://127.0.0.1/main/homepage'>http://127.0.0.1/main/homepage</a><br/><img src='screen-thread-50-1660904215745.png' style='max-width:auto%; height:96%; min-height:500px;'></body></html>";
//        Page mock = Mockito.mock(Page.class,RETURNS_DEEP_STUBS);
//        when(mock.getReportType()).thenReturn("SCREENSHOT");
//        when(mock.getExtension()).thenReturn("html");
//        when(mock.getSource()).thenReturn(exampleSource);
//        return mock;
//    }

    private void setMocks(TestRunContext testRunContext) {
        Mockito.when(atpReceiverRamAdapter.startAtpRun(any(), any())).thenReturn(testRunContext);
        mockStatic(TestRunContextHolder.class);
        when(TestRunContextHolder.getContext(any())).thenReturn(testRunContext);
        when(TestRunContextHolder.hasContext(any())).thenReturn(true);
        writer.createContext(testRunContext.getTestRunId());
        doCallRealMethod().when(writer).openSection(any(WebReportItem.OpenSection.class));
        Mockito.when(atpReceiverRamAdapter.openSection(any(), any(), any(), any(), any())).thenReturn(testRunContext);
    }


}
