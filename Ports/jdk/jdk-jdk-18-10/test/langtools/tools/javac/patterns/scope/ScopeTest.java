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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import tools.javac.combo.JavacTemplateTestBase;

import static java.util.stream.Collectors.toList;

@Test
public class ScopeTest extends JavacTemplateTestBase {

    private static String st_block(String... statements) {
        return Arrays.stream(statements).collect(Collectors.joining("", "{", "}"));
    }

    private static String st_if(String condition, String then, String els) {
        return "if (" + condition + ") " + then + " else " + els;
    }

    private static String st_while(String condition, String body) {
        return "while (" + condition + ") " + body;
    }

    private static String st_do_while(String body, String condition) {
        return "do " + body + " while (" + condition + ");";
    }

    private static String st_for(String init, String condition, String update, String body) {
        return "for (" + init + "; " + condition + "; " + update + ") " + body;
    }

    private static String st_s_use() {
        return "s.length();";
    }

    private static String st_break() {
        return "break;";
    }

    private static String st_return() {
        return "return;";
    }

    private static String st_noop() {
        return ";";
    }

    private static String expr_empty() {
        return "";
    }

    private static String expr_o_match_str() {
        return "o instanceof String s";
    }

    private static String expr_not(String expr) {
        return "!(" + expr + ")";
    }

    @AfterMethod
    public void dumpTemplateIfError(ITestResult result) {
        // Make sure offending template ends up in log file on failure
        if (!result.isSuccess()) {
            System.err.printf("Diagnostics: %s%nTemplate: %s%n", diags.errorKeys(), sourceFiles.stream().map(p -> p.snd).collect(toList()));
        }
    }

    private void program(String block) {
        String s = "class C { void m(Object o) " + block + "}";
        addSourceFile("C.java", s);
    }

    private void assertOK(String block) {
        reset();
        program(block);
        try {
            compile();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertCompileSucceeded();
    }

    private void assertFail(String expectedDiag, String block) {
        reset();
        program(block);
        try {
            compile();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertCompileFailed(expectedDiag);
    }

    public void testIf() {
        assertOK(st_block(st_if(expr_o_match_str(), st_s_use(), st_return()), st_s_use()));
        assertOK(st_block(st_if(expr_not(expr_o_match_str()), st_return(), st_s_use()), st_s_use()));
        assertFail("compiler.err.cant.resolve.location", st_block(st_if(expr_o_match_str(), st_s_use(), st_noop()), st_s_use()));
        assertFail("compiler.err.cant.resolve.location", st_block(st_if(expr_not(expr_o_match_str()), st_noop(), st_s_use()), st_s_use()));
    }

    public void testWhile() {
        assertOK(st_block(st_while(expr_not(expr_o_match_str()), st_noop()), st_s_use()));
        assertFail("compiler.err.cant.resolve.location", st_block(st_while(expr_not(expr_o_match_str()), st_break()), st_s_use()));
    }

    public void testDoWhile() {
        assertOK(st_block(st_do_while(st_noop(), expr_not(expr_o_match_str())), st_s_use()));
        assertFail("compiler.err.cant.resolve.location", st_block(st_do_while(st_break(), expr_not(expr_o_match_str())), st_s_use()));
    }

    public void testFor() {
        assertOK(st_block(st_for(expr_empty(), expr_not(expr_o_match_str()), expr_empty(), st_noop()), st_s_use()));
        assertFail("compiler.err.cant.resolve.location", st_block(st_for(expr_empty(), expr_not(expr_o_match_str()), expr_empty(), st_break()), st_s_use()));
    }

}
