/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Interoperability tests with Buypass Class 2 and Class 3 CA
 * @build ValidatePathWithParams
 * @run main/othervm/timeout=180 -Djava.security.debug=certpath BuypassCA OCSP
 * @run main/othervm/timeout=180 -Djava.security.debug=certpath BuypassCA CRL
 */

 /*
 * Obtain test artifacts for Buypass Class 2 and Class 3 CAs from:
 *  Buypass Class 3 CA 2
 *  https://valid.qcevident.ca23.ssl.buypass.no/
 *  https://revoked.qcevident.ca23.ssl.buypass.no/
 *  https://expired.qcevident.ca23.ssl.buypass.no/
 *  https://valid.evident.ca23.ssl.buypass.no/
 *  https://revoked.evident.ca23.ssl.buypass.no/
 *  https://expired.evident.ca23.ssl.buypass.no/
 *  https://valid.businessplus.ca23.ssl.buypass.no
 *  https://revoked.businessplus.ca23.ssl.buypass.no
 *  https://expired.businessplus.ca23.ssl.buypass.no

 *  Buypass Class 2 CA 2
 *  https://valid.business.ca22.ssl.buypass.no
 *  https://revoked.business.ca22.ssl.buypass.no
 *  https://expired.business.ca22.ssl.buypass.no
 *  https://valid.domain.ca22.ssl.buypass.no
 *  https://revoked.domain.ca22.ssl.buypass.no
 *  https://expired.domain.ca22.ssl.buypass.no/
 */
public class BuypassCA {

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

        new BuypassClass2().runTest(pathValidator);
        new BuypassClass3().runTest(pathValidator, ocspEnabled);
    }
}

class BuypassClass2 {

    // Owner: CN=Buypass Class 2 CA 2, O=Buypass AS-983163327, C=NO
    // Issuer: CN=Buypass Class 2 Root CA, O=Buypass AS-983163327, C=NO
    // Serial number: 1b781c6d5e34ce1f77
    // Valid from: Mon Mar 25 05:17:10 PDT 2019 until: Sat Oct 26 02:16:17 PDT 2030
    private static final String INT_CLASS_2 = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFKTCCAxGgAwIBAgIJG3gcbV40zh93MA0GCSqGSIb3DQEBCwUAME4xCzAJBgNV\n" +
            "BAYTAk5PMR0wGwYDVQQKDBRCdXlwYXNzIEFTLTk4MzE2MzMyNzEgMB4GA1UEAwwX\n" +
            "QnV5cGFzcyBDbGFzcyAyIFJvb3QgQ0EwHhcNMTkwMzI1MTIxNzEwWhcNMzAxMDI2\n" +
            "MDkxNjE3WjBLMQswCQYDVQQGEwJOTzEdMBsGA1UECgwUQnV5cGFzcyBBUy05ODMx\n" +
            "NjMzMjcxHTAbBgNVBAMMFEJ1eXBhc3MgQ2xhc3MgMiBDQSAyMIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnKtnxpZLDQ+R0uzKzDMr83L8Dn+5ToSpD31z\n" +
            "qiYMykj7I2geNQ7javKsBOzhgeVr7GP4yJ5bK/P0dhoKesYvQITihfwztbMP6DyH\n" +
            "q1QJLQBqQnF0Lk8GhxSSNAZnlkCgX3aazoL32p9BeEfHuUE/8BlPywJY/RyE5/39\n" +
            "w3EKmWylhUkeRCMo3dUZr4khJq8JwGp/feKFs9n5FouM5PGhpFpZO+WQXEeqxpnc\n" +
            "CxbvWpInBoTnmX3+ofjm+fmY+sdAnyHkuOBBw3koGbFQygDaJP9VItOGByCX4iSV\n" +
            "ty/2uzppowIkf7Mpu5v5HJGKObLMP1gGv5lNqjAe8mz0bn25kwIDAQABo4IBCzCC\n" +
            "AQcwDwYDVR0TAQH/BAUwAwEB/zAfBgNVHSMEGDAWgBTJgHfgYpKC9Uac87r3TMPe\n" +
            "uKOtOTAdBgNVHQ4EFgQUkq1libIAD8tRDcEj7JROj8EEP3cwDgYDVR0PAQH/BAQD\n" +
            "AgEGMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjARBgNVHSAECjAIMAYG\n" +
            "BFUdIAAwPQYDVR0fBDYwNDAyoDCgLoYsaHR0cDovL2NybC5idXlwYXNzLm5vL2Ny\n" +
            "bC9CUENsYXNzMlJvb3RDQS5jcmwwMwYIKwYBBQUHAQEEJzAlMCMGCCsGAQUFBzAB\n" +
            "hhdodHRwOi8vb2NzcC5idXlwYXNzLmNvbTANBgkqhkiG9w0BAQsFAAOCAgEApNka\n" +
            "48a+qhJXXS9R24p34CWnirlyxPMhxFfQyvPFXnwBQGHvrm7H5KY3/9/etShFXdY/\n" +
            "N05Aq6UnE8my8jR4iHMm2e9iEf4v+O2E2JGH/5/H8wup160GBAsp4zAmJIT8KEgh\n" +
            "YAA1j+NaClVryZfEaaDfAdF6LbU3cW0ZgooILPMeeCEXso23KsdCD1Q+SMvD6nQJ\n" +
            "86iTvzWPY2GFJyEmvG/N2f29nBaHxWwZBwCfWB4Hqsw9wdKfY5M9SE/AGSLZ7LRM\n" +
            "BmkkF9nqkWxxISadx12nbxn0LsU2k8Xyt830DqhHGSoYHEC/iGxbU4Bub8NC0uw/\n" +
            "QNBj5Gd5cXLFhRUWLLBTq4p6P6kLc7JudpM4FNQ+stWK/eDZylbDLN3iCBRnHH4p\n" +
            "qg6HAlWuieiAKVsidBMxPUyDLJ/8Dt+aW8Z3vCNcYC2n7wqrLZz5e4FG+Wn9teFW\n" +
            "Rt5pO6ZUZAkDS59ZVojbbjOdQzNw3QHtZl0IMHeNYXJlPIUlHi4hGL3maGZ9sBF+\n" +
            "AMfMLDu56+J2DewIuTXPzCeJeSTam/ybNt5FxTznxCSCIDqwmZMy3AQEz9nGSbE8\n" +
            "zfwB5VT2ijLB0PpPX4YbLf33Vodf0NAkBUv6N5It30XiTUPhdk+caBYPoljz/J9U\n" +
            "15T5+EGHs8ccHQWyYQ6gqYk8o4JgP4rSJqO1sMI=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=valid.domain.ca22.ssl.buypass.no
    // Issuer: CN=Buypass Class 2 CA 2, O=Buypass AS-983163327, C=NO
    // Serial number: 34e2bff8063debd18d79
    // Valid from: Mon Sep 23 04:12:34 PDT 2019 until: Mon Oct 11 14:59:00 PDT 2021
    private static final String VALID_CLASS_2 = "-----BEGIN CERTIFICATE-----\n" +
            "MIIF8jCCBNqgAwIBAgIKNOK/+AY969GNeTANBgkqhkiG9w0BAQsFADBLMQswCQYD\n" +
            "VQQGEwJOTzEdMBsGA1UECgwUQnV5cGFzcyBBUy05ODMxNjMzMjcxHTAbBgNVBAMM\n" +
            "FEJ1eXBhc3MgQ2xhc3MgMiBDQSAyMB4XDTE5MDkyMzExMTIzNFoXDTIxMTAxMTIx\n" +
            "NTkwMFowKzEpMCcGA1UEAwwgdmFsaWQuZG9tYWluLmNhMjIuc3NsLmJ1eXBhc3Mu\n" +
            "bm8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCeu/8j7y55R3OucDek\n" +
            "gtdoVOEJQb2XaCR4OwiRzn74hLYhKGdFmFwSp+bPCT62NzjdK1urVeKrCQdC1Gkm\n" +
            "e7iSOsHHO5aC8oxkgdv8mwEwwvH7xHCcpEVLDlE5Oc0d4cS4QIwFAhNIC77slixL\n" +
            "fEdupc5e8FfQf3MlnhX+8gpgRzTx3iw8sb3gUwi3+7PRommHOhC7Ll+iI9LiLODJ\n" +
            "qrkHnCbM2HJMK+SGTOQ/whiQwMCnkLaEG0WO1rYc4BGRGfFb8qmQWw/tDKkEey7X\n" +
            "nLIFHSC33OiexQshAwRIAE7r1h9gMY1aAAB2Uxwi9/3l6fsd/VPmK7s7lYTBsrpK\n" +
            "r4bTAgMBAAGjggL2MIIC8jAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFJKtZYmyAA/L\n" +
            "UQ3BI+yUTo/BBD93MB0GA1UdDgQWBBSy+COaEmU2/BeF4g1OglFvAEYkIDAOBgNV\n" +
            "HQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMB8GA1Ud\n" +
            "IAQYMBYwCgYIYIRCARoBAgQwCAYGZ4EMAQIBMDoGA1UdHwQzMDEwL6AtoCuGKWh0\n" +
            "dHA6Ly9jcmwuYnV5cGFzcy5uby9jcmwvQlBDbGFzczJDQTIuY3JsMCsGA1UdEQQk\n" +
            "MCKCIHZhbGlkLmRvbWFpbi5jYTIyLnNzbC5idXlwYXNzLm5vMGoGCCsGAQUFBwEB\n" +
            "BF4wXDAjBggrBgEFBQcwAYYXaHR0cDovL29jc3AuYnV5cGFzcy5jb20wNQYIKwYB\n" +
            "BQUHMAKGKWh0dHA6Ly9jcnQuYnV5cGFzcy5uby9jcnQvQlBDbGFzczJDQTIuY2Vy\n" +
            "MIIBfgYKKwYBBAHWeQIEAgSCAW4EggFqAWgAdwC72d+8H4pxtZOUI5eqkntHOFeV\n" +
            "CqtS6BqQlmQ2jh7RhQAAAW1d0tivAAAEAwBIMEYCIQDFRAH98gYpvMMTVa3d5Wcq\n" +
            "0tOwpZZyUHiOjUlR3SD14QIhAKZp0cdwFpm+hh0taFVSTmluGsHmXPMCIQq9hLAB\n" +
            "VYgyAHYApLkJkLQYWBSHuxOizGdwCjw1mAT5G9+443fNDsgN3BAAAAFtXdLbFwAA\n" +
            "BAMARzBFAiBhr7KQc9yO3zb1iLlE0JW9whR0/bhrPDkk5BYnBKjzFAIhAMMTdHfk\n" +
            "1ljso5jKzIUcBpSW0HnTcuKiB3VxGpL7GFVWAHUAb1N2rDHwMRnYmQCkURX/dxUc\n" +
            "EdkCwQApBo2yCJo32RMAAAFtXdLYSAAABAMARjBEAiADoZr6Cp5AGM1eT2aUeRaQ\n" +
            "kv0vRaegjRGIhKRCvRGyFAIgWLU/7zh28LI8vAyWr8mpDqlUXvF13i3zSD3whq4L\n" +
            "Lu4wDQYJKoZIhvcNAQELBQADggEBAJH1RhTuMbhEOYlw+Efbx7PP7EEC/GQ1ijET\n" +
            "vZS45jFQyTKhFUcdP2QPAtEVo1nS8PBs0txQJBf0xceWUjer9ruxiAS+JlW21AOi\n" +
            "Uq9Kahpj5k63Z7tN8KTeOUE8wZGmHyvVcPP6mkC94RbjYIb4gd13eYxd2Vv1a7YX\n" +
            "dNI+J3g7sX5ijssfJxzDd0hORj2584YY2WiKKvIGxwDnLkxk09i3IvjEKsAi4Cgn\n" +
            "5798X5sSL1Q9C6gHEWt+cB5UtfILCfbLNRczS9zGku6gjh1c8dB7zc63mn7oCf1C\n" +
            "gnQ2xqwbZb3Wau8CPwcMqJWgQZLQFPbZd+4Xo5SDDqYppV4oN2A=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=revoked.domain.ca22.ssl.buypass.no
    // Issuer: CN=Buypass Class 2 CA 2, O=Buypass AS-983163327, C=NO
    // Serial number: 34e4b97261795f98c495
    // Valid from: Mon Sep 23 04:52:42 PDT 2019 until: Thu Sep 23 14:59:00 PDT 2021
    private static final String REVOKED_CLASS_2 = "-----BEGIN CERTIFICATE-----\n" +
            "MIIF9zCCBN+gAwIBAgIKNOS5cmF5X5jElTANBgkqhkiG9w0BAQsFADBLMQswCQYD\n" +
            "VQQGEwJOTzEdMBsGA1UECgwUQnV5cGFzcyBBUy05ODMxNjMzMjcxHTAbBgNVBAMM\n" +
            "FEJ1eXBhc3MgQ2xhc3MgMiBDQSAyMB4XDTE5MDkyMzExNTI0MloXDTIxMDkyMzIx\n" +
            "NTkwMFowLTErMCkGA1UEAwwicmV2b2tlZC5kb21haW4uY2EyMi5zc2wuYnV5cGFz\n" +
            "cy5ubzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAOiChajGbQktGjbl\n" +
            "k/i5PtqfMN6cMyjJdOirxzXdUG8dT+QErC5zcElCwuyy5MH7DQJRbSYsPxQmr6z5\n" +
            "OSheBxX0lPPLjJFfEafBZ+Fw1xmCVy3Xjt3GEl85iqv5y0/E/UlQPc0f7s6WxU0L\n" +
            "cItkyN0rWAa+uQY018qDFn+gDYIKWPzTCf5nkXIgob/IgBM1Bj7vSZ/LI1iB+I+G\n" +
            "dgLbSGBlJgK6lhCTc1tunZlSbKdPM2Th8Hbl6Uk7WormR/8SrGQA9AAd7BWa43V5\n" +
            "HHvf/oArsx0afp3zXNiMw9RgHVHI5uUAzkNnL8NMUpI1sK7/ndTlm0nXsHpPKrPo\n" +
            "e+NpKaMCAwEAAaOCAvkwggL1MAkGA1UdEwQCMAAwHwYDVR0jBBgwFoAUkq1libIA\n" +
            "D8tRDcEj7JROj8EEP3cwHQYDVR0OBBYEFDoBaIahoDhRhA3WVyT/XukqZzmAMA4G\n" +
            "A1UdDwEB/wQEAwIFoDAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwHwYD\n" +
            "VR0gBBgwFjAKBghghEIBGgECBDAIBgZngQwBAgEwOgYDVR0fBDMwMTAvoC2gK4Yp\n" +
            "aHR0cDovL2NybC5idXlwYXNzLm5vL2NybC9CUENsYXNzMkNBMi5jcmwwLQYDVR0R\n" +
            "BCYwJIIicmV2b2tlZC5kb21haW4uY2EyMi5zc2wuYnV5cGFzcy5ubzBqBggrBgEF\n" +
            "BQcBAQReMFwwIwYIKwYBBQUHMAGGF2h0dHA6Ly9vY3NwLmJ1eXBhc3MuY29tMDUG\n" +
            "CCsGAQUFBzAChilodHRwOi8vY3J0LmJ1eXBhc3Mubm8vY3J0L0JQQ2xhc3MyQ0Ey\n" +
            "LmNlcjCCAX8GCisGAQQB1nkCBAIEggFvBIIBawFpAHYAu9nfvB+KcbWTlCOXqpJ7\n" +
            "RzhXlQqrUugakJZkNo4e0YUAAAFtXfeApgAABAMARzBFAiARoEDgK57YWEW2R21d\n" +
            "jFMphF5c9PypIwbZFHiWxdyCyAIhALsjjtPGgcrT/7KebYFPuKDyQO6rc8YYvm0z\n" +
            "Q+Xt7NhxAHYApLkJkLQYWBSHuxOizGdwCjw1mAT5G9+443fNDsgN3BAAAAFtXfeD\n" +
            "eQAABAMARzBFAiBCXKlCGkqa85QVqMR5dYDDz3F5aQgLXPubrQLX7cAZ0wIhALRr\n" +
            "p8F6OfIdccSUhzEcNdtensQ/7zxgn81bUzn1ar9EAHcAb1N2rDHwMRnYmQCkURX/\n" +
            "dxUcEdkCwQApBo2yCJo32RMAAAFtXfeBSQAABAMASDBGAiEAyrR31T85HGekHZdD\n" +
            "r/m6flxqQaUIGcAJ5WcrBuIBuYkCIQD0rDdm+vM5/lNXIfjjrPLhATFEvrxpXJvu\n" +
            "+sW4Ntm94jANBgkqhkiG9w0BAQsFAAOCAQEAjbMEFeNXFy3YQSr8O0+fY7qwaAzk\n" +
            "vq65Ef/B2zvqO375+JI21grUikmFUnDiAaM8Y+8PJkOXDiuxR2/XCLsXpxCcPqQh\n" +
            "V0MZlqXtjKZjBACILBX7aqGibojJTIlo0Dkd+LfPwswfXscTbb1CUXpUPn7CiUj5\n" +
            "0WwfvjjQXny0NAB6WEkBMEBx6/Q75dvltoV9N1BZVer9hov6UTDuSad86faX2QF2\n" +
            "aIEjrTJY3m2HqnIYf/lQxuDUDW0h7ddGGsIEBDM8z7M/rvT068ssRqJ8uecGjMaz\n" +
            "JElX8VDgMux2kyjTAiAFD5QO+KTfySri9QXptik3wo66zDOmkVES1snvVQ==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID_CLASS_2, INT_CLASS_2},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED_CLASS_2, INT_CLASS_2},
                ValidatePathWithParams.Status.REVOKED,
                "Mon Sep 23 04:53:18 PDT 2019", System.out);
    }
}

class BuypassClass3 {

    // Owner: CN=Buypass Class 3 CA 2, O=Buypass AS-983163327, C=NO
    // Issuer: CN=Buypass Class 3 Root CA, O=Buypass AS-983163327, C=NO
    // Serial number: 1be0dc6a3e7f220475
    // Valid from: Mon Mar 25 05:12:16 PDT 2019 until: Sat Oct 26 01:16:17 PDT 2030
    private static final String INT_CLASS_3 = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFKTCCAxGgAwIBAgIJG+Dcaj5/IgR1MA0GCSqGSIb3DQEBCwUAME4xCzAJBgNV\n" +
            "BAYTAk5PMR0wGwYDVQQKDBRCdXlwYXNzIEFTLTk4MzE2MzMyNzEgMB4GA1UEAwwX\n" +
            "QnV5cGFzcyBDbGFzcyAzIFJvb3QgQ0EwHhcNMTkwMzI1MTIxMjE2WhcNMzAxMDI2\n" +
            "MDgxNjE3WjBLMQswCQYDVQQGEwJOTzEdMBsGA1UECgwUQnV5cGFzcyBBUy05ODMx\n" +
            "NjMzMjcxHTAbBgNVBAMMFEJ1eXBhc3MgQ2xhc3MgMyBDQSAyMIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvU4V2hRFFe4K7BMEmm4IoMTwS7NyDQB4JEar\n" +
            "dV1qBsKHIIQJDm9hbCakcLIWXVv6vYrJZ1AEF0b6awBwhhlqXlyNnOtNa9uR+IAP\n" +
            "86d4yOGpgHSlNAhdtOOk9Qw6MUzzBo1lyoYmoL0f5n02SMrlMcArSg458o08eDUx\n" +
            "4iZs4dXDR9Hjxac2s+mdAO35Js8VK/D50AIMDJvHVeCMw+rumZkNZuRqM7PjIK+u\n" +
            "BmbqO8A95PeqQEWHvM5nchlV1+ZGNVqHHSJenlMnVKytGv+4KJp7U741H/9cMbd2\n" +
            "X2PXsewWWFhGXoS8R9VXQ5xb3hF6324FQXvcA1mXRv6DAJedXQIDAQABo4IBCzCC\n" +
            "AQcwDwYDVR0TAQH/BAUwAwEB/zAfBgNVHSMEGDAWgBRHuM3/5W/u+LLsL04O+SWw\n" +
            "jjxrwzAdBgNVHQ4EFgQUIjAu0vv2S8rAuDvSBMTpcuaXmwwwDgYDVR0PAQH/BAQD\n" +
            "AgEGMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjARBgNVHSAECjAIMAYG\n" +
            "BFUdIAAwPQYDVR0fBDYwNDAyoDCgLoYsaHR0cDovL2NybC5idXlwYXNzLm5vL2Ny\n" +
            "bC9CUENsYXNzM1Jvb3RDQS5jcmwwMwYIKwYBBQUHAQEEJzAlMCMGCCsGAQUFBzAB\n" +
            "hhdodHRwOi8vb2NzcC5idXlwYXNzLmNvbTANBgkqhkiG9w0BAQsFAAOCAgEAo42Y\n" +
            "fp96nUiZbZsqvYBID3Sqtx3jJfU8gNHFeXgkS0pxYHHYUwsVSVRjw+BGVEGUswpF\n" +
            "MaYMCZD37ZL0JpvvXWrCDaMb/GqDJAQHLLTyVKPGGGIWCZH/FrhnNvcpt2XXA8lU\n" +
            "Ujzp5nZPuqvenzQ/aXHI4sH5sN/QjyKVMSa/6RbWBeQmvIdgyM+0jIR5/r6UGiKM\n" +
            "ar55trZgnlIbvQJ/w8QTmI/NwvA5CtRaOslQBxeKoAR0BuA/lRWnocXa/BM5uO6P\n" +
            "ULL7ct/uI1bS+YThHXHmFybI6kDf+RhRzWY9165ZP96PBph6smQkxPDAz2b8v+mh\n" +
            "LThH+5hkqnoetYfK2MdBYinceGPP3gZ+uBSDDI2o6vdVvdg7G96GP1OEtgTEqZa3\n" +
            "glVafckpn/8F5CisypdQuZ5zyy/6SXZCKkPcikR87ysSKnjtteXbxMWVtwkeBALT\n" +
            "K7DbJA+5aOCYRNj6CJGULQKiGlC01/ipORKewf5J3yus81lLHzBmgQMA5l9RL8rV\n" +
            "6dI246mPpQ+8WDLsDrK3ydSDv5izgdVHzhL0tT2u4vwSq2WUqCgi4xLIA1N/fA2H\n" +
            "xEW7zh0X/3YVz++g/6bd7iqRD9nRRZxACekRbza7AqU5xN1UjvVtCJQ9VC74K9KP\n" +
            "pBoLWE2Bz5ksL9VUc4kS+WGORvZrSE1EpBq6cHc=\n" +
            "-----END CERTIFICATE-----";

    // Owner: SERIALNUMBER=983163327, CN=valid.businessplus.ca23.ssl.buypass.no, O=BUYPASS AS,
    // L=OSLO, OID.2.5.4.17=0484, C=NO
    // Issuer: CN=Buypass Class 3 CA 2, O=Buypass AS-983163327, C=NO
    // Serial number: 267b7a9f0c3da9b94b39
    // Valid from: Mon Sep 23 04:17:42 PDT 2019 until: Mon Oct 11 14:59:00 PDT 2021
    private static final String VALID_CLASS_3 = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGUTCCBTmgAwIBAgIKJnt6nww9qblLOTANBgkqhkiG9w0BAQsFADBLMQswCQYD\n" +
            "VQQGEwJOTzEdMBsGA1UECgwUQnV5cGFzcyBBUy05ODMxNjMzMjcxHTAbBgNVBAMM\n" +
            "FEJ1eXBhc3MgQ2xhc3MgMyBDQSAyMB4XDTE5MDkyMzExMTc0MloXDTIxMTAxMTIx\n" +
            "NTkwMFowgYUxCzAJBgNVBAYTAk5PMQ0wCwYDVQQRDAQwNDg0MQ0wCwYDVQQHDARP\n" +
            "U0xPMRMwEQYDVQQKDApCVVlQQVNTIEFTMS8wLQYDVQQDDCZ2YWxpZC5idXNpbmVz\n" +
            "c3BsdXMuY2EyMy5zc2wuYnV5cGFzcy5ubzESMBAGA1UEBRMJOTgzMTYzMzI3MIIB\n" +
            "IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArqj6dPVIQUULBV/S+u2/Rfko\n" +
            "3BljX/KMEAclJHPu6AbJ2Dw5oLqCynOfTwLmGl3IRBQuDUAuoLdaptIhaXR2VTsF\n" +
            "8SWdHNXkykC2eD0XkAUdTuKgRm/3U4f0T3XQsjwKOEQGECwGEWJekBL73retSRWe\n" +
            "Ccc19NpSKZ5rmRnQSlKLfqUyihmw2xXmIWwEmBq0OOyG8ic3C11Zxh6yUOtlZJqB\n" +
            "lWqbAAOK5SXTNV0qozwgkSvtAtJvUo2++rng35Oj8MvjKQjLi92NnSpjbj3rUivW\n" +
            "++44X94IgoF9dITkSMnubXhaTLnciM08R8jmCFj877NRrVJRmcJhPfP1yHnR3wID\n" +
            "AQABo4IC+jCCAvYwCQYDVR0TBAIwADAfBgNVHSMEGDAWgBQiMC7S+/ZLysC4O9IE\n" +
            "xOly5pebDDAdBgNVHQ4EFgQUKJCKAxRR7K6pedVONDSn58EOzQcwDgYDVR0PAQH/\n" +
            "BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAfBgNVHSAEGDAW\n" +
            "MAoGCGCEQgEaAQMEMAgGBmeBDAECAjA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8v\n" +
            "Y3JsLmJ1eXBhc3Mubm8vY3JsL0JQQ2xhc3MzQ0EyLmNybDAxBgNVHREEKjAogiZ2\n" +
            "YWxpZC5idXNpbmVzc3BsdXMuY2EyMy5zc2wuYnV5cGFzcy5ubzBqBggrBgEFBQcB\n" +
            "AQReMFwwIwYIKwYBBQUHMAGGF2h0dHA6Ly9vY3NwLmJ1eXBhc3MuY29tMDUGCCsG\n" +
            "AQUFBzAChilodHRwOi8vY3J0LmJ1eXBhc3Mubm8vY3J0L0JQQ2xhc3MzQ0EyLmNl\n" +
            "cjCCAXwGCisGAQQB1nkCBAIEggFsBIIBaAFmAHYAu9nfvB+KcbWTlCOXqpJ7RzhX\n" +
            "lQqrUugakJZkNo4e0YUAAAFtXdd3CgAABAMARzBFAiEA/pTOtw6i2DJS0R56KwVF\n" +
            "Huy+LonG7bICWAe1vnCNud4CIE7/KRDu9Jys24rtmLz9yCNYJfZDvooK5PT9+rWR\n" +
            "OC4+AHUApLkJkLQYWBSHuxOizGdwCjw1mAT5G9+443fNDsgN3BAAAAFtXdd54gAA\n" +
            "BAMARjBEAiB09qp4sGA+Kxg823hea3ZyTV7mU1ZQ9j9fqqX8KZ1mpwIgUICM2H0Y\n" +
            "8z+V9m+6SutZ5WTD+Arg3K8O6/dvyKu0QmEAdQBvU3asMfAxGdiZAKRRFf93FRwR\n" +
            "2QLBACkGjbIImjfZEwAAAW1d13cSAAAEAwBGMEQCIFLqxvNOKVFlTjHPXwk93VeW\n" +
            "zCqFtcxJkunD/iiv0Kn9AiBoyvUrjYn4MPTht9zb0OyaSMWb00/HXP/4AVmUzHrz\n" +
            "YzANBgkqhkiG9w0BAQsFAAOCAQEAsmQAOn1f1CbvnOpggS2efmy1pQXvvw+YeCYP\n" +
            "bElO578h7scn8al4N7huQZ/z14BELe0chGWNA/ReW5nAu3SUOiv+E8/kv9i9Y8ul\n" +
            "MJPL62nXW6Z/mkyystuBNtON420iWL/gS/vduxSZE/iBB4znctDpXS917/XWf31Y\n" +
            "ZonemF3MSfi/s9V0Ic82ZY/+HZ4NLTDyKRd4kFF58OoH9RZNb6g8MbTp+gPadiUG\n" +
            "UcfPGV3yGiugQa7WHTl7QJ9ishyafiZ4hpeKem6TMDEztgGyLIZ4MSxQvoeI2jJP\n" +
            "KjHd5fW/HClbEcrN+w0a0MUNMaAOaZfMS7jS6sDpaVL8D0EX5A==\n" +
            "-----END CERTIFICATE-----";

    // Owner: SERIALNUMBER=983163327, CN=revoked.businessplus.ca23.ssl.buypass.no, O=BUYPASS AS,
    // L=OSLO, OID.2.5.4.17=0484, C=NO
    // Issuer: CN=Buypass Class 3 CA 2, O=Buypass AS-983163327, C=NO
    // Serial number: 267cee3fab06c615fb27
    // Valid from: Mon Sep 23 04:56:56 PDT 2019 until: Thu Sep 23 14:59:00 PDT 2021
    private static final String REVOKED_CLASS_3 = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGWDCCBUCgAwIBAgIKJnzuP6sGxhX7JzANBgkqhkiG9w0BAQsFADBLMQswCQYD\n" +
            "VQQGEwJOTzEdMBsGA1UECgwUQnV5cGFzcyBBUy05ODMxNjMzMjcxHTAbBgNVBAMM\n" +
            "FEJ1eXBhc3MgQ2xhc3MgMyBDQSAyMB4XDTE5MDkyMzExNTY1NloXDTIxMDkyMzIx\n" +
            "NTkwMFowgYcxCzAJBgNVBAYTAk5PMQ0wCwYDVQQRDAQwNDg0MQ0wCwYDVQQHDARP\n" +
            "U0xPMRMwEQYDVQQKDApCVVlQQVNTIEFTMTEwLwYDVQQDDChyZXZva2VkLmJ1c2lu\n" +
            "ZXNzcGx1cy5jYTIzLnNzbC5idXlwYXNzLm5vMRIwEAYDVQQFEwk5ODMxNjMzMjcw\n" +
            "ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDtpNExWd+hjl/ouL/B/pdc\n" +
            "InUzEywQO3rzXs3psBdQ1lDhG/9Fcq78uqyri4edtJNDGb1XadktKeRC+NtUqMkE\n" +
            "IFOXvaVjLxa61c8K5mh3CVDrAiPyxVcnm8vkuQPMsy1BTOl9TZq9heIukG/lcfzW\n" +
            "6tU6mOD9yx1NzXSVN5cvDCbbDnEZiJSuazXI4O02as66SWI27WKsk21+SKCGAtGC\n" +
            "kI0PW4FrXm43/jxX1CoImIfTLkDInMq7HHsQRsGQ3OjbJLfRz/2obyjHUU5ki6vd\n" +
            "z16mA5ITLFIG36HxbPn337175R9RwOpWkN84xVlL3VQdznCVoiOjzBiOMpdm0Jwp\n" +
            "AgMBAAGjggL/MIIC+zAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFCIwLtL79kvKwLg7\n" +
            "0gTE6XLml5sMMB0GA1UdDgQWBBSGUQTUB4BilG/EMaHHDAYNPewf8zAOBgNVHQ8B\n" +
            "Af8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMB8GA1UdIAQY\n" +
            "MBYwCgYIYIRCARoBAwQwCAYGZ4EMAQICMDoGA1UdHwQzMDEwL6AtoCuGKWh0dHA6\n" +
            "Ly9jcmwuYnV5cGFzcy5uby9jcmwvQlBDbGFzczNDQTIuY3JsMDMGA1UdEQQsMCqC\n" +
            "KHJldm9rZWQuYnVzaW5lc3NwbHVzLmNhMjMuc3NsLmJ1eXBhc3Mubm8wagYIKwYB\n" +
            "BQUHAQEEXjBcMCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5idXlwYXNzLmNvbTA1\n" +
            "BggrBgEFBQcwAoYpaHR0cDovL2NydC5idXlwYXNzLm5vL2NydC9CUENsYXNzM0NB\n" +
            "Mi5jZXIwggF/BgorBgEEAdZ5AgQCBIIBbwSCAWsBaQB2ALvZ37wfinG1k5Qjl6qS\n" +
            "e0c4V5UKq1LoGpCWZDaOHtGFAAABbV37Y7oAAAQDAEcwRQIgYbaNSR3R5x9p9sYJ\n" +
            "UzRDdd/lbELb05u9GqlLtl4M61YCIQCTBecXTbMs4zuG/wu722HZy/XgD6fiQySp\n" +
            "FhHDO3CYagB2AKS5CZC0GFgUh7sTosxncAo8NZgE+RvfuON3zQ7IDdwQAAABbV37\n" +
            "Y7wAAAQDAEcwRQIgD8j40M03oLMCg5WmFBN7VL6169F7rKatE12btLQRYtYCIQC0\n" +
            "rDhQiZP7j14Y4JqEFQx6UHl3dvxLxZTDW34Z54IUWQB3AG9Tdqwx8DEZ2JkApFEV\n" +
            "/3cVHBHZAsEAKQaNsgiaN9kTAAABbV37YOUAAAQDAEgwRgIhANTGHD1g2pbsTtoN\n" +
            "CJ2m6nfxm9jB3huftKGDjeo7EyxHAiEA3EYNUc6hr+4Q9lMAphUgpW6oyaNCsIzl\n" +
            "izbNhq8dBRYwDQYJKoZIhvcNAQELBQADggEBADUuO4MmYjPkmkik5tjUPiiDDXEQ\n" +
            "A41jr72qmdleYdkhnaKAJa8Enn6j/ySRV0enA7yqJeNp1qgPQFvlOh3TqFB3Ae5b\n" +
            "XAfL2B7vKbegpjKm8dVH5RurqVm9xZcXb1nbwfu2k3lqqsp/uwqvLBItJDvA8pfi\n" +
            "2R46sEtj2gFpAlKFDwepuaklqhrvEoIjIaAL0RrGfKY0oRQw1YMbPNIebsVaWr04\n" +
            "rt6tlxrq7PyW1w9Mt3445WA1NzSWc7pAjFLfY6u87QaPHI4ES31H9xxRDsxmr6Y3\n" +
            "BJmiWd5uUxev0nVw0saqvlo4yAEBq4rI/DieKcQI4qEI8myzoS0R0azMfLM=\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled)
            throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID_CLASS_3, INT_CLASS_3},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED_CLASS_3, INT_CLASS_3},
                ValidatePathWithParams.Status.REVOKED,
                "Mon Sep 23 04:57:31 PDT 2019", System.out);
    }
}
