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
 * @bug 8189131 8231887
 * @summary Interoperability tests with Comodo RSA, ECC, userTrust RSA, and
 *          userTrust ECC CAs
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath ComodoCA OCSP
 * @run main/othervm -Djava.security.debug=certpath ComodoCA CRL
 */

 /*
 * Obtain TLS test artifacts for Comodo CAs from:
 *
 * Valid TLS Certificates:
 * https://comodorsacertificationauthority-ev.comodoca.com
 * https://comodoecccertificationauthority-ev.comodoca.com
 * https://usertrustrsacertificationauthority-ev.comodoca.com
 * https://usertrustecccertificationauthority-ev.comodoca.com
 *
 * Revoked TLS Certificates:
 * https://comodorsacertificationauthority-ev.comodoca.com:444
 * https://comodoecccertificationauthority-ev.comodoca.com:444
 * https://usertrustrsacertificationauthority-ev.comodoca.com:444
 * https://usertrustecccertificationauthority-ev.comodoca.com:444
 */
public class ComodoCA {

    public static void main(String[] args) throws Exception {

        ValidatePathWithParams pathValidator = new ValidatePathWithParams(null);

        if (args.length >= 1 && "CRL".equalsIgnoreCase(args[0])) {
            pathValidator.enableCRLCheck();
        } else {
            // OCSP check by default
            pathValidator.enableOCSPCheck();
        }

        new ComodoRSA().runTest(pathValidator);
        new ComodoECC().runTest(pathValidator);
        new ComodoUserTrustRSA().runTest(pathValidator);
        new ComodoUserTrustECC().runTest(pathValidator);
    }
}

class ComodoRSA {

    // Owner: CN=COMODO RSA Extended Validation Secure Server CA,
    // O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB
    // Issuer: CN=COMODO RSA Certification Authority, O=COMODO CA Limited,
    // L=Salford, ST=Greater Manchester, C=GB
    // Serial number: 6a74380d4ebfed435b5a3f7e16abdd8
    // Valid from: Sat Feb 11 16:00:00 PST 2012 until: Thu Feb 11 15:59:59 PST 2027
    private static final String INT = "-----BEGIN CERTIFICATE-----\n"
            + "MIIGDjCCA/agAwIBAgIQBqdDgNTr/tQ1taP34Wq92DANBgkqhkiG9w0BAQwFADCB\n"
            + "hTELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n"
            + "A1UEBxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxKzApBgNV\n"
            + "BAMTIkNPTU9ETyBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMTIwMjEy\n"
            + "MDAwMDAwWhcNMjcwMjExMjM1OTU5WjCBkjELMAkGA1UEBhMCR0IxGzAZBgNVBAgT\n"
            + "EkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UEBxMHU2FsZm9yZDEaMBgGA1UEChMR\n"
            + "Q09NT0RPIENBIExpbWl0ZWQxODA2BgNVBAMTL0NPTU9ETyBSU0EgRXh0ZW5kZWQg\n"
            + "VmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENBMIIBIjANBgkqhkiG9w0BAQEFAAOC\n"
            + "AQ8AMIIBCgKCAQEAlVbeVLTf1QJJe9FbXKKyHo+cK2JMK40SKPMalaPGEP0p3uGf\n"
            + "CzhAk9HvbpUQ/OGQF3cs7nU+e2PsYZJuTzurgElr3wDqAwB/L3XVKC/sVmePgIOj\n"
            + "vdwDmZOLlJFWW6G4ajo/Br0OksxgnP214J9mMF/b5pTwlWqvyIqvgNnmiDkBfBzA\n"
            + "xSr3e5Wg8narbZtyOTDr0VdVAZ1YEZ18bYSPSeidCfw8/QpKdhQhXBZzQCMZdMO6\n"
            + "WAqmli7eNuWf0MLw4eDBYuPCGEUZUaoXHugjddTI0JYT/8ck0YwLJ66eetw6YWNg\n"
            + "iJctXQUL5Tvrrs46R3N2qPos3cCHF+msMJn4HwIDAQABo4IBaTCCAWUwHwYDVR0j\n"
            + "BBgwFoAUu69+Aj36pvE8hI6t7jiY7NkyMtQwHQYDVR0OBBYEFDna/8ooFIqodBMI\n"
            + "ueQOqdL6fp1pMA4GA1UdDwEB/wQEAwIBBjASBgNVHRMBAf8ECDAGAQH/AgEAMD4G\n"
            + "A1UdIAQ3MDUwMwYEVR0gADArMCkGCCsGAQUFBwIBFh1odHRwczovL3NlY3VyZS5j\n"
            + "b21vZG8uY29tL0NQUzBMBgNVHR8ERTBDMEGgP6A9hjtodHRwOi8vY3JsLmNvbW9k\n"
            + "b2NhLmNvbS9DT01PRE9SU0FDZXJ0aWZpY2F0aW9uQXV0aG9yaXR5LmNybDBxBggr\n"
            + "BgEFBQcBAQRlMGMwOwYIKwYBBQUHMAKGL2h0dHA6Ly9jcnQuY29tb2RvY2EuY29t\n"
            + "L0NPTU9ET1JTQUFkZFRydXN0Q0EuY3J0MCQGCCsGAQUFBzABhhhodHRwOi8vb2Nz\n"
            + "cC5jb21vZG9jYS5jb20wDQYJKoZIhvcNAQEMBQADggIBAERCnUFRK0iIXZebeV4R\n"
            + "AUpSGXtBLMeJPNBy3IX6WK/VJeQT+FhlZ58N/1eLqYVeyqZLsKeyLeCMIs37/3mk\n"
            + "jCuN/gI9JN6pXV/kD0fQ22YlPodHDK4ixVAihNftSlka9pOlk7DgG4HyVsTIEFPk\n"
            + "1Hax0VtpS3ey4E/EhOfUoFDuPPpE/NBXueEoU/1Tzdy5H3pAvTA/2GzS8+cHnx8i\n"
            + "teoiccsq8FZ8/qyo0QYPFBRSTP5kKwxpKrgNUG4+BAe/eiCL+O5lCeHHSQgyPQ0o\n"
            + "fkkdt0rvAucNgBfIXOBhYsvss2B5JdoaZXOcOBCgJjqwyBZ9kzEi7nQLiMBciUEA\n"
            + "KKlHMd99SUWa9eanRRrSjhMQ34Ovmw2tfn6dNVA0BM7pINae253UqNpktNEvWS5e\n"
            + "ojZh1CSggjMziqHRbO9haKPl0latxf1eYusVqHQSTC8xjOnB3xBLAer2VBvNfzu9\n"
            + "XJ/B288ByvK6YBIhMe2pZLiySVgXbVrXzYxtvp5/4gJYp9vDLVj2dAZqmvZh+fYA\n"
            + "tmnYOosxWd2R5nwnI4fdAw+PKowegwFOAWEMUnNt/AiiuSpm5HZNMaBWm9lTjaK2\n"
            + "jwLI5jqmBNFI+8NKAnb9L9K8E7bobTQk+p0pisehKxTxlgBzuRPpwLk6R1YCcYAn\n"
            + "pLwltum95OmYdBbxN4SBB7SC\n"
            + "-----END CERTIFICATE-----";

    // Owner: CN=comodorsacertificationauthority-ev.comodoca.com,
    // O=Sectigo Limited, STREET="3rd Floor, 26 Office Village", STREET=Exchange Quay, STREET=Trafford Road,
    // L=Salford, ST=Manchester, OID.2.5.4.17=M5 3EQ, C=GB, OID.2.5.4.15=Private Organization,
    // OID.1.3.6.1.4.1.311.60.2.1.3=GB, SERIALNUMBER=04058690
    // Issuer: CN=COMODO RSA Extended Validation Secure Server CA, O=COMODO CA Limited, L=Salford,
    // ST=Greater Manchester, C=GB
    // Serial number: 9eb246629030e0b527ca2f93e5ebf25a
    // Valid from: Mon Mar 01 16:00:00 PST 2021 until: Sat Apr 02 16:59:59 PDT 2022
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHSzCCBjOgAwIBAgIRAJ6yRmKQMOC1J8ovk+Xr8lowDQYJKoZIhvcNAQELBQAw\n" +
            "gZIxCzAJBgNVBAYTAkdCMRswGQYDVQQIExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAO\n" +
            "BgNVBAcTB1NhbGZvcmQxGjAYBgNVBAoTEUNPTU9ETyBDQSBMaW1pdGVkMTgwNgYD\n" +
            "VQQDEy9DT01PRE8gUlNBIEV4dGVuZGVkIFZhbGlkYXRpb24gU2VjdXJlIFNlcnZl\n" +
            "ciBDQTAeFw0yMTAzMDIwMDAwMDBaFw0yMjA0MDIyMzU5NTlaMIIBNzERMA8GA1UE\n" +
            "BRMIMDQwNTg2OTAxEzARBgsrBgEEAYI3PAIBAxMCR0IxHTAbBgNVBA8TFFByaXZh\n" +
            "dGUgT3JnYW5pemF0aW9uMQswCQYDVQQGEwJHQjEPMA0GA1UEERMGTTUgM0VRMRMw\n" +
            "EQYDVQQIEwpNYW5jaGVzdGVyMRAwDgYDVQQHEwdTYWxmb3JkMRYwFAYDVQQJEw1U\n" +
            "cmFmZm9yZCBSb2FkMRYwFAYDVQQJEw1FeGNoYW5nZSBRdWF5MSUwIwYDVQQJExwz\n" +
            "cmQgRmxvb3IsIDI2IE9mZmljZSBWaWxsYWdlMRgwFgYDVQQKEw9TZWN0aWdvIExp\n" +
            "bWl0ZWQxODA2BgNVBAMTL2NvbW9kb3JzYWNlcnRpZmljYXRpb25hdXRob3JpdHkt\n" +
            "ZXYuY29tb2RvY2EuY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n" +
            "0P95lAFOOkEOy614rCX7OlOK0Xy0nPAcCFxAcLYBosX8YmXWuePHg596UyEqE3U5\n" +
            "30pTqiccY53bDiYPgSJgr1OlfC7BPLN+QKaeSrFmNgrcoAk3TXejgv7zLXOwZVS6\n" +
            "Wk38Z8xrFNvhd2Z5J6RM/3U+HDfF7OKMGrexr77Ws7lEFpPUgd4eEe+IL1Y2sbwI\n" +
            "iD+PkzIL2LjctkeJFcsRHUvNP8wIhGyIbkARuJhdXkE13lKKIe0EnWrRkkf4DEvY\n" +
            "RFpPjVUKmluhnBOGYkYaiTL0VaOnrPxToSfHR8Awkhk0TNbosAkUo8TKcRTTTiMU\n" +
            "UIS6Y9SqoILiiDG6WmFjzQIDAQABo4IC8jCCAu4wHwYDVR0jBBgwFoAUOdr/yigU\n" +
            "iqh0Ewi55A6p0vp+nWkwHQYDVR0OBBYEFD5LhmEivA6h4az0EFPi5erz1TH+MA4G\n" +
            "A1UdDwEB/wQEAwIFoDAMBgNVHRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMB\n" +
            "BggrBgEFBQcDAjBJBgNVHSAEQjBAMDUGDCsGAQQBsjEBAgEFATAlMCMGCCsGAQUF\n" +
            "BwIBFhdodHRwczovL3NlY3RpZ28uY29tL0NQUzAHBgVngQwBATBWBgNVHR8ETzBN\n" +
            "MEugSaBHhkVodHRwOi8vY3JsLmNvbW9kb2NhLmNvbS9DT01PRE9SU0FFeHRlbmRl\n" +
            "ZFZhbGlkYXRpb25TZWN1cmVTZXJ2ZXJDQS5jcmwwgYcGCCsGAQUFBwEBBHsweTBR\n" +
            "BggrBgEFBQcwAoZFaHR0cDovL2NydC5jb21vZG9jYS5jb20vQ09NT0RPUlNBRXh0\n" +
            "ZW5kZWRWYWxpZGF0aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCQGCCsGAQUFBzABhhho\n" +
            "dHRwOi8vb2NzcC5jb21vZG9jYS5jb20wOgYDVR0RBDMwMYIvY29tb2RvcnNhY2Vy\n" +
            "dGlmaWNhdGlvbmF1dGhvcml0eS1ldi5jb21vZG9jYS5jb20wggEEBgorBgEEAdZ5\n" +
            "AgQCBIH1BIHyAPAAdgBGpVXrdfqRIDC1oolp9PN9ESxBdL79SbiFq/L8cP5tRwAA\n" +
            "AXfyqEfyAAAEAwBHMEUCIQDJbHPgbqK21/Nugwl5mgMO81YQSHOm4VcQ8UvOJjnN\n" +
            "JQIgWw9fortwJBtv2Mts6xJYr5D6itPpEYP8uegURneBwRsAdgBvU3asMfAxGdiZ\n" +
            "AKRRFf93FRwR2QLBACkGjbIImjfZEwAAAXfyqEjyAAAEAwBHMEUCIDifAsuw37D4\n" +
            "beHZ9Ed5/Pab0Eg6Cobrh4jv3bjfA6KIAiEAmiA/XD+AccfI85c+C2zH9wNIs+Zm\n" +
            "/V/uo/sv0i9eCAYwDQYJKoZIhvcNAQELBQADggEBADRFnOFgb3mzCUpXxiU5/mM5\n" +
            "ECRj3NzXKXjcYlSMhVcWA7Eqa5rhJuh11vbPoDQzQcGxntS/zhRwJFRF3hnyFa3m\n" +
            "4t+7ZnUaJN+GOMTABh4kYiOSpE9id12URdJzWv2IHg4CU3OLnsBHGh7H9eWfbPvn\n" +
            "OW4owV1ChpiEHh40i/NQkTn9JzjlZepI9+EsSdhn2tpis7tko6PX/plgw8bRgm7f\n" +
            "ong2QaX/DE6z4VIdomW8TQhB9turhKxwjzPTbtYDQIgZfRP/H1S5jYutqbE5yL5B\n" +
            "r+VOiSuB8234P4xWg1IBL2EFbxPdgOSMTWRJutUcj44kJKmwp5GUQtySSccw4gk=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=comodorsacertificationauthority-ev.comodoca.com, OU=COMODO EV SGC SSL,
    // O=Sectigo Limited, STREET="3rd Floor, 26 Office Village", STREET=Exchange Quay, STREET=Trafford Road,
    // L=Salford, OID.2.5.4.17=M5 3EQ, C=GB, OID.2.5.4.15=Private Organization,
    // OID.1.3.6.1.4.1.311.60.2.1.3=GB, SERIALNUMBER=04058690
    // Issuer: CN=COMODO RSA Extended Validation Secure Server CA, O=COMODO CA Limited, L=Salford,
    // ST=Greater Manchester, C=GB
    // Serial number: a0c7cabcc25ed9358ded02cc1d485545
    // Valid from: Sun Sep 29 17:00:00 PDT 2019 until: Tue Dec 28 15:59:59 PST 2021
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIH0TCCBrmgAwIBAgIRAKDHyrzCXtk1je0CzB1IVUUwDQYJKoZIhvcNAQELBQAw\n" +
            "gZIxCzAJBgNVBAYTAkdCMRswGQYDVQQIExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAO\n" +
            "BgNVBAcTB1NhbGZvcmQxGjAYBgNVBAoTEUNPTU9ETyBDQSBMaW1pdGVkMTgwNgYD\n" +
            "VQQDEy9DT01PRE8gUlNBIEV4dGVuZGVkIFZhbGlkYXRpb24gU2VjdXJlIFNlcnZl\n" +
            "ciBDQTAeFw0xOTA5MzAwMDAwMDBaFw0yMTEyMjgyMzU5NTlaMIIBPjERMA8GA1UE\n" +
            "BRMIMDQwNTg2OTAxEzARBgsrBgEEAYI3PAIBAxMCR0IxHTAbBgNVBA8TFFByaXZh\n" +
            "dGUgT3JnYW5pemF0aW9uMQswCQYDVQQGEwJHQjEPMA0GA1UEERMGTTUgM0VRMRAw\n" +
            "DgYDVQQHEwdTYWxmb3JkMRYwFAYDVQQJEw1UcmFmZm9yZCBSb2FkMRYwFAYDVQQJ\n" +
            "Ew1FeGNoYW5nZSBRdWF5MSUwIwYDVQQJExwzcmQgRmxvb3IsIDI2IE9mZmljZSBW\n" +
            "aWxsYWdlMRgwFgYDVQQKEw9TZWN0aWdvIExpbWl0ZWQxGjAYBgNVBAsTEUNPTU9E\n" +
            "TyBFViBTR0MgU1NMMTgwNgYDVQQDEy9jb21vZG9yc2FjZXJ0aWZpY2F0aW9uYXV0\n" +
            "aG9yaXR5LWV2LmNvbW9kb2NhLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC\n" +
            "AQoCggEBAND/eZQBTjpBDsuteKwl+zpTitF8tJzwHAhcQHC2AaLF/GJl1rnjx4Of\n" +
            "elMhKhN1Od9KU6onHGOd2w4mD4EiYK9TpXwuwTyzfkCmnkqxZjYK3KAJN013o4L+\n" +
            "8y1zsGVUulpN/GfMaxTb4XdmeSekTP91Phw3xezijBq3sa++1rO5RBaT1IHeHhHv\n" +
            "iC9WNrG8CIg/j5MyC9i43LZHiRXLER1LzT/MCIRsiG5AEbiYXV5BNd5SiiHtBJ1q\n" +
            "0ZJH+AxL2ERaT41VCppboZwThmJGGoky9FWjp6z8U6Enx0fAMJIZNEzW6LAJFKPE\n" +
            "ynEU004jFFCEumPUqqCC4ogxulphY80CAwEAAaOCA3EwggNtMB8GA1UdIwQYMBaA\n" +
            "FDna/8ooFIqodBMIueQOqdL6fp1pMB0GA1UdDgQWBBQ+S4ZhIrwOoeGs9BBT4uXq\n" +
            "89Ux/jAOBgNVHQ8BAf8EBAMCBaAwDAYDVR0TAQH/BAIwADAdBgNVHSUEFjAUBggr\n" +
            "BgEFBQcDAQYIKwYBBQUHAwIwTwYDVR0gBEgwRjA7BgwrBgEEAbIxAQIBBQEwKzAp\n" +
            "BggrBgEFBQcCARYdaHR0cHM6Ly9zZWN1cmUuY29tb2RvLmNvbS9DUFMwBwYFZ4EM\n" +
            "AQEwVgYDVR0fBE8wTTBLoEmgR4ZFaHR0cDovL2NybC5jb21vZG9jYS5jb20vQ09N\n" +
            "T0RPUlNBRXh0ZW5kZWRWYWxpZGF0aW9uU2VjdXJlU2VydmVyQ0EuY3JsMIGHBggr\n" +
            "BgEFBQcBAQR7MHkwUQYIKwYBBQUHMAKGRWh0dHA6Ly9jcnQuY29tb2RvY2EuY29t\n" +
            "L0NPTU9ET1JTQUV4dGVuZGVkVmFsaWRhdGlvblNlY3VyZVNlcnZlckNBLmNydDAk\n" +
            "BggrBgEFBQcwAYYYaHR0cDovL29jc3AuY29tb2RvY2EuY29tMDoGA1UdEQQzMDGC\n" +
            "L2NvbW9kb3JzYWNlcnRpZmljYXRpb25hdXRob3JpdHktZXYuY29tb2RvY2EuY29t\n" +
            "MIIBfQYKKwYBBAHWeQIEAgSCAW0EggFpAWcAdQDuS723dc5guuFCaR+r4Z5mow9+\n" +
            "X7By2IMAxHuJeqj9ywAAAW2DAXefAAAEAwBGMEQCIDqP1einOiPHnaG1fOZMDrEc\n" +
            "RAxjq3vEl94fp4pkmke7AiBsJOvPE6irgcOO1/lnP7NRuln7iPJjU7T20PEK5/rm\n" +
            "KwB2AFWB1MIWkDYBSuoLm1c8U/DA5Dh4cCUIFy+jqh0HE9MMAAABbYMBd0kAAAQD\n" +
            "AEcwRQIhALgUI5XxM1NHbJDdr19h2pe3LhzK4tpuB/OQ9BgCyrGXAiBdr6mNCB/G\n" +
            "rbdVx0u7iezwC7mq7iaWugR3rrWlSA8fWQB2ALvZ37wfinG1k5Qjl6qSe0c4V5UK\n" +
            "q1LoGpCWZDaOHtGFAAABbYMBd1oAAAQDAEcwRQIgXbG32dagMeLhuZb+LSpJO1vI\n" +
            "BmxmRnNdiz5FbG9cCbwCIQCr1X9f+ebT5fhlDUNBURUorTtM8QQciBiueBqvHk7+\n" +
            "1DANBgkqhkiG9w0BAQsFAAOCAQEAM/A/1dgoc5NP1n+w3SX9qWcN7QT7ExdrnZSl\n" +
            "Ygn0PF2fx4gz7cvNKucbpQJNA4C9awGydyYK8/o5KDUXt3K7eb1OAZ/NZBjygsJs\n" +
            "ikXvxlBh8oEoqBOfOtr24l0NGUWnP8Qeu/VPcIMER4V8qX+in0pCXkSd67nkp6Bs\n" +
            "EcqhDPgmzdSC1gQHsZuBdotG14OfdH1cG1bRK6GadISLG1h8BFukVem42B149v8F\n" +
            "MCIUQAYprAVv2WlTZKBx9XzuK6IK3+klHZ07Jfvjvt7PPG5HKSMWBMnMaTHKcyQI\n" +
            "G3t91yw7BnNNInZlBSsFtqjbHhDcr7uruZdbi0rerSsi2qDr0w==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Tue Mar 02 02:51:39 PST 2021", System.out);
    }
}

class ComodoECC {

    // Owner: CN=COMODO ECC Extended Validation Secure Server CA,
    // O=COMODO CA Limited, L=Salford, ST=Greater Manchester, C=GB
    // Issuer: CN=COMODO ECC Certification Authority, O=COMODO CA Limited,
    // L=Salford, ST=Greater Manchester, C=GB
    // Serial number: 61d4643b412b5d8d715499d8553aa03
    // Valid from: Sun Apr 14 17:00:00 PDT 2013 until: Fri Apr 14 16:59:59 PDT 2028
    private static final String INT = "-----BEGIN CERTIFICATE-----\n"
            + "MIIDojCCAyigAwIBAgIQBh1GQ7QStdjXFUmdhVOqAzAKBggqhkjOPQQDAzCBhTEL\n"
            + "MAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UE\n"
            + "BxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxKzApBgNVBAMT\n"
            + "IkNPTU9ETyBFQ0MgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMTMwNDE1MDAw\n"
            + "MDAwWhcNMjgwNDE0MjM1OTU5WjCBkjELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdy\n"
            + "ZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UEBxMHU2FsZm9yZDEaMBgGA1UEChMRQ09N\n"
            + "T0RPIENBIExpbWl0ZWQxODA2BgNVBAMTL0NPTU9ETyBFQ0MgRXh0ZW5kZWQgVmFs\n"
            + "aWRhdGlvbiBTZWN1cmUgU2VydmVyIENBMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcD\n"
            + "QgAEV3AaPyeTQy0aWXXkBJMR42DsJ5pnbliJe7ndaHzCDslVlY8ofpxeFiqluZrK\n"
            + "KNcJeBU/Jl1YI9jLMyMZKsfSoaOCAWkwggFlMB8GA1UdIwQYMBaAFHVxpxlIGbyd\n"
            + "nepBR9+UxEh3mdN5MB0GA1UdDgQWBBTTTsMZulhZ0Rxgt2FTRzund4/4ijAOBgNV\n"
            + "HQ8BAf8EBAMCAYYwEgYDVR0TAQH/BAgwBgEB/wIBADA+BgNVHSAENzA1MDMGBFUd\n"
            + "IAAwKzApBggrBgEFBQcCARYdaHR0cHM6Ly9zZWN1cmUuY29tb2RvLmNvbS9DUFMw\n"
            + "TAYDVR0fBEUwQzBBoD+gPYY7aHR0cDovL2NybC5jb21vZG9jYS5jb20vQ09NT0RP\n"
            + "RUNDQ2VydGlmaWNhdGlvbkF1dGhvcml0eS5jcmwwcQYIKwYBBQUHAQEEZTBjMDsG\n"
            + "CCsGAQUFBzAChi9odHRwOi8vY3J0LmNvbW9kb2NhLmNvbS9DT01PRE9FQ0NBZGRU\n"
            + "cnVzdENBLmNydDAkBggrBgEFBQcwAYYYaHR0cDovL29jc3AuY29tb2RvY2EuY29t\n"
            + "MAoGCCqGSM49BAMDA2gAMGUCMQDmPWS98nREWdt4xB83r9MVvgG5INpKHi6V1dUY\n"
            + "lCqvSvXXjK0QvZSrOB7cj9RavGgCMG2xJNG+SvlTWEYpmK7eXSgmRUgoBDeQ0yDK\n"
            + "lnxmeeOBnnCaDIxAcA3aCj2Gtdt3sA==\n"
            + "-----END CERTIFICATE-----";

    // Owner: CN=comodoecccertificationauthority-ev.comodoca.com, O=Sectigo Limited, STREET="3rd Floor,
    // 26 Office Village", STREET=Exchange Quay, STREET=Trafford Road, L=Salford, ST=Manchester, OID.2.5.4.17=M5 3EQ,
    // C=GB, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.3=GB, SERIALNUMBER=04058690
    // Issuer: CN=COMODO ECC Extended Validation Secure Server CA, O=COMODO CA Limited, L=Salford,
    // ST=Greater Manchester, C=GB
    // Serial number: db437a31e5be29a62443e3caa1479001
    // Valid from: Mon Mar 01 16:00:00 PST 2021 until: Sat Apr 02 16:59:59 PDT 2022
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFvzCCBWWgAwIBAgIRANtDejHlvimmJEPjyqFHkAEwCgYIKoZIzj0EAwIwgZIx\n" +
            "CzAJBgNVBAYTAkdCMRswGQYDVQQIExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAOBgNV\n" +
            "BAcTB1NhbGZvcmQxGjAYBgNVBAoTEUNPTU9ETyBDQSBMaW1pdGVkMTgwNgYDVQQD\n" +
            "Ey9DT01PRE8gRUNDIEV4dGVuZGVkIFZhbGlkYXRpb24gU2VjdXJlIFNlcnZlciBD\n" +
            "QTAeFw0yMTAzMDIwMDAwMDBaFw0yMjA0MDIyMzU5NTlaMIIBNzERMA8GA1UEBRMI\n" +
            "MDQwNTg2OTAxEzARBgsrBgEEAYI3PAIBAxMCR0IxHTAbBgNVBA8TFFByaXZhdGUg\n" +
            "T3JnYW5pemF0aW9uMQswCQYDVQQGEwJHQjEPMA0GA1UEERMGTTUgM0VRMRMwEQYD\n" +
            "VQQIEwpNYW5jaGVzdGVyMRAwDgYDVQQHEwdTYWxmb3JkMRYwFAYDVQQJEw1UcmFm\n" +
            "Zm9yZCBSb2FkMRYwFAYDVQQJEw1FeGNoYW5nZSBRdWF5MSUwIwYDVQQJExwzcmQg\n" +
            "Rmxvb3IsIDI2IE9mZmljZSBWaWxsYWdlMRgwFgYDVQQKEw9TZWN0aWdvIExpbWl0\n" +
            "ZWQxODA2BgNVBAMTL2NvbW9kb2VjY2NlcnRpZmljYXRpb25hdXRob3JpdHktZXYu\n" +
            "Y29tb2RvY2EuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEt26qBS7TRu/y\n" +
            "fR+RiqLAzW2C+UspFZlORc4EhLfNYMgFkoZKjEnwJzudH6a+uRPqPOhPgUd6PFfR\n" +
            "QFOcLjmhgaOCAvIwggLuMB8GA1UdIwQYMBaAFNNOwxm6WFnRHGC3YVNHO6d3j/iK\n" +
            "MB0GA1UdDgQWBBTpZ0tzKscFw6Z3vCEDFzGR5VSkVzAOBgNVHQ8BAf8EBAMCBYAw\n" +
            "DAYDVR0TAQH/BAIwADAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwSQYD\n" +
            "VR0gBEIwQDA1BgwrBgEEAbIxAQIBBQEwJTAjBggrBgEFBQcCARYXaHR0cHM6Ly9z\n" +
            "ZWN0aWdvLmNvbS9DUFMwBwYFZ4EMAQEwVgYDVR0fBE8wTTBLoEmgR4ZFaHR0cDov\n" +
            "L2NybC5jb21vZG9jYS5jb20vQ09NT0RPRUNDRXh0ZW5kZWRWYWxpZGF0aW9uU2Vj\n" +
            "dXJlU2VydmVyQ0EuY3JsMIGHBggrBgEFBQcBAQR7MHkwUQYIKwYBBQUHMAKGRWh0\n" +
            "dHA6Ly9jcnQuY29tb2RvY2EuY29tL0NPTU9ET0VDQ0V4dGVuZGVkVmFsaWRhdGlv\n" +
            "blNlY3VyZVNlcnZlckNBLmNydDAkBggrBgEFBQcwAYYYaHR0cDovL29jc3AuY29t\n" +
            "b2RvY2EuY29tMDoGA1UdEQQzMDGCL2NvbW9kb2VjY2NlcnRpZmljYXRpb25hdXRo\n" +
            "b3JpdHktZXYuY29tb2RvY2EuY29tMIIBBAYKKwYBBAHWeQIEAgSB9QSB8gDwAHYA\n" +
            "RqVV63X6kSAwtaKJafTzfREsQXS+/Um4havy/HD+bUcAAAF38qtH4AAABAMARzBF\n" +
            "AiBsKoB1TTfoUYUNqF160/vlOENHyK1zzARcnfGKYURHTwIhANKYWg1CO7jyCPk+\n" +
            "IrrLaR+461snNK4LJZXJm4o/9GeeAHYAb1N2rDHwMRnYmQCkURX/dxUcEdkCwQAp\n" +
            "Bo2yCJo32RMAAAF38qtJIAAABAMARzBFAiEA1hgxkYZb5Tc9+vQsDnsfXVewClN2\n" +
            "7gzwd4hZdqAsOSYCID9CWcBvkKrL44mfe9ky1Z6BnAWHUBMCxTjt8MO/IMZ8MAoG\n" +
            "CCqGSM49BAMCA0gAMEUCIBa3sfOiVb0q4LcXU9umKjzVw3Ib8VdiPTtXSnyl0oLb\n" +
            "AiEAnpRB53UtLAF7xw98ELmK/LEk1b5KSlqoO8sFHgwQ8vI=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=comodoecccertificationauthority-ev.comodoca.com, OU=COMODO EV SSL, O=Sectigo Limited,
    // STREET="3rd Floor, 26 Office Village", STREET=Exchange Quay, STREET=Trafford Road, L=Salford, OID.2.5.4.17=M5 3EQ,
    // C=GB, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.3=GB, SERIALNUMBER=04058690
    // Issuer: CN=COMODO ECC Extended Validation Secure Server CA, O=COMODO CA Limited, L=Salford,
    // ST=Greater Manchester, C=GB
    // Serial number: 7972d9d8472a2d52ad1ee6edfb16cbe1
    // Valid from: Sun Sep 29 17:00:00 PDT 2019 until: Tue Dec 28 15:59:59 PST 2021
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGPzCCBeWgAwIBAgIQeXLZ2EcqLVKtHubt+xbL4TAKBggqhkjOPQQDAjCBkjEL\n" +
            "MAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UE\n" +
            "BxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxODA2BgNVBAMT\n" +
            "L0NPTU9ETyBFQ0MgRXh0ZW5kZWQgVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
            "MB4XDTE5MDkzMDAwMDAwMFoXDTIxMTIyODIzNTk1OVowggE6MREwDwYDVQQFEwgw\n" +
            "NDA1ODY5MDETMBEGCysGAQQBgjc8AgEDEwJHQjEdMBsGA1UEDxMUUHJpdmF0ZSBP\n" +
            "cmdhbml6YXRpb24xCzAJBgNVBAYTAkdCMQ8wDQYDVQQREwZNNSAzRVExEDAOBgNV\n" +
            "BAcTB1NhbGZvcmQxFjAUBgNVBAkTDVRyYWZmb3JkIFJvYWQxFjAUBgNVBAkTDUV4\n" +
            "Y2hhbmdlIFF1YXkxJTAjBgNVBAkTHDNyZCBGbG9vciwgMjYgT2ZmaWNlIFZpbGxh\n" +
            "Z2UxGDAWBgNVBAoTD1NlY3RpZ28gTGltaXRlZDEWMBQGA1UECxMNQ09NT0RPIEVW\n" +
            "IFNTTDE4MDYGA1UEAxMvY29tb2RvZWNjY2VydGlmaWNhdGlvbmF1dGhvcml0eS1l\n" +
            "di5jb21vZG9jYS5jb20wWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAS3bqoFLtNG\n" +
            "7/J9H5GKosDNbYL5SykVmU5FzgSEt81gyAWShkqMSfAnO50fpr65E+o86E+BR3o8\n" +
            "V9FAU5wuOaGBo4IDcDCCA2wwHwYDVR0jBBgwFoAU007DGbpYWdEcYLdhU0c7p3eP\n" +
            "+IowHQYDVR0OBBYEFOlnS3MqxwXDpne8IQMXMZHlVKRXMA4GA1UdDwEB/wQEAwIF\n" +
            "gDAMBgNVHRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBP\n" +
            "BgNVHSAESDBGMDsGDCsGAQQBsjEBAgEFATArMCkGCCsGAQUFBwIBFh1odHRwczov\n" +
            "L3NlY3VyZS5jb21vZG8uY29tL0NQUzAHBgVngQwBATBWBgNVHR8ETzBNMEugSaBH\n" +
            "hkVodHRwOi8vY3JsLmNvbW9kb2NhLmNvbS9DT01PRE9FQ0NFeHRlbmRlZFZhbGlk\n" +
            "YXRpb25TZWN1cmVTZXJ2ZXJDQS5jcmwwgYcGCCsGAQUFBwEBBHsweTBRBggrBgEF\n" +
            "BQcwAoZFaHR0cDovL2NydC5jb21vZG9jYS5jb20vQ09NT0RPRUNDRXh0ZW5kZWRW\n" +
            "YWxpZGF0aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCQGCCsGAQUFBzABhhhodHRwOi8v\n" +
            "b2NzcC5jb21vZG9jYS5jb20wOgYDVR0RBDMwMYIvY29tb2RvZWNjY2VydGlmaWNh\n" +
            "dGlvbmF1dGhvcml0eS1ldi5jb21vZG9jYS5jb20wggF8BgorBgEEAdZ5AgQCBIIB\n" +
            "bASCAWgBZgB1AO5Lvbd1zmC64UJpH6vhnmajD35fsHLYgwDEe4l6qP3LAAABbYME\n" +
            "EzgAAAQDAEYwRAIgbdo71lBleuJiq+D0ZLp51oVUyWD9EyrtgBSCNwIW4cMCIAqg\n" +
            "0VFTWHEmAVjaV23fGj3Ybu3mpSiHr6viGlgA2lYaAHUAVYHUwhaQNgFK6gubVzxT\n" +
            "8MDkOHhwJQgXL6OqHQcT0wwAAAFtgwQTKAAABAMARjBEAiBb/gW1RU7kgFBiNpHx\n" +
            "LStujKIocyENUTXsMbsac+LktwIgXbEr8vOOCEdBdXQ2F/FKec8ft6gz57mHNmwl\n" +
            "pp7phbQAdgC72d+8H4pxtZOUI5eqkntHOFeVCqtS6BqQlmQ2jh7RhQAAAW2DBBM6\n" +
            "AAAEAwBHMEUCIQDjKN3h86ofR94+JxLFoYuoA+DRtxEY8XGg+NQXlZfUrgIgEoO2\n" +
            "ZzKbGfohdwj/WtDwJDRX5pjXF4M0nECiwtYXDIwwCgYIKoZIzj0EAwIDSAAwRQIg\n" +
            "AkIRVQBwrElFjrnqk5XPvnlnwkIm1A70ayqOf1FexoQCIQC8tBTn//RCfrhcgTjd\n" +
            "ER4wRjFfFoc6lC68OHGVg9CZZg==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Tue Mar 02 02:53:40 PST 2021", System.out);
    }
}

class ComodoUserTrustRSA {

    // Owner: CN=Sectigo RSA Extended Validation Secure Server CA, O=Sectigo Limited, L=Salford,
    // ST=Greater Manchester, C=GB
    // Issuer: CN=USERTrust RSA Certification Authority, O=The USERTRUST Network, L=Jersey City, ST=New Jersey, C=US
    // Serial number: 284e39c14b386d889c7299e58cd05a57
    // Valid from: Thu Nov 01 17:00:00 PDT 2018 until: Tue Dec 31 15:59:59 PST 2030
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGNDCCBBygAwIBAgIQKE45wUs4bYiccpnljNBaVzANBgkqhkiG9w0BAQwFADCB\n" +
            "iDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCk5ldyBKZXJzZXkxFDASBgNVBAcTC0pl\n" +
            "cnNleSBDaXR5MR4wHAYDVQQKExVUaGUgVVNFUlRSVVNUIE5ldHdvcmsxLjAsBgNV\n" +
            "BAMTJVVTRVJUcnVzdCBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMTgx\n" +
            "MTAyMDAwMDAwWhcNMzAxMjMxMjM1OTU5WjCBkTELMAkGA1UEBhMCR0IxGzAZBgNV\n" +
            "BAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UEBxMHU2FsZm9yZDEYMBYGA1UE\n" +
            "ChMPU2VjdGlnbyBMaW1pdGVkMTkwNwYDVQQDEzBTZWN0aWdvIFJTQSBFeHRlbmRl\n" +
            "ZCBWYWxpZGF0aW9uIFNlY3VyZSBTZXJ2ZXIgQ0EwggEiMA0GCSqGSIb3DQEBAQUA\n" +
            "A4IBDwAwggEKAoIBAQCaoslYBiqFev0Yc4TXPa0s9oliMcn9VaENfTUK4GVT7niB\n" +
            "QXxC6Mt8kTtvyr5lU92hDQDh2WDPQsZ7oibh75t2kowT3z1S+Sy1GsUDM4NbdOde\n" +
            "orcmzFm/b4bwD4G/G+pB4EX1HSfjN9eT0Hje+AGvCrd2MmnxJ+Yymv9BH9OB65jK\n" +
            "rUO9Na4iHr48XWBDFvzsPCJ11Uioof6dRBVp+Lauj88Z7k2X8d606HeXn43h6acp\n" +
            "LLURWyqXM0CrzedVWBzuXKuBEaqD6w/1VpLJvSU+wl3ScvXSLFp82DSRJVJONXWl\n" +
            "dp9gjJioPGRByeZw11k3galbbF5gFK9xSnbDx29LAgMBAAGjggGNMIIBiTAfBgNV\n" +
            "HSMEGDAWgBRTeb9aqitKz1SA4dibwJ3ysgNmyzAdBgNVHQ4EFgQULGn/gMmHkK40\n" +
            "4bTnTJOFmUDpp7IwDgYDVR0PAQH/BAQDAgGGMBIGA1UdEwEB/wQIMAYBAf8CAQAw\n" +
            "HQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMDoGA1UdIAQzMDEwLwYEVR0g\n" +
            "ADAnMCUGCCsGAQUFBwIBFhlodHRwczovL2Nwcy51c2VydHJ1c3QuY29tMFAGA1Ud\n" +
            "HwRJMEcwRaBDoEGGP2h0dHA6Ly9jcmwudXNlcnRydXN0LmNvbS9VU0VSVHJ1c3RS\n" +
            "U0FDZXJ0aWZpY2F0aW9uQXV0aG9yaXR5LmNybDB2BggrBgEFBQcBAQRqMGgwPwYI\n" +
            "KwYBBQUHMAKGM2h0dHA6Ly9jcnQudXNlcnRydXN0LmNvbS9VU0VSVHJ1c3RSU0FB\n" +
            "ZGRUcnVzdENBLmNydDAlBggrBgEFBQcwAYYZaHR0cDovL29jc3AudXNlcnRydXN0\n" +
            "LmNvbTANBgkqhkiG9w0BAQwFAAOCAgEAQ4AzPxVypLyy3IjUUmVl7FaxrHsXQq2z\n" +
            "Zt2gKnHQShuA+5xpRPNndjvhHk4D08PZXUe6Im7E5knqxtyl5aYdldb+HI/7f+zd\n" +
            "W/1ub2N4Vq4ZYUjcZ1ECOFK7Z2zoNicDmU+Fe/TreXPuPsDicTG/tMcWEVM558OQ\n" +
            "TJkB2LK3ZhGukWM/RTMRcRdXaXOX8Lh0ylzRO1O0ObXytvOFpkkkD92HGsfS06i7\n" +
            "NLDPJEeZXqzHE5Tqj7VSAj+2luwfaXaPLD8lQEVci8xmsPGOn0mXE1ZzsChEPhVq\n" +
            "FYQUsbiRJRhidKauhd+G2CkRTcR5fpsuz+iStB9s5Fks9lKoXnn0hv78VYjvR78C\n" +
            "Cvj5FW/ounHjWTWMb3il9S5ngbFGcelB1l/MQkR63+1ybdi2OpjNWJCftxOWUpkC\n" +
            "xaRdnOnSj7GQY0NLn8Gtq9FcSZydtkVgXpouSFZkXNS/MYwbcCCcRKBbrk8ss0SI\n" +
            "Xg1gTURjh9VP1OHm0OktYcUw9e90wHIDn7h0qA+bWOsZquSRzT4s2crF3ZSA3tuV\n" +
            "/UJ33mjdVO8wBD8aI5y10QreSPJvZHHNDyCmoyjXvNhR+u3arXUoHWxO+MZBeXbi\n" +
            "iF7Nwn/IEmQvWBW8l6D26CXIavcY1kAJcfyzHkrPbLo+fAOa/KFl3lIU+0biEVNk\n" +
            "Q9zXE6hC6X4=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=usertrustrsacertificationauthority-ev.comodoca.com, O=Sectigo Limited, STREET="3rd Floor,
    // 26 Office Village", STREET=Exchange Quay, STREET=Trafford Road, L=Salford, ST=Manchester, OID.2.5.4.17=M5 3EQ,
    // C=GB, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.3=GB, SERIALNUMBER=04058690
    // Issuer: CN=Sectigo RSA Extended Validation Secure Server CA, O=Sectigo Limited, L=Salford,
    // ST=Greater Manchester, C=GB
    // Serial number: 4e484426dbfed0c222b2ed152465614a
    // Valid from: Mon Mar 01 16:00:00 PST 2021 until: Sat Apr 02 16:59:59 PDT 2022
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHTzCCBjegAwIBAgIQTkhEJtv+0MIisu0VJGVhSjANBgkqhkiG9w0BAQsFADCB\n" +
            "kTELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G\n" +
            "A1UEBxMHU2FsZm9yZDEYMBYGA1UEChMPU2VjdGlnbyBMaW1pdGVkMTkwNwYDVQQD\n" +
            "EzBTZWN0aWdvIFJTQSBFeHRlbmRlZCBWYWxpZGF0aW9uIFNlY3VyZSBTZXJ2ZXIg\n" +
            "Q0EwHhcNMjEwMzAyMDAwMDAwWhcNMjIwNDAyMjM1OTU5WjCCAToxETAPBgNVBAUT\n" +
            "CDA0MDU4NjkwMRMwEQYLKwYBBAGCNzwCAQMTAkdCMR0wGwYDVQQPExRQcml2YXRl\n" +
            "IE9yZ2FuaXphdGlvbjELMAkGA1UEBhMCR0IxDzANBgNVBBETBk01IDNFUTETMBEG\n" +
            "A1UECBMKTWFuY2hlc3RlcjEQMA4GA1UEBxMHU2FsZm9yZDEWMBQGA1UECRMNVHJh\n" +
            "ZmZvcmQgUm9hZDEWMBQGA1UECRMNRXhjaGFuZ2UgUXVheTElMCMGA1UECRMcM3Jk\n" +
            "IEZsb29yLCAyNiBPZmZpY2UgVmlsbGFnZTEYMBYGA1UEChMPU2VjdGlnbyBMaW1p\n" +
            "dGVkMTswOQYDVQQDEzJ1c2VydHJ1c3Ryc2FjZXJ0aWZpY2F0aW9uYXV0aG9yaXR5\n" +
            "LWV2LmNvbW9kb2NhLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
            "AJ4f68XomMKS2uudXi7xp0fkRK4Q1pE2bamXB8PTsuyS9rhC8hD2zPr9Gs+NHAR0\n" +
            "tG0GSWW1plzbpDFDEsCG+M+7fDl5cc/br8RLn75agZeKngv89y6RQUURxHq6N8hi\n" +
            "lcJKHtWj9j6u1HYvu4u3lWWXQNbYnMWVqP1AVPZsGyDmKn/+Mc2ehvPdYSm/jQLr\n" +
            "hH8Rudr12ZfKHTE4Xx7g5ZH0u52TEAWjuNCiXkhAYa/uUyEu3e7VlsnvxeqBENPn\n" +
            "RwYhfT8mdXV6DvGrnv/NJj/tBTGE5kRbCh4HumY6I3x/XC5UeZE6rT+U6oeRgUOM\n" +
            "6d7siAQVOspSqfTzR5HsBlECAwEAAaOCAvUwggLxMB8GA1UdIwQYMBaAFCxp/4DJ\n" +
            "h5CuNOG050yThZlA6aeyMB0GA1UdDgQWBBR8+3Lw59S2HtjPs+KZcEJ+67fd/DAO\n" +
            "BgNVHQ8BAf8EBAMCBaAwDAYDVR0TAQH/BAIwADAdBgNVHSUEFjAUBggrBgEFBQcD\n" +
            "AQYIKwYBBQUHAwIwSQYDVR0gBEIwQDA1BgwrBgEEAbIxAQIBBQEwJTAjBggrBgEF\n" +
            "BQcCARYXaHR0cHM6Ly9zZWN0aWdvLmNvbS9DUFMwBwYFZ4EMAQEwVgYDVR0fBE8w\n" +
            "TTBLoEmgR4ZFaHR0cDovL2NybC5zZWN0aWdvLmNvbS9TZWN0aWdvUlNBRXh0ZW5k\n" +
            "ZWRWYWxpZGF0aW9uU2VjdXJlU2VydmVyQ0EuY3JsMIGGBggrBgEFBQcBAQR6MHgw\n" +
            "UQYIKwYBBQUHMAKGRWh0dHA6Ly9jcnQuc2VjdGlnby5jb20vU2VjdGlnb1JTQUV4\n" +
            "dGVuZGVkVmFsaWRhdGlvblNlY3VyZVNlcnZlckNBLmNydDAjBggrBgEFBQcwAYYX\n" +
            "aHR0cDovL29jc3Auc2VjdGlnby5jb20wPQYDVR0RBDYwNIIydXNlcnRydXN0cnNh\n" +
            "Y2VydGlmaWNhdGlvbmF1dGhvcml0eS1ldi5jb21vZG9jYS5jb20wggEFBgorBgEE\n" +
            "AdZ5AgQCBIH2BIHzAPEAdwBGpVXrdfqRIDC1oolp9PN9ESxBdL79SbiFq/L8cP5t\n" +
            "RwAAAXfyrRCwAAAEAwBIMEYCIQCeOHfnABa6cl0EHTzyMj2t2qBqORBAC16hJIIl\n" +
            "Y52W4QIhAKHDk1m9lW0kmcZJWEko3eA9QKJSDLNLpdUoBPzNNc76AHYAb1N2rDHw\n" +
            "MRnYmQCkURX/dxUcEdkCwQApBo2yCJo32RMAAAF38q0R6wAABAMARzBFAiEAywsh\n" +
            "8Ki6fFOExwR6de0qzTmf7bJMuQcY0Ry463/9R44CIDeAcX7Z9S1vlRB9gzVomNIN\n" +
            "vkcnUazq7dowPnr5rYMOMA0GCSqGSIb3DQEBCwUAA4IBAQA3a+PBgH1SBVEDpgAN\n" +
            "mWaqIQzJzMRfSgvopQ6nC8iD95SfYD/rvic7aOeBLh/5aEs/CknJsg6o0qB3wz1v\n" +
            "T5JXd5JldRWw3nP80jkIaYgq97RUIkjcHhuw4hTyQP6wk7XVlPVLvBo9ePWxJjmn\n" +
            "whxlSyxQ5A5NdrTqZOJmu9nFr2HXpX75kGwCkUKZI050FAZZydsK3LfMBTqe1Xwi\n" +
            "PKyjXDWd40LjOEg31sA43ofO8n2pySP5LG5XAsvoAyPCy3zXhx5cdtmQFLIkntus\n" +
            "DCfN+n51HPUo8r4PUhQtOiRUB3K871LTdwyv4/CRXS2fIhtO1pxYNKFOw0yrUf6j\n" +
            "ECgk\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=usertrustrsacertificationauthority-ev.comodoca.com, OU=COMODO EV SGC SSL, O=Sectigo Limited,
    // STREET="3rd Floor, 26 Office Village", STREET=Exchange Quay, STREET=Trafford Road, L=Salford, ST=Manchester,
    // OID.2.5.4.17=M5 3EQ, C=GB, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.3=GB, SERIALNUMBER=04058690
    // Issuer: CN=Sectigo RSA Extended Validation Secure Server CA, O=Sectigo Limited, L=Salford,
    // ST=Greater Manchester, C=GB
    // Serial number: b07fd164b5790c9d5d1fddff5819cdb2
    // Valid from: Sun Sep 29 17:00:00 PDT 2019 until: Tue Dec 28 15:59:59 PST 2021
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIH5TCCBs2gAwIBAgIRALB/0WS1eQydXR/d/1gZzbIwDQYJKoZIhvcNAQELBQAw\n" +
            "gZExCzAJBgNVBAYTAkdCMRswGQYDVQQIExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAO\n" +
            "BgNVBAcTB1NhbGZvcmQxGDAWBgNVBAoTD1NlY3RpZ28gTGltaXRlZDE5MDcGA1UE\n" +
            "AxMwU2VjdGlnbyBSU0EgRXh0ZW5kZWQgVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVy\n" +
            "IENBMB4XDTE5MDkzMDAwMDAwMFoXDTIxMTIyODIzNTk1OVowggFWMREwDwYDVQQF\n" +
            "EwgwNDA1ODY5MDETMBEGCysGAQQBgjc8AgEDEwJHQjEdMBsGA1UEDxMUUHJpdmF0\n" +
            "ZSBPcmdhbml6YXRpb24xCzAJBgNVBAYTAkdCMQ8wDQYDVQQREwZNNSAzRVExEzAR\n" +
            "BgNVBAgTCk1hbmNoZXN0ZXIxEDAOBgNVBAcTB1NhbGZvcmQxFjAUBgNVBAkTDVRy\n" +
            "YWZmb3JkIFJvYWQxFjAUBgNVBAkTDUV4Y2hhbmdlIFF1YXkxJTAjBgNVBAkTHDNy\n" +
            "ZCBGbG9vciwgMjYgT2ZmaWNlIFZpbGxhZ2UxGDAWBgNVBAoTD1NlY3RpZ28gTGlt\n" +
            "aXRlZDEaMBgGA1UECxMRQ09NT0RPIEVWIFNHQyBTU0wxOzA5BgNVBAMTMnVzZXJ0\n" +
            "cnVzdHJzYWNlcnRpZmljYXRpb25hdXRob3JpdHktZXYuY29tb2RvY2EuY29tMIIB\n" +
            "IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnh/rxeiYwpLa651eLvGnR+RE\n" +
            "rhDWkTZtqZcHw9Oy7JL2uELyEPbM+v0az40cBHS0bQZJZbWmXNukMUMSwIb4z7t8\n" +
            "OXlxz9uvxEufvlqBl4qeC/z3LpFBRRHEero3yGKVwkoe1aP2Pq7Udi+7i7eVZZdA\n" +
            "1ticxZWo/UBU9mwbIOYqf/4xzZ6G891hKb+NAuuEfxG52vXZl8odMThfHuDlkfS7\n" +
            "nZMQBaO40KJeSEBhr+5TIS7d7tWWye/F6oEQ0+dHBiF9PyZ1dXoO8aue/80mP+0F\n" +
            "MYTmRFsKHge6ZjojfH9cLlR5kTqtP5Tqh5GBQ4zp3uyIBBU6ylKp9PNHkewGUQID\n" +
            "AQABo4IDbjCCA2owHwYDVR0jBBgwFoAULGn/gMmHkK404bTnTJOFmUDpp7IwHQYD\n" +
            "VR0OBBYEFHz7cvDn1LYe2M+z4plwQn7rt938MA4GA1UdDwEB/wQEAwIFoDAMBgNV\n" +
            "HRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJBgNVHSAE\n" +
            "QjBAMDUGDCsGAQQBsjEBAgEFATAlMCMGCCsGAQUFBwIBFhdodHRwczovL3NlY3Rp\n" +
            "Z28uY29tL0NQUzAHBgVngQwBATBWBgNVHR8ETzBNMEugSaBHhkVodHRwOi8vY3Js\n" +
            "LnNlY3RpZ28uY29tL1NlY3RpZ29SU0FFeHRlbmRlZFZhbGlkYXRpb25TZWN1cmVT\n" +
            "ZXJ2ZXJDQS5jcmwwgYYGCCsGAQUFBwEBBHoweDBRBggrBgEFBQcwAoZFaHR0cDov\n" +
            "L2NydC5zZWN0aWdvLmNvbS9TZWN0aWdvUlNBRXh0ZW5kZWRWYWxpZGF0aW9uU2Vj\n" +
            "dXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5zZWN0aWdv\n" +
            "LmNvbTA9BgNVHREENjA0gjJ1c2VydHJ1c3Ryc2FjZXJ0aWZpY2F0aW9uYXV0aG9y\n" +
            "aXR5LWV2LmNvbW9kb2NhLmNvbTCCAX4GCisGAQQB1nkCBAIEggFuBIIBagFoAHYA\n" +
            "7ku9t3XOYLrhQmkfq+GeZqMPfl+wctiDAMR7iXqo/csAAAFtgzv54wAABAMARzBF\n" +
            "AiB5PmhsK3zU3XdKvyxw/wWHMmLI7apHLa1yKdjkA8H+ggIhALdUx7Tl8aeWhK6z\n" +
            "lh+PHvMAdCcAJK6w9qBJGQtSrYO5AHUAVYHUwhaQNgFK6gubVzxT8MDkOHhwJQgX\n" +
            "L6OqHQcT0wwAAAFtgzv5zgAABAMARjBEAiBumSwAUamibqJXTN2cf/H3mjd0T35/\n" +
            "UK9w2hu9gFobxgIgSXTLndHyqFUmcmquu3It0WC1yl6YMceGixbQL1e8BQcAdwC7\n" +
            "2d+8H4pxtZOUI5eqkntHOFeVCqtS6BqQlmQ2jh7RhQAAAW2DO/nXAAAEAwBIMEYC\n" +
            "IQDHRs10oYoXE5yq6WsiksjdQsUWZNpbSsrmz0u+KlxTVQIhAJ4rvHItKSeJLkaN\n" +
            "S3YpVZnkN8tOwuxPsYeyVx/BtaNpMA0GCSqGSIb3DQEBCwUAA4IBAQAPFIsUFymo\n" +
            "VTp0vntHrZpBApBQzDeriQv7Bi7tmou/Ng47RtXW3DjGdrePGSfOdl7h62k8qprU\n" +
            "JeLyloDqhvmT/CG/hdwrfZ3Sv3N2xpetGcnW5S3oEi3m+/M1ls9eD+x1vybqV9Kd\n" +
            "lcjuV7SYDlbvAS9w7TcygudhdW0cI8XTCvesGKohBkAlqaQ/MWYpt4WvsxHjbWgn\n" +
            "5ZlIYR6A1ZFEjADifViH/5AA79lgGhAskkIWPjvRFalEVKTKtjhRK76eCfZs4Frr\n" +
            "CEOpon+BeNKk+x/K/r10dSoWe0SV2uGVxTD83zkP++eREwo1hTgn8bXn7ftlnA3j\n" +
            "7ml+Usz6udaD\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Tue Mar 02 02:55:42 PST 2021", System.out);
    }
}

class ComodoUserTrustECC {

    // Owner: CN=Sectigo ECC Extended Validation Secure Server CA, O=Sectigo Limited, L=Salford,
    // ST=Greater Manchester, C=GB
    // Issuer: CN=USERTrust ECC Certification Authority, O=The USERTRUST Network, L=Jersey City, ST=New Jersey, C=US
    // Serial number: 80f5606d3a162b143adc12fbe8c2066f
    // Valid from: Thu Nov 01 17:00:00 PDT 2018 until: Tue Dec 31 15:59:59 PST 2030
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDyTCCA0+gAwIBAgIRAID1YG06FisUOtwS++jCBm8wCgYIKoZIzj0EAwMwgYgx\n" +
            "CzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpOZXcgSmVyc2V5MRQwEgYDVQQHEwtKZXJz\n" +
            "ZXkgQ2l0eTEeMBwGA1UEChMVVGhlIFVTRVJUUlVTVCBOZXR3b3JrMS4wLAYDVQQD\n" +
            "EyVVU0VSVHJ1c3QgRUNDIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MB4XDTE4MTEw\n" +
            "MjAwMDAwMFoXDTMwMTIzMTIzNTk1OVowgZExCzAJBgNVBAYTAkdCMRswGQYDVQQI\n" +
            "ExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAOBgNVBAcTB1NhbGZvcmQxGDAWBgNVBAoT\n" +
            "D1NlY3RpZ28gTGltaXRlZDE5MDcGA1UEAxMwU2VjdGlnbyBFQ0MgRXh0ZW5kZWQg\n" +
            "VmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENBMFkwEwYHKoZIzj0CAQYIKoZIzj0D\n" +
            "AQcDQgAEAyJ5Ca9JyXq8bO+krLVWysbtm7fdMSJ54uFD23t0x6JAC4IjxevfQJzW\n" +
            "z4T6yY+FybTBqtOa++ijJFnkB5wKy6OCAY0wggGJMB8GA1UdIwQYMBaAFDrhCYbU\n" +
            "zxnClnZ0SXbc4DXGY2OaMB0GA1UdDgQWBBTvwSqVDDLa+3Mw3IoT2BVL9xPo+DAO\n" +
            "BgNVHQ8BAf8EBAMCAYYwEgYDVR0TAQH/BAgwBgEB/wIBADAdBgNVHSUEFjAUBggr\n" +
            "BgEFBQcDAQYIKwYBBQUHAwIwOgYDVR0gBDMwMTAvBgRVHSAAMCcwJQYIKwYBBQUH\n" +
            "AgEWGWh0dHBzOi8vY3BzLnVzZXJ0cnVzdC5jb20wUAYDVR0fBEkwRzBFoEOgQYY/\n" +
            "aHR0cDovL2NybC51c2VydHJ1c3QuY29tL1VTRVJUcnVzdEVDQ0NlcnRpZmljYXRp\n" +
            "b25BdXRob3JpdHkuY3JsMHYGCCsGAQUFBwEBBGowaDA/BggrBgEFBQcwAoYzaHR0\n" +
            "cDovL2NydC51c2VydHJ1c3QuY29tL1VTRVJUcnVzdEVDQ0FkZFRydXN0Q0EuY3J0\n" +
            "MCUGCCsGAQUFBzABhhlodHRwOi8vb2NzcC51c2VydHJ1c3QuY29tMAoGCCqGSM49\n" +
            "BAMDA2gAMGUCMQCjHztBDL90GCRXHlGqm0H7kzP04hd0MxwakKjWzOmstXNFLONj\n" +
            "RFa0JqI/iKUJMFcCMCbLgyzcFW7DihtY5XE0XCLCw+git0NjxiFB6FaOFIlyDdqT\n" +
            "j+Th+DJ92JLvICVD/g==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=usertrustecccertificationauthority-ev.comodoca.com, O=Sectigo Limited,
    // STREET="3rd Floor, 26 Office Village", STREET=Exchange Quay, STREET=Trafford Road, L=Salford,
    // ST=Manchester, OID.2.5.4.17=M5 3EQ,
    // C=GB, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.3=GB, SERIALNUMBER=04058690
    // Issuer: CN=Sectigo ECC Extended Validation Secure Server CA, O=Sectigo Limited, L=Salford,
    // ST=Greater Manchester, C=GB
    // Serial number: 9aa5da67480446fd7bf408fd5fdaa1d8
    // Valid from: Mon Mar 01 16:00:00 PST 2021 until: Sat Apr 02 16:59:59 PDT 2022
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFwTCCBWigAwIBAgIRAJql2mdIBEb9e/QI/V/aodgwCgYIKoZIzj0EAwIwgZEx\n" +
            "CzAJBgNVBAYTAkdCMRswGQYDVQQIExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAOBgNV\n" +
            "BAcTB1NhbGZvcmQxGDAWBgNVBAoTD1NlY3RpZ28gTGltaXRlZDE5MDcGA1UEAxMw\n" +
            "U2VjdGlnbyBFQ0MgRXh0ZW5kZWQgVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
            "MB4XDTIxMDMwMjAwMDAwMFoXDTIyMDQwMjIzNTk1OVowggE6MREwDwYDVQQFEwgw\n" +
            "NDA1ODY5MDETMBEGCysGAQQBgjc8AgEDEwJHQjEdMBsGA1UEDxMUUHJpdmF0ZSBP\n" +
            "cmdhbml6YXRpb24xCzAJBgNVBAYTAkdCMQ8wDQYDVQQREwZNNSAzRVExEzARBgNV\n" +
            "BAgTCk1hbmNoZXN0ZXIxEDAOBgNVBAcTB1NhbGZvcmQxFjAUBgNVBAkTDVRyYWZm\n" +
            "b3JkIFJvYWQxFjAUBgNVBAkTDUV4Y2hhbmdlIFF1YXkxJTAjBgNVBAkTHDNyZCBG\n" +
            "bG9vciwgMjYgT2ZmaWNlIFZpbGxhZ2UxGDAWBgNVBAoTD1NlY3RpZ28gTGltaXRl\n" +
            "ZDE7MDkGA1UEAxMydXNlcnRydXN0ZWNjY2VydGlmaWNhdGlvbmF1dGhvcml0eS1l\n" +
            "di5jb21vZG9jYS5jb20wWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQtMl8R33Za\n" +
            "WD6H8BW0+wybBf0+6+L5YYK/eyAVGm6vwjLaQZWlcdFBMKfaP1qTLi0VAabs4baS\n" +
            "UkD8wR568pVpo4IC8zCCAu8wHwYDVR0jBBgwFoAU78EqlQwy2vtzMNyKE9gVS/cT\n" +
            "6PgwHQYDVR0OBBYEFLOtYfOaIfDHZGubtKNELRR6A2srMA4GA1UdDwEB/wQEAwIH\n" +
            "gDAMBgNVHRMBAf8EAjAAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjBJ\n" +
            "BgNVHSAEQjBAMDUGDCsGAQQBsjEBAgEFATAlMCMGCCsGAQUFBwIBFhdodHRwczov\n" +
            "L3NlY3RpZ28uY29tL0NQUzAHBgVngQwBATBWBgNVHR8ETzBNMEugSaBHhkVodHRw\n" +
            "Oi8vY3JsLnNlY3RpZ28uY29tL1NlY3RpZ29FQ0NFeHRlbmRlZFZhbGlkYXRpb25T\n" +
            "ZWN1cmVTZXJ2ZXJDQS5jcmwwgYYGCCsGAQUFBwEBBHoweDBRBggrBgEFBQcwAoZF\n" +
            "aHR0cDovL2NydC5zZWN0aWdvLmNvbS9TZWN0aWdvRUNDRXh0ZW5kZWRWYWxpZGF0\n" +
            "aW9uU2VjdXJlU2VydmVyQ0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5z\n" +
            "ZWN0aWdvLmNvbTA9BgNVHREENjA0gjJ1c2VydHJ1c3RlY2NjZXJ0aWZpY2F0aW9u\n" +
            "YXV0aG9yaXR5LWV2LmNvbW9kb2NhLmNvbTCCAQMGCisGAQQB1nkCBAIEgfQEgfEA\n" +
            "7wB2AEalVet1+pEgMLWiiWn0830RLEF0vv1JuIWr8vxw/m1HAAABd/Kung0AAAQD\n" +
            "AEcwRQIhAI16l52NctGAphhc6eh2kK2vO5QYk5nyouL3P6U/gG/dAiBfJRJ+iqE/\n" +
            "noco35RpNtlV4GABrwmw1I/1R+L79VzwEAB1AG9Tdqwx8DEZ2JkApFEV/3cVHBHZ\n" +
            "AsEAKQaNsgiaN9kTAAABd/KunvwAAAQDAEYwRAIgS+r3C10ua38DPJKvUJvW5bvL\n" +
            "SCQ949n3sBJvhV6aXq4CIH/oEGgvJmKtMEjVKUQg8TrZO6LwQ+0sYfL79Qvm8wL3\n" +
            "MAoGCCqGSM49BAMCA0cAMEQCID4Q9cc8OQ9tmKnnKZyplPsPipI5apVGkBqFRUSt\n" +
            "zzM3AiAw5tw3cv/oabDsYdU+lmp5kZ/S3Z97ANAAaHE0AfXe/Q==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=usertrustecccertificationauthority-ev.comodoca.com, OU=COMODO EV SGC SSL,
    // O=Sectigo Limited, STREET="3rd Floor, 26 Office Village", STREET=Exchange Quay, STREET=Trafford Road,
    // L=Salford, OID.2.5.4.17=M5 3EQ,
    // C=GB, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.3=GB, SERIALNUMBER=04058690
    // Issuer: CN=Sectigo ECC Extended Validation Secure Server CA, O=Sectigo Limited, L=Salford,
    // ST=Greater Manchester, C=GB
    // Serial number: 8b72489b7f505a55e2a22659c90ed2ab
    // Valid from: Sun Sep 29 17:00:00 PDT 2019 until: Tue Dec 28 15:59:59 PST 2021
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGRTCCBeugAwIBAgIRAItySJt/UFpV4qImWckO0qswCgYIKoZIzj0EAwIwgZEx\n" +
            "CzAJBgNVBAYTAkdCMRswGQYDVQQIExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAOBgNV\n" +
            "BAcTB1NhbGZvcmQxGDAWBgNVBAoTD1NlY3RpZ28gTGltaXRlZDE5MDcGA1UEAxMw\n" +
            "U2VjdGlnbyBFQ0MgRXh0ZW5kZWQgVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENB\n" +
            "MB4XDTE5MDkzMDAwMDAwMFoXDTIxMTIyODIzNTk1OVowggFBMREwDwYDVQQFEwgw\n" +
            "NDA1ODY5MDETMBEGCysGAQQBgjc8AgEDEwJHQjEdMBsGA1UEDxMUUHJpdmF0ZSBP\n" +
            "cmdhbml6YXRpb24xCzAJBgNVBAYTAkdCMQ8wDQYDVQQREwZNNSAzRVExEDAOBgNV\n" +
            "BAcTB1NhbGZvcmQxFjAUBgNVBAkTDVRyYWZmb3JkIFJvYWQxFjAUBgNVBAkTDUV4\n" +
            "Y2hhbmdlIFF1YXkxJTAjBgNVBAkTHDNyZCBGbG9vciwgMjYgT2ZmaWNlIFZpbGxh\n" +
            "Z2UxGDAWBgNVBAoTD1NlY3RpZ28gTGltaXRlZDEaMBgGA1UECxMRQ09NT0RPIEVW\n" +
            "IFNHQyBTU0wxOzA5BgNVBAMTMnVzZXJ0cnVzdGVjY2NlcnRpZmljYXRpb25hdXRo\n" +
            "b3JpdHktZXYuY29tb2RvY2EuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE\n" +
            "LTJfEd92Wlg+h/AVtPsMmwX9Puvi+WGCv3sgFRpur8Iy2kGVpXHRQTCn2j9aky4t\n" +
            "FQGm7OG2klJA/MEeevKVaaOCA28wggNrMB8GA1UdIwQYMBaAFO/BKpUMMtr7czDc\n" +
            "ihPYFUv3E+j4MB0GA1UdDgQWBBSzrWHzmiHwx2Rrm7SjRC0UegNrKzAOBgNVHQ8B\n" +
            "Af8EBAMCB4AwDAYDVR0TAQH/BAIwADAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYB\n" +
            "BQUHAwIwSQYDVR0gBEIwQDA1BgwrBgEEAbIxAQIBBQEwJTAjBggrBgEFBQcCARYX\n" +
            "aHR0cHM6Ly9zZWN0aWdvLmNvbS9DUFMwBwYFZ4EMAQEwVgYDVR0fBE8wTTBLoEmg\n" +
            "R4ZFaHR0cDovL2NybC5zZWN0aWdvLmNvbS9TZWN0aWdvRUNDRXh0ZW5kZWRWYWxp\n" +
            "ZGF0aW9uU2VjdXJlU2VydmVyQ0EuY3JsMIGGBggrBgEFBQcBAQR6MHgwUQYIKwYB\n" +
            "BQUHMAKGRWh0dHA6Ly9jcnQuc2VjdGlnby5jb20vU2VjdGlnb0VDQ0V4dGVuZGVk\n" +
            "VmFsaWRhdGlvblNlY3VyZVNlcnZlckNBLmNydDAjBggrBgEFBQcwAYYXaHR0cDov\n" +
            "L29jc3Auc2VjdGlnby5jb20wPQYDVR0RBDYwNIIydXNlcnRydXN0ZWNjY2VydGlm\n" +
            "aWNhdGlvbmF1dGhvcml0eS1ldi5jb21vZG9jYS5jb20wggF/BgorBgEEAdZ5AgQC\n" +
            "BIIBbwSCAWsBaQB2AO5Lvbd1zmC64UJpH6vhnmajD35fsHLYgwDEe4l6qP3LAAAB\n" +
            "bYL/SJoAAAQDAEcwRQIhAL7EJt/Rgz6NBnx2v8Hevux3Gpcxy64kaeyLVgFeNqFk\n" +
            "AiBRf+OWLOtZzEav/oERljrk8hgZB4CR1nj/Tn98cmRrwwB2AFWB1MIWkDYBSuoL\n" +
            "m1c8U/DA5Dh4cCUIFy+jqh0HE9MMAAABbYL/SIgAAAQDAEcwRQIgVtZZaiBMC2lu\n" +
            "atBzUHQmOq4qrUQP7nS83cd3VzPhToECIQDnlpOCdaxJwr8C0MtkvYpKSabwBPFL\n" +
            "ASEkwmOpjuQErAB3ALvZ37wfinG1k5Qjl6qSe0c4V5UKq1LoGpCWZDaOHtGFAAAB\n" +
            "bYL/SJoAAAQDAEgwRgIhAI8OgzP/kzF1bOJRHU2S/ewij/6HpGPy7Mbm7Hyuv3IU\n" +
            "AiEAxDmX2FmORlgeerQmQ+ar3D9/TwA9RQckVDu5IrgweREwCgYIKoZIzj0EAwID\n" +
            "SAAwRQIhAPwQWGWd3oR7YJ7ngCDQ9TAbdPgND51SiR34WfEgaTQtAiAxD4umKm02\n" +
            "59GEMj5NpyF2ZQEq5mEGcjJNojrn+PC4zg==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator) throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Tue Mar 02 02:59:25 PST 2021", System.out);
    }
}
