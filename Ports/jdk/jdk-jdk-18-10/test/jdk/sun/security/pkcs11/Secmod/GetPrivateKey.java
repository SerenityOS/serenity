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
 * @bug 6273877 6322208 6275523
 * @summary make sure we can access the NSS softtoken KeyStore
 *          and use a private key
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm GetPrivateKey
 * @run main/othervm -Djava.security.manager=allow GetPrivateKey sm policy
 */

import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;

public class GetPrivateKey extends SecmodTest {

    public static void main(String[] args) throws Exception {
        if (args.length > 1 && "sm".equals(args[0])) {
            System.setProperty("java.security.policy",
                    BASE + File.separator + args[1]);
        }

        if (initSecmod() == false) {
            return;
        }

        String configName = BASE + SEP + "nss.cfg";
        Provider p = getSunPKCS11(configName);

        System.out.println(p);
        Security.addProvider(p);

        if (args.length > 1 && "sm".equals(args[0])) {
            System.setSecurityManager(new SecurityManager());
        }

        KeyStore ks = KeyStore.getInstance(PKCS11, p);
        ks.load(null, password);
        Collection<String> aliases = new TreeSet<>(
                Collections.list(ks.aliases()));
        System.out.println("entries: " + aliases.size());
        System.out.println(aliases);

        PrivateKey privateKey = (PrivateKey)ks.getKey(keyAlias, password);
        System.out.println(privateKey);

        byte[] data = generateData(1024);

        System.out.println("Signing...");
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initSign(privateKey);
        signature.update(data);
        byte[] sig = signature.sign();

        X509Certificate[] chain =
                (X509Certificate[]) ks.getCertificateChain(keyAlias);
        signature.initVerify(chain[0].getPublicKey());
        signature.update(data);
        boolean ok = signature.verify(sig);
        if (ok == false) {
            throw new Exception("Signature verification error");
        }

        System.out.println("OK");

    }

}
