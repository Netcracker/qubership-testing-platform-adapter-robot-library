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

package org.qubership.atp.adapter.common.utils;

import static org.qubership.atp.adapter.common.RamConstants.OBJECT_MAPPER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.ram.enums.BvStatus;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTableLine;
import net.sf.json.JSONObject;

public class Utils {

    /**
     * Create validation table From Json object.
     * @param table Compare result in json object.
     * @return {@link ValidationTable}.
     * @throws IOException if failed to parse validation table.
     */
    public static ValidationTable parseValidationTableFromJson(JSONObject table) throws IOException {
        if (Objects.nonNull(table)) {
            List<ValidationTableLine> lines = new ArrayList<>();
            ArrayNode steps = OBJECT_MAPPER.readValue(table.getString(RamConstants.ARRAY_PARAMETERS_KEY),
                    ArrayNode.class);
            steps.forEach(step -> {
                ValidationTableLine line = new ValidationTableLine();
                line.setName(step.get(RamConstants.PARAMETER_NAME_KEY).asText());
                line.setExpectedResult(step.get(RamConstants.ER_KEY).asText());
                line.setActualResult(step.get(RamConstants.AR_KEY).asText());
                BvStatus bvStatus = BvStatus.findByValue(step.get(RamConstants.BV_STATUS_KEY).asText());
                line.setBvStatus(bvStatus == null ? BvStatus.UNDEFINED : bvStatus);
                line.setStatus(transformStatusFromBv(line.getBvStatus()));

                lines.add(line);
            });
            ValidationTable validationTable = new ValidationTable();
            validationTable.setSteps(lines);

            return validationTable;
        }
        return null;
    }

    private static TestingStatuses transformStatusFromBv(BvStatus bvStatus) {
        if (Objects.isNull(bvStatus) || BvStatus.UNDEFINED.equals(bvStatus)) {
            return null;
        } else if (BvStatus.IDENTICAL.equals(bvStatus)) {
            return TestingStatuses.PASSED;
        } else if (BvStatus.SIMILAR.equals(bvStatus)) {
            return TestingStatuses.WARNING;
        } else {
            return TestingStatuses.FAILED;
        }
    }
}
