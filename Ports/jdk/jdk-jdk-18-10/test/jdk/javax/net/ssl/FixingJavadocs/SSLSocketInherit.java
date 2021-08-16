/*
 * Copyright (c) 2001, 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4387882
 * @summary Need to revisit the javadocs for JSSE, especially the
 *      promoted classes.  This test checks to see if the settings
 *      on the server sockets get propagated to the sockets.
 * @run main/othervm SSLSocketInherit
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 * @author Brad Wetmore
 */

import java.net.*;
import javax.net.ssl.*;

public class SSLSocketInherit {
    String pathToStores = "../etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    volatile int serverPort = 0;

    /*
     * Let's just create silly sockets to do a basic connection,
     * that's all we really need.
     */
    Thread forkClient() {
        Thread clientThread = new Thread() {
            public void run() {
                try {
                    new Socket("localhost", serverPort);
                } catch (Exception e) {
                    // ignore for now...
                }
            }
        };
        clientThread.start();
        return clientThread;
    }

    SSLSocketInherit() throws Exception {
        Exception exc = null;

        String keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;
        String trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;

        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);
        System.setProperty("javax.net.ssl.trustStore", trustFilename);
        System.setProperty("javax.net.ssl.trustStorePassword", passwd);

        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sslss =
            (SSLServerSocket) sslssf.createServerSocket(0);
        serverPort = sslss.getLocalPort();

        Thread client = forkClient();

        String [] ciphers =
            new String [] { "SSL_RSA_WITH_DES_CBC_SHA" };

        String [] protocols =
            new String [] { "SSLv3" };

        sslss.setEnabledCipherSuites(ciphers);
        sslss.setEnabledProtocols(protocols);
        sslss.setNeedClientAuth(true);
        sslss.setUseClientMode(true);
        sslss.setEnableSessionCreation(true);

        SSLSocket ssls = (SSLSocket) sslss.accept();

        if (((ciphers = ssls.getEnabledCipherSuites()) == null) ||
                (ciphers.length != 1) ||
                (ciphers[0].compareToIgnoreCase(
                "SSL_RSA_WITH_DES_CBC_SHA") != 0)) {
            exc = new Exception("problem with get/setEnabledCipherSuites()");
        }

        if (((protocols = ssls.getEnabledProtocols()) == null) ||
                (protocols.length != 1) ||
                (protocols[0].compareToIgnoreCase(
                "SSLv3") != 0)) {
            exc = new Exception("problem with get/setEnabledProtocols()");
        }

        if (ssls.getNeedClientAuth() != true) {
            exc = new Exception("problem with get/setNeedClientAuth()");
        }

        if (ssls.getUseClientMode() != true) {
            exc = new Exception("problem with get/setUseClientMode()");
        }

        client.join();

        if (exc != null) {
            throw exc;
        }

        System.out.println("First SSLSocket inherited right info");

        /*
         * Try it again.
         */
        client = forkClient();

        ciphers = new String [] { "SSL_DH_anon_WITH_DES_CBC_SHA" };
        protocols = new String [] { "TLSv1" };

        sslss.setEnabledCipherSuites(ciphers);
        sslss.setEnabledProtocols(protocols);
        sslss.setWantClientAuth(true);
        sslss.setUseClientMode(false);
        sslss.setEnableSessionCreation(false);

        ssls = (SSLSocket) sslss.accept();

        if (((ciphers = ssls.getEnabledCipherSuites()) == null) ||
                (ciphers.length != 1) ||
                (ciphers[0].compareToIgnoreCase(
                "SSL_DH_anon_WITH_DES_CBC_SHA") != 0)) {
            exc = new Exception("problem with get/setEnabledCipherSuites()");
        }

        if (((protocols = ssls.getEnabledProtocols()) == null) ||
                (protocols.length != 1) ||
                (protocols[0].compareToIgnoreCase(
                "TLSv1") != 0)) {
            exc = new Exception("problem with get/setEnabledProtocols()");
        }

        if (ssls.getWantClientAuth() != true) {
            exc = new Exception("problem with get/setWantClientAuth()");
        }

        if (ssls.getUseClientMode() != false) {
            exc = new Exception("problem with get/setUseClientMode()");
        }

        client.join();

        if (exc != null) {
            throw exc;
        }

        System.out.println("Second SSLSocket inherited right info");

        /*
         * Lastly, try to set some wild suites, and make sure we
         * catch it.
         */

        ciphers = sslss.getSupportedCipherSuites();
        ciphers[1] = "this isn't a cipher suite";

        try {
            sslss.setEnabledCipherSuites(ciphers);
            throw new Exception(
                "server socket setEnabledCipherSuites didn't throw Exception");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught proper Exception on server socket");
        }

        try {
            ssls.setEnabledCipherSuites(ciphers);
            throw new Exception(
                "socket setEnabledCipherSuites didn't throw Exception");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught proper Exception on socket");
        }

        try {
            ssls.setEnabledProtocols(null);
            throw new Exception(
                "socket setEnabledProtocols null didn't throw Exception");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught proper Exception on socket");
        }

        try {
            sslss.setEnabledProtocols(null);
            throw new Exception(
                "server socket setEnabledProtocols null "+
                "didn't throw Exception");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught proper Exception on server socket");
        }

        try {
            ssls.setEnabledCipherSuites(null);
            throw new Exception(
                "socket setEnabledCipherSuites null didn't throw Exception");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught proper Exception on socket");
        }

        try {
            sslss.setEnabledCipherSuites(null);
            throw new Exception(
                "server socket setEnabledCipherSuites null "+
                "didn't throw Exception");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught proper Exception on server socket");
        }

        System.out.println("All tests PASS!");

    }

    public static void main(String args[]) throws Exception {
        new SSLSocketInherit();
    }
}
