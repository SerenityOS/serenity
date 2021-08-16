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
 * @bug 8189131 8207059
 * @summary Interoperability tests with QuoVadis Root CA1, CA2, and CA3 CAs
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath QuoVadisCA OCSP
 * @run main/othervm -Djava.security.debug=certpath QuoVadisCA CRL
 */

/*
 * Obtain TLS test artifacts for QuoVadis CAs from:
 *
 * https://www.quovadisglobal.com/download-roots-crl/
 *
 */
public class QuoVadisCA {

    public static void main(String[] args) throws Exception {

        ValidatePathWithParams pathValidator = new ValidatePathWithParams(null);

        if (args.length >= 1 && "CRL".equalsIgnoreCase(args[0])) {
            pathValidator.enableCRLCheck();
        } else {
            // OCSP check by default
            pathValidator.enableOCSPCheck();
        }

        new RootCA1G3().runTest(pathValidator);
        new RootCA2G3().runTest(pathValidator);
        new RootCA3G3().runTest(pathValidator);
    }
}

class RootCA1G3 {

    // Owner: CN=DigiCert QuoVadis TLS ICA QV Root CA 1 G3, O="DigiCert, Inc", C=US
    // Issuer: CN=QuoVadis Root CA 1 G3, O=QuoVadis Limited, C=BM
    // Serial number: 2837d5c3c2b57294becf99afe8bbdcd1bb0b20f1
    // Valid from: Wed Jan 06 12:50:51 PST 2021 until: Sat Jan 04 12:50:51 PST 2031
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFgDCCA2igAwIBAgIUKDfVw8K1cpS+z5mv6Lvc0bsLIPEwDQYJKoZIhvcNAQEL\n" +
            "BQAwSDELMAkGA1UEBhMCQk0xGTAXBgNVBAoTEFF1b1ZhZGlzIExpbWl0ZWQxHjAc\n" +
            "BgNVBAMTFVF1b1ZhZGlzIFJvb3QgQ0EgMSBHMzAeFw0yMTAxMDYyMDUwNTFaFw0z\n" +
            "MTAxMDQyMDUwNTFaMFkxCzAJBgNVBAYTAlVTMRYwFAYDVQQKDA1EaWdpQ2VydCwg\n" +
            "SW5jMTIwMAYDVQQDDClEaWdpQ2VydCBRdW9WYWRpcyBUTFMgSUNBIFFWIFJvb3Qg\n" +
            "Q0EgMSBHMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALMrbkb9kz/4\n" +
            "y00r7tfK+uDRomMNd5iCDVMWOvSx1VygKoBn3aavw7gq9Vfb2fIMIWkWG0GMxWbG\n" +
            "cx3wDHLWemd7yl9MxRUTGXkvH6/dNEavAQhUTL9TSf/N2e8f7q2dRDNYT7lXi/vR\n" +
            "fTBiYlY7BLNha8C3sPHsKduaJN32cjdjVFH51rFDRdhUXlo2hhOjgB6bqoqs75A3\n" +
            "Y3w88AdbMkapT63oGsCDO6N/uX2Mo9GSWREvlxHiXSMFf5qFw41vn5QIa5ADL1MP\n" +
            "CzlLmJSHXE138H1+cG5IutD7tIieKjo/t+66PGMo8xicj3yUd8rHEmBqClG4Ty3d\n" +
            "fF+bETFjLIUCAwEAAaOCAU8wggFLMBIGA1UdEwEB/wQIMAYBAf8CAQAwHwYDVR0j\n" +
            "BBgwFoAUo5fW816iEOGrRZ88F2Q87gFwnMwwdAYIKwYBBQUHAQEEaDBmMDgGCCsG\n" +
            "AQUFBzAChixodHRwOi8vdHJ1c3QucXVvdmFkaXNnbG9iYWwuY29tL3F2cmNhMWcz\n" +
            "LmNydDAqBggrBgEFBQcwAYYeaHR0cDovL29jc3AucXVvdmFkaXNnbG9iYWwuY29t\n" +
            "MBMGA1UdIAQMMAowCAYGZ4EMAQICMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEF\n" +
            "BQcDATA7BgNVHR8ENDAyMDCgLqAshipodHRwOi8vY3JsLnF1b3ZhZGlzZ2xvYmFs\n" +
            "LmNvbS9xdnJjYTFnMy5jcmwwHQYDVR0OBBYEFJkRfemwrS1iWnDTPI2HIK3a2i5B\n" +
            "MA4GA1UdDwEB/wQEAwIBhjANBgkqhkiG9w0BAQsFAAOCAgEAb6tTptzzi4ssb+jA\n" +
            "n2O2vAjAo7ydlfN9v+QH0ZuGHlUc9bm8dpNpBo9yt6fWHIprGLJjVOF7HwVDQcJD\n" +
            "DhX4638Q7ETDrbTVQ4/edX6Yesq6C1G8Pza1LwStXD/jCQHFvWbPud86V0ikS4rS\n" +
            "qlmu3fzUrGZ2/Q+n5jrnRqM5IS8TXYcnzLD3azH1+aZjkwQt9HP4IuvAe/Bg9aWE\n" +
            "XeDmksbg0SqQInrWn+BVYtD+hCZNz8K0GnKKpx3Q9VxzRv+BMbO5e9iqK1Hcj5Wv\n" +
            "ZXvU45j2r5y9WML4fc8CvphzbF6ezr1e51i+yabNmfld33gRX48V5oNk16wX32ed\n" +
            "kQ83sKNomQm1dXURWK8aSDcZFAvJQ8vKTLIE9wiQmtjfSGoJzQhKLaN+egrp4L9y\n" +
            "fjpFIeK4zgAH39P4s4kaPWTdfXe2n6P5o7Xolp4R22SVkI76d8d+5Iv7Rtqd+mqI\n" +
            "y1hkwyTBbOBLtyF7yMtJQewkkZ0MWxkPvWg193RbYVRx8w1EycnxMgNwy2sJw7MR\n" +
            "XM6Mihkw910BkvlbsFUXw4uSvRkkRWSBWVrkM5hvZGtbIJkqrdnj55RSk4DLOOT/\n" +
            "LUyji/KpgD7YCi7emFA4tH6OpkNrjUJ3gdRnD4GwQj/87tYeoQWZ6uCl0MHDUCmw\n" +
            "73bpxSkjPrYbmKo9mGEAMhW1ZxY=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=quovadis-root-ca-1-g3.chain-demos.digicert.com, O="DigiCert, Inc.", L=Lehi, ST=Utah, C=US
    // Issuer: CN=DigiCert QuoVadis TLS ICA QV Root CA 1 G3, O="DigiCert, Inc", C=US
    // Serial number: 2b9bf892d8abbde06c26d764263bfa8
    // Valid from: Tue Jan 19 16:00:00 PST 2021 until: Sun Feb 20 15:59:59 PST 2022
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGhjCCBW6gAwIBAgIQArm/iS2Ku94Gwm12QmO/qDANBgkqhkiG9w0BAQsFADBZ\n" +
            "MQswCQYDVQQGEwJVUzEWMBQGA1UECgwNRGlnaUNlcnQsIEluYzEyMDAGA1UEAwwp\n" +
            "RGlnaUNlcnQgUXVvVmFkaXMgVExTIElDQSBRViBSb290IENBIDEgRzMwHhcNMjEw\n" +
            "MTIwMDAwMDAwWhcNMjIwMjIwMjM1OTU5WjB9MQswCQYDVQQGEwJVUzENMAsGA1UE\n" +
            "CBMEVXRhaDENMAsGA1UEBxMETGVoaTEXMBUGA1UEChMORGlnaUNlcnQsIEluYy4x\n" +
            "NzA1BgNVBAMTLnF1b3ZhZGlzLXJvb3QtY2EtMS1nMy5jaGFpbi1kZW1vcy5kaWdp\n" +
            "Y2VydC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDUqAk3cOpq\n" +
            "E+70B1WMI5R3QEqbrF4TE9jpXFIy2HQV60HSyxrsh2xDpU/zD0BF2mwOGZvQKc1F\n" +
            "JcVpwWXJ4vjS/lqDvZBNlzDejr4sQbzZVGFkqP6wu+KrAa+Eu7Lvayke3vi99Ba3\n" +
            "MmcauuHQsLwh/A+nLzUZIL56HKREDA6YmvF2Bkz6La+f3y4BhiFcQLWQOSr4LgvD\n" +
            "7qqdtd550sT4agIh6G1C3CDgUU2OYX8d70YOokO+msZXppD2le5HJDAUwrEKXB/L\n" +
            "3+jxA2wxgxmGzWigkRFl7UlsuLezUZKIHj8YoQu/0LGLZZUcW0OEHj5sspifmQJ7\n" +
            "DccQrGWyqU6XAgMBAAGjggMkMIIDIDAfBgNVHSMEGDAWgBSZEX3psK0tYlpw0zyN\n" +
            "hyCt2touQTAdBgNVHQ4EFgQUDBReNH49ishzOagWgVaxd7nSyrgwOQYDVR0RBDIw\n" +
            "MIIucXVvdmFkaXMtcm9vdC1jYS0xLWczLmNoYWluLWRlbW9zLmRpZ2ljZXJ0LmNv\n" +
            "bTAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMC\n" +
            "MIGXBgNVHR8EgY8wgYwwRKBCoECGPmh0dHA6Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9E\n" +
            "aWdpQ2VydFF1b1ZhZGlzVExTSUNBUVZSb290Q0ExRzMuY3JsMESgQqBAhj5odHRw\n" +
            "Oi8vY3JsNC5kaWdpY2VydC5jb20vRGlnaUNlcnRRdW9WYWRpc1RMU0lDQVFWUm9v\n" +
            "dENBMUczLmNybDA+BgNVHSAENzA1MDMGBmeBDAECAjApMCcGCCsGAQUFBwIBFhto\n" +
            "dHRwOi8vd3d3LmRpZ2ljZXJ0LmNvbS9DUFMwgYMGCCsGAQUFBwEBBHcwdTAkBggr\n" +
            "BgEFBQcwAYYYaHR0cDovL29jc3AuZGlnaWNlcnQuY29tME0GCCsGAQUFBzAChkFo\n" +
            "dHRwOi8vY2FjZXJ0cy5kaWdpY2VydC5jb20vRGlnaUNlcnRRdW9WYWRpc1RMU0lD\n" +
            "QVFWUm9vdENBMUczLmNydDAMBgNVHRMBAf8EAjAAMIIBBAYKKwYBBAHWeQIEAgSB\n" +
            "9QSB8gDwAHUAKXm+8J45OSHwVnOfY6V35b5XfZxgCvj5TV0mXCVdx4QAAAF3IHgp\n" +
            "NQAABAMARjBEAiBNOisRzreFHgpQYJmdzN8pKAGcerQHCWfeWeWyDDD8hQIgP2sh\n" +
            "8KGW4sOt/BqEUNhGdD+ZAbv2+xrT57aSRONrG/UAdwAiRUUHWVUkVpY/oS/x922G\n" +
            "4CMmY63AS39dxoNcbuIPAgAAAXcgeCmLAAAEAwBIMEYCIQCKVxPY+JbJX4IMcbVc\n" +
            "HypoqNyozayPjzuHPZXsr5sZfAIhAIdS8wHmUZz6w5Frqa/CTUSugoNgx9H0K9dN\n" +
            "Cl6gsvdcMA0GCSqGSIb3DQEBCwUAA4IBAQB35ONSVAMAhFpTwQl6HEFEmhh++JaF\n" +
            "3WLMUODAa79N/GXe0JMvPuTbLsB/H1WXP/27D9ImvWlqLQU3EgqRp0fTdWCnnP4q\n" +
            "8FKsKslnKPoiqNyTjpKHWk+gugFAyAAUXEFP4QYSMgye7kBNQZoX9MX4LhPBtqTa\n" +
            "cPXLprb/gZMrNbbf9FdOGQXQgpFoaGnvyGw2oGU1Spb+Tx1QbK8NUWCdc8KNo1WB\n" +
            "XH37EY8XJJxdjjoocLlFxRBUothYEUk+HdE5MGNqF3egtFKVvDk2OC8redCQ6dDC\n" +
            "MKkGc7Avhn43gkRpzuush9Ph79aT1lf4Mm4Rs0FSYm8e62bQspH8UTdh\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=quovadis-root-ca-1-g3-revoked.chain-demos.digicert.com, O="DigiCert, Inc.", L=Lehi, ST=Utah, C=US
    // Issuer: CN=DigiCert QuoVadis TLS ICA QV Root CA 1 G3, O="DigiCert, Inc", C=US
    // Serial number: 8fb833dd0472302df1ddc134d36840d
    // Valid from: Tue Jan 19 16:00:00 PST 2021 until: Sun Feb 20 15:59:59 PST 2022
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIG1TCCBb2gAwIBAgIQCPuDPdBHIwLfHdwTTTaEDTANBgkqhkiG9w0BAQsFADBZ\n" +
            "MQswCQYDVQQGEwJVUzEWMBQGA1UECgwNRGlnaUNlcnQsIEluYzEyMDAGA1UEAwwp\n" +
            "RGlnaUNlcnQgUXVvVmFkaXMgVExTIElDQSBRViBSb290IENBIDEgRzMwHhcNMjEw\n" +
            "MTIwMDAwMDAwWhcNMjIwMjIwMjM1OTU5WjCBhTELMAkGA1UEBhMCVVMxDTALBgNV\n" +
            "BAgTBFV0YWgxDTALBgNVBAcTBExlaGkxFzAVBgNVBAoTDkRpZ2lDZXJ0LCBJbmMu\n" +
            "MT8wPQYDVQQDEzZxdW92YWRpcy1yb290LWNhLTEtZzMtcmV2b2tlZC5jaGFpbi1k\n" +
            "ZW1vcy5kaWdpY2VydC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB\n" +
            "AQC/YHOKNPYW2a/aji68VN4EYwz5ANxRuCSHXOe6mg8pYaelrnky3hmNT3Ro86wc\n" +
            "3UaXE07+fVGjXbbe4eRF5zj1pWsMe81n7NySoLud5C9CU/3okywv6SEPc/RiFKJk\n" +
            "ic/BpRTE2pMtFfGsqWALjyDhM+W7SvM5eAXqcVVnqHgO5oToe2Cd0IqhgTgOUDdm\n" +
            "+JuY/kY8gRmFyzfs4Oqe8nkgrdsvsyvRtQt1ldc9XKb+C/gnl8ZMiuLpuKWqezce\n" +
            "2ED0SW4bVDbaDIhf97CNFeuMHHyZzI7Z0DH29WDcACOVBqVhGJl56T89COM3ISle\n" +
            "WpYCOUgyzlPVODisfq/1zgepAgMBAAGjggNqMIIDZjAfBgNVHSMEGDAWgBSZEX3p\n" +
            "sK0tYlpw0zyNhyCt2touQTAdBgNVHQ4EFgQUdZf0H3+QjsIMtfpwZUf3YNaVBQ0w\n" +
            "fQYDVR0RBHYwdII2cXVvdmFkaXMtcm9vdC1jYS0xLWczLXJldm9rZWQuY2hhaW4t\n" +
            "ZGVtb3MuZGlnaWNlcnQuY29tgjp3d3cucXVvdmFkaXMtcm9vdC1jYS0xLWczLXJl\n" +
            "dm9rZWQuY2hhaW4tZGVtb3MuZGlnaWNlcnQuY29tMA4GA1UdDwEB/wQEAwIFoDAd\n" +
            "BgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgZcGA1UdHwSBjzCBjDBEoEKg\n" +
            "QIY+aHR0cDovL2NybDMuZGlnaWNlcnQuY29tL0RpZ2lDZXJ0UXVvVmFkaXNUTFNJ\n" +
            "Q0FRVlJvb3RDQTFHMy5jcmwwRKBCoECGPmh0dHA6Ly9jcmw0LmRpZ2ljZXJ0LmNv\n" +
            "bS9EaWdpQ2VydFF1b1ZhZGlzVExTSUNBUVZSb290Q0ExRzMuY3JsMD4GA1UdIAQ3\n" +
            "MDUwMwYGZ4EMAQICMCkwJwYIKwYBBQUHAgEWG2h0dHA6Ly93d3cuZGlnaWNlcnQu\n" +
            "Y29tL0NQUzCBgwYIKwYBBQUHAQEEdzB1MCQGCCsGAQUFBzABhhhodHRwOi8vb2Nz\n" +
            "cC5kaWdpY2VydC5jb20wTQYIKwYBBQUHMAKGQWh0dHA6Ly9jYWNlcnRzLmRpZ2lj\n" +
            "ZXJ0LmNvbS9EaWdpQ2VydFF1b1ZhZGlzVExTSUNBUVZSb290Q0ExRzMuY3J0MAwG\n" +
            "A1UdEwEB/wQCMAAwggEGBgorBgEEAdZ5AgQCBIH3BIH0APIAdwApeb7wnjk5IfBW\n" +
            "c59jpXflvld9nGAK+PlNXSZcJV3HhAAAAXcgeVnsAAAEAwBIMEYCIQCBIKm+1buq\n" +
            "Ws6nDYt1OMGxHa7yaxwQu5yaWCq6wk3kXQIhAL0ImrZZWp0Uq2i3QhDVjAfsP9lF\n" +
            "LRf2f0qbqJUXzTV7AHcAIkVFB1lVJFaWP6Ev8fdthuAjJmOtwEt/XcaDXG7iDwIA\n" +
            "AAF3IHlaMwAABAMASDBGAiEAm+RVoIBHAiMN+m/fuo65CLCwTRa/YVrP8wjUT4pM\n" +
            "5toCIQDhV6pbmxbwGl9rM56ypsKZLJeB4LeCWqJmb7yf61E9WzANBgkqhkiG9w0B\n" +
            "AQsFAAOCAQEAh82J/F2rBZ6GUFmOdIS8mhzO/ItkpAX+bta5lyPasCgP3JEmHONr\n" +
            "HBX1UIvKYo7/McgFBt1V90ncQXfuutIyh0hPXSbTqK/b7XCVarh9J0ZXp8gLstQ5\n" +
            "39DiuiS+qWBSc8C14VIOjeTblTBM6hjvkdnYhTZt2ip3aKYu8sHyC+3DpAYuD2og\n" +
            "Lzs8Q7wvPwvBE2HMsr6c8dFjC9S8HZvrfJTwuh/7QhafueuwPEfr6a2/TExF/3B1\n" +
            "QXxgYPg7pQ2gPUqucO47FcEhr5GHzTwNoGAropkFj3g0byEt0i+EwLMjdAlDABzP\n" +
            "CFww1aRfNklbdRBZ/rr7QzmGtn39JzZKMQ==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator)
            throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Wed Jan 20 07:46:23 PST 2021", System.out);
    }
}

class RootCA2G3 {

    // Owner: CN=DigiCert QV EV TLS ICA G1, O="DigiCert, Inc.", C=US
    // Issuer: CN=QuoVadis Root CA 2 G3, O=QuoVadis Limited, C=BM
    // Serial number: 65e9bcd53e791df22dffeb5ecc2bc7a5588d0883
    // Valid from: Mon Mar 16 12:39:42 PDT 2020 until: Thu Mar 14 12:39:42 PDT 2030
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFbzCCA1egAwIBAgIUZem81T55HfIt/+tezCvHpViNCIMwDQYJKoZIhvcNAQEL\n" +
            "BQAwSDELMAkGA1UEBhMCQk0xGTAXBgNVBAoTEFF1b1ZhZGlzIExpbWl0ZWQxHjAc\n" +
            "BgNVBAMTFVF1b1ZhZGlzIFJvb3QgQ0EgMiBHMzAeFw0yMDAzMTYxOTM5NDJaFw0z\n" +
            "MDAzMTQxOTM5NDJaMEoxCzAJBgNVBAYTAlVTMRcwFQYDVQQKDA5EaWdpQ2VydCwg\n" +
            "SW5jLjEiMCAGA1UEAwwZRGlnaUNlcnQgUVYgRVYgVExTIElDQSBHMTCCASIwDQYJ\n" +
            "KoZIhvcNAQEBBQADggEPADCCAQoCggEBAMhwn6I+pGrJsnisnzP7EU5cFN9UT5XF\n" +
            "auA13F3jHeUUZmBOcMSOJEhx/e7oeVScTnmKpe7t7uey7lIIC9DWFmP8klbtLBgL\n" +
            "0jY4MPlCkVyxUIhZ73EHCPqDCX9bo+rMB6C758/tKZOPcoWRixQypPwoC4cXNOOk\n" +
            "ntqFPRxFSZoBdTDNlAmkAQJCRsXGCEC5pZ0JqzGcAA0/Pw1fB8lSPAti3trubYmd\n" +
            "aaPFAKzGK7vsexxpuSUKO0opNkFWbLdHZ8jkr86R80oo1vhURJXWNeMS74ws5nbt\n" +
            "Ll9sJTDW33MQPS0/JO3xYI7bQcW3K1sPSERa4BahqgOJvEXMk1eWRcUCAwEAAaOC\n" +
            "AU0wggFJMBIGA1UdEwEB/wQIMAYBAf8CAQAwHwYDVR0jBBgwFoAU7edvdlq/YOxJ\n" +
            "W8ald7tyFnGbxD0wOgYIKwYBBQUHAQEELjAsMCoGCCsGAQUFBzABhh5odHRwOi8v\n" +
            "b2NzcC5xdW92YWRpc2dsb2JhbC5jb20wSwYDVR0gBEQwQjAHBgVngQwBATA3Bglg\n" +
            "hkgBhv1sAgEwKjAoBggrBgEFBQcCARYcaHR0cHM6Ly93d3cuZGlnaWNlcnQuY29t\n" +
            "L0NQUzAdBgNVHSUEFjAUBggrBgEFBQcDAgYIKwYBBQUHAwEwOwYDVR0fBDQwMjAw\n" +
            "oC6gLIYqaHR0cDovL2NybC5xdW92YWRpc2dsb2JhbC5jb20vcXZyY2EyZzMuY3Js\n" +
            "MB0GA1UdDgQWBBQTL6fobnFR9uIMmEeDnn+deHk08zAOBgNVHQ8BAf8EBAMCAYYw\n" +
            "DQYJKoZIhvcNAQELBQADggIBAEoOxze3kgnR39LX8M63EjiNxx0LThZHROqYqev6\n" +
            "5ox/c5NNitk8/ODA8osdPpvnUBAlmE0+gqBvnTBRPVrJFd9bOr5BK8z6Os9/U0ed\n" +
            "c3UINkWLS05B7ChC9s6Zw1Vd/WlW08TQJ80GpvAIbEKcg8EO/DXPniHxC4cMtv1T\n" +
            "jtNeh98XiVgQXHL1FY+u/l413J8C4utKi4ZOQeCJDqvlSDzRsOi+tHsXrCJxnMWN\n" +
            "2QBgMGgdPW37zwf0EffoH0Gee3pTgg7I5SzmvBq0t5xRDfv4N0OdM/sN1mc5f3o7\n" +
            "0YCd9WXhyDCV5W2O8QIbrd42CK5k1rlM6gXwOyDmYY5CVAl1QeXEeRfDk/zNjU/1\n" +
            "+LnH/Dv88VcZhODYq+VGbyM8bpNr0v95PY3yaH4kzpWGqWAN5i9LosfcaqRPmyL4\n" +
            "PcKTQwcA9AVTjITExFua/QtGrXLPvMVxR248G9IQpJMxP3JEGkjlKCenmc29r2u1\n" +
            "KE4TeCs2xxjR1PusTfX91bBW3YAoAPDTRQKZjolegLUY44j3uKSzAdhMEbZQhovH\n" +
            "Lraqx1WjTayTuq1Vuakcia5shmgFVSNcE+NVgLEIe32oTOm/G6Kd1lcm9C4Ph1Cg\n" +
            "nfDuqohZrk76kJTk8poAY5aFCQHhVzbpSw3zooMGjjvWnkG+/DC6SZM8rKoOdKiB\n" +
            "cy+N\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=quovadis-root-ca-2-g3.chain-demos.digicert.com, O="DigiCert, Inc.", L=Lehi, ST=Utah, C=US,
    // SERIALNUMBER=5299537-0142, OID.1.3.6.1.4.1.311.60.2.1.2=Utah, OID.1.3.6.1.4.1.311.60.2.1.3=US,
    // OID.2.5.4.15=Private Organization
    // Issuer: CN=DigiCert QV EV TLS ICA G1, O="DigiCert, Inc.", C=US
    // Serial number: a54e525a61f2552f65885c6dce9fe21
    // Valid from: Mon Feb 01 16:00:00 PST 2021 until: Sat Mar 05 15:59:59 PST 2022
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGuzCCBaOgAwIBAgIQClTlJaYfJVL2WIXG3On+ITANBgkqhkiG9w0BAQsFADBK\n" +
            "MQswCQYDVQQGEwJVUzEXMBUGA1UECgwORGlnaUNlcnQsIEluYy4xIjAgBgNVBAMM\n" +
            "GURpZ2lDZXJ0IFFWIEVWIFRMUyBJQ0EgRzEwHhcNMjEwMjAyMDAwMDAwWhcNMjIw\n" +
            "MzA1MjM1OTU5WjCB3zEdMBsGA1UEDwwUUHJpdmF0ZSBPcmdhbml6YXRpb24xEzAR\n" +
            "BgsrBgEEAYI3PAIBAxMCVVMxFTATBgsrBgEEAYI3PAIBAhMEVXRhaDEVMBMGA1UE\n" +
            "BRMMNTI5OTUzNy0wMTQyMQswCQYDVQQGEwJVUzENMAsGA1UECBMEVXRhaDENMAsG\n" +
            "A1UEBxMETGVoaTEXMBUGA1UEChMORGlnaUNlcnQsIEluYy4xNzA1BgNVBAMTLnF1\n" +
            "b3ZhZGlzLXJvb3QtY2EtMi1nMy5jaGFpbi1kZW1vcy5kaWdpY2VydC5jb20wggEi\n" +
            "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDfknHK7boXh9ysZ0FLDQKEyT2x\n" +
            "Swtyecb5kkVgJU75XpXccV724mntCu5hpZ7Yt4tOmDZvbpYcqWbhIJgxGFNLyPdB\n" +
            "Fn8jgZ4N0WoD7u295HI9izEmbM0XrO2rvHUc6ZhFFyx0jhvJPf/k9QbQB4TwKZri\n" +
            "Iuf1E1Ek70DkTWAg6OrPHMe2ER3aSz2S2rNkMSopURvZuabzPovsGaz+XEZNfE4N\n" +
            "UfkBLa0DUjFCamOMZKIfkzxpH/NhQcigGnZgxiyUb6KRhu9ydpWeOvOHwPWwR/fV\n" +
            "7WT+X1DUHojoXeCk2RtIRMihDWPd+lqiUppM8IlEW/gxWbK1wP41qioiK9j5AgMB\n" +
            "AAGjggMFMIIDATAfBgNVHSMEGDAWgBQTL6fobnFR9uIMmEeDnn+deHk08zAdBgNV\n" +
            "HQ4EFgQUtAEN4g3bzwES6MoOINihiZQrt+owOQYDVR0RBDIwMIIucXVvdmFkaXMt\n" +
            "cm9vdC1jYS0yLWczLmNoYWluLWRlbW9zLmRpZ2ljZXJ0LmNvbTAOBgNVHQ8BAf8E\n" +
            "BAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMHsGA1UdHwR0MHIw\n" +
            "N6A1oDOGMWh0dHA6Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydFFWRVZUTFNJ\n" +
            "Q0FHMS5jcmwwN6A1oDOGMWh0dHA6Ly9jcmw0LmRpZ2ljZXJ0LmNvbS9EaWdpQ2Vy\n" +
            "dFFWRVZUTFNJQ0FHMS5jcmwwSgYDVR0gBEMwQTALBglghkgBhv1sAgEwMgYFZ4EM\n" +
            "AQEwKTAnBggrBgEFBQcCARYbaHR0cDovL3d3dy5kaWdpY2VydC5jb20vQ1BTMHYG\n" +
            "CCsGAQUFBwEBBGowaDAkBggrBgEFBQcwAYYYaHR0cDovL29jc3AuZGlnaWNlcnQu\n" +
            "Y29tMEAGCCsGAQUFBzAChjRodHRwOi8vY2FjZXJ0cy5kaWdpY2VydC5jb20vRGln\n" +
            "aUNlcnRRVkVWVExTSUNBRzEuY3J0MAwGA1UdEwEB/wQCMAAwggEEBgorBgEEAdZ5\n" +
            "AgQCBIH1BIHyAPAAdgApeb7wnjk5IfBWc59jpXflvld9nGAK+PlNXSZcJV3HhAAA\n" +
            "AXdkNiYhAAAEAwBHMEUCIQDg62+lrhAGdZcklHTEzYpVIki6v6fYgsbxmkTqV6M2\n" +
            "BgIgOcYjjM1DGT2FWFK5OO4Qs7T7V28neWwqbMPqLBZgRCoAdgAiRUUHWVUkVpY/\n" +
            "oS/x922G4CMmY63AS39dxoNcbuIPAgAAAXdkNiaKAAAEAwBHMEUCIQC0ZH0YX6BP\n" +
            "k4QAm22mVvXXG9EVBWIS5LP49f9VFhb67QIgfYqYV/wJt3czP5rA4NbJXBeFxZXy\n" +
            "DVnewfs2mz7kNOIwDQYJKoZIhvcNAQELBQADggEBAHIFaq9Edv37sXl07n3H93ym\n" +
            "AzvQKJGD1khWwHQCGBXVzpUBi2v9Il0oS2pTATGD3ZB81Clryy9X3gHgkJcHFRn0\n" +
            "cVr88GcyEWrxE8D6sx1X4WhlQknbKXNw8eL97EpZ4zKF1nDaYmf+e/lbmjnZIcdr\n" +
            "DRcMmWD6LZBWP8MMD+5uJvooO4eBYM/WKVRgg6jAj1HELBHi9E6Xwm5jPx3DKt6W\n" +
            "voJqcRJg3d4TiV8U/yOs8+514iAw+1YJFtAaxg5/eUQM/22BGXki8QCJ3NaaUGrj\n" +
            "UNASswMDevD8rVwZ5Bc2iC+c0WVIgHZepyioNC2XSVPovV9pF4Q3ppkSvEGQGSo=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=quovadis-root-ca-2-g3-revoked.chain-demos.digicert.com, O="DigiCert, Inc.", L=Lehi, ST=Utah, C=US,
    // SERIALNUMBER=5299537-0142, OID.1.3.6.1.4.1.311.60.2.1.2=Utah, OID.1.3.6.1.4.1.311.60.2.1.3=US,
    // OID.2.5.4.15=Private Organization
    // Issuer: CN=DigiCert QV EV TLS ICA G1, O="DigiCert, Inc.", C=US
    // Serial number: 27c8845dc98ebf433dc5ce8618570d1
    // Valid from: Mon Feb 01 16:00:00 PST 2021 until: Sat Mar 05 15:59:59 PST 2022
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGzDCCBbSgAwIBAgIQAnyIRdyY6/Qz3FzoYYVw0TANBgkqhkiG9w0BAQsFADBK\n" +
            "MQswCQYDVQQGEwJVUzEXMBUGA1UECgwORGlnaUNlcnQsIEluYy4xIjAgBgNVBAMM\n" +
            "GURpZ2lDZXJ0IFFWIEVWIFRMUyBJQ0EgRzEwHhcNMjEwMjAyMDAwMDAwWhcNMjIw\n" +
            "MzA1MjM1OTU5WjCB5zEdMBsGA1UEDwwUUHJpdmF0ZSBPcmdhbml6YXRpb24xEzAR\n" +
            "BgsrBgEEAYI3PAIBAxMCVVMxFTATBgsrBgEEAYI3PAIBAhMEVXRhaDEVMBMGA1UE\n" +
            "BRMMNTI5OTUzNy0wMTQyMQswCQYDVQQGEwJVUzENMAsGA1UECBMEVXRhaDENMAsG\n" +
            "A1UEBxMETGVoaTEXMBUGA1UEChMORGlnaUNlcnQsIEluYy4xPzA9BgNVBAMTNnF1\n" +
            "b3ZhZGlzLXJvb3QtY2EtMi1nMy1yZXZva2VkLmNoYWluLWRlbW9zLmRpZ2ljZXJ0\n" +
            "LmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL8H5MLxLmlUrb/C\n" +
            "+GndqTYyAWf7dlxmqK4bRtIQf6YIAum8sZwo7M+av3lNQAqRa9PsV4ms50lUSP6u\n" +
            "sFPAKINoOIu3ABT3qlSNb7ChxYCaZAiW3D1N4Jc9SU4Pa6tfPM3mSWc2HR7kSqj+\n" +
            "PtdDHgB8/wjgQwvGRFXTacM/dQVtd8cSLYGMaSgPTKqV87aps7gvd0a9jvJRPmzr\n" +
            "vJihL8K5MRiKTwXofT2s8/+/XMn0qWzWzBPe/bQuTT3Ot0Ee5u54FVPi0659rDtj\n" +
            "hDIf1M7GmSA/kuhcxI6vGusPZhrr+BbSbX8bRyXMPyE/V2qN2rGMVqDgo8CYP9oY\n" +
            "BXBocaECAwEAAaOCAw4wggMKMB8GA1UdIwQYMBaAFBMvp+hucVH24gyYR4Oef514\n" +
            "eTTzMB0GA1UdDgQWBBSxl5QSK4J6HfhClE1Vq1ElwnnNDzBBBgNVHREEOjA4gjZx\n" +
            "dW92YWRpcy1yb290LWNhLTItZzMtcmV2b2tlZC5jaGFpbi1kZW1vcy5kaWdpY2Vy\n" +
            "dC5jb20wDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEF\n" +
            "BQcDAjB7BgNVHR8EdDByMDegNaAzhjFodHRwOi8vY3JsMy5kaWdpY2VydC5jb20v\n" +
            "RGlnaUNlcnRRVkVWVExTSUNBRzEuY3JsMDegNaAzhjFodHRwOi8vY3JsNC5kaWdp\n" +
            "Y2VydC5jb20vRGlnaUNlcnRRVkVWVExTSUNBRzEuY3JsMEoGA1UdIARDMEEwCwYJ\n" +
            "YIZIAYb9bAIBMDIGBWeBDAEBMCkwJwYIKwYBBQUHAgEWG2h0dHA6Ly93d3cuZGln\n" +
            "aWNlcnQuY29tL0NQUzB2BggrBgEFBQcBAQRqMGgwJAYIKwYBBQUHMAGGGGh0dHA6\n" +
            "Ly9vY3NwLmRpZ2ljZXJ0LmNvbTBABggrBgEFBQcwAoY0aHR0cDovL2NhY2VydHMu\n" +
            "ZGlnaWNlcnQuY29tL0RpZ2lDZXJ0UVZFVlRMU0lDQUcxLmNydDAMBgNVHRMBAf8E\n" +
            "AjAAMIIBBQYKKwYBBAHWeQIEAgSB9gSB8wDxAHcAKXm+8J45OSHwVnOfY6V35b5X\n" +
            "fZxgCvj5TV0mXCVdx4QAAAF3ZDY16wAABAMASDBGAiEAxiHgWMmElnreF1ItPryX\n" +
            "1zHYYy8MswsWwPSLCRoNIh8CIQCoMhTfGdeb/YfgU7ADy0zo1ktLjnoWUXy57e2s\n" +
            "syevwAB2ACJFRQdZVSRWlj+hL/H3bYbgIyZjrcBLf13Gg1xu4g8CAAABd2Q2Nh0A\n" +
            "AAQDAEcwRQIhAIEUtUcXMIj/5+uNhqtRuzeppiC0dU9pcWuCkOqazlSsAiBeuAjH\n" +
            "z1EyH5zDDR2dJoVJekXsihpIoXUiClDNBgCNUjANBgkqhkiG9w0BAQsFAAOCAQEA\n" +
            "s/QeNaPZt8WftrnUqWI+neGyFlh6odWgpKxudOKVdvlGIUZ1bZlNzPcoHvdqgeVy\n" +
            "Vys3NxfSAX812lYo+IvPM9l3CmuaHkb7l/5O8bwnEPZuLd6EExp8BVYCIwfW+9cm\n" +
            "uyHXBJUEm1gkF2Utd8EYdf4jEcBCdDIXgNJI+DY05OlHE0E2IseQIHf9kttZzQHH\n" +
            "QEVtowJsaa2EksksBAmDDHH6ybyee7bZl8W2xkpbzvUvb6SuK+M/XyUjS9SYtun3\n" +
            "z82BhOhRMbpokpK2Eba6IeC7WoVX2Y8Wsqj198qTVvuyRiw2B3SdZarKV5VTySY3\n" +
            "aqsEyxenca5EjxU7QZDAHQ==\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator)
            throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Tue Feb 02 11:26:52 PST 2021", System.out);
    }
}

class RootCA3G3 {

    // Owner: CN=DigiCert QuoVadis TLS ICA QV Root CA 3 G3, O="DigiCert, Inc", C=US
    // Issuer: CN=QuoVadis Root CA 3 G3, O=QuoVadis Limited, C=BM
    // Serial number: 427dd33a8ff51d8152e813c7dec93ba76312a7d8
    // Valid from: Wed Jan 06 12:55:40 PST 2021 until: Sat Jan 04 12:55:40 PST 2031
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFgDCCA2igAwIBAgIUQn3TOo/1HYFS6BPH3sk7p2MSp9gwDQYJKoZIhvcNAQEL\n" +
            "BQAwSDELMAkGA1UEBhMCQk0xGTAXBgNVBAoTEFF1b1ZhZGlzIExpbWl0ZWQxHjAc\n" +
            "BgNVBAMTFVF1b1ZhZGlzIFJvb3QgQ0EgMyBHMzAeFw0yMTAxMDYyMDU1NDBaFw0z\n" +
            "MTAxMDQyMDU1NDBaMFkxCzAJBgNVBAYTAlVTMRYwFAYDVQQKDA1EaWdpQ2VydCwg\n" +
            "SW5jMTIwMAYDVQQDDClEaWdpQ2VydCBRdW9WYWRpcyBUTFMgSUNBIFFWIFJvb3Qg\n" +
            "Q0EgMyBHMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALxNTdqnFD+A\n" +
            "MhketYfVfVUWQKPkVEuyYj7Y2uwXBMRP4RStO4CoQih+hX/h94vRlObOIsqcNnyC\n" +
            "ElwBnLbmusaWYLYnDEWoROL8uN0pkWk0asfhhEsXTkAJ6FLHUD85WBkED4gIVWPi\n" +
            "Sp4AOwiA+/zpbwgVAgdjJTO3jjMsp4F1lBrdViYSwoPRACH1ZMjJG572oXTpZkQX\n" +
            "uWmEKLUOnik1i5cbqGLnwXiDvTAhxit7aBlj/C5IDvONWVQL34ZTYppvo8S3Hhy9\n" +
            "xX0S4HCpTpeBe3mas7VOrjsXNlEoFvejrxcQ+fB/gUf6fLUPxUhcPtm8keBPQuxc\n" +
            "qP12/+KG0WECAwEAAaOCAU8wggFLMBIGA1UdEwEB/wQIMAYBAf8CAQAwHwYDVR0j\n" +
            "BBgwFoAUxhfQvKjqAkPyGwaZXSuQILnXnOQwdAYIKwYBBQUHAQEEaDBmMDgGCCsG\n" +
            "AQUFBzAChixodHRwOi8vdHJ1c3QucXVvdmFkaXNnbG9iYWwuY29tL3F2cmNhM2cz\n" +
            "LmNydDAqBggrBgEFBQcwAYYeaHR0cDovL29jc3AucXVvdmFkaXNnbG9iYWwuY29t\n" +
            "MBMGA1UdIAQMMAowCAYGZ4EMAQICMB0GA1UdJQQWMBQGCCsGAQUFBwMCBggrBgEF\n" +
            "BQcDATA7BgNVHR8ENDAyMDCgLqAshipodHRwOi8vY3JsLnF1b3ZhZGlzZ2xvYmFs\n" +
            "LmNvbS9xdnJjYTNnMy5jcmwwHQYDVR0OBBYEFDNm+y+RBcyzYlLvzTz1fhzOpxeW\n" +
            "MA4GA1UdDwEB/wQEAwIBhjANBgkqhkiG9w0BAQsFAAOCAgEAY0ZuDgyM4I4MO9ll\n" +
            "D8qFUPQ8xtcGOuJgSRhDS2onIJ0M8yOGOYJCobIEGIgqyx94kI/n/1Xw+Wvsnhwb\n" +
            "OYOtVedx6VGDu6IuSKTVgPPhzwKP5ZA7wtmgKR8+W4E3DM1VerA9Po9ycDK9qCdl\n" +
            "K4tuF37grKEzlQKovG+kn0z+Zi0D/E1kN1Q8YmX35HHRenJWKEnAL9QROh0X9jFi\n" +
            "SlsHPrxWC3adOdAW+B+kVG0cM2nurd0Ic2YkiLKOOaSd5hbCQY/fCZwohtest+ZU\n" +
            "Ajyd+FVzSNvEFrwPzZwKfcdemvD4kew8lx5sG6BUL4GkFWnotxSr+F9Huwgj4pC+\n" +
            "cxE2841a/9r/gliuwDM/8jkt16epFAdw0fXemyM8FdHJDnB++3d8SyjOOQ8j+VHW\n" +
            "31NWx27sORa5CgRchlldXWDzIIEwbc82a1OAfGUmNAsdEHjMl1HMcZHbjCmdSdsw\n" +
            "fmyldZrj2YmvOI5ZlE9z4vzi35KyqlxWCtu9O/SJq/rBvYS0TPmm8HbhJQbeMe6p\n" +
            "vJGrxcb1muSBANn9T9wvukjiNNw32ciSDCjZ0h4N+CGxbzoZtgIAQ29IunYdnJix\n" +
            "ZiP+ED6xvwgVRBkDSgWD2W/hex/+z4fNmGQJDcri51/tZCqHHv2Y7XReuf4Fk+nP\n" +
            "l8Sd/Kpqwde/sJkoqwDcBSJygh0=\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=quovadis-root-ca-3-g3.chain-demos.digicert.com, O="DigiCert, Inc.", L=Lehi, ST=Utah, C=US
    // Issuer: CN=DigiCert QuoVadis TLS ICA QV Root CA 3 G3, O="DigiCert, Inc", C=US
    // Serial number: 19a05758f4ac7061ce9b3badd54d9b3
    // Valid from: Tue Jan 19 16:00:00 PST 2021 until: Sun Feb 20 15:59:59 PST 2022
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGhjCCBW6gAwIBAgIQAZoFdY9KxwYc6bO63VTZszANBgkqhkiG9w0BAQsFADBZ\n" +
            "MQswCQYDVQQGEwJVUzEWMBQGA1UECgwNRGlnaUNlcnQsIEluYzEyMDAGA1UEAwwp\n" +
            "RGlnaUNlcnQgUXVvVmFkaXMgVExTIElDQSBRViBSb290IENBIDMgRzMwHhcNMjEw\n" +
            "MTIwMDAwMDAwWhcNMjIwMjIwMjM1OTU5WjB9MQswCQYDVQQGEwJVUzENMAsGA1UE\n" +
            "CBMEVXRhaDENMAsGA1UEBxMETGVoaTEXMBUGA1UEChMORGlnaUNlcnQsIEluYy4x\n" +
            "NzA1BgNVBAMTLnF1b3ZhZGlzLXJvb3QtY2EtMy1nMy5jaGFpbi1kZW1vcy5kaWdp\n" +
            "Y2VydC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDCm/KON4FI\n" +
            "zslgCHcJkdamTiryUif9w+BD1xGb7W7hCo+JuLkMzDSH+gGAdwdjYK1NY5c0gFiw\n" +
            "Ud3OeHbNe7dZJSXhBP1oF3Xwl0f0DWIfa9nJObzQrN94PnYzDNGuFZut69dqzghO\n" +
            "1QGG/+dVdITvzqyA/Mqy7TUsZyoaG4OALTdOXAlFmeMf5UP7EDrAEC0dlUoYJnwK\n" +
            "0BeRNZDPYc/OsHviNa6/PnBli81C1QSrAT93STEAnWsCJrG8KlbSElYz2zwPwBZ7\n" +
            "ye1m8vH/K7npIIL0d/vSCxLUbC1pstOUahXvZE/Wl1zIV+JyCRmZS7sxTy6s5M4+\n" +
            "31Va8Y9jdbeBAgMBAAGjggMkMIIDIDAfBgNVHSMEGDAWgBQzZvsvkQXMs2JS7808\n" +
            "9X4czqcXljAdBgNVHQ4EFgQUsVGPx+H0EHp+wefHsSq0rN9eOzMwOQYDVR0RBDIw\n" +
            "MIIucXVvdmFkaXMtcm9vdC1jYS0zLWczLmNoYWluLWRlbW9zLmRpZ2ljZXJ0LmNv\n" +
            "bTAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMC\n" +
            "MIGXBgNVHR8EgY8wgYwwRKBCoECGPmh0dHA6Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9E\n" +
            "aWdpQ2VydFF1b1ZhZGlzVExTSUNBUVZSb290Q0EzRzMuY3JsMESgQqBAhj5odHRw\n" +
            "Oi8vY3JsNC5kaWdpY2VydC5jb20vRGlnaUNlcnRRdW9WYWRpc1RMU0lDQVFWUm9v\n" +
            "dENBM0czLmNybDA+BgNVHSAENzA1MDMGBmeBDAECAjApMCcGCCsGAQUFBwIBFhto\n" +
            "dHRwOi8vd3d3LmRpZ2ljZXJ0LmNvbS9DUFMwgYMGCCsGAQUFBwEBBHcwdTAkBggr\n" +
            "BgEFBQcwAYYYaHR0cDovL29jc3AuZGlnaWNlcnQuY29tME0GCCsGAQUFBzAChkFo\n" +
            "dHRwOi8vY2FjZXJ0cy5kaWdpY2VydC5jb20vRGlnaUNlcnRRdW9WYWRpc1RMU0lD\n" +
            "QVFWUm9vdENBM0czLmNydDAMBgNVHRMBAf8EAjAAMIIBBAYKKwYBBAHWeQIEAgSB\n" +
            "9QSB8gDwAHYAKXm+8J45OSHwVnOfY6V35b5XfZxgCvj5TV0mXCVdx4QAAAF3IHx1\n" +
            "rwAABAMARzBFAiAHK4QuN606ween6tDb2EiNdYTNGhWayvJemSNKuGIELgIhAKaF\n" +
            "1/emwlNTMJDlG+kqEhqeO/+XjtfUfhdIeT8dR7dPAHYAIkVFB1lVJFaWP6Ev8fdt\n" +
            "huAjJmOtwEt/XcaDXG7iDwIAAAF3IHx13QAABAMARzBFAiEA7qBblGimuzeSA3S5\n" +
            "hkE+fVi0DLn/eElclZRk/RSCcsECIDN91iQV6iQryAeD597RExuxunvz+DXn6iz7\n" +
            "43/4LdbkMA0GCSqGSIb3DQEBCwUAA4IBAQBZl4PoBJhh6j32nW39aRpOWTEhxZt+\n" +
            "NItfFhaQSrJWWmWkKyIQ6roKf8mDyf2x6PGiWV1+ZLEJ5iUmuFtcjlvdd6PjP2J9\n" +
            "qEev/kWfdnBY0Ocvfo6ZYhXy/9lW/1s9RQSNiEXTAi+C3R6iTQ5RYW6w4mStynqH\n" +
            "dDO763+aE7vxSh6BxvbZugbkDkrHpjrpoWceT0huwbS7y7U0mk94eXXnTzisBHPY\n" +
            "zbWlJNJVmt2ut9L9TIAZfqTmdWNKVXiVd2tOB2vKwHYzKM55/LqwcX+XfSpT2FQP\n" +
            "OQ1TllgAl+B+4PexVRfO7MNuZgKwT8LuJp1d6xZNvXiNx6dhNBnaY7eE\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=quovadis-root-ca-3-g3-revoked.chain-demos.digicert.com, O="DigiCert, Inc.", L=Lehi, ST=Utah, C=US
    // Issuer: CN=DigiCert QuoVadis TLS ICA QV Root CA 3 G3, O="DigiCert, Inc", C=US
    // Serial number: 804a1f35d45fb0f799c9dd1e54f0a41
    // Valid from: Tue Jan 19 16:00:00 PST 2021 until: Sun Feb 20 15:59:59 PST 2022
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGlzCCBX+gAwIBAgIQCASh811F+w95nJ3R5U8KQTANBgkqhkiG9w0BAQsFADBZ\n" +
            "MQswCQYDVQQGEwJVUzEWMBQGA1UECgwNRGlnaUNlcnQsIEluYzEyMDAGA1UEAwwp\n" +
            "RGlnaUNlcnQgUXVvVmFkaXMgVExTIElDQSBRViBSb290IENBIDMgRzMwHhcNMjEw\n" +
            "MTIwMDAwMDAwWhcNMjIwMjIwMjM1OTU5WjCBhTELMAkGA1UEBhMCVVMxDTALBgNV\n" +
            "BAgTBFV0YWgxDTALBgNVBAcTBExlaGkxFzAVBgNVBAoTDkRpZ2lDZXJ0LCBJbmMu\n" +
            "MT8wPQYDVQQDEzZxdW92YWRpcy1yb290LWNhLTMtZzMtcmV2b2tlZC5jaGFpbi1k\n" +
            "ZW1vcy5kaWdpY2VydC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB\n" +
            "AQC1ZjOf9P8AsZUMI2utVu8I9Ap1xOfIrZ0CGh5ZT1O/+PqE27hnqwG7Ed0yuQs7\n" +
            "OMyo0BP5d8Fg7dbW4LqaA+al3vRH+CQQoeD8zsQd1jWS6ZuitB3CQecZpCaMdzBe\n" +
            "kQrmj0ofmyEcI5tvQTh4UeGkT66ONB0t4Urol0PCanX8Y3jQVt3FdPDdDTiIHRnl\n" +
            "maOD1nCkkEEKGbRhxHH/8p8NCQD+i1Rn6KVMjy/hdMVEJCdFDMfKETgfcs2ZgKce\n" +
            "A8UyPo5pfpvPEptss2Ndj80ou4X0ACV8x0nqWpOojFsml5BYIzRK8s8adAmcUUkG\n" +
            "uukBGeJTQLd3Ygt6wa2gkeP/AgMBAAGjggMsMIIDKDAfBgNVHSMEGDAWgBQzZvsv\n" +
            "kQXMs2JS78089X4czqcXljAdBgNVHQ4EFgQUidJlKGbvtsxFbZ+KM2iajjjrEdcw\n" +
            "QQYDVR0RBDowOII2cXVvdmFkaXMtcm9vdC1jYS0zLWczLXJldm9rZWQuY2hhaW4t\n" +
            "ZGVtb3MuZGlnaWNlcnQuY29tMA4GA1UdDwEB/wQEAwIFoDAdBgNVHSUEFjAUBggr\n" +
            "BgEFBQcDAQYIKwYBBQUHAwIwgZcGA1UdHwSBjzCBjDBEoEKgQIY+aHR0cDovL2Ny\n" +
            "bDMuZGlnaWNlcnQuY29tL0RpZ2lDZXJ0UXVvVmFkaXNUTFNJQ0FRVlJvb3RDQTNH\n" +
            "My5jcmwwRKBCoECGPmh0dHA6Ly9jcmw0LmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydFF1\n" +
            "b1ZhZGlzVExTSUNBUVZSb290Q0EzRzMuY3JsMD4GA1UdIAQ3MDUwMwYGZ4EMAQIC\n" +
            "MCkwJwYIKwYBBQUHAgEWG2h0dHA6Ly93d3cuZGlnaWNlcnQuY29tL0NQUzCBgwYI\n" +
            "KwYBBQUHAQEEdzB1MCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5kaWdpY2VydC5j\n" +
            "b20wTQYIKwYBBQUHMAKGQWh0dHA6Ly9jYWNlcnRzLmRpZ2ljZXJ0LmNvbS9EaWdp\n" +
            "Q2VydFF1b1ZhZGlzVExTSUNBUVZSb290Q0EzRzMuY3J0MAwGA1UdEwEB/wQCMAAw\n" +
            "ggEEBgorBgEEAdZ5AgQCBIH1BIHyAPAAdQApeb7wnjk5IfBWc59jpXflvld9nGAK\n" +
            "+PlNXSZcJV3HhAAAAXcgfZ+yAAAEAwBGMEQCIC/jlb+hFwAO0CDUL1hJSIQ9QZOH\n" +
            "y0fR4gQ3dIXicm26AiAeYJl/ldEn2ojtlL5SYlkpMyf0ry6dNVP0iD8ctUNyngB3\n" +
            "ACJFRQdZVSRWlj+hL/H3bYbgIyZjrcBLf13Gg1xu4g8CAAABdyB9oAUAAAQDAEgw\n" +
            "RgIhAMc2AQO/4o79MHwNZAaAFicxyvLCxjKbmR2kPxmL6oSUAiEA1ue6oGS9cIoy\n" +
            "gTYMxvrLLcETbvapKRFu49dUeger7aUwDQYJKoZIhvcNAQELBQADggEBAG9vJ2sF\n" +
            "Q/guZdJr8tnMcHNISmZSTCXPvc20kjkVF0M0kW979sskISxEmV9pI49FJqQB1hoz\n" +
            "4YJhtOKAg5a9PsvOlMaeM/QNtGHdIf+BKJelpH38klHZ51zMPoWJZVDs+FX1s2fZ\n" +
            "O9kJyU6u22eKhsea2Zt726sIQpL+d4cfBqyXWdUBFp+Kapa/tYhVYW5wo3lxGdf+\n" +
            "ZgtZMd8tRrYtqD7Zz4fxB9SQj4uqbUydb6n95CR4agO+yNH0mzkD3csoeFfTyKkU\n" +
            "93P11+3GMBIMY+Tqtm37eDQlZiJZt8zxdjQLO8oM8MgwimKvNeOIdETvZq6dHu8L\n" +
            "d9QqGEKe5acHUL0=\n" +
            "-----END CERTIFICATE-----";

    public void runTest(ValidatePathWithParams pathValidator)
            throws Exception {
        // Validate valid
        pathValidator.validate(new String[]{VALID, INT},
                ValidatePathWithParams.Status.GOOD, null, System.out);

        // Validate Revoked
        pathValidator.validate(new String[]{REVOKED, INT},
                ValidatePathWithParams.Status.REVOKED,
                "Wed Jan 20 07:51:03 PST 2021", System.out);
    }
}
