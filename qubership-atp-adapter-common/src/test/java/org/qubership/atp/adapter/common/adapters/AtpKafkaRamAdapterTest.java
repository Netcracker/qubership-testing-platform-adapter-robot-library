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

import static java.util.Arrays.asList;
import static net.sf.ezmorph.test.ArrayAssertions.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.protos.KafkaLogRecord;
import org.qubership.atp.adapter.common.utils.ActionParametersTrimmer;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.OpenMode;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.CustomLink;
import org.qubership.atp.ram.models.LogRecord;
import org.qubership.atp.ram.models.Table;

public class AtpKafkaRamAdapterTest {

    private AtpKafkaRamAdapter atpKafkaRamAdapter;

    @Before
    public void setUp() {
        atpKafkaRamAdapter = Mockito.mock(AtpKafkaRamAdapter.class, Mockito.CALLS_REAL_METHODS);
        atpKafkaRamAdapter.actionParametersTrimmer = new ActionParametersTrimmer(256);
        atpKafkaRamAdapter.context = new TestRunContext();
    }

    @Test
    public void createKafkaLogRecord_TableNotProvided_TableNotPresentInResultBuilder() {
        LogRecord logRecord = prepareLogRecord();
        KafkaLogRecord.LogRecord.Builder builder = atpKafkaRamAdapter.createKafkaLogRecord(logRecord);
        assertFalse(builder.hasTable());
    }

    @Test
    public void createKafkaLogRecord_TableProvided_TableNotPresentInResultBuilder() {
        LogRecord logRecord = prepareLogRecord();
        Table.Cell cell1 = new Table.Cell();
        Table.Cell cell2 = new Table.Cell();
        cell1.setValue("value1");
        cell2.setValue("value2");
        Table.Row row = new Table.Row();
        row.setCells(new LinkedList<>(asList(cell1, cell2)));
        Table table = new Table();
        table.setRows(new LinkedList<>(asList(row)));
        logRecord.setTable(table);
        KafkaLogRecord.LogRecord.Builder builder = atpKafkaRamAdapter.createKafkaLogRecord(logRecord);
        assertTrue(builder.hasTable());
        assertEquals(1, builder.getTable().getRowsCount());
        assertEquals(2, builder.getTable().getRowsList().get(0).getCellsCount());
        assertEquals("value1", builder.getTable().getRowsList().get(0).getCellsList().get(0).getValue());
        assertEquals("value2", builder.getTable().getRowsList().get(0).getCellsList().get(1).getValue());
    }

    @Test
    public void createKafkaLogRecord_CustomLinksProvided_CustomLinksArePresentInResultBuilder() {
        LogRecord logRecord = prepareLogRecord();
        List<CustomLink> customLinks = new ArrayList<>();
        customLinks.add(new CustomLink("name1", "http://url1", OpenMode.NEW_TAB));
        customLinks.add(new CustomLink("name2", "http://url2", OpenMode.CURRENT_TAB));
        logRecord.setCustomLinks(customLinks);

        KafkaLogRecord.LogRecord.Builder builder = atpKafkaRamAdapter.createKafkaLogRecord(logRecord);
        List<KafkaLogRecord.CustomLink> resCustomLinks = builder.getCustomLinksList();
        assertNotNull(resCustomLinks);
        assertEquals(2, resCustomLinks.size());
        KafkaLogRecord.CustomLink customLink = resCustomLinks.get(0);
        assertEquals("name1", customLink.getName());
        assertEquals("http://url1", customLink.getUrl());
        assertEquals("NEW_TAB", customLink.getOpenMode());
        customLink = resCustomLinks.get(1);
        assertEquals("name2", customLink.getName());
        assertEquals("http://url2", customLink.getUrl());
        assertEquals("CURRENT_TAB", customLink.getOpenMode());
    }

    public static LogRecord prepareLogRecord() {
        LogRecord logRecord = new LogRecord();
        logRecord.setName("");
        logRecord.setUuid(UUID.randomUUID());
        logRecord.setTestRunId(UUID.randomUUID());
        logRecord.setExecutionStatus(ExecutionStatuses.IN_PROGRESS);
        logRecord.setTestingStatus(TestingStatuses.FAILED);
        logRecord.setSection(true);
        logRecord.setCompaund(true);
        logRecord.setMessage("");
        logRecord.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        logRecord.setCreatedDateStamp(System.currentTimeMillis());
        logRecord.setType(TypeAction.COMPOUND);
        return logRecord;
    }
}
