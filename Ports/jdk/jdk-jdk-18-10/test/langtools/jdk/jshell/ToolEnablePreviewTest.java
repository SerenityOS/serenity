/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8199193
 * @summary Tests for the --enable-preview option
 * @run testng ToolEnablePreviewTest
 */

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class ToolEnablePreviewTest extends ReplToolTesting {

    @Test
    public void testOptionDebug() {
        String release = System.getProperty("java.specification.version");
        test(
                (a) -> assertCommand(a, "/debug b",
                        "RemoteVM Options: []\n"
                        + "Compiler options: []"),
                (a) -> assertCommand(a, "/env --enable-preview",
                        "|  Setting new options and restoring state."),
                (a) -> assertCommandCheckOutput(a, "/debug b", s -> {
                    assertTrue(s.contains("RemoteVM Options: [--enable-preview]"));
                    assertTrue(s.contains("Compiler options: [-source, " + release + ", --enable-preview]")
                            || s.contains("Compiler options: [--enable-preview, -source, " + release + "]"),
                            "\nExpected -- " + "Compiler options: [-source, " + release + ", --enable-preview]"
                            + "\nOr -- " + "Compiler options: [--enable-preview, -source, " + release + "]"
                            + "\nBut got -- " + s);
                })
        );
    }

    @Test
    public void testCommandLineFlag() {
        String release = System.getProperty("java.specification.version");
        test(new String[] {"--enable-preview"},
                (a) -> assertCommandCheckOutput(a, "/debug b", s -> {
                    assertTrue(s.contains("RemoteVM Options: [--enable-preview]"));
                    assertTrue(s.contains("Compiler options: [-source, " + release + ", --enable-preview]")
                            || s.contains("Compiler options: [--enable-preview, -source, " + release + "]"),
                            "\nExpected -- " + "Compiler options: [-source, " + release + ", --enable-preview]"
                            + "\nOr -- " + "Compiler options: [--enable-preview, -source, " + release + "]"
                            + "\nBut got -- " + s);
                })
        );
    }

    @Test
    public void testCompilerTestFlagEnv() {
        test(new String[] {"-C", "-XDforcePreview"},
                (a) -> assertCommandOutputContains(a, "Function<Integer,Integer> f = i -> i + i",
                        "Error", "preview feature"),
                (a) -> assertCommand(a, "/env --enable-preview",
                        "|  Setting new options and restoring state."),
                (a) -> assertCommandOutputContains(a, "Function<Integer,Integer> f = i -> i + i",
                        "f ==> ")
        );
    }

    @Test
    public void testCompilerTestFlag() {
        test(new String[] {"-C", "-XDforcePreview", "--enable-preview"},
                (a) -> assertCommandOutputContains(a, "Function<Integer,Integer> f = i -> i + i",
                        "f ==> "),
                (a) -> assertCommandOutputContains(a, "f.apply(2)", "==> 4")
        );
    }

}
