/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8022761
 * @summary regression: SecurityException is NOT thrown while trying
 *          to pack a wrongly signed Indexed Jar file
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JvIndex {
    public static void main(String[] args) throws Exception {

        Files.write(Path.of("abcde"), List.of("12345"));
        JarUtils.createJarFile(Path.of("jvindex.jar"), Path.of("."),
                Path.of("abcde"));
        SecurityTools.keytool("-storepass changeit -keypass changeit "
                + "-keystore ks -keyalg rsa -alias a -dname CN=a "
                + "-genkey -validity 300")
                .shouldHaveExitValue(0);
        SecurityTools.jarsigner("-keystore ks -storepass changeit jvindex.jar a")
                .shouldHaveExitValue(0);

        SecurityTools.jar("i jvindex.jar");

        // Make sure the $F line has "sm" (signed and in manifest)
        SecurityTools.jarsigner("-keystore ks -storepass changeit -verify "
                + "-verbose jvindex.jar")
                .shouldHaveExitValue(0)
                .shouldMatch("sm.*abcde");
    }
}
