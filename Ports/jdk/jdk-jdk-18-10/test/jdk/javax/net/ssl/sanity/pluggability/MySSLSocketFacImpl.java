/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;

public class MySSLSocketFacImpl extends SSLSocketFactory {
    private static String[] supportedCS = CipherSuites.CUSTOM;

    public static void useStandardCipherSuites() {
        supportedCS = CipherSuites.STANDARD;
    }
    public static void useCustomCipherSuites() {
        supportedCS = CipherSuites.CUSTOM;
    }

    public MySSLSocketFacImpl() {
        super();
    }
    public String[] getDefaultCipherSuites() {
        return (String[]) supportedCS.clone();
    }
    public String[] getSupportedCipherSuites() {
        return getDefaultCipherSuites();
    }
    public Socket createSocket(Socket s, String host, int port,
        boolean autoClose) { return new MySSLSocket(this); }
    public Socket createSocket(InetAddress host, int port) {
        return new MySSLSocket(this);
    }
    public Socket createSocket(InetAddress address, int port,
        InetAddress localAddress, int localPort) {
        return new MySSLSocket(this);
    }
    public Socket createSocket(String host, int port) {
        return new MySSLSocket(this);
    }
    public Socket createSocket(String host, int port, InetAddress
        localHost, int localPort) { return new MySSLSocket(this); }
}

class MySSLSocket extends SSLSocket {
    SSLSocketFactory fac = null;

    public MySSLSocket(SSLSocketFactory fac) {
        this.fac = fac;
    }
    public String[] getSupportedCipherSuites() {
        return fac.getSupportedCipherSuites();
    }
    public String[] getEnabledCipherSuites() {
        return fac.getSupportedCipherSuites();
    }
    public void setEnabledCipherSuites(String suites[]) {}
    public String[] getSupportedProtocols() { return null; }
    public String[] getEnabledProtocols() { return null; }
    public void setEnabledProtocols(String protocols[]) {}
    public SSLSession getSession() { return null; }
    public void addHandshakeCompletedListener
        (HandshakeCompletedListener listener) {}
    public void removeHandshakeCompletedListener
        (HandshakeCompletedListener listener) {}
    public void startHandshake() throws IOException {}
    public void setUseClientMode(boolean mode) {}
    public boolean getUseClientMode() { return true; }
    public void setNeedClientAuth(boolean need) {}
    public boolean getNeedClientAuth() { return false; }
    public void setWantClientAuth(boolean want) {}
    public boolean getWantClientAuth() { return false; }
    public void setEnableSessionCreation(boolean flag) {}
    public boolean getEnableSessionCreation() { return true; }
}
