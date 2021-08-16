/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.security.timestamp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Objects;

import com.sun.net.httpserver.HttpServer;

/**
 * A {@link HttpServer}-based {@code TSA} server working with/without a
 * time-stamping request. With a set of specific parameters defined in
 * {@link TsaParam} and delivered by {@code HTTP} query (see {@link TsaHandler}),
 * an application could affect the time-stamping response generating process
 * directly. The application parameters can override the similar fields in the
 * time-stamping request. Furthermore an application can provide an alternative
 * {@link RespInterceptor} or even extend {@link TsaSigner} for generating custom
 * time-stamping response.
 */
public class TsaServer implements AutoCloseable {

    private final HttpServer server;

    /**
     * Create {@code TSA} server with the given port and {@link TsaHandler}.
     *
     * @param port the port number
     * @param handler a {@link TsaHandler} instance
     * @throws IOException the I/O exception
     */
    public TsaServer(int port, TsaHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        if (handler != null) {
            setHandler(handler);
        }
    }

    /**
     * Create {@code TSA} server with the given port, but without {@link TsaHandler}.
     *
     * @param port the port number
     * @throws IOException the I/O exception
     */
    public TsaServer(int port) throws IOException {
        this(port, null);
    }

    /**
     * Create {@code TSA} server with the given port, key store and the
     * associated passphrase if any. A default {@link TsaHandler} is initialized
     * along with this server.
     *
     * @param port port
     * @param keyStore a key store
     * @param passphrase the private key passphrase
     * @throws IOException the I/O exception
     */
    public TsaServer(int port, KeyStore keyStore, String passphrase)
            throws IOException {
        this(port, new TsaHandler(keyStore, passphrase));
    }

    /**
     * Setup a {@link TsaHandler} for this {@code TSA} server.
     *
     * @param handler a {@link TsaHandler}
     */
    public void setHandler(TsaHandler handler) {
        server.createContext("/", handler);
    }

    /**
     * Start {@code TSA} server.
     */
    public void start() {
        server.start();
    }

    /**
     * Stop {@code TSA} server.
     */
    public void stop() {
        server.stop(0);
    }

    /**
     * Return the server port.
     */
    public int getPort() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() throws Exception {
        stop();
    }
}
