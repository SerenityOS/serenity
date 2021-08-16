/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046321
 * @summary OCSP Stapling for TLS (ResponderId tests)
 * @modules java.base/sun.security.provider.certpath
 *          java.base/sun.security.x509
 */

import java.io.*;
import java.security.cert.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import javax.security.auth.x500.X500Principal;
import sun.security.x509.KeyIdentifier;
import sun.security.provider.certpath.ResponderId;

/*
 * NOTE: this test uses Sun private classes which are subject to change.
 */
public class ResponderIdTests {

    private static final boolean debug = true;

    // Source certificate created with the following command:
    // keytool -genkeypair -alias test1 -keyalg rsa -keysize 2048 \
    //   -validity 7300 -keystore test1.jks \
    //   -dname "CN=SelfSignedResponder, OU=Validation Services, O=FakeCompany"
    private static final String RESP_CERT_1 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDQzCCAiugAwIBAgIEXTqCCjANBgkqhkiG9w0BAQsFADBSMRQwEgYDVQQKEwtG\n" +
        "YWtlQ29tcGFueTEcMBoGA1UECxMTVmFsaWRhdGlvbiBTZXJ2aWNlczEcMBoGA1UE\n" +
        "AxMTU2VsZlNpZ25lZFJlc3BvbmRlcjAeFw0xNDA4MTcwNDM2MzBaFw0zNDA4MTIw\n" +
        "NDM2MzBaMFIxFDASBgNVBAoTC0Zha2VDb21wYW55MRwwGgYDVQQLExNWYWxpZGF0\n" +
        "aW9uIFNlcnZpY2VzMRwwGgYDVQQDExNTZWxmU2lnbmVkUmVzcG9uZGVyMIIBIjAN\n" +
        "BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApt2Cmw2k9tviLxaxE8aWNuoosWKL\n" +
        "h+K4mNcDGKSoiChsqRqeJEnOxijDZqyFwfkaXvpAduFqYjz+Lij2HumvAjHDTui6\n" +
        "bGcbsndRDPjvVo1S7f1oWsg7oiA8Lzmjl452S7UNBsDX5Dt1e84Xxwi40B1J2y8D\n" +
        "FRPfYRWRlC1Z4kzqkBBa7JhANS+W8KDstFZxL4AwWH/byNwB5dl2j04ohg/Ar54e\n" +
        "mu08PIH3hmi0pAu5wn9ariA7UA5lFWRJzvgGXV5J+QVEFuvKmeJ/Q6tU5OBJGw98\n" +
        "zjd7F5B0iE+rJHTNF1aGaQfIorz04onV2WjH2VZA18AaMwqlY2br1SBdTQIDAQAB\n" +
        "oyEwHzAdBgNVHQ4EFgQUG09HasSTYaTIh/CxxV/rcJV1LvowDQYJKoZIhvcNAQEL\n" +
        "BQADggEBAIcUomNpZxGkocIzzybLyeyC6vLF1k0/unuPAHZLDP3o2JTstPhLHOCg\n" +
        "FYw1VG2i23pjwKK2x/o80tJAOmW6vowbAPnNmtNIYO3gB/ZGiKeORoGKBCRDNvFa\n" +
        "6ZrWxwTzT3EpVwRe7ameES0uP8+S4q2P5LhwMIMw7vGHoOQJgkAh/NUiCli1qRnJ\n" +
        "FYd6cHMJJK5gF2FqQ7tdbA26pS06bkIEvil2M5wyKKWOydOa/pr1LgMf9KxljJ8J\n" +
        "XlAOO/mGZGkYmWnQaQuBIDyWunWYlhsyCXMa8AScgs0uUeQp19tO7R0f03q/JXoZ\n" +
        "1At1gZiMS7SdQaRWP5q+FunAeFWjsFE=\n" +
        "-----END CERTIFICATE-----";

    private static final String RESP_CERT_1_SUBJ =
        "CN=SelfSignedResponder, OU=Validation Services, O=FakeCompany";

    private static X509Certificate cert = null;

    // The expected DER-encoding for a byName ResponderId derived
    // from RESP_CERT_1
    private static final byte[] EXP_NAME_ID_BYTES = {
        -95,   84,   48,   82,   49,   20,   48,   18,
          6,    3,   85,    4,   10,   19,   11,   70,
         97,  107,  101,   67,  111,  109,  112,   97,
        110,  121,   49,   28,   48,   26,    6,    3,
         85,    4,   11,   19,   19,   86,   97,  108,
        105,  100,   97,  116,  105,  111,  110,   32,
         83,  101,  114,  118,  105,   99,  101,  115,
         49,   28,   48,   26,    6,    3,   85,    4,
          3,   19,   19,   83,  101,  108,  102,   83,
        105,  103,  110,  101,  100,   82,  101,  115,
        112,  111,  110,  100,  101,  114
    };

    // The expected DER-encoding for a byKey ResponderId derived
    // from RESP_CERT_1
    private static final byte[] EXP_KEY_ID_BYTES = {
         -94,   22,    4,   20,   27,   79,   71,  106,
         -60, -109,   97,  -92,  -56, -121,  -16,  -79,
         -59,   95,  -21,  112, -107,  117,   46,   -6
    };

    // The DER encoding of a byKey ResponderId, but using an
    // incorrect explicit tagging (CONTEXT CONSTRUCTED 3)
    private static final byte[] INV_EXPLICIT_TAG_KEY_ID = {
         -93,   22,    4,   20,   27,   79,   71,  106,
         -60, -109,   97,  -92,  -56, -121,  -16,  -79,
         -59,   95,  -21,  112, -107,  117,   46,   -6
    };

    // These two ResponderId objects will have objects attached to them
    // after the pos_CtorByName and pos_CtorByKeyId tests run.  Those
    // two tests should always be the first two that run.
    public static ResponderId respByName;
    public static ResponderId respByKeyId;

    public static void main(String[] args) throws Exception {
        List<TestCase> testList = new ArrayList<>();

        testList.add(pos_CtorByName);
        testList.add(pos_CtorByKeyId);
        testList.add(pos_CtorByEncoding);
        testList.add(neg_CtorByEncoding);
        testList.add(pos_Equality);
        testList.add(pos_GetEncoded);
        testList.add(pos_GetRespName);
        testList.add(pos_GetRespKeyId);

        // Load the certificate object we can use for subsequent tests
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        cert = (X509Certificate)cf.generateCertificate(
                new ByteArrayInputStream(RESP_CERT_1.getBytes()));

        System.out.println("============ Tests ============");
        int testNo = 0;
        int numberFailed = 0;
        Map.Entry<Boolean, String> result;
        for (TestCase test : testList) {
            System.out.println("Test " + ++testNo + ": " + test.getName());
            result = test.runTest();
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

    private static void dumpHexBytes(byte[] data) {
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                if (i % 16 == 0 && i != 0) {
                    System.out.print("\n");
                }
                System.out.print(String.format("%02X ", data[i]));
            }
            System.out.print("\n");
        }
    }

    public interface TestCase {
        String getName();
        Map.Entry<Boolean, String> runTest();
    }

    public static final TestCase pos_CtorByName = new TestCase() {
        @Override
        public String getName() {
            return "CTOR Test (by-name)";
        }

        @Override
        public Map.Entry<Boolean, String> runTest() {
            Boolean pass = Boolean.FALSE;
            String message = null;
            try {
                respByName = new ResponderId(cert.getSubjectX500Principal());
                pass = Boolean.TRUE;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

    public static final TestCase pos_CtorByKeyId = new TestCase() {
        @Override
        public String getName() {
            return "CTOR Test (by-keyID)";
        }

        @Override
        public Map.Entry<Boolean, String> runTest() {
            Boolean pass = Boolean.FALSE;
            String message = null;
            try {
                respByKeyId = new ResponderId(cert.getPublicKey());
                pass = Boolean.TRUE;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

    public static final TestCase pos_CtorByEncoding = new TestCase() {
        @Override
        public String getName() {
            return "CTOR Test (encoded bytes)";
        }

        @Override
        public Map.Entry<Boolean, String> runTest() {
            Boolean pass = Boolean.FALSE;
            String message = null;
            try {
                ResponderId ridByNameBytes = new ResponderId(EXP_NAME_ID_BYTES);
                ResponderId ridByKeyIdBytes = new ResponderId(EXP_KEY_ID_BYTES);

                if (!ridByNameBytes.equals(respByName)) {
                    throw new RuntimeException(
                            "Equals failed: respNameFromBytes vs. respByName");
                } else if (!ridByKeyIdBytes.equals(respByKeyId)) {
                    throw new RuntimeException(
                            "Equals failed: respKeyFromBytes vs. respByKeyId");
                }
                pass = Boolean.TRUE;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };

    public static final TestCase neg_CtorByEncoding = new TestCase() {
        @Override
        public String getName() {
            return "CTOR Test (by encoding, unknown explicit tag)";
        }

        @Override
        public Map.Entry<Boolean, String> runTest() {
            Boolean pass = Boolean.FALSE;
            String message = null;
            try {
                ResponderId ridByKeyIdBytes =
                        new ResponderId(INV_EXPLICIT_TAG_KEY_ID);
                throw new RuntimeException("Expected IOException not thrown");
            } catch (IOException ioe) {
                // Make sure it's the IOException we're looking for
                if (ioe.getMessage().contains("Invalid ResponderId content")) {
                    pass = Boolean.TRUE;
                } else {
                    ioe.printStackTrace(System.out);
                    message = ioe.getClass().getName();
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
                message = e.getClass().getName();
            }

            return new AbstractMap.SimpleEntry<>(pass, message);
        }
    };


    public static final TestCase pos_Equality = new TestCase() {
        @Override
        public String getName() {
            return "Simple Equality Test";
        }

        @Override
        public Map.Entry<Boolean, String> runTest() {
            Boolean pass = Boolean.FALSE;
            String message = null;

            try {
                // byName ResponderId equality test
                ResponderId compName =
                    new ResponderId(new X500Principal(RESP_CERT_1_SUBJ));
                if (!respByName.equals(compName)) {
                    message = "ResponderId mismatch in byName comparison";
                } else if (respByKeyId.equals(compName)) {
                    message = "Invalid ResponderId match in byKeyId comparison";
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

    public static final TestCase pos_GetEncoded = new TestCase() {
        @Override
        public String getName() {
            return "Get Encoded Value";
        }

        @Override
        public Map.Entry<Boolean, String> runTest() {
            Boolean pass = Boolean.FALSE;
            String message = null;

            try {
                // Pull out byName and byKey encodings, they should match
                // the expected values
                if (!Arrays.equals(respByName.getEncoded(), EXP_NAME_ID_BYTES)) {
                    message = "ResponderId byName encoding did not " +
                            "match expected value";
                } else if (!Arrays.equals(respByKeyId.getEncoded(), EXP_KEY_ID_BYTES)) {
                    message = "ResponderId byKeyId encoding did not " +
                            "match expected value";
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

    public static final TestCase pos_GetRespName = new TestCase() {
        @Override
        public String getName() {
            return "Get Underlying Responder Name";
        }

        @Override
        public Map.Entry<Boolean, String> runTest() {
            Boolean pass = Boolean.FALSE;
            String message = null;

            try {
                // Test methods for pulling out the underlying
                // X500Principal object
                X500Principal testPrincipal =
                        new X500Principal(RESP_CERT_1_SUBJ);
                if (!respByName.getResponderName().equals(testPrincipal)) {
                    message = "ResponderId Name did not match expected value";
                } else if (respByKeyId.getResponderName() != null) {
                    message = "Non-null responder name returned from " +
                            "ResponderId constructed byKey";
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

    public static final TestCase pos_GetRespKeyId = new TestCase() {
        @Override
        public String getName() {
            return "Get Underlying Responder Key ID";
        }

        @Override
        public Map.Entry<Boolean, String> runTest() {
            Boolean pass = Boolean.FALSE;
            String message = null;

            try {
                // Test methods for pulling out the underlying
                // KeyIdentifier object.  Note: There is a minute chance that
                // an RSA public key, once hashed into a key ID might collide
                // with the one extracted from the certificate used to create
                // respByKeyId.  This is so unlikely to happen it is considered
                // virtually impossible.
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair rsaKey = kpg.generateKeyPair();
                KeyIdentifier testKeyId = new KeyIdentifier(rsaKey.getPublic());

                if (respByKeyId.getKeyIdentifier().equals(testKeyId)) {
                    message = "Unexpected match in ResponderId Key ID";
                } else if (respByName.getKeyIdentifier() != null) {
                    message = "Non-null key ID returned from " +
                            "ResponderId constructed byName";
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

}
