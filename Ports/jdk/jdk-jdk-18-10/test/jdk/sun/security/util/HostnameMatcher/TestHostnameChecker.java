/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4514108
 * @summary Verify host name matching behaves as defined in RFC2818.
 * @library /test/lib
 * @modules java.base/sun.security.util
 */

import java.security.cert.*;

import jdk.test.lib.security.CertUtils;
import sun.security.util.*;

/**
 * Certificate 1:
 *   . no subject alternative names, most specific CN is foo1.com.
 *     (also test if serialnumber attribute is ignored)
 * <pre>
    [
    [
      Version: V1
      Subject: SERIALNUMBER=12 + CN=foo1.com, CN=foo2.com, O=Sun, C=US
      Signature Algorithm: SHA1withRSA, OID = 1.2.840.113549.1.1.5

      Key:  com.sun.net.ssl.internal.ssl.JSA_RSAPublicKey@1d8957f
      Validity: [From: Fri Mar 22 12:22:57 GMT 2002,
                   To: Tue Mar 22 12:22:57 GMT 2022]
      Issuer: CN=CA, O=Sun, C=US
      SerialNumber: [  0  ]

    ]
      Algorithm: [SHA1withRSA]
      Signature:
    0000: B8 E1 F8 A8 23 EB C4 E9   F9 03 F6 97 FA DD A2 56  ....#..........V
    0010: 06 B6 95 99 68 6B F8 72   6A 27 F4 13 CC 40 06 83  ....hk.rj'...@..
    0020: 62 C1 94 72 63 EA 69 FD   78 A2 06 D8 3B F5 D0 2C  b..rc.i.x...;..,
    0030: 97 D6 29 84 FA 6F 2E E1   13 6A C7 5D DE 2F 6A 2F  ..)..o...j.]./j/
    0040: 08 85 43 3B 7D DB C1 AB   1A DC 38 BE F7 4B 6B 82  ..C;......8..Kk.
    0050: 84 06 FA 46 82 77 42 A6   47 55 33 63 2B D4 05 2E  ...F.wB.GU3c+...
    0060: 76 F5 1B 6A CD 8E B2 B3   83 8E 9D 13 BC 82 0C 4B  v..j...........K
    0070: DE 8A 63 B5 EA F1 07 00   C0 7E C9 75 DF 13 FC 34  ..c........u...4

    ]
 * </pre>
 *
 * Certificate 2:
 *   . CN contains IP address, must be ignored.
 * <pre>
    [
    [
      Version: V1
      Subject: CN=1.2.3.4, O=Sun, C=US
      Signature Algorithm: SHA1withRSA, OID = 1.2.840.113549.1.1.5

      Key:  com.sun.net.ssl.internal.ssl.JSA_RSAPublicKey@173831b
      Validity: [From: Fri Mar 22 12:22:57 GMT 2002,
                   To: Tue Mar 22 12:22:57 GMT 2022]
      Issuer: CN=CA, O=Sun, C=US
      SerialNumber: [    01]

    ]
      Algorithm: [SHA1withRSA]
      Signature:
    0000: 15 86 83 1E 79 7F 8B 06   1F E0 BF 79 0F EA 84 D1  ....y......y....
    0010: C5 CD 2C D6 9D 4A 36 7C   75 41 E6 0D 8C 1D 65 60  ..,..J6.uA....e`
    0020: 0F 53 15 54 41 43 AE F9   E9 54 34 8A 4B B9 39 12  .S.TAC...T4.K.9.
    0030: DE 58 21 86 D3 F9 11 6C   4F 72 EF 8C 4B C3 66 FC  .X!....lOr..K.f.
    0040: BD AB 87 63 8F 59 1D C3   FE 76 DB B7 76 43 C2 A1  ...c.Y...v..vC..
    0050: 4D D7 92 C4 CA C6 DC 59   CA A6 1B 6D FE 01 AC F4  M......Y...m....
    0060: 09 86 D8 A1 40 C4 C2 77   BF 53 21 9A 3B 43 2E 9A  ....@..w.S!.;C..
    0070: D4 0C BE 85 47 A5 02 35   7D EE 27 11 36 0E 80 14  ....G..5..'.6...

    ]
 * </pre>
 *
 * Certificate 3:
 *   . Contains subject alternative name extension with DNS altfoo1.com,
 *     DNS altfoo2.com, IP 5.6.7.8. CNs in subject DN must be ignored.
 * <pre>
    [
    [
      Version: V3
      Subject: SERIALNUMBER=12 + CN=foo1.com, CN=foo2.com, O=Sun, C=US
      Signature Algorithm: SHA1withRSA, OID = 1.2.840.113549.1.1.5

      Key:  com.sun.net.ssl.internal.ssl.JSA_RSAPublicKey@18e2b22
      Validity: [From: Fri Mar 22 12:22:57 GMT 2002,
                   To: Tue Mar 22 12:22:57 GMT 2022]
      Issuer: CN=CA, O=Sun, C=US
      SerialNumber: [    02]

    Certificate Extensions: 1
    [1]: ObjectId: 2.5.29.17 Criticality=false
    SubjectAlternativeName [
    [DNSName: altfoo1.com, DNSName: altfoo2.com, IPAddress: 5.6.7.8]]

    ]
      Algorithm: [SHA1withRSA]
      Signature:
    0000: 65 A6 E5 96 4A D5 8F 8D   3E 70 7D 63 BE B1 58 1C  e...J...>p.c..X.
    0010: B5 35 EF 7D E8 00 9C 9E   56 E7 E9 52 71 7B BD 35  .5......V..Rq..5
    0020: 3D B5 F5 F7 B2 49 A0 E4   23 BB 2A 0A 25 84 0E E2  =....I..#.*.%...
    0030: 4B 6D 61 73 D5 C8 F1 0A   EC 2B F2 98 3D 80 F9 DC  Kmas.....+..=...
    0040: F5 D1 2A 36 44 EB 59 9A   E9 DF 97 FA AE C0 86 F1  ..*6D.Y.........
    0050: 2C 7B 54 21 F2 3A 56 83   0D E0 5A E1 0D FC D5 E2  ,.T!.:V...Z.....
    0060: 45 44 ED C8 C6 F8 26 8E   0C 14 ED D0 F7 37 1C 01  ED....&......7..
    0070: A5 E2 61 29 5D 14 B3 5A   EF 72 CC 9B 13 05 B0 B1  ..a)]..Z.r......

    ]
 * </pre>
 *
 * Certificate 4:
 *   . SubjAltName contains wildcard char, in the leftmost as well as
 *     in the middle component.
 * <pre>
   [
   [
    Version: V3
    Subject: SERIALNUMBER=12, O=Sun, C=US
    Signature Algorithm: SHA1withRSA, OID = 1.2.840.113549.1.1.5

    Key:  com.sun.net.ssl.internal.ssl.JSA_RSAPublicKey@17a8bd
    Validity: [From: Mon Apr 01 22:32:38 PST 2002,
               To: Fri Apr 01 22:32:38 PST 2022]
    Issuer: CN=CA, O=Sun, C=US
    SerialNumber: [    02]

    Certificate Extensions: 1
    [1]: ObjectId: 2.5.29.17 Criticality=false
    SubjectAlternativeName [

    [DNSName: f*.bar.com, DNSName: altfoo.b*.com]]

   ]
   Algorithm: [SHA1withRSA]
   Signature:
   0000: BB 9B 00 DB C9 94 15 34   03 9F 28 59 20 4D 10 63  .......4..(Y M.c
   0010: 63 6A F5 C8 56 7B 7C CC   E7 06 7E 1E BE 0B 84 92  cj..V...........
   0020: 05 0A 12 5E 21 5D 70 03   DA 27 0B 4E 39 67 FC 8D  ...^!]p..'.N9g..
   0030: D6 FC A6 5B F5 CA F1 4C   75 53 33 E3 4F 30 88 68  ...[...LuS3.O0.h
   0040: B8 F1 2C DF C2 A8 71 A7   37 55 66 7C 51 23 BB C2  ..,...q.7Uf.Q#..
   0050: 25 70 EA EE 44 ED 32 63   56 E0 BA C8 94 36 87 E2  %p..D.2cV....6..
   0060: 94 00 2A 3D 4C 46 78 E9   2B 20 9F 73 3C 8E 92 2E  ..*=LFx.+ .s<...
   0070: 66 85 C6 27 3A 84 21 94   82 93 B1 A4 94 B6 DE 2F  f..':.!......../

   ]
 * </pre>
 *
 */

public class TestHostnameChecker {

    public static void main(String[] args) throws Exception {
        X509Certificate cert1 = CertUtils.getCertFromFile("cert1.crt");
        X509Certificate cert2 = CertUtils.getCertFromFile("cert2.crt");
        X509Certificate cert3 = CertUtils.getCertFromFile("cert3.crt");
        X509Certificate cert4 = CertUtils.getCertFromFile("cert4.crt");
        X509Certificate cert5 = CertUtils.getCertFromFile("cert5.crt");

        HostnameChecker checker = HostnameChecker.getInstance(
                HostnameChecker.TYPE_TLS);
        System.out.println("TLS tests.........");
        System.out.println("==================");
        check(checker, "foo1.com", cert1, true);
        check(checker, "foo2.com", cert1, false);
        check(checker, "1.2.3.4", cert2, false);
        check(checker, "foo1.com", cert3, false);
        check(checker, "foo2.com", cert3, false);
        check(checker, "altfoo1.com", cert3, true);
        check(checker, "altfoo2.com", cert3, true);
        check(checker, "5.6.7.8", cert3, true);
        check(checker, "foo.bar.com", cert4, true);
        check(checker, "altfoo.bar.com", cert4, true);
        check(checker, "2001:db8:3c4d:15::1a2f:1a2b", cert5, true);
        check(checker, "2001:0db8:3c4d:0015:0000:0000:1a2f:1a2b", cert5, true);
        check(checker, "2002:db8:3c4d:15::1a2f:1a2b", cert5, false);

        checker = HostnameChecker.getInstance(
                                HostnameChecker.TYPE_LDAP);
        System.out.println();
        System.out.println("LDAP tests.........");
        System.out.println("==================");
        check(checker, "foo1.com", cert1, true);
        check(checker, "foo2.com", cert1, false);
        check(checker, "foo1.com", cert3, false);
        check(checker, "foo2.com", cert3, false);
        check(checker, "altfoo1.com", cert3, true);
        check(checker, "altfoo2.com", cert3, true);
        check(checker, "5.6.7.8", cert3, true);
        check(checker, "foo.bar.com", cert4, true);
        check(checker, "altfoo.bar.com", cert4, false);
    }

    private static void check(HostnameChecker checker, String name,
                 X509Certificate cert, boolean expectedResult)
                 throws Exception {
        try {
            checker.match(name, cert);
            if (expectedResult == false) {
                throw new Exception("Passed invalid test: " + name);
            }
        } catch (CertificateException e) {
            if (expectedResult == true) {
                throw e;
            }
        }
        System.out.println("OK: " + name);
    }
}
