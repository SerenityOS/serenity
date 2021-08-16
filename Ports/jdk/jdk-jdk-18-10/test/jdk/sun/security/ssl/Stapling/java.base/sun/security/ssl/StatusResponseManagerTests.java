/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ssl;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.*;
import java.util.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

import sun.security.testlibrary.SimpleOCSPServer;
import sun.security.testlibrary.CertificateBuilder;

import static sun.security.ssl.CertStatusExtension.*;

/*
 * Checks that the hash value for a certificate's issuer name is generated
 * correctly. Requires any certificate that is not self-signed.
 *
 * NOTE: this test uses Sun private classes which are subject to change.
 */
public class StatusResponseManagerTests {

    private static final boolean debug = true;
    private static final boolean ocspDebug = false;

    // PKI components we will need for this test
    static String passwd = "passphrase";
    static String ROOT_ALIAS = "root";
    static String INT_ALIAS = "intermediate";
    static String SSL_ALIAS = "ssl";
    static KeyStore rootKeystore;           // Root CA Keystore
    static KeyStore intKeystore;            // Intermediate CA Keystore
    static KeyStore serverKeystore;         // SSL Server Keystore
    static KeyStore trustStore;             // SSL Client trust store
    static X509Certificate rootCert;
    static X509Certificate intCert;
    static X509Certificate sslCert;
    static SimpleOCSPServer rootOcsp;       // Root CA OCSP Responder
    static int rootOcspPort;                // Port number for root OCSP
    static SimpleOCSPServer intOcsp;        // Intermediate CA OCSP Responder
    static int intOcspPort;                 // Port number for intermed. OCSP

    static X509Certificate[] chain;

    public static void main(String[] args) throws Exception {
        Map<String, TestCase> testList =
                new LinkedHashMap<String, TestCase>() {{
            put("Basic OCSP fetch test", testOcspFetch);
            put("Clear StatusResponseManager cache", testClearSRM);
            put("Basic OCSP_MULTI fetch test", testOcspMultiFetch);
            put("Test Cache Expiration", testCacheExpiry);
        }};

        // Create the CAs and OCSP responders
        createPKI();

        // Grab the certificates and make a chain we can reuse for tests
        sslCert = (X509Certificate)serverKeystore.getCertificate(SSL_ALIAS);
        intCert = (X509Certificate)intKeystore.getCertificate(INT_ALIAS);
        rootCert = (X509Certificate)rootKeystore.getCertificate(ROOT_ALIAS);
        chain = new X509Certificate[3];
        chain[0] = sslCert;
        chain[1] = intCert;
        chain[2] = rootCert;

        runTests(testList);

        intOcsp.stop();
        rootOcsp.stop();
    }

    // Test a simple RFC 6066 server-side fetch
    public static final TestCase testOcspFetch = new TestCase() {
        @Override
        public Map.Entry<Boolean, String> runTest() {
            StatusResponseManager srm = new StatusResponseManager();
            Boolean pass = Boolean.FALSE;
            String message = null;
            CertStatusRequest oReq = OCSPStatusRequest.EMPTY_OCSP;

            try {
                // Get OCSP responses for non-root certs in the chain
                Map<X509Certificate, byte[]> responseMap = srm.get(
                        CertStatusRequestType.OCSP, oReq, chain, 5000,
                        TimeUnit.MILLISECONDS);

                // There should be one entry in the returned map and
                // one entry in the cache when the operation is complete.
                if (responseMap.size() != 1) {
                    message = "Incorrect number of responses: expected 1, got "
                            + responseMap.size();
                } else if (!responseMap.containsKey(sslCert)) {
                    message = "Response map key is incorrect, expected " +
                            sslCert.getSubjectX500Principal().toString();
                } else if (srm.size() != 1) {
                    message = "Incorrect number of cache entries: " +
                            "expected 1, got " + srm.size();
                } else {
                    pass = Boolean.TRUE;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

    // Test clearing the StatusResponseManager cache.
    public static final TestCase testClearSRM = new TestCase() {
        @Override
        public Map.Entry<Boolean, String> runTest() {
            StatusResponseManager srm = new StatusResponseManager();
            Boolean pass = Boolean.FALSE;
            String message = null;
            CertStatusRequest oReq = OCSPStatusRequest.EMPTY_OCSP_MULTI;

            try {
                // Get OCSP responses for non-root certs in the chain
                srm.get(CertStatusRequestType.OCSP_MULTI, oReq, chain, 5000,
                        TimeUnit.MILLISECONDS);

                // There should be two entries in the returned map and
                // two entries in the cache when the operation is complete.
                if (srm.size() != 2) {
                    message = "Incorrect number of responses: expected 2, got "
                            + srm.size();
                } else {
                    // Next, clear the SRM, then check the size again
                    srm.clear();
                    if (srm.size() != 0) {
                        message = "Incorrect number of responses: expected 0," +
                                " got " + srm.size();
                    } else {
                        pass = Boolean.TRUE;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

    // Test a simple RFC 6961 server-side fetch
    public static final TestCase testOcspMultiFetch = new TestCase() {
        @Override
        public Map.Entry<Boolean, String> runTest() {
            StatusResponseManager srm = new StatusResponseManager();
            Boolean pass = Boolean.FALSE;
            String message = null;
            CertStatusRequest oReq = OCSPStatusRequest.EMPTY_OCSP_MULTI;

            try {
                // Get OCSP responses for non-root certs in the chain
                Map<X509Certificate, byte[]> responseMap = srm.get(
                        CertStatusRequestType.OCSP_MULTI, oReq, chain, 5000,
                        TimeUnit.MILLISECONDS);

                // There should be two entries in the returned map and
                // two entries in the cache when the operation is complete.
                if (responseMap.size() != 2) {
                    message = "Incorrect number of responses: expected 2, got "
                            + responseMap.size();
                } else if (!responseMap.containsKey(sslCert) ||
                        !responseMap.containsKey(intCert)) {
                    message = "Response map keys are incorrect, expected " +
                            sslCert.getSubjectX500Principal().toString() +
                            " and " +
                            intCert.getSubjectX500Principal().toString();
                } else if (srm.size() != 2) {
                    message = "Incorrect number of cache entries: " +
                            "expected 2, got " + srm.size();
                } else {
                    pass = Boolean.TRUE;
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

    // Test cache expiration
    public static final TestCase testCacheExpiry = new TestCase() {
        @Override
        public Map.Entry<Boolean, String> runTest() {
            // For this test, we will set the cache expiry to 5 seconds
            System.setProperty("jdk.tls.stapling.cacheLifetime", "5");
            StatusResponseManager srm = new StatusResponseManager();
            Boolean pass = Boolean.FALSE;
            String message = null;
            CertStatusRequest oReq = OCSPStatusRequest.EMPTY_OCSP_MULTI;

            try {
                // Get OCSP responses for non-root certs in the chain
                srm.get(CertStatusRequestType.OCSP_MULTI, oReq, chain, 5000,
                        TimeUnit.MILLISECONDS);

                // There should be two entries in the returned map and
                // two entries in the cache when the operation is complete.
                if (srm.size() != 2) {
                    message = "Incorrect number of responses: expected 2, got "
                            + srm.size();
                } else {
                    // Next, wait for more than 5 seconds so the responses
                    // in the SRM will expire.
                    Thread.sleep(7000);
                    if (srm.size() != 0) {
                        message = "Incorrect number of responses: expected 0," +
                                " got " + srm.size();
                    } else {
                        pass = Boolean.TRUE;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            // Set the cache lifetime back to the default
            System.setProperty("jdk.tls.stapling.cacheLifetime", "");
            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

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
        Certificate[] rootChain = {rootCert};
        rootKeystore.setKeyEntry(ROOT_ALIAS, rootCaKP.getPrivate(),
                passwd.toCharArray(), rootChain);

        // Now fire up the OCSP responder
        rootOcsp = new SimpleOCSPServer(rootKeystore, passwd, ROOT_ALIAS, null);
        rootOcsp.enableLog(ocspDebug);
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
        Certificate[] intChain = {intCaCert, rootCert};
        intKeystore.setKeyEntry(INT_ALIAS, intCaKP.getPrivate(),
                passwd.toCharArray(), intChain);
        intKeystore.setCertificateEntry(ROOT_ALIAS, rootCert);

        // Now fire up the Intermediate CA OCSP responder
        intOcsp = new SimpleOCSPServer(intKeystore, passwd,
                INT_ALIAS, null);
        intOcsp.enableLog(ocspDebug);
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
        Certificate[] sslChain = {sslCert, intCaCert, rootCert};
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
     * @param cert An X509Certificate to be displayed
     *
     * @return The {@link String} output of the issuer, subject and
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

    /**
     * Log a message on stdout
     *
     * @param message The message to log
     */
    private static void log(String message) {
        if (debug) {
            System.out.println(message);
        }
    }

    public static void runTests(Map<String, TestCase> testList) {
        int testNo = 0;
        int numberFailed = 0;
        Map.Entry<Boolean, String> result;

        System.out.println("============ Tests ============");
        for (String testName : testList.keySet()) {
            System.out.println("Test " + ++testNo + ": " + testName);
            result = testList.get(testName).runTest();
            System.out.print("Result: " + (result.getKey() ? "PASS" : "FAIL"));
            System.out.println(" " +
                    (result.getValue() != null ? result.getValue() : ""));
            System.out.println("-------------------------------------------");
            if (!result.getKey()) {
                numberFailed++;
            }
        }

        System.out.println("End Results: " + (testList.size() - numberFailed) +
                " Passed" + ", " + numberFailed + " Failed.");
        if (numberFailed > 0) {
            throw new RuntimeException(
                    "One or more tests failed, see test output for details");
        }
    }

    public interface TestCase {
        Map.Entry<Boolean, String> runTest();
    }
}
