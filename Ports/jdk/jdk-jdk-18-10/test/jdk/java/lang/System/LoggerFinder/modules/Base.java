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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.compiler.CompilerUtils;

import static jdk.test.lib.process.ProcessTools.executeCommand;

/*
 * Base class for tests.
 * The tests focuse on that LoggerFinder works well in jigsaw environment,
 * i.e. make sure correct Logger can be retrieved,
 * also verify that basic functionality of retrieved Logger's works well.
 *
 * Note: As the test will take long time, to avoid timeout,
 * split it as several tests, this class is the base class for tests.
 */
public class Base {
    protected static final String JAVA_HOME = System.getProperty("java.home");
    protected static final Path JDK_IMAGE = Paths.get(JAVA_HOME);
    protected static final Path JMODS = Paths.get(JAVA_HOME, "jmods");

    protected static final String TEST_SRC = System.getProperty("test.src");

    // logger client to get logger from java.base module, it should get a lazy logger
    // which wraps the underlying real logger implementation
    protected static final Path SRC_PATCHED_USAGE =
            Paths.get(TEST_SRC, "patched_usage", "java.base");
    protected static final Path DEST_PATCHED_USAGE = Paths.get("patched_usage", "java.base");
    protected static final Path SRC_PATCHED_CLIENT = Paths.get(TEST_SRC, "patched_client");
    protected static final Path DEST_PATCHED_CLIENT = Paths.get("patched_client");

    // logger client to get logger from bootclasspath/a, it should get a lazy logger
    // which wraps the underlying real logger implementation
    protected static final Path SRC_BOOT_USAGE = Paths.get(TEST_SRC, "boot_usage");
    protected static final Path DEST_BOOT_USAGE = Paths.get("boot_usage");
    protected static final Path SRC_BOOT_CLIENT = Paths.get(TEST_SRC, "boot_client");
    protected static final Path DEST_BOOT_CLIENT = Paths.get("boot_client");

    // logger provider in named module m.l.a
    protected static final Path SRC_NAMED_LOGGER = Paths.get(TEST_SRC, "named_logger");
    protected static final Path DEST_NAMED_LOGGER = Paths.get("mods_named_logger");

    // logger provider in unnamed module
    protected static final Path SRC_UNNAMED_LOGGER = Paths.get(TEST_SRC, "unnamed_logger");
    protected static final Path DEST_UNNAMED_LOGGER = Paths.get("cp_unnamed_logger");
    protected static final Path SRC_UNNAMED_LOGGER_SERVICE_FILE =
            SRC_UNNAMED_LOGGER.resolve("META-INF/services/java.lang.System$LoggerFinder");
    protected static final Path DEST_UNNAMED_LOGGER_SERVICE_DIR =
            DEST_UNNAMED_LOGGER.resolve("META-INF/services");
    protected static final Path DEST_UNNAMED_LOGGER_SERVICE_FILE =
            DEST_UNNAMED_LOGGER.resolve("META-INF/services/java.lang.System$LoggerFinder");

    // logger client in named module m.t.a
    protected static final Path SRC_NAMED_CLIENT = Paths.get(TEST_SRC, "named_client");
    protected static final Path DEST_NAMED_CLIENT = Paths.get("mods_named_client");

    // logger client in unnamed module
    protected static final Path SRC_UNNAMED_CLIENT = Paths.get(TEST_SRC, "unnamed_client");
    protected static final Path DEST_UNNAMED_CLIENT = Paths.get("cp_unnamed_client");

    // customized image with only module java.base
    protected static final Path IMAGE = Paths.get("image");
    // customized image with java.base and logger provider module m.l.a
    protected static final Path IMAGE_LOGGER = Paths.get("image_logger");
    // customized image with module java.base and logger client module m.t.a
    protected static final Path IMAGE_CLIENT = Paths.get("image_client");
    // customized image with module java.base, logger provider module m.l.a
    // and logger client module m.t.a
    protected static final Path IMAGE_CLIENT_LOGGER = Paths.get("image_all");

    // lazy logger class which wraps the underlying real logger implementation
    protected static final String LAZY_LOGGER =
            "jdk.internal.logger.LazyLoggers$JdkLazyLogger";
    // JUL logger class which wraps java.util.logging.Logger
    protected static final String JUL_LOGGER =
            "sun.util.logging.internal.LoggingProviderImpl$JULWrapper";
    // default simple logger class when no logger provider can be found
    protected static final String SIMPLE_LOGGER =
            "jdk.internal.logger.SimpleConsoleLogger";
    // logger class in named module m.l.a
    protected static final String LOGGER_A = "pkg.a.l.LoggerA";
    // logger class in unnamed module m.l.b
    protected static final String LOGGER_B = "pkg.b.l.LoggerB";

    // logger client in named module
    protected static final String CLIENT_A = "m.t.a/pkg.a.t.TestA";
    // logger client in unnamed module
    protected static final String CLIENT_B = "pkg.b.t.TestB";
    // logger client which gets logger through boot class BootUsage
    protected static final String BOOT_CLIENT = "BootClient";
    // logger client which gets logger through patched class
    // java.base/java.lang.PatchedUsage
    protected static final String PATCHED_CLIENT = "PatchedClient";

    protected void setupAllClient() throws Throwable {
        // compiles logger client which will get logger through patched
        // class java.base/java.lang.PatchedUsage
        compile(SRC_BOOT_USAGE, DEST_BOOT_USAGE);
        compile(SRC_BOOT_CLIENT, DEST_BOOT_CLIENT,
                "--class-path", DEST_BOOT_USAGE.toString());

        // compiles logger client which will get logger through boot
        // class BootUsage
        compile(SRC_PATCHED_USAGE, DEST_PATCHED_USAGE,
                "--patch-module", "java.base=" + SRC_PATCHED_USAGE.toString());
        compile(SRC_PATCHED_CLIENT, DEST_PATCHED_CLIENT,
                "--patch-module", "java.base=" + DEST_PATCHED_USAGE.toString());

        // compiles logger client in unnamed module
        compile(SRC_UNNAMED_CLIENT, DEST_UNNAMED_CLIENT,
                "--source-path", SRC_UNNAMED_CLIENT.toString());

        // compiles logger client in named module m.t.a
        compile(SRC_NAMED_CLIENT, DEST_NAMED_CLIENT,
                "--module-source-path", SRC_NAMED_CLIENT.toString());
    }

    protected void setupNamedLogger() throws Throwable {
        // compiles logger provider in named module m.l.a
        compile(SRC_NAMED_LOGGER, DEST_NAMED_LOGGER,
                "--module-source-path", SRC_NAMED_LOGGER.toString());
    }

    protected void setupUnnamedLogger() throws Throwable {
        // compiles logger provider in unnamed module
        compile(SRC_UNNAMED_LOGGER, DEST_UNNAMED_LOGGER,
                "--source-path", SRC_UNNAMED_LOGGER.toString());
        Files.createDirectories(DEST_UNNAMED_LOGGER_SERVICE_DIR);
        Files.copy(SRC_UNNAMED_LOGGER_SERVICE_FILE, DEST_UNNAMED_LOGGER_SERVICE_FILE,
                   StandardCopyOption.REPLACE_EXISTING);
    }

    protected boolean checkJMODS() throws Throwable {
        // if $JAVA_HOME/jmods does not exist, skip below steps
        // as there is no way to build customized images by jlink
        if (Files.notExists(JMODS)) {
            System.err.println("Skip tests which require image");
            return false;
        }
        return true;
    }

    protected void setupJavaBaseImage() throws Throwable {
        if (!checkJMODS()) {
            return;
        }

        // build image with just java.base module
        String mpath = JMODS.toString();
        execTool("jlink",
                "--module-path", mpath,
                "--add-modules", "java.base",
                "--output", IMAGE.toString());
    }

    protected void setupLoggerImage() throws Throwable {
        if (!checkJMODS()) {
            return;
        }

        // build image with java.base + m.l.a modules
        String mpath = DEST_NAMED_LOGGER.toString() + File.pathSeparator + JMODS.toString();
        execTool("jlink",
                "--module-path", mpath,
                "--add-modules", "m.l.a",
                "--output", IMAGE_LOGGER.toString());
    }

    protected void setupClientImage() throws Throwable {
        if (!checkJMODS()) {
            return;
        }

        // build image with java.base + m.t.a modules
        String mpath = DEST_NAMED_CLIENT.toString() + File.pathSeparator + JMODS.toString();
        execTool("jlink",
                "--module-path", mpath,
                "--add-modules", "m.t.a",
                "--output", IMAGE_CLIENT.toString());
    }

    protected void setupFullImage() throws Throwable {
        if (!checkJMODS()) {
            return;
        }

        // build image with java.base + m.l.a + m.t.a modules
        String mpath = DEST_NAMED_LOGGER.toString() + File.pathSeparator
                + DEST_NAMED_CLIENT.toString() + File.pathSeparator + JMODS.toString();
        execTool("jlink",
                "--module-path", mpath,
                "--add-modules", "m.l.a,m.t.a",
                "--output", IMAGE_CLIENT_LOGGER.toString());

    }

    protected static void assertTrue(boolean b) {
        if (!b) {
            throw new RuntimeException("expected true, but get false.");
        }
    }

    /*
     * run test with supplied java image which could be jdk image or customized image
     */
    protected void runTest(Path image, String... opts) throws Throwable {
        String[] options = Stream.concat(Stream.of(getJava(image)), Stream.of(opts))
                                 .toArray(String[]::new);

        ProcessBuilder pb = new ProcessBuilder(options);
        int exitValue = executeCommand(pb).outputTo(System.out)
                                          .errorTo(System.err)
                                          .getExitValue();
        assertTrue(exitValue == 0);
    }

    private void compile(Path src, Path dest, String... params) throws Throwable {
        assertTrue(CompilerUtils.compile(src, dest, params));
    }

    private String getJava(Path image) {
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        Path java = image.resolve("bin").resolve(isWindows ? "java.exe" : "java");
        if (Files.notExists(java))
            throw new RuntimeException(java + " not found");
        return java.toAbsolutePath().toString();
    }

    private void execTool(String tool, String... args) throws Throwable {
        String path = JDKToolFinder.getJDKTool(tool);
        List<String> commands = new ArrayList<>();
        commands.add(path);
        Stream.of(args).forEach(commands::add);
        ProcessBuilder pb = new ProcessBuilder(commands);

        int exitValue = executeCommand(pb).outputTo(System.out)
                                          .errorTo(System.out)
                                          .shouldNotContain("no module is recorded in hash")
                                          .getExitValue();
        assertTrue(exitValue == 0);
    }
}
