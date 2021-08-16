/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test MultiReleaseJars
 * @summary Test multi-release jar with AppCDS.
 * @requires vm.cds
 * @library /test/lib
 * @run main/othervm/timeout=2400 MultiReleaseJars
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class MultiReleaseJars {

    static final int MAJOR_VERSION = Runtime.version().major();
    static final String MAJOR_VERSION_STRING = String.valueOf(MAJOR_VERSION);

    static String[] getMain() {
        String[] sts = {
            "package version;",
            "public class Main {",
            "    public static void main(String[] args) {",
            "        Version version = new Version();",
            "        System.out.println(\"I am running on version \" + version.getVersion());",
            "    }",
            "}"
        };
        return sts;
    }

    static String[] getVersion(int version) {
        String[] sts = {
            "package version;",
            "public class Version {",
            "    public int getVersion(){ return " + version + "; }",
            "}"
        };
        return sts;
    }

    static void writeFile(File file, String... contents) throws Exception {
        if (contents == null) {
            throw new java.lang.RuntimeException("No input for writing to file" + file);
        }
        try (
             FileOutputStream fos = new FileOutputStream(file);
             PrintStream ps = new PrintStream(fos)
        ) {
            for (String str : contents) {
                ps.println(str);
            }
        }
    }

    /* version.jar entries and files:
     * META-INF/
     * META-INF/MANIFEST.MF
     * version/
     * version/Main.class
     * version/Version.class
     * META-INF/versions/
     * META-INF/versions/<major-version>/
     * META-INF/versions/<major-version>/version/
     * META-INF/versions/<major-version>/version/Version.class
     */
    static void createClassFilesAndJar() throws Exception {
        String tempDir = CDSTestUtils.getOutputDir();
        File baseDir = new File(tempDir + File.separator + "base");
        File vDir    = new File(tempDir + File.separator + MAJOR_VERSION_STRING);

        baseDir.mkdirs();
        vDir.mkdirs();

        File fileMain = TestCommon.getOutputSourceFile("Main.java");
        writeFile(fileMain, getMain());

        File fileVersion = TestCommon.getOutputSourceFile("Version.java");
        writeFile(fileVersion, getVersion(7));
        JarBuilder.compile(baseDir.getAbsolutePath(), fileVersion.getAbsolutePath(), "--release", "7");
        JarBuilder.compile(baseDir.getAbsolutePath(), fileMain.getAbsolutePath(),
            "-cp", baseDir.getAbsolutePath(), "--release", MAJOR_VERSION_STRING);

        String[] meta = {
            "Multi-Release: true",
            "Main-Class: version.Main"
        };
        File metainf = new File(tempDir, "mf.txt");
        writeFile(metainf, meta);

        fileVersion = TestCommon.getOutputSourceFile("Version.java");
        writeFile(fileVersion, getVersion(MAJOR_VERSION));
        JarBuilder.compile(vDir.getAbsolutePath(), fileVersion.getAbsolutePath(), "--release", MAJOR_VERSION_STRING);

        JarBuilder.build("version", baseDir, metainf.getAbsolutePath(),
            "--release", MAJOR_VERSION_STRING, "-C", vDir.getAbsolutePath(), ".");

        // the following jar file is for testing case-insensitive "Multi-Release"
        // attibute name
        String[] meta2 = {
            "multi-Release: true",
            "Main-Class: version.Main"
        };
        metainf = new File(tempDir, "mf2.txt");
        writeFile(metainf, meta2);
        JarBuilder.build("version2", baseDir, metainf.getAbsolutePath(),
            "--release", MAJOR_VERSION_STRING, "-C", vDir.getAbsolutePath(), ".");
    }

    static void checkExecOutput(OutputAnalyzer output, String expectedOutput) throws Exception {
        try {
            TestCommon.checkExec(output, expectedOutput);
        } catch (java.lang.RuntimeException re) {
            String cause = re.getMessage();
            if (!expectedOutput.equals(cause)) {
                throw re;
            }
        }
    }

    public static void main(String... args) throws Exception {
        // create version.jar which contains Main.class and Version.class.
        // Version.class has two versions: 8 and the current version.
        createClassFilesAndJar();

        String mainClass          = "version.Main";
        String loadInfo           = "[class,load] version.Version source: shared objects file";
        String appClasses[]       = {"version/Main", "version/Version"};
        String appJar             = TestCommon.getTestJar("version.jar");
        String appJar2            = TestCommon.getTestJar("version2.jar");
        String enableMultiRelease = "-Djdk.util.jar.enableMultiRelease=true";
        String jarVersion         = null;
        String expectedOutput     = null;

        // 1. default to highest version
        //    if META-INF/versions exists, no other commandline options like -Djdk.util.jar.version and
        //    -Djdk.util.jar.enableMultiRelease passed to vm
        OutputAnalyzer output = TestCommon.dump(appJar, appClasses);
        output.shouldContain("Loading classes to share: done.");
        output.shouldHaveExitValue(0);

        output = TestCommon.exec(appJar, mainClass);
        checkExecOutput(output, "I am running on version " + MAJOR_VERSION_STRING);

        // 2. Test versions 7 and the current major version.
        //    -Djdk.util.jar.enableMultiRelease=true (or force), default is true.
        //    a) -Djdk.util.jar.version=7 does not exist in jar.
        //        It will fallback to the root version which is also 7 in this test.
        //    b) -Djdk.util.jar.version=MAJOR_VERSION exists in the jar.
        for (int i : new int[] {7, MAJOR_VERSION}) {
            jarVersion = "-Djdk.util.jar.version=" + i;
            expectedOutput = "I am running on version " + i;
            output = TestCommon.dump(appJar, appClasses, enableMultiRelease, jarVersion);
            output.shouldContain("Loading classes to share: done.");
            output.shouldHaveExitValue(0);

            output = TestCommon.exec(appJar, mainClass);
            checkExecOutput(output, expectedOutput);
        }

        // 3. For unsupported version, 5 and current major version + 1, the multiversion
        // will be turned off, so it will use the default (root) version.
        for (int i : new int[] {5, MAJOR_VERSION + 1}) {
            jarVersion = "-Djdk.util.jar.version=" + i;
            output = TestCommon.dump(appJar, appClasses, enableMultiRelease, jarVersion);
            output.shouldHaveExitValue(0);
            // With the fix for 8172218, multi-release jar is being handled in
            // jdk corelib which doesn't emit the following warning message.
            //output.shouldContain("JDK" + i + " is not supported in multiple version jars");

            output = TestCommon.exec(appJar, mainClass);
            if (i == 5)
                checkExecOutput(output, "I am running on version 7");
            else
                checkExecOutput(output, "I am running on version " + MAJOR_VERSION_STRING);
        }

        // 4. If explicitly disabled from command line for multiversion jar, it will use default
        //    version at root regardless multiversion versions exists.
        //    -Djdk.util.jar.enableMultiRelease=false (not 'true' or 'force')
        for (int i = 6; i < MAJOR_VERSION + 1; i++) {
            jarVersion = "-Djdk.util.jar.version=" + i;
            output = TestCommon.dump(appJar, appClasses, "-Djdk.util.jar.enableMultiRelease=false", jarVersion);
            output.shouldHaveExitValue(0);

            output = TestCommon.exec(appJar, mainClass);
            expectedOutput = "I am running on version 7";
            checkExecOutput(output, expectedOutput);
        }

        // 5. Sanity test with -Xbootclasspath/a
        //    AppCDS behaves the same as the non-AppCDS case. A multi-release
        //    jar file in the -Xbootclasspath/a will be ignored.
        output = TestCommon.dump(appJar, appClasses, "-Xbootclasspath/a:" + appJar, enableMultiRelease, jarVersion);
        output.shouldContain("Loading classes to share: done.");
        output.shouldHaveExitValue(0);

        output = TestCommon.exec(appJar, "-Xbootclasspath/a:" + appJar, mainClass);
        checkExecOutput(output, "I am running on version 7");

        // 6. Sanity test case-insensitive "Multi-Release" attribute name
        output = TestCommon.dump(appJar2, appClasses);
        output.shouldContain("Loading classes to share: done.");
        output.shouldHaveExitValue(0);

        output = TestCommon.exec(appJar2, mainClass);
        checkExecOutput(output, "I am running on version " + MAJOR_VERSION_STRING);
    }
}
