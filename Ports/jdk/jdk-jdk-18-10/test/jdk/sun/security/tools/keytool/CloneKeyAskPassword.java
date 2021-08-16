/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6178366
 * @library /test/lib
 * @summary confirm that keytool correctly finds (and clones) a private key
 *          when the user is prompted for the key's password.
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.SecurityTools;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

public class CloneKeyAskPassword {
    public static void main(String[] args) throws Exception {

        // Different storepass and keypass
        Files.copy(Path.of(
                    System.getProperty("test.src"), "CloneKeyAskPassword.jks"),
                Path.of("CloneKeyAskPassword.jks"));

        // Clone with original keypass
        SecurityTools.setResponse("test456", "");
        SecurityTools.keytool(
                "-keyclone",
                "-alias", "mykey",
                "-dest", "myclone1",
                "-keystore", "CloneKeyAskPassword.jks",
                "-storepass", "test123").shouldHaveExitValue(0);

        // Clone with new keypass
        SecurityTools.setResponse("test456", "test789", "test789");
        SecurityTools.keytool(
                "-keyclone",
                "-alias", "mykey",
                "-dest", "myclone2",
                "-keystore", "CloneKeyAskPassword.jks",
                "-storepass", "test123").shouldHaveExitValue(0);

        KeyStore ks = KeyStore.getInstance(
                new File("CloneKeyAskPassword.jks"), "test123".toCharArray());
        Asserts.assertNotNull(ks.getKey("myclone1", "test456".toCharArray()));
        Asserts.assertNotNull(ks.getKey("myclone2", "test789".toCharArray()));
    }
}
