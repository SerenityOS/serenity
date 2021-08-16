/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8188225 8243557
 * @summary Tests for shell error translation
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.internal.jshell.tool
 * @library /tools/lib
 * @build KullaTesting TestingInputStream ExpectedDiagnostic toolbox.ToolBox Compiler
 * @run testng ErrorTranslationTest
 */

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.tools.Diagnostic;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class ErrorTranslationTest extends ReplToolTesting {

    @Test(enabled = false) // TODO 8080353
    public void testErrors() {
        test(
                a -> assertDiagnostic(a, "abstract void f();", newExpectedDiagnostic(0, 8, 0, -1, -1, Diagnostic.Kind.ERROR)),
                a -> assertDiagnostic(a, "native void f();", newExpectedDiagnostic(0, 6, 0, -1, -1, Diagnostic.Kind.ERROR)),
                a -> assertDiagnostic(a, "static void f();", newExpectedDiagnostic(0, 16, 0, -1, -1, Diagnostic.Kind.ERROR)),
                a -> assertDiagnostic(a, "synchronized void f();", newExpectedDiagnostic(0, 12, 0, -1, -1, Diagnostic.Kind.ERROR)),
                a -> assertDiagnostic(a, "default void f();", newExpectedDiagnostic(0, 7, 0, -1, -1, Diagnostic.Kind.ERROR))
        );
    }

    public void testlvtiErrors() {
        test(
                a -> assertDiagnostic(a, "var broken = () -> {};", newExpectedDiagnostic(0, 22, 0, -1, -1, Diagnostic.Kind.ERROR)),
                a -> assertDiagnostic(a, "void t () { var broken = () -> {}; }", newExpectedDiagnostic(12, 34, 0, -1, -1, Diagnostic.Kind.ERROR))
        );
    }

    public void testExceptionErrors() {
        test(
                a -> assertDiagnostic(a, "try { } catch (IllegalStateException | java.io.IOException ex) { }", newExpectedDiagnostic(39, 58, -1, -1, -1, Diagnostic.Kind.ERROR))
        );
    }

    @Test(enabled = false) // TODO 8132147
    public void stressTest() {
        Compiler compiler = new Compiler();
        Path oome = compiler.getPath("OOME.repl");
        Path soe = compiler.getPath("SOE.repl");
        compiler.writeToFile(oome,
                "List<byte[]> list = new ArrayList<>();\n",
                "while (true) {\n" +
                "   list.add(new byte[1000000]);\n" +
                "}");
        compiler.writeToFile(soe,
                "void f() { f(); }",
                "f();");
        List<ReplTest> tests = new ArrayList<>();
        for (int i = 0; i < 25; ++i) {
            tests.add(a -> assertCommandCheckOutput(a, "/o " + soe.toString(),
                    assertStartsWith("|  java.lang.StackOverflowError thrown")));
            tests.add(a -> assertCommandCheckOutput(a, "/o " + oome.toString(),
                    assertStartsWith("|  java.lang.OutOfMemoryError thrown: Java heap space")));
        }
        test(tests.toArray(new ReplTest[tests.size()]));
    }

    private ExpectedDiagnostic newExpectedDiagnostic(long startPosition, long endPosition, long position,
                                                     long lineNumber, long columnNumber, Diagnostic.Kind kind) {
        return new ExpectedDiagnostic("", startPosition, endPosition, position, lineNumber, columnNumber, kind);
    }

    private void assertDiagnostic(boolean after, String cmd, ExpectedDiagnostic expectedDiagnostic) {
        assertCommandCheckOutput(after, cmd, assertDiagnostic(cmd, expectedDiagnostic));
    }

    private Consumer<String> assertDiagnostic(String expectedSource, ExpectedDiagnostic expectedDiagnostic) {
        int start = (int) expectedDiagnostic.getStartPosition();
        int end = (int) expectedDiagnostic.getEndPosition();
        String expectedMarkingLine = createMarkingLine(start, end);
        return s -> {
            String[] lines = s.split("\n");
            if (lines.length <= 3) {
                throw new AssertionError("Not enough lines: " + s);
            }
            String kind = getKind(expectedDiagnostic.getKind());
            assertEquals(lines[0], kind);
            boolean found = false;
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].endsWith(expectedSource)) {
                    assertEquals(lines[i + 1], expectedMarkingLine, "Input: " + expectedSource + ", marking line: ");
                    found = true;
                }
            }
            if (!found) {
                throw new AssertionError("Did not find: " + expectedSource + " in: " + s);
            }
        };
    }

    private String createMarkingLine(int start, int end) {
        assertTrue(end >= start, String.format("End position %d is less than start position %d", end, start));
        StringBuilder sb = new StringBuilder();
        sb.append("|  ");
        for (int i = 0; i < start; ++i) {
            sb.append(' ');
        }
        sb.append('^');
        for (int i = 1; i < end - start - 1; ++i) {
            sb.append('-');
        }
        if (start < end) {
            sb.append('^');
        }
        return sb.toString();
    }

    public String getKind(Diagnostic.Kind kind) {
        switch (kind) {
            case WARNING:
                return "|  Warning:";
            case ERROR:
                return "|  Error:";
            default:
                throw new AssertionError("Unsupported kind: " + kind);
        }
    }
}
