/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ModuleTestUtil {

    private ModuleTestUtil() {
        // Private constructor to prevent class instantiation
    }

    /**
     * Compile all the java sources and copy the resource files in the module.
     *
     * @param src path to the source directory
     * @param dest path to the destination directory
     * @param mn module name
     * @param resFormat resource format
     */
    public static void prepareModule(Path src, Path dest, String mn,
            String resFormat) {
        compileModule(src, dest, mn);
        copyResFiles(src, dest, mn, resFormat);
    }

    /**
     * Compile all the java sources in the module.
     *
     * @param src path to the source directory
     * @param dest path to the destination directory
     * @param mn module name
     */
    public static void compileModule(Path src, Path dest, String mn) {
        try {
            boolean compiled = CompilerUtils.compile(src.resolve(mn), dest,
                    "--module-source-path", src.toString());
            if (!compiled) {
                throw new RuntimeException("Compile module " + mn + " failed.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Compile module " + mn + " failed.");
        }
    }

    /**
     * Compile all the java sources in the unnamed package.
     *
     * @param src path to the source directory
     * @param dest path to the destination directory
     * @param pn package name
     */
    public static void compilePkg(Path src, Path dest, String pn) {
        try {
            boolean compiled = CompilerUtils.compile(src.resolve(pn),
                    dest.resolve(pn));
            if (!compiled) {
                throw new RuntimeException("Compile package " + pn + " failed.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Compile package " + pn + " failed.");
        }
    }

    /**
     * Copy all the resource files.
     *
     * @param src path to the source directory
     * @param dest path to the destination directory
     * @param mn module name
     * @param resFormat resource format
     */
    public static void copyResFiles(Path src, Path dest, String mn,
            String resFormat) {
        try (Stream<Path> stream = Files.walk(src.resolve(mn))
                .filter(path -> path.toString().endsWith(resFormat))) {
            stream.forEach(f -> {
                String resName = f.toString();
                String relativePath = resName.substring(src.toString().length());
                Path destFile = Paths.get(dest.toString() + relativePath);
                try {
                    Path destParentDir = destFile.getParent();
                    if (Files.notExists(destParentDir)) {
                        Files.createDirectories(destParentDir);
                    }
                    Files.copy(f, destFile, REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Copy " + f.toString() + " to "
                            + destFile.toString() + " failed.");
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Copy resource files failed.");
        }
    }

    /**
     * Run the module test.
     *
     * @param mp module path
     * @param mn module name
     * @param localeList locale list
     */
    public static void runModule(String mp, String mn, List<String> localeList)
            throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("java");
        launcher.addToolArg("-ea")
                .addToolArg("-esa")
                .addToolArg("-p")
                .addToolArg(mp)
                .addToolArg("-m")
                .addToolArg(mn);
        localeList.forEach(launcher::addToolArg);

        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                                   .getExitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Execution of the test failed. "
                    + "Unexpected exit code: " + exitCode);
        }
    }

    /**
     * Run the module test with a jar file specified by the classpath.
     *
     * @param cp classpath
     * @param mp module path
     * @param mn module name
     * @param localeList locale list
     * @param expected expected execution status
     */
    public static void runModuleWithCp(String cp, String mp, String mn,
            List<String> localeList, boolean expected) throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("java");
        launcher.addToolArg("-ea")
                .addToolArg("-esa")
                .addToolArg("-cp")
                .addToolArg(cp)
                .addToolArg("-p")
                .addToolArg(mp)
                .addToolArg("-m")
                .addToolArg(mn);
        localeList.forEach(launcher::addToolArg);

        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                                   .getExitValue();
        if (expected) {
            if (exitCode != 0) {
                throw new RuntimeException("Execution of the test loads bundles "
                        + "from the jar file specified by the class-path failed. "
                        + "Unexpected exit code: " + exitCode);
            }
        } else {
            if (exitCode == 0) {
                throw new RuntimeException("Execution of the test not loads bundles "
                        + "from the jar file specified by the class-path failed. "
                        + "Unexpected exit code: " + exitCode);
            }
        }
    }

    /**
     * Run the module test with "useOldISOCodes=true".
     *
     * @param mp module path
     * @param mn module name
     * @param localeList locale list
     */
    public static void runModuleWithLegacyCode(String mp, String mn, List<String> localeList)
            throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("java");
        launcher.addToolArg("-ea")
                .addToolArg("-esa")
                .addToolArg("-Djava.locale.useOldISOCodes=true")
                .addToolArg("-p")
                .addToolArg(mp)
                .addToolArg("-m")
                .addToolArg(mn);
        localeList.forEach(launcher::addToolArg);

        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                .getExitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Execution of the test failed. "
                    + "Unexpected exit code: " + exitCode);
        }
    }
}