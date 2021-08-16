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

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/*
 * A JDK server based on SSLServerSocket.
 */
public class JdkServer extends AbstractServer {

    protected final String response;

    protected final SSLServerSocket serverSocket;

    protected final SSLContext context;
    private SSLSocket socket;

    public JdkServer(Builder builder) throws Exception {
        response = builder.getMessage();

        context = Utilities.createSSLContext(builder.getCertTuple());
        SSLServerSocketFactory serverFactory = context.getServerSocketFactory();
        serverSocket
                = (SSLServerSocket) serverFactory.createServerSocket(builder.getPort());
        configServerSocket(builder);
    }

    protected void configServerSocket(Builder builder) throws SocketException {
        serverSocket.setSoTimeout(builder.getTimeout() * 1000);
        if (builder.getProtocols() != null) {
            serverSocket.setEnabledProtocols(Utilities.enumsToStrs(protocol -> {
                return JdkUtils.protocol((Protocol) protocol);
            }, builder.getProtocols()));
        }
        if (builder.getCipherSuites() != null) {
            serverSocket.setEnabledCipherSuites(
                    Utilities.enumsToStrs(builder.getCipherSuites()));
        }
        serverSocket.setNeedClientAuth(builder.getClientAuth());
        SSLParameters sslParams = serverSocket.getSSLParameters();
        if (builder.getServerNames() != null) {
            List<SNIMatcher> matchers = new ArrayList<>();
            for(String bufServerName : builder.getServerNames()) {
                matchers.add(SNIHostName.createSNIMatcher(bufServerName));
            }
            sslParams.setSNIMatchers(matchers);
        }
        if (builder.getAppProtocols() != null) {
            sslParams.setApplicationProtocols(builder.getAppProtocols());
            for (String appProtocol : sslParams.getApplicationProtocols()) {
                System.out.println("appProtocol: " + appProtocol);
            }
        }
        serverSocket.setSSLParameters(sslParams);
    }

    public static class Builder extends AbstractServer.Builder {

        @Override
        public JdkServer build() throws Exception {
            return new JdkServer(this);
        }
    }

    @Override
    public Product getProduct() {
        return Jdk.DEFAULT;
    }

    @Override
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public void accept() throws IOException {
        while (true) {
            try (SSLSocket socket = (SSLSocket) serverSocket.accept()) {
                this.socket = socket;
                System.out.println("Server accepted connection");

                String request = Utilities.readIn(socket.getInputStream());
                System.out.printf("Server received request: [%s]%n", request);

                if (response != null) {
                    Utilities.writeOut(socket.getOutputStream(), response);
                    System.out.printf("Server sent response: [%s]%n", response);
                }
            }
        }
    }

    private synchronized SSLSocket getSocket() {
        return socket;
    }

    public SSLSession getSession() {
        return getSocket().getSession();
    }

    @Override
    public String getNegoAppProtocol() throws SSLTestException {
        return getSocket().getApplicationProtocol();
    }

    @Override
    public boolean isAlive() {
        return !serverSocket.isClosed();
    }

    @Override
    public void close() throws IOException {
        if (!serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}
