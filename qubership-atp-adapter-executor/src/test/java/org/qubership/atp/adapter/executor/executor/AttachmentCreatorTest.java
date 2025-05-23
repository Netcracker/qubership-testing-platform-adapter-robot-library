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
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import org.qubership.atp.adapter.executor.executor.sourceproviders.CommonFileSourceProvider;
import org.qubership.atp.adapter.executor.executor.sourceproviders.FileSource;
import org.qubership.atp.adapter.executor.executor.sourceproviders.TestSnapshot;
import org.qubership.atp.adapter.executor.executor.sourceproviders.TestSnapshotWithoutExtension;

import org.qubership.atp.adapter.executor.executor.utils.AttachmentCreator;
import org.qubership.atp.adapter.executor.executor.utils.MimeType;
import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;
import org.qubership.atp.ram.models.logrecords.parts.FileType;

public class AttachmentCreatorTest {

    @Test
    public void testCreate_CreateAttachmentFromExtendedFileSourceProvider_ReturnCorrectAttachment() throws Exception {
        String fileName = "testFile.txt";
        String fileContent = "test";
        FileMetadata expectedFileMetadata = new FileMetadata(FileType.COMMON, fileName);

        org.qubership.atp.adapter.executor.executor.NttAttachment attachment = AttachmentCreator.create(new CommonFileSourceProvider(fileContent, fileName));

        Assert.assertEquals(MimeType.HTML.toString(), attachment.getContentType());
        Assert.assertEquals(fileName, attachment.getFileName());
        Assert.assertEquals(fileContent, attachment.getFileSource().toString());
        Assert.assertEquals(expectedFileMetadata, attachment.getFileMetadata());
    }

    @Test
    public void testCreate_CreateAttachmentFromOtherSourceProvider_ReturnCorrectAttachment() throws Exception {
        String fileName = "testFile.html";
        String fileContent = "test";
        FileMetadata expectedFileMetadata = new FileMetadata(FileType.COMMON, fileName);
        File file = new File(fileName);
        FileUtils.writeStringToFile(file, fileContent, StandardCharsets.UTF_8);

        org.qubership.atp.adapter.executor.executor.NttAttachment attachment = AttachmentCreator.create(new FileSource(fileName));

        Assert.assertEquals(MimeType.HTML.toString(), attachment.getContentType());
        Assert.assertEquals(fileName, attachment.getFileName());
        Assert.assertEquals(fileName, attachment.getFileSource().toString());
        Assert.assertEquals(expectedFileMetadata, attachment.getFileMetadata());
    }

    @Test
    public void testCreate_CreateAttachmentFromSnapshot_ReturnCorrectAttachment() throws Exception {
        String fileNameWithExtension = "ramSnapshot.html";
        String fileName = "ramSnapshot";
        String fileContent = "test";
        FileMetadata expectedFileMetadata = new FileMetadata(FileType.COMMON, fileNameWithExtension);

        org.qubership.atp.adapter.executor.executor.NttAttachment attachment = AttachmentCreator.create(new TestSnapshot(fileContent));

        Assert.assertEquals(MimeType.HTML.toString(), attachment.getContentType());
        Assert.assertEquals(fileNameWithExtension, attachment.getFileName());
        Assert.assertTrue(attachment.getFileSource().getName().startsWith(fileName));
        Assert.assertEquals(expectedFileMetadata, attachment.getFileMetadata());
        attachment.getFileSource().delete();
    }

    @Test
    public void testCreate_CreateAttachmentFromSnapshotWithoutExtension_ReturnCorrectAttachment() throws Exception {
        String fileNameWithExtension = "ramSnapshot.txt";
        String fileName = "ramSnapshot";
        String fileContent = "test";
        FileMetadata expectedFileMetadata = new FileMetadata(FileType.COMMON, fileNameWithExtension);

        NttAttachment attachment = AttachmentCreator.create(new TestSnapshotWithoutExtension(fileContent));

        Assert.assertEquals(MimeType.HTML.toString(), attachment.getContentType());
        Assert.assertEquals(fileNameWithExtension, attachment.getFileName());
        Assert.assertTrue(attachment.getFileSource().getName().startsWith(fileName));
        Assert.assertEquals(expectedFileMetadata, attachment.getFileMetadata());
        attachment.getFileSource().delete();
    }
}
