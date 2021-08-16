/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;

/**
 * This class provides a simple way for servers to support conventional
 * use of the Secure Sockets Layer (SSL).  Application code uses an
 * SSLServerSocketImpl exactly like it uses a regular TCP ServerSocket; the
 * difference is that the connections established are secured using SSL.
 *
 * <P> Also, the constructors take an explicit authentication context
 * parameter, giving flexibility with respect to how the server socket
 * authenticates itself.  That policy flexibility is not exposed through
 * the standard SSLServerSocketFactory API.
 *
 * <P> System security defaults prevent server sockets from accepting
 * connections if they the authentication context has not been given
 * a certificate chain and its matching private key.  If the clients
 * of your application support "anonymous" cipher suites, you may be
 * able to configure a server socket to accept those suites.
 *
 * @see SSLSocketImpl
 * @see SSLServerSocketFactoryImpl
 *
 * @author David Brownell
 */
final class SSLServerSocketImpl extends SSLServerSocket {
    private final SSLContextImpl        sslContext;
    private final SSLConfiguration      sslConfig;
    private final ReentrantLock         serverSocketLock = new ReentrantLock();

    SSLServerSocketImpl(SSLContextImpl sslContext) throws IOException {

        super();
        this.sslContext = sslContext;
        this.sslConfig = new SSLConfiguration(sslContext, false);
    }

    SSLServerSocketImpl(SSLContextImpl sslContext,
            int port, int backlog) throws IOException {

        super(port, backlog);
        this.sslContext = sslContext;
        this.sslConfig = new SSLConfiguration(sslContext, false);
    }

    SSLServerSocketImpl(SSLContextImpl sslContext,
            int port, int backlog, InetAddress address) throws IOException {

        super(port, backlog, address);
        this.sslContext = sslContext;
        this.sslConfig = new SSLConfiguration(sslContext, false);
    }

    @Override
    public String[] getEnabledCipherSuites() {
        serverSocketLock.lock();
        try {
            return CipherSuite.namesOf(sslConfig.enabledCipherSuites);
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        serverSocketLock.lock();
        try {
            sslConfig.enabledCipherSuites =
                    CipherSuite.validValuesOf(suites);
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return CipherSuite.namesOf(sslContext.getSupportedCipherSuites());
    }

    @Override
    public String[] getSupportedProtocols() {
        return ProtocolVersion.toStringArray(
                sslContext.getSupportedProtocolVersions());
    }

    @Override
    public String[] getEnabledProtocols() {
        serverSocketLock.lock();
        try {
            return ProtocolVersion.toStringArray(sslConfig.enabledProtocols);
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        serverSocketLock.lock();
        try {
            if (protocols == null) {
                throw new IllegalArgumentException("Protocols cannot be null");
            }

            sslConfig.enabledProtocols = ProtocolVersion.namesOf(protocols);
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        serverSocketLock.lock();
        try {
            sslConfig.clientAuthType =
                    (need ? ClientAuthType.CLIENT_AUTH_REQUIRED :
                            ClientAuthType.CLIENT_AUTH_NONE);
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public boolean getNeedClientAuth() {
        serverSocketLock.lock();
        try {
            return (sslConfig.clientAuthType ==
                        ClientAuthType.CLIENT_AUTH_REQUIRED);
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public void setWantClientAuth(boolean want) {
        serverSocketLock.lock();
        try {
            sslConfig.clientAuthType =
                    (want ? ClientAuthType.CLIENT_AUTH_REQUESTED :
                            ClientAuthType.CLIENT_AUTH_NONE);
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public boolean getWantClientAuth() {
        serverSocketLock.lock();
        try {
            return (sslConfig.clientAuthType ==
                        ClientAuthType.CLIENT_AUTH_REQUESTED);
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public void setUseClientMode(boolean useClientMode) {
        serverSocketLock.lock();
        try {
            /*
             * If we need to change the client mode and the enabled
             * protocols and cipher suites haven't specifically been
             * set by the user, change them to the corresponding
             * default ones.
             */
            if (sslConfig.isClientMode != useClientMode) {
                if (sslContext.isDefaultProtocolVesions(
                        sslConfig.enabledProtocols)) {
                    sslConfig.enabledProtocols =
                        sslContext.getDefaultProtocolVersions(!useClientMode);
                }

                if (sslContext.isDefaultCipherSuiteList(
                        sslConfig.enabledCipherSuites)) {
                    sslConfig.enabledCipherSuites =
                        sslContext.getDefaultCipherSuites(!useClientMode);
                }

                sslConfig.toggleClientMode();
            }
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public boolean getUseClientMode() {
        serverSocketLock.lock();
        try {
            return sslConfig.isClientMode;
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        serverSocketLock.lock();
        try {
            sslConfig.enableSessionCreation = flag;
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public boolean getEnableSessionCreation() {
        serverSocketLock.lock();
        try {
            return sslConfig.enableSessionCreation;
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public SSLParameters getSSLParameters() {
        serverSocketLock.lock();
        try {
            return sslConfig.getSSLParameters();
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public void setSSLParameters(SSLParameters params) {
        serverSocketLock.lock();
        try {
            sslConfig.setSSLParameters(params);
        } finally {
            serverSocketLock.unlock();
        }
    }

    @Override
    public Socket accept() throws IOException {
        SSLSocketImpl s = new SSLSocketImpl(sslContext, sslConfig);

        implAccept(s);
        s.doneConnect();
        return s;
    }

    @Override
    public String toString() {
        return "[SSL: "+ super.toString() + "]";
    }
}
