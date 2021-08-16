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
 * @summary  Positive combo test for use of Inherited on baseAnno/containerAnno
 * @modules jdk.compiler
 * @build    Helper
 * @compile  InheritedAnnoCombo.java
 * @run main InheritedAnnoCombo
 */

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/*
 * Generate valid test src for the use of @Inherited on container anno
 * or on both base anno and container anno. Both test src should compile.
 * Repeating annotations used only on class for these generated test src.
 */

public class InheritedAnnoCombo extends Helper {
    static int errors = 0;
    enum TestCases {
        InheritedonBothAnno(true),
        InheritedonBase(true);

        TestCases(boolean compile) {
            this.compile = compile;
        }

        boolean compile;
        boolean shouldCompile() {
            return compile;
        }
    }

    public static void main(String[] args) throws Exception {
        new InheritedAnnoCombo().runTest();
    }

    public void runTest() throws Exception {
        int testCtr = 0;
        boolean ok = false;

        // Create test source content
        for (TestCases className : TestCases.values()) {
            testCtr++;
            String contents = getContent(className.toString());

            // Compile the generated code
            DiagnosticCollector<JavaFileObject> diagnostics =
                    new DiagnosticCollector<JavaFileObject>();
            ok = compileCode(className.toString(), contents, diagnostics);

            if (!ok) {
                error("Class="+ className +" did not compile as expected", contents);
            } else {
                System.out.println("Test passed for className: " + className);
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
        case "InheritedonBothAnno":
            annoData.append(Helper.ContentVars.INHERITED.getVal())
            .append(Helper.ContentVars.CONTAINER.getVal())
            .append(Helper.ContentVars.INHERITED.getVal())
            .append(Helper.ContentVars.REPEATABLE.getVal())
            .append(Helper.ContentVars.BASE.getVal());
            break;
        case "InheritedonBase":
            annoData.append(Helper.ContentVars.INHERITED.getVal())
            .append(Helper.ContentVars.CONTAINER.getVal())
            .append(Helper.ContentVars.REPEATABLE.getVal())
            .append(Helper.ContentVars.BASE.getVal());
            break;
        }

        String contents = Helper.ContentVars.IMPORTCONTAINERSTMTS.getVal()
                          + Helper.ContentVars.IMPORTINHERITED.getVal()
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

