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

package org.qubership.atp.adapter.robot.utils;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScreenShotHelper {
    private final String sep = File.separator;

    /**
     * returns screenshot filename.
     */
    public String extractImage(String messageText) {
        Pattern p = Pattern.compile("(<img[^>]*src=\")([^\"]*)(\"[^>]*>)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(messageText);
        if (m.find()) {
            return m.group(2);
        } else {
            return "";
        }
    }

    /**
     * returns screenshot file.
     */
    public File getScreenshotFile(String fileName, String paBotPoolId, String outPutDir) throws IOException {
        File resultDir = paBotPoolId == null ? new File(outPutDir) : new File(outPutDir).getParentFile();
        File screen = new File(resultDir.getAbsolutePath() + sep + fileName);
        if (screen.exists()) {
            return screen;
        }
        return null;
    }

}
