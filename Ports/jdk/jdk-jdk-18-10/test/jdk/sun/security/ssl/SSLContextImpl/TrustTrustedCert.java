/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7113275 8164846
 * @summary compatibility issue with MD2 trust anchor and old X509TrustManager
 * @library /javax/net/ssl/templates
 * @run main/othervm TrustTrustedCert PKIX TLSv1.1 true
 * @run main/othervm TrustTrustedCert PKIX TLSv1.1 false
 * @run main/othervm TrustTrustedCert SunX509 TLSv1.1 false
 * @run main/othervm TrustTrustedCert PKIX TLSv1.2 false
 * @run main/othervm TrustTrustedCert SunX509 TLSv1.2 false
 */

import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.*;
import java.security.interfaces.*;
import java.util.Base64;

public class TrustTrustedCert extends SSLSocketTemplate {

    /*
     * Certificates and key used in the test.
     */

    // It's a trust anchor signed with MD2 hash function.
    static String trustedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICkjCCAfugAwIBAgIBADANBgkqhkiG9w0BAQIFADA7MQswCQYDVQQGEwJVUzEN\n" +
        "MAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UwHhcN\n" +
        "MTExMTE4MTExNDA0WhcNMzIxMDI4MTExNDA0WjA7MQswCQYDVQQGEwJVUzENMAsG\n" +
        "A1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UwgZ8wDQYJ\n" +
        "KoZIhvcNAQEBBQADgY0AMIGJAoGBAPGyB9tugUGgxtdeqe0qJEwf9x1Gy4BOi1yR\n" +
        "wzDZY4H5LquvIfQ2V3J9X1MQENVsFvkvp65ZcFcy+ObOucXUUPFcd/iw2DVb5QXA\n" +
        "ffyeVqWD56GPi8Qe37wrJO3L6fBhN9oxp/BbdRLgjU81zx8qLEyPODhPMxV4OkcA\n" +
        "SDwZTSxxAgMBAAGjgaUwgaIwHQYDVR0OBBYEFLOAtr/YrYj9H04EDLA0fd14jisF\n" +
        "MGMGA1UdIwRcMFqAFLOAtr/YrYj9H04EDLA0fd14jisFoT+kPTA7MQswCQYDVQQG\n" +
        "EwJVUzENMAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2\n" +
        "Y2WCAQAwDwYDVR0TAQH/BAUwAwEB/zALBgNVHQ8EBAMCAQYwDQYJKoZIhvcNAQEC\n" +
        "BQADgYEAr8ExpXu/FTIRiMzPm0ubqwME4lniilwQUiEOD/4DbksNjEIcUyS2hIk1\n" +
        "qsmjJz3SHBnwhxl9dhJVwk2tZLkPGW86Zn0TPVRsttK4inTgCC9GFGeqQBdrU/uf\n" +
        "lipBzXWljrfbg4N/kK8m2LabtKUMMnGysM8rN0Fx2PYm5xxGvtM=\n" +
        "-----END CERTIFICATE-----";

    // The certificate issued by above trust anchor, signed with MD5
    static String targetCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICeDCCAeGgAwIBAgIBAjANBgkqhkiG9w0BAQQFADA7MQswCQYDVQQGEwJVUzEN\n" +
        "MAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UwHhcN\n" +
        "MTExMTE4MTExNDA2WhcNMzEwODA1MTExNDA2WjBPMQswCQYDVQQGEwJVUzENMAsG\n" +
        "A1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UxEjAQBgNV\n" +
        "BAMTCWxvY2FsaG9zdDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAwDnm96mw\n" +
        "fXCH4bgXk1US0VcJsQVxUtGMyncAveMuzBzNzOmKZPeqyYX1Fuh4q+cuza03WTJd\n" +
        "G9nOkNr364e3Rn1aaHjCMcBmFflObnGnhhufNmIGYogJ9dJPmhUVPEVAXrMG+Ces\n" +
        "NKy2E8woGnLMrqu6yiuTClbLBPK8fWzTXrECAwEAAaN4MHYwCwYDVR0PBAQDAgPo\n" +
        "MB0GA1UdDgQWBBSdRrpocLPJXyGfDmMWJrcEf29WGDAfBgNVHSMEGDAWgBSzgLa/\n" +
        "2K2I/R9OBAywNH3deI4rBTAnBgNVHSUEIDAeBggrBgEFBQcDAQYIKwYBBQUHAwIG\n" +
        "CCsGAQUFBwMDMA0GCSqGSIb3DQEBBAUAA4GBAKJ71ZiCUykkJrCLYUxlFlhvUcr9\n" +
        "sTcOc67QdroW5f412NI15SXWDiley/JOasIiuIFPjaJBjOKoHOvTjG/snVu9wEgq\n" +
        "YNR8dPsO+NM8r79C6jO+Jx5fYAC7os2XxS75h3NX0ElJcbwIXGBJ6xRrsFh/BGYH\n" +
        "yvudOlX4BkVR0l1K\n" +
        "-----END CERTIFICATE-----";

    // Private key in the format of PKCS#8.
    static String targetPrivateKey =
        "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMA55vepsH1wh+G4\n" +
        "F5NVEtFXCbEFcVLRjMp3AL3jLswczczpimT3qsmF9RboeKvnLs2tN1kyXRvZzpDa\n" +
        "9+uHt0Z9Wmh4wjHAZhX5Tm5xp4YbnzZiBmKICfXST5oVFTxFQF6zBvgnrDSsthPM\n" +
        "KBpyzK6rusorkwpWywTyvH1s016xAgMBAAECgYEAn9bF3oRkdDoBU0i/mcww5I+K\n" +
        "SH9tFt+WQbiojjz9ac49trkvUfu7MO1Jui2+QbrvaSkyj+HYGFOJd1wMsPXeB7ck\n" +
        "5mOIYV4uZK8jfNMSQ8v0tFEeIPp5lKdw1XnrQfSe+abo2eL5Lwso437Y4s3w37+H\n" +
        "aY3d76hR5qly+Ys+Ww0CQQDjeOoX89d/xhRqGXKjCx8ImE/dPmsI8O27cwtKrDYJ\n" +
        "6t0v/xryVIdvOYcRBvKnqEogOH7T1kI+LnWKUTJ2ehJ7AkEA2FVloPVqCehXcc7e\n" +
        "z3TDpU9w1B0JXklcV5HddYsRqp9RukN/VK4szKE7F1yoarIUtfE9Lr9082Jwyp3M\n" +
        "L11xwwJBAKsZ+Hur3x0tUY29No2Nf/pnFyvEF57SGwA0uPmiL8Ol9lpz+UDudDEl\n" +
        "hIM6Rqv12kwCMuQE9i7vo1o3WU3k5KECQEqhg1L49yD935TqiiFFpe0Ur9btQXse\n" +
        "kdXAA4d2d5zGI7q/aGD9SYU6phkUJSHR16VA2RuUfzMrpb+wmm1IrmMCQFtLoKRT\n" +
        "A5kokFb+E3Gplu29tJvCUpfwgBFRS+wmkvtiaU/tiyDcVgDO+An5DwedxxdVzqiE\n" +
        "njWHoKY3axDQ8OU=\n";

    static char passphrase[] = "passphrase".toCharArray();

    @Override
    protected SSLContext createServerSSLContext() throws Exception {
        return generateSSLContext();
    }

    @Override
    protected void configureServerSocket(SSLServerSocket socket) {
        socket.setNeedClientAuth(true);
    }

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        try {
            sslIS.read();
            sslOS.write('A');
            sslOS.flush();
        } catch (SSLException | SocketException se) {
            if (!expectFail) {
                throw se;
            }   // Otherwise, ignore.
        }
    }

    @Override
    protected SSLContext createClientSSLContext() throws Exception {
        return generateSSLContext();
    }

    @Override
    protected void runClientApplication(SSLSocket socket) throws Exception {
        // enable the specified TLS protocol
        socket.setEnabledProtocols(new String[] { tlsProtocol });

        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        try {
            sslOS.write('B');
            sslOS.flush();
            sslIS.read();
        } catch (SSLHandshakeException e) {
            if (expectFail) {
            // focus on the CertPathValidatorException
                Throwable t = e.getCause().getCause();
                if (t == null || !t.toString().contains("MD5withRSA")) {
                    throw new RuntimeException(
                        "Expected to see MD5withRSA in exception output", t);
                }
            } else {
                throw e;
            }
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */
    private static String tmAlgorithm;        // trust manager
    private static String tlsProtocol;        // trust manager
    // set this flag to test context of CertificateException
    private static boolean expectFail;

    private static void parseArguments(String[] args) {
        tmAlgorithm = args[0];
        tlsProtocol = args[1];
        expectFail = Boolean.parseBoolean(args[2]);
    }

    private static SSLContext generateSSLContext() throws Exception {

        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // create a key store
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // import the trused cert
        X509Certificate trusedCert = null;
        ByteArrayInputStream is =
                new ByteArrayInputStream(trustedCertStr.getBytes());
        trusedCert = (X509Certificate)cf.generateCertificate(is);
        is.close();

        ks.setCertificateEntry("Trusted RSA Signer", trusedCert);

        // generate the private key.
        PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                            Base64.getMimeDecoder().decode(targetPrivateKey));
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKey priKey =
                (RSAPrivateKey)kf.generatePrivate(priKeySpec);

        // generate certificate chain
        is = new ByteArrayInputStream(targetCertStr.getBytes());
        X509Certificate keyCert = (X509Certificate)cf.generateCertificate(is);
        is.close();

        X509Certificate[] chain = new X509Certificate[2];
        chain[0] = keyCert;
        chain[1] = trusedCert;

        // import the key entry and the chain
        ks.setKeyEntry("TheKey", priKey, passphrase, chain);

        // create SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmAlgorithm);
        tmf.init(ks);

        // create the customized KM and TM
        NoneExtendedX509TM myTM =
            new NoneExtendedX509TM(tmf.getTrustManagers()[0]);
        NoneExtendedX509KM myKM =
            new NoneExtendedX509KM("TheKey", chain, priKey);

        SSLContext ctx = SSLContext.getInstance(tlsProtocol);
        // KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
        // kmf.init(ks, passphrase);
        // ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        ctx.init(new KeyManager[]{myKM}, new TrustManager[]{myTM}, null);
        ks = null;

        return ctx;
    }

    static class NoneExtendedX509TM implements X509TrustManager {
        X509TrustManager tm;

        NoneExtendedX509TM(TrustManager tm) {
            this.tm = (X509TrustManager)tm;
        }

        public void checkClientTrusted(X509Certificate chain[], String authType)
                throws CertificateException {
            tm.checkClientTrusted(chain, authType);
        }

        public void checkServerTrusted(X509Certificate chain[], String authType)
                throws CertificateException {
            tm.checkServerTrusted(chain, authType);
        }

        public X509Certificate[] getAcceptedIssuers() {
            return tm.getAcceptedIssuers();
        }
    }

    static class NoneExtendedX509KM implements X509KeyManager {
        private String keyAlias;
        private X509Certificate[] chain;
        private PrivateKey privateKey;

        NoneExtendedX509KM(String keyAlias, X509Certificate[] chain,
                PrivateKey privateKey) {
            this.keyAlias = keyAlias;
            this.chain = chain;
            this.privateKey = privateKey;
        }

        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[] {keyAlias};
        }

        public String chooseClientAlias(String[] keyType, Principal[] issuers,
                Socket socket) {
            return keyAlias;
        }

        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return new String[] {keyAlias};
        }

        public String chooseServerAlias(String keyType, Principal[] issuers,
                Socket socket) {
            return keyAlias;
        }

        public X509Certificate[] getCertificateChain(String alias) {
            return chain;
        }

        public PrivateKey getPrivateKey(String alias) {
            return privateKey;
        }
    }

    public static void main(String[] args) throws Exception {
        /*
         * Get the customized arguments.
         */
        parseArguments(args);

        /*
         * MD5 is used in this test case, don't disable MD5 algorithm.
         * if expectFail is set, we're testing exception message
         */
        if (!expectFail) {
            Security.setProperty("jdk.certpath.disabledAlgorithms",
                "MD2, RSA keySize < 1024");
        }
        Security.setProperty("jdk.tls.disabledAlgorithms",
                "SSLv3, RC4, DH keySize < 768");

        /*
         * Start the tests.
         */
        new TrustTrustedCert().run();
    }
}
