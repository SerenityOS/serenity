/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6876328
 * @summary different names for the same digest algorithms breaks jarsigner
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NameClash {
    public static void main(String[] args) throws Exception {
        String common = "-storepass changeit -keypass changeit -keystore ks ";

        SecurityTools.keytool(common + "-alias a -dname CN=a -keyalg rsa "
                + "-genkey -validity 300");
        SecurityTools.keytool(common + "-alias b -dname CN=b -keyalg rsa "
                + "-genkey -validity 300");

        Files.write(Path.of("A"), List.of("A"));
        JarUtils.createJarFile(Path.of("a.jar"), Path.of("."), Path.of("A"));

        SecurityTools.jarsigner(common + "a.jar a -digestalg SHA-256")
                .shouldHaveExitValue(0);
        SecurityTools.jarsigner(common + "a.jar b -digestalg SHA-256")
                .shouldHaveExitValue(0);

        SecurityTools.jarsigner(common + "-verify -debug -strict a.jar")
                .shouldHaveExitValue(0);
    }
}
