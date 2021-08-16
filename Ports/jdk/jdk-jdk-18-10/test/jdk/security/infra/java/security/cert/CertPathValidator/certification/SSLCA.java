/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8243320 8256895
 * @summary Interoperability tests with SSL.com's RSA, EV RSA, and ECC CA
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath SSLCA OCSP
 * @run main/othervm -Djava.security.debug=certpath SSLCA CRL
 */

/*
 * Obtain TLS test artifacts for SSL.com CAs from:
 *
 * SSL.com RSA CA
 *     Valid - https://test-dv-rsa.ssl.com
 *     Revoked - https://revoked-rsa-dv.ssl.com/
 * SSL.com EV RSA CA
 *     Valid - https://test-ev-rsa.ssl.com
 *     Revoked - https://revoked-rsa-ev.ssl.com/
 * SSL.com ECC CA
 *     Valid - https://test-dv-ecc.ssl.com
 *     Revoked - https://revoked-ecc-dv.ssl.com/
 */
public class SSLCA {

    public static void main(String[] args) throws Exception {

        System.setProperty("jdk.security.certpath.ocspNonce", "true");
        ValidatePathWithParams pathValidator = new ValidatePathWithParams(null);
        boolean ocspEnabled = false;

        if (args.length >= 1 && "CRL".equalsIgnoreCase(args[0])) {
            pathValidator.enableCRLCheck();
        } else {
            // OCSP check by default
            pathValidator.enableOCSPCheck();
            ocspEnabled = true;
        }

        new SSLCA_RSA().runTest(pathValidator, ocspEnabled);
        new SSLCA_EV_RSA().runTest(pathValidator, ocspEnabled);
        new SSLCA_ECC().runTest(pathValidator, ocspEnabled);
    }
}

class SSLCA_RSA {

    // Owner: CN=SSL.com RSA SSL subCA, O=SSL Corporation, L=Houston, ST=Texas, C=US
    // Issuer: CN=SSL.com Root Certification Authority RSA, O=SSL Corporation, L=Houston, ST=Texas, C=US
    // Serial number: 997ed109d1f07fc
    // Valid from: Fri Feb 12 10:48:52 PST 2016 until: Wed Feb 12 10:48:52 PST 2031
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGbzCCBFegAwIBAgIICZftEJ0fB/wwDQYJKoZIhvcNAQELBQAwfDELMAkGA1UE\n" +
            "BhMCVVMxDjAMBgNVBAgMBVRleGFzMRAwDgYDVQQHDAdIb3VzdG9uMRgwFgYDVQQK\n" +
            "DA9TU0wgQ29ycG9yYXRpb24xMTAvBgNVBAMMKFNTTC5jb20gUm9vdCBDZXJ0aWZp\n" +
            "Y2F0aW9uIEF1dGhvcml0eSBSU0EwHhcNMTYwMjEyMTg0ODUyWhcNMzEwMjEyMTg0\n" +
            "ODUyWjBpMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxEDAOBgNVBAcMB0hv\n" +
            "dXN0b24xGDAWBgNVBAoMD1NTTCBDb3Jwb3JhdGlvbjEeMBwGA1UEAwwVU1NMLmNv\n" +
            "bSBSU0EgU1NMIHN1YkNBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA\n" +
            "hPYpOunhcxiF6xNzl6Tsm/Q89rnu2jVTXTBOZPaBkSD1Ic4lm7qkYwlZ/UgV5nn1\n" +
            "5ohhceYDC2AlR9RvGbP+26qrNcuE0XOdHJOB4SoY4d6OqLAQ6ZB0LdERK1Saa5lp\n" +
            "QlqHE8936dpr3hGWyqMb2LsdUuhQIzwNkLU/n9HO35irKCbKgS3FeejqkdqK5l6B\n" +
            "b11693o4bz9UZCUdBcQ/Xz06tA5cfnHvYkmmjxhj1lLTKwkQhWuIDrpbwWLO0QVO\n" +
            "c29s9ieomRKm8sYMyiBG4QqRQ/+bXwp48cF0qAByGWD6b8/gG4Xq1IBgO5p+aWFS\n" +
            "0mszkk5rsh4b3XbTHohP3oWQIOV20WWdtVWXiQuBB8RocAl0Ga//b+epiGgME5JX\n" +
            "LWXD1aDg/xHy8MUsaMlh6jDfVIFepkPnkwXDpR/n36hpgKa9dErMkgbYeEaPanLH\n" +
            "Yd0kv4xQ36PlMMs9WhoDErGcEG9KxAXN4Axr5wl6PTDn/lXcUFvQoIq/5CSP+Kt5\n" +
            "jq9tK/gRrAc4AWqRugDvQPYUm00Rqzj5Oxm5NVQYDzbyoA66CD68LETuVrfa9GuW\n" +
            "9MAZRO6CDzonAezIdNHsslDb1H8VN/k0zMxjI+0ub4IAmc3I5GfZtvYcpjtMj8L4\n" +
            "2TDS34/COov/Pf2HZ/XXGlzjZ7WPmLl4fdB6hhjs2BsCAwEAAaOCAQYwggECMDAG\n" +
            "CCsGAQUFBwEBBCQwIjAgBggrBgEFBQcwAYYUaHR0cDovL29jc3BzLnNzbC5jb20w\n" +
            "HQYDVR0OBBYEFCYUfuDc16b34tQEJ99h8cLs5zLKMA8GA1UdEwEB/wQFMAMBAf8w\n" +
            "HwYDVR0jBBgwFoAU3QQJB6L1en1SUxKSle44gCUNplkwEQYDVR0gBAowCDAGBgRV\n" +
            "HSAAMDsGA1UdHwQ0MDIwMKAuoCyGKmh0dHA6Ly9jcmxzLnNzbC5jb20vc3NsLmNv\n" +
            "bS1yc2EtUm9vdENBLmNybDAOBgNVHQ8BAf8EBAMCAYYwHQYDVR0lBBYwFAYIKwYB\n" +
            "BQUHAwEGCCsGAQUFBwMCMA0GCSqGSIb3DQEBCwUAA4ICAQAi6e/iSV5DEqDO6XjQ\n" +
            "SIIzXgc255yv6Oc2sqZnvRyVBHtHvo62jMoHY3Xunc/EofbeS4aHdYBvgkn6CNTj\n" +
            "VkCU+psWwcT3Pg83uP4k4Thu7bXvrClfS+XBlbJiCF/PSJxLrKnxRn+XIGiYl62H\n" +
            "glBhq9K8/fZrI2Qh1mZJmWE0FlxEDCb4i8SBNi8lmDogaFi8/yl32Z9ahmhxcLit\n" +
            "DU/XyKA0yOqvIrOGKH95v+/l8fQkzE1VEFvj+iyv4TXd7mRZDOsfqfIDZhrpou02\n" +
            "kXH/hcXlrR++t8kjj9wt8HHQ+FkryWI6bU3KPRJR6N8EH2EHi23Rp8/kyMs+gwaz\n" +
            "zMqnkNPbMME723rXk6/85sjOUaZCmhmRIx9rgqIWQesU962J0FruGOOasLT7WbZi\n" +
            "FsmSblmpjUAo49sIRi7X493qegyCEAa412ynybhQ7LVsTLEPxVbdmGVih3jVTif/\n" +
            "Nztr2Isaaz4LpMEo4mGCiGxec5mKr1w8AE9n6D91CvxR5/zL1VU1JCVC7sAtkdki\n" +
            "vnN1/6jEKFJvlUr5/FX04JXeomIjXTI8ciruZ6HIkbtJup1n9Zxvmr9JQcFTsP2c\n" +
            "bRbjaT7JD6MBidAWRCJWClR/5etTZwWwWrRCrzvIHC7WO6rCzwu69a+l7ofCKlWs\n" +
            "y702dmPTKEdEfwhgLx0LxJr/Aw==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=test-dv-rsa.ssl.com
    // Issuer: CN=SSL.com RSA SSL subCA, O=SSL Corporation, L=Houston, ST=Texas, C=US
    // Serial number: 4ceada4ade82a6ccd0b2ae32c0dbfd62
    // Valid from: Fri Jun 28 07:06:50 PDT 2019 until: Sun Jun 27 07:06:50 PDT 2021
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHTjCCBTagAwIBAgIQTOraSt6CpszQsq4ywNv9YjANBgkqhkiG9w0BAQsFADBp\n" +
            "MQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxEDAOBgNVBAcMB0hvdXN0b24x\n" +
            "GDAWBgNVBAoMD1NTTCBDb3Jwb3JhdGlvbjEeMBwGA1UEAwwVU1NMLmNvbSBSU0Eg\n" +
            "U1NMIHN1YkNBMB4XDTE5MDYyODE0MDY1MFoXDTIxMDYyNzE0MDY1MFowHjEcMBoG\n" +
            "A1UEAwwTdGVzdC1kdi1yc2Euc3NsLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEP\n" +
            "ADCCAQoCggEBAKlOrYr8fnHN8REfJDwgsBhJvnsU4beQIYYaOAzR8pmo8eq1U/K0\n" +
            "uwRrgJ5K61V78zBO5qmZNiivBobViftObWrq2H6QhQsYdMYXld3SEnEotIIriRHY\n" +
            "2PcqlgnFYXkqI0ZKs4kNs+j3GS0IwncJJwKtypmtLTCLK5J/kG7qB2MNfXZTIzKI\n" +
            "iZza4RUM1j67Hv3fPJzNEJ9urfjaI4xcRh5airlzBWOBU9pW87P7BgQN7cNzJQji\n" +
            "4DSvb1pSXv8sBbZk5fmG+81PyUxcfqj7Dbih0J1Aoq0YysHugsrK/kLz+CvqL9B2\n" +
            "a1JMZfob9jzcA7XPjpggLc3az2Wvv3XKqokCAwEAAaOCAzswggM3MB8GA1UdIwQY\n" +
            "MBaAFCYUfuDc16b34tQEJ99h8cLs5zLKMHwGCCsGAQUFBwEBBHAwbjBKBggrBgEF\n" +
            "BQcwAoY+aHR0cDovL3d3dy5zc2wuY29tL3JlcG9zaXRvcnkvU1NMY29tLVN1YkNB\n" +
            "LVNTTC1SU0EtNDA5Ni1SMS5jcnQwIAYIKwYBBQUHMAGGFGh0dHA6Ly9vY3Nwcy5z\n" +
            "c2wuY29tMDcGA1UdEQQwMC6CE3Rlc3QtZHYtcnNhLnNzbC5jb22CF3d3dy50ZXN0\n" +
            "LWR2LXJzYS5zc2wuY29tMFEGA1UdIARKMEgwCAYGZ4EMAQIBMDwGDCsGAQQBgqkw\n" +
            "AQMBATAsMCoGCCsGAQUFBwIBFh5odHRwczovL3d3dy5zc2wuY29tL3JlcG9zaXRv\n" +
            "cnkwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMDoGA1UdHwQzMDEwL6At\n" +
            "oCuGKWh0dHA6Ly9jcmxzLnNzbC5jb20vU1NMY29tUlNBU1NMc3ViQ0EuY3JsMB0G\n" +
            "A1UdDgQWBBQD/cmwQI853u0mOlmCjNRsAZOlEDAOBgNVHQ8BAf8EBAMCBaAwggF+\n" +
            "BgorBgEEAdZ5AgQCBIIBbgSCAWoBaAB2AO5Lvbd1zmC64UJpH6vhnmajD35fsHLY\n" +
            "gwDEe4l6qP3LAAABa55yL0QAAAQDAEcwRQIgWo8UQY3EYwyzkGLBLS0Zxu7oMmB7\n" +
            "dnpzsEcoexWzZrQCIQCR6FkAe5ns84x2phRkn6nV7a0anjnxjpJUNeCfc3/pxAB2\n" +
            "AG9Tdqwx8DEZ2JkApFEV/3cVHBHZAsEAKQaNsgiaN9kTAAABa55yLzsAAAQDAEcw\n" +
            "RQIhAKhGKQIpSd59tJm/Yac7Xo05u93CWbnDwoDgSMS+HBs5AiAfOSOc3BzY/2MF\n" +
            "AM4GWrkK5Ehs9JMafo/+VBM0OrwVKQB2AId1v+dZfPiMQ5lfvfNu/1aNR1Y2/0q1\n" +
            "YMG06v9eoIMPAAABa55yL4IAAAQDAEcwRQIhANcF26iGoUuzZL6rGKduPtyyYusf\n" +
            "03lBKSyvxabB9WuvAiBNbxR210L+JP89s/ONw53lYVr+1m/c3u9/9Wpu7c3n5jAN\n" +
            "BgkqhkiG9w0BAQsFAAOCAgEACX2CbVM8MCIJ+2Wsap1v6VU2kpCS/FBIsLSTWNEf\n" +
            "dREv1nh93qQ2CPIxj5kP/0EOUfq7tmQCJHMODVgz3iHrdxRB1E58nXHlZ6vUdrCo\n" +
            "pD9d6Cp+AwvrOdv6MndVJgel9tVOAqAUblwdLzPNQHEcXoKnFEVv2SVQCmAYLlkP\n" +
            "xX2RS73gseiit4QnVZOWi/wDhqMm7/iq8n7rL/f7+ly2+7e3LVjxd24HZkgxNgbn\n" +
            "JDjYvIla+EvyrY8514Ru3Pf1UICY03VpYjE8R7SxrqcvOLtwvOVew6TuCUl6RNpl\n" +
            "xeC9Oa1dgf+QRXN7LvmBXUP2nOCnwJE1ENvThPLw9BXLatVJgkA/v/mYWE5VjzIL\n" +
            "hboPH2fNWemUv5QMzxUkqhgHgrhr8wnhI6xYIYciGDbmmfnItHex7bxktT7axoCD\n" +
            "3dTQQe01YfK/LlkHtnBmJf/t0F33m8KXcQ51fic/TR2U5Tampxp2kdFdTyvRRqMl\n" +
            "igqo3EhiPmB9bKsnXDA2AnvdjZT9uFwbUu5lNxjiMQcSZikjQAjJPgjCZ9BQOGbL\n" +
            "eqgZcw2CxWMxFSTLL3TIBlNL/0GpRlTvr3IGyvHEr7EESXKD+Ar8XW+4VlMc1s8F\n" +
            "cdtnus71s7wm+JUSXcM0WJUkRUvWqHlPi3Ucfe7k6x6BG9Mb42ECjorefPXvFu7v\n" +
            "OT4=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=revoked-rsa-dv.ssl.com
    // Issuer: CN=SSL.com RSA SSL subCA, O=SSL Corporation, L=Houston, ST=Texas, C=US
    // Serial number: 3f527e677d00558272ac90d1620b67f4
    // Valid from: Fri Jun 28 07:13:48 PDT 2019 until: Sun Jun 27 07:13:48 PDT 2021
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHVzCCBT+gAwIBAgIQP1J+Z30AVYJyrJDRYgtn9DANBgkqhkiG9w0BAQsFADBp\n" +
            "MQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxEDAOBgNVBAcMB0hvdXN0b24x\n" +
            "GDAWBgNVBAoMD1NTTCBDb3Jwb3JhdGlvbjEeMBwGA1UEAwwVU1NMLmNvbSBSU0Eg\n" +
            "U1NMIHN1YkNBMB4XDTE5MDYyODE0MTM0OFoXDTIxMDYyNzE0MTM0OFowITEfMB0G\n" +
            "A1UEAwwWcmV2b2tlZC1yc2EtZHYuc3NsLmNvbTCCASIwDQYJKoZIhvcNAQEBBQAD\n" +
            "ggEPADCCAQoCggEBAMKtMVeo+fMoeu1nLrcwxNAdfUysNKEhNZbMUOu9pzCChEvJ\n" +
            "QHUicdrIZYl9m59uKUMh3Dj2nJLZ3a0pP4iWKcOEfCVMtA83/GDJl/BVj3XFbsMl\n" +
            "+HSIu7R0vQM4enOztLabnOzvE4pQOFUp8u5SKO+hmB0zQ1iWkevYjJOf5DBZ7Zsa\n" +
            "uF4qy9JqSF07gj/7FNqmqnfy6Z8yc8WAMjoUJrVrvmHQZeX/bCWxczFhYmAtYlwO\n" +
            "7a914VP79b3Jq60HbLbYBdILnuU1Uu5L/JbG+hm/fH2meY30aWUaKcGY04ej6xuM\n" +
            "hWsLhOrmcl3P7/E5UUojaR1Zvdtsn7jkQ8Y3iOsCAwEAAaOCA0EwggM9MB8GA1Ud\n" +
            "IwQYMBaAFCYUfuDc16b34tQEJ99h8cLs5zLKMHwGCCsGAQUFBwEBBHAwbjBKBggr\n" +
            "BgEFBQcwAoY+aHR0cDovL3d3dy5zc2wuY29tL3JlcG9zaXRvcnkvU1NMY29tLVN1\n" +
            "YkNBLVNTTC1SU0EtNDA5Ni1SMS5jcnQwIAYIKwYBBQUHMAGGFGh0dHA6Ly9vY3Nw\n" +
            "cy5zc2wuY29tMD0GA1UdEQQ2MDSCFnJldm9rZWQtcnNhLWR2LnNzbC5jb22CGnd3\n" +
            "dy5yZXZva2VkLXJzYS1kdi5zc2wuY29tMFEGA1UdIARKMEgwCAYGZ4EMAQIBMDwG\n" +
            "DCsGAQQBgqkwAQMBATAsMCoGCCsGAQUFBwIBFh5odHRwczovL3d3dy5zc2wuY29t\n" +
            "L3JlcG9zaXRvcnkwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMDoGA1Ud\n" +
            "HwQzMDEwL6AtoCuGKWh0dHA6Ly9jcmxzLnNzbC5jb20vU1NMY29tUlNBU1NMc3Vi\n" +
            "Q0EuY3JsMB0GA1UdDgQWBBSTrHG0Sh+8BEp+oP+avIGAtSdyajAOBgNVHQ8BAf8E\n" +
            "BAMCBaAwggF+BgorBgEEAdZ5AgQCBIIBbgSCAWoBaAB2AESUZS6w7s6vxEAH2Kj+\n" +
            "KMDa5oK+2MsxtT/TM5a1toGoAAABa554kQsAAAQDAEcwRQIhAIfU+5HWDnqZdlMN\n" +
            "Z+CEkBE8wBFUWzG0ixSQ5S1Tryt4AiAQevLU7OF3N90zIt2QpwVAIGve5lBElhMH\n" +
            "fRqXTkeZZwB2AG9Tdqwx8DEZ2JkApFEV/3cVHBHZAsEAKQaNsgiaN9kTAAABa554\n" +
            "jJQAAAQDAEcwRQIhAPd8mNiDFHA74Bl16nwOPehQZmiFltzCYDsd0uHv5qCfAiB+\n" +
            "S43G7Yhq62Ofma6wXsag+UEl/tttzfbfASGz1WPBOQB2AKS5CZC0GFgUh7sTosxn\n" +
            "cAo8NZgE+RvfuON3zQ7IDdwQAAABa554kDoAAAQDAEcwRQIgUs8O4gQ34Sp0K4Dn\n" +
            "Wh7FRFJWwZ6cGYvqmKT+UyCeVisCIQDl0AYXsn4ILMafvmJwnXlcduZ3z6P0jwGK\n" +
            "Cjh26ETDFzANBgkqhkiG9w0BAQsFAAOCAgEAAtTlh2YMwe6E0+EWKU3H79NmgLjK\n" +
            "xoR3VtT56ILRt0qJuJ+z1iqq/IxZBe7wnUUWU46SWmBfDEQcGI7Hdomr67QBZNZz\n" +
            "+wvnatMzrCPM7jPsb05Motz99NSk6yzQzR2c030sy1d78mRKJ/4wpidNDHpjuYL9\n" +
            "cBp2gKf2/RxU74+BhugCjLqB1gojGO0CT1/g5a1QMtqRMM0EPrJrrtcEM0zG48yI\n" +
            "P3b57Nl2ZbshRvY9bVi3of2SaPFQgu99/zAlerPUThz4O2CskOgKt77y6KOgCbBp\n" +
            "7fQF6vh/aOm0Xba2Z0CtB+uVN2g4+LwyuovOy+JyjGKv7GxRKEQmGZsRLDVpxOs5\n" +
            "W47K+iuOEhTRWRkStfuk2LcCLwTrgxHv2/Wo+80ME/7wxGKs1IzlkcFtFLhaeN4p\n" +
            "QsmADpcyBfeWmvTdKgaVBOE2F/nenIiKpo+0jcoMAW6JgMD+otn8gofBq+Za1N4X\n" +
            "xckvLWbMDAj4lELBHXu7gLHHLJCL9GGPD5HKjH/RyLtKKaRgT/AV6jl/woKTAzGF\n" +
            "SPqgNQsu+sCdUbO0nDONkXDxhfan8XNrd32KMPGucJySiyjpHkurobMuGbs/LQzd\n" +
            "JLTSTIIIPpEHBk7PHRGPSFewIhi0aDhupgZLU9UGrLRw/xV/KlGqTcGFWBvvOC+I\n" +
            "CSZFRr0hWBv/dfw=\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Fri Jun 28 07:25:02 PDT 2019", System.out);
    }
}

class SSLCA_EV_RSA {

    // Owner: CN=SSL.com EV SSL Intermediate CA RSA R3, O=SSL Corp, L=Houston, ST=Texas, C=US
    // Issuer: CN=SSL.com EV Root Certification Authority RSA R2, O=SSL Corporation, L=Houston, ST=Texas, C=US
    // Serial number: 56b629cd34bc78f6
    // Valid from: Wed May 31 11:14:37 PDT 2017 until: Fri May 30 11:14:37 PDT 2042
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIG4DCCBMigAwIBAgIQA6P00GAwUqM3zjgKiDAxjDANBgkqhkiG9w0BAQsFADCB\n" +
            "gjELMAkGA1UEBhMCVVMxDjAMBgNVBAgMBVRleGFzMRAwDgYDVQQHDAdIb3VzdG9u\n" +
            "MRgwFgYDVQQKDA9TU0wgQ29ycG9yYXRpb24xNzA1BgNVBAMMLlNTTC5jb20gRVYg\n" +
            "Um9vdCBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eSBSU0EgUjIwHhcNMTkwMzI2MTc0\n" +
            "NjUzWhcNMzQwMzIyMTc0NjUzWjByMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4\n" +
            "YXMxEDAOBgNVBAcMB0hvdXN0b24xETAPBgNVBAoMCFNTTCBDb3JwMS4wLAYDVQQD\n" +
            "DCVTU0wuY29tIEVWIFNTTCBJbnRlcm1lZGlhdGUgQ0EgUlNBIFIzMIICIjANBgkq\n" +
            "hkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAkby+CNUTyO0wakMc6VeJxQLGcTtfwJG6\n" +
            "W9MYhMBWW22YUMtfCL7at/ey89eCc0cNy9uekJqitJe78Ion5qHBLfSpahYWttzr\n" +
            "LflXkdlPz6xsZuw7F/tp6oYrcUpRIX92ci0EhORtb5xoX7rwzrBnG2Jv7fPn8JGj\n" +
            "wmvYPS0meVkuKGtdR/s3dkl0tDraq2xti8cN7W9VawzLDL9yNyEw2GWAp3M5Uqex\n" +
            "Yjh9HY5w/4bgk7K0KSw+2njaXCEa2MugM6txHDKjocVFBe7G8JPMKkCcbbrgZo/q\n" +
            "ygTnIY8q7B1XQG2wrdsu4LTo9ijIYmoZHBAKN/XCdPecQYF9cHrv6NjVUcMrNmHT\n" +
            "B43NrIvrXmm3lZJU4PZNUhb7YrDtpN+rV6zSaKAu/EArGDzYv8iHKT2E+wjhwqOC\n" +
            "WnXv1qSa//xvN6RSoDMpj7q7iTxfdrQqRFsr70hyPrUmnoJLrBBg1+IqFTkaNtuk\n" +
            "misP4Bd0zeqkEuxYCmhKcCTM2iS9RMCIot5HI5qeAcVs63WzM+ax0zbHK1F9AIOG\n" +
            "gwrVRrdwXRSXO4TlvamsL6klJMnjSCs7E1l8xeE403nZPp4RGr5ZQFrhfdG9nL7w\n" +
            "66osGX+dGHGZkFjASS3Bw0RCiz4oCJxFGE+FAD7pJaV8GP6XTkaZp9n1ooYzCC48\n" +
            "vq0OtfRS62MCAwEAAaOCAV8wggFbMBIGA1UdEwEB/wQIMAYBAf8CAQAwHwYDVR0j\n" +
            "BBgwFoAU+WC71OPVNPa49QaAJadz20ZpqJ4wfAYIKwYBBQUHAQEEcDBuMEoGCCsG\n" +
            "AQUFBzAChj5odHRwOi8vd3d3LnNzbC5jb20vcmVwb3NpdG9yeS9TU0xjb20tUm9v\n" +
            "dENBLUVWLVJTQS00MDk2LVIyLmNydDAgBggrBgEFBQcwAYYUaHR0cDovL29jc3Bz\n" +
            "LnNzbC5jb20wEQYDVR0gBAowCDAGBgRVHSAAMB0GA1UdJQQWMBQGCCsGAQUFBwMC\n" +
            "BggrBgEFBQcDATBFBgNVHR8EPjA8MDqgOKA2hjRodHRwOi8vY3Jscy5zc2wuY29t\n" +
            "L1NTTGNvbS1Sb290Q0EtRVYtUlNBLTQwOTYtUjIuY3JsMB0GA1UdDgQWBBS/wVqH\n" +
            "/yj6QT39t0/kHa+gYVgpvTAOBgNVHQ8BAf8EBAMCAYYwDQYJKoZIhvcNAQELBQAD\n" +
            "ggIBAAoTAGRea1Lg+Rlvnhj6lHbvhn9mjUlXZuI1b4d4jDDk5X29gNKhW7Rg97Qt\n" +
            "oBoJaLb9gZkJ2MkUbCE1x2jIghjLmmFvaIq+nAZEMtWWEi0ycqQm8rVUHioZ2Mfn\n" +
            "2SoFtQeY+5MFLO9l8IeDaNZ+LV3su8YTsh/453vExhiNhPVEqLyGlkkW0B2gNW8z\n" +
            "bsRy6L5QW0cZ4gZrY86MvHB0Gl299mTJ4jcgic+Oalbz9SZJ+EiW/aUDSpZ2zawi\n" +
            "ackPWmAbk0y0gouOymrwOJZTuq+AJEJ6M+WSVdknwE7YwDpVMszHXS38BS1A5N1i\n" +
            "rzW3BcARHbtCb00vEy2mzW5JPM2LjkzfgJ0lBiyDCE3ZeBeUtKmcdFUFrHwHl3gV\n" +
            "aRipD+xMa1hGOTh33eMzwWoRxvk6o7y73Sy6XBfycN+8LhXUZT0X8STmWtBtLSMp\n" +
            "blWMjuuFyUVQvIj05N7hORY/LhdQhEx8kVwS5RkLVSpRnohdk+nI69yIA7EwZKlw\n" +
            "kKEsDqlVOeDYWVWQANDC55kJ7nOyJbqtGJqImwWXdQcf37fi80cf+mKOYs5vNmkx\n" +
            "D9bwFWsKnP71x0liSlv8z79vRAo8FJwTgXRNO1c0ACf0rXEJy3GRAXRWiTvuGahR\n" +
            "JVM3Jnn0G6o3+vTfwa7CKR/9Jc4t25iRU3xmSgiusg4u8i5x\n" +
            "-----END CERTIFICATE-----";

    // Owner: OID.1.3.6.1.4.1.311.60.2.1.3=US, OID.1.3.6.1.4.1.311.60.2.1.2=Nevada, STREET=3100 Richmond Ave,
    // OID.2.5.4.15=Private Organization, OID.2.5.4.17=77098, CN=test-ev-rsa.ssl.com, SERIALNUMBER=NV20081614243,
    // O=SSL Corp, L=Houston, ST=Texas, C=US
    // Issuer: CN=SSL.com EV SSL Intermediate CA RSA R3, O=SSL Corp, L=Houston, ST=Texas, C=US
    // Serial number: 558089b221d7cd9c7a4bc4a7fd7e2969
    // Valid from: Mon Jul 01 13:28:01 PDT 2019 until: Wed Jun 30 13:28:01 PDT 2021
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIISTCCBjGgAwIBAgIQVYCJsiHXzZx6S8Sn/X4paTANBgkqhkiG9w0BAQsFADBy\n" +
            "MQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxEDAOBgNVBAcMB0hvdXN0b24x\n" +
            "ETAPBgNVBAoMCFNTTCBDb3JwMS4wLAYDVQQDDCVTU0wuY29tIEVWIFNTTCBJbnRl\n" +
            "cm1lZGlhdGUgQ0EgUlNBIFIzMB4XDTE5MDcwMTIwMjgwMVoXDTIxMDYzMDIwMjgw\n" +
            "MVowgfExCzAJBgNVBAYTAlVTMQ4wDAYDVQQIDAVUZXhhczEQMA4GA1UEBwwHSG91\n" +
            "c3RvbjERMA8GA1UECgwIU1NMIENvcnAxFjAUBgNVBAUTDU5WMjAwODE2MTQyNDMx\n" +
            "HDAaBgNVBAMME3Rlc3QtZXYtcnNhLnNzbC5jb20xDjAMBgNVBBEMBTc3MDk4MR0w\n" +
            "GwYDVQQPDBRQcml2YXRlIE9yZ2FuaXphdGlvbjEaMBgGA1UECQwRMzEwMCBSaWNo\n" +
            "bW9uZCBBdmUxFzAVBgsrBgEEAYI3PAIBAgwGTmV2YWRhMRMwEQYLKwYBBAGCNzwC\n" +
            "AQMTAlVTMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsmMOHfREGN48\n" +
            "nlgxYiF0EJsytoM98UAslRRlGHJyZw3SLcPx9u/I82h0KKjLtnY3/o62mCoEZYlc\n" +
            "1UKKEIi3NgByU8yJ0yynm3I0LJHEZqOKoahtzwP787/OtqsSsWeblrTnfxVO7G1J\n" +
            "bPYrPtNuQ9ZnmByyhA+hlTIY48kJh5WtmBeftBSynuKCgpVnkv2y2LKZJc4t6JQX\n" +
            "XO6Geev8LPUd2uPVjatZv0se2YKdixFQQKwWcLJV5LZqjZDhZtPomCN0sp+wle4p\n" +
            "rRTZPSWRB98mI1X+UBTFGFKS9cxzO2NwmVcbgN2WYR+FpWbatoS/RThGC7mKQB7i\n" +
            "5BEQHNZMawIDAQABo4IDWTCCA1UwHwYDVR0jBBgwFoAUv8Fah/8o+kE9/bdP5B2v\n" +
            "oGFYKb0wfwYIKwYBBQUHAQEEczBxME0GCCsGAQUFBzAChkFodHRwOi8vd3d3LnNz\n" +
            "bC5jb20vcmVwb3NpdG9yeS9TU0xjb20tU3ViQ0EtRVYtU1NMLVJTQS00MDk2LVIz\n" +
            "LmNydDAgBggrBgEFBQcwAYYUaHR0cDovL29jc3BzLnNzbC5jb20wNwYDVR0RBDAw\n" +
            "LoITdGVzdC1ldi1yc2Euc3NsLmNvbYIXd3d3LnRlc3QtZXYtcnNhLnNzbC5jb20w\n" +
            "XwYDVR0gBFgwVjAHBgVngQwBATANBgsqhGgBhvZ3AgUBATA8BgwrBgEEAYKpMAED\n" +
            "AQQwLDAqBggrBgEFBQcCARYeaHR0cHM6Ly93d3cuc3NsLmNvbS9yZXBvc2l0b3J5\n" +
            "MB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDATBIBgNVHR8EQTA/MD2gO6A5\n" +
            "hjdodHRwOi8vY3Jscy5zc2wuY29tL1NTTGNvbS1TdWJDQS1FVi1TU0wtUlNBLTQw\n" +
            "OTYtUjMuY3JsMB0GA1UdDgQWBBTIDVTF3DDhdwudatuodPyHe1jcOzAOBgNVHQ8B\n" +
            "Af8EBAMCBaAwggF9BgorBgEEAdZ5AgQCBIIBbQSCAWkBZwB1AG9Tdqwx8DEZ2JkA\n" +
            "pFEV/3cVHBHZAsEAKQaNsgiaN9kTAAABa69CQcUAAAQDAEYwRAIgEYzpfp8v+gG3\n" +
            "S9cgZIuFCKPeoSM85gag8/iBJhNIb9oCIDcq+2Pi8+E3LAVmZfgcMhg30t821LNn\n" +
            "PWATU5+gAmmzAHUAh3W/51l8+IxDmV+9827/Vo1HVjb/SrVgwbTq/16ggw8AAAFr\n" +
            "r0JCCQAABAMARjBEAiAzeyNw/2osk+xktY8VpFTsROj7jRODS2G3G2MDV6ZmMwIg\n" +
            "bwuFbNxSEqUfKhveZJVVLYzZtzXcjkhflaazupumZrkAdwC72d+8H4pxtZOUI5eq\n" +
            "kntHOFeVCqtS6BqQlmQ2jh7RhQAAAWuvQkGUAAAEAwBIMEYCIQCEfoPIKoy0Rv/d\n" +
            "DXOVm0FzKDH2zWHN/oQZ/7gwd21hvAIhAL2gDESf+tcjCkbjdj9NpDa/fVWO9VZD\n" +
            "uPPnAZ6jf2G3MA0GCSqGSIb3DQEBCwUAA4ICAQAcYH/+o9N0E3H9h0GfohGElfRw\n" +
            "XPUnQI3/CZwuG0ShCbpVspvUkuR/P0Hjr9XgDVy39R9SOaEDK3/coG8/Ry56Lrm0\n" +
            "17v+yeEzAVK51eQeinHoCYc9TIwmyrwt36JE/zIwnDB623Y4ccxYN5LZxjVx668/\n" +
            "xj3JffaY5185qPjAqkjLUzj9TeeAJk/ws1YXbQJvO4CZV2QXrishC+dEoqvfOe/u\n" +
            "sMHcMJy+cFrPhe4cC7s9fHeYTpF36yvfWrgjGwDki/9zgRhOvDuM72dIMkrcHkZi\n" +
            "OvZMgyoXz/Nw3D514K9BSt6xRB2qGzI8fx0EOGzEEjX1Zdie2uVDy9aC8k8TjQAM\n" +
            "v/YT7Bggpv300hWvBGw0QT8l7Nk1PZFBagAhqRCKRsR1pUZ8CyZzwNkNyUSYV4Or\n" +
            "n0vYwVEgpMeSMu/ObWwWPM7QKSNcSSIV5lxmsZX+wS76OpDMHm27P94RTEePF4sG\n" +
            "QmvY6hgHSlREJUL0vyGGY2Rbm3cL3zaM4qTquN18v61uUVKakELYIcRZwVTyBj5M\n" +
            "KxOkjGXnLYpDOLFHD4WB1q7J+SorG43V+nbmTEN5fshGUjjWoz5ykfErnyJa1+Py\n" +
            "FXWoPFb425DelhuDe94btROuJELRfzhqDXoKrhDgSQGV2qM3sk6uIPOaoH4N31ko\n" +
            "C41bezSdJ5r4mif8iA==\n" +
            "-----END CERTIFICATE-----";

    // Owner: OID.1.3.6.1.4.1.311.60.2.1.3=US, OID.1.3.6.1.4.1.311.60.2.1.2=Nevada, STREET=3100 Richmond Ave,
    // OID.2.5.4.15=Private Organization, OID.2.5.4.17=77098, CN=revoked-rsa-ev.ssl.com, SERIALNUMBER=NV20081614243,
    // O=SSL Corp, L=Houston, ST=Texas, C=US
    // Issuer: CN=SSL.com EV SSL Intermediate CA RSA R3, O=SSL Corp, L=Houston, ST=Texas, C=US
    // Serial number: 1ea7f53492bded2d425135bdf525889f
    // Valid from: Mon Jul 01 13:29:02 PDT 2019 until: Wed Jun 30 13:29:02 PDT 2021
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIIUzCCBjugAwIBAgIQHqf1NJK97S1CUTW99SWInzANBgkqhkiG9w0BAQsFADBy\n" +
            "MQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxEDAOBgNVBAcMB0hvdXN0b24x\n" +
            "ETAPBgNVBAoMCFNTTCBDb3JwMS4wLAYDVQQDDCVTU0wuY29tIEVWIFNTTCBJbnRl\n" +
            "cm1lZGlhdGUgQ0EgUlNBIFIzMB4XDTE5MDcwMTIwMjkwMloXDTIxMDYzMDIwMjkw\n" +
            "MlowgfQxCzAJBgNVBAYTAlVTMQ4wDAYDVQQIDAVUZXhhczEQMA4GA1UEBwwHSG91\n" +
            "c3RvbjERMA8GA1UECgwIU1NMIENvcnAxFjAUBgNVBAUTDU5WMjAwODE2MTQyNDMx\n" +
            "HzAdBgNVBAMMFnJldm9rZWQtcnNhLWV2LnNzbC5jb20xDjAMBgNVBBEMBTc3MDk4\n" +
            "MR0wGwYDVQQPDBRQcml2YXRlIE9yZ2FuaXphdGlvbjEaMBgGA1UECQwRMzEwMCBS\n" +
            "aWNobW9uZCBBdmUxFzAVBgsrBgEEAYI3PAIBAgwGTmV2YWRhMRMwEQYLKwYBBAGC\n" +
            "NzwCAQMTAlVTMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlqZwW2n7\n" +
            "Ot8ujRGyCkzf/FqkiIi6+mq7QXlBMsZVNmLcWzatoS9K8WOviU+lmYpdV3rkrX1v\n" +
            "e/FZBwtBR/x1FRN3CPoGcO0Yu6CZjknHtyyNQ36mwUy7UW+rQKYjDfU4aXme4bP8\n" +
            "Dk2rUYQtM/xpYHKDk9x7Vg4zAmk+L0LQmSU0103DRuANnxOszEK196UbLE4W+2+i\n" +
            "Xat40jHW3KU2PxVfCajgB1mdrDt2b5j/qDAL+Wo2DzCtE62UPJvI6UyEqJ24jinS\n" +
            "A4l4NgkMPDMWNU5QIkV/EhQvZMUKCvNUv+Gsq8pcOeDXxKpBIe/KoQSMH18mym1U\n" +
            "vIaTjAzDDsWjqwIDAQABo4IDYDCCA1wwHwYDVR0jBBgwFoAUv8Fah/8o+kE9/bdP\n" +
            "5B2voGFYKb0wfwYIKwYBBQUHAQEEczBxME0GCCsGAQUFBzAChkFodHRwOi8vd3d3\n" +
            "LnNzbC5jb20vcmVwb3NpdG9yeS9TU0xjb20tU3ViQ0EtRVYtU1NMLVJTQS00MDk2\n" +
            "LVIzLmNydDAgBggrBgEFBQcwAYYUaHR0cDovL29jc3BzLnNzbC5jb20wPQYDVR0R\n" +
            "BDYwNIIWcmV2b2tlZC1yc2EtZXYuc3NsLmNvbYIad3d3LnJldm9rZWQtcnNhLWV2\n" +
            "LnNzbC5jb20wXwYDVR0gBFgwVjAHBgVngQwBATANBgsqhGgBhvZ3AgUBATA8Bgwr\n" +
            "BgEEAYKpMAEDAQQwLDAqBggrBgEFBQcCARYeaHR0cHM6Ly93d3cuc3NsLmNvbS9y\n" +
            "ZXBvc2l0b3J5MB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDATBIBgNVHR8E\n" +
            "QTA/MD2gO6A5hjdodHRwOi8vY3Jscy5zc2wuY29tL1NTTGNvbS1TdWJDQS1FVi1T\n" +
            "U0wtUlNBLTQwOTYtUjMuY3JsMB0GA1UdDgQWBBQnclOL04VraXmRZEkhwgMbajmy\n" +
            "YTAOBgNVHQ8BAf8EBAMCBaAwggF+BgorBgEEAdZ5AgQCBIIBbgSCAWoBaAB3AG9T\n" +
            "dqwx8DEZ2JkApFEV/3cVHBHZAsEAKQaNsgiaN9kTAAABa69DLjEAAAQDAEgwRgIh\n" +
            "AMd3B9Gt/hpTCZ+2xsOTTKBaDjh+EsMcKuwZkEpO6UN0AiEA8yiZ9ZIrCOUxsdQp\n" +
            "FJi+MtsNQxvgu8igdv+l34jHZA0AdgCHdb/nWXz4jEOZX73zbv9WjUdWNv9KtWDB\n" +
            "tOr/XqCDDwAAAWuvQy52AAAEAwBHMEUCIQCFPALMZd6xk4NgYuTXoJGo/FRX0Wub\n" +
            "VWSgTZQwld5fTQIgDDp8vajs+7R7XyKOv41xP26NQ3zR4EegwOGeb0paiIIAdQC7\n" +
            "2d+8H4pxtZOUI5eqkntHOFeVCqtS6BqQlmQ2jh7RhQAAAWuvQy4MAAAEAwBGMEQC\n" +
            "IGFiEQ8fMrjm1bV/mbT35bvJWf4mUbb92/NkHkQvHcaQAiBcS4CclZmzQLj4w6CV\n" +
            "JsLf1P6+OhCDtvxWZdndGwJRczANBgkqhkiG9w0BAQsFAAOCAgEAFwE/RMAk871D\n" +
            "acLlB0Jb29+WBmCgIu1pA+bh5/lMxn5KoPxkbHPFVHlfenDgZHUNU6DKH4HdCUG7\n" +
            "GSAyajLiYRkcrDtBfp5MtNUAqnOJbh2NWiJ3FgSdAjfeSXPhhGfQ3U+0YCWarBfO\n" +
            "xZ49eyhTzhHMoW+caJV3jC442Ebzh2X243MwcxqIkjgzWs6duiHnpHfT9gZBl3ou\n" +
            "eu85LVFwzxNdrrAx1yG9PA05wCsYYlzwx7fC8ycfbvs+2ORIztiEScyr9VCg5sho\n" +
            "YGuBFuP38sWRwiV5K7+EqpGjY+4R3BLWol7lzWsqWJC1J4zkd6Df5reSGBt0wlbx\n" +
            "7MdUTXzHMtP8NDIYpdMBrPbkzOKIDzO6bDMsBWWFz7rWCmxUI6sSf0yknPtmBgCd\n" +
            "rJAq25V/DqSRGrkaY4Dx1CPGtwYN34fCDLxKeN69rG5mkR2w7HRR5eMXek6oi3Pr\n" +
            "hQrKt5NgrYjO6HJ6ABI5xoDM9doXy9BYbz5RX43RTU399aIqyXZh0d3W0rr7wggt\n" +
            "+PFRU1OJqhpPQgKsB5zFT3G2HgVBD0hawHS+0Hu+CHpngiDziH+eyvTk3tdhIq2x\n" +
            "oDZXs7SSZK6hf/im+7OFSkROy6CwhAn3nxRI9lpag1tTgF4kVSctBv+301ev0twX\n" +
            "0w6RymKcvEbcuSDHkzOYWxc1cqwOxjA=\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Mon Jul 01 20:53:13 PDT 2019", System.out);
    }
}

class SSLCA_ECC {

    // Owner: CN=SSL.com SSL Intermediate CA ECC R2, O=SSL Corp, L=Houston, ST=Texas, C=US
    // Issuer: CN=SSL.com Root Certification Authority ECC, O=SSL Corporation, L=Houston, ST=Texas, C=US
    // Serial number: 75e6dfcbc1685ba8
    // Valid from: Fri Feb 12 10:14:03 PST 2016 until: Tue Feb 12 10:14:03 PST 2041
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDejCCAv+gAwIBAgIQHNcSEt4VENkSgtozEEoQLzAKBggqhkjOPQQDAzB8MQsw\n" +
            "CQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxEDAOBgNVBAcMB0hvdXN0b24xGDAW\n" +
            "BgNVBAoMD1NTTCBDb3Jwb3JhdGlvbjExMC8GA1UEAwwoU1NMLmNvbSBSb290IENl\n" +
            "cnRpZmljYXRpb24gQXV0aG9yaXR5IEVDQzAeFw0xOTAzMDcxOTQyNDJaFw0zNDAz\n" +
            "MDMxOTQyNDJaMG8xCzAJBgNVBAYTAlVTMQ4wDAYDVQQIDAVUZXhhczEQMA4GA1UE\n" +
            "BwwHSG91c3RvbjERMA8GA1UECgwIU1NMIENvcnAxKzApBgNVBAMMIlNTTC5jb20g\n" +
            "U1NMIEludGVybWVkaWF0ZSBDQSBFQ0MgUjIwdjAQBgcqhkjOPQIBBgUrgQQAIgNi\n" +
            "AASEOWn30uEYKDLFu4sCjFQ1VupFaeMtQjqVWyWSA7+KFljnsVaFQ2hgs4cQk1f/\n" +
            "RQ2INSwdVCYU0i5qsbom20rigUhDh9dM/r6bEZ75eFE899kSCI14xqThYVLPdLEl\n" +
            "+dyjggFRMIIBTTASBgNVHRMBAf8ECDAGAQH/AgEAMB8GA1UdIwQYMBaAFILRhXMw\n" +
            "5zUE044CkvvlpNHEIejNMHgGCCsGAQUFBwEBBGwwajBGBggrBgEFBQcwAoY6aHR0\n" +
            "cDovL3d3dy5zc2wuY29tL3JlcG9zaXRvcnkvU1NMY29tLVJvb3RDQS1FQ0MtMzg0\n" +
            "LVIxLmNydDAgBggrBgEFBQcwAYYUaHR0cDovL29jc3BzLnNzbC5jb20wEQYDVR0g\n" +
            "BAowCDAGBgRVHSAAMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEFBQcDATA7BgNV\n" +
            "HR8ENDAyMDCgLqAshipodHRwOi8vY3Jscy5zc2wuY29tL3NzbC5jb20tZWNjLVJv\n" +
            "b3RDQS5jcmwwHQYDVR0OBBYEFA10Zgpen+Is7NXCXSUEf3Uyuv99MA4GA1UdDwEB\n" +
            "/wQEAwIBhjAKBggqhkjOPQQDAwNpADBmAjEAxYt6Ylk/N8Fch/3fgKYKwI5A011Q\n" +
            "MKW0h3F9JW/NX/F7oYtWrxljheH8n2BrkDybAjEAlCxkLE0vQTYcFzrR24oogyw6\n" +
            "VkgTm92+jiqJTO5SSA9QUa092S5cTKiHkH2cOM6m\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=test-dv-ecc.ssl.com
    // Issuer: CN=SSL.com SSL Intermediate CA ECC R2, O=SSL Corp, L=Houston, ST=Texas, C=US
    // Serial number: 1bfbd8e4bea894f3d1887c50e7d366d7
    // Valid from: Fri Jun 28 06:58:27 PDT 2019 until: Sun Jun 27 06:58:27 PDT 2021
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIE9TCCBHqgAwIBAgIQG/vY5L6olPPRiHxQ59Nm1zAKBggqhkjOPQQDAzBvMQsw\n" +
            "CQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxEDAOBgNVBAcMB0hvdXN0b24xETAP\n" +
            "BgNVBAoMCFNTTCBDb3JwMSswKQYDVQQDDCJTU0wuY29tIFNTTCBJbnRlcm1lZGlh\n" +
            "dGUgQ0EgRUNDIFIyMB4XDTE5MDYyODEzNTgyN1oXDTIxMDYyNzEzNTgyN1owHjEc\n" +
            "MBoGA1UEAwwTdGVzdC1kdi1lY2Muc3NsLmNvbTBZMBMGByqGSM49AgEGCCqGSM49\n" +
            "AwEHA0IABJ5u0b8BID+8+TKxn+os0rdwvWB7mUJ4lcCthTADMhnr1VUWBbmBEelB\n" +
            "666WbvbVXooPMUbhE5JvhXCTDyI7RRmjggNHMIIDQzAfBgNVHSMEGDAWgBQNdGYK\n" +
            "Xp/iLOzVwl0lBH91Mrr/fTB7BggrBgEFBQcBAQRvMG0wSQYIKwYBBQUHMAKGPWh0\n" +
            "dHA6Ly93d3cuc3NsLmNvbS9yZXBvc2l0b3J5L1NTTGNvbS1TdWJDQS1TU0wtRUND\n" +
            "LTM4NC1SMi5jcnQwIAYIKwYBBQUHMAGGFGh0dHA6Ly9vY3Nwcy5zc2wuY29tMDcG\n" +
            "A1UdEQQwMC6CE3Rlc3QtZHYtZWNjLnNzbC5jb22CF3d3dy50ZXN0LWR2LWVjYy5z\n" +
            "c2wuY29tMFEGA1UdIARKMEgwCAYGZ4EMAQIBMDwGDCsGAQQBgqkwAQMBATAsMCoG\n" +
            "CCsGAQUFBwIBFh5odHRwczovL3d3dy5zc2wuY29tL3JlcG9zaXRvcnkwHQYDVR0l\n" +
            "BBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMEQGA1UdHwQ9MDswOaA3oDWGM2h0dHA6\n" +
            "Ly9jcmxzLnNzbC5jb20vU1NMY29tLVN1YkNBLVNTTC1FQ0MtMzg0LVIyLmNybDAd\n" +
            "BgNVHQ4EFgQUGCTbprTbVmmNOgJjUgiHonbu8b8wDgYDVR0PAQH/BAQDAgeAMIIB\n" +
            "gQYKKwYBBAHWeQIEAgSCAXEEggFtAWsAdwCHdb/nWXz4jEOZX73zbv9WjUdWNv9K\n" +
            "tWDBtOr/XqCDDwAAAWueaoEnAAAEAwBIMEYCIQCdy3N9w0pem1XShE/rkVSpHxQb\n" +
            "8QdUu3E6R+oncxOGXgIhAJoWg2gJYc9DWDl5ImnrqsmVS6OPgSQRvDsjRIN9gH7a\n" +
            "AHcAu9nfvB+KcbWTlCOXqpJ7RzhXlQqrUugakJZkNo4e0YUAAAFrnmqArQAABAMA\n" +
            "SDBGAiEAs2yfi9e1h6dTQbe4WPd7+5qf7kvP7Vr2k0nAtBS1IgECIQCQYL9he9J4\n" +
            "Bh5cpQezTVPgLAOGcf5xIcCrBs1QJe66/AB3AFWB1MIWkDYBSuoLm1c8U/DA5Dh4\n" +
            "cCUIFy+jqh0HE9MMAAABa55qgaEAAAQDAEgwRgIhAI/27txsvzpbBXkMICi/UOzE\n" +
            "t8uZidbF9KSwmGRPT/6gAiEAhm/VeWHDeWK8gFMU+f0/x4jK7UbzySGBvPzbPpNd\n" +
            "EDwwCgYIKoZIzj0EAwMDaQAwZgIxAJKn8Hr68Z/2rA+VHfZo8eeIFaZ3nvSvQO92\n" +
            "1Byl6cPAm8DsdCnYT16uNSL8Zb5IQAIxAOFLsqPDCSAYkpgutAnVgwI+c549SIRU\n" +
            "k8ol+wUx6zgMmt8VHYagyj6IO0GRDjm/eA==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=revoked-ecc-dv.ssl.com
    // Issuer: CN=SSL.com SSL Intermediate CA ECC R2, O=SSL Corp, L=Houston, ST=Texas, C=US
    // Serial number: 423c2b57dfa379d0c45ffceb6284ed99
    // Valid from: Fri Jun 28 07:09:30 PDT 2019 until: Sun Jun 27 07:09:30 PDT 2021
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIE+TCCBH+gAwIBAgIQQjwrV9+jedDEX/zrYoTtmTAKBggqhkjOPQQDAzBvMQsw\n" +
            "CQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxEDAOBgNVBAcMB0hvdXN0b24xETAP\n" +
            "BgNVBAoMCFNTTCBDb3JwMSswKQYDVQQDDCJTU0wuY29tIFNTTCBJbnRlcm1lZGlh\n" +
            "dGUgQ0EgRUNDIFIyMB4XDTE5MDYyODE0MDkzMFoXDTIxMDYyNzE0MDkzMFowITEf\n" +
            "MB0GA1UEAwwWcmV2b2tlZC1lY2MtZHYuc3NsLmNvbTBZMBMGByqGSM49AgEGCCqG\n" +
            "SM49AwEHA0IABH4nWtnAwPIdcQOSNI72IJJ/I1ZL2XQUAfa3ox5taFQQAalng6N9\n" +
            "Od9t9de1vIMDzUvs5sMWw4YrqAlywFKMraajggNJMIIDRTAfBgNVHSMEGDAWgBQN\n" +
            "dGYKXp/iLOzVwl0lBH91Mrr/fTB7BggrBgEFBQcBAQRvMG0wSQYIKwYBBQUHMAKG\n" +
            "PWh0dHA6Ly93d3cuc3NsLmNvbS9yZXBvc2l0b3J5L1NTTGNvbS1TdWJDQS1TU0wt\n" +
            "RUNDLTM4NC1SMi5jcnQwIAYIKwYBBQUHMAGGFGh0dHA6Ly9vY3Nwcy5zc2wuY29t\n" +
            "MD0GA1UdEQQ2MDSCFnJldm9rZWQtZWNjLWR2LnNzbC5jb22CGnd3dy5yZXZva2Vk\n" +
            "LWVjYy1kdi5zc2wuY29tMFEGA1UdIARKMEgwCAYGZ4EMAQIBMDwGDCsGAQQBgqkw\n" +
            "AQMBATAsMCoGCCsGAQUFBwIBFh5odHRwczovL3d3dy5zc2wuY29tL3JlcG9zaXRv\n" +
            "cnkwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMEQGA1UdHwQ9MDswOaA3\n" +
            "oDWGM2h0dHA6Ly9jcmxzLnNzbC5jb20vU1NMY29tLVN1YkNBLVNTTC1FQ0MtMzg0\n" +
            "LVIyLmNybDAdBgNVHQ4EFgQUY7q+xN9nV1nPQ/dJ5rUC8OKgaoMwDgYDVR0PAQH/\n" +
            "BAQDAgeAMIIBfQYKKwYBBAHWeQIEAgSCAW0EggFpAWcAdQBElGUusO7Or8RAB9io\n" +
            "/ijA2uaCvtjLMbU/0zOWtbaBqAAAAWuedJ/tAAAEAwBGMEQCIGPBF546Tn/lzB22\n" +
            "ICpFLOWOIyIOPwL9S4ikS8Vt1aFTAiBe8mp/WCJnV7WxMIVWEUSLVOYn7erwyu6D\n" +
            "hWNIST4W8wB2AG9Tdqwx8DEZ2JkApFEV/3cVHBHZAsEAKQaNsgiaN9kTAAABa550\n" +
            "oQEAAAQDAEcwRQIhAJ3nwLI7kLP2SKicFKuJoqRYKE/FR2Ff65WL+iWxm/6nAiAJ\n" +
            "cd9EKnBETwM9qQfKoSSs2oTQL4QjSKJZi/sPfKQaagB2ALvZ37wfinG1k5Qjl6qS\n" +
            "e0c4V5UKq1LoGpCWZDaOHtGFAAABa550oH4AAAQDAEcwRQIhAIo6k5BMSFN3FnD4\n" +
            "UFbyJJG/Bujh+OFTYzVM8vuIBoU0AiAhBe+air4wHvd68ykK6xOPv9Qshje9F6LC\n" +
            "gxTqbMOEkDAKBggqhkjOPQQDAwNoADBlAjEAyayBtbcCQB0fE+cCc7OHLuNvb9tl\n" +
            "uiHWy/Ika6IA72WJLLmED971ik08OMa2mGt4AjAklxdElQ5Z/nSeJ2CNEwD7pcYz\n" +
            "468kkrMoGU2lk3QmwcXZscPIoh4Pwew6QteY4J0=\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Fri Jun 28 07:59:20 PDT 2019", System.out);
    }
}
