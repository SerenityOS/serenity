/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

public class ConnectorsJarBuilder {
    public static void main(String[] args) {
        Path src = Paths.get(Utils.TEST_SRC)
                        .resolve("connectors")
                        .toAbsolutePath();
        if (Files.notExists(src)) {
            throw new Error("connectors dir [" + src + "] doesn't exist");
        }
        Path classes = Paths.get("connectors","classes")
                            .toAbsolutePath();
        try {
            Files.createDirectories(classes);
        } catch (IOException e) {
            throw new Error("can't create dir " + classes, e);
        }
        compile(src, classes);
        Path target = Paths.get("jars", "connectors.jar")
                           .toAbsolutePath();
        buildJar(classes, target);
        addMetaInf(src, target);
    }

    private static void compile(Path src, Path classes) {
        JDKToolLauncher javac = JDKToolLauncher.create("javac")
                                               .addToolArg("-d")
                                               .addToolArg(classes.toString())
                                               .addToolArg("-cp")
                                               .addToolArg(Utils.TEST_CLASS_PATH);
        try (Stream<Path> stream = Files.walk(src)) {
            stream.map(Path::toAbsolutePath)
                  .map(Path::toString)
                  .filter(s -> s.endsWith(".java"))
                  .forEach(javac::addToolArg);
        } catch (IOException e) {
            throw new Error("traverse dir " + src, e);
        }

        executeTool(javac);
    }

    private static void buildJar(Path classes, Path target) {
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            throw new Error("can't create dir " + target.getParent(), e);
        }
        JDKToolLauncher jar = JDKToolLauncher.create("jar")
                                             .addToolArg("cf")
                                             .addToolArg(target.toString())
                                             .addToolArg("-C")
                                             .addToolArg(classes.toString())
                                             .addToolArg(".");
        executeTool(jar);
    }

    private static void addMetaInf(Path src, Path jarFile) {
        Path metaInf = src.resolve("META-INF");
        if (Files.exists(metaInf)) {
            JDKToolLauncher jar = JDKToolLauncher.create("jar")
                                             .addToolArg("uf")
                                             .addToolArg(jarFile.toString())
                                             .addToolArg("-C")
                                             .addToolArg(src.toString())
                                             .addToolArg("META-INF");
            executeTool(jar);
        }
    }

    private static void executeTool(JDKToolLauncher tool) {
        String[] command = tool.getCommand();
        try {
            ProcessTools.executeCommand(command)
                        .shouldHaveExitValue(0);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new Error("execution of " + Arrays.toString(command) + " failed", e);
        }
    }
}
