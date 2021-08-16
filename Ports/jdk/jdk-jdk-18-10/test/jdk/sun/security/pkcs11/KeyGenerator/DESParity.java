/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4898479
 * @summary Verify that the parity bits are set correctly
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm DESParity
 * @run main/othervm -Djava.security.manager=allow DESParity sm
 */

import java.security.Provider;
import java.util.Random;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class DESParity extends PKCS11Test {

    @Override
    public void main(Provider p) throws Exception {
        if (p.getService("SecretKeyFactory", "DES") == null) {
            System.out.println("Not supported by provider, skipping");
            return;
        }
        Random random = new Random();
        SecretKeyFactory kf;
        // DES
        kf = SecretKeyFactory.getInstance("DES", p);
        for (int i = 0; i < 10; i++ ) {
            byte[] b = new byte[8];
            random.nextBytes(b);
            SecretKeySpec spec = new SecretKeySpec(b, "DES");
            SecretKey key = kf.generateSecret(spec);
            if (DESKeySpec.isParityAdjusted(key.getEncoded(), 0) == false) {
                throw new Exception("DES key not parity adjusted");
            }
        }
        // DESede
        kf = SecretKeyFactory.getInstance("DESede", p);
        for (int i = 0; i < 10; i++ ) {
            byte[] b = new byte[24];
            random.nextBytes(b);
            SecretKeySpec spec = new SecretKeySpec(b, "DESede");
            SecretKey key = kf.generateSecret(spec);
            if (DESedeKeySpec.isParityAdjusted(key.getEncoded(), 0) == false) {
                throw new Exception("DESede key not parity adjusted");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        main(new DESParity(), args);
    }

}
