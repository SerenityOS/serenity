/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6956398
 * @summary make ephemeral DH key match the length of the certificate key
 * @run main/othervm -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing TLS_DHE_RSA_WITH_AES_128_CBC_SHA  false 1643 267
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA true 1259 75
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.ephemeralDHKeySize=matched
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA true 1259 75
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.ephemeralDHKeySize=legacy
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA true 1259 75
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.ephemeralDHKeySize=1024
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA true 1259 75
 *
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA true 233 75
 *
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing TLS_DHE_RSA_WITH_AES_128_CBC_SHA  false 1387 139
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.ephemeralDHKeySize=legacy
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing TLS_DHE_RSA_WITH_AES_128_CBC_SHA  false 1323 107
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.ephemeralDHKeySize=matched
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing TLS_DHE_RSA_WITH_AES_128_CBC_SHA  false 1643 267
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.ephemeralDHKeySize=1024
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing TLS_DHE_RSA_WITH_AES_128_CBC_SHA  false 1387 139
 *
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      DHEKeySizing SSL_DH_anon_WITH_RC4_128_MD5  false 361 139
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      -Djdk.tls.ephemeralDHKeySize=legacy
 *      DHEKeySizing SSL_DH_anon_WITH_RC4_128_MD5  false 297 107
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      -Djdk.tls.ephemeralDHKeySize=matched
 *      DHEKeySizing SSL_DH_anon_WITH_RC4_128_MD5  false 361 139
 * @run main/othervm -Djsse.enableFFDHE=false
 *      -Djdk.tls.client.enableSessionTicketExtension=false
 *      -Djdk.tls.ephemeralDHKeySize=1024
 *      DHEKeySizing SSL_DH_anon_WITH_RC4_128_MD5  false 361 139
 */

/*
 * This is a simple hack to test key sizes of Diffie-Hellman key exchanging
 * during SSL/TLS handshaking.
 *
 * The record length of DH ServerKeyExchange and ClientKeyExchange.
 * ServerKeyExchange message are wrapped in ServerHello series messages, which
 * contains ServerHello, Certificate and ServerKeyExchange message.
 *
 *    struct {
 *        opaque dh_p<1..2^16-1>;
 *        opaque dh_g<1..2^16-1>;
 *        opaque dh_Ys<1..2^16-1>;
 *    } ServerDHParams;     // Ephemeral DH parameters
 *
 *    struct {
 *        select (PublicValueEncoding) {
 *            case implicit: struct { };
 *            case explicit: opaque dh_Yc<1..2^16-1>;
 *        } dh_public;
 *    } ClientDiffieHellmanPublic;
 *
 * Fomr above structures, it is clear that if the DH key size increasing 128
 * bits (16 bytes), the ServerHello series messages increases 48 bytes
 * (becuase dh_p, dh_g and dh_Ys each increase 16 bytes) and ClientKeyExchange
 * increases 16 bytes (because of the size increasing of dh_Yc).
 *
 * Here is a summary of the record length in the test case.
 *
 *            |  ServerHello Series  |  ClientKeyExchange | ServerHello Anon
 *   512-bit  |          1259 bytes  |           75 bytes |        233 bytes
 *   768-bit  |          1323 bytes  |          107 bytes |        297 bytes
 *  1024-bit  |          1387 bytes  |          139 bytes |        361 bytes
 *  2048-bit  |          1643 bytes  |          267 bytes |        361 bytes
 */

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.nio.*;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.interfaces.*;
import java.util.Base64;

public class DHEKeySizing {

    private final static boolean debug = true;

    // key length bias because of the stripping of leading zero bytes of
    // negotiated DH keys.
    //
    // This is an effort to mimum intermittent failure when we cannot
    // estimate what's the exact number of leading zero bytes of
    // negotiated DH keys.
    private final static int KEY_LEN_BIAS = 6;

    private SSLContext sslc;
    private SSLEngine ssle1;    // client
    private SSLEngine ssle2;    // server

    private ByteBuffer appOut1;         // write side of ssle1
    private ByteBuffer appIn1;          // read side of ssle1
    private ByteBuffer appOut2;         // write side of ssle2
    private ByteBuffer appIn2;          // read side of ssle2

    private ByteBuffer oneToTwo;        // "reliable" transport ssle1->ssle2
    private ByteBuffer twoToOne;        // "reliable" transport ssle2->ssle1

    /*
     * Where do we find the keystores?
     */
    // Certificates and key used in the test.
    static String trustedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC8jCCAdqgAwIBAgIEUjkuRzANBgkqhkiG9w0BAQUFADA7MR0wGwYDVQQLExRT\n" +
        "dW5KU1NFIFRlc3QgU2VyaXZjZTENMAsGA1UEChMESmF2YTELMAkGA1UEBhMCVVMw\n" +
        "HhcNMTMwOTE4MDQzODMxWhcNMTMxMjE3MDQzODMxWjA7MR0wGwYDVQQLExRTdW5K\n" +
        "U1NFIFRlc3QgU2VyaXZjZTENMAsGA1UEChMESmF2YTELMAkGA1UEBhMCVVMwggEi\n" +
        "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCO+IGeaskJAvEcYc7pCl9neK3E\n" +
        "a28fwWLtChufYNaC9hQfZlUdETWYjV7fZJVJKT/oLzdDNMWuVA0LKXArpI3thLNK\n" +
        "QLXisdF9hKPlZRDazACL9kWUUtJ0FzpEySK4e8wW/z9FuU6e6iO19FbjxAfInJqk\n" +
        "3EDiEhB5g73S2vtvPCxgq2DvWw9TDl/LIqdKG2JCS93koXCCaHmQ7MrIOqHPd+8r\n" +
        "RbGpatXT9qyHKppUv9ATxVygO4rA794mgCFxpT+fkhz+NEB0twTkM65T1hnnOv5n\n" +
        "ZIxkcjBggt85UlZtnP3b9P7SYxsWIa46Oc38Od2f3YejfVg6B+PqPgWNl3+/AgMB\n" +
        "AAEwDQYJKoZIhvcNAQEFBQADggEBAAlrP6DFLRPSy0IgQhcI2i56tR/na8pezSte\n" +
        "ZHcCdaCZPDy4UP8mpLJ9QCjEB5VJv8hPm4xdK7ULnKGOGHgYqDpV2ZHvQlhV1woQ\n" +
        "TZGb/LM3c6kAs0j4j9KM2fq3iYUYexjIkS1KzsziflxMM6igS9BRMBR2LQyU+cYq\n" +
        "YEsFzkF7Aj2ET4v/+tgot9mRr2NioJcaJkdsPDpMU3IKB1cczfu+OuLQ/GCG0Fqu\n" +
        "6ijCeCqfnaAbemHbJeVZZ6Qgka3uC2YMntLBmLkhqEo1d9zGYLoh7oWL77y5ibQZ\n" +
        "LK5/H/zikcu579TWjlDHcqL3arCwBcrtsjSaPrRSWMrWV/6c0qw=\n" +
        "-----END CERTIFICATE-----";

    // Private key in the format of PKCS#8
    static String targetPrivateKey =
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCO+IGeaskJAvEc\n" +
        "Yc7pCl9neK3Ea28fwWLtChufYNaC9hQfZlUdETWYjV7fZJVJKT/oLzdDNMWuVA0L\n" +
        "KXArpI3thLNKQLXisdF9hKPlZRDazACL9kWUUtJ0FzpEySK4e8wW/z9FuU6e6iO1\n" +
        "9FbjxAfInJqk3EDiEhB5g73S2vtvPCxgq2DvWw9TDl/LIqdKG2JCS93koXCCaHmQ\n" +
        "7MrIOqHPd+8rRbGpatXT9qyHKppUv9ATxVygO4rA794mgCFxpT+fkhz+NEB0twTk\n" +
        "M65T1hnnOv5nZIxkcjBggt85UlZtnP3b9P7SYxsWIa46Oc38Od2f3YejfVg6B+Pq\n" +
        "PgWNl3+/AgMBAAECggEAPdb5Ycc4m4A9QBSCRcRpzbyiFLKPh0HDg1n65q4hOtYr\n" +
        "kAVYTVFTSF/lqGS+Ob3w2YIKujQKSUQrvCc5UHdFuHXMgxKIWbymK0+DAMb9SlYw\n" +
        "6lkkcWp9gx9E4dnJ/df2SAAxovvrKMuHlL1SFASHhVtPfH2URvSfUaANLDXxyYOs\n" +
        "8BX0Nr6wazhWjLjXo9yIGnKSvFfB8XisYcA78kEgas43zhmIGCDPqaYyyffOfRbx\n" +
        "pM1KNwGmlN86iWR1CbwA/wwhcMySWQueS+s7cHbpRqZIYJF9jEeELiwi0vxjealS\n" +
        "EMuHYedIRFMWaDIq9XyjrvXamHb0Z25jlXBNZHaM0QKBgQDE9adl+zAezR/n79vw\n" +
        "0XiX2Fx1UEo3ApZHuoA2Q/PcBk+rlKqqQ3IwTcy6Wo648wK7v6Nq7w5nEWcsf0dU\n" +
        "QA2Ng/AJEev/IfF34x7sKGYxtk1gcE0EuSBA3R+ocEZxnNw1Ryd5nUU24s8d4jCP\n" +
        "Mkothnyaim+zE2raDlEtVc0CaQKBgQC509av+02Uq5oMjzbQp5PBJfQFjATOQT15\n" +
        "eefYnVYurkQ1kcVfixkrO2ORhg4SjmI2Z5hJDgGtXdwgidpzkad+R2epS5qLMyno\n" +
        "lQVpY6bMpEZ7Mos0yQygxnm8uNohEcTExOe+nP5fNJVpzBsGmfeyYOhnPQlf6oqf\n" +
        "0cHizedb5wKBgQC/l5LyMil6HOGHlhzmIm3jj7VI7QR0hJC5T6N+phVml8ESUDjA\n" +
        "DYHbmSKouISTRtkG14FY+RiSjCxH7bvuKazFV2289PETquogTA/9e8MFYqfcQwG4\n" +
        "sXi9gBxWlnj/9a2EKiYtOB5nKLR/BlNkSHA93tAA6N+FXEMZwMmYhxk42QKBgAuY\n" +
        "HQgD3PZOsqDf+qKQIhbmAFCsSMx5o5VFtuJ8BpmJA/Z3ruHkMuDQpsi4nX4o5hXQ\n" +
        "5t6AAjjH52kcUMXvK40kdWJJtk3DFnVNfvXxYsHX6hHbuHXFqYUKfSP6QJnZmvZP\n" +
        "9smcz/4usLfWJUWHK740b6upUkFqx9Vq5/b3s9y3AoGAdM5TW7LkkOFsdMGVAUzR\n" +
        "9iXmCWElHTK2Pcp/3yqDBHSfiQx6Yp5ANyPnE9NBM0yauCfOyBB2oxLO4Rdv3Rqk\n" +
        "9V9kyR/YAGr7dJaPcQ7pZX0OpkzgueAOJYPrx5VUzPYUtklYV1ycFZTfKlpFCxT+\n" +
        "Ei6KUo0NXSdUIcB4yib1J10=";

    static char passphrase[] = "passphrase".toCharArray();

    /*
     * Majority of the test case is here, setup is done below.
     */

    private void createSSLEngines() throws Exception {
        ssle1 = sslc.createSSLEngine("client", 1);
        ssle1.setUseClientMode(true);

        ssle2 = sslc.createSSLEngine("server", 2);
        ssle2.setUseClientMode(false);
    }

    private boolean isHandshaking(SSLEngine e) {
        return (e.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING);
    }

    private void checkResult(ByteBuffer bbIn, ByteBuffer bbOut,
            SSLEngineResult result,
            Status status, HandshakeStatus hsStatus,
            int consumed, int produced)
            throws Exception {

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

        if ((consumed != -1) && (bbIn.position() != result.bytesConsumed())) {
            throw new Exception("Consumed " + bbIn.position() +
                " != " + consumed);
        }

        if ((produced != -1) && (bbOut.position() != result.bytesProduced())) {
            throw new Exception("produced " + bbOut.position() +
                " != " + produced);
        }
    }

    private void test(String cipherSuite, boolean exportable,
            int lenServerKeyEx, int lenClientKeyEx) throws Exception {

        createSSLEngines();
        createBuffers();

        SSLEngineResult result1;        // ssle1's results from last operation
        SSLEngineResult result2;        // ssle2's results from last operation

        String[] suites = new String [] {cipherSuite};

        ssle1.setEnabledCipherSuites(suites);
        ssle2.setEnabledCipherSuites(suites);

        log("======================================");
        log("===================");
        log("client hello");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
            Status.OK, HandshakeStatus.NEED_UNWRAP, 0, -1);
        oneToTwo.flip();

        result2 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(oneToTwo, appIn2, result2,
            Status.OK, HandshakeStatus.NEED_TASK, result1.bytesProduced(), 0);
        runDelegatedTasks(ssle2);
        oneToTwo.compact();

        log("===================");
        log("ServerHello");
        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.NEED_UNWRAP, 0, -1);
        twoToOne.flip();

        log("Message length of ServerHello series: " + twoToOne.remaining());
        if (twoToOne.remaining() < (lenServerKeyEx - KEY_LEN_BIAS) ||
                twoToOne.remaining() > lenServerKeyEx) {
            throw new Exception(
                "Expected to generate ServerHello series messages of " +
                lenServerKeyEx + " bytes, but not " + twoToOne.remaining());
        }

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.NEED_TASK, result2.bytesProduced(), 0);
        runDelegatedTasks(ssle1);
        twoToOne.compact();

        log("===================");
        log("Key Exchange");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
            Status.OK, HandshakeStatus.NEED_WRAP, 0, -1);
        oneToTwo.flip();

        log("Message length of ClientKeyExchange: " + oneToTwo.remaining());
        if (oneToTwo.remaining() < (lenClientKeyEx - KEY_LEN_BIAS) ||
                oneToTwo.remaining() > lenClientKeyEx) {
            throw new Exception(
                "Expected to generate ClientKeyExchange message of " +
                lenClientKeyEx + " bytes, but not " + oneToTwo.remaining());
        }
        result2 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(oneToTwo, appIn2, result2,
            Status.OK, HandshakeStatus.NEED_TASK, result1.bytesProduced(), 0);
        runDelegatedTasks(ssle2);
        oneToTwo.compact();

        log("===================");
        log("Client CCS");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
            Status.OK, HandshakeStatus.NEED_WRAP, 0, -1);
        oneToTwo.flip();

        result2 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(oneToTwo, appIn2, result2,
            Status.OK, HandshakeStatus.NEED_UNWRAP,
            result1.bytesProduced(), 0);
        oneToTwo.compact();

        log("===================");
        log("Client Finished");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
            Status.OK, HandshakeStatus.NEED_UNWRAP, 0, -1);
        oneToTwo.flip();

        result2 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(oneToTwo, appIn2, result2,
            Status.OK, HandshakeStatus.NEED_WRAP,
            result1.bytesProduced(), 0);
        oneToTwo.compact();

        log("===================");
        log("Server CCS");
        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.NEED_WRAP, 0, -1);
        twoToOne.flip();

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.NEED_UNWRAP, result2.bytesProduced(), 0);
        twoToOne.compact();

        log("===================");
        log("Server Finished");
        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.FINISHED, 0, -1);
        twoToOne.flip();

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.FINISHED, result2.bytesProduced(), 0);
        twoToOne.compact();

        log("===================");
        log("Check Session/Ciphers");
        String cs = ssle1.getSession().getCipherSuite();
        if (!cs.equals(suites[0])) {
            throw new Exception("suites not equal: " + cs + "/" + suites[0]);
        }

        cs = ssle2.getSession().getCipherSuite();
        if (!cs.equals(suites[0])) {
            throw new Exception("suites not equal: " + cs + "/" + suites[0]);
        }

        log("===================");
        log("Done with SSL/TLS handshaking");
    }

    public static void main(String args[]) throws Exception {
        // reset security properties to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");

        if (args.length != 4) {
            System.out.println(
                "Usage: java DHEKeySizing cipher-suite " +
                "exportable(true|false)\n" +
                "    size-of-server-hello-record size-of-client-key-exchange");
            throw new Exception("Incorrect usage!");
        }

        (new DHEKeySizing()).test(args[0],
                Boolean.parseBoolean(args[1]),
                Integer.parseInt(args[2]),
                Integer.parseInt(args[3]));
        System.out.println("Test Passed.");
    }

    /*
     * **********************************************************
     * Majority of the test case is above, below is just setup stuff
     * **********************************************************
     */

    public DHEKeySizing() throws Exception {
        sslc = getSSLContext();
    }

    /*
     * Create an initialized SSLContext to use for this test.
     */
    private SSLContext getSSLContext() throws Exception {

        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // create a key store
        KeyStore ts = KeyStore.getInstance("JKS");
        KeyStore ks = KeyStore.getInstance("JKS");
        ts.load(null, null);
        ks.load(null, null);

        // import the trused cert
        ByteArrayInputStream is =
                    new ByteArrayInputStream(trustedCertStr.getBytes());
        Certificate trusedCert = cf.generateCertificate(is);
        is.close();
        ts.setCertificateEntry("rsa-trusted-2048", trusedCert);

        // generate the private key.
        String keySpecStr = targetPrivateKey;
        PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                            Base64.getMimeDecoder().decode(keySpecStr));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKey priKey = (RSAPrivateKey)kf.generatePrivate(priKeySpec);

        Certificate[] chain = new Certificate[1];
        chain[0] = trusedCert;

        // import the key entry.
        ks.setKeyEntry("rsa-key-2048", priKey, passphrase, chain);

        // create SSL context
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("TLSv1");
        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslCtx;
    }

    private void createBuffers() {
        // Size the buffers as appropriate.

        SSLSession session = ssle1.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();

        appIn1 = ByteBuffer.allocateDirect(appBufferMax + 50);
        appIn2 = ByteBuffer.allocateDirect(appBufferMax + 50);

        oneToTwo = ByteBuffer.allocateDirect(netBufferMax);
        twoToOne = ByteBuffer.allocateDirect(netBufferMax);

        appOut1 = ByteBuffer.wrap("Hi Engine2, I'm SSLEngine1".getBytes());
        appOut2 = ByteBuffer.wrap("Hello Engine1, I'm SSLEngine2".getBytes());

        log("AppOut1 = " + appOut1);
        log("AppOut2 = " + appOut2);
        log("");
    }

    private static void runDelegatedTasks(SSLEngine engine) throws Exception {

        Runnable runnable;
        while ((runnable = engine.getDelegatedTask()) != null) {
            log("running delegated task...");
            runnable.run();
        }
    }

    private static void log(String str) {
        if (debug) {
            System.out.println(str);
        }
    }
}
