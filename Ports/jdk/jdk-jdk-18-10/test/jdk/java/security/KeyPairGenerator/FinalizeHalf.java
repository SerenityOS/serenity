/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8163896 8223003
 * @summary Finalizing one key of a KeyPair invalidates the other key
 */

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.Security;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.List;

public class FinalizeHalf {

    static int failures = 0;

    public static void main(String[] args) throws Throwable {
        List<Consumer<Key>> methods = new ArrayList<>();
        methods.add((Key k) -> k.getEncoded());
        methods.add((Key k) -> k.toString());

        for (String algo : new String[] {"DiffieHellman", "DSA", "RSA"}) {
            for (Provider provider : Security.getProviders()) {
                for (boolean priv : new boolean[] {true, false}) {
                    for (Consumer<Key> method : methods) {
                        test(algo, provider, priv, method);
                    }
                }
            }
        }

        if (failures > 0) {
            throw new RuntimeException(failures + " test(s) failed.");
        }
    }

    static void test(String algo, Provider provider, boolean priv,
            Consumer<Key> method) throws Exception {
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance(algo, provider);
        } catch (NoSuchAlgorithmException nsae) {
            return;
        }

        System.out.println("Checking " + provider.getName() + ", " + algo);

        KeyPair pair = generator.generateKeyPair();
        Key key = priv ? pair.getPrivate() : pair.getPublic();

        pair = null;
        for (int i = 0; i < 32; ++i) {
            System.gc();
        }

        try {
            method.accept(key);
        } catch (ProviderException pe) {
            failures++;
        }
    }
}
