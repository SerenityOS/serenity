/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

// This test case relies on updated static security property, no way to re-use
// security property in samevm/agentvm mode.

/**
 * @test
 *
 * @bug 6861062
 * @summary Disable MD2 support
 *
 * @run main/othervm CPBuilder trustAnchor_SHA1withRSA_1024 0 true
 * @run main/othervm CPBuilder trustAnchor_SHA1withRSA_512  0 true
 * @run main/othervm CPBuilder intermediate_SHA1withRSA_1024_1024 1 true
 * @run main/othervm CPBuilder intermediate_SHA1withRSA_1024_512  1 true
 * @run main/othervm CPBuilder intermediate_SHA1withRSA_512_1024  1 true
 * @run main/othervm CPBuilder intermediate_SHA1withRSA_512_512  1 true
 * @run main/othervm CPBuilder intermediate_MD2withRSA_1024_1024  1 false
 * @run main/othervm CPBuilder intermediate_MD2withRSA_1024_512  1 false
 * @run main/othervm CPBuilder endentiry_SHA1withRSA_1024_1024  2 true
 * @run main/othervm CPBuilder endentiry_SHA1withRSA_1024_512  2 true
 * @run main/othervm CPBuilder endentiry_SHA1withRSA_512_1024  2 true
 * @run main/othervm CPBuilder endentiry_SHA1withRSA_512_512  2 true
 * @run main/othervm CPBuilder endentiry_MD2withRSA_1024_1024  2 false
 * @run main/othervm CPBuilder endentiry_MD2withRSA_1024_512  2 false
 *
 * @author Xuelei Fan
 */

import java.io.*;
import java.net.SocketException;
import java.util.*;
import java.security.Security;
import java.security.cert.*;

public class CPBuilder {

    // SHA1withRSA 1024
    static String trustAnchor_SHA1withRSA_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICPjCCAaegAwIBAgIBADANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDRaFw0zMDA3MTcwMTExNDRa\n" +
        "MB8xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMIGfMA0GCSqGSIb3DQEB\n" +
        "AQUAA4GNADCBiQKBgQC8UdC863pFk1Rvd7xUYd60+e9KsLhb6SqOfU42ZA715FcH\n" +
        "E1TRvQPmYzAnHcO04TrWZQtO6E+E2RCmeBnetBvIMVka688QkO14wnrIrf2tRodd\n" +
        "rZNZEBzkX+zyXCRo9tKEUDFf9Qze7Ilbb+Zzm9CUfu4M1Oz6iQcXRx7aM0jEAQID\n" +
        "AQABo4GJMIGGMB0GA1UdDgQWBBTn0C+xmZY/BTab4W9gBp3dGa7WgjBHBgNVHSME\n" +
        "QDA+gBTn0C+xmZY/BTab4W9gBp3dGa7WgqEjpCEwHzELMAkGA1UEBhMCVVMxEDAO\n" +
        "BgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUwAwEB/zALBgNVHQ8EBAMCAgQw\n" +
        "DQYJKoZIhvcNAQEFBQADgYEAiCXL2Yp4ruyRXAIJ8zBEaPC9oV2agqgbSbly2z8z\n" +
        "Ik5SeSRysP+GHBpb8uNyANJnQKv+T0GrJiTLMBjKCOiJl6xzk3EZ2wbQB6G/SQ9+\n" +
        "UWcsXSC8oGSEPpkj5In/9/UbuUIfT9H8jmdyLNKQvlqgq6kyfnskME7ptGgT95Hc\n" +
        "tas=\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 512
    static String trustAnchor_SHA1withRSA_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIBuTCCAWOgAwIBAgIBADANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDRaFw0zMDA3MTcwMTExNDRa\n" +
        "MB8xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMFwwDQYJKoZIhvcNAQEB\n" +
        "BQADSwAwSAJBAM0Kn4ieCdCHsrm78ZMMN4jQEEEqACAMKB7O8j9g4gfz2oAfmHwv\n" +
        "7JH/hZ0Xen1zUmBbwe+e2J5D/4Fisp9Bn98CAwEAAaOBiTCBhjAdBgNVHQ4EFgQU\n" +
        "g4Kwd47hdNQBp8grZsRJ5XvhvxAwRwYDVR0jBEAwPoAUg4Kwd47hdNQBp8grZsRJ\n" +
        "5XvhvxChI6QhMB8xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlggEAMA8G\n" +
        "A1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgIEMA0GCSqGSIb3DQEBBQUAA0EAn77b\n" +
        "FJx+HvyRvjZYCzMjnUct3Ql4iLOkURYDh93J5TXi/l9ajvAMEuwzYj0qZ+Ktm/ia\n" +
        "U5r+8B9nzx+j2Zh3kw==\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 1024 signed with RSA 1024
    static String intermediate_SHA1withRSA_1024_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICUDCCAbmgAwIBAgIBAjANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDhaFw0yOTA0MjMwMTExNDha\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCVOqnlZspyAEr90ELFaUo8\n" +
        "BF0O2Kn0yTdUeyiLOth4RA3qxWrjxJq45VmEBjZpEzPHfnp3PhnfmLcLfhoPONFg\n" +
        "bcHzlkj75ZaKCgHoyV456fMBmj348fcoUkH2WdSQ82pmxHOiHqquYNUSTimFIq82\n" +
        "AayhbKqDmhfx5lJdYNqd5QIDAQABo4GJMIGGMB0GA1UdDgQWBBTfWD9mRTppcUAl\n" +
        "UqGuu/R5t8CB5jBHBgNVHSMEQDA+gBTn0C+xmZY/BTab4W9gBp3dGa7WgqEjpCEw\n" +
        "HzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUw\n" +
        "AwEB/zALBgNVHQ8EBAMCAgQwDQYJKoZIhvcNAQEFBQADgYEAHze3wAcIe84zNOoN\n" +
        "P8l9EmlVVoU30z3LB3hxq3m/dC/4gE5Z9Z8EG1wJw4qaxlTZ4dif12nbTTdofVhb\n" +
        "Bd4syjo6fcUA4q7sfg9TFpoHQ+Ap7PgjK99moMKdMy50Xy8s6FPvaVkF89s66Z6y\n" +
        "e4q7TSwe6QevGOZaL5N/iy2XGEs=\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 1024 signed with RSA 512
    static String intermediate_SHA1withRSA_1024_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICDzCCAbmgAwIBAgIBAzANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDlaFw0yOTA0MjMwMTExNDla\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCVOqnlZspyAEr90ELFaUo8\n" +
        "BF0O2Kn0yTdUeyiLOth4RA3qxWrjxJq45VmEBjZpEzPHfnp3PhnfmLcLfhoPONFg\n" +
        "bcHzlkj75ZaKCgHoyV456fMBmj348fcoUkH2WdSQ82pmxHOiHqquYNUSTimFIq82\n" +
        "AayhbKqDmhfx5lJdYNqd5QIDAQABo4GJMIGGMB0GA1UdDgQWBBTfWD9mRTppcUAl\n" +
        "UqGuu/R5t8CB5jBHBgNVHSMEQDA+gBSDgrB3juF01AGnyCtmxEnle+G/EKEjpCEw\n" +
        "HzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUw\n" +
        "AwEB/zALBgNVHQ8EBAMCAgQwDQYJKoZIhvcNAQEFBQADQQCYNmdkONfuk07XjRze\n" +
        "WQyq2cfdae4uIdyUfa2rpgYMtSXuQW3/XrQGiz4G6WBXA2wo7folOOpAKYgvHPrm\n" +
        "w6Dd\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 512 signed with RSA 1024
    static String intermediate_SHA1withRSA_512_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICDDCCAXWgAwIBAgIBBDANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDlaFw0yOTA0MjMwMTExNDla\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKubXYoEHZpZkhzA9XX+NrpqJ4SV\n" +
        "lOMBoL3aWExQpJIgrUaZfbGMBBozIHBJMMayokguHbJvq4QigEgLuhfJNqsCAwEA\n" +
        "AaOBiTCBhjAdBgNVHQ4EFgQUN0CHiTYPtjyvpP2a6y6mhsZ6U40wRwYDVR0jBEAw\n" +
        "PoAU59AvsZmWPwU2m+FvYAad3Rmu1oKhI6QhMB8xCzAJBgNVBAYTAlVTMRAwDgYD\n" +
        "VQQKEwdFeGFtcGxlggEAMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgIEMA0G\n" +
        "CSqGSIb3DQEBBQUAA4GBAE2VOlw5ySLT3gUzKCYEga4QPaSrf6lHHPi2g48LscEY\n" +
        "h9qQXh4nuIVugReBIEf6N49RdT+M2cgRJo4sZ3ukYLGQzxNuttL5nPSuuvrAR1oG\n" +
        "LUyzOWcUpKHbVHi6zlTt79RvTKZvLcduLutmtPtLJcM9PdiAI1wEooSgxTwZtB/Z\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 512 signed with RSA 512
    static String intermediate_SHA1withRSA_512_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIByzCCAXWgAwIBAgIBBTANBgkqhkiG9w0BAQUFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDlaFw0yOTA0MjMwMTExNDla\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKubXYoEHZpZkhzA9XX+NrpqJ4SV\n" +
        "lOMBoL3aWExQpJIgrUaZfbGMBBozIHBJMMayokguHbJvq4QigEgLuhfJNqsCAwEA\n" +
        "AaOBiTCBhjAdBgNVHQ4EFgQUN0CHiTYPtjyvpP2a6y6mhsZ6U40wRwYDVR0jBEAw\n" +
        "PoAUg4Kwd47hdNQBp8grZsRJ5XvhvxChI6QhMB8xCzAJBgNVBAYTAlVTMRAwDgYD\n" +
        "VQQKEwdFeGFtcGxlggEAMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgIEMA0G\n" +
        "CSqGSIb3DQEBBQUAA0EAoCf0Zu559qcB4xPpzqkVsYiyW49S4Yc0mmQXb1yoQgLx\n" +
        "O+DCkjG5d14+t1MsnkhB2izoQUMxQ3vDc1YnA/tEpw==\n" +
        "-----END CERTIFICATE-----";

    // MD2withRSA 1024 signed with RSA 1024
    static String intermediate_MD2withRSA_1024_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICUDCCAbmgAwIBAgIBBjANBgkqhkiG9w0BAQIFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDlaFw0yOTA0MjMwMTExNDla\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCVOqnlZspyAEr90ELFaUo8\n" +
        "BF0O2Kn0yTdUeyiLOth4RA3qxWrjxJq45VmEBjZpEzPHfnp3PhnfmLcLfhoPONFg\n" +
        "bcHzlkj75ZaKCgHoyV456fMBmj348fcoUkH2WdSQ82pmxHOiHqquYNUSTimFIq82\n" +
        "AayhbKqDmhfx5lJdYNqd5QIDAQABo4GJMIGGMB0GA1UdDgQWBBTfWD9mRTppcUAl\n" +
        "UqGuu/R5t8CB5jBHBgNVHSMEQDA+gBTn0C+xmZY/BTab4W9gBp3dGa7WgqEjpCEw\n" +
        "HzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUw\n" +
        "AwEB/zALBgNVHQ8EBAMCAgQwDQYJKoZIhvcNAQECBQADgYEAPtEjwbWuC5kc4DPc\n" +
        "Ttf/wdbD8ZCdAWzcc3XF9q1TlvwVMNk6mbfM05y6ZVsztKTkwZ4EcvFu/yIqw1EB\n" +
        "E1zlXQCaWXT3/ZMbqYZV4+mx+RUl8spUCb1tda25jnTg3mTOzB1iztm4gy903EMd\n" +
        "m8omKDKeCgcw5dR4ITQYvyxe1as=\n" +
        "-----END CERTIFICATE-----";

    // MD2withRSA 1024 signed with RSA 512
    static String intermediate_MD2withRSA_1024_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICDzCCAbmgAwIBAgIBBzANBgkqhkiG9w0BAQIFADAfMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTAeFw0wOTA4MDYwMTExNDlaFw0yOTA0MjMwMTExNDla\n" +
        "MDExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFtcGxlMRAwDgYDVQQLEwdDbGFz\n" +
        "cy0xMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCVOqnlZspyAEr90ELFaUo8\n" +
        "BF0O2Kn0yTdUeyiLOth4RA3qxWrjxJq45VmEBjZpEzPHfnp3PhnfmLcLfhoPONFg\n" +
        "bcHzlkj75ZaKCgHoyV456fMBmj348fcoUkH2WdSQ82pmxHOiHqquYNUSTimFIq82\n" +
        "AayhbKqDmhfx5lJdYNqd5QIDAQABo4GJMIGGMB0GA1UdDgQWBBTfWD9mRTppcUAl\n" +
        "UqGuu/R5t8CB5jBHBgNVHSMEQDA+gBSDgrB3juF01AGnyCtmxEnle+G/EKEjpCEw\n" +
        "HzELMAkGA1UEBhMCVVMxEDAOBgNVBAoTB0V4YW1wbGWCAQAwDwYDVR0TAQH/BAUw\n" +
        "AwEB/zALBgNVHQ8EBAMCAgQwDQYJKoZIhvcNAQECBQADQQBHok1v6xymtpB7N9xy\n" +
        "0OmDT27uhmzlP0eOzJvXVxj3Oi9TLQJgCUJ9122MzfRAs1E1uJTtvuu+UmI80NQx\n" +
        "KQdp\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 1024 signed with RSA 1024
    static String endentiry_SHA1withRSA_1024_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICNzCCAaCgAwIBAgIBAjANBgkqhkiG9w0BAQUFADAxMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTEQMA4GA1UECxMHQ2xhc3MtMTAeFw0wOTA4MDYwMTEx\n" +
        "NTBaFw0yOTA0MjMwMTExNTBaMEExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFt\n" +
        "cGxlMRAwDgYDVQQLEwdDbGFzcy0xMQ4wDAYDVQQDEwVBbGljZTCBnzANBgkqhkiG\n" +
        "9w0BAQEFAAOBjQAwgYkCgYEAy6/2g3rxQzJEvTyOnBcEnZthmAD0AnP6LG8b35jt\n" +
        "vh71LHbF1FhkOT42Rfg20aBfWTMRf+FeOJBXpD4gCNjQA40vy8FaQxgYNAf7ho5v\n" +
        "z6yAEE6SG7YviE+XGcvpQo47w8c6QSQjpBzdw7JxwbVlzUT7pF8x3RnXlGhWnWv6\n" +
        "c1ECAwEAAaNPME0wCwYDVR0PBAQDAgPoMB0GA1UdDgQWBBSaXXERsow2Wm/6uT07\n" +
        "OorBleV92TAfBgNVHSMEGDAWgBTfWD9mRTppcUAlUqGuu/R5t8CB5jANBgkqhkiG\n" +
        "9w0BAQUFAAOBgQAOfIeasDg91CR3jGfuAEVKwncM1OPFmniAUcdPm74cCAyJ90Me\n" +
        "dhUElWPGoAuXGfiyZlOlGUYWqEroe/dnkmnotJjLWR+MA4ZyX3O1YI8T4W3deWcC\n" +
        "J4WMCF7mp17SaYYKX9F0AxwNJFpUkbB41IkTxPr0MmzB1871/pbY8dLAvA==\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 1024 signed with RSA 512
    static String endentiry_SHA1withRSA_1024_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIB9jCCAaCgAwIBAgIBAzANBgkqhkiG9w0BAQUFADAxMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTEQMA4GA1UECxMHQ2xhc3MtMTAeFw0wOTA4MDYwMTEx\n" +
        "NTBaFw0yOTA0MjMwMTExNTBaMEExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFt\n" +
        "cGxlMRAwDgYDVQQLEwdDbGFzcy0xMQ4wDAYDVQQDEwVBbGljZTCBnzANBgkqhkiG\n" +
        "9w0BAQEFAAOBjQAwgYkCgYEAy6/2g3rxQzJEvTyOnBcEnZthmAD0AnP6LG8b35jt\n" +
        "vh71LHbF1FhkOT42Rfg20aBfWTMRf+FeOJBXpD4gCNjQA40vy8FaQxgYNAf7ho5v\n" +
        "z6yAEE6SG7YviE+XGcvpQo47w8c6QSQjpBzdw7JxwbVlzUT7pF8x3RnXlGhWnWv6\n" +
        "c1ECAwEAAaNPME0wCwYDVR0PBAQDAgPoMB0GA1UdDgQWBBSaXXERsow2Wm/6uT07\n" +
        "OorBleV92TAfBgNVHSMEGDAWgBQ3QIeJNg+2PK+k/ZrrLqaGxnpTjTANBgkqhkiG\n" +
        "9w0BAQUFAANBADV6X+ea0ftEKXy7yKNAbdIp35893T6AVwbdclomPkeOs86OtoTG\n" +
        "1BIzWSK9QE7W6Wbf63e2RdcqoLK+DxsuwUg=\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 512 signed with RSA 1024
    static String endentiry_SHA1withRSA_512_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIB8zCCAVygAwIBAgIBBDANBgkqhkiG9w0BAQUFADAxMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTEQMA4GA1UECxMHQ2xhc3MtMTAeFw0wOTA4MDYwMTEx\n" +
        "NTFaFw0yOTA0MjMwMTExNTFaMEExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFt\n" +
        "cGxlMRAwDgYDVQQLEwdDbGFzcy0xMQ4wDAYDVQQDEwVBbGljZTBcMA0GCSqGSIb3\n" +
        "DQEBAQUAA0sAMEgCQQCpfQzhld7w2JhW/aRaLkmrLrc/QAsQE+J4DXioXaajsWPo\n" +
        "uMmYmuiQolb6OIY/LcivSubKM3G5PkAWoovUPIWLAgMBAAGjTzBNMAsGA1UdDwQE\n" +
        "AwID6DAdBgNVHQ4EFgQUFWuXLkf4Ji57H9ISycgWi982TUIwHwYDVR0jBBgwFoAU\n" +
        "31g/ZkU6aXFAJVKhrrv0ebfAgeYwDQYJKoZIhvcNAQEFBQADgYEAUyW8PrEdbzLu\n" +
        "B+h6UemBOJ024rYq90hJE/5wUEKPvxZ9vPEUgl+io6cGhL3cLfxfh6z5xtEGp4Tb\n" +
        "NB0Ye3Qi01FBiNDY8s3rQRrmel6VysU8u+0Oi2jmQY6vZXn/zXN5rrTLITCaSicG\n" +
        "dOMv1xLM83Ee432WWlDwKOUxhzDGpWc=\n" +
        "-----END CERTIFICATE-----";

    // SHA1withRSA 512 signed with RSA 512
    static String endentiry_SHA1withRSA_512_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIBsjCCAVygAwIBAgIBBTANBgkqhkiG9w0BAQUFADAxMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTEQMA4GA1UECxMHQ2xhc3MtMTAeFw0wOTA4MDYwMTEx\n" +
        "NTFaFw0yOTA0MjMwMTExNTFaMEExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFt\n" +
        "cGxlMRAwDgYDVQQLEwdDbGFzcy0xMQ4wDAYDVQQDEwVBbGljZTBcMA0GCSqGSIb3\n" +
        "DQEBAQUAA0sAMEgCQQCpfQzhld7w2JhW/aRaLkmrLrc/QAsQE+J4DXioXaajsWPo\n" +
        "uMmYmuiQolb6OIY/LcivSubKM3G5PkAWoovUPIWLAgMBAAGjTzBNMAsGA1UdDwQE\n" +
        "AwID6DAdBgNVHQ4EFgQUFWuXLkf4Ji57H9ISycgWi982TUIwHwYDVR0jBBgwFoAU\n" +
        "N0CHiTYPtjyvpP2a6y6mhsZ6U40wDQYJKoZIhvcNAQEFBQADQQBG4grtrVEHick0\n" +
        "z/6Lcl/MGyHT0c8KTXE0AMVXG1NRjAicAmYno/yDaJ9OmfymObKZKV9fF7yCW/N/\n" +
        "TMU6m7N0\n" +
        "-----END CERTIFICATE-----";

    // MD2withRSA 1024 signed with RSA 1024
    static String endentiry_MD2withRSA_1024_1024 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIICNzCCAaCgAwIBAgIBBjANBgkqhkiG9w0BAQIFADAxMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTEQMA4GA1UECxMHQ2xhc3MtMTAeFw0wOTA4MDYwMTEx\n" +
        "NTFaFw0yOTA0MjMwMTExNTFaMEExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFt\n" +
        "cGxlMRAwDgYDVQQLEwdDbGFzcy0xMQ4wDAYDVQQDEwVBbGljZTCBnzANBgkqhkiG\n" +
        "9w0BAQEFAAOBjQAwgYkCgYEAy6/2g3rxQzJEvTyOnBcEnZthmAD0AnP6LG8b35jt\n" +
        "vh71LHbF1FhkOT42Rfg20aBfWTMRf+FeOJBXpD4gCNjQA40vy8FaQxgYNAf7ho5v\n" +
        "z6yAEE6SG7YviE+XGcvpQo47w8c6QSQjpBzdw7JxwbVlzUT7pF8x3RnXlGhWnWv6\n" +
        "c1ECAwEAAaNPME0wCwYDVR0PBAQDAgPoMB0GA1UdDgQWBBSaXXERsow2Wm/6uT07\n" +
        "OorBleV92TAfBgNVHSMEGDAWgBTfWD9mRTppcUAlUqGuu/R5t8CB5jANBgkqhkiG\n" +
        "9w0BAQIFAAOBgQBxKsFf8NNQcXjDoKJJSG4Rk6ikcrhiGYuUI32+XHvs6hnav1Zc\n" +
        "aJUpy7J4gMj/MnysMh/4AF9+m6zEEjuisXKUbYZhgtJxz+ukGSo163mJ8QJiAlRb\n" +
        "Iwsy81r08mlSCR6jx2YhDAUxJIPC92R5Vb4CEutB7tWTwwz7vIHq330erA==\n" +
        "-----END CERTIFICATE-----";

    // MD2withRSA 1024 signed with RSA 512
    static String endentiry_MD2withRSA_1024_512 =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIB9jCCAaCgAwIBAgIBBzANBgkqhkiG9w0BAQIFADAxMQswCQYDVQQGEwJVUzEQ\n" +
        "MA4GA1UEChMHRXhhbXBsZTEQMA4GA1UECxMHQ2xhc3MtMTAeFw0wOTA4MDYwMTEx\n" +
        "NTFaFw0yOTA0MjMwMTExNTFaMEExCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFeGFt\n" +
        "cGxlMRAwDgYDVQQLEwdDbGFzcy0xMQ4wDAYDVQQDEwVBbGljZTCBnzANBgkqhkiG\n" +
        "9w0BAQEFAAOBjQAwgYkCgYEAy6/2g3rxQzJEvTyOnBcEnZthmAD0AnP6LG8b35jt\n" +
        "vh71LHbF1FhkOT42Rfg20aBfWTMRf+FeOJBXpD4gCNjQA40vy8FaQxgYNAf7ho5v\n" +
        "z6yAEE6SG7YviE+XGcvpQo47w8c6QSQjpBzdw7JxwbVlzUT7pF8x3RnXlGhWnWv6\n" +
        "c1ECAwEAAaNPME0wCwYDVR0PBAQDAgPoMB0GA1UdDgQWBBSaXXERsow2Wm/6uT07\n" +
        "OorBleV92TAfBgNVHSMEGDAWgBQ3QIeJNg+2PK+k/ZrrLqaGxnpTjTANBgkqhkiG\n" +
        "9w0BAQIFAANBAIX63Ypi9P71RnC/pcMbhD+wekRFsTzU593X3MC7tyBJtEXwvAZG\n" +
        "iMxXF5A+ohlr7/CrkV7ZTL8PLxnJdY5Y8rQ=\n" +
        "-----END CERTIFICATE-----";

    static HashMap<String, String> certmap = new HashMap<String, String>();
    static {
        certmap.put("trustAnchor_SHA1withRSA_1024",
                                    trustAnchor_SHA1withRSA_1024);
        certmap.put("trustAnchor_SHA1withRSA_512",
                                    trustAnchor_SHA1withRSA_512);
        certmap.put("intermediate_SHA1withRSA_1024_1024",
                                    intermediate_SHA1withRSA_1024_1024);
        certmap.put("intermediate_SHA1withRSA_1024_512",
                                    intermediate_SHA1withRSA_1024_512);
        certmap.put("intermediate_SHA1withRSA_512_1024",
                                    intermediate_SHA1withRSA_512_1024);
        certmap.put("intermediate_SHA1withRSA_512_512",
                                    intermediate_SHA1withRSA_512_512);
        certmap.put("intermediate_MD2withRSA_1024_1024",
                                    intermediate_MD2withRSA_1024_1024);
        certmap.put("intermediate_MD2withRSA_1024_512",
                                    intermediate_MD2withRSA_1024_512);
        certmap.put("endentiry_SHA1withRSA_1024_1024",
                                    endentiry_SHA1withRSA_1024_1024);
        certmap.put("endentiry_SHA1withRSA_1024_512",
                                    endentiry_SHA1withRSA_1024_512);
        certmap.put("endentiry_SHA1withRSA_512_1024",
                                    endentiry_SHA1withRSA_512_1024);
        certmap.put("endentiry_SHA1withRSA_512_512",
                                    endentiry_SHA1withRSA_512_512);
        certmap.put("endentiry_MD2withRSA_1024_1024",
                                    endentiry_MD2withRSA_1024_1024);
        certmap.put("endentiry_MD2withRSA_1024_512",
                                    endentiry_MD2withRSA_1024_512);
    }

    private static Set<TrustAnchor> generateTrustAnchors()
            throws CertificateException {
        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        HashSet<TrustAnchor> anchors = new HashSet<TrustAnchor>();

        ByteArrayInputStream is =
            new ByteArrayInputStream(trustAnchor_SHA1withRSA_1024.getBytes());
        Certificate cert = cf.generateCertificate(is);
        TrustAnchor anchor = new TrustAnchor((X509Certificate)cert, null);
        anchors.add(anchor);

        is = new ByteArrayInputStream(trustAnchor_SHA1withRSA_512.getBytes());
        cert = cf.generateCertificate(is);
        anchor = new TrustAnchor((X509Certificate)cert, null);
        anchors.add(anchor);

        return anchors;
    }

    private static CertStore generateCertificateStore() throws Exception {
        Collection entries = new HashSet();

        // generate certificate from certificate string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        for (String key : certmap.keySet()) {
            String certStr = certmap.get(key);
            ByteArrayInputStream is =
                        new ByteArrayInputStream(certStr.getBytes());;
            Certificate cert = cf.generateCertificate(is);
            entries.add(cert);
        }

        return CertStore.getInstance("Collection",
                            new CollectionCertStoreParameters(entries));
    }

    private static X509CertSelector generateSelector(String name)
                throws Exception {
        X509CertSelector selector = new X509CertSelector();

        String certStr = certmap.get(name);
        if (certStr == null) {
            return null;
        }

        // generate certificate from certificate string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream is = new ByteArrayInputStream(certStr.getBytes());
        X509Certificate target = (X509Certificate)cf.generateCertificate(is);

        selector.setCertificate(target);

        return selector;
    }

    private static boolean match(String name, Certificate cert)
                throws Exception {
        X509CertSelector selector = new X509CertSelector();

        String certStr = certmap.get(name);
        if (certStr == null) {
            return false;
        }

        // generate certificate from certificate string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream is = new ByteArrayInputStream(certStr.getBytes());
        X509Certificate target = (X509Certificate)cf.generateCertificate(is);

        return target.equals(cert);
    }

    public static void main(String args[]) throws Exception {
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.certpath.disabledAlgorithms", "MD2");

        CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");

        X509CertSelector selector = generateSelector(args[0]);
        if (selector == null) {
            // no target certificate, ignore it
            return;
        }

        Set<TrustAnchor> anchors = generateTrustAnchors();
        CertStore certs = generateCertificateStore();

        PKIXBuilderParameters params =
                new PKIXBuilderParameters(anchors, selector);
        params.addCertStore(certs);
        params.setRevocationEnabled(false);
        params.setDate(new Date(109, 9, 1));   // 2009-09-01

        boolean success = Boolean.valueOf(args[2]);
        try {
            PKIXCertPathBuilderResult result =
                        (PKIXCertPathBuilderResult)builder.build(params);
            if (!success) {
                throw new Exception("expected algorithm disabled exception");
            }

            int length = Integer.parseInt(args[1]);
            List<? extends Certificate> path =
                                    result.getCertPath().getCertificates();
            if (length != path.size()) {
                throw new Exception("unexpected certification path length");
            }

            if (!path.isEmpty()) {    // the target is not a trust anchor
                if (!match(args[0], path.get(0))) {
                    throw new Exception("unexpected certificate");
                }
            }
        } catch (CertPathBuilderException cpbe) {
            if (success) {
                throw new Exception("unexpected exception");
            } else {
                System.out.println("Get the expected exception " + cpbe);
            }
        }
    }

}
