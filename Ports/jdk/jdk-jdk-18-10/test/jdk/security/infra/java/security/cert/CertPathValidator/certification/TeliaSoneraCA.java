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
 * @bug 8210432
 * @summary Interoperability tests with TeliaSonera Root CA v1
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath TeliaSoneraCA OCSP
 * @run main/othervm -Djava.security.debug=certpath TeliaSoneraCA CRL
 */

/*
 * Obtain TLS test artifacts for TeliaSonera Root CA v1 from:
 *
 * Valid TLS Certificates:
 * https://juolukka.cover.sonera.net:10443/
 *
 * Revoked TLS Certificates:
 * https://juolukka.cover.sonera.net:10444/
 */
public class TeliaSoneraCA {

    // Owner: CN=TeliaSonera Server CA v2, O=TeliaSonera, C=FI
    // Issuer: CN=TeliaSonera Root CA v1, O=TeliaSonera
    private static final String INT = "-----BEGIN CERTIFICATE-----\n"
            + "MIIHHjCCBQagAwIBAgIQTEYq9tv794BPhMF8/qlytjANBgkqhkiG9w0BAQsFADA3\n"
            + "MRQwEgYDVQQKDAtUZWxpYVNvbmVyYTEfMB0GA1UEAwwWVGVsaWFTb25lcmEgUm9v\n"
            + "dCBDQSB2MTAeFw0xNDEwMTYwODA5NTdaFw0zMjEwMTYwNTA0MDBaMEYxCzAJBgNV\n"
            + "BAYTAkZJMRQwEgYDVQQKDAtUZWxpYVNvbmVyYTEhMB8GA1UEAwwYVGVsaWFTb25l\n"
            + "cmEgU2VydmVyIENBIHYyMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA\n"
            + "rwQN5rfRLbVAiYWLJF9SI4YLm8oqrtf8OjGybgoLyiMIo8nhY/atuGRFWCQNOnUK\n"
            + "caZn29C360PlC5yYrsrSHuouROisqHSJcgA7HvV+37Rcry7daeDj6rfyx4yI5dmj\n"
            + "LwHkK0j1NzhX1JxFDgPeLNuebgzv/j8OfRhYK/BttpystC4Zgm3gZheKDjYsDS5D\n"
            + "gjffuOysP3vewrcuw0EIZFx+HawuwNBLq4tMf4VSitYDHJSLIM2TeXZGGY5slTbT\n"
            + "yLnrU5mIzG9WKqxyy7qHuFw1JtlAXkCLmUEVaF9M+dRPiGIjlDrpBgbDD9mT2CSk\n"
            + "V/XG1696/voY5xB8KNIC1cOSmSO7kdJyR5tWiDIJiwMXrTwG+kZiqlbcKDsZeJ9p\n"
            + "5bZxXO0pEpde3wgEYRvFr5Cx4vcz4h5pom9coJOCW9tqXU43KcueTrt4Ks9f92q1\n"
            + "ehjyEnCh0BCdrjUOXsUtFosm9qxJnDwVlThYhS9EHuCTNBgj1Yxj6A+8fwwJP9DN\n"
            + "CbWQx5afT+h+9FNDNRC/nEcesP1Yh9s15Se270pQW0CejUNziYG7Dft7T+PVH/fU\n"
            + "zaWU8g0tJjtuQgiCWVqw4WkUmYY2S0R89zAotcpz2mvNO8ma2iJbubHi3c0ULfHH\n"
            + "nkWKsdpzZmK4N0Wi6/V5yWdmL5RFkFecL8r7+9OtCB0CAwEAAaOCAhUwggIRMIGK\n"
            + "BggrBgEFBQcBAQR+MHwwLQYIKwYBBQUHMAGGIWh0dHA6Ly9vY3NwLnRydXN0LnRl\n"
            + "bGlhc29uZXJhLmNvbTBLBggrBgEFBQcwAoY/aHR0cDovL3JlcG9zaXRvcnkudHJ1\n"
            + "c3QudGVsaWFzb25lcmEuY29tL3RlbGlhc29uZXJhcm9vdGNhdjEuY2VyMBIGA1Ud\n"
            + "EwEB/wQIMAYBAf8CAQAwVQYDVR0gBE4wTDBKBgwrBgEEAYIPAgMBAQIwOjA4Bggr\n"
            + "BgEFBQcCARYsaHR0cHM6Ly9yZXBvc2l0b3J5LnRydXN0LnRlbGlhc29uZXJhLmNv\n"
            + "bS9DUFMwDgYDVR0PAQH/BAQDAgEGMIHGBgNVHR8Egb4wgbswQKA+oDyGOmh0dHA6\n"
            + "Ly9jcmwtMy50cnVzdC50ZWxpYXNvbmVyYS5jb20vdGVsaWFzb25lcmFyb290Y2F2\n"
            + "MS5jcmwwd6B1oHOGcWxkYXA6Ly9jcmwtMS50cnVzdC50ZWxpYXNvbmVyYS5jb20v\n"
            + "Y249VGVsaWFTb25lcmElMjBSb290JTIwQ0ElMjB2MSxvPVRlbGlhU29uZXJhP2Nl\n"
            + "cnRpZmljYXRlcmV2b2NhdGlvbmxpc3Q7YmluYXJ5MB0GA1UdDgQWBBQvSTwpT9cH\n"
            + "JfnGjNVk9WY9EoMilTAfBgNVHSMEGDAWgBTwj1k4ALP1j5qWDNXr+nuqF+gTEjAN\n"
            + "BgkqhkiG9w0BAQsFAAOCAgEAg9EVFW6ioZ2ctrX8KqvW9XPYZR01yNgqlO7pwBWf\n"
            + "HzuBCbUdyVzumfQnU24Sce92oMtEfyuxIOmhvoXU7LpnYlH3Q29UGP5dL0D3edGz\n"
            + "HeU6Tf8bkcOEHtnTrkd+y+rfFSDWYl9r1y993NAcrBHhroQCE53mlrO7TjXa3zDq\n"
            + "6LGR8T8VgvGw0IBz6mzAks0wMYB0b4uREPmWXi+m+RqG3lnpl+eBzz6YVLkxIYMq\n"
            + "QIXJIBsu4/ybmadsfdql6E8Lo3dKVD4UG10mtd+iPbJiBiW/a9VbEe3NVKIv4H2y\n"
            + "HqYcxDXAeUI66E3K2cjCmKoQaa0Ywt02ikZFd0v1OWNPS7YWbEJWkVR1PcPMESK9\n"
            + "6HKI4xhG2tJesmXjQ8q8aSx2u79Zts3ewjKqTmurf6FXW3u9TpSCUe6Drr/3X7Ve\n"
            + "nBy4M0sLwCecD/L9gjTa+EItQTYzCkpxiMO49tQdX/BpwgWju4Kg3qkaBNTzvSlk\n"
            + "gdnRJqCUkVuzwK4yBqUoyRz3prlhvvRGdZJKf6IXRDhncpey5pm0PQYQ4cArx7Go\n"
            + "AaAKz0ZTHOKjnM2KIdUhBJQybL7oPklSfkeMWoUoYED6R4YMTt/JXX4ixEb5DgDJ\n"
            + "0F+bNcF7qGrJTkTx0Ccy4BuuY05hJckd72E7WdmjN7DDeosghgWZNV/6D7N5tfxo\n"
            + "nlU=\n"
            + "-----END CERTIFICATE-----";

    // Owner: CN=juolukka.cover.sonera.net, OU=security, O=Telia Finland Oyj, L=helsinki, C=FI
    // Issuer: CN=TeliaSonera Server CA v2, O=TeliaSonera, C=FI
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHiDCCBXCgAwIBAgIPAWOq14hk136UDQY3WSjLMA0GCSqGSIb3DQEBCwUAMEYx\n" +
            "CzAJBgNVBAYTAkZJMRQwEgYDVQQKDAtUZWxpYVNvbmVyYTEhMB8GA1UEAwwYVGVs\n" +
            "aWFTb25lcmEgU2VydmVyIENBIHYyMB4XDTE4MDUyOTA3NDA0MVoXDTE5MDUyOTA3\n" +
            "NDA0MVowczELMAkGA1UEBhMCRkkxETAPBgNVBAcMCGhlbHNpbmtpMRowGAYDVQQK\n" +
            "DBFUZWxpYSBGaW5sYW5kIE95ajERMA8GA1UECwwIc2VjdXJpdHkxIjAgBgNVBAMM\n" +
            "GWp1b2x1a2thLmNvdmVyLnNvbmVyYS5uZXQwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n" +
            "DwAwggEKAoIBAQDLks9F8ZUqV9G4jn3fY234OX09Dmqqtuk0qAmjWpF0JAn2o64t\n" +
            "whVxFLx9e2IwUPTQgyo6FwRsiT19m99BhgxYnJOxVRwURxSL3mqlV9gX4oFMmT4O\n" +
            "EOYEjaJXi8ne1pJX80y2hVQ48XqgODnKdKZVwa5YoeWZQJiaq+C5JkMDN8qzpiyQ\n" +
            "X3EfJspLkKy2E+UVxWmfnyf0v70ES9TQ8qgxwvsf7LRZ8Jixq7TTO5VbqWsdBvJC\n" +
            "9Zm2aBOYJ7ptSZQ5YDfeUJG2c9S/zFmngoPnTrvAZwUeU3YTrbdZQy899ZOatWac\n" +
            "6lHUYU2EagEmbj/jtIvJ6wMbzhleIXRQFWibAgMBAAGjggNEMIIDQDAfBgNVHSME\n" +
            "GDAWgBQvSTwpT9cHJfnGjNVk9WY9EoMilTAdBgNVHQ4EFgQUbMozh4osL4gFJvb5\n" +
            "baELpQSKEhIwDgYDVR0PAQH/BAQDAgSwME4GA1UdIARHMEUwQwYGZ4EMAQICMDkw\n" +
            "NwYIKwYBBQUHAgEWK2h0dHA6Ly9yZXBvc2l0b3J5LnRydXN0LnRlbGlhc29uZXJh\n" +
            "LmNvbS9DUFMwJAYDVR0RBB0wG4IZanVvbHVra2EuY292ZXIuc29uZXJhLm5ldDBN\n" +
            "BgNVHR8ERjBEMEKgQKA+hjxodHRwOi8vY3JsLTMudHJ1c3QudGVsaWFzb25lcmEu\n" +
            "Y29tL3RlbGlhc29uZXJhc2VydmVyY2F2Mi5jcmwwHQYDVR0lBBYwFAYIKwYBBQUH\n" +
            "AwIGCCsGAQUFBwMBMIGGBggrBgEFBQcBAQR6MHgwJwYIKwYBBQUHMAGGG2h0dHA6\n" +
            "Ly9vY3NwLnRydXN0LnRlbGlhLmNvbTBNBggrBgEFBQcwAoZBaHR0cDovL3JlcG9z\n" +
            "aXRvcnkudHJ1c3QudGVsaWFzb25lcmEuY29tL3RlbGlhc29uZXJhc2VydmVyY2F2\n" +
            "Mi5jZXIwggF/BgorBgEEAdZ5AgQCBIIBbwSCAWsBaQB2AG9Tdqwx8DEZ2JkApFEV\n" +
            "/3cVHBHZAsEAKQaNsgiaN9kTAAABY6rXpS0AAAQDAEcwRQIgfMLEFYxQcncL3am/\n" +
            "W2x7DMZ1+Vh1tDLw/0qIQB40VBQCIQC1eyF8Q6CcQs+gIgzpy7OiZSosSlykyOgW\n" +
            "qHkj/0UPygB3AO5Lvbd1zmC64UJpH6vhnmajD35fsHLYgwDEe4l6qP3LAAABY6rX\n" +
            "pLEAAAQDAEgwRgIhAJxveFVsFrfttSJIxHsMPAvvevptaV2CxsGwubAi8wDDAiEA\n" +
            "jNbbYfUiYtmQ5v4yc6T+GcixztNIlMzQ7OTK+u9zqSoAdgBVgdTCFpA2AUrqC5tX\n" +
            "PFPwwOQ4eHAlCBcvo6odBxPTDAAAAWOq16YXAAAEAwBHMEUCIQCCkCL2zn/AoMVI\n" +
            "BdsoJelUBLsAnQ+GlIafiyZYcCwhBAIgdsFM05eNmL5hfn3+WtfgmipwcK1qp7kO\n" +
            "ONzO69aqrnEwDQYJKoZIhvcNAQELBQADggIBAIl5UWSwCXF85+2lU6t89K7I4TvZ\n" +
            "Ggof0NLngea9qxBq00opfnl9i2LPRnsjh9s3iA29i2daTEuJn3qt3Ygcm27Jd7WM\n" +
            "5StcxQ483GAaL5s5m2QqkZB8eLfez3tIyCMGCAyixBDNRNPVI4xZr6sSOenWtipo\n" +
            "gMt+/gvRIMdMT79IXPFz4W9RWCwnfJNOlfH2OkS3KZYaPSaEvs6sfMW1DDZosrBy\n" +
            "6F+DITPLllOVSE4+PTxvXLKVy+srFwF1VocQXKkWMHQ7AfWNnOGzb7B1qg7gsw0n\n" +
            "axqinyCjkhMpHpcVtmD9Pi15HLFDIy9yI2S+FHJQfhUSmM/LdCWzQpnee6/Wo+uw\n" +
            "p0Jg2v6v9GGaqfpuiVJPFN9dOv3OjMU7DL5lgMRWFRo2T8+wBHXDyBhT0W0y5kRJ\n" +
            "eWA7t6CnkziHuaOihZAHUH3nn5exjqUFVS0ThbF6hxN7HAlq/xIbTKlZjkLlc14W\n" +
            "fB8vkxJyy/tgBZ4dCj9Y1Y32d4eFT5JZJgqgkN59SmX56BswNXncGrk/vWZFFx+g\n" +
            "9dgb8QSe8KseD1iSLc7SsqVDv8NPYdaI3eZ90W8Wv0/CDls321O6UbAmURzQwFGB\n" +
            "w8WnteoVBi6Wf6M1TxIfJsXBYeIN0BB6AYc8cmZIOtx2C8aH4JJT45MyFnBv3ac5\n" +
            "Ahs9pGn/+K+5yb2e\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=juolukka.cover.sonera.net, OU=Security, O=TeliaSonera Finland, L=Helsinki, C=FI
    // Issuer: CN=TeliaSonera Server CA v2, O=TeliaSonera, C=FI
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGEDCCA/igAwIBAgIRAKWJTjs6v04ZTyb2wJxfnJswDQYJKoZIhvcNAQELBQAw\n" +
            "RjELMAkGA1UEBhMCRkkxFDASBgNVBAoMC1RlbGlhU29uZXJhMSEwHwYDVQQDDBhU\n" +
            "ZWxpYVNvbmVyYSBTZXJ2ZXIgQ0EgdjIwHhcNMTYxMjIzMDcwMTQ2WhcNMTkxMjIz\n" +
            "MDcwMTQ2WjB1MQswCQYDVQQGEwJGSTERMA8GA1UEBwwISGVsc2lua2kxHDAaBgNV\n" +
            "BAoME1RlbGlhU29uZXJhIEZpbmxhbmQxETAPBgNVBAsMCFNlY3VyaXR5MSIwIAYD\n" +
            "VQQDDBlqdW9sdWtrYS5jb3Zlci5zb25lcmEubmV0MIIBIjANBgkqhkiG9w0BAQEF\n" +
            "AAOCAQ8AMIIBCgKCAQEAt2u92TgTFdm1OEfmWFPe+ESBi+2ox4y1EDoin8RydMyO\n" +
            "DI6+0HHnKfDZa1YViI5b6MLJKWIAyUszAg5hc0S3upElfSsBvUW6zuQTxMi2vTYE\n" +
            "4tcqwIEyCUaiv4wC+DuO5CyGR32yR6HB/W5Ny200dPs2SO03ESEJ+LH4Tw5AI8JJ\n" +
            "UZHW+lA+yUHnlc3q47svpbspjt0C/THyukd1hbXTBB0mPXqPux+ClvtZBWUJb7ti\n" +
            "1cPfcCNd79KRObzcgxqcOIaUFz4LjjKezhzVSL7tJOANOHZ09qDeOAkk/X9POx4h\n" +
            "a5XyWfH1zaQ0QlZ2mKBeHebCIJkgTZZVipagRVOgcwIDAQABo4IByDCCAcQwgY0G\n" +
            "CCsGAQUFBwEBBIGAMH4wLQYIKwYBBQUHMAGGIWh0dHA6Ly9vY3NwLnRydXN0LnRl\n" +
            "bGlhc29uZXJhLmNvbTBNBggrBgEFBQcwAoZBaHR0cDovL3JlcG9zaXRvcnkudHJ1\n" +
            "c3QudGVsaWFzb25lcmEuY29tL3RlbGlhc29uZXJhc2VydmVyY2F2Mi5jZXIwHwYD\n" +
            "VR0jBBgwFoAUL0k8KU/XByX5xozVZPVmPRKDIpUwTgYDVR0gBEcwRTBDBgZngQwB\n" +
            "AgIwOTA3BggrBgEFBQcCARYraHR0cDovL3JlcG9zaXRvcnkudHJ1c3QudGVsaWFz\n" +
            "b25lcmEuY29tL0NQUzBNBgNVHR8ERjBEMEKgQKA+hjxodHRwOi8vY3JsLTMudHJ1\n" +
            "c3QudGVsaWFzb25lcmEuY29tL3RlbGlhc29uZXJhc2VydmVyY2F2Mi5jcmwwHQYD\n" +
            "VR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMA4GA1UdDwEB/wQEAwIEsDAkBgNV\n" +
            "HREEHTAbghlqdW9sdWtrYS5jb3Zlci5zb25lcmEubmV0MB0GA1UdDgQWBBSa+vJH\n" +
            "I6Lt9Aqw5ondhoZu4/IJezANBgkqhkiG9w0BAQsFAAOCAgEASRK1l1MZb/IRlyi+\n" +
            "XjfZcxJdFuNzW2kpZstW6Ni2XiD3p7aROBfDFtu7GajzZHb6p76auDb4NwJgeE/3\n" +
            "6gnXoIK00HwpF2RAhxDpkF8r3q0jSqGhSv/xz9Nx7JBzgqfSw3Ha4ohioIed3uc+\n" +
            "nMDyvVenio4GYgtxIIubSybCxMv/lBA/S4daIVCYK3VOoBbM2F36ecAKvRU5vIWM\n" +
            "urXsfANL3u4qgJpaM0DclzFsOkVsRPffzToko/Nr6pGXYjt47IzTRlwLMnLehoZW\n" +
            "ZZMGMVVOlR7XGf81UjWB6OsKeoQ4FWgcb/rIJcZusm+LqvnsCHuC3gtuC2nGA7lr\n" +
            "fseUlG7QZN9/QfUIyvL69wAzeVj1cUcd7GHcAH9DyZJfI8orv4PyUvitDdgISkFu\n" +
            "GZ562O7cGmCv00/6I4t0z9wZal8a5lRDoKXAYy+u/adrO1JjLwi11y/DTw9LQ7sJ\n" +
            "gVP/v2GsI0ajF9A6z33UHN9uxXZVmQNvOiMkcJiGLovFgu5zxoAg2W3pHjbBbeL8\n" +
            "v5MPqgsKafgzaSRtXBBvaISHi9hhRR8v/qSwO3NyLm8uAhQD4x+OPHrmQ/s16j45\n" +
            "Ib53UHj1k6byXGUqDgzFBsmEPV6Shf2C4/HcRHpAX8wQx3xVwDtRzDpNUR6vnNfi\n" +
            "PwzRU1xsQKd8llmgl4l+fYV0tBA=\n" +
            "-----END CERTIFICATE-----";

    public static void main(String[] args) throws Exception {

        ValidatePathWithParams pathValidator = new ValidatePathWithParams(null);

        if (args.length >= 1 && "CRL".equalsIgnoreCase(args[0])) {
            pathValidator.enableCRLCheck();
        } else {
            // OCSP check by default
            pathValidator.enableOCSPCheck();
        }

        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Thu Dec 22 23:14:55 PST 2016", System.out);

        // reset validation date back to current date
        pathValidator.resetValidationDate();
    }
}
