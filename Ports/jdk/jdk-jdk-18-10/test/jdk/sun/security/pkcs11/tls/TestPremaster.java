/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6316539 8136355
 * @summary Basic tests for TlsRsaPremasterSecret generator
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules java.base/sun.security.internal.spec
 *          jdk.crypto.cryptoki
 * @run main/othervm TestPremaster
 * @run main/othervm -Djava.security.manager=allow TestPremaster sm policy
 */

import java.security.Provider;
import java.security.InvalidAlgorithmParameterException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import sun.security.internal.spec.TlsRsaPremasterSecretParameterSpec;

public class TestPremaster extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new TestPremaster(), args);
    }

    @Override
    public void main(Provider provider) throws Exception {
        if (provider.getService(
                "KeyGenerator", "SunTlsRsaPremasterSecret") == null) {
            System.out.println("Not supported by provider, skipping");
            return;
        }
        KeyGenerator kg;
        kg = KeyGenerator.getInstance("SunTlsRsaPremasterSecret", provider);

        try {
            kg.generateKey();
            throw new Exception("no exception");
        } catch (IllegalStateException e) {
            System.out.println("OK: " + e);
        }

        int[] protocolVersions = {0x0300, 0x0301, 0x0302};
        for (int clientVersion : protocolVersions) {
            for (int serverVersion : protocolVersions) {
                test(kg, clientVersion, serverVersion);
                if (serverVersion >= clientVersion) {
                    break;
                }
            }
        }

        System.out.println("Done.");
    }

    private static void test(KeyGenerator kg,
            int clientVersion, int serverVersion) throws Exception {

        System.out.printf(
                "Testing RSA pre-master secret key generation between " +
                "client (0x%04X) and server(0x%04X)%n",
                clientVersion, serverVersion);
        try {
            kg.init(new TlsRsaPremasterSecretParameterSpec(
                                    clientVersion, serverVersion));
        } catch (InvalidAlgorithmParameterException iape) {
            // S12 removed support for SSL v3.0
            if (clientVersion == 0x300 || serverVersion == 0x300) {
                System.out.println("Skip testing SSLv3 due to no support");
                return;
            }
            // unexpected, pass it up
            throw iape;
        }
        SecretKey key = kg.generateKey();
        byte[] encoded = key.getEncoded();
        if (encoded != null) {  // raw key material may be not extractable
            if (encoded.length != 48) {
                throw new Exception("length: " + encoded.length);
            }
            int v = versionOf(encoded[0], encoded[1]);
            if (clientVersion != v) {
                if (serverVersion != v || clientVersion >= 0x0302) {
                    throw new Exception(String.format(
                        "version mismatch: (0x%04X) rather than (0x%04X) " +
                        "is used in pre-master secret", v, clientVersion));
                }
                System.out.printf("Use compatible version (0x%04X)%n", v);
            }
            System.out.println("Passed, version matches!");
       } else {
            System.out.println("Raw key material is not extractable");
       }
    }

    private static int versionOf(int major, int minor) {
        return ((major & 0xFF) << 8) | (minor & 0xFF);
    }

}
