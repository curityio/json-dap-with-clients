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

import io.curity.identityserver.plugin.data.access.json.config.DatabaseClientConfiguration;
import io.curity.identityserver.plugin.data.access.json.config.JsonDataAccessProviderConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.ThreadSafe;
import se.curity.identityserver.sdk.attribute.client.database.DatabaseClientAttributes;
import se.curity.identityserver.sdk.attribute.scim.v2.Meta;
import se.curity.identityserver.sdk.datasource.DatabaseClientDataAccessProvider;
import se.curity.identityserver.sdk.datasource.pagination.PaginatedDataAccessResult;
import se.curity.identityserver.sdk.datasource.pagination.PaginationRequest;
import se.curity.identityserver.sdk.datasource.query.DatabaseClientAttributesFiltering;
import se.curity.identityserver.sdk.datasource.query.DatabaseClientAttributesSorting;
import se.curity.identityserver.sdk.http.HttpRequest;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.service.Json;
import se.curity.identityserver.sdk.service.WebServiceClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.curity.identityserver.plugin.data.access.json.CollectionUtils.putIfNotEmpty;
import static io.curity.identityserver.plugin.data.access.json.CollectionUtils.putIfNotNull;
import static se.curity.identityserver.sdk.http.HttpResponse.asString;

public class JsonDatabaseClientDataAccessProvider implements DatabaseClientDataAccessProvider, ThreadSafe
{
    private static final Logger _logger = LoggerFactory.getLogger(JsonDatabaseClientDataAccessProvider.class);
    private final DatabaseClientConfiguration _configuration;
    private final WebServiceClient _webServiceClient;
    private final Json _json;

    public JsonDatabaseClientDataAccessProvider(JsonDataAccessProviderConfiguration configuration)
    {
        _configuration = configuration.getDatabaseClientConfiguration();
        _webServiceClient = configuration.webServiceClient();
        _json = configuration.json();
    }

    @Override
    public @Nullable DatabaseClientAttributes getClientById(String clientId, String profileId)
    {
        _logger.debug("Getting database client with Id: {} and profileId: {}", clientId, profileId);

        HttpResponse jsonResponse = _webServiceClient
                .withPath(String.join("/", _configuration.urlPath(), clientId))
                .request()
                .accept(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .contentType(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .method("GET")
                .response();

        String jsonResponseBody = jsonResponse.body(asString());
        _logger.debug("Received database client JSON response: {}", jsonResponseBody);

        Map<String, Object> databaseClientMap = _json.fromJson(jsonResponseBody);
        if ("404".equals(databaseClientMap.get("status")))
        {
            return null;
        }

        return DatabaseClientAttributes.from(databaseClientMap);
    }

    @Override
    public DatabaseClientAttributes create(DatabaseClientAttributes attributes, String profileId)
    {
        _logger.debug("Creating a new database client with profileId: {} and attributes : {}", profileId, attributes);
        attributes = attributes.withMeta(Meta.of("dbClient", Instant.now(), Instant.now()));

        HttpResponse jsonResponse = _webServiceClient
                .withPath(_configuration.urlPath())
                .request()
                .accept(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .contentType(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .body(HttpRequest.fromString(_json.toJson(attributes), StandardCharsets.UTF_8))
                .method("POST")
                .response();

        String jsonResponseBody = jsonResponse.body(asString());
        _logger.debug("Received new database client JSON response: {}", jsonResponseBody);

        Map<String, Object> databaseClientMap = _json.fromJson(jsonResponseBody);

        return DatabaseClientAttributes.from(databaseClientMap);
    }

    @Override
    public @Nullable DatabaseClientAttributes update(DatabaseClientAttributes attributes, String profileId)
    {
        _logger.debug("Updating the database client with profileId: {} and attributes : {}", profileId, attributes);
        attributes = attributes.withMeta(Meta.of("dbClient", null, Instant.now()));

        HttpResponse jsonResponse = _webServiceClient
                .withPath(_configuration.urlPath())
                .request()
                .accept(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .contentType(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .body(HttpRequest.fromString(_json.toJson(attributes), StandardCharsets.UTF_8))
                .method("PUT")
                .response();

        String jsonResponseBody = jsonResponse.body(asString());
        _logger.debug("Received updated database client JSON response: {}", jsonResponseBody);

        Map<String, Object> databaseClientMap = _json.fromJson(jsonResponseBody);

        return DatabaseClientAttributes.from(databaseClientMap);
    }

    @Override
    public boolean delete(String clientId, String profileId)
    {
        _logger.debug("Deleting database client with Id: {} and profileId: {}", clientId, profileId);

        _webServiceClient
                .withPath(String.join("/", _configuration.urlPath(), clientId))
                .request()
                .accept(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .contentType(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .method("DELETE")
                .response();

        return true;
    }

    @Override
    public PaginatedDataAccessResult<DatabaseClientAttributes> getAllClientsBy(
            String profileId,
            @Nullable DatabaseClientAttributesFiltering filters,
            @Nullable PaginationRequest paginationRequest,
            @Nullable DatabaseClientAttributesSorting sortRequest,
            boolean activeClientsOnly)
    {

        _logger.debug("Requesting database clients with profileId: {}, activeClientsOnly: {}", profileId, activeClientsOnly);

        Map<String, Collection<String>> queryParams = prepareQueryParamsMap(filters, paginationRequest, sortRequest, activeClientsOnly);
        _logger.debug("getAllClientsBy Query Parameters: {}", queryParams);

        HttpResponse jsonResponse = _webServiceClient
                .withPath(_configuration.urlPath())
                .withQueries(queryParams)
                .request()
                .accept(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .contentType(JsonClientRequestContentType.APPLICATION_JSON.toString())
                .method("GET")
                .response();

        String jsonResponseBody = jsonResponse.body(asString());
        _logger.debug("Received database clients JSON response: {}", jsonResponseBody);

        List<Map> objects = (List<Map>) _json.fromJsonArray(jsonResponseBody);

        List<DatabaseClientAttributes> databaseClientList = objects.stream()
                .map(DatabaseClientAttributes::from)
                .toList();

        return new PaginatedDataAccessResult<>(databaseClientList, paginationRequest != null ? paginationRequest.getCursor() : null);
    }

    private Map<String, Collection<String>> prepareQueryParamsMap(DatabaseClientAttributesFiltering filters,
                                                                  PaginationRequest paginationRequest,
                                                                  DatabaseClientAttributesSorting sortRequest,
                                                                  boolean activeClientsOnly)
    {
        Map<String, Collection<String>> queryParams = new HashMap<>();
        // Add activeClientsOnly if filters is not null
        if (filters != null)
        {
            putIfNotNull(queryParams, "activeClientsOnly", String.valueOf(activeClientsOnly));
            putIfNotNull(queryParams, "client_name_filter", filters.getClientNameFilter());
            putIfNotNull(queryParams, "search_terms_filter", filters.getSearchTermsFilter());
            putIfNotEmpty(queryParams, "tags_filter", filters.getTagsFilter());
        }

        // Add pagination parameters if paginationRequest is not null
        if (paginationRequest != null)
        {
            putIfNotNull(queryParams, "count", String.valueOf(paginationRequest.getCount()));
            putIfNotNull(queryParams, "cursor", paginationRequest.getCursor());
        }

        // Add sorting parameters if sortRequest is not null
        if (sortRequest != null)
        {
            putIfNotNull(queryParams, "sort_by", sortRequest.getSortBy());
            putIfNotNull(queryParams, "sort_order", sortRequest.getSortOrder().name());
            putIfNotNull(queryParams, "secondary_sort_by", sortRequest.getSecondarySortBy());
        }

        return queryParams;
    }

    @Override
    public long getClientCountBy(String profileId, @Nullable DatabaseClientAttributesFiltering filters, boolean activeClientsOnly)
    {
        return 0;
    }
}