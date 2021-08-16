/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7026347
 * @summary Certificate should have
 *          verify(PublicKey key, Provider sigProvider)
 */

import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.*;

public class Verify {

    static String selfSignedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICPjCCAaegAwIBAgIBADANBgkqhkiG9w0BAQQFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA0MjcwMjI0MzJaFw0zMDA0MDcwMjI0MzJa\n" +
        "MB8xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMIGfMA0GCSqGSIb3DQEB\n" +
        "AQUAA4GNADCBiQKBgQC4OTag24sTxL2tXTNuvpmUEtdxrYAZoFsslFQ60T+WD9wQ\n" +
        "Jeiw87FSPsR2vxRuv0j8DNm2a4h7LNNIFcLurfNldbz5pvgZ7VqdbbUMPE9qP85n\n" +
        "jgDl4woyRTSUeRI4A7O0CO6NpES21dtbdhroWQrEkHxpnrDPxsxrz5gf2m3gqwID\n" +
        "AQABo4GJMIGGMB0GA1UdDgQWBBSCJd0hpl5PdAD9IZS+Hzng4lXLGzBHBgNVHSME\n" +
        "QDA+gBSCJd0hpl5PdAD9IZS+Hzng4lXLG6EjpCEwHzELMAkGA1UEBhMCVVMxEDAO\n" +
        "BgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUwAwEB/zALBgNVHQ8EBAMCAgQw\n" +
        "DQYJKoZIhvcNAQEEBQADgYEAluy6HIjWcq009lTLmhp+Np6dxU78pInBK8RZkza0\n" +
        "484qGaxFGD3UGyZkI5uWmsH2XuMbuox5khfIq6781gmkPBHXBIEtJN8eLusOHEye\n" +
        "iE8h7WI+N3qa6Pj56WionMrioqC/3X+b06o147bbhx8U0vkYv/HyPaITOFfMXTdz\n" +
        "Vjw=\n" +
        "-----END CERTIFICATE-----";

    static String crlIssuerCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICKzCCAZSgAwIBAgIBAjANBgkqhkiG9w0BAQQFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA0MjcwMjI0MzNaFw0yOTAxMTIwMjI0MzNa\n" +
        "MB8xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMIGfMA0GCSqGSIb3DQEB\n" +
        "AQUAA4GNADCBiQKBgQDMJeBMBybHykI/YpwUJ4O9euqDSLb1kpWpceBS8TVqvgBC\n" +
        "SgUJWtFZL0i6bdvF6mMdlbuBkGzhXqHiVAi96/zRLbUC9F8SMEJ6MuD+YhQ0ZFTQ\n" +
        "atKy8zf8O9XzztelLJ26Gqb7QPV133WY3haAqHtCXOhEKkCN16NOYNC37DTaJwID\n" +
        "AQABo3cwdTAdBgNVHQ4EFgQULXSWzXzUOIpOJpzbSCpW42IJUugwRwYDVR0jBEAw\n" +
        "PoAUgiXdIaZeT3QA/SGUvh854OJVyxuhI6QhMB8xCzAJBgNVBAYTAlVTMRAwDgYD\n" +
        "VQQKEwdFeGFtcGxlggEAMAsGA1UdDwQEAwIBAjANBgkqhkiG9w0BAQQFAAOBgQAY\n" +
        "eMnf5AHSNlyUlzXk8o2S0h4gCuvKX6C3kFfKuZcWvFAbx4yQOWLS2s15/nzR4+AP\n" +
        "FGX3lgJjROyAh7fGedTQK+NFWwkM2ag1g3hXktnlnT1qHohi0w31nVBJxXEDO/Ck\n" +
        "uJTpJGt8XxxbFaw5v7cHy7XuTAeU/sekvjEiNHW00Q==\n" +
        "-----END CERTIFICATE-----";

    private static X509Certificate cert;
    private static PublicKey selfSignedCertPubKey;
    private static PublicKey crlIssuerCertPubKey;

    public static void main(String[] args) throws Exception {
        setup();

        /*
         * Verify certificate with its own public key.
         * Should pass.
         */
        verifyCert(selfSignedCertPubKey,"SunRsaSign");

        /*
         * Try to verify certificate with a provider that does not have a
         * Signature implementation.
         * Should fail with NoSuchAlgorithmException.
         */
        try {
            verifyCert(selfSignedCertPubKey, "SunJCE");
            throw new RuntimeException("Didn't catch the exception properly");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Caught the correct exception.");
        }

        /*
         * Try to verify certificate with a provider that has a Signature
         * implementation but not of the right algorithm (MD5withRSA).
         * Should fail with NoSuchAlgorithmException.
         */
        try {
            verifyCert(selfSignedCertPubKey, "SUN");
            throw new RuntimeException("Didn't catch the exception properly");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Caught the correct exception.");
        }

        /*
         * Try to verify certificate with the wrong public key.
         * Should fail with SignatureException.
         */
        try {
            verifyCert(crlIssuerCertPubKey, "SunRsaSign");
            throw new RuntimeException("Didn't catch the exception properly");
        } catch (SignatureException e) {
            System.out.println("Caught the correct exception.");
        }
    }

    private static void setup() throws CertificateException, CRLException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        /* Get public key of the CRL issuer cert */
        ByteArrayInputStream inputStream
                = new ByteArrayInputStream(crlIssuerCertStr.getBytes());
        cert = (X509Certificate)cf.generateCertificate(inputStream);
        crlIssuerCertPubKey = cert.getPublicKey();

        /* Get public key of the self-signed Cert */
        inputStream = new ByteArrayInputStream(selfSignedCertStr.getBytes());
        selfSignedCertPubKey = cf.generateCertificate(inputStream).getPublicKey();
    }

    private static void verifyCert(PublicKey key, String providerName)
            throws CertificateException, NoSuchAlgorithmException,
            InvalidKeyException, SignatureException {
        Provider provider = Security.getProvider(providerName);
        if (provider == null) {
            throw new RuntimeException("Provider " + providerName
                                                   + " not found.");
        }
        cert.verify(key, provider);
    }
}
