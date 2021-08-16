/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048622 8134232
 * @summary Checks that PKCS#11 keystore can't be loaded with wrong password
 * @library /test/lib ../
 * @modules jdk.crypto.cryptoki
 * @run main/othervm LoadKeystore
 * @run main/othervm -Djava.security.manager=allow LoadKeystore sm policy
 */

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.util.Collections;

public class LoadKeystore extends SecmodTest {

    public static void main(String[] args) throws Exception {
        if (args.length > 1 && "sm".equals(args[0])) {
            System.setProperty("java.security.policy",
                    BASE + File.separator + args[1]);
        }

        if (!initSecmod()) {
            return;
        }

        String configName = BASE + SEP + "nss.cfg";
        Provider p = getSunPKCS11(configName);

        System.out.println("Add provider " + p);
        System.out.println();
        Security.addProvider(p);

        if (args.length > 1 && "sm".equals(args[0])) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            System.out.println("Load keystore with wrong type");
            KeyStore.getInstance("unknown", p);
            throw new RuntimeException("Expected exception not thrown");
        } catch(KeyStoreException e) {
            System.out.println("Expected exception: " + e);
        }

        KeyStore ks = KeyStore.getInstance("PKCS11", p);
        if (!"PKCS11".equals(ks.getType())) {
            throw new RuntimeException("Unexpected keystore type: "
                    + ks.getType());
        }
        if (!p.equals(ks.getProvider())) {
            throw new RuntimeException("Unexpected keystore provider: "
                    + ks.getProvider());
        }

        try {
            System.out.println("Load keystore with wrong password");
            ks.load(null, "wrong".toCharArray());
            throw new RuntimeException("Expected exception not thrown");
        } catch(IOException e) {
            System.out.println("Expected exception: " + e);
            Throwable cause = e.getCause();
            if (!(cause instanceof UnrecoverableKeyException)) {
                e.printStackTrace(System.out);
                throw new RuntimeException("Unexpected cause: " + cause);
            }
            System.out.println("Expected cause: " + cause);
        }

        System.out.println("Load keystore with correct password");
        ks.load(null, password);
        for (String alias : Collections.list(ks.aliases())) {
            System.out.println("Alias: " + alias);
        }

        System.out.println("Test passed");
    }

}
