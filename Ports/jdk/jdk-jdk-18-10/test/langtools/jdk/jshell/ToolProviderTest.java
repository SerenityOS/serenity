/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ServiceLoader;
import javax.tools.Tool;
import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;

/*
 * @test
 * @bug 8170044 8171343 8179856 8185840 8190383
 * @summary Test ServiceLoader launching of jshell tool
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.internal.jshell.tool
 * @library /tools/lib
 * @build Compiler toolbox.ToolBox
 * @run testng ToolProviderTest
 */
@Test
public class ToolProviderTest extends StartOptionTest {

    // Through the provider, the console and console go to command out (we assume,
    // because it works with the current tests) that console and user output are
    // after command out.
    @Override
    protected void startExCoUoCeCn(int expectedExitCode,
            String expectedCmdOutput,
            String expectedUserOutput,
            String expectedError,
            String expectedConsole,
            String... args) {
        super.startExCoUoCeCn(expectedExitCode,
                (expectedCmdOutput  == null? "" : expectedCmdOutput) +
                (expectedConsole    == null? "" : expectedConsole) +
                (expectedUserOutput == null? "" : expectedUserOutput),
                null, expectedError, null, args);
    }

    @Override
    protected int runShell(String... args) {
        ServiceLoader<Tool> sl = ServiceLoader.load(Tool.class);
        for (Tool provider : sl) {
            if (provider.name().equals("jshell")) {
                return provider.run(cmdInStream, cmdout, cmderr, args);
            }
        }
        throw new AssertionError("Repl tool not found by ServiceLoader: " + sl);
    }

    // Test --show-version
    @Override
    public void testShowVersion() {
        startCo(s -> {
            assertTrue(s.startsWith("jshell "), "unexpected version: " + s);
            assertTrue(s.contains("Welcome"), "Expected start (but got no welcome): " + s);
            assertTrue(s.trim().contains("jshell>"), "Expected prompt, got: " + s);
        },
                "--show-version");
    }
}
