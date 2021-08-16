/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8149524 8131024 8165211 8080071 8130454 8167343 8129559 8114842 8182268 8223782 8235474 8246774
 * @summary Test SourceCodeAnalysis
 * @build KullaTesting TestingInputStream
 * @run testng CompletenessTest
 */

import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import javax.lang.model.SourceVersion;
import jdk.jshell.JShell;

import org.testng.annotations.Test;
import jdk.jshell.SourceCodeAnalysis.Completeness;

import static jdk.jshell.SourceCodeAnalysis.Completeness.*;
import org.testng.annotations.BeforeMethod;

@Test
public class CompletenessTest extends KullaTesting {

    // Add complete units that end with semicolon to complete_with_semi (without
    // the semicolon).  Both cases will be tested.
    static final String[] complete = new String[] {
        "{ x= 4; }",
        "int mm(int x) {kll}",
        "if (t) { ddd; }",
        "for (int i = 0; i < lines.length(); ++i) { foo }",
        "while (ct == null) { switch (current.kind) { case EOF: { } } }",
        "if (match.kind == BRACES && (prevCT.kind == ARROW || prevCT.kind == NEW_MIDDLE)) { new CT(UNMATCHED, current, \"Unmatched \" + unmatched); }",
        "enum TK { EOF(TokenKind.EOF, 0), NEW_MIDDLE(XEXPR1|XTERM); }",
        "List<T> f() { return null; }",
        "List<?> f() { return null; }",
        "List<? extends Object> f() { return null; }",
        "Map<? extends Object, ? super Object> f() { return null; }",
        "class C { int z; }",
        "synchronized (r) { f(); }",
        "try { } catch (Exception ex) { }",
        "try { } catch (Exception ex) { } finally { }",
        "try { } finally { }",
        "try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFileName)) { }",
        "foo: while (true) { printf(\"Innn\"); break foo; }",
        "class Case<E1 extends Enum<E1>, E2 extends Enum<E2>, E3 extends Enum<E3>> {}",
        ";",
        "enum Tt { FOO, BAR, BAZ,; }",
        "record D(int i) {}",
        "static record D(int i) {}",
    };

    static final String[] expression = new String[] {
        "test",
        "x + y",
        "x + y ++",
        "p = 9",
        "match(BRACKETS, TokenKind.LBRACKET)",
        "new C()",
        "new C() { public String toString() { return \"Hi\"; } }",
        "new int[]",
        "new int[] {1, 2,3}",
        "new Foo() {}",
        "i >= 0 && Character.isWhitespace(s.charAt(i))",
        "int.class",
        "String.class",
        "record.any",
        "record()",
        "record(1)",
        "record.length()"
    };

    static final String[] complete_with_semi = new String[] {
        "int mm",
        "if (t) ddd",
        "int p = 9",
        "int p",
        "Deque<Token> stack = new ArrayDeque<>()",
        "final Deque<Token> stack = new ArrayDeque<>()",
        "java.util.Scanner input = new java.util.Scanner(System.in)",
        "java.util.Scanner input = new java.util.Scanner(System.in) { }",
        "int j = -i",
        "String[] a = { \"AAA\" }",
        "assert true",
        "int path[]",
        "int path[][]",
        "int path[][] = new int[22][]",
        "int path[] = new int[22]",
        "int path[] = new int[] {1, 2, 3}",
        "int[] path",
        "int path[] = new int[22]",
        "int path[][] = new int[22][]",
        "for (Object o : a) System.out.println(\"Yep\")",
        "while (os == null) System.out.println(\"Yep\")",
        "do f(); while (t)",
        "if (os == null) System.out.println(\"Yep\")",
        "if (t) if (!t) System.out.println(123)",
        "for (int i = 0; i < 10; ++i) if (i < 5) System.out.println(i); else break",
        "for (int i = 0; i < 10; ++i) if (i < 5) System.out.println(i); else continue",
        "for (int i = 0; i < 10; ++i) if (i < 5) System.out.println(i); else return",
        "throw ex",
        "C c = new C()",
        "java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFileName)",
        "BufferedReader br = new BufferedReader(new FileReader(path))",
        "bar: g()",
        "baz: while (true) if (t()) printf('-'); else break baz",
        "java.util.function.IntFunction<int[]> ggg = int[]::new",
        "List<? extends Object> l",
        "int[] m = {1, 2}",
        "int[] m = {1, 2}, n = null",
        "int[] m = {1, 2}, n",
        "int[] m = {1, 2}, n = {3, 4}",
    };

    static final String[] considered_incomplete = new String[] {
        "if (t)",
        "if (t) { } else",
        "if (t) if (!t)",
        "if (match.kind == BRACES && (prevCT.kind == ARROW || prevCT.kind == NEW_MIDDLE))",
        "for (int i = 0; i < 10; ++i)",
        "while (os == null)",
    };

    static final String[] definitely_incomplete = new String[] {
        "int mm(",
        "int mm(int x",
        "int mm(int x)",
        "int mm(int x) {",
        "int mm(int x) {kll",
        "if",
        "if (",
        "if (t",
        "if (t) {",
        "if (t) { ddd",
        "if (t) { ddd;",
        "if (t) if (",
        "if (stack.isEmpty()) {",
        "if (match.kind == BRACES && (prevCT.kind == ARROW || prevCT.kind == NEW_MIDDLE)) {",
        "if (match.kind == BRACES && (prevCT.kind == ARROW || prevCT.kind == NEW_MIDDLE)) { new CT(UNMATCHED, current, \"Unmatched \" + unmatched);",
        "x +",
        "x *",
        "3 *",
        "int",
        "for (int i = 0; i < lines.length(); ++i) {",
        "new",
        "new C(",
        "new int[",
        "new int[] {1, 2,3",
        "new int[] {",
        "while (ct == null) {",
        "while (ct == null) { switch (current.kind) {",
        "while (ct == null) { switch (current.kind) { case EOF: {",
        "while (ct == null) { switch (current.kind) { case EOF: { } }",
        "enum TK {",
        "enum TK { EOF(TokenKind.EOF, 0),",
        "enum TK { EOF(TokenKind.EOF, 0), NEW_MIDDLE(XEXPR1|XTERM)",
        "enum TK { EOF(TokenKind.EOF, 0), NEW_MIDDLE(XEXPR1|XTERM); ",
        "enum Tt { FOO, BAR, BAZ,;",
        "class C",
        "class C extends D",
        "class C implements D",
        "class C implements D, E",
        "interface I extends D",
        "interface I extends D, E",
        "enum E",
        "enum E implements I1",
        "enum E implements I1, I2",
        "@interface Anno",
        "void f()",
        "void f() throws E",
        "@A(",
        "int n = 4,",
        "int n,",
        "int[] m = {1, 2},",
        "int[] m = {1, 2}, n = {3, 4},",
        "Map<String,",
        "switch (x) {",
        "var v = switch (x) {",
        "var v = switch (x) { case ",
        "var v = switch (x) { case 0:",
        "var v = switch (x) { case 0: break 12; ",
        "record D",
        "record D(",
        "record D(String",
        "record D(String i",
        "record D(String i,",
        "record D(String i, String",
        "record D(String i, String j",
        "record D(String i)",
        "record D(String i, String j)",
        "record D(String i) {",
        "record D(String i, String j) {",
        "static record D",
        "static record D(",
        "static record D(String",
        "static record D(String i",
        "static record D(String i,",
        "static record D(String i, String",
        "static record D(String i, String j",
        "static record D(String i)",
        "static record D(String i, String j)",
        "static record D(String i) {",
        "static record D(String i, String j) {",
    };

    static final String[] unknown = new String[] {
        "new ;"
    };

    static final Map<Completeness, String[]> statusToCases = new HashMap<>();
    static {
        statusToCases.put(COMPLETE, complete);
        statusToCases.put(COMPLETE_WITH_SEMI, complete_with_semi);
        statusToCases.put(CONSIDERED_INCOMPLETE, considered_incomplete);
        statusToCases.put(DEFINITELY_INCOMPLETE, definitely_incomplete);
    }

    private void assertStatus(String input, Completeness status, String source) {
        String augSrc;
        switch (status) {
            case COMPLETE_WITH_SEMI:
                augSrc = source + ";";
                break;

            case DEFINITELY_INCOMPLETE:
                augSrc = null;
                break;

            case CONSIDERED_INCOMPLETE:
                augSrc = source + ";";
                break;

            case EMPTY:
            case COMPLETE:
            case UNKNOWN:
                augSrc = source;
                break;

            default:
                throw new AssertionError();
        }
        assertAnalyze(input, status, augSrc);
    }

    private void assertStatus(String[] ins, Completeness status) {
        for (String input : ins) {
            assertStatus(input, status, input);
        }
    }

    public void test_complete() {
         assertStatus(complete, COMPLETE);
    }

    public void test_expression() {
        assertStatus(expression, COMPLETE);
    }

    public void test_complete_with_semi() {
        assertStatus(complete_with_semi, COMPLETE_WITH_SEMI);
    }

    public void test_considered_incomplete() {
        assertStatus(considered_incomplete, CONSIDERED_INCOMPLETE);
    }

    public void test_definitely_incomplete() {
        assertStatus(definitely_incomplete, DEFINITELY_INCOMPLETE);
    }

    public void test_unknown() {
        assertStatus(definitely_incomplete, DEFINITELY_INCOMPLETE);
    }

    public void testCompleted_complete_with_semi() {
        for (String in : complete_with_semi) {
            String input = in + ";";
            assertStatus(input, COMPLETE, input);
        }
    }

    public void testCompleted_expression_with_semi() {
        for (String in : expression) {
            String input = in + ";";
            assertStatus(input, COMPLETE, input);
        }
    }

    public void testCompleted_considered_incomplete() {
        for (String in : considered_incomplete) {
            String input = in + ";";
            assertStatus(input, COMPLETE, input);
        }
    }

    private void assertSourceByStatus(String first) {
        for (Map.Entry<Completeness, String[]> e : statusToCases.entrySet()) {
            for (String in : e.getValue()) {
                String input = first + in;
                assertAnalyze(input, COMPLETE, first, in, true);
            }
        }
    }

    public void testCompleteSource_complete() {
        for (String input : complete) {
            assertSourceByStatus(input);
        }
    }

    public void testCompleteSource_complete_with_semi() {
        for (String in : complete_with_semi) {
            String input = in + ";";
            assertSourceByStatus(input);
        }
    }

    public void testCompleteSource_expression() {
        for (String in : expression) {
            String input = in + ";";
            assertSourceByStatus(input);
        }
    }

    public void testCompleteSource_considered_incomplete() {
        for (String in : considered_incomplete) {
            String input = in + ";";
            assertSourceByStatus(input);
        }
    }

    public void testTrailingSlash() {
        assertStatus("\"abc\\", UNKNOWN, "\"abc\\");
    }

    public void testOpenComment() {
        assertStatus("int xx; /* hello", DEFINITELY_INCOMPLETE, null);
        assertStatus("/**  test", DEFINITELY_INCOMPLETE, null);
    }

    public void testTextBlocks() {
        assertStatus("\"\"\"", DEFINITELY_INCOMPLETE, null);
        assertStatus("\"\"\"broken", DEFINITELY_INCOMPLETE, null);
        assertStatus("\"\"\"\ntext", DEFINITELY_INCOMPLETE, null);
        assertStatus("\"\"\"\ntext\"\"", DEFINITELY_INCOMPLETE, "\"\"\"\ntext\"\"\"");
        assertStatus("\"\"\"\ntext\"\"\"", COMPLETE, "\"\"\"\ntext\"\"\"");
        assertStatus("\"\"\"\ntext\\\"\"\"\"", COMPLETE, "\"\"\"\ntext\\\"\"\"\"");
        assertStatus("\"\"\"\ntext\\\"\"\"", DEFINITELY_INCOMPLETE, null);
        assertStatus("\"\"\"\ntext\\\"\"\"\\\"\"\"", DEFINITELY_INCOMPLETE, null);
        assertStatus("\"\"\"\ntext\\\"\"\"\\\"\"\"\"\"\"", COMPLETE, "\"\"\"\ntext\\\"\"\"\\\"\"\"\"\"\"");
    }

    public void testMiscSource() {
        assertStatus("if (t) if ", DEFINITELY_INCOMPLETE, "if (t) if"); //Bug
        assertStatus("int m() {} dfd", COMPLETE, "int m() {}");
        assertStatus("int p = ", DEFINITELY_INCOMPLETE, "int p ="); //Bug
        assertStatus("int[] m = {1, 2}, n = new int[0];  int i;", COMPLETE,
                     "int[] m = {1, 2}, n = new int[0];");
    }
}
