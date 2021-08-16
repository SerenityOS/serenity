/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049321
 * @summary Support SHA256WithDSA in JSSE
 * @run main/othervm SignatureAlgorithms PKIX "SHA-224,SHA-256"
 *                   TLS_DHE_DSS_WITH_AES_128_CBC_SHA
 * @run main/othervm SignatureAlgorithms PKIX "SHA-1,SHA-224"
 *                   TLS_DHE_DSS_WITH_AES_128_CBC_SHA
 * @run main/othervm SignatureAlgorithms PKIX "SHA-1,SHA-256"
 *                   TLS_DHE_DSS_WITH_AES_128_CBC_SHA
 * @run main/othervm SignatureAlgorithms PKIX "SHA-224,SHA-256"
 *                   TLS_DHE_DSS_WITH_AES_128_CBC_SHA256
 * @run main/othervm SignatureAlgorithms PKIX "SHA-1,SHA-224"
 *                   TLS_DHE_DSS_WITH_AES_128_CBC_SHA256
 * @run main/othervm SignatureAlgorithms PKIX "SHA-1,SHA-256"
 *                   TLS_DHE_DSS_WITH_AES_128_CBC_SHA256
 */

import java.net.*;
import java.util.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.Security;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.*;
import java.security.interfaces.*;

public class SignatureAlgorithms {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    /*
     * Should we run the client or server in a separate thread?
     * Both sides can throw exceptions, but do you have a preference
     * as to which side should be the main thread.
     */
    static boolean separateServerThread = true;

    /*
     * Where do we find the keystores?
     */
    // Certificates and key (DSA) used in the test.
    static String trustedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDYTCCAyGgAwIBAgIJAK8/gw6zg/DPMAkGByqGSM44BAMwOzELMAkGA1UEBhMC\n" +
        "VVMxDTALBgNVBAoTBEphdmExHTAbBgNVBAsTFFN1bkpTU0UgVGVzdCBTZXJpdmNl\n" +
        "MB4XDTE1MTIwMzEzNTIyNVoXDTM2MTExMjEzNTIyNVowOzELMAkGA1UEBhMCVVMx\n" +
        "DTALBgNVBAoTBEphdmExHTAbBgNVBAsTFFN1bkpTU0UgVGVzdCBTZXJpdmNlMIIB\n" +
        "uDCCASwGByqGSM44BAEwggEfAoGBAPH+b+GSMX6KS7jXDRevzc464DFG4X+uxu5V\n" +
        "b3U4yhsU8A8cuH4gwin6L/IDkmZQ7N0zC0jRsiGVSMsFETTq10F39pH2eBfUv/hJ\n" +
        "cLfBnIjBEtVqV/dExK88Hul2sZ4mQihQ4issPl7hsroS9EWYicnX0oNAqAB9PO5Y\n" +
        "zKbfpL7TAhUA13WW48rln2UP/LaAgtnzKhqcNtMCgYEA3Rv0GirTbAaor8iURd82\n" +
        "b5FlDTevOCTuq7ZIpfZVV30neS7cBYNet6m/3/4cfUlbbrqhbqIJ2I+I81drnN0Y\n" +
        "lyN4KkuxEcB6OTwfWkIUj6rvPaCQrBH8Q213bDq3HHtYNaP8OoeQUyVXW+SEGADC\n" +
        "J1+z8uqP3lIB6ltdgOiV/GQDgYUAAoGBAOXRppuJSGdt6AiZkb81P1DCUgIUlZFI\n" +
        "J9GxWrjbbHDmGllMwPNhK6dU7LJKJJuYVPW+95rUGlSJEjRqSlHuyHkNb6e3e7qx\n" +
        "tmx1/oIyq+oLult50hBS7uBvLLR0JbIKjBzzkudL8Rjze4G/Wq7KDM2T1JOP49tW\n" +
        "eocCvaC8h8uQo4GtMIGqMB0GA1UdDgQWBBT17HcqLllsqnZzP+kElcGcBGmubjBr\n" +
        "BgNVHSMEZDBigBT17HcqLllsqnZzP+kElcGcBGmubqE/pD0wOzELMAkGA1UEBhMC\n" +
        "VVMxDTALBgNVBAoTBEphdmExHTAbBgNVBAsTFFN1bkpTU0UgVGVzdCBTZXJpdmNl\n" +
        "ggkArz+DDrOD8M8wDwYDVR0TAQH/BAUwAwEB/zALBgNVHQ8EBAMCAQYwCQYHKoZI\n" +
        "zjgEAwMvADAsAhQ6Y1I6LtIEBMqNo8o6GIe4LLEJuwIUbVQUKi8tvtWyRoxm8AFV\n" +
        "0axJYUU=\n" +
        "-----END CERTIFICATE-----";

    static String[] targetCertStr = {
        // DSA-SHA1
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDKTCCAumgAwIBAgIJAOy5c0b+8stFMAkGByqGSM44BAMwOzELMAkGA1UEBhMC\n" +
        "VVMxDTALBgNVBAoTBEphdmExHTAbBgNVBAsTFFN1bkpTU0UgVGVzdCBTZXJpdmNl\n" +
        "MB4XDTE1MTIwMzEzNTIyNVoXDTM1MDgyMDEzNTIyNVowTzELMAkGA1UEBhMCVVMx\n" +
        "DTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpTU0UgVGVzdCBTZXJpdmNlMRIw\n" +
        "EAYDVQQDDAlsb2NhbGhvc3QwggG3MIIBLAYHKoZIzjgEATCCAR8CgYEA8f5v4ZIx\n" +
        "fopLuNcNF6/NzjrgMUbhf67G7lVvdTjKGxTwDxy4fiDCKfov8gOSZlDs3TMLSNGy\n" +
        "IZVIywURNOrXQXf2kfZ4F9S/+Elwt8GciMES1WpX90TErzwe6XaxniZCKFDiKyw+\n" +
        "XuGyuhL0RZiJydfSg0CoAH087ljMpt+kvtMCFQDXdZbjyuWfZQ/8toCC2fMqGpw2\n" +
        "0wKBgQDdG/QaKtNsBqivyJRF3zZvkWUNN684JO6rtkil9lVXfSd5LtwFg163qb/f\n" +
        "/hx9SVtuuqFuognYj4jzV2uc3RiXI3gqS7ERwHo5PB9aQhSPqu89oJCsEfxDbXds\n" +
        "Orcce1g1o/w6h5BTJVdb5IQYAMInX7Py6o/eUgHqW12A6JX8ZAOBhAACgYB+zYqn\n" +
        "jJwG4GZpBIN/6qhzbp0flChsV+Trlu0SL0agAQzb6XdI/4JnO87Pgbxaxh3VNAj3\n" +
        "3+Ghr1NLBuBfTKzJ4j9msWT3EpLupkMyNtXvBYM0iyMrll67lSjMdv++wLEw35Af\n" +
        "/bzVcjGyA5Q0i0cuEzDmHTVfi0OydynbwSLxtKNjMGEwCwYDVR0PBAQDAgPoMB0G\n" +
        "A1UdDgQWBBQXJI8AxM0qsYCbbkIMuI5zJ+nMEDAfBgNVHSMEGDAWgBT17HcqLlls\n" +
        "qnZzP+kElcGcBGmubjASBgNVHREBAf8ECDAGhwR/AAABMAkGByqGSM44BAMDLwAw\n" +
        "LAIUXgyJ0xll4FrZAKXi8bj7Kiz+SA4CFH9WCSZIBYA9lmJkiTgRS7iM/6IC\n" +
        "-----END CERTIFICATE-----",

        // DSA-SHA224
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDLzCCAuugAwIBAgIJAOy5c0b+8stGMAsGCWCGSAFlAwQDATA7MQswCQYDVQQG\n" +
        "EwJVUzENMAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2\n" +
        "Y2UwHhcNMTUxMjAzMTU0NDM5WhcNMzUwODIwMTU0NDM5WjBPMQswCQYDVQQGEwJV\n" +
        "UzENMAsGA1UECgwESmF2YTEdMBsGA1UECwwUU3VuSlNTRSBUZXN0IFNlcml2Y2Ux\n" +
        "EjAQBgNVBAMMCWxvY2FsaG9zdDCCAbcwggEsBgcqhkjOOAQBMIIBHwKBgQDx/m/h\n" +
        "kjF+iku41w0Xr83OOuAxRuF/rsbuVW91OMobFPAPHLh+IMIp+i/yA5JmUOzdMwtI\n" +
        "0bIhlUjLBRE06tdBd/aR9ngX1L/4SXC3wZyIwRLValf3RMSvPB7pdrGeJkIoUOIr\n" +
        "LD5e4bK6EvRFmInJ19KDQKgAfTzuWMym36S+0wIVANd1luPK5Z9lD/y2gILZ8yoa\n" +
        "nDbTAoGBAN0b9Boq02wGqK/IlEXfNm+RZQ03rzgk7qu2SKX2VVd9J3ku3AWDXrep\n" +
        "v9/+HH1JW266oW6iCdiPiPNXa5zdGJcjeCpLsRHAejk8H1pCFI+q7z2gkKwR/ENt\n" +
        "d2w6txx7WDWj/DqHkFMlV1vkhBgAwidfs/Lqj95SAepbXYDolfxkA4GEAAKBgA81\n" +
        "CJKEv+pwiqYgxtw/9rkQ9748WP3mKrEC06kjUG+94/Z9dQloNFFfj6LiO1bymc5l\n" +
        "6QIR8XCi4Po3N80K3+WxhBGFhY+RkVWTh43JV8epb41aH2qiWErarBwBGEh8LyGT\n" +
        "i30db+Nkz2gfvyz9H/9T0jmYgfLEOlMCusali1qHo2MwYTALBgNVHQ8EBAMCA+gw\n" +
        "HQYDVR0OBBYEFBqSP0S4+X+zOCTEnlp2hbAjV/W5MB8GA1UdIwQYMBaAFPXsdyou\n" +
        "WWyqdnM/6QSVwZwEaa5uMBIGA1UdEQEB/wQIMAaHBH8AAAEwCwYJYIZIAWUDBAMB\n" +
        "AzEAMC4CFQChiRaOnAnsCSJFwdpK22jSxU/mhQIVALgLbj/G39+1Ej8UuSWnEQyU\n" +
        "4DA+\n" +
        "-----END CERTIFICATE-----",

        // DSA-SHA256
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDLTCCAuugAwIBAgIJAOy5c0b+8stHMAsGCWCGSAFlAwQDAjA7MQswCQYDVQQG\n" +
        "EwJVUzENMAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2\n" +
        "Y2UwHhcNMTUxMjAzMTU0NjUxWhcNMzUwODIwMTU0NjUxWjBPMQswCQYDVQQGEwJV\n" +
        "UzENMAsGA1UECgwESmF2YTEdMBsGA1UECwwUU3VuSlNTRSBUZXN0IFNlcml2Y2Ux\n" +
        "EjAQBgNVBAMMCWxvY2FsaG9zdDCCAbcwggEsBgcqhkjOOAQBMIIBHwKBgQDx/m/h\n" +
        "kjF+iku41w0Xr83OOuAxRuF/rsbuVW91OMobFPAPHLh+IMIp+i/yA5JmUOzdMwtI\n" +
        "0bIhlUjLBRE06tdBd/aR9ngX1L/4SXC3wZyIwRLValf3RMSvPB7pdrGeJkIoUOIr\n" +
        "LD5e4bK6EvRFmInJ19KDQKgAfTzuWMym36S+0wIVANd1luPK5Z9lD/y2gILZ8yoa\n" +
        "nDbTAoGBAN0b9Boq02wGqK/IlEXfNm+RZQ03rzgk7qu2SKX2VVd9J3ku3AWDXrep\n" +
        "v9/+HH1JW266oW6iCdiPiPNXa5zdGJcjeCpLsRHAejk8H1pCFI+q7z2gkKwR/ENt\n" +
        "d2w6txx7WDWj/DqHkFMlV1vkhBgAwidfs/Lqj95SAepbXYDolfxkA4GEAAKBgEF7\n" +
        "2qiYxGrjX4KCOy0k5nK/RYlgLy4gYDChihQpiaa+fbA5JOBOxPWsh7rdtmJuDrEJ\n" +
        "keacU223+DIhOKC49fa+EvhLNqo6U1oPn8n/yvBsvvnWkcynw5KfNzaLlaPmzugh\n" +
        "v9xl/GhyZNAXc1QUcW3C+ceHVNrKnkfbTKZz5eRSo2MwYTALBgNVHQ8EBAMCA+gw\n" +
        "HQYDVR0OBBYEFNMkPrt40oO9Dpy+bcbQdEvOlNlyMB8GA1UdIwQYMBaAFPXsdyou\n" +
        "WWyqdnM/6QSVwZwEaa5uMBIGA1UdEQEB/wQIMAaHBH8AAAEwCwYJYIZIAWUDBAMC\n" +
        "Ay8AMCwCFCvA2QiKSe/n+6GqSYQwgQ/zL5M9AhQfSiuWdMJKWpgPJKakvzhBUbMb\n" +
        "vA==\n" +
        "-----END CERTIFICATE-----"};

    // Private key in the format of PKCS#8, key size is 1024 bits.
    static String[] targetPrivateKey = {
        // For cert DSA-SHA1
        "MIIBSwIBADCCASwGByqGSM44BAEwggEfAoGBAPH+b+GSMX6KS7jXDRevzc464DFG\n" +
        "4X+uxu5Vb3U4yhsU8A8cuH4gwin6L/IDkmZQ7N0zC0jRsiGVSMsFETTq10F39pH2\n" +
        "eBfUv/hJcLfBnIjBEtVqV/dExK88Hul2sZ4mQihQ4issPl7hsroS9EWYicnX0oNA\n" +
        "qAB9PO5YzKbfpL7TAhUA13WW48rln2UP/LaAgtnzKhqcNtMCgYEA3Rv0GirTbAao\n" +
        "r8iURd82b5FlDTevOCTuq7ZIpfZVV30neS7cBYNet6m/3/4cfUlbbrqhbqIJ2I+I\n" +
        "81drnN0YlyN4KkuxEcB6OTwfWkIUj6rvPaCQrBH8Q213bDq3HHtYNaP8OoeQUyVX\n" +
        "W+SEGADCJ1+z8uqP3lIB6ltdgOiV/GQEFgIUOiB7J/lrFrNduQ8nDNTe8VspoAI=",

        // For cert DSA-SHA224
        "MIIBSwIBADCCASwGByqGSM44BAEwggEfAoGBAPH+b+GSMX6KS7jXDRevzc464DFG\n" +
        "4X+uxu5Vb3U4yhsU8A8cuH4gwin6L/IDkmZQ7N0zC0jRsiGVSMsFETTq10F39pH2\n" +
        "eBfUv/hJcLfBnIjBEtVqV/dExK88Hul2sZ4mQihQ4issPl7hsroS9EWYicnX0oNA\n" +
        "qAB9PO5YzKbfpL7TAhUA13WW48rln2UP/LaAgtnzKhqcNtMCgYEA3Rv0GirTbAao\n" +
        "r8iURd82b5FlDTevOCTuq7ZIpfZVV30neS7cBYNet6m/3/4cfUlbbrqhbqIJ2I+I\n" +
        "81drnN0YlyN4KkuxEcB6OTwfWkIUj6rvPaCQrBH8Q213bDq3HHtYNaP8OoeQUyVX\n" +
        "W+SEGADCJ1+z8uqP3lIB6ltdgOiV/GQEFgIUOj9F5mxWd9W1tiLSdsOAt8BUBzE=",

        // For cert DSA-SHA256
        "MIIBSwIBADCCASwGByqGSM44BAEwggEfAoGBAPH+b+GSMX6KS7jXDRevzc464DFG\n" +
        "4X+uxu5Vb3U4yhsU8A8cuH4gwin6L/IDkmZQ7N0zC0jRsiGVSMsFETTq10F39pH2\n" +
        "eBfUv/hJcLfBnIjBEtVqV/dExK88Hul2sZ4mQihQ4issPl7hsroS9EWYicnX0oNA\n" +
        "qAB9PO5YzKbfpL7TAhUA13WW48rln2UP/LaAgtnzKhqcNtMCgYEA3Rv0GirTbAao\n" +
        "r8iURd82b5FlDTevOCTuq7ZIpfZVV30neS7cBYNet6m/3/4cfUlbbrqhbqIJ2I+I\n" +
        "81drnN0YlyN4KkuxEcB6OTwfWkIUj6rvPaCQrBH8Q213bDq3HHtYNaP8OoeQUyVX\n" +
        "W+SEGADCJ1+z8uqP3lIB6ltdgOiV/GQEFgIUQ2WGgg+OO39Aujj0e4lM4pP4/9g="};


    static char passphrase[] = "passphrase".toCharArray();

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = false;

    /*
     * Is the server ready to serve?
     */
    volatile boolean serverReady = false;

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {

        SSLContext context = generateSSLContext(
                null, targetCertStr, targetPrivateKey);
        SSLServerSocketFactory sslssf = context.getServerSocketFactory();
        try (SSLServerSocket sslServerSocket =
                (SSLServerSocket)sslssf.createServerSocket(serverPort)) {

            serverPort = sslServerSocket.getLocalPort();

            /*
             * Signal Client, we're ready for his connect.
             */
            serverReady = true;

            try (SSLSocket sslSocket = (SSLSocket)sslServerSocket.accept()) {
                sslSocket.setEnabledCipherSuites(
                        sslSocket.getSupportedCipherSuites());
                InputStream sslIS = sslSocket.getInputStream();
                OutputStream sslOS = sslSocket.getOutputStream();

                sslIS.read();
                sslOS.write('A');
                sslOS.flush();

                dumpSignatureAlgorithms(sslSocket);
            }
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {

        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        SSLContext context = generateSSLContext(trustedCertStr, null, null);
        SSLSocketFactory sslsf = context.getSocketFactory();

        try (SSLSocket sslSocket =
                (SSLSocket)sslsf.createSocket("localhost", serverPort)) {

            // enable TLSv1.2 only
            sslSocket.setEnabledProtocols(new String[] {"TLSv1.2"});

            // enable a block cipher
            sslSocket.setEnabledCipherSuites(new String[] {cipherSuite});

            InputStream sslIS = sslSocket.getInputStream();
            OutputStream sslOS = sslSocket.getOutputStream();

            sslOS.write('B');
            sslOS.flush();
            sslIS.read();

            dumpSignatureAlgorithms(sslSocket);
        }
    }

    static void dumpSignatureAlgorithms(SSLSocket sslSocket) throws Exception {

        boolean isClient = sslSocket.getUseClientMode();
        String mode = "[" + (isClient ? "Client" : "Server") + "]";
        ExtendedSSLSession session =
                (ExtendedSSLSession)sslSocket.getSession();
        String[] signAlgs = session.getLocalSupportedSignatureAlgorithms();
        System.out.println(
                mode + " local supported signature algorithms: " +
                Arrays.asList(signAlgs));

        if (!isClient) {
            signAlgs = session.getPeerSupportedSignatureAlgorithms();
            System.out.println(
                mode + " peer supported signature algorithms: " +
                Arrays.asList(signAlgs));
        } else {
            Certificate[] serverCerts = session.getPeerCertificates();

            // server should always send the authentication cert.
            String sigAlg = ((X509Certificate)serverCerts[0]).getSigAlgName();
            System.out.println(
                mode + " the signature algorithm of server certificate: " +
                sigAlg);
            if (sigAlg.contains("SHA1")) {
                if (disabledAlgorithms.contains("SHA-1")) {
                    throw new Exception(
                            "Not the expected server certificate. " +
                            "SHA-1 should be disabled");
                }
            } else if (sigAlg.contains("SHA224")) {
                if (disabledAlgorithms.contains("SHA-224")) {
                    throw new Exception(
                            "Not the expected server certificate. " +
                            "SHA-224 should be disabled");
                }
            } else {    // SHA-256
                if (disabledAlgorithms.contains("SHA-256")) {
                    throw new Exception(
                            "Not the expected server certificate. " +
                            "SHA-256 should be disabled");
                }
            }
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */
    private static String tmAlgorithm;          // trust manager
    private static String disabledAlgorithms;   // disabled algorithms
    private static String cipherSuite;          // cipher suite

    private static void parseArguments(String[] args) {
        tmAlgorithm = args[0];
        disabledAlgorithms = args[1];
        cipherSuite = args[2];
    }

    private static SSLContext generateSSLContext(String trustedCertStr,
            String[] keyCertStrs, String[] keySpecStrs) throws Exception {

        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // create a key store
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // import the trused cert
        Certificate trusedCert = null;
        ByteArrayInputStream is = null;
        if (trustedCertStr != null) {
            is = new ByteArrayInputStream(trustedCertStr.getBytes());
            trusedCert = cf.generateCertificate(is);
            is.close();

            ks.setCertificateEntry("DSA Signer", trusedCert);
        }

        if (keyCertStrs != null && keyCertStrs.length != 0) {
            for (int i = 0; i < keyCertStrs.length; i++) {
                String keyCertStr = keyCertStrs[i];
                String keySpecStr = keySpecStrs[i];

                // generate the private key.
                PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                                Base64.getMimeDecoder().decode(keySpecStr));
                KeyFactory kf = KeyFactory.getInstance("DSA");
                DSAPrivateKey priKey =
                        (DSAPrivateKey)kf.generatePrivate(priKeySpec);

                // generate certificate chain
                is = new ByteArrayInputStream(keyCertStr.getBytes());
                Certificate keyCert = cf.generateCertificate(is);
                is.close();

                Certificate[] chain = null;
                if (trusedCert != null) {
                    chain = new Certificate[2];
                    chain[0] = keyCert;
                    chain[1] = trusedCert;
                } else {
                    chain = new Certificate[1];
                    chain[0] = keyCert;
                }

                // import the key entry.
                ks.setKeyEntry("DSA Entry " + i, priKey, passphrase, chain);
            }
        }

        // create SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmAlgorithm);
        tmf.init(ks);

        SSLContext ctx = SSLContext.getInstance("TLS");
        if (keyCertStrs != null && keyCertStrs.length != 0) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
            kmf.init(ks, passphrase);

            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            ks = null;
        } else {
            ctx.init(null, tmf.getTrustManagers(), null);
        }

        return ctx;
    }


    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String[] args) throws Exception {
        /*
         * debug option
         */
        if (debug) {
            System.setProperty("javax.net.debug", "all");
        }

        /*
         * Get the customized arguments.
         */
        parseArguments(args);


        /*
         * Ignore testing on Windows if only SHA-224 is available.
         */
        if ((Security.getProvider("SunMSCAPI") != null) &&
                (disabledAlgorithms.contains("SHA-1")) &&
                (disabledAlgorithms.contains("SHA-256"))) {

            System.out.println(
                "Windows system does not support SHA-224 algorithms yet. " +
                "Ignore the testing");

            return;
        }

        /*
         * Expose the target algorithms by diabling unexpected algorithms.
         */
        Security.setProperty(
                "jdk.certpath.disabledAlgorithms", disabledAlgorithms);

        /*
         * Reset the security property to make sure that the algorithms
         * and keys used in this test are not disabled by default.
         */
        Security.setProperty( "jdk.tls.disabledAlgorithms", "");

        /*
         * Start the tests.
         */
        new SignatureAlgorithms();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SignatureAlgorithms() throws Exception {
        try {
            if (separateServerThread) {
                startServer(true);
                startClient(false);
            } else {
                startClient(true);
                startServer(false);
            }
        } catch (Exception e) {
            // swallow for now.  Show later
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            serverThread.join();
        } else {
            clientThread.join();
        }

        /*
         * When we get here, the test is pretty much over.
         * Which side threw the error?
         */
        Exception local;
        Exception remote;
        String whichRemote;

        if (separateServerThread) {
            remote = serverException;
            local = clientException;
            whichRemote = "server";
        } else {
            remote = clientException;
            local = serverException;
            whichRemote = "client";
        }

        /*
         * If both failed, return the curthread's exception, but also
         * print the remote side Exception
         */
        if ((local != null) && (remote != null)) {
            System.out.println(whichRemote + " also threw:");
            remote.printStackTrace();
            System.out.println();
            throw local;
        }

        if (remote != null) {
            throw remote;
        }

        if (local != null) {
            throw local;
        }
    }

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide();
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died..." + e);
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            try {
                doServerSide();
            } catch (Exception e) {
                serverException = e;
            } finally {
                serverReady = true;
            }
        }
    }

    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide();
                    } catch (Exception e) {
                        /*
                         * Our client thread just died.
                         */
                        System.err.println("Client died..." + e);
                        clientException = e;
                    }
                }
            };
            clientThread.start();
        } else {
            try {
                doClientSide();
            } catch (Exception e) {
                clientException = e;
            }
        }
    }
}
