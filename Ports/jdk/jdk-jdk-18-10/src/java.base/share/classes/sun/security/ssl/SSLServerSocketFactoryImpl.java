/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * This class creates SSL server sockets.
 *
 * @author David Brownell
 */
public final class SSLServerSocketFactoryImpl extends SSLServerSocketFactory {
    private static final int DEFAULT_BACKLOG = 50;
    private final SSLContextImpl context;


    /**
     * Constructor used to instantiate the default factory. This method is
     * only called if the old "ssl.ServerSocketFactory.provider" property in the
     * java.security file is set.
     */
    public SSLServerSocketFactoryImpl() throws Exception {
        this.context = SSLContextImpl.DefaultSSLContext.getDefaultImpl();
    }

    /**
     * Called from SSLContextImpl's getSSLServerSocketFactory().
     */
    SSLServerSocketFactoryImpl(SSLContextImpl context) {
        this.context = context;
    }

    /**
     * Returns an unbound server socket.
     *
     * @return the unbound socket
     * @throws IOException if the socket cannot be created
     * @see java.net.Socket#bind(java.net.SocketAddress)
     */
    @Override
    public ServerSocket createServerSocket() throws IOException {
        return new SSLServerSocketImpl(context);
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return new SSLServerSocketImpl(context, port, DEFAULT_BACKLOG);
    }


    @Override
    public ServerSocket createServerSocket (
            int port, int backlog) throws IOException {
        return new SSLServerSocketImpl(context, port, backlog);
    }

    @Override
    public ServerSocket
    createServerSocket (int port,
            int backlog, InetAddress ifAddress) throws IOException {
        return new SSLServerSocketImpl(context, port, backlog, ifAddress);
    }

    /**
     * Returns the subset of the supported cipher suites which are
     * enabled by default.  These cipher suites all provide a minimum
     * quality of service whereby the server authenticates itself
     * (preventing person-in-the-middle attacks) and where traffic
     * is encrypted to provide confidentiality.
     */
    @Override
    public String[] getDefaultCipherSuites() {
        return CipherSuite.namesOf(context.getDefaultCipherSuites(true));
    }

    /**
     * Returns the names of the cipher suites which could be enabled for use
     * on an SSL connection.  Normally, only a subset of these will actually
     * be enabled by default, since this list may include cipher suites which
     * do not support the mutual authentication of servers and clients, or
     * which do not protect data confidentiality.  Servers may also need
     * certain kinds of certificates to use certain cipher suites.
     *
     * @return an array of cipher suite names
     */
    @Override
    public String[] getSupportedCipherSuites() {
        return CipherSuite.namesOf(context.getSupportedCipherSuites());
    }
}
