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

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/*
 * A custom socket factory used to override the default socket factory and track the LDAP client connection.
 * Factory can create only one SSLSocket. See the DeadServerTimeoutSSLTest test.
 */
public class DeadSSLSocketFactory extends SocketFactory {
    // Client socket that is used by LDAP connection
    public static AtomicReference<SSLSocket> firstCreatedSocket = new AtomicReference<>();

    // Boolean to track if connection socket has been opened
    public static AtomicBoolean isConnectionOpened = new AtomicBoolean(false);

    // Default SSLSocketFactory that will be used for SSL socket creation
    final SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory.getDefault();

    // Create unconnected socket
    public Socket createSocket() throws IOException {
        if (!isConnectionOpened.getAndSet(true)) {
            System.err.println("DeadSSLSocketFactory: Creating unconnected socket");
            firstCreatedSocket.set((SSLSocket) factory.createSocket());
            return firstCreatedSocket.get();
        } else {
            throw new RuntimeException("DeadSSLSocketFactory only allows creation of one SSL socket");
        }
    }

    public DeadSSLSocketFactory() {
        System.err.println("DeadSSLSocketFactory: Constructor call");
    }

    public static SocketFactory getDefault() {
        System.err.println("DeadSSLSocketFactory: acquiring DeadSSLSocketFactory as default socket factory");
        return new DeadSSLSocketFactory();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        // Not used by DeadSSLLdapTimeoutTest
        return factory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost,
                               int localPort) throws IOException {
        // Not used by DeadSSLLdapTimeoutTest
        return factory.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        // Not used by DeadSSLLdapTimeoutTest
        return factory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddress, int localPort) throws IOException {
        // Not used by DeadSSLLdapTimeoutTest
        return factory.createSocket(address, port, localAddress, localPort);
    }
}

