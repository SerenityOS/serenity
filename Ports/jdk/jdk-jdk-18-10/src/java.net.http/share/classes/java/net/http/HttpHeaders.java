/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.net.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A read-only view of a set of HTTP headers.
 *
 * <p> An {@code HttpHeaders} is not typically created directly, but rather
 * returned from an {@link HttpRequest#headers() HttpRequest} or an
 * {@link HttpResponse#headers() HttpResponse}. Specific HTTP headers can be
 * set for a {@linkplain HttpRequest request} through one of the request
 * builder's {@link HttpRequest.Builder#header(String, String) headers} methods.
 *
 * <p> The methods of this class ( that accept a String header name ), and the
 * {@code Map} returned by the {@link #map() map} method, operate without regard
 * to case when retrieving the header value(s).
 *
 * <p> An HTTP header name may appear more than once in the HTTP protocol. As
 * such, headers are represented as a name and a list of values. Each occurrence
 * of a header value is added verbatim, to the appropriate header name list,
 * without interpreting its value. In particular, {@code HttpHeaders} does not
 * perform any splitting or joining of comma separated header value strings. The
 * order of elements in a header value list is preserved when {@link
 * HttpRequest.Builder#header(String, String) building} a request. For
 * responses, the order of elements in a header value list is the order in which
 * they were received. The {@code Map} returned by the {@code map} method,
 * however, does not provide any guarantee with regard to the ordering of its
 * entries.
 *
 * <p> {@code HttpHeaders} instances are immutable.
 *
 * @since 11
 */
public final class HttpHeaders {

    /**
     * Returns an {@link Optional} containing the first header string value of
     * the given named (and possibly multi-valued) header. If the header is not
     * present, then the returned {@code Optional} is empty.
     *
     * @param name the header name
     * @return an {@code Optional<String>} containing the first named header
     *         string value, if present
     */
    public Optional<String> firstValue(String name) {
        return allValues(name).stream().findFirst();
    }

    /**
     * Returns an {@link OptionalLong} containing the first header string value
     * of the named header field. If the header is not present, then the
     * Optional is empty. If the header is present but contains a value that
     * does not parse as a {@code Long} value, then an exception is thrown.
     *
     * @param name the header name
     * @return  an {@code OptionalLong}
     * @throws NumberFormatException if a value is found, but does not parse as
     *                               a Long
     */
    public OptionalLong firstValueAsLong(String name) {
        return allValues(name).stream().mapToLong(Long::valueOf).findFirst();
    }

    /**
     * Returns an unmodifiable List of all of the header string values of the
     * given named header. Always returns a List, which may be empty if the
     * header is not present.
     *
     * @param name the header name
     * @return a List of headers string values
     */
    public List<String> allValues(String name) {
        requireNonNull(name);
        List<String> values = map().get(name);
        // Making unmodifiable list out of empty in order to make a list which
        // throws UOE unconditionally
        return values != null ? values : List.of();
    }

    /**
     * Returns an unmodifiable multi Map view of this HttpHeaders.
     *
     * @return the Map
     */
    public Map<String,List<String>> map() {
        return headers;
    }

    /**
     * Tests this HTTP headers instance for equality with the given object.
     *
     * <p> If the given object is not an {@code HttpHeaders} then this
     * method returns {@code false}. Two HTTP headers are equal if each
     * of their corresponding {@linkplain #map() maps} are equal.
     *
     * <p> This method satisfies the general contract of the {@link
     * Object#equals(Object) Object.equals} method.
     *
     * @param obj the object to which this object is to be compared
     * @return {@code true} if, and only if, the given object is an {@code
     *         HttpHeaders} that is equal to this HTTP headers
     */
    public final boolean equals(Object obj) {
        if (!(obj instanceof HttpHeaders))
            return false;
        HttpHeaders that = (HttpHeaders)obj;
        return this.map().equals(that.map());
    }

    /**
     * Computes a hash code for this HTTP headers instance.
     *
     * <p> The hash code is based upon the components of the HTTP headers
     * {@link #map() map}, and satisfies the general contract of the
     * {@link Object#hashCode Object.hashCode} method.
     *
     * @return the hash-code value for this HTTP headers
     */
    public final int hashCode() {
        int h = 0;
        for (Map.Entry<String, List<String>> e : map().entrySet()) {
            h += entryHash(e);
        }
        return h;
    }

    /**
     * Returns this HTTP headers as a string.
     *
     * @return a string describing the HTTP headers
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(" { ");
        sb.append(map());
        sb.append(" }");
        return sb.toString();
    }

    /**
     * Returns an HTTP headers from the given map. The given map's key
     * represents the header name, and its value the list of string header
     * values for that header name.
     *
     * <p> An HTTP header name may appear more than once in the HTTP protocol.
     * Such, <i>multi-valued</i>, headers must be represented by a single entry
     * in the given map, whose entry value is a list that represents the
     * multiple header string values. Leading and trailing whitespaces are
     * removed from all string values retrieved from the given map and its lists
     * before processing. Only headers that, after filtering, contain at least
     * one, possibly empty string, value will be added to the HTTP headers.
     *
     * @apiNote The primary purpose of this method is for testing frameworks.
     * Per-request headers can be set through one of the {@code HttpRequest}
     * {@link HttpRequest.Builder#header(String, String) headers} methods.
     *
     * @param headerMap the map containing the header names and values
     * @param filter a filter that can be used to inspect each
     *               header-name-and-value pair in the given map to determine if
     *               it should, or should not, be added to the to the HTTP
     *               headers
     * @return an HTTP headers instance containing the given headers
     * @throws NullPointerException if any of: {@code headerMap}, a key or value
     *        in the given map, or an entry in the map's value list, or
     *        {@code filter}, is {@code null}
     * @throws IllegalArgumentException if the given {@code headerMap} contains
     *         any two keys that are equal ( without regard to case ); or if the
     *         given map contains any key whose length, after trimming
     *         whitespaces, is {@code 0}
     */
    public static HttpHeaders of(Map<String,List<String>> headerMap,
                                 BiPredicate<String,String> filter) {
        requireNonNull(headerMap);
        requireNonNull(filter);
        return headersOf(headerMap, filter);
    }

    // --

    private static final HttpHeaders NO_HEADERS = new HttpHeaders(Map.of());

    private final Map<String,List<String>> headers;

    private HttpHeaders(Map<String,List<String>> headers) {
        this.headers = headers;
    }

    private static final int entryHash(Map.Entry<String, List<String>> e) {
        String key = e.getKey();
        List<String> value = e.getValue();
        // we know that by construction key and values can't be null
        int keyHash = key.toLowerCase(Locale.ROOT).hashCode();
        int valueHash = value.hashCode();
        return keyHash ^ valueHash;
    }

    // Returns a new HTTP headers after performing a structural copy and filtering.
    private static HttpHeaders headersOf(Map<String,List<String>> map,
                                         BiPredicate<String,String> filter) {
        TreeMap<String,List<String>> other = new TreeMap<>(CASE_INSENSITIVE_ORDER);
        TreeSet<String> notAdded = new TreeSet<>(CASE_INSENSITIVE_ORDER);
        ArrayList<String> tempList = new ArrayList<>();
        map.forEach((key, value) -> {
            String headerName = requireNonNull(key).trim();
            if (headerName.isEmpty()) {
                throw new IllegalArgumentException("empty key");
            }
            List<String> headerValues = requireNonNull(value);
            headerValues.forEach(headerValue -> {
                headerValue = requireNonNull(headerValue).trim();
                if (filter.test(headerName, headerValue)) {
                    tempList.add(headerValue);
                }
            });

            if (tempList.isEmpty()) {
                if (other.containsKey(headerName)
                        || notAdded.contains(headerName.toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("duplicate key: " + headerName);
                notAdded.add(headerName.toLowerCase(Locale.ROOT));
            } else if (other.put(headerName, List.copyOf(tempList)) != null) {
                throw new IllegalArgumentException("duplicate key: " + headerName);
            }
            tempList.clear();
        });
        return other.isEmpty() ? NO_HEADERS : new HttpHeaders(unmodifiableMap(other));
    }
}
