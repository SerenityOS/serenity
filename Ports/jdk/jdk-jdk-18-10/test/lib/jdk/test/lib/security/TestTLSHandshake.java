/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.security;

import java.io.*;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;


public final class TestTLSHandshake extends SSLSocketTest {

    public static final String CIPHER_SUITE =
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384";
    public static final long HASHCODE = -1057291798L;
    public static final long ANCHOR_HASHCODE = 1688661792L;
    public static final String CERT_SERIAL = "edbec8f705af2514";
    public static final String ANCHOR_CERT_SERIAL = "8e191778b2f331be";

    public String protocolVersion;
    public String peerHost;
    public int peerPort;

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslIS.read();
        sslOS.write(85);
        sslOS.flush();
    }

    @Override
    protected void runClientApplication(SSLSocket socket) throws Exception {
        socket.setEnabledCipherSuites(new String[] { CIPHER_SUITE });
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();

        SSLSession sslSession = socket.getSession();
        protocolVersion =  sslSession.getProtocol();
        peerHost = sslSession.getPeerHost();
        peerPort = sslSession.getPeerPort();
    }
}
