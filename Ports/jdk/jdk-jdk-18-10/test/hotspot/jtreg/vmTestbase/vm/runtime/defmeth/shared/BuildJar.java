/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Build {@code retransform.jar} in current directory using
 * {@code vm/runtime/defmeth/shared/retransform.mf} and classes from
 * {@code vm.runtime.defmeth.shared} package.
 */
public class BuildJar {
    public static void main(String[] args) {
        Path manifest = Paths.get(Utils.TEST_ROOT)
                             .resolve("vmTestbase")
                             .resolve("vm")
                             .resolve("runtime")
                             .resolve("defmeth")
                             .resolve("shared")
                             .resolve("retransform.mf")
                             .toAbsolutePath();
        if (Files.notExists(manifest)) {
            throw new Error("can't find manifest file: " + manifest);
        }

        Path file = foundInClassPath(Util.Transformer.class).toAbsolutePath();
        // Util$Transformer.class is in vm/runtime/defmeth/shared
        Path dir = file.getParent()
                       .getParent()
                       .getParent()
                       .getParent()
                       .getParent()
                       .toAbsolutePath();

        JDKToolLauncher jar = JDKToolLauncher.create("jar")
                                             .addToolArg("cmf")
                                             .addToolArg(manifest.toString())
                                             .addToolArg("retransform.jar")
                                             .addToolArg("-C")
                                             .addToolArg(dir.toString())
                                             .addToolArg(dir.relativize(file).toString());
        String[] command = jar.getCommand();
        try {
            ProcessTools.executeCommand(command)
                        .shouldHaveExitValue(0);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new Error("execution of jar [" + Arrays.toString(command) + "] failed", e);
        }
    }

    private static Path foundInClassPath(Class<?> aClass) {
        Path file = Paths.get(aClass.getName()
                                    .replace(".", File.separator) + ".class");
        for (String dir : Utils.TEST_CLASS_PATH.split(File.pathSeparator)) {
            Path result = Paths.get(dir).resolve(file);
            if (Files.exists(result)) {
                return result;
            }
        }
        throw new Error("can't find " + file + " in " + Utils.TEST_CLASS_PATH);
    }
}

