/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5091374 5100603
 * @summary make sure the JKS case sensitivity works correctly
 * @author  Andreas Sterbenz
 */

import java.io.*;
import java.util.*;

import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;

public class CaseSensitiveAliases {

    // some arbitrary certs

    private final static String S1 =
"-----BEGIN CERTIFICATE-----\n" +
"MIIB4DCCAYoCAQEwDQYJKoZIhvcNAQEEBQAwezELMAkGA1UEBhMCVVMxCzAJBgNV" +
"BAgTAkNBMRIwEAYDVQQHEwlDdXBlcnRpbm8xGTAXBgNVBAoTEFN1biBNaWNyb3N5" +
"c3RlbXMxFjAUBgNVBAsTDUphdmEgU29mdHdhcmUxGDAWBgNVBAMTD0pDRSBEZXZl" +
"bG9wbWVudDAeFw0wMjEwMzExNTI3NDRaFw0wNzEwMzExNTI3NDRaMHsxCzAJBgNV" +
"BAYTAlVTMQswCQYDVQQIEwJDQTESMBAGA1UEBxMJQ3VwZXJ0aW5vMRkwFwYDVQQK" +
"ExBTdW4gTWljcm9zeXN0ZW1zMRYwFAYDVQQLEw1KYXZhIFNvZnR3YXJlMRgwFgYD" +
"VQQDEw9KQ0UgRGV2ZWxvcG1lbnQwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAo/4C" +
"ddEOa3M6v9JFAhnBYgTq54Y30++F8yzCK9EeYaG3AzvzZqNshDy579647p0cOM/4" +
"VO6rU2PgbzgKXPcs8wIDAQABMA0GCSqGSIb3DQEBBAUAA0EACqPlFmVdKdYSCTNl" +
"tXKQnBqss9GNjbnB+CitvWrwN+oOK8qQpvV+5LB6LruvRy6zCedCV95Z2kXKg/Fn" +
"j0gvsg==\n" +
"-----END CERTIFICATE-----";

    private final static String S2 =
"-----BEGIN CERTIFICATE-----\n" +
"MIIB4DCCAYoCAQIwDQYJKoZIhvcNAQEEBQAwezELMAkGA1UEBhMCVVMxCzAJBgNV" +
"BAgTAkNBMRIwEAYDVQQHEwlDdXBlcnRpbm8xGTAXBgNVBAoTEFN1biBNaWNyb3N5" +
"c3RlbXMxFjAUBgNVBAsTDUphdmEgU29mdHdhcmUxGDAWBgNVBAMTD0pDRSBEZXZl" +
"bG9wbWVudDAeFw0wMjEwMzExNTI3NDRaFw0wNzEwMzExNTI3NDRaMHsxCzAJBgNV" +
"BAYTAlVTMQswCQYDVQQIEwJDQTESMBAGA1UEBxMJQ3VwZXJ0aW5vMRkwFwYDVQQK" +
"ExBTdW4gTWljcm9zeXN0ZW1zMRYwFAYDVQQLEw1KYXZhIFNvZnR3YXJlMRgwFgYD" +
"VQQDEw9KQ0UgRGV2ZWxvcG1lbnQwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAr1OS" +
"XaOzpnVoqL2LqS5+HLy1kVvBwiM/E5iYT9eZaghE8qvF+4fETipWUNTWCQzHR4cD" +
"JGJOl9Nm77tELhES4QIDAQABMA0GCSqGSIb3DQEBBAUAA0EAL+WcVFyj+iXlEVNV" +
"QbNOOUlWmlmXGiNKKXnIdNcc1ZUyi+JW0zmlfZ7iU/eRYhEEJBwdrUoyiGOGLo7p" +
"i6JzAA==\n" +
"-----END CERTIFICATE-----";

    private static CertificateFactory cf;

    private static X509Certificate parseCert(String s) throws Exception {
        if (cf == null) {
            cf = CertificateFactory.getInstance("X.509");
        }
        InputStream in = new ByteArrayInputStream(s.getBytes("UTF8"));
        return (X509Certificate)cf.generateCertificate(in);
    }

    public static void main(String[] args) throws Exception {
        main("JKS", true);
        main("CaseExactJKS", false);
    }

    private static void main(String jks, boolean caseInsensitive) throws Exception {
        X509Certificate c1 = parseCert(S1);
        X509Certificate c2 = parseCert(S2);
        X509Certificate[] a1 = {c1};
        X509Certificate[] a2 = {c2};

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        PrivateKey p1 = kpg.generateKeyPair().getPrivate();
        PrivateKey p2 = kpg.generateKeyPair().getPrivate();

        KeyStore ks = KeyStore.getInstance(jks);
        ks.load(null, null);

        char[] pw = "pw".toCharArray();

        ks.setKeyEntry("Alias", p1, pw, a1);
        ks.setKeyEntry("ALIAS", p2, pw, a2);

        if (caseInsensitive) {
            if (ks.size() != 1) {
                throw new Exception("size mismatch: " + ks.size());
            }
            match(p2, ks.getKey("alias", pw));
            match(p2, ks.getKey("Alias", pw));
            match(p2, ks.getKey("ALIAS", pw));
            match(a2, ks.getCertificateChain("alias"));
            match(a2, ks.getCertificateChain("Alias"));
            match(a2, ks.getCertificateChain("ALIAS"));
        } else {
            if (ks.size() != 2) {
                throw new Exception("size mismatch: " + ks.size());
            }
            match(null, ks.getKey("alias", pw));
            match(p1, ks.getKey("Alias", pw));
            match(p2, ks.getKey("ALIAS", pw));
            match(null, ks.getCertificateChain("alias"));
            match(a1, ks.getCertificateChain("Alias"));
            match(a2, ks.getCertificateChain("ALIAS"));
        }

        System.out.println("OK: " + jks);
    }

    private static void match(Key p1, Key p2) throws Exception {
        System.out.println(String.valueOf(p2).split("\\n")[0]);
        if ((p1 != p2) && (p1.equals(p2) == false)) {
            throw new Exception("Private key mismatch");
        }
    }

    private static void match(Certificate[] a1, Certificate[] a2) throws Exception {
        System.out.println(String.valueOf(a2).split("\\n")[0]);
        if ((a1 != a2) && (Arrays.equals(a1, a2) == false)) {
            throw new Exception("chain mismatch");
        }
    }

}
