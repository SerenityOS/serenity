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
 * @bug 8021789
 * @summary jarsigner parses alias as command line option (depending on locale)
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Collator {
    public static void main(String[] args) throws Exception {

        Files.write(Path.of("collator"), List.of("12345"));
        JarUtils.createJarFile(
                Path.of("collator.jar"), Path.of("."), Path.of("collator"));

        SecurityTools.keytool("-storepass changeit -keypass changeit "
                + "-keyalg rsa -keystore collator.jks -alias debug "
                + "-dname CN=debug -genkey -validity 300")
                .shouldHaveExitValue(0);

        // use "debug" as alias name
        SecurityTools.jarsigner("-keystore collator.jks "
                + "-storepass changeit collator.jar debug")
                .shouldHaveExitValue(0);

        // use "" as alias name (although there will be a warning)
        SecurityTools.jarsigner("-keystore", "collator.jks",
                "-storepass", "changeit", "-verify", "collator.jar", "")
                .shouldHaveExitValue(0);
    }
}
