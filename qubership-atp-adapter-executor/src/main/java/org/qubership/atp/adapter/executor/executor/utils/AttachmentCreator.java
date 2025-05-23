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

package org.qubership.atp.adapter.executor.executor.utils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Strings;
import org.qubership.atp.adapter.executor.executor.NttAttachment;
import org.qubership.atp.adapter.executor.executor.providers.ExtendedFileSourceProvider;
import org.qubership.atp.ram.models.logrecords.parts.FileMetadata;
import org.qubership.atp.ram.models.logrecords.parts.FileType;
import org.qubership.atp.adapter.report.SourceProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AttachmentCreator {
    private static final Pattern FIND_IMAGE_SOURCE_PATTERN = Pattern.compile("<a href='(.*?)'>");
    private static final Pattern IMAGE_TAG_SRC_ATTR_PATTERN = Pattern.compile("<img src='(.*?\\.png)'");
    private static final String SCREENSHOT_NAME = "ramScreenshot.png";
    private static final String SNAPSHOT_NAME = "ramSnapshot";
    private static final String SNAPSHOT_EXTENSION = ".txt";
    private static final String SNAPSHOT_FILE_NAME = SNAPSHOT_NAME + SNAPSHOT_EXTENSION;

    /**
     * Create NTT attachment.
     */
    public static NttAttachment create(SourceProvider sourceProvider) throws Exception {
        NttAttachment attachment = new NttAttachment();

        String pageSource = sourceProvider.getSource();
        if (pageSource == null) {
            pageSource = "";
        }
        String reportType = sourceProvider.getReportType();
        if (reportType == null) {
            reportType = "";
        }

        if ("SCREENSHOT".equalsIgnoreCase(reportType)) {
            attachment.setContentType(MimeType.IMAGE.toString());
            attachment.setFileName(SCREENSHOT_NAME);
            if (StringUtils.isNotEmpty(pageSource)) {
                Matcher matcher = FIND_IMAGE_SOURCE_PATTERN.matcher(pageSource);

                if (matcher.find()) {
                    String imageSource = matcher.group(1);
                    attachment.setSnapshotSource(imageSource);
                }
            }

            File imageFile = getImageFileByDescriptor(pageSource);
            if (imageFile != null) {
                attachment.setFileSource(imageFile);
            }
        } else if ("SNAPSHOT".equalsIgnoreCase(reportType)) {
            String sourceExtension = sourceProvider.getExtension();
            String extension = Strings.isNullOrEmpty(sourceExtension) || sourceExtension.startsWith(".")
                    ? sourceExtension : '.' + sourceExtension;
            String fileName = Strings.isNullOrEmpty(extension) ? SNAPSHOT_FILE_NAME : SNAPSHOT_NAME + extension;

            File snapshot = File.createTempFile(SNAPSHOT_NAME,
                    Strings.isNullOrEmpty(extension) ? SNAPSHOT_EXTENSION : extension);
            FileUtils.writeStringToFile(snapshot, pageSource, "UTF-8");

            attachment.setContentType(MimeType.HTML.toString());
            attachment.setFileSource(snapshot);
            attachment.setFile(true);
            createFileMetadata(sourceProvider, fileName, attachment);
        } else if ("FILE".equalsIgnoreCase(reportType)) {
            File localFile = new File(pageSource);
            attachment.setContentType(MimeType.HTML.toString());

            if (localFile.exists()) {
                if (FilenameUtils.isExtension(localFile.getName(),"doc")) {
                    attachment.setContentType(MimeType.DOC.toString());
                } else if (FilenameUtils.isExtension(localFile.getName(),"docx")) {
                    attachment.setContentType(MimeType.DOCX.toString());
                }
                attachment.setSnapshotSource(localFile.getName());
            }
            attachment.setFileSource(localFile);
            attachment.setFile(true);

            createFileMetadata(sourceProvider, localFile.getName(), attachment);
        }
        return attachment;
    }

    private static void createFileMetadata(SourceProvider sourceProvider, String fileName, NttAttachment attachment) {
        FileMetadata fileMetadata;
        if (sourceProvider instanceof ExtendedFileSourceProvider) {
            fileMetadata = ((ExtendedFileSourceProvider) sourceProvider).getFileMetadata();
        } else {
            fileMetadata = new FileMetadata(FileType.COMMON, fileName);
        }
        attachment.setSnapshotSource(fileMetadata.getFileName());
        attachment.setFileName(fileMetadata.getFileName());
        attachment.setFileMetadata(fileMetadata);
    }

    private static File getImageFileByDescriptor(String descriptorContent) {
        Matcher srcAttrImageTagMatcher = IMAGE_TAG_SRC_ATTR_PATTERN.matcher(descriptorContent);

        File fileToReturn = null;
        if (srcAttrImageTagMatcher.find()) {
            String imageSrcAttributeValue = srcAttrImageTagMatcher.group(1);

            File localImageFile = new File( imageSrcAttributeValue);

            if (localImageFile.exists()) {
                fileToReturn = localImageFile;
            } else {
                log.debug("ScreenShot does not exist :" + localImageFile.getAbsolutePath());
            }
        }
        return fileToReturn;
    }
}
