/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8166334 8188894
 * @summary test shift-tab shortcuts "fixes"
 * @modules
 *     jdk.jshell/jdk.internal.jshell.tool:open
 *     jdk.jshell/jdk.internal.jshell.tool.resources:open
 *     jdk.jshell/jdk.jshell:open
 * @build UITesting
 * @build ToolShiftTabTest
 * @run testng/timeout=300 ToolShiftTabTest
 */

import java.util.regex.Pattern;
import org.testng.annotations.Test;

@Test
public class ToolShiftTabTest extends UITesting {

    // Shift-tab as escape sequence
    private String FIX = "\033\133\132";

    public void testFixVariable() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("3+4");
            inputSink.write(FIX + "v");
            inputSink.write("jj\n");
            waitOutput(out, "jj ==> 7");
            inputSink.write("jj\n");
            waitOutput(out, "jj ==> 7");
        });
    }

    public void testFixMethod() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("5.5 >= 3.1415926535");
            inputSink.write(FIX + "m");
            waitOutput(out, "boolean ");
            inputSink.write("mm\n");
            waitOutput(out, "|  created method mm()");
            inputSink.write("mm()\n");
            waitOutput(out, "==> true");
            inputSink.write("/method\n");
            waitOutput(out, "boolean mm()");
        });
    }

    public void testFixMethodVoid() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("System.out.println(\"Testing\")");
            inputSink.write(FIX + "m");
            inputSink.write("p\n");
            waitOutput(out, "|  created method p()");
            inputSink.write("p()\n");
            waitOutput(out, "Testing");
            inputSink.write("/method\n");
            waitOutput(out, "void p()");
        });
    }

    public void testFixMethodNoLeaks() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("4");
            inputSink.write(FIX + "m");
            inputSink.write(INTERRUPT + " 55");
            inputSink.write(FIX + "m");
            inputSink.write(INTERRUPT + " 55");
            inputSink.write(FIX + "m");
            inputSink.write(INTERRUPT + " 55");
            inputSink.write(FIX + "m");
            inputSink.write(INTERRUPT + " 55");
            inputSink.write(FIX + "m");
            inputSink.write(INTERRUPT + "'X'");
            inputSink.write(FIX + "m");
            inputSink.write("nl\n");
            waitOutput(out, "|  created method nl()");
            inputSink.write("/list\n");
            waitOutput(out, Pattern.quote("1 : char nl() { return 'X'; }"));
            inputSink.write("true\n");
            waitOutput(out, Pattern.quote("$2 ==> true"));
            inputSink.write("/list\n");
            waitOutput(out, "2 : true");
        });
    }

    public void testFixImport() throws Exception {
        doRunTest((inputSink, out) -> {
            do {
                inputSink.write("Frame");
                inputSink.write(FIX + "i");
                inputSink.write("1");
                inputSink.write(".WIDTH\n");
            } while (!waitOutput(out, "==> 1", "Results may be incomplete"));
            inputSink.write("/import\n");
            waitOutput(out, "|    import java.awt.Frame");

            inputSink.write("Object");
            inputSink.write(FIX + "i");
            waitOutput(out, "The identifier is resolvable in this context");
        });
    }

    public void testFixBad() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("123");
            inputSink.write(FIX + "z");
            waitOutput(out, "Unexpected character after Shift\\+Tab");
        });
    }
}
