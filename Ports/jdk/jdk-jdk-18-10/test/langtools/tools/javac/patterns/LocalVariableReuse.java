/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8260593
 * @summary Verify that a temporary storage variable is or is not used as needed when pattern matching.
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.file
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build combo.ComboTestHelper
 * @compile LocalVariableReuse.java
 * @run main LocalVariableReuse
 */

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask;
import combo.ComboTestHelper;
import java.io.IOException;
import javax.tools.JavaFileObject;
import toolbox.ToolBox;

public class LocalVariableReuse extends ComboInstance<LocalVariableReuse> {
    protected ToolBox tb;

    LocalVariableReuse() {
        super();
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<LocalVariableReuse>()
                .withDimension("CODE", (x, code) -> x.code = code, Code.values())
                .run(LocalVariableReuse::new);
    }

    private Code code;

    private static final String MAIN_TEMPLATE =
            """
            public class Test {
                #{CODE}
            }
            """;

    @Override
    protected void doWork() throws Throwable {
        ComboTask task = newCompilationTask()
                .withSourceFromTemplate(MAIN_TEMPLATE, pname -> switch (pname) {
                        case "CODE" -> code;
                        default -> throw new UnsupportedOperationException(pname);
                    });

        task.withOption("-printsource");
        task.generate(result -> {
            for (JavaFileObject out : result.get()) {
                try {
                    String actualDesugared = out.getCharContent(false).toString();
                    boolean hasTempVar = actualDesugared.contains("$temp");
                    if (hasTempVar != code.useTemporaryVariable) {
                        throw new AssertionError("Expected temporary variable: " + code.useTemporaryVariable +
                                                  ", but got: " + actualDesugared);
                    }
                } catch (IOException ex) {
                    throw new AssertionError(ex);
                }
            }
        });
    }

    public enum Code implements ComboParameter {
        LOCAL_VARIABLE(
                """
                private boolean test() {
                    Object o = null;
                    return o instanceof String s && s.length() > 0;
                }
                """, false),
        PARAMETER(
                """
                private boolean test(Object o) {
                    return o instanceof String s && s.length() > 0;
                }
                """, false),
        LAMBDA_PARAMETER(
                """
                private void test(Object o) {
                    I i = o -> o instanceof String s && s.length() > 0;
                interface I {
                    public boolean run(Object o);
                }
                """, false),
        EXCEPTION(
                """
                private boolean test() {
                    try {
                        throw new Exception();
                    } catch (Exception o) {
                        return o instanceof RuntimeException re && re.getMessage() != null;
                    }
                }
                """, false),
        RESOURCE(
                """
                private boolean test() throws Exception {
                    try (AutoCloseable r = null) {
                        return r instanceof java.io.InputStream in && in.read() != (-1);
                    } catch (Exception o) {
                    }
                }
                """, false),
        FIELD("""
              private Object o;
              private boolean test() {
                  return o instanceof String s && s.length() > 0;
              }
              """,
              true),
        FINAL_FIELD("""
              private final Object o;
              private boolean test() {
                  return o instanceof String s && s.length() > 0;
              }
              """,
              true),
        ARRAY_ACCESS("""
              private boolean test() {
                  Object[] o = null;
                  return o[0] instanceof String s && s.length() > 0;
              }
              """,
              true),
        METHOD_INVOCATION("""
              private boolean test() {
                  return get() instanceof String s && s.length() > 0;
              }
              private Object get() {
                  return null;
              }
              """,
              true),
        ;
        private final String body;
        private final boolean useTemporaryVariable;

        private Code(String body, boolean useTemporaryVariable) {
            this.body = body;
            this.useTemporaryVariable = useTemporaryVariable;
        }

        @Override
        public String expand(String optParameter) {
            return body;
        }
    }

}
