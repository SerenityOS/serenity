/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8245679
 * @summary KeyStore cannot probe PKCS12 keystore if BouncyCastle is the top security provider
 * @run main/othervm OneProbeOneNot
 */

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Enumeration;

public class OneProbeOneNot {
    public static final void main(String[] args) throws Exception {
        Files.write(Path.of("ks"), "".getBytes());
        // 1st provider do not support probe
        Security.insertProviderAt(new P1(), 1);
        // 2nd provider support probe
        Security.insertProviderAt(new P2(), 2);
        KeyStore ks = KeyStore.getInstance(new File("ks"), (char[])null);
        System.out.println(ks.getProvider().getName());
        System.out.println(ks.getType());
    }

    public static class P1 extends Provider {
        public P1() {
            super("P1", "P1", "P1");
            putService(new Service(this, "KeyStore", "Oops",
                    K1.class.getName(), null, null));
        }
    }
    public static class P2 extends Provider {
        public P2() {
            super("P2", "P2", "P2");
            putService(new Service(this, "KeyStore", "Oops",
                    K2.class.getName(), null, null));
        }
    }

    public static class K1 extends KeyStoreSpi {
        public Key engineGetKey(String a, char[] p) { return null; }
        public Certificate[] engineGetCertificateChain(String a) { return null; }
        public Certificate engineGetCertificate(String a) { return null; }
        public Date engineGetCreationDate(String a) { return null; }
        public void engineSetKeyEntry(String a, Key k, char[] p, Certificate[] c) { }
        public void engineSetKeyEntry(String a, byte[] k, Certificate[] c) { }
        public void engineSetCertificateEntry(String a, Certificate c) { }
        public void engineDeleteEntry(String a) { }
        public Enumeration<String> engineAliases() { return null; }
        public boolean engineContainsAlias(String a) { return false; }
        public int engineSize() { return 0; }
        public boolean engineIsKeyEntry(String a) { return false; }
        public boolean engineIsCertificateEntry(String a) { return false; }
        public String engineGetCertificateAlias(Certificate c) { return null; }
        public void engineStore(OutputStream stream, char[] password) { }
        public void engineLoad(InputStream stream, char[] password) { }
    }

    public static class K2 extends K1 {
        public boolean engineProbe(InputStream s) {
            return true;
        }
    }
}
