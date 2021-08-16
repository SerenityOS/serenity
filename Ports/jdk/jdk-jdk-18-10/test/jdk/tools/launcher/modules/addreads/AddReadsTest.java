/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @build AddReadsTest
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.JarUtils
 * @run testng AddReadsTest
 * @summary Basic tests for java --add-reads
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * The tests consists of two modules: m1 and junit.
 * Code in module m1 calls into code in module junit but the module-info.java
 * does not have a 'requires'. Instead a read edge is added via the command
 * line option --add-reads.
 */

@Test
public class AddReadsTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path CLASSES_DIR = Paths.get("classes");
    private static final Path MODS_DIR = Paths.get("mods");

    private static final String MAIN = "m1/p.Main";


    @BeforeTest
    public void setup() throws Exception {

        // javac -d classes src/junit/**
        assertTrue(CompilerUtils
            .compile(SRC_DIR.resolve("junit"), CLASSES_DIR));

        // javac -d mods/m1 -cp classes/ src/m1/**
        assertTrue(CompilerUtils
            .compile(SRC_DIR.resolve("m1"),
                    MODS_DIR.resolve("m1"),
                    "-cp", CLASSES_DIR.toString(),
                    "--add-reads", "m1=ALL-UNNAMED"));

        // jar cf mods/junit.jar -C classes .
        JarUtils.createJarFile(MODS_DIR.resolve("junit.jar"), CLASSES_DIR);

    }

    private OutputAnalyzer run(String... options) throws Exception {
        return executeTestJava(options)
            .outputTo(System.out)
            .errorTo(System.out);
    }


    /**
     * Run with junit as a module on the module path.
     */
    public void testJUnitOnModulePath() throws Exception {

        // java --module-path mods --add-modules junit --add-reads m1=junit -m ..
        int exitValue
            = run("--module-path", MODS_DIR.toString(),
                  "--add-modules", "junit",
                  "--add-reads", "m1=junit",
                  "-m", MAIN)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Exercise --add-reads m1=ALL-UNNAMED by running with junit on the
     * class path.
     */
    public void testJUnitOnClassPath() throws Exception {

        // java --module-path mods -cp mods/junit.jar --add-reads m1=ALL-UNNAMED -m ..
        String cp = MODS_DIR.resolve("junit.jar").toString();
        int exitValue
            = run("--module-path", MODS_DIR.toString(),
                  "-cp", cp,
                  "--add-reads", "m1=ALL-UNNAMED",
                  "-m", MAIN)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Run with junit as a module on the module path but without --add-reads.
     */
    public void testJUnitOnModulePathMissingAddReads() throws Exception {
        // java --module-path mods --add-modules junit --module ..
        int exitValue
            = run("--module-path", MODS_DIR.toString(),
                  "--add-modules", "junit",
                  "--module", MAIN)
                .shouldContain("IllegalAccessError")
                .getExitValue();

        assertTrue(exitValue != 0);
    }


    /**
     * Run with junit on the class path but without --add-reads.
     */
    public void testJUnitOnClassPathMissingAddReads() throws Exception {
        // java --module-path mods -cp mods/junit.jar -m ..
        String cp = MODS_DIR.resolve("junit.jar").toString();
        int exitValue
            = run("--module-path", MODS_DIR.toString(),
                  "-cp", cp,
                  "-m", MAIN)
                .shouldContain("IllegalAccessError")
                .getExitValue();

        assertTrue(exitValue != 0);
    }


    /**
     * Exercise --add-reads with a more than one source module.
     */
    public void testJUnitWithMultiValueOption() throws Exception {

        int exitValue
            = run("--module-path", MODS_DIR.toString(),
                  "--add-modules", "java.xml,junit",
                  "--add-reads", "m1=java.xml,junit",
                  "--module", MAIN)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Exercise --add-reads where the target module is specified more than once
     */
    public void testWithTargetSpecifiedManyTimes() throws Exception {

        int exitValue
            = run("--module-path", MODS_DIR.toString(),
                  "--add-modules", "java.xml,junit",
                  "--add-reads", "m1=java.xml",
                  "--add-reads", "m1=junit",
                  "-m", MAIN)
                 .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Exercise --add-reads with missing source
     */
    public void testWithMissingSource() throws Exception {

        //  --add-exports $VALUE -version
        assertTrue(run("--add-reads", "java.base", "-version").getExitValue() != 0);
    }


    /**
     * Exercise --add-reads with unknown source/target module.
     * Warning is emitted.
     */
    @Test(dataProvider = "badvalues")
    public void testWithBadValue(String value, String ignore) throws Exception {

        //  --add-exports $VALUE -version
        int exitValue = run("--add-reads", value, "-version")
                            .stderrShouldMatch("WARNING: Unknown module: .*.monkey")
                            .outputTo(System.out)
                            .errorTo(System.out)
                            .getExitValue();

        assertTrue(exitValue == 0);
    }

    @DataProvider(name = "badvalues")
    public Object[][] badValues() {
        return new Object[][]{

            { "java.monkey=java.base",      null }, // unknown module
            { "java.base=sun.monkey",       null }, // unknown source

        };
    }
}
