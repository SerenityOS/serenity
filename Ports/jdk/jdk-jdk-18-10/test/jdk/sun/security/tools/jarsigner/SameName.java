/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6866479
 * @summary libzip.so caused JVM to crash when running jarsigner
 * @library /test/lib
 */

import jdk.test.lib.Platform;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SameName {
    public static void main(String[] args) throws Exception {

        String signedJar = Platform.isWindows() ? "EM.jar" : "em.jar";

        Files.write(Path.of("A"), List.of("A"));
        JarUtils.createJarFile(Path.of("em.jar"), Path.of("."), Path.of("A"));

        SecurityTools.keytool("-storepass changeit -keypass changeit "
                + "-keystore ks -keyalg rsa  -alias a -dname CN=a "
                + "-keyalg rsa -genkey -validity 300")
                .shouldHaveExitValue(0);

        SecurityTools.jarsigner("-keystore ks -storepass changeit "
                + "-signedjar " + signedJar + " em.jar a")
                .shouldHaveExitValue(0);
    }
}
