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
 *
 * @bug 6720721
 * @summary CRL check with circular depency support needed
 * @run main/othervm CircularCRLOneLevel
 * @author Xuelei Fan
 */

import java.io.*;
import java.net.SocketException;
import java.util.*;
import java.security.Security;
import java.security.cert.*;
import java.security.cert.CertPathValidatorException.BasicReason;

public class CircularCRLOneLevel {

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

    static String subCaCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICUDCCAbmgAwIBAgIBAzANBgkqhkiG9w0BAQQFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA0MjcwMjI0MzRaFw0yOTAxMTIwMjI0MzRa\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCiAJnAQW2ad3ZMKUhSJVZj\n" +
        "8pBqxTcHSTwAVguQkDglsN/OIwUpvR5Jgp3lpRWUEt6idEp0FZzORpvtjt3pr5MG\n" +
        "Eg2CDptekC5BSPS+fIAIKlncB3HwOiFFhH6b3wTydDCdEd2fvsi4QMOSVrIYMeA8\n" +
        "P/mCz6kRhfUQPE0CMmOUewIDAQABo4GJMIGGMB0GA1UdDgQWBBT0/nNP8WpyxmYr\n" +
        "IBp4tN8y08jw2jBHBgNVHSMEQDA+gBSCJd0hpl5PdAD9IZS+Hzng4lXLG6EjpCEw\n" +
        "HzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUw\n" +
        "AwEB/zALBgNVHQ8EBAMCAgQwDQYJKoZIhvcNAQEEBQADgYEAS9PzI6B39R/U9fRj\n" +
        "UExzN1FXNP5awnAPtiv34kSCL6n6MryqkfG+8aaAOdZsSjmTylNFaF7cW/Xp1VBF\n" +
        "hq0bg/SbEAbK7+UwL8GSC3crhULHLbh+1iFdVTEwxCw5YmB8ji3BaZ/WKW/PkjCZ\n" +
        "7cXP6VDeZMG6oRQ4hbOcixoFPXo=\n" +
        "-----END CERTIFICATE-----";

    static String targetCertStr = subCaCertStr;

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

    static String crlStr =
        "-----BEGIN X509 CRL-----\n" +
        "MIIBGzCBhQIBATANBgkqhkiG9w0BAQQFADAfMQswCQYDVQQGEwJVUzEQMA4GA1UE\n" +
        "ChMHRXhhbXBsZRcNMDkwNDI3MDIzODA0WhcNMjgwNjI2MDIzODA0WjAiMCACAQUX\n" +
        "DTA5MDQyNzAyMzgwMFowDDAKBgNVHRUEAwoBBKAOMAwwCgYDVR0UBAMCAQIwDQYJ\n" +
        "KoZIhvcNAQEEBQADgYEAoarfzXEtw3ZDi4f9U8eSvRIipHSyxOrJC7HR/hM5VhmY\n" +
        "CErChny6x9lBVg9s57tfD/P9PSzBLusCcHwHMAbMOEcTltVVKUWZnnbumpywlYyg\n" +
        "oKLrE9+yCOkYUOpiRlz43/3vkEL5hjIKMcDSZnPKBZi1h16Yj2hPe9GMibNip54=\n" +
        "-----END X509 CRL-----";

    private static CertPath generateCertificatePath()
            throws CertificateException {
        // generate certificate from cert strings
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is;

        is = new ByteArrayInputStream(targetCertStr.getBytes());
        Certificate targetCert = cf.generateCertificate(is);

        is = new ByteArrayInputStream(selfSignedCertStr.getBytes());
        Certificate selfSignedCert = cf.generateCertificate(is);

        // generate certification path
        List<Certificate> list = Arrays.asList(new Certificate[] {
                        targetCert, selfSignedCert});

        return cf.generateCertPath(list);
    }

    private static Set<TrustAnchor> generateTrustAnchors()
            throws CertificateException {
        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is =
                    new ByteArrayInputStream(selfSignedCertStr.getBytes());
        Certificate selfSignedCert = cf.generateCertificate(is);

        // generate a trust anchor
        TrustAnchor anchor =
            new TrustAnchor((X509Certificate)selfSignedCert, null);

        return Collections.singleton(anchor);
    }

    private static CertStore generateCertificateStore() throws Exception {
        // generate CRL from CRL string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is =
                    new ByteArrayInputStream(crlStr.getBytes());

        // generate a cert store
        Collection crls = cf.generateCRLs(is);

        is = new ByteArrayInputStream(crlIssuerCertStr.getBytes());
        Collection certs = cf.generateCertificates(is);

        Collection entries = new HashSet();
        entries.addAll(crls);
        entries.addAll(certs);

        return CertStore.getInstance("Collection",
                            new CollectionCertStoreParameters(entries));
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

        // set the validation time
        params.setDate(new Date(109, 5, 1));   // 2009-05-01

        // disable OCSP checker
        Security.setProperty("ocsp.enable", "false");

        // enable CRL checker
        System.setProperty("com.sun.security.enableCRLDP", "true");

        CertPathValidator validator = CertPathValidator.getInstance("PKIX");

        try {
            validator.validate(path, params);
        } catch (CertPathValidatorException cpve) {
            if (cpve.getReason() != BasicReason.REVOKED) {
                throw new Exception(
                    "unexpect exception, should be a REVOKED CPVE", cpve);
            }
        }
    }
}
