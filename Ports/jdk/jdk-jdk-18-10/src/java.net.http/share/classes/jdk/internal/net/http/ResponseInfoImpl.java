/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import java.net.http.HttpResponse.ResponseInfo;
import java.net.http.HttpHeaders;
import java.net.http.HttpClient;

class ResponseInfoImpl implements ResponseInfo {
    private final int statusCode;
    private final HttpHeaders headers;
    private final HttpClient.Version version;

    ResponseInfoImpl(Response response) {
        this.statusCode = response.statusCode();
        this.headers = response.headers();
        this.version = response.version();
    }

    ResponseInfoImpl(int statusCode, HttpHeaders headers, HttpClient.Version version) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.version = version;
    }

    /**
     * Provides the response status code
     * @return the response status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Provides the response headers
     * @return the response headers
     */
    public HttpHeaders headers() {
        return headers;
    }

    /**
     * provides the response protocol version
     * @return the response protocol version
     */
    public HttpClient.Version version() {
        return version;
    }
}
