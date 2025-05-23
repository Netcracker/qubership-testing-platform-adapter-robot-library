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

import java.io.IOException;
import java.util.Map;

public interface ListenerInterface {
    int ROBOT_LISTENER_API_VERSION = 2;
    String ROBOT_LIBRARY_SCOPE = "GLOBAL";
    String STATUS_PASS = "PASS";
    String STATUS_FAIL = "FAIL";

    void startSuite(String name, Map attributes);

    void endSuite(String name, Map attributes);

    void startTest(String name, Map attributes);

    void endTest(String name, Map attributes);

    void startKeyword(String name, Map attributes);

    void endKeyword(String name, Map attributes);

    void logMessage(Map message) throws IOException;

    void message(Map message);

    void outputFile(String path);

    void logFile(String path);

    void reportFile(String path);

    void debugFile(String path);

    void close();
}
