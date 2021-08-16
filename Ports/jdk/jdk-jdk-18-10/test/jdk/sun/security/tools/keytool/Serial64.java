/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8221257 8222275
 * @summary Improve serial number generation mechanism for keytool -gencert
 * @library /test/lib
 * @key randomness
 */

import jdk.test.lib.SecurityTools;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class Serial64 {

    public static void main(String[] args) throws Exception {

        boolean see64 = false;

        see64 |= see64(genkeypair("ca"));
        see64 |= see64(genkeypair("user"));

        keytool("-certreq -alias user -file req");

        for (int i = 3; i <= 30; i++) {
            see64 |= see64(gencert());
            if (i >= 10 && see64) {
                // As long as we have generated >=10 (non-negative) SNs and
                // at least one is 64 bit it's good to go.
                return;
            }
        }

        // None is 64 bit. There is a chance of 2^-30 we reach here.
        // Or, maybe we do have a bug?
        throw new RuntimeException("No 64-bit serial number");
    }

    static boolean see64(BigInteger sn) {
        System.out.println(sn.toString(16));

        if (sn.signum() != 1) {
            throw new RuntimeException("Must be positive");
        }
        return sn.bitLength() == 64;
    }

    static void keytool(String s) throws Exception {
        SecurityTools.keytool("-storepass changeit -keypass changeit "
                + "-keystore ks -keyalg rsa " + s)
                .shouldHaveExitValue(0);
    }

    static BigInteger genkeypair(String a) throws Exception {
        keytool("-genkeypair -alias " + a + " -dname CN=" + a);
        return ((X509Certificate)KeyStore.getInstance(
                new File("ks"), "changeit".toCharArray())
                    .getCertificate(a)).getSerialNumber();
    }

    static BigInteger gencert() throws Exception {
        keytool("-gencert -alias ca -infile req -outfile cert");
        try (FileInputStream fis = new FileInputStream("cert")) {
            return ((X509Certificate)CertificateFactory.getInstance("X.509")
                    .generateCertificate(fis)).getSerialNumber();
        }
    }
}
