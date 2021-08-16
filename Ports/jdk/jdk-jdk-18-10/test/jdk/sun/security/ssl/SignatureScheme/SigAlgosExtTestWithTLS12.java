/*
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
 * @test 8263188
 * @summary If TLS the server and client has no common signature algorithms,
 *     the connection should fail fast with "No supported signature algorithm".
 *     This test only covers TLS 1.2.
 *
 * @library /test/lib
 *          /javax/net/ssl/templates
 *
 * @run main/othervm
 *     -Djdk.tls.server.SignatureSchemes=ecdsa_secp384r1_sha384
 *     -Djdk.tls.client.SignatureSchemes=ecdsa_secp256r1_sha256,ecdsa_secp384r1_sha384
 *     -Dtest.clientAuth=false
 *     -Dtest.expectFail=false
 *     SigAlgosExtTestWithTLS12
 * @run main/othervm
 *     -Djdk.tls.server.SignatureSchemes=ecdsa_secp384r1_sha384
 *     -Djdk.tls.client.SignatureSchemes=ecdsa_secp256r1_sha256
 *     -Dtest.clientAuth=false
 *     -Dtest.expectFail=true
 *     SigAlgosExtTestWithTLS12
 * @run main/othervm
 *     -Djdk.tls.server.SignatureSchemes=ecdsa_secp256r1_sha256
 *     -Djdk.tls.client.SignatureSchemes=ecdsa_secp256r1_sha256
 *     -Dtest.clientAuth=true
 *     -Dtest.expectFail=true
 *     SigAlgosExtTestWithTLS12
 */

import javax.net.ssl.*;
import java.nio.ByteBuffer;
import java.util.*;

public class SigAlgosExtTestWithTLS12 extends SSLEngineTemplate {

    private static final boolean CLIENT_AUTH
            = Boolean.getBoolean("test.clientAuth");
    private static final boolean EXPECT_FAIL
            = Boolean.getBoolean("test.expectFail");

    private static final String[] CA_CERTS = new String[] {
            // SHA256withECDSA, curve secp256r1
            // Validity
            //     Not Before: May 22 07:18:16 2018 GMT
            //     Not After : May 17 07:18:16 2038 GMT
            // Subject Key Identifier:
            //     60:CF:BD:73:FF:FA:1A:30:D2:A4:EC:D3:49:71:46:EF:1A:35:A0:86
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBvjCCAWOgAwIBAgIJAIvFG6GbTroCMAoGCCqGSM49BAMCMDsxCzAJBgNVBAYT\n" +
            "AlVTMQ0wCwYDVQQKDARKYXZhMR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZj\n" +
            "ZTAeFw0xODA1MjIwNzE4MTZaFw0zODA1MTcwNzE4MTZaMDsxCzAJBgNVBAYTAlVT\n" +
            "MQ0wCwYDVQQKDARKYXZhMR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZjZTBZ\n" +
            "MBMGByqGSM49AgEGCCqGSM49AwEHA0IABBz1WeVb6gM2mh85z3QlvaB/l11b5h0v\n" +
            "LIzmkC3DKlVukZT+ltH2Eq1oEkpXuf7QmbM0ibrUgtjsWH3mULfmcWmjUDBOMB0G\n" +
            "A1UdDgQWBBRgz71z//oaMNKk7NNJcUbvGjWghjAfBgNVHSMEGDAWgBRgz71z//oa\n" +
            "MNKk7NNJcUbvGjWghjAMBgNVHRMEBTADAQH/MAoGCCqGSM49BAMCA0kAMEYCIQCG\n" +
            "6wluh1r2/T6L31mZXRKf9JxeSf9pIzoLj+8xQeUChQIhAJ09wAi1kV8yePLh2FD9\n" +
            "2YEHlSQUAbwwqCDEVB5KxaqP\n" +
            "-----END CERTIFICATE-----",

            // SHA384withECDSA, curve secp384r1
            // Validity
            //     Not Before: Jun 24 08:15:06 2019 GMT
            //     Not After : Jun 19 08:15:06 2039 GMT
            // Subject Key Identifier:
            //     0a:93:a9:a0:bf:e7:d5:48:9d:4f:89:15:c6:51:98:80:05:51:4e:4e
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICCDCCAY6gAwIBAgIUCpOpoL/n1UidT4kVxlGYgAVRTk4wCgYIKoZIzj0EAwMw\n" +
            "OzELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpTU0Ug\n" +
            "VGVzdCBTZXJpdmNlMB4XDTE5MDYyNDA4MTUwNloXDTM5MDYxOTA4MTUwNlowOzEL\n" +
            "MAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpTU0UgVGVz\n" +
            "dCBTZXJpdmNlMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAENVQN1wXWFdgC6u/dDdiC\n" +
            "y+WtMTF66oL/0BSm+1ZqsogamzCryawOcHgiuXgWzx5CQ3LuOC+tDFyXpGfHuCvb\n" +
            "dkzxPrP5n9NrR8/uRPe5l1KOUbchviU8z9cTP+LZxnZDo1MwUTAdBgNVHQ4EFgQU\n" +
            "SktSFArR1p/5mXV0kyo0RxIVa/UwHwYDVR0jBBgwFoAUSktSFArR1p/5mXV0kyo0\n" +
            "RxIVa/UwDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAwNoADBlAjBZvoNmq3/v\n" +
            "RD2gBTyvxjS9h0rsMRLHDnvul/KWngytwGPTOBo0Y8ixQXSjdKoc3rkCMQDkiNgx\n" +
            "IDxuHedmrLQKIPnVcthTmwv7//jHiqGoKofwChMo2a1P+DQdhszmeHD/ARQ=\n" +
            "-----END CERTIFICATE-----"
    };

    private static final String[] EE_CERTS = new String[] {
            // SHA256withECDSA, curve secp256r1
            // Validity
            //     Not Before: May 22 07:18:16 2018 GMT
            //     Not After : May 17 07:18:16 2038 GMT
            // Authority Key Identifier:
            //     60:CF:BD:73:FF:FA:1A:30:D2:A4:EC:D3:49:71:46:EF:1A:35:A0:86
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBqjCCAVCgAwIBAgIJAPLY8qZjgNRAMAoGCCqGSM49BAMCMDsxCzAJBgNVBAYT\n" +
            "AlVTMQ0wCwYDVQQKDARKYXZhMR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZj\n" +
            "ZTAeFw0xODA1MjIwNzE4MTZaFw0zODA1MTcwNzE4MTZaMFUxCzAJBgNVBAYTAlVT\n" +
            "MQ0wCwYDVQQKDARKYXZhMR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZjZTEY\n" +
            "MBYGA1UEAwwPUmVncmVzc2lvbiBUZXN0MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcD\n" +
            "QgAEb+9n05qfXnfHUb0xtQJNS4JeSi6IjOfW5NqchvKnfJey9VkJzR7QHLuOESdf\n" +
            "xlR7q8YIWgih3iWLGfB+wxHiOqMjMCEwHwYDVR0jBBgwFoAUYM+9c//6GjDSpOzT\n" +
            "SXFG7xo1oIYwCgYIKoZIzj0EAwIDSAAwRQIgWpRegWXMheiD3qFdd8kMdrkLxRbq\n" +
            "1zj8nQMEwFTUjjQCIQDRIrAjZX+YXHN9b0SoWWLPUq0HmiFIi8RwMnO//wJIGQ==\n" +
            "-----END CERTIFICATE-----",

            // SHA384withECDSA, curve secp384r1
            // Validity
            //     Not Before: Jun 24 08:15:06 2019 GMT
            //     Not After : Jun 19 08:15:06 2039 GMT
            // Authority Key Identifier:
            //     40:2D:AA:EE:66:AA:33:27:AD:9B:5D:52:9B:60:67:6A:2B:AD:52:D2
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICEjCCAZegAwIBAgIUS3F0AqAXWRg07CnbknJzxofyBQMwCgYIKoZIzj0EAwMw\n" +
            "OzELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpTU0Ug\n" +
            "VGVzdCBTZXJpdmNlMB4XDTE5MDYyNDA4MTUwNloXDTM5MDYxOTA4MTUwNlowVTEL\n" +
            "MAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpTU0UgVGVz\n" +
            "dCBTZXJpdmNlMRgwFgYDVQQDDA9SZWdyZXNzaW9uIFRlc3QwdjAQBgcqhkjOPQIB\n" +
            "BgUrgQQAIgNiAARqElz8b6T07eyKomIinhztV3/3XBk9bKGtJ0W+JOltjuhMmP/w\n" +
            "G8ASSevpgqgpi6EzpBZaaJxE3zNfkNnxXOZmQi2Ypd1uK0zRdbEOKg0XOcTTZwEj\n" +
            "iLjYmt3O0pwpklijQjBAMB0GA1UdDgQWBBRALaruZqozJ62bXVKbYGdqK61S0jAf\n" +
            "BgNVHSMEGDAWgBRKS1IUCtHWn/mZdXSTKjRHEhVr9TAKBggqhkjOPQQDAwNpADBm\n" +
            "AjEArVDFKf48xijN6huVUJzKCOP0zlWB5Js+DItIkZmLQuhciPLhLIB/rChf3Y4C\n" +
            "xuP4AjEAmfLhQRI0O3pifpYzYSVh2G7/jHNG4eO+2dvgAcU+Lh2IIj/cpLaPFSvL\n" +
            "J8FXY9Nj\n" +
            "-----END CERTIFICATE-----"
    };

    private static final String[] EE_KEYS = new String[] {
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgn5K03bpTLjEtFQRa\n" +
            "JUtx22gtmGEvvSUSQdimhGthdtihRANCAARv72fTmp9ed8dRvTG1Ak1Lgl5KLoiM\n" +
            "59bk2pyG8qd8l7L1WQnNHtAcu44RJ1/GVHurxghaCKHeJYsZ8H7DEeI6",
            "MIG2AgEAMBAGByqGSM49AgEGBSuBBAAiBIGeMIGbAgEBBDASuI9EtK29APXPipkc\n" +
            "qDA+qwlewMjv/OcjUJ77kP1Vz62oVF9iY9SRIyFIUju8wt+hZANiAARqElz8b6T0\n" +
            "7eyKomIinhztV3/3XBk9bKGtJ0W+JOltjuhMmP/wG8ASSevpgqgpi6EzpBZaaJxE\n" +
            "3zNfkNnxXOZmQi2Ypd1uK0zRdbEOKg0XOcTTZwEjiLjYmt3O0pwpklg="
    };

    private static final String[] EE_ALGS = new String[] {
            "EC",
            "EC"
    };

    private static final String[] EE_ALIASES = new String[] {
            "EC-SHA256",
            "EC-SHA384"
    };

    private static final Map<Integer, String> SIG_SCHEMES_MAP = Map.of(
            0x0403, "ecdsa_secp256r1_sha256",
            0x0503, "ecdsa_secp384r1_sha384");

    private static final int TLS_HS_CLI_HELLO = 1;
    private static final int TLS_HS_CERT_REQ = 13;
    private static final int HELLO_EXT_SIG_ALGS = 13;

    public SigAlgosExtTestWithTLS12() throws Exception {
        super();
    }

    /*
     * Create an instance of KeyManager for client use.
     */
    public KeyManager createClientKeyManager() throws Exception {
        return SSLContextTemplate.createKeyManager(
                EE_CERTS,
                EE_KEYS,
                EE_ALGS,
                EE_ALIASES,
                getServerContextParameters());
    }

    @Override
    public TrustManager createClientTrustManager() throws Exception {
        return SSLContextTemplate.createTrustManager(
                CA_CERTS,
                getServerContextParameters());
    }

    @Override
    public KeyManager createServerKeyManager() throws Exception {
        return SSLContextTemplate.createKeyManager(
                EE_CERTS,
                EE_KEYS,
                EE_ALGS,
                EE_ALIASES,
                getServerContextParameters());
    }

    @Override
    public TrustManager createServerTrustManager() throws Exception {
        return SSLContextTemplate.createTrustManager(
                CA_CERTS,
                getServerContextParameters());
    }

    @Override
    protected SSLEngine configureServerEngine(SSLEngine serverEngine) {
        serverEngine.setUseClientMode(false);
        serverEngine.setNeedClientAuth(CLIENT_AUTH);
        return serverEngine;
    }

    @Override
    protected SSLEngine configureClientEngine(SSLEngine clientEngine) {
        clientEngine.setUseClientMode(true);
        clientEngine.setEnabledProtocols(new String[] { "TLSv1.2" });
        return clientEngine;
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("javax.net.debug", "ssl:handshake");

        try {
            new SigAlgosExtTestWithTLS12().run();
            if (EXPECT_FAIL) {
                throw new RuntimeException(
                        "Expected SSLHandshakeException wasn't thrown");
            }
        } catch (SSLHandshakeException e) {
            if (EXPECT_FAIL && e.getMessage().equals(
                    "No supported signature algorithm")) {
                System.out.println("Expected SSLHandshakeException");
            } else {
                throw e;
            }
        }
    }

    private void run() throws Exception {
        boolean dataDone = false;
        while (isOpen(clientEngine) || isOpen(serverEngine)) {
            clientEngine.wrap(clientOut, cTOs);
            cTOs.flip();

            // Consume the ClientHello and get the server flight of handshake
            // messages.  We expect that it will be one TLS record containing
            // multiple handshake messages, one of which is a CertificateRequest
            // when the client authentication is required.
            serverEngine.unwrap(cTOs, serverIn);
            runDelegatedTasks(serverEngine);

            // Wrap the server flight
            serverEngine.wrap(serverOut, sTOc);
            sTOc.flip();

            if (CLIENT_AUTH && EXPECT_FAIL) {
                twistCertReqMsg(sTOc);
            }

            clientEngine.unwrap(sTOc, clientIn);
            runDelegatedTasks(clientEngine);

            serverEngine.unwrap(cTOs, serverIn);
            runDelegatedTasks(serverEngine);

            cTOs.compact();
            sTOc.compact();

            if (!dataDone && (clientOut.limit() == serverIn.position()) &&
                    (serverOut.limit() == clientIn.position())) {
                checkTransfer(serverOut, clientIn);
                checkTransfer(clientOut, serverIn);

                clientEngine.closeOutbound();
                dataDone = true;
                serverEngine.closeOutbound();
            }
        }
    }

    /**
     * Twists signature schemes in CertificateRequest message for negative
     * client authentication cases.
     *
     * @param tlsRecord a ByteBuffer containing a TLS record.  It is assumed
     *      that the position of the ByteBuffer is on the first byte of the TLS
     *      record header.
     *
     * @throws SSLException if the incoming ByteBuffer does not contain a
     *      well-formed TLS message.
     */
    private static void twistCertReqMsg(
            ByteBuffer tlsRecord) throws SSLException {
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

            if (msgType == TLS_HS_CERT_REQ) {
                // Slice the buffer such that it contains the entire
                // handshake message (less the handshake header).
                int bufPos = tlsRecord.position();
                ByteBuffer buf = tlsRecord.slice(bufPos, msgLen);

                // Replace the signature scheme with an unknown value
                twistSigSchemesCertReq(buf, (short) 0x0000);
                byte[] bufBytes = new byte[buf.limit()];
                buf.get(bufBytes);
                tlsRecord.put(bufPos, bufBytes);

                break;
            } else {
                // Skip to the next handshake message, if there is one
                tlsRecord.position(tlsRecord.position() + msgLen);
            }
        }

        tlsRecord.reset();
    }

    /**
     * Replace the signature schemes in CertificateRequest message with an
     * alternative value.  It is assumed that the provided ByteBuffer has its
     * position set at the first byte of the CertificateRequest message body
     * (AFTER the handshake header) and contains the entire CR message.  Upon
     * successful completion of this method the ByteBuffer will have its
     * position reset to the initial offset in the buffer.
     * If an exception is thrown the position at the time of the exception
     * will be preserved.
     *
     * @param data the ByteBuffer containing the CertificateRequest bytes
     * @param altSigScheme an alternative signature scheme
     */
    private static void twistSigSchemesCertReq(ByteBuffer data,
                                               Short altSigScheme) {
        Objects.requireNonNull(data);
        data.mark();

        // Jump past the certificate types
        int certTypeLen = Byte.toUnsignedInt(data.get());
        if (certTypeLen != 0) {
            data.position(data.position() + certTypeLen);
        }

        int sigSchemeLen = Short.toUnsignedInt(data.getShort());
        for (int ssOff = 0; ssOff < sigSchemeLen; ssOff += 2) {
            System.err.println(
                    "Use alternative signature scheme: " + altSigScheme);
            data.putShort(data.position(), altSigScheme);
        }

        data.reset();
    }
}
