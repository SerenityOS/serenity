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
 * @summary  Combo test for all possible combinations for Retention Values
 * @modules jdk.compiler
 * @build    Helper
 * @compile  RetentionAnnoCombo.java
 * @run main RetentionAnnoCombo
 */

import java.util.HashMap;
import java.util.Map;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic;

/*
 * Generate all combinations for the use of @Retention on base anno or container
 * anno or both. The test passes if valid test src compile as expected and
 * and invalid test src fail as expected.
 * Repeating annotations used only on class for these generated test src.
 */

public class RetentionAnnoCombo extends Helper {
    static int errors = 0;
    static boolean exitMode = false;
    static {
        String exitOnFail = System.getenv("EXIT_ON_FAIL");
        if (exitOnFail == null || exitOnFail == ""  ) {
            exitMode = false;
        }
        else {
            if (exitOnFail.equalsIgnoreCase("YES") ||
                    exitOnFail.equalsIgnoreCase("Y") ||
                    exitOnFail.equalsIgnoreCase("TRUE") ||
                    exitOnFail.equalsIgnoreCase("T")) {
                exitMode = true;
            }
        }
    }

    public static void main(String args[]) throws Exception {
        new RetentionAnnoCombo().runTest();
    }

    public void runTest() throws Exception {

        /* 4x4 matrix for Retention values SOURCE, DEFAULT, CLASS, RUNTIME
         * i -> Retention value on ContainerAnno
         * j -> Retention value on BaseAnno
         * 1 -> retention value combo should compile
         */
        int[][] retention = { {1, 0, 0, 0},
                              {1, 1, 1, 0},
                              {1, 1, 1, 0},
                              {1, 1, 1, 1} };

        Map<Integer, String> retMap = setRetentionValMatrix();
        String contents = "";
        boolean result = false;
        int testCtr = 0;
        for (int i = 0; i < 4 ; i ++) {
            for (int j = 0; j < 4; j++ ) {
                testCtr++;
                String className = "RetentionTest_"+i+"_"+j;
                contents = getContent(className, retMap, i, j);
                if (retention[i][j] == 1) {
                    // Code generated should compile
                    result = getCompileResult(contents,className, true);
                    if (!result) {
                        error("FAIL: " +  className + " did not compile as expected!", contents);
                    }
                } else {
                    result = getCompileResult(contents,className, false);
                    if (!result) {
                        error("FAIL: " +  className + " compiled unexpectedly!", contents);
                    }
                }
                if (result) {
                    System.out.println("Test passed for className = " + className);
                }
            }
        }

        System.out.println("Total number of tests run: " + testCtr);
        System.out.println("Total number of errors: " + errors);

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    private boolean getCompileResult(String contents, String className,
            boolean shouldCompile) throws Exception{

        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<JavaFileObject>();
        boolean ok = compileCode(className, contents, diagnostics);

        String expectedErrKey = "compiler.err.invalid.repeatable" +
                                        ".annotation.retention";
        if (!shouldCompile && !ok) {
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                if (!((d.getKind() == Diagnostic.Kind.ERROR) &&
                    d.getCode().contains(expectedErrKey))) {
                    error("FAIL: Incorrect error given, expected = "
                            + expectedErrKey + ", Actual = " + d.getCode()
                            + " for className = " + className, contents);
                }
            }
        }

        return (shouldCompile  == ok);
    }

    private Map<Integer,String> setRetentionValMatrix() {
        HashMap<Integer,String> hm = new HashMap<>();
        hm.put(0,"SOURCE");
        hm.put(1,"DEFAULT");
        hm.put(2,"CLASS");
        hm.put(3,"RUNTIME");
        return hm;
    }

    private String getContent(String className, Map<Integer, String> retMap,
            int i, int j) {

        String retContainerVal = retMap.get(i).toString();
        String retBaseVal = retMap.get(j).toString();
        String replacedRetBaseVal = "", replacedRetCAVal = "";
        String retention = Helper.ContentVars.RETENTION.getVal();

        // @Retention is available as default for both base and container anno
        if (retContainerVal.equalsIgnoreCase("DEFAULT")
                && retBaseVal.equalsIgnoreCase("DEFAULT")) {
            replacedRetBaseVal = "";
            replacedRetCAVal = "";
        // @Retention is available as default for container anno
        } else if (retContainerVal.equalsIgnoreCase("DEFAULT")) {
            replacedRetBaseVal = retention.replace("#VAL", retBaseVal);
            replacedRetCAVal = "";
        // @Retention is available as default for base anno
        } else if (retBaseVal.equalsIgnoreCase("DEFAULT")) {
            replacedRetBaseVal = "";
            replacedRetCAVal = retention.replace("#VAL", retContainerVal);
        // @Retention is not available as default for both base and container anno
        } else {
            replacedRetBaseVal = retention.replace("#VAL", retBaseVal);
            replacedRetCAVal = retention.replace("#VAL", retContainerVal);
        }

        StringBuilder annoData = new StringBuilder();
        annoData.append(Helper.ContentVars.IMPORTCONTAINERSTMTS.getVal())
                .append(Helper.ContentVars.IMPORTRETENTION.getVal())
                .append(replacedRetCAVal)
                .append(Helper.ContentVars.CONTAINER.getVal())
                .append(Helper.ContentVars.REPEATABLE.getVal())
                .append(replacedRetBaseVal)
                .append(Helper.ContentVars.BASE.getVal());

        String contents = annoData
                          + Helper.ContentVars.REPEATABLEANNO.getVal()
                          + "\nclass "+ className + "{}";
        return contents;
    }

    private void error(String msg,String... contents) throws Exception {
        System.out.println("error: " + msg);
        errors++;
        if (contents.length == 1) {
            System.out.println("Contents = " + contents[0]);
        }
        // Test exits as soon as it gets a failure
        if (exitMode) throw new Exception();
    }
}

