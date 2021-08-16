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
 * @library /test/lib
 * @run main/othervm -Djdk.tls.namedGroups=x25519,secp256r1,secp384r1 HRRKeyShares
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jdk.test.lib.Utils;


public class HRRKeyShares {

    // Some TLS constants we'll use for testing
    private static final int TLS_REC_HANDSHAKE = 22;
    private static final int TLS_REC_ALERT = 21;
    private static final int HS_MSG_CLIHELLO = 1;
    private static final int HS_MSG_SERVHELLO = 2;          // Also for HRR
    private static final int HELLO_EXT_SUPP_GROUPS = 10;
    private static final int HELLO_EXT_SUPP_VERS = 43;
    private static final int HELLO_EXT_KEY_SHARE = 51;
    private static final int TLS_LEGACY_VER = 0x0303;       // TLSv1.2
    private static final int TLS_PROT_VER_13 = 0x0304;      // TLSv1.3
    private static final int NG_SECP256R1 = 0x0017;
    private static final int NG_SECP384R1 = 0x0018;
    private static final int NG_X25519 = 0x001D;
    private static final int NG_X448 = 0x001E;
    private static final int NG_GC512A = 0x0026;
    private static final int COMP_NONE = 0;
    private static final int ALERT_TYPE_FATAL = 2;
    private static final int ALERT_DESC_ILLEGAL_PARAM = 47;
    private static final byte[] HRR_RANDOM = Utils.toByteArray(
            "CF21AD74E59A6111BE1D8C021E65B891" +
            "C2A211167ABB8C5E079E09E2C8A8339C");

    static class ClientHello {
        // TLS Record header fields
        final int recType;
        final int recVers;
        final int recLength;

        // Handshake header fields
        final int hsMsgType;
        final int hsMsgLength;

        // ClientHello fields
        final int version;
        final byte[] random;
        final byte[] sessId;
        final List<Integer> cipherSuites = new ArrayList<>();
        final List<Integer> compressionList = new ArrayList<>();
        final Map<Integer,byte[]> extensionMap = new LinkedHashMap<>();

        // These are fields built from specific extension data fields we
        // are interested in for our tests
        final List<Integer> suppGroups = new ArrayList<>();
        final Map<Integer,byte[]> keyShares = new LinkedHashMap<>();
        final List<Integer> suppVersions = new ArrayList<>();

        ClientHello(ByteBuffer data) {
            Objects.requireNonNull(data);
            data.mark();

            // Process the TLS record header
            recType = Byte.toUnsignedInt(data.get());
            recVers = Short.toUnsignedInt(data.getShort());
            recLength = Short.toUnsignedInt(data.getShort());
            if (recType != TLS_REC_HANDSHAKE) {
                throw new RuntimeException("Not a Handshake TLS record. " +
                        "Type = " + recType);
            }

            // Process the Handshake message header
            int recHdr = data.getInt();
            hsMsgType = recHdr >>> 24;
            hsMsgLength = recHdr & 0x00FFFFFF;
            if (hsMsgType != HS_MSG_CLIHELLO) {
                throw new RuntimeException("Not a ClientHello message. " +
                        "Type = " + hsMsgType);
            } else if (hsMsgLength > data.remaining()) {
                throw new RuntimeException("Incomplete record in buffer: " +
                        "Record length = " + hsMsgLength + ", Remaining = " +
                        data.remaining());
            }

            version = Short.toUnsignedInt(data.getShort());
            random = new byte[32];
            data.get(random);
            sessId = new byte[Byte.toUnsignedInt(data.get())];
            data.get(sessId);

            int suiteLen = Short.toUnsignedInt(data.getShort());
            while (suiteLen > 0) {
                cipherSuites.add(Short.toUnsignedInt(data.getShort()));
                suiteLen -= 2;
            }

            int compLen = Byte.toUnsignedInt(data.get());
            while (compLen > 0) {
                compressionList.add(Byte.toUnsignedInt(data.get()));
                compLen--;
            }

            // Extension processing time!
            int extListLen = Short.toUnsignedInt(data.getShort());
            while (extListLen > 0) {
                int extType = Short.toUnsignedInt(data.getShort());
                int extLen = Short.toUnsignedInt(data.getShort());
                byte[] extData = new byte[extLen];
                data.get(extData);
                extensionMap.put(extType, extData);
                switch (extType) {
                    case HELLO_EXT_SUPP_GROUPS:
                        ByteBuffer sgBuf = ByteBuffer.wrap(extData);
                        int supGrpLen = Short.toUnsignedInt(sgBuf.getShort());
                        for (int remain = supGrpLen; remain > 0; remain -= 2) {
                            suppGroups.add(Short.toUnsignedInt(
                                    sgBuf.getShort()));
                        }
                        break;
                    case HELLO_EXT_SUPP_VERS:
                        ByteBuffer svBuf = ByteBuffer.wrap(extData);
                        int supVerLen = Byte.toUnsignedInt(svBuf.get());
                        for (int remain = supVerLen; remain > 0; remain -= 2) {
                            suppVersions.add(Short.toUnsignedInt(
                                    svBuf.getShort()));
                        }
                        break;
                    case HELLO_EXT_KEY_SHARE:
                        ByteBuffer ksBuf = ByteBuffer.wrap(extData);
                        int ksListLen = Short.toUnsignedInt(ksBuf.getShort());
                        while (ksListLen > 0) {
                            int namedGroup = Short.toUnsignedInt(
                                    ksBuf.getShort());
                            int ksLen = Short.toUnsignedInt(ksBuf.getShort());
                            byte[] ksData = new byte[ksLen];
                            ksBuf.get(ksData);
                            keyShares.put(namedGroup, ksData);
                            ksListLen -= (4 + ksLen);
                        }
                        break;
                }
                extListLen -= (4 + extLen);
            }
        }
    }

    static class Alert {
        final int recType;
        final int recVers;
        final int recLength;
        final int alertType;
        final int alertDesc;

        Alert(ByteBuffer data) {
            Objects.requireNonNull(data);
            data.mark();

            // Process the TLS record header
            recType = Byte.toUnsignedInt(data.get());
            recVers = Short.toUnsignedInt(data.getShort());
            recLength = Short.toUnsignedInt(data.getShort());
            if (recType != TLS_REC_ALERT) {
                throw new RuntimeException("Not a Handshake TLS record. " +
                        "Type = " + recType);
            }

            alertType = Byte.toUnsignedInt(data.get());
            alertDesc = Byte.toUnsignedInt(data.get());
        }
    }

    public static void main(String args[]) throws Exception {
        System.out.println("Test 1: Good HRR exchange using secp384r1");
        hrrKeyShareTest(NG_SECP384R1, true);
        System.out.println();

        System.out.println("Test 2: Bad HRR exchange using secp256r1");
        hrrKeyShareTest(NG_SECP256R1, false);
        System.out.println();

        System.out.println("Test 3: Bad HRR using unknown GC512A");
        hrrKeyShareTest(NG_GC512A, false);
        System.out.println();

        System.out.println("Test 4: Bad HRR using known / unasserted x448");
        hrrKeyShareTest(NG_X448, false);
        System.out.println();
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

    /*
     * If the result indicates that we have outstanding tasks to do,
     * go ahead and run them in this thread.
     */
    private static void runDelegatedTasks(SSLEngine engine) throws Exception {
        if (engine.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                System.out.println("    running delegated task...");
                runnable.run();
            }
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
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

    private static void hrrKeyShareTest(int hrrNamedGroup, boolean expectedPass)
            throws Exception {
        SSLContext sslCtx = SSLContext.getDefault();
        SSLEngine engine = sslCtx.createSSLEngine();
        engine.setUseClientMode(true);
        SSLSession session = engine.getSession();
        ByteBuffer clientOut =
                ByteBuffer.wrap("I'm a Client".getBytes());
        ByteBuffer cTOs = ByteBuffer.allocateDirect(
                session.getPacketBufferSize());

        // Create and check the ClientHello message
        SSLEngineResult clientResult = engine.wrap(clientOut, cTOs);
        logResult("client wrap: ", clientResult);
        if (clientResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new RuntimeException("Client wrap got status: " +
                    clientResult.getStatus());
        }

        cTOs.flip();
        System.out.println("----- ORIGINAL CLIENT HELLO -----\n" +
                dumpHexBytes(cTOs));
        ClientHello initialCh = new ClientHello(cTOs);

        if (!initialCh.suppVersions.contains(TLS_PROT_VER_13)) {
            throw new RuntimeException(
                    "Missing TLSv1.3 protocol in supported_versions");
        } else if (!initialCh.keyShares.containsKey(NG_X25519) ||
                !initialCh.keyShares.containsKey(NG_SECP256R1)) {
            throw new RuntimeException(
                    "Missing one or more expected KeyShares");
        }

        // Craft the HRR message with the passed-in named group as the
        // key share named group to request.
        ByteBuffer sTOc = buildHRRMessage(initialCh, hrrNamedGroup);
        System.out.println("----- SERVER HELLO RETRY REQUEST -----\n" +
                dumpHexBytes(sTOc));

        // Unwrap the HRR and process it
        clientResult = engine.unwrap(sTOc, clientOut);
        logResult("client unwrap: ", clientResult);
        if (clientResult.getStatus() != SSLEngineResult.Status.OK) {
            throw new RuntimeException("Client wrap got status: " +
                    clientResult.getStatus());
        }
        runDelegatedTasks(engine);

        try {
            // Now we're expecting to reissue the ClientHello, this time
            // with a secp384r1 share.
            cTOs.compact();
            clientResult = engine.wrap(clientOut, cTOs);
            logResult("client wrap: ", clientResult);
            if (clientResult.getStatus() != SSLEngineResult.Status.OK) {
                throw new RuntimeException("Client wrap got status: " +
                        clientResult.getStatus());
            }
        } catch (RuntimeException | SSLException ssle) {
            if (expectedPass) {
                System.out.println("Caught unexpected exception");
                throw ssle;
            } else {
                System.out.println("Caught expected exception: " + ssle);

                // Try issuing another wrap call and see if we can get
                // the Alert out.
                clientResult = engine.wrap(clientOut, cTOs);
                logResult("client wrap: ", clientResult);
                if (clientResult.getStatus() != SSLEngineResult.Status.CLOSED) {
                    throw new RuntimeException("Client wrap got status: " +
                            clientResult.getStatus());
                }

                cTOs.flip();
                System.out.println("----- ALERT -----\n" + dumpHexBytes(cTOs));
                Alert alert = new Alert(cTOs);
                if (alert.alertType != ALERT_TYPE_FATAL ||
                        alert.alertDesc != ALERT_DESC_ILLEGAL_PARAM) {
                    throw new RuntimeException("Unexpected alert.  " +
                            "received " + alert.alertType + " / " +
                            alert.alertDesc);
                }
                return;
            }
        }

        cTOs.flip();
        System.out.println("----- REISSUED CLIENT HELLO -----\n" +
                dumpHexBytes(cTOs));
        ClientHello reissuedCh = new ClientHello(cTOs);

        if (!reissuedCh.keyShares.containsKey(hrrNamedGroup)) {
            throw new RuntimeException("Missing secp384r1 key share");
        }
    }

    private static ByteBuffer buildHRRMessage(ClientHello cliHello,
            int namedGroup) throws IOException {
        // Create a ByteBuffer that will be large enough to handle
        // the HelloRetryRequest
        ByteBuffer hrrBuf = ByteBuffer.allocate(2048);  // More than enough!

        // Advance past the TLS record and handshake message headers.  We will
        // go back later and scribble in the proper lengths.  The record header
        // is 5 bytes long, the handshake header is 4.
        hrrBuf.position(9);
        hrrBuf.putShort((short)TLS_LEGACY_VER);
        hrrBuf.put(HRR_RANDOM);
        hrrBuf.put((byte)cliHello.sessId.length);
        hrrBuf.put(cliHello.sessId);
        hrrBuf.putShort(cliHello.cipherSuites.get(0).shortValue());
        hrrBuf.put((byte)COMP_NONE);

        // Use a separate stream for creating the extension section
        ByteArrayOutputStream extBaos = new ByteArrayOutputStream();
        DataOutputStream extStream = new DataOutputStream(extBaos);

        // Supported version
        extStream.writeShort(HELLO_EXT_SUPP_VERS);
        extStream.writeShort(2);
        extStream.writeShort(TLS_PROT_VER_13);

        // Key share
        extStream.writeShort(HELLO_EXT_KEY_SHARE);
        extStream.writeShort(2);
        extStream.writeShort(namedGroup);

        // Now add in the extensions into the main message
        hrrBuf.putShort((short)extStream.size());
        hrrBuf.put(extBaos.toByteArray());

        // At this point we can go back and write in the TLS record and
        // handshake message headers.
        hrrBuf.flip();

        // Write in the TLS record header
        hrrBuf.put((byte)TLS_REC_HANDSHAKE);
        hrrBuf.putShort((short)TLS_LEGACY_VER);
        hrrBuf.putShort((short)(hrrBuf.limit() - 5));

        // Write the Handshake message header
        hrrBuf.putInt((HS_MSG_SERVHELLO << 24) |
                ((hrrBuf.limit() - 9) & 0x00FFFFFF));

        hrrBuf.rewind();
        return hrrBuf;
    }
}
