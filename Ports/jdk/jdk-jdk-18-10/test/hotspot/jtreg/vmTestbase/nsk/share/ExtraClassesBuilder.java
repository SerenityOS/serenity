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

package nsk.share;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

public class ExtraClassesBuilder {
    public static void main(String[] args) {
        String[] javacOpts = Arrays.stream(args)
                                   .takeWhile(s -> s.startsWith("-"))
                                   .toArray(String[]::new);

        Arrays.stream(args)
              .dropWhile(s -> s.startsWith("-"))
              .forEach(s -> ExtraClassesBuilder.compile(s, javacOpts));
    }

    private static void compile(String name, String[] args) {
        Path src = Paths.get(Utils.TEST_SRC)
                        .resolve(name)
                        .toAbsolutePath();
        if (Files.notExists(src)) {
            throw new Error(src + " doesn't exist");
        }
        Path dst = Paths.get("bin")
                        .resolve(Paths.get(name).getFileName())
                        .toAbsolutePath();
        try {
            Files.createDirectories(dst);
        } catch (IOException e) {
            throw new Error("can't create dir " + dst, e);
        }
        JDKToolLauncher javac = JDKToolLauncher.create("javac")
                                               .addToolArg("-d")
                                               .addToolArg(dst.toString())
                                               .addToolArg("-cp")
                                               .addToolArg(Utils.TEST_CLASS_PATH);

        for (String arg : args) {
            javac.addToolArg(arg);
        }

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
