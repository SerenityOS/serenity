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
 * @bug 4856966
 * @summary
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm ReinitCipher
 * @run main/othervm -Djava.security.manager=allow ReinitCipher sm
 */

import java.security.Provider;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class ReinitCipher extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new ReinitCipher(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        if (p.getService("Cipher", "ARCFOUR") == null) {
            System.out.println("Not supported by provider, skipping");
            return;
        }
        Random random = new Random();
        byte[] data1 = new byte[10 * 1024];
        random.nextBytes(data1);
        byte[] keyData = new byte[16];
        random.nextBytes(keyData);
        SecretKeySpec key = new SecretKeySpec(keyData, "ARCFOUR");

        Cipher cipher = Cipher.getInstance("ARCFOUR", p);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipher.update(data1);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipher.update(data1);
        cipher.doFinal();
        cipher.doFinal();
        cipher.update(data1);
        cipher.doFinal();
        cipher.init(Cipher.ENCRYPT_MODE, key);
        cipher.doFinal();

        System.out.println("All tests passed");
    }
}
