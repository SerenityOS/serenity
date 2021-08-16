/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8234922
 * @summary Verify proper scope of binding related to loops and breaks.
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.file
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build combo.ComboTestHelper
 * @compile BreakAndLoops.java
 * @run main BreakAndLoops
 */

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask;
import combo.ComboTestHelper;
import java.nio.file.Path;
import java.nio.file.Paths;
import toolbox.ToolBox;

public class BreakAndLoops extends ComboInstance<BreakAndLoops> {
    protected ToolBox tb;

    BreakAndLoops() {
        super();
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<BreakAndLoops>()
                .withDimension("OUTTER_LABEL", (x, outterLabel) -> x.outterLabel = outterLabel, OutterLabel.values())
                .withDimension("OUTTER_LOOP", (x, outterLoop) -> x.outterLoop = outterLoop, OutterLoop.values())
                .withDimension("MAIN_LOOP", (x, mainLoop) -> x.mainLoop = mainLoop, MainLoop.values())
                .withDimension("INNER_LABEL", (x, innerLabel) -> x.innerLabel = innerLabel, Label.values())
                .withDimension("INNER_LOOP", (x, innerLoop) -> x.innerLoop = innerLoop, Loop.values())
                .withDimension("BREAK", (x, brk) -> x.brk = brk, Break.values())
                .withFilter(bal -> bal.outterLabel != OutterLabel.LABEL || bal.innerLabel != Label.LABEL)
                .run(BreakAndLoops::new);
    }

    private OutterLabel outterLabel;
    private OutterLoop outterLoop;
    private MainLoop mainLoop;
    private Label innerLabel;
    private Loop innerLoop;
    private Break brk;

    private static final String MAIN_TEMPLATE =
            """
            public class Test {
                public static void doTest(Object o, int i, Object[] arr) {
                    #{OUTTER_LABEL}
                }
            }
            """;

    @Override
    protected void doWork() throws Throwable {
        Path base = Paths.get(".");

        ComboTask task = newCompilationTask()
                .withSourceFromTemplate(MAIN_TEMPLATE, pname -> switch (pname) {
                        case "OUTTER_LABEL" -> outterLabel;
                        case "OUTTER_LOOP" -> outterLoop;
                        case "MAIN_LOOP" -> mainLoop;
                        case "NESTED_LOOP" -> innerLoop;
                        case "NESTED" -> brk;
                        case "BODY" -> innerLabel;
                        default -> throw new UnsupportedOperationException(pname);
                    });

        task.generate(result -> {
            boolean shouldPass;
            if (brk == Break.NONE) {
                shouldPass = true;
            } else if (innerLabel == Label.LABEL && brk == Break.BREAK_LABEL) {
                shouldPass = true;
            } else if (innerLoop.supportsAnonymousBreak && brk == Break.BREAK) {
                shouldPass = true;
            } else {
                shouldPass = false;
            }
            if (!(shouldPass ^ result.hasErrors())) {
                throw new AssertionError("Unexpected result: " + result.compilationInfo());
            }
        });
    }

    public enum MainLoop implements ComboParameter {
        WHILE("""
              while (!(o instanceof String s)) {
                  #{BODY}
              }
              """),
        FOR("""
            for( ; !(o instanceof String s) ; ) {
                #{BODY}
            }
            """),
        DO_WHILE("""
                 do {
                     #{BODY}
                 } while (!(o instanceof String s));
                 """);
        private final String body;

        private MainLoop(String body) {
            this.body = body;
        }

        @Override
        public String expand(String optParameter) {
            return body;
        }
    }

    public enum OutterLabel implements ComboParameter {
        NONE("#{OUTTER_LOOP}"),
        LABEL("LABEL: #{OUTTER_LOOP}");
        private final String code;

        private OutterLabel(String code) {
            this.code = code;
        }

        @Override
        public String expand(String optParameter) {
            return code;
        }
    }

    public enum OutterLoop implements ComboParameter {
        NONE("#{MAIN_LOOP} System.err.println(s);"),
        BLOCK("{ #{MAIN_LOOP} System.err.println(s); }"),
        WHILE("while (i-- > 0) { #{MAIN_LOOP} System.err.println(s); }"),
        FOR("for ( ; i-- > 0; ) { #{MAIN_LOOP} System.err.println(s); }"),
        FOR_EACH("for (Object outterO : arr) { #{MAIN_LOOP} System.err.println(s); }"),
        DO_WHILE("do { #{MAIN_LOOP} System.err.println(s); } while (i-- > 0);");
        private final String code;

        private OutterLoop(String code) {
            this.code = code;
        }

        @Override
        public String expand(String optParameter) {
            return code;
        }
    }

    public enum Label implements ComboParameter {
        NONE("#{NESTED_LOOP}"),
        LABEL("LABEL: #{NESTED_LOOP}");
        private final String code;

        private Label(String code) {
            this.code = code;
        }

        @Override
        public String expand(String optParameter) {
            return code;
        }
    }

    public enum Loop implements ComboParameter {
        NONE("#{NESTED}", false),
        BLOCK("{ #{NESTED} }", false),
        WHILE("while (i-- > 0) { #{NESTED} }", true),
        FOR("for ( ; i-- > 0; ) { #{NESTED} }", true),
        FOR_EACH("for (Object innerO : arr) { #{NESTED} }", true),
        DO_WHILE("do { #{NESTED} } while (i-- > 0);", true);
        private final String code;
        private final boolean supportsAnonymousBreak;

        private Loop(String code, boolean supportsAnonymousBreak) {
            this.code = code;
            this.supportsAnonymousBreak = supportsAnonymousBreak;
        }

        @Override
        public String expand(String optParameter) {
            return code;
        }
    }

    public enum Break implements ComboParameter {
        NONE(";"),
        BREAK("break;"),
        BREAK_LABEL("break LABEL;");
        private final String code;

        private Break(String code) {
            this.code = code;
        }

        @Override
        public String expand(String optParameter) {
            return code;
        }
    }
}