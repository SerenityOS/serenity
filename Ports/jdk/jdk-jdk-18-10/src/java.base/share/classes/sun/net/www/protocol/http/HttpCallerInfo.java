/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.http;

import java.net.Authenticator;
import java.net.Authenticator.RequestorType;
import java.net.InetAddress;
import java.net.URL;

/**
 * Used in HTTP/Negotiate, to feed HTTP request info into JGSS as a HttpCaller,
 * so that special actions can be taken, including special callback handler,
 * special useSubjectCredsOnly value.
 *
 * This is an immutable class. It can be instantiated in two styles;
 *
 * 1. Un-schemed: Create at the beginning before the preferred scheme is
 * determined. This object can be fed into AuthenticationHeader to check
 * for the preference.
 *
 * 2. Schemed: With the scheme field filled, can be used in JGSS-API calls.
 */
public final class HttpCallerInfo {
    // All info that an Authenticator needs.
    public final URL url;
    public final String host, protocol, prompt, scheme;
    public final int port;
    public final InetAddress addr;
    public final RequestorType authType;
    public final Authenticator authenticator;

    /**
     * Create a schemed object based on an un-schemed one.
     */
    public HttpCallerInfo(HttpCallerInfo old, String scheme) {
        this.url = old.url;
        this.host = old.host;
        this.protocol = old.protocol;
        this.prompt = old.prompt;
        this.port = old.port;
        this.addr = old.addr;
        this.authType = old.authType;
        this.scheme = scheme;
        this.authenticator =  old.authenticator;
    }

    /**
     * Constructor an un-schemed object for site access.
     */
    public HttpCallerInfo(URL url, Authenticator a) {
        this.url= url;
        prompt = "";
        host = url.getHost();

        int p = url.getPort();
        if (p == -1) {
            port = url.getDefaultPort();
        } else {
            port = p;
        }

        InetAddress ia;
        try {
            ia = InetAddress.getByName(url.getHost());
        } catch (Exception e) {
            ia = null;
        }
        addr = ia;

        protocol = url.getProtocol();
        authType = RequestorType.SERVER;
        scheme = "";
        authenticator = a;
    }

    /**
     * Constructor an un-schemed object for proxy access.
     */
    public HttpCallerInfo(URL url, String host, int port, Authenticator a) {
        this.url= url;
        this.host = host;
        this.port = port;
        prompt = "";
        addr = null;
        protocol = url.getProtocol();
        authType = RequestorType.PROXY;
        scheme = "";
        authenticator = a;
    }
}
