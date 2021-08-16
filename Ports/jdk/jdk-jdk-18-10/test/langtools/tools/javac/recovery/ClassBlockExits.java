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
 * @bug 8243047
 * @summary javac should not crash while processing exits in class initializers in Flow
 * @library /tools/lib /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build combo.ComboTestHelper
 * @compile ClassBlockExits.java
 * @run main ClassBlockExits
 */

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask;
import combo.ComboTestHelper;
import java.io.StringWriter;
import toolbox.ToolBox;

public class ClassBlockExits extends ComboInstance<ClassBlockExits> {
    protected ToolBox tb;

    ClassBlockExits() {
        super();
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<ClassBlockExits>()
                .withDimension("BLOCK", (x, block) -> x.block = block, Block.values())
                .withDimension("EXIT", (x, exit) -> x.exit = exit, Exit.values())
                .run(ClassBlockExits::new);
    }

    private Block block;
    private Exit exit;

    private static final String MAIN_TEMPLATE =
            """
            public class Test {
                #{BLOCK}
                void t() {}
            }
            """;

    @Override
    protected void doWork() throws Throwable {
        StringWriter out = new StringWriter();

        ComboTask task = newCompilationTask()
                .withSourceFromTemplate(MAIN_TEMPLATE, pname -> switch (pname) {
                        case "BLOCK" -> block;
                        case "BODY" -> exit;
                        default -> throw new UnsupportedOperationException(pname);
                    })
                .withOption("-XDshould-stop.at=FLOW")
                .withOption("-XDdev")
                .withWriter(out);

        task.analyze(result -> {
            if (out.toString().length() > 0) {
                throw new AssertionError("No output expected, but got" + out + "\n\n" + result.compilationInfo());
            }
        });
    }

    public enum Block implements ComboParameter {
        STATIC("""
               static {
                   #{BODY}
               }
               """),
        INSTANCE("""
                 {
                     #{BODY}
                 }
                 """),
        STATIC_INITIALIZER("""
                    private static int i = switch (0) { default: #{BODY} case 0: yield 0; };
                    """),
        INITIALIZER("""
                    private int i = switch (0) { default: #{BODY} case 0: yield 0; };
                    """);
        private final String block;

        private Block(String block) {
            this.block = block;
        }

        @Override
        public String expand(String optParameter) {
            return block;
        }
    }

    public enum Exit implements ComboParameter {
        RETURN("return;"),
        RETURN_VALUE("return null;"),
        BREAK("break;"),
        BREAK_LABEL("break LABEL;"),
        CONTINUE("continue;"),
        CONTINUE_LABEL("continue LABEL;"),
        YIELD("yield 0;");
        private final String code;

        private Exit(String code) {
            this.code = code;
        }

        @Override
        public String expand(String optParameter) {
            return code;
        }
    }
}