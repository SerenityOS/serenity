/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package build.tools.pandocfilter.json;

import java.util.*;
import java.util.stream.Collectors;

public class JSONObject implements JSONValue {
    public static class Field {
        private final String name;
        private final JSONValue value;

        private Field(String name, JSONValue value) {
            this.name = name;
            this.value = value;
        }

        public String name() {
            return name;
        }

        public JSONValue value() {
            return value;
        }
    }

    private final Map<String, JSONValue> value;

    public JSONObject() {
        this.value = new HashMap<String, JSONValue>();
    }

    public JSONObject(Map<String, JSONValue> map) {
        this.value = new HashMap<String, JSONValue>(map);
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public JSONObject asObject() {
        return this;
    }

    public JSONObject put(String k, boolean v) {
        value.put(k, JSON.of(v));
        return this;
    }

    public JSONObject put(String k, int v) {
        value.put(k, JSON.of(v));
        return this;
    }

    public JSONObject put(String k, long v) {
        value.put(k, JSON.of(v));
        return this;
    }

    public JSONObject put(String k, String v) {
        value.put(k, JSON.of(v));
        return this;
    }

    public JSONObject put(String k, double v) {
        value.put(k, JSON.of(v));
        return this;
    }

    public JSONObject put(String k, JSONArray v) {
        value.put(k, v);
        return this;
    }

    public JSONObject put(String k, JSONObject v) {
        value.put(k, v);
        return this;
    }

    public JSONObject put(String k, JSONValue v) {
        value.put(k, v);
        return this;
    }

    public JSONObject putNull(String k) {
        value.put(k, JSON.of());
        return this;
    }

    public JSONValue remove(String k) {
        return value.remove(k);
    }

    public JSONValue get(String k) {
        return value.get(k);
    }

    public List<Field> fields() {
        return value.entrySet()
                    .stream()
                    .map(e -> new Field(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
    }

    public boolean contains(String field) {
        return value.containsKey(field);
    }

    public Set<String> keys() {
        return value.keySet();
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append("{");
        for (var key : value.keySet()) {
            builder.append("\"");
            builder.append(key);
            builder.append("\":");
            builder.append(value.get(key).toString());
            builder.append(",");
        }

        var end = builder.length() - 1;
        if (builder.charAt(end) == ',') {
            builder.deleteCharAt(end);
        }

        builder.append("}");
        return builder.toString();
    }
}
