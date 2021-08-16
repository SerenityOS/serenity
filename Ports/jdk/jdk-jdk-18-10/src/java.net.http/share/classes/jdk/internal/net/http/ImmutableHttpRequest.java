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

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.net.http.HttpClient.Version;
import static jdk.internal.net.http.common.Utils.ALLOWED_HEADERS;

final class ImmutableHttpRequest extends HttpRequest {

    private final String method;
    private final URI uri;
    private final HttpHeaders headers;
    private final Optional<BodyPublisher> requestPublisher;
    private final boolean expectContinue;
    private final Optional<Duration> timeout;
    private final Optional<Version> version;

    /** Creates an ImmutableHttpRequest from the given builder. */
    ImmutableHttpRequest(HttpRequestBuilderImpl builder) {
        this.method = Objects.requireNonNull(builder.method());
        this.uri = Objects.requireNonNull(builder.uri());
        this.headers = HttpHeaders.of(builder.headersBuilder().map(), ALLOWED_HEADERS);
        this.requestPublisher = Optional.ofNullable(builder.bodyPublisher());
        this.expectContinue = builder.expectContinue();
        this.timeout = Optional.ofNullable(builder.timeout());
        this.version = Objects.requireNonNull(builder.version());
    }

    @Override
    public String method() { return method; }

    @Override
    public URI uri() { return uri; }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public Optional<BodyPublisher> bodyPublisher() { return requestPublisher; }

    @Override
    public boolean expectContinue() { return expectContinue; }

    @Override
    public Optional<Duration> timeout() { return timeout; }

    @Override
    public Optional<Version> version() { return version; }

    @Override
    public String toString() {
        return uri.toString() + " " + method;
    }
}
