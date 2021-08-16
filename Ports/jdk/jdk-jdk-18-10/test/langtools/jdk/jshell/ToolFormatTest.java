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
 * @bug 8148316 8148317 8151755 8152246 8153551 8154812 8157261 8163840 8166637 8161969 8173007
 * @summary Tests for output customization
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.internal.jshell.tool
 * @build KullaTesting TestingInputStream toolbox.ToolBox Compiler
 * @run testng ToolFormatTest
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Test
public class ToolFormatTest extends ReplToolTesting {

    public void testSetFormat() {
        try {
            test(
                    (a) -> assertCommandOutputStartsWith(a, "/set mode test -command", "|  Created new feedback mode: test"),
                    (a) -> assertCommand(a, "/set format test pre '$ '", ""),
                    (a) -> assertCommand(a, "/set format test post ''", ""),
                    (a) -> assertCommand(a, "/set format test act 'ADD' added", ""),
                    (a) -> assertCommand(a, "/set format test act 'MOD' modified", ""),
                    (a) -> assertCommand(a, "/set format test act 'REP' replaced", ""),
                    (a) -> assertCommand(a, "/set format test act 'OVR' overwrote", ""),
                    (a) -> assertCommand(a, "/set format test act 'USE' used", ""),
                    (a) -> assertCommand(a, "/set format test act 'DRP' dropped", ""),
                    (a) -> assertCommand(a, "/set format test up 'UP-' update", ""),
                    (a) -> assertCommand(a, "/set format test action '{up}{act} '", ""),
                    (a) -> assertCommand(a, "/set format test resolve 'OK' ok", ""),
                    (a) -> assertCommand(a, "/set format test resolve 'DEF' defined", ""),
                    (a) -> assertCommand(a, "/set format test resolve 'NODEF' notdefined", ""),
                    (a) -> assertCommand(a, "/set format test fname ':{name} ' ", ""),
                    (a) -> assertCommand(a, "/set format test ftype '[{type}]' method,expression", ""),
                    (a) -> assertCommand(a, "/set format test result '={value} ' expression", ""),
                    (a) -> assertCommand(a, "/set format test display '{pre}{action}{ftype}{fname}{result}{resolve}'", ""),
                    (a) -> assertCommand(a, "/set format test display '{pre}HI this is enum' enum", ""),
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback test", "$ Feedback mode: test"),
                    (a) -> assertCommand(a, "class D {}", "$ ADD :D OK"),
                    (a) -> assertCommand(a, "void m() {}", "$ ADD []:m OK"),
                    (a) -> assertCommand(a, "interface EX extends EEX {}", "$ ADD :EX NODEF"),
                    (a) -> assertCommand(a, "56", "$ ADD [int]:$4 =56 OK"),
                    (a) -> assertCommand(a, "class D { int hh; }", "$ REP :D OK$ UP-OVR :D OK"),
                    (a) -> assertCommand(a, "enum E {A,B}", "$ HI this is enum"),
                    (a) -> assertCommand(a, "int z() { return f(); }", "$ ADD []:z DEF"),
                    (a) -> assertCommand(a, "z()", "$ UP-USE []:z DEF"),
                    (a) -> assertCommand(a, "/drop z", "$ DRP []:z OK"),
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback normal", "|  Feedback mode: normal")
            );
        } finally {
            assertCommandCheckOutput(false, "/set feedback normal", s -> {
            });
        }
    }

    public void testSetFormatOverride() {
        test(
                (a) -> assertCommand(a, "/set mode tm -c", "|  Created new feedback mode: tm"),
                (a) -> assertCommand(a, "/se fo tm x \"aaa\"", ""),
                (a) -> assertCommand(a, "/se fo tm x \"bbb\" class,method-added", ""),
                (a) -> assertCommand(a, "/se fo tm x",
                        "|  /set format tm x \"aaa\" \n" +
                        "|  /set format tm x \"bbb\" class,method-added"),
                (a) -> assertCommand(a, "/se fo tm x \"ccc\" class,method-added,modified", ""),
                (a) -> assertCommand(a, "/se fo tm x \"ddd\" class,method-added", ""),
                (a) -> assertCommand(a, "/se fo tm x \"eee\" method-added", ""),
                (a) -> assertCommand(a, "/se fo tm x",
                        "|  /set format tm x \"aaa\" \n" +
                        "|  /set format tm x \"ccc\" class,method-added,modified\n" +
                        "|  /set format tm x \"ddd\" class,method-added\n" +
                        "|  /set format tm x \"eee\" method-added"),
                (a) -> assertCommand(a, "/se fo tm x \"EEE\" method-added,replaced", ""),
                (a) -> assertCommand(a, "/se fo tm x",
                        "|  /set format tm x \"aaa\" \n" +
                        "|  /set format tm x \"ccc\" class,method-added,modified\n" +
                        "|  /set format tm x \"ddd\" class,method-added\n" +
                        "|  /set format tm x \"EEE\" method-added,replaced"),
                (a) -> assertCommand(a, "/se fo tm x \"fff\" method-added,replaced-ok", ""),
                (a) -> assertCommand(a, "/se fo tm x",
                        "|  /set format tm x \"aaa\" \n" +
                        "|  /set format tm x \"ccc\" class,method-added,modified\n" +
                        "|  /set format tm x \"ddd\" class,method-added\n" +
                        "|  /set format tm x \"EEE\" method-added,replaced\n" +
                        "|  /set format tm x \"fff\" method-added,replaced-ok"),
                (a) -> assertCommand(a, "/se fo tm x \"ggg\" method-ok", ""),
                (a) -> assertCommand(a, "/se fo tm x",
                        "|  /set format tm x \"aaa\" \n" +
                        "|  /set format tm x \"ccc\" class,method-added,modified\n" +
                        "|  /set format tm x \"ddd\" class,method-added\n" +
                        "|  /set format tm x \"EEE\" method-added,replaced\n" +
                        "|  /set format tm x \"ggg\" method-ok"),
                (a) -> assertCommand(a, "/se fo tm x \"hhh\" method", ""),
                (a) -> assertCommand(a, "/se fo tm x",
                        "|  /set format tm x \"aaa\" \n" +
                        "|  /set format tm x \"ccc\" class,method-added,modified\n" +
                        "|  /set format tm x \"ddd\" class,method-added\n" +
                        "|  /set format tm x \"hhh\" method"),
                (a) -> assertCommand(a, "/se fo tm x \"iii\" method,class", ""),
                (a) -> assertCommand(a, "/se fo tm x",
                        "|  /set format tm x \"aaa\" \n" +
                        "|  /set format tm x \"iii\" method,class"),
                (a) -> assertCommand(a, "/se fo tm x \"jjj\"", ""),
                (a) -> assertCommand(a, "/se fo tm x",
                        "|  /set format tm x \"jjj\"")
        );
    }

    public void testSetFormatSelectorSample() {
        test(
                (a) -> assertCommandOutputStartsWith(a, "/set mode ate -quiet",
                            "|  Created new feedback mode: ate"),
                (a) -> assertCommand(a, "/set feedback ate", ""),

                (a) -> assertCommand(a, "/set format ate display '---replaced,modified,added-primary---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++replaced,modified,added-primary+++' replaced,modified,added-primary", ""),
                (a) -> assertCommand(a, "\"replaced,modified,added-primary\"", "+++replaced,modified,added-primary+++"),

                (a) -> assertCommand(a, "/set format ate display '---added-primary,update---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++added-primary,update+++' added-primary,update", ""),
                (a) -> assertCommand(a, "\"added-primary,update\"", "+++added-primary,update+++"),


                (a) -> assertCommand(a, "/set format ate display '---method-replaced-primary---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++method-replaced-primary+++' method-replaced-primary", ""),
                (a) -> assertCommand(a, "\"method-replaced-primary\"", "---method-replaced-primary---"),

                (a) -> assertCommand(a, "/set format ate display '---method-replaced-update---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++method-replaced-update+++' method-replaced-update", ""),
                (a) -> assertCommand(a, "\"method-replaced-update\"", "---method-replaced-update---"),

                (a) -> assertCommand(a, "/set format ate display '---method-added-update---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++method-added-update+++' method-added-update", ""),
                (a) -> assertCommand(a, "\"method-added-update\"", "---method-added-update---"),

                (a) -> assertCommand(a, "/set format ate display '---method-added---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++method-added+++' method-added", ""),
                (a) -> assertCommand(a, "\"method-added\"", "---method-added---"),

                (a) -> assertCommand(a, "/set format ate display '---class-modified,added-primary,update---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++class-modified,added-primary,update+++' class-modified,added-primary,update", ""),
                (a) -> assertCommand(a, "\"class-modified,added-primary,update\"", "---class-modified,added-primary,update---"),

                (a) -> assertCommand(a, "/set format ate display '---class-modified,added-primary---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++class-modified,added-primary+++' class-modified,added-primary", ""),
                (a) -> assertCommand(a, "\"class-modified,added-primary\"", "---class-modified,added-primary---"),

                (a) -> assertCommand(a, "/set format ate display '---class-modified,added-update---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++class-modified,added-update+++' class-modified,added-update", ""),
                (a) -> assertCommand(a, "\"class-modified,added-update\"", "---class-modified,added-update---"),

                (a) -> assertCommand(a, "/set format ate display '---replaced,added---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++replaced,added+++' replaced,added", ""),
                (a) -> assertCommand(a, "\"replaced,added\"", "+++replaced,added+++"),

                (a) -> assertCommand(a, "/set format ate display '---replaced-primary,update---'", ""),
                (a) -> assertCommand(a, "/set format ate display '+++replaced-primary,update+++' replaced-primary,update", ""),
                (a) -> assertCommand(a, "\"replaced-primary,update\"", "---replaced-primary,update---"),

                (a) -> assertCommandOutputStartsWith(a, "/set feedback normal", "|  Feedback mode: normal")
        );
    }

    // This test is exhaustive and takes to long for routine testing -- disabled.
    // A sampling of these has been added (above: testSetFormatSelectorSample).
    // See 8173007
    // Save for possible future deep testing or debugging
    @Test(enabled = false)
    public void testSetFormatSelector() {
        List<ReplTest> tests = new ArrayList<>();
        tests.add((a) -> assertCommandOutputStartsWith(a, "/set mode ate -quiet",
                            "|  Created new feedback mode: ate"));
        tests.add((a) -> assertCommand(a, "/set feedback ate", ""));
        StringBuilder sb = new StringBuilder();
        class KindList {
            final String[] values;
            final int matchIndex;
            int current;
            boolean match;
            KindList(String[] values, int matchIndex) {
                this.values = values;
                this.matchIndex = matchIndex;
                this.current = 1 << values.length;
            }
            boolean next() {
                if (current <= 0) {
                    return false;
                }
                --current;
                return true;
            }
            boolean append(boolean ahead) {
                boolean any = false;
                match = false;
                for (int i = values.length - 1; i >= 0 ; --i) {
                    if ((current & (1 << i)) != 0) {
                        match |= i == matchIndex;
                        if (any) {
                            sb.append(",");
                        } else {
                            if (ahead) {
                                sb.append("-");
                            }
                        }
                        sb.append(values[i]);
                        any = true;
                    }
                }
                match |= !any;
                return ahead || any;
            }
        }
        KindList klcase = new KindList(new String[] {"class", "method", "expression", "vardecl"}, 2);
        while (klcase.next()) {
            KindList klact  = new KindList(new String[] {"added", "modified", "replaced"}, 0);
            while (klact.next()) {
                KindList klwhen = new KindList(new String[] {"update", "primary"}, 1);
                while (klwhen.next()) {
                    sb.setLength(0);
                    klwhen.append(
                        klact.append(
                            klcase.append(false)));
                    boolean match = klcase.match && klact.match && klwhen.match;
                    String select = sb.toString();
                    String yes = "+++" + select + "+++";
                    String no  = "---" + select + "---";
                    String expect = match? yes : no;
                    tests.add((a) -> assertCommand(a, "/set format ate display '" + no  + "'", ""));
                    tests.add((a) -> assertCommand(a, "/set format ate display '" + yes + "' " + select, ""));
                    tests.add((a) -> assertCommand(a, "\"" + select + "\"", expect));
             /**** for sample generation ****
             System.err.println("                (a) -> assertCommand(a, \"/set format ate display '" + no  + "'\", \"\"),");
             System.err.println("                (a) -> assertCommand(a, \"/set format ate display '" + yes + "' " + select + "\", \"\"),");
             System.err.println("                (a) -> assertCommand(a, \"\\\"" + select + "\\\"\", \"" + expect + "\"),\n");
             ****/
                }
            }
        }
        tests.add((a) -> assertCommandOutputStartsWith(a, "/set feedback normal", "|  Feedback mode: normal"));

        try {
            test(tests.toArray(new ReplTest[tests.size()]));
        } finally {
            assertCommandCheckOutput(false, "/set feedback normal", s -> {
            });
        }
    }

    public void testSetTruncation() {
        try {
            test(
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback normal", ""),
                    (a) -> assertCommand(a, "String s = java.util.stream.IntStream.range(65, 74)"+
                            ".mapToObj(i -> \"\"+(char)i).reduce((a,b) -> a + b + a).get() + \"XYZ\"",
                            "s ==> \"ABACABADABACABAEABACABADABACABAFABACABADABACABAE ... BACABAEABACABADABACABAXYZ\""),
                    (a) -> assertCommandOutputStartsWith(a, "/set mode test -quiet", ""),
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback test", ""),
                    (a) -> assertCommand(a, "/set format test display '{type}:{value}' primary", ""),
                    (a) -> assertCommand(a, "/set truncation test 20", ""),
                    (a) -> assertCommand(a, "/set truncation test", "|  /set truncation test 20"),
                    (a) -> assertCommandOutputContains(a, "/set truncation", "/set truncation test 20"),
                    (a) -> assertCommand(a, "/set trunc test 10 varvalue", ""),
                    (a) -> assertCommand(a, "/set trunc test 3 assignment", ""),
                    (a) -> assertCommandOutputContains(a, "/set truncation",
                            "/set truncation test 10 varvalue"),
                    (a) -> assertCommandOutputContains(a, "/set truncation test",
                            "/set truncation test 10 varvalue"),
                    (a) -> assertCommand(a, "/var", "|    String s = \"ABACABADA"),
                    (a) -> assertCommand(a, "String r = s", "String:\"ABACABAD ... BAXYZ\""),
                    (a) -> assertCommand(a, "r", "String:\"ABACABADA"),
                    (a) -> assertCommand(a, "r=s", "String:\"AB")
            );
        } finally {
            assertCommandCheckOutput(false, "/set feedback normal", s -> {
            });
        }
    }

    public void testDefaultTruncation() {
        test(
                    (a) -> assertCommand(a, "char[] cs = new char[2000];", null),
                    (a) -> assertCommand(a, "Arrays.fill(cs, 'A');", ""),
                    (a) -> assertCommandCheckOutput(a, "String s = new String(cs)",
                            (s) -> {
                                assertTrue(s.length() < 120, "Result too long (" + s.length() + ") -- " + s);
                                assertTrue(s.contains("AAAAAAAAAAAAAAAAAA"), "Bad value: " + s);
                            }),
                    (a) -> assertCommandCheckOutput(a, "s",
                            (s) -> {
                                assertTrue(s.length() > 300, "Result too short (" + s.length() + ") -- " + s);
                                assertTrue(s.contains("AAAAAAAAAAAAAAAAAA"), "Bad value: " + s);
                            }),
                    (a) -> assertCommandCheckOutput(a, "\"X\" + s",
                            (s) -> {
                                assertTrue(s.length() > 300, "Result too short (" + s.length() + ") -- " + s);
                                assertTrue(s.contains("XAAAAAAAAAAAAAAAAAA"), "Bad value: " + s);
                            })
        );
    }

    public void testPrompt() {
        test(
                (a) -> assertCommand(a, "/set mode tp -quiet", "|  Created new feedback mode: tp"),
                (a) -> assertCommand(a, "/set prompt tp 'aaa' 'bbb'", ""),
                (a) -> assertCommand(a, "/set prompt tp",
                        "|  /set prompt tp \"aaa\" \"bbb\""),
                (a) -> assertCommandOutputContains(a, "/set prompt",
                        "|  /set prompt tp \"aaa\" \"bbb\""),
                (a) -> assertCommand(a, "/set mode -retain tp", ""),
                (a) -> assertCommand(a, "/set prompt tp 'ccc' 'ddd'", ""),
                (a) -> assertCommand(a, "/set prompt tp",
                        "|  /set prompt tp \"ccc\" \"ddd\""),
                (a) -> assertCommandCheckOutput(a, "/set mode tp",
                        (s) -> {
                            try {
                                BufferedReader rdr = new BufferedReader(new StringReader(s));
                                assertEquals(rdr.readLine(), "|  /set mode tp -quiet",
                                        "|  /set mode tp -quiet");
                                assertEquals(rdr.readLine(), "|  /set prompt tp \"aaa\" \"bbb\"",
                                        "|  /set prompt tp \"aaa\" \"bbb\"");
                                String l = rdr.readLine();
                                while (l.startsWith("|  /set format tp ")) {
                                    l = rdr.readLine();
                                }
                                assertEquals(l, "|  /set mode -retain tp",
                                        "|  /set mode -retain tp");
                                assertEquals(rdr.readLine(), "|  ",
                                        "|  ");
                                assertEquals(rdr.readLine(), "|  /set mode tp -quiet",
                                        "|  /set mode tp -quiet");
                                assertEquals(rdr.readLine(), "|  /set prompt tp \"ccc\" \"ddd\"",
                                        "|  /set prompt tp \"ccc\" \"ddd\"");
                            } catch (IOException ex) {
                                fail("threw " + ex);
                            }
                        })
        );
    }

    public void testShowFeedbackModes() {
        test(
                (a) -> assertCommandOutputContains(a, "/set feedback", "normal")
        );
    }

    public void testSetNewModeQuiet() {
        try {
            test(
                    (a) -> assertCommandOutputStartsWith(a, "/set mode nmq -quiet normal", "|  Created new feedback mode: nmq"),
                    (a) -> assertCommand(a, "/set feedback nmq", ""),
                    (a) -> assertCommand(a, "/se mo nmq2 -q nor", ""),
                    (a) -> assertCommand(a, "/se fee nmq2", ""),
                    (a) -> assertCommand(a, "/set mode nmc -command normal", ""),
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback nmc", "|  Feedback mode: nmc"),
                    (a) -> assertCommandOutputStartsWith(a, "/set mode nm -command",
                            "|  Created new feedback mode: nm"),
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback nm", "|  Feedback mode: nm"),
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback normal", "|  Feedback mode: normal")
            );
        } finally {
            assertCommandCheckOutput(false, "/set feedback normal", s -> {
            });
        }
    }

    public void testSetError() {
        try {
            test(
                    (a) -> assertCommandOutputStartsWith(a, "/set mode tee -command foo",
                            "|  Does not match any current feedback mode: foo"),
                    (a) -> assertCommandOutputStartsWith(a, "/set mode tee -quiet flurb",
                            "|  Does not match any current feedback mode: flurb"),
                    (a) -> assertCommandOutputStartsWith(a, "/set mode -command tee",
                            "|  Created new feedback mode: tee"),
                    (a) -> assertCommandOutputStartsWith(a, "/set mode verbose -command",
                            "|  Mode to be created already exists: verbose"),
                    (a) -> assertCommandOutputStartsWith(a, "/set mode te -command normal",
                            "|  Created new feedback mode: te"),
                    (a) -> assertCommand(a, "/set format te errorpre 'ERROR: '", ""),
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback te",
                            ""),
                    (a) -> assertCommandOutputStartsWith(a, "/set xyz",
                            "ERROR: Invalid '/set' argument: xyz"),
                    (a) -> assertCommandOutputStartsWith(a, "/set f",
                            "ERROR: Ambiguous sub-command argument to '/set': f"),
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback",
                            "|  /set feedback te"),
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback xyz",
                            "ERROR: Does not match any current feedback mode"),
                    (a) -> assertCommandOutputStartsWith(a, "/set feed",
                            "|  /set feedback te"),
                    (a) -> assertCommandOutputStartsWith(a, "/set format xyz",
                            "ERROR: Does not match any current feedback mode"),
                    (a) -> assertCommandOutputStartsWith(a, "/set format t",
                            "ERROR: Matches more then one current feedback mode: t"),
                    (a) -> assertCommandOutputStartsWith(a, "/set format qqq",
                            "ERROR: Does not match any current feedback mode: qqq"),
                    (a) -> assertCommandOutputStartsWith(a, "/set format te fld",
                            "ERROR: Expected a field name:"),
                    (a) -> assertCommandOutputStartsWith(a, "/set format te fld aaa",
                            "ERROR: Format 'aaa' must be quoted"),
                    (a) -> assertCommandOutputStartsWith(a, "/set format te fld 'aaa' frog",
                            "ERROR: Not a valid selector"),
                    (a) -> assertCommandOutputStartsWith(a, "/set format te fld 'aaa' import-frog",
                            "ERROR: Not a valid selector"),
                    (a) -> assertCommandOutputStartsWith(a, "/set format te fld 'aaa' import-import",
                            "ERROR: Selector kind in multiple sections of"),
                    (a) -> assertCommandOutputStartsWith(a, "/set format te fld 'aaa' import,added",
                            "ERROR: Different selector kinds in same sections of"),
                    (a) -> assertCommandOutputStartsWith(a, "/set trunc te 20x",
                            "ERROR: Truncation length must be an integer: 20x"),
                    (a) -> assertCommandOutputStartsWith(a, "/set trunc qaz",
                            "ERROR: Does not match any current feedback mode: qaz -- /set trunc qaz"),
                    (a) -> assertCommandOutputStartsWith(a, "/set truncation te 111 import,added",
                            "ERROR: Different selector kinds in same sections of"),
                    (a) -> assertCommandOutputContains(a, "/set mode",
                            "|  /set truncation verbose"),
                    (a) -> assertCommandOutputStartsWith(a, "/set mode -command",
                            "ERROR: Missing the feedback mode"),
                    (a) -> assertCommandOutputStartsWith(a, "/set mode x -quiet y",
                            "ERROR: Does not match any current feedback mode"),
                    (a) -> assertCommandOutputStartsWith(a, "/set prompt",
                            "|  /set prompt"),
                    (a) -> assertCommandOutputStartsWith(a, "/set prompt te",
                            "|  /set prompt te "),
                    (a) -> assertCommandOutputStartsWith(a, "/set prompt te aaa xyz",
                            "ERROR: Format 'aaa' must be quoted"),
                    (a) -> assertCommandOutputStartsWith(a, "/set prompt te 'aaa' xyz",
                            "ERROR: Format 'xyz' must be quoted"),
                    (a) -> assertCommandOutputStartsWith(a, "/set prompt te aaa",
                            "ERROR: Format 'aaa' must be quoted"),
                    (a) -> assertCommandOutputStartsWith(a, "/set prompt te 'aaa'",
                            "ERROR: Continuation prompt required"),
                    (a) -> assertCommandOutputStartsWith(a, "/set feedback normal",
                            "|  Feedback mode: normal")
            );
        } finally {
            assertCommandCheckOutput(false, "/set feedback normal", s -> {
            });
        }
    }

    public void testSetHelp() {
        try {
            test(
                    (a) -> assertCommandOutputContains(a, "/help /set", "command to launch"),
                    (a) -> assertCommandOutputContains(a, "/help /set format", "display"),
                    (a) -> assertCommandOutputContains(a, "/hel /se for", "vardecl"),
                    (a) -> assertCommandOutputContains(a, "/help /set editor", "temporary file")
            );
        } finally {
            assertCommandCheckOutput(false, "/set feedback normal", s -> {
            });
        }
    }
}
