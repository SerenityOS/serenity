/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.

/*
 * @test
 * @bug 8145854 8153829
 * @summary SSLContextImpl.statusResponseManager should be generated if required
 * @library ../../../../java/security/testlibrary
 * @build CertificateBuilder SimpleOCSPServer
 * @run main/othervm StapleEnableProps
 */

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.nio.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import sun.security.testlibrary.SimpleOCSPServer;
import sun.security.testlibrary.CertificateBuilder;

public class StapleEnableProps {

    /*
     * Enables logging of the SSLEngine operations.
     */
    private static final boolean logging = true;

    /*
     * Enables the JSSE system debugging system property:
     *
     *     -Djavax.net.debug=all
     *
     * This gives a lot of low-level information about operations underway,
     * including specific handshake messages, and might be best examined
     * after gaining some familiarity with this application.
     */
    private static final boolean debug = false;

    // These four ByteBuffer references will be used to hang onto ClientHello
    // messages with and without the status_request[_v2] extensions.  These
    // will be used in the server-side stapling tests.  There are two sets,
    // one for 1.2 and earlier versions of the protocol and one for 1.3
    // and later versions, since the handshake and extension sets differ
    // between the two sets.
    private static ByteBuffer cHello12Staple;
    private static ByteBuffer cHello12NoStaple;
    private static ByteBuffer cHello13Staple;
    private static ByteBuffer cHello13NoStaple;

    // The following items are used to set up the keystores.
    private static final String passwd = "passphrase";
    private static final String ROOT_ALIAS = "root";
    private static final String INT_ALIAS = "intermediate";
    private static final String SSL_ALIAS = "ssl";

    // PKI components we will need for this test
    private static KeyManagerFactory kmf;
    private static TrustManagerFactory tmf;
    private static KeyStore rootKeystore;       // Root CA Keystore
    private static KeyStore intKeystore;        // Intermediate CA Keystore
    private static KeyStore serverKeystore;     // SSL Server Keystore
    private static KeyStore trustStore;         // SSL Client trust store
    private static SimpleOCSPServer rootOcsp;   // Root CA OCSP Responder
    private static int rootOcspPort;            // Port for root OCSP
    private static SimpleOCSPServer intOcsp;    // Intermediate CA OCSP server
    private static int intOcspPort;             // Port for intermediate OCSP

    // Extra configuration parameters and constants
    static final String[] TLS13ONLY = new String[] { "TLSv1.3" };
    static final String[] TLS12MAX =
            new String[] { "TLSv1.2", "TLSv1.1", "TLSv1" };

    // A few helpful TLS definitions to make it easier
    private static final int HELLO_EXT_STATUS_REQ = 5;
    private static final int HELLO_EXT_STATUS_REQ_V2 = 17;

    /*
     * Main entry point for this test.
     */
    public static void main(String args[]) throws Exception {
        if (debug) {
            System.setProperty("javax.net.debug", "ssl:handshake,verbose");
        }

        // Create the PKI we will use for the test and start the OCSP servers
        createPKI();

        // Set up the KeyManagerFactory and TrustManagerFactory
        kmf = KeyManagerFactory.getInstance("PKIX");
        kmf.init(serverKeystore, passwd.toCharArray());
        tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(trustStore);

        // Run the client and server property tests
        testClientProp();
        testServerProp();

    }

    private static void testClientProp() throws Exception {
        SSLEngineResult clientResult;

        // Test with the client-side enable property set to true
        System.out.println("=========================================");
        System.out.println("Client Test 1: " +
                "jdk.tls.client.enableStatusRequestExtension = true");
        System.out.println("Version = TLS 1.2");
        System.out.println("=========================================");

        System.setProperty("jdk.tls.client.enableStatusRequestExtension",
                "true");
        SSLContext ctxStaple = SSLContext.getInstance("TLS");
        ctxStaple.init(null, tmf.getTrustManagers(), null);
        SSLEngine engine = ctxStaple.createSSLEngine();
        engine.setUseClientMode(true);
        engine.setEnabledProtocols(TLS12MAX);
        SSLSession session = engine.getSession();
        ByteBuffer clientOut = ByteBuffer.wrap("I'm a Client".getBytes());
        ByteBuffer cTOs =
                ByteBuffer.allocateDirect(session.getPacketBufferSize());

        // Create and check the ClientHello message
        clientResult = engine.wrap(clientOut, cTOs);
        log("client wrap: ", clientResult);
        if (clientResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new SSLException("Client wrap got status: " +
                    clientResult.getStatus());
        }
        cTOs.flip();
        System.out.println(dumpHexBytes(cTOs));
        checkClientHello(cTOs, true, true);
        cHello12Staple = cTOs;

        // Test with the property set to false
        System.out.println("=========================================");
        System.out.println("Client Test 2: " +
                "jdk.tls.client.enableStatusRequestExtension = false");
        System.out.println("Version = TLS 1.2");
        System.out.println("=========================================");

        System.setProperty("jdk.tls.client.enableStatusRequestExtension",
                "false");
        SSLContext ctxNoStaple = SSLContext.getInstance("TLS");
        ctxNoStaple.init(null, tmf.getTrustManagers(), null);
        engine = ctxNoStaple.createSSLEngine();
        engine.setUseClientMode(true);
        engine.setEnabledProtocols(TLS12MAX);
        session = engine.getSession();
        cTOs = ByteBuffer.allocateDirect(session.getPacketBufferSize());

        // Create and check the ClientHello message
        clientResult = engine.wrap(clientOut, cTOs);
        log("client wrap: ", clientResult);
        if (clientResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new SSLException("Client wrap got status: " +
                    clientResult.getStatus());
        }
        cTOs.flip();
        System.out.println(dumpHexBytes(cTOs));
        checkClientHello(cTOs, false, false);
        cHello12NoStaple = cTOs;

        // Turn the property back on to true and test using TLS 1.3
        System.out.println("=========================================");
        System.out.println("Client Test 3: " +
                "jdk.tls.client.enableStatusRequestExtension = true");
        System.out.println("Version = TLS 1.3");
        System.out.println("=========================================");

        System.setProperty("jdk.tls.client.enableStatusRequestExtension",
                "true");
        ctxStaple = SSLContext.getInstance("TLS");
        ctxStaple.init(null, tmf.getTrustManagers(), null);
        engine = ctxStaple.createSSLEngine();
        engine.setUseClientMode(true);
        engine.setEnabledProtocols(TLS13ONLY);
        session = engine.getSession();
        cTOs = ByteBuffer.allocateDirect(session.getPacketBufferSize());

        // Create and check the ClientHello message
        clientResult = engine.wrap(clientOut, cTOs);
        log("client wrap: ", clientResult);
        if (clientResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new SSLException("Client wrap got status: " +
                    clientResult.getStatus());
        }
        cTOs.flip();
        System.out.println(dumpHexBytes(cTOs));
        checkClientHello(cTOs, true, false);
        cHello13Staple = cTOs;

        // Turn the property off again and test in a TLS 1.3 handshake
        System.out.println("=========================================");
        System.out.println("Client Test 4: " +
                "jdk.tls.client.enableStatusRequestExtension = false");
        System.out.println("Version = TLS 1.3");
        System.out.println("=========================================");

        System.setProperty("jdk.tls.client.enableStatusRequestExtension",
                "false");
        ctxNoStaple = SSLContext.getInstance("TLS");
        ctxNoStaple.init(null, tmf.getTrustManagers(), null);
        engine = ctxNoStaple.createSSLEngine();
        engine.setUseClientMode(true);
        engine.setEnabledProtocols(TLS13ONLY);
        session = engine.getSession();
        cTOs = ByteBuffer.allocateDirect(session.getPacketBufferSize());

        // Create and check the ClientHello message
        clientResult = engine.wrap(clientOut, cTOs);
        log("client wrap: ", clientResult);
        if (clientResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new SSLException("Client wrap got status: " +
                    clientResult.getStatus());
        }
        cTOs.flip();
        System.out.println(dumpHexBytes(cTOs));
        checkClientHello(cTOs, false, false);
        cHello13NoStaple = cTOs;

        // A TLS 1.3-capable hello, one that is not strictly limited to
        // the TLS 1.3 protocol should have both status_request and
        // status_request_v2
        System.out.println("=========================================");
        System.out.println("Client Test 5: " +
                "jdk.tls.client.enableStatusRequestExtension = true");
        System.out.println("Version = TLS 1.3 capable [default hello]");
        System.out.println("=========================================");

        System.setProperty("jdk.tls.client.enableStatusRequestExtension",
                "true");
        ctxStaple = SSLContext.getInstance("TLS");
        ctxStaple.init(null, tmf.getTrustManagers(), null);
        engine = ctxStaple.createSSLEngine();
        engine.setUseClientMode(true);
        // Note: Unlike the other tests, there is no explicit protocol setting
        session = engine.getSession();
        cTOs = ByteBuffer.allocateDirect(session.getPacketBufferSize());

        // Create and check the ClientHello message
        clientResult = engine.wrap(clientOut, cTOs);
        log("client wrap: ", clientResult);
        if (clientResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new SSLException("Client wrap got status: " +
                    clientResult.getStatus());
        }
        cTOs.flip();
        System.out.println(dumpHexBytes(cTOs));
        checkClientHello(cTOs, true, true);
    }

    private static void testServerProp() throws Exception {
        SSLEngineResult serverResult;
        HandshakeStatus hsStat;

        // Test with the server-side enable property set to true
        System.out.println("=========================================");
        System.out.println("Server Test 1: " +
                "jdk.tls.server.enableStatusRequestExtension = true");
        System.out.println("Version = TLS 1.2");
        System.out.println("=========================================");

        System.setProperty("jdk.tls.server.enableStatusRequestExtension",
                "true");
        SSLContext ctxStaple = SSLContext.getInstance("TLS");
        ctxStaple.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        SSLEngine engine = ctxStaple.createSSLEngine();
        engine.setUseClientMode(false);
        engine.setEnabledProtocols(TLS12MAX);
        SSLSession session = engine.getSession();
        ByteBuffer serverOut = ByteBuffer.wrap("I'm a Server".getBytes());
        ByteBuffer serverIn =
                ByteBuffer.allocate(session.getApplicationBufferSize() + 50);
        ByteBuffer sTOc =
                ByteBuffer.allocateDirect(session.getPacketBufferSize());

        // Consume the client hello
        serverResult = engine.unwrap(cHello12Staple, serverIn);
        log("server unwrap: ", serverResult);
        if (serverResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new SSLException("Server unwrap got status: " +
                    serverResult.getStatus());
        } else if (serverResult.getHandshakeStatus() !=
                SSLEngineResult.HandshakeStatus.NEED_TASK) {
             throw new SSLException("Server unwrap expected NEED_TASK, got: " +
                    serverResult.getHandshakeStatus());
        }
        runDelegatedTasks(serverResult, engine);
        if (engine.getHandshakeStatus() !=
                SSLEngineResult.HandshakeStatus.NEED_WRAP) {
            throw new SSLException("Expected NEED_WRAP, got: " +
                    engine.getHandshakeStatus());
        }

        // Generate a TLS record with the ServerHello
        serverResult = engine.wrap(serverOut, sTOc);
        log("client wrap: ", serverResult);
        if (serverResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new SSLException("Client wrap got status: " +
                    serverResult.getStatus());
        }
        sTOc.flip();
        System.out.println(dumpHexBytes(sTOc));
        checkServerHello(sTOc, false, true);

        // Flip the client hello so we can reuse it in the next test.
        cHello12Staple.flip();

        // Test with the server-side enable property set to false
        System.out.println("=========================================");
        System.out.println("Server Test 2: " +
                "jdk.tls.server.enableStatusRequestExtension = false");
        System.out.println("Version = TLS 1.2");
        System.out.println("=========================================");

        System.setProperty("jdk.tls.server.enableStatusRequestExtension",
                "false");
        SSLContext ctxNoStaple = SSLContext.getInstance("TLS");
        ctxNoStaple.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        engine = ctxNoStaple.createSSLEngine();
        engine.setUseClientMode(false);
        engine.setEnabledProtocols(TLS12MAX);
        session = engine.getSession();
        serverIn = ByteBuffer.allocate(session.getApplicationBufferSize() + 50);
        sTOc = ByteBuffer.allocateDirect(session.getPacketBufferSize());

        // Consume the client hello
        serverResult = engine.unwrap(cHello12Staple, serverIn);
        log("server unwrap: ", serverResult);
        if (serverResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new SSLException("Server unwrap got status: " +
                    serverResult.getStatus());
        } else if (serverResult.getHandshakeStatus() !=
                SSLEngineResult.HandshakeStatus.NEED_TASK) {
             throw new SSLException("Server unwrap expected NEED_TASK, got: " +
                    serverResult.getHandshakeStatus());
        }
        runDelegatedTasks(serverResult, engine);
        if (engine.getHandshakeStatus() !=
                SSLEngineResult.HandshakeStatus.NEED_WRAP) {
            throw new SSLException("Expected NEED_WRAP, got: " +
                    engine.getHandshakeStatus());
        }

        // Generate a TLS record with the ServerHello
        serverResult = engine.wrap(serverOut, sTOc);
        log("client wrap: ", serverResult);
        if (serverResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new SSLException("Client wrap got status: " +
                    serverResult.getStatus());
        }
        sTOc.flip();
        System.out.println(dumpHexBytes(sTOc));
        checkServerHello(sTOc, false, false);
    }

    /*
     * If the result indicates that we have outstanding tasks to do,
     * go ahead and run them in this thread.
     */
    private static void runDelegatedTasks(SSLEngineResult result,
            SSLEngine engine) throws Exception {

        if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                log("\trunning delegated task...");
                runnable.run();
            }
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
            log("\tnew HandshakeStatus: " + hsStatus);
        }
    }

    private static void log(String str, SSLEngineResult result) {
        if (!logging) {
            return;
        }
        HandshakeStatus hsStatus = result.getHandshakeStatus();
        log(str +
            result.getStatus() + "/" + hsStatus + ", " +
            result.bytesConsumed() + "/" + result.bytesProduced() +
            " bytes");
        if (hsStatus == HandshakeStatus.FINISHED) {
            log("\t...ready for application data");
        }
    }

    private static void log(String str) {
        if (logging) {
            System.out.println(str);
        }
    }

    /**
     * Dump a ByteBuffer as a hexdump to stdout.  The dumping routine will
     * start at the current position of the buffer and run to its limit.
     * After completing the dump, the position will be returned to its
     * starting point.
     *
     * @param data the ByteBuffer to dump to stdout.
     *
     * @return the hexdump of the byte array.
     */
    private static String dumpHexBytes(ByteBuffer data) {
        StringBuilder sb = new StringBuilder();
        if (data != null) {
            int i = 0;
            data.mark();
            while (data.hasRemaining()) {
                if (i % 16 == 0 && i != 0) {
                    sb.append("\n");
                }
                sb.append(String.format("%02X ", data.get()));
                i++;
            }
            data.reset();
        }

        return sb.toString();
    }

    /**
     * Tests the ClientHello for the presence (or not) of the status_request
     * and status_request_v2 hello extensions.  It is assumed that the provided
     * ByteBuffer has its position set at the first byte of the TLS record
     * containing the ClientHello and contains the entire hello message.  Upon
     * successful completion of this method the ByteBuffer will have its
     * position reset to the initial offset in the buffer.  If an exception is
     * thrown the position at the time of the exception will be preserved.
     *
     * @param data the ByteBuffer containing the ClientHello bytes
     * @param statReqPresent true if the status_request hello extension should
     * be present.
     * @param statReqV2Present true if the status_request_v2 hello extension
     * should be present.
     *
     * @throws SSLException if the presence or lack of either the
     * status_request or status_request_v2 extensions is inconsistent with
     * the expected settings in the statReqPresent or statReqV2Present
     * parameters.
     */
    private static void checkClientHello(ByteBuffer data,
            boolean statReqPresent, boolean statReqV2Present)
            throws SSLException {
        boolean hasV1 = false;
        boolean hasV2 = false;
        Objects.requireNonNull(data);
        data.mark();

        // Process the TLS record header
        int type = Byte.toUnsignedInt(data.get());
        int ver_major = Byte.toUnsignedInt(data.get());
        int ver_minor = Byte.toUnsignedInt(data.get());
        int recLen = Short.toUnsignedInt(data.getShort());

        // Simple sanity checks
        if (type != 22) {
            throw new SSLException("Not a handshake: Type = " + type);
        } else if (recLen > data.remaining()) {
            throw new SSLException("Incomplete record in buffer: " +
                    "Record length = " + recLen + ", Remaining = " +
                    data.remaining());
        }

        // Grab the handshake message header.
        int msgHdr = data.getInt();
        int msgType = (msgHdr >> 24) & 0x000000FF;
        int msgLen = msgHdr & 0x00FFFFFF;

        // More simple sanity checks
        if (msgType != 1) {
            throw new SSLException("Not a ClientHello: Type = " + msgType);
        }

        // Skip over the protocol version and client random
        data.position(data.position() + 34);

        // Jump past the session ID (if there is one)
        int sessLen = Byte.toUnsignedInt(data.get());
        if (sessLen != 0) {
            data.position(data.position() + sessLen);
        }

        // Jump past the cipher suites
        int csLen = Short.toUnsignedInt(data.getShort());
        if (csLen != 0) {
            data.position(data.position() + csLen);
        }

        // ...and the compression
        int compLen = Byte.toUnsignedInt(data.get());
        if (compLen != 0) {
            data.position(data.position() + compLen);
        }

        // Now for the fun part.  Go through the extensions and look
        // for the two status request exts.
        int extsLen = Short.toUnsignedInt(data.getShort());
        while (data.hasRemaining()) {
            int extType = Short.toUnsignedInt(data.getShort());
            int extLen = Short.toUnsignedInt(data.getShort());
            hasV1 |= (extType == HELLO_EXT_STATUS_REQ);
            hasV2 |= (extType == HELLO_EXT_STATUS_REQ_V2);
            data.position(data.position() + extLen);
        }

        if (hasV1 != statReqPresent) {
            throw new SSLException("The status_request extension is " +
                    "inconsistent with the expected result: expected = " +
                    statReqPresent + ", actual = " + hasV1);
        } else if (hasV2 != statReqV2Present) {
            throw new SSLException("The status_request_v2 extension is " +
                    "inconsistent with the expected result: expected = " +
                    statReqV2Present + ", actual = " + hasV2);
        }

        // We should be at the end of the ClientHello
        data.reset();
    }

    /**
     * Tests the ServerHello for the presence (or not) of the status_request
     * or status_request_v2 hello extension.  It is assumed that the provided
     * ByteBuffer has its position set at the first byte of the TLS record
     * containing the ServerHello and contains the entire hello message.  Upon
     * successful completion of this method the ByteBuffer will have its
     * position reset to the initial offset in the buffer.  If an exception is
     * thrown the position at the time of the exception will be preserved.
     *
     * @param statReqPresent true if the status_request hello extension should
     * be present.
     * @param statReqV2Present true if the status_request_v2 hello extension
     * should be present.
     *
     * @throws SSLException if the presence or lack of either the
     * status_request or status_request_v2 extensions is inconsistent with
     * the expected settings in the statReqPresent or statReqV2Present
     * parameters.
     */
    private static void checkServerHello(ByteBuffer data,
            boolean statReqPresent, boolean statReqV2Present)
            throws SSLException {
        boolean hasV1 = false;
        boolean hasV2 = false;
        Objects.requireNonNull(data);
        int startPos = data.position();
        data.mark();

        // Process the TLS record header
        int type = Byte.toUnsignedInt(data.get());
        int ver_major = Byte.toUnsignedInt(data.get());
        int ver_minor = Byte.toUnsignedInt(data.get());
        int recLen = Short.toUnsignedInt(data.getShort());

        // Simple sanity checks
        if (type != 22) {
            throw new SSLException("Not a handshake: Type = " + type);
        } else if (recLen > data.remaining()) {
            throw new SSLException("Incomplete record in buffer: " +
                    "Record length = " + recLen + ", Remaining = " +
                    data.remaining());
        }

        // Grab the handshake message header.
        int msgHdr = data.getInt();
        int msgType = (msgHdr >> 24) & 0x000000FF;
        int msgLen = msgHdr & 0x00FFFFFF;

        // More simple sanity checks
        if (msgType != 2) {
            throw new SSLException("Not a ServerHello: Type = " + msgType);
        }

        // Skip over the protocol version and server random
        data.position(data.position() + 34);

        // Jump past the session ID
        int sessLen = Byte.toUnsignedInt(data.get());
        if (sessLen != 0) {
            data.position(data.position() + sessLen);
        }

        // Skip the cipher suite and compression method
        data.position(data.position() + 3);

        // Go through the extensions and look for the request extension
        // expected by the caller.
        int extsLen = Short.toUnsignedInt(data.getShort());
        while (data.position() < recLen + startPos + 5) {
            int extType = Short.toUnsignedInt(data.getShort());
            int extLen = Short.toUnsignedInt(data.getShort());
            hasV1 |= (extType == HELLO_EXT_STATUS_REQ);
            hasV2 |= (extType == HELLO_EXT_STATUS_REQ_V2);
            data.position(data.position() + extLen);
        }

        if (hasV1 != statReqPresent) {
            throw new SSLException("The status_request extension is " +
                    "inconsistent with the expected result: expected = " +
                    statReqPresent + ", actual = " + hasV1);
        } else if (hasV2 != statReqV2Present) {
            throw new SSLException("The status_request_v2 extension is " +
                    "inconsistent with the expected result: expected = " +
                    statReqV2Present + ", actual = " + hasV2);
        }

        // Reset the position to the initial spot at the start of this method.
        data.reset();
    }

    /**
     * Creates the PKI components necessary for this test, including
     * Root CA, Intermediate CA and SSL server certificates, the keystores
     * for each entity, a client trust store, and starts the OCSP responders.
     */
    private static void createPKI() throws Exception {
        CertificateBuilder cbld = new CertificateBuilder();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyStore.Builder keyStoreBuilder =
                KeyStore.Builder.newInstance("PKCS12", null,
                        new KeyStore.PasswordProtection(passwd.toCharArray()));

        // Generate Root, IntCA, EE keys
        KeyPair rootCaKP = keyGen.genKeyPair();
        log("Generated Root CA KeyPair");
        KeyPair intCaKP = keyGen.genKeyPair();
        log("Generated Intermediate CA KeyPair");
        KeyPair sslKP = keyGen.genKeyPair();
        log("Generated SSL Cert KeyPair");

        // Set up the Root CA Cert
        cbld.setSubjectName("CN=Root CA Cert, O=SomeCompany");
        cbld.setPublicKey(rootCaKP.getPublic());
        cbld.setSerialNumber(new BigInteger("1"));
        // Make a 3 year validity starting from 60 days ago
        long start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60);
        long end = start + TimeUnit.DAYS.toMillis(1085);
        cbld.setValidity(new Date(start), new Date(end));
        addCommonExts(cbld, rootCaKP.getPublic(), rootCaKP.getPublic());
        addCommonCAExts(cbld);
        // Make our Root CA Cert!
        X509Certificate rootCert = cbld.build(null, rootCaKP.getPrivate(),
                "SHA256withRSA");
        log("Root CA Created:\n" + certInfo(rootCert));

        // Now build a keystore and add the keys and cert
        rootKeystore = keyStoreBuilder.getKeyStore();
        java.security.cert.Certificate[] rootChain = {rootCert};
        rootKeystore.setKeyEntry(ROOT_ALIAS, rootCaKP.getPrivate(),
                passwd.toCharArray(), rootChain);

        // Now fire up the OCSP responder
        rootOcsp = new SimpleOCSPServer(rootKeystore, passwd, ROOT_ALIAS, null);
        rootOcsp.enableLog(logging);
        rootOcsp.setNextUpdateInterval(3600);
        rootOcsp.start();

        // Wait 5 seconds for server ready
        for (int i = 0; (i < 100 && !rootOcsp.isServerReady()); i++) {
            Thread.sleep(50);
        }
        if (!rootOcsp.isServerReady()) {
            throw new RuntimeException("Server not ready yet");
        }

        rootOcspPort = rootOcsp.getPort();
        String rootRespURI = "http://localhost:" + rootOcspPort;
        log("Root OCSP Responder URI is " + rootRespURI);

        // Now that we have the root keystore and OCSP responder we can
        // create our intermediate CA.
        cbld.reset();
        cbld.setSubjectName("CN=Intermediate CA Cert, O=SomeCompany");
        cbld.setPublicKey(intCaKP.getPublic());
        cbld.setSerialNumber(new BigInteger("100"));
        // Make a 2 year validity starting from 30 days ago
        start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        end = start + TimeUnit.DAYS.toMillis(730);
        cbld.setValidity(new Date(start), new Date(end));
        addCommonExts(cbld, intCaKP.getPublic(), rootCaKP.getPublic());
        addCommonCAExts(cbld);
        cbld.addAIAExt(Collections.singletonList(rootRespURI));
        // Make our Intermediate CA Cert!
        X509Certificate intCaCert = cbld.build(rootCert, rootCaKP.getPrivate(),
                "SHA256withRSA");
        log("Intermediate CA Created:\n" + certInfo(intCaCert));

        // Provide intermediate CA cert revocation info to the Root CA
        // OCSP responder.
        Map<BigInteger, SimpleOCSPServer.CertStatusInfo> revInfo =
            new HashMap<>();
        revInfo.put(intCaCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_GOOD));
        rootOcsp.updateStatusDb(revInfo);

        // Now build a keystore and add the keys, chain and root cert as a TA
        intKeystore = keyStoreBuilder.getKeyStore();
        java.security.cert.Certificate[] intChain = {intCaCert, rootCert};
        intKeystore.setKeyEntry(INT_ALIAS, intCaKP.getPrivate(),
                passwd.toCharArray(), intChain);
        intKeystore.setCertificateEntry(ROOT_ALIAS, rootCert);

        // Now fire up the Intermediate CA OCSP responder
        intOcsp = new SimpleOCSPServer(intKeystore, passwd,
                INT_ALIAS, null);
        intOcsp.enableLog(logging);
        intOcsp.setNextUpdateInterval(3600);
        intOcsp.start();

        // Wait 5 seconds for server ready
        for (int i = 0; (i < 100 && !intOcsp.isServerReady()); i++) {
            Thread.sleep(50);
        }
        if (!intOcsp.isServerReady()) {
            throw new RuntimeException("Server not ready yet");
        }

        intOcspPort = intOcsp.getPort();
        String intCaRespURI = "http://localhost:" + intOcspPort;
        log("Intermediate CA OCSP Responder URI is " + intCaRespURI);

        // Last but not least, let's make our SSLCert and add it to its own
        // Keystore
        cbld.reset();
        cbld.setSubjectName("CN=SSLCertificate, O=SomeCompany");
        cbld.setPublicKey(sslKP.getPublic());
        cbld.setSerialNumber(new BigInteger("4096"));
        // Make a 1 year validity starting from 7 days ago
        start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        end = start + TimeUnit.DAYS.toMillis(365);
        cbld.setValidity(new Date(start), new Date(end));

        // Add extensions
        addCommonExts(cbld, sslKP.getPublic(), intCaKP.getPublic());
        boolean[] kuBits = {true, false, true, false, false, false,
            false, false, false};
        cbld.addKeyUsageExt(kuBits);
        List<String> ekuOids = new ArrayList<>();
        ekuOids.add("1.3.6.1.5.5.7.3.1");
        ekuOids.add("1.3.6.1.5.5.7.3.2");
        cbld.addExtendedKeyUsageExt(ekuOids);
        cbld.addSubjectAltNameDNSExt(Collections.singletonList("localhost"));
        cbld.addAIAExt(Collections.singletonList(intCaRespURI));
        // Make our SSL Server Cert!
        X509Certificate sslCert = cbld.build(intCaCert, intCaKP.getPrivate(),
                "SHA256withRSA");
        log("SSL Certificate Created:\n" + certInfo(sslCert));

        // Provide SSL server cert revocation info to the Intermeidate CA
        // OCSP responder.
        revInfo = new HashMap<>();
        revInfo.put(sslCert.getSerialNumber(),
                new SimpleOCSPServer.CertStatusInfo(
                        SimpleOCSPServer.CertStatus.CERT_STATUS_GOOD));
        intOcsp.updateStatusDb(revInfo);

        // Now build a keystore and add the keys, chain and root cert as a TA
        serverKeystore = keyStoreBuilder.getKeyStore();
        java.security.cert.Certificate[] sslChain = {sslCert, intCaCert, rootCert};
        serverKeystore.setKeyEntry(SSL_ALIAS, sslKP.getPrivate(),
                passwd.toCharArray(), sslChain);
        serverKeystore.setCertificateEntry(ROOT_ALIAS, rootCert);

        // And finally a Trust Store for the client
        trustStore = keyStoreBuilder.getKeyStore();
        trustStore.setCertificateEntry(ROOT_ALIAS, rootCert);
    }

    private static void addCommonExts(CertificateBuilder cbld,
            PublicKey subjKey, PublicKey authKey) throws IOException {
        cbld.addSubjectKeyIdExt(subjKey);
        cbld.addAuthorityKeyIdExt(authKey);
    }

    private static void addCommonCAExts(CertificateBuilder cbld)
            throws IOException {
        cbld.addBasicConstraintsExt(true, true, -1);
        // Set key usage bits for digitalSignature, keyCertSign and cRLSign
        boolean[] kuBitSettings = {true, false, false, false, false, true,
            true, false, false};
        cbld.addKeyUsageExt(kuBitSettings);
    }

    /**
     * Helper routine that dumps only a few cert fields rather than
     * the whole toString() output.
     *
     * @param cert an X509Certificate to be displayed
     *
     * @return the String output of the issuer, subject and
     * serial number
     */
    private static String certInfo(X509Certificate cert) {
        StringBuilder sb = new StringBuilder();
        sb.append("Issuer: ").append(cert.getIssuerX500Principal()).
                append("\n");
        sb.append("Subject: ").append(cert.getSubjectX500Principal()).
                append("\n");
        sb.append("Serial: ").append(cert.getSerialNumber()).append("\n");
        return sb.toString();
    }
}
