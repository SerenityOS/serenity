/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
// system properties in samevm/agentvm mode.  For further debugging output
// set the -Djavax.net.debug=ssl:handshake property on the @run lines.

/*
 * @test
 * @bug 8247630
 * @summary Use two key share entries
 * @run main/othervm ClientHelloKeyShares 29 23
 * @run main/othervm -Djdk.tls.namedGroups=secp384r1,secp521r1,x448,ffdhe2048 ClientHelloKeyShares 24 30
 * @run main/othervm -Djdk.tls.namedGroups=sect163k1,sect163r1,x25519 ClientHelloKeyShares 29
 * @run main/othervm -Djdk.tls.namedGroups=sect163k1,sect163r1,secp256r1 ClientHelloKeyShares 23
 * @run main/othervm -Djdk.tls.namedGroups=sect163k1,sect163r1,ffdhe2048,ffdhe3072,ffdhe4096 ClientHelloKeyShares 256
 * @run main/othervm -Djdk.tls.namedGroups=sect163k1,ffdhe2048,x25519,secp256r1 ClientHelloKeyShares 256 29
 * @run main/othervm -Djdk.tls.namedGroups=secp256r1,secp384r1,ffdhe2048,x25519 ClientHelloKeyShares 23 256
 */

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.nio.ByteBuffer;
import java.util.*;


public class ClientHelloKeyShares {

    // Some TLS constants we'll use for testing
    private static final int TLS_REC_HANDSHAKE = 22;
    private static final int HELLO_EXT_SUPP_GROUPS = 10;
    private static final int HELLO_EXT_SUPP_VERS = 43;
    private static final int HELLO_EXT_KEY_SHARE = 51;
    private static final int TLS_PROT_VER_13 = 0x0304;
    private static final int NG_SECP256R1 = 0x0017;
    private static final int NG_SECP384R1 = 0x0018;
    private static final int NG_X25519 = 0x001D;
    private static final int NG_X448 = 0x001E;

    public static void main(String args[]) throws Exception {
        // Arguments to this test are an abitrary number of integer
        // values which will be the expected NamedGroup IDs in the key_share
        // extension.  Expected named group assertions may also be affected
        // by setting the jdk.tls.namedGroups System property.
        List<Integer> expectedKeyShares = new ArrayList<>();
        Arrays.stream(args).forEach(arg ->
                expectedKeyShares.add(Integer.valueOf(arg)));

        SSLContext sslCtx = SSLContext.getDefault();
        SSLEngine engine = sslCtx.createSSLEngine();
        engine.setUseClientMode(true);
        SSLSession session = engine.getSession();
        ByteBuffer clientOut = ByteBuffer.wrap("I'm a Client".getBytes());
        ByteBuffer cTOs =
                ByteBuffer.allocateDirect(session.getPacketBufferSize());

        // Create and check the ClientHello message
        SSLEngineResult clientResult = engine.wrap(clientOut, cTOs);
        logResult("client wrap: ", clientResult);
        if (clientResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new RuntimeException("Client wrap got status: " +
                    clientResult.getStatus());
        }

        cTOs.flip();
        System.out.println(dumpHexBytes(cTOs));
        checkClientHello(cTOs, expectedKeyShares);
    }

    private static void logResult(String str, SSLEngineResult result) {
        HandshakeStatus hsStatus = result.getHandshakeStatus();
        System.out.println(str +
            result.getStatus() + "/" + hsStatus + ", " +
            result.bytesConsumed() + "/" + result.bytesProduced() +
            " bytes");
        if (hsStatus == HandshakeStatus.FINISHED) {
            System.out.println("\t...ready for application data");
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
     * Tests the ClientHello for the presence of the key shares in the supplied
     * List of key share identifiers.
     *
     * @param data the ByteBuffer containing the ClientHello bytes
     * @param keyShareTypes a List containing the expected key shares
     *
     * @throws RuntimeException if there is a deviation between what is expected
     * and what is supplied.  It will also throw this exception if other
     * basic structural elements of the ClientHello are not found (e.g. TLS 1.3
     * is not in the list of supported groups, etc.)
     */
    private static void checkClientHello(ByteBuffer data,
            List<Integer> expectedKeyShares) {
        Objects.requireNonNull(data);
        data.mark();

        // Process the TLS record header
        int type = Byte.toUnsignedInt(data.get());
        int ver_major = Byte.toUnsignedInt(data.get());
        int ver_minor = Byte.toUnsignedInt(data.get());
        int recLen = Short.toUnsignedInt(data.getShort());

        // Simple sanity checks
        if (type != 22) {
            throw new RuntimeException("Not a handshake: Type = " + type);
        } else if (recLen > data.remaining()) {
            throw new RuntimeException("Incomplete record in buffer: " +
                    "Record length = " + recLen + ", Remaining = " +
                    data.remaining());
        }

        // Grab the handshake message header.
        int msgHdr = data.getInt();
        int msgType = (msgHdr >> 24) & 0x000000FF;
        int msgLen = msgHdr & 0x00FFFFFF;

        // More simple sanity checks
        if (msgType != 1) {
            throw new RuntimeException("Not a ClientHello: Type = " + msgType);
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
        // for supported_versions (to make sure TLS 1.3 is asserted) and
        // the expected key shares are present.
        boolean foundSupVer = false;
        boolean foundKeyShare = false;
        int extsLen = Short.toUnsignedInt(data.getShort());
        List<Integer> supGrpList = new ArrayList<>();
        List<Integer> chKeyShares = new ArrayList<>();
        while (data.hasRemaining()) {
            int extType = Short.toUnsignedInt(data.getShort());
            int extLen = Short.toUnsignedInt(data.getShort());
            boolean foundTLS13 = false;
            switch (extType) {
                case HELLO_EXT_SUPP_GROUPS:
                    int supGrpLen = Short.toUnsignedInt(data.getShort());
                    for (int remain = supGrpLen; remain > 0; remain -= 2) {
                        supGrpList.add(Short.toUnsignedInt(data.getShort()));
                    }
                    break;
                case HELLO_EXT_SUPP_VERS:
                    foundSupVer = true;
                    int supVerLen = Byte.toUnsignedInt(data.get());
                    for (int remain = supVerLen; remain > 0; remain -= 2) {
                        foundTLS13 |= (Short.toUnsignedInt(data.getShort()) ==
                                TLS_PROT_VER_13);
                    }

                    if (!foundTLS13) {
                        throw new RuntimeException("Missing TLS 1.3 Protocol " +
                                "Version in supported_groups");
                    }
                    break;
                case HELLO_EXT_KEY_SHARE:
                    foundKeyShare = true;
                    int ksListLen = Short.toUnsignedInt(data.getShort());
                    while (ksListLen > 0) {
                        chKeyShares.add(Short.toUnsignedInt(data.getShort()));
                        int ksLen = Short.toUnsignedInt(data.getShort());
                        data.position(data.position() + ksLen);
                        ksListLen -= (4 + ksLen);
                    }
                    break;
                default:
                    data.position(data.position() + extLen);
                    break;
            }
        }

        // We must have parsed supported_versions, key_share and
        // supported_groups extensions.
        if ((foundSupVer && foundKeyShare && !supGrpList.isEmpty()) == false) {
            throw new RuntimeException("Missing one or more of key_share, " +
                    "supported_versions and/or supported_groups extensions");
        }

        // The key share types we expected in the test should match exactly what
        // was asserted in the client hello
        if (!expectedKeyShares.equals(chKeyShares)) {
            StringBuilder sb = new StringBuilder(
                    "Expected and Actual key_share lists differ: ");
            sb.append("Expected: ");
            expectedKeyShares.forEach(ng -> sb.append(ng).append(" "));
            sb.append(", Actual: ");
            chKeyShares.forEach(ng -> sb.append(ng).append(" "));
            throw new RuntimeException(sb.toString());
        }

        // The order of the key shares should match the order of precedence
        // of the same named groups asserted in the supported_groups extension.
        // (RFC 8446, 4.2.8)
        int prevChNg = -1;
        for (int ng : chKeyShares) {
            int chNgPos = supGrpList.indexOf(ng);
            if (chNgPos <= prevChNg) {
                throw new RuntimeException("Order of precedence violation " +
                        "for NamedGroup " + ng + " between key_share and " +
                        "supported_groups extensions");
            }
            prevChNg = chNgPos;
        }

        // We should be at the end of the ClientHello
        data.reset();
    }
}
