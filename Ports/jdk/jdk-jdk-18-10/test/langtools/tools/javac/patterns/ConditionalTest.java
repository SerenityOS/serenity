/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8236670
 * @summary Verify proper scope of binding related to loops and breaks.
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.file
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build combo.ComboTestHelper
 * @compile ConditionalTest.java
 * @run main ConditionalTest
 */

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask;
import combo.ComboTestHelper;
import java.nio.file.Path;
import java.nio.file.Paths;
import toolbox.ToolBox;

public class ConditionalTest extends ComboInstance<ConditionalTest> {
    protected ToolBox tb;

    ConditionalTest() {
        super();
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<ConditionalTest>()
                .withDimension("COND", (x, cond) -> x.cond = cond, Pattern.values())
                .withDimension("TRUE", (x, trueSec) -> x.trueSec = trueSec, Pattern.values())
                .withDimension("FALSE", (x, falseSec) -> x.falseSec = falseSec, Pattern.values())
                .run(ConditionalTest::new);
    }

    private Pattern cond;
    private Pattern trueSec;
    private Pattern falseSec;

    private static final String MAIN_TEMPLATE =
            """
            public class Test {
                public static boolean doTest(Object o, boolean b) {
                    return #{COND} ? #{TRUE} : #{FALSE}
                }
            }
            """;

    @Override
    protected void doWork() throws Throwable {
        Path base = Paths.get(".");

        ComboTask task = newCompilationTask()
                .withSourceFromTemplate(MAIN_TEMPLATE, pname -> switch (pname) {
                        case "COND" -> cond;
                        case "TRUE" -> trueSec;
                        case "FALSE" -> falseSec;
                        default -> throw new UnsupportedOperationException(pname);
                    });

        task.analyze(result -> {
            boolean shouldPass;
            if (cond == Pattern.TRUE && (trueSec == Pattern.TRUE || trueSec == Pattern.FALSE)) { //6 cases covered
                shouldPass = false; //already in scope in true section
            } else if (cond == Pattern.FALSE && (falseSec == Pattern.TRUE || falseSec == Pattern.FALSE)) { //6 cases covered
                shouldPass = false; //already in scope in false section
            } else if (cond == Pattern.TRUE && falseSec == Pattern.TRUE) {
                shouldPass = false; //JLS 6.3.1.4
            } else if (cond == Pattern.FALSE && trueSec == Pattern.TRUE) {
                shouldPass = false; //JLS 6.3.1.4
            } else if (trueSec == Pattern.TRUE && falseSec == Pattern.TRUE) {
                shouldPass = false; //JLS 6.3.1.4
            } else if (trueSec == Pattern.TRUE && falseSec == Pattern.TRUE) {
                shouldPass = false; //JLS 6.3.1.4
            } else if (cond == Pattern.TRUE && falseSec == Pattern.FALSE) {
                shouldPass = false; //JLS 6.3.1.4
            } else if (cond == Pattern.FALSE && trueSec == Pattern.FALSE) {
                shouldPass = false; //JLS 6.3.1.4
            } else if (trueSec == Pattern.FALSE && falseSec == Pattern.FALSE) {
                shouldPass = false; //JLS 6.3.1.4
            } else {
                shouldPass = true;
            }

            if (!shouldPass) {
                result.containsKey("Blabla");
            }
        });
    }

    public enum Pattern implements ComboParameter {
        NONE("b"),
        TRUE("o instanceof String s"),
        FALSE("!(o instanceof String s)");
        private final String code;

        private Pattern(String code) {
            this.code = code;
        }

        @Override
        public String expand(String optParameter) {
            return code;
        }
    }

}
