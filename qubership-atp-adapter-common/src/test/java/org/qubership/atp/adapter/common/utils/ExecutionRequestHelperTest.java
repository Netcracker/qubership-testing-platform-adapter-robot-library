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


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import org.qubership.atp.adapter.common.RamConstants;

public class ExecutionRequestHelperTest {

    @Test
    public void prepareErName() {
        String requestName = ExecutionRequestHelper.generateRequestName();
        Pattern p = Pattern.compile("(Default)\\s(ER)\\s(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4}) (\\d{1,2}):(\\d{2})");
        Matcher m = p.matcher(requestName);
        Assert.assertTrue(m.matches());
    }


    @Test
    public void getSolutionBuild_whenGetSolutionBuildEnabled_secondGetterDoesNotInvokeConnectAndFetchPage()
            throws IOException, ExecutionException, InterruptedException {
        Config.getConfig().setProperty("atp2.get.solution.build.enabled", "true");
        SolutionBuildGetter getter1 = mock(SolutionBuildGetter.class);
        when(getter1.connectAndFetchPage(anyString()))
                .thenReturn("9.3.NC.ATP.CD91\nbuild_number:32_9.3.NC.ATP.CD91_rev13108");
        when(getter1.getSolutionBuild(any())).thenCallRealMethod();

        SolutionBuildGetter getter2 = mock(SolutionBuildGetter.class);
        when(getter2.connectAndFetchPage(anyString()))
                .thenReturn("9.3.NC.ATP.CD91\nbuild_number:32_9.3.NC.ATP.CD91_rev13108");
        when(getter2.getSolutionBuild(any())).thenCallRealMethod();

        Callable<String> callable1 = () -> getter1.getSolutionBuild("url");
        Callable<String> callable2 = () -> getter2.getSolutionBuild("url");

        ExecutorService executors = Executors.newFixedThreadPool(2);

        Future<String> futureResult1 = executors.submit(callable1);
        String result1 = futureResult1.get();
        Future<String> futureResult2 = executors.submit(callable2);
        String result2 = futureResult2.get();

        Assert.assertEquals(result1, result2);
        Assert.assertEquals("32_9.3.NC.ATP.CD91_rev13108", result1);
        verify(getter2, times(0)).connectAndFetchPage(anyString());
    }

    @Test
    public void getSolutionBuild_whenGetSolutionBuildNotEnabled_resultIsDefault()
            throws IOException, ExecutionException, InterruptedException {
        SolutionBuildGetter getter1 = mock(SolutionBuildGetter.class);
        when(getter1.connectAndFetchPage(anyString()))
                .thenReturn("9.3.NC.ATP.CD91\nbuild_number:32_9.3.NC.ATP.CD91_rev13108");
        when(getter1.getSolutionBuild(any())).thenCallRealMethod();

        String result1 = getter1.getSolutionBuild("url");

        Assert.assertEquals("Unknown solution build", result1);
    }

    @Test
    public void parseResponse() {
        String response = "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "   <S:Body>\n" +
                "      <ns2:startRunResponse xmlns:ns2=\"http://server.ws.integration.atp.solutions.somedomain.com/\">\n"
                +
                "         <RunResponse>\n" +
                "            <executionRequestInfo>\n" +
                "               <reportLink>http://qaapp125.somedomain.com:6820/common/uobject.jsp?tab=_Test+Run+Tree+View&amp;object=9151562714613898495</reportLink>\n"
                +
                "            </executionRequestInfo>\n" +
                "            <testRunId>9151562714613898494</testRunId>\n" +
                "            <recordId>9151562714613898494</recordId>\n" +
                "         </RunResponse>\n" +
                "      </ns2:startRunResponse>\n" +
                "   </S:Body>\n" +
                "</S:Envelope>";
        Map<String, String> result = ExecutionRequestHelper.parseResponse(response);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.containsKey(RamConstants.TEST_RUN_ID_KEY));
        Assert.assertTrue(result.containsKey(RamConstants.RECORD_ID_KEY));
    }

    @Test
    public void testEqualsString() {
        String a1 = new String("TEST");
        String a2 = new String("TEST");
        System.out.println(a1 == a2);
        System.out.println(a1.equals(a2));

    }
}
