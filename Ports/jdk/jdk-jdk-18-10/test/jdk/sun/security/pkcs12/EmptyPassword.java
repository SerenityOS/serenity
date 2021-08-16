/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202299
 * @modules java.base/sun.security.tools.keytool
 *          java.base/sun.security.x509
 * @library /test/lib
 * @summary Java Keystore fails to load PKCS12/PFX certificates created in WindowsServer2016
 */

import jdk.test.lib.Asserts;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;

public class EmptyPassword {

    public static void main(String[] args) throws Exception {

        // KeyStore is protected with password "\0".
        CertAndKeyGen gen = new CertAndKeyGen("RSA", "SHA256withRSA");
        gen.generate(2048);
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("a", gen.getPrivateKey(), new char[1],
                new Certificate[] {
                        gen.getSelfCertificate(new X500Name("CN=Me"), 100)
                });
        try (FileOutputStream fos = new FileOutputStream("p12")) {
            ks.store(fos, new char[1]);
        }

        // It can be loaded with password "".
        ks = KeyStore.getInstance(new File("p12"), new char[0]);
        Asserts.assertTrue(ks.getKey("a", new char[0]) != null);
        Asserts.assertTrue(ks.getCertificate("a") != null);
    }
}
