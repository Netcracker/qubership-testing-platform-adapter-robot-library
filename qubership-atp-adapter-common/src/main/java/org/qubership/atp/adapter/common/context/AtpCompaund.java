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

package org.qubership.atp.adapter.common.context;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtpCompaund implements Serializable {
    String sectionId;
    String sectionName;
    AtpCompaund parentSection;
    TypeAction type;
    TestingStatuses testingStatuses;
    boolean lastInSection;
    Timestamp startDate;
    Long createdDateStamp;
    boolean hidden;
    String browserName;

    public AtpCompaund(String sectionId, String sectionName, AtpCompaund parentSection, TypeAction type,
                       TestingStatuses testingStatuses, boolean hidden) {
        this.sectionId = sectionId;
        this.sectionName = sectionName;
        this.parentSection = parentSection;
        this.type = Objects.isNull(type) ? TypeAction.TECHNICAL : type;
        this.testingStatuses = Objects.isNull(testingStatuses) ? TestingStatuses.UNKNOWN : testingStatuses;
        this.hidden = hidden;
    }
}
