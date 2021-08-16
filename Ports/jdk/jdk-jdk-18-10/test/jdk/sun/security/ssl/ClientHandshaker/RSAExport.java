/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.

/*
 * @test
 * @bug 6690018
 * @summary RSAClientKeyExchange NullPointerException
 * @run main/othervm RSAExport
 */

/*
 * Certificates and key used in the test.
 *
 * TLS server certificate:
 * server private key:
 * -----BEGIN RSA PRIVATE KEY-----
 * Proc-Type: 4,ENCRYPTED
 * DEK-Info: DES-EDE3-CBC,97EC03A2D031B7BC
 *
 * 22wrD+DPv3VF8xg9xoeBqHzFnOVbTLQgVulzaCECDF4zWdxElYKy4yYyY6dMDehi
 * XT77NTsq1J14zjJHPp2/U6B5OpZxnf97ZSD0ZC9/DDe/2gjW4fY1Lv0TVP0PdXnm
 * cj84RaDiiSk/cERlFzFJ5L8ULMwxdOtYwXwZ4upITw2lT+8zDlBD2i3zZ4TcWrzE
 * /su5Kpu+Mp3wthfGX+ZGga2T/NS8ZCKZE+gJDPKQZ/x34VBw+YANQGyCJPv1iMaE
 * RyagnpApH9OPSrRIp2iR6uWT6836CET2erbfPaC1odyd8IsbnLldVs9CklH7EgXL
 * Nms+DqrQEbNmvMuQYEFyZEHN9D1fGONeacx+cjI85FyMSHSEO65JJmasAxgQe4nF
 * /yVz3rNQ2qAGqBhjsjP/WaXuB2aLZiAli/HjN17EJws=
 * -----END RSA PRIVATE KEY-----
 *
 * -----BEGIN RSA PRIVATE KEY-----
 * MIIBOQIBAAJBALlfGg/5ZweJcW5zqLdnQ2uyircqDDlENKnv9FABOm/j0wnlPHqX
 * CCqFBLoM7tG8ohci1SPy6fLJ5dqLf5FOH2sCAwEAAQJATO0/hpOMgx8xmJGc2Yeb
 * /gyY7kwfyIAajs9Khw0LcDTYTo2EAI+vMmDpU+dvmOCLUqq/Z2tiKJhGyrmcBlxr
 * kQIhAPYkbYovtvWHslxRb78x4eCrn2p1H7iolNKbyepjCI3zAiEAwMufJlLI9Q0O
 * BIr7fPnUhbs9NyMHLIvIQAf/hXYubqkCIGJZR9NxIT+VyrSMbYQNoF0u9fGJfvU/
 * lsdYLCOVEnP1AiAsSFjUx50K1CXNG1MqYIPU963W1T/Xln+3XV7ue7esiQIgW2Lu
 * xGvz2dAUsGId+Xr2GZXb7ZucY/cPt4o5qdP1m7c=
 * -----END RSA PRIVATE KEY-----
 *
 * Private-Key: (512 bit)
 * modulus:
 *     00:b9:5f:1a:0f:f9:67:07:89:71:6e:73:a8:b7:67:
 *     43:6b:b2:8a:b7:2a:0c:39:44:34:a9:ef:f4:50:01:
 *     3a:6f:e3:d3:09:e5:3c:7a:97:08:2a:85:04:ba:0c:
 *     ee:d1:bc:a2:17:22:d5:23:f2:e9:f2:c9:e5:da:8b:
 *     7f:91:4e:1f:6b
 * publicExponent: 65537 (0x10001)
 * privateExponent:
 *     4c:ed:3f:86:93:8c:83:1f:31:98:91:9c:d9:87:9b:
 *     fe:0c:98:ee:4c:1f:c8:80:1a:8e:cf:4a:87:0d:0b:
 *     70:34:d8:4e:8d:84:00:8f:af:32:60:e9:53:e7:6f:
 *     98:e0:8b:52:aa:bf:67:6b:62:28:98:46:ca:b9:9c:
 *     06:5c:6b:91
 * prime1:
 *     00:f6:24:6d:8a:2f:b6:f5:87:b2:5c:51:6f:bf:31:
 *     e1:e0:ab:9f:6a:75:1f:b8:a8:94:d2:9b:c9:ea:63:
 *     08:8d:f3
 * prime2:
 *     00:c0:cb:9f:26:52:c8:f5:0d:0e:04:8a:fb:7c:f9:
 *     d4:85:bb:3d:37:23:07:2c:8b:c8:40:07:ff:85:76:
 *     2e:6e:a9
 * exponent1:
 *     62:59:47:d3:71:21:3f:95:ca:b4:8c:6d:84:0d:a0:
 *     5d:2e:f5:f1:89:7e:f5:3f:96:c7:58:2c:23:95:12:
 *     73:f5
 * exponent2:
 *     2c:48:58:d4:c7:9d:0a:d4:25:cd:1b:53:2a:60:83:
 *     d4:f7:ad:d6:d5:3f:d7:96:7f:b7:5d:5e:ee:7b:b7:
 *     ac:89
 * coefficient:
 *     5b:62:ee:c4:6b:f3:d9:d0:14:b0:62:1d:f9:7a:f6:
 *     19:95:db:ed:9b:9c:63:f7:0f:b7:8a:39:a9:d3:f5:
 *     9b:b7
 *
 *
 * server certificate:
 *  Data:
 *      Version: 3 (0x2)
 *      Serial Number: 11 (0xb)
 *      Signature Algorithm: sha1WithRSAEncryption
 *      Issuer: C=US, ST=Some-State, O=Some Org, CN=Someone
 *      Validity
 *          Not Before: Apr 18 15:07:30 2008 GMT
 *          Not After : Jan  4 15:07:30 2028 GMT
 *      Subject: C=US, ST=Some-State, O=Some Org, CN=SomeoneExport
 *      Subject Public Key Info:
 *          Public Key Algorithm: rsaEncryption
 *          RSA Public Key: (512 bit)
 *              Modulus (512 bit):
 *                  00:b9:5f:1a:0f:f9:67:07:89:71:6e:73:a8:b7:67:
 *                  43:6b:b2:8a:b7:2a:0c:39:44:34:a9:ef:f4:50:01:
 *                  3a:6f:e3:d3:09:e5:3c:7a:97:08:2a:85:04:ba:0c:
 *                  ee:d1:bc:a2:17:22:d5:23:f2:e9:f2:c9:e5:da:8b:
 *                  7f:91:4e:1f:6b
 *              Exponent: 65537 (0x10001)
 *      X509v3 extensions:
 *          X509v3 Basic Constraints:
 *              CA:FALSE
 *          X509v3 Key Usage:
 *              Digital Signature, Non Repudiation, Key Encipherment
 *          X509v3 Subject Key Identifier:
 *              F1:30:98:BE:7C:AA:F9:B1:91:38:60:AE:13:5F:67:9C:0A:32:9E:31
 *          X509v3 Authority Key Identifier:
 *              keyid:B5:32:43:D7:00:24:92:BA:E9:95:E5:F9:A3:64:6C:84:EE:33:2E:15
 *
 * -----BEGIN CERTIFICATE-----
 * MIICIDCCAYmgAwIBAgIBCzANBgkqhkiG9w0BAQUFADBHMQswCQYDVQQGEwJVUzET
 * MBEGA1UECBMKU29tZS1TdGF0ZTERMA8GA1UEChMIU29tZSBPcmcxEDAOBgNVBAMT
 * B1NvbWVvbmUwHhcNMDgwNDE4MTUwNzMwWhcNMjgwMTA0MTUwNzMwWjBNMQswCQYD
 * VQQGEwJVUzETMBEGA1UECBMKU29tZS1TdGF0ZTERMA8GA1UEChMIU29tZSBPcmcx
 * FjAUBgNVBAMTDVNvbWVvbmVFeHBvcnQwXDANBgkqhkiG9w0BAQEFAANLADBIAkEA
 * uV8aD/lnB4lxbnOot2dDa7KKtyoMOUQ0qe/0UAE6b+PTCeU8epcIKoUEugzu0byi
 * FyLVI/Lp8snl2ot/kU4fawIDAQABo1owWDAJBgNVHRMEAjAAMAsGA1UdDwQEAwIF
 * 4DAdBgNVHQ4EFgQU8TCYvnyq+bGROGCuE19nnAoynjEwHwYDVR0jBBgwFoAUtTJD
 * 1wAkkrrpleX5o2RshO4zLhUwDQYJKoZIhvcNAQEFBQADgYEAFU+fP9FSTQNVZOhv
 * eJ+zq6wI/biwzTgPbAq3yu2gb5kT85z4nzqBhPd2LWWFXhUW/D8QyNZ54X30y0Ug
 * 3NfUAvOANW7CgUbHBmm77KQiF4nWdh338qqq9HzLGrPqcxX0dmiq2RBVPy9wb2Ea
 * FTZiU2v+9pkoLoSDnCOfPCg/4Q4=
 * -----END CERTIFICATE-----
 *
 *
 * Trusted CA certificate:
 * Certificate:
 *   Data:
 *       Version: 3 (0x2)
 *       Serial Number: 0 (0x0)
 *       Signature Algorithm: md5WithRSAEncryption
 *       Issuer: C=US, ST=Some-State, O=Some Org, CN=Someone
 *       Validity
 *           Not Before: Mar 30 11:44:47 2001 GMT
 *           Not After : Apr 27 11:44:47 2028 GMT
 *       Subject: C=US, ST=Some-State, O=Some Org, CN=Someone
 *       Subject Public Key Info:
 *           Public Key Algorithm: rsaEncryption
 *           RSA Public Key: (1024 bit)
 *               Modulus (1024 bit):
 *                   00:c1:98:e4:7a:87:53:0f:94:87:dc:da:f3:59:39:
 *                   3e:36:95:e8:77:58:ff:46:8a:81:1b:5e:c5:4c:fa:
 *                   b6:91:19:30:be:5b:ef:4c:aa:84:30:a4:9a:d4:68:
 *                   af:ef:fa:b4:2c:76:8b:29:33:46:cf:38:74:7c:79:
 *                   d5:07:a6:43:39:84:52:39:4f:8a:1c:f3:73:19:12:
 *                   40:cf:ee:a1:77:43:01:02:be:8d:32:11:28:70:f4:
 *                   cf:ab:43:75:e4:fb:74:f1:8c:2e:43:24:ba:85:3f:
 *                   66:3a:05:ea:f7:ce:5b:97:e2:34:a3:f0:87:f4:f8:
 *                   d1:59:12:5a:68:b7:78:64:a9
 *               Exponent: 65537 (0x10001)
 *       X509v3 extensions:
 *           X509v3 Subject Key Identifier:
 *               B5:32:43:D7:00:24:92:BA:E9:95:E5:F9:A3:64:6C:84:EE:33:2E:15
 *           X509v3 Authority Key Identifier:
 *               keyid:B5:32:43:D7:00:24:92:BA:E9:95:E5:F9:A3:64:6C:84:EE:33:2E:15
 *               DirName:/C=US/ST=Some-State/O=Some Org/CN=Someone
 *               serial:00
 *
 *           X509v3 Basic Constraints:
 *               CA:TRUE
 *
 * -----BEGIN CERTIFICATE-----
 * MIICpjCCAg+gAwIBAgIBADANBgkqhkiG9w0BAQQFADBHMQswCQYDVQQGEwJVUzET
 * MBEGA1UECBMKU29tZS1TdGF0ZTERMA8GA1UEChMIU29tZSBPcmcxEDAOBgNVBAMT
 * B1NvbWVvbmUwHhcNMDEwMzMwMTE0NDQ3WhcNMjgwNDI3MTE0NDQ3WjBHMQswCQYD
 * VQQGEwJVUzETMBEGA1UECBMKU29tZS1TdGF0ZTERMA8GA1UEChMIU29tZSBPcmcx
 * EDAOBgNVBAMTB1NvbWVvbmUwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMGY
 * 5HqHUw+Uh9za81k5PjaV6HdY/0aKgRtexUz6tpEZML5b70yqhDCkmtRor+/6tCx2
 * iykzRs84dHx51QemQzmEUjlPihzzcxkSQM/uoXdDAQK+jTIRKHD0z6tDdeT7dPGM
 * LkMkuoU/ZjoF6vfOW5fiNKPwh/T40VkSWmi3eGSpAgMBAAGjgaEwgZ4wHQYDVR0O
 * BBYEFLUyQ9cAJJK66ZXl+aNkbITuMy4VMG8GA1UdIwRoMGaAFLUyQ9cAJJK66ZXl
 * +aNkbITuMy4VoUukSTBHMQswCQYDVQQGEwJVUzETMBEGA1UECBMKU29tZS1TdGF0
 * ZTERMA8GA1UEChMIU29tZSBPcmcxEDAOBgNVBAMTB1NvbWVvbmWCAQAwDAYDVR0T
 * BAUwAwEB/zANBgkqhkiG9w0BAQQFAAOBgQBhf3PX0xWxtaUwZlWCO7GfPwCKgBWr
 * CXqlqjtWHCshaaU7wUsDOwxFDWwKjFrMerQLsLuBlhdXEbNfSPjychkQtfezQHcS
 * q0Atq7+KVSmRbDw6oKVRs5v1BBzLCupy+o16fNz3/hwreAWwQnSMtAh/osNS9w1b
 * QeVWU+JV47H+vg==
 * -----END CERTIFICATE-----
 *
 */

import java.io.*;
import java.net.*;
import java.security.Security;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.net.ssl.*;
import java.math.BigInteger;

public class RSAExport {

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
    static boolean separateServerThread = true;

    /*
     * Where do we find the keystores?
     */
    static String trusedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICpjCCAg+gAwIBAgIBADANBgkqhkiG9w0BAQQFADBHMQswCQYDVQQGEwJVUzET\n" +
        "MBEGA1UECBMKU29tZS1TdGF0ZTERMA8GA1UEChMIU29tZSBPcmcxEDAOBgNVBAMT\n" +
        "B1NvbWVvbmUwHhcNMDEwMzMwMTE0NDQ3WhcNMjgwNDI3MTE0NDQ3WjBHMQswCQYD\n" +
        "VQQGEwJVUzETMBEGA1UECBMKU29tZS1TdGF0ZTERMA8GA1UEChMIU29tZSBPcmcx\n" +
        "EDAOBgNVBAMTB1NvbWVvbmUwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMGY\n" +
        "5HqHUw+Uh9za81k5PjaV6HdY/0aKgRtexUz6tpEZML5b70yqhDCkmtRor+/6tCx2\n" +
        "iykzRs84dHx51QemQzmEUjlPihzzcxkSQM/uoXdDAQK+jTIRKHD0z6tDdeT7dPGM\n" +
        "LkMkuoU/ZjoF6vfOW5fiNKPwh/T40VkSWmi3eGSpAgMBAAGjgaEwgZ4wHQYDVR0O\n" +
        "BBYEFLUyQ9cAJJK66ZXl+aNkbITuMy4VMG8GA1UdIwRoMGaAFLUyQ9cAJJK66ZXl\n" +
        "+aNkbITuMy4VoUukSTBHMQswCQYDVQQGEwJVUzETMBEGA1UECBMKU29tZS1TdGF0\n" +
        "ZTERMA8GA1UEChMIU29tZSBPcmcxEDAOBgNVBAMTB1NvbWVvbmWCAQAwDAYDVR0T\n" +
        "BAUwAwEB/zANBgkqhkiG9w0BAQQFAAOBgQBhf3PX0xWxtaUwZlWCO7GfPwCKgBWr\n" +
        "CXqlqjtWHCshaaU7wUsDOwxFDWwKjFrMerQLsLuBlhdXEbNfSPjychkQtfezQHcS\n" +
        "q0Atq7+KVSmRbDw6oKVRs5v1BBzLCupy+o16fNz3/hwreAWwQnSMtAh/osNS9w1b\n" +
        "QeVWU+JV47H+vg==\n" +
        "-----END CERTIFICATE-----";

    static String serverCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICIDCCAYmgAwIBAgIBCzANBgkqhkiG9w0BAQUFADBHMQswCQYDVQQGEwJVUzET\n" +
        "MBEGA1UECBMKU29tZS1TdGF0ZTERMA8GA1UEChMIU29tZSBPcmcxEDAOBgNVBAMT\n" +
        "B1NvbWVvbmUwHhcNMDgwNDE4MTUwNzMwWhcNMjgwMTA0MTUwNzMwWjBNMQswCQYD\n" +
        "VQQGEwJVUzETMBEGA1UECBMKU29tZS1TdGF0ZTERMA8GA1UEChMIU29tZSBPcmcx\n" +
        "FjAUBgNVBAMTDVNvbWVvbmVFeHBvcnQwXDANBgkqhkiG9w0BAQEFAANLADBIAkEA\n" +
        "uV8aD/lnB4lxbnOot2dDa7KKtyoMOUQ0qe/0UAE6b+PTCeU8epcIKoUEugzu0byi\n" +
        "FyLVI/Lp8snl2ot/kU4fawIDAQABo1owWDAJBgNVHRMEAjAAMAsGA1UdDwQEAwIF\n" +
        "4DAdBgNVHQ4EFgQU8TCYvnyq+bGROGCuE19nnAoynjEwHwYDVR0jBBgwFoAUtTJD\n" +
        "1wAkkrrpleX5o2RshO4zLhUwDQYJKoZIhvcNAQEFBQADgYEAFU+fP9FSTQNVZOhv\n" +
        "eJ+zq6wI/biwzTgPbAq3yu2gb5kT85z4nzqBhPd2LWWFXhUW/D8QyNZ54X30y0Ug\n" +
        "3NfUAvOANW7CgUbHBmm77KQiF4nWdh338qqq9HzLGrPqcxX0dmiq2RBVPy9wb2Ea\n" +
        "FTZiU2v+9pkoLoSDnCOfPCg/4Q4=\n" +
        "-----END CERTIFICATE-----";

    static byte privateExponent[] = {
        (byte)0x4c, (byte)0xed, (byte)0x3f, (byte)0x86,
        (byte)0x93, (byte)0x8c, (byte)0x83, (byte)0x1f,
        (byte)0x31, (byte)0x98, (byte)0x91, (byte)0x9c,
        (byte)0xd9, (byte)0x87, (byte)0x9b, (byte)0xfe,
        (byte)0x0c, (byte)0x98, (byte)0xee, (byte)0x4c,
        (byte)0x1f, (byte)0xc8, (byte)0x80, (byte)0x1a,
        (byte)0x8e, (byte)0xcf, (byte)0x4a, (byte)0x87,
        (byte)0x0d, (byte)0x0b, (byte)0x70, (byte)0x34,
        (byte)0xd8, (byte)0x4e, (byte)0x8d, (byte)0x84,
        (byte)0x00, (byte)0x8f, (byte)0xaf, (byte)0x32,
        (byte)0x60, (byte)0xe9, (byte)0x53, (byte)0xe7,
        (byte)0x6f, (byte)0x98, (byte)0xe0, (byte)0x8b,
        (byte)0x52, (byte)0xaa, (byte)0xbf, (byte)0x67,
        (byte)0x6b, (byte)0x62, (byte)0x28, (byte)0x98,
        (byte)0x46, (byte)0xca, (byte)0xb9, (byte)0x9c,
        (byte)0x06, (byte)0x5c, (byte)0x6b, (byte)0x91
    };

    static byte modulus[] = {
        (byte)0x00,
        (byte)0xb9, (byte)0x5f, (byte)0x1a, (byte)0x0f,
        (byte)0xf9, (byte)0x67, (byte)0x07, (byte)0x89,
        (byte)0x71, (byte)0x6e, (byte)0x73, (byte)0xa8,
        (byte)0xb7, (byte)0x67, (byte)0x43, (byte)0x6b,
        (byte)0xb2, (byte)0x8a, (byte)0xb7, (byte)0x2a,
        (byte)0x0c, (byte)0x39, (byte)0x44, (byte)0x34,
        (byte)0xa9, (byte)0xef, (byte)0xf4, (byte)0x50,
        (byte)0x01, (byte)0x3a, (byte)0x6f, (byte)0xe3,
        (byte)0xd3, (byte)0x09, (byte)0xe5, (byte)0x3c,
        (byte)0x7a, (byte)0x97, (byte)0x08, (byte)0x2a,
        (byte)0x85, (byte)0x04, (byte)0xba, (byte)0x0c,
        (byte)0xee, (byte)0xd1, (byte)0xbc, (byte)0xa2,
        (byte)0x17, (byte)0x22, (byte)0xd5, (byte)0x23,
        (byte)0xf2, (byte)0xe9, (byte)0xf2, (byte)0xc9,
        (byte)0xe5, (byte)0xda, (byte)0x8b, (byte)0x7f,
        (byte)0x91, (byte)0x4e, (byte)0x1f, (byte)0x6b
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
     * If the client or server is doing some kind of object creation
     * that the other side depends on, and that thread prematurely
     * exits, you may experience a hang.  The test harness will
     * terminate all hung threads after its timeout has expired,
     * currently 3 minutes by default, but you might try to be
     * smart about it....
     */

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {
        SSLServerSocketFactory sslssf =
                getSSLContext(true).getServerSocketFactory();
        SSLServerSocket sslServerSocket =
                (SSLServerSocket) sslssf.createServerSocket(serverPort);

        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for this connect.
         */
        serverReady = true;

        // Enable RSA_EXPORT cipher suites only.
        try {
            String enabledSuites[] = {
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA"};
            sslServerSocket.setEnabledCipherSuites(enabledSuites);
        } catch (IllegalArgumentException iae) {
            // ignore the exception a cipher suite is unsupported.
        }

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
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

        SSLSocketFactory sslsf =
                getSSLContext(false).getSocketFactory();
        SSLSocket sslSocket = (SSLSocket)
                sslsf.createSocket("localhost", serverPort);

        // Enable RSA_EXPORT cipher suites only.
        try {
            String enabledSuites[] = {
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA"};
            sslSocket.setEnabledCipherSuites(enabledSuites);
        } catch (IllegalArgumentException iae) {
            // ignore the exception a cipher suite is unsupported.
        }

        InputStream sslIS = sslSocket.getInputStream();
        OutputStream sslOS = sslSocket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();

        sslSocket.close();
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String[] args) throws Exception {
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.certpath.disabledAlgorithms", "MD2");
        Security.setProperty("jdk.tls.disabledAlgorithms", "MD2");

        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new RSAExport();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    RSAExport() throws Exception {
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
                        System.err.println("Server died..." + e);
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

    // Get the SSL context
    private SSLContext getSSLContext(boolean authnRequired) throws Exception {
        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream is =
                    new ByteArrayInputStream(trusedCertStr.getBytes());
        Certificate trustedCert = cf.generateCertificate(is);

        // create a key store
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // import the trusted cert
        ks.setCertificateEntry("RSA Export Signer", trustedCert);

        if (authnRequired) {
            // generate the private key.
            RSAPrivateKeySpec priKeySpec = new RSAPrivateKeySpec(
                                            new BigInteger(modulus),
                                            new BigInteger(privateExponent));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPrivateKey priKey =
                    (RSAPrivateKey)kf.generatePrivate(priKeySpec);

            // generate certificate chain
            is = new ByteArrayInputStream(serverCertStr.getBytes());
            Certificate serverCert = cf.generateCertificate(is);

            Certificate[] chain = new Certificate[2];
            chain[0] = serverCert;
            chain[1] = trustedCert;

            // import the key entry.
            ks.setKeyEntry("RSA Export", priKey, passphrase, chain);
        }

        // create SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(ks);

        SSLContext ctx = SSLContext.getInstance("TLS");
        if (authnRequired) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passphrase);

            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } else {
            ctx.init(null, tmf.getTrustManagers(), null);
        }

        return ctx;
    }

}
