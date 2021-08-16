/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 8255867
 * @summary SignatureScheme JSSE property does not preserve ordering in handshake messages
 * @library /javax/net/ssl/templates
 * @run main/othervm SigSchemePropOrdering
 */

import java.nio.ByteBuffer;
import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

public class SigSchemePropOrdering extends SSLEngineTemplate {

    // Helper map to correlate integral SignatureScheme identifiers to
    // their IANA string name counterparts.
    static final Map<Integer, String> sigSchemeMap = Map.ofEntries(
            new SimpleImmutableEntry(0x0401, "rsa_pkcs1_sha256"),
            new SimpleImmutableEntry(0x0501, "rsa_pkcs1_sha384"),
            new SimpleImmutableEntry(0x0601, "rsa_pkcs1_sha512"),
            new SimpleImmutableEntry(0x0403, "ecdsa_secp256r1_sha256"),
            new SimpleImmutableEntry(0x0503, "ecdsa_secp384r1_sha384"),
            new SimpleImmutableEntry(0x0603, "ecdsa_secp521r1_sha512"),
            new SimpleImmutableEntry(0x0804, "rsa_pss_rsae_sha256"),
            new SimpleImmutableEntry(0x0805, "rsa_pss_rsae_sha384"),
            new SimpleImmutableEntry(0x0806, "rsa_pss_rsae_sha512"),
            new SimpleImmutableEntry(0x0807, "ed25519"),
            new SimpleImmutableEntry(0x0808, "ed448"),
            new SimpleImmutableEntry(0x0809, "rsa_pss_pss_sha256"),
            new SimpleImmutableEntry(0x080a, "rsa_pss_pss_sha384"),
            new SimpleImmutableEntry(0x080b, "rsa_pss_pss_sha512"),
            new SimpleImmutableEntry(0x0101, "rsa_md5"),
            new SimpleImmutableEntry(0x0201, "rsa_pkcs1_sha1"),
            new SimpleImmutableEntry(0x0202, "dsa_sha1"),
            new SimpleImmutableEntry(0x0203, "ecdsa_sha1"),
            new SimpleImmutableEntry(0x0301, "rsa_sha224"),
            new SimpleImmutableEntry(0x0302, "dsa_sha224"),
            new SimpleImmutableEntry(0x0303, "ecdsa_sha224"),
            new SimpleImmutableEntry(0x0402, "rsa_pkcs1_sha256"));

    // Other useful TLS definitions for these tests
    private static final int TLS_HS_CLI_HELLO = 1;
    private static final int TLS_HS_CERT_REQ = 13;
    private static final int HELLO_EXT_SIG_ALGS = 13;

    private static final String SIG_SCHEME_STR =
            "rsa_pkcs1_sha256,rsa_pss_rsae_sha256,rsa_pss_pss_sha256," +
            "ed448,ed25519,ecdsa_secp256r1_sha256";

    SigSchemePropOrdering() throws Exception {
        super();
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("javax.net.debug", "ssl:handshake");
        System.setProperty("jdk.tls.client.SignatureSchemes", SIG_SCHEME_STR);
        System.setProperty("jdk.tls.server.SignatureSchemes", SIG_SCHEME_STR);
        new SigSchemePropOrdering().run();
    }

    @Override
    protected SSLEngine configureClientEngine(SSLEngine clientEngine) {
        clientEngine.setUseClientMode(true);
        clientEngine.setEnabledProtocols(new String[] { "TLSv1.2" });
        return clientEngine;
    }

    @Override
    protected SSLEngine configureServerEngine(SSLEngine serverEngine) {
        serverEngine.setUseClientMode(false);
        serverEngine.setWantClientAuth(true);
        return serverEngine;
    }

    private void run() throws Exception {
        // Start the handshake.  Check the ClientHello's signature_algorithms
        // extension and make sure the ordering matches what we specified
        // in the property above.
        List<String> expectedSS = Arrays.asList(SIG_SCHEME_STR.split(","));

        clientEngine.wrap(clientOut, cTOs);
        cTOs.flip();

        List<String> actualSS = getSigSchemesCliHello(
                extractHandshakeMsg(cTOs, TLS_HS_CLI_HELLO));

        // Make sure the ordering is correct
        if (!expectedSS.equals(actualSS)) {
            System.out.println("FAIL: Mismatch between property ordering " +
                    "and ClientHello message");
            System.out.print("Expected SigSchemes: ");
            expectedSS.forEach(ss -> System.out.print(ss + " "));
            System.out.println();
            System.out.print("Actual SigSchemes: ");
            actualSS.forEach(ss -> System.out.print(ss + " "));
            System.out.println();
            throw new RuntimeException(
                    "FAIL: Expected and Actual values differ.");
        }

        // Consume the ClientHello and get the server flight of handshake
        // messages.  We expect that it will be one TLS record containing
        // multiple handshake messages, one of which is a CertificateRequest.
        serverEngine.unwrap(cTOs, serverIn);
        runDelegatedTasks(serverEngine);

        // Wrap the server flight
        serverEngine.wrap(serverOut, sTOc);
        sTOc.flip();

        actualSS = getSigSchemesCertReq(
                extractHandshakeMsg(sTOc, TLS_HS_CERT_REQ));

        // Make sure the ordering is correct
        if (!expectedSS.equals(actualSS)) {
            System.out.println("FAIL: Mismatch between property ordering " +
                    "and CertificateRequest message");
            System.out.print("Expected SigSchemes: ");
            expectedSS.forEach(ss -> System.out.print(ss + " "));
            System.out.println();
            System.out.print("Actual SigSchemes: ");
            actualSS.forEach(ss -> System.out.print(ss + " "));
            System.out.println();
            throw new RuntimeException(
                    "FAIL: Expected and Actual values differ.");
        }
    }

    /**
     * Given a TLS record containing one or more handshake messages, return
     * the specific handshake message as a ByteBuffer (a slice of the record)
     *
     * @param tlsRecord a ByteBuffer containing a TLS record.  It is assumed
     *      that the position of the ByteBuffer is on the first byte of the TLS
     *      record header.
     * @param hsMsgId the message identifier for the handshake message being
     *      sought.
     *
     * @return a ByteBuffer containing the TLS handshake message.  The position
     *      of the returned ByteBuffer will be on the first byte of the TLS
     *      handshake message data, immediately following the handshake header.
     *      If the message is not found, null will be returned.
     *
     * @throws SSLException if the incoming ByteBuffer does not contain a
     *      well-formed TLS message.
     */
    private static ByteBuffer extractHandshakeMsg(ByteBuffer tlsRecord,
            int hsMsgId) throws SSLException {
        Objects.requireNonNull(tlsRecord);
        tlsRecord.mark();

        // Process the TLS record header
        int type = Byte.toUnsignedInt(tlsRecord.get());
        int ver_major = Byte.toUnsignedInt(tlsRecord.get());
        int ver_minor = Byte.toUnsignedInt(tlsRecord.get());
        int recLen = Short.toUnsignedInt(tlsRecord.getShort());

        // Simple sanity checks
        if (type != 22) {
            throw new SSLException("Not a handshake: Type = " + type);
        } else if (recLen > tlsRecord.remaining()) {
            throw new SSLException("Incomplete record in buffer: " +
                    "Record length = " + recLen + ", Remaining = " +
                    tlsRecord.remaining());
        }

        while (tlsRecord.hasRemaining()) {
            // Grab the handshake message header.
            int msgHdr = tlsRecord.getInt();
            int msgType = (msgHdr >> 24) & 0x000000FF;
            int msgLen = msgHdr & 0x00FFFFFF;

            if (msgType == hsMsgId) {
                // Slice the buffer such that it contains the entire
                // handshake message (less the handshake header).
                ByteBuffer buf = tlsRecord.slice(tlsRecord.position(), msgLen);
                tlsRecord.reset();
                return buf;
            } else {
                // Skip to the next handshake message, if there is one
                tlsRecord.position(tlsRecord.position() + msgLen);
            }
        }

        tlsRecord.reset();
        return null;
    }


    /**
     * Parses the ClientHello message and extracts from it a list of
     * SignatureScheme values in string form.  It is assumed that the provided
     * ByteBuffer has its position set at the first byte of the ClientHello
     * message body (AFTER the handshake header) and contains the entire
     * hello message.  Upon successful completion of this method the ByteBuffer
     * will have its position reset to the initial offset in the buffer.
     * If an exception is thrown the position at the time of the exception
     * will be preserved.
     *
     * @param data the ByteBuffer containing the ClientHello bytes
     *
     * @returns A List of the signature schemes in string form.  If no
     * signature_algorithms extension is present in the client hello then
     * an empty list will be returned.
     */
    private static List<String> getSigSchemesCliHello(ByteBuffer data) {
        Objects.requireNonNull(data);
        data.mark();

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
        List<String> extSigAlgs = new ArrayList();
        int extsLen = Short.toUnsignedInt(data.getShort());
        while (data.hasRemaining()) {
            int extType = Short.toUnsignedInt(data.getShort());
            int extLen = Short.toUnsignedInt(data.getShort());
            if (extType == HELLO_EXT_SIG_ALGS) {
                // Start processing signature algorithms
                int sigSchemeLen = Short.toUnsignedInt(data.getShort());
                for (int ssOff = 0; ssOff < sigSchemeLen; ssOff += 2) {
                    String schemeName = sigSchemeMap.get(
                            Short.toUnsignedInt(data.getShort()));
                    if (schemeName != null) {
                        extSigAlgs.add(schemeName);
                    }
                }
            } else {
                // Not the extension we're looking for.  Skip past the
                // extension data
                data.position(data.position() + extLen);
            }
        }

        // We should be at the end of the ClientHello
        data.reset();
        return extSigAlgs;
    }

    /**
     * Parses the CertificateRequest message and extracts from it a list of
     * SignatureScheme values in string form.  It is assumed that the provided
     * ByteBuffer has its position set at the first byte of the
     * CertificateRequest message body (AFTER the handshake header) and
     * contains the entire CR message.  Upon successful completion of this
     * method the ByteBuffer will have its position reset to the initial
     * offset in the buffer.
     * If an exception is thrown the position at the time of the exception
     * will be preserved.
     *
     * @param data the ByteBuffer containing the CertificateRequest bytes
     *
     * @returns A List of the signature schemes in string form.  If no
     * signature_algorithms extension is present in the CertificateRequest
     * then an empty list will be returned.
     */
    private static List<String> getSigSchemesCertReq(ByteBuffer data) {
        Objects.requireNonNull(data);
        data.mark();

        // Jump past the certificate types
        int certTypeLen = Byte.toUnsignedInt(data.get());
        if (certTypeLen != 0) {
            data.position(data.position() + certTypeLen);
        }

        // Collect the SignatureAndHashAlgorithms
        List<String> extSigAlgs = new ArrayList();
        int sigSchemeLen = Short.toUnsignedInt(data.getShort());
        for (int ssOff = 0; ssOff < sigSchemeLen; ssOff += 2) {
            String schemeName = sigSchemeMap.get(
                    Short.toUnsignedInt(data.getShort()));
            if (schemeName != null) {
                extSigAlgs.add(schemeName);
            }
        }

        data.reset();
        return extSigAlgs;
    }
}
