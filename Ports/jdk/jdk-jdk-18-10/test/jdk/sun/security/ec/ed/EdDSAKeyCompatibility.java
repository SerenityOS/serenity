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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import sun.security.util.DerValue;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.EdECPrivateKeySpec;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/*
 * @test
 * @bug 8209632
 * @summary OpenSSL generated compatibility test with EDDSA Java.
 * @modules java.base/sun.security.util
 * @run main EdDSAKeyCompatibility
 */
public class EdDSAKeyCompatibility {

    private static final String EDDSA = "EdDSA";
    private static final String ED25519 = "Ed25519";
    private static final String ED448 = "Ed448";
    private static final String PROVIDER = "SunEC";

    public static void main(String[] args) throws Exception {

        boolean result = true;
        result &= validateCert(EDDSA, PROVIDER, ED25519CERT);
        result &= validateCert(EDDSA, PROVIDER, ED448CERT);
        result &= validateCert(ED25519, PROVIDER, ED25519CERT);
        result &= validateCert(ED448, PROVIDER, ED448CERT);

        result &= validatePrivate(ED25519, PROVIDER, ED25519KEY);
        result &= validatePrivate(ED448, PROVIDER, ED448KEY);

        if (!result) {
            throw new RuntimeException("Some test cases failed");
        }
    }

    private static boolean validatePrivate(String algorithm, String provider,
            String key) {

        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm, provider);
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(
                    Base64.getMimeDecoder().decode(key));
            EdECPrivateKey privKey
                    = (EdECPrivateKey) kf.generatePrivate(privSpec);
            checkPrivKeyFormat(privKey.getEncoded());

            NamedParameterSpec namedSpec = new NamedParameterSpec(algorithm);
            EdECPrivateKeySpec edprivSpec = new EdECPrivateKeySpec(
                    namedSpec, privKey.getBytes().get());
            EdECPrivateKey privKey1
                    = (EdECPrivateKey) kf.generatePrivate(edprivSpec);
            checkPrivKeyFormat(privKey1.getEncoded());
            EdECPrivateKey privKey2 = (EdECPrivateKey) kf.translateKey(privKey);
            checkPrivKeyFormat(privKey2.getEncoded());
            equals(privKey, privKey1);
            equals(privKey, privKey2);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException
                | NoSuchProviderException | InvalidKeyException e) {
            e.printStackTrace(System.out);
            return false;
        }
        System.out.println("PASSED - validatePrivate");
        return true;
    }

    private static boolean validateCert(String algorithm, String provider,
            String certificate) {

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(
                    new ByteArrayInputStream(certificate.getBytes()));
            System.out.println(cert);
            KeyFactory kf = KeyFactory.getInstance(algorithm, provider);
            X509EncodedKeySpec pubSpec = kf.getKeySpec(
                    cert.getPublicKey(), X509EncodedKeySpec.class);
            EdECPublicKey pubKey = (EdECPublicKey) kf.generatePublic(pubSpec);
            EdECPublicKeySpec edpubSpec = kf.getKeySpec(
                    cert.getPublicKey(), EdECPublicKeySpec.class);
            EdECPublicKey pubKey1 = (EdECPublicKey) kf.generatePublic(edpubSpec);
            EdECPublicKey pubKey2 = (EdECPublicKey) kf.translateKey(pubKey);
            equals(pubKey, pubKey1);
            equals(pubKey, pubKey2);
            equals(pubKey, cert.getPublicKey());
        } catch (CertificateException | NoSuchAlgorithmException
                | InvalidKeySpecException | NoSuchProviderException
                | InvalidKeyException e) {
            e.printStackTrace(System.out);
            return false;
        }
        System.out.println("PASSED - validateCert");
        return true;
    }

    private static void checkPrivKeyFormat(byte[] key) throws IOException {

        // key value should be nested octet strings
        DerValue val = new DerValue(new ByteArrayInputStream(key));
        BigInteger version = val.data.getBigInteger();
        DerValue algId = val.data.getDerValue();
        byte[] keyValue = val.data.getOctetString();
        val = new DerValue(new ByteArrayInputStream(keyValue));
        if (val.tag != DerValue.tag_OctetString) {
            throw new RuntimeException("incorrect format");
        }
    }

    private static void equals(Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw new RuntimeException(String.format("Actual: %s, Expected: %s",
                    actual, expected));
        }
    }

    // Following EdDSA Certificates/Keys are generated through openssl_1.1.1d

    /*
     * Certificate:
     *  Data:
     *     Version: 3 (0x2)
     *     Serial Number:
     *         1d:d3:87:b9:e4:c6:9e:4a:5c:78:a8:f0:c7:9b:37:be:1e:31:dd:20
     *     Signature Algorithm: ED25519
     *     Issuer: C = US, ST = CA, L = SCA, O = test, OU = test, CN = localhost
     *     Validity
     *         Not Before: Mar  6 05:55:31 2020 GMT
     *         Not After : Mar  1 05:55:31 2040 GMT
     *     Subject: C = US, ST = CA, L = SCA, O = test, OU = test, CN = localhost
     *     Subject Public Key Info:
     *         Public Key Algorithm: ED25519
     */
    private static final String ED25519KEY
            = "MC4CAQAwBQYDK2VwBCIEIP8xGaQTMh+I+59I66AaN+B4qnY1oWGjPwbSY4r+D08f";

    private static final String ED25519CERT
            = "-----BEGIN CERTIFICATE-----\n"
            + "MIIByTCCAXugAwIBAgIUHdOHueTGnkpceKjwx5s3vh4x3SAwBQYDK2VwMFoxCzAJ\n"
            + "BgNVBAYTAlVTMQswCQYDVQQIDAJDQTEMMAoGA1UEBwwDU0NBMQ0wCwYDVQQKDAR0\n"
            + "ZXN0MQ0wCwYDVQQLDAR0ZXN0MRIwEAYDVQQDDAlsb2NhbGhvc3QwHhcNMjAwMzA2\n"
            + "MDU1NTMxWhcNNDAwMzAxMDU1NTMxWjBaMQswCQYDVQQGEwJVUzELMAkGA1UECAwC\n"
            + "Q0ExDDAKBgNVBAcMA1NDQTENMAsGA1UECgwEdGVzdDENMAsGA1UECwwEdGVzdDES\n"
            + "MBAGA1UEAwwJbG9jYWxob3N0MCowBQYDK2VwAyEAdqQ4Nduhbl+ShGeKdOryVMKy\n"
            + "1t1LjyjPyCBC+gSk0eCjUzBRMB0GA1UdDgQWBBS01/VQEzwkFNRW/esQxaB6+uId\n"
            + "8jAfBgNVHSMEGDAWgBS01/VQEzwkFNRW/esQxaB6+uId8jAPBgNVHRMBAf8EBTAD\n"
            + "AQH/MAUGAytlcANBAEJkLuNfyVws7HKqHL7oDqQkp5DSwh+bGjrr2p4zSvs5PZ8o\n"
            + "jRXWV0SMt/MB+90ubMD5tL7H7J6DR5PUFBIwGwc=\n"
            + "-----END CERTIFICATE-----";

    /*
     * Certificate:
     *  Data:
     *     Version: 3 (0x2)
     *     Serial Number:
     *         42:eb:8c:a2:a0:6f:8e:5a:a5:f8:7c:72:c1:f1:8b:7e:44:1b:37:80
     *     Signature Algorithm: ED448
     *     Issuer: C = US, ST = CA, L = SCA, O = test, OU = test, CN = localhost
     *     Validity
     *         Not Before: Mar  6 05:57:42 2020 GMT
     *         Not After : Mar  1 05:57:42 2040 GMT
     *     Subject: C = US, ST = CA, L = SCA, O = test, OU = test, CN = localhost
     *     Subject Public Key Info:
     *         Public Key Algorithm: ED448
     */
    private static final String ED448KEY
            = "MEcCAQAwBQYDK2VxBDsEOdG4lrYO0xBaf3aJWYMZ8XAxitA1zV4/ghG8wPBag8HQ"
            + "XN+3OmS8wR1KfeGQysHQr3JHco3Mwiaz8w==";

    private static final String ED448CERT
            = "-----BEGIN CERTIFICATE-----\n"
            + "MIICFDCCAZSgAwIBAgIUQuuMoqBvjlql+HxywfGLfkQbN4AwBQYDK2VxMFoxCzAJ\n"
            + "BgNVBAYTAlVTMQswCQYDVQQIDAJDQTEMMAoGA1UEBwwDU0NBMQ0wCwYDVQQKDAR0\n"
            + "ZXN0MQ0wCwYDVQQLDAR0ZXN0MRIwEAYDVQQDDAlsb2NhbGhvc3QwHhcNMjAwMzA2\n"
            + "MDU1NzQyWhcNNDAwMzAxMDU1NzQyWjBaMQswCQYDVQQGEwJVUzELMAkGA1UECAwC\n"
            + "Q0ExDDAKBgNVBAcMA1NDQTENMAsGA1UECgwEdGVzdDENMAsGA1UECwwEdGVzdDES\n"
            + "MBAGA1UEAwwJbG9jYWxob3N0MEMwBQYDK2VxAzoAfKlXpT0ymcvz2Gp+8HLzBpaz\n"
            + "5mQqMaDbGmcq8gSIdeEUtVmv4OplE+4GSnrbJnEn99LQdbanL/MAo1MwUTAdBgNV\n"
            + "HQ4EFgQUXkm9LVUkB0f/1MiPFjQPHGJ8THIwHwYDVR0jBBgwFoAUXkm9LVUkB0f/\n"
            + "1MiPFjQPHGJ8THIwDwYDVR0TAQH/BAUwAwEB/zAFBgMrZXEDcwDvE3LKCg1bTjHi\n"
            + "MI1EiMqZN3PJYVBsztecBXm3ELDlT+F0Z1H2vjaROkJc8PdpUOxyed1xDjwq3IB/\n"
            + "nYYJNVyt6Dy3d12kl77ev+YMD83OuqM6F5O6MdDUxYQu9u3NasZAU5FQ6zklWWpI\n"
            + "8jCPtOvcAQA=\n"
            + "-----END CERTIFICATE-----";
}
