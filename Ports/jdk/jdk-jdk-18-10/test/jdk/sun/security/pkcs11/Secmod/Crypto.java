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
 * @bug 6329006
 * @summary verify that NSS no-db mode works correctly
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm Crypto
 * @run main/othervm -Djava.security.manager=allow Crypto sm policy
 */

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Signature;

public class Crypto extends SecmodTest {

    public static void main(String[] args) throws Exception {
        if (initSecmod() == false) {
            return;
        }

        String configName = BASE + SEP + "nsscrypto.cfg";
        Provider p = getSunPKCS11(configName);

        if (args.length > 1 && "sm".equals(args[0])) {
            System.setProperty("java.security.policy",
                    BASE + File.separator + args[1]);
            System.setSecurityManager(new SecurityManager());
        }

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", p);
        KeyPair kp = kpg.generateKeyPair();

        System.out.println(kp.getPublic());
        System.out.println(kp.getPrivate());

        byte[] data = generateData(2048);

        Signature sig = Signature.getInstance("SHA1withRSA", p);
        sig.initSign(kp.getPrivate());

        sig.update(data);
        byte[] s = sig.sign();
        System.out.println("signature: " + toString(s));

        sig.initVerify(kp.getPublic());
        sig.update(data);
        boolean ok = sig.verify(s);
        if (ok == false) {
            throw new Exception("Signature verification failed");
        }

        System.out.println("OK");
    }

}
