/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.List;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import tools.javac.combo.CompilationTestCase;
import tools.javac.combo.JavacTemplateTestBase;

import static java.util.stream.Collectors.toList;

@Test
public class ExpSwitchNestingTest extends CompilationTestCase {
    private static final String RUNNABLE = "Runnable r = () -> { # };";
    private static final String INT_FN = "java.util.function.IntSupplier r = () -> { # };";
    private static final String LABEL = "label: #";
    private static final String DEF_LABEL_VAR = "int label = 0; { # }";
    private static final String FOR = "for (int i=0; i<10; i++) { # }";
    private static final String FOR_EACH = "for (int i : new int[] {}) { # }";
    private static final String WHILE = "while (cond) { # }";
    private static final String DO = "do { # } while (cond);";
    private static final String SSWITCH = "switch (x) { case 0: # };";
    private static final String ESWITCH_Z = "int res = switch (x) { case 0 -> { # } default -> 0; };";
    private static final String ESWITCH_S = "String res_string = switch (x) { case 0 -> { # } default -> \"default\"; };";
    private static final String INT_FN_ESWITCH = "java.util.function.IntSupplier r = switch (x) { case 0 -> { # } default -> null; };";
    private static final String INT_ESWITCH_DEFAULT = "int res = switch (x) { default -> { # } };";
    private static final String IF = "if (cond) { # } else throw new RuntimeException();";
    private static final String BLOCK = "{ # }";
    private static final String YIELD_Z = "yield 0;";
    private static final String YIELD_S = "yield \"hello world\";";
    private static final String YIELD_INT_FN = "yield () -> 0 ;";
    private static final String BREAK_N = "break;";
    private static final String BREAK_L = "break label;";
    private static final String YIELD_L = "yield label;";
    private static final String RETURN_Z = "return 0;";
    private static final String RETURN_N = "return;";
    private static final String RETURN_S = "return \"Hello\";";
    private static final String CONTINUE_N = "continue;";
    private static final String CONTINUE_L = "continue label;";
    private static final String NOTHING = "System.out.println();";

    // containers that do not require exhaustiveness
    private static final List<String> CONTAINERS
            = List.of(RUNNABLE, FOR, WHILE, DO, SSWITCH, IF, BLOCK);
    // containers that do not require exhaustiveness that are statements
    private static final List<String> CONTAINER_STATEMENTS
            = List.of(FOR, WHILE, DO, SSWITCH, IF, BLOCK);

    // @@@ When expression switch becomes a permanent feature, we don't need these any more
    private static final String SHELL = "class C { static boolean cond = false; static int x = 0; void m() { # } }";

    {
        setDefaultFilename("C.java");
        setProgramShell(SHELL);
    }

    public void testReallySimpleCases() {
        for (String s : CONTAINERS)
            assertOK(s, NOTHING);
        for (String s : CONTAINER_STATEMENTS)
            assertOK(LABEL, s, NOTHING);
    }

    public void testLambda() {
        assertOK(RUNNABLE, RETURN_N);
        assertOK(RUNNABLE, NOTHING);
        assertOK(INT_FN, RETURN_Z);
        assertFail("compiler.err.break.outside.switch.loop", RUNNABLE, BREAK_N);
        assertFail("compiler.err.no.switch.expression", RUNNABLE, YIELD_Z);
        assertFail("compiler.err.no.switch.expression", RUNNABLE, YIELD_S);
        assertFail("compiler.err.break.outside.switch.loop", INT_FN, BREAK_N);
        assertFail("compiler.err.no.switch.expression", INT_FN, YIELD_Z);
        assertFail("compiler.err.no.switch.expression", INT_FN, YIELD_S);
        assertFail("compiler.err.cont.outside.loop", RUNNABLE, CONTINUE_N);
        assertFail("compiler.err.undef.label", RUNNABLE, BREAK_L);
        assertFail("compiler.err.undef.label", RUNNABLE, CONTINUE_L);
        assertFail("compiler.err.cont.outside.loop", INT_FN, CONTINUE_N);
        assertFail("compiler.err.undef.label", INT_FN, BREAK_L);
        assertFail("compiler.err.undef.label", INT_FN, CONTINUE_L);
        assertFail("compiler.err.undef.label", LABEL, BLOCK, RUNNABLE, BREAK_L);
        assertFail("compiler.err.undef.label", LABEL, BLOCK, RUNNABLE, CONTINUE_L);
        assertFail("compiler.err.undef.label", LABEL, BLOCK, INT_FN, BREAK_L);
        assertFail("compiler.err.undef.label", LABEL, BLOCK, INT_FN, CONTINUE_L);
    }

    public void testEswitch() {
        //Int-valued switch expressions
        assertOK(ESWITCH_Z, YIELD_Z);
        assertOK(LABEL, BLOCK, ESWITCH_Z, YIELD_Z);
        assertFail("compiler.err.break.outside.switch.expression", ESWITCH_Z, BREAK_N);
        assertFail("compiler.err.prob.found.req", ESWITCH_Z, YIELD_S);
        assertFail("compiler.err.undef.label", ESWITCH_Z, BREAK_L);
        assertFail("compiler.err.break.outside.switch.expression", LABEL, BLOCK, ESWITCH_Z, BREAK_L);
        assertFail("compiler.err.undef.label", ESWITCH_Z, CONTINUE_L);
        assertFail("compiler.err.continue.outside.switch.expression", ESWITCH_Z, CONTINUE_N);
        assertFail("compiler.err.return.outside.switch.expression", ESWITCH_Z, RETURN_N);
        assertFail("compiler.err.return.outside.switch.expression", ESWITCH_Z, RETURN_Z);

        assertOK(INT_ESWITCH_DEFAULT, YIELD_Z);
        assertFail("compiler.err.break.outside.switch.expression", INT_ESWITCH_DEFAULT, BREAK_N);
        assertFail("compiler.err.prob.found.req", INT_ESWITCH_DEFAULT, YIELD_S);
        assertFail("compiler.err.undef.label", INT_ESWITCH_DEFAULT, BREAK_L);


        // String-valued switch expressions
        assertOK(ESWITCH_S, YIELD_S);
        assertOK(LABEL, BLOCK, ESWITCH_S, YIELD_S);
        assertFail("compiler.err.break.outside.switch.expression", ESWITCH_S, BREAK_N);
        assertFail("compiler.err.prob.found.req", ESWITCH_S, YIELD_Z);
        assertFail("compiler.err.undef.label", ESWITCH_S, BREAK_L);
        assertFail("compiler.err.break.outside.switch.expression", LABEL, BLOCK, ESWITCH_S, BREAK_L);
        assertFail("compiler.err.undef.label", ESWITCH_S, CONTINUE_L);
        assertFail("compiler.err.continue.outside.switch.expression", ESWITCH_S, CONTINUE_N);
        assertFail("compiler.err.return.outside.switch.expression", ESWITCH_S, RETURN_N);
        assertFail("compiler.err.return.outside.switch.expression", ESWITCH_S, RETURN_S);
        // Function-valued switch expression
        assertOK(INT_FN_ESWITCH, YIELD_INT_FN);
        assertFail("compiler.err.break.outside.switch.expression", INT_FN_ESWITCH, BREAK_N);
        assertFail("compiler.err.prob.found.req", INT_FN_ESWITCH, YIELD_Z);
        assertFail("compiler.err.prob.found.req", INT_FN_ESWITCH, YIELD_S);

        assertFail("compiler.err.undef.label", INT_FN_ESWITCH, BREAK_L);
        assertFail("compiler.err.break.outside.switch.expression", LABEL, BLOCK, INT_FN_ESWITCH, BREAK_L);
        assertFail("compiler.err.undef.label", INT_FN_ESWITCH, CONTINUE_L);
        assertFail("compiler.err.continue.outside.switch.expression", INT_FN_ESWITCH, CONTINUE_N);
        assertFail("compiler.err.return.outside.switch.expression", INT_FN_ESWITCH, RETURN_N);
        assertFail("compiler.err.return.outside.switch.expression", INT_FN_ESWITCH, RETURN_S);

    }

    public void testNestedInExpSwitch() {
        assertOK(ESWITCH_Z, IF,     YIELD_Z);
        assertOK(ESWITCH_Z, BLOCK,  YIELD_Z);
        //
        assertOK(ESWITCH_Z, IF,     IF,     YIELD_Z);
        assertOK(ESWITCH_Z, IF,     BLOCK,  YIELD_Z);
        assertOK(ESWITCH_Z, BLOCK,  IF,     YIELD_Z);
        assertOK(ESWITCH_Z, BLOCK,  BLOCK,  YIELD_Z);
        //
        assertOK(ESWITCH_Z, IF,     IF,     IF,     YIELD_Z);
        assertOK(ESWITCH_Z, IF,     IF,     BLOCK,  YIELD_Z);
        assertOK(ESWITCH_Z, IF,     BLOCK,  IF,     YIELD_Z);
        assertOK(ESWITCH_Z, IF,     BLOCK,  BLOCK,  YIELD_Z);
        assertOK(ESWITCH_Z, BLOCK,  IF,     IF,     YIELD_Z);
        assertOK(ESWITCH_Z, BLOCK,  IF,     BLOCK,  YIELD_Z);
        assertOK(ESWITCH_Z, BLOCK,  BLOCK,  IF,     YIELD_Z);
        assertOK(ESWITCH_Z, BLOCK,  BLOCK,  BLOCK,  YIELD_Z);
        //
        assertOK(ESWITCH_Z, YIELD_Z, SSWITCH, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, FOR, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, WHILE, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, DO, YIELD_Z);
        assertFail("compiler.err.no.switch.expression", ESWITCH_Z, INT_FN, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, SSWITCH, IF, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, FOR, IF, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, WHILE, IF, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, DO, IF, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, BLOCK, SSWITCH, IF, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, BLOCK, FOR, IF, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, BLOCK, WHILE, IF, YIELD_Z);
        assertOK(ESWITCH_Z, YIELD_Z, BLOCK, DO, IF, YIELD_Z);
    }

    public void testBreakExpressionLabelDisambiguation() {
        assertOK(DEF_LABEL_VAR, ESWITCH_Z, YIELD_L);
        assertFail("compiler.err.undef.label", DEF_LABEL_VAR, ESWITCH_Z, BREAK_L);
        assertFail("compiler.err.break.outside.switch.expression", LABEL, FOR, BLOCK, DEF_LABEL_VAR, ESWITCH_Z, BREAK_L);
        assertOK(DEF_LABEL_VAR, ESWITCH_Z, YIELD_Z, LABEL, FOR, BREAK_L); //label break
        assertFail("compiler.err.break.outside.switch.expression", DEF_LABEL_VAR, LABEL, BLOCK, ESWITCH_Z, BREAK_L);
        assertOK(DEF_LABEL_VAR, LABEL, BLOCK, ESWITCH_Z, YIELD_L); //expression break
        //
    }

    public void testFunReturningSwitchExp() {
        assertOK(INT_FN_ESWITCH, YIELD_INT_FN);
    }

    public void testContinueLoops() {
        assertOK(LABEL, FOR, CONTINUE_L);
        assertOK(LABEL, FOR_EACH, CONTINUE_L);
        assertOK(LABEL, WHILE, CONTINUE_L);
        assertOK(LABEL, DO, CONTINUE_L);
        assertFail("compiler.err.not.loop.label", LABEL, CONTINUE_L);
    }
}
