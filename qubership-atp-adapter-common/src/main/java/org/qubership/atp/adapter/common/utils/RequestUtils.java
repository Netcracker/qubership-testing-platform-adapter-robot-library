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

import static org.qubership.atp.adapter.common.RamConstants.BUSINESS_IDS_KEYS_KEY;
import static org.qubership.atp.adapter.common.RamConstants.OBJECT_MAPPER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.adapters.interceptors.MdcHttpRequestInterceptor;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.adapter.common.entities.UploadScreenshotResponse;
import org.qubership.atp.ram.enums.ExecutionStatuses;
import org.qubership.atp.ram.enums.TypeAction;

public class RequestUtils {

    private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);
    private static final Executor executor = Executors.newCachedThreadPool();
    private static final String IMAGE_PNG = "image/png";
    private static final String HTTPS = "https";
    private static final String HTTP = "http";

    private static final List<HttpRequestInterceptor> interceptors = new ArrayList<>();
    private static HttpClientBuilderProvider httpClientBuilderProvider;
    private static final List<String> businessIds = Arrays.stream(Config.getConfig()
            .getProperty(BUSINESS_IDS_KEYS_KEY, "userId,projectId,executionRequestId,testRunId,bvTestRunId,bvTestCaseId,"
                    + "environmentId,systemId,subscriberId,tsgSessionId,svpSessionId,dataSetId,dataSetListId,attributeId,"
                    + "itfLiteRequestId,reportType,itfSessionId,itfContextId,callChainId")
            .split(","))
            .map(String::trim)
            .collect(Collectors.toList());

    /**
     * Extension Point that allows to specify custom HttpClient implementation, for example with enabled tracing.
     *
     * @param clientBuilderProvider implementation of HttpClientBuilderProvider
     */
    public static void setHttpClientBuilderProvider(HttpClientBuilderProvider clientBuilderProvider) {
        httpClientBuilderProvider = clientBuilderProvider;
    }

    /**
     * Register new HttpRequestInterceptor.
     * This Interceptor will be added to HttpClient interceptors
     *
     * @param interceptor interceptor
     */
    public static void registerHttpInterceptor(HttpRequestInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    /**
     * Sending request to the specified url.
     */
    public static ObjectNode postRequest(String url, String request) {
        ObjectNode result = OBJECT_MAPPER.createObjectNode();
        try {
            String output = postRequestAsString(url, request);
            if (StringUtils.isEmpty(output)) {
                log.debug("Response from url [{}] is empty", url);
                return result;
            }
            log.debug("Response from url [{}]: {} ", url, output);
            result = OBJECT_MAPPER.readValue(output, ObjectNode.class);
        } catch (IOException jpe) {
            log.error("Cannot parse response from [{}]", url, jpe);
        }
        return result;
    }

    /**
     * Sending POST request to the specified url and returns result in provided response type.
     * For serialization and deserialization {@link com.fasterxml.jackson.databind.ObjectMapper}
     * is used.
     *
     * @param url          request url
     * @param request      object
     * @param responseType type of response, provide null if there is no body in response
     * @return response body of responseType type, or null in case responseType is null
     * @throws IOException in case of response status >= 300 or deserialization error.
     */
    public <ResponseTypeT> ResponseTypeT postRequest(
            String url,
            Object request,
            Class<ResponseTypeT> responseType) throws IOException {
        return request != null ? requestWithBody(url, request, responseType, Request::Post) :
                requestWithoutBody(url, responseType, Request::Post);
    }

    /**
     * Sending PUT request to the specified url and returns result in provided response type.
     * For serialization and deserialization {@link com.fasterxml.jackson.databind.ObjectMapper}
     * is used.
     *
     * @param url          request url
     * @param request      object
     * @param responseType type of response, provide null if there is no body in response
     * @return response body of responseType type, or null in case responseType is null
     * @throws IOException in case of response status >= 300 or deserialization error.
     */
    public <ResponseTypeT> ResponseTypeT putRequest(
            String url,
            Object request,
            Class<ResponseTypeT> responseType) throws IOException {
        return request != null ? requestWithBody(url, request, responseType, Request::Put) :
                requestWithoutBody(url, responseType, Request::Put);
    }

    /**
     * Sending PATCH request to the specified url and returns result in provided response type.
     * For serialization and deserialization {@link com.fasterxml.jackson.databind.ObjectMapper}
     * is used.
     *
     * @param url          request url
     * @param request      object
     * @param responseType type of response, provide null if there is no body in response
     * @return response body of responseType type, or null in case responseType is null
     * @throws IOException in case of response status >= 300 or deserialization error.
     */
    public <ResponseTypeT> ResponseTypeT patchRequest(
            String url,
            Object request,
            Class<ResponseTypeT> responseType) throws IOException {
        return request != null ? requestWithBody(url, request, responseType, Request::Patch) :
                requestWithoutBody(url, responseType, Request::Patch);
    }

    /**
     * Sending POST request of OCTET_STREAM content type to specified url and returns result in provided response type.
     * For serialization and deserialization {@link com.fasterxml.jackson.databind.ObjectMapper}
     * is used.
     *
     * @param url           request url
     * @param requestStream object
     * @param responseType  type of response, provide null if there is no body in response
     * @return response body of responseType type, or null in case responseType is null
     * @throws IOException in case of response status >= 300 or deserialization error.
     */
    public <ResponseTypeT> ResponseTypeT postRequestStream(
            String url,
            InputStream requestStream,
            Class<ResponseTypeT> responseType) throws IOException {
        return requestWithBodyStream(url, Objects.requireNonNull(requestStream), responseType, Request::Post);
    }

    private static <ResponseTypeT> ResponseTypeT requestWithBody(
            String url,
            Object request,
            Class<ResponseTypeT> responseType,
            Function<String, Request> requestMethodProvider) throws IOException {
        log.trace("Send request to url {} for entity of type {}", url, responseType);
        String requestString = OBJECT_MAPPER.writeValueAsString(request);
        final Content responseContent = getHttpExecutor().execute(
                requestMethodProvider.apply(url).bodyString(requestString, ContentType.APPLICATION_JSON))
                .returnContent();
        if (responseType != null) {
            return OBJECT_MAPPER.readValue(responseContent.asStream(), responseType);
        }
        return null;
    }

    private static <ResponseTypeT> ResponseTypeT requestWithBodyStream(
            String url,
            InputStream request,
            Class<ResponseTypeT> responseType,
            Function<String, Request> requestMethodProvider) throws IOException {
        log.trace("Send request to url {} for entity of type {}", url, responseType);
        final Content responseContent = getHttpExecutor().execute(
                requestMethodProvider.apply(url).bodyStream(request, ContentType.APPLICATION_OCTET_STREAM))
                .returnContent();
        if (responseType != null) {
            return OBJECT_MAPPER.readValue(responseContent.asStream(), responseType);
        }
        return null;
    }

    private static <ResponseTypeT> ResponseTypeT requestWithoutBody(
            String url,
            Class<ResponseTypeT> responseType,
            Function<String, Request> requestMethodProvider) throws IOException {
        log.trace("Send request to url {} for entity of type {}", url, responseType);
        final Content responseContent = getHttpExecutor().execute(
                requestMethodProvider.apply(url))
                .returnContent();
        if (responseType != null) {
            return OBJECT_MAPPER.readValue(responseContent.asStream(), responseType);
        }
        return null;
    }

    /**
     * Sending request to the specified url and returns result as String.
     *
     * @param url     request url
     * @param request object in String representation
     * @return response body as String, null in case of NO_CONTENT, empty String in case of IOException raised.
     */
    public static String postRequestAsString(String url, String request) {
        String result = "";
        try {
            final Content postResult = getHttpExecutor()
                    .execute(Request.Post(url)
                            .bodyString(request, ContentType.APPLICATION_JSON)).returnContent();
            if (postResult.equals(Content.NO_CONTENT)) {
                return null;
            }
            result = postResult.asString();
            if (log.isDebugEnabled()) {
                log.debug("LOGGER RESPONSE: " + result);
            }
        } catch (IOException io) {
            log.error("Error due sending request. {}", result, io);
        }
        return result;
    }

    public static ObjectNode putRequest(String url) {
        try {
            return OBJECT_MAPPER.readValue(
                    getHttpExecutor()
                            .execute(Request.Put(url))
                            .returnContent()
                            .asString(), ObjectNode.class);
        } catch (IOException e) {
            log.error("Error while execute request on url: {}.", url, e);
        }
        return null;
    }

    public static void putRequest(String url, String request) {
        try {
            OBJECT_MAPPER.readValue(
                    getHttpExecutor()
                            .execute(Request.Put(url).bodyString(request, ContentType.APPLICATION_JSON))
                            .returnContent()
                            .asString(), ObjectNode.class);
        } catch (IOException e) {
            log.error("Error while execute request on url: {}.", url, e);
        }
    }

    public static ObjectNode getRequest(String url) {
        try {
            return OBJECT_MAPPER.readValue(
                    getHttpExecutor()
                            .execute(Request.Get(url))
                            .returnContent()
                            .asString(), ObjectNode.class);
        } catch (IOException e) {
            log.error("Error while execute request on url: {}.", url, e);
        }
        return null;
    }

    public static UploadScreenshotResponse upload(String url,
                                                  String fileName,
                                                  File file,
                                                  String contentType,
                                                  String snapshotSource,
                                                  String snapshotExternalSource) {
        UploadScreenshotResponse output = new UploadScreenshotResponse();
        if (!Objects.nonNull(file)) {
            log.debug("File is null, output {}", output);
            return output;
        }
        try (InputStream stream = new FileInputStream(file)) {
            output = upload(url, fileName, stream, contentType, snapshotSource, snapshotExternalSource);
        } catch (Exception io) {
            log.error("Error during upload attach. ", io);
        }
        return output;
    }

    public static UploadScreenshotResponse upload(String url,
                                                  String fileName,
                                                  InputStream stream,
                                                  String contentType,
                                                  String snapshotSource,
                                                  String snapshotExternalSource) {
        UploadScreenshotResponse output = new UploadScreenshotResponse();
        final Content postResult;
        try {
            postResult = getHttpExecutor()
                    .execute(Request.Post(url + "/?fileName="
                            + fileName + "&contentType=" + contentType + "&snapshotSource="
                            + URLEncoder.encode(StringUtils.defaultIfEmpty(snapshotSource, ""), "UTF-8")
                            + "&snapshotExternalSource=" + URLEncoder.encode(StringUtils.defaultIfEmpty(
                            snapshotExternalSource, ""), "UTF-8"))
                            .bodyStream(stream, ContentType.APPLICATION_OCTET_STREAM)).returnContent();
            output = OBJECT_MAPPER.readValue(postResult.asString(), UploadScreenshotResponse.class);
            log.debug("Url: {}, RAM response: {} ", url, output);
        } catch (IOException ioException) {
            log.error("Failed to send attachment {} with content type {} to RAM",
                    fileName, contentType, ioException);
        }
        return output;
    }

    public static ObjectNode buildUpdateStatusRequest(ExecutionStatuses statuses, String idKey, String id) {
        ObjectNode updateStatusRequest = OBJECT_MAPPER.createObjectNode();
        updateStatusRequest.put(RamConstants.EXECUTION_STATUS_KEY, statuses.getName());
        updateStatusRequest.put(idKey, id);
        return updateStatusRequest;
    }

    /**
     * Provides Executor with interceptors.
     *
     * @return Executor
     */
    public static org.apache.http.client.fluent.Executor getHttpExecutor() {
        HttpClientBuilder httpClientBuilder = Optional.ofNullable(httpClientBuilderProvider)
                .map(HttpClientBuilderProvider::getBuilder)
                .orElseGet(HttpClientBuilder::create);
        try {
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, (cert, authType) -> true).build();
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                    NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register(HTTPS, sslsf)
                            .register(HTTP, new PlainConnectionSocketFactory())
                            .build();

            BasicHttpClientConnectionManager connectionManager =
                    new BasicHttpClientConnectionManager(socketFactoryRegistry);
            httpClientBuilder.setSSLSocketFactory(sslsf).setConnectionManager(connectionManager);
        } catch (GeneralSecurityException exception) {
            log.error("An error occurred while creating the SSLContext for http executor.", exception);
        }
        httpClientBuilder.addInterceptorFirst(new MdcHttpRequestInterceptor(businessIds));
        for (HttpRequestInterceptor interceptor : interceptors) {
            httpClientBuilder.addInterceptorLast(interceptor);
        }
        return org.apache.http.client.fluent.Executor.newInstance(httpClientBuilder.build());
    }

    /**
     * Upload one file to RAM.
     *
     * @param message   Message with attributes.
     * @param atpRamUrl Url to RAM.
     * @deprecated moved to AbstractAdapter
     */
    @Deprecated
    public static void uploadOneFile(Message message, String atpRamUrl) {
        Map attributes = message.getAttributes().get(0);
        UploadScreenshotResponse response = uploadFile(attributes, message.getUuid(), atpRamUrl);

        String contentType = (String) attributes.get(RamConstants.SCREENSHOT_TYPE_KEY);
        if (Objects.isNull(contentType) || contentType.equals(IMAGE_PNG)) {
            message.setType(TypeAction.UI.name());
            message.setScreenId(response.getFileId());
            message.setPreview(response.getPreview());
        }
    }

    /**
     * Upload all files to RAM.
     *
     * @param message   Message with id and attributes.
     * @param uploadUrl Url to RAM.
     * @deprecated moved to AbstractAdapter
     */
    @Deprecated
    public static void uploadAllFiles(Message message, String uploadUrl) {
        List<Map<String, Object>> attributes = message.getAttributes();
        String id = message.getUuid();
        attributes.forEach(attribute -> uploadFile(attribute, id, uploadUrl));
    }

    private static UploadScreenshotResponse uploadFile(Map<String, Object> attribute, String id, String uploadUrl) {
        String contentType = (String) attribute.get(RamConstants.SCREENSHOT_TYPE_KEY);
        log.debug("Try to upload a file with type {} for LogRecord: {}", contentType, id);
        UploadScreenshotResponse response;
        if (attribute.containsKey(RamConstants.ATTACHMENT_STREAM_KEY)) {
            response = RequestUtils.upload(uploadUrl,
                    (String) attribute.get(RamConstants.SCREENSHOT_NAME_KEY),
                    (InputStream) attribute.get(RamConstants.ATTACHMENT_STREAM_KEY),
                    contentType,
                    (String) attribute.get(RamConstants.SCREENSHOT_SOURCE_KEY),
                    (String) attribute.get(RamConstants.SCREENSHOT_EXTERNAL_SOURCE_KEY));
        } else {
            response = RequestUtils.upload(uploadUrl,
                    (String) attribute.get(RamConstants.SCREENSHOT_NAME_KEY),
                    (File) attribute.get(RamConstants.SCREENSHOT_FILE_KEY),
                    contentType,
                    (String) attribute.get(RamConstants.SCREENSHOT_SOURCE_KEY),
                    (String) attribute.get(RamConstants.SCREENSHOT_EXTERNAL_SOURCE_KEY));
        }
        if (Strings.isNullOrEmpty(response.getFileId())) {
            log.error("File id is NULL for Log Record: [{}]. Response {}", id, response);
        }
        log.debug("Uploaded file with id {} for LR {}", response.getFileId(), id);
        return response;
    }
}
