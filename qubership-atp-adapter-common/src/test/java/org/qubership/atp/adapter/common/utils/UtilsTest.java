/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ram.enums.BvStatus;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTable;
import org.qubership.atp.ram.models.logrecords.parts.ValidationTableLine;

import net.sf.json.JSONObject;

public class UtilsTest {
    private static final String compareResult = """
            {
                "tcId": "5a117edc-fad7-423a-abe9-51d7084a396b",
                "tcName": "OCS.Product.Smoke. Successful Data Customer creation for sanity",
                "compareResult": "MODIFIED",
                "trDate": null,
                "trId": "1",
                "resultLink": "http://localhost:8080/bvtool/#/validation?trid=1",
                "steps": [
                    {
                        "objectId": "d971d7f9-e162-4b95-b72d-a8302651d529",
                        "stepName": "123",
                        "compareResult": "IDENTICAL",
                        "diffs": [],
                        "highlightedEr": "123\\n",
                        "highlightedAr": "123\\n"
                    },
                    {
                        "objectId": "33e92903-d400-45ee-be4a-e2b0bbc9f7c3",
                        "stepName": "CCA-T",
                        "compareResult": "MODIFIED",
                        "diffs": [
                            {
                                "orderId": 1,
                                "expected": "0-0",
                                "actual": "0-0",
                                "description": "ER row(s)## 0-0 are replaced with AR row(s)## 0-0",
                                "result": "MODIFIED"
                            }
                        ],
                        "highlightedEr": "<span data-block-id=\\"pc-highlight-block\\" class=\\"MODIFIED\\">234\\n</span>",
                        "highlightedAr": "<span data-block-id=\\"pc-highlight-block\\" class=\\"MODIFIED\\">123\\n</span>"
                    }]
            }\
            """;

    @Test
    public void parseValidationTableFromJson_CompareTwoObject_ReturnValidValidationTable() throws IOException {
        ValidationTable expectedResult = createValidationTable();
        ValidationTable result = Utils.parseValidationTableFromJson(JSONObject.fromObject(compareResult));
        Assertions.assertEquals(expectedResult, result);
    }

    private ValidationTable createValidationTable() {
        ValidationTable table = new ValidationTable();
        List<ValidationTableLine> lines = new ArrayList<>();

        ValidationTableLine step1 = new ValidationTableLine();
        step1.setName("123");
        step1.setBvStatus(BvStatus.valueOf("IDENTICAL"));
        step1.setStatus(TestingStatuses.PASSED);
        step1.setExpectedResult("123\n");
        step1.setActualResult("123\n");

        ValidationTableLine step2 = new ValidationTableLine();
        step2.setName("CCA-T");
        step2.setBvStatus(BvStatus.valueOf("MODIFIED"));
        step2.setStatus(TestingStatuses.FAILED);
        step2.setExpectedResult("<span data-block-id=\"pc-highlight-block\" class=\"MODIFIED\">234\n</span>");
        step2.setActualResult("<span data-block-id=\"pc-highlight-block\" class=\"MODIFIED\">123\n</span>");

        lines.add(step1);
        lines.add(step2);

        table.setSteps(lines);
        return table;
    }

}
