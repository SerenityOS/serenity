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

import java.security.*;
import java.net.*;
import javax.net.ssl.*;

public class MySSLContextImpl extends SSLContextSpi {
    private static MySSLServerSocketFacImpl ssf =
        new MySSLServerSocketFacImpl();
    private static MySSLSocketFacImpl sf = new MySSLSocketFacImpl();

    private static MySSLEngineImpl se1 = new MySSLEngineImpl();
    private static MySSLEngineImpl se2 = new MySSLEngineImpl("schnauzer", 1024);

    public static void useStandardCipherSuites() {
        MySSLServerSocketFacImpl.useStandardCipherSuites();
        MySSLSocketFacImpl.useStandardCipherSuites();
        MySSLEngineImpl.useStandardCipherSuites();
    }
    public static void useCustomCipherSuites() {
        MySSLServerSocketFacImpl.useCustomCipherSuites();
        MySSLSocketFacImpl.useCustomCipherSuites();
        MySSLEngineImpl.useCustomCipherSuites();
    }

    protected SSLSessionContext engineGetClientSessionContext() {
        return null;
    }
    protected SSLSessionContext engineGetServerSessionContext() {
        return null;
    }
    protected SSLServerSocketFactory engineGetServerSocketFactory() {
        return ssf;
    }
    protected SSLSocketFactory engineGetSocketFactory() {
        return sf;
    }
    protected SSLEngine engineCreateSSLEngine() {
        return se1;
    }
    protected SSLEngine engineCreateSSLEngine(String host, int port) {
        return se2;
    }
    protected void engineInit(KeyManager[] km, TrustManager[] tm,
                              SecureRandom sr) throws KeyManagementException {}
}
