/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4856966 8242332
 * @summary
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm ReinitMac
 * @run main/othervm -Djava.security.manager=allow ReinitMac sm
 */

import java.security.Provider;
import java.util.Random;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ReinitMac extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new ReinitMac(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        List<String> algorithms = getSupportedAlgorithms("Mac", "Hmac", p);
        Random random = new Random();
        byte[] data = new byte[10 * 1024];
        random.nextBytes(data);
        byte[] keyVal = new byte[16];
        random.nextBytes(keyVal);

        boolean success = true;
        for (String alg : algorithms) {
            try {
                doTest(alg, p, keyVal, data);
            } catch (Exception e) {
                System.out.println("Unexpected exception: " + e);
                e.printStackTrace();
                success = false;
            }
        }

        if (!success) {
            throw new RuntimeException("Test failed");
        } else {
            System.out.println("All tests passed");
        }
    }

    private void doTest(String alg, Provider p, byte[] keyVal, byte[] data)
            throws Exception {
        System.out.println("Testing " + alg);
        SecretKeySpec key = new SecretKeySpec(keyVal, alg);
        Mac mac = Mac.getInstance(alg, p);
        mac.init(key);
        mac.init(key);
        mac.update(data);
        mac.init(key);
        mac.doFinal();
        mac.doFinal();
        mac.update(data);
        mac.doFinal();
        mac.reset();
        mac.reset();
        mac.init(key);
        mac.reset();
        mac.update(data);
        mac.reset();
    }
}
