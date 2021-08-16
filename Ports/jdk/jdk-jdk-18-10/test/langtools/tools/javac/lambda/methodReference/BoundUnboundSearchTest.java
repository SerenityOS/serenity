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
 * @bug 8231461
 * @summary static/instance overload leads to 'unexpected static method found in unbound lookup' when resolving method reference
 * @library /lib/combo /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.util
 * @run testng BoundUnboundSearchTest
 */

import java.util.function.*;

import javax.tools.Diagnostic;

import com.sun.tools.javac.api.ClientCodeWrapper.DiagnosticSourceUnwrapper;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.JCDiagnostic;

import org.testng.annotations.Test;
import tools.javac.combo.CompilationTestCase;

import static org.testng.Assert.assertEquals;

@Test
public class BoundUnboundSearchTest extends CompilationTestCase {
    static final String TEMPLATE =
            """
            import java.util.function.*;
            class Test {
                #CANDIDATES
                void m() {
                    Function<String, String> f = Test::foo;
                }
            }
            """;

    public BoundUnboundSearchTest() {
        setDefaultFilename("Test.java");
        setCompileOptions(new String[]{"--debug=dumpMethodReferenceSearchResults"});
    }

    private Consumer<Diagnostic<?>> getDiagConsumer(final int boundCandidate, final int unboundCandidate) {
        return diagWrapper -> {
            JCDiagnostic diagnostic = ((DiagnosticSourceUnwrapper)diagWrapper).d;
            Object[] args = diagnostic.getArgs();
            if (args[0].toString().equals("bound")) {
                Assert.check(args[2].equals(boundCandidate));
            } else if (args[0].toString().equals("unbound")) {
                Assert.check(args[2].equals(unboundCandidate));
            }
        };
    }

    public void test() {
        assertOK(
            getDiagConsumer(0, -1),
                TEMPLATE.replaceFirst("#CANDIDATES",
                    """
                    public String foo(Object o) { return "foo"; }           // candidate 0
                    public static String foo(String o) { return "bar"; }    // candidate 1
                    """
            )
        );

        assertOK(
                getDiagConsumer(0, -1),
                TEMPLATE.replaceFirst("#CANDIDATES",
                    """
                    public static String foo(Object o) { return "foo"; }    // candidate 0
                    public static String foo(String o) { return "bar"; }    // candidate 0
                    """
                )
        );

        assertFail("compiler.err.prob.found.req",
                getDiagConsumer(0, -1),
                TEMPLATE.replaceFirst("#CANDIDATES",
                    """
                    public static String foo(Object o) { return "foo"; }    // candidate 0
                    public String foo(String o) { return "bar"; }           // candidate 1
                    """
                )
        );

        assertFail("compiler.err.prob.found.req",
                getDiagConsumer(0, -1),
                TEMPLATE.replaceFirst("#CANDIDATES",
                    """
                    public String foo(Object o) { return "foo"; }           // candidate 0
                    public String foo(String o) { return "bar"; }           // candidate 1
                    """
                )
        );

        assertFail("compiler.err.invalid.mref",
                getDiagConsumer(-1, -1),
                """
                import java.util.function.*;

                public class Test {
                    public String foo(Object o) { return "foo"; }
                    public static String foo(String o) { return "bar"; }

                    public void test() {
                        // method bar doesn't exist
                        Function<String, String> f = Test::bar;
                    }
                }
                """
        );
    }
}
