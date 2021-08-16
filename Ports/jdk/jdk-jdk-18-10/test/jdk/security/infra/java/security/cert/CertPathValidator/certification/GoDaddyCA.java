/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8196141
 * @summary Interoperability tests with GoDaddy/Starfield CA
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath GoDaddyCA OCSP
 * @run main/othervm -Djava.security.debug=certpath GoDaddyCA CRL
 */

/*
 * Obtain test artifacts for GoDaddy/Starfield CAs from:
 *
 * Go Daddy Root Certificate Authority - G2:
 *    valid: https://valid.gdig2.catest.godaddy.com/
 *    expired: https://expired.gdig2.catest.godaddy.com/
 *    revoked: https://revoked.gdig2.catest.godaddy.com/
 *
 * Starfield Root Certificate Authority - G2:
 *    valid: https://valid.sfig2.catest.starfieldtech.com/
 *    expired: https://expired.sfig2.catest.starfieldtech.com/
 *    revoked: https://revoked.sfig2.catest.starfieldtech.com/
 */
public class GoDaddyCA {

    public static void main(String[] args) throws Exception {

        ValidatePathWithParams pathValidator = new ValidatePathWithParams(null);

        if (args.length >= 1 && "CRL".equalsIgnoreCase(args[0])) {
            // CRL check
            pathValidator.enableCRLCheck();
        } else {
            // OCSP check by default
            pathValidator.enableOCSPCheck();
        }

        new GoDaddyGdig2().runTest(pathValidator);
        new GoDaddySfig2().runTest(pathValidator);
    }
}

class GoDaddyGdig2 {

    // Owner: CN=Go Daddy Secure Certificate Authority - G2,
    // OU=http://certs.godaddy.com/repository/, O="GoDaddy.com, Inc.",
    // L=Scottsdale, ST=Arizona, C=US
    // Issuer: CN=Go Daddy Root Certificate Authority - G2, O="GoDaddy.com, Inc.",
    // L=Scottsdale, ST=Arizona, C=US
    private static final String INT = "-----BEGIN CERTIFICATE-----\n"
            + "MIIE0DCCA7igAwIBAgIBBzANBgkqhkiG9w0BAQsFADCBgzELMAkGA1UEBhMCVVMx\n"
            + "EDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxGjAYBgNVBAoT\n"
            + "EUdvRGFkZHkuY29tLCBJbmMuMTEwLwYDVQQDEyhHbyBEYWRkeSBSb290IENlcnRp\n"
            + "ZmljYXRlIEF1dGhvcml0eSAtIEcyMB4XDTExMDUwMzA3MDAwMFoXDTMxMDUwMzA3\n"
            + "MDAwMFowgbQxCzAJBgNVBAYTAlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQH\n"
            + "EwpTY290dHNkYWxlMRowGAYDVQQKExFHb0RhZGR5LmNvbSwgSW5jLjEtMCsGA1UE\n"
            + "CxMkaHR0cDovL2NlcnRzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkvMTMwMQYDVQQD\n"
            + "EypHbyBEYWRkeSBTZWN1cmUgQ2VydGlmaWNhdGUgQXV0aG9yaXR5IC0gRzIwggEi\n"
            + "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC54MsQ1K92vdSTYuswZLiBCGzD\n"
            + "BNliF44v/z5lz4/OYuY8UhzaFkVLVat4a2ODYpDOD2lsmcgaFItMzEUz6ojcnqOv\n"
            + "K/6AYZ15V8TPLvQ/MDxdR/yaFrzDN5ZBUY4RS1T4KL7QjL7wMDge87Am+GZHY23e\n"
            + "cSZHjzhHU9FGHbTj3ADqRay9vHHZqm8A29vNMDp5T19MR/gd71vCxJ1gO7GyQ5HY\n"
            + "pDNO6rPWJ0+tJYqlxvTV0KaudAVkV4i1RFXULSo6Pvi4vekyCgKUZMQWOlDxSq7n\n"
            + "eTOvDCAHf+jfBDnCaQJsY1L6d8EbyHSHyLmTGFBUNUtpTrw700kuH9zB0lL7AgMB\n"
            + "AAGjggEaMIIBFjAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNV\n"
            + "HQ4EFgQUQMK9J47MNIMwojPX+2yz8LQsgM4wHwYDVR0jBBgwFoAUOpqFBxBnKLbv\n"
            + "9r0FQW4gwZTaD94wNAYIKwYBBQUHAQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8v\n"
            + "b2NzcC5nb2RhZGR5LmNvbS8wNQYDVR0fBC4wLDAqoCigJoYkaHR0cDovL2NybC5n\n"
            + "b2RhZGR5LmNvbS9nZHJvb3QtZzIuY3JsMEYGA1UdIAQ/MD0wOwYEVR0gADAzMDEG\n"
            + "CCsGAQUFBwIBFiVodHRwczovL2NlcnRzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkv\n"
            + "MA0GCSqGSIb3DQEBCwUAA4IBAQAIfmyTEMg4uJapkEv/oV9PBO9sPpyIBslQj6Zz\n"
            + "91cxG7685C/b+LrTW+C05+Z5Yg4MotdqY3MxtfWoSKQ7CC2iXZDXtHwlTxFWMMS2\n"
            + "RJ17LJ3lXubvDGGqv+QqG+6EnriDfcFDzkSnE3ANkR/0yBOtg2DZ2HKocyQetawi\n"
            + "DsoXiWJYRBuriSUBAA/NxBti21G00w9RKpv0vHP8ds42pM3Z2Czqrpv1KrKQ0U11\n"
            + "GIo/ikGQI31bS/6kA1ibRrLDYGCD+H1QQc7CoZDDu+8CL9IVVO5EFdkKrqeKM+2x\n"
            + "LXY2JtwE65/3YR8V3Idv7kaWKK2hJn0KCacuBKONvPi8BDAB\n"
            + "-----END CERTIFICATE-----";

    // 1.3.6.1.4.1.311.60.2.1.3=US/1.3.6.1.4.1.311.60.2.1.2=Delaware/businessCategory=Private
    // Organization/serialNumber=5510922, C=US, ST=Arizona, L=Scottsdale, O=GoDaddy INC., CN=valid.gdig2.catest.godaddy.com
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHbzCCBlegAwIBAgIIC3Go9uPeseowDQYJKoZIhvcNAQELBQAwgbQxCzAJBgNV\n" +
            "BAYTAlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQHEwpTY290dHNkYWxlMRow\n" +
            "GAYDVQQKExFHb0RhZGR5LmNvbSwgSW5jLjEtMCsGA1UECxMkaHR0cDovL2NlcnRz\n" +
            "LmdvZGFkZHkuY29tL3JlcG9zaXRvcnkvMTMwMQYDVQQDEypHbyBEYWRkeSBTZWN1\n" +
            "cmUgQ2VydGlmaWNhdGUgQXV0aG9yaXR5IC0gRzIwHhcNMTcwOTE1MjMyMzAyWhcN\n" +
            "MTkwOTE1MjMyMzAyWjCB1TETMBEGCysGAQQBgjc8AgEDEwJVUzEZMBcGCysGAQQB\n" +
            "gjc8AgECEwhEZWxhd2FyZTEdMBsGA1UEDxMUUHJpdmF0ZSBPcmdhbml6YXRpb24x\n" +
            "EDAOBgNVBAUTBzU1MTA5MjIxCzAJBgNVBAYTAlVTMRAwDgYDVQQIEwdBcml6b25h\n" +
            "MRMwEQYDVQQHEwpTY290dHNkYWxlMRUwEwYDVQQKEwxHb0RhZGR5IElOQy4xJzAl\n" +
            "BgNVBAMTHnZhbGlkLmdkaWcyLmNhdGVzdC5nb2RhZGR5LmNvbTCCASIwDQYJKoZI\n" +
            "hvcNAQEBBQADggEPADCCAQoCggEBAO3xTbLfdIHiG1MIsBCz0oIg5vBxlzZyK5Rw\n" +
            "DM6A/TWUDelFWyYj6fZDXYyHby4nAK9ibfhiT2f+q+5lEslye5Mt9gC39pZbpHE2\n" +
            "eyJgmtNgmPGq15pf/87JE697BRwp9CWJP3yNYeamFl/F2THZOqlXCiSRbIGZ5TsZ\n" +
            "sVb1vjFPmh249Ujw1zSThY9hA669Cyp3xb4iTowjCqdNYqbn22Jbk0SEXPYzLMf0\n" +
            "mlY8xZ/e/8NxzJgev3N1LR3bPEijLYDZeZJ6WKc75pqNvgo8A+dEeX9bxFkCnstY\n" +
            "6Iq0HTJua0TTD6V585YXNm4Z5OxjBE5kPkkFfwW0bb5dRZp86HUCAwEAAaOCA2Aw\n" +
            "ggNcMAwGA1UdEwEB/wQCMAAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMC\n" +
            "MA4GA1UdDwEB/wQEAwIFoDA1BgNVHR8ELjAsMCqgKKAmhiRodHRwOi8vY3JsLmdv\n" +
            "ZGFkZHkuY29tL2dkaWcyczMtOS5jcmwwXAYDVR0gBFUwUzBIBgtghkgBhv1tAQcX\n" +
            "AzA5MDcGCCsGAQUFBwIBFitodHRwOi8vY2VydGlmaWNhdGVzLmdvZGFkZHkuY29t\n" +
            "L3JlcG9zaXRvcnkvMAcGBWeBDAEBMHYGCCsGAQUFBwEBBGowaDAkBggrBgEFBQcw\n" +
            "AYYYaHR0cDovL29jc3AuZ29kYWRkeS5jb20vMEAGCCsGAQUFBzAChjRodHRwOi8v\n" +
            "Y2VydGlmaWNhdGVzLmdvZGFkZHkuY29tL3JlcG9zaXRvcnkvZ2RpZzIuY3J0MB8G\n" +
            "A1UdIwQYMBaAFEDCvSeOzDSDMKIz1/tss/C0LIDOME0GA1UdEQRGMESCHnZhbGlk\n" +
            "LmdkaWcyLmNhdGVzdC5nb2RhZGR5LmNvbYIid3d3LnZhbGlkLmdkaWcyLmNhdGVz\n" +
            "dC5nb2RhZGR5LmNvbTAdBgNVHQ4EFgQUKSs41O+5SnkjAEaNyHk6sxq5sn8wggF/\n" +
            "BgorBgEEAdZ5AgQCBIIBbwSCAWsBaQB3AFYUBpov18Ls0/XhvUSyPsdGdrm8mRFc\n" +
            "wO+UmFXWidDdAAABXofbjGMAAAQDAEgwRgIhAPZEqPZAlYpSTx+R/+7mOUa+BcBz\n" +
            "U1JHZDpcy98am0glAiEA1u2FxjgAa4L5HVGYV2LSQZIltGRJ8mBT8V0JVsdm3dsA\n" +
            "dgDuS723dc5guuFCaR+r4Z5mow9+X7By2IMAxHuJeqj9ywAAAV6H25ASAAAEAwBH\n" +
            "MEUCIQCFowkRXyR8gkX8cL7RbPSwiKCHy/1I1WVzpinmrHlZFQIgE5nShGeK7cqT\n" +
            "j2C9FfrPc/Axe3/pzAFxD/BNQD1RO5sAdgCkuQmQtBhYFIe7E6LMZ3AKPDWYBPkb\n" +
            "37jjd80OyA3cEAAAAV6H25GdAAAEAwBHMEUCIBQrE+FqILUhI0wdp2X+lf/e3UG1\n" +
            "gyxHmSVeN2+CkrXPAiEA1mIIVmLNURGyI8wnZ5KRnBPOKYM2MC54RJ8CFrEHIz4w\n" +
            "DQYJKoZIhvcNAQELBQADggEBADInvf3eS6SgQ1qxPx4RT2hPeU5frlWJWcOWUdZB\n" +
            "6mVNcmUQMkYnjkg8+PQ782HGP0DvAfcIRDhSfXdIqzEk8MPUq1XHEOfwRzLpTiCN\n" +
            "FQDQIt1LXnzESCUurJS8r4mxgaVLAwHFytOTDrQn0Xfs93dm0tnRGAg7iBg+N33V\n" +
            "zOR4aqojdDUWa1Rr4WFqZMkZIxzREQCYC8HXSYqLA1oPuoMMog8dId7XSalBmGJ4\n" +
            "KQVsZ0/Hpi0y9k/Zw5obGcEYJWMbuU1iaEkvdtXOiXEQfJ1WS+Yy55J4GSjpIiop\n" +
            "qDZD88xA9r7ttzM/khao7jfIpVWG2HuX0JlHWdh3y9aegiw=\n" +
            "-----END CERTIFICATE-----";

    // 1.3.6.1.4.1.311.60.2.1.3=US/1.3.6.1.4.1.311.60.2.1.2=Delaware/businessCategory=Private
    // Organization/serialNumber=5510922, C=US, ST=Arizona, L=Scottsdale, O=GoDaddy INC., CN=revoked.gdig2.catest.godaddy.com
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHdDCCBlygAwIBAgIIEBJV3vmogM8wDQYJKoZIhvcNAQELBQAwgbQxCzAJBgNV\n" +
            "BAYTAlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQHEwpTY290dHNkYWxlMRow\n" +
            "GAYDVQQKExFHb0RhZGR5LmNvbSwgSW5jLjEtMCsGA1UECxMkaHR0cDovL2NlcnRz\n" +
            "LmdvZGFkZHkuY29tL3JlcG9zaXRvcnkvMTMwMQYDVQQDEypHbyBEYWRkeSBTZWN1\n" +
            "cmUgQ2VydGlmaWNhdGUgQXV0aG9yaXR5IC0gRzIwHhcNMTcwOTE1MjMyMzAzWhcN\n" +
            "MTkwOTE1MjMyMzAzWjCB1zETMBEGCysGAQQBgjc8AgEDEwJVUzEZMBcGCysGAQQB\n" +
            "gjc8AgECEwhEZWxhd2FyZTEdMBsGA1UEDxMUUHJpdmF0ZSBPcmdhbml6YXRpb24x\n" +
            "EDAOBgNVBAUTBzU1MTA5MjIxCzAJBgNVBAYTAlVTMRAwDgYDVQQIEwdBcml6b25h\n" +
            "MRMwEQYDVQQHEwpTY290dHNkYWxlMRUwEwYDVQQKEwxHb0RhZGR5IElOQy4xKTAn\n" +
            "BgNVBAMTIHJldm9rZWQuZ2RpZzIuY2F0ZXN0LmdvZGFkZHkuY29tMIIBIjANBgkq\n" +
            "hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxCuBsAR2XGf05mYOuag+0aS4lBuLO5/f\n" +
            "kEO7KNo7BcdY7J78yXYRYW0jGnV29bjrQZJfu5yv5bU+OjTIDVbCWZAwtBXEKrJj\n" +
            "riIOUXi3hXphtlyMMAaiXQoA84jwS634DsD0w6XUUP2Lem8jC3RudjvmkDQHoY3M\n" +
            "uhhS7jLxKnYKnXbLwlqxpdwmEgbqIb5DN5snLAyinTkALLVWZ6RneIuSjhKWbuef\n" +
            "cEKFScHm6SFsKraltV/T17SWi6zQd/AypKA8JeWXD9WZcsSR9z/41VMJbvTeuP+d\n" +
            "ZBA4dqPsBTl4N4i54rNEyzMyxDwdvIGrJJ+FVRMKoYjuUi5wY9zO4QIDAQABo4ID\n" +
            "YzCCA18wDAYDVR0TAQH/BAIwADAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUH\n" +
            "AwIwDgYDVR0PAQH/BAQDAgWgMDUGA1UdHwQuMCwwKqAooCaGJGh0dHA6Ly9jcmwu\n" +
            "Z29kYWRkeS5jb20vZ2RpZzJzMy05LmNybDBcBgNVHSAEVTBTMEgGC2CGSAGG/W0B\n" +
            "BxcDMDkwNwYIKwYBBQUHAgEWK2h0dHA6Ly9jZXJ0aWZpY2F0ZXMuZ29kYWRkeS5j\n" +
            "b20vcmVwb3NpdG9yeS8wBwYFZ4EMAQEwdgYIKwYBBQUHAQEEajBoMCQGCCsGAQUF\n" +
            "BzABhhhodHRwOi8vb2NzcC5nb2RhZGR5LmNvbS8wQAYIKwYBBQUHMAKGNGh0dHA6\n" +
            "Ly9jZXJ0aWZpY2F0ZXMuZ29kYWRkeS5jb20vcmVwb3NpdG9yeS9nZGlnMi5jcnQw\n" +
            "HwYDVR0jBBgwFoAUQMK9J47MNIMwojPX+2yz8LQsgM4wUQYDVR0RBEowSIIgcmV2\n" +
            "b2tlZC5nZGlnMi5jYXRlc3QuZ29kYWRkeS5jb22CJHd3dy5yZXZva2VkLmdkaWcy\n" +
            "LmNhdGVzdC5nb2RhZGR5LmNvbTAdBgNVHQ4EFgQUCJELlWq8+ntmR5JTjmZMG+HI\n" +
            "e5EwggF+BgorBgEEAdZ5AgQCBIIBbgSCAWoBaAB1AFYUBpov18Ls0/XhvUSyPsdG\n" +
            "drm8mRFcwO+UmFXWidDdAAABXofbk3cAAAQDAEYwRAIgHo8UllsN8FcaF16xx7kT\n" +
            "vQU1wM7qUKnhN38/z8dU4QUCIFrzGJyajoVPQ2fzOTb9ygzA7T3wqsnT3ML5/KJ6\n" +
            "+6+CAHYA7ku9t3XOYLrhQmkfq+GeZqMPfl+wctiDAMR7iXqo/csAAAFeh9uXHQAA\n" +
            "BAMARzBFAiEA5DENZZT7SBxNRvo9yFHNNeWqH2d4uqGUwc1rKILrMGsCIHZ3N4dZ\n" +
            "zv/J+7fbLP1nrAmdUT92ow1bhtMPuq2PfXsAAHcApLkJkLQYWBSHuxOizGdwCjw1\n" +
            "mAT5G9+443fNDsgN3BAAAAFeh9uYjAAABAMASDBGAiEAyY8ylnGHiH5L3yXE7BsH\n" +
            "v75ja2RtuuYbMADAlDK/ZDoCIQDwuCq3x+egpB/GISxTnwkrDwhNhhIJNyk5F4j1\n" +
            "/J8A0DANBgkqhkiG9w0BAQsFAAOCAQEAMGot6gBZ77HIDMb1n/HPrKdSHN0ngq7Z\n" +
            "rhrkgbp+mH1Cs1lZA3qldMDxKXgNiodFqU/e4VewasQ9tJMmDXrTZIHualJGmIvq\n" +
            "ISvV0ZUfSW/sJmo0ZDw8iBM993LDkA4wSc6SunhjOwu3LBfl9aKkeq6IhUEAG8X7\n" +
            "54oO4iApt+APLMyeV9lZ/T7MGVbAjwdm+T1RMa/Ca99BahaRWN7hiM+zS3Ly+l6G\n" +
            "7kqAkBFuJWbbZImADZ2RPldY6hBzTk6MT2hLCV40UD8JqwJo+qq7nGfJdTaFyZI6\n" +
            "nJvrVATO7jL64YFP3xlVi8EQaCeKdZdn+BCCNA/ja0mWMj8EU9Islg==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED, null, System.out);
    }
}

class GoDaddySfig2 {

    // Owner: CN=Starfield Secure Certificate Authority - G2,
    // OU=http://certs.starfieldtech.com/repository/, O="Starfield Technologies, Inc.",
    // L=Scottsdale, ST=Arizona, C=US
    // Issuer: CN=Starfield Root Certificate Authority - G2,
    // O="Starfield Technologies, Inc.", L=Scottsdale, ST=Arizona, C=US
    private static final String INT = "-----BEGIN CERTIFICATE-----\n"
            + "MIIFADCCA+igAwIBAgIBBzANBgkqhkiG9w0BAQsFADCBjzELMAkGA1UEBhMCVVMx\n"
            + "EDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxJTAjBgNVBAoT\n"
            + "HFN0YXJmaWVsZCBUZWNobm9sb2dpZXMsIEluYy4xMjAwBgNVBAMTKVN0YXJmaWVs\n"
            + "ZCBSb290IENlcnRpZmljYXRlIEF1dGhvcml0eSAtIEcyMB4XDTExMDUwMzA3MDAw\n"
            + "MFoXDTMxMDUwMzA3MDAwMFowgcYxCzAJBgNVBAYTAlVTMRAwDgYDVQQIEwdBcml6\n"
            + "b25hMRMwEQYDVQQHEwpTY290dHNkYWxlMSUwIwYDVQQKExxTdGFyZmllbGQgVGVj\n"
            + "aG5vbG9naWVzLCBJbmMuMTMwMQYDVQQLEypodHRwOi8vY2VydHMuc3RhcmZpZWxk\n"
            + "dGVjaC5jb20vcmVwb3NpdG9yeS8xNDAyBgNVBAMTK1N0YXJmaWVsZCBTZWN1cmUg\n"
            + "Q2VydGlmaWNhdGUgQXV0aG9yaXR5IC0gRzIwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n"
            + "DwAwggEKAoIBAQDlkGZL7PlGcakgg77pbL9KyUhpgXVObST2yxcT+LBxWYR6ayuF\n"
            + "pDS1FuXLzOlBcCykLtb6Mn3hqN6UEKwxwcDYav9ZJ6t21vwLdGu4p64/xFT0tDFE\n"
            + "3ZNWjKRMXpuJyySDm+JXfbfYEh/JhW300YDxUJuHrtQLEAX7J7oobRfpDtZNuTlV\n"
            + "Bv8KJAV+L8YdcmzUiymMV33a2etmGtNPp99/UsQwxaXJDgLFU793OGgGJMNmyDd+\n"
            + "MB5FcSM1/5DYKp2N57CSTTx/KgqT3M0WRmX3YISLdkuRJ3MUkuDq7o8W6o0OPnYX\n"
            + "v32JgIBEQ+ct4EMJddo26K3biTr1XRKOIwSDAgMBAAGjggEsMIIBKDAPBgNVHRMB\n"
            + "Af8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQUJUWBaFAmOD07LSy+\n"
            + "zWrZtj2zZmMwHwYDVR0jBBgwFoAUfAwyH6fZMH/EfWijYqihzqsHWycwOgYIKwYB\n"
            + "BQUHAQEELjAsMCoGCCsGAQUFBzABhh5odHRwOi8vb2NzcC5zdGFyZmllbGR0ZWNo\n"
            + "LmNvbS8wOwYDVR0fBDQwMjAwoC6gLIYqaHR0cDovL2NybC5zdGFyZmllbGR0ZWNo\n"
            + "LmNvbS9zZnJvb3QtZzIuY3JsMEwGA1UdIARFMEMwQQYEVR0gADA5MDcGCCsGAQUF\n"
            + "BwIBFitodHRwczovL2NlcnRzLnN0YXJmaWVsZHRlY2guY29tL3JlcG9zaXRvcnkv\n"
            + "MA0GCSqGSIb3DQEBCwUAA4IBAQBWZcr+8z8KqJOLGMfeQ2kTNCC+Tl94qGuc22pN\n"
            + "QdvBE+zcMQAiXvcAngzgNGU0+bE6TkjIEoGIXFs+CFN69xpk37hQYcxTUUApS8L0\n"
            + "rjpf5MqtJsxOYUPl/VemN3DOQyuwlMOS6eFfqhBJt2nk4NAfZKQrzR9voPiEJBjO\n"
            + "eT2pkb9UGBOJmVQRDVXFJgt5T1ocbvlj2xSApAer+rKluYjdkf5lO6Sjeb6JTeHQ\n"
            + "sPTIFwwKlhR8Cbds4cLYVdQYoKpBaXAko7nv6VrcPuuUSvC33l8Odvr7+2kDRUBQ\n"
            + "7nIMpBKGgc0T0U7EPMpODdIm8QC3tKai4W56gf0wrHofx1l7\n"
            + "-----END CERTIFICATE-----";

    // 1.3.6.1.4.1.311.60.2.1.3=US/1.3.6.1.4.1.311.60.2.1.2=Arizona/businessCategory=Private
    // Organization/serialNumber=R17247416, C=US, ST=Arizona, L=Scottsdale, O=Starfield Technologies, LLC,
    // CN=valid.sfig2.catest.starfieldtech.com
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHuzCCBqOgAwIBAgIIaZoUcUIjkGwwDQYJKoZIhvcNAQELBQAwgcYxCzAJBgNV\n" +
            "BAYTAlVTMRAwDgYDVQQIEwdBcml6b25hMRMwEQYDVQQHEwpTY290dHNkYWxlMSUw\n" +
            "IwYDVQQKExxTdGFyZmllbGQgVGVjaG5vbG9naWVzLCBJbmMuMTMwMQYDVQQLEypo\n" +
            "dHRwOi8vY2VydHMuc3RhcmZpZWxkdGVjaC5jb20vcmVwb3NpdG9yeS8xNDAyBgNV\n" +
            "BAMTK1N0YXJmaWVsZCBTZWN1cmUgQ2VydGlmaWNhdGUgQXV0aG9yaXR5IC0gRzIw\n" +
            "HhcNMTcwOTE3MDM0ODAxWhcNMTkwOTE3MDM0ODAxWjCB6zETMBEGCysGAQQBgjc8\n" +
            "AgEDEwJVUzEYMBYGCysGAQQBgjc8AgECEwdBcml6b25hMR0wGwYDVQQPExRQcml2\n" +
            "YXRlIE9yZ2FuaXphdGlvbjESMBAGA1UEBRMJUjE3MjQ3NDE2MQswCQYDVQQGEwJV\n" +
            "UzEQMA4GA1UECBMHQXJpem9uYTETMBEGA1UEBxMKU2NvdHRzZGFsZTEkMCIGA1UE\n" +
            "ChMbU3RhcmZpZWxkIFRlY2hub2xvZ2llcywgTExDMS0wKwYDVQQDEyR2YWxpZC5z\n" +
            "ZmlnMi5jYXRlc3Quc3RhcmZpZWxkdGVjaC5jb20wggEiMA0GCSqGSIb3DQEBAQUA\n" +
            "A4IBDwAwggEKAoIBAQDVxhI45IQtNrJuun7HU8v2CKg/h/euysft2VrRsaGSMAln\n" +
            "V6TtpWj2UGm7OmzE2NNzOhD9JJQSc1W6aHEsCTVJ148sgldFFmP39cboBFoLCFlJ\n" +
            "DxsVGeyKu+KlDKq7Vp2+ty3TeFNOBXEVtEc8SsC8mVjsk2VWW7X/fCVFYEzzyPUI\n" +
            "sJPWahNOW2wVxNWKeW5jwzeNMOFVQiT9+YpZVQnV06uK3rPd9tVYU5SfdfPVpScY\n" +
            "/O/tyZyflTGuXZ+YXn1CYRsOq3VypVFfhXunV5prQ/vTnyjddVWce1wwoUT5DvFO\n" +
            "/0vcWolHktiOAJkmAiGRfHvjhxW8mkjKqaMnstKRAgMBAAGjggOEMIIDgDAMBgNV\n" +
            "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAOBgNVHQ8B\n" +
            "Af8EBAMCBaAwOwYDVR0fBDQwMjAwoC6gLIYqaHR0cDovL2NybC5zdGFyZmllbGR0\n" +
            "ZWNoLmNvbS9zZmlnMnMzLTEuY3JsMGIGA1UdIARbMFkwTgYLYIZIAYb9bgEHFwMw\n" +
            "PzA9BggrBgEFBQcCARYxaHR0cDovL2NlcnRpZmljYXRlcy5zdGFyZmllbGR0ZWNo\n" +
            "LmNvbS9yZXBvc2l0b3J5LzAHBgVngQwBATCBggYIKwYBBQUHAQEEdjB0MCoGCCsG\n" +
            "AQUFBzABhh5odHRwOi8vb2NzcC5zdGFyZmllbGR0ZWNoLmNvbS8wRgYIKwYBBQUH\n" +
            "MAKGOmh0dHA6Ly9jZXJ0aWZpY2F0ZXMuc3RhcmZpZWxkdGVjaC5jb20vcmVwb3Np\n" +
            "dG9yeS9zZmlnMi5jcnQwHwYDVR0jBBgwFoAUJUWBaFAmOD07LSy+zWrZtj2zZmMw\n" +
            "WQYDVR0RBFIwUIIkdmFsaWQuc2ZpZzIuY2F0ZXN0LnN0YXJmaWVsZHRlY2guY29t\n" +
            "gih3d3cudmFsaWQuc2ZpZzIuY2F0ZXN0LnN0YXJmaWVsZHRlY2guY29tMB0GA1Ud\n" +
            "DgQWBBTxiYdHMn55sMWTFgp7xif7ludWTjCCAX4GCisGAQQB1nkCBAIEggFuBIIB\n" +
            "agFoAHcAVhQGmi/XwuzT9eG9RLI+x0Z2ubyZEVzA75SYVdaJ0N0AAAFejfR7OAAA\n" +
            "BAMASDBGAiEA/s7a5OGhtaCutT1l4KNE7dUbM3WGUExG/ZJ+Y6IH3nUCIQCvpVJf\n" +
            "Y0XBInIUv391hNzSEhv6nvIBEjZtKdvGcP8/5QB2AO5Lvbd1zmC64UJpH6vhnmaj\n" +
            "D35fsHLYgwDEe4l6qP3LAAABXo30fxEAAAQDAEcwRQIhANqG9yfi3ax0pTnwr4Ti\n" +
            "wVfUrZclJDS06ePkTHppLkLTAiBTRKkVf1df4Irvmd7neT1wdS2fhDxmnVIYAN5J\n" +
            "6tOGDQB1AKS5CZC0GFgUh7sTosxncAo8NZgE+RvfuON3zQ7IDdwQAAABXo30gFsA\n" +
            "AAQDAEYwRAIgb8Xc54M+QD4wfSWLj5Ae/wrSEgRp7Kbf4Lf4vT4W0usCIGAShkJI\n" +
            "CRxoudQDRxooNJhfXgsTB8QhwFC9PUPo3ZV+MA0GCSqGSIb3DQEBCwUAA4IBAQBt\n" +
            "TqvwxqrkPYm/ssbN9cpVWlrQPw3DblsAEV6gnrrTJMd7HB042H3HLUiitddRjO40\n" +
            "0EJM/tUOSGcWfqnJHWFDKoWzdrF5lHAzSRkMjdXgY9TTN5K5tUMEpfRjtink/zoY\n" +
            "pNyc5ua4SXn94KfMZcOYGRvUM+0q6vLRBBMH541E3M6q6JbEBqZJFY8gBWwYqHH0\n" +
            "xNGahm5++v4trFFCJzSfvfV1v+rnqy8tRivi7ZFLXWCcSyAqMH+T9Q36lKeFtaw4\n" +
            "Sapf+dh2yrd2IBLW5eaAD13nCAjO/W0GuC7zw4+4mhW5+DTVJXrCkK5XddkVLhML\n" +
            "k5pMoIv5EsFIm0Cs+DfF\n" +
            "-----END CERTIFICATE-----";

    // 1.3.6.1.4.1.311.60.2.1.3=US/1.3.6.1.4.1.311.60.2.1.2=Arizona/businessCategory=Private
    // Organization/serialNumber=R17247416, C=US, ST=Arizona, L=Scottsdale, O=Starfield Technologies, LLC,
    // CN=revoked.sfig2.catest.starfieldtech.com
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHwTCCBqmgAwIBAgIJAPc1qVz+WDxpMA0GCSqGSIb3DQEBCwUAMIHGMQswCQYD\n" +
            "VQQGEwJVUzEQMA4GA1UECBMHQXJpem9uYTETMBEGA1UEBxMKU2NvdHRzZGFsZTEl\n" +
            "MCMGA1UEChMcU3RhcmZpZWxkIFRlY2hub2xvZ2llcywgSW5jLjEzMDEGA1UECxMq\n" +
            "aHR0cDovL2NlcnRzLnN0YXJmaWVsZHRlY2guY29tL3JlcG9zaXRvcnkvMTQwMgYD\n" +
            "VQQDEytTdGFyZmllbGQgU2VjdXJlIENlcnRpZmljYXRlIEF1dGhvcml0eSAtIEcy\n" +
            "MB4XDTE3MDkxOTEzMDkwMVoXDTE5MDkxOTEzMDkwMVowge0xEzARBgsrBgEEAYI3\n" +
            "PAIBAxMCVVMxGDAWBgsrBgEEAYI3PAIBAhMHQXJpem9uYTEdMBsGA1UEDxMUUHJp\n" +
            "dmF0ZSBPcmdhbml6YXRpb24xEjAQBgNVBAUTCVIxNzI0NzQxNjELMAkGA1UEBhMC\n" +
            "VVMxEDAOBgNVBAgTB0FyaXpvbmExEzARBgNVBAcTClNjb3R0c2RhbGUxJDAiBgNV\n" +
            "BAoTG1N0YXJmaWVsZCBUZWNobm9sb2dpZXMsIExMQzEvMC0GA1UEAxMmcmV2b2tl\n" +
            "ZC5zZmlnMi5jYXRlc3Quc3RhcmZpZWxkdGVjaC5jb20wggEiMA0GCSqGSIb3DQEB\n" +
            "AQUAA4IBDwAwggEKAoIBAQCWsAZC9goWW6yzg9HiLjCG4Gv2PCHlUIQGqyhc1y9a\n" +
            "YZVXUI27/NhHjNNMTwP9TKmncrxnGaTZ9+ZCS1JlSgsNYQcLKKZW+SiEOzwpOfwV\n" +
            "dOCSWrt/EDyJHktx3VIbfi+mD7dvzH3B/iGxMrmdCGIy3xiVAc7MkfsWzcLlPUP3\n" +
            "oUpPBYyzWqZ2tVsBDigoirERFqZNfHZ7ZNMnn8FcmAt7udKjAAewNRlwzR7ZVp5s\n" +
            "f5pbnRlRikF30msSHVJoPBICEYmzCxUI+zFlDBjf4vlJojwV0/Rfq85it2yhN/MV\n" +
            "we2IBC+z9FAAogYo+JFw7Uxq8nsLCKX1tTPsqxGXWNonAgMBAAGjggOHMIIDgzAM\n" +
            "BgNVHRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAOBgNV\n" +
            "HQ8BAf8EBAMCBaAwOwYDVR0fBDQwMjAwoC6gLIYqaHR0cDovL2NybC5zdGFyZmll\n" +
            "bGR0ZWNoLmNvbS9zZmlnMnMzLTEuY3JsMGIGA1UdIARbMFkwTgYLYIZIAYb9bgEH\n" +
            "FwMwPzA9BggrBgEFBQcCARYxaHR0cDovL2NlcnRpZmljYXRlcy5zdGFyZmllbGR0\n" +
            "ZWNoLmNvbS9yZXBvc2l0b3J5LzAHBgVngQwBATCBggYIKwYBBQUHAQEEdjB0MCoG\n" +
            "CCsGAQUFBzABhh5odHRwOi8vb2NzcC5zdGFyZmllbGR0ZWNoLmNvbS8wRgYIKwYB\n" +
            "BQUHMAKGOmh0dHA6Ly9jZXJ0aWZpY2F0ZXMuc3RhcmZpZWxkdGVjaC5jb20vcmVw\n" +
            "b3NpdG9yeS9zZmlnMi5jcnQwHwYDVR0jBBgwFoAUJUWBaFAmOD07LSy+zWrZtj2z\n" +
            "ZmMwXQYDVR0RBFYwVIImcmV2b2tlZC5zZmlnMi5jYXRlc3Quc3RhcmZpZWxkdGVj\n" +
            "aC5jb22CKnd3dy5yZXZva2VkLnNmaWcyLmNhdGVzdC5zdGFyZmllbGR0ZWNoLmNv\n" +
            "bTAdBgNVHQ4EFgQU9hCSl7QoQ8KdsGgwMDwlvSurKNcwggF9BgorBgEEAdZ5AgQC\n" +
            "BIIBbQSCAWkBZwB1AFYUBpov18Ls0/XhvUSyPsdGdrm8mRFcwO+UmFXWidDdAAAB\n" +
            "XppC0cEAAAQDAEYwRAIgIO8sIG88JlA73P2myZ7EshemxaR8qBgf3wlYZpg5aZEC\n" +
            "IGtlcUL7Il1uOLN0LTAzNTQ7pfb7oFYbr0R4LWe2ZvBIAHYA7ku9t3XOYLrhQmkf\n" +
            "q+GeZqMPfl+wctiDAMR7iXqo/csAAAFemkLVbwAABAMARzBFAiEAmWkzcotxZSwb\n" +
            "xPS3MG13TVXGu2+MiXXjOIf42DR8zJQCIBL4cSOJh+LX5kpPub6KOiEOn7TVE1Zv\n" +
            "IQUxuf+vyAD4AHYApLkJkLQYWBSHuxOizGdwCjw1mAT5G9+443fNDsgN3BAAAAFe\n" +
            "mkLXRQAABAMARzBFAiBX8foh/KrYr34O2c9cH6uyWW2XjBHNLsYX1mr+8VuNaAIh\n" +
            "AObDQwpDYh/bNp6k547gDxnR73LeU3kvl1Y76GjgxLAhMA0GCSqGSIb3DQEBCwUA\n" +
            "A4IBAQDJ5vlagzOH8/ORUMgT33muSDFXCe5el/sQzVg8dridw9qjnxOpkGibdCiT\n" +
            "b9Il1bdi7UnG8MlA3XpDjGgp6J/mUTijD9WcFx4lp5JnPaIbShHWCyIlRVZJzrZc\n" +
            "UYhR56xXOKDYKYOIvM6qTqegXyEynJrIVTArMk7jQf0oNQLLHzXE1fVS1zut0H5l\n" +
            "GE+TBgjasMEa1o1e/H/heSytb2zFNsZr8oxojzGBmlKyfCoIIcCv3PxX2ur57zJE\n" +
            "9ADWoYK/7gYVba0JmLV4nQltDPp06nOYT9imxBWTrFahgPx1jOQDLgIpitkjyCy4\n" +
            "xpmxUk8L6yc3O3aSD9OU/fzk/t/d\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED, null, System.out);
    }
}

