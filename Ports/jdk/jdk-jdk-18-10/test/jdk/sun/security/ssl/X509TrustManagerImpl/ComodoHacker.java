/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7123519
 * @summary Problem with java/classes_security
 * @run main/othervm ComodoHacker PKIX
 * @run main/othervm ComodoHacker SunX509
 */

import java.net.*;
import java.util.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.spec.*;
import java.security.interfaces.*;

public class ComodoHacker {
    // DigiNotar Root CA, untrusted root certificate
    static String trustedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIE2DCCBEGgAwIBAgIEN0rSQzANBgkqhkiG9w0BAQUFADCBwzELMAkGA1UEBhMC\n" +
        "VVMxFDASBgNVBAoTC0VudHJ1c3QubmV0MTswOQYDVQQLEzJ3d3cuZW50cnVzdC5u\n" +
        "ZXQvQ1BTIGluY29ycC4gYnkgcmVmLiAobGltaXRzIGxpYWIuKTElMCMGA1UECxMc\n" +
        "KGMpIDE5OTkgRW50cnVzdC5uZXQgTGltaXRlZDE6MDgGA1UEAxMxRW50cnVzdC5u\n" +
        "ZXQgU2VjdXJlIFNlcnZlciBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTAeFw05OTA1\n" +
        "MjUxNjA5NDBaFw0xOTA1MjUxNjM5NDBaMIHDMQswCQYDVQQGEwJVUzEUMBIGA1UE\n" +
        "ChMLRW50cnVzdC5uZXQxOzA5BgNVBAsTMnd3dy5lbnRydXN0Lm5ldC9DUFMgaW5j\n" +
        "b3JwLiBieSByZWYuIChsaW1pdHMgbGlhYi4pMSUwIwYDVQQLExwoYykgMTk5OSBF\n" +
        "bnRydXN0Lm5ldCBMaW1pdGVkMTowOAYDVQQDEzFFbnRydXN0Lm5ldCBTZWN1cmUg\n" +
        "U2VydmVyIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MIGdMA0GCSqGSIb3DQEBAQUA\n" +
        "A4GLADCBhwKBgQDNKIM0VBuJ8w+vN5Ex/68xYMmo6LIQaO2f55M28Qpku0f1BBc/\n" +
        "I0dNxScZgSYMVHINiC3ZH5oSn7yzcdOAGT9HZnuMNSjSuQrfJNqc1lB5gXpa0zf3\n" +
        "wkrYKZImZNHkmGw6AIr1NJtl+O3jEP/9uElY3KDegjlrgbEWGWG5VLbmQwIBA6OC\n" +
        "AdcwggHTMBEGCWCGSAGG+EIBAQQEAwIABzCCARkGA1UdHwSCARAwggEMMIHeoIHb\n" +
        "oIHYpIHVMIHSMQswCQYDVQQGEwJVUzEUMBIGA1UEChMLRW50cnVzdC5uZXQxOzA5\n" +
        "BgNVBAsTMnd3dy5lbnRydXN0Lm5ldC9DUFMgaW5jb3JwLiBieSByZWYuIChsaW1p\n" +
        "dHMgbGlhYi4pMSUwIwYDVQQLExwoYykgMTk5OSBFbnRydXN0Lm5ldCBMaW1pdGVk\n" +
        "MTowOAYDVQQDEzFFbnRydXN0Lm5ldCBTZWN1cmUgU2VydmVyIENlcnRpZmljYXRp\n" +
        "b24gQXV0aG9yaXR5MQ0wCwYDVQQDEwRDUkwxMCmgJ6AlhiNodHRwOi8vd3d3LmVu\n" +
        "dHJ1c3QubmV0L0NSTC9uZXQxLmNybDArBgNVHRAEJDAigA8xOTk5MDUyNTE2MDk0\n" +
        "MFqBDzIwMTkwNTI1MTYwOTQwWjALBgNVHQ8EBAMCAQYwHwYDVR0jBBgwFoAU8Bdi\n" +
        "E1U9s/8KAGv7UISX8+1i0BowHQYDVR0OBBYEFPAXYhNVPbP/CgBr+1CEl/PtYtAa\n" +
        "MAwGA1UdEwQFMAMBAf8wGQYJKoZIhvZ9B0EABAwwChsEVjQuMAMCBJAwDQYJKoZI\n" +
        "hvcNAQEFBQADgYEAkNwwAvpkdMKnCqV8IY00F6j7Rw7/JXyNEwr75Ji174z4xRAN\n" +
        "95K+8cPV1ZVqBLssziY2ZcgxxufuP+NXdYR6Ee9GTxj005i7qIcyunL2POI9n9cd\n" +
        "2cNgQ4xYDiKWL2KjLB+6rQXvqzJ4h6BUcxm1XAX5Uj5tLUUL9wqT6u0G+bI=\n" +
        "-----END CERTIFICATE-----";

    // DigiNotar Root CA, untrusted cross-certificate
    static String untrustedCrossCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIFSDCCBLGgAwIBAgIERpwsrzANBgkqhkiG9w0BAQUFADCBwzELMAkGA1UEBhMC\n" +
        "VVMxFDASBgNVBAoTC0VudHJ1c3QubmV0MTswOQYDVQQLEzJ3d3cuZW50cnVzdC5u\n" +
        "ZXQvQ1BTIGluY29ycC4gYnkgcmVmLiAobGltaXRzIGxpYWIuKTElMCMGA1UECxMc\n" +
        "KGMpIDE5OTkgRW50cnVzdC5uZXQgTGltaXRlZDE6MDgGA1UEAxMxRW50cnVzdC5u\n" +
        "ZXQgU2VjdXJlIFNlcnZlciBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTAeFw0wNzA3\n" +
        "MjYxNTU3MzlaFw0xMzA4MjYxNjI3MzlaMF8xCzAJBgNVBAYTAk5MMRIwEAYDVQQK\n" +
        "EwlEaWdpTm90YXIxGjAYBgNVBAMTEURpZ2lOb3RhciBSb290IENBMSAwHgYJKoZI\n" +
        "hvcNAQkBFhFpbmZvQGRpZ2lub3Rhci5ubDCCAiIwDQYJKoZIhvcNAQEBBQADggIP\n" +
        "ADCCAgoCggIBAKywWMEAvdghCAsrmv5uVjAFnxt3kBBBXMMNhxF3joHxynzpjGrt\n" +
        "OHQ1u9rf+bvACTe0lnOBfTMamDn3k2+Vfz25sXWHulFI6ItwPpUExdi2wxbZiLCx\n" +
        "hx1w2oa0DxSLes8Q0XQ2ohJ7d4ZKeeZ73wIRaKVOhq40WJskE3hWIiUeAYtLUXH7\n" +
        "gsxZlmmIWmhTxbkNAjfLS7xmSpB+KgsFB+0WX1WQddhGyRuD4gi+8SPMmR3WKg+D\n" +
        "IBVYJ4Iu+uIiwkmxuQGBap1tnUB3aHZOISpthECFTnaZfILz87cCWdQmARuO361T\n" +
        "BtGuGN3isjrL14g4jqxbKbkZ05j5GAPPSIKGZgsbaQ/J6ziIeiYaBUyS1yTUlvKs\n" +
        "Ui2jR9VS9j/+zoQGcKaqPqLytlY0GFei5IFt58rwatPHkWsCg0F8Fe9rmmRe49A8\n" +
        "5bHre12G+8vmd0nNo2Xc97mcuOQLX5PPzDAaMhzOHGOVpfnq4XSLnukrqTB7oBgf\n" +
        "DhgL5Vup09FsHgdnj5FLqYq80maqkwGIspH6MVzVpsFSCAnNCmOi0yKm6KHZOQaX\n" +
        "9W6NApCMFHs/gM0bnLrEWHIjr7ZWn8Z6QjMpBz+CyeYfBQ3NTCg2i9PIPhzGiO9e\n" +
        "7olk6R3r2ol+MqZp0d3MiJ/R0MlmIdwGZ8WUepptYkx9zOBkgLKeR46jAgMBAAGj\n" +
        "ggEmMIIBIjASBgNVHRMBAf8ECDAGAQH/AgEBMCcGA1UdJQQgMB4GCCsGAQUFBwMB\n" +
        "BggrBgEFBQcDAgYIKwYBBQUHAwQwEQYDVR0gBAowCDAGBgRVHSAAMDMGCCsGAQUF\n" +
        "BwEBBCcwJTAjBggrBgEFBQcwAYYXaHR0cDovL29jc3AuZW50cnVzdC5uZXQwMwYD\n" +
        "VR0fBCwwKjAooCagJIYiaHR0cDovL2NybC5lbnRydXN0Lm5ldC9zZXJ2ZXIxLmNy\n" +
        "bDAdBgNVHQ4EFgQUiGi/4I41xDs4a2L3KDuEgcgM100wCwYDVR0PBAQDAgEGMB8G\n" +
        "A1UdIwQYMBaAFPAXYhNVPbP/CgBr+1CEl/PtYtAaMBkGCSqGSIb2fQdBAAQMMAob\n" +
        "BFY3LjEDAgCBMA0GCSqGSIb3DQEBBQUAA4GBAEa6RcDNcEIGUlkDJUY/pWTds4zh\n" +
        "xbVkp3wSmpwPFhx5fxTyF4HD2L60jl3aqjTB7gPpsL2Pk5QZlNsi3t4UkCV70UOd\n" +
        "ueJRN3o/LOtk4+bjXY2lC0qTHbN80VMLqPjmaf9ghSA9hwhskdtMgRsgfd90q5QP\n" +
        "ZFdYf+hthc3m6IcJ\n" +
        "-----END CERTIFICATE-----";

    // DigiNotar Root CA, compromised certificate
    static String compromisedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "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\n" +
        "-----END CERTIFICATE-----";

    // DigiNotar Public CA 2025, intermediate certificate
    static String intermediateCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIGAzCCA+ugAwIBAgIQHn16Uz1FMEGWQA9xSB9FBDANBgkqhkiG9w0BAQUFADBf\n" +
        "MQswCQYDVQQGEwJOTDESMBAGA1UEChMJRGlnaU5vdGFyMRowGAYDVQQDExFEaWdp\n" +
        "Tm90YXIgUm9vdCBDQTEgMB4GCSqGSIb3DQEJARYRaW5mb0BkaWdpbm90YXIubmww\n" +
        "HhcNMDYwMjA2MTYwNzAyWhcNMjUwMzI4MTYwNzAyWjBmMQswCQYDVQQGEwJOTDES\n" +
        "MBAGA1UEChMJRGlnaU5vdGFyMSEwHwYDVQQDExhEaWdpTm90YXIgUHVibGljIENB\n" +
        "IDIwMjUxIDAeBgkqhkiG9w0BCQEWEWluZm9AZGlnaW5vdGFyLm5sMIIBIjANBgkq\n" +
        "hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs/2eu/I5fMG8lbvPph3e8zfJpZQtg/72\n" +
        "Yx29+ivtKehiF6A3n785XyoY6IT3vlCrhy1CbMOY3M0x1n4YQlv17B0XZ/DqHyBA\n" +
        "SQvnDNbkM9j4NoSy/sRtGsP6PetIFFjrhE9whZuvuSUC1PY4PruEEJp8zOCx4+wU\n" +
        "Zt9xvjy4Xra+bSia5rwccQ/R5FYTGKrYCthOy9C9ud5Fhd++rlVhgdA/78w+Cs2s\n" +
        "xS4i0MAxG75P3/e/bATJKepbydHdDjkyz9o3RW/wdPUXhzEw4EwUjYg6XJrDzMad\n" +
        "6aL9M/eaxDjgz6o48EaWRDrGptaE2uJRuErVz7oOO0p/wYKq/BU+/wIDAQABo4IB\n" +
        "sjCCAa4wOgYIKwYBBQUHAQEELjAsMCoGCCsGAQUFBzABhh5odHRwOi8vdmFsaWRh\n" +
        "dGlvbi5kaWdpbm90YXIubmwwHwYDVR0jBBgwFoAUiGi/4I41xDs4a2L3KDuEgcgM\n" +
        "100wEgYDVR0TAQH/BAgwBgEB/wIBADCBxgYDVR0gBIG+MIG7MIG4Bg5ghBABh2kB\n" +
        "AQEBBQIGBDCBpTAnBggrBgEFBQcCARYbaHR0cDovL3d3dy5kaWdpbm90YXIubmwv\n" +
        "Y3BzMHoGCCsGAQUFBwICMG4abENvbmRpdGlvbnMsIGFzIG1lbnRpb25lZCBvbiBv\n" +
        "dXIgd2Vic2l0ZSAod3d3LmRpZ2lub3Rhci5ubCksIGFyZSBhcHBsaWNhYmxlIHRv\n" +
        "IGFsbCBvdXIgcHJvZHVjdHMgYW5kIHNlcnZpY2VzLjBDBgNVHR8EPDA6MDigNqA0\n" +
        "hjJodHRwOi8vc2VydmljZS5kaWdpbm90YXIubmwvY3JsL3Jvb3QvbGF0ZXN0Q1JM\n" +
        "LmNybDAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFN8zwK+S/jf8ttgWFtDZsZHV\n" +
        "+m6lMA0GCSqGSIb3DQEBBQUAA4ICAQCfV1rmBd9QStEyQ40lT0tqby0/3ez0STuJ\n" +
        "ESBQLQD56XYdb4VFSuqA6xTtiuSVHLoiv2xyISN9FvX3A5VtifkJ00JEaLQJiSsE\n" +
        "wGDkYGl1DT7SsqtAVKdMAuCM+e0j0/RV3hZ6kcrM7/wFccHwM+/TiurR9lgZDzB4\n" +
        "a7++A4XrYyKx9vc9ZwBEnD1nrAe7++gg9cuZgP7e+QL0FBHMjpw+gnCDjr2dzBZC\n" +
        "4r+b8SOqlbPRPexBuNghlc7PfcPIyFis2LJXDRMWiAd3TcfdALwRsuKMR/T+cwyr\n" +
        "asy69OEGHplLT57otQ524BDctDXNzlH9bHEh52QzqkWvIDqs42910IUy1nYNPIUG\n" +
        "yYJV/T7H8Jb6vfMZWe47iUFvtNZCi8+b542gRUwdi+ca+hGviBC9Qr4Wv1pl7CBQ\n" +
        "Hy1axTkHiQawUo/hgmoetCpftugl9yJTfvsBorUV1ZMxn9B1JLSGtWnbUsFRla7G\n" +
        "fNa0IsUkzmmha8XCzvNu0d1PDGtcQyUqmDOE1Hx4cIBeuF8ipuIXkrVCr9zAZ4ZC\n" +
        "hgz6aA1gDTW8whSRJqYEYEQ0pcMEFLyXE+Nz3O8NinO2AuxqKhjMk13203xA7lPY\n" +
        "MnBQ0v7S3qqbp/pvPMiUhOz/VaYted6QmOY5EATBnFiLCuw87JXoAyp382eJ3WX1\n" +
        "hOiR4IX9Tg==\n" +
        "-----END CERTIFICATE-----";

    // The fraudulent certificate issued by above compromised CA
    static String targetCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIFKDCCBBCgAwIBAgIQBeLmpM0J6lTWZbB1/iKiVjANBgkqhkiG9w0BAQUFADBm\n" +
        "MQswCQYDVQQGEwJOTDESMBAGA1UEChMJRGlnaU5vdGFyMSEwHwYDVQQDExhEaWdp\n" +
        "Tm90YXIgUHVibGljIENBIDIwMjUxIDAeBgkqhkiG9w0BCQEWEWluZm9AZGlnaW5v\n" +
        "dGFyLm5sMB4XDTExMDcxMDE5MDYzMFoXDTEzMDcwOTE5MDYzMFowajELMAkGA1UE\n" +
        "BhMCVVMxEzARBgNVBAoTCkdvb2dsZSBJbmMxFjAUBgNVBAcTDU1vdW50YWluIFZp\n" +
        "ZXcxFzAVBgNVBAUTDlBLMDAwMjI5MjAwMDAyMRUwEwYDVQQDEwwqLmdvb2dsZS5j\n" +
        "b20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDNbeKubCV0aCxhOiOS\n" +
        "CSQ/w9HXTYuD5BLKuiqXNw3setdTymeJz2L8aWOHo3nicFNDVwWTgwWomGNr2J6Q\n" +
        "7g1iINNSW0rR4E1l2szRkcnAY6c6i/Eke93nF4i2hDsnIBveolF5yjpuRm73uQQD\n" +
        "ulHjA3BFRF/PTi0fw2/Yt+8ieoMuNcMWN6Eou5Gqt5YZkWv176ofeCbsBmMrP87x\n" +
        "OhhtTDckCapk4VQZG2XrfzZcV6tdzCp5TI8uHdu17cdzXm1imZ8tyvzFeiCEOQN8\n" +
        "vPNzB/fIr3CJQ5q4uM5aKT3DD5PeVzf4rfJKQNgCTWiIBc9XcWEUuszwAsnmg7e2\n" +
        "EJRdAgMBAAGjggHMMIIByDA6BggrBgEFBQcBAQQuMCwwKgYIKwYBBQUHMAGGHmh0\n" +
        "dHA6Ly92YWxpZGF0aW9uLmRpZ2lub3Rhci5ubDAfBgNVHSMEGDAWgBTfM8Cvkv43\n" +
        "/LbYFhbQ2bGR1fpupTAJBgNVHRMEAjAAMIHGBgNVHSAEgb4wgbswgbgGDmCEEAGH\n" +
        "aQEBAQIEAQICMIGlMCcGCCsGAQUFBwIBFhtodHRwOi8vd3d3LmRpZ2lub3Rhci5u\n" +
        "bC9jcHMwegYIKwYBBQUHAgIwbhpsQ29uZGl0aW9ucywgYXMgbWVudGlvbmVkIG9u\n" +
        "IG91ciB3ZWJzaXRlICh3d3cuZGlnaW5vdGFyLm5sKSwgYXJlIGFwcGxpY2FibGUg\n" +
        "dG8gYWxsIG91ciBwcm9kdWN0cyBhbmQgc2VydmljZXMuMEkGA1UdHwRCMEAwPqA8\n" +
        "oDqGOGh0dHA6Ly9zZXJ2aWNlLmRpZ2lub3Rhci5ubC9jcmwvcHVibGljMjAyNS9s\n" +
        "YXRlc3RDUkwuY3JsMA4GA1UdDwEB/wQEAwIEsDAbBgNVHREEFDASgRBhZG1pbkBn\n" +
        "b29nbGUuY29tMB0GA1UdDgQWBBQHSn0WJzIo0eMBMQUNsMqN6eF/7TANBgkqhkiG\n" +
        "9w0BAQUFAAOCAQEAAs5dL7N9wzRJkI4Aq4lC5t8j5ZadqnqUcgYLADzSv4ExytNH\n" +
        "UY2nH6iVTihC0UPSsILWraoeApdT7Rphz/8DLQEBRGdeKWAptNM3EbiXtQaZT2uB\n" +
        "pidL8UoafX0kch3f71Y1scpBEjvu5ZZLnjg0A8AL0tnsereOVdDpU98bKqdbbrnM\n" +
        "FRmBlSf7xdaNca6JJHeEpga4E9Ty683CmccrSGXdU2tTCuHEJww+iOAUtPIZcsum\n" +
        "U7/eYeY1pMyGLyIjbNgRY7nDzRwvM/BsbL9eh4/mSQj/4nncqJd22sVQpCggQiVK\n" +
        "baB2sVGcVNBkK55bT8gPqnx8JypubyUvayzZGg==\n" +
        "-----END CERTIFICATE-----";

    private static String tmAlgorithm;               // trust manager

    public static void main(String args[]) throws Exception {
        // Get the customized arguments.
        parseArguments(args);

        X509TrustManager tm = getTrustManager();
        X509Certificate[] chain = getFraudulentChain();

        Exception reservedException = null;
        try {
            tm.checkClientTrusted(chain, "RSA");
        } catch (CertificateException ce) {
            reservedException = ce;
        }

        if (reservedException == null) {
            throw new Exception("Unable to block fraudulent certificate");
        }

        reservedException = null;
        try {
            tm.checkServerTrusted(chain, "RSA");
        } catch (CertificateException ce) {
            reservedException = ce;
        }

        if (reservedException == null) {
            throw new Exception("Unable to block fraudulent certificate");
        }

        System.out.println(
            "The expected untrusted cert exception: " + reservedException);
    }

    private static void parseArguments(String[] args) {
        tmAlgorithm = args[0];
    }

    private static X509TrustManager getTrustManager() throws Exception {
        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // create a key store
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // import the trusted cert
        try (ByteArrayInputStream is =
                new ByteArrayInputStream(trustedCertStr.getBytes())) {
            Certificate trustedCert = cf.generateCertificate(is);
            ks.setCertificateEntry("RSA Export Signer", trustedCert);
        }

        // create the trust manager
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmAlgorithm);
        tmf.init(ks);

        return (X509TrustManager)tmf.getTrustManagers()[0];
    }

    private static X509Certificate[] getFraudulentChain() throws Exception {
        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        X509Certificate[] chain = new X509Certificate[4];
        try (ByteArrayInputStream is =
                new ByteArrayInputStream(targetCertStr.getBytes())) {
            chain[0] = (X509Certificate)cf.generateCertificate(is);
        }

        try (ByteArrayInputStream is =
                new ByteArrayInputStream(intermediateCertStr.getBytes())) {
            chain[1] = (X509Certificate)cf.generateCertificate(is);
        }

        try (ByteArrayInputStream is =
                new ByteArrayInputStream(compromisedCertStr.getBytes())) {
            chain[2] = (X509Certificate)cf.generateCertificate(is);
        }

        try (ByteArrayInputStream is =
                new ByteArrayInputStream(untrustedCrossCertStr.getBytes())) {
            chain[3] = (X509Certificate)cf.generateCertificate(is);
        }

        return chain;
    }
}

