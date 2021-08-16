/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8233223
 * @summary Interoperability tests with Amazon's CA1, CA2, CA3, and CA4
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath AmazonCA OCSP
 * @run main/othervm -Djava.security.debug=certpath AmazonCA CRL
 */

/*
 * Obtain TLS test artifacts for Amazon CAs from:
 *
 * Amazon Root CA 1
 *     Valid - https://good.sca1a.amazontrust.com/
 *     Revoked - https://revoked.sca1a.amazontrust.com/
 * Amazon Root CA 2
 *     Valid - https://good.sca2a.amazontrust.com/
 *     Revoked - https://revoked.sca2a.amazontrust.com/
 * Amazon Root CA 3
 *     Valid - https://good.sca3a.amazontrust.com/
 *     Revoked - https://revoked.sca3a.amazontrust.com/
 * Amazon Root CA 4
 *     Valid - https://good.sca4a.amazontrust.com/
 *     Revoked - https://revoked.sca4a.amazontrust.com/
 */
public class AmazonCA {

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

        new AmazonCA_1().runTest(pathValidator, ocspEnabled);
        new AmazonCA_2().runTest(pathValidator, ocspEnabled);
        new AmazonCA_3().runTest(pathValidator, ocspEnabled);
        new AmazonCA_4().runTest(pathValidator, ocspEnabled);
    }
}

class AmazonCA_1 {

    // Owner: CN=Amazon, OU=Server CA 1A, O=Amazon, C=US
    // Issuer: CN=Amazon Root CA 1, O=Amazon, C=US
    // Serial number: 67f9457508c648c09ca652e71791830e72592
    // Valid from: Wed Oct 21 17:00:00 PDT 2015 until: Sat Oct 18 17:00:00 PDT 2025
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIERzCCAy+gAwIBAgITBn+UV1CMZIwJymUucXkYMOclkjANBgkqhkiG9w0BAQsF\n" +
            "ADA5MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRkwFwYDVQQDExBBbWF6\n" +
            "b24gUm9vdCBDQSAxMB4XDTE1MTAyMjAwMDAwMFoXDTI1MTAxOTAwMDAwMFowRjEL\n" +
            "MAkGA1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEVMBMGA1UECxMMU2VydmVyIENB\n" +
            "IDFBMQ8wDQYDVQQDEwZBbWF6b24wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK\n" +
            "AoIBAQCeQM3XCsIZunv8bSJxOqkc/ed87uL76FDB7teBNThDRB+1J7aITuadbNfH\n" +
            "5ZfZykrdZ1qQLKxP6DwHOmJr9u2b4IxjUX9qUMuq4B02ghD2g6yU3YivEosZ7fpo\n" +
            "srD2TBN29JpgPGrOrpOE+ArZuIpBjdKFinemu6fTDD0NCeQlfyHXd1NOYyfYRLTa\n" +
            "xlpDqr/2M41BgSkWQfSPHHyRWNQgWBiGsIQaS8TK0g8OWi1ov78+2K9DWT+AHgXW\n" +
            "AanjZK91GfygPXJYSlAGxSiBAwH/KhAMifhaoFYAbH0Yuohmd85B45G2xVsop4TM\n" +
            "Dsl007U7qnS7sdJ4jYGzEvva/a95AgMBAAGjggE5MIIBNTASBgNVHRMBAf8ECDAG\n" +
            "AQH/AgEAMA4GA1UdDwEB/wQEAwIBhjAdBgNVHQ4EFgQUYtRCXoZwdWqQvMa40k1g\n" +
            "wjS6UTowHwYDVR0jBBgwFoAUhBjMhTTsvAyUlC4IWZzHshBOCggwewYIKwYBBQUH\n" +
            "AQEEbzBtMC8GCCsGAQUFBzABhiNodHRwOi8vb2NzcC5yb290Y2ExLmFtYXpvbnRy\n" +
            "dXN0LmNvbTA6BggrBgEFBQcwAoYuaHR0cDovL2NydC5yb290Y2ExLmFtYXpvbnRy\n" +
            "dXN0LmNvbS9yb290Y2ExLmNlcjA/BgNVHR8EODA2MDSgMqAwhi5odHRwOi8vY3Js\n" +
            "LnJvb3RjYTEuYW1hem9udHJ1c3QuY29tL3Jvb3RjYTEuY3JsMBEGA1UdIAQKMAgw\n" +
            "BgYEVR0gADANBgkqhkiG9w0BAQsFAAOCAQEAMHbSWHRFMzGNIE0qhN6gnRahTrTU\n" +
            "CDPwe7l9/q0IA+QBlrpUHnlAreetYeH1jB8uF3qXXzy22gpBU7NqulTkqSPByT1J\n" +
            "xOhpT2FpO5R3VAdMPdWfSEgtrED0jkmyUQrR1T+/A+nBLdJZeQcl+OqLgeY790JM\n" +
            "JJTsJnnI6FBWeTGhcDI4Y+n3KS3QCVePeWI7jx1dhrHcXH+QDX8Ywe31hV7YENdr\n" +
            "HDpUXrjK6eHN8gazy8G6pndXHFwHp4auiZbJbYAk/q1peOTRagD2JojcLkm+i3cD\n" +
            "843t4By6YT/PVlePU2PCWejkrJQnKQAPOov7IA8kuO2RDWuzE/zF6Hotdg==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=good.sca1a.amazontrust.com, O=Amazon Trust Services, L=Seattle, ST=Washington, C=US, \
    // SERIALNUMBER=5846743, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.2=Delaware, \
    // OID.1.3.6.1.4.1.311.60.2.1.3=US
    // Issuer: CN=Amazon, OU=Server CA 1A, O=Amazon, C=US
    // Serial number: 703e4e4bbd78e2b6db5634f36c4ee944cb1a4
    // Valid from: Mon Jul 29 16:53:36 PDT 2019 until: Sat Aug 29 16:53:36 PDT 2020
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFEzCCA/ugAwIBAgITBwPk5LvXjitttWNPNsTulEyxpDANBgkqhkiG9w0BAQsF\n" +
            "ADBGMQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRUwEwYDVQQLEwxTZXJ2\n" +
            "ZXIgQ0EgMUExDzANBgNVBAMTBkFtYXpvbjAeFw0xOTA3MjkyMzUzMzZaFw0yMDA4\n" +
            "MjkyMzUzMzZaMIHaMRMwEQYLKwYBBAGCNzwCAQMTAlVTMRkwFwYLKwYBBAGCNzwC\n" +
            "AQITCERlbGF3YXJlMR0wGwYDVQQPExRQcml2YXRlIE9yZ2FuaXphdGlvbjEQMA4G\n" +
            "A1UEBRMHNTg0Njc0MzELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24x\n" +
            "EDAOBgNVBAcTB1NlYXR0bGUxHjAcBgNVBAoTFUFtYXpvbiBUcnVzdCBTZXJ2aWNl\n" +
            "czEjMCEGA1UEAxMaZ29vZC5zY2ExYS5hbWF6b250cnVzdC5jb20wggEiMA0GCSqG\n" +
            "SIb3DQEBAQUAA4IBDwAwggEKAoIBAQDQyuJ83c2Zf9k29f6iLqd8nJSuHSk1v+SS\n" +
            "0sYyG8tjscfCC1HcOdNj37vtiNN65sXh/e/kBKH9wvzhCLOJbBqVKRHOZuHdJEpH\n" +
            "35R6C/PbcV/tp49g6mNmBe+lcmm/cwwCtYvkL0rgL/OKB0liFhhRIqy2TPg08op/\n" +
            "RlY2DdbgBA2B3g7wdMo0hK3SO56/QUccUtLRm43km9Yd4E3U+CEUyDd0Bmc/YbPa\n" +
            "htuXVsXJwiwlwooomujIIENhFw3htdcsu2apRj8EYUrKL8Mvvn+h16gDyobj0f01\n" +
            "jWXlUgmH2lzUzca5eGuphfvmWN/ME/yqC2mMvWGnWySycqtT8VdJAgMBAAGjggFj\n" +
            "MIIBXzAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0OBBYEFFENOZBwFkjVdQX0iK32c77z\n" +
            "SUl6MB8GA1UdIwQYMBaAFGLUQl6GcHVqkLzGuNJNYMI0ulE6MB0GA1UdJQQWMBQG\n" +
            "CCsGAQUFBwMBBggrBgEFBQcDAjB1BggrBgEFBQcBAQRpMGcwLQYIKwYBBQUHMAGG\n" +
            "IWh0dHA6Ly9vY3NwLnNjYTFhLmFtYXpvbnRydXN0LmNvbTA2BggrBgEFBQcwAoYq\n" +
            "aHR0cDovL2NydC5zY2ExYS5hbWF6b250cnVzdC5jb20vc2NhMWEuY2VyMCUGA1Ud\n" +
            "EQQeMByCGmdvb2Quc2NhMWEuYW1hem9udHJ1c3QuY29tMFAGA1UdIARJMEcwDQYL\n" +
            "YIZIAYb9bgEHGAMwNgYFZ4EMAQEwLTArBggrBgEFBQcCARYfaHR0cHM6Ly93d3cu\n" +
            "YW1hem9udHJ1c3QuY29tL2NwczANBgkqhkiG9w0BAQsFAAOCAQEAmn7z6Ub1sL77\n" +
            "wyUEaCq/Odqm+2RtYYMJ1MeW6nTXTfAgZ/iLx/6hStafd9AK9gHiTCggBpj6KgnF\n" +
            "UsGMDeX879jP675fH6SEk710QPDhIrfAzwE0pF/eUNsd7pLwne32zHX0ouCoAt4d\n" +
            "KwBCZkKNUkdj4U+bpOJzvtcTP9JlzziLp9IFRjjQh3xKgfblx57CmRJbqH3fT5JJ\n" +
            "IAIDVTz3ZUcqhPTFAnNsO1oNBEyrO5X9rwCiSy7aRijY/11R75mIIvyA9zyd9ss1\n" +
            "kvrrER0GWMTDvC84FZD2vhkXgPTFrB1Dn9f3QgO5APT9GCFY5hdpqqPEXOSdRzQo\n" +
            "h9j4OQAqtA==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=revoked.sca1a.amazontrust.com, O=Amazon Trust Services, L=Seattle, ST=Washington, C=US, \
    // SERIALNUMBER=5846743, OID.2.5.4.15=PrivateOrganization, OID.1.3.6.1.4.1.311.60.2.1.2=Delaware, \
    // OID.1.3.6.1.4.1.311.60.2.1.3=US
    // Issuer: CN=Amazon, OU=Server CA 1A, O=Amazon, C=US
    // Serial number: 6f1d774ad5e7b6d251d217661782bbdb6f37d
    // Valid from: Mon Jan 28 15:34:38 PST 2019 until: Thu Apr 28 16:34:38 PDT 2022
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIE2zCCA8OgAwIBAgITBvHXdK1ee20lHSF2YXgrvbbzfTANBgkqhkiG9w0BAQsF\n" +
            "ADBGMQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRUwEwYDVQQLEwxTZXJ2\n" +
            "ZXIgQ0EgMUExDzANBgNVBAMTBkFtYXpvbjAeFw0xOTAxMjgyMzM0MzhaFw0yMjA0\n" +
            "MjgyMzM0MzhaMIHcMRMwEQYLKwYBBAGCNzwCAQMTAlVTMRkwFwYLKwYBBAGCNzwC\n" +
            "AQITCERlbGF3YXJlMRwwGgYDVQQPExNQcml2YXRlT3JnYW5pemF0aW9uMRAwDgYD\n" +
            "VQQFEwc1ODQ2NzQzMQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQ\n" +
            "MA4GA1UEBxMHU2VhdHRsZTEeMBwGA1UEChMVQW1hem9uIFRydXN0IFNlcnZpY2Vz\n" +
            "MSYwJAYDVQQDEx1yZXZva2VkLnNjYTFhLmFtYXpvbnRydXN0LmNvbTCCASIwDQYJ\n" +
            "KoZIhvcNAQEBBQADggEPADCCAQoCggEBANUoHop9sW+QlgVsdtacioraTAWHcSTd\n" +
            "MNkOkOEMgJIFPyfdcDvW/H2NvpdYeIQqzaCgT2kcsONWTZTPJMirCPnzl1ohHOZU\n" +
            "uTnOVkamGxvNmQCURLBXmlCMRTCI5RY3CuYntFFbSPAnbumsF+K/gKqcE6ME53Bw\n" +
            "PAwn4qwavB0i5Ib7Jk8XYzxSYXC9l8QLxt6fshPJRlecpXzfmVFvMAm3IbaLcpuv\n" +
            "AtD+8I2KwjNtBPRPNYeFsWxwsgUGAyHEGa61oTGUqqAXu5YmPfyK+YTOJdoofsh4\n" +
            "Tf3K7AKxnPWuvY3RNTs1pzEVwJYZqSsNwbgyKJJ4+0Xe4iP7qB8SYf8CAwEAAaOC\n" +
            "ASkwggElMA4GA1UdDwEB/wQEAwIFoDAdBgNVHQ4EFgQUGHreoz+LP/Wr+RKzuexO\n" +
            "V8ICtmEwHwYDVR0jBBgwFoAUYtRCXoZwdWqQvMa40k1gwjS6UTowHQYDVR0lBBYw\n" +
            "FAYIKwYBBQUHAwEGCCsGAQUFBwMCMHUGCCsGAQUFBwEBBGkwZzAtBggrBgEFBQcw\n" +
            "AYYhaHR0cDovL29jc3Auc2NhMWEuYW1hem9udHJ1c3QuY29tMDYGCCsGAQUFBzAC\n" +
            "hipodHRwOi8vY3J0LnNjYTFhLmFtYXpvbnRydXN0LmNvbS9zY2ExYS5jZXIwKAYD\n" +
            "VR0RBCEwH4IdcmV2b2tlZC5zY2ExYS5hbWF6b250cnVzdC5jb20wEwYDVR0gBAww\n" +
            "CjAIBgZngQwBAgEwDQYJKoZIhvcNAQELBQADggEBABSbe1UCLL7Qay6XK5wD8B5a\n" +
            "wvR1XG3UrggpVIz/w5cutEm/yE71hzE0gag/3YPbNYEnaLbJH+9jz4YW9wd/cEPj\n" +
            "xSK5PErAQjCd+aA4LKN1xqkSysgYknl0y47hJBXGnWf+hxvBBHeSoUzM0KIC21pC\n" +
            "ZyXrmfaPCQAz13ruYIYdQaETqXGVORmKbf/a+Zn18/tfQt0LeeCYVoSopbXWQvcJ\n" +
            "gUMtdIqYQmb8aVj0pdZXwKl4yZ2DtlS3Z9MpWNgQNlhRPmiYlu28y2yTtZ9SwD6m\n" +
            "2f+cwc19aJrDT4Y280px+jRU7dIE6oZVJU+yBRVIZYpUFAB7extCMVxnTkCf8Dk=\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // EE certificates don't have CRLDP extension
        if (!ocspEnabled){
            pathValidator.validate(new String[]{INT},
                    ValidatePathWithParams.Status.GOOD, null, System.out);

            return;
        }

        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Mon Jan 28 15:35:56 PST 2019", System.out);
    }
}

class AmazonCA_2 {

    // Owner: CN=Amazon, OU=Server CA 2A, O=Amazon, C=US
    // Issuer: CN=Amazon Root CA 2, O=Amazon, C=US
    // Serial number: 67f945755f187a91f8163f3e624620177ff38
    // Valid from: Wed Oct 21 17:00:00 PDT 2015 until: Sat Oct 18 17:00:00 PDT 2025
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGRzCCBC+gAwIBAgITBn+UV1Xxh6kfgWPz5iRiAXf/ODANBgkqhkiG9w0BAQwF\n" +
            "ADA5MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRkwFwYDVQQDExBBbWF6\n" +
            "b24gUm9vdCBDQSAyMB4XDTE1MTAyMjAwMDAwMFoXDTI1MTAxOTAwMDAwMFowRjEL\n" +
            "MAkGA1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEVMBMGA1UECxMMU2VydmVyIENB\n" +
            "IDJBMQ8wDQYDVQQDEwZBbWF6b24wggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIK\n" +
            "AoICAQC0P8hSLewmrZ41CCPBQytZs5NBFMq5ztbnMf+kZUp9S25LPfjNW3zgC/6E\n" +
            "qCTWNVMMHhq7ez9IQJk48qbfBTLlZkuKnUWbA9vowrDfcxUN0mRE4B/TJbveXyTf\n" +
            "vE91iDlqDrERecE9D8sdjzURrtHTp27lZdRkXFvfEVCq4hl3sHkzjodisaQthLp1\n" +
            "gLsiA7vKt+8zcL4Aeq52UyYb8r4/jdZ3KaQp8O/T4VwDCRKm8ey3kttpJWaflci7\n" +
            "eRzNjY7gE3NMANVXCeQwOBfH2GjINFCObmPsqiBuoAnsv2k5aQLNoU1OZk08ClXm\n" +
            "mEZ2rI5qZUTX1HuefBJnpMkPugFCw8afaHnB13SkLE7wxX8SZRdDIe5WiwyDL1tR\n" +
            "2+8lpz4JsMoFopHmD3GaHyjbN+hkOqHgLltwewOsiyM0u3CZphypN2KeD+1FLjnY\n" +
            "TgdIAd1FRgK2ZXDDrEdjnsSEfShKf0l4mFPSBs9E3U6sLmubDRXKLLLpa/dF4eKu\n" +
            "LEKS1bXYT28iM6D5gSCnzho5G4d18jQD/slmc5XmRo5Pig0RyBwDaLuxeIZuiJ0A\n" +
            "J6YFhffbrLYF5dEQl0cU+t3VBK5u/o1WkWXsZawU038lWn/AXerodT/pAcrtWA4E\n" +
            "NQEN09WEKMhZVPhqdwhF/Gusr04mQtKt7T2v6UMQvtVglv5E7wIDAQABo4IBOTCC\n" +
            "ATUwEgYDVR0TAQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAYYwHQYDVR0OBBYE\n" +
            "FNpDStD8AcBLv1gnjHbNCoHzlC70MB8GA1UdIwQYMBaAFLAM8Eww9AVYAkj9M+VS\n" +
            "r0uE42ZSMHsGCCsGAQUFBwEBBG8wbTAvBggrBgEFBQcwAYYjaHR0cDovL29jc3Au\n" +
            "cm9vdGNhMi5hbWF6b250cnVzdC5jb20wOgYIKwYBBQUHMAKGLmh0dHA6Ly9jcnQu\n" +
            "cm9vdGNhMi5hbWF6b250cnVzdC5jb20vcm9vdGNhMi5jZXIwPwYDVR0fBDgwNjA0\n" +
            "oDKgMIYuaHR0cDovL2NybC5yb290Y2EyLmFtYXpvbnRydXN0LmNvbS9yb290Y2Ey\n" +
            "LmNybDARBgNVHSAECjAIMAYGBFUdIAAwDQYJKoZIhvcNAQEMBQADggIBAEO5W+iF\n" +
            "yChjDyyrmiwFupVWQ0Xy2ReFNQiZq7XKVHvsLQe01moSLnxcBxioOPBKt1KkZO7w\n" +
            "Gcbmke0+7AxLaG/F5NPnzRtK1/pRhXQ0XdU8pVh/1/h4GoqRlZ/eN0JDarUhZPkV\n" +
            "kSr96LUYDTxcsAidF7zkzWfmtcJg/Aw8mi14xKVEa6aVyKu54c8kKkdlt0WaigOv\n" +
            "Z/xYhxp24AfoFKaIraDNdsD8q2N7eDYeN4WGLzNSlil+iFjzflI9mq1hTuI/ZNjV\n" +
            "rbvob6FUQ8Cc524gMjbpZCNuZ1gfXzwwhGp0AnQF6CJsWF9uwPpZEVFnnnfiWH3M\n" +
            "oup41EvBhqaAqOlny0sm5pI82nRUCAE3DLkJ1+eAtdQaYblZQkQrRyTuPmJEm+5y\n" +
            "QwdDVw6uHc5OsSj/tyhh8zJ2Xq3zgh3dMONGjJEysxGaCoIb+61PWwMy2dIarVwI\n" +
            "r+c+AY+3PrhgBspNdWZ87JzNHii7ksdjUSVGTTy1vGXgPYrv0lp0IMnKaZP58xiw\n" +
            "rDx7uTlQuPVWNOZvCaT3ZcoxTsNKNscIUe+WJjWx5hdzpv/oksDPY5ltZ0j3hlDS\n" +
            "D+Itk95/cNJVRM/0HpxI1SX9MTZtOSJoEDdUtOpVaOuBAvEK4gvTzdt0r5L+fuI6\n" +
            "o5LAuRo/LO1xVRH49KFRoaznzU3Ch9+kbPb3\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=good.sca2a.amazontrust.com, O=Amazon Trust Services, L=Seattle, ST=Washington, C=US, \
    // SERIALNUMBER=5846743, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.2=Delaware, \
    // OID.1.3.6.1.4.1.311.60.2.1.3=US
    // Issuer: CN=Amazon, OU=Server CA 2A, O=Amazon, C=US
    // Serial number: 703e4e70616c90d611fd04a5ecc635665184e
    // Valid from: Mon Jul 29 16:54:06 PDT 2019 until: Sat Aug 29 16:54:06 PDT 2020
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHEzCCBPugAwIBAgITBwPk5wYWyQ1hH9BKXsxjVmUYTjANBgkqhkiG9w0BAQwF\n" +
            "ADBGMQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRUwEwYDVQQLEwxTZXJ2\n" +
            "ZXIgQ0EgMkExDzANBgNVBAMTBkFtYXpvbjAeFw0xOTA3MjkyMzU0MDZaFw0yMDA4\n" +
            "MjkyMzU0MDZaMIHaMRMwEQYLKwYBBAGCNzwCAQMTAlVTMRkwFwYLKwYBBAGCNzwC\n" +
            "AQITCERlbGF3YXJlMR0wGwYDVQQPExRQcml2YXRlIE9yZ2FuaXphdGlvbjEQMA4G\n" +
            "A1UEBRMHNTg0Njc0MzELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24x\n" +
            "EDAOBgNVBAcTB1NlYXR0bGUxHjAcBgNVBAoTFUFtYXpvbiBUcnVzdCBTZXJ2aWNl\n" +
            "czEjMCEGA1UEAxMaZ29vZC5zY2EyYS5hbWF6b250cnVzdC5jb20wggIiMA0GCSqG\n" +
            "SIb3DQEBAQUAA4ICDwAwggIKAoICAQC+XjOB3ZCFX+b9y9reP+e6EAQz4ytiMSqU\n" +
            "O4s5MyYLkY6n4BIZHmgWeQ2IgW1VrH8ho+Iu3UsTiuhd3/L/q/w+T0OJfcrWngTs\n" +
            "uVcIuvUr32ObPeeWbg/m/lkN7hqH1jY62iybYVrFXiLo1+0G92PUazcyNvyA20+G\n" +
            "HsvGG5jlArWNgRLdc8KUXxvnDUxx5vu4jeHEZnqSwuulV1h9ve0UutkmoK0Sk7Rz\n" +
            "HMxYK0LmUT5OvcNQSkUi5nLi+M1FxnYYgsELwSiKSSEDfEdgxooMAiVTgw51Q/DB\n" +
            "lTOjAIDL3K3J0yGfIG3bwLvE1qz2Z5yWn8f3JibIah7LrC4PiZDDLHFM6V9l+YqU\n" +
            "RqimJ5BltSyAx7bxQNZ1AW3Lxvvm894i4k6/Vdf1CDovRuTMPCDAQmKA/A/AQ7TN\n" +
            "q3bBimX6UyuJu0I8RyvAYKzFhOOqe4vXrbndTbje/jnzTNQPeIIcuRa9cgXTOrbw\n" +
            "86FTUKj6AZXihRWjKWsQpDwdgE0tQETZ3ynCXfbBKfFmn0MSjeX0CEEAZdYHR8EV\n" +
            "F271Yt7UJjS/FP702aHTOWk7zFbIRfFQODvBhn0I8p/Stk2sDq4/YsbXVZOe3+ad\n" +
            "YavoiODGSAH6ZcZzULumgK9eii0koAOPB/xqXnkcTS63gEHOKjLQl3hqdVZRCugv\n" +
            "1CwUXLvoSwIDAQABo4IBYzCCAV8wDgYDVR0PAQH/BAQDAgWgMB0GA1UdDgQWBBTa\n" +
            "j6dHgPdOxTGLcwaNDeaMnlSxNjAfBgNVHSMEGDAWgBTaQ0rQ/AHAS79YJ4x2zQqB\n" +
            "85Qu9DAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwdQYIKwYBBQUHAQEE\n" +
            "aTBnMC0GCCsGAQUFBzABhiFodHRwOi8vb2NzcC5zY2EyYS5hbWF6b250cnVzdC5j\n" +
            "b20wNgYIKwYBBQUHMAKGKmh0dHA6Ly9jcnQuc2NhMmEuYW1hem9udHJ1c3QuY29t\n" +
            "L3NjYTJhLmNlcjAlBgNVHREEHjAcghpnb29kLnNjYTJhLmFtYXpvbnRydXN0LmNv\n" +
            "bTBQBgNVHSAESTBHMA0GC2CGSAGG/W4BBxgDMDYGBWeBDAEBMC0wKwYIKwYBBQUH\n" +
            "AgEWH2h0dHBzOi8vd3d3LmFtYXpvbnRydXN0LmNvbS9jcHMwDQYJKoZIhvcNAQEM\n" +
            "BQADggIBAE6RwZAZvN0i9ygwzqoX9DhSPtvZ3xIO0G0Bhgjkb986+p8XJstU3gEM\n" +
            "8P2i1J/YthXCnRGedm+Odxx+31G6xIYfP5S5g7HyRGkj/aXNXy4s3KjH8HJgOY9N\n" +
            "ra3XfC05OKq5FpyZQDZ+hxCdLrH3Gs+UxREbu+LuIKUpI7nMVEjn9XynKyOdKN21\n" +
            "Kq5VsuI0fDWCYvUN1M+lI/LgE5HbNJVQJs+dB7g1/kaOeaLia7Wk1ys+uRzB58rp\n" +
            "FKAoLk++HWTfNDkbN8vKRfHhJ/xhI9ju3TWcci6EyFVAym1C62UkJNI0KHgQ+zc7\n" +
            "nl1tv/ytj8N/eJoysyp23lJ5qrVetlQORfgXryGkWBMYBvYF8zbBb/f+UXHDKVWt\n" +
            "9l1lL6HQGY/tTo253pj6/FgDD35bZdjLQeUVmbnz679S5oUmoH5ZtSdnpUTghU3p\n" +
            "bae9adBFY9S1pm50Q3ckRVBAwNqNmI0KKUh14Ms8KSAUHg19NvGsBonqwOT2rdbv\n" +
            "xZ47N6c2eCl/cjMvzre0v0NoUO+3og2GHeAoOwVos6480YDbMqp739tOFPxBcsII\n" +
            "6SjpDVh+14dkSW6kEKeaCFLR+eChqutri1VQbQ49nmADQWw9Al8vBytSnPv0YN6W\n" +
            "XfIE1Qj7YmHu/UuoeKVsqDqoP/no29+96dtfd4afJqlIoyZUqXpt\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=revoked.sca2a.amazontrust.com, O=Amazon Trust Services, L=Seattle, ST=Washington, C=US, \
    // SERIALNUMBER=5846743, OID.2.5.4.15=PrivateOrganization, OID.1.3.6.1.4.1.311.60.2.1.2=Delaware, \
    // OID.1.3.6.1.4.1.311.60.2.1.3=US
    //Issuer: CN=Amazon, OU=Server CA 2A, O=Amazon, C=US
    //Serial number: 6f1d782c0aa2f4866b7b522c279b939b92369
    //Valid from: Mon Jan 28 15:37:45 PST 2019 until: Thu Apr 28 16:37:45 PDT 2022
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIG2zCCBMOgAwIBAgITBvHXgsCqL0hmt7Uiwnm5ObkjaTANBgkqhkiG9w0BAQwF\n" +
            "ADBGMQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRUwEwYDVQQLEwxTZXJ2\n" +
            "ZXIgQ0EgMkExDzANBgNVBAMTBkFtYXpvbjAeFw0xOTAxMjgyMzM3NDVaFw0yMjA0\n" +
            "MjgyMzM3NDVaMIHcMRMwEQYLKwYBBAGCNzwCAQMTAlVTMRkwFwYLKwYBBAGCNzwC\n" +
            "AQITCERlbGF3YXJlMRwwGgYDVQQPExNQcml2YXRlT3JnYW5pemF0aW9uMRAwDgYD\n" +
            "VQQFEwc1ODQ2NzQzMQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQ\n" +
            "MA4GA1UEBxMHU2VhdHRsZTEeMBwGA1UEChMVQW1hem9uIFRydXN0IFNlcnZpY2Vz\n" +
            "MSYwJAYDVQQDEx1yZXZva2VkLnNjYTJhLmFtYXpvbnRydXN0LmNvbTCCAiIwDQYJ\n" +
            "KoZIhvcNAQEBBQADggIPADCCAgoCggIBAKFm418X8hN1YTgD2XpMb4sp78mw8k3j\n" +
            "Dq/vnpX48evVUzNpHpy4qRz/ZHBR4HUJO4lhfnX+CO0uRqqqx4F0JZRQB3KevaU8\n" +
            "QGWHdJGhEddnurDhrgOUa+ZroqUnMCsTJfbyGtC6aiEXeu/eMhEUFkuBxJH1JtwD\n" +
            "dQXMXuMjG07SVjOkhTkbMDzA/YbUqkDeOIybifDuvA5LEsl+kReY0b6RYFo2Tt/M\n" +
            "dPhJD8q3Wsu+XCiCnbpcwlEVGxiD2RVRXJJ9o3ALGOxqU69V+lYS0kkwNHT7oV9J\n" +
            "rhgt7iOCq0aoTAxu2j4FCp0JHNhGoW9pXoMXnmS6kK80hzLNYDxvKEaVaKkiYHw5\n" +
            "CV0Vwii05ICa14nrStH/jcRNLyU+gp+6OeerPV3jpKWshGKWewF+2UiWU2WHTSrd\n" +
            "Wis0/qEfFK/kSraAxpd+KavEEavKeudoMAHIxMACOk9E/fF5zhd2y4G1q1BdoRlR\n" +
            "KP4GIV2v6qH6Ru2mNSuge9il6kDXxFNucrYKLDbAqkqalohkvDavcPoG9gZT3etv\n" +
            "4IcgJriIWRxbJwKPpwJM+6wa6RpwoeJMuEp3ZBP7KDaQ8YX4rlf4zXLAsOKCNA9K\n" +
            "OS/qYQ/I4g0E1WhfgEKClaLPS2u7jeVR6s1t4txGo4vq5Dkt17KTCew/WsX3rckf\n" +
            "a2p5zvFcfpCNAgMBAAGjggEpMIIBJTAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0OBBYE\n" +
            "FAF8N1wV8EoYFkMXH6tEnmR/7vI+MB8GA1UdIwQYMBaAFNpDStD8AcBLv1gnjHbN\n" +
            "CoHzlC70MB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjB1BggrBgEFBQcB\n" +
            "AQRpMGcwLQYIKwYBBQUHMAGGIWh0dHA6Ly9vY3NwLnNjYTJhLmFtYXpvbnRydXN0\n" +
            "LmNvbTA2BggrBgEFBQcwAoYqaHR0cDovL2NydC5zY2EyYS5hbWF6b250cnVzdC5j\n" +
            "b20vc2NhMmEuY2VyMCgGA1UdEQQhMB+CHXJldm9rZWQuc2NhMmEuYW1hem9udHJ1\n" +
            "c3QuY29tMBMGA1UdIAQMMAowCAYGZ4EMAQIBMA0GCSqGSIb3DQEBDAUAA4ICAQBC\n" +
            "VwR1NFk1IYIF4cjU7ML1aj8OIn+8mtakGQnuSJLK6ypSysINJBS48ZDdP6XZXvyD\n" +
            "iTS0xEAPjAZHTqrABdNYmvJeL2RnN99DIwVzBpZp4NLTXbiSW7jb0Y5cEPDGJMOo\n" +
            "SUAAM6fsiPRfz5vX4XVPznbcF2AwE/NVV+L3n9LVRt7qv2VqIEvLioR56Dq+5ofR\n" +
            "4bw0BVlEYWF4Gsy7WDDTL1iLNBUwZTqBHwTv0fgDRiPqb/odmLQuRANwcJy8B8Zr\n" +
            "s/yX4SeESaRdA82lAlQilksQitXS2qvQN06GEDOgUxYE6EabFdgklV5JypKqdOly\n" +
            "vzpaDpF3z5W8Bj3D4fns1Kjrh1pPh5JRvg+616diKnQRt4X5q+EtmnXhDvIGMISI\n" +
            "FuGwj57CNQ2x2MY2HHKWPrOccpQfEEvoSNR+ntYWrtSSttZq948O+zZBk1TXWuXV\n" +
            "TVXllqTg8lp6d5cfKgvtHKgt98WkpPOcLVrNuVnMAIfDw6ar54dVKqrvkeEcF6mJ\n" +
            "7oMKjJX/Vu9lYoGViBIfdeqcCPWSI8BpnCKaG7dTQO3Q1ObGmLdGBRlsRh+d+S5l\n" +
            "Fq326ckbjx537e5/ai31lOR7OwVh9TDweKLqIACjs987C0EJSEfoOue25WRww2va\n" +
            "iX9SrTPm4GxQ2OJgYwx0+HbezJXFN+dhaOFUavTSFw==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // EE certificates don't have CRLDP extension
        if (!ocspEnabled){
            pathValidator.validate(new String[]{INT},
                    ValidatePathWithParams.Status.GOOD, null, System.out);

            return;
        }

        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Mon Jan 28 15:38:57 PST 2019", System.out);
    }
}

class AmazonCA_3 {

    // Owner: CN=Amazon, OU=Server CA 3A, O=Amazon, C=US
    // Issuer: CN=Amazon Root CA 3, O=Amazon, C=US
    // Serial number: 67f945758fe55b9ee3f75831d47f07d226c8a
    // Valid from: Wed Oct 21 17:00:00 PDT 2015 until: Sat Oct 18 17:00:00 PDT 2025
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIICuzCCAmGgAwIBAgITBn+UV1j+VbnuP3WDHUfwfSJsijAKBggqhkjOPQQDAjA5\n" +
            "MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRkwFwYDVQQDExBBbWF6b24g\n" +
            "Um9vdCBDQSAzMB4XDTE1MTAyMjAwMDAwMFoXDTI1MTAxOTAwMDAwMFowRjELMAkG\n" +
            "A1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEVMBMGA1UECxMMU2VydmVyIENBIDNB\n" +
            "MQ8wDQYDVQQDEwZBbWF6b24wWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATYcYsK\n" +
            "mYdR0Gj8Xz45E/lfcTTnXhg2EtAIYBIHyXv/ZQyyyCas1aptX/I5T1coT6XK181g\n" +
            "nB8hADuKfWlNoIYRo4IBOTCCATUwEgYDVR0TAQH/BAgwBgEB/wIBADAOBgNVHQ8B\n" +
            "Af8EBAMCAYYwHQYDVR0OBBYEFATc4JXl6LlrlKHvjFsxHhN+VZfaMB8GA1UdIwQY\n" +
            "MBaAFKu229cGnjesMIYHkXDHnMQZsXjAMHsGCCsGAQUFBwEBBG8wbTAvBggrBgEF\n" +
            "BQcwAYYjaHR0cDovL29jc3Aucm9vdGNhMy5hbWF6b250cnVzdC5jb20wOgYIKwYB\n" +
            "BQUHMAKGLmh0dHA6Ly9jcnQucm9vdGNhMy5hbWF6b250cnVzdC5jb20vcm9vdGNh\n" +
            "My5jZXIwPwYDVR0fBDgwNjA0oDKgMIYuaHR0cDovL2NybC5yb290Y2EzLmFtYXpv\n" +
            "bnRydXN0LmNvbS9yb290Y2EzLmNybDARBgNVHSAECjAIMAYGBFUdIAAwCgYIKoZI\n" +
            "zj0EAwIDSAAwRQIgOl/vux0qfxNm05W3eofa9lKwz6oKvdu6g6Sc0UlwgRcCIQCS\n" +
            "WSQ6F6JHLoeOWLyFFF658eNKEKbkEGMHz34gLX/N3g==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=good.sca3a.amazontrust.com, O=Amazon Trust Services, L=Seattle, ST=Washington, C=US, \
    // SERIALNUMBER=5846743, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.2=Delaware, \
    // OID.1.3.6.1.4.1.311.60.2.1.3=US
    // Issuer: CN=Amazon, OU=Server CA 3A, O=Amazon, C=US
    // Serial number: 703e4e9bbc2605f37967a0e95f31f4789a677
    // Valid from: Mon Jul 29 16:54:43 PDT 2019 until: Sat Aug 29 16:54:43 PDT 2020
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDhzCCAy2gAwIBAgITBwPk6bvCYF83lnoOlfMfR4mmdzAKBggqhkjOPQQDAjBG\n" +
            "MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRUwEwYDVQQLEwxTZXJ2ZXIg\n" +
            "Q0EgM0ExDzANBgNVBAMTBkFtYXpvbjAeFw0xOTA3MjkyMzU0NDNaFw0yMDA4Mjky\n" +
            "MzU0NDNaMIHaMRMwEQYLKwYBBAGCNzwCAQMTAlVTMRkwFwYLKwYBBAGCNzwCAQIT\n" +
            "CERlbGF3YXJlMR0wGwYDVQQPExRQcml2YXRlIE9yZ2FuaXphdGlvbjEQMA4GA1UE\n" +
            "BRMHNTg0Njc0MzELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAO\n" +
            "BgNVBAcTB1NlYXR0bGUxHjAcBgNVBAoTFUFtYXpvbiBUcnVzdCBTZXJ2aWNlczEj\n" +
            "MCEGA1UEAxMaZ29vZC5zY2EzYS5hbWF6b250cnVzdC5jb20wWTATBgcqhkjOPQIB\n" +
            "BggqhkjOPQMBBwNCAARl4yxf8XcvWR0LZ+YuBC0CpkwtU2NiMdlIM7eX0lxhQp53\n" +
            "NpLlCrPRNzOWrjCJDdn21D0u7PrtN94UHLHOg9X0o4IBYzCCAV8wDgYDVR0PAQH/\n" +
            "BAQDAgeAMB0GA1UdDgQWBBT2cHmOJFLWfg1Op7xAdAnqYcwaPzAfBgNVHSMEGDAW\n" +
            "gBQE3OCV5ei5a5Sh74xbMR4TflWX2jAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYB\n" +
            "BQUHAwIwdQYIKwYBBQUHAQEEaTBnMC0GCCsGAQUFBzABhiFodHRwOi8vb2NzcC5z\n" +
            "Y2EzYS5hbWF6b250cnVzdC5jb20wNgYIKwYBBQUHMAKGKmh0dHA6Ly9jcnQuc2Nh\n" +
            "M2EuYW1hem9udHJ1c3QuY29tL3NjYTNhLmNlcjAlBgNVHREEHjAcghpnb29kLnNj\n" +
            "YTNhLmFtYXpvbnRydXN0LmNvbTBQBgNVHSAESTBHMA0GC2CGSAGG/W4BBxgDMDYG\n" +
            "BWeBDAEBMC0wKwYIKwYBBQUHAgEWH2h0dHBzOi8vd3d3LmFtYXpvbnRydXN0LmNv\n" +
            "bS9jcHMwCgYIKoZIzj0EAwIDSAAwRQIgURdcqJVr4PWNIkmWcSKmzgZ1i94hQpGe\n" +
            "mWbE9osk4m0CIQDhxIguihwvDa5RsBwdM0aRDgGKLNHigGqJoKqgH0d2qg==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=revoked.sca3a.amazontrust.com, O=Amazon Trust Services, L=Seattle, ST=Washington, C=US, \
    // SERIALNUMBER=5846743, OID.2.5.4.15=PrivateOrganization, OID.1.3.6.1.4.1.311.60.2.1.2=Delaware, \
    // OID.1.3.6.1.4.1.311.60.2.1.3=US
    // Issuer: CN=Amazon, OU=Server CA 3A, O=Amazon, C=US
    // Serial number: 6f1d78cf0ca64ce7f551a6f2a0715cc0e8b50
    // Valid from: Mon Jan 28 15:40:01 PST 2019 until: Thu Apr 28 16:40:01 PDT 2022
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDTzCCAvWgAwIBAgITBvHXjPDKZM5/VRpvKgcVzA6LUDAKBggqhkjOPQQDAjBG\n" +
            "MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRUwEwYDVQQLEwxTZXJ2ZXIg\n" +
            "Q0EgM0ExDzANBgNVBAMTBkFtYXpvbjAeFw0xOTAxMjgyMzQwMDFaFw0yMjA0Mjgy\n" +
            "MzQwMDFaMIHcMRMwEQYLKwYBBAGCNzwCAQMTAlVTMRkwFwYLKwYBBAGCNzwCAQIT\n" +
            "CERlbGF3YXJlMRwwGgYDVQQPExNQcml2YXRlT3JnYW5pemF0aW9uMRAwDgYDVQQF\n" +
            "Ewc1ODQ2NzQzMQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4G\n" +
            "A1UEBxMHU2VhdHRsZTEeMBwGA1UEChMVQW1hem9uIFRydXN0IFNlcnZpY2VzMSYw\n" +
            "JAYDVQQDEx1yZXZva2VkLnNjYTNhLmFtYXpvbnRydXN0LmNvbTBZMBMGByqGSM49\n" +
            "AgEGCCqGSM49AwEHA0IABJNl90Jq0wddpFj+JbLtmvGR/1geL5t1tvV406jGpYn2\n" +
            "C5lAFjwASFy7pAnazZbfSkIDUU2i2XU0+7Cs+j1S/EOjggEpMIIBJTAOBgNVHQ8B\n" +
            "Af8EBAMCB4AwHQYDVR0OBBYEFPhX3dYays5Sps0xTgouLkZzYLg4MB8GA1UdIwQY\n" +
            "MBaAFATc4JXl6LlrlKHvjFsxHhN+VZfaMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggr\n" +
            "BgEFBQcDAjB1BggrBgEFBQcBAQRpMGcwLQYIKwYBBQUHMAGGIWh0dHA6Ly9vY3Nw\n" +
            "LnNjYTNhLmFtYXpvbnRydXN0LmNvbTA2BggrBgEFBQcwAoYqaHR0cDovL2NydC5z\n" +
            "Y2EzYS5hbWF6b250cnVzdC5jb20vc2NhM2EuY2VyMCgGA1UdEQQhMB+CHXJldm9r\n" +
            "ZWQuc2NhM2EuYW1hem9udHJ1c3QuY29tMBMGA1UdIAQMMAowCAYGZ4EMAQIBMAoG\n" +
            "CCqGSM49BAMCA0gAMEUCICLb16/50S4fOAFafi5lagdx7q6EDPPm596g19eQDMXk\n" +
            "AiEAksCMLypRB4t30FABlsEjhVCBIxay0iIer2OcCIrhfEI=\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // EE certificates don't have CRLDP extension
        if (!ocspEnabled){
            pathValidator.validate(new String[]{INT},
                    ValidatePathWithParams.Status.GOOD, null, System.out);

            return;
        }

        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Mon Jan 28 15:40:35 PST 2019", System.out);
    }
}

class AmazonCA_4 {

    // Owner: CN=Amazon, OU=Server CA 4A, O=Amazon, C=US
    // Issuer: CN=Amazon Root CA 4, O=Amazon, C=US
    // Serial number: 67f94575a8862a9072e3239c37ceba1274e18
    // Valid from: Wed Oct 21 17:00:00 PDT 2015 until: Sat Oct 18 17:00:00 PDT 2025
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIC+TCCAn6gAwIBAgITBn+UV1qIYqkHLjI5w3zroSdOGDAKBggqhkjOPQQDAzA5\n" +
            "MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRkwFwYDVQQDExBBbWF6b24g\n" +
            "Um9vdCBDQSA0MB4XDTE1MTAyMjAwMDAwMFoXDTI1MTAxOTAwMDAwMFowRjELMAkG\n" +
            "A1UEBhMCVVMxDzANBgNVBAoTBkFtYXpvbjEVMBMGA1UECxMMU2VydmVyIENBIDRB\n" +
            "MQ8wDQYDVQQDEwZBbWF6b24wdjAQBgcqhkjOPQIBBgUrgQQAIgNiAASRP0kIW0Ha\n" +
            "7+ORvEVhIS5gIgkH66X5W9vBRTX14oG/1elIyI6LbFZ+E5KAufL0XoWJGI1WbPRm\n" +
            "HW246FKSzF0wOEZZyxEROz6tuaVsnXRHRE76roS/Wr064uJpKH+Lv+SjggE5MIIB\n" +
            "NTASBgNVHRMBAf8ECDAGAQH/AgEAMA4GA1UdDwEB/wQEAwIBhjAdBgNVHQ4EFgQU\n" +
            "pSHN2+tTIZmqytlnQpQlsnv0wuMwHwYDVR0jBBgwFoAU0+zHOmVuzOHadppW+5zz\n" +
            "hm1X5YEwewYIKwYBBQUHAQEEbzBtMC8GCCsGAQUFBzABhiNodHRwOi8vb2NzcC5y\n" +
            "b290Y2E0LmFtYXpvbnRydXN0LmNvbTA6BggrBgEFBQcwAoYuaHR0cDovL2NydC5y\n" +
            "b290Y2E0LmFtYXpvbnRydXN0LmNvbS9yb290Y2E0LmNlcjA/BgNVHR8EODA2MDSg\n" +
            "MqAwhi5odHRwOi8vY3JsLnJvb3RjYTQuYW1hem9udHJ1c3QuY29tL3Jvb3RjYTQu\n" +
            "Y3JsMBEGA1UdIAQKMAgwBgYEVR0gADAKBggqhkjOPQQDAwNpADBmAjEA59RAOBaj\n" +
            "uh0rT/OOTWPEv6TBnb9XEadburBaXb8SSrR8il+NdkfS9WXRAzbwrG7LAjEA3ukD\n" +
            "1HrQq+WXHBM5sIuViJI/Zh7MOjsc159Q+dn36PBqLRq03AXqE/lRjnv8C5nj\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=good.sca4a.amazontrust.com, O=Amazon Trust Services, L=Seattle, ST=Washington, C=US, \
    // SERIALNUMBER=5846743, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.2=Delaware, \
    // OID.1.3.6.1.4.1.311.60.2.1.3=US
    // Issuer: CN=Amazon, OU=Server CA 4A, O=Amazon, C=US
    // Serial number: 703e4ec57c72d5669efbc98875c3f6bc3f934
    // Valid from: Mon Jul 29 16:55:17 PDT 2019 until: Sat Aug 29 16:55:17 PDT 2020
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDxTCCA0qgAwIBAgITBwPk7FfHLVZp77yYh1w/a8P5NDAKBggqhkjOPQQDAzBG\n" +
            "MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRUwEwYDVQQLEwxTZXJ2ZXIg\n" +
            "Q0EgNEExDzANBgNVBAMTBkFtYXpvbjAeFw0xOTA3MjkyMzU1MTdaFw0yMDA4Mjky\n" +
            "MzU1MTdaMIHaMRMwEQYLKwYBBAGCNzwCAQMTAlVTMRkwFwYLKwYBBAGCNzwCAQIT\n" +
            "CERlbGF3YXJlMR0wGwYDVQQPExRQcml2YXRlIE9yZ2FuaXphdGlvbjEQMA4GA1UE\n" +
            "BRMHNTg0Njc0MzELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAO\n" +
            "BgNVBAcTB1NlYXR0bGUxHjAcBgNVBAoTFUFtYXpvbiBUcnVzdCBTZXJ2aWNlczEj\n" +
            "MCEGA1UEAxMaZ29vZC5zY2E0YS5hbWF6b250cnVzdC5jb20wdjAQBgcqhkjOPQIB\n" +
            "BgUrgQQAIgNiAAS9fqMYfOBsdXMSsPjqOlTgIGOlOQWA7Wg6XwVvHTr0+UN+XTeC\n" +
            "yZN+XjLbEDQ0CF5eryRZ535sDpwh3qNe0lYFO1n1+2iDtDI1jhhLNYNxBpVnR2BU\n" +
            "2l9EuRmgRbQpDCajggFjMIIBXzAOBgNVHQ8BAf8EBAMCB4AwHQYDVR0OBBYEFMd0\n" +
            "itH5IcE6DpM1uTSBV/6DLmK7MB8GA1UdIwQYMBaAFKUhzdvrUyGZqsrZZ0KUJbJ7\n" +
            "9MLjMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjB1BggrBgEFBQcBAQRp\n" +
            "MGcwLQYIKwYBBQUHMAGGIWh0dHA6Ly9vY3NwLnNjYTRhLmFtYXpvbnRydXN0LmNv\n" +
            "bTA2BggrBgEFBQcwAoYqaHR0cDovL2NydC5zY2E0YS5hbWF6b250cnVzdC5jb20v\n" +
            "c2NhNGEuY2VyMCUGA1UdEQQeMByCGmdvb2Quc2NhNGEuYW1hem9udHJ1c3QuY29t\n" +
            "MFAGA1UdIARJMEcwDQYLYIZIAYb9bgEHGAMwNgYFZ4EMAQEwLTArBggrBgEFBQcC\n" +
            "ARYfaHR0cHM6Ly93d3cuYW1hem9udHJ1c3QuY29tL2NwczAKBggqhkjOPQQDAwNp\n" +
            "ADBmAjEA2RBD1F+rnm394VkqA3ncysM3deoyfWqaoAO5923MNisswPnHfVqnfeXf\n" +
            "ZwTAvVTBAjEAiiaPx9GRjEk8IBKvCSbTp9rPogVTN7zDDQGrwA83O0pRP7A0dxtT\n" +
            "pn/0K5Sj8otp\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=revoked.sca4a.amazontrust.com, O=Amazon Trust Services, L=Seattle, ST=Washington, C=US, \
    // SERIALNUMBER=5846743, OID.2.5.4.15=PrivateOrganization, OID.1.3.6.1.4.1.311.60.2.1.2=Delaware, \
    // OID.1.3.6.1.4.1.311.60.2.1.3=US
    // Issuer: CN=Amazon, OU=Server CA 4A, O=Amazon, C=US
    // Serial number: 6f1d79295c384a699d51c2d756bd46213b5b3
    // Valid from: Mon Jan 28 15:41:16 PST 2019 until: Thu Apr 28 16:41:16 PDT 2022
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDjTCCAxKgAwIBAgITBvHXkpXDhKaZ1RwtdWvUYhO1szAKBggqhkjOPQQDAzBG\n" +
            "MQswCQYDVQQGEwJVUzEPMA0GA1UEChMGQW1hem9uMRUwEwYDVQQLEwxTZXJ2ZXIg\n" +
            "Q0EgNEExDzANBgNVBAMTBkFtYXpvbjAeFw0xOTAxMjgyMzQxMTZaFw0yMjA0Mjgy\n" +
            "MzQxMTZaMIHcMRMwEQYLKwYBBAGCNzwCAQMTAlVTMRkwFwYLKwYBBAGCNzwCAQIT\n" +
            "CERlbGF3YXJlMRwwGgYDVQQPExNQcml2YXRlT3JnYW5pemF0aW9uMRAwDgYDVQQF\n" +
            "Ewc1ODQ2NzQzMQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4G\n" +
            "A1UEBxMHU2VhdHRsZTEeMBwGA1UEChMVQW1hem9uIFRydXN0IFNlcnZpY2VzMSYw\n" +
            "JAYDVQQDEx1yZXZva2VkLnNjYTRhLmFtYXpvbnRydXN0LmNvbTB2MBAGByqGSM49\n" +
            "AgEGBSuBBAAiA2IABLuNpZTcNU3FElNP3Y/OeXIZcIMXkFTBi/n92fNwHfqUbEhH\n" +
            "H+PovJ26eAGvb5a8bGc275MBFcVnWL0rCVgM+j9KAtBDCRJX3f7mo0D2VKcmtZKu\n" +
            "jPxwGPy2kuqM505dGqOCASkwggElMA4GA1UdDwEB/wQEAwIHgDAdBgNVHQ4EFgQU\n" +
            "zUFIhn+hphzCKA2qgAdLztSBzJgwHwYDVR0jBBgwFoAUpSHN2+tTIZmqytlnQpQl\n" +
            "snv0wuMwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMHUGCCsGAQUFBwEB\n" +
            "BGkwZzAtBggrBgEFBQcwAYYhaHR0cDovL29jc3Auc2NhNGEuYW1hem9udHJ1c3Qu\n" +
            "Y29tMDYGCCsGAQUFBzAChipodHRwOi8vY3J0LnNjYTRhLmFtYXpvbnRydXN0LmNv\n" +
            "bS9zY2E0YS5jZXIwKAYDVR0RBCEwH4IdcmV2b2tlZC5zY2E0YS5hbWF6b250cnVz\n" +
            "dC5jb20wEwYDVR0gBAwwCjAIBgZngQwBAgEwCgYIKoZIzj0EAwMDaQAwZgIxALDA\n" +
            "klY3iKwyzwpwVtLfLxzQEl45xvE2VjBJvfJJ60KhJt7Ud0gt0zxkogh29+mpEQIx\n" +
            "ANTG1mk8OJB41DU7ru1Pwc6ju8STw1FdwDp/Eliqhvnm2i0k4/F1bBHLta2mlC2V\n" +
            "hg==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator, boolean ocspEnabled) throws Exception {
        // EE certificates don't have CRLDP extension
        if (!ocspEnabled){
            pathValidator.validate(new String[]{INT},
                    ValidatePathWithParams.Status.GOOD, null, System.out);

            return;
        }

        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Mon Jan 28 15:41:53 PST 2019", System.out);
    }
}
