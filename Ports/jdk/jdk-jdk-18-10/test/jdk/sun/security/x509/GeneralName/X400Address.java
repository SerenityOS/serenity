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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * @test
 * @bug 8058543
 * @summary Check that a Subject Alternative Name of type X400Address
 *      can be parsed
 */
public class X400Address {

    /*
     * A cert containing an X400Address SAN:
     *    ObjectId: 2.5.29.17 Criticality=false
     *    SubjectAlternativeName [
     *      RFC822Name: testuser@test.net
     *      X400Address: <DER-encoded value>
     *    ]
     */
    private static final String certStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIEXjCCA0agAwIBAgIEAm+MuDANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQDEwlU\n" +
        "ZXN0IFVzZXIwHhcNMTQwOTI2MDgyNTA1WhcNMjQwOTIzMDgyNTA1WjAUMRIwEAYD\n" +
        "VQQDEwlUZXN0IFVzZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCM\n" +
        "Lj3k7is6bGoZgoONF15PT/GnfGtFJFYF2alnuHYjj/zdlKtU2DAPadz2EAJHczhy\n" +
        "/p1DVvyrOUgCviB0nzijuLt9YHr6goel7Pe/YtcRavfC5HzEVOjhp92WZX/jIO9X\n" +
        "xlpC/aQKt0pqCgUkvzVzB2Lm8EB/bcxlRKNh6WpAGfH/23XnJZ/UPRYgu+C+00i2\n" +
        "vv26kHb2hZ3px1RP4b1FMiKc5mZxd+D9Ep3kGf+SnrBROR9v2u4hOobxzC9avAfq\n" +
        "vTJ1rauR08eUHD15AUrR6AlOhb4Y6XqzPZ+aPb1Mj1XSuMsXvApWhXmavRA5wK1S\n" +
        "ZtnvibB/0l57AMSUh24NAgMBAAGjggG2MIIBsjAPBgNVHRMBAf8EBTADAQH/MA8G\n" +
        "A1UdJQQIMAYGBFUdJQAwDgYDVR0PAQH/BAQDAgbAMEcGA1UdEQRAMD6BEXRlc3R1\n" +
        "c2VyQHRlc3QubmV0oykwE2EEEwJ0cmIDEwEgogYTBFRFU1QxEjAQgAEBoQsTCVRl\n" +
        "c3QgVXNlcjCCATMGA1UdDgSCASoEggEmMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n" +
        "MIIBCgKCAQEAjC495O4rOmxqGYKDjRdeT0/xp3xrRSRWBdmpZ7h2I4/83ZSrVNgw\n" +
        "D2nc9hACR3M4cv6dQ1b8qzlIAr4gdJ84o7i7fWB6+oKHpez3v2LXEWr3wuR8xFTo\n" +
        "4afdlmV/4yDvV8ZaQv2kCrdKagoFJL81cwdi5vBAf23MZUSjYelqQBnx/9t15yWf\n" +
        "1D0WILvgvtNItr79upB29oWd6cdUT+G9RTIinOZmcXfg/RKd5Bn/kp6wUTkfb9ru\n" +
        "ITqG8cwvWrwH6r0yda2rkdPHlBw9eQFK0egJToW+GOl6sz2fmj29TI9V0rjLF7wK\n" +
        "VoV5mr0QOcCtUmbZ74mwf9JeewDElIduDQIDAQABMA0GCSqGSIb3DQEBCwUAA4IB\n" +
        "AQCD3p0mhvqDLYqTL9l5cRK9+cGOS5nbDvUmd1NutCIOJHMT96ESob9fqM9zo23l\n" +
        "Yudajxa3/GooKTCe1TcaWIzFVqvO8wezMLkwuZcoto1ST8+lS36ZwZ1gMipE2Kh1\n" +
        "XmnBn0wkUNfqypQ+nWH9vsYuJkeDb8402BOEdb/pvGYKC8d6EzQwUukLpXyOMs9r\n" +
        "aCcsnSYRV8gFYCUxZkm0+tQOBjUsQ1SP/ww7SicPPSHqb08HvH4ALG75MUxIMVF1\n" +
        "vyTeStZzsFXQaUq026U/rDXf18bV3Vi5EI5jR9p6CDXgundqnMmiEo4BXWCp/KeB\n" +
        "JxndcaVIF7PbMkUw01elOmj0\n" +
        "-----END CERTIFICATE-----";

    public static void main(String[] args) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream is = new ByteArrayInputStream(certStr.getBytes());
        X509Certificate cert = (X509Certificate) cf.generateCertificate(is);

        if (cert.getSubjectAlternativeNames() == null) {
            throw new Exception("Failed to parse Subject Alternative Name");
        }
    }
}
