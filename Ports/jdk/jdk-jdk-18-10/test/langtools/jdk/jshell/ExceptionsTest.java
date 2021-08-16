/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests for exceptions
 * @bug 8198801 8212167 8210527
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build KullaTesting TestingInputStream Compiler
 * @run testng ExceptionsTest
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import jdk.jshell.EvalException;
import jdk.jshell.JShellException;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.UnresolvedReferenceException;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Test
public class ExceptionsTest extends KullaTesting {

    private final Compiler compiler = new Compiler();
    private final Path outDir = Paths.get("test_class_path");

    public void throwUncheckedException() {
        String message = "error_message";
        SnippetEvent cr = assertEvalException("throw new RuntimeException(\"" + message + "\");");
        assertExceptionMatch(cr,
                new ExceptionInfo(RuntimeException.class, message,
                        newStackTraceElement("", "", cr.snippet(), 1)));
    }

    public void throwCheckedException() {
        String message = "error_message";
        SnippetEvent cr = assertEvalException("throw new Exception(\"" + message + "\");");
        assertExceptionMatch(cr,
                new ExceptionInfo(Exception.class, message,
                        newStackTraceElement("", "", cr.snippet(), 1)));
    }

    public void throwFromStaticMethodOfClass() {
        String message = "error_message";
        Snippet s1 = methodKey(assertEval("void f() { throw new RuntimeException(\"" + message + "\"); }"));
        Snippet s2 = classKey(assertEval("class A { static void g() { f(); } }"));
        SnippetEvent cr3 = assertEvalException("A.g();");
        assertExceptionMatch(cr3,
                new ExceptionInfo(RuntimeException.class, message,
                        newStackTraceElement("", "f", s1, 1),
                        newStackTraceElement("A", "g", s2, 1),
                        newStackTraceElement("", "", cr3.snippet(), 1)));
    }

    public void throwFromStaticMethodOfInterface() {
        String message = "error_message";
        Snippet s1 = methodKey(assertEval("void f() { throw new RuntimeException(\"" + message + "\"); }"));
        Snippet s2 = classKey(assertEval("interface A { static void g() { f(); } }"));
        SnippetEvent cr3 = assertEvalException("A.g();");
        assertExceptionMatch(cr3,
                new ExceptionInfo(RuntimeException.class, message,
                        newStackTraceElement("", "f", s1, 1),
                        newStackTraceElement("A", "g", s2, 1),
                        newStackTraceElement("", "", cr3.snippet(), 1)));
    }

    public void throwChained() {
        String message1 = "error_message1";
        String message2 = "error_message2";
        Snippet s1 = methodKey(assertEval("void p() throws Exception { ((String) null).toString(); }"));
        Snippet s2 = methodKey(assertEval("void n() throws Exception { try { p(); } catch (Exception ex) { throw new java.io.IOException(\"" + message2 + "\", ex); }}"));
        Snippet s3 = methodKey(assertEval("void m() {\n"
                + "try { n(); }\n"
                + "catch (Exception ex) {\n"
                + "    throw new RuntimeException(\"" + message1 + "\", ex);\n"
                + "}}"));
        SnippetEvent cr4 = assertEvalException("m();");
        assertExceptionMatch(cr4,
                new ExceptionInfo(RuntimeException.class, message1,
                        new ExceptionInfo(IOException.class, message2,
                                new ExceptionInfo(NullPointerException.class, null,
                                        newStackTraceElement("", "p", s1, 1),
                                        newStackTraceElement("", "n", s2, 1),
                                        newStackTraceElement("", "m", s3, 2),
                                        newStackTraceElement("", "", cr4.snippet(), 1)),
                                newStackTraceElement("", "n", s2, 1),
                                newStackTraceElement("", "m", s3, 2),
                                newStackTraceElement("", "", cr4.snippet(), 1)),
                        newStackTraceElement("", "m", s3, 4),
                        newStackTraceElement("", "", cr4.snippet(), 1)));
    }

    public void throwChainedUnresolved() {
        String message1 = "error_message1";
        String message2 = "error_message2";
        Snippet s1 = methodKey(assertEval("void p() throws Exception { ((String) null).toString(); }"));
        Snippet s2 = methodKey(assertEval("void n() throws Exception { try { p(); } catch (Exception ex) { throw new java.io.IOException(\"" + message2 + "\", ex); }}"));
        Snippet s3 = methodKey(assertEval("void m() {\n"
                + "try { n(); }\n"
                + "catch (Exception ex) {\n"
                + "    throw new RuntimeException(\"" + message1 + "\", ex);\n"
                + "}}"));
        getState().drop(s1);
        SnippetEvent cr4 = assertEvalException("m();");
        assertExceptionMatch(cr4,
                new ExceptionInfo(RuntimeException.class, message1,
                        new UnresolvedExceptionInfo(s2,
                                newStackTraceElement("", "n", s2, 1),
                                newStackTraceElement("", "m", s3, 2),
                                newStackTraceElement("", "", cr4.snippet(), 1)),
                        newStackTraceElement("", "m", s3, 4),
                        newStackTraceElement("", "", cr4.snippet(), 1)));
    }

    public void throwFromConstructor() {
        String message = "error_message";
        Snippet s1 = methodKey(assertEval("void f() { throw new RuntimeException(\"" + message + "\"); }"));
        Snippet s2 = classKey(assertEval("class A { A() { f(); } }"));
        SnippetEvent cr3 = assertEvalException("new A();");
        assertExceptionMatch(cr3,
                new ExceptionInfo(RuntimeException.class, message,
                        newStackTraceElement("", "f", s1, 1),
                        newStackTraceElement("A", "<init>", s2, 1),
                        newStackTraceElement("", "", cr3.snippet(), 1)));
    }

    public void throwFromDefaultMethodOfInterface() {
        String message = "error_message";
        Snippet s1 = methodKey(assertEval("void f() { throw new RuntimeException(\"" + message + "\"); }"));
        Snippet s2 = classKey(assertEval("interface A { default void g() { f(); } }"));
        SnippetEvent cr3 = assertEvalException("new A() { }.g();");
        assertExceptionMatch(cr3,
                new ExceptionInfo(RuntimeException.class, message,
                        newStackTraceElement("", "f", s1, 1),
                        newStackTraceElement("A", "g", s2, 1),
                        newStackTraceElement("", "", cr3.snippet(), 1)));
    }

    public void throwFromLambda() {
        String message = "lambda";
        Snippet s1 = varKey(assertEval(
                "Runnable run = () -> {\n" +
                "   throw new RuntimeException(\"" + message + "\");\n" +
                "};"
        ));
        SnippetEvent cr2 = assertEvalException("run.run();");
        assertExceptionMatch(cr2,
                new ExceptionInfo(RuntimeException.class, message,
                        newStackTraceElement("", "lambda$", s1, 2),
                        newStackTraceElement("", "", cr2.snippet(), 1)));
    }

    public void throwFromAnonymousClass() {
        String message = "anonymous";
        Snippet s1 = varKey(assertEval(
                "Runnable run = new Runnable() {\n" +
                "   public void run() {\n"+
                "       throw new RuntimeException(\"" + message + "\");\n" +
                "   }\n" +
                "};"
        ));
        SnippetEvent cr2 = assertEvalException("run.run();");
        assertExceptionMatch(cr2,
                new ExceptionInfo(RuntimeException.class, message,
                        newStackTraceElement("1", "run", s1, 3),
                        newStackTraceElement("", "", cr2.snippet(), 1)));
    }

    public void throwFromLocalClass() {
        String message = "local";
        Snippet s1 = methodKey(assertEval(
                "void f() {\n" +
                "   class A {\n" +
                "       void f() {\n"+
                "           throw new RuntimeException(\"" + message + "\");\n" +
                "       }\n" +
                "   }\n" +
                "   new A().f();\n" +
                "}"
        ));
        SnippetEvent cr2 = assertEvalException("f();");
        assertExceptionMatch(cr2,
                new ExceptionInfo(RuntimeException.class, message,
                        newStackTraceElement("1A", "f", s1, 4),
                        newStackTraceElement("", "f", s1, 7),
                        newStackTraceElement("", "", cr2.snippet(), 1)));
    }

    // test 8210527
    public void throwFromWithoutSource() {
        String message = "show this";
        SnippetEvent se = assertEvalException("java.lang.reflect.Proxy.newProxyInstance(" +
                "Thread.currentThread().getContextClassLoader(), new Class[] {}," +
                "(p, m, a) -> { throw new IllegalStateException(\"" + message + "\"); }).hashCode()");
        assertExceptionMatch(se,
                new ExceptionInfo(IllegalStateException.class, message,
                        newStackTraceElement("", "lambda$do_it$$0", se.snippet(), 1),
                        new StackTraceElement("jdk.proxy1.$Proxy0", "hashCode", null, -1),
                        newStackTraceElement("", "", se.snippet(), 1)));
    }

    // test 8210527
    public void throwFromNoSource() {
        Path path = outDir.resolve("fail");
        compiler.compile(path,
                "package fail;\n" +
                        "public class Fail {\n" +
                        "  static { int x = 1 / 0; }\n" +
                        "}\n");
        addToClasspath(compiler.getPath(path));
        SnippetEvent se = assertEvalException("Class.forName(\"fail.Fail\")");
        assertExceptionMatch(se,
                new ExceptionInfo(ExceptionInInitializerError.class, null,
                        new StackTraceElement("java.lang.Class", "forName0",  "Class.java", -2),
                        new StackTraceElement("java.lang.Class", "forName", "Class.java", -2),
                        newStackTraceElement("", "", se.snippet(), 1)));
    }

    // test 8212167
    public void throwLineFormat1() {
        SnippetEvent se = assertEvalException(
                "if (true) { \n" +
                        "   int x = 10; \n" +
                        "   int y = 10 / 0;}"
        );
        assertExceptionMatch(se,
                new ExceptionInfo(ArithmeticException.class, "/ by zero",
                        newStackTraceElement("", "", se.snippet(), 3)));
    }

    public void throwLineFormat3() {
        Snippet sp = methodKey(assertEval(
                "int p() \n" +
                        "  { return 4/0; }"));
        Snippet sm = methodKey(assertEval(
                "int m(int x)\n" +
                        "       \n" +
                        "       {\n" +
                        "          return p() + x; \n" +
                        "       }"));
        Snippet sn = methodKey(assertEval(
                "int n(int x) {\n" +
                        "         try {\n" +
                        "           return m(x);\n" +
                        "         }\n" +
                        "         catch (Throwable ex) {\n" +
                        "           throw new IllegalArgumentException( \"GOT:\", ex);\n" +
                        "         }\n" +
                        "       }"));
        SnippetEvent se = assertEvalException("n(33);");
        assertExceptionMatch(se,
                new ExceptionInfo(IllegalArgumentException.class, null,
                        new ExceptionInfo(ArithmeticException.class, "/ by zero",
                                newStackTraceElement("", "p", sp, 2),
                                newStackTraceElement("", "m", sm, 4),
                                newStackTraceElement("", "n", sn, 3),
                                newStackTraceElement("", "", se.snippet(), 1)),
                        newStackTraceElement("", "n", sn, 6),
                        newStackTraceElement("", "", se.snippet(), 1)));
    }

    @Test(enabled = false) // TODO 8129427
    public void outOfMemory() {
        assertEval("import java.util.*;");
        assertEval("List<byte[]> list = new ArrayList<>();");
        assertExecuteException("while (true) { list.add(new byte[10000]); }", OutOfMemoryError.class);
    }

    public void stackOverflow() {
        assertEval("void f() { f(); }");
        assertExecuteException("f();", StackOverflowError.class);
    }

    private StackTraceElement newStackTraceElement(String className, String methodName, Snippet key, int lineNumber) {
        return new StackTraceElement(className, methodName, "#" + key.id(), lineNumber);
    }

    private static class AnyExceptionInfo {

        public final StackTraceElement[] stackTraceElements;

        public AnyExceptionInfo(StackTraceElement... stackTraceElements) {
            this.stackTraceElements = stackTraceElements.length == 0 ? null : stackTraceElements;
        }
    }

    private static class UnresolvedExceptionInfo extends AnyExceptionInfo {

        public final Snippet sn;

        public UnresolvedExceptionInfo(Snippet sn, StackTraceElement... stackTraceElements) {
            super(stackTraceElements);
            this.sn = sn;
        }
    }

    private static class ExceptionInfo extends AnyExceptionInfo {

        public final Class<? extends Throwable> exception;
        public final String message;
        public final AnyExceptionInfo cause;

        public ExceptionInfo(Class<? extends Throwable> exception, String message,
                StackTraceElement... stackTraceElements) {
            this(exception, message, null, stackTraceElements);
        }

        public ExceptionInfo(Class<? extends Throwable> exception, String message,
                AnyExceptionInfo cause, StackTraceElement... stackTraceElements) {
            super(stackTraceElements);
            this.exception = exception;
            this.message = message;
            this.cause = cause;
        }
    }

    private void assertExecuteException(String input, Class<? extends Throwable> exception) {
        assertExceptionMatch(assertEvalException(input), new ExceptionInfo(exception, null));
    }

    private void assertExceptionMatch(SnippetEvent cr, ExceptionInfo exceptionInfo) {
        assertExceptionMatch(cr.exception(), cr.snippet().source(), exceptionInfo);
    }

    private void assertExceptionMatch(Throwable exception, String source, ExceptionInfo exceptionInfo) {
        assertNotNull(exception, "Expected exception was not thrown: " + exceptionInfo.exception);
        if (exception instanceof EvalException) {
            EvalException ex = (EvalException) exception;
            String actualException = ex.getExceptionClassName();
            String expectedException = exceptionInfo.exception.getCanonicalName();
            assertEquals(actualException, expectedException,
                    String.format("Given \"%s\" expected exception: %s, got: %s%nStack trace:%n%s",
                            source, expectedException, actualException, getStackTrace(ex)));
            if (exceptionInfo.message != null) {
                assertEquals(ex.getMessage(), exceptionInfo.message,
                        String.format("Given \"%s\" expected message: %s, got: %s",
                                source, exceptionInfo.message, ex.getMessage()));
            }
            assertStackMatch(ex, source, exceptionInfo);
            if (exceptionInfo.cause != null) {
                assertAnyExceptionMatch(exception.getCause(), exceptionInfo.cause);
            }
        } else {
            fail("Unexpected exception: " + exception + " or exceptionInfo: " + exceptionInfo);
        }
    }

    private void assertStackMatch(JShellException exception, String source, AnyExceptionInfo exceptionInfo) {
        if (exceptionInfo.stackTraceElements != null) {
            assertStackTrace(exception.getStackTrace(), exceptionInfo.stackTraceElements,
                    String.format("Given \"%s\"%nStack trace:%n%s%n",
                            source, getStackTrace(exception)));
        }
    }

    private void assertAnyExceptionMatch(Throwable exception, AnyExceptionInfo exceptionInfo) {
        if (exceptionInfo instanceof ExceptionInfo) {
            assertExceptionMatch(exception, "", (ExceptionInfo) exceptionInfo);
        } else {
            assertTrue(exceptionInfo instanceof UnresolvedExceptionInfo, "Bad exceptionInfo: " + exceptionInfo);
            assertTrue(exception instanceof UnresolvedReferenceException,
                    "Expected UnresolvedReferenceException: " + exception);
            UnresolvedExceptionInfo uei = (UnresolvedExceptionInfo) exceptionInfo;
            UnresolvedReferenceException ure = (UnresolvedReferenceException) exception;
            assertEquals(ure.getSnippet(), uei.sn);
            assertStackMatch(ure, "", exceptionInfo);
        }
    }

    private void assertStackTrace(StackTraceElement[] actual, StackTraceElement[] expected, String message) {
        if (actual != expected) {
            if (actual == null || expected == null) {
                fail(message);
            } else {
                assertEquals(actual.length, expected.length, message + " : arrays do not have the same size");
                for (int i = 0; i < actual.length; ++i) {
                    StackTraceElement actualElement = actual[i];
                    StackTraceElement expectedElement = expected[i];
                    assertEquals(actualElement.getClassName(), expectedElement.getClassName(), message + " : class names [" + i + "]");
                    String expectedMethodName = expectedElement.getMethodName();
                    if (expectedMethodName.startsWith("lambda$")) {
                        assertTrue(actualElement.getMethodName().startsWith("lambda$"), message + " : method names");
                    } else {
                        assertEquals(actualElement.getMethodName(), expectedElement.getMethodName(), message + " : method names [" + i + "]");
                    }
                    assertEquals(actualElement.getFileName(), expectedElement.getFileName(), message + " : file names [" + i + "]");
                    if (expectedElement.getLineNumber() >= 0) {
                        assertEquals(actualElement.getLineNumber(), expectedElement.getLineNumber(), message + " : line numbers [" + i + "]"
                                + " -- actual: " + actualElement.getLineNumber() + ", expected: " + expectedElement.getLineNumber() +
                                " -- in: " + actualElement.getClassName());
                    }
                }
            }
        }
    }

    private String getStackTrace(Throwable ex) {
        StringWriter st = new StringWriter();
        ex.printStackTrace(new PrintWriter(st));
        return st.toString();
    }
}
