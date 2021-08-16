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

import java.util.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;

public class MySSLServerSocketFacImpl extends SSLServerSocketFactory {

    // default to custom ones
    private static String[] supportedCS = CipherSuites.CUSTOM;

    public static void useStandardCipherSuites() {
        supportedCS = CipherSuites.STANDARD;
    }
    public static void useCustomCipherSuites() {
        supportedCS = CipherSuites.CUSTOM;
    }

    public MySSLServerSocketFacImpl() {
        super();
    }
    public String[] getDefaultCipherSuites() {
        return (String[]) supportedCS.clone();
    }
    public String[] getSupportedCipherSuites() {
        return getDefaultCipherSuites();
    }
    public ServerSocket createServerSocket(int port) { return null; }
    public ServerSocket createServerSocket(int port, int backlog) {
        return null;
    }
    public ServerSocket createServerSocket(int port, int backlog,
                                           InetAddress ifAddress) {
        return null;
    }
}
