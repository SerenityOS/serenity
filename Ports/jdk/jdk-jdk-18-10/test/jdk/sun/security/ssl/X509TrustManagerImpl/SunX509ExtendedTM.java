/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 6916074
 * @summary Add support for TLS 1.2
 * @run main/othervm SunX509ExtendedTM
 */

import java.net.*;
import java.util.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.Security;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.*;
import java.security.interfaces.*;
import java.math.BigInteger;


/*
 * Certificates and key used in the test.
 *
 * TLS server certificate:
 * server private key:
 * -----BEGIN RSA PRIVATE KEY-----
 * Proc-Type: 4,ENCRYPTED
 * DEK-Info: DES-EDE3-CBC,D9AE407F6D0E389A
 *
 * WPrA7TFol/cQCcp9oHnXWNpYlvRbbIcQj0m+RKT2Iuzfus+DHt3Zadf8nJpKfX2e
 * h2rnhlzCN9M7djRDooZKDOPCsdBn51Au7HlZF3S3Opgo7D8XFM1a8t1Je4ke14oI
 * nw6QKYsBblRziPnP2PZ0zvX24nOv7bbY8beynlJHGs00VWSFdoH2DS0aE1p6D+3n
 * ptJuJ75dVfZFK4X7162APlNXevX8D6PEQpSiRw1rjjGGcnvQ4HdWk3BxDVDcCNJb
 * Y1aGNRxsjTDvPi3R9Qx2M+W03QzEPx4SR3ZHVskeSJHaetM0TM/w/45Paq4GokXP
 * ZeTnbEx1xmjkA7h+t4doLL4watx5F6yLsJzu8xB3lt/1EtmkYtLz1t7X4BetPAXz
 * zS69X/VwhKfsOI3qXBWuL2oHPyhDmT1gcaUQwEPSV6ogHEEQEDXdiUS8heNK13KF
 * TCQYFkETvV2BLxUhV1hypPzRQ6tUpJiAbD5KmoK2lD9slshG2QtvKQq0/bgkDY5J
 * LhDHV2dtcZ3kDPkkZXpbcJQvoeH3d09C5sIsuTFo2zgNR6oETHUc5TzP6FY2YYRa
 * QcK5HcmtsRRiXFm01ac+aMejJUIujjFt84SiKWT/73vC8AmY4tYcJBLjCg4XIxSH
 * fdDFLL1YZENNO5ivlp8mdiHqcawx+36L7DrEZQ8RZt6cqST5t/+XTdM74s6k81GT
 * pNsa82P2K2zmIUZ/DL2mKjW1vfRByw1NQFEBkN3vdyZxYfM/JyUzX4hbjXBEkh9Q
 * QYrcwLKLjis2QzSvK04B3bvRzRb+4ocWiso8ZPAXAIxZFBWDpTMM2A==
 * -----END RSA PRIVATE KEY-----
 *
 * -----BEGIN RSA PRIVATE KEY-----
 * MIICXAIBAAKBgQClrFscN6LdmYktsnm4j9VIpecchBeNaZzGrG358h0fORna03Ie
 * buxEzHCk3LoAMPagTz1UemFqzFfQCn+VKBg/mtmU8hvIJIh+/p0PPftXUwizIDPU
 * PxdHFNHN6gjYDnVOr77M0uyvqXpJ38LZrLgkQJCmA1Yq0DAFQCxPq9l0iQIDAQAB
 * AoGAbqcbg1E1mkR99uOJoNeQYKFOJyGiiXTMnXV1TseC4+PDfQBU7Dax35GcesBi
 * CtapIpFKKS5D+ozY6b7ZT8ojxuQ/uHLPAvz0WDR3ds4iRF8tyu71Q1ZHcQsJa17y
 * yO7UbkSSKn/Mp9Rb+/dKqftUGNXVFLqgHBOzN2s3We3bbbECQQDYBPKOg3hkaGHo
 * OhpHKqtQ6EVkldihG/3i4WejRonelXN+HRh1KrB2HBx0M8D/qAzP1i3rNSlSHer4
 * 59YRTJnHAkEAxFX/sVYSn07BHv9Zhn6XXct/Cj43z/tKNbzlNbcxqQwQerw3IH51
 * 8UH2YOA+GD3lXbKp+MytoFLWv8zg4YT/LwJAfqan75Z1R6lLffRS49bIiq8jwE16
 * rTrUJ+kv8jKxMqc9B3vXkxpsS1M/+4E8bqgAmvpgAb8xcsvHsBd9ErdukQJBAKs2
 * j67W75BrPjBI34pQ1LEfp56IGWXOrq1kF8IbCjxv3+MYRT6Z6UJFkpRymNPNDjsC
 * dgUYgITiGJHUGXuw3lMCQHEHqo9ZtXz92yFT+VhsNc29B8m/sqUJdtCcMd/jGpAF
 * u6GHufjqIZBpQsk63wbwESAPZZ+kk1O1kS5GIRLX608=
 * -----END RSA PRIVATE KEY-----
 *
 * Private-Key: (1024 bit)
 * modulus:
 *     00:a5:ac:5b:1c:37:a2:dd:99:89:2d:b2:79:b8:8f:
 *     d5:48:a5:e7:1c:84:17:8d:69:9c:c6:ac:6d:f9:f2:
 *     1d:1f:39:19:da:d3:72:1e:6e:ec:44:cc:70:a4:dc:
 *     ba:00:30:f6:a0:4f:3d:54:7a:61:6a:cc:57:d0:0a:
 *     7f:95:28:18:3f:9a:d9:94:f2:1b:c8:24:88:7e:fe:
 *     9d:0f:3d:fb:57:53:08:b3:20:33:d4:3f:17:47:14:
 *     d1:cd:ea:08:d8:0e:75:4e:af:be:cc:d2:ec:af:a9:
 *     7a:49:df:c2:d9:ac:b8:24:40:90:a6:03:56:2a:d0:
 *     30:05:40:2c:4f:ab:d9:74:89
 * publicExponent: 65537 (0x10001)
 * privateExponent:
 *     6e:a7:1b:83:51:35:9a:44:7d:f6:e3:89:a0:d7:90:
 *     60:a1:4e:27:21:a2:89:74:cc:9d:75:75:4e:c7:82:
 *     e3:e3:c3:7d:00:54:ec:36:b1:df:91:9c:7a:c0:62:
 *     0a:d6:a9:22:91:4a:29:2e:43:fa:8c:d8:e9:be:d9:
 *     4f:ca:23:c6:e4:3f:b8:72:cf:02:fc:f4:58:34:77:
 *     76:ce:22:44:5f:2d:ca:ee:f5:43:56:47:71:0b:09:
 *     6b:5e:f2:c8:ee:d4:6e:44:92:2a:7f:cc:a7:d4:5b:
 *     fb:f7:4a:a9:fb:54:18:d5:d5:14:ba:a0:1c:13:b3:
 *     37:6b:37:59:ed:db:6d:b1
 * prime1:
 *     00:d8:04:f2:8e:83:78:64:68:61:e8:3a:1a:47:2a:
 *     ab:50:e8:45:64:95:d8:a1:1b:fd:e2:e1:67:a3:46:
 *     89:de:95:73:7e:1d:18:75:2a:b0:76:1c:1c:74:33:
 *     c0:ff:a8:0c:cf:d6:2d:eb:35:29:52:1d:ea:f8:e7:
 *     d6:11:4c:99:c7
 * prime2:
 *     00:c4:55:ff:b1:56:12:9f:4e:c1:1e:ff:59:86:7e:
 *     97:5d:cb:7f:0a:3e:37:cf:fb:4a:35:bc:e5:35:b7:
 *     31:a9:0c:10:7a:bc:37:20:7e:75:f1:41:f6:60:e0:
 *     3e:18:3d:e5:5d:b2:a9:f8:cc:ad:a0:52:d6:bf:cc:
 *     e0:e1:84:ff:2f
 * exponent1:
 *     7e:a6:a7:ef:96:75:47:a9:4b:7d:f4:52:e3:d6:c8:
 *     8a:af:23:c0:4d:7a:ad:3a:d4:27:e9:2f:f2:32:b1:
 *     32:a7:3d:07:7b:d7:93:1a:6c:4b:53:3f:fb:81:3c:
 *     6e:a8:00:9a:fa:60:01:bf:31:72:cb:c7:b0:17:7d:
 *     12:b7:6e:91
 * exponent2:
 *     00:ab:36:8f:ae:d6:ef:90:6b:3e:30:48:df:8a:50:
 *     d4:b1:1f:a7:9e:88:19:65:ce:ae:ad:64:17:c2:1b:
 *     0a:3c:6f:df:e3:18:45:3e:99:e9:42:45:92:94:72:
 *     98:d3:cd:0e:3b:02:76:05:18:80:84:e2:18:91:d4:
 *     19:7b:b0:de:53
 * coefficient:
 *     71:07:aa:8f:59:b5:7c:fd:db:21:53:f9:58:6c:35:
 *     cd:bd:07:c9:bf:b2:a5:09:76:d0:9c:31:df:e3:1a:
 *     90:05:bb:a1:87:b9:f8:ea:21:90:69:42:c9:3a:df:
 *     06:f0:11:20:0f:65:9f:a4:93:53:b5:91:2e:46:21:
 *     12:d7:eb:4f
 *
 *
 * server certificate:
 * Data:
 *     Version: 3 (0x2)
 *     Serial Number: 8 (0x8)
 *     Signature Algorithm: md5WithRSAEncryption
 *     Issuer: C=US, ST=Some-State, L=Some-City, O=Some-Org
 *     Validity
 *         Not Before: Dec  8 03:43:04 2008 GMT
 *         Not After : Aug 25 03:43:04 2028 GMT
 *     Subject: C=US, ST=Some-State, L=Some-City, O=Some-Org, OU=SSL-Server, CN=localhost
 *     Subject Public Key Info:
 *         Public Key Algorithm: rsaEncryption
 *         RSA Public Key: (1024 bit)
 *             Modulus (1024 bit):
 *                 00:a5:ac:5b:1c:37:a2:dd:99:89:2d:b2:79:b8:8f:
 *                 d5:48:a5:e7:1c:84:17:8d:69:9c:c6:ac:6d:f9:f2:
 *                 1d:1f:39:19:da:d3:72:1e:6e:ec:44:cc:70:a4:dc:
 *                 ba:00:30:f6:a0:4f:3d:54:7a:61:6a:cc:57:d0:0a:
 *                 7f:95:28:18:3f:9a:d9:94:f2:1b:c8:24:88:7e:fe:
 *                 9d:0f:3d:fb:57:53:08:b3:20:33:d4:3f:17:47:14:
 *                 d1:cd:ea:08:d8:0e:75:4e:af:be:cc:d2:ec:af:a9:
 *                 7a:49:df:c2:d9:ac:b8:24:40:90:a6:03:56:2a:d0:
 *                 30:05:40:2c:4f:ab:d9:74:89
 *             Exponent: 65537 (0x10001)
 *     X509v3 extensions:
 *         X509v3 Basic Constraints:
 *             CA:FALSE
 *         X509v3 Key Usage:
 *             Digital Signature, Non Repudiation, Key Encipherment
 *         X509v3 Subject Key Identifier:
 *             ED:6E:DB:F4:B5:56:C8:FB:1A:06:61:3F:0F:08:BB:A6:04:D8:16:54
 *         X509v3 Authority Key Identifier:
 *             keyid:FA:B9:51:BF:4C:E7:D9:86:98:33:F9:E7:CB:1E:F1:33:49:F7:A8:14
 *
 *         X509v3 Subject Alternative Name: critical
 *             DNS:localhost
 * Signature Algorithm: md5WithRSAEncryption0
 *
 * -----BEGIN CERTIFICATE-----
 * MIICpDCCAg2gAwIBAgIBCDANBgkqhkiG9w0BAQQFADBJMQswCQYDVQQGEwJVUzET
 * MBEGA1UECBMKU29tZS1TdGF0ZTESMBAGA1UEBxMJU29tZS1DaXR5MREwDwYDVQQK
 * EwhTb21lLU9yZzAeFw0wODEyMDgwMzQzMDRaFw0yODA4MjUwMzQzMDRaMHIxCzAJ
 * BgNVBAYTAlVTMRMwEQYDVQQIEwpTb21lLVN0YXRlMRIwEAYDVQQHEwlTb21lLUNp
 * dHkxETAPBgNVBAoTCFNvbWUtT3JnMRMwEQYDVQQLEwpTU0wtU2VydmVyMRIwEAYD
 * VQQDEwlsb2NhbGhvc3QwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKWsWxw3
 * ot2ZiS2yebiP1Uil5xyEF41pnMasbfnyHR85GdrTch5u7ETMcKTcugAw9qBPPVR6
 * YWrMV9AKf5UoGD+a2ZTyG8gkiH7+nQ89+1dTCLMgM9Q/F0cU0c3qCNgOdU6vvszS
 * 7K+peknfwtmsuCRAkKYDVirQMAVALE+r2XSJAgMBAAGjczBxMAkGA1UdEwQCMAAw
 * CwYDVR0PBAQDAgXgMB0GA1UdDgQWBBTtbtv0tVbI+xoGYT8PCLumBNgWVDAfBgNV
 * HSMEGDAWgBT6uVG/TOfZhpgz+efLHvEzSfeoFDAXBgNVHREBAf8EDTALgglsb2Nh
 * bGhvc3QwDQYJKoZIhvcNAQEEBQADgYEAoqVTciHtcvsUj+YaTct8tUh3aTCsKsac
 * PHhfQ+ObjiXSgxsKYTX7ym/wk/wvlbUcbqLKxsu7qrcJitH+H9heV1hEHEu65Uoi
 * nRugFruyOrwvAylV8Cm2af7ddilmYJ+sdJA6N2M3xJRxR0G2LFHEXDNEjYReyexn
 * JqCpf5uZGOo=
 * -----END CERTIFICATE-----
 *
 *
 * TLS client certificate:
 * client private key:
 * ----BEGIN RSA PRIVATE KEY-----
 * Proc-Type: 4,ENCRYPTED
 * DEK-Info: DES-EDE3-CBC,FA2A435CD35A9390
 *
 * Z+Y2uaETbsUWIyJUyVu1UV2G4rgFYJyACZT6Tp1KjRtxflSh2kXkJ9MpuXMXA0V4
 * Yy3fDzPqCL9NJmQAYRlAx/W/+j4F5EyMWDIx8fUxzONRZyoiwF7jLm+KscAfv6Pf
 * q7ItWOdj3z7IYrwlB8YIGd3F2cDKT3S+lYRk7rKb/qT7itbuHnY4Ardh3yl+MZak
 * jBp+ELUlRsUqSr1V0LoM+0rCCykarpyfhpxEcqsrl0v9Cyi5uhU50/oKv5zql3SH
 * l2ImgDjp3batAs8+Bd4NF2aqi0a7Hy44JUHxRm4caZryU/i/D9N1MbuM6882HLat
 * 5N0G+NaIUfywa8mjwq2D5aiit18HqKA6XeRRYeJ5Dvu9DCO4GeFSwcUFIBMI0L46
 * 7s114+oDodg57pMgITi+04vmUxvqlN9aiyd7f5Fgd7PeHGeOdbMz1NaJLJaPI9++
 * NakK8eK9iwT/Gdq0Uap5/CHW7vCT5PO+h3HY0STH0lWStXhdWnFO04zTdywsbSp+
 * DLpHeFT66shfeUlxR0PsCbG9vPRt/QmGLeYQZITppWo/ylSq4j+pRIuXvuWHdBRN
 * rTZ8QF4Y7AxQUXVz1j1++s6ZMHTzaK2i9HrhmDs1MbJl+QwWre3Xpv3LvTVz3k5U
 * wX8kuY1m3STt71QCaRWENq5sRaMImLxZbxc/ivFl9RAzUqo4NCxLod/QgA4iLqtO
 * ztnlpzwlC/F8HbQ1oqYWwnZAPhzU/cULtstl+Yrws2c2atO323LbPXZqbASySgig
 * sNpFXQMObdfP6LN23bY+1SvtK7V4NUTNhpdIc6INQAQ=
 * -----END RSA PRIVATE KEY-----
 *
 * -----BEGIN RSA PRIVATE KEY-----
 * MIICWwIBAAKBgQC78EA2rCZUTvSjWgAvaSFvuXo6k+yi9uGOx2PYLxIwmS6w8o/4
 * Jy0keCiE9wG/jUR53TvSVfPOPLJbIX3v/TNKsaP/xsibuQ98QTWX+ds6BWAFFa9Z
 * F5KjEK0WHOQHU6+odqJWKpLT+SjgeM9eH0irXBnd4WdDunWN9YKsQ5JEGwIDAQAB
 * AoGAEbdqNj0wN85hnWyEi/ObJU8UyKTdL9eaF72QGfcF/fLSxfd3vurihIeXOkGW
 * tpn4lIxYcVGM9CognhqgJpl11jFTQzn1KqZ+NEJRKkCHA4hDabKJbSC9fXHvRwrf
 * BsFpZqgiNxp3HseUTiwnaUVeyPgMt/jAj5nB5Sib+UyUxrECQQDnNQBiF2aifEg6
 * zbJOOC7he5CHAdkFxSxWVFVHL6EfXfqdLVkUohMbgZv+XxyIeU2biOExSg49Kds3
 * FOKgTau1AkEA0Bd1haj6QuCo8I0AXm2WO+MMTZMTvtHD/bGjKNM+fT4I8rKYnQRX
 * 1acHdqS9Xx2rNJqZgkMmpESIdPR2fc4yjwJALFeM6EMmqvj8/VIf5UJ/Mz14fXwM
 * PEARfckUxd9LnnFutCBTWlKvKXJVEZb6KO5ixPaegc57Jp3Vbh3yTN44lQJADD/1
 * SSMDaIB1MYP7a5Oj7m6VQNPRq8AJe5vDcRnOae0G9dKRrVyeFxO4GsHj6/+BHp2j
 * P8nYMn9eURQ7DXjf/QJAAQzMlWnKGSO8pyTDtnQx3hRMoUkOEhmNq4bQhLkYqtnY
 * FcqpUQ2qMjW+NiNWk5HnTrMS3L9EdJobMUzaNZLy4w==
 * -----END RSA PRIVATE KEY-----
 *
 * Private-Key: (1024 bit)
 * modulus:
 *     00:bb:f0:40:36:ac:26:54:4e:f4:a3:5a:00:2f:69:
 *     21:6f:b9:7a:3a:93:ec:a2:f6:e1:8e:c7:63:d8:2f:
 *     12:30:99:2e:b0:f2:8f:f8:27:2d:24:78:28:84:f7:
 *     01:bf:8d:44:79:dd:3b:d2:55:f3:ce:3c:b2:5b:21:
 *     7d:ef:fd:33:4a:b1:a3:ff:c6:c8:9b:b9:0f:7c:41:
 *     35:97:f9:db:3a:05:60:05:15:af:59:17:92:a3:10:
 *     ad:16:1c:e4:07:53:af:a8:76:a2:56:2a:92:d3:f9:
 *     28:e0:78:cf:5e:1f:48:ab:5c:19:dd:e1:67:43:ba:
 *     75:8d:f5:82:ac:43:92:44:1b
 * publicExponent: 65537 (0x10001)
 * privateExponent:
 *     11:b7:6a:36:3d:30:37:ce:61:9d:6c:84:8b:f3:9b:
 *     25:4f:14:c8:a4:dd:2f:d7:9a:17:bd:90:19:f7:05:
 *     fd:f2:d2:c5:f7:77:be:ea:e2:84:87:97:3a:41:96:
 *     b6:99:f8:94:8c:58:71:51:8c:f4:2a:20:9e:1a:a0:
 *     26:99:75:d6:31:53:43:39:f5:2a:a6:7e:34:42:51:
 *     2a:40:87:03:88:43:69:b2:89:6d:20:bd:7d:71:ef:
 *     47:0a:df:06:c1:69:66:a8:22:37:1a:77:1e:c7:94:
 *     4e:2c:27:69:45:5e:c8:f8:0c:b7:f8:c0:8f:99:c1:
 *     e5:28:9b:f9:4c:94:c6:b1
 * prime1:
 *     00:e7:35:00:62:17:66:a2:7c:48:3a:cd:b2:4e:38:
 *     2e:e1:7b:90:87:01:d9:05:c5:2c:56:54:55:47:2f:
 *     a1:1f:5d:fa:9d:2d:59:14:a2:13:1b:81:9b:fe:5f:
 *     1c:88:79:4d:9b:88:e1:31:4a:0e:3d:29:db:37:14:
 *     e2:a0:4d:ab:b5
 * prime2:
 *     00:d0:17:75:85:a8:fa:42:e0:a8:f0:8d:00:5e:6d:
 *     96:3b:e3:0c:4d:93:13:be:d1:c3:fd:b1:a3:28:d3:
 *     3e:7d:3e:08:f2:b2:98:9d:04:57:d5:a7:07:76:a4:
 *     bd:5f:1d:ab:34:9a:99:82:43:26:a4:44:88:74:f4:
 *     76:7d:ce:32:8f
 * exponent1:
 *     2c:57:8c:e8:43:26:aa:f8:fc:fd:52:1f:e5:42:7f:
 *     33:3d:78:7d:7c:0c:3c:40:11:7d:c9:14:c5:df:4b:
 *     9e:71:6e:b4:20:53:5a:52:af:29:72:55:11:96:fa:
 *     28:ee:62:c4:f6:9e:81:ce:7b:26:9d:d5:6e:1d:f2:
 *     4c:de:38:95
 * exponent2:
 *     0c:3f:f5:49:23:03:68:80:75:31:83:fb:6b:93:a3:
 *     ee:6e:95:40:d3:d1:ab:c0:09:7b:9b:c3:71:19:ce:
 *     69:ed:06:f5:d2:91:ad:5c:9e:17:13:b8:1a:c1:e3:
 *     eb:ff:81:1e:9d:a3:3f:c9:d8:32:7f:5e:51:14:3b:
 *     0d:78:df:fd
 * coefficient:
 *     01:0c:cc:95:69:ca:19:23:bc:a7:24:c3:b6:74:31:
 *     de:14:4c:a1:49:0e:12:19:8d:ab:86:d0:84:b9:18:
 *     aa:d9:d8:15:ca:a9:51:0d:aa:32:35:be:36:23:56:
 *     93:91:e7:4e:b3:12:dc:bf:44:74:9a:1b:31:4c:da:
 *     35:92:f2:e3
 *
 * client certificate:
 * Data:
 *     Version: 3 (0x2)
 *     Serial Number: 9 (0x9)
 *     Signature Algorithm: md5WithRSAEncryption
 *     Issuer: C=US, ST=Some-State, L=Some-City, O=Some-Org
 *     Validity
 *         Not Before: Dec  8 03:43:24 2008 GMT
 *         Not After : Aug 25 03:43:24 2028 GMT
 *     Subject: C=US, ST=Some-State, L=Some-City, O=Some-Org, OU=SSL-Client, CN=localhost
 *     Subject Public Key Info:
 *         Public Key Algorithm: rsaEncryption
 *         RSA Public Key: (1024 bit)
 *             Modulus (1024 bit):
 *                 00:bb:f0:40:36:ac:26:54:4e:f4:a3:5a:00:2f:69:
 *                 21:6f:b9:7a:3a:93:ec:a2:f6:e1:8e:c7:63:d8:2f:
 *                 12:30:99:2e:b0:f2:8f:f8:27:2d:24:78:28:84:f7:
 *                 01:bf:8d:44:79:dd:3b:d2:55:f3:ce:3c:b2:5b:21:
 *                 7d:ef:fd:33:4a:b1:a3:ff:c6:c8:9b:b9:0f:7c:41:
 *                 35:97:f9:db:3a:05:60:05:15:af:59:17:92:a3:10:
 *                 ad:16:1c:e4:07:53:af:a8:76:a2:56:2a:92:d3:f9:
 *                 28:e0:78:cf:5e:1f:48:ab:5c:19:dd:e1:67:43:ba:
 *                 75:8d:f5:82:ac:43:92:44:1b
 *             Exponent: 65537 (0x10001)
 *     X509v3 extensions:
 *         X509v3 Basic Constraints:
 *             CA:FALSE
 *         X509v3 Key Usage:
 *             Digital Signature, Non Repudiation, Key Encipherment
 *         X509v3 Subject Key Identifier:
 *             CD:BB:C8:85:AA:91:BD:FD:1D:BE:CD:67:7C:FF:B3:E9:4C:A8:22:E6
 *         X509v3 Authority Key Identifier:
 *             keyid:FA:B9:51:BF:4C:E7:D9:86:98:33:F9:E7:CB:1E:F1:33:49:F7:A8:14
 *
 *         X509v3 Subject Alternative Name: critical
 *             DNS:localhost
 * Signature Algorithm: md5WithRSAEncryption
 *
 * -----BEGIN CERTIFICATE-----
 * MIICpDCCAg2gAwIBAgIBCTANBgkqhkiG9w0BAQQFADBJMQswCQYDVQQGEwJVUzET
 * MBEGA1UECBMKU29tZS1TdGF0ZTESMBAGA1UEBxMJU29tZS1DaXR5MREwDwYDVQQK
 * EwhTb21lLU9yZzAeFw0wODEyMDgwMzQzMjRaFw0yODA4MjUwMzQzMjRaMHIxCzAJ
 * BgNVBAYTAlVTMRMwEQYDVQQIEwpTb21lLVN0YXRlMRIwEAYDVQQHEwlTb21lLUNp
 * dHkxETAPBgNVBAoTCFNvbWUtT3JnMRMwEQYDVQQLEwpTU0wtQ2xpZW50MRIwEAYD
 * VQQDEwlsb2NhbGhvc3QwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALvwQDas
 * JlRO9KNaAC9pIW+5ejqT7KL24Y7HY9gvEjCZLrDyj/gnLSR4KIT3Ab+NRHndO9JV
 * 8848slshfe/9M0qxo//GyJu5D3xBNZf52zoFYAUVr1kXkqMQrRYc5AdTr6h2olYq
 * ktP5KOB4z14fSKtcGd3hZ0O6dY31gqxDkkQbAgMBAAGjczBxMAkGA1UdEwQCMAAw
 * CwYDVR0PBAQDAgXgMB0GA1UdDgQWBBTNu8iFqpG9/R2+zWd8/7PpTKgi5jAfBgNV
 * HSMEGDAWgBT6uVG/TOfZhpgz+efLHvEzSfeoFDAXBgNVHREBAf8EDTALgglsb2Nh
 * bGhvc3QwDQYJKoZIhvcNAQEEBQADgYEAm25gJyqW1JznQ1EyOtTGswBVwfgBOf+F
 * HJuBTcflYQLbTD/AETPQJGvZU9tdhuLtbG3OPhR7vSY8zeAbfM3dbH7QFr3r47Gj
 * XEH7qM/MX+Z3ifVaC4MeJmrYQkYFSuKeyyKpdRVX4w4nnFHF6OsNASsYrMW6LpxN
 * cl/epUcHL7E=
 * -----END CERTIFICATE-----
 *
 *
 *
 * Trusted CA certificate:
 * Certificate:
 *   Data:
 *     Version: 3 (0x2)
 *     Serial Number: 0 (0x0)
 *     Signature Algorithm: md5WithRSAEncryption
 *     Issuer: C=US, ST=Some-State, L=Some-City, O=Some-Org
 *     Validity
 *         Not Before: Dec  8 02:43:36 2008 GMT
 *         Not After : Aug 25 02:43:36 2028 GMT
 *     Subject: C=US, ST=Some-State, L=Some-City, O=Some-Org
 *     Subject Public Key Info:
 *         Public Key Algorithm: rsaEncryption
 *         RSA Public Key: (1024 bit)
 *             Modulus (1024 bit):
 *                 00:cb:c4:38:20:07:be:88:a7:93:b0:a1:43:51:2d:
 *                 d7:8e:85:af:54:dd:ad:a2:7b:23:5b:cf:99:13:53:
 *                 99:45:7d:ee:6d:ba:2d:bf:e3:ad:6e:3d:9f:1a:f9:
 *                 03:97:e0:17:55:ae:11:26:57:de:01:29:8e:05:3f:
 *                 21:f7:e7:36:e8:2e:37:d7:48:ac:53:d6:60:0e:c7:
 *                 50:6d:f6:c5:85:f7:8b:a6:c5:91:35:72:3c:94:ee:
 *                 f1:17:f0:71:e3:ec:1b:ce:ca:4e:40:42:b0:6d:ee:
 *                 6a:0e:d6:e5:ad:3c:0f:c9:ba:82:4f:78:f8:89:97:
 *                 89:2a:95:12:4c:d8:09:2a:e9
 *             Exponent: 65537 (0x10001)
 *     X509v3 extensions:
 *         X509v3 Subject Key Identifier:
 *             FA:B9:51:BF:4C:E7:D9:86:98:33:F9:E7:CB:1E:F1:33:49:F7:A8:14
 *         X509v3 Authority Key Identifier:
 *             keyid:FA:B9:51:BF:4C:E7:D9:86:98:33:F9:E7:CB:1E:F1:33:49:F7:A8:14
 *             DirName:/C=US/ST=Some-State/L=Some-City/O=Some-Org
 *             serial:00
 *
 *         X509v3 Basic Constraints:
 *             CA:TRUE
 *  Signature Algorithm: md5WithRSAEncryption
 *
 * -----BEGIN CERTIFICATE-----
 * MIICrDCCAhWgAwIBAgIBADANBgkqhkiG9w0BAQQFADBJMQswCQYDVQQGEwJVUzET
 * MBEGA1UECBMKU29tZS1TdGF0ZTESMBAGA1UEBxMJU29tZS1DaXR5MREwDwYDVQQK
 * EwhTb21lLU9yZzAeFw0wODEyMDgwMjQzMzZaFw0yODA4MjUwMjQzMzZaMEkxCzAJ
 * BgNVBAYTAlVTMRMwEQYDVQQIEwpTb21lLVN0YXRlMRIwEAYDVQQHEwlTb21lLUNp
 * dHkxETAPBgNVBAoTCFNvbWUtT3JnMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB
 * gQDLxDggB76Ip5OwoUNRLdeOha9U3a2ieyNbz5kTU5lFfe5tui2/461uPZ8a+QOX
 * 4BdVrhEmV94BKY4FPyH35zboLjfXSKxT1mAOx1Bt9sWF94umxZE1cjyU7vEX8HHj
 * 7BvOyk5AQrBt7moO1uWtPA/JuoJPePiJl4kqlRJM2Akq6QIDAQABo4GjMIGgMB0G
 * A1UdDgQWBBT6uVG/TOfZhpgz+efLHvEzSfeoFDBxBgNVHSMEajBogBT6uVG/TOfZ
 * hpgz+efLHvEzSfeoFKFNpEswSTELMAkGA1UEBhMCVVMxEzARBgNVBAgTClNvbWUt
 * U3RhdGUxEjAQBgNVBAcTCVNvbWUtQ2l0eTERMA8GA1UEChMIU29tZS1PcmeCAQAw
 * DAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQQFAAOBgQBcIm534U123Hz+rtyYO5uA
 * ofd81G6FnTfEAV8Kw9fGyyEbQZclBv34A9JsFKeMvU4OFIaixD7nLZ/NZ+IWbhmZ
 * LovmJXyCkOufea73pNiZ+f/4/ScZaIlM/PRycQSqbFNd4j9Wott+08qxHPLpsf3P
 * 6Mvf0r1PNTY2hwTJLJmKtg==
 * -----END CERTIFICATE---
 */


public class SunX509ExtendedTM {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    /*
     * Should we run the client or server in a separate thread?
     * Both sides can throw exceptions, but do you have a preference
     * as to which side should be the main thread.
     */
    static boolean separateServerThread = false;

    /*
     * Where do we find the keystores?
     */
    static String trusedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICrDCCAhWgAwIBAgIBADANBgkqhkiG9w0BAQQFADBJMQswCQYDVQQGEwJVUzET\n" +
        "MBEGA1UECBMKU29tZS1TdGF0ZTESMBAGA1UEBxMJU29tZS1DaXR5MREwDwYDVQQK\n" +
        "EwhTb21lLU9yZzAeFw0wODEyMDgwMjQzMzZaFw0yODA4MjUwMjQzMzZaMEkxCzAJ\n" +
        "BgNVBAYTAlVTMRMwEQYDVQQIEwpTb21lLVN0YXRlMRIwEAYDVQQHEwlTb21lLUNp\n" +
        "dHkxETAPBgNVBAoTCFNvbWUtT3JnMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n" +
        "gQDLxDggB76Ip5OwoUNRLdeOha9U3a2ieyNbz5kTU5lFfe5tui2/461uPZ8a+QOX\n" +
        "4BdVrhEmV94BKY4FPyH35zboLjfXSKxT1mAOx1Bt9sWF94umxZE1cjyU7vEX8HHj\n" +
        "7BvOyk5AQrBt7moO1uWtPA/JuoJPePiJl4kqlRJM2Akq6QIDAQABo4GjMIGgMB0G\n" +
        "A1UdDgQWBBT6uVG/TOfZhpgz+efLHvEzSfeoFDBxBgNVHSMEajBogBT6uVG/TOfZ\n" +
        "hpgz+efLHvEzSfeoFKFNpEswSTELMAkGA1UEBhMCVVMxEzARBgNVBAgTClNvbWUt\n" +
        "U3RhdGUxEjAQBgNVBAcTCVNvbWUtQ2l0eTERMA8GA1UEChMIU29tZS1PcmeCAQAw\n" +
        "DAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQQFAAOBgQBcIm534U123Hz+rtyYO5uA\n" +
        "ofd81G6FnTfEAV8Kw9fGyyEbQZclBv34A9JsFKeMvU4OFIaixD7nLZ/NZ+IWbhmZ\n" +
        "LovmJXyCkOufea73pNiZ+f/4/ScZaIlM/PRycQSqbFNd4j9Wott+08qxHPLpsf3P\n" +
        "6Mvf0r1PNTY2hwTJLJmKtg==\n" +
        "-----END CERTIFICATE-----";

    static String serverCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICpDCCAg2gAwIBAgIBCDANBgkqhkiG9w0BAQQFADBJMQswCQYDVQQGEwJVUzET\n" +
        "MBEGA1UECBMKU29tZS1TdGF0ZTESMBAGA1UEBxMJU29tZS1DaXR5MREwDwYDVQQK\n" +
        "EwhTb21lLU9yZzAeFw0wODEyMDgwMzQzMDRaFw0yODA4MjUwMzQzMDRaMHIxCzAJ\n" +
        "BgNVBAYTAlVTMRMwEQYDVQQIEwpTb21lLVN0YXRlMRIwEAYDVQQHEwlTb21lLUNp\n" +
        "dHkxETAPBgNVBAoTCFNvbWUtT3JnMRMwEQYDVQQLEwpTU0wtU2VydmVyMRIwEAYD\n" +
        "VQQDEwlsb2NhbGhvc3QwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAKWsWxw3\n" +
        "ot2ZiS2yebiP1Uil5xyEF41pnMasbfnyHR85GdrTch5u7ETMcKTcugAw9qBPPVR6\n" +
        "YWrMV9AKf5UoGD+a2ZTyG8gkiH7+nQ89+1dTCLMgM9Q/F0cU0c3qCNgOdU6vvszS\n" +
        "7K+peknfwtmsuCRAkKYDVirQMAVALE+r2XSJAgMBAAGjczBxMAkGA1UdEwQCMAAw\n" +
        "CwYDVR0PBAQDAgXgMB0GA1UdDgQWBBTtbtv0tVbI+xoGYT8PCLumBNgWVDAfBgNV\n" +
        "HSMEGDAWgBT6uVG/TOfZhpgz+efLHvEzSfeoFDAXBgNVHREBAf8EDTALgglsb2Nh\n" +
        "bGhvc3QwDQYJKoZIhvcNAQEEBQADgYEAoqVTciHtcvsUj+YaTct8tUh3aTCsKsac\n" +
        "PHhfQ+ObjiXSgxsKYTX7ym/wk/wvlbUcbqLKxsu7qrcJitH+H9heV1hEHEu65Uoi\n" +
        "nRugFruyOrwvAylV8Cm2af7ddilmYJ+sdJA6N2M3xJRxR0G2LFHEXDNEjYReyexn\n" +
        "JqCpf5uZGOo=\n" +
        "-----END CERTIFICATE-----";

    static String clientCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICpDCCAg2gAwIBAgIBCTANBgkqhkiG9w0BAQQFADBJMQswCQYDVQQGEwJVUzET\n" +
        "MBEGA1UECBMKU29tZS1TdGF0ZTESMBAGA1UEBxMJU29tZS1DaXR5MREwDwYDVQQK\n" +
        "EwhTb21lLU9yZzAeFw0wODEyMDgwMzQzMjRaFw0yODA4MjUwMzQzMjRaMHIxCzAJ\n" +
        "BgNVBAYTAlVTMRMwEQYDVQQIEwpTb21lLVN0YXRlMRIwEAYDVQQHEwlTb21lLUNp\n" +
        "dHkxETAPBgNVBAoTCFNvbWUtT3JnMRMwEQYDVQQLEwpTU0wtQ2xpZW50MRIwEAYD\n" +
        "VQQDEwlsb2NhbGhvc3QwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALvwQDas\n" +
        "JlRO9KNaAC9pIW+5ejqT7KL24Y7HY9gvEjCZLrDyj/gnLSR4KIT3Ab+NRHndO9JV\n" +
        "8848slshfe/9M0qxo//GyJu5D3xBNZf52zoFYAUVr1kXkqMQrRYc5AdTr6h2olYq\n" +
        "ktP5KOB4z14fSKtcGd3hZ0O6dY31gqxDkkQbAgMBAAGjczBxMAkGA1UdEwQCMAAw\n" +
        "CwYDVR0PBAQDAgXgMB0GA1UdDgQWBBTNu8iFqpG9/R2+zWd8/7PpTKgi5jAfBgNV\n" +
        "HSMEGDAWgBT6uVG/TOfZhpgz+efLHvEzSfeoFDAXBgNVHREBAf8EDTALgglsb2Nh\n" +
        "bGhvc3QwDQYJKoZIhvcNAQEEBQADgYEAm25gJyqW1JznQ1EyOtTGswBVwfgBOf+F\n" +
        "HJuBTcflYQLbTD/AETPQJGvZU9tdhuLtbG3OPhR7vSY8zeAbfM3dbH7QFr3r47Gj\n" +
        "XEH7qM/MX+Z3ifVaC4MeJmrYQkYFSuKeyyKpdRVX4w4nnFHF6OsNASsYrMW6LpxN\n" +
        "cl/epUcHL7E=\n" +
        "-----END CERTIFICATE-----";

    static byte serverPrivateExponent[] = {
        (byte)0x6e, (byte)0xa7, (byte)0x1b, (byte)0x83,
        (byte)0x51, (byte)0x35, (byte)0x9a, (byte)0x44,
        (byte)0x7d, (byte)0xf6, (byte)0xe3, (byte)0x89,
        (byte)0xa0, (byte)0xd7, (byte)0x90, (byte)0x60,
        (byte)0xa1, (byte)0x4e, (byte)0x27, (byte)0x21,
        (byte)0xa2, (byte)0x89, (byte)0x74, (byte)0xcc,
        (byte)0x9d, (byte)0x75, (byte)0x75, (byte)0x4e,
        (byte)0xc7, (byte)0x82, (byte)0xe3, (byte)0xe3,
        (byte)0xc3, (byte)0x7d, (byte)0x00, (byte)0x54,
        (byte)0xec, (byte)0x36, (byte)0xb1, (byte)0xdf,
        (byte)0x91, (byte)0x9c, (byte)0x7a, (byte)0xc0,
        (byte)0x62, (byte)0x0a, (byte)0xd6, (byte)0xa9,
        (byte)0x22, (byte)0x91, (byte)0x4a, (byte)0x29,
        (byte)0x2e, (byte)0x43, (byte)0xfa, (byte)0x8c,
        (byte)0xd8, (byte)0xe9, (byte)0xbe, (byte)0xd9,
        (byte)0x4f, (byte)0xca, (byte)0x23, (byte)0xc6,
        (byte)0xe4, (byte)0x3f, (byte)0xb8, (byte)0x72,
        (byte)0xcf, (byte)0x02, (byte)0xfc, (byte)0xf4,
        (byte)0x58, (byte)0x34, (byte)0x77, (byte)0x76,
        (byte)0xce, (byte)0x22, (byte)0x44, (byte)0x5f,
        (byte)0x2d, (byte)0xca, (byte)0xee, (byte)0xf5,
        (byte)0x43, (byte)0x56, (byte)0x47, (byte)0x71,
        (byte)0x0b, (byte)0x09, (byte)0x6b, (byte)0x5e,
        (byte)0xf2, (byte)0xc8, (byte)0xee, (byte)0xd4,
        (byte)0x6e, (byte)0x44, (byte)0x92, (byte)0x2a,
        (byte)0x7f, (byte)0xcc, (byte)0xa7, (byte)0xd4,
        (byte)0x5b, (byte)0xfb, (byte)0xf7, (byte)0x4a,
        (byte)0xa9, (byte)0xfb, (byte)0x54, (byte)0x18,
        (byte)0xd5, (byte)0xd5, (byte)0x14, (byte)0xba,
        (byte)0xa0, (byte)0x1c, (byte)0x13, (byte)0xb3,
        (byte)0x37, (byte)0x6b, (byte)0x37, (byte)0x59,
        (byte)0xed, (byte)0xdb, (byte)0x6d, (byte)0xb1
    };

    static byte serverModulus[] = {
        (byte)0x00,
        (byte)0xa5, (byte)0xac, (byte)0x5b, (byte)0x1c,
        (byte)0x37, (byte)0xa2, (byte)0xdd, (byte)0x99,
        (byte)0x89, (byte)0x2d, (byte)0xb2, (byte)0x79,
        (byte)0xb8, (byte)0x8f, (byte)0xd5, (byte)0x48,
        (byte)0xa5, (byte)0xe7, (byte)0x1c, (byte)0x84,
        (byte)0x17, (byte)0x8d, (byte)0x69, (byte)0x9c,
        (byte)0xc6, (byte)0xac, (byte)0x6d, (byte)0xf9,
        (byte)0xf2, (byte)0x1d, (byte)0x1f, (byte)0x39,
        (byte)0x19, (byte)0xda, (byte)0xd3, (byte)0x72,
        (byte)0x1e, (byte)0x6e, (byte)0xec, (byte)0x44,
        (byte)0xcc, (byte)0x70, (byte)0xa4, (byte)0xdc,
        (byte)0xba, (byte)0x00, (byte)0x30, (byte)0xf6,
        (byte)0xa0, (byte)0x4f, (byte)0x3d, (byte)0x54,
        (byte)0x7a, (byte)0x61, (byte)0x6a, (byte)0xcc,
        (byte)0x57, (byte)0xd0, (byte)0x0a, (byte)0x7f,
        (byte)0x95, (byte)0x28, (byte)0x18, (byte)0x3f,
        (byte)0x9a, (byte)0xd9, (byte)0x94, (byte)0xf2,
        (byte)0x1b, (byte)0xc8, (byte)0x24, (byte)0x88,
        (byte)0x7e, (byte)0xfe, (byte)0x9d, (byte)0x0f,
        (byte)0x3d, (byte)0xfb, (byte)0x57, (byte)0x53,
        (byte)0x08, (byte)0xb3, (byte)0x20, (byte)0x33,
        (byte)0xd4, (byte)0x3f, (byte)0x17, (byte)0x47,
        (byte)0x14, (byte)0xd1, (byte)0xcd, (byte)0xea,
        (byte)0x08, (byte)0xd8, (byte)0x0e, (byte)0x75,
        (byte)0x4e, (byte)0xaf, (byte)0xbe, (byte)0xcc,
        (byte)0xd2, (byte)0xec, (byte)0xaf, (byte)0xa9,
        (byte)0x7a, (byte)0x49, (byte)0xdf, (byte)0xc2,
        (byte)0xd9, (byte)0xac, (byte)0xb8, (byte)0x24,
        (byte)0x40, (byte)0x90, (byte)0xa6, (byte)0x03,
        (byte)0x56, (byte)0x2a, (byte)0xd0, (byte)0x30,
        (byte)0x05, (byte)0x40, (byte)0x2c, (byte)0x4f,
        (byte)0xab, (byte)0xd9, (byte)0x74, (byte)0x89
    };

    static byte clientPrivateExponent[] = {
        (byte)0x11, (byte)0xb7, (byte)0x6a, (byte)0x36,
        (byte)0x3d, (byte)0x30, (byte)0x37, (byte)0xce,
        (byte)0x61, (byte)0x9d, (byte)0x6c, (byte)0x84,
        (byte)0x8b, (byte)0xf3, (byte)0x9b, (byte)0x25,
        (byte)0x4f, (byte)0x14, (byte)0xc8, (byte)0xa4,
        (byte)0xdd, (byte)0x2f, (byte)0xd7, (byte)0x9a,
        (byte)0x17, (byte)0xbd, (byte)0x90, (byte)0x19,
        (byte)0xf7, (byte)0x05, (byte)0xfd, (byte)0xf2,
        (byte)0xd2, (byte)0xc5, (byte)0xf7, (byte)0x77,
        (byte)0xbe, (byte)0xea, (byte)0xe2, (byte)0x84,
        (byte)0x87, (byte)0x97, (byte)0x3a, (byte)0x41,
        (byte)0x96, (byte)0xb6, (byte)0x99, (byte)0xf8,
        (byte)0x94, (byte)0x8c, (byte)0x58, (byte)0x71,
        (byte)0x51, (byte)0x8c, (byte)0xf4, (byte)0x2a,
        (byte)0x20, (byte)0x9e, (byte)0x1a, (byte)0xa0,
        (byte)0x26, (byte)0x99, (byte)0x75, (byte)0xd6,
        (byte)0x31, (byte)0x53, (byte)0x43, (byte)0x39,
        (byte)0xf5, (byte)0x2a, (byte)0xa6, (byte)0x7e,
        (byte)0x34, (byte)0x42, (byte)0x51, (byte)0x2a,
        (byte)0x40, (byte)0x87, (byte)0x03, (byte)0x88,
        (byte)0x43, (byte)0x69, (byte)0xb2, (byte)0x89,
        (byte)0x6d, (byte)0x20, (byte)0xbd, (byte)0x7d,
        (byte)0x71, (byte)0xef, (byte)0x47, (byte)0x0a,
        (byte)0xdf, (byte)0x06, (byte)0xc1, (byte)0x69,
        (byte)0x66, (byte)0xa8, (byte)0x22, (byte)0x37,
        (byte)0x1a, (byte)0x77, (byte)0x1e, (byte)0xc7,
        (byte)0x94, (byte)0x4e, (byte)0x2c, (byte)0x27,
        (byte)0x69, (byte)0x45, (byte)0x5e, (byte)0xc8,
        (byte)0xf8, (byte)0x0c, (byte)0xb7, (byte)0xf8,
        (byte)0xc0, (byte)0x8f, (byte)0x99, (byte)0xc1,
        (byte)0xe5, (byte)0x28, (byte)0x9b, (byte)0xf9,
        (byte)0x4c, (byte)0x94, (byte)0xc6, (byte)0xb1
    };

    static byte clientModulus[] = {
        (byte)0x00,
        (byte)0xbb, (byte)0xf0, (byte)0x40, (byte)0x36,
        (byte)0xac, (byte)0x26, (byte)0x54, (byte)0x4e,
        (byte)0xf4, (byte)0xa3, (byte)0x5a, (byte)0x00,
        (byte)0x2f, (byte)0x69, (byte)0x21, (byte)0x6f,
        (byte)0xb9, (byte)0x7a, (byte)0x3a, (byte)0x93,
        (byte)0xec, (byte)0xa2, (byte)0xf6, (byte)0xe1,
        (byte)0x8e, (byte)0xc7, (byte)0x63, (byte)0xd8,
        (byte)0x2f, (byte)0x12, (byte)0x30, (byte)0x99,
        (byte)0x2e, (byte)0xb0, (byte)0xf2, (byte)0x8f,
        (byte)0xf8, (byte)0x27, (byte)0x2d, (byte)0x24,
        (byte)0x78, (byte)0x28, (byte)0x84, (byte)0xf7,
        (byte)0x01, (byte)0xbf, (byte)0x8d, (byte)0x44,
        (byte)0x79, (byte)0xdd, (byte)0x3b, (byte)0xd2,
        (byte)0x55, (byte)0xf3, (byte)0xce, (byte)0x3c,
        (byte)0xb2, (byte)0x5b, (byte)0x21, (byte)0x7d,
        (byte)0xef, (byte)0xfd, (byte)0x33, (byte)0x4a,
        (byte)0xb1, (byte)0xa3, (byte)0xff, (byte)0xc6,
        (byte)0xc8, (byte)0x9b, (byte)0xb9, (byte)0x0f,
        (byte)0x7c, (byte)0x41, (byte)0x35, (byte)0x97,
        (byte)0xf9, (byte)0xdb, (byte)0x3a, (byte)0x05,
        (byte)0x60, (byte)0x05, (byte)0x15, (byte)0xaf,
        (byte)0x59, (byte)0x17, (byte)0x92, (byte)0xa3,
        (byte)0x10, (byte)0xad, (byte)0x16, (byte)0x1c,
        (byte)0xe4, (byte)0x07, (byte)0x53, (byte)0xaf,
        (byte)0xa8, (byte)0x76, (byte)0xa2, (byte)0x56,
        (byte)0x2a, (byte)0x92, (byte)0xd3, (byte)0xf9,
        (byte)0x28, (byte)0xe0, (byte)0x78, (byte)0xcf,
        (byte)0x5e, (byte)0x1f, (byte)0x48, (byte)0xab,
        (byte)0x5c, (byte)0x19, (byte)0xdd, (byte)0xe1,
        (byte)0x67, (byte)0x43, (byte)0xba, (byte)0x75,
        (byte)0x8d, (byte)0xf5, (byte)0x82, (byte)0xac,
        (byte)0x43, (byte)0x92, (byte)0x44, (byte)0x1b
    };

    static char passphrase[] = "passphrase".toCharArray();

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = false;

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {
        SSLContext context = getSSLContext(trusedCertStr, serverCertStr,
            serverModulus, serverPrivateExponent, passphrase);
        SSLServerSocketFactory sslssf = context.getServerSocketFactory();

        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket(serverPort);
        serverPort = sslServerSocket.getLocalPort();


        // enable endpoint identification
        // ignore, we may test the feature when known how to parse client
        // hostname
        //SSLParameters params = sslServerSocket.getSSLParameters();
        //params.setEndpointIdentificationAlgorithm("HTTPS");
        //sslServerSocket.setSSLParameters(params);

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        sslSocket.setNeedClientAuth(true);

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        sslIS.read();
        sslOS.write(85);
        sslOS.flush();

        sslSocket.close();

    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {
        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        SSLContext context = getSSLContext(trusedCertStr, clientCertStr,
            clientModulus, clientPrivateExponent, passphrase);

        SSLSocketFactory sslsf = context.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket)
            sslsf.createSocket("localhost", serverPort);

        // enable endpoint identification
        SSLParameters params = sslSocket.getSSLParameters();
        params.setEndpointIdentificationAlgorithm("HTTPS");
        sslSocket.setSSLParameters(params);

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();

        sslSocket.close();

    }

    // get the ssl context
    private static SSLContext getSSLContext(String trusedCertStr,
            String keyCertStr, byte[] modulus,
            byte[] privateExponent, char[] passphrase) throws Exception {

        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is =
                    new ByteArrayInputStream(trusedCertStr.getBytes());
        Certificate trusedCert = cf.generateCertificate(is);
        is.close();

        // create a key store
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // import the trused cert
        ks.setCertificateEntry("RSA Export Signer", trusedCert);

        if (keyCertStr != null) {
            // generate the private key.
            RSAPrivateKeySpec priKeySpec = new RSAPrivateKeySpec(
                                            new BigInteger(modulus),
                                            new BigInteger(privateExponent));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPrivateKey priKey =
                    (RSAPrivateKey)kf.generatePrivate(priKeySpec);

            // generate certificate chain
            is = new ByteArrayInputStream(keyCertStr.getBytes());
            Certificate keyCert = cf.generateCertificate(is);
            is.close();

            Certificate[] chain = new Certificate[2];
            chain[0] = keyCert;
            chain[1] = trusedCert;

            // import the key entry.
            ks.setKeyEntry("Whatever", priKey, passphrase, chain);
        }

        // create SSL context
        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        TrustManager tms[] = tmf.getTrustManagers();
        if (tms == null || tms.length == 0) {
            throw new Exception("unexpected trust manager implementation");
        } else {
           if (!(tms[0] instanceof X509ExtendedTrustManager)) {
            throw new Exception("unexpected trust manager implementation: "
                                + tms[0].getClass().getCanonicalName());
           }
        }


        SSLContext ctx = SSLContext.getInstance("TLS");

        if (keyCertStr != null) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passphrase);

            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } else {
            ctx.init(null, tmf.getTrustManagers(), null);
        }

        return ctx;
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String args[]) throws Exception {
        // MD5 is used in this test case, don't disable MD5 algorithm.
        Security.setProperty("jdk.certpath.disabledAlgorithms",
                "MD2, RSA keySize < 1024");
        Security.setProperty("jdk.tls.disabledAlgorithms",
                "SSLv3, RC4, DH keySize < 768");

        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new SunX509ExtendedTM();
    }

    Thread clientThread = null;
    Thread serverThread = null;
    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SunX509ExtendedTM() throws Exception {
        if (separateServerThread) {
            startServer(true);
            startClient(false);
        } else {
            startClient(true);
            startServer(false);
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            serverThread.join();
        } else {
            clientThread.join();
        }

        /*
         * When we get here, the test is pretty much over.
         *
         * If the main thread excepted, that propagates back
         * immediately.  If the other thread threw an exception, we
         * should report back.
         */
        if (serverException != null)
            throw serverException;
        if (clientException != null)
            throw clientException;
    }

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide();
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            doServerSide();
        }
    }

    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide();
                    } catch (Exception e) {
                        /*
                         * Our client thread just died.
                         */
                        System.err.println("Client died...");
                        clientException = e;
                    }
                }
            };
            clientThread.start();
        } else {
            doClientSide();
        }
    }

}
