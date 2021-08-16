/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4955844
 * @summary ensure that the NONEwithRSA adapter works correctly
 * @author Andreas Sterbenz
 * @key randomness
 */

import java.util.*;

import java.security.*;

import javax.crypto.*;

public class NONEwithRSA {

    public static void main(String[] args) throws Exception {
//      showProvider(Security.getProvider("SUN"));
        Random random = new Random();
        byte[] b = new byte[16];
        random.nextBytes(b);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();

        Signature sig = Signature.getInstance("NONEwithRSA");
        sig.initSign(kp.getPrivate());
        System.out.println("Provider: " + sig.getProvider());
        sig.update(b);
        byte[] sb = sig.sign();

        sig.initVerify(kp.getPublic());
        sig.update(b);
        if (sig.verify(sb) == false) {
            throw new Exception("verification failed");
        }

        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.DECRYPT_MODE, kp.getPublic());
        byte[] dec = c.doFinal(sb);
        if (Arrays.equals(dec, b) == false) {
            throw new Exception("decryption failed");
        }

        sig = Signature.getInstance("NONEwithRSA", "SunJCE");
        sig.initSign(kp.getPrivate());
        sig = Signature.getInstance("NONEwithRSA", Security.getProvider("SunJCE"));
        sig.initSign(kp.getPrivate());

        try {
            Signature.getInstance("NONEwithRSA", "SUN");
            throw new Exception("call succeeded");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        System.out.println("OK");
    }

    private static void showProvider(Provider p) {
        System.out.println(p);
        for (Iterator t = p.getServices().iterator(); t.hasNext(); ) {
            System.out.println(t.next());
        }
    }

}
