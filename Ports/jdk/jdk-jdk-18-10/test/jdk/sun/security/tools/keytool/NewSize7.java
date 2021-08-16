/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6561126
 * @summary keytool should use larger default keysize for keypairs
 * @modules java.base/sun.security.tools.keytool
 * @compile -XDignore.symbol.file NewSize7.java
 * @run main NewSize7
 */

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

public class NewSize7 {
    public static void main(String[] args) throws Exception {
        String FILE = "newsize7-ks";
        new File(FILE).delete();
        sun.security.tools.keytool.Main.main(("-debug -genkeypair -keystore " +
                FILE +
                " -alias a -dname cn=c -storepass changeit" +
                " -keypass changeit -keyalg rsa").split(" "));
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fin = new FileInputStream(FILE)) {
            ks.load(fin, "changeit".toCharArray());
        }
        Files.delete(Paths.get(FILE));
        RSAPublicKey r = (RSAPublicKey)ks.getCertificate("a").getPublicKey();
        if (r.getModulus().bitLength() != 2048) {
            throw new Exception("Bad keysize");
        }
        X509Certificate x = (X509Certificate)ks.getCertificate("a");
        if (!x.getSigAlgName().equals("SHA256withRSA")) {
            throw new Exception("Bad sigalg");
        }
    }
}
