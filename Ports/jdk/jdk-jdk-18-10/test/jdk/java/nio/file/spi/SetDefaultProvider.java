/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8266345
 * @modules jdk.jartool
 * @library /test/lib
 * @build SetDefaultProvider TestProvider m/* jdk.test.lib.process.ProcessTools
 * @run testng/othervm SetDefaultProvider
 * @summary Runs tests with -Djava.nio.file.spi.DefaultFileSystemProvider set on
 *          the command line to override the default file system provider
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.spi.ToolProvider;

import jdk.test.lib.process.ProcessTools;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class SetDefaultProvider {

    private static String SET_DEFAULT_FSP =
        "-Djava.nio.file.spi.DefaultFileSystemProvider=TestProvider";

    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
        .orElseThrow(() ->
            new RuntimeException("jar tool not found")
        );

    private static Path createTempDirectory(String prefix) throws IOException {
        Path testDir = Paths.get(System.getProperty("test.dir", "."));
        return Files.createTempDirectory(testDir, prefix);
    }

    /**
     * Test override of default FileSystemProvider with the main application
     * on the class path.
     */
    public void testClassPath() throws Exception {
        String moduleClasses = moduleClasses();
        String testClasses = System.getProperty("test.classes");
        String classpath = moduleClasses + File.pathSeparator + testClasses;
        int exitValue = exec(SET_DEFAULT_FSP, "-cp", classpath, "p.Main");
        assertTrue(exitValue == 0);
    }

    /**
     * Test override of default FileSystemProvider with the main application
     * on the class path and a SecurityManager enabled.
     */
    public void testClassPathWithSecurityManager() throws Exception {
        String moduleClasses = moduleClasses();
        String testClasses = System.getProperty("test.classes");
        String classpath = moduleClasses + File.pathSeparator + testClasses;
        String policyFile = System.getProperty("test.src", ".")
            + File.separator + "fs.policy";
        int exitValue = exec(SET_DEFAULT_FSP, "-cp", classpath,
            "-Dtest.classes=" + testClasses, "-Djava.security.manager",
            "-Djava.security.policy==" + policyFile, "p.Main");
        assertTrue(exitValue == 0);
    }

    /**
     * Test override of default FileSystemProvider with the main application
     * on the module path as an exploded module.
     */
    public void testExplodedModule() throws Exception {
        String modulePath = System.getProperty("jdk.module.path");
        int exitValue = exec(SET_DEFAULT_FSP, "-p", modulePath, "-m", "m/p.Main");
        assertTrue(exitValue == 0);
    }

    /**
     * Test override of default FileSystemProvider with the main application
     * on the module path as a modular JAR.
     */
    public void testModularJar() throws Exception {
        String jarFile = createModularJar();
        int exitValue = exec(SET_DEFAULT_FSP, "-p", jarFile, "-m", "m/p.Main");
        assertTrue(exitValue == 0);
    }

    /**
     * Test override of default FileSystemProvider where the main application
     * is a module that is patched by an exploded patch.
     */
    public void testExplodedModuleWithExplodedPatch() throws Exception {
        Path patchdir = createTempDirectory("patch");
        String modulePath = System.getProperty("jdk.module.path");
        int exitValue = exec(SET_DEFAULT_FSP,
                             "--patch-module", "m=" + patchdir,
                             "-p", modulePath,
                             "-m", "m/p.Main");
        assertTrue(exitValue == 0);
    }

    /**
     * Test override of default FileSystemProvider where the main application
     * is a module that is patched by an exploded patch.
     */
    public void testExplodedModuleWithJarPatch() throws Exception {
        Path patchdir = createTempDirectory("patch");
        Files.createDirectory(patchdir.resolve("m.properties"));
        Path patch = createJarFile(patchdir);
        String modulePath = System.getProperty("jdk.module.path");
        int exitValue = exec(SET_DEFAULT_FSP,
                             "--patch-module", "m=" + patch,
                             "-p", modulePath,
                             "-m", "m/p.Main");
        assertTrue(exitValue == 0);
    }

    /**
     * Returns the directory containing the classes for module "m".
     */
    private String moduleClasses() {
        String mp = System.getProperty("jdk.module.path");
        for (String dir : mp.split(File.pathSeparator)) {
            Path m = Paths.get(dir, "m");
            if (Files.exists(m)) return m.toString();
        }
        assertFalse(true);
        return null;
    }

    /**
     * Creates a modular JAR containing module "m".
     */
    private String createModularJar() throws Exception {
        Path dir = Paths.get(moduleClasses());
        Path jar = createJarFile(dir);
        return jar.toString();
    }

    /**
     * Creates a JAR file containing the entries in the given file tree.
     */
    private Path createJarFile(Path dir) throws Exception {
        Path jar = createTempDirectory("tmp").resolve("m.jar");
        String[] args = { "--create", "--file=" + jar, "-C", dir.toString(), "." };
        int ret = JAR_TOOL.run(System.out, System.out, args);
        assertTrue(ret == 0);
        return jar;
    }

    /**
     * Invokes the java launcher with the given arguments, returning the exit code.
     */
    private int exec(String... args) throws Exception {
       return ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();
    }
}
