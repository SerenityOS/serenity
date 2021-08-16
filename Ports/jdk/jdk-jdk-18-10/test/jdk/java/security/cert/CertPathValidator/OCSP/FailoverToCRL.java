/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
// Security properties, once set, cannot revert to unset.  To avoid
// conflicts with tests running in the same VM isolate this test by
// running it in otherVM mode.
//

/**
 * @test
 * @bug 6383095
 * @summary CRL revoked certificate failures masked by OCSP failures
 * @run main/othervm FailoverToCRL
 * @author Xuelei Fan
 */

/*
 * Note that the certificate validity is from Mar 16 14:55:35 2009 GMT to
 * Dec 1 14:55:35 2028 GMT, please update it with newer certificate if
 * expires.
 */

/*
 * Certificates used in the test.
 *
 * end entity certificate:
 * Data:
 *     Version: 3 (0x2)
 *     Serial Number: 25 (0x19)
 *     Signature Algorithm: md5WithRSAEncryption
 *     Issuer: C=US, ST=Some-State, L=Some-City, O=Some-Org
 *     Validity
 *         Not Before: Mar 16 14:55:35 2009 GMT
 *         Not After : Dec  1 14:55:35 2028 GMT
 *     Subject: C=US, ST=Some-State, L=Some-City, O=Some-Org, OU=SSL-Client,
 *              CN=localhost
 *     Subject Public Key Info:
 *         Public Key Algorithm: rsaEncryption
 *         RSA Public Key: (1024 bit)
 *             Modulus (1024 bit):
 *                 00:bb:f0:40:36:ac:26:54:4e:f4:a3:5a:00:2f:69:
 *                 21:6f:b9:7a:3a:93:ec:a2:f6:e1:8e:c7:63:d8:2f:
 *                 12:30:99:2e:b0:f2:8f:f8:27:2d:24:78:28:84:f7:
 *                 01:bf:8d:44:79:dd:3b:d2:55:f3:ce:3c:b2:5b:21:
 *                 7d:ef:fd:33:4a:b1:a3:ff:c6:c8:9b:b9:0f:7c:41:
 *                 35:97:f9:db:3a:05:60:05:15:af:59:17:92:a3:10:
 *                 ad:16:1c:e4:07:53:af:a8:76:a2:56:2a:92:d3:f9:
 *                 28:e0:78:cf:5e:1f:48:ab:5c:19:dd:e1:67:43:ba:
 *                 75:8d:f5:82:ac:43:92:44:1b
 *             Exponent: 65537 (0x10001)
 *     X509v3 extensions:
 *         X509v3 Basic Constraints:
 *             CA:FALSE
 *         X509v3 Key Usage:
 *             Digital Signature, Non Repudiation, Key Encipherment
 *         X509v3 Subject Key Identifier:
 *             CD:BB:C8:85:AA:91:BD:FD:1D:BE:CD:67:7C:FF:B3:E9:4C:A8:22:E6
 *         X509v3 Authority Key Identifier:
 *             keyid:FA:B9:51:BF:4C:E7:D9:86:98:33:F9:E7:CB:1E:F1:33:49:F7:A8:14
 * Signature Algorithm: md5WithRSAEncryption
 *
 *
 * trusted certificate authority:
 * Data:
 *     Version: 3 (0x2)
 *     Serial Number: 0 (0x0)
 *     Signature Algorithm: md5WithRSAEncryption
 *     Issuer: C=US, ST=Some-State, L=Some-City, O=Some-Org
 *     Validity
 *         Not Before: Dec  8 02:43:36 2008 GMT
 *         Not After : Aug 25 02:43:36 2028 GMT
 *     Subject: C=US, ST=Some-State, L=Some-City, O=Some-Org
 *     Subject Public Key Info:
 *         Public Key Algorithm: rsaEncryption
 *         RSA Public Key: (1024 bit)
 *             Modulus (1024 bit):
 *                 00:cb:c4:38:20:07:be:88:a7:93:b0:a1:43:51:2d:
 *                 d7:8e:85:af:54:dd:ad:a2:7b:23:5b:cf:99:13:53:
 *                 99:45:7d:ee:6d:ba:2d:bf:e3:ad:6e:3d:9f:1a:f9:
 *                 03:97:e0:17:55:ae:11:26:57:de:01:29:8e:05:3f:
 *                 21:f7:e7:36:e8:2e:37:d7:48:ac:53:d6:60:0e:c7:
 *                 50:6d:f6:c5:85:f7:8b:a6:c5:91:35:72:3c:94:ee:
 *                 f1:17:f0:71:e3:ec:1b:ce:ca:4e:40:42:b0:6d:ee:
 *                 6a:0e:d6:e5:ad:3c:0f:c9:ba:82:4f:78:f8:89:97:
 *                 89:2a:95:12:4c:d8:09:2a:e9
 *             Exponent: 65537 (0x10001)
 *     X509v3 extensions:
 *         X509v3 Subject Key Identifier:
 *             FA:B9:51:BF:4C:E7:D9:86:98:33:F9:E7:CB:1E:F1:33:49:F7:A8:14
 *         X509v3 Authority Key Identifier:
 *             keyid:FA:B9:51:BF:4C:E7:D9:86:98:33:F9:E7:CB:1E:F1:33:49:F7:A8:14
 *             DirName:/C=US/ST=Some-State/L=Some-City/O=Some-Org
 *         X509v3 Basic Constraints:
 *             CA:TRUE
 * Signature Algorithm: md5WithRSAEncryption
 *
 * CRL:
 * Certificate Revocation List (CRL):
 *    Version 2 (0x1)
 *    Signature Algorithm: md5WithRSAEncryption
 *    Issuer: /C=US/ST=Some-State/L=Some-City/O=Some-Org
 *    Last Update: Mar 16 16:27:14 2009 GMT
 *    Next Update: May 15 16:27:14 2028 GMT
 *    CRL extensions:
 *       X509v3 CRL Number:
 *              2
 * Revoked Certificates:
 *    Serial Number: 19
 *        Revocation Date: Mar 16 16:22:08 2009 GMT
 *        CRL entry extensions:
 *            X509v3 CRL Reason Code:
 *                Superseded
 *    Signature Algorithm: md5WithRSAEncryption
 */

import java.io.*;
import java.net.SocketException;
import java.util.*;
import java.security.Security;
import java.security.cert.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPathValidatorException.BasicReason;

public class FailoverToCRL {

    static String trusedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICrDCCAhWgAwIBAgIBADANBgkqhkiG9w0BAQQFADBJMQswCQYDVQQGEwJVUzET\n" +
        "MBEGA1UECBMKU29tZS1TdGF0ZTESMBAGA1UEBxMJU29tZS1DaXR5MREwDwYDVQQK\n" +
        "EwhTb21lLU9yZzAeFw0wODEyMDgwMjQzMzZaFw0yODA4MjUwMjQzMzZaMEkxCzAJ\n" +
        "BgNVBAYTAlVTMRMwEQYDVQQIEwpTb21lLVN0YXRlMRIwEAYDVQQHEwlTb21lLUNp\n" +
        "dHkxETAPBgNVBAoTCFNvbWUtT3JnMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n" +
        "gQDLxDggB76Ip5OwoUNRLdeOha9U3a2ieyNbz5kTU5lFfe5tui2/461uPZ8a+QOX\n" +
        "4BdVrhEmV94BKY4FPyH35zboLjfXSKxT1mAOx1Bt9sWF94umxZE1cjyU7vEX8HHj\n" +
        "7BvOyk5AQrBt7moO1uWtPA/JuoJPePiJl4kqlRJM2Akq6QIDAQABo4GjMIGgMB0G\n" +
        "A1UdDgQWBBT6uVG/TOfZhpgz+efLHvEzSfeoFDBxBgNVHSMEajBogBT6uVG/TOfZ\n" +
        "hpgz+efLHvEzSfeoFKFNpEswSTELMAkGA1UEBhMCVVMxEzARBgNVBAgTClNvbWUt\n" +
        "U3RhdGUxEjAQBgNVBAcTCVNvbWUtQ2l0eTERMA8GA1UEChMIU29tZS1PcmeCAQAw\n" +
        "DAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQQFAAOBgQBcIm534U123Hz+rtyYO5uA\n" +
        "ofd81G6FnTfEAV8Kw9fGyyEbQZclBv34A9JsFKeMvU4OFIaixD7nLZ/NZ+IWbhmZ\n" +
        "LovmJXyCkOufea73pNiZ+f/4/ScZaIlM/PRycQSqbFNd4j9Wott+08qxHPLpsf3P\n" +
        "6Mvf0r1PNTY2hwTJLJmKtg==\n" +
        "-----END CERTIFICATE-----";

    static String targetCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICizCCAfSgAwIBAgIBGTANBgkqhkiG9w0BAQQFADBJMQswCQYDVQQGEwJVUzET\n" +
        "MBEGA1UECBMKU29tZS1TdGF0ZTESMBAGA1UEBxMJU29tZS1DaXR5MREwDwYDVQQK\n" +
        "EwhTb21lLU9yZzAeFw0wOTAzMTYxNDU1MzVaFw0yODEyMDExNDU1MzVaMHIxCzAJ\n" +
        "BgNVBAYTAlVTMRMwEQYDVQQIEwpTb21lLVN0YXRlMRIwEAYDVQQHEwlTb21lLUNp\n" +
        "dHkxETAPBgNVBAoTCFNvbWUtT3JnMRMwEQYDVQQLEwpTU0wtQ2xpZW50MRIwEAYD\n" +
        "VQQDEwlsb2NhbGhvc3QwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALvwQDas\n" +
        "JlRO9KNaAC9pIW+5ejqT7KL24Y7HY9gvEjCZLrDyj/gnLSR4KIT3Ab+NRHndO9JV\n" +
        "8848slshfe/9M0qxo//GyJu5D3xBNZf52zoFYAUVr1kXkqMQrRYc5AdTr6h2olYq\n" +
        "ktP5KOB4z14fSKtcGd3hZ0O6dY31gqxDkkQbAgMBAAGjWjBYMAkGA1UdEwQCMAAw\n" +
        "CwYDVR0PBAQDAgXgMB0GA1UdDgQWBBTNu8iFqpG9/R2+zWd8/7PpTKgi5jAfBgNV\n" +
        "HSMEGDAWgBT6uVG/TOfZhpgz+efLHvEzSfeoFDANBgkqhkiG9w0BAQQFAAOBgQBv\n" +
        "p7JjCDOrMBNun46xs4Gz7Y4ygM5VHaFP0oO7369twvRSu0pCuIdZd5OIMPFeRqQw\n" +
        "PA68ZdhYVR0pG5W7isV+jB+Dfge/IOgOA85sZ/6FlP3PBRW+YMQKKdRr5So3ook9\n" +
        "PimQ7rbxRAofPECv20IUKFBbOUkU+gFcn+WbTKYxBw==\n" +
        "-----END CERTIFICATE-----";

    static String crlStr =
        "-----BEGIN X509 CRL-----\n" +
        "MIIBRTCBrwIBATANBgkqhkiG9w0BAQQFADBJMQswCQYDVQQGEwJVUzETMBEGA1UE\n" +
        "CBMKU29tZS1TdGF0ZTESMBAGA1UEBxMJU29tZS1DaXR5MREwDwYDVQQKEwhTb21l\n" +
        "LU9yZxcNMDkwMzE2MTYyNzE0WhcNMjgwNTE1MTYyNzE0WjAiMCACARkXDTA5MDMx\n" +
        "NjE2MjIwOFowDDAKBgNVHRUEAwoBBKAOMAwwCgYDVR0UBAMCAQIwDQYJKoZIhvcN\n" +
        "AQEEBQADgYEAMixJI9vBwYpOGosn46+T/MTEtlm2S5pIVT/xPDrHkCPfw8l4Zrgp\n" +
        "dGPuUkglWdrGdxY9MNRUj2YFNfdZi6zZ7JF6XbkDHYOAKYgPDJRjS/0VcBntn5RJ\n" +
        "sQfZsBqc9fFSP8gknRRn3LT41kr9xNRxTT1t3YYjv7J3zkMYyInqeUA=\n" +
        "-----END X509 CRL-----";


    private static CertPath generateCertificatePath()
            throws CertificateException {
        // generate certificate from cert strings
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is =
                new ByteArrayInputStream(targetCertStr.getBytes());
        Certificate targetCert = cf.generateCertificate(is);

        // generate certification path
        List<Certificate> list = Arrays.asList(new Certificate[] {targetCert});

        return cf.generateCertPath(list);
    }

    private static Set<TrustAnchor> generateTrustAnchors()
            throws CertificateException {
        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is =
                    new ByteArrayInputStream(trusedCertStr.getBytes());
        Certificate trusedCert = cf.generateCertificate(is);

        // generate a trust anchor
        TrustAnchor anchor = new TrustAnchor((X509Certificate)trusedCert, null);

        return Collections.singleton(anchor);
    }

    private static CertStore generateCertificateStore() throws Exception {
        // generate CRL from CRL string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is =
                    new ByteArrayInputStream(crlStr.getBytes());

        // generate a cert store
        Collection<? extends CRL> crls = cf.generateCRLs(is);
        return CertStore.getInstance("Collection",
                            new CollectionCertStoreParameters(crls));
    }

    public static void main(String args[]) throws Exception {
        // MD5 is used in this test case, don't disable MD5 algorithm.
        Security.setProperty(
                "jdk.certpath.disabledAlgorithms", "MD2, RSA keySize < 1024");

        CertPath path = generateCertificatePath();
        Set<TrustAnchor> anchors = generateTrustAnchors();
        CertStore crls = generateCertificateStore();

        PKIXParameters params = new PKIXParameters(anchors);

        // add the CRL store
        params.addCertStore(crls);

        // Activate certificate revocation checking
        params.setRevocationEnabled(true);

        // Activate OCSP
        Security.setProperty("ocsp.enable", "true");
        System.setProperty("com.sun.security.enableCRLDP", "true");

        // Ensure that the ocsp.responderURL property is not set.
        if (Security.getProperty("ocsp.responderURL") != null) {
            throw new
                Exception("The ocsp.responderURL property must not be set");
        }

        CertPathValidator validator = CertPathValidator.getInstance("PKIX");

        try {
            validator.validate(path, params);
        } catch (CertPathValidatorException cpve) {
            if (cpve.getReason() != BasicReason.REVOKED) {
                throw new Exception(
                    "unexpected exception, should be a REVOKED CPVE", cpve);
            }
        }
    }
}
