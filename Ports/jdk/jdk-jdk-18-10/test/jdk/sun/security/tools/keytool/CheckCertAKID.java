/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8257497
 * @summary Check if issuer's SKID is used to establish the AKID for the subject cert
 * @library /test/lib
 * @modules java.base/sun.security.util
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import sun.security.util.DerValue;
import sun.security.util.KnownOIDs;
import static sun.security.util.KnownOIDs.*;

public class CheckCertAKID {

    static OutputAnalyzer kt(String cmd, String ks) throws Exception {
        return SecurityTools.keytool("-storepass changeit " + cmd +
                " -keystore " + ks);
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Generating a root cert with SubjectKeyIdentifier extension");
        kt("-genkeypair -keyalg rsa -alias ca -dname CN=CA -ext bc:c " +
                "-ext 2.5.29.14=04:14:00:01:02:03:04:05:06:07:08:09:10:11:12:13:14:15:16:17:18:19",
                "ks");

        kt("-exportcert -alias ca -rfc -file root.cert", "ks");

        SecurityTools.keytool("-keystore ks -storepass changeit " +
                "-printcert -file root.cert")
                .shouldNotContain("AuthorityKeyIdentifier")
                .shouldContain("SubjectKeyIdentifier")
                .shouldContain("0000: 00 01 02 03 04 05 06 07   08 09 10 11 12 13 14 15")
                .shouldContain("0010: 16 17 18 19")
                .shouldHaveExitValue(0);

        System.out.println("Generating an end entity cert using issuer CA's SKID as its AKID");
        kt("-genkeypair -keyalg rsa -alias e1 -dname CN=E1", "ks");
        kt("-certreq -alias e1 -file tmp.req", "ks");
        kt("-gencert -alias ca -ext san=dns:e1 -infile tmp.req -outfile tmp.cert ",
                "ks");
        kt("-importcert -alias e1 -file tmp.cert", "ks");

        byte[] expectedId = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19};

        KeyStore kstore = KeyStore.getInstance(new File("ks"),
                "changeit".toCharArray());
        X509Certificate cert = (X509Certificate)kstore.getCertificate("e1");
        byte[] authorityKeyIdExt = cert.getExtensionValue(
                KnownOIDs.AuthorityKeyID.value());

        byte[] authorityKeyId = null;
        if (authorityKeyIdExt == null) {
            System.out.println("Failed to get AKID extension from the cert");
            System.exit(1);
        } else {
            try {
                authorityKeyId = new DerValue(authorityKeyIdExt).getOctetString();
            } catch (IOException e) {
                System.out.println("Failed to get AKID encoded OctetString in the cert");
                System.exit(1);
            }
        }

        authorityKeyId = Arrays.copyOfRange(authorityKeyId, 4, authorityKeyId.length);
        if (!Arrays.equals(authorityKeyId, expectedId)) {
            System.out.println("Failed due to AKID mismatch");
            System.exit(1);
        }
    }
}
