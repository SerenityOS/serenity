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
import java.util.stream.Stream;

public class JSONArray implements JSONValue, Iterable<JSONValue> {
    private final List<JSONValue> values;

    public JSONArray() {
        this.values = new ArrayList<JSONValue>();
    }

    public JSONArray(JSONValue[] array) {
        this.values = new ArrayList<JSONValue>(array.length);
        for (var v : array) {
            values.add(v);
        }
    }

    private void append(JSONValue value) {
        if (value instanceof JSONArray) {
            for (var v : value.asArray()) {
                append(v);
            }
        } else {
            this.values.add(value);
        }
    }

    public JSONArray(JSONValue value, JSONValue... values) {
        this.values = new ArrayList<JSONValue>(values.length + 1);
        append(value);
        for (var v : values) {
            append(v);
        }
    }

    public JSONArray(List<JSONValue> values) {
        this.values = new ArrayList<JSONValue>(values);
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public JSONArray asArray() {
        return this;
    }

    public JSONArray set(int i, boolean value) {
        values.set(i, JSON.of(value));
        return this;
    }

    public JSONArray set(int i, int value) {
        values.set(i, JSON.of(value));
        return this;
    }

    public JSONArray set(int i, long value) {
        values.set(i, JSON.of(value));
        return this;
    }

    public JSONArray set(int i, String value) {
        values.set(i, JSON.of(value));
        return this;
    }

    public JSONArray set(int i, double value) {
        values.set(i, JSON.of(value));
        return this;
    }

    public JSONArray set(int i, JSONValue value) {
        values.set(i, value);
        return this;
    }

    public JSONArray setNull(int i) {
        values.set(i, JSON.of());
        return this;
    }

    public JSONArray add(boolean value) {
        values.add(JSON.of(value));
        return this;
    }

    public JSONArray add(int value) {
        values.add(JSON.of(value));
        return this;
    }

    public JSONArray add(long value) {
        values.add(JSON.of(value));
        return this;
    }

    public JSONArray add(String value) {
        values.add(JSON.of(value));
        return this;
    }

    public JSONArray add(double value) {
        values.add(JSON.of(value));
        return this;
    }

    public JSONArray add(JSONValue value) {
        values.add(value);
        return this;
    }

    public JSONArray addNull() {
        values.add(JSON.of());
        return this;
    }

    public JSONValue get(int i) {
        return values.get(i);
    }

    public int size() {
        return values.size();
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();

        builder.append("[");
        for (var i = 0; i < size(); i++) {
            builder.append(get(i).toString());
            if (i != (size() - 1)) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public Stream<JSONValue> stream() {
        return values.stream();
    }

    @Override
    public Iterator<JSONValue> iterator() {
        return values.iterator();
    }
}
