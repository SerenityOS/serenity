/*
 * Copyright (c) 2019, Google Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @summary Test with symbolic linked lib/modules
 * @bug 8220095
 * @requires os.family == "linux" | os.family == "mac"
 * @library /test/lib
 * @modules java.management
 *          jdk.jlink
 * @run driver ModulesSymLink
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class ModulesSymLink {
    static String java_home;
    static String test_jdk;

    public static void main(String[] args) throws Throwable {
        java_home = System.getProperty("java.home");
        test_jdk = System.getProperty("user.dir") + File.separator +
                   "modulessymlink_jdk" + Long.toString(System.currentTimeMillis());

        constructTestJDK();

        ProcessBuilder pb = new ProcessBuilder(
            test_jdk + File.separator + "bin" + File.separator + "java",
            "-version");
        OutputAnalyzer out = new OutputAnalyzer(pb.start());
        out.shouldHaveExitValue(0);
    }

    // 1) Create a test JDK binary (jlink is used to help simplify the process,
    //    alternatively a test JDK could be copied from JAVA_HOME.)
    // 2) Rename the test JDK's lib/modules to lib/0.
    // 3) Then create a link to lib/0 as lib/modules.
    static void constructTestJDK() throws Throwable {
        Path jlink = Paths.get(java_home, "bin", "jlink");
        System.out.println("Jlink = " + jlink);
        OutputAnalyzer out = ProcessTools.executeProcess(jlink.toString(),
                  "--output", test_jdk,
                  "--add-modules", "java.base");
        out.shouldHaveExitValue(0);

        Path modules = Paths.get(test_jdk, "lib", "modules");
        Path renamed_modules = Paths.get(test_jdk, "lib", "0");
        Files.move(modules, renamed_modules);
        Files.createSymbolicLink(modules, renamed_modules);
    }
}
