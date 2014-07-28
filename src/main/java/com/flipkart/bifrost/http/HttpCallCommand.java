/**
 * Copyright 2014 Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.bifrost.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.bifrost.framework.BifrostException;
import com.flipkart.bifrost.framework.RemoteCallable;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;


public class HttpCallCommand<T> extends RemoteCallable<T> {
    private static final Logger logger = LoggerFactory.getLogger(HttpCallCommand.class.getSimpleName());

    private static final CloseableHttpClient client = HttpClients.createDefault();
    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonProperty
    private RequestType requestType= RequestType.HTTP_GET;

    @JsonProperty
    private String url;

    @JsonProperty
    private Map<String, String> headers;

    @JsonProperty
    private String contentType = "application/json";

    @JsonProperty
    private Object request;

    @JsonProperty
    private int timeout;

    private int successStatus = HttpStatus.SC_OK;

    public HttpCallCommand() {
        super("http");
    }

    public static<U> HttpCallCommand<U> createGet(final String url) {
        HttpCallCommand<U> request = new HttpCallCommand<U>();
        request.setRequestType(RequestType.HTTP_GET);
        request.setUrl(url);
        return request;
    }

    public static<U> HttpCallCommand<U> createPost(final String url, Object payload) {
        HttpCallCommand<U> request = new HttpCallCommand<U>();
        request.setRequestType(RequestType.HTTP_POST);
        request.setUrl(url);
        request.setRequest(payload);
        return request;
    }

    public static<U> HttpCallCommand<U> createPut(final String url, Object payload) {
        HttpCallCommand<U> request = new HttpCallCommand<U>();
        request.setRequestType(RequestType.HTTP_PUT);
        request.setUrl(url);
        request.setRequest(payload);
        return request;
    }

    public static<U> HttpCallCommand<U> createDelete(final String url) {
        HttpCallCommand<U> request = new HttpCallCommand<U>();
        request.setRequestType(RequestType.HTTP_DELETE);
        request.setUrl(url);
        return request;
    }

    @Override
    public T call() throws Exception {
        return makeHttpCall();
    }

    private T makeHttpCall() throws Exception {
        HttpUriRequest httpRequest = generateRequestObject();
        if(null != headers) {
            for(Map.Entry<String, String> header : headers.entrySet()) {
                httpRequest.setHeader(header.getKey(), header.getValue());
            }
        }
        CloseableHttpResponse response = null;
        try {
            response = client.execute(httpRequest);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            byte [] responseBytes = (null != entity) ? EntityUtils.toByteArray(entity) : null;
            if(statusCode != successStatus) {
                throw new BifrostException(BifrostException.ErrorCode.SUCCESS_STATUS_MISMATCH,
                        String.format("Call status mismatch. Expected %d Got %d",
                                successStatus, statusCode));
            }
            return mapper.readValue(responseBytes, new TypeReference<T>() {});
        } finally {
            if(null != response) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.error("Error closing HTTP response: ", e);
                }
            }
        }
    }

    private HttpUriRequest generateRequestObject() throws BifrostException {
        try {
            switch (requestType) {
                case HTTP_GET:
                    return new HttpGet(url);
                case HTTP_POST:
                    HttpPost post = new HttpPost(url);
                    post.setEntity(new ByteArrayEntity(mapper.writeValueAsBytes(request),
                            ContentType.create(contentType, CharsetUtils.lookup("utf-8"))));
                    return post;
                case HTTP_PUT:
                    HttpPut put = new HttpPut(url);
                    put.setEntity(new ByteArrayEntity(mapper.writeValueAsBytes(request),
                            ContentType.create(contentType, CharsetUtils.lookup("utf-8"))));
                    return put;
                case HTTP_DELETE:
                    return new HttpDelete(url);
            }
        } catch (JsonProcessingException e) {
            throw new BifrostException(BifrostException.ErrorCode.SERIALIZATION_ERROR,
                    "Could not serialize request body");
        }
        throw new BifrostException(BifrostException.ErrorCode.UNSUPPORTED_REQUEST_TYPE,
                String.format("Request type %s is not supported", requestType.name()));
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getSuccessStatus() {
        return successStatus;
    }

    public void setSuccessStatus(int successStatus) {
        this.successStatus = successStatus;
    }
}
