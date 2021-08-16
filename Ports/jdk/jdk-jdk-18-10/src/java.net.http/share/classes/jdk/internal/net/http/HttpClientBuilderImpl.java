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

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;
import jdk.internal.net.http.common.Utils;
import static java.util.Objects.requireNonNull;

public class HttpClientBuilderImpl implements HttpClient.Builder {

    CookieHandler cookieHandler;
    Duration connectTimeout;
    HttpClient.Redirect followRedirects;
    ProxySelector proxy;
    Authenticator authenticator;
    HttpClient.Version version;
    Executor executor;
    // Security parameters
    SSLContext sslContext;
    SSLParameters sslParams;
    int priority = -1;

    @Override
    public HttpClientBuilderImpl cookieHandler(CookieHandler cookieHandler) {
        requireNonNull(cookieHandler);
        this.cookieHandler = cookieHandler;
        return this;
    }

    @Override
    public HttpClientBuilderImpl connectTimeout(Duration duration) {
        requireNonNull(duration);
        if (duration.isNegative() || Duration.ZERO.equals(duration))
            throw new IllegalArgumentException("Invalid duration: " + duration);
        this.connectTimeout = duration;
        return this;
    }

    @Override
    public HttpClientBuilderImpl sslContext(SSLContext sslContext) {
        requireNonNull(sslContext);
        this.sslContext = sslContext;
        return this;
    }


    @Override
    public HttpClientBuilderImpl sslParameters(SSLParameters sslParameters) {
        requireNonNull(sslParameters);
        this.sslParams = Utils.copySSLParameters(sslParameters);
        return this;
    }


    @Override
    public HttpClientBuilderImpl executor(Executor s) {
        requireNonNull(s);
        this.executor = s;
        return this;
    }


    @Override
    public HttpClientBuilderImpl followRedirects(HttpClient.Redirect policy) {
        requireNonNull(policy);
        this.followRedirects = policy;
        return this;
    }


    @Override
    public HttpClientBuilderImpl version(HttpClient.Version version) {
        requireNonNull(version);
        this.version = version;
        return this;
    }


    @Override
    public HttpClientBuilderImpl priority(int priority) {
        if (priority < 1 || priority > 256) {
            throw new IllegalArgumentException("priority must be between 1 and 256");
        }
        this.priority = priority;
        return this;
    }

    @Override
    public HttpClientBuilderImpl proxy(ProxySelector proxy) {
        requireNonNull(proxy);
        this.proxy = proxy;
        return this;
    }


    @Override
    public HttpClientBuilderImpl authenticator(Authenticator a) {
        requireNonNull(a);
        this.authenticator = a;
        return this;
    }

    @Override
    public HttpClient build() {
        return HttpClientImpl.create(this);
    }
}
