/*
 * Copyright (C) 2024 Curity AB. All rights reserved.
 *
 * The contents of this file are the property of Curity AB.
 * You may not copy or use this file, in either source code
 * or executable form, except in compliance with terms
 * set by Curity AB.
 *
 * For further information, please contact Curity AB.
 */

package io.curity.identityserver.plugin.data.access.json;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

final class CollectionUtils
{
    private CollectionUtils()
    {
    }

    static Map<String, Collection<String>> toMultiMap(Map<String, String> simpleMap)
    {
        return simpleMap.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, (entry) -> singletonList(entry.getValue())));
    }

    static String[] toArray(Map<String, String> map)
    {
        String[] result = new String[map.size() * 2];

        int i = 0;

        for (Map.Entry<String, String> entry : map.entrySet())
        {
            result[i++] = entry.getKey();
            result[i++] = entry.getValue();
        }

        return result;
    }

    // Helper method to put a single value in the map if the value is not null
    static void putIfNotNull(Map<String, Collection<String>> map, String key, String value)
    {
        if (value != null && !value.isEmpty())
        {
            map.put(key, Collections.singletonList(value));
        }
    }

    // Helper method to put a collection in the map if the collection is not empty or null
    static void putIfNotEmpty(Map<String, Collection<String>> map, String key, Collection<String> values)
    {
        if (values != null && !values.isEmpty())
        {
            map.put(key, values);
        }
    }
}
