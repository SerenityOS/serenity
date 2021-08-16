/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.net.httpserver;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * HTTP request and response headers are represented by this class which
 * implements the interface
 * {@link java.util.Map}{@literal <}{@link java.lang.String},
 * {@link java.util.List} {@literal <}{@link java.lang.String}{@literal >>}.
 * The keys are case-insensitive Strings representing the header names and
 * the value associated with each key is
 * a {@link List}{@literal <}{@link String}{@literal >} with one
 * element for each occurrence of the header name in the request or response.
 *
 * <p> For example, if a response header instance contains
 * one key "HeaderName" with two values "value1 and value2"
 * then this object is output as two header lines:
 *
 * <blockquote><pre>
 * HeaderName: value1
 * HeaderName: value2
 * </pre></blockquote>
 *
 * All the normal {@link java.util.Map} methods are provided, but the
 * following additional convenience methods are most likely to be used:
 *
 * <ul>
 *     <li>{@link #getFirst(String)} returns a single valued header or the first
 *     value of a multi-valued header.
 *     <li>{@link #add(String,String)} adds the given header value to the list
 *     for the given key.
 *     <li>{@link #set(String,String)} sets the given header field to the single
 *     value given overwriting any existing values in the value list.
 * </ul>
 *
 * <p> All methods in this class reject {@code null} values for keys and values.
 * {@code null} keys will never be present in HTTP request or response headers.
 * @since 1.6
 */
public class Headers implements Map<String,List<String>> {

    HashMap<String,List<String>> map;

    /**
     * Creates an empty instance of {@code Headers}.
     */
    public Headers() {map = new HashMap<>(32);}

    /**
     * Normalize the key by converting to following form.
     * First {@code char} upper case, rest lower case.
     * key is presumed to be {@code ASCII}.
     */
    private String normalize(String key) {
        Objects.requireNonNull(key);
        int len = key.length();
        if (len == 0) {
            return key;
        }
        char[] b = key.toCharArray();
        if (b[0] >= 'a' && b[0] <= 'z') {
            b[0] = (char)(b[0] - ('a' - 'A'));
        } else if (b[0] == '\r' || b[0] == '\n')
            throw new IllegalArgumentException("illegal character in key");

        for (int i=1; i<len; i++) {
            if (b[i] >= 'A' && b[i] <= 'Z') {
                b[i] = (char) (b[i] + ('a' - 'A'));
            } else if (b[i] == '\r' || b[i] == '\n')
                throw new IllegalArgumentException("illegal character in key");
        }
        return new String(b);
    }

    @Override
    public int size() {return map.size();}

    @Override
    public boolean isEmpty() {return map.isEmpty();}

    @Override
    public boolean containsKey(Object key) {
        Objects.requireNonNull(key);
        return key instanceof String k && map.containsKey(normalize(k));
    }

    @Override
    public boolean containsValue(Object value) {
        Objects.requireNonNull(value);
        return map.containsValue(value);
    }

    @Override
    public List<String> get(Object key) {
        return map.get(normalize((String)key));
    }

    /**
     * Returns the first value from the {@link List} of {@code String} values
     * for the given {@code key}, or {@code null} if no mapping for the
     * {@code key} exists.
     *
     * @param key the key to search for
     * @return    the first {@code String} value associated with the key,
     *            or {@code null} if no mapping for the key exists
     */
    public String getFirst(String key) {
        List<String> l = map.get(normalize(key));
        if (l == null || l.size() == 0) {  // no mapping exists
            return null;
        }
        return l.get(0);
    }

    @Override
    public List<String> put(String key, List<String> value) {
        for (String v : value)
            checkValue(v);
        return map.put(normalize(key), value);
    }

    /**
     * Adds the given {@code value} to the list of headers for the given
     * {@code key}. If the mapping does not already exist, then it is created.
     *
     * @param key   the header name
     * @param value the value to add to the header
     */
    public void add(String key, String value) {
        checkValue(value);
        String k = normalize(key);
        List<String> l = map.get(k);
        if (l == null) {
            l = new LinkedList<>();
            map.put(k,l);
        }
        l.add(value);
    }

    private static void checkValue(String value) {
        int len = value.length();
        for (int i=0; i<len; i++) {
            char c = value.charAt(i);
            if (c == '\r') {
                // is allowed if it is followed by \n and a whitespace char
                if (i >= len - 2) {
                    throw new IllegalArgumentException("Illegal CR found in header");
                }
                char c1 = value.charAt(i+1);
                char c2 = value.charAt(i+2);
                if (c1 != '\n') {
                    throw new IllegalArgumentException("Illegal char found after CR in header");
                }
                if (c2 != ' ' && c2 != '\t') {
                    throw new IllegalArgumentException("No whitespace found after CRLF in header");
                }
                i+=2;
            } else if (c == '\n') {
                throw new IllegalArgumentException("Illegal LF found in header");
            }
        }
    }

    /**
     * Sets the given {@code value} as the sole header value for the given
     * {@code key}. If the mapping does not already exist, then it is created.
     *
     * @param key   the header name
     * @param value the header value to set
     */
    public void set(String key, String value) {
        LinkedList<String> l = new LinkedList<>();
        l.add(value);
        put(key, l);
    }

    @Override
    public List<String> remove(Object key) {
        return map.remove(normalize((String)key));
    }

    @Override
    public void putAll(Map<? extends String,? extends List<String>> t)  {
        t.forEach(this::put);
    }

    @Override
    public void clear() {map.clear();}

    @Override
    public Set<String> keySet() {return map.keySet();}

    @Override
    public Collection<List<String>> values() {return map.values();}

    @Override
    public Set<Map.Entry<String, List<String>>> entrySet() {
        return map.entrySet();
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super List<String>, ? extends List<String>> function) {
        var f = function.andThen(values -> {
            Objects.requireNonNull(values);
            values.forEach(Headers::checkValue);
            return values;
        });
        Map.super.replaceAll(f);
    }

    @Override
    public boolean equals(Object o) { return map.equals(o); }

    @Override
    public int hashCode() {return map.hashCode();}

    @Override
    public String toString() {
        final var sb = new StringBuilder(Headers.class.getSimpleName());
        sb.append(" { ");
        sb.append(map.toString());
        sb.append(" }");
        return sb.toString();
    }
}
