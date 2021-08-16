/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8195774 8243321
 * @summary Interoperability tests with Entrust CAs
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath EntrustCA OCSP
 * @run main/othervm -Djava.security.debug=certpath EntrustCA CRL
 */

/*
 * Obtain test artifacts for Entrust CA from:
 *
 * EC CA:
 * Valid: https://validec.entrust.net
 * Revoked https://revokedec.entrust.net
 *
 * G4 CA:
 * Valid: https://validg4.entrust.net
 * Revoked: https://revokedg4.entrust.net
 */
public class EntrustCA {

    public static void main(String[] args) throws Exception {

        ValidatePathWithParams pathValidator = new ValidatePathWithParams(null);
        boolean ocspEnabled = false;

        if (args.length >= 1 && "CRL".equalsIgnoreCase(args[0])) {
            pathValidator.enableCRLCheck();
        } else {
            // OCSP check by default
            pathValidator.enableOCSPCheck();
            ocspEnabled = true;
        }

        new Entrust_ECCA().runTest(pathValidator, ocspEnabled);
        new Entrust_G4().runTest(pathValidator, ocspEnabled);
    }
}

class Entrust_ECCA {

    // Owner: CN=Entrust Certification Authority - L1J, OU="(c) 2016 Entrust, Inc. - for authorized use only",
    // OU=See www.entrust.net/legal-terms, O="Entrust, Inc.", C=US
    // Issuer: CN=Entrust Root Certification Authority - EC1, OU="(c) 2012 Entrust, Inc. - for authorized use only",
    // OU=See www.entrust.net/legal-terms, O="Entrust, Inc.", C=US
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIID5zCCA2ygAwIBAgIQCoPUgD5+n1EAAAAAUdTB9zAKBggqhkjOPQQDAzCBvzEL\n" +
            "MAkGA1UEBhMCVVMxFjAUBgNVBAoTDUVudHJ1c3QsIEluYy4xKDAmBgNVBAsTH1Nl\n" +
            "ZSB3d3cuZW50cnVzdC5uZXQvbGVnYWwtdGVybXMxOTA3BgNVBAsTMChjKSAyMDEy\n" +
            "IEVudHJ1c3QsIEluYy4gLSBmb3IgYXV0aG9yaXplZCB1c2Ugb25seTEzMDEGA1UE\n" +
            "AxMqRW50cnVzdCBSb290IENlcnRpZmljYXRpb24gQXV0aG9yaXR5IC0gRUMxMB4X\n" +
            "DTE2MDQwNTIwMTk1NFoXDTM3MTAwNTIwNDk1NFowgboxCzAJBgNVBAYTAlVTMRYw\n" +
            "FAYDVQQKEw1FbnRydXN0LCBJbmMuMSgwJgYDVQQLEx9TZWUgd3d3LmVudHJ1c3Qu\n" +
            "bmV0L2xlZ2FsLXRlcm1zMTkwNwYDVQQLEzAoYykgMjAxNiBFbnRydXN0LCBJbmMu\n" +
            "IC0gZm9yIGF1dGhvcml6ZWQgdXNlIG9ubHkxLjAsBgNVBAMTJUVudHJ1c3QgQ2Vy\n" +
            "dGlmaWNhdGlvbiBBdXRob3JpdHkgLSBMMUowdjAQBgcqhkjOPQIBBgUrgQQAIgNi\n" +
            "AAT14eFXmpQX/dEf7NAxrMH13n0btz1KKvH2S1rROGPAKex2CY8yxznbffK/MbCk\n" +
            "F7ByYXGs1+8kL5xmTysU/c+YmjOZx2mMSAk2DPw30fijJ3tRrwChZ+TBpgtB6+A5\n" +
            "MsCjggEuMIIBKjAOBgNVHQ8BAf8EBAMCAQYwEgYDVR0TAQH/BAgwBgEB/wIBADAz\n" +
            "BggrBgEFBQcBAQQnMCUwIwYIKwYBBQUHMAGGF2h0dHA6Ly9vY3NwLmVudHJ1c3Qu\n" +
            "bmV0MDMGA1UdHwQsMCowKKAmoCSGImh0dHA6Ly9jcmwuZW50cnVzdC5uZXQvZWMx\n" +
            "cm9vdC5jcmwwOwYDVR0gBDQwMjAwBgRVHSAAMCgwJgYIKwYBBQUHAgEWGmh0dHA6\n" +
            "Ly93d3cuZW50cnVzdC5uZXQvcnBhMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEF\n" +
            "BQcDAjAdBgNVHQ4EFgQUw/lFA77I+Qs8RTXz63Ls5+jrlJswHwYDVR0jBBgwFoAU\n" +
            "t2PnGt2N6QimVYOk4GpQQWURQkkwCgYIKoZIzj0EAwMDaQAwZgIxAPnVAOqxKDd7\n" +
            "v37EBmpPqWCCWBFPKW6HpRx3GUWc9caeQIw8rO2HXYgf92pb/TsJYAIxAJhI0MpR\n" +
            "z5L42xF1R9UIPfQxCMwgsnWBqIqcfMrMO+2DxQy6GIP3cFFj9gRyxguKWw==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=validec.entrust.net, SERIALNUMBER=D15576572, OID.2.5.4.15=Private Organization, O="Entrust, Inc.",
    // OID.1.3.6.1.4.1.311.60.2.1.2=Maryland, OID.1.3.6.1.4.1.311.60.2.1.3=US, L=Kanata, ST=Ontario, C=CA
    // Issuer: CN=Entrust Certification Authority - L1J, OU="(c) 2016 Entrust, Inc. - for authorized use only",
    // OU=See www.entrust.net/legal-terms, O="Entrust, Inc.", C=US
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFrTCCBTKgAwIBAgIQYtgW4DLwh74AAAAAVqBXkTAKBggqhkjOPQQDAjCBujEL\n" +
            "MAkGA1UEBhMCVVMxFjAUBgNVBAoTDUVudHJ1c3QsIEluYy4xKDAmBgNVBAsTH1Nl\n" +
            "ZSB3d3cuZW50cnVzdC5uZXQvbGVnYWwtdGVybXMxOTA3BgNVBAsTMChjKSAyMDE2\n" +
            "IEVudHJ1c3QsIEluYy4gLSBmb3IgYXV0aG9yaXplZCB1c2Ugb25seTEuMCwGA1UE\n" +
            "AxMlRW50cnVzdCBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eSAtIEwxSjAeFw0xODA2\n" +
            "MjUxMzE1NTdaFw0xOTA2MjUxMzQ1NTBaMIHJMQswCQYDVQQGEwJDQTEQMA4GA1UE\n" +
            "CBMHT250YXJpbzEPMA0GA1UEBxMGS2FuYXRhMRMwEQYLKwYBBAGCNzwCAQMTAlVT\n" +
            "MRkwFwYLKwYBBAGCNzwCAQITCE1hcnlsYW5kMRYwFAYDVQQKEw1FbnRydXN0LCBJ\n" +
            "bmMuMR0wGwYDVQQPExRQcml2YXRlIE9yZ2FuaXphdGlvbjESMBAGA1UEBRMJRDE1\n" +
            "NTc2NTcyMRwwGgYDVQQDExN2YWxpZGVjLmVudHJ1c3QubmV0MFkwEwYHKoZIzj0C\n" +
            "AQYIKoZIzj0DAQcDQgAEHQe7lUaAUgIwR9EiLJlhkbx+HfSr22M3JvQD6+fnYgqd\n" +
            "55e6E1UE45fk92UpqPi1CEbXrdpmWKu1Z470B9cPGaOCAwcwggMDMB4GA1UdEQQX\n" +
            "MBWCE3ZhbGlkZWMuZW50cnVzdC5uZXQwggF/BgorBgEEAdZ5AgQCBIIBbwSCAWsB\n" +
            "aQB1AFWB1MIWkDYBSuoLm1c8U/DA5Dh4cCUIFy+jqh0HE9MMAAABZDcxpMkAAAQD\n" +
            "AEYwRAIgIb0PwjCcNOchJg8Zywz/0Lwm2vEOJUSao6BqNUIsyaYCIElHHexB06LE\n" +
            "yXWDXO7UqOtWT6uqkdJN8V4TzwT9B4o4AHcA3esdK3oNT6Ygi4GtgWhwfi6OnQHV\n" +
            "XIiNPRHEzbbsvswAAAFkNzGkvgAABAMASDBGAiEAlxy/kxB9waIifYn+EV550pvA\n" +
            "C3jUfS/bjsKbcsBH9cQCIQDSHTJORz6fZu8uLFhpV525pw7iHVh2dSn3gpcteObh\n" +
            "DQB3ALvZ37wfinG1k5Qjl6qSe0c4V5UKq1LoGpCWZDaOHtGFAAABZDcxpTsAAAQD\n" +
            "AEgwRgIhAPCBqVqSvAEIXMPloV0tfBEEdjRrAhiG407cPqYwt9AFAiEAuQf4R5os\n" +
            "MLkD3XhxvrTDvnD+PUOf8PzPevsWkuxNqcQwDgYDVR0PAQH/BAQDAgeAMB0GA1Ud\n" +
            "JQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBjBggrBgEFBQcBAQRXMFUwIwYIKwYB\n" +
            "BQUHMAGGF2h0dHA6Ly9vY3NwLmVudHJ1c3QubmV0MC4GCCsGAQUFBzAChiJodHRw\n" +
            "Oi8vYWlhLmVudHJ1c3QubmV0L2wxai1lYzEuY2VyMDMGA1UdHwQsMCowKKAmoCSG\n" +
            "Imh0dHA6Ly9jcmwuZW50cnVzdC5uZXQvbGV2ZWwxai5jcmwwSgYDVR0gBEMwQTA2\n" +
            "BgpghkgBhvpsCgECMCgwJgYIKwYBBQUHAgEWGmh0dHA6Ly93d3cuZW50cnVzdC5u\n" +
            "ZXQvcnBhMAcGBWeBDAEBMB8GA1UdIwQYMBaAFMP5RQO+yPkLPEU18+ty7Ofo65Sb\n" +
            "MB0GA1UdDgQWBBT+J7OhS6gskCanmOGnx10DPSF8ATAJBgNVHRMEAjAAMAoGCCqG\n" +
            "SM49BAMCA2kAMGYCMQCQLUQABT74TmdHzAtB97uNF5+Zy15wzkmlKeRSOXCIf2C5\n" +
            "YKjsgdkR1OdzZXcpjNgCMQDfWcdPhodNXZC4l1lLPOPaTzPPw6uVqqoITQlc6r1t\n" +
            "dRkkD6K9ii/X8EtwoFp7s80=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=revokedec.entrust.net, SERIALNUMBER=115868500, OID.2.5.4.15=Private Organization, O="Entrust, Inc.",
    // OID.1.3.6.1.4.1.311.60.2.1.2=Texas, OID.1.3.6.1.4.1.311.60.2.1.3=US, L=Kanata, ST=Ontario, C=CA
    // Issuer: CN=Entrust Certification Authority - L1J, OU="(c) 2016 Entrust, Inc. - for authorized use only",
    // OU=See www.entrust.net/legal-terms, O="Entrust, Inc.", C=US
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGJzCCBaygAwIBAgIRAM0WDfag1taIAAAAAFagJ5gwCgYIKoZIzj0EAwIwgbox\n" +
            "CzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1FbnRydXN0LCBJbmMuMSgwJgYDVQQLEx9T\n" +
            "ZWUgd3d3LmVudHJ1c3QubmV0L2xlZ2FsLXRlcm1zMTkwNwYDVQQLEzAoYykgMjAx\n" +
            "NiBFbnRydXN0LCBJbmMuIC0gZm9yIGF1dGhvcml6ZWQgdXNlIG9ubHkxLjAsBgNV\n" +
            "BAMTJUVudHJ1c3QgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkgLSBMMUowHhcNMTcw\n" +
            "NTI0MTcwNzA4WhcNMTkwNTI0MTczNjU1WjCByDELMAkGA1UEBhMCQ0ExEDAOBgNV\n" +
            "BAgTB09udGFyaW8xDzANBgNVBAcTBkthbmF0YTETMBEGCysGAQQBgjc8AgEDEwJV\n" +
            "UzEWMBQGCysGAQQBgjc8AgECEwVUZXhhczEWMBQGA1UEChMNRW50cnVzdCwgSW5j\n" +
            "LjEdMBsGA1UEDxMUUHJpdmF0ZSBPcmdhbml6YXRpb24xEjAQBgNVBAUTCTExNTg2\n" +
            "ODUwMDEeMBwGA1UEAxMVcmV2b2tlZGVjLmVudHJ1c3QubmV0MFkwEwYHKoZIzj0C\n" +
            "AQYIKoZIzj0DAQcDQgAEN5MP/59yrs9uwVM/Mrc8IuHonMChAZgN2twwvh8KTnR2\n" +
            "3stfem/R+NtLccq+4ds1+8ktnXgP7u1x0as6IJOH1qOCA4EwggN9MCAGA1UdEQQZ\n" +
            "MBeCFXJldm9rZWRlYy5lbnRydXN0Lm5ldDCCAfcGCisGAQQB1nkCBAIEggHnBIIB\n" +
            "4wHhAHYA7ku9t3XOYLrhQmkfq+GeZqMPfl+wctiDAMR7iXqo/csAAAFcO4iiogAA\n" +
            "BAMARzBFAiAgHVpryyNVgnsUIihu+5DC2/vuP8Cy5iXq8NhCBXg8UgIhAKi5jImT\n" +
            "f1FJksvHboc0EZh9TWhWljVZ6E5jB2CL+qzeAHcAVhQGmi/XwuzT9eG9RLI+x0Z2\n" +
            "ubyZEVzA75SYVdaJ0N0AAAFcO4ij9QAABAMASDBGAiEA4B2p2726ISSkKC9WVlzj\n" +
            "BVwYZ1Hr7mTjPrFqkoGpEHYCIQC5iuInkJXGBANLTH06BHIQkkr4KnFRl9QBOSw4\n" +
            "b+kNqgB1AN3rHSt6DU+mIIuBrYFocH4ujp0B1VyIjT0RxM227L7MAAABXDuIpkcA\n" +
            "AAQDAEYwRAIgQ9ssw19wIhHWW6IWgwnIyB7e30HacBNX6S1eQ3GUX04CICffGj3A\n" +
            "WWmK9lixmk35YklMnSXNqHQezSYRiCYtXxejAHcApLkJkLQYWBSHuxOizGdwCjw1\n" +
            "mAT5G9+443fNDsgN3BAAAAFcO4inUwAABAMASDBGAiEA+8T9tpPw/mU/STsNv0oz\n" +
            "8Nla21fKlpEOyWqDKWPSUeYCIQCwI5tDyyaJtyFY9/OVqLG+BKPKjscUtTqGJYl4\n" +
            "XbOo1jAOBgNVHQ8BAf8EBAMCB4AwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUF\n" +
            "BwMCMGMGCCsGAQUFBwEBBFcwVTAjBggrBgEFBQcwAYYXaHR0cDovL29jc3AuZW50\n" +
            "cnVzdC5uZXQwLgYIKwYBBQUHMAKGImh0dHA6Ly9haWEuZW50cnVzdC5uZXQvbDFq\n" +
            "LWVjMS5jZXIwMwYDVR0fBCwwKjAooCagJIYiaHR0cDovL2NybC5lbnRydXN0Lm5l\n" +
            "dC9sZXZlbDFqLmNybDBKBgNVHSAEQzBBMDYGCmCGSAGG+mwKAQIwKDAmBggrBgEF\n" +
            "BQcCARYaaHR0cDovL3d3dy5lbnRydXN0Lm5ldC9ycGEwBwYFZ4EMAQEwHwYDVR0j\n" +
            "BBgwFoAUw/lFA77I+Qs8RTXz63Ls5+jrlJswHQYDVR0OBBYEFIj28ytR8ulo1p2t\n" +
            "ZnBQOLK0rlLUMAkGA1UdEwQCMAAwCgYIKoZIzj0EAwIDaQAwZgIxANzqGRI0en5P\n" +
            "gSUDcdwoQSNKrBPBfGz2AQVLHAXsxvIlGhKZAQtM49zxA8AdFy/agwIxAMEjJH6A\n" +
            "4UbcGZc40eYu6wUbAxiUDD3gwSElNQ8Z6IhNLPCCdMM6KZORyaagAcXn4A==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Wed May 24 10:39:28 PDT 2017", System.out);
    }
}

class Entrust_G4 {

    // Owner: CN=Entrust Certification Authority - L1N, OU="(c) 2014 Entrust, Inc. - for authorized use only",
    // OU=See www.entrust.net/legal-terms, O="Entrust, Inc.", C=US
    // Issuer: CN=Entrust Root Certification Authority - G4, OU="(c) 2015 Entrust, Inc. - for authorized use only",
    // OU=See www.entrust.net/legal-terms, O="Entrust, Inc.", C=US
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGMjCCBBqgAwIBAgIRAKvsd/8bQQwHAAAAAFVl2AUwDQYJKoZIhvcNAQELBQAw\n" +
            "gb4xCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1FbnRydXN0LCBJbmMuMSgwJgYDVQQL\n" +
            "Ex9TZWUgd3d3LmVudHJ1c3QubmV0L2xlZ2FsLXRlcm1zMTkwNwYDVQQLEzAoYykg\n" +
            "MjAxNSBFbnRydXN0LCBJbmMuIC0gZm9yIGF1dGhvcml6ZWQgdXNlIG9ubHkxMjAw\n" +
            "BgNVBAMTKUVudHJ1c3QgUm9vdCBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eSAtIEc0\n" +
            "MB4XDTE3MTEyMjIwMDQyMFoXDTMwMTIyMjIwMzQyMFowgboxCzAJBgNVBAYTAlVT\n" +
            "MRYwFAYDVQQKEw1FbnRydXN0LCBJbmMuMSgwJgYDVQQLEx9TZWUgd3d3LmVudHJ1\n" +
            "c3QubmV0L2xlZ2FsLXRlcm1zMTkwNwYDVQQLEzAoYykgMjAxNCBFbnRydXN0LCBJ\n" +
            "bmMuIC0gZm9yIGF1dGhvcml6ZWQgdXNlIG9ubHkxLjAsBgNVBAMTJUVudHJ1c3Qg\n" +
            "Q2VydGlmaWNhdGlvbiBBdXRob3JpdHkgLSBMMU4wggEiMA0GCSqGSIb3DQEBAQUA\n" +
            "A4IBDwAwggEKAoIBAQDcSG+caYQ4xcvf+dt8bgCEHorO0g5j0H1NOtQzRXgUoG8y\n" +
            "QuRbJX9swyKqQZbsc18YvTV8OKA/uSNE46Jvq47TFPojWWTVLbNDqpM07e4EFYKs\n" +
            "A9NFzAUngijnf3ivnXA6iNPAMXaEhXmhY/YFjk8NoM7Y1PFsA0oj5hamKQ06iO/j\n" +
            "gvBScLmnQ1ju9Qj9IGIg18UL5AJNw0frspLUQBYVrLGaqAy5Nl2BUJKaZ4vnSLvP\n" +
            "nk6YrB15mo1phHae10Ba4fx7R3z8IZ/hby4OXTy/KZpu107VEQPAwTuDK8ZXxB5y\n" +
            "0DSzi4vaw27aLrUsq4aFqUo03gEfC31vWW76TNkFAgMBAAGjggErMIIBJzAOBgNV\n" +
            "HQ8BAf8EBAMCAQYwEgYDVR0TAQH/BAgwBgEB/wIBADAdBgNVHSUEFjAUBggrBgEF\n" +
            "BQcDAQYIKwYBBQUHAwIwOwYDVR0gBDQwMjAwBgRVHSAAMCgwJgYIKwYBBQUHAgEW\n" +
            "Gmh0dHA6Ly93d3cuZW50cnVzdC5uZXQvcnBhMDMGCCsGAQUFBwEBBCcwJTAjBggr\n" +
            "BgEFBQcwAYYXaHR0cDovL29jc3AuZW50cnVzdC5uZXQwMAYDVR0fBCkwJzAloCOg\n" +
            "IYYfaHR0cDovL2NybC5lbnRydXN0Lm5ldC9nNGNhLmNybDAdBgNVHQ4EFgQU7kfR\n" +
            "hXHx/S23P7s+Y1h3F0lADpUwHwYDVR0jBBgwFoAUnzjEViPDOeigcWzoVEzk6Dqx\n" +
            "v2cwDQYJKoZIhvcNAQELBQADggIBACMeFFgsWmC7h6D1v8DJUkOpm/m5UhVhO0hb\n" +
            "pQMQKMhKkl744Y9SWG4WNmpQy743TTciEJPZFhc7ke2R6VmK8ZJUqro2awOw1RWZ\n" +
            "OtHla59Btf1NQd41vOVdU+qFhs8lFfXg9sK7YHTrfxHtMXLoGnkkamK3xJgn7sXa\n" +
            "/zUvUDBTpDCXcpO9SyHoKIQswmkIPpRyIdPF4biRdR3N+9MYmlfqN/Nk3OEZ73xZ\n" +
            "AUZP6Gu+f9cEiHTA8NdYHCPLJWyFnIHWK+QuTFEnKYnOYxCeroLBNOO64e8JWZ39\n" +
            "kZ22BBXhHzqOCCczS7JOJTRF+JgvWuxbFwRstj8qf3fE+JndWmq2FC4hTHtpuK5K\n" +
            "ENuiRm5gdkXfsXmB+qB6y5gaajiTIMscGIcZIKTe2YdKrLoicvEz8k+loM7favik\n" +
            "vzFioTNTDHYGx3mkfElBE7ycY8n+jZE3QBBv33k28MeQi7XNgEaMc4tYwoZIdE9A\n" +
            "xVccXTzEQzka82dOkRB1dU0XZId9XAWv+CtNc2TjF6Wgx2seA/c6H8S0IfgQBIV2\n" +
            "8iN2wZns2QFdawkdy3hMUqPnA++kuGhLW3GemsIY5dP/WxY8rd+OfLb/Ks9T1pCd\n" +
            "28t7PQRcQsgkYmouzrOW9ASBvYqLLdhl4y+fFXff8RkPIKMNoYP06WJvRKmky9R/\n" +
            "41/nXRas\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=validg4.entrust.net, SERIALNUMBER=1913605, OID.2.5.4.15=Private Organization,
    // O=Entrust Datacard Limited, OID.1.3.6.1.4.1.311.60.2.1.2=Ontario, OID.1.3.6.1.4.1.311.60.2.1.3=CA,
    // L=Ottawa, ST=Ontario, C=CA
    // Issuer: CN=Entrust Certification Authority - L1N, OU="(c) 2014 Entrust, Inc. - for authorized use only",
    // OU=See www.entrust.net/legal-terms, O="Entrust, Inc.", C=US
    // Serial number: 83790beb78eeb966007ad3dbf11d570
    // Valid from: Fri May 29 13:29:00 PDT 2020 until: Sun Aug 28 13:34:23 PDT 2022
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFpjCCBI6gAwIBAgIQCDeQvreO65ZgB609vxHVcDANBgkqhkiG9w0BAQsFADCB\n" +
            "ujELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUVudHJ1c3QsIEluYy4xKDAmBgNVBAsT\n" +
            "H1NlZSB3d3cuZW50cnVzdC5uZXQvbGVnYWwtdGVybXMxOTA3BgNVBAsTMChjKSAy\n" +
            "MDE0IEVudHJ1c3QsIEluYy4gLSBmb3IgYXV0aG9yaXplZCB1c2Ugb25seTEuMCwG\n" +
            "A1UEAxMlRW50cnVzdCBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eSAtIEwxTjAeFw0y\n" +
            "MDA1MjkyMDI5MDBaFw0yMjA4MjgyMDM0MjNaMIHRMQswCQYDVQQGEwJDQTEQMA4G\n" +
            "A1UECBMHT250YXJpbzEPMA0GA1UEBxMGT3R0YXdhMRMwEQYLKwYBBAGCNzwCAQMT\n" +
            "AkNBMRgwFgYLKwYBBAGCNzwCAQITB09udGFyaW8xITAfBgNVBAoTGEVudHJ1c3Qg\n" +
            "RGF0YWNhcmQgTGltaXRlZDEdMBsGA1UEDxMUUHJpdmF0ZSBPcmdhbml6YXRpb24x\n" +
            "EDAOBgNVBAUTBzE5MTM2MDUxHDAaBgNVBAMTE3ZhbGlkZzQuZW50cnVzdC5uZXQw\n" +
            "ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC508f77Kp/kfbqs9DHfa+V\n" +
            "977gsVzI78TzfN4tF3ujwnPgd9mzLArM71VJvceOJUto7ywRasxmFxOLHf7WN2Kg\n" +
            "U1yk/Kp9WUNfjmjIkI+JfCTkaz1RztpW85GNN9SL/W2yFIxv0ijAiGoQeC7J80Ni\n" +
            "+y31Q5+M0oPMzngBOtD8LpyVt+/lSwUvxwhlChu7LWpIFmBUriILkvh11vxaItZV\n" +
            "Jm4g8amE33/eXPFjZxB4ABQpBMC4QVg10UP+DpimZuJa6oQZfoNUjDF2yKlyrA+z\n" +
            "s3kK8SXzJhE5LQxBp158jAoCVZuER08cumw3wvXI5NGzkzDxpTGacDO0bDo2ULpN\n" +
            "AgMBAAGjggGNMIIBiTAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUH\n" +
            "AwIGCCsGAQUFBwMBMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFOA38RC6Sv6hMUgY\n" +
            "eLACjvqO13vsMB8GA1UdIwQYMBaAFO5H0YVx8f0ttz+7PmNYdxdJQA6VMGgGCCsG\n" +
            "AQUFBwEBBFwwWjAjBggrBgEFBQcwAYYXaHR0cDovL29jc3AuZW50cnVzdC5uZXQw\n" +
            "MwYIKwYBBQUHMAKGJ2h0dHA6Ly9haWEuZW50cnVzdC5uZXQvbDFuLWNoYWluMjU2\n" +
            "LmNlcjAzBgNVHR8ELDAqMCigJqAkhiJodHRwOi8vY3JsLmVudHJ1c3QubmV0L2xl\n" +
            "dmVsMW4uY3JsMB4GA1UdEQQXMBWCE3ZhbGlkZzQuZW50cnVzdC5uZXQwSwYDVR0g\n" +
            "BEQwQjA3BgpghkgBhvpsCgECMCkwJwYIKwYBBQUHAgEWG2h0dHBzOi8vd3d3LmVu\n" +
            "dHJ1c3QubmV0L3JwYTAHBgVngQwBATANBgkqhkiG9w0BAQsFAAOCAQEAOExxxxEk\n" +
            "iAZZ4RJSWwI/CBQYAlUmd2wb/SBk9eYNAu/UL0XiAbwbOjH2dV6JHwAdwn0eoPR1\n" +
            "KK/E1/OVoVibVBdxLMISPqdodRgHps6kGCOJxS8Zz8d3AEvx27EQ/Hg/EwIJZsUK\n" +
            "dyb48V6a3XzExqLiwGu9oI9Ozm3/mo11ixmhvSFXH+FZf93qvvCSO+XTGGrLv5ja\n" +
            "Tkazn/HgnwUBHd1TiO0jLhAdc+rZyd/SDjXMAXsa99zVfc2MY0Mb8+MohNHOwqYg\n" +
            "tuYuirvtt9P0oteauL+iEBCRcqsmJaHGeaEyJH2QMxC5W22KpW245eHisW7rMoGQ\n" +
            "9nbGmfe97p7bHQ==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=revokedg4.entrust.net, SERIALNUMBER=1913605, OID.2.5.4.15=Private Organization,
    // O=Entrust Datacard Limited, OID.1.3.6.1.4.1.311.60.2.1.2=Ontario, OID.1.3.6.1.4.1.311.60.2.1.3=CA,
    // L=Ottawa, ST=Ontario, C=CA
    // Issuer: CN=Entrust Certification Authority - L1N, OU="(c) 2014 Entrust, Inc. - for authorized use only",
    // OU=See www.entrust.net/legal-terms, O="Entrust, Inc.", C=US
    // Serial number: 24c5f46412b9dcc242a93017176979d6
    // Valid from: Fri May 29 13:36:00 PDT 2020 until: Sun Aug 28 13:40:43 PDT 2022
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFqjCCBJKgAwIBAgIQJMX0ZBK53MJCqTAXF2l51jANBgkqhkiG9w0BAQsFADCB\n" +
            "ujELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUVudHJ1c3QsIEluYy4xKDAmBgNVBAsT\n" +
            "H1NlZSB3d3cuZW50cnVzdC5uZXQvbGVnYWwtdGVybXMxOTA3BgNVBAsTMChjKSAy\n" +
            "MDE0IEVudHJ1c3QsIEluYy4gLSBmb3IgYXV0aG9yaXplZCB1c2Ugb25seTEuMCwG\n" +
            "A1UEAxMlRW50cnVzdCBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eSAtIEwxTjAeFw0y\n" +
            "MDA1MjkyMDM2MDBaFw0yMjA4MjgyMDQwNDNaMIHTMQswCQYDVQQGEwJDQTEQMA4G\n" +
            "A1UECBMHT250YXJpbzEPMA0GA1UEBxMGT3R0YXdhMRMwEQYLKwYBBAGCNzwCAQMT\n" +
            "AkNBMRgwFgYLKwYBBAGCNzwCAQITB09udGFyaW8xITAfBgNVBAoTGEVudHJ1c3Qg\n" +
            "RGF0YWNhcmQgTGltaXRlZDEdMBsGA1UEDxMUUHJpdmF0ZSBPcmdhbml6YXRpb24x\n" +
            "EDAOBgNVBAUTBzE5MTM2MDUxHjAcBgNVBAMTFXJldm9rZWRnNC5lbnRydXN0Lm5l\n" +
            "dDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAN6Bvaj7EG752e15UQH9\n" +
            "4o8+660Gi3caUAAu45vZebO7EfRgrz0zyalpiexmQzocGn6Zog2yVqmMZjrMY11a\n" +
            "q96s0pzVKImnA/787G7J5lRncP+PM6/WGtUUGS2hHiifoW5Ya/kcI1uk6EDT0leb\n" +
            "HIedOiwcfDkq38g5ckuWNae24DAD8AM9XBJXMuNbuiqo03wMlDL3Jif8wNQfpmPD\n" +
            "b+KR6IwGJdYwLBMoMcPmZF0rykW3YTO2NTDGCwvT8zzvjIKp8caRkI6pfkKmc89U\n" +
            "Nvgbk/d9JEsgQLbYmRKVnhtnt756U7v3+0kZITxzfsBvQZ6zC7X4FAcTN1302RGn\n" +
            "NGsCAwEAAaOCAY8wggGLMA4GA1UdDwEB/wQEAwIFoDAdBgNVHSUEFjAUBggrBgEF\n" +
            "BQcDAgYIKwYBBQUHAwEwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQULjRc9DEsa0kD\n" +
            "uhKNo6cCqQ+mPjgwHwYDVR0jBBgwFoAU7kfRhXHx/S23P7s+Y1h3F0lADpUwaAYI\n" +
            "KwYBBQUHAQEEXDBaMCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5lbnRydXN0Lm5l\n" +
            "dDAzBggrBgEFBQcwAoYnaHR0cDovL2FpYS5lbnRydXN0Lm5ldC9sMW4tY2hhaW4y\n" +
            "NTYuY2VyMDMGA1UdHwQsMCowKKAmoCSGImh0dHA6Ly9jcmwuZW50cnVzdC5uZXQv\n" +
            "bGV2ZWwxbi5jcmwwIAYDVR0RBBkwF4IVcmV2b2tlZGc0LmVudHJ1c3QubmV0MEsG\n" +
            "A1UdIAREMEIwNwYKYIZIAYb6bAoBAjApMCcGCCsGAQUFBwIBFhtodHRwczovL3d3\n" +
            "dy5lbnRydXN0Lm5ldC9ycGEwBwYFZ4EMAQEwDQYJKoZIhvcNAQELBQADggEBAGab\n" +
            "wtgpooQW3YL2Cqk9RDJFbNct5BSbzgY9qN1TOe4L7gbjV0BJBCcsHOCjvbgEuzME\n" +
            "FC/kAmBu7eMnKVAqCCsWaI8XV7xB7P/BqHpvf9LI/GyHg4wCYdxgFGBXHOjlSy+8\n" +
            "YWRM5UnFUknqbj1B4u2/U+U3X66QXi+MWrmBdjpcMahpY5zP1Bh90OmIc8DY4arf\n" +
            "widObgJe2H/VFScudLf5JMpBso2v772GYTRr5Tqqq3ouS9WvDf0NBvoStt1oiUMP\n" +
            "oowesfNiaYa/rZzWRlhYNs089KUeLhjOZswtIY5LCyy+Wt3CHgXljGEQFgi7p59s\n" +
            "gk0aMRYM9Gri26VbD5A=\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Fri May 29 13:42:13 PDT 2020", System.out);
    }
}
