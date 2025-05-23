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

package org.qubership.atp.adapter.executor.executor.sourceproviders;

import org.apache.commons.io.FilenameUtils;

import org.qubership.atp.adapter.executor.executor.providers.ExtendedFileSourceProvider;
import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;
import org.qubership.atp.ram.models.logrecords.parts.FileType;

public class CommonFileSourceProvider implements ExtendedFileSourceProvider {
    private final String source;
    private final FileMetadata fileMetadata;
    private final String extension;

    /** Constructor of the PotFileSourceProvider.
     * @param source path to the file
     * @param fileName name of the file
     */
    public CommonFileSourceProvider(String source, String fileName) {
        super();
        this.source = source;
        this.fileMetadata = new FileMetadata(FileType.COMMON, fileName);
        this.extension = FilenameUtils.getExtension(fileName);
    }

    @Override
    public String getSource() {
        return this.source;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public String getReportType() {
        return "FILE";
    }

    @Override
    public void setReportType(String reportType) {
    }

    @Override
    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }
}
