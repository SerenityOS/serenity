/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6543940 6868865 8217375
 * @summary Exception thrown when signing a jarfile in java 1.5
 * @library /test/lib
 */
/*
 * See also PreserveRawManifestEntryAndDigest.java for tests with arbitrarily
 * formatted individual sections in addition the the main attributes tested
 * here.
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Files;
import java.nio.file.Path;

public class OldSig {
    public static void main(String[] args) throws Exception {
        Path src = Path.of(System.getProperty("test.src"));
        // copy jar file into writeable location
        Files.copy(src.resolve("oldsig/A.jar"), Path.of("B.jar"));
        Files.copy(src.resolve("oldsig/A.class"), Path.of("B.class"));

        JarUtils.updateJarFile(Path.of("B.jar"), Path.of("."),
                Path.of("B.class"));

        String ksArgs = "-keystore " + src.resolve("JarSigning.keystore")
                + " -storepass bbbbbb";
        SecurityTools.jarsigner(ksArgs + " -digestalg SHA1 B.jar c");
        SecurityTools.jarsigner("-verify B.jar").shouldHaveExitValue(0);
        SecurityTools.jarsigner("-verify " + ksArgs + " -verbose B.jar c")
                .stdoutShouldMatch("^smk .* B[.]class$").shouldHaveExitValue(0);
    }
}
