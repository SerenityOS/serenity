/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8189131
 * @summary Interoperability tests with "D-Trust Root Class 3 CA 2 2009" and
 *          "D-Trust Root Class 3 CA 2 EV 2009" CAs
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath DTrustCA OCSP
 * @run main/othervm -Djava.security.debug=certpath DTrustCA CRL
 */
public class DTrustCA {

    public static void main(String[] args) throws Exception {

        ValidatePathWithParams pathValidator = new ValidatePathWithParams(null);

        boolean ocspEnabled = true;

        if (args.length >= 1 && "CRL".equalsIgnoreCase(args[0])) {
            pathValidator.enableCRLCheck();
            ocspEnabled = false;
        } else {
            // OCSP check by default
            pathValidator.enableOCSPCheck();
        }

        new RootClass3CA2().runTest(pathValidator, ocspEnabled);
        new RootClass3CA2EV().runTest(pathValidator, ocspEnabled);
    }
}

class RootClass3CA2 {

    // Owner: CN=D-TRUST SSL Class 3 CA 1 2009, O=D-Trust GmbH, C=DE
    private static final String INT = "-----BEGIN CERTIFICATE-----\n"
            + "MIIFMjCCBBqgAwIBAgIDCZBjMA0GCSqGSIb3DQEBCwUAME0xCzAJBgNVBAYTAkRF\n"
            + "MRUwEwYDVQQKDAxELVRydXN0IEdtYkgxJzAlBgNVBAMMHkQtVFJVU1QgUm9vdCBD\n"
            + "bGFzcyAzIENBIDIgMjAwOTAeFw0wOTExMTIxMjQ2NTVaFw0yOTExMDUwODM1NTha\n"
            + "MEwxCzAJBgNVBAYTAkRFMRUwEwYDVQQKDAxELVRydXN0IEdtYkgxJjAkBgNVBAMM\n"
            + "HUQtVFJVU1QgU1NMIENsYXNzIDMgQ0EgMSAyMDA5MIIBIjANBgkqhkiG9w0BAQEF\n"
            + "AAOCAQ8AMIIBCgKCAQEAoal0SyLSijE0JkuhHJmOCbmQznyxuSY7DaEwhUsdUpI+\n"
            + "2llkDLz6s9BWQe1zCVXDhrt3qz5U5H4h6jxm5Ec+ZbFiU3Gv2yxpI5cRPrqj9mJU\n"
            + "1CGgy1+29khuUnoopzSq66HPuGZGh06I7bJkXTQ7AQ92z1MdL2wATj1UWdNid3sQ\n"
            + "NiWIm+69nURHY6tmCNenNcG6aV4qjHMUPsjpCRabNY9nUO12rsmiDW2mbAC3gcxQ\n"
            + "lqLgLYur9HvB8cW0xu2JZ/B3PXmNphVuWskp3Y1u0SvIYzuEsE7lWDbBmtWZtabB\n"
            + "hzThkDQvd+3keQ1sU/beq1NeXfgKzQ5G+4Ql2PUY/wIDAQABo4ICGjCCAhYwHwYD\n"
            + "VR0jBBgwFoAU/doUxJ8w3iG9HkI5/KtjI0ng8YQwRAYIKwYBBQUHAQEEODA2MDQG\n"
            + "CCsGAQUFBzABhihodHRwOi8vcm9vdC1jMy1jYTItMjAwOS5vY3NwLmQtdHJ1c3Qu\n"
            + "bmV0MF8GA1UdIARYMFYwVAYEVR0gADBMMEoGCCsGAQUFBwIBFj5odHRwOi8vd3d3\n"
            + "LmQtdHJ1c3QubmV0L2ludGVybmV0L2ZpbGVzL0QtVFJVU1RfUm9vdF9QS0lfQ1BT\n"
            + "LnBkZjAzBgNVHREELDAqgRBpbmZvQGQtdHJ1c3QubmV0hhZodHRwOi8vd3d3LmQt\n"
            + "dHJ1c3QubmV0MIHTBgNVHR8EgcswgcgwgYCgfqB8hnpsZGFwOi8vZGlyZWN0b3J5\n"
            + "LmQtdHJ1c3QubmV0L0NOPUQtVFJVU1QlMjBSb290JTIwQ2xhc3MlMjAzJTIwQ0El\n"
            + "MjAyJTIwMjAwOSxPPUQtVHJ1c3QlMjBHbWJILEM9REU/Y2VydGlmaWNhdGVyZXZv\n"
            + "Y2F0aW9ubGlzdDBDoEGgP4Y9aHR0cDovL3d3dy5kLXRydXN0Lm5ldC9jcmwvZC10\n"
            + "cnVzdF9yb290X2NsYXNzXzNfY2FfMl8yMDA5LmNybDAdBgNVHQ4EFgQUUBkylJrE\n"
            + "tQRNVtDAgyHVNVWwsXowDgYDVR0PAQH/BAQDAgEGMBIGA1UdEwEB/wQIMAYBAf8C\n"
            + "AQAwDQYJKoZIhvcNAQELBQADggEBABM5QRHX/yInsmZLWVlvmWmKb3c4IB3hAIVR\n"
            + "sAGhkvQJ/RD1GZjZUBBYMWkD1P37fTQxlqTOe3NecVvElkYZuCq7HSM6o7awzb3m\n"
            + "yLn1kN+hDCsxX0EYbVSNjEjkW3QEkqJH9owH4qeMDxf7tfXB7BVKO+rarYPa2PR8\n"
            + "Wz2KhjFDmAeFg2J89YcpeJJEEJXoweAkgJEEwwEIfJ2yLjYo78RD0Rvij/+zkfj9\n"
            + "+dSvTiZTuqicyo37qNoYHgchuqXnKodhWkW89oo2NKhfeNHHbqvXEJmx0PbI6YyQ\n"
            + "50GnYECZRHNKhgbPEtNy/QetU53aWlTlvu4NIwLW5XVsrxlQ2Zw=\n"
            + "-----END CERTIFICATE-----";

    // Owner: CN=certdemo-ov-valid.ssl.d-trust.net, O=D-Trust GmbH, OU=IT,
    // L=Berlin, ST=Berlin, C=DE, SERIALNUMBER=DTRWS354803406304201, DNQ=7223150018
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n"
            + "MIIF1jCCBL6gAwIBAgIDD07RMA0GCSqGSIb3DQEBCwUAMEwxCzAJBgNVBAYTAkRF\n"
            + "MRUwEwYDVQQKDAxELVRydXN0IEdtYkgxJjAkBgNVBAMMHUQtVFJVU1QgU1NMIENs\n"
            + "YXNzIDMgQ0EgMSAyMDA5MB4XDTEyMTIxMTEwMTgzN1oXDTE1MTIyMTExMTgwOVow\n"
            + "gbMxEzARBgNVBC4TCjcyMjMxNTAwMTgxHTAbBgNVBAUTFERUUldTMzU0ODAzNDA2\n"
            + "MzA0MjAxMQswCQYDVQQGEwJERTEPMA0GA1UECAwGQmVybGluMQ8wDQYDVQQHDAZC\n"
            + "ZXJsaW4xCzAJBgNVBAsMAklUMRUwEwYDVQQKDAxELVRydXN0IEdtYkgxKjAoBgNV\n"
            + "BAMMIWNlcnRkZW1vLW92LXZhbGlkLnNzbC5kLXRydXN0Lm5ldDCCASIwDQYJKoZI\n"
            + "hvcNAQEBBQADggEPADCCAQoCggEBAMbo9ih0Bo4zKaKwl+mClCxhedC3YOpBzrun\n"
            + "zbqYJuy6vbHuZdMtU3nO7ziTPbnoVFboKmyEtAMwJ+qudHdWaa/nA4Hlhmg5+CWZ\n"
            + "OolX3VmMlrZ+LpaeajduOgDa7DQDcixZ+ndd24Xc/u9L83CH7ziQDs4XNJxx63Wf\n"
            + "lSMKBKkmvry7CfCXcsR4dYW8tTBm1PESJZVNqOKkOiwHwMA69knpXwghmDbKgZro\n"
            + "01chjeyYb39ZhwHNWlxh5rgd2HZpgrl8kUY3yV9PrQcjFPbKT6ZgHfRiHlax4vbX\n"
            + "qiHHcHRr7iVPruyCf0DU3BqhDVUhnrJ+vqTyg+m/OJduznF2nXcCAwEAAaOCAlcw\n"
            + "ggJTMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAfBgNVHSMEGDAWgBRQ\n"
            + "GTKUmsS1BE1W0MCDIdU1VbCxejBDBggrBgEFBQcBAQQ3MDUwMwYIKwYBBQUHMAGG\n"
            + "J2h0dHA6Ly9zc2wtYzMtY2ExLTIwMDkub2NzcC5kLXRydXN0Lm5ldDBmBgNVHSAE\n"
            + "XzBdMFsGCysGAQQBpTQCgUgBMEwwSgYIKwYBBQUHAgEWPmh0dHA6Ly93d3cuZC10\n"
            + "cnVzdC5uZXQvaW50ZXJuZXQvZmlsZXMvRC1UUlVTVF9Sb290X1BLSV9DUFMucGRm\n"
            + "MIHRBgNVHR8EgckwgcYwgcOggcCggb2GeWxkYXA6Ly9kaXJlY3RvcnkuZC10cnVz\n"
            + "dC5uZXQvQ049RC1UUlVTVCUyMFNTTCUyMENsYXNzJTIwMyUyMENBJTIwMSUyMDIw\n"
            + "MDksTz1ELVRydXN0JTIwR21iSCxDPURFP2NlcnRpZmljYXRlcmV2b2NhdGlvbmxp\n"
            + "c3SGQGh0dHA6Ly9jcmwuZC10cnVzdC5uZXQvY3JsL2QtdHJ1c3Rfc3NsX2NsYXNz\n"
            + "XzNfY2FfMV8yMDA5LmRlci5jcmwwMwYDVR0SBCwwKoEQaW5mb0BkLXRydXN0Lm5l\n"
            + "dIYWaHR0cDovL3d3dy5kLXRydXN0Lm5ldDAdBgNVHQ4EFgQUHjGMR/EdDBRf+Ejf\n"
            + "WW5a8beoBrwwDgYDVR0PAQH/BAQDAgSwMCwGA1UdEQQlMCOCIWNlcnRkZW1vLW92\n"
            + "LXZhbGlkLnNzbC5kLXRydXN0Lm5ldDANBgkqhkiG9w0BAQsFAAOCAQEAGN4yxyF3\n"
            + "sszODgDSkCNX1s4R874jmBmMYy4Af9/kwKNp2GtqPPhnDu8VFtq0bqs1e06XZ4/W\n"
            + "6pUPRZIlynjPASkQl+aJGzyZlaH+K0Al80M/7FRRmLCW9Do/RszRihdhcjeyG+Bi\n"
            + "2k+A35aVqKMAWzoH4M7TCPg4+ECltaFgJ+25loXl3j0yiP/DmBwATO80Nx78ILl5\n"
            + "D6cDyftMKUwdKKlUsB2RMOJsVBcotBMGTB1i/YoSKIu6t7QnoVFMHEia2wZegPCj\n"
            + "hBKhLf/Zde/VrSN3IIft93XRabqXWqjpDCvpb/b06/0o5aZIycrj+Kya54dsdXMO\n"
            + "FRy9N0HZYzvt9g==\n"
            + "-----END CERTIFICATE-----";

    // Owner: CN=certdemo-ov-revoked.ssl.d-trust.net, O=D-Trust GmbH, OU=IT,
    // L=Berlin, ST=Berlin, C=DE, DNQ=5562882417
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n"
            + "MIIFuzCCBKOgAwIBAgIDExFnMA0GCSqGSIb3DQEBCwUAMEwxCzAJBgNVBAYTAkRF\n"
            + "MRUwEwYDVQQKDAxELVRydXN0IEdtYkgxJjAkBgNVBAMMHUQtVFJVU1QgU1NMIENs\n"
            + "YXNzIDMgQ0EgMSAyMDA5MB4XDTE0MDYyNjE2MTg1NloXDTE1MDYyOTE2MTg1Nlow\n"
            + "gZYxEzARBgNVBC4TCjU1NjI4ODI0MTcxCzAJBgNVBAYTAkRFMQ8wDQYDVQQIEwZC\n"
            + "ZXJsaW4xDzANBgNVBAcTBkJlcmxpbjELMAkGA1UECxMCSVQxFTATBgNVBAoTDEQt\n"
            + "VHJ1c3QgR21iSDEsMCoGA1UEAxMjY2VydGRlbW8tb3YtcmV2b2tlZC5zc2wuZC10\n"
            + "cnVzdC5uZXQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCtdH2wqHgG\n"
            + "tqlekrfRQzJuhMzRllfYcmmsxr7jsnwgPe0+zib+GeTDm9U5+XKjT1uYETL501ov\n"
            + "HfKsZ/aK+k58iFF5evEtdHic/2v868uwxcm/Kcn+zt2uX9QvfSUzJPQkW/Ynu3w2\n"
            + "IhuBNBlFAJgxjYr2xMUmDrVDx1/ZfBc0ddyo87MccLZOdmqLhef8bJQ+3q6DA+Z1\n"
            + "bGk1wHl9KgFNtOjlKws5nKzCzyugy+MhLo+4wPxi0UhUA7QA7fk7lWBwJ9fZRTT/\n"
            + "cKfP4lUucXdQBS2ZhvpEZggjjBDhTHtZLwdfEUlf1GZ+GwD8IB9whlwqT2cS9WUR\n"
            + "XI9b14TJM2zfAgMBAAGjggJZMIICVTAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYB\n"
            + "BQUHAwIwHwYDVR0jBBgwFoAUUBkylJrEtQRNVtDAgyHVNVWwsXowQwYIKwYBBQUH\n"
            + "AQEENzA1MDMGCCsGAQUFBzABhidodHRwOi8vc3NsLWMzLWNhMS0yMDA5Lm9jc3Au\n"
            + "ZC10cnVzdC5uZXQwZgYDVR0gBF8wXTBbBgsrBgEEAaU0AoFIATBMMEoGCCsGAQUF\n"
            + "BwIBFj5odHRwOi8vd3d3LmQtdHJ1c3QubmV0L2ludGVybmV0L2ZpbGVzL0QtVFJV\n"
            + "U1RfUm9vdF9QS0lfQ1BTLnBkZjCB0QYDVR0fBIHJMIHGMIHDoIHAoIG9hnlsZGFw\n"
            + "Oi8vZGlyZWN0b3J5LmQtdHJ1c3QubmV0L0NOPUQtVFJVU1QlMjBTU0wlMjBDbGFz\n"
            + "cyUyMDMlMjBDQSUyMDElMjAyMDA5LE89RC1UcnVzdCUyMEdtYkgsQz1ERT9jZXJ0\n"
            + "aWZpY2F0ZXJldm9jYXRpb25saXN0hkBodHRwOi8vY3JsLmQtdHJ1c3QubmV0L2Ny\n"
            + "bC9kLXRydXN0X3NzbF9jbGFzc18zX2NhXzFfMjAwOS5kZXIuY3JsMDMGA1UdEgQs\n"
            + "MCqBEGluZm9AZC10cnVzdC5uZXSGFmh0dHA6Ly93d3cuZC10cnVzdC5uZXQwHQYD\n"
            + "VR0OBBYEFC4+5qwI2S+t/TaZ/kMADTR7FjdOMA4GA1UdDwEB/wQEAwIEsDAuBgNV\n"
            + "HREEJzAlgiNjZXJ0ZGVtby1vdi1yZXZva2VkLnNzbC5kLXRydXN0Lm5ldDANBgkq\n"
            + "hkiG9w0BAQsFAAOCAQEAO3sbXee7GbEyXSRZOgwk2LloPNIFriFGP8WAWnsaf056\n"
            + "jxHRnjjPQRyqhBmGQAGwrEp3a3uF+6gbM2XuoKPjNFqjqnQNR2+lVRs8pVTTjJ+r\n"
            + "SekcOUbCx6nIe98OBheAljAxfeal3e8bBrP3VA+QvOscaLJiC1ZsGfqvrGYJDt6b\n"
            + "UFMKbNuwDcfpKkrB0AyW0NvYALwgTPr+SgbxB0Xrp0W+dg6XfHmpuRSSPUkZqzEY\n"
            + "uPTmIgs7qCtVEIpV91gDFBDNfr4QbFVCNvDmMIZNMnXUEmTW81N1KUVTNdz8k5TY\n"
            + "HO/7TeeAi2u0m3ERrLXE9SKtNwUMJujEOQ/UmQkIQw==\n"
            + "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled)
            throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        if (ocspEnabled) {
            // Test certificates are expired in 2015
            // and backdated revocation check is only possible with OCSP
            pathValidator.setValidationDate("Jan 01, 2015");
        }

        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Thu Jun 26 09:28:39 PDT 2014", System.out);

        // reset validation date back to current date
        pathValidator.resetValidationDate();
    }
}

class RootClass3CA2EV {

    // Owner: CN=D-TRUST SSL Class 3 CA 1 EV 2009, O=D-Trust GmbH, C=DE
    private static final String INT = "-----BEGIN CERTIFICATE-----\n"
            + "MIIFRTCCBC2gAwIBAgIDCZBkMA0GCSqGSIb3DQEBCwUAMFAxCzAJBgNVBAYTAkRF\n"
            + "MRUwEwYDVQQKDAxELVRydXN0IEdtYkgxKjAoBgNVBAMMIUQtVFJVU1QgUm9vdCBD\n"
            + "bGFzcyAzIENBIDIgRVYgMjAwOTAeFw0wOTExMTIxMjUyNDNaFw0yOTExMDUwODUw\n"
            + "NDZaME8xCzAJBgNVBAYTAkRFMRUwEwYDVQQKDAxELVRydXN0IEdtYkgxKTAnBgNV\n"
            + "BAMMIEQtVFJVU1QgU1NMIENsYXNzIDMgQ0EgMSBFViAyMDA5MIIBIjANBgkqhkiG\n"
            + "9w0BAQEFAAOCAQ8AMIIBCgKCAQEAygp+ZziakFyPq80fk1QIT9UCcPy0R3UIyq56\n"
            + "hXA6lhgfs1l9R9wRM9/DIVX2olb0gHCXdpnHRm+jwzeL3dHJO8Im5Om/c24ZfSVE\n"
            + "zBcgKxS5X7X5e7oCYb9tozd9xs04WqYd5kWrvCJsSQf5gtv5gAeJt+QiU7dtXs3A\n"
            + "YDflWv4g9eEaDExxM0VQmceEAo5qc7I7dk5ry356G14zQmr29cxie6YS0kH+7qn5\n"
            + "g+c21M01sENle0tBPxIfkv+nV95Ih3JkpHSPm/wgFKfCtwRtG+5VehUoMEpgfi0X\n"
            + "fmVkag558aQpaaeQCtYZnXuq6g1D1LAcjIqMpOP4wNRp1ldLzQIDAQABo4ICJzCC\n"
            + "AiMwHwYDVR0jBBgwFoAU05SKTGITKhkuzK9yin0215oc3GcwRwYIKwYBBQUHAQEE\n"
            + "OzA5MDcGCCsGAQUFBzABhitodHRwOi8vcm9vdC1jMy1jYTItZXYtMjAwOS5vY3Nw\n"
            + "LmQtdHJ1c3QubmV0MF8GA1UdIARYMFYwVAYEVR0gADBMMEoGCCsGAQUFBwIBFj5o\n"
            + "dHRwOi8vd3d3LmQtdHJ1c3QubmV0L2ludGVybmV0L2ZpbGVzL0QtVFJVU1RfUm9v\n"
            + "dF9QS0lfQ1BTLnBkZjAzBgNVHREELDAqgRBpbmZvQGQtdHJ1c3QubmV0hhZodHRw\n"
            + "Oi8vd3d3LmQtdHJ1c3QubmV0MIHdBgNVHR8EgdUwgdIwgYeggYSggYGGf2xkYXA6\n"
            + "Ly9kaXJlY3RvcnkuZC10cnVzdC5uZXQvQ049RC1UUlVTVCUyMFJvb3QlMjBDbGFz\n"
            + "cyUyMDMlMjBDQSUyMDIlMjBFViUyMDIwMDksTz1ELVRydXN0JTIwR21iSCxDPURF\n"
            + "P2NlcnRpZmljYXRlcmV2b2NhdGlvbmxpc3QwRqBEoEKGQGh0dHA6Ly93d3cuZC10\n"
            + "cnVzdC5uZXQvY3JsL2QtdHJ1c3Rfcm9vdF9jbGFzc18zX2NhXzJfZXZfMjAwOS5j\n"
            + "cmwwHQYDVR0OBBYEFKztpZ16orZD8RiKJWpsscyo8lrUMA4GA1UdDwEB/wQEAwIB\n"
            + "BjASBgNVHRMBAf8ECDAGAQH/AgEAMA0GCSqGSIb3DQEBCwUAA4IBAQA6I3sGyvb4\n"
            + "MdTyEZFBBWBN/5Kx1SVkkPsll8DvgosJiuuK4I7mD6FFKDjKgogr407EoDSS2t1+\n"
            + "pSmQCb0rNXoJT3YIlpZGqPYU2rcwrelabJQZWAfoRnbkDx2aqofhp5u45dyQpM2t\n"
            + "R93/oA36iuHYc9Ewq8CaLGolrpT138RD7i4nN7sZFuFH0IseNz0+EZm88NHi9WeJ\n"
            + "UyshWFKBKARi+589Y4P/G2XnbckxFKUxa7uEroZcMwvKBy469K0Au0zVTxs1zNtf\n"
            + "Ol3QkNgPwzOPeHhOnpzcenyPgNEm+HQ0FPTnB4HeKBqTeLpkM7h4gq5MZ2TPmfuX\n"
            + "KDz3AHrWLLdH\n"
            + "-----END CERTIFICATE-----";

    // Owner: CN=certdemo-ev-revoked.ssl.d-trust.net, O=D-Trust GmbH, OU=IT,
    // STREET=Berlin, OID.2.5.4.17=10969, L=Berlin, ST=Berlin, C=DE,
    // SERIALNUMBER=HRB74346, OID.2.5.4.15=Private Organization,
    // OID.1.3.6.1.4.1.311.60.2.1.1=Berlin, OID.1.3.6.1.4.1.311.60.2.1.2=Berlin,
    // OID.1.3.6.1.4.1.311.60.2.1.3=DE, DNQ=4028175542
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n"
            + "MIIGZDCCBUygAwIBAgIDExFtMA0GCSqGSIb3DQEBCwUAME8xCzAJBgNVBAYTAkRF\n"
            + "MRUwEwYDVQQKDAxELVRydXN0IEdtYkgxKTAnBgNVBAMMIEQtVFJVU1QgU1NMIENs\n"
            + "YXNzIDMgQ0EgMSBFViAyMDA5MB4XDTE0MDYyNjE2NDMyOFoXDTE1MDYyOTE2NDMy\n"
            + "OFowggEwMRMwEQYDVQQuEwo0MDI4MTc1NTQyMRMwEQYLKwYBBAGCNzwCAQMMAkRF\n"
            + "MRcwFQYLKwYBBAGCNzwCAQIMBkJlcmxpbjEXMBUGCysGAQQBgjc8AgEBDAZCZXJs\n"
            + "aW4xHTAbBgNVBA8MFFByaXZhdGUgT3JnYW5pemF0aW9uMREwDwYDVQQFEwhIUkI3\n"
            + "NDM0NjELMAkGA1UEBhMCREUxDzANBgNVBAgTBkJlcmxpbjEPMA0GA1UEBxMGQmVy\n"
            + "bGluMQ4wDAYDVQQRDAUxMDk2OTEPMA0GA1UECRMGQmVybGluMQswCQYDVQQLEwJJ\n"
            + "VDEVMBMGA1UEChMMRC1UcnVzdCBHbWJIMSwwKgYDVQQDEyNjZXJ0ZGVtby1ldi1y\n"
            + "ZXZva2VkLnNzbC5kLXRydXN0Lm5ldDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC\n"
            + "AQoCggEBAMjX4zZxaSl+7eLXXVyO1HzQTymgsI4WlMpVMczyA21kXnx4iBZ9JeHW\n"
            + "W3Jv4SxxqtHut98eCq30r7yniCy7zGX35iuSy2zMf0u0tRraP5b2c590UMRgKOSU\n"
            + "DvahC+SlyJWGimt2Dtej2T1kcQvhUmonUkIimQOpM0MOIFxB5d494TzkQAYOV6yb\n"
            + "AHoIsMWMeMm24Rr6o8QnJqhb9A13keYRK8t0u7F5+fvONlFT2YnjbCoRlxa48i1b\n"
            + "PZwtE/NZ4bpZmv765tyfl9R5FatANnuja04Dd9StbTbjDezYzilF4qpSWtSKwmEl\n"
            + "J6fRxJ1kNAEThyzNZMnFjh8htZ7PL18CAwEAAaOCAmQwggJgMB0GA1UdJQQWMBQG\n"
            + "CCsGAQUFBwMBBggrBgEFBQcDAjAfBgNVHSMEGDAWgBSs7aWdeqK2Q/EYiiVqbLHM\n"
            + "qPJa1DBGBggrBgEFBQcBAQQ6MDgwNgYIKwYBBQUHMAGGKmh0dHA6Ly9zc2wtYzMt\n"
            + "Y2ExLWV2LTIwMDkub2NzcC5kLXRydXN0Lm5ldDBmBgNVHSAEXzBdMFsGCysGAQQB\n"
            + "pTQCgUoBMEwwSgYIKwYBBQUHAgEWPmh0dHA6Ly93d3cuZC10cnVzdC5uZXQvaW50\n"
            + "ZXJuZXQvZmlsZXMvRC1UUlVTVF9Sb290X1BLSV9DUFMucGRmMIHZBgNVHR8EgdEw\n"
            + "gc4wgcuggciggcWGfmxkYXA6Ly9kaXJlY3RvcnkuZC10cnVzdC5uZXQvQ049RC1U\n"
            + "UlVTVCUyMFNTTCUyMENsYXNzJTIwMyUyMENBJTIwMSUyMEVWJTIwMjAwOSxPPUQt\n"
            + "VHJ1c3QlMjBHbWJILEM9REU/Y2VydGlmaWNhdGVyZXZvY2F0aW9ubGlzdIZDaHR0\n"
            + "cDovL2NybC5kLXRydXN0Lm5ldC9jcmwvZC10cnVzdF9zc2xfY2xhc3NfM19jYV8x\n"
            + "X2V2XzIwMDkuZGVyLmNybDAzBgNVHRIELDAqgRBpbmZvQGQtdHJ1c3QubmV0hhZo\n"
            + "dHRwOi8vd3d3LmQtdHJ1c3QubmV0MB0GA1UdDgQWBBTFei056yoNM1HWYbBCixQw\n"
            + "wXnf0TAOBgNVHQ8BAf8EBAMCBLAwLgYDVR0RBCcwJYIjY2VydGRlbW8tZXYtcmV2\n"
            + "b2tlZC5zc2wuZC10cnVzdC5uZXQwDQYJKoZIhvcNAQELBQADggEBALv0OA+x401T\n"
            + "CvGQL1Ah7rclRgtxT3UjmphiLs9EE1YbweIUrN3R4tZuryyv9xslAoLCfMrHUe+f\n"
            + "jv1hsKqw+gGlrA8d5VnAqKfUR+KCiZivdlQ2sl4PDTZWpUQYlBnjQrD8h6UrcgTA\n"
            + "g1zUpDnioAKAQSWWxHVpcOX0IXCl3RgRz0GqUIZQ0Q8ZwYbIDEI+JzDEJgKkTzet\n"
            + "uzin8P54PjuJO801gENp43z++xHVuBcEWkU0TMDbmdL9vPZqnxsaoL5e/llGzor5\n"
            + "6JbU6Fc0MkuziaLPUsIxVVx3ZhZ6UFdv34swKyq6ycvKW2fgccwsQCFMrVjIo6HR\n"
            + "qiZC9Z+23vM=\n"
            + "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled)
            throws Exception {
        // Validate valid
        // Valid cert received as test artifact was revoked so remove test

        // Validate Revoked
        if (ocspEnabled) {
            // Revoked certificates are expired in 2015
            // and backdated revocation check is only possible with OCSP
            pathValidator.setValidationDate("Jan 01, 2015");
        }

        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Thu Jun 26 09:45:14 PDT 2014", System.out);

        // reset validation date back to current date
        pathValidator.resetValidationDate();
    }
}
