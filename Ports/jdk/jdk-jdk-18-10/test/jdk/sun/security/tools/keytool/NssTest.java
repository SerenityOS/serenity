/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/*
 * @test
 * @summary It tests (almost) all keytool behaviors with NSS.
 * @library /test/lib /test/jdk/sun/security/pkcs11
 * @modules java.base/sun.security.tools.keytool
 *          java.base/sun.security.util
 *          java.base/sun.security.x509
 * @run main/othervm/timeout=600 NssTest
 */
public class NssTest {

    public static void main(String[] args) throws Exception {
        Path libPath = PKCS11Test.getNSSLibPath("softokn3");
        if (libPath == null) {
            return;
        }
        System.out.println("Using NSS lib at " + libPath);

        copyFiles();
        System.setProperty("nss", "");
        System.setProperty("nss.lib", String.valueOf(libPath));

        PKCS11Test.loadNSPR(libPath.getParent().toString());
        KeyToolTest.main(args);
    }

    private static void copyFiles() throws IOException {
        Path srcPath = Paths.get(System.getProperty("test.src"));
        Files.copy(srcPath.resolve("p11-nss.txt"), Paths.get("p11-nss.txt"));

        Path dbPath = srcPath.getParent().getParent()
                .resolve("pkcs11").resolve("nss").resolve("db");
        Files.copy(dbPath.resolve("cert8.db"), Paths.get("cert8.db"));
        Files.copy(dbPath.resolve("key3.db"), Paths.get("key3.db"));
        Files.copy(dbPath.resolve("secmod.db"), Paths.get("secmod.db"));
    }
}
