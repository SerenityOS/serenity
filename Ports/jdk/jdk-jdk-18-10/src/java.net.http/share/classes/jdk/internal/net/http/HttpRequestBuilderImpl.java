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

package jdk.internal.net.http;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;

import jdk.internal.net.http.common.HttpHeadersBuilder;
import jdk.internal.net.http.common.Utils;
import static java.util.Objects.requireNonNull;
import static jdk.internal.net.http.common.Utils.isValidName;
import static jdk.internal.net.http.common.Utils.isValidValue;
import static jdk.internal.net.http.common.Utils.newIAE;

public class HttpRequestBuilderImpl implements HttpRequest.Builder {

    private HttpHeadersBuilder headersBuilder;
    private URI uri;
    private String method;
    private boolean expectContinue;
    private BodyPublisher bodyPublisher;
    private volatile Optional<HttpClient.Version> version;
    private Duration duration;

    public HttpRequestBuilderImpl(URI uri) {
        requireNonNull(uri, "uri must be non-null");
        checkURI(uri);
        this.uri = uri;
        this.headersBuilder = new HttpHeadersBuilder();
        this.method = "GET"; // default, as per spec
        this.version = Optional.empty();
    }

    public HttpRequestBuilderImpl() {
        this.headersBuilder = new HttpHeadersBuilder();
        this.method = "GET"; // default, as per spec
        this.version = Optional.empty();
    }

    @Override
    public HttpRequestBuilderImpl uri(URI uri) {
        requireNonNull(uri, "uri must be non-null");
        checkURI(uri);
        this.uri = uri;
        return this;
    }

    static void checkURI(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null)
            throw newIAE("URI with undefined scheme");
        scheme = scheme.toLowerCase(Locale.US);
        if (!(scheme.equals("https") || scheme.equals("http"))) {
            throw newIAE("invalid URI scheme %s", scheme);
        }
        if (uri.getHost() == null) {
            throw newIAE("unsupported URI %s", uri);
        }
    }

    @Override
    public HttpRequestBuilderImpl copy() {
        HttpRequestBuilderImpl b = new HttpRequestBuilderImpl();
        b.uri = this.uri;
        b.headersBuilder = this.headersBuilder.structuralCopy();
        b.method = this.method;
        b.expectContinue = this.expectContinue;
        b.bodyPublisher = bodyPublisher;
        b.uri = uri;
        b.duration = duration;
        b.version = version;
        return b;
    }

    private void checkNameAndValue(String name, String value) {
        requireNonNull(name, "name");
        requireNonNull(value, "value");
        if (!isValidName(name)) {
            throw newIAE("invalid header name: \"%s\"", name);
        }
        if (!Utils.ALLOWED_HEADERS.test(name, null)) {
            throw newIAE("restricted header name: \"%s\"", name);
        }
        if (!isValidValue(value)) {
            throw newIAE("invalid header value: \"%s\"", value);
        }
    }

    @Override
    public HttpRequestBuilderImpl setHeader(String name, String value) {
        checkNameAndValue(name, value);
        headersBuilder.setHeader(name, value);
        return this;
    }

    @Override
    public HttpRequestBuilderImpl header(String name, String value) {
        checkNameAndValue(name, value);
        headersBuilder.addHeader(name, value);
        return this;
    }

    @Override
    public HttpRequestBuilderImpl headers(String... params) {
        requireNonNull(params);
        if (params.length == 0 || params.length % 2 != 0) {
            throw newIAE("wrong number, %d, of parameters", params.length);
        }
        for (int i = 0; i < params.length; i += 2) {
            String name  = params[i];
            String value = params[i + 1];
            header(name, value);
        }
        return this;
    }

    @Override
    public HttpRequestBuilderImpl expectContinue(boolean enable) {
        expectContinue = enable;
        return this;
    }

    @Override
    public HttpRequestBuilderImpl version(HttpClient.Version version) {
        requireNonNull(version);
        this.version = Optional.of(version);
        return this;
    }

    HttpHeadersBuilder headersBuilder() {  return headersBuilder; }

    URI uri() { return uri; }

    String method() { return method; }

    boolean expectContinue() { return expectContinue; }

    BodyPublisher bodyPublisher() { return bodyPublisher; }

    Optional<HttpClient.Version> version() { return version; }

    @Override
    public HttpRequest.Builder GET() {
        return method0("GET", null);
    }

    @Override
    public HttpRequest.Builder POST(BodyPublisher body) {
        return method0("POST", requireNonNull(body));
    }

    @Override
    public HttpRequest.Builder DELETE() {
        return method0("DELETE", null);
    }

    @Override
    public HttpRequest.Builder PUT(BodyPublisher body) {
        return method0("PUT", requireNonNull(body));
    }

    @Override
    public HttpRequest.Builder method(String method, BodyPublisher body) {
        requireNonNull(method);
        if (method.isEmpty())
            throw newIAE("illegal method <empty string>");
        if (method.equals("CONNECT"))
            throw newIAE("method CONNECT is not supported");
        if (!Utils.isValidName(method))
            throw newIAE("illegal method \""
                    + method.replace("\n","\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                    + "\"");
        return method0(method, requireNonNull(body));
    }

    private HttpRequest.Builder method0(String method, BodyPublisher body) {
        assert method != null;
        assert !method.isEmpty();
        this.method = method;
        this.bodyPublisher = body;
        return this;
    }

    @Override
    public HttpRequest build() {
        if (uri == null)
            throw new IllegalStateException("uri is null");
        assert method != null;
        return new ImmutableHttpRequest(this);
    }

    public HttpRequestImpl buildForWebSocket() {
        if (uri == null)
            throw new IllegalStateException("uri is null");
        assert method != null;
        return new HttpRequestImpl(this);
    }

    @Override
    public HttpRequest.Builder timeout(Duration duration) {
        requireNonNull(duration);
        if (duration.isNegative() || Duration.ZERO.equals(duration))
            throw new IllegalArgumentException("Invalid duration: " + duration);
        this.duration = duration;
        return this;
    }

    Duration timeout() { return duration; }

}
