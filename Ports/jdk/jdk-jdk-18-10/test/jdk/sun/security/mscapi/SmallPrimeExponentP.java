/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8023546 8151834
 * @modules java.base/sun.security.x509
 *          java.base/sun.security.tools.keytool
 * @summary Test prime exponent (p) lengths 63 and 65 bytes with SunMSCAPI.
 *         The seed 76 has the fastest test execution now (only 5 rounds) and is
 *         hard-coded in run tag. This number might change if algorithms for
 *         RSA key pair generation or BigInteger prime searching gets updated.
 * @requires os.family == "windows"
 * @run main SmallPrimeExponentP 76
 */
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Random;

public class SmallPrimeExponentP {

    public static void main(String argv[]) throws Exception {

        long seed = Long.parseLong(argv[0]);
        System.out.println("Seed for SecureRandom = " + seed + "L");

        KeyStore ks = KeyStore.getInstance("Windows-MY");
        ks.load(null, null);

        CertAndKeyGen ckg = new CertAndKeyGen("RSA", "SHA1withRSA");
        ckg.setRandom(new MySecureRandom(seed));

        String alias = "anything";
        int count = 0;

        boolean see63 = false;
        boolean see65 = false;
        while (!see63 || !see65) {
            ckg.generate(1024);
            RSAPrivateCrtKey k = (RSAPrivateCrtKey) ckg.getPrivateKey();

            int len = k.getPrimeExponentP().toByteArray().length;
            System.out.println("Length of P = " + len);
            if (len == 63 || len == 65) {
                if (len == 63) {
                    if (see63) {
                        continue;
                    } else {
                        see63 = true;
                    }
                }
                if (len == 65) {
                    if (see65) {
                        continue;
                    } else {
                        see65 = true;
                    }
                }
                ks.setKeyEntry(alias, k, null, new X509Certificate[]{
                    ckg.getSelfCertificate(new X500Name("CN=Me"), 1000)
                });
                count++;
            }
        }

        // Because of JDK-8185844, it has to reload the key store after
        // deleting an entry.
        for (int i = 0; i < count; i++) {
            ks.deleteEntry(alias);
            ks.load(null, null);
        }
    }

    static class MySecureRandom extends SecureRandom {

        final Random random;

        public MySecureRandom(long seed) {
            random = new Random(seed);
        }

        @Override
        public void nextBytes(byte[] bytes) {
            random.nextBytes(bytes);
        }
    }
}
