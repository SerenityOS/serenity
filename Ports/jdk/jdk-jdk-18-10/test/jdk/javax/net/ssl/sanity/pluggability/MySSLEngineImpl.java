/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;

public class MySSLEngineImpl extends SSLEngine {
    private static String[] supportedCS = CipherSuites.CUSTOM;

    public static void useStandardCipherSuites() {
        supportedCS = CipherSuites.STANDARD;
    }
    public static void useCustomCipherSuites() {
        supportedCS = CipherSuites.CUSTOM;
    }
    public MySSLEngineImpl() {
        super();
    }
    public MySSLEngineImpl(String host, int port) {
        super(host, port);
    }
    public SSLEngineResult wrap(ByteBuffer [] src, int off, int len,
        ByteBuffer dst) throws SSLException { return null; }
    public SSLEngineResult unwrap(ByteBuffer src,
        ByteBuffer [] dst, int off, int len)
        throws SSLException { return null; }
    public Runnable getDelegatedTask() { return null; }
    public void closeInbound() {}
    public boolean isInboundDone() { return false; }
    public void closeOutbound() {}
    public boolean isOutboundDone() { return false; }

    public String[] getEnabledCipherSuites() {
        return getSupportedCipherSuites();
    }
    public String[] getSupportedCipherSuites() {
        return (String[]) supportedCS.clone();
    }
    public void setEnabledCipherSuites(String[] suites) {}
    public String[] getSupportedProtocols() { return null; }
    public String[] getEnabledProtocols() { return null; }
    public void setEnabledProtocols(String[] protocols) {}
    public SSLSession getSession() { return null; }
    public void beginHandshake() throws SSLException {}
    public SSLEngineResult.HandshakeStatus getHandshakeStatus() {
        return SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
    }
    public void setUseClientMode(boolean mode) {};
    public boolean getUseClientMode() { return false; }
    public void setNeedClientAuth(boolean need) {}
    public boolean getNeedClientAuth() { return false; }
    public void setWantClientAuth(boolean need) {}
    public boolean getWantClientAuth() { return false; }
    public void setEnableSessionCreation(boolean flag) {}
    public boolean getEnableSessionCreation() { return false; }
}
