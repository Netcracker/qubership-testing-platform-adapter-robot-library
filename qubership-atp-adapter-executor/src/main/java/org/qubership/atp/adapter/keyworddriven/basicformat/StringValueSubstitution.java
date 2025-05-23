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

package org.qubership.atp.adapter.keyworddriven.basicformat;

import org.qubership.atp.adapter.keyworddriven.executable.Executable;
import org.qubership.atp.adapter.report.Report;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StringValueSubstitution {
    private static final Log log = LogFactory.getLog(StringValueSubstitution.class);
    private static LinkedList<ParamReplacer> replacers = new LinkedList();

    public StringValueSubstitution() {
    }

    protected static void addReplacerFirst(ParamReplacer replacer) {
        replacers.addFirst(replacer);
        if (log.isTraceEnabled()) {
            log.trace("ParamReplacer added : " + replacer);
        }

    }

    public static void addReplacerLast(ParamReplacer replacer) {
        replacers.addLast(replacer);
        if (log.isTraceEnabled()) {
            log.trace("ParamReplacer added : " + replacer);
        }

    }

    public static LinkedList<ParamReplacer> getReplacers() {
        return replacers;
    }

    public static String replaceParametersInString(Executable section, String data, Report report) {
        Iterator var3 = getReplacers().iterator();

        while(var3.hasNext()) {
            ParamReplacer repl = (ParamReplacer)var3.next();
            if (log.isTraceEnabled()) {
                log.trace(repl + " start processing " + data + " value");
            }

            data = repl.replaceParam(section, data, report);
            if (log.isTraceEnabled()) {
                log.trace(repl + " finished processing " + data + " value");
            }
        }

        return data;
    }
}

