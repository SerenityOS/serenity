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
 * @bug 8235564
 * @summary Verify that passing member references to a method not accepting
 *          functional interface does not crash the compiler.
 * @library /tools/lib /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build combo.ComboTestHelper
 * @compile T8235564.java
 * @run main T8235564
 */

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask;
import combo.ComboTestHelper;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import toolbox.ToolBox;

public class T8235564 extends ComboInstance<T8235564> {
    protected ToolBox tb;

    T8235564() {
        super();
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<T8235564>()
                .withDimension("INVOCATION", (x, invocation) -> x.invocation = invocation, Invocation.values())
                .withDimension("PARAM", (x, param) -> x.param = param, Parameter.values())
                .run(T8235564::new);
    }

    private Invocation invocation;
    private Parameter param;

    private static final String MAIN_TEMPLATE =
            """
            public class Test {
                static void test() {
                    Runnable r = () -> {};
                    #{INVOCATION};
                }
                private static void existingWithFunctional(Runnable r) {}
                private static void existingWithoutFunctional(String parameter) {}
            }
            """;

    @Override
    protected void doWork() throws Throwable {
        StringWriter out = new StringWriter();

        ComboTask task = newCompilationTask()
                .withSourceFromTemplate(MAIN_TEMPLATE, pname -> switch (pname) {
                        case "INVOCATION" -> invocation;
                        case "PARAM" -> param;
                        default -> throw new UnsupportedOperationException(pname);
                    })
                .withOption("-XDshould-stop.at=FLOW")
                .withOption("-XDrawDiagnostics")
                .withOption("-XDdev")
                .withWriter(out);

        task.analyze(result -> {
            List<String> diags = result.diagnosticsForKind(Diagnostic.Kind.ERROR)
                                       .stream()
                                       .map(d -> d.getLineNumber() + ":" + d.getCode())
                                       .collect(Collectors.toList());
            List<String> expected = new ArrayList<>();
            switch (param) {
                case VALID_VARIABLE, VALID_LAMBDA, VALID_MEMBER_REF -> {}
                case UNDEFINED_VARIABLE ->
                    expected.add("4:compiler.err.cant.resolve.location");
                case UNDEFINED_METHOD, UNDEFINED_LAMBDA ->
                    expected.add("4:compiler.err.cant.resolve.location.args");
                case UNDEFINED_MEMBER_REF ->
                    expected.add("4:compiler.err.invalid.mref");
                case UNDEFINED_CONDEXPR -> {
                    if (invocation != Invocation.EXISTING_WITHOUT_FUNCTIONAL) {
                        expected.add("4:compiler.err.invalid.mref");
                        expected.add("4:compiler.err.invalid.mref");
                    }
                }
            }
            switch (invocation) {
                case EXISTING_WITH_FUNCTIONAL -> {
                    if (param == Parameter.UNDEFINED_CONDEXPR) {
                        expected.add("4:compiler.err.cant.apply.symbol");
                    }
                }
                case EXISTING_WITHOUT_FUNCTIONAL -> {
                    if (param != Parameter.UNDEFINED_VARIABLE &&
                        param != Parameter.UNDEFINED_MEMBER_REF &&
                        param != Parameter.UNDEFINED_METHOD) {
                        expected.add("4:compiler.err.cant.apply.symbol");
                    }
                }
                case UNDEFINED -> {
                    if (param != Parameter.UNDEFINED_VARIABLE &&
                        param != Parameter.UNDEFINED_MEMBER_REF &&
                        param != Parameter.UNDEFINED_METHOD) {
                        expected.add("4:compiler.err.cant.resolve.location.args");
                    }
                }
            }
            if (!expected.equals(diags)) {
                throw new AssertionError("Expected errors not found, expected: " + expected + ", actual: " + diags);
            }
            if (out.toString().length() > 0) {
                throw new AssertionError("No output expected, but got:\n" + out + "\n\n" + result.compilationInfo());
            }
        });
    }

    public enum Invocation implements ComboParameter {
        EXISTING_WITH_FUNCTIONAL("existingWithFunctional(#{PARAM})"),
        EXISTING_WITHOUT_FUNCTIONAL("existingWithoutFunctional(#{PARAM})"),
        UNDEFINED("undefined(#{PARAM})");
        private final String invocation;

        private Invocation(String invocation) {
            this.invocation = invocation;
        }

        @Override
        public String expand(String optParameter) {
            return invocation;
        }
    }

    public enum Parameter implements ComboParameter {
        VALID_VARIABLE("r"),
        VALID_LAMBDA("() -> {}"),
        VALID_MEMBER_REF("Test::test"),
        UNDEFINED_VARIABLE("undefined"),
        UNDEFINED_LAMBDA("() -> {undefined();}"),
        UNDEFINED_MEMBER_REF("Test::undefined"),
        UNDEFINED_METHOD("undefined()"),
        UNDEFINED_CONDEXPR("1 == 2 ? Test::undefined : Test::undefined");
        private final String code;

        private Parameter(String code) {
            this.code = code;
        }

        @Override
        public String expand(String optParameter) {
            return code;
        }
    }
}
