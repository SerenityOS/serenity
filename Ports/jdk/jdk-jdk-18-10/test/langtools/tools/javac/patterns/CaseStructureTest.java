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
 * @bug 8269146
 * @summary Check compilation outcomes for various combinations of case label element.
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.file
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build combo.ComboTestHelper
 * @compile CaseStructureTest.java
 * @run main CaseStructureTest
 */

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask;
import combo.ComboTestHelper;
import java.util.Arrays;
import java.util.stream.Collectors;
import toolbox.ToolBox;

public class CaseStructureTest extends ComboInstance<CaseStructureTest> {
    private static final String JAVA_VERSION = System.getProperty("java.specification.version");

    protected ToolBox tb;

    CaseStructureTest() {
        super();
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<CaseStructureTest>()
                .withDimension("AS_CASE_LABEL_ELEMENTS", (x, asCaseLabelElements) -> x.asCaseLabelElements = asCaseLabelElements, true, false)
                .withArrayDimension("CASE_LABELS", (x, caseLabels, idx) -> x.caseLabels[idx] = caseLabels, DIMENSIONS, CaseLabel.values())
                .withFilter(t -> Arrays.stream(t.caseLabels).anyMatch(l -> l != CaseLabel.NONE))
                .withFailMode(ComboTestHelper.FailMode.FAIL_FAST)
                .run(CaseStructureTest::new);
    }

    private static final int DIMENSIONS = 4;
    private boolean asCaseLabelElements;
    private CaseLabel[] caseLabels = new CaseLabel[DIMENSIONS];

    private static final String MAIN_TEMPLATE =
            """
            public class Test {
                public static void doTest(Integer in) {
                    switch (in) {
                        case -1: break;
                        #{CASES}
                        #{DEFAULT}
                    }
                }
            }
            """;

    @Override
    protected void doWork() throws Throwable {
        String labelSeparator = asCaseLabelElements ? ", " : ": case ";
        String labels = Arrays.stream(caseLabels).filter(l -> l != CaseLabel.NONE).map(l -> l.code).collect(Collectors.joining(labelSeparator, "case ", ": break;"));
        boolean hasDefault = Arrays.stream(caseLabels).anyMatch(l -> l == CaseLabel.DEFAULT || l == CaseLabel.TYPE_PATTERN || l == CaseLabel.PARENTHESIZED_PATTERN);

        ComboTask task = newCompilationTask()
                .withSourceFromTemplate(MAIN_TEMPLATE.replace("#{CASES}", labels).replace("#{DEFAULT}", hasDefault ? "" : "default: break;"))
                .withOption("--enable-preview")
                .withOption("-source").withOption(JAVA_VERSION);

        task.generate(result -> {
            boolean shouldPass = true;
            long patternCases = Arrays.stream(caseLabels).filter(l -> l == CaseLabel.TYPE_PATTERN || l == CaseLabel.GUARDED_PATTERN || l == CaseLabel.PARENTHESIZED_PATTERN).count();
            long typePatternCases = Arrays.stream(caseLabels).filter(l -> l == CaseLabel.TYPE_PATTERN).count();
            long constantCases = Arrays.stream(caseLabels).filter(l -> l == CaseLabel.CONSTANT).count();
            long nullCases = Arrays.stream(caseLabels).filter(l -> l == CaseLabel.NULL).count();
            long defaultCases = Arrays.stream(caseLabels).filter(l -> l == CaseLabel.DEFAULT).count();
            if (constantCases > 1) {
                shouldPass &= false;
            }
            if (constantCases > 0) {
                shouldPass &= patternCases == 0;
            }
            if (defaultCases > 1) {
                shouldPass &= false;
            }
            if (nullCases > 1) {
                shouldPass &= false;
            }
            if (nullCases > 0 && patternCases > 0) {
                shouldPass &= patternCases == typePatternCases;
            }
            if (patternCases > 1) {
                shouldPass &= false;
            }
            if (patternCases > 0 && defaultCases > 0) {
                shouldPass &= false;
            }
            if (!asCaseLabelElements) {
                //as an edge case, `case <total-pattern>: case null:` is prohibited:
                boolean seenPattern = false;
                for (CaseLabel label : caseLabels) {
                    switch (label) {
                        case NULL: if (seenPattern) shouldPass = false; break;
                        case GUARDED_PATTERN, PARENTHESIZED_PATTERN, TYPE_PATTERN: seenPattern = true; break;
                    }
                }
            }
            if (!(shouldPass ^ result.hasErrors())) {
                throw new AssertionError("Unexpected result: shouldPass=" + shouldPass + ", actual: " + !result.hasErrors() + ", info: " + result.compilationInfo());
            }
        });
    }

    public enum CaseLabel implements ComboParameter {
        NONE(""),
        TYPE_PATTERN("Integer i"),
        PARENTHESIZED_PATTERN("(Integer i)"),
        GUARDED_PATTERN("Integer i && i > 0"),
        CONSTANT("1"),
        NULL("null"),
        DEFAULT("default");

        private final String code;

        private CaseLabel(String code) {
            this.code = code;
        }

        @Override
        public String expand(String optParameter) {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

}
