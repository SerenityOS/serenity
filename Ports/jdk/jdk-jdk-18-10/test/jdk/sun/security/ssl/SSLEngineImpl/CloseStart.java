/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 5019096
 * @summary Add scatter/gather APIs for SSLEngine
 * @run main/othervm CloseStart
 */

//
// Check to see if the args are being parsed properly.
//

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.security.*;
import java.nio.*;

public class CloseStart {

    private static boolean debug = false;

    private static String pathToStores = "../../../../javax/net/ssl/etc";
    private static String keyStoreFile = "keystore";
    private static String trustStoreFile = "truststore";
    private static String passwd = "passphrase";

    private static String keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;
    private static String trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;

    private static void checkDone(SSLEngine ssle) throws Exception {
        if (!ssle.isInboundDone()) {
            throw new Exception("isInboundDone isn't done");
        }
        if (!ssle.isOutboundDone()) {
            throw new Exception("isOutboundDone isn't done");
        }
    }

    private static void runTest2(SSLEngine ssle) throws Exception {
        ssle.closeOutbound();
        checkDone(ssle);
    }

    public static void main(String args[]) throws Exception {

        SSLEngine ssle = createSSLEngine(keyFilename, trustFilename);
        ssle.closeInbound();
        if (!ssle.isInboundDone()) {
            throw new Exception("isInboundDone isn't done");
        }

        ssle = createSSLEngine(keyFilename, trustFilename);
        ssle.closeOutbound();
        if (!ssle.isOutboundDone()) {
            throw new Exception("isOutboundDone isn't done");
        }

        System.out.println("Test Passed.");
    }

    /*
     * Create an initialized SSLContext to use for this test.
     */
    static private SSLEngine createSSLEngine(String keyFile, String trustFile)
            throws Exception {

        SSLEngine ssle;

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        char[] passphrase = "passphrase".toCharArray();

        ks.load(new FileInputStream(keyFile), passphrase);
        ts.load(new FileInputStream(trustFile), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("TLS");

        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        ssle = sslCtx.createSSLEngine("client", 1001);
        ssle.setUseClientMode(true);

        return ssle;
    }
}
