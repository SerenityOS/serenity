/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4361044 4388202 4418643 4523159 4730642
 * @library /test/lib
 * @modules jdk.compiler
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        jdk.test.lib.util.JarUtils
 *        src.test.src.TestDriver
 * @summary various resource and classloading bugs related to jar files
 * @run main/othervm TestDriver
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

public class TestDriver {
    public static void main(String[] args) throws Throwable {
        Path srcDir = Paths.get(System.getProperty("test.src"));
        Path targetDir = Paths.get(System.getProperty("user.dir"));
        Path jar1SrcDir = srcDir.resolve("src").resolve("jar1");
        Path jar1TargetDir =  targetDir.resolve("jar1");
        Path ectJar1Dir = srcDir.resolve("etc").resolve("jar1");
        Path jarFile = targetDir.resolve("jar1.jar");
        Path[]  files= new Path[] {
                Paths.get("res1.txt"), Paths.get("jar1", "bundle.properties")
        };

        // Copy files to target directory and change permission
        for (Path file : files) {
            Path dest = jar1TargetDir.resolve(file);
            Files.createDirectories(dest.getParent());
            Files.copy(ectJar1Dir.resolve(file), dest, REPLACE_EXISTING);
        }

        // Compile and build jar1.jar
        ProcessTools.executeCommand("chmod", "-R", "u+w", "./jar1")
                    .outputTo(System.out)
                    .errorTo(System.out)
                    .shouldHaveExitValue(0);
        CompilerUtils.compile(jar1SrcDir, jar1TargetDir);
        JarUtils.createJarFile(jarFile, jar1TargetDir);

        // Compile test files
        CompilerUtils.compile(srcDir.resolve("src").resolve("test"), targetDir);

        // Run tests
        String java = JDKToolFinder.getTestJDKTool("java");
        String cp = targetDir.toString() + File.pathSeparator + jarFile;
        String[] tests = new String[]{"TestBug4361044", "TestBug4523159"};
        for (String test : tests) {
            ProcessTools.executeCommand(java, "-cp", cp, test)
                        .outputTo(System.out)
                        .errorTo(System.out)
                        .shouldHaveExitValue(0);
        }
    }
}
