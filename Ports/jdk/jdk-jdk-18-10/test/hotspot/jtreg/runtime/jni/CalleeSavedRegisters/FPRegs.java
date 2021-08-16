/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @bug 8067744
 * @comment Test uses custom launcher that starts VM in primordial thread. This is
 *          not possible on aix.
 * @requires os.family != "aix"
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run main/native FPRegs
 */

import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class FPRegs {
    public static void main(String[] args) throws IOException {
        Path launcher = Paths.get(Utils.TEST_NATIVE_PATH, "FPRegs" + (Platform.isWindows() ? ".exe" : ""));
        System.out.println("Launcher = " + launcher + (Files.exists(launcher) ? " (exists)" : " (not exists)"));
        Path jvmLib = findJVM();
        ProcessBuilder pb = new ProcessBuilder(launcher.toString(), jvmLib.toString());
        // bin as working directory to let Windows load dll
        pb.directory(jvmLib.getParent().getParent().toFile());
        OutputAnalyzer oa = new OutputAnalyzer(pb.start());
        oa.shouldHaveExitValue(0);
    }

    static Path findJVM() throws IOException {
        String root = Utils.TEST_JDK;
        String lib = System.mapLibraryName("jvm");
        System.out.println("Root = " + root);
        System.out.println("Library = " + lib);

        Optional<Path> jvmLib = Files.find(new File(root).toPath(), 4, (p, attr) -> p.toFile().getName().equals(lib)).findFirst();
        Path p = null;
        if (jvmLib.isPresent()) {
            p = jvmLib.get().toRealPath();
            System.out.println("JVM = " + p);
        } else {
            System.out.println("TESTBUG: JVM not found in ");
            Files.walk(new File(root).toPath(), 4).map(Path::toString).forEach(System.out::println);
        }
        return p;
    }
}

