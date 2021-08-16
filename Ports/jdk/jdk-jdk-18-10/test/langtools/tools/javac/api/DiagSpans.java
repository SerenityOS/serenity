/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8243557
 * @summary Verify spans of diagnostics
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.JavacTask toolbox.TestRunner toolbox.ToolBox
 * @run main DiagSpans
 */

import java.io.IOException;
import java.util.List;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import toolbox.TestRunner;
import toolbox.ToolBox;

public class DiagSpans extends TestRunner {

    public static void main(String... args) throws Exception {
        DiagSpans t = new DiagSpans();
        t.runTests();
    }

    private final ToolBox tb = new ToolBox();

    DiagSpans() throws IOException {
        super(System.err);
    }

    @Test
    public void testCannotBeThrownMultiple() throws Exception {
        runDiagSpanTest("""
                        class Test {
                            public void exception() {
                                try {
                                } catch (RuntimeException | /^ReflectiveOperationException/ ex) {
                                }
                            }
                        }
                        """,
                        '/',
                        '^');
    }

    @Test
    public void testCannotBeThrownMultiplePrefered() throws Exception {
        runDiagSpanTest("""
                        class Test {
                            public void exception() {
                                try {
                                } catch (RuntimeException | /java.lang^.ReflectiveOperationException/ ex) {
                                }
                            }
                        }
                        """,
                        '/',
                        '^');
    }

    @Test
    public void testCannotBeThrownSingle() throws Exception {
        runDiagSpanTest("""
                        class Test {
                            public void exception() {
                                try {
                                } /^catch (ReflectiveOperationException ex) {
                                }/
                            }
                        }
                        """,
                        '/',
                        '^');
    }

    @Test
    public void testAlreadyCaughtMultiple() throws Exception {
        runDiagSpanTest("""
                        class Test {
                            public void exception() {
                                try {
                                } catch (IllegalStateException ex) {
                                } catch (IllegalArgumentException | /^IllegalStateException/ ex) {
                                }
                            }
                        }
                        """,
                        '/',
                        '^');
    }

    @Test
    public void testAlreadyCaughtSimple() throws Exception {
        runDiagSpanTest("""
                        class Test {
                            public void exception() {
                                try {
                                } catch (IllegalStateException ex) {
                                } /^catch (IllegalStateException ex) {
                                }/
                            }
                        }
                        """,
                        '/',
                        '^');
    }

    @Test
    public void testUnreachableCatchMulti() throws Exception {
        runDiagSpanTest("""
                        class Test {
                            public void exception(boolean b) {
                                try {
                                    if (b)
                                        throw new Sub1();
                                    else
                                        throw new Sub2();
                                } catch(Sub1 exc) {
                                } catch(Sub2 exc) {
                                } catch(IllegalStateException | /^Base1/ | /^Base2/ exc) { }
                            }
                        }
                        class Base1 extends Exception {}
                        class Sub1 extends Base1 {}
                        class Base2 extends Exception {}
                        class Sub2 extends Base2 {}
                        """,
                        '/',
                        '^');
    }

    @Test
    public void testSubtypeMulti1() throws Exception {
        runDiagSpanTest("""
                        class Test {
                            public void exception(boolean b) {
                                try {
                                    throw new Sub1();
                                } catch(Base1 | /^Sub1/ exc) { }
                            }
                        }
                        class Base1 extends Exception {}
                        class Sub1 extends Base1 {}
                        """,
                        '/',
                        '^');
    }

    @Test
    public void testSubtypeMulti2() throws Exception {
        runDiagSpanTest("""
                        class Test {
                            public void exception(boolean b) {
                                try {
                                    throw new Sub1();
                                } catch(Sub1 | /^Base1/ exc) { }
                            }
                        }
                        class Base1 extends Exception {}
                        class Sub1 extends Base1 {}
                        """,
                        '/',
                        '^');
    }

    private void runDiagSpanTest(String code, char spanMarker, char prefMarker) throws Exception {
        var realCode = new StringBuilder();
        var expectedError = new ArrayList<String>();
        int startPos = -1;
        int preferedPos = -1;
        int pos = 0;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == spanMarker) {
                if (startPos == (-1)) {
                    startPos = pos;
                } else {
                    expectedError.add("" + startPos + ":" + pos + ":" + preferedPos);
                    startPos = (-1);
                    preferedPos = (-1);
                }
            } else if (c == prefMarker) {
                if (preferedPos == (-1)) {
                    preferedPos = pos;
                } else {
                    throw new AssertionError("Too many markers!");
                }
            } else {
                realCode.append(c);
                pos++;
            }
        }

        if (startPos != (-1) || preferedPos != (-1)) {
            throw new AssertionError("Incorrect number of markers!");
        }

        var compiler = ToolProvider.getSystemJavaCompiler();
        var actualErrors = new ArrayList<String>();
        DiagnosticListener<JavaFileObject> dl = d -> {
            System.err.println("d=" + d);
            actualErrors.add(""  + d.getStartPosition() +
                             ":" + d.getEndPosition() +
                             ":" + d.getPosition());
        };
        var sourceFiles = List.of(new JFOImpl(realCode.toString()));
        var task = compiler.getTask(null, null, dl, null, null, sourceFiles);
        task.call();
        if (!Objects.equals(expectedError, actualErrors)) {
            throw new AssertionError("Expected error spans not found, expected: " +
                                     expectedError + ", actual: " + actualErrors);
        }
    }

    class JFOImpl extends SimpleJavaFileObject {

        private final String code;

        public JFOImpl(String code) throws URISyntaxException {
            super(new URI("mem://Test.java"), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return code;
        }
    }
}
