/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 *
 * @bug 6845286
 * @summary Add regression test for name constraints
 * @author Xuelei Fan
 */

import java.io.*;
import java.net.SocketException;
import java.util.*;
import java.security.Security;
import java.security.cert.*;
import java.security.cert.CertPathValidatorException.BasicReason;

public class NameConstraintsWithUnexpectedRID {

    static String selfSignedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICTjCCAbegAwIBAgIJAIoSzC1A/k4vMA0GCSqGSIb3DQEBBQUAMB8xCzAJBgNV\n" +
        "BAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMB4XDTA5MDUwNzA5MjcxMloXDTMwMDQx\n" +
        "NzA5MjcxMlowHzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGUwgZ8wDQYJ\n" +
        "KoZIhvcNAQEBBQADgY0AMIGJAoGBANXzlv5Fn2cdgBRdEK/37/o8rqQXIRIMZqX6\n" +
        "BPuo46Cdhctv+n3hu5bj/PwgJVbAJcqcQfDudSSF5gwGlRqDX9vekPSS47XZXjOZ\n" +
        "qFcnDoWP0gSQXLYVVtjuItkecTrPyUE5v2lRIAh13MGKOSh3ZsrtFvj7Y5d9EqIP\n" +
        "SLxWWPuHAgMBAAGjgZEwgY4wHQYDVR0OBBYEFFydJvQMB2j4EDHW2bQabNsPUvDt\n" +
        "ME8GA1UdIwRIMEaAFFydJvQMB2j4EDHW2bQabNsPUvDtoSOkITAfMQswCQYDVQQG\n" +
        "EwJVUzEQMA4GA1UEChMHRXhhbXBsZYIJAIoSzC1A/k4vMA8GA1UdEwEB/wQFMAMB\n" +
        "Af8wCwYDVR0PBAQDAgIEMA0GCSqGSIb3DQEBBQUAA4GBAHgoopmZ1Q4qXhMDbbYQ\n" +
        "YCi4Cg6cXPFblx5gzhWu/6l9SkvZbAZiLszgyMq5dGj9WyTtibNEp232dQsKTFu7\n" +
        "3ag0DiFqoQ8btgvbwBlzhnRagoeVFjhuBBQutOScw7x8NCSBkZQow+31127mwu3y\n" +
        "YGYhEmI2dNmgbv1hVYTGmLXW\n" +
        "-----END CERTIFICATE-----";

    static String subCaCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICdTCCAd6gAwIBAgIJAL+MYVyy7k5YMA0GCSqGSIb3DQEBBQUAMB8xCzAJBgNV\n" +
        "BAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMB4XDTA5MDUwNzA5MjcxNFoXDTI5MDEy\n" +
        "MjA5MjcxNFowMTELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGUxEDAOBgNV\n" +
        "BAsTB0NsYXNzLTEwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAM2mwX8dhP3M\n" +
        "i6ATRsd0wco+c7rsyEbP0CRQunVIP8/kOL8+zyQix+QZquY23tvBCbia424GXDkT\n" +
        "irvK/M4yGzrdS51hA5dlH3SHY3CWOAqEPqKtNLn1My4MWtTiUWbHi0YjFuOv0BXz\n" +
        "x9lTEfMf+3QcOgO5FitcqHIMP4jIlT+lAgMBAAGjgaYwgaMwHQYDVR0OBBYEFJHg\n" +
        "eyEWcjxcAwc01BPQrau/4HJaME8GA1UdIwRIMEaAFFydJvQMB2j4EDHW2bQabNsP\n" +
        "UvDtoSOkITAfMQswCQYDVQQGEwJVUzEQMA4GA1UEChMHRXhhbXBsZYIJAIoSzC1A\n" +
        "/k4vMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgIEMBMGA1UdHgQMMAqhCDAG\n" +
        "iAQqAwQFMA0GCSqGSIb3DQEBBQUAA4GBAI3CDQWZiTlVVVqfCiZwc/yIL7G5bu2g\n" +
        "ccgVz9PyKfTpq8vk59S23TvPwdPt4ZVx4RSoar9ONtbrcLxfP3X6WQ7e9popWNZV\n" +
        "q49YfyU1tD5HFuxj7CAsvfykuRo4ovXaTCVWlTMi7fJJdzU0Eb4xkXXhiWT/RbHG\n" +
        "R7J+8ROMZ+nR\n" +
        "-----END CERTIFICATE-----";

    static String targetCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICTzCCAbigAwIBAgIJAOA8c10w019UMA0GCSqGSIb3DQEBBQUAMDExCzAJBgNV\n" +
        "BAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFzcy0xMB4XDTA5\n" +
        "MDUwNzA5NTg0OVoXDTI5MDEyMjA5NTg0OVowQTELMAkGA1UEBhMCVVMxEDAOBgNV\n" +
        "BAoTB0V4YW1wbGUxEDAOBgNVBAsTB0NsYXNzLTExDjAMBgNVBAMTBUFsaWNlMIGf\n" +
        "MA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDfekJF8IZeOe3Ff1rexVyx9yTmPSKh\n" +
        "itEW7tW9m8DgqLGDptJLmbexvUCWNkFquQW1J8sjzjqrkIk8amA2SlHQ6Z15RoxC\n" +
        "E19qi5V5ms97X3lyuJcwwtT24J5PBk9ic/V6zclsNXSj/NoqlciKMxyvRy9zWk6Z\n" +
        "W5cVDf7DTzN2cwIDAQABo18wXTALBgNVHQ8EBAMCA+gwDgYDVR0RBAcwBYgDKgME\n" +
        "MB0GA1UdDgQWBBRh8rvMhT17VI+S3pCVzTwQzVMjOTAfBgNVHSMEGDAWgBSR4Hsh\n" +
        "FnI8XAMHNNQT0K2rv+ByWjANBgkqhkiG9w0BAQUFAAOBgQCNDnJ0Jz37+SmO9uRJ\n" +
        "z5Rr15oJAKsde5LGhghHZwTTYInOwGOYAABkWRB7JhUHNjIoQg9veqObSHEgcYMh\n" +
        "ZmO3rklIxyTeoyn86KR49cdvQUoqEhx1jKrEbFBsAwSbJDw//S+wNYgMHYtcynf4\n" +
        "dcVScVdLUDeqE/3f+5yt1JPRuA==\n" +
        "-----END CERTIFICATE-----";

    private static CertPath generateCertificatePath()
            throws CertificateException {
        // generate certificate from cert strings
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is;

        is = new ByteArrayInputStream(targetCertStr.getBytes());
        Certificate targetCert = cf.generateCertificate(is);

        is = new ByteArrayInputStream(subCaCertStr.getBytes());
        Certificate subCaCert = cf.generateCertificate(is);

        is = new ByteArrayInputStream(selfSignedCertStr.getBytes());
        Certificate selfSignedCert = cf.generateCertificate(is);

        // generate certification path
        List<Certificate> list = Arrays.asList(new Certificate[] {
                        targetCert, subCaCert, selfSignedCert});

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

    public static void main(String args[]) throws Exception {
        CertPath path = generateCertificatePath();
        Set<TrustAnchor> anchors = generateTrustAnchors();

        PKIXParameters params = new PKIXParameters(anchors);

        // disable certificate revocation checking
        params.setRevocationEnabled(false);

        // set the validation time
        params.setDate(new Date(109, 5, 8));   // 2009-05-01

        // disable OCSP checker
        Security.setProperty("ocsp.enable", "false");

        // disable CRL checker
        System.setProperty("com.sun.security.enableCRLDP", "false");

        CertPathValidator validator = CertPathValidator.getInstance("PKIX");

        try {
            validator.validate(path, params);
            throw new Exception("Should thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException uoe) {
            // that is the expected exception.
        }
    }
}
