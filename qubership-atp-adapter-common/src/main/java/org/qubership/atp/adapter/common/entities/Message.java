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

package org.qubership.atp.adapter.common.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;
import org.qubership.atp.ram.dto.response.MessageParameter;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.BrowserConsoleLogsTable;
import org.qubership.atp.ram.models.CustomLink;
import org.qubership.atp.ram.models.MetaInfo;
import org.qubership.atp.ram.models.Table;
import org.qubership.atp.ram.models.logrecords.parts.ContextVariable;
import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;
import org.qubership.atp.ram.models.logrecords.parts.Request;
import org.qubership.atp.ram.models.logrecords.parts.Response;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.ram.models.steplink.ItfLiteStepLinkMetaInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Message {
    private String uuid;
    private String parentRecordId;

    private String name;
    private String message;
    private boolean isSection;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isPreScriptPresent;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isPostScriptPresent;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isGroup;

    private String type;

    private String testingStatus;
    private String executionStatus;
    private Timestamp startDate;
    private Timestamp endDate;

    private MetaInfo metaInfo;
    private Set<String> configInfoId;

    private List<FileMetadata> fileMetadata;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Request request;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Response response;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ValidationTable validationTable;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String command;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String output;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, List<String>> result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> connectionInfo;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String stage;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String linkToTool;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Table table;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String screenId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String preview;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String browserName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String server;

    @JsonIgnore
    private List<Map<String, Object>> attributes = new ArrayList<>();
    @JsonIgnore
    private boolean lastInSection;
    @JsonIgnore
    private boolean hidden;

    @JsonIgnore
    private List<BrowserConsoleLogsTable> browserLogs;

    private Set<String> validationLabels;
    private List<MessageParameter> messageParameters;
    private List<ContextVariable> stepContextVariables;
    private String protocolType;
    private String linkToSvp;
    private Long createdDateStamp;
    private List<CustomLink> customLinks;

    /**
     * Get {@link TypeAction}.
     * 'TECHNICAL' is default value
     *
     * @return type
     */
    public String getType() {
        if (Strings.isNullOrEmpty(type)) {
            return TypeAction.TECHNICAL.toString();
        }
        return type;
    }

    public Message(String uuid, String parentRecordId, String name, String message, String testingStatus, String type,
                   boolean hidden) {
        setUuid(uuid);
        this.parentRecordId = parentRecordId;
        this.name = name;
        this.message = message;
        this.testingStatus = testingStatus;
        this.type = type;
        this.hidden = hidden;
    }

    public void setUuid(String uuid) {
        if ("null".equalsIgnoreCase(uuid)) {
            this.uuid = null;
        } else {
            this.uuid = uuid;
        }
    }

    public void setItfLiteRequestId(UUID id) {
        if (this.metaInfo == null) {
            this.metaInfo = new MetaInfo();
        }
        this.metaInfo.setEditorMetaInfo(new ItfLiteStepLinkMetaInfo(id));
    }
}
