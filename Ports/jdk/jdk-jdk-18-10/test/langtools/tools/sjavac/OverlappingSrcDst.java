/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Make sure sjavac doesn't allow overlapping source and destination
 *          directories.
 * @bug 8061320
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac
 * @build Wrapper toolbox.ToolBox
 * @run main Wrapper OverlappingSrcDst
 */

import java.io.File;
import java.nio.file.Paths;

import toolbox.ToolBox;

public class OverlappingSrcDst extends SJavacTester {
    public static void main(String... args) {
        new OverlappingSrcDst().run();
    }

    public void run() {
        String abs = ToolBox.currDir.toAbsolutePath().toString();

        // Relative vs relative
        test("dir", "dir", false);
        test("dir", "dir/dst", false);
        test("dir/src", "dir", false);
        test("src", "dst", true);

        // Absolute vs absolute
        test(abs + "/dir", abs + "/dir", false);
        test(abs + "/dir", abs + "/dir/dst", false);
        test(abs + "/dir/src", abs + "/dir", false);
        test(abs + "/src", abs + "/dst", true);

        // Absolute vs relative
        test(abs + "/dir", "dir", false);
        test(abs + "/dir", "dir/dst", false);
        test(abs + "/dir/src", "dir", false);
        test(abs + "/src", "dst", true);

        // Relative vs absolute
        test("dir", abs + "/dir", false);
        test("dir", abs + "/dir/dst", false);
        test("dir/src", abs + "/dir", false);
        test("src", abs + "/dst", true);
    }

    private void test(String srcDir, String dstDir, boolean shouldSucceed) {
        boolean succeeded = testCompilation(srcDir, dstDir);
        if (shouldSucceed != succeeded) {
            throw new AssertionError(
                    String.format("Test failed for "
                                          + "srcDir=\"%s\", "
                                          + "dstDir=\"%s\". "
                                          + "Compilation was expected to %s but %s.",
                                  srcDir,
                                  dstDir,
                                  shouldSucceed ? "succeed" : "fail",
                                  succeeded ? "succeeded" : "failed"));
        }
    }

    private boolean testCompilation(String srcDir, String dstDir) {
        try {
            srcDir = srcDir.replace('/', File.separatorChar);
            dstDir = dstDir.replace('/', File.separatorChar);
            tb.writeFile(Paths.get(srcDir, "pkg", "A.java"), "package pkg; class A {}");
            compile("--state-dir=state", "-src", srcDir, "-d", dstDir);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
