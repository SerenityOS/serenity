/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary JVM should be able to handle full path (directory path plus
 *          class name) or directory path longer than MAX_PATH specified
 *          in -Xbootclasspath/a on windows.
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          jdk.jartool/sun.tools.jar
 * @run driver LongBCP
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.spi.ToolProvider;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class LongBCP {

    private static final int MAX_PATH = 260;

    private static final ToolProvider JAR = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new RuntimeException("ToolProvider for jar not found"));

    public static void main(String args[]) throws Exception {
        Path sourceDir = Paths.get(System.getProperty("test.src"), "test-classes");
        Path classDir = Paths.get(System.getProperty("test.classes"));
        Path destDir = classDir;

        // create a sub-path so that the destDir length is almost MAX_PATH
        // so that the full path (with the class name) will exceed MAX_PATH
        int subDirLen = MAX_PATH - classDir.toString().length() - 2;
        if (subDirLen > 0) {
            char[] chars = new char[subDirLen];
            Arrays.fill(chars, 'x');
            String subPath = new String(chars);
            destDir = Paths.get(System.getProperty("test.classes"), subPath);
        }

        CompilerUtils.compile(sourceDir, destDir);

        String bootCP = "-Xbootclasspath/a:" + destDir.toString();
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            bootCP, "Hello");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);

        // increase the length of destDir to slightly over MAX_PATH
        destDir = Paths.get(destDir.toString(), "xxxxx");
        CompilerUtils.compile(sourceDir, destDir);

        bootCP = "-Xbootclasspath/a:" + destDir.toString();
        pb = ProcessTools.createJavaProcessBuilder(
            bootCP, "Hello");

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);

        // create a hello.jar
        String helloJar = destDir.toString() + File.separator + "hello.jar";
        if (JAR.run(System.out, System.err, "-cf", helloJar, "-C", destDir.toString(), "Hello.class") != 0) {
            throw new RuntimeException("jar operation for hello.jar failed");
        }

        // run with long bootclasspath to hello.jar
        bootCP = "-Xbootclasspath/a:" + helloJar;
        pb = ProcessTools.createJavaProcessBuilder(
            bootCP, "Hello");

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);

        // relative path tests
        //
        // relative path length within the file system limit
        int fn_max_length = 255;
        // In AUFS file system, the maximal file name length is 242
        FileStore store = Files.getFileStore(new File(".").toPath());
        String fs_type = store.type();
        if ("aufs".equals(fs_type)) {
            fn_max_length = 242;
        }
        char[] chars = new char[fn_max_length];
        Arrays.fill(chars, 'y');
        String subPath = new String(chars);
        destDir = Paths.get(".", subPath);

        CompilerUtils.compile(sourceDir, destDir);

        bootCP = "-Xbootclasspath/a:" + destDir.toString();
        pb = ProcessTools.createJavaProcessBuilder(
            bootCP, "Hello");

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);

        // Test a relative path for a jar file < MAX_PATH, but where the
        // absolute path is > MAX_PATH.
        Path jarDir = Paths.get(".");
        for (int i = 0; i < 21; ++i) {
            jarDir = jarDir.resolve("0123456789");
        }
        Files.createDirectories(jarDir);
        Path jarPath = jarDir.resolve("hello.jar");
        Files.copy(Paths.get(helloJar), jarPath);
        bootCP = "-Xbootclasspath/a:" + jarPath.toString();
        pb = ProcessTools.createJavaProcessBuilder(bootCP, "Hello");

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);

        // total relative path length exceeds MAX_PATH
        destDir = Paths.get(destDir.toString(), "yyyyyyyy");

        CompilerUtils.compile(sourceDir, destDir);

        bootCP = "-Xbootclasspath/a:" + destDir.toString();
        pb = ProcessTools.createJavaProcessBuilder(
            bootCP, "Hello");

        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Hello World")
              .shouldHaveExitValue(0);
    }
}
