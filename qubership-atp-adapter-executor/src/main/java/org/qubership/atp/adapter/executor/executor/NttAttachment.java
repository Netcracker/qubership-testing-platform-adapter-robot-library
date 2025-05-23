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

package org.qubership.atp.adapter.executor.executor;

import java.io.File;

import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;

public class NttAttachment {

    private boolean isFile;
    private String contentType;

    private String snapshotSource;
    private File fileSource;

    private String fileName = "ramScreenshot.png";

    private FileMetadata fileMetadata;

    public NttAttachment() {
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public void setSnapshotSource(String snapshotSource) {
        this.snapshotSource = snapshotSource;
    }

    public String getSnapshotSource() {
        return snapshotSource;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setFileSource(File fileSource) {
        this.fileSource = fileSource;
    }

    public File getFileSource() {
        return fileSource;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }
}
