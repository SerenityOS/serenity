/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Test combinations of jigsaw options that affect the use of AppCDS
 *
 * @requires vm.cds & !vm.graal.enabled
 * @library /test/lib ..
 * @compile ../test-classes/Hello.java ../test-classes/HelloMore.java
 * @run driver JigsawOptionsCombo
 */
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

import java.util.ArrayList;


// Remaining WORK: TODO:
// 1. test with -m initial-module; waiting for changes from Chris will provide
//    utils to build modules
// 2. Loading classes from Jmod files - waiting on utils
// 3. Loading classes from exploded module dir"

public class JigsawOptionsCombo {

    public static void main(String[] args) throws Exception {
        String source = "package javax.naming.spi; "                +
                        "public class NamingManager { "             +
                        "    static { "                             +
                        "        System.out.println(\"I pass!\"); " +
                        "    } "                                    +
                        "}";
        ClassFileInstaller.writeClassToDisk("javax/naming/spi/NamingManager",
            InMemoryJavaCompiler.compile("javax.naming.spi.NamingManager", source, "--patch-module=java.naming"),
            "mods/java.naming");

        JarBuilder.build("hello", "Hello");
        JarBuilder.build("hello_more", "HelloMore");

        (new JigsawOptionsCombo()).runTests();
    }


    private ArrayList<TestCase> testCaseTable = new ArrayList<TestCase>();

    public static String infoDuringDump(String option) {
        return "Cannot use the following option when dumping the shared archive: " + option;
    }

    public void runTests() throws Exception {

        testCaseTable.add(new TestCase(
            "basic: Basic dump and execute, to verify the test plumbing works",
            "", "", 0,
            "", "", 0, true) );

        String bcpArg = "-Xbootclasspath/a:" +
        TestCommon.getTestJar("hello_more.jar");

        testCaseTable.add(new TestCase(
            "Xbootclasspath/a: is OK for both dump and run time",
            bcpArg, "", 0,
            bcpArg, "", 0, true) );

        testCaseTable.add(new TestCase(
            "module-path-01: --module-path is ignored for dump time",
            "--module-path mods", "", 0,
            null, null, 0, true) );

        testCaseTable.add(new TestCase(
            "module-path-02: --module-path is ok for run time",
            "", "", 0,
            "--module-path mods", "", 0, true) );

        testCaseTable.add(new TestCase(
            "add-modules-01: --add-modules is ok at dump time",
            "--add-modules java.management",
            "", 0,
            null, null, 0, true) );

        testCaseTable.add(new TestCase(
            "add-modules-02: --add-modules is ok at run time",
            "", "", 0,
            "--add-modules java.management", "", 0, true) );

        testCaseTable.add(new TestCase(
            "limit-modules-01: --limit-modules is ignored at dump time",
            "--limit-modules java.base",
            infoDuringDump("--limit-modules"), 1,
            null, null, 0, true) );

        testCaseTable.add(new TestCase(
            "limit-modules-02: --limit-modules is ok at run time",
            "", "", 0,
            "--limit-modules java.base", "", 0, false) );

        testCaseTable.add(new TestCase(
            "upgrade-module-path-01: --upgrade-module-path is ignored at dump time",
            "--upgrade-module-path mods",
            infoDuringDump("--upgrade-module-path"), 1,
            null, null, 0, true) );

        testCaseTable.add(new TestCase(
            "-upgrade-module-path-module-path-02: --upgrade-module-path is ok at run time",
            "", "", 0,
            "--upgrade-module-path mods", "", 0, false) );

        for (TestCase tc : testCaseTable) tc.execute();
    }


    // class representing a singe test case
    public class TestCase {
        String description;
        String dumpTimeArgs;
        String dumpTimeExpectedOutput;
        int    dumpTimeExpectedExitValue;
        String runTimeArgs;
        String runTimeExpectedOutput;
        int    runTimeExpectedExitValue;
        boolean sharingOn;

        private String appJar = TestCommon.getTestJar("hello.jar");
        private String appClasses[] = {"Hello"};


        public TestCase(String description,
            String dumpTimeArgs, String dumpTimeExpectedOutput, int dumpTimeExpectedExitValue,
            String runTimeArgs, String runTimeExpectedOutput, int runTimeExpectedExitValue,
            boolean sharingOn) {

            this.description = description;
            this.dumpTimeArgs = dumpTimeArgs;
            this.dumpTimeExpectedOutput = dumpTimeExpectedOutput;
            this.dumpTimeExpectedExitValue = dumpTimeExpectedExitValue;
            this.runTimeArgs = runTimeArgs;
            this.runTimeExpectedOutput = runTimeExpectedOutput;
            this.runTimeExpectedExitValue = runTimeExpectedExitValue;
            this.sharingOn = sharingOn;
        }


        public void execute() throws Exception {
            System.out.println("Description: " + description);

            // ===== dump step - create the archive
            OutputAnalyzer dumpOutput = TestCommon.dump(
                appJar, appClasses, getDumpOptions());

            if (dumpTimeExpectedExitValue == 0) {
                TestCommon.checkDump(dumpOutput, dumpTimeExpectedOutput);
            } else {
                dumpOutput.shouldMatch(dumpTimeExpectedOutput);
                dumpOutput.shouldHaveExitValue(dumpTimeExpectedExitValue);
            }

            // ===== exec step - use the archive
            if (runTimeArgs != null) {
                OutputAnalyzer execOutput = TestCommon.exec(appJar, getRunOptions());

                if (runTimeExpectedExitValue == 0) {
                    if (sharingOn) {
                        TestCommon.checkExec(execOutput, runTimeExpectedOutput, "Hello World");
                    } else {
                        execOutput.shouldHaveExitValue(0)
                                  .shouldContain(runTimeExpectedOutput)
                                  .shouldContain("Hello World");
                    }
                } else {
                    execOutput.shouldMatch(dumpTimeExpectedOutput);
                    execOutput.shouldHaveExitValue(dumpTimeExpectedExitValue);
                }
            }
        }


        // dump command line options can be separated by a space
        private String[] getDumpOptions() {
            return dumpTimeArgs.split(" ");
        }


        // run command line options can be separated by a space
        private String[] getRunOptions() {
            ArrayList<String> result = new ArrayList<>();

            if (runTimeArgs != "") {
                String splitArgs[] = runTimeArgs.split(" ");
                for (String arg : splitArgs)
                    result.add(arg);
            }

            result.add("Hello");
            return result.toArray(new String[1]);
        }
    }
}
