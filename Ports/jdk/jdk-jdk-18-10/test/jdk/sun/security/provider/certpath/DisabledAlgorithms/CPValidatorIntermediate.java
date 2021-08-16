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
 * @run main/othervm CPValidatorIntermediate
 * @author Xuelei Fan
 */

import java.io.*;
import java.net.SocketException;
import java.util.*;
import java.security.Security;
import java.security.cert.*;
import java.security.cert.CertPathValidatorException.*;

public class CPValidatorIntermediate {

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

    // SHA1withRSA 1024 signed with RSA 1024
    static String intermediate_SHA1withRSA_1024_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICUDCCAbmgAwIBAgIBAjANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDhaFw0yOTA0MjMwMTExNDha\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCVOqnlZspyAEr90ELFaUo8\n" +
        "BF0O2Kn0yTdUeyiLOth4RA3qxWrjxJq45VmEBjZpEzPHfnp3PhnfmLcLfhoPONFg\n" +
        "bcHzlkj75ZaKCgHoyV456fMBmj348fcoUkH2WdSQ82pmxHOiHqquYNUSTimFIq82\n" +
        "AayhbKqDmhfx5lJdYNqd5QIDAQABo4GJMIGGMB0GA1UdDgQWBBTfWD9mRTppcUAl\n" +
        "UqGuu/R5t8CB5jBHBgNVHSMEQDA+gBTn0C+xmZY/BTab4W9gBp3dGa7WgqEjpCEw\n" +
        "HzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUw\n" +
        "AwEB/zALBgNVHQ8EBAMCAgQwDQYJKoZIhvcNAQEFBQADgYEAHze3wAcIe84zNOoN\n" +
        "P8l9EmlVVoU30z3LB3hxq3m/dC/4gE5Z9Z8EG1wJw4qaxlTZ4dif12nbTTdofVhb\n" +
        "Bd4syjo6fcUA4q7sfg9TFpoHQ+Ap7PgjK99moMKdMy50Xy8s6FPvaVkF89s66Z6y\n" +
        "e4q7TSwe6QevGOZaL5N/iy2XGEs=\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 1024 signed with RSA 512
    static String intermediate_SHA1withRSA_1024_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICDzCCAbmgAwIBAgIBAzANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDlaFw0yOTA0MjMwMTExNDla\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCVOqnlZspyAEr90ELFaUo8\n" +
        "BF0O2Kn0yTdUeyiLOth4RA3qxWrjxJq45VmEBjZpEzPHfnp3PhnfmLcLfhoPONFg\n" +
        "bcHzlkj75ZaKCgHoyV456fMBmj348fcoUkH2WdSQ82pmxHOiHqquYNUSTimFIq82\n" +
        "AayhbKqDmhfx5lJdYNqd5QIDAQABo4GJMIGGMB0GA1UdDgQWBBTfWD9mRTppcUAl\n" +
        "UqGuu/R5t8CB5jBHBgNVHSMEQDA+gBSDgrB3juF01AGnyCtmxEnle+G/EKEjpCEw\n" +
        "HzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUw\n" +
        "AwEB/zALBgNVHQ8EBAMCAgQwDQYJKoZIhvcNAQEFBQADQQCYNmdkONfuk07XjRze\n" +
        "WQyq2cfdae4uIdyUfa2rpgYMtSXuQW3/XrQGiz4G6WBXA2wo7folOOpAKYgvHPrm\n" +
        "w6Dd\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 512 signed with RSA 1024
    static String intermediate_SHA1withRSA_512_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICDDCCAXWgAwIBAgIBBDANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDlaFw0yOTA0MjMwMTExNDla\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKubXYoEHZpZkhzA9XX+NrpqJ4SV\n" +
        "lOMBoL3aWExQpJIgrUaZfbGMBBozIHBJMMayokguHbJvq4QigEgLuhfJNqsCAwEA\n" +
        "AaOBiTCBhjAdBgNVHQ4EFgQUN0CHiTYPtjyvpP2a6y6mhsZ6U40wRwYDVR0jBEAw\n" +
        "PoAU59AvsZmWPwU2m+FvYAad3Rmu1oKhI6QhMB8xCzAJBgNVBAYTAlVTMRAwDgYD\n" +
        "VQQKEwdFeGFtcGxlggEAMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgIEMA0G\n" +
        "CSqGSIb3DQEBBQUAA4GBAE2VOlw5ySLT3gUzKCYEga4QPaSrf6lHHPi2g48LscEY\n" +
        "h9qQXh4nuIVugReBIEf6N49RdT+M2cgRJo4sZ3ukYLGQzxNuttL5nPSuuvrAR1oG\n" +
        "LUyzOWcUpKHbVHi6zlTt79RvTKZvLcduLutmtPtLJcM9PdiAI1wEooSgxTwZtB/Z\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 512 signed with RSA 512
    static String intermediate_SHA1withRSA_512_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIByzCCAXWgAwIBAgIBBTANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDlaFw0yOTA0MjMwMTExNDla\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKubXYoEHZpZkhzA9XX+NrpqJ4SV\n" +
        "lOMBoL3aWExQpJIgrUaZfbGMBBozIHBJMMayokguHbJvq4QigEgLuhfJNqsCAwEA\n" +
        "AaOBiTCBhjAdBgNVHQ4EFgQUN0CHiTYPtjyvpP2a6y6mhsZ6U40wRwYDVR0jBEAw\n" +
        "PoAUg4Kwd47hdNQBp8grZsRJ5XvhvxChI6QhMB8xCzAJBgNVBAYTAlVTMRAwDgYD\n" +
        "VQQKEwdFeGFtcGxlggEAMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgIEMA0G\n" +
        "CSqGSIb3DQEBBQUAA0EAoCf0Zu559qcB4xPpzqkVsYiyW49S4Yc0mmQXb1yoQgLx\n" +
        "O+DCkjG5d14+t1MsnkhB2izoQUMxQ3vDc1YnA/tEpw==\n" +
        "-----END CERTIFICATE-----";

    // MD2withRSA 1024 signed with RSA 1024
    static String intermediate_MD2withRSA_1024_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICUDCCAbmgAwIBAgIBBjANBgkqhkiG9w0BAQIFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDlaFw0yOTA0MjMwMTExNDla\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCVOqnlZspyAEr90ELFaUo8\n" +
        "BF0O2Kn0yTdUeyiLOth4RA3qxWrjxJq45VmEBjZpEzPHfnp3PhnfmLcLfhoPONFg\n" +
        "bcHzlkj75ZaKCgHoyV456fMBmj348fcoUkH2WdSQ82pmxHOiHqquYNUSTimFIq82\n" +
        "AayhbKqDmhfx5lJdYNqd5QIDAQABo4GJMIGGMB0GA1UdDgQWBBTfWD9mRTppcUAl\n" +
        "UqGuu/R5t8CB5jBHBgNVHSMEQDA+gBTn0C+xmZY/BTab4W9gBp3dGa7WgqEjpCEw\n" +
        "HzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUw\n" +
        "AwEB/zALBgNVHQ8EBAMCAgQwDQYJKoZIhvcNAQECBQADgYEAPtEjwbWuC5kc4DPc\n" +
        "Ttf/wdbD8ZCdAWzcc3XF9q1TlvwVMNk6mbfM05y6ZVsztKTkwZ4EcvFu/yIqw1EB\n" +
        "E1zlXQCaWXT3/ZMbqYZV4+mx+RUl8spUCb1tda25jnTg3mTOzB1iztm4gy903EMd\n" +
        "m8omKDKeCgcw5dR4ITQYvyxe1as=\n" +
        "-----END CERTIFICATE-----";

    // MD2withRSA 1024 signed with RSA 512
    static String intermediate_MD2withRSA_1024_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICDzCCAbmgAwIBAgIBBzANBgkqhkiG9w0BAQIFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDlaFw0yOTA0MjMwMTExNDla\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCVOqnlZspyAEr90ELFaUo8\n" +
        "BF0O2Kn0yTdUeyiLOth4RA3qxWrjxJq45VmEBjZpEzPHfnp3PhnfmLcLfhoPONFg\n" +
        "bcHzlkj75ZaKCgHoyV456fMBmj348fcoUkH2WdSQ82pmxHOiHqquYNUSTimFIq82\n" +
        "AayhbKqDmhfx5lJdYNqd5QIDAQABo4GJMIGGMB0GA1UdDgQWBBTfWD9mRTppcUAl\n" +
        "UqGuu/R5t8CB5jBHBgNVHSMEQDA+gBSDgrB3juF01AGnyCtmxEnle+G/EKEjpCEw\n" +
        "HzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUw\n" +
        "AwEB/zALBgNVHQ8EBAMCAgQwDQYJKoZIhvcNAQECBQADQQBHok1v6xymtpB7N9xy\n" +
        "0OmDT27uhmzlP0eOzJvXVxj3Oi9TLQJgCUJ9122MzfRAs1E1uJTtvuu+UmI80NQx\n" +
        "KQdp\n" +
        "-----END CERTIFICATE-----";

    private static CertPath generateCertificatePath(String certStr)
            throws CertificateException {
        // generate certificate from cert strings
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is;

        is = new ByteArrayInputStream(certStr.getBytes());
        Certificate cert = cf.generateCertificate(is);

        // generate certification path
        List<Certificate> list = Arrays.asList(new Certificate[] {cert});

        return cf.generateCertPath(list);
    }

    private static Set<TrustAnchor> generateTrustAnchors()
            throws CertificateException {
        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        HashSet<TrustAnchor> anchors = new HashSet<TrustAnchor>();

        ByteArrayInputStream is =
            new ByteArrayInputStream(trustAnchor_SHA1withRSA_1024.getBytes());
        Certificate cert = cf.generateCertificate(is);
        TrustAnchor anchor = new TrustAnchor((X509Certificate)cert, null);
        anchors.add(anchor);

        is = new ByteArrayInputStream(trustAnchor_SHA1withRSA_512.getBytes());
        cert = cf.generateCertificate(is);
        anchor = new TrustAnchor((X509Certificate)cert, null);
        anchors.add(anchor);

        return anchors;
    }

    public static void main(String args[]) throws Exception {
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.certpath.disabledAlgorithms", "MD2");

        try {
            validate(intermediate_SHA1withRSA_1024_1024);
            validate(intermediate_SHA1withRSA_1024_512);
            validate(intermediate_SHA1withRSA_512_1024);
            validate(intermediate_SHA1withRSA_512_512);
        } catch (CertPathValidatorException cpve) {
            throw new Exception(
                "unexpect exception, it is valid cert", cpve);
        }

        try {
            validate(intermediate_MD2withRSA_1024_1024);
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

        try {
            validate(intermediate_MD2withRSA_1024_512);
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

    private static void validate(String intermediate)
            throws CertPathValidatorException, Exception {

        CertPath path = generateCertificatePath(intermediate);
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
