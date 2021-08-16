/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug      8002157
 * @author   sogoel
 * @summary  Combo test to check for usage of Deprecated
 * @modules jdk.compiler
 * @build    Helper
 * @compile  DeprecatedAnnoCombo.java
 * @run main DeprecatedAnnoCombo
 */

import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/*
 * Generate test src for use of @Deprecated on base anno
 * or container anno or on both. In all cases, test src should compile and a
 * warning should be generated. Repeating annotations used only on class for
 * these generated test src.
 */

public class DeprecatedAnnoCombo extends Helper {
    static int errors = 0;

    enum TestCases {
        DeprecatedonBoth,
        DeprecatedonContainer,
        DeprecatedonBase;
    }

    public static void main(String[] args) throws Exception {
        new DeprecatedAnnoCombo().runTest();
    }

    public void runTest() throws Exception {
        boolean ok = false;
        int testCtr = 0;

        for (TestCases clName : TestCases.values()) {
            testCtr++;

            // Create test source content
            String contents = getContent(clName.toString());

            // Compile the generated source file
            DiagnosticCollector<JavaFileObject> diagnostics =
                    new DiagnosticCollector<JavaFileObject>();
            ok = compileCode(clName.toString(), contents, diagnostics);

            String errorKey1 = "compiler.note.deprecated.filename";
            String errorKey2 = "compiler.note.deprecated.recompile";
            List<Diagnostic<? extends JavaFileObject>> diags = diagnostics.getDiagnostics();

            //Check for deprecated warnings
            if (ok) {
                if (diags.size() == 0) {
                    error("Did not get any warnings for @Deprecated usage");
                } else {
                    for (Diagnostic<?> d : diags) {
                        if (d.getKind() == Diagnostic.Kind.NOTE) {
                            if (d.getCode().contains(errorKey1)
                                || d.getCode().contains(errorKey2)) {
                                System.out.println("TestCase =" + clName + " passed as expected");
                            } else {
                                error("TestCase =" + clName + " did not give correct warnings" +
                                    "Expected warning keys: " +
                                    "errorKey1 = " + errorKey1 +
                                    "errorKey2 = " + errorKey2 +
                                    "actualErrorKey = " + d.getCode(), contents);
                            }
                        } else {
                            error("Diagnostic Kind is incorrect, expected = " +
                                 Diagnostic.Kind.NOTE + "actual = " + d.getKind(), contents);
                        }
                    }
                }
            } else {
                error("TestCase =" + clName + " did not compile as expected", contents);
            }
        }

        System.out.println("Total number of tests run: " + testCtr);
        System.out.println("Total number of errors: " + errors);
        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    private String getContent(String className) {
        StringBuilder annoData = new StringBuilder();

        switch(className) {
        case "DeprecatedonBoth":
            annoData.append(Helper.ContentVars.DEPRECATED.getVal())
                    .append(Helper.ContentVars.CONTAINER.getVal())
                    .append(Helper.ContentVars.DEPRECATED.getVal())
                    .append(Helper.ContentVars.REPEATABLE.getVal())
                    .append(Helper.ContentVars.BASE.getVal());
            break;
        case "DeprecatedonBase":
            annoData.append(Helper.ContentVars.CONTAINER.getVal())
                    .append(Helper.ContentVars.DEPRECATED.getVal())
                    .append(Helper.ContentVars.REPEATABLE.getVal())
                    .append(Helper.ContentVars.BASE.getVal());
            break;
        case "DeprecatedonContainer":
            annoData.append(Helper.ContentVars.DEPRECATED.getVal())
                    .append(Helper.ContentVars.CONTAINER.getVal())
                    .append(Helper.ContentVars.REPEATABLE.getVal())
                    .append(Helper.ContentVars.BASE.getVal());
            break;
        }

        String contents = Helper.ContentVars.IMPORTCONTAINERSTMTS.getVal()
                          + Helper.ContentVars.IMPORTDEPRECATED.getVal()
                          + annoData
                          + Helper.ContentVars.REPEATABLEANNO.getVal()
                          + "\nclass "+ className + "{}";
        return contents;
    }

    private void error(String msg, String... contents) {
        System.out.println("error: " + msg);
        errors++;
        if (contents.length == 1) {
            System.out.println("Contents = " + contents[0]);
        }
    }
}
