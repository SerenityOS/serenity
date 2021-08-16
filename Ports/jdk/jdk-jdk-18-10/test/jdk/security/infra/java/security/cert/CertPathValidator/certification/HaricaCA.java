/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8256421
 * @summary Interoperability tests with Harica CAs
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath HaricaCA OCSP
 * @run main/othervm -Djava.security.debug=certpath HaricaCA CRL
 */

/*
 * Obtain test artifacts for Harica CA from:
 *
 * CA has no published test sites so we will need to communicate with CA for test.
 */
public class HaricaCA {

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

        new Harica_CA().runTest(pathValidator, ocspEnabled);
        new Harica_ECC().runTest(pathValidator, ocspEnabled);
    }
}

class Harica_CA {

    // Owner: CN=Hellenic Academic and Research Institutions Code Signing CA R1,
    // O=Hellenic Academic and Research Institutions Cert. Authority, L=Athens, C=GR
    // Issuer: CN=Hellenic Academic and Research Institutions RootCA 2015,
    // O=Hellenic Academic and Research Institutions Cert. Authority, L=Athens, C=GR
    // Serial number: 1bc8b59e0cea0b7c
    // Valid from: Fri Apr 08 00:37:41 PDT 2016 until: Sat Apr 06 00:37:41 PDT 2024
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHnjCCBYagAwIBAgIIG8i1ngzqC3wwDQYJKoZIhvcNAQELBQAwgaYxCzAJBgNV\n" +
            "BAYTAkdSMQ8wDQYDVQQHEwZBdGhlbnMxRDBCBgNVBAoTO0hlbGxlbmljIEFjYWRl\n" +
            "bWljIGFuZCBSZXNlYXJjaCBJbnN0aXR1dGlvbnMgQ2VydC4gQXV0aG9yaXR5MUAw\n" +
            "PgYDVQQDEzdIZWxsZW5pYyBBY2FkZW1pYyBhbmQgUmVzZWFyY2ggSW5zdGl0dXRp\n" +
            "b25zIFJvb3RDQSAyMDE1MB4XDTE2MDQwODA3Mzc0MVoXDTI0MDQwNjA3Mzc0MVow\n" +
            "ga0xCzAJBgNVBAYTAkdSMQ8wDQYDVQQHEwZBdGhlbnMxRDBCBgNVBAoTO0hlbGxl\n" +
            "bmljIEFjYWRlbWljIGFuZCBSZXNlYXJjaCBJbnN0aXR1dGlvbnMgQ2VydC4gQXV0\n" +
            "aG9yaXR5MUcwRQYDVQQDEz5IZWxsZW5pYyBBY2FkZW1pYyBhbmQgUmVzZWFyY2gg\n" +
            "SW5zdGl0dXRpb25zIENvZGUgU2lnbmluZyBDQSBSMTCCAiIwDQYJKoZIhvcNAQEB\n" +
            "BQADggIPADCCAgoCggIBAJYQhJz8QjfP1wOc1sHxTKe9wy7d3oBScTslX5Yn1IE8\n" +
            "j5SzsKilvbu9rp8bd8IKHnPrl1Vnjpvna/Vx/XKlXDxnIxgfLINfmbjJRoEbzj1t\n" +
            "fxdTnZSN5c6vHYr5112/XCr2VQ7gIL+2jujXlJQYs/aUzAb9RR35dy4z2tQJQwtA\n" +
            "gG/paNGgnOU2ROrcaEYfvG4YQWaHAwqjJ53ZY5HVD7NmLp5SZTA5Q3eHAYae99fq\n" +
            "iEgMti3zMejzrAM1h+iVpLUxA7iMFh5nwCOvYJWVYqN3YOEmksxh7HOWlpe1PgDT\n" +
            "5SezPCHmOilNizMSRsdW7fE8apYVq/O0yStFW32kS9RQzYiBsdgPvNzDGec7Jj8X\n" +
            "SozhS6to+A6RdgfpHccHc2jhIRkFocA63OEde3Eo/NPf/WbFX2tpgDIv+HF/YZV3\n" +
            "iaMeKeOKDIxa4c3t3qBsZNtFEGOQyDouVH4g2Y8gHhUCcR7gQrHEYy/9FbtUDviu\n" +
            "abBVPFnNbEwT0uKS20aLgldkJ4/b3UzeTVzXVwLB8ruBfZX/e60YFaUJAZbbYRbE\n" +
            "mD6KmDOCTfalpBRhXar6U1wf+GRp1wv01Vw88EJ6VEBxXAYrfQ28ER9IuLqVQRlN\n" +
            "xpZCP71DRCvWljkXAKvWrs3JAw926n5K7sL+fv6mn+6iGBHrSEcyX8g8EZv6XuLP\n" +
            "AgMBAAGjggHFMIIBwTAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAT\n" +
            "BgNVHSUEDDAKBggrBgEFBQcDAzAdBgNVHQ4EFgQUDyz8Ez5xsy3SqelOr4FBCaaY\n" +
            "3UgwRgYDVR0fBD8wPTA7oDmgN4Y1aHR0cDovL2NybHYxLmhhcmljYS5nci9IYXJp\n" +
            "Y2FSb290Q0EyMDE1L2NybHYxLmRlci5jcmwwHwYDVR0jBBgwFoAUcRVnyMjJvXVd\n" +
            "ctA4GGqd83EkVAswbgYIKwYBBQUHAQEEYjBgMCEGCCsGAQUFBzABhhVodHRwOi8v\n" +
            "b2NzcC5oYXJpY2EuZ3IwOwYIKwYBBQUHMAKGL2h0dHA6Ly93d3cuaGFyaWNhLmdy\n" +
            "L2NlcnRzL0hhcmljYVJvb3RDQTIwMTUuY3J0MIGQBgNVHSAEgYgwgYUwgYIGBFUd\n" +
            "IAAwejAyBggrBgEFBQcCARYmaHR0cDovL3d3dy5oYXJpY2EuZ3IvZG9jdW1lbnRz\n" +
            "L0NQUy5waHAwRAYIKwYBBQUHAgIwOAw2VGhpcyBjZXJ0aWZpY2F0ZSBpcyBzdWJq\n" +
            "ZWN0IHRvIEdyZWVrIGxhd3MgYW5kIG91ciBDUFMuMA0GCSqGSIb3DQEBCwUAA4IC\n" +
            "AQA2t84B3ImFaWsnaYoPxwYu9njKaDc/QsaxT4AKP8env/Zr+fjD5s0Bjd1gZijC\n" +
            "9LRTzIovpldc/yADy7SVboIH0uNEiBC0kc0DaCq0ceCGIe6dw92Mu9DJ3UpMGAuF\n" +
            "fLpyearmfX9qzi0KhPGhjTxSTD4vWw3E0nvDVMBG54Im0OUGeVzZ9SpvRRhRnPOk\n" +
            "1eplEYRRDVTGYQJ84GdVC4H4U3TjlbO9gppeSnBoVHyCqDDpyd8HhTuY3P5VRWaf\n" +
            "dvAkwrW2Vv53zIYpDtcwG7mf4UXKbfYGtOIg/xaqHV82J6MYZHW1RNIlSh7F+1S5\n" +
            "XamkPCQZZI87CMt/OZH+RaO87Vr2V02wkYwEeAvjOwq9l7LtOJaOznKS+BlMi009\n" +
            "ljgHWUQcA2wk25hOAS3YtvhI1l2UrRHSqmHd7Jah4clskIjURt3b2Ez9nuZh1dC8\n" +
            "NfaoPCkpK3leLBezubtq48MRe5jkAgen5UXvxq9zOZSen4pYeBK72ugNyLjzOhY8\n" +
            "GrSeE1voLnvyZIguM2hrI8nEjI4rSXL6lsqXyG/ODzDMMdKq4FrjoW3y9KnV/n8t\n" +
            "BvKTAlDcXQTqfdTykDWnxwNTp6dU+MOom9LGy6ZNFbei7XuvjrREiXUEJ/I2dGSD\n" +
            "3zeNek32BS2BBZNN8jeP4szLs85G5HWql59OyePZfARA6Q==\n" +
            "-----END CERTIFICATE-----";

    // Owner: OID.1.3.6.1.4.1.311.60.2.1.3=GR, OID.2.5.4.15=Private Organization,
    // CN=Greek Universities Network (GUnet), SERIALNUMBER=VATGR-099028220,
    // OU=Class A - Private Key created and stored in hardware CSP, O=Greek Universities Network, L=Athens, C=GR
    // Issuer: CN=Hellenic Academic and Research Institutions Code Signing CA R1,
    // O=Hellenic Academic and Research Institutions Cert. Authority, L=Athens, C=GR
    // Serial number: 2c9a7af519251c645c1bed40b9d1aeca
    // Valid from: Mon Sep 09 01:15:13 PDT 2019 until: Wed Sep 08 01:15:13 PDT 2021
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGyzCCBLOgAwIBAgIQLJp69RklHGRcG+1AudGuyjANBgkqhkiG9w0BAQsFADCB\n" +
            "rTELMAkGA1UEBhMCR1IxDzANBgNVBAcTBkF0aGVuczFEMEIGA1UEChM7SGVsbGVu\n" +
            "aWMgQWNhZGVtaWMgYW5kIFJlc2VhcmNoIEluc3RpdHV0aW9ucyBDZXJ0LiBBdXRo\n" +
            "b3JpdHkxRzBFBgNVBAMTPkhlbGxlbmljIEFjYWRlbWljIGFuZCBSZXNlYXJjaCBJ\n" +
            "bnN0aXR1dGlvbnMgQ29kZSBTaWduaW5nIENBIFIxMB4XDTE5MDkwOTA4MTUxM1oX\n" +
            "DTIxMDkwODA4MTUxM1owggEBMQswCQYDVQQGEwJHUjEPMA0GA1UEBwwGQXRoZW5z\n" +
            "MSMwIQYDVQQKDBpHcmVlayBVbml2ZXJzaXRpZXMgTmV0d29yazFBMD8GA1UECww4\n" +
            "Q2xhc3MgQSAtIFByaXZhdGUgS2V5IGNyZWF0ZWQgYW5kIHN0b3JlZCBpbiBoYXJk\n" +
            "d2FyZSBDU1AxGDAWBgNVBAUTD1ZBVEdSLTA5OTAyODIyMDErMCkGA1UEAwwiR3Jl\n" +
            "ZWsgVW5pdmVyc2l0aWVzIE5ldHdvcmsgKEdVbmV0KTEdMBsGA1UEDwwUUHJpdmF0\n" +
            "ZSBPcmdhbml6YXRpb24xEzARBgsrBgEEAYI3PAIBAxMCR1IwggEiMA0GCSqGSIb3\n" +
            "DQEBAQUAA4IBDwAwggEKAoIBAQDdkvuJzgjD3Hr1RoDtPZPp5ZelMl6Xh5vnKVuG\n" +
            "KX9gwHbDaXpLkp2bF2497R3olBaeYcEgappsWLPvLMEA75BSRopFQmhX3PAsjfAG\n" +
            "JZGM3A53n4NAQscCmtD6echJi4P6RThRcpfqObfe0vqZUVzSWRC8fU1P4lt/KzJj\n" +
            "DGSJtOP/SJfgp3M7hEp54fn+15DlYp+0e4aa5HF2HpGIy2ghPbRvkMJWbHmp3KZG\n" +
            "NOXViQdr/ogpqRsbdP7kN78WLhwrLu+xRUmf9A845jxyNO7269jOQHztfPAcgvDw\n" +
            "NbZouGtN3IIWoXWMLipNgzC9CYdqgLsLI9lXV9oQrXaICQ27AgMBAAGjggGOMIIB\n" +
            "ijAfBgNVHSMEGDAWgBQPLPwTPnGzLdKp6U6vgUEJppjdSDB0BggrBgEFBQcBAQRo\n" +
            "MGYwQQYIKwYBBQUHMAKGNWh0dHA6Ly9yZXBvLmhhcmljYS5nci9jZXJ0cy9IYXJp\n" +
            "Y2FDb2RlU2lnbmluZ0NBUjEuY3J0MCEGCCsGAQUFBzABhhVodHRwOi8vb2NzcC5o\n" +
            "YXJpY2EuZ3IwYAYDVR0gBFkwVzAHBgVngQwBAzAIBgYEAI96AQIwQgYMKwYBBAGB\n" +
            "zxEBAQMDMDIwMAYIKwYBBQUHAgEWJGh0dHBzOi8vcmVwby5oYXJpY2EuZ3IvZG9j\n" +
            "dW1lbnRzL0NQUzATBgNVHSUEDDAKBggrBgEFBQcDAzBLBgNVHR8ERDBCMECgPqA8\n" +
            "hjpodHRwOi8vY3JsdjEuaGFyaWNhLmdyL0hhcmljYUNvZGVTaWduaW5nQ0FSMS9j\n" +
            "cmx2MS5kZXIuY3JsMB0GA1UdDgQWBBR0K0c/UEGkfRtqAEwoONenOUa0WTAOBgNV\n" +
            "HQ8BAf8EBAMCB4AwDQYJKoZIhvcNAQELBQADggIBAHXVdzMwKeZSWmXZu/MyoyH8\n" +
            "yTHYi/yvaPpR88/Q/j5wxzuceECZJgH74xgePCIXx74IKTADUyIj7eWuTkzEnZF9\n" +
            "yqJyG+tArrIH7zdnjC0OgbJVFeUUY45rS24u8n/+46LnXy4A+bRa0qX/QikHxMvj\n" +
            "0I0/RTZpdQFjhnmhZyZzUWReFlAEG1fUz9TNwcCfGrv3z6YHCNAh/HWFtM7te6H7\n" +
            "LNjoRw0fC5edqI16ca2VrOLObbg64zdk7Ll/fu3Ll5UcAthve/PTez6R1wm47ETT\n" +
            "ItjinmQI3vRhIls/UwvF4ey5Linz80rnzIlrrMgn3G2gRpfJ55CxsXOZDGS9iy8C\n" +
            "jU8EiKkjbdDH6mOdQO11+FlwhBwSxVbr004RDPJJqKHKtKTWx6mEbW+ZrMXaEUNZ\n" +
            "mSPwDCUgDzrFfSspYJL0UW7zV+SbK9u5nf09m6qOpkZ5OE91IEpkotTWqMe/yOJu\n" +
            "Yey5wvAmEqQM6fcQCTfV5JBtzU5LOsm5Uwg728DcHfal21dJWtY3fi7+CgDRutU3\n" +
            "Ee8uZUmCd/KCSQgP8B/QTu7wLXHd4IQz2EP2tmN9Zrv/MsDjpSHgRrU6Hwi49bPQ\n" +
            "R43rmXHC7e2GZpBdPsfG0VDnpzgCx5Kogi5nJwwUevbvC2CT11AAlaOmTQPflqQj\n" +
            "w2LPatMDMTAga/l+CE8j\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=Greek Universities Network (GUnet), SERIALNUMBER=VATGR-099028220,
    // OU=Class B - Private Key created and stored in software CSP, O=Greek Universities Network, L=Athens, C=GR
    // Issuer: CN=Hellenic Academic and Research Institutions Code Signing CA R1,
    // O=Hellenic Academic and Research Institutions Cert. Authority, L=Athens, C=GR
    // Serial number: 29c4a7abf35bd0acb1638abbf22da1d3
    // Valid from: Mon Feb 17 01:47:19 PST 2020 until: Mon Feb 15 16:00:00 PST 2021
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGmDCCBICgAwIBAgIQKcSnq/Nb0KyxY4q78i2h0zANBgkqhkiG9w0BAQsFADCB\n" +
            "rTELMAkGA1UEBhMCR1IxDzANBgNVBAcTBkF0aGVuczFEMEIGA1UEChM7SGVsbGVu\n" +
            "aWMgQWNhZGVtaWMgYW5kIFJlc2VhcmNoIEluc3RpdHV0aW9ucyBDZXJ0LiBBdXRo\n" +
            "b3JpdHkxRzBFBgNVBAMTPkhlbGxlbmljIEFjYWRlbWljIGFuZCBSZXNlYXJjaCBJ\n" +
            "bnN0aXR1dGlvbnMgQ29kZSBTaWduaW5nIENBIFIxMB4XDTIwMDIxNzA5NDcxOVoX\n" +
            "DTIxMDIxNjAwMDAwMFowgc0xCzAJBgNVBAYTAkdSMQ8wDQYDVQQHDAZBdGhlbnMx\n" +
            "IzAhBgNVBAoMGkdyZWVrIFVuaXZlcnNpdGllcyBOZXR3b3JrMUEwPwYDVQQLDDhD\n" +
            "bGFzcyBCIC0gUHJpdmF0ZSBLZXkgY3JlYXRlZCBhbmQgc3RvcmVkIGluIHNvZnR3\n" +
            "YXJlIENTUDEYMBYGA1UEBRMPVkFUR1ItMDk5MDI4MjIwMSswKQYDVQQDDCJHcmVl\n" +
            "ayBVbml2ZXJzaXRpZXMgTmV0d29yayAoR1VuZXQpMIIBIjANBgkqhkiG9w0BAQEF\n" +
            "AAOCAQ8AMIIBCgKCAQEArSBfy0xwIHpFxFrAHiYpMOkILjOvlhRCBmfzrJFSILou\n" +
            "8vc1wxZw/olBa38khtIybE0GJnzZqoFTsalAcLg0CzfqucDoWL+uVLURmEmYZExj\n" +
            "ngYfjrpB7ypjWqxfBVqqLgxugY1XV3oaQRNWgFEn/mrQhv+0azySJdRSiW0BH4rP\n" +
            "wXp9LHgzHc7oxVOHsOKApwRN7TRVKz0/EoPhyUzdk5xoRTQMRalwZ0/E24v+CyoF\n" +
            "bPl/v+dlRK5n9It8yjCtqOMZ1z7l8sKmJ7q9iLvzAF58a8B5lPueC+opHPEy4VD0\n" +
            "7xZHYgdHliDsVIcWDXAzfcfo7l9EUX17onEXKMWKDwIDAQABo4IBkDCCAYwwHwYD\n" +
            "VR0jBBgwFoAUDyz8Ez5xsy3SqelOr4FBCaaY3UgwdAYIKwYBBQUHAQEEaDBmMEEG\n" +
            "CCsGAQUFBzAChjVodHRwOi8vcmVwby5oYXJpY2EuZ3IvY2VydHMvSGFyaWNhQ29k\n" +
            "ZVNpZ25pbmdDQVIxLmNydDAhBggrBgEFBQcwAYYVaHR0cDovL29jc3AuaGFyaWNh\n" +
            "LmdyMGIGA1UdIARbMFkwCAYGZ4EMAQQBMAgGBgQAj3oBATBDBg0rBgEEAYHPEQEB\n" +
            "AwEBMDIwMAYIKwYBBQUHAgEWJGh0dHBzOi8vcmVwby5oYXJpY2EuZ3IvZG9jdW1l\n" +
            "bnRzL0NQUzATBgNVHSUEDDAKBggrBgEFBQcDAzBLBgNVHR8ERDBCMECgPqA8hjpo\n" +
            "dHRwOi8vY3JsdjEuaGFyaWNhLmdyL0hhcmljYUNvZGVTaWduaW5nQ0FSMS9jcmx2\n" +
            "MS5kZXIuY3JsMB0GA1UdDgQWBBTj6v8Qkmosm+VNGsd/prwylbsM7TAOBgNVHQ8B\n" +
            "Af8EBAMCB4AwDQYJKoZIhvcNAQELBQADggIBADYBQvc68KwX28UFeE/nXcowBTSd\n" +
            "z+tQexloeBa4Z5G0yRr9URPc95H1R4SJiGQxb/KKqX2LqZtowc5rOXFBchI3da24\n" +
            "kn8pmoi4/U2o8iS+DSlAMsn4ZW2lyDd0jyNeRiuZXvkdpqxa9u/x0TyXJ6qUxSYM\n" +
            "oUC/H7RI1rxA+NgQ+otLHVs4ye53qzO44mXJv5Qq4dQ+GEdD+dShrYs69WMC0mV0\n" +
            "mVs//R1qp8G3tThAiJJF55kV8w1JH2+yv8RXAdQ2cRpN2W6wsnm2DxldHKM4Kel4\n" +
            "ON0TXlARGjoGqfEjFyZYtvpjzIPGoj3MHgCdiVb+zQJavYZJfLymlnIXajVb80Hh\n" +
            "9xSar/uZ0NwtZgX8EwOrBiDUuCjyvlYrcbpSN26Q4fuUfPdIJQXMQDLTFMQLQ/o4\n" +
            "5dn9FLLaJF7CA8VFeyJYi9pWEiIIpmPzIibd2+CAEQ43Q87GAvLpC09Rf6yggRh5\n" +
            "by/FAOOzfbEQMhvJfGcJrPeG8JdzG95N5ycAeeyb/iboYptRwsLWwurASmUEKn0y\n" +
            "BHlQPhut0mqNJQsoxvW6yIipFAQPBxfRHeLG71UO1kRxCleDTMsb3Zzt8hv1G4+m\n" +
            "Nsm3ZMOV8TMdlBxYO/xA9dqwAxMuPvShx6LdiiyQhdeEmIFvKZeTD22cIOVLDsrd\n" +
            "D2GbhbvPInvh21Qz\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Mon Feb 17 02:00:10 PST 2020", System.out);
    }
}

class Harica_ECC {

    // Owner: CN=HARICA Code Signing ECC SubCA R2,
    // O=Hellenic Academic and Research Institutions Cert. Authority, L=Athens, C=GR
    // Issuer: CN=Hellenic Academic and Research Institutions ECC RootCA 2015,
    // O=Hellenic Academic and Research Institutions Cert. Authority, L=Athens, C=GR
    // Serial number: 1f6ea9c2f4d7f80ee6a046d8b822dd3f
    // Valid from: Wed Mar 06 02:49:26 PST 2019 until: Thu Mar 02 02:49:26 PST 2034
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDxzCCA02gAwIBAgIQH26pwvTX+A7moEbYuCLdPzAKBggqhkjOPQQDAzCBqjEL\n" +
            "MAkGA1UEBhMCR1IxDzANBgNVBAcTBkF0aGVuczFEMEIGA1UEChM7SGVsbGVuaWMg\n" +
            "QWNhZGVtaWMgYW5kIFJlc2VhcmNoIEluc3RpdHV0aW9ucyBDZXJ0LiBBdXRob3Jp\n" +
            "dHkxRDBCBgNVBAMTO0hlbGxlbmljIEFjYWRlbWljIGFuZCBSZXNlYXJjaCBJbnN0\n" +
            "aXR1dGlvbnMgRUNDIFJvb3RDQSAyMDE1MB4XDTE5MDMwNjEwNDkyNloXDTM0MDMw\n" +
            "MjEwNDkyNlowgY8xCzAJBgNVBAYTAkdSMQ8wDQYDVQQHDAZBdGhlbnMxRDBCBgNV\n" +
            "BAoMO0hlbGxlbmljIEFjYWRlbWljIGFuZCBSZXNlYXJjaCBJbnN0aXR1dGlvbnMg\n" +
            "Q2VydC4gQXV0aG9yaXR5MSkwJwYDVQQDDCBIQVJJQ0EgQ29kZSBTaWduaW5nIEVD\n" +
            "QyBTdWJDQSBSMjB2MBAGByqGSM49AgEGBSuBBAAiA2IABKPWDfLT76KnCZ7dujms\n" +
            "bFYFLTvCWTyk8l0JqdhvZ+Tk6dWYAViIX90fc8+3HPAdHvkofNWvI/PW0qlZoRn3\n" +
            "De/Nrp+ZCWjg7UrUrmKH2z/N3A7lgZFqrwlWPxHSPSYVkqOCAU8wggFLMBIGA1Ud\n" +
            "EwEB/wQIMAYBAf8CAQAwHwYDVR0jBBgwFoAUtCILgpkkAQ6cu+QO/b/7lyCTmSow\n" +
            "cgYIKwYBBQUHAQEEZjBkMD8GCCsGAQUFBzAChjNodHRwOi8vcmVwby5oYXJpY2Eu\n" +
            "Z3IvY2VydHMvSGFyaWNhRUNDUm9vdENBMjAxNS5jcnQwIQYIKwYBBQUHMAGGFWh0\n" +
            "dHA6Ly9vY3NwLmhhcmljYS5ncjARBgNVHSAECjAIMAYGBFUdIAAwEwYDVR0lBAww\n" +
            "CgYIKwYBBQUHAwMwSQYDVR0fBEIwQDA+oDygOoY4aHR0cDovL2NybHYxLmhhcmlj\n" +
            "YS5nci9IYXJpY2FFQ0NSb290Q0EyMDE1L2NybHYxLmRlci5jcmwwHQYDVR0OBBYE\n" +
            "FMdgA8SpJhybKTZTnW4Xu3NWRKZeMA4GA1UdDwEB/wQEAwIBhjAKBggqhkjOPQQD\n" +
            "AwNoADBlAjAcJj7q9ujC+87/b81QowGo87VJhn9XWzRtpwjQFbIilqEfO3ot1d5F\n" +
            "MWT1Xn2Qg3sCMQCoM+eV2KkQluHn2N6+GMImJqVObVUyZv0E+BvvszdJubo1cAM0\n" +
            "SUImVT1t2wZpJKg=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=Dimitrios Zacharopoulos, GIVENNAME=Dimitrios, SURNAME=Zacharopoulos,
    // OU=Class B - Private Key created and stored in software CSP,
    // O=Hellenic Academic and Research Institutions Cert. Authority, L=Athens, C=GR
    // Issuer: CN=HARICA Code Signing ECC SubCA R2, O=Hellenic Academic and Research Institutions Cert. Authority,
    // L=Athens, C=GR
    // Serial number: 11a187e3b5b0eabea4ba4987e76cd09d
    // Valid from: Fri Jul 31 02:11:08 PDT 2020 until: Sun Jul 31 02:11:08 PDT 2022
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIENTCCA7ugAwIBAgIQEaGH47Ww6r6kukmH52zQnTAKBggqhkjOPQQDAzCBjzEL\n" +
            "MAkGA1UEBhMCR1IxDzANBgNVBAcMBkF0aGVuczFEMEIGA1UECgw7SGVsbGVuaWMg\n" +
            "QWNhZGVtaWMgYW5kIFJlc2VhcmNoIEluc3RpdHV0aW9ucyBDZXJ0LiBBdXRob3Jp\n" +
            "dHkxKTAnBgNVBAMMIEhBUklDQSBDb2RlIFNpZ25pbmcgRUNDIFN1YkNBIFIyMB4X\n" +
            "DTIwMDczMTA5MTEwOFoXDTIyMDczMTA5MTEwOFowgfUxCzAJBgNVBAYTAkdSMQ8w\n" +
            "DQYDVQQHDAZBdGhlbnMxRDBCBgNVBAoMO0hlbGxlbmljIEFjYWRlbWljIGFuZCBS\n" +
            "ZXNlYXJjaCBJbnN0aXR1dGlvbnMgQ2VydC4gQXV0aG9yaXR5MUEwPwYDVQQLDDhD\n" +
            "bGFzcyBCIC0gUHJpdmF0ZSBLZXkgY3JlYXRlZCBhbmQgc3RvcmVkIGluIHNvZnR3\n" +
            "YXJlIENTUDEWMBQGA1UEBAwNWmFjaGFyb3BvdWxvczESMBAGA1UEKgwJRGltaXRy\n" +
            "aW9zMSAwHgYDVQQDDBdEaW1pdHJpb3MgWmFjaGFyb3BvdWxvczBZMBMGByqGSM49\n" +
            "AgEGCCqGSM49AwEHA0IABOzRSqoI9HoxXjWerBBw8Croi1bJlUzoFXGjdszJIThf\n" +
            "UG3YOiR1whG7wY5H5v8v4IIB5pJanArJk4CzxBLKob6jggGPMIIBizAfBgNVHSME\n" +
            "GDAWgBTHYAPEqSYcmyk2U51uF7tzVkSmXjB6BggrBgEFBQcBAQRuMGwwRwYIKwYB\n" +
            "BQUHMAKGO2h0dHA6Ly9yZXBvLmhhcmljYS5nci9jZXJ0cy9IYXJpY2FFQ0NDb2Rl\n" +
            "U2lnbmluZ1N1YkNBUjIuY3J0MCEGCCsGAQUFBzABhhVodHRwOi8vb2NzcC5oYXJp\n" +
            "Y2EuZ3IwYQYDVR0gBFowWDAIBgZngQwBBAEwCAYGBACPegEBMEIGDCsGAQQBgc8R\n" +
            "AQMCATAyMDAGCCsGAQUFBwIBFiRodHRwczovL3JlcG8uaGFyaWNhLmdyL2RvY3Vt\n" +
            "ZW50cy9DUFMwEwYDVR0lBAwwCgYIKwYBBQUHAwMwRQYDVR0fBD4wPDA6oDigNoY0\n" +
            "aHR0cDovL2NybC5oYXJpY2EuZ3IvSGFyaWNhRUNDQ29kZVNpZ25pbmdTdWJDQVIy\n" +
            "LmNybDAdBgNVHQ4EFgQUpNjChUi9LGgwz7E18N1EwXooEFcwDgYDVR0PAQH/BAQD\n" +
            "AgeAMAoGCCqGSM49BAMDA2gAMGUCMQCDvQIjqgssTRHqp+gu7l21XmAQ4EMqr4+B\n" +
            "05iU/edLxxV+z5roTswxnPnr43+EbvkCMHq4aFW1u1+RvPBQJNRnYmdXCnj0lUw4\n" +
            "Ug7JnsEYMffLakDudzgsNv5ByJf9SO84Lw==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=Greek Universities Network (GUnet), SERIALNUMBER=VATGR-099028220,
    // OU=Class B - Private Key created and stored in software CSP, O=Greek Universities Network, L=Athens, C=GR
    // Issuer: CN=HARICA Code Signing ECC SubCA R2, O=Hellenic Academic and Research Institutions Cert. Authority,
    // L=Athens, C=GR
    // Serial number: 4b14719c7195c96de8a5663724454205
    // Valid from: Mon Feb 17 01:48:32 PST 2020 until: Mon Feb 15 16:00:00 PST 2021
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIEDjCCA5SgAwIBAgIQSxRxnHGVyW3opWY3JEVCBTAKBggqhkjOPQQDAzCBjzEL\n" +
            "MAkGA1UEBhMCR1IxDzANBgNVBAcMBkF0aGVuczFEMEIGA1UECgw7SGVsbGVuaWMg\n" +
            "QWNhZGVtaWMgYW5kIFJlc2VhcmNoIEluc3RpdHV0aW9ucyBDZXJ0LiBBdXRob3Jp\n" +
            "dHkxKTAnBgNVBAMMIEhBUklDQSBDb2RlIFNpZ25pbmcgRUNDIFN1YkNBIFIyMB4X\n" +
            "DTIwMDIxNzA5NDgzMloXDTIxMDIxNjAwMDAwMFowgc0xCzAJBgNVBAYTAkdSMQ8w\n" +
            "DQYDVQQHDAZBdGhlbnMxIzAhBgNVBAoMGkdyZWVrIFVuaXZlcnNpdGllcyBOZXR3\n" +
            "b3JrMUEwPwYDVQQLDDhDbGFzcyBCIC0gUHJpdmF0ZSBLZXkgY3JlYXRlZCBhbmQg\n" +
            "c3RvcmVkIGluIHNvZnR3YXJlIENTUDEYMBYGA1UEBRMPVkFUR1ItMDk5MDI4MjIw\n" +
            "MSswKQYDVQQDDCJHcmVlayBVbml2ZXJzaXRpZXMgTmV0d29yayAoR1VuZXQpMFkw\n" +
            "EwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAECC+lD6hphUDLSGs/Vw4Tvj8RAuCQAyi+\n" +
            "MUiXXXzGSp5e6ksBVlRlawluNR+BUtY93BRCjjNr3OuvSVALyrH7HKOCAZAwggGM\n" +
            "MB8GA1UdIwQYMBaAFMdgA8SpJhybKTZTnW4Xu3NWRKZeMHoGCCsGAQUFBwEBBG4w\n" +
            "bDBHBggrBgEFBQcwAoY7aHR0cDovL3JlcG8uaGFyaWNhLmdyL2NlcnRzL0hhcmlj\n" +
            "YUVDQ0NvZGVTaWduaW5nU3ViQ0FSMi5jcnQwIQYIKwYBBQUHMAGGFWh0dHA6Ly9v\n" +
            "Y3NwLmhhcmljYS5ncjBiBgNVHSAEWzBZMAgGBmeBDAEEATAIBgYEAI96AQEwQwYN\n" +
            "KwYBBAGBzxEBAQMBATAyMDAGCCsGAQUFBwIBFiRodHRwczovL3JlcG8uaGFyaWNh\n" +
            "LmdyL2RvY3VtZW50cy9DUFMwEwYDVR0lBAwwCgYIKwYBBQUHAwMwRQYDVR0fBD4w\n" +
            "PDA6oDigNoY0aHR0cDovL2NybC5oYXJpY2EuZ3IvSGFyaWNhRUNDQ29kZVNpZ25p\n" +
            "bmdTdWJDQVIyLmNybDAdBgNVHQ4EFgQUT4+WfQu125tLqla60vhK7ijBBWkwDgYD\n" +
            "VR0PAQH/BAQDAgeAMAoGCCqGSM49BAMDA2gAMGUCMQDADGDAxKpUkyEKBLzpX+88\n" +
            "ONLYDgFSie5W2ULWk02sDRO/k5xcSSjTf0FFa4+l6E8CME/b27e7NUGHPVYTLTWL\n" +
            "5yPiboyNsT8QpV+hZiWh/Qqiw1E/bR2SPIGEwYUrOV61fA==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Mon Feb 17 02:00:57 PST 2020", System.out);
    }
}
