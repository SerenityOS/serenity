/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

// This test case relies on updated static security property, no way to re-use
// security property in samevm/agentvm mode.

/**
 * @test
 *
 * @bug 6861062 7011497
 * @summary Disable MD2 support
 *          new CertPathValidatorException.BasicReason enum constant for
 *     constrained algorithm
 * @run main/othervm CPValidatorTrustAnchor
 * @author Xuelei Fan
 */

import java.io.*;
import java.net.SocketException;
import java.util.*;
import java.security.Security;
import java.security.cert.*;
import java.security.cert.CertPathValidatorException.*;

public class CPValidatorTrustAnchor {

    static String selfSignedCertStr = null;

    // SHA1withRSA 1024
    static String trustAnchor_SHA1withRSA_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICPjCCAaegAwIBAgIBADANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDRaFw0zMDA3MTcwMTExNDRa\n" +
        "MB8xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMIGfMA0GCSqGSIb3DQEB\n" +
        "AQUAA4GNADCBiQKBgQC8UdC863pFk1Rvd7xUYd60+e9KsLhb6SqOfU42ZA715FcH\n" +
        "E1TRvQPmYzAnHcO04TrWZQtO6E+E2RCmeBnetBvIMVka688QkO14wnrIrf2tRodd\n" +
        "rZNZEBzkX+zyXCRo9tKEUDFf9Qze7Ilbb+Zzm9CUfu4M1Oz6iQcXRx7aM0jEAQID\n" +
        "AQABo4GJMIGGMB0GA1UdDgQWBBTn0C+xmZY/BTab4W9gBp3dGa7WgjBHBgNVHSME\n" +
        "QDA+gBTn0C+xmZY/BTab4W9gBp3dGa7WgqEjpCEwHzELMAkGA1UEBhMCVVMxEDAO\n" +
        "BgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUwAwEB/zALBgNVHQ8EBAMCAgQw\n" +
        "DQYJKoZIhvcNAQEFBQADgYEAiCXL2Yp4ruyRXAIJ8zBEaPC9oV2agqgbSbly2z8z\n" +
        "Ik5SeSRysP+GHBpb8uNyANJnQKv+T0GrJiTLMBjKCOiJl6xzk3EZ2wbQB6G/SQ9+\n" +
        "UWcsXSC8oGSEPpkj5In/9/UbuUIfT9H8jmdyLNKQvlqgq6kyfnskME7ptGgT95Hc\n" +
        "tas=\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 512
    static String trustAnchor_SHA1withRSA_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIBuTCCAWOgAwIBAgIBADANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDRaFw0zMDA3MTcwMTExNDRa\n" +
        "MB8xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMFwwDQYJKoZIhvcNAQEB\n" +
        "BQADSwAwSAJBAM0Kn4ieCdCHsrm78ZMMN4jQEEEqACAMKB7O8j9g4gfz2oAfmHwv\n" +
        "7JH/hZ0Xen1zUmBbwe+e2J5D/4Fisp9Bn98CAwEAAaOBiTCBhjAdBgNVHQ4EFgQU\n" +
        "g4Kwd47hdNQBp8grZsRJ5XvhvxAwRwYDVR0jBEAwPoAUg4Kwd47hdNQBp8grZsRJ\n" +
        "5XvhvxChI6QhMB8xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlggEAMA8G\n" +
        "A1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgIEMA0GCSqGSIb3DQEBBQUAA0EAn77b\n" +
        "FJx+HvyRvjZYCzMjnUct3Ql4iLOkURYDh93J5TXi/l9ajvAMEuwzYj0qZ+Ktm/ia\n" +
        "U5r+8B9nzx+j2Zh3kw==\n" +
        "-----END CERTIFICATE-----";

    // MD2withRSA 2048
    static String trustAnchor_MD2withRSA_2048 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDQzCCAiugAwIBAgIBADANBgkqhkiG9w0BAQIFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDdaFw0zMDA3MTcwMTExNDda\n" +
        "MB8xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMIIBIjANBgkqhkiG9w0B\n" +
        "AQEFAAOCAQ8AMIIBCgKCAQEArF5pINc5s+aUlmdYlxtAQ3V4TXFnP/XOYHxjfLuX\n" +
        "eKO/kh78LMvbDisTPQ2yo9YEawwwbUU40xcuzgi0axXgKveHXYdUmTr0hEapq3rv\n" +
        "g/q2EbOjyXvq4qK2RDoVCN8R3wXiytnY2OFALTx6zc2tW4imJ20svdNVtWhv2syj\n" +
        "ZTmmRXAeFUbD4qKWAFij0I6pnSgVssvWzeyJUNemym+oiYyaSd7n5j1RNAqUKioo\n" +
        "K/T0FOOiuPGMqottgx5YRHa6yapCP5QVWRQ+WBIYJY3Wyq7N+Es20LT6761Pk3to\n" +
        "EFCzM7+zqT/c+pC079HOKXz+m2us+HKp5BKWNnbvgaYPOQIDAQABo4GJMIGGMB0G\n" +
        "A1UdDgQWBBSrSukJf+mO5LTRasAGD9RRs7SASTBHBgNVHSMEQDA+gBSrSukJf+mO\n" +
        "5LTRasAGD9RRs7SASaEjpCEwHzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1w\n" +
        "bGWCAQAwDwYDVR0TAQH/BAUwAwEB/zALBgNVHQ8EBAMCAgQwDQYJKoZIhvcNAQEC\n" +
        "BQADggEBAHvsv+DqMJeIW/D+ltkhw37OdMzkMPp4E6Hbp03O3GZ5LfNGczHCb2uL\n" +
        "sr5T7e/jaBFn6QfmqbOAYAHJSNq2bNNtTbatnHBLuVx13cfxmwk89Cg/tFeoUdcf\n" +
        "m5hzurB6Ub6SsYMOxZHUYp/KxM9x9a7llC1bK3SKXwd4rVDlXh8DOBvdQNr5Q3yq\n" +
        "JjY86bSXO14VzNxL/1rqHiszQdPyR/28SBsQVYSi0Zeyc4Yy1ui/cXu1+PWYw3YZ\n" +
        "QUPHTnkVdPGwRiUqeZIcps+q+ePlQQmDu5qiLD6d8gsyGyY/RvCHWKO5Y9DuX9hs\n" +
        "he/AhCWQx+TQYGLu0liQqLkGZydyRnA=\n" +
        "-----END CERTIFICATE-----";

    private static CertPath generateCertificatePath()
            throws CertificateException {
        // generate certificate from cert strings
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is;

        is = new ByteArrayInputStream(selfSignedCertStr.getBytes());
        Certificate selfSignedCert = cf.generateCertificate(is);

        // generate certification path
        List<Certificate> list = Arrays.asList(new Certificate[] {
                        selfSignedCert});

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
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.certpath.disabledAlgorithms", "MD2");

        try {
            validate(trustAnchor_SHA1withRSA_1024);
            validate(trustAnchor_SHA1withRSA_512);
        } catch (CertPathValidatorException cpve) {
            throw new Exception(
                "unexpect exception, it is valid cert", cpve);
        }

        try {
            validate(trustAnchor_MD2withRSA_2048);
            throw new Exception("expected algorithm disabled exception");
        } catch (CertPathValidatorException cpve) {
            // we may get ClassCastException here
            BasicReason reason = (BasicReason)cpve.getReason();
            if (reason != BasicReason.ALGORITHM_CONSTRAINED) {
                throw new Exception(
                    "Expect to get ALGORITHM_CONSTRAINED CPVE", cpve);
            }

            System.out.println("Get the expected exception " + cpve);
        }
    }

    private static void validate(String trustAnchor)
            throws CertPathValidatorException, Exception {
        selfSignedCertStr = trustAnchor;

        CertPath path = generateCertificatePath();
        Set<TrustAnchor> anchors = generateTrustAnchors();

        PKIXParameters params = new PKIXParameters(anchors);

        // disable certificate revocation checking
        params.setRevocationEnabled(false);

        // set the validation time
        params.setDate(new Date(109, 9, 1));   // 2009-09-01

        CertPathValidator validator = CertPathValidator.getInstance("PKIX");

        validator.validate(path, params);
    }

}
