/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.unloading.bytecode;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Uses {@link gc.g1.unloading.bytecode.HumongousTemplateClassGen} to generate
 * source code of HumongousTemplateClass and compiles it to {@code test.classes}.
 */
public class GenClassesBuilder {
    public static void main(String[] args) {
        Path genSrc = Paths.get("genSrc").toAbsolutePath();
        Path classesDir = Paths.get(Utils.TEST_CLASSES).toAbsolutePath();
        generateSource(genSrc);
        compileSource(genSrc, classesDir);
    }

    private static void compileSource(Path srcDst, Path classesDir) {
        JDKToolLauncher javac = JDKToolLauncher.create("javac")
                                               .addToolArg("-d")
                                               .addToolArg(classesDir.toString())
                                               .addToolArg("-cp")
                                               .addToolArg(Utils.TEST_CLASS_PATH);
        try (Stream<Path> stream = Files.walk(srcDst)) {
            stream.map(Path::toAbsolutePath)
                  .map(Path::toString)
                  .filter(s -> s.endsWith(".java"))
                  .forEach(javac::addToolArg);
        } catch (IOException e) {
            throw new Error("traverse source dir " + srcDst, e);
        }
        String[] command = javac.getCommand();
        try {
            ProcessTools.executeCommand(command)
                        .shouldHaveExitValue(0);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new Error("execution of javac(" + Arrays.toString(command) + ") failed", e);
        }
    }

    private static void generateSource(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new Error("can't create dirs for" + dir, e);
        }

        try {
            HumongousTemplateClassGen.main(new String[]{dir.toString()});
        } catch (Exception e) {
            throw new Error("can't generate classes", e);
        }
    }
}

