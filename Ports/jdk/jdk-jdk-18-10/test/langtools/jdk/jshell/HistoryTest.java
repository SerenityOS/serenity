/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8166744
 * @summary Test Completion
 * @modules jdk.internal.le/jdk.internal.org.jline.reader
 *          jdk.jshell/jdk.internal.jshell.tool:+open
 * @build HistoryTest
 * @run testng HistoryTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.testng.annotations.Test;
import jdk.internal.jshell.tool.JShellTool;
import jdk.internal.jshell.tool.JShellToolBuilder;
import jdk.internal.org.jline.reader.History;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;

public class HistoryTest extends ReplToolTesting {

    private JShellTool repl;

    @Override
    protected void testRawRun(Locale locale, String[] args) {
        // turn on logging of launch failures
        Logger.getLogger("jdk.jshell.execution").setLevel(Level.ALL);
        repl = ((JShellToolBuilder) builder(locale))
                .rawTool();
        try {
            repl.start(args);
        } catch (Exception ex) {
            fail("Repl tool died with exception", ex);
        }
    }

    @Test
    public void testHistory() {
        test(
             a -> {if (!a) setCommandInput("void test() {\n");},
             a -> {if (!a) setCommandInput("    System.err.println(1);\n");},
             a -> {if (!a) setCommandInput("    System.err.println(1);\n");},
             a -> {assertCommand(a, "} //test", "|  created method test()");},
             a -> {
                 if (!a) {
                     try {
                         previousAndAssert(getHistory(), "void test() {\n" +
                                                         "    System.err.println(1);\n" +
                                                         "    System.err.println(1);\n" +
                                                         "} //test");
                     } catch (Exception ex) {
                         throw new IllegalStateException(ex);
                     }
                 }
                 assertCommand(a, "int dummy;", "dummy ==> 0");
             });
        test(
             a -> {if (!a) setCommandInput("void test2() {\n");},
             a -> {assertCommand(a, "} //test2", "|  created method test2()");},
             a -> {
                 if (!a) {
                     try {
                         previousAndAssert(getHistory(), "void test2() {\n" +
                                                         "} //test2");
                         previousAndAssert(getHistory(), "/debug 0"); //added by test framework
                         previousAndAssert(getHistory(), "/exit");
                         previousAndAssert(getHistory(), "int dummy;");
                         previousAndAssert(getHistory(), "void test() {\n" +
                                                         "    System.err.println(1);\n" +
                                                         "    System.err.println(1);\n" +
                                                         "} //test");
                     } catch (Exception ex) {
                         throw new IllegalStateException(ex);
                     }
                 }
                 assertCommand(a, "int dummy;", "dummy ==> 0");
             });
    }

    @Test
    public void test8166744() {
        test(
             a -> {if (!a) setCommandInput("class C {\n");},
             a -> {if (!a) setCommandInput("void f() {\n");},
             a -> {if (!a) setCommandInput("}\n");},
             a -> {assertCommand(a, "}", "|  created class C");},
             a -> {
                 if (!a) {
                     try {
                         previousAndAssert(getHistory(), "class C {\n" +
                                                         "void f() {\n" +
                                                         "}\n" +
                                                         "}");
                         getHistory().add("class C {\n" +
                                          "void f() {\n" +
                                          "}\n" +
                                          "}");
                     } catch (Exception ex) {
                         throw new IllegalStateException(ex);
                     }
                 }
                 assertCommand(a, "int dummy;", "dummy ==> 0");
             });
        test(
             a -> {if (!a) setCommandInput("class C {\n");},
             a -> {if (!a) setCommandInput("void f() {\n");},
             a -> {if (!a) setCommandInput("}\n");},
             a -> {assertCommand(a, "}", "|  created class C");},
             a -> {
                 if (!a) {
                     try {
                         previousAndAssert(getHistory(), "class C {\n" +
                                                         "void f() {\n" +
                                                         "}\n" +
                                                         "}");
                         getHistory().add("class C {\n" +
                                          "void f() {\n" +
                                          "}\n" +
                                          "}");
                     } catch (Exception ex) {
                         throw new IllegalStateException(ex);
                     }
                 }
                 assertCommand(a, "int dummy;", "dummy ==> 0");
             });
    }

    @Test
    public void testReadExistingHistory() {
        prefsMap.put("HISTORY_LINE_0", "/debug 0");
        prefsMap.put("HISTORY_LINE_1", "void test() {\\");
        prefsMap.put("HISTORY_LINE_2", "    System.err.println(1);\\");
        prefsMap.put("HISTORY_LINE_3", "    System.err.println(`\\\\\\\\\\");
        prefsMap.put("HISTORY_LINE_4", "    \\\\\\");
        prefsMap.put("HISTORY_LINE_5", "`);\\");
        prefsMap.put("HISTORY_LINE_6", "} //test");
        test(
             a -> {assertCommand(a, "int i", "i ==> 0");},
             a -> {
                 if (!a) {
                     try {
                         previousAndAssert(getHistory(), "int i");
                         previousAndAssert(getHistory(), "/debug 0"); //added by test framework
                         previousAndAssert(getHistory(), "void test() {\n" +
                                                         "    System.err.println(1);\n" +
                                                         "    System.err.println(`\\\\\n" +
                                                         "    \\\n" +
                                                         "`);\n" +
                                                         "} //test");
                     } catch (Exception ex) {
                         throw new IllegalStateException(ex);
                     }
                 }
                  assertCommand(a, "/exit", "");
             });
        assertEquals(prefsMap.get("HISTORY_LINE_00"), "/debug 0");
        assertEquals(prefsMap.get("HISTORY_LINE_01"), "void test() {\\");
        assertEquals(prefsMap.get("HISTORY_LINE_02"), "    System.err.println(1);\\");
        assertEquals(prefsMap.get("HISTORY_LINE_03"), "    System.err.println(`\\\\\\\\\\");
        assertEquals(prefsMap.get("HISTORY_LINE_04"), "    \\\\\\");
        assertEquals(prefsMap.get("HISTORY_LINE_05"), "`);\\");
        assertEquals(prefsMap.get("HISTORY_LINE_06"), "} //test");
        assertEquals(prefsMap.get("HISTORY_LINE_07"), "/debug 0");
        assertEquals(prefsMap.get("HISTORY_LINE_08"), "int i");
        assertEquals(prefsMap.get("HISTORY_LINE_09"), "/exit");
        System.err.println("prefsMap: " + prefsMap);
    }

    private History getHistory() throws Exception {
        Field input = repl.getClass().getDeclaredField("input");
        input.setAccessible(true);
        Object console = input.get(repl);
        Method getHistory = console.getClass().getDeclaredMethod("getHistory");
        getHistory.setAccessible(true);
        return (History) getHistory.invoke(console);
    }

    private void previousAndAssert(History history, String expected) {
        assertTrue(history.previous());
        assertEquals(history.current().toString(), expected);
    }

    @BeforeMethod
    public void setUp() {
        super.setUp();
        System.setProperty("jshell.test.allow.incomplete.inputs", "false");
    }

}
