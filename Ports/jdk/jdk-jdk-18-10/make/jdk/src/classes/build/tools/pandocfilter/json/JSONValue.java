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

import java.util.stream.Stream;
import java.util.List;

public interface JSONValue {
    default int asInt() {
        throw new IllegalStateException("Unsupported conversion to int");
    }

    default long asLong() {
        throw new IllegalStateException("Unsupported conversion to long");
    }

    default double asDouble() {
        throw new IllegalStateException("Unsupported conversion to double");
    }

    default String asString() {
        throw new IllegalStateException("Unsupported conversion to String");
    }

    default boolean asBoolean() {
        throw new IllegalStateException("Unsupported conversion to boolean");
    }

    default JSONArray asArray() {
        throw new IllegalStateException("Unsupported conversion to array");
    }

    default JSONObject asObject() {
        throw new IllegalStateException("Unsupported conversion to object");
    }

    default boolean isInt() {
        return false;
    }

    default boolean isLong() {
        return false;
    }

    default boolean isDouble() {
        return false;
    }

    default boolean isString() {
        return false;
    }

    default boolean isBoolean() {
        return false;
    }

    default boolean isArray() {
        return false;
    }

    default boolean isObject() {
        return false;
    }

    default boolean isNull() {
        return false;
    }

    default List<JSONObject.Field> fields() {
        return asObject().fields();
    }

    default boolean contains(String key) {
        return asObject().contains(key);
    }

    default JSONValue get(String key) {
        return asObject().get(key);
    }

    default JSONValue get(int i) {
        return asArray().get(i);
    }

    default Stream<JSONValue> stream() {
        return Stream.of(this);
    }

    static JSONValue from(int i) {
        return new JSONNumber(i);
    }

    static JSONValue from(long l) {
        return new JSONNumber(l);
    }

    static JSONValue from(double d) {
        return new JSONDecimal(d);
    }

    static JSONValue from(boolean b) {
        return new JSONBoolean(b);
    }

    static JSONValue from(String s) {
        return new JSONString(s);
    }

    static JSONValue fromNull() {
        return new JSONNull();
    }
}
