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
 * @bug 8216577 8249176
 * @summary Interoperability tests with GlobalSign R6 CA
 * @build ValidatePathWithParams
 * @run main/othervm -Djava.security.debug=certpath GlobalSignR6CA OCSP
 * @run main/othervm -Djava.security.debug=certpath GlobalSignR6CA CRL
 */

 /*
 *
 * Obtain TLS test artifacts for GlobalSign R6 CA from:
 *
 * Valid TLS Certificates:
 * https://valid.r6.roots.globalsign.com/
 *
 * Revoked TLS Certificates:
 * https://revoked.r6.roots.globalsign.com/
 */
public class GlobalSignR6CA {

    // Owner: CN=GlobalSign Atlas R6 EV TLS CA 2020, O=GlobalSign nv-sa, C=BE
    // Issuer: CN=GlobalSign, O=GlobalSign, OU=GlobalSign Root CA - R6
    // Serial number: 7803182afbecd89eb19309bb4a25bdaa
    // Valid from: Mon Jul 27 17:00:00 PDT 2020 until: Sat Jul 27 17:00:00 PDT 2030
    private static final String INT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIGwDCCBKigAwIBAgIQeAMYKvvs2J6xkwm7SiW9qjANBgkqhkiG9w0BAQwFADBM\n" +
            "MSAwHgYDVQQLExdHbG9iYWxTaWduIFJvb3QgQ0EgLSBSNjETMBEGA1UEChMKR2xv\n" +
            "YmFsU2lnbjETMBEGA1UEAxMKR2xvYmFsU2lnbjAeFw0yMDA3MjgwMDAwMDBaFw0z\n" +
            "MDA3MjgwMDAwMDBaMFUxCzAJBgNVBAYTAkJFMRkwFwYDVQQKExBHbG9iYWxTaWdu\n" +
            "IG52LXNhMSswKQYDVQQDEyJHbG9iYWxTaWduIEF0bGFzIFI2IEVWIFRMUyBDQSAy\n" +
            "MDIwMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtQ8IiN2Ukq/Clynv\n" +
            "HhqugFQg5SXIyVO4ZRnxo0hNnaek78LRn4Bkaqcwv6Ls0Ftn4bK2zvBaS1zsfUTA\n" +
            "vfup/s86zHCRvOqAL8zO/WiMV1G5ikHSlD6RtpIOHRX4y0oIGW59ADY0ANwDeDWL\n" +
            "x/RgSltuQIqeGXwZnyZFwWtxVkSE4p5tn2Lb6USzwcD22taiXmeYsPMWfJfmWPRj\n" +
            "ZuYBgxn6tvUVRO+ZzAUKEEaJK/LVLieAVEmfR6anEJ/gWczxz12Lwu6qF5ov0OQt\n" +
            "AP0rfruyje/EJt6xHjpJ2OgDzCWYstXOpRPDHYS3klpaRbowAlpJdYMRAqY5CNiP\n" +
            "RAx3wvsWCVI5UkzKVD6RuHHVpfzfdKAfsjHa/aSunHtTpE+NUf3Q/3qHXW5cyDnP\n" +
            "Jt6VTVVVevjTquwH1xrUigukDbeopV1owsqIA5aw2io7RbBorwPBA0veinHN4vP9\n" +
            "X8jbTiIiLjlfJOnHZe7pIhb3T9WCqhwwsBNPQpKizGHCj5kL2UJe7N5u4RywFOZE\n" +
            "l5mbTX4zO6Vj3WM9ZVbZgXVNwEjS5mYq/rvC1yr9obNUJ8br6JAd2ZBnzhA5Zn4s\n" +
            "bIP99TlUBZWczw+vPM7g1S4e4cyd+8CULVhVs87QlyvwWnRbH7fXZo8xLzhzMCjB\n" +
            "8Y0cNdL1S6QKrrhC6Pf6tV/JU20CAwEAAaOCAZMwggGPMA4GA1UdDwEB/wQEAwIB\n" +
            "hjAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwEgYDVR0TAQH/BAgwBgEB\n" +
            "/wIBADAdBgNVHQ4EFgQUhNwhC8eoXXKXhId+8tW2+nFWTvswHwYDVR0jBBgwFoAU\n" +
            "rmwFo5MT4qLn4tcc1sfwf8hnU6AwewYIKwYBBQUHAQEEbzBtMC4GCCsGAQUFBzAB\n" +
            "hiJodHRwOi8vb2NzcDIuZ2xvYmFsc2lnbi5jb20vcm9vdHI2MDsGCCsGAQUFBzAC\n" +
            "hi9odHRwOi8vc2VjdXJlLmdsb2JhbHNpZ24uY29tL2NhY2VydC9yb290LXI2LmNy\n" +
            "dDA2BgNVHR8ELzAtMCugKaAnhiVodHRwOi8vY3JsLmdsb2JhbHNpZ24uY29tL3Jv\n" +
            "b3QtcjYuY3JsMFUGA1UdIAROMEwwQQYJKwYBBAGgMgEBMDQwMgYIKwYBBQUHAgEW\n" +
            "Jmh0dHBzOi8vd3d3Lmdsb2JhbHNpZ24uY29tL3JlcG9zaXRvcnkvMAcGBWeBDAEB\n" +
            "MA0GCSqGSIb3DQEBDAUAA4ICAQBD+97H2N1BgiliKQFrb+jcWjkmPP8cdF/eiBW1\n" +
            "cEzOOhsuVqxbyIk8qdw3UueHSDjqWUjHYoo8TV3DLqUXmIy1Ks3MkESsFKeLpEbk\n" +
            "VMZga0lbDnqqRc5a2yzrXmwVYDeWVeD20s5vPoKCnFzmcR+2v9TKD4bI6XWVl84q\n" +
            "GzfFRVdY9f8KN+7891+47ZhptvxtNqJKVI2O+EAP/PvTpwes983LkFzsev4/+Qxs\n" +
            "EszD7/pE+Byj3t9CMat2XoX0jfJjbEXgewFb/gCwHvqNKLNWrYfE9qN8b6qm4xQk\n" +
            "qGQKTrFKsBJx4TU+h10qXDhpmOBswiJqoG16XCV32oSn0JUYvXVAvP6YjueOv/jr\n" +
            "0ZMTWGh8wCz6v3XBaXR0rxDAz9GImpU+xPx2XjuHac7OnYbN+i8p7cJPUxABjHiA\n" +
            "LWXIZtCn5ziCfvYC6+SCp8x9TPJzAIfJ4NKv/8SpvvzuchVkAQqlQaGFBEdkX84R\n" +
            "I/WYYG+2BliFIpbQnfljYWCURbfsYz7+Zxb94+4yzva49p8T6lALoK3s2kqIVLKN\n" +
            "s6qAnk/qX6JihkaR3W+iViHMC5tqQX/pd8QIXccF3PA2OdeNGU4iUNZqUbYB4VZd\n" +
            "AaOaeaUl0LwAta6DB5w344eUIqDgaitSwQZBnxppmwL3tGzP1ero2e2RvBmphbxI\n" +
            "atIdxA==\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=valid.r6.roots.globalsign.com,
    // O=GMO GlobalSign LTD, STREET="Springfield House, Sandling Road", OID.2.5.4.17=ME14 2LP, L=Maidstone, ST=Kent,
    // C=GB, SERIALNUMBER=04705639, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.3=GB
    // Issuer: CN=GlobalSign Atlas R6 EV TLS CA 2020, O=GlobalSign nv-sa, C=BE
    // Serial number: 1aff2829dd8bf07aa65a7b3c920ca4b
    // Valid from: Thu Aug 27 00:20:06 PDT 2020 until: Tue Sep 28 00:20:06 PDT 2021
    private static final String VALID = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHyjCCBbKgAwIBAgIQAa/ygp3YvweqZaezySDKSzANBgkqhkiG9w0BAQsFADBV\n" +
            "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQR2xvYmFsU2lnbiBudi1zYTErMCkGA1UE\n" +
            "AxMiR2xvYmFsU2lnbiBBdGxhcyBSNiBFViBUTFMgQ0EgMjAyMDAeFw0yMDA4Mjcw\n" +
            "NzIwMDZaFw0yMTA5MjgwNzIwMDZaMIH6MRMwEQYLKwYBBAGCNzwCAQMTAkdCMR0w\n" +
            "GwYDVQQPDBRQcml2YXRlIE9yZ2FuaXphdGlvbjERMA8GA1UEBRMIMDQ3MDU2Mzkx\n" +
            "CzAJBgNVBAYTAkdCMQ0wCwYDVQQIDARLZW50MRIwEAYDVQQHDAlNYWlkc3RvbmUx\n" +
            "ETAPBgNVBBEMCE1FMTQgMkxQMSkwJwYDVQQJDCBTcHJpbmdmaWVsZCBIb3VzZSwg\n" +
            "U2FuZGxpbmcgUm9hZDEbMBkGA1UECgwSR01PIEdsb2JhbFNpZ24gTFREMSYwJAYD\n" +
            "VQQDDB12YWxpZC5yNi5yb290cy5nbG9iYWxzaWduLmNvbTCCASIwDQYJKoZIhvcN\n" +
            "AQEBBQADggEPADCCAQoCggEBAMOxbh7fZVLUB06xxNBePa9vpOuAS5km1w8ngsTu\n" +
            "SvH1LZnPFd4nu40fi8bPbHd4J2oRWZ28f7LKVQgBupn9knrTQxfTV361WpmwqCcH\n" +
            "MxornKyHx4t5uGrtTtX2fYoNQQk330dIKAfKpUrOiaDybB7irG2JEHdGD3Iv7ud8\n" +
            "FXfXgXte26mUDX3XeCvE0pbuNKpTKApqOeojlVR6TCNB1n6KGYLMIz/1ow6XBZ64\n" +
            "1zKG/9o0gSHelkUHGmGLzOAE5YpkhwzhpND9opycnfieHuy5BcoBIpeMqGNwOsGu\n" +
            "p+nhFz+N8mPjSjZEf0qx+FLF2cBmNFknJJCdnV7OYfKZHE0CAwEAAaOCAu4wggLq\n" +
            "MCgGA1UdEQQhMB+CHXZhbGlkLnI2LnJvb3RzLmdsb2JhbHNpZ24uY29tMA4GA1Ud\n" +
            "DwEB/wQEAwIFoDAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwHQYDVR0O\n" +
            "BBYEFLZolpEC8/bF44e/gnh4StQ9+URwMFUGA1UdIAROMEwwBwYFZ4EMAQEwQQYJ\n" +
            "KwYBBAGgMgEBMDQwMgYIKwYBBQUHAgEWJmh0dHBzOi8vd3d3Lmdsb2JhbHNpZ24u\n" +
            "Y29tL3JlcG9zaXRvcnkvMAwGA1UdEwEB/wQCMAAwgZoGCCsGAQUFBwEBBIGNMIGK\n" +
            "MD4GCCsGAQUFBzABhjJodHRwOi8vb2NzcC5nbG9iYWxzaWduLmNvbS9jYS9nc2F0\n" +
            "bGFzcjZldnRsc2NhMjAyMDBIBggrBgEFBQcwAoY8aHR0cDovL3NlY3VyZS5nbG9i\n" +
            "YWxzaWduLmNvbS9jYWNlcnQvZ3NhdGxhc3I2ZXZ0bHNjYTIwMjAuY3J0MB8GA1Ud\n" +
            "IwQYMBaAFITcIQvHqF1yl4SHfvLVtvpxVk77MEYGA1UdHwQ/MD0wO6A5oDeGNWh0\n" +
            "dHA6Ly9jcmwuZ2xvYmFsc2lnbi5jb20vY2EvZ3NhdGxhc3I2ZXZ0bHNjYTIwMjAu\n" +
            "Y3JsMIIBAwYKKwYBBAHWeQIEAgSB9ASB8QDvAHYAfT7y+I//iFVoJMLAyp5SiXkr\n" +
            "xQ54CX8uapdomX4i8NcAAAF0Lsm7CwAABAMARzBFAiB0fLxAlPzkPxZOVj7c8OFc\n" +
            "YwycekW0Mo+sRm/BQYoeOgIhAK2lNW7ebraH//ZlLQD7dyzWCO+kgmkQo+mqdm1x\n" +
            "4P15AHUAb1N2rDHwMRnYmQCkURX/dxUcEdkCwQApBo2yCJo32RMAAAF0Lsm7JAAA\n" +
            "BAMARjBEAiALOZvdNiA9q1Ysr7ejTGdivUqNJNm9KftmGXwHFGwf2QIgDodNLmbZ\n" +
            "JFGt8l5ul0fHw2Gn8KqhRUW6CMRT58svhcswDQYJKoZIhvcNAQELBQADggIBAByb\n" +
            "hoL/sArmkNjTFiEEBocMfb+brgRQdb08NKC1BDxGnfIFjUmOFzI2SVgtBmcoF8FI\n" +
            "0WyXQv6ZxVE01DFZpeZpsJJYfBAjg9NR4/B7UjajvOJwQNpaciAGQ0ZzTu+SmHja\n" +
            "jIiC2KqiA7Me2MoUne6hhxZ3dXEneIml8hnbTf2mjSBCVpQqyf2goslhGduPitI6\n" +
            "guTtVD2PVaNCVkjlRn4Euspl2JjQWzGcEruqGyQN+Bu4yt1hsD4Jj6V9Hmzo8Vrd\n" +
            "5LUxFPRGIgCUDiiwnENVsQB/D24y3IapPkojujrvsVsmQN42GIgOY5tLK/8cCziD\n" +
            "vf0GzZnmL1D2ezi3TaBj+XBWFcAyF2Y9AnVRmC9CrVcp6EX0KhD4g9ZgbpJZpVlk\n" +
            "G3xfOiZWTeqLnQhCMXcdcutWIwXAX5gueyF1t545vECCE4PeGZNAeWqdbrj7xaS8\n" +
            "3rKQdgwF9r6p7F5HHwEVCckhovEYU4DNFzYb9n/YmC3hmskFB1keTYqydKUYEGZ5\n" +
            "fvLvsjRj9xwOCqIs5j1vuKw2CaqmHxrfYaDMMSZPq/iYrOWrf72wZIvtnAHePt3X\n" +
            "atQMqNbDMQrjul31ljDP9CIbbtuZSkSACyMxiC10l4uTTLQiTxtZPkwIazOjnbBe\n" +
            "A4fruOEQ2k1gu5oFgqmo+xuclOKNjwd/RkK4FXnD\n" +
            "-----END CERTIFICATE-----";

    // Owner: CN=revoked.r6.roots.globalsign.com,
    // O=GMO GlobalSign LTD, STREET="Springfield House, Sandling Road", OID.2.5.4.17=ME14 2LP, L=Maidstone, ST=Kent,
    // C=GB, SERIALNUMBER=04705639, OID.2.5.4.15=Private Organization, OID.1.3.6.1.4.1.311.60.2.1.3=GB
    // Issuer: CN=GlobalSign Atlas R6 EV TLS CA 2020, O=GlobalSign nv-sa, C=BE
    // Serial number: 1df30d84796ac20c47da63b8e681e8f
    // Valid from: Thu Aug 27 00:37:53 PDT 2020 until: Tue Sep 28 00:37:53 PDT 2021
    private static final String REVOKED = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHzzCCBbegAwIBAgIQAd8w2EeWrCDEfaY7jmgejzANBgkqhkiG9w0BAQsFADBV\n" +
            "MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQR2xvYmFsU2lnbiBudi1zYTErMCkGA1UE\n" +
            "AxMiR2xvYmFsU2lnbiBBdGxhcyBSNiBFViBUTFMgQ0EgMjAyMDAeFw0yMDA4Mjcw\n" +
            "NzM3NTNaFw0yMTA5MjgwNzM3NTNaMIH8MRMwEQYLKwYBBAGCNzwCAQMTAkdCMR0w\n" +
            "GwYDVQQPDBRQcml2YXRlIE9yZ2FuaXphdGlvbjERMA8GA1UEBRMIMDQ3MDU2Mzkx\n" +
            "CzAJBgNVBAYTAkdCMQ0wCwYDVQQIDARLZW50MRIwEAYDVQQHDAlNYWlkc3RvbmUx\n" +
            "ETAPBgNVBBEMCE1FMTQgMkxQMSkwJwYDVQQJDCBTcHJpbmdmaWVsZCBIb3VzZSwg\n" +
            "U2FuZGxpbmcgUm9hZDEbMBkGA1UECgwSR01PIEdsb2JhbFNpZ24gTFREMSgwJgYD\n" +
            "VQQDDB9yZXZva2VkLnI2LnJvb3RzLmdsb2JhbHNpZ24uY29tMIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvaNcp7bzmm02Z0S92ZzJ/ul3uQWz3EnBORcI\n" +
            "RuEzm0HY4t0n9DGnxpxOi/aWGX/Vj7qZC4m3G7uCE7dMy6CfXTwh4UZ+nPVijImo\n" +
            "q/msJzmju/pk8HVeOEhk88yvwfzmzYLjoQagmHnDUSQULEmNWihejIh4B61qx4SI\n" +
            "UoBPoBgqDfZW27HkJeqNAO6rljZTZwLenJesm2QMjebYaKxQBi3fLy0Lua2sxTik\n" +
            "fbT3swEPN9xxvMomtNNM2tJwdExL2RpO8dObUe37ep6roG7gWh8NYDKMo6j9Rn9e\n" +
            "f0S9jwkcRM2kZSHR09HSu8ULBgP+KYa8DDpOyt+HO+2G57MhbQIDAQABo4IC8TCC\n" +
            "Au0wKgYDVR0RBCMwIYIfcmV2b2tlZC5yNi5yb290cy5nbG9iYWxzaWduLmNvbTAO\n" +
            "BgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMB0G\n" +
            "A1UdDgQWBBTa1/37G4T022LEW3WwIVV99qtjsjBVBgNVHSAETjBMMAcGBWeBDAEB\n" +
            "MEEGCSsGAQQBoDIBATA0MDIGCCsGAQUFBwIBFiZodHRwczovL3d3dy5nbG9iYWxz\n" +
            "aWduLmNvbS9yZXBvc2l0b3J5LzAMBgNVHRMBAf8EAjAAMIGaBggrBgEFBQcBAQSB\n" +
            "jTCBijA+BggrBgEFBQcwAYYyaHR0cDovL29jc3AuZ2xvYmFsc2lnbi5jb20vY2Ev\n" +
            "Z3NhdGxhc3I2ZXZ0bHNjYTIwMjAwSAYIKwYBBQUHMAKGPGh0dHA6Ly9zZWN1cmUu\n" +
            "Z2xvYmFsc2lnbi5jb20vY2FjZXJ0L2dzYXRsYXNyNmV2dGxzY2EyMDIwLmNydDAf\n" +
            "BgNVHSMEGDAWgBSE3CELx6hdcpeEh37y1bb6cVZO+zBGBgNVHR8EPzA9MDugOaA3\n" +
            "hjVodHRwOi8vY3JsLmdsb2JhbHNpZ24uY29tL2NhL2dzYXRsYXNyNmV2dGxzY2Ey\n" +
            "MDIwLmNybDCCAQQGCisGAQQB1nkCBAIEgfUEgfIA8AB2AG9Tdqwx8DEZ2JkApFEV\n" +
            "/3cVHBHZAsEAKQaNsgiaN9kTAAABdC7aAfUAAAQDAEcwRQIgHIAHHw/Y/VKaaHhy\n" +
            "rZ/cMinivfZ4lUq2ejV7FRPbT8ECIQD3RoE13/MBVMVBLCQ2ErKsB5+7F31dX/tv\n" +
            "Z/muQi5UrQB2AH0+8viP/4hVaCTCwMqeUol5K8UOeAl/LmqXaJl+IvDXAAABdC7a\n" +
            "AegAAAQDAEcwRQIhALl0LXt6pFqS0cHF/XkxSfDJJdhppR2eSlcMFpZY0q1PAiBJ\n" +
            "YkKHqq/YD0gwtZAUEPSk54G1cLxFoUiounjya1XTRzANBgkqhkiG9w0BAQsFAAOC\n" +
            "AgEAdeQotBhB7bn+CztQmF13rdBphHrGkkyHC3hL1bxkmHJcrLQ5ochqPvgdgAVq\n" +
            "DXcV8zSyNwVxW6REi+uYzcsOPKo/llmgF7Psqn1t/EDcutWlykh8UwE5UaLJ2EWD\n" +
            "HnIu06n47lWtAwlNMXJ/ce0oVjqsgY52Y1u54e8wFXt6lsSw02tzIC6eo1BFKxQ3\n" +
            "lDKYVXgg0OvMG/C2rvH/EIq5r+st49rNGWfcWRoHsDUruChZOHwJ9PrXKBLB/QVd\n" +
            "4uw2V/0ipOETDudly7yLodXP8quhet4bCEO9gweXppL/MikLrE5xt46HW1/6w+jF\n" +
            "wKCHWlq4ViswlaQ8q0oY/97o2udnuDQaNdrLgW3VofMeBIMNPBgkLDicOH6bLwNf\n" +
            "lV68qi1ZBxBuOdoOqQyZ9RU9d3EL50XEJ4MtUvjJRAT5EWdFaB8SGGZbD5fyza8c\n" +
            "KmeO5tkZWYecLd8CKqwKcW7umPflEwOzw60Cxg6eyBYA8Jfagpbdb/kXsF6Ov8IW\n" +
            "vxNdHCnXnR3oBWm2uHddESO2zGF1ZfOb0O3cHHG5nCgVkWW68VpgX/LaN90u6Dzw\n" +
            "diJX7esZV5ZaniqD+flWldgAdcfeXlJ5b7I7GnFr61ycmZT/qupagUS1WDq/zfct\n" +
            "QcB4QmnAzGe6kcqiDOSyIYWpiw09jha63KpJtJDWRemrlQI=\n" +
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
                "Thu Aug 27 00:38:11 PDT 2020", System.out);
    }
}

