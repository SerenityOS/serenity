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

/*
 * @test
 * @bug 8189782 8210555
 * @summary Test for isSupportedOption
 * @modules java.compiler
 *          jdk.compiler
 * @run main IsSupportedOptionTest
 */

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Tests for JavaCompiler.isSupportedOption method.
 */
public class IsSupportedOptionTest {
    public static void main(String... args) throws Exception {
        new IsSupportedOptionTest().run();
    }

    public void run() throws Exception {
        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        check(tool, "-source", 1);
        check(tool, "--source", 1);
        check(tool, "-target", 1);
        check(tool, "--target", 1);
        check(tool, "--add-modules", 1);
        check(tool, "-verbose", 0);
        check(tool, "-proc:none", 0);
        check(tool, "-Xlint", 0);
        check(tool, "-Xlint:unchecked", 0);
        check(tool, "-Xdoclint", 0);
        check(tool, "-Xdoclint:stats", 0);
        check(tool, "-Xdoclint/package:foo", 0);
        check(tool, "--debug=any", 1);
        check(tool, "-g", 0);
        check(tool, "-g:vars", 0);
        check(tool, "-g:none", 0);
        check(tool, "-ZZZ", -1);
        check(tool, "-Afoobar", 0);

        try {
            check(tool, null, -1);
            throw new AssertionError("null was accepted without exception");
        } catch (NullPointerException e) {
        }
    }

    private void check(JavaCompiler tool, String option, int numArgs) {
        System.err.println("check " + option);
        int n = tool.isSupportedOption(option);
        if (n != numArgs) {
            throw new AssertionError("unexpected result for option: " + option + ": " + n);
        }
    }
}

