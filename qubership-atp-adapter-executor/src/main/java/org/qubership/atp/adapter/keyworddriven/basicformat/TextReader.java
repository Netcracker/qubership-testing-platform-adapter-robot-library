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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TextReader {
    private static final Log log = LogFactory.getLog(TextReader.class);
    private List<String> fileContent;
    private File file;

    public List<String> getFileContent() {
        return this.fileContent;
    }

    public String getName() {
        return this.file.getName();
    }

    public String getPath() {
        return this.file.getPath();
    }

    public TextReader(File file) {
        this.file = file;
        if (file.exists()) {
            this.readFile(file);
        } else {
            log.error("Text file is NOT FOUND: " + file);
        }

    }

    public TextReader(String file) {
        this(new File(file));
    }

    private void readFile(File file) {
        try {
            List fileList;
            try {
                fileList = Files.readAllLines(file.toPath(), Charset.forName("UTF-8"));
            } catch (MalformedInputException var4) {
                fileList = Files.readAllLines(file.toPath(), Charset.forName("Windows-1251"));
            }

            this.fileContent = structuredWithContent(fileList);
        } catch (IOException var5) {
            IOException ex = var5;
            log.error("Cannot open file : " + file.getName(), ex);
        }

    }

    public static List<String> structuredWithContent(List<String> buffer) {
        List<String> normalContent = new ArrayList();
        StringBuilder tempLine = new StringBuilder();
        boolean doubleQuoteOpened = false;
        boolean singleQuoteOpened = false;
        Iterator var5 = buffer.iterator();

        while(true) {
            while(true) {
                String line;
                do {
                    if (!var5.hasNext()) {
                        return normalContent;
                    }

                    String l = (String)var5.next();
                    line = l.trim();
                } while(line.startsWith("#"));

                char[] string = line.toCharArray();

                for(int i = 0; i < string.length; ++i) {
                    switch (string[i]) {
                        case '"':
                            if (!singleQuoteOpened) {
                                doubleQuoteOpened = isParameter(doubleQuoteOpened, string, i);
                            }
                            break;
                        case '\'':
                            if (!doubleQuoteOpened) {
                                singleQuoteOpened = isParameter(singleQuoteOpened, string, i);
                            }
                    }

                    tempLine.append(string[i]);
                }

                if (!doubleQuoteOpened && !singleQuoteOpened) {
                    normalContent.add(tempLine.toString());
                    tempLine = new StringBuilder();
                } else {
                    tempLine.append("\r\n");
                }
            }
        }
    }

    private static boolean isParameter(boolean quoteOpened, char[] string, int i) {
        return i != 0 && string[i - 1] == '\\' ? quoteOpened : !quoteOpened;
    }
}

