/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.jartool
 *          jdk.jlink
 * @build BasicTest jdk.test.lib.compiler.CompilerUtils
 * @run testng BasicTest
 * @bug 8234076
 * @summary Basic test of starting an application as a module
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.spi.ToolProvider;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;


@Test
public class BasicTest {
    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
        .orElseThrow(() ->
            new RuntimeException("jar tool not found")
        );
    private static final ToolProvider JMOD_TOOL = ToolProvider.findFirst("jmod")
        .orElseThrow(() ->
            new RuntimeException("jmod tool not found")
        );

    private static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name of the test module
    private static final String TEST_MODULE = "test";

    // the module main class
    private static final String MAIN_CLASS = "jdk.test.Main";

    // for Windows specific launcher tests
    static final boolean IS_WINDOWS = System.getProperty("os.name", "unknown").startsWith("Windows");

    @BeforeTest
    public void compileTestModule() throws Exception {

        // javac -d mods/$TESTMODULE src/$TESTMODULE/**
        boolean compiled
            = CompilerUtils.compile(SRC_DIR.resolve(TEST_MODULE),
                                    MODS_DIR.resolve(TEST_MODULE));

        assertTrue(compiled, "test module did not compile");
    }

    /**
     * Execute "java" with the given arguments, returning the exit code.
     */
    private int exec(String... args) throws Exception {
       return ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();
    }


    /**
     * The initial module is loaded from an exploded module
     */
    public void testRunWithExplodedModule() throws Exception {
        String dir = MODS_DIR.toString();
        String subdir = MODS_DIR.resolve(TEST_MODULE).toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        // java --module-path mods -module $TESTMODULE/$MAINCLASS
        int exitValue = exec("--module-path", dir, "--module", mid);
        assertTrue(exitValue == 0);

        // java --module-path mods/$TESTMODULE --module $TESTMODULE/$MAINCLASS
        exitValue = exec("--module-path", subdir, "--module", mid);
        assertTrue(exitValue == 0);

        // java --module-path=mods --module=$TESTMODULE/$MAINCLASS
        exitValue = exec("--module-path=" + dir, "--module=" + mid);
        assertTrue(exitValue == 0);

        // java --module-path=mods/$TESTMODULE --module=$TESTMODULE/$MAINCLASS
        exitValue = exec("--module-path=" + subdir, "--module=" + mid);
        assertTrue(exitValue == 0);

        // java -p mods -m $TESTMODULE/$MAINCLASS
        exitValue = exec("-p", dir, "-m", mid);
        assertTrue(exitValue == 0);

        // java -p mods/$TESTMODULE -m $TESTMODULE/$MAINCLASS
        exitValue = exec("-p", subdir, "-m", mid);
        assertTrue(exitValue == 0);
    }


    /**
     * The initial module is loaded from a modular JAR file
     */
    public void testRunWithModularJar() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mlib");
        Path jar = dir.resolve("m.jar");

        // jar --create ...
        String classes = MODS_DIR.resolve(TEST_MODULE).toString();
        String[] args = {
            "--create",
            "--file=" + jar,
            "--main-class=" + MAIN_CLASS,
            "-C", classes, "."
        };
        int rc = JAR_TOOL.run(System.out, System.out, args);
        assertTrue(rc == 0);

        // java --module-path mlib -module $TESTMODULE
        int exitValue = exec("--module-path", dir.toString(),
                             "--module", TEST_MODULE);
        assertTrue(exitValue == 0);

        // java --module-path mlib/m.jar -module $TESTMODULE
        exitValue = exec("--module-path", jar.toString(),
                         "--module", TEST_MODULE);
        assertTrue(exitValue == 0);
    }


    /**
     * Attempt to run with the initial module packaged as a JMOD file.
     */
    public void testTryRunWithJMod() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mlib");

        // jmod create ...
        String cp = MODS_DIR.resolve(TEST_MODULE).toString();
        String jmod = dir.resolve("m.jmod").toString();
        String[] args = {
            "create",
            "--class-path", cp,
            "--main-class", MAIN_CLASS,
            jmod
        };

        assertEquals(JMOD_TOOL.run(System.out, System.out, args), 0);

        // java --module-path mods --module $TESTMODULE
        int exitValue = exec("--module-path", dir.toString(),
                             "--module", TEST_MODULE);
        assertTrue(exitValue != 0);
    }


    /**
     * Run the test with a non-existent file on the application module path.
     * It should be silently ignored.
     */
    public void testRunWithNonExistentEntry() throws Exception {
        String mp = "DoesNotExist" + File.pathSeparator + MODS_DIR.toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        // java --module-path mods --module $TESTMODULE/$MAINCLASS
        int exitValue = exec("--module-path", mp, "--module", mid);
        assertTrue(exitValue == 0);
    }


    /**
     * Attempt to run an unknown initial module
     */
    public void testTryRunWithBadModule() throws Exception {
        String modulepath = MODS_DIR.toString();

        // java --module-path mods -m $TESTMODULE
        int exitValue = exec("--module-path", modulepath, "-m", "rhubarb");
        assertTrue(exitValue != 0);
    }


    /**
     * Attempt to run with -m specifying a main class that does not
     * exist.
     */
    public void testTryRunWithBadMainClass() throws Exception {
        String modulepath = MODS_DIR.toString();
        String mid = TEST_MODULE + "/p.rhubarb";

        // java --module-path mods -m $TESTMODULE/$MAINCLASS
        int exitValue = exec("--module-path", modulepath, "-m", mid);
        assertTrue(exitValue != 0);
    }


    /**
     * Attempt to run with -m specifying a modular JAR that does not have
     * a MainClass attribute
     */
    public void testTryRunWithMissingMainClass() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mlib");

        // jar --create ...
        String classes = MODS_DIR.resolve(TEST_MODULE).toString();
        String jar = dir.resolve("m.jar").toString();
        String[] args = {
            "--create",
            "--file=" + jar,
            "-C", classes, "."
        };
        int rc = JAR_TOOL.run(System.out, System.out, args);
        assertTrue(rc == 0);

        // java --module-path mods -m $TESTMODULE
        int exitValue = exec("--module-path", dir.toString(), "-m", TEST_MODULE);
        assertTrue(exitValue != 0);
    }


    /**
     * Attempt to run with -m specifying a main class that is a different
     * module to that specified to -m
     */
    public void testTryRunWithMainClassInWrongModule() throws Exception {
        String modulepath = MODS_DIR.toString();
        String mid = "java.base/" + MAIN_CLASS;

        // java --module-path mods --module $TESTMODULE/$MAINCLASS
        int exitValue = exec("--module-path", modulepath, "--module", mid);
        assertTrue(exitValue != 0);
    }


    /**
     * Helper method that creates a ProcessBuilder with command line arguments
     * while setting the _JAVA_LAUNCHER_DEBUG environment variable.
     */
    private ProcessBuilder createProcessWithLauncherDebugging(String... cmds) {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(Utils.addTestJavaOpts(cmds));
        pb.environment().put("_JAVA_LAUNCHER_DEBUG", "true");

        return pb;
    }

     /**
     * Test the ability for the Windows launcher to do proper application argument
     * detection and expansion, when using the long form module option and all passed in
     * command line arguments are prefixed with a dash.
     *
     * These tests are not expected to work on *nixes, and are ignored.
     */
    public void testWindowsWithLongFormModuleOption() throws Exception {
        if (!IS_WINDOWS) {
            return;
        }

        String dir = MODS_DIR.toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        // java --module-path=mods --module=$TESTMODULE/$MAINCLASS --help
        // We should be able to find the argument --help as an application argument
        ProcessTools.executeProcess(
            createProcessWithLauncherDebugging(
                "--module-path=" + dir,
                "--module=" + mid,
                "--help"))
            .outputTo(System.out)
            .errorTo(System.out)
            .shouldContain("F--help");

        // java --module-path=mods --module=$TESTMODULE/$MAINCLASS <...src/test>/*.java --help
        // We should be able to see argument expansion happen
        ProcessTools.executeProcess(
            createProcessWithLauncherDebugging(
                "--module-path=" + dir,
                "--module=" + mid,
                SRC_DIR.resolve(TEST_MODULE).toString() + "\\*.java",
                "--help"))
            .outputTo(System.out)
            .errorTo(System.out)
            .shouldContain("F--help")
            .shouldContain("module-info.java");
    }


    /**
     * Test that --module= is terminating for VM argument processing just like --module
     */
    public void testLongFormModuleOptionTermination() throws Exception {
        String dir = MODS_DIR.toString();
        String mid = TEST_MODULE + "/" + MAIN_CLASS;

        // java --module-path=mods --module=$TESTMODULE/$MAINCLASS --module-path=mods --module=$TESTMODULE/$MAINCLASS
        // The first --module= will terminate the VM arguments processing. The second pair of module-path and module will be
        // deemed as application arguments
        OutputAnalyzer output = ProcessTools.executeProcess(
            createProcessWithLauncherDebugging(
                "--module-path=" + dir,
                "--module=" + mid,
                "--module-path=" + dir,
                "--module=" + mid))
            .outputTo(System.out)
            .errorTo(System.out)
            .shouldContain("argv[ 0] = '--module-path=" + dir)
            .shouldContain("argv[ 1] = '--module=" + mid);

        if (IS_WINDOWS) {
            output.shouldContain("F--module-path=" + dir).shouldContain("F--module=" + mid);
        }

        // java --module=$TESTMODULE/$MAINCLASS --module-path=mods
        // This command line will not work as --module= is terminating and the module will be not found
        int exitValue = exec("--module=" + mid, "--module-path" + dir);
        assertTrue(exitValue != 0);
    }
}
