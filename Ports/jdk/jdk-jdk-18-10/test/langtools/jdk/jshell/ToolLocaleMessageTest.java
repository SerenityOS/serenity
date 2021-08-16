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
 * @bug 8147515
 * @summary Tests for output customization
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.internal.jshell.tool
 * @build KullaTesting TestingInputStream toolbox.ToolBox Compiler
 * @run testng ToolLocaleMessageTest
 * @key intermittent
 */

import java.util.Locale;
import org.testng.annotations.Test;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test
public class ToolLocaleMessageTest extends ReplToolTesting {

    void testLocale(ReplTest... tests) {
        test(Locale.getDefault(), false, new String[]{"--no-startup"}, "", tests);
    }

    void assertCommandOK(boolean after, String cmd, String... contains) {
        assertCommandCheckOutput(after, cmd, s -> {
            assertFalse(s.contains("Exception"), "Output of '" + cmd + "' has Exception: " + s);
            assertFalse(s.contains("ERROR:"), "Output of '" + cmd + "' has error: " + s);
            for (String m : contains) {
                assertTrue(s.contains(m), "Expected to find '" + m + "' in output of '" + cmd + "' -- output: " + s);
            }
        });
    }

    void assertCommandFail(boolean after, String cmd, String... contains) {
        assertCommandCheckOutput(after, cmd, s -> {
            assertFalse(s.contains("Exception"), "Output of '" + cmd + "' has Exception: " + s);
            assertTrue(s.contains("ERROR:"), "Expected to find error in output of '" + cmd + "' has error: " + s);
            for (String m : contains) {
                assertTrue(s.contains(m), "Expected to find '" + m + "' in output of '" + cmd + "' -- output: " + s);
            }
        });
    }

    public void testTerminate() {
        testLocale(
                (a) -> assertCommandOK(a, "System.exit(1)", "/reload")
        );
    }

    public void testSample() {
        try {
            testLocale(
                    (a) -> assertCommandOK(a, "/set mode test -command normal", "test"),
                    (a) -> assertCommandOK(a, "/set format test errorpre 'ERROR: '"),
                    (a) -> assertCommandOK(a, "/set feedback test", "test"),

                    (a) -> assertCommandFail(a, "/turkey", "/turkey"),
                    (a) -> assertCommandFail(a, "/s", "/set"),
                    (a) -> assertCommandOK(a, "void m() { blah(); }", "blah"),
                    (a) -> assertCommandOK(a, "void m() {}"),
                    (a) -> assertCommandOK(a, "class C {}"),
                    (a) -> assertCommandOK(a, "47"),
                    (a) -> assertCommandOK(a, "double d"),
                    (a) -> assertCommandOK(a, "/drop m", "m"),
                    (a) -> assertCommandOK(a, "void dup() {}"),
                    (a) -> assertCommandOK(a, "int dup"),

                    (a) -> assertCommandOK(a, "/set feedback normal", "normal")
            );
        } finally {
            assertCommandOK(false, "/set feedback normal");
        }
    }

    public void testCommand() {
        try {
            testLocale(
                    (a) -> assertCommandOK(a, "/set mode test -command normal", "test"),
                    (a) -> assertCommandOK(a, "/set format test errorpre 'ERROR: '"),
                    (a) -> assertCommandOK(a, "/set feedback test", "test"),

                    (a) -> assertCommandFail(a, "/list zebra"),
                    (a) -> assertCommandFail(a, "/set editor -rot", "/set editor -rot"),
                    (a) -> assertCommandFail(a, "/set snowball", "/set", "snowball"),
                    (a) -> assertCommandOK(a, "/set", "|  /set feedback test", "verbose"),
                    (a) -> assertCommandFail(a, "/set f", "/set"),
                    (a) -> assertCommandOK(a, "/set fe", "|  /set feedback test"),
                    (a) -> assertCommandFail(a, "/classpath", "/classpath"),
                    (a) -> assertCommandFail(a, "/help rabbits", "rabbits"),
                    (a) -> assertCommandFail(a, "/drop"),
                    (a) -> assertCommandFail(a, "/drop rats"),
                    (a) -> assertCommandOK(a, "void dup() {}"),
                    (a) -> assertCommandOK(a, "int dup"),
                    (a) -> assertCommandFail(a, "/edit zebra", "zebra"),
                    (a) -> assertCommandFail(a, "/list zebra", "zebra", "No such snippet: zebra"),
                    (a) -> assertCommandFail(a, "/open", "/open"),
                    (a) -> assertCommandFail(a, "/open zebra", "zebra", "/open"),
                    (a) -> assertCommandFail(a, "/reload zebra", "zebra", "Unexpected arguments at end of command"),
                    (a) -> assertCommandFail(a, "/save", "/save"),
                    (a) -> assertCommandFail(a, "/-99"),

                    (a) -> assertCommandOK(a, "/set feedback normal", "normal")
            );
        } finally {
            assertCommandOK(false, "/set feedback normal");
        }
    }

    public void testHelp() {
        testLocale(
                (a) -> assertCommandOK(a, "/help", "/list", "/save", "/set", "[-restore]"),
                (a) -> assertCommandOK(a, "/help /list", "-start", "-all"),
                (a) -> assertCommandOK(a, "/help /edit", "/set editor"),
                (a) -> assertCommandOK(a, "/help /drop", "/drop"),
                (a) -> assertCommandOK(a, "/help /save", "-all", "-start"),
                (a) -> assertCommandOK(a, "/help /open", "/open"),
                (a) -> assertCommandOK(a, "/help /reload", "-restore"),
                (a) -> assertCommandOK(a, "/help /help", "intro"),
                (a) -> assertCommandOK(a, "/help /set", "mode"),
                (a) -> assertCommandOK(a, "/help /?", "intro"),
                (a) -> assertCommandOK(a, "/help intro", "/help"),
                (a) -> assertCommandOK(a, "/help /set format", "import", "case", "{value}", "added"),
                (a) -> assertCommandOK(a, "/help /set feedback", "mode"),
                (a) -> assertCommandOK(a, "/help /set mode", "feedback"),
                (a) -> assertCommandOK(a, "/help /set prompt", "/set prompt"),
                (a) -> assertCommandOK(a, "/help /set editor", "/edit")
        );
    }

    public void testFeedbackError() {
        try {
            testLocale(
                    (a) -> assertCommandOK(a, "/set mode te2", "te2"),
                    (a) -> assertCommandOK(a, "/set mode te3 -command", "te3"),
                    (a) -> assertCommandOK(a, "/set mode te2 -command"),
                    (a) -> assertCommandOK(a, "/set mode te -command normal", "te"),
                    (a) -> assertCommandOK(a, "/set mode te"),
                    (a) -> assertCommandOK(a, "/set format te errorpre 'ERROR: '"),
                    (a) -> assertCommandOK(a, "/set feedback te"),

                    (a) -> assertCommandFail(a, "/set xyz", "xyz"),
                    (a) -> assertCommandFail(a, "/set f", "/set", "f"),
                    (a) -> assertCommandFail(a, "/set feedback xyz"),
                    (a) -> assertCommandFail(a, "/set format xyz"),
                    (a) -> assertCommandFail(a, "/set format t"),
                    (a) -> assertCommandFail(a, "/set format te fld"),
                    (a) -> assertCommandFail(a, "/set format te fld aaa", "aaa"),
                    (a) -> assertCommandFail(a, "/set format te fld 'aaa' frog"),
                    (a) -> assertCommandFail(a, "/set format te fld 'aaa' import-frog"),
                    (a) -> assertCommandFail(a, "/set format te fld 'aaa' import-import"),
                    (a) -> assertCommandFail(a, "/set format te fld 'aaa' import,added"),
                    (a) -> assertCommandFail(a, "/set mode x xyz"),
                    (a) -> assertCommandFail(a, "/set mode x -quiet y"),
                    (a) -> assertCommandFail(a, "/set mode tee -command foo", "foo"),
                    (a) -> assertCommandFail(a, "/set prompt te aaa xyz", "aaa"),
                    (a) -> assertCommandFail(a, "/set prompt te 'aaa' xyz", "xyz"),
                    (a) -> assertCommandFail(a, "/set prompt te aaa"),
                    (a) -> assertCommandFail(a, "/set prompt te 'aaa'"),

                    (a) -> assertCommandOK(a, "/set feedback normal")
            );
        } finally {
            assertCommandOK(false, "/set feedback normal");
        }
    }


}
