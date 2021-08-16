/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 7068321
 * @summary Support TLS Server Name Indication (SNI) Extension in JSSE Server
 * @run main/othervm SSLSocketSNISensitive PKIX www.example.com
 * @run main/othervm SSLSocketSNISensitive SunX509 www.example.com
 * @run main/othervm SSLSocketSNISensitive PKIX www.example.net
 * @run main/othervm SSLSocketSNISensitive SunX509 www.example.net
 * @run main/othervm SSLSocketSNISensitive PKIX www.invalid.com
 * @run main/othervm SSLSocketSNISensitive SunX509 www.invalid.com
 */

import java.net.*;
import java.util.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.Security;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.*;
import java.security.interfaces.*;
import java.util.Base64;

// Note: this test case works only on TLS 1.2 and prior versions because of
// the use of MD5withRSA signed certificate.
public class SSLSocketSNISensitive {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    /*
     * Should we run the client or server in a separate thread?
     * Both sides can throw exceptions, but do you have a preference
     * as to which side should be the main thread.
     */
    static boolean separateServerThread = false;

    /*
     * Where do we find the keystores?
     */
    // Certificates and key used in the test.
    static String trustedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICkjCCAfugAwIBAgIBADANBgkqhkiG9w0BAQQFADA7MQswCQYDVQQGEwJVUzEN\n" +
        "MAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UwHhcN\n" +
        "MTIwNDE3MTIwNjA3WhcNMzMwMzI4MTIwNjA3WjA7MQswCQYDVQQGEwJVUzENMAsG\n" +
        "A1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UwgZ8wDQYJ\n" +
        "KoZIhvcNAQEBBQADgY0AMIGJAoGBANY+7Enp+1S566kLcKk+qe4Ki6BxaHGZ+v7r\n" +
        "vLksx9IQZCbAEf4YLbrZhKzKD3SPIJXyxPFwknAknIh3Knk8mViOZks7T8L3GnJr\n" +
        "TBaVvDyTzDJum/QYiahfO2qpfN/Oya2UILmqsBAeLyWpzbQsAyWBXfoUtkOUgnzK\n" +
        "fk6QAKYrAgMBAAGjgaUwgaIwHQYDVR0OBBYEFEtmQi7jT1ijXOafPsfkrLwSVu9e\n" +
        "MGMGA1UdIwRcMFqAFEtmQi7jT1ijXOafPsfkrLwSVu9eoT+kPTA7MQswCQYDVQQG\n" +
        "EwJVUzENMAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2\n" +
        "Y2WCAQAwDwYDVR0TAQH/BAUwAwEB/zALBgNVHQ8EBAMCAQYwDQYJKoZIhvcNAQEE\n" +
        "BQADgYEAkKWxMc4+ODk5WwLXXweB8/IKfVfrizNn0KLEgsZ6xNXFIXDpiPGAFcgl\n" +
        "MzFO424JgyvUulsUc/X16Cnuwwntkk6KUG7vEV7h4o9sAV7Cax3gfQE/EZFb4ybn\n" +
        "aBm1UsujMKd/ovqbbbxJbmOWzCeo0QfIGleDEyh3NBBZ0i11Kiw=\n" +
        "-----END CERTIFICATE-----";

    // web server certificate, www.example.com
    static String targetCertStr_A =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICVTCCAb6gAwIBAgIBAjANBgkqhkiG9w0BAQQFADA7MQswCQYDVQQGEwJVUzEN\n" +
        "MAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UwHhcN\n" +
        "MTIwNDE3MTIwNjA4WhcNMzIwMTAzMTIwNjA4WjBVMQswCQYDVQQGEwJVUzENMAsG\n" +
        "A1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UxGDAWBgNV\n" +
        "BAMTD3d3dy5leGFtcGxlLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA\n" +
        "4zFp3PZNzsd3ZwG6FNNWO9eSN+UBymlf8oCwpKJM2tIinmMWvWIXnlx/2UXIfSAq\n" +
        "QEG3aXkAFyEiGGpQlBbqcfrESsHsiz2pnnm5dG2v/eS0Bwz1jmcuNmwnh3UQw2Vl\n" +
        "+BLk8ukdrLjiCT8jARiHExYf1Xg+wUqQ9y8NV26hdaUCAwEAAaNPME0wCwYDVR0P\n" +
        "BAQDAgPoMB0GA1UdDgQWBBQwtx+gqzn2w4y82brXlp7tqBYEZDAfBgNVHSMEGDAW\n" +
        "gBRLZkIu409Yo1zmnz7H5Ky8ElbvXjANBgkqhkiG9w0BAQQFAAOBgQAJWo8B6Ud+\n" +
        "/OU+UcZLihlfMX02OSlK2ZB7mfqpj2G3JT9yb0A+VbY3uuajmaYYIIxl3kXGz/n8\n" +
        "M2Q/Ux/MDxG+IFKHC26Kuj4dAQgzjq2pILVPTE2QnaQTNCsgVZtTaC47SG9FRSoC\n" +
        "qvnIvn/oTpKSqus76I1cR4joDtiV2OEuVw==\n" +
        "-----END CERTIFICATE-----";

    // Private key in the format of PKCS#8
    static String targetPrivateKey_A =
        "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAOMxadz2Tc7Hd2cB\n" +
        "uhTTVjvXkjflAcppX/KAsKSiTNrSIp5jFr1iF55cf9lFyH0gKkBBt2l5ABchIhhq\n" +
        "UJQW6nH6xErB7Is9qZ55uXRtr/3ktAcM9Y5nLjZsJ4d1EMNlZfgS5PLpHay44gk/\n" +
        "IwEYhxMWH9V4PsFKkPcvDVduoXWlAgMBAAECgYAqX2nuIyXp3fvgA0twXOYlbRRB\n" +
        "Rn3qAXM6qFPJsNeCrFR2k+aG1cev6nKR1FkLNTeMGnWZv06MAcr5IML8i7WXyG4C\n" +
        "LY/C0gedn94FDKFlln+bTENwQTGjn4lKysDA+IuNpasTeMCajbic+dPByhIdTOjZ\n" +
        "iMCyxbLfpk40zQopVQJBAPyfGmkeHB3GjdbdgujWCGKb2UxBa4O8dy3O4l2yizTn\n" +
        "uUqMGcwGY4ciNSVvZQ7jKo4vDmkSuYib4/woPChaNfMCQQDmO0BQuSWYGNtSwV35\n" +
        "lafZfX1dNCLKm1iNA6A12evXgvQiE9WT4mqionig0VZW16HtiY4/BkHOcos/K9Um\n" +
        "ARQHAkA8mkaRtSF1my5nv1gqVz5Hua+VdZQ/VDUbDiiL5cszc+ulkJqXsWirAG/T\n" +
        "fTe3LJQG7A7+8fkEZrF4yoY0AAA1AkEAotokezULj5N9iAL5SzL9wIzQYV4ggfny\n" +
        "YATBjXXxKccakwQ+ndWZIiMUeoS4ssLialhTgucVI0fIkU2a/r/ifwJAc6e+5Pvh\n" +
        "MghQj/U788Od/v6rgqz/NGsduZ7uilCMcWiwA73OR2MHMH/OIuoofuEPrfuV9isV\n" +
        "xVXhgpKfP/pdOA==";

    // web server certificate, www.example.net
    static String targetCertStr_B =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICVTCCAb6gAwIBAgIBBDANBgkqhkiG9w0BAQQFADA7MQswCQYDVQQGEwJVUzEN\n" +
        "MAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UwHhcN\n" +
        "MTIwNDE3MTIwNjA5WhcNMzIwMTAzMTIwNjA5WjBVMQswCQYDVQQGEwJVUzENMAsG\n" +
        "A1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UxGDAWBgNV\n" +
        "BAMTD3d3dy5leGFtcGxlLm5ldDCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA\n" +
        "2VlzF1fvWYczDChrUeJiLJ1M/dIShCaOTfYGiXfQGEZCAWTacUclwr+rVMnZ75/c\n" +
        "wwg5pNdXRijxMil8DBTS1gFcIFQhosLHvzIAe6ULlg/xB+/L6KBz+NTWfo/2KF6t\n" +
        "xatmcToNrCcwi7eUOfbzQje65Tizs56jJYem2m7Rk0ECAwEAAaNPME0wCwYDVR0P\n" +
        "BAQDAgPoMB0GA1UdDgQWBBQT/FR0cAWcZQ7h0X79KGki34OSQjAfBgNVHSMEGDAW\n" +
        "gBRLZkIu409Yo1zmnz7H5Ky8ElbvXjANBgkqhkiG9w0BAQQFAAOBgQB67cPIT6fz\n" +
        "6Ws8fBpYgW2ad4ci66i1WduBD9CpGFE+jRK2feRj6hvYBXocKj0AMWUFIEB2E3hA\n" +
        "oIjxcf1GxIpHVl9DjlhxqXbA0Ktl7/NGNRlDSLTizOTl3FB1mMTlOGvXDVmpcFhl\n" +
        "HuoP1hYvhTsBwPx5igGNchuPtDIUzL2mXw==\n" +
        "-----END CERTIFICATE-----";

    static String targetPrivateKey_B =
        "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBANlZcxdX71mHMwwo\n" +
        "a1HiYiydTP3SEoQmjk32Bol30BhGQgFk2nFHJcK/q1TJ2e+f3MMIOaTXV0Yo8TIp\n" +
        "fAwU0tYBXCBUIaLCx78yAHulC5YP8Qfvy+igc/jU1n6P9ihercWrZnE6DawnMIu3\n" +
        "lDn280I3uuU4s7OeoyWHptpu0ZNBAgMBAAECgYEAl19H26sfhD+32rDPxZCgBShs\n" +
        "dZ33zVe45i0Bcn4iTLWpxKTDyf7eGps4rO2DvfKdYqt40ggzvSZIjUH9JcDe8GmG\n" +
        "d3m0ILB7pg4jsFlpyeHpTO8grPLxA1G9s3o0DoFpz/rooqgFfe/DrRDmRoOSkgfV\n" +
        "/gseIbgJHRO/Ctyvdh0CQQD6uFd0HxhH1jl/JzvPzIH4LSnPcdEh9zsMEb6uzh75\n" +
        "9qL+IHD5N2I/pYZTKqDFIwhJf701+LKag55AX/zrDt7rAkEA3e00AbnwanDMa6Wj\n" +
        "+gFekUQveSVra38LiihzCkyVvQpFjbiF1rUhSNQ0dpU5/hmrYF0C6H9VXAesfkUY\n" +
        "WhpDgwJAYjgZOop77piDycZK7isFt32p5XSHIzFBVocVFlH1XKM8UyXOXDNQL/Le\n" +
        "XnJSrSf+NRzvuNcG0PVC56Ey6brXpQJAY4M4vcltt5zq3R5CQBmbGRJ1IyKXX3Vx\n" +
        "bDslEqoyvri7ZYgnY5aG3UxiVgYmIf3KrgQnCLAIS6MZQumiuMxsFwJAK5pEG063\n" +
        "9ngUof4fDMvZphqZjZR1zMKz/V/9ge0DWBINaqFgsgebNu+MyImsC8C6WKjGmV/2\n" +
        "f1MY0D7sC2vU/Q==";

    // web server certificate, www.invalid.com
    static String targetCertStr_C =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICVTCCAb6gAwIBAgIBAzANBgkqhkiG9w0BAQQFADA7MQswCQYDVQQGEwJVUzEN\n" +
        "MAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UwHhcN\n" +
        "MTIwNDE3MTIwNjA5WhcNMzIwMTAzMTIwNjA5WjBVMQswCQYDVQQGEwJVUzENMAsG\n" +
        "A1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UxGDAWBgNV\n" +
        "BAMTD3d3dy5pbnZhbGlkLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA\n" +
        "q6MyQwzCr2nJ41l0frmHL0qULSyW51MhevBC+1W28i0LE/efrmpwV3LdnlQEGFak\n" +
        "DLDwtnff3iru8dSMcA7KdWVkivsE7ZTP+qFDaWBAy7XXiSsv6yZ2Nh4jJb0YcD28\n" +
        "45zk2nAl5Az1/PuoTi1vpQxzFZKuBm1HGgz3MEZvBvMCAwEAAaNPME0wCwYDVR0P\n" +
        "BAQDAgPoMB0GA1UdDgQWBBRRMifrND015Nm8N6gV5X7cg1YjjjAfBgNVHSMEGDAW\n" +
        "gBRLZkIu409Yo1zmnz7H5Ky8ElbvXjANBgkqhkiG9w0BAQQFAAOBgQBjkUO6Ri/B\n" +
        "uDC2gDMIyL5+NTe/1dPPQYM4HhCNa/KQYvU5lzCKO9Vpa+i+nyrUNNXUu8Tkyq4Y\n" +
        "A+aGSm6+FT/i9rFwkYUdorBtD3KfQiwTIWrVERXBkWI5iZNaVZhx0TFy4vUpf65d\n" +
        "QtwkbHpC66fdKc2EdLXkuY9KkmtZZJJ7YA==\n" +
        "-----END CERTIFICATE-----";

    static String targetPrivateKey_C =
        "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAKujMkMMwq9pyeNZ\n" +
        "dH65hy9KlC0sludTIXrwQvtVtvItCxP3n65qcFdy3Z5UBBhWpAyw8LZ3394q7vHU\n" +
        "jHAOynVlZIr7BO2Uz/qhQ2lgQMu114krL+smdjYeIyW9GHA9vOOc5NpwJeQM9fz7\n" +
        "qE4tb6UMcxWSrgZtRxoM9zBGbwbzAgMBAAECgYASJDK40Y12Wvki1Z6xkkyOnBRj\n" +
        "XfYpRykfxGtgA2RN3qLwHlk7Zzaul46DIKA6LlYynTUkJDF+Ww1cdDnP0lBlwcmM\n" +
        "iD0ck3zYyYBLhQHuVbkK3SYE+ANRhM0icvvqANP2at/U4awQcPNEae/KCiecLNu3\n" +
        "CJGqyhPDdrEAqPuJGQJBAN46pQC6l3yrcSYE2s53jSmsm2HVVOFlFXjU6k/RMTxG\n" +
        "FfDJtGUAOQ37rPQ06ugr/gjLAmmPp+FXozaBdA32D80CQQDFuGRgv3WYqbglIcRL\n" +
        "JRs6xlj9w1F97s/aiUenuwhIPNiUoRbV7mnNuZ/sGF0svOVE7SazRjuFX6UqL9Y9\n" +
        "HzG/AkEA170pCI8cl4w8eUNHRB9trGKEKjMXhwVCFh7lJf2ZBcGodSzr8w2HVhrZ\n" +
        "Ke7hiemDYffrbJ1oxmv05+o+x3r0lQJBAL6adVm2+FyFMFnLZXmzeb59O4jWY5bt\n" +
        "Qz6/HG6bpO5OidMuP99YCHMkQQDOs/PO3Y5GuAoW6IY4n/Y9S2B80+0CQBl1/H9/\n" +
        "0n/vrb6vW6Azds49tuS82RFAnOhtwTyBEajs08WF8rZQ3WD2RHJnH0+jjfL0anIp\n" +
        "dQBSeNN7s7b6rRk=";

    // This is a certificate for client
    static String targetCertStr_D=
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICVDCCAb2gAwIBAgIBBTANBgkqhkiG9w0BAQQFADA7MQswCQYDVQQGEwJVUzEN\n" +
        "MAsGA1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UwHhcN\n" +
        "MTIwNDE3MTIwNjEwWhcNMzIwMTAzMTIwNjEwWjBUMQswCQYDVQQGEwJVUzENMAsG\n" +
        "A1UEChMESmF2YTEdMBsGA1UECxMUU3VuSlNTRSBUZXN0IFNlcml2Y2UxFzAVBgNV\n" +
        "BAMTDkludGVyT3AgVGVzdGVyMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDo\n" +
        "Q/KoAIAC2ljFfW2KwjnxTzi4NQJeUuk2seqKpsAY8x4O5dvixzUl6142zmljapqi\n" +
        "bJloQVpfB+CEc5/l4h5gzGRVzkuqP1oPzDrpZ5GsvmvuHenV/TzCIgX1cLETzQVt\n" +
        "6Rk06okoBPnw3hDJEJiEc1Rv7HCE8p/p+SaiHrskwwIDAQABo08wTTALBgNVHQ8E\n" +
        "BAMCA+gwHQYDVR0OBBYEFPr91O33RIGfFSqza2AwQIgE4QswMB8GA1UdIwQYMBaA\n" +
        "FEtmQi7jT1ijXOafPsfkrLwSVu9eMA0GCSqGSIb3DQEBBAUAA4GBANIDFYgAhoj3\n" +
        "B8u1YpqeoEp2Lt9TwrYBshaIrbmBPCwCGio0JIsoov3n8BCSg5F+8MnOtPl+TjeO\n" +
        "0Ug+7guPdCk/wg8YNxLHgSsQlpcNJDjWiErqmUPVrg5BPPQb65qMund6KTmMN0y6\n" +
        "4EbSmxRpZO/N0/5oK4umTk0EeXKNekBj\n" +
        "-----END CERTIFICATE-----";

    static String targetPrivateKey_D =
        "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAOhD8qgAgALaWMV9\n" +
        "bYrCOfFPOLg1Al5S6Tax6oqmwBjzHg7l2+LHNSXrXjbOaWNqmqJsmWhBWl8H4IRz\n" +
        "n+XiHmDMZFXOS6o/Wg/MOulnkay+a+4d6dX9PMIiBfVwsRPNBW3pGTTqiSgE+fDe\n" +
        "EMkQmIRzVG/scITyn+n5JqIeuyTDAgMBAAECgYBw37yIKp4LRONJLnhSq6sO+0n8\n" +
        "Mz6waiiN/Q6XTQwj09pysQAYCGlqwSRrDAqpVsBJWO+Ae+oYLrLMi4hUZnwN75v3\n" +
        "pe1nXlrD11RmPLXwBxqFxNSvAs2FgLHZEtwHI7Bn8KybT/8bGkQ8csLceInYtMDD\n" +
        "MuTyy2KRk/pj60zIKQJBAPgebQiAH6viFQ88AwHaNvQhlUfwmSC1i6f8LVoeqaHC\n" +
        "lnP0LJBwlyDeeEInhHrCR2ibnCB6I/Pig+49XQgabK8CQQDvpJwuGEbsOO+3rkJJ\n" +
        "OpOw4toG0QJZdRnT6l8I6BlboQRZSfFh+lGGahvFXkxc4KdUpJ7QPtXU7HHk6Huk\n" +
        "8RYtAkA9CW8VGj+wTuuTVdX/jKjcIa7RhbSFwWNbrcOSWdys+Gt+luCnn6rt4QyA\n" +
        "aaxDbquWZkFgE+voQR7nap0KM0XtAkAznd0WAJymHM1lXt9gLoHJQ9N6TGKZKiPa\n" +
        "BU1a+cMcfV4WbVrUo7oTnZ9Fr73681iXXq3mZOJh7lvJ1llreZIxAkBEnbiTgEf4\n" +
        "tvku68jHcRbRPmdS7CBSWNEBaHLOm4pUSTcxVTKKMHw7vmM5/UYUxJ8QNKCYxn6O\n" +
        "+vtiBwBawwzN";

    static String[] serverCerts = {targetCertStr_A,
                                targetCertStr_B, targetCertStr_C};
    static String[] serverKeys  = {targetPrivateKey_A,
                                targetPrivateKey_B, targetPrivateKey_C};
    static String[] clientCerts = {targetCertStr_D};
    static String[] clientKeys  = {targetPrivateKey_D};

    static char passphrase[] = "passphrase".toCharArray();

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = false;

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {
        SSLContext context = generateSSLContext(false);
        SSLServerSocketFactory sslssf = context.getServerSocketFactory();
        SSLServerSocket sslServerSocket =
            (SSLServerSocket)sslssf.createServerSocket(serverPort);
        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        SSLSocket sslSocket = (SSLSocket)sslServerSocket.accept();
        try {
            sslSocket.setSoTimeout(5000);
            sslSocket.setSoLinger(true, 5);

            InputStream sslIS = sslSocket.getInputStream();
            OutputStream sslOS = sslSocket.getOutputStream();

            sslIS.read();
            sslOS.write('A');
            sslOS.flush();

            SSLSession session = sslSocket.getSession();
            checkCertificate(session.getLocalCertificates(),
                                                clientRequestedHostname);
        } finally {
            sslSocket.close();
            sslServerSocket.close();
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {

        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        SSLContext context = generateSSLContext(true);
        SSLSocketFactory sslsf = context.getSocketFactory();

        SSLSocket sslSocket =
            (SSLSocket)sslsf.createSocket("localhost", serverPort);

        SNIHostName serverName = new SNIHostName(clientRequestedHostname);
        List<SNIServerName> serverNames = new ArrayList<>(1);
        serverNames.add(serverName);
        SSLParameters params = sslSocket.getSSLParameters();
        params.setServerNames(serverNames);
        sslSocket.setSSLParameters(params);

        try {
            sslSocket.setSoTimeout(5000);
            sslSocket.setSoLinger(true, 5);

            InputStream sslIS = sslSocket.getInputStream();
            OutputStream sslOS = sslSocket.getOutputStream();

            sslOS.write('B');
            sslOS.flush();
            sslIS.read();

            SSLSession session = sslSocket.getSession();
            checkCertificate(session.getPeerCertificates(),
                                                clientRequestedHostname);
        } finally {
            sslSocket.close();
        }
    }

    private static void checkCertificate(Certificate[] certs,
            String hostname) throws Exception {
        if (certs != null && certs.length != 0) {
            X509Certificate x509Cert = (X509Certificate)certs[0];

            String subject = x509Cert.getSubjectX500Principal().getName();

            if (!subject.contains(hostname)) {
                throw new Exception(
                        "Not the expected certificate: " + subject);
            }
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */
    private static String tmAlgorithm;             // trust manager
    private static String clientRequestedHostname; // server name indication

    private static void parseArguments(String[] args) {
        tmAlgorithm = args[0];
        clientRequestedHostname = args[1];
    }

    private static SSLContext generateSSLContext(boolean isClient)
            throws Exception {

        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // create a key store
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // import the trused cert
        ByteArrayInputStream is =
                    new ByteArrayInputStream(trustedCertStr.getBytes());
        Certificate trusedCert = cf.generateCertificate(is);
        is.close();

        ks.setCertificateEntry("RSA Export Signer", trusedCert);

        String[] certStrs = null;
        String[] keyStrs = null;
        if (isClient) {
            certStrs = clientCerts;
            keyStrs = clientKeys;
        } else {
            certStrs = serverCerts;
            keyStrs = serverKeys;
        }

        for (int i = 0; i < certStrs.length; i++) {
            // generate the private key.
            String keySpecStr = keyStrs[i];
            PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(
                                Base64.getMimeDecoder().decode(keySpecStr));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPrivateKey priKey =
                    (RSAPrivateKey)kf.generatePrivate(priKeySpec);

            // generate certificate chain
            String keyCertStr = certStrs[i];
            is = new ByteArrayInputStream(keyCertStr.getBytes());
            Certificate keyCert = cf.generateCertificate(is);
            is.close();

            Certificate[] chain = new Certificate[2];
            chain[0] = keyCert;
            chain[1] = trusedCert;

            // import the key entry.
            ks.setKeyEntry("key-entry-" + i, priKey, passphrase, chain);
        }

        // create SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmAlgorithm);
        tmf.init(ks);

        SSLContext ctx = SSLContext.getInstance("TLSv1.2");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
        kmf.init(ks, passphrase);

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        ks = null;

        return ctx;
    }

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String[] args) throws Exception {
        // MD5 is used in this test case, don't disable MD5 algorithm.
        Security.setProperty("jdk.certpath.disabledAlgorithms",
                "MD2, RSA keySize < 1024");
        Security.setProperty("jdk.tls.disabledAlgorithms",
                "SSLv3, RC4, DH keySize < 768");

        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Get the customized arguments.
         */
        parseArguments(args);

        /*
         * Start the tests.
         */
        new SSLSocketSNISensitive();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SSLSocketSNISensitive() throws Exception {
        try {
            if (separateServerThread) {
                startServer(true);
                startClient(false);
            } else {
                startClient(true);
                startServer(false);
            }
        } catch (Exception e) {
            // swallow for now.  Show later
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            serverThread.join();
        } else {
            clientThread.join();
        }

        /*
         * When we get here, the test is pretty much over.
         * Which side threw the error?
         */
        Exception local;
        Exception remote;
        String whichRemote;

        if (separateServerThread) {
            remote = serverException;
            local = clientException;
            whichRemote = "server";
        } else {
            remote = clientException;
            local = serverException;
            whichRemote = "client";
        }

        /*
         * If both failed, return the curthread's exception, but also
         * print the remote side Exception
         */
        if ((local != null) && (remote != null)) {
            System.out.println(whichRemote + " also threw:");
            remote.printStackTrace();
            System.out.println();
            throw local;
        }

        if (remote != null) {
            throw remote;
        }

        if (local != null) {
            throw local;
        }
    }

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide();
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died, because of " + e);
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            try {
                doServerSide();
            } catch (Exception e) {
                serverException = e;
            } finally {
                serverReady = true;
            }
        }
    }

    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide();
                    } catch (Exception e) {
                        /*
                         * Our client thread just died.
                         */
                        System.err.println("Client died, because of " + e);
                        clientException = e;
                    }
                }
            };
            clientThread.start();
        } else {
            try {
                doClientSide();
            } catch (Exception e) {
                clientException = e;
            }
        }
    }
}
