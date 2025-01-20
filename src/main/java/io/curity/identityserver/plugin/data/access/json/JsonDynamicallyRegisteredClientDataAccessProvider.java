/*
 *  Copyright 2024 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.plugin.data.access.json;

import io.curity.identityserver.plugin.data.access.json.config.DynamicallyRegisteredClientConfiguration;
import io.curity.identityserver.plugin.data.access.json.config.JsonDataAccessProviderConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.Attributes;
import se.curity.identityserver.sdk.attribute.scim.v2.extensions.DynamicallyRegisteredClientAttributes;
import se.curity.identityserver.sdk.datasource.DynamicallyRegisteredClientDataAccessProvider;
import se.curity.identityserver.sdk.http.HttpMethod;
import se.curity.identityserver.sdk.http.HttpRequest;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.service.Json;
import se.curity.identityserver.sdk.service.WebServiceClient;

import static se.curity.identityserver.sdk.http.HttpResponse.asString;

public class JsonDynamicallyRegisteredClientDataAccessProvider implements DynamicallyRegisteredClientDataAccessProvider {

    private static final Logger _logger = LoggerFactory.getLogger(JsonDynamicallyRegisteredClientDataAccessProvider.class);
    private final DynamicallyRegisteredClientConfiguration _configuration;
    private final WebServiceClient _webServiceClient;
    private final Json _json;

    public JsonDynamicallyRegisteredClientDataAccessProvider(JsonDataAccessProviderConfiguration configuration)
    {
        _configuration = configuration.getDynamicallyRegisterClientConfiguration();
        _webServiceClient = configuration.webServiceClient();
        _json = configuration.json();
    }

    @Override
    public @Nullable DynamicallyRegisteredClientAttributes getByClientId(String clientId) {
        _logger.debug("Getting dynamic client with Id: {}", clientId);
        HttpResponse httpResponse = sendHttpRequest(HttpMethod.GET.getMethodString(), String.join("/", _configuration.urlPath(), clientId), (String) null);

        if (httpResponse.statusCode() != 200) {
            return null;
        }

        String responseBody = httpResponse.body(asString());
        _logger.debug("Received dynamic client JSON response: {}", responseBody);

        return DynamicallyRegisteredClientAttributes.of(Attributes.fromMap(_json.fromJson(responseBody)));
    }

    @Override
    public void create(DynamicallyRegisteredClientAttributes attributes) {
        _logger.debug("Creating a new dynamic client with attributes : {}", attributes);
        sendHttpRequest(HttpMethod.POST.getMethodString(), _configuration.urlPath(), _json.toJson(attributes));
    }

    @Override
    public void update(DynamicallyRegisteredClientAttributes attributes) {
        String clientId = attributes.getClientId();
        _logger.debug("Updating dynamic client with ID: {}  and attributes : {}", clientId,  attributes);
        sendHttpRequest(HttpMethod.PUT.getMethodString(), String.join("/", _configuration.urlPath(), clientId), _json.toJson(attributes));
    }

    @Override
    public void delete(String clientId) {
        _logger.debug("Deleting dynamic client with Id: {}", clientId);
        sendHttpRequest(HttpMethod.DELETE.getMethodString(), String.join("/", _configuration.urlPath(), clientId), (String) null);
    }

    private HttpResponse sendHttpRequest(String method, String urlPath, @Nullable String requestBody)
    {
        HttpRequest.Builder requestBuilder = _webServiceClient.withPath(urlPath)
                .request()
                .accept(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .contentType(JsonClientRequestContentType.APPLICATION_JSON.toString());

        if (requestBody != null)
        {
            requestBuilder.body(HttpRequest.fromString(requestBody));
        }

        return requestBuilder.method(method).response();
    }
}
