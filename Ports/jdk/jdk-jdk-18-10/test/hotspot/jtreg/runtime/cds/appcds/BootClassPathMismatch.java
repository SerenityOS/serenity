/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary bootclasspath mismatch test.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @compile test-classes/C2.java
 * @run driver BootClassPathMismatch
 */

import jdk.test.lib.Platform;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.helpers.ClassFileInstaller;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;


public class BootClassPathMismatch {
    private static final String mismatchMessage = "shared class paths mismatch";

    public static void main(String[] args) throws Exception {
        JarBuilder.getOrCreateHelloJar();
        copyHelloToNewDir();

        BootClassPathMismatch test = new BootClassPathMismatch();
        test.testBootClassPathMismatch();
        test.testBootClassPathMismatchWithAppClass();
        test.testBootClassPathMismatchWithBadPath();
        if (!TestCommon.isDynamicArchive()) {
            // this test is not applicable to dynamic archive since
            // there is no class to be archived in the top archive
            test.testBootClassPathMatchWithAppend();
        }
        test.testBootClassPathMatch();
        test.testBootClassPathMismatchTwoJars();
    }

    /* Archive contains boot classes only, with Hello class on -Xbootclasspath/a path.
     *
     * Error should be detected if:
     * dump time: -Xbootclasspath/a:${testdir}/hello.jar
     * run-time : -Xbootclasspath/a:${testdir}/newdir/hello.jar
     *
     * or
     * dump time: -Xbootclasspath/a:${testdir}/newdir/hello.jar
     * run-time : -Xbootclasspath/a:${testdir}/hello.jar
     */
    public void testBootClassPathMismatch() throws Exception {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String appClasses[] = {"Hello"};
        String testDir = TestCommon.getTestDir("newdir");
        String otherJar = testDir + File.separator + "hello.jar";

        TestCommon.dump(appJar, appClasses, "-Xbootclasspath/a:" + appJar);
        TestCommon.run(
                "-Xlog:cds",
                "-cp", appJar, "-Xbootclasspath/a:" + otherJar, "Hello")
            .assertAbnormalExit(mismatchMessage);

        TestCommon.dump(appJar, appClasses, "-Xbootclasspath/a:" + otherJar);
        TestCommon.run(
                "-Xlog:cds",
                "-cp", appJar, "-Xbootclasspath/a:" + appJar, "Hello")
            .assertAbnormalExit(mismatchMessage);
    }

    /* Archive contains boot classes only.
     *
     * Error should be detected if:
     * dump time: -Xbootclasspath/a:${testdir}/newdir/hello.jar
     * run-time : -Xbootclasspath/a:${testdir}/newdir/hello.jar1
     */
    public void testBootClassPathMismatchWithBadPath() throws Exception {
        String appClasses[] = {"Hello"};
        String testDir = TestCommon.getTestDir("newdir");
        String appJar = testDir + File.separator + "hello.jar";
        String otherJar = testDir + File.separator + "hello.jar1";

        TestCommon.dump(appJar, appClasses, "-Xbootclasspath/a:" + appJar);
        TestCommon.run(
                "-Xlog:cds",
                "-cp", appJar, "-Xbootclasspath/a:" + otherJar, "Hello")
            .assertAbnormalExit(mismatchMessage);
    }

    /* Archive contains boot classes only, with Hello loaded from -Xbootclasspath/a at dump time.
     *
     * No error if:
     * dump time: -Xbootclasspath/a:${testdir}/hello.jar
     * run-time : -Xbootclasspath/a:${testdir}/hello.jar
     */
    public void testBootClassPathMatch() throws Exception {
        String appJar = TestCommon.getTestJar("hello.jar");
        String appClasses[] = {"Hello"};
        TestCommon.dump(
            appJar, appClasses, "-Xbootclasspath/a:" + appJar);
        TestCommon.run(
                "-cp", appJar, "-verbose:class",
                "-Xbootclasspath/a:" + appJar, "Hello")
            .assertNormalExit("[class,load] Hello source: shared objects file");

        // test relative path to appJar
        String newJar = TestCommon.composeRelPath(appJar);
        TestCommon.run(
                "-cp", newJar, "-verbose:class",
                "-Xbootclasspath/a:" + newJar, "Hello")
            .assertNormalExit("[class,load] Hello source: shared objects file");

        int idx = appJar.lastIndexOf(File.separator);
        String jarName = appJar.substring(idx + 1);
        String jarDir = appJar.substring(0, idx);
        // relative path starting with "."
        TestCommon.runWithRelativePath(
            jarDir,
            "-Xshare:on",
            "-XX:SharedArchiveFile=" + TestCommon.getCurrentArchiveName(),
            "-cp", "." + File.separator + jarName,
            "-Xbootclasspath/a:" + "." + File.separator + jarName,
            "-Xlog:class+load=trace,class+path=info",
            "Hello")
            .assertNormalExit(output -> {
                output.shouldContain("Hello source: shared objects file")
                      .shouldHaveExitValue(0);
                });

        // relative path starting with ".."
        idx = jarDir.lastIndexOf(File.separator);
        String jarSubDir = jarDir.substring(idx + 1);
        TestCommon.runWithRelativePath(
            jarDir,
            "-Xshare:on",
            "-XX:SharedArchiveFile=" + TestCommon.getCurrentArchiveName(),
            "-cp", ".." + File.separator + jarSubDir + File.separator + jarName,
            "-Xbootclasspath/a:" + ".." + File.separator + jarSubDir + File.separator + jarName,
            "-Xlog:class+load=trace,class+path=info",
            "Hello")
            .assertNormalExit(output -> {
                output.shouldContain("Hello source: shared objects file")
                      .shouldHaveExitValue(0);
                });

        // test sym link to appJar
        if (!Platform.isWindows()) {
            File linkedJar = TestCommon.createSymLink(appJar);
            TestCommon.run(
                    "-cp", linkedJar.getPath(), "-verbose:class",
                    "-Xbootclasspath/a:" + linkedJar.getPath(), "Hello")
                .assertNormalExit("[class,load] Hello source: shared objects file");
        }
    }

    /* Archive contains boot classes only, runtime add -Xbootclasspath/a path.
     *
     * No error:
     * dump time: No -Xbootclasspath/a
     * run-time : -Xbootclasspath/a:${testdir}/hello.jar
     */
    public void testBootClassPathMatchWithAppend() throws Exception {
      CDSOptions opts = new CDSOptions().setUseVersion(false);
      CDSTestUtils.createArchiveAndCheck(opts);

      String appJar = JarBuilder.getOrCreateHelloJar();
      opts.addPrefix("-Xbootclasspath/a:" + appJar, "-showversion").addSuffix("Hello");
      CDSTestUtils.runWithArchiveAndCheck(opts);
    }

    /* Archive contains app classes, with Hello on -cp path at dump time.
     *
     * Error should be detected if:
     * dump time: <no bootclasspath specified>
     * run-time : -Xbootclasspath/a:${testdir}/hello.jar
     */
    public void testBootClassPathMismatchWithAppClass() throws Exception {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String appClasses[] = {"Hello"};
        TestCommon.dump(appJar, appClasses);
        TestCommon.run(
                "-Xlog:cds",
                "-cp", appJar, "-Xbootclasspath/a:" + appJar, "Hello")
            .assertAbnormalExit(mismatchMessage);

        // test relative path to appJar
        String newJar = TestCommon.composeRelPath(appJar);
        TestCommon.run(
                "-cp", newJar, "-Xbootclasspath/a:" + newJar, "Hello")
            .assertAbnormalExit(mismatchMessage);
    }

    /* Archive contains app classes, with 2 jars in bootclasspath at dump time.
     *
     * Error should be detected if:
     * dump time: -Xbootclasspath/a:hello.jar:jar2.jar
     * run-time : -Xbootclasspath/a:hello.jar:jar2.jarx
     * Note: the second jar (jar2.jarx) specified for run-time doesn't exist.
     */
    public void testBootClassPathMismatchTwoJars() throws Exception {
        String appJar = JarBuilder.getOrCreateHelloJar();
        String jar2 = ClassFileInstaller.writeJar("jar2.jar", "pkg/C2");
        String jars = appJar + File.pathSeparator + jar2;
        String appClasses[] = {"Hello", "pkg/C2"};
        TestCommon.dump(
            appJar, appClasses, "-Xbootclasspath/a:" + jars);
        TestCommon.run(
                "-cp", appJar, "-Xbootclasspath/a:" + jars + "x", "Hello")
            .assertAbnormalExit(mismatchMessage);
    }

    private static void copyHelloToNewDir() throws Exception {
        String classDir = CDSTestUtils.getOutputDir();
        String dstDir = classDir + File.separator + "newdir";
        try {
            Files.createDirectory(Paths.get(dstDir));
        } catch (FileAlreadyExistsException e) { }

        // copy as hello.jar
        Path dstPath = Paths.get(dstDir, "hello.jar");
        Files.copy(Paths.get(classDir, "hello.jar"),
            dstPath,
            StandardCopyOption.REPLACE_EXISTING);

        File helloJar = dstPath.toFile();
        long modTime = helloJar.lastModified();

        // copy as hello.jar1
        Path dstPath2 = Paths.get(dstDir, "hello.jar1");
        Files.copy(Paths.get(classDir, "hello.jar"),
            dstPath2,
            StandardCopyOption.REPLACE_EXISTING);

        // On Windows, we rely on the file size, creation time, and
        // modification time in order to differentiate between 2 files.
        // Setting a different modification time on hello.jar1 so that this test
        // runs more reliably on Windows.
        modTime += 10000;
        Files.setAttribute(dstPath2, "lastModifiedTime", FileTime.fromMillis(modTime));
    }
}
