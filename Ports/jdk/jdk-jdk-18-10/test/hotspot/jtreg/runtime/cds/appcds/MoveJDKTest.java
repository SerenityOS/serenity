/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that CDS still works when the JDK is moved to a new directory
 * @requires vm.cds
 * @requires os.family == "linux"
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @run driver MoveJDKTest
 */

// This test works only on Linux because it depends on symlinks and the name of the hotspot DLL (libjvm.so).
// It probably doesn't work on Windows.
// TODO: change libjvm.so to libjvm.dylib on MacOS, before adding "@requires os.family == mac"

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class MoveJDKTest {
    public static void main(String[] args) throws Exception {
        String java_home_src = System.getProperty("java.home");
        String java_home_dst = CDSTestUtils.getOutputDir() + File.separator + "moved_jdk";

        TestCommon.startNewArchiveName();
        String jsaFile = TestCommon.getCurrentArchiveName();
        String jsaOpt = "-XX:SharedArchiveFile=" + jsaFile;
        {
            ProcessBuilder pb = makeBuilder(java_home_src + "/bin/java", "-Xshare:dump", jsaOpt);
            TestCommon.executeAndLog(pb, "dump");
        }
        {
            ProcessBuilder pb = makeBuilder(java_home_src + "/bin/java",
                                            "-Xshare:auto",
                                            jsaOpt,
                                            "-Xlog:class+path=info",
                                            "-version");
            OutputAnalyzer out = TestCommon.executeAndLog(pb, "exec-src");
            out.shouldNotContain("shared class paths mismatch");
            out.shouldNotContain("BOOT classpath mismatch");
        }

        clone(new File(java_home_src), new File(java_home_dst));
        System.out.println("============== Cloned JDK at " + java_home_dst);

        // Test runtime with cloned JDK
        {
            ProcessBuilder pb = makeBuilder(java_home_dst + "/bin/java",
                                            "-Xshare:auto",
                                            jsaOpt,
                                            "-Xlog:class+path=info",
                                            "-version");
            OutputAnalyzer out = TestCommon.executeAndLog(pb, "exec-dst");
            out.shouldNotContain("shared class paths mismatch");
            out.shouldNotContain("BOOT classpath mismatch");
        }

        // Test with bad JAR file name, hello.modules
        String helloJar = JarBuilder.getOrCreateHelloJar();
        String fake_modules = copyFakeModulesFromHelloJar();
        String dumptimeBootAppendOpt = "-Xbootclasspath/a:" + fake_modules;
        {
            ProcessBuilder pb = makeBuilder(java_home_src + "/bin/java",
                                            "-Xshare:dump",
                                            dumptimeBootAppendOpt,
                                            jsaOpt);
            TestCommon.executeAndLog(pb, "dump");
        }
        {
            String runtimeBootAppendOpt = dumptimeBootAppendOpt + System.getProperty("path.separator") + helloJar;
            ProcessBuilder pb = makeBuilder(java_home_dst + "/bin/java",
                                            "-Xshare:auto",
                                            runtimeBootAppendOpt,
                                            jsaOpt,
                                            "-Xlog:class+path=info",
                                            "-version");
            OutputAnalyzer out = TestCommon.executeAndLog(pb, "exec-dst");
            out.shouldNotContain("shared class paths mismatch");
            out.shouldNotContain("BOOT classpath mismatch");
        }

        // Test with no modules image in the <java home>/lib directory
        renameModulesFile(java_home_dst);
        {
            ProcessBuilder pb = makeBuilder(java_home_dst + "/bin/java",
                                            "-version");
            OutputAnalyzer out = TestCommon.executeAndLog(pb, "exec-missing-modules");
            out.shouldHaveExitValue(1);
            out.shouldContain("Failed setting boot class path.");
        }
    }

    // Do a cheap clone of the JDK. Most files can be sym-linked. However, $JAVA_HOME/bin/java and $JAVA_HOME/lib/.../libjvm.so"
    // must be copied, because the java.home property is derived from the canonicalized paths of these 2 files.
    static void clone(File src, File dst) throws Exception {
        if (dst.exists()) {
            if (!dst.isDirectory()) {
                throw new RuntimeException("Not a directory :" + dst);
            }
        } else {
            if (!dst.mkdir()) {
                throw new RuntimeException("Cannot create directory: " + dst);
            }
        }
        for (String child : src.list()) {
            if (child.equals(".") || child.equals("..")) {
                continue;
            }

            File child_src = new File(src, child);
            File child_dst = new File(dst, child);
            if (child_dst.exists()) {
                throw new RuntimeException("Already exists: " + child_dst);
            }
            if (child_src.isFile()) {
                if (child.equals("libjvm.so") || child.equals("java")) {
                    Files.copy(child_src.toPath(), /* copy data to -> */ child_dst.toPath());
                } else {
                    Files.createSymbolicLink(child_dst.toPath(),  /* link to -> */ child_src.toPath());
                }
            } else {
                clone(child_src, child_dst);
            }
        }
    }

    static void renameModulesFile(String javaHome) throws Exception {
        String modulesDir = javaHome + File.separator + "lib";
        File origModules = new File(modulesDir, "modules");
        if (!origModules.exists()) {
            throw new RuntimeException("modules file not found");
        }

        File renamedModules = new File(modulesDir, "orig_modules");
        if (renamedModules.exists()) {
            throw new RuntimeException("found orig_modules unexpectedly");
        }

        boolean success = origModules.renameTo(renamedModules);
        if (!success) {
            throw new RuntimeException("rename modules file failed");
        }
    }

    static ProcessBuilder makeBuilder(String... args) throws Exception {
        System.out.print("[");
        for (String s : args) {
            System.out.print(" " + s);
        }
        System.out.println(" ]");
        return new ProcessBuilder(args);
    }

    private static String copyFakeModulesFromHelloJar() throws Exception {
        String outDir = CDSTestUtils.getOutputDir();
        String newFile = "hello.modules";
        String path = outDir + File.separator + newFile;

        Files.copy(Paths.get(outDir, "hello.jar"),
            Paths.get(outDir, newFile),
            StandardCopyOption.REPLACE_EXISTING);
        return path;
    }
}
