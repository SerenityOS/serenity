/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.https;

import java.net.URL;
import java.net.Proxy;
import java.io.IOException;

/**
 * This class was introduced to provide an additional level of
 * abstraction between javax.net.ssl.HttpURLConnection and
 * com.sun.net.ssl.HttpURLConnection objects. <p>
 *
 * javax.net.ssl.HttpURLConnection is used in the new sun.net version
 * of protocol implementation (this one)
 * com.sun.net.ssl.HttpURLConnection is used in the com.sun version.
 *
 */
public class DelegateHttpsURLConnection extends AbstractDelegateHttpsURLConnection {

    // we need a reference to the HttpsURLConnection to get
    // the properties set there
    // we also need it to be public so that it can be referenced
    // from sun.net.www.protocol.http.HttpURLConnection
    // this is for ResponseCache.put(URI, URLConnection)
    // second parameter needs to be cast to javax.net.ssl.HttpsURLConnection
    // instead of AbstractDelegateHttpsURLConnection
    public javax.net.ssl.HttpsURLConnection httpsURLConnection;

    DelegateHttpsURLConnection(URL url,
            sun.net.www.protocol.http.Handler handler,
            javax.net.ssl.HttpsURLConnection httpsURLConnection)
            throws IOException {
        this(url, null, handler, httpsURLConnection);
    }

    DelegateHttpsURLConnection(URL url, Proxy p,
            sun.net.www.protocol.http.Handler handler,
            javax.net.ssl.HttpsURLConnection httpsURLConnection)
            throws IOException {
        super(url, p, handler);
        this.httpsURLConnection = httpsURLConnection;
    }

    protected javax.net.ssl.SSLSocketFactory getSSLSocketFactory() {
        return httpsURLConnection.getSSLSocketFactory();
    }

    protected javax.net.ssl.HostnameVerifier getHostnameVerifier() {
        return httpsURLConnection.getHostnameVerifier();
    }
}
