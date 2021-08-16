/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8225745
 * @summary Ensure ECDSA certificates with signature algorithm parameters
 *          can be verified successfully
 * @run main ECSigParamsVerifyWithCert
 */
import java.io.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class ECSigParamsVerifyWithCert {

    // ECDSA certificate with non-null signature parameters, i.e.
    // Signature Algorithm: SHA256withECDSA, params unparsed,
    // OID = 1.2.840.10045.4.3.2
    private static String ecEntityWithSigParamsStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICXjCCAfmgAwIBAgIIHzREzASpiTowFAYIKoZIzj0EAwIGCCqGSM49AwEHMGAx\n" +
        "IzAhBgNVBAMMGkNvcmRhIE5vZGUgSW50ZXJtZWRpYXRlIENBMQswCQYDVQQKDAJS\n" +
        "MzEOMAwGA1UECwwFY29yZGExDzANBgNVBAcMBkxvbmRvbjELMAkGA1UEBhMCVUsw\n" +
        "HhcNMTgwNjI1MDAwMDAwWhcNMjcwNTIwMDAwMDAwWjAxMQswCQYDVQQGEwJHQjEP\n" +
        "MA0GA1UEBwwGTG9uZG9uMREwDwYDVQQKDAhNZWdhQ29ycDBZMBMGByqGSM49AgEG\n" +
        "CCqGSM49AwEHA0IABG2VjWPPFnGVka3G9++Sz/GPRkAkht4BDoYTlkRz8hpwr4iu\n" +
        "fU6NlReirLOB4LBLZcmp16xm4RYsN5ouTS7Z3wKjgcEwgb4wHQYDVR0OBBYEFBnY\n" +
        "sikYpaSL9U8FUygbqN3sIvMOMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgGG\n" +
        "MCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADARBgorBgEEAYOK\n" +
        "YgEBBAMCAQQwRwYDVR0eAQH/BD0wO6A3MDWkMzAxMQswCQYDVQQGEwJHQjEPMA0G\n" +
        "A1UEBwwGTG9uZG9uMREwDwYDVQQKDAhNZWdhQ29ycKEAMBQGCCqGSM49BAMCBggq\n" +
        "hkjOPQMBBwNJADBGAiEAos+QzgwwH2hfOtrlLncHnoT2YXXHP4q5h01T2DRmjcMC\n" +
        "IQDa3xZz7CkyyNO1+paAthiNVIlGwwnl4UxuYMwkAiWACw==\n" +
        "-----END CERTIFICATE-----\n";

    // ECDSA certificate with only signature algorithm oid, no parameters, i.e.
    // Signature Algorithm: SHA256withECDSA, OID = 1.2.840.10045.4.3.2
    private static String ecSigner =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICETCCAbigAwIBAgIIaHr3YTnjT8YwCgYIKoZIzj0EAwIwWDEbMBkGA1UEAwwS\n" +
        "Q29yZGEgTm9kZSBSb290IENBMQswCQYDVQQKDAJSMzEOMAwGA1UECwwFY29yZGEx\n" +
        "DzANBgNVBAcMBkxvbmRvbjELMAkGA1UEBhMCVUswHhcNMTcwNTIyMDAwMDAwWhcN\n" +
        "MjcwNTIwMDAwMDAwWjBgMSMwIQYDVQQDDBpDb3JkYSBOb2RlIEludGVybWVkaWF0\n" +
        "ZSBDQTELMAkGA1UECgwCUjMxDjAMBgNVBAsMBWNvcmRhMQ8wDQYDVQQHDAZMb25k\n" +
        "b24xCzAJBgNVBAYTAlVLMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEA8veoCbh\n" +
        "ZmazlyIFWjExBd8ru5OYdFW9Z9ZD5BVg/dswdKC4dlHMHe/sQ4TxFmkYNqf7DTTt\n" +
        "ePtdHT7Eb1LGYKNkMGIwHQYDVR0OBBYEFOvuLjAVKUCuGZge2G/jfX8HosITMAsG\n" +
        "A1UdDwQEAwIBhjAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAw\n" +
        "DwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNHADBEAiB6wr47tuC71qi6+FbY\n" +
        "XYDTvK+QmAi5ywkFc95I9fPLaQIgIM+nNNQ50NwK610h3bG37XC2tGu+A7Dhtt2Q\n" +
        "4nDqu30=\n" +
        "-----END CERTIFICATE-----\n";

    public static void main(String[] args) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream is
                = new ByteArrayInputStream(ecEntityWithSigParamsStr.getBytes());
        X509Certificate ecEntityWithSigParams = (X509Certificate)certFactory.generateCertificate(is);
        is = new ByteArrayInputStream(ecSigner.getBytes());
        X509Certificate ecSigner = (X509Certificate)certFactory.generateCertificate(is);

        try {
            ecEntityWithSigParams.verify(ecSigner.getPublicKey());
            System.out.println("Test Passed: EC Cert verified");
        } catch (Exception e) {
            System.out.println("Failed, cannot verify EC certificate with sig params");
            throw e;
        }
    }
}
