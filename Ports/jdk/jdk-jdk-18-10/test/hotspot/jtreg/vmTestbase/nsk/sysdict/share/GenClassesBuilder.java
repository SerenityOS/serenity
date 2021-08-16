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

package nsk.sysdict.share;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

public class GenClassesBuilder {
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            throw new Error("args can't be empty");
        }
        for (String arg : args) {
            switch (arg) {
                case "btree":
                    build("btree", "BTree",
                            () -> BTreeGen.main(new String[] {"default"}));
                    break;
                case "leans":
                    build("leans", "Leans",
                            () -> ChainGen.main(new String[] {"leans"}));
                    break;
                case "fats":
                    build("fats", "Fats",
                            () -> ChainGen.main(new String[] {"fats"}));
                    break;
                default:
                    throw new Error("unkown target " + arg);
            }
        }
    }

    private static void build(String name, String prefix, Runnable generator) {
        generator.run();
        Path genSrcDir = Paths.get(name, "genSrc", "nsk", "sysdict", "share");
        Path classesDir = Paths.get(name,"classes").toAbsolutePath();
        try {
            Files.createDirectories(genSrcDir);
        } catch (IOException e) {
            throw new Error("can't create dir " + genSrcDir, e);
        }
        moveJavaFiles(genSrcDir, prefix);

        JDKToolLauncher javac = JDKToolLauncher.create("javac")
                                               .addToolArg("-d")
                                               .addToolArg(classesDir.toString())
                                               .addToolArg("-cp")
                                               .addToolArg(Utils.TEST_CLASS_PATH);

        try (Stream<Path> stream = Files.walk(genSrcDir)) {
            stream.map(Path::toAbsolutePath)
                  .map(Path::toString)
                  .filter(s -> s.endsWith(".java"))
                  .forEach(javac::addToolArg);
        } catch (IOException e) {
            throw new Error("traverse dir " + genSrcDir, e);
        }

        executeTool(javac);
        buildJar(name + ".jar", classesDir);
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

    private static void buildJar(String name, Path dir) {
        JDKToolLauncher jar = JDKToolLauncher.create("jar")
                                             .addToolArg("cf")
                                             .addToolArg(name)
                                             .addToolArg("-C")
                                             .addToolArg(dir.toString())
                                             .addToolArg(".");
        executeTool(jar);
    }

    private static void moveJavaFiles(Path dir, String prefix) {
        try (Stream<Path> stream = Files.list(Paths.get("."))) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> {
                      String s = p.getFileName().toString();
                      return s.startsWith(prefix) && s.endsWith(".java");})
                  .forEach(p -> move(p, dir));
        } catch (IOException e) {
            throw new Error("can't traverse current dir", e);
        }
    }

    private static void move(Path src, Path dstDir) {
        if (Files.notExists(src)) {
            throw new Error("file " + src + " doesn't exit");
        }
        try {
            Files.move(src, dstDir.resolve(src.getFileName()));
        } catch (IOException e) {
            throw new Error("can't move " + src + " to " + dstDir, e);
        }
    }
}
