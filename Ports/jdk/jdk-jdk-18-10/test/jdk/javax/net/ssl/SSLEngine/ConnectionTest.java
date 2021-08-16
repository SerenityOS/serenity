/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4495742
 * @summary Add non-blocking SSL/TLS functionality, usable with any
 *      I/O abstraction
 * @ignore the dependent implementation details are changed
 * @author Brad Wetmore
 *
 * @run main/othervm ConnectionTest
 */

/*
 * This is a bit hacky, meant to test various conditions.  The main
 * thing I wanted to do with this was to do buffer reads/writes
 * when buffers were not empty.  (buffer.position() = 10)
 * The code could certainly be tightened up a lot.
 */
import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.security.*;
import java.nio.*;

public class ConnectionTest {

    private SSLContext sslc;
    private SSLEngine ssle1;
    private SSLEngine ssle2;

    private static String pathToStores = "../etc";
    private static String keyStoreFile = "keystore";
    private static String trustStoreFile = "truststore";
    private static String passwd = "passphrase";

    private static String keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;
    private static String trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;

    private ByteBuffer appIn1, appOut1;
    private ByteBuffer appIn2, appOut2;
    private ByteBuffer oneToTwo, twoToOne;
    private ByteBuffer emptyBuffer;

    private ByteBuffer  oneToTwoShifter, twoToOneShifter;

    private String hostname = "hostname";
    private int portNumber = 77;

    public ConnectionTest()
            throws Exception {

        sslc = getSSLContext();
        ssle1 = sslc.createSSLEngine(hostname, portNumber);
        ssle2 = sslc.createSSLEngine();

        ssle1.setEnabledCipherSuites(new String [] {
            "SSL_RSA_WITH_RC4_128_MD5"});

        ssle2.setEnabledCipherSuites(new String [] {
            "SSL_RSA_WITH_RC4_128_MD5"});

        createBuffers();
    }

    private SSLContext getSSLContext() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");
        char[] passphrase = "passphrase".toCharArray();

        ks.load(new FileInputStream(keyFilename), passphrase);
        ts.load(new FileInputStream(trustFilename), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("TLS");

        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslCtx;
    }

    private void createBuffers() {
        // Size the buffers as appropriate.
        SSLSession session = ssle1.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();

        appIn1 = ByteBuffer.allocateDirect(appBufferMax + 10);
        appIn2 = ByteBuffer.allocateDirect(appBufferMax + 10);

        appIn1.position(10);
        appIn2.position(10);

        oneToTwo = ByteBuffer.allocateDirect(netBufferMax + 10);
        twoToOne = ByteBuffer.allocateDirect(netBufferMax + 10);

        oneToTwo.position(10);
        twoToOne.position(10);
        oneToTwoShifter = oneToTwo.slice();
        twoToOneShifter = twoToOne.slice();

        appOut1 = ByteBuffer.wrap("Hi Engine2, I'm SSLEngine1".getBytes());
        appOut2 = ByteBuffer.wrap("Hello Engine1, I'm SSLEngine2".getBytes());

        emptyBuffer = ByteBuffer.allocate(10);
        emptyBuffer.limit(5);
        emptyBuffer.position(emptyBuffer.limit());

        System.out.println("AppOut1 = " + appOut1);
        System.out.println("AppOut2 = " + appOut2);
        System.out.println();
    }

    private void checkResult(SSLEngineResult result, Status status,
            HandshakeStatus hsStatus, int consumed, int produced,
            boolean done) throws Exception {

        if ((status != null) && (result.getStatus() != status)) {
            throw new Exception("Unexpected Status: need = " + status +
                " got = " + result.getStatus());
        }

        if ((hsStatus != null) && (result.getHandshakeStatus() != hsStatus)) {
            throw new Exception("Unexpected hsStatus: need = " + hsStatus +
                " got = " + result.getHandshakeStatus());
        }

        if ((consumed != -1) && (consumed != result.bytesConsumed())) {
            throw new Exception("Unexpected consumed: need = " + consumed +
                " got = " + result.bytesConsumed());
        }

        if ((produced != -1) && (produced != result.bytesProduced())) {
            throw new Exception("Unexpected produced: need = " + produced +
                " got = " + result.bytesProduced());
        }

        if (done && (hsStatus == HandshakeStatus.FINISHED)) {
            throw new Exception(
                "Handshake already reported finished");
        }

    }

    private boolean isHandshaking(SSLEngine e) {
        return (e.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING);
    }

    private void test() throws Exception {
        ssle1.setUseClientMode(true);
        ssle2.setUseClientMode(false);
        ssle2.setNeedClientAuth(true);

        System.out.println("Testing for early unwrap/wrap");
        SSLEngineResult result1 = ssle1.unwrap(twoToOne, appIn1);
        SSLEngineResult result2 = ssle2.wrap(appOut2, oneToTwo);

        /*
         * These should not consume/produce data, because they
         * are client and server, respectively, and don't
         * start handshaking this way.
         */
        checkResult(result1, Status.OK, HandshakeStatus.NEED_WRAP,
            0, 0, false);
        checkResult(result2, Status.OK, HandshakeStatus.NEED_UNWRAP,
            0, 0, false);

        System.out.println("Doing Initial Handshake");

        boolean done1 = false;
        boolean done2 = false;

        /*
         * Do initial handshaking
         */
        while (isHandshaking(ssle1) ||
                isHandshaking(ssle2)) {

            System.out.println("================");

            result1 = ssle1.wrap(emptyBuffer, oneToTwo);
            checkResult(result1, null, null, 0, -1, done1);
            result2 = ssle2.wrap(emptyBuffer, twoToOne);
            checkResult(result2, null, null, 0, -1, done2);

            if (result1.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                done1 = true;
            }

            if (result2.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                done2 = true;
            }

            System.out.println("wrap1 = " + result1);
            System.out.println("wrap2 = " + result2);

            if (result1.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = ssle1.getDelegatedTask()) != null) {
                    runnable.run();
                }
            }

            if (result2.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = ssle2.getDelegatedTask()) != null) {
                    runnable.run();
                }
            }

            oneToTwo.flip();
            twoToOne.flip();

            oneToTwo.position(10);
            twoToOne.position(10);

            System.out.println("----");

            result1 = ssle1.unwrap(twoToOne, appIn1);
            checkResult(result1, null, null, -1, 0, done1);
            result2 = ssle2.unwrap(oneToTwo, appIn2);
            checkResult(result2, null, null, -1, 0, done2);

            if (result1.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                done1 = true;
            }

            if (result2.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                done2 = true;
            }

            if (result1.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = ssle1.getDelegatedTask()) != null) {
                    runnable.run();
                }
            }

            if (result2.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = ssle2.getDelegatedTask()) != null) {
                    runnable.run();
                }
            }

            System.out.println("unwrap1 = " + result1);
            System.out.println("unwrap2 = " + result2);

            oneToTwoShifter.position(oneToTwo.position() - 10);
            oneToTwoShifter.limit(oneToTwo.limit() - 10);
            twoToOneShifter.position(twoToOne.position() - 10);
            twoToOneShifter.limit(twoToOne.limit() - 10);
            oneToTwoShifter.compact();
            twoToOneShifter.compact();
            oneToTwo.position(oneToTwoShifter.position() + 10);
            oneToTwo.limit(oneToTwoShifter.limit() + 10);
            twoToOne.position(twoToOneShifter.position() + 10);
            twoToOne.limit(twoToOneShifter.limit() + 10);
        }

        System.out.println("\nDONE HANDSHAKING");
        System.out.println("================");

        if (!done1 || !done2) {
            throw new Exception("Both should be true:\n" +
                " done1 = " + done1 + " done2 = " + done2);
        }

        String host = ssle1.getPeerHost();
        int port = ssle1.getPeerPort();
        if (!host.equals(hostname) || (port != portNumber)) {
            throw new Exception("unexpected host/port " + host + ":" + port);
        }

        host = ssle2.getPeerHost();
        port = ssle2.getPeerPort();
        if ((host != null) || (port != -1)) {
            throw new Exception("unexpected host/port " + host + ":" + port);
        }

        SSLSession ssls1 = ssle1.getSession();

        host = ssls1.getPeerHost();
        port = ssls1.getPeerPort();
        if (!host.equals(hostname) || (port != portNumber)) {
            throw new Exception("unexpected host/port " + host + ":" + port);
        }

        SSLSession ssls2 = ssle2.getSession();

        host = ssls2.getPeerHost();
        port = ssls2.getPeerPort();
        if ((host != null) || (port != -1)) {
            throw new Exception("unexpected host/port " + host + ":" + port);
        }

        /*
         * Should be able to write/read a small buffer like this.
         */
        int appOut1Len = appOut1.remaining();
        int appOut2Len = appOut2.remaining();
        int net1Len;
        int net2Len;

        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(result1, Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            appOut1Len, -1, false);
        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(result2, Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            appOut2Len, -1, false);
        net1Len = result1.bytesProduced();
        net2Len = result2.bytesProduced();

        System.out.println("wrap1 = " + result1);
        System.out.println("wrap2 = " + result2);

        oneToTwo.flip();
        twoToOne.flip();

        oneToTwo.position(10);
        twoToOne.position(10);

        System.out.println("----");

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(result1, Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            net2Len, appOut2Len, false);
        result2 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(result2, Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            net1Len, appOut1Len, false);

        System.out.println("unwrap1 = " + result1);
        System.out.println("unwrap2 = " + result2);

        oneToTwoShifter.position(oneToTwo.position() - 10);
        oneToTwoShifter.limit(oneToTwo.limit() - 10);
        twoToOneShifter.position(twoToOne.position() - 10);
        twoToOneShifter.limit(twoToOne.limit() - 10);
        oneToTwoShifter.compact();
        twoToOneShifter.compact();
        oneToTwo.position(oneToTwoShifter.position() + 10);
        oneToTwo.limit(oneToTwoShifter.limit() + 10);
        twoToOne.position(twoToOneShifter.position() + 10);
        twoToOne.limit(twoToOneShifter.limit() + 10);

        ssls2.invalidate();
        ssle2.beginHandshake();

        System.out.println("\nRENEGOTIATING");
        System.out.println("=============");

        done1 = false;
        done2 = false;

        appIn1.clear();
        appIn2.clear();

        /*
         * Do a quick test to see if this can do a switch
         * into client mode, at this point, you shouldn't be able
         * to switch back.
         */
        try {
            System.out.println("Try to change client mode");
            ssle2.setUseClientMode(true);
            throw new Exception("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught correct IllegalArgumentException");
        }

        while (isHandshaking(ssle1) ||
                isHandshaking(ssle2)) {

            System.out.println("================");

            result1 = ssle1.wrap(emptyBuffer, oneToTwo);
            checkResult(result1, null, null, 0, -1, done1);
            result2 = ssle2.wrap(emptyBuffer, twoToOne);
            checkResult(result2, null, null, 0, -1, done2);

            if (result1.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                done1 = true;
            }

            if (result2.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                done2 = true;
            }

            System.out.println("wrap1 = " + result1);
            System.out.println("wrap2 = " + result2);

            if (result1.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = ssle1.getDelegatedTask()) != null) {
                    runnable.run();
                }
            }

            if (result2.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = ssle2.getDelegatedTask()) != null) {
                    runnable.run();
                }
            }

            oneToTwo.flip();
            twoToOne.flip();

            oneToTwo.position(10);
            twoToOne.position(10);

            System.out.println("----");

            result1 = ssle1.unwrap(twoToOne, appIn1);
            checkResult(result1, null, null, -1, 0, done1);
            result2 = ssle2.unwrap(oneToTwo, appIn2);
            checkResult(result2, null, null, -1, 0, done2);

            if (result1.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                done1 = true;
            }

            if (result2.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                done2 = true;
            }

            System.out.println("unwrap1 = " + result1);
            System.out.println("unwrap2 = " + result2);

            if (result1.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = ssle1.getDelegatedTask()) != null) {
                    runnable.run();
                }
            }

            if (result2.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                Runnable runnable;
                while ((runnable = ssle2.getDelegatedTask()) != null) {
                    runnable.run();
                }
            }

            oneToTwoShifter.position(oneToTwo.position() - 10);
            oneToTwoShifter.limit(oneToTwo.limit() - 10);
            twoToOneShifter.position(twoToOne.position() - 10);
            twoToOneShifter.limit(twoToOne.limit() - 10);
            oneToTwoShifter.compact();
            twoToOneShifter.compact();
            oneToTwo.position(oneToTwoShifter.position() + 10);
            oneToTwo.limit(oneToTwoShifter.limit() + 10);
            twoToOne.position(twoToOneShifter.position() + 10);
            twoToOne.limit(twoToOneShifter.limit() + 10);
        }

        host = ssle1.getPeerHost();
        port = ssle1.getPeerPort();
        if (!host.equals(hostname) || (port != portNumber)) {
            throw new Exception("unexpected host/port " + host + ":" + port);
        }

        host = ssle2.getPeerHost();
        port = ssle2.getPeerPort();
        if ((host != null) || (port != -1)) {
            throw new Exception("unexpected host/port " + host + ":" + port);
        }

        SSLSession ssls3 = ssle2.getSession();

        host = ssls1.getPeerHost();
        port = ssls1.getPeerPort();
        if (!host.equals(hostname) || (port != portNumber)) {
            throw new Exception("unexpected host/port " + host + ":" + port);
        }

        SSLSession ssls4 = ssle2.getSession();

        host = ssls2.getPeerHost();
        port = ssls2.getPeerPort();
        if ((host != null) || (port != -1)) {
            throw new Exception("unexpected host/port " + host + ":" + port);
        }

        System.out.println("\nDoing close");
        System.out.println("===========");

        ssle1.closeOutbound();
        ssle2.closeOutbound();

        oneToTwo.flip();
        twoToOne.flip();
        oneToTwo.position(10);
        twoToOne.position(10);

        appIn1.clear();
        appIn2.clear();

        System.out.println("LAST UNWRAP");
        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(result1, Status.BUFFER_UNDERFLOW,
            HandshakeStatus.NEED_WRAP, 0, 0, false);
        result2 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(result2, Status.BUFFER_UNDERFLOW,
            HandshakeStatus.NEED_WRAP, 0, 0, false);

        System.out.println("unwrap1 = " + result1);
        System.out.println("unwrap2 = " + result2);

        oneToTwoShifter.position(oneToTwo.position() - 10);
        oneToTwoShifter.limit(oneToTwo.limit() - 10);
        twoToOneShifter.position(twoToOne.position() - 10);
        twoToOneShifter.limit(twoToOne.limit() - 10);
        oneToTwoShifter.compact();
        twoToOneShifter.compact();
        oneToTwo.position(oneToTwoShifter.position() + 10);
        oneToTwo.limit(oneToTwoShifter.limit() + 10);
        twoToOne.position(twoToOneShifter.position() + 10);
        twoToOne.limit(twoToOneShifter.limit() + 10);

        System.out.println("LAST WRAP");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(result1, Status.CLOSED, HandshakeStatus.NEED_UNWRAP,
            0, -1, false);
        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(result2, Status.CLOSED, HandshakeStatus.NEED_UNWRAP,
            0, -1, false);

        System.out.println("wrap1 = " + result1);
        System.out.println("wrap2 = " + result2);

        net1Len = result1.bytesProduced();
        net2Len = result2.bytesProduced();

        oneToTwo.flip();
        twoToOne.flip();

        oneToTwo.position(10);
        twoToOne.position(10);

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(result1, Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING,
            net1Len, 0, false);
        result2 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(result2, Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING,
            net2Len, 0, false);

        System.out.println("unwrap1 = " + result1);
        System.out.println("unwrap2 = " + result2);

        oneToTwoShifter.position(oneToTwo.position() - 10);
        oneToTwoShifter.limit(oneToTwo.limit() - 10);
        twoToOneShifter.position(twoToOne.position() - 10);
        twoToOneShifter.limit(twoToOne.limit() - 10);
        oneToTwoShifter.compact();
        twoToOneShifter.compact();
        oneToTwo.position(oneToTwoShifter.position() + 10);
        oneToTwo.limit(oneToTwoShifter.limit() + 10);
        twoToOne.position(twoToOneShifter.position() + 10);
        twoToOne.limit(twoToOneShifter.limit() + 10);

        System.out.println("EXTRA WRAP");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(result1, Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING,
            0, 0, false);
        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(result2, Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING,
            0, 0, false);

        System.out.println("wrap1 = " + result1);
        System.out.println("wrap2 = " + result2);

        oneToTwo.flip();
        twoToOne.flip();
        oneToTwo.position(10);
        twoToOne.position(10);

        System.out.println("EXTRA UNWRAP");
        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(result1, Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING,
            0, 0, false);
        result2 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(result2, Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING,
            0, 0, false);

        System.out.println("unwrap1 = " + result1);
        System.out.println("unwrap2 = " + result2);

        checkSession(ssls1, ssls2, ssls3, ssls4);
        System.out.println(ssle1);
        System.out.println(ssle2);
    }

    private static void checkSession(SSLSession ssls1, SSLSession ssls2,
            SSLSession ssls3, SSLSession ssls4) throws Exception {
        System.out.println("\nSession Info for SSLEngine1");
        System.out.println(ssls1);
        System.out.println(ssls1.getCreationTime());
        String peer1 = ssls1.getPeerHost();
        System.out.println(peer1);
        String protocol1 = ssls1.getProtocol();
        System.out.println(protocol1);
        java.security.cert.Certificate cert1 = ssls1.getPeerCertificates()[0];
        System.out.println(cert1);
        String ciphersuite1 = ssls1.getCipherSuite();
        System.out.println(ciphersuite1);
        System.out.println();

        System.out.println("\nSession Info for SSLEngine2");
        System.out.println(ssls2);
        System.out.println(ssls2.getCreationTime());
        String peer2 = ssls2.getPeerHost();
        System.out.println(peer2);
        String protocol2 = ssls2.getProtocol();
        System.out.println(protocol2);
        java.security.cert.Certificate cert2 = ssls2.getPeerCertificates()[0];
        System.out.println(cert2);
        String ciphersuite2 = ssls2.getCipherSuite();
        System.out.println(ciphersuite2);
        System.out.println();

        if (peer1.equals(peer2)) {
            throw new Exception("peer hostnames not equal");
        }

        if (!protocol1.equals(protocol2)) {
            throw new Exception("protocols not equal");
        }

        if (!cert1.equals(cert2)) {
            throw new Exception("certs not equal");
        }

        if (!ciphersuite1.equals(ciphersuite2)) {
            throw new Exception("ciphersuites not equal");
        }

        System.out.println("\nSession Info for SSLEngine3");
        System.out.println(ssls3);
        System.out.println("\nSession Info for SSLEngine4");
        System.out.println(ssls4);

        if (ssls3.equals(ssls1) || ssls4.equals(ssls2)) {
            throw new Exception("sessions should not be equals");
        }
    }

    public static void main(String args[]) throws Exception {
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        ConnectionTest ct = new ConnectionTest();
        ct.test();
    }
}
