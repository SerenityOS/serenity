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

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import jdk.internal.net.http.common.SSLTube;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Utils;
import static jdk.internal.net.http.common.Utils.ServerName;

/**
 * Asynchronous version of SSLConnection.
 *
 * There are two concrete implementations of this class: AsyncSSLConnection
 * and AsyncSSLTunnelConnection.
 * This abstraction is useful when downgrading from HTTP/2 to HTTP/1.1 over
 * an SSL connection. See ExchangeImpl::get in the case where an ALPNException
 * is thrown.
 *
 * Note: An AsyncSSLConnection wraps a PlainHttpConnection, while an
 *       AsyncSSLTunnelConnection wraps a PlainTunnelingConnection.
 *       If both these wrapped classes where made to inherit from a
 *       common abstraction then it might be possible to merge
 *       AsyncSSLConnection and AsyncSSLTunnelConnection back into
 *       a single class - and simply use different factory methods to
 *       create different wrappees, but this is left up for further cleanup.
 *
 */
abstract class AbstractAsyncSSLConnection extends HttpConnection
{
    protected final SSLEngine engine;
    protected final String serverName;
    protected final SSLParameters sslParameters;

    // Setting this property disables HTTPS hostname verification. Use with care.
    private static final boolean disableHostnameVerification
            = Utils.isHostnameVerificationDisabled();

    AbstractAsyncSSLConnection(InetSocketAddress addr,
                               HttpClientImpl client,
                               ServerName serverName, int port,
                               String[] alpn) {
        super(addr, client);
        this.serverName = serverName.getName();
        SSLContext context = client.theSSLContext();
        sslParameters = createSSLParameters(client, serverName, alpn);
        Log.logParams(sslParameters);
        engine = createEngine(context, serverName.getName(), port, sslParameters);
    }

    abstract SSLTube getConnectionFlow();

    final CompletableFuture<String> getALPN() {
        return getConnectionFlow().getALPN();
    }

    final SSLEngine getEngine() { return engine; }

    private static boolean contains(String[] rr, String target) {
        for (String s : rr)
            if (target.equalsIgnoreCase(s))
                return true;
        return false;
    }

    private static SSLParameters createSSLParameters(HttpClientImpl client,
                                                     ServerName serverName,
                                                     String[] alpn) {
        SSLParameters sslp = client.sslParameters();
        SSLParameters sslParameters = Utils.copySSLParameters(sslp);
        // filter out unwanted protocols, if h2 only
        if (alpn != null && alpn.length != 0 && !contains(alpn, "http/1.1")) {
            ArrayDeque<String> l = new ArrayDeque<>();
            for (String proto : sslParameters.getProtocols()) {
                if (!proto.startsWith("SSL") && !proto.endsWith("v1.1") && !proto.endsWith("v1")) {
                    l.add(proto);
                }
            }
            String[] a1 = l.toArray(new String[0]);
            sslParameters.setProtocols(a1);
        }

        if (!disableHostnameVerification)
            sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        if (alpn != null) {
            Log.logSSL("AbstractAsyncSSLConnection: Setting application protocols: {0}",
                       Arrays.toString(alpn));
            sslParameters.setApplicationProtocols(alpn);
        } else {
            Log.logSSL("AbstractAsyncSSLConnection: no applications set!");
        }
        if (!serverName.isLiteral()) {
            String name = serverName.getName();
            if (name != null && name.length() > 0) {
                sslParameters.setServerNames(List.of(new SNIHostName(name)));
            }
        }
        return sslParameters;
    }


    private static SSLEngine createEngine(SSLContext context, String serverName, int port,
                                          SSLParameters sslParameters) {
        SSLEngine engine = context.createSSLEngine(serverName, port);
        engine.setUseClientMode(true);

        engine.setSSLParameters(sslParameters);
        return engine;
    }

    @Override
    final boolean isSecure() {
        return true;
    }

}
