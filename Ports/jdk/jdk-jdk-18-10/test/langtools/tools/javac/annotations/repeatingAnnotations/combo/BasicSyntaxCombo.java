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
 * @summary  Basic Syntax test for repeating annotations on all elements
 * @modules jdk.compiler
 * @build    Helper
 * @compile  BasicSyntaxCombo.java
 * @run main BasicSyntaxCombo
 */


import java.util.Arrays;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic;

/*
 * Generate test src for element kinds with repeating annotations.
 * The test uses Helper.java to get the template to create test src and
 * compile the test src.
 * The test passes if valid test src compile as expected and
 * and invalid test src fail as expected.
 */

public class BasicSyntaxCombo extends Helper{
    static int errors = 0;
    static boolean exitMode = false;
    static String TESTPKG = "testpkg";
    static String srcContent = "";
    static String pkgInfoContent = "";

    static {
        // If EXIT_ON_FAIL is set, the combo test will exit at the first error
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

    enum TestElem {
        ANNOTATION_TYPE(true),
        PACKAGE(true),
        CONSTRUCTOR(true),
        FIELD(true),
        LOCAL_VARIABLE(true),
        METHOD(true),
        TYPE(true),
        PARAMETER(true),
        INNER_CLASS(true),
        STATIC_INI(false),
        INSTANCE_INI(false);

        TestElem(boolean compile) {
            this.compile = compile;
        }

        boolean compile;
        boolean shouldCompile() {
            return compile;
        }
    }

    public static void main(String[] args) throws Exception {
        new BasicSyntaxCombo().runTest();
    }

    public void runTest() throws Exception {
        boolean result = false;
        Iterable<? extends JavaFileObject> files = null;
        int testCtr = 0;
        for (TestElem type : TestElem.values()) {
            testCtr++;
            String className = "BasicCombo_"+type;
            files = getFileList(className, type);

            boolean shouldCompile = type.shouldCompile();
            result = getCompileResult(className, shouldCompile,files);

            if (shouldCompile && !result) {
                error(className + " did not compile as expected", srcContent);
                if(!pkgInfoContent.isEmpty()) {
                    System.out.println("package-info.java contents: " + pkgInfoContent);
                }
            }

            if (!shouldCompile && !result) {
                error(className + " compiled unexpectedly", srcContent);
                if(!pkgInfoContent.isEmpty()) {
                    System.out.println("package-info.java contents: " + pkgInfoContent);
                }
            }
        }

        System.out.println("Total number of tests run: " + testCtr);
        System.out.println("Total number of errors: " + errors);

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    private boolean getCompileResult(String className, boolean shouldCompile,
            Iterable<? extends JavaFileObject> files) throws Exception {

        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<JavaFileObject>();
        boolean ok =  Helper.compileCode(diagnostics,files);
        if (!shouldCompile && !ok) {
            checkErrorKeys(className, diagnostics);
        }
        return (shouldCompile == ok);
    }

    private void checkErrorKeys (
            String className, DiagnosticCollector<JavaFileObject> diagnostics) throws Exception {
        String expectedErrKey = "compiler.err.illegal.start.of.type";
        for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
            if ((d.getKind() == Diagnostic.Kind.ERROR) &&
                d.getCode().contains(expectedErrKey)) {
                break; // Found the expected error
            } else {
                error("Incorrect error key, expected = "
                      + expectedErrKey + ", Actual = " + d.getCode()
                      + " for className = " + className, srcContent);
            }
        }
    }

    private Iterable<? extends JavaFileObject> getFileList(String className,
            TestElem type ) {

        String template = Helper.template;
        String replaceStr = "/*"+type+"*/";
        StringBuilder annoData = new StringBuilder();
        annoData.append(Helper.ContentVars.IMPORTCONTAINERSTMTS.getVal())
                .append(Helper.ContentVars.CONTAINER.getVal())
                .append(Helper.ContentVars.REPEATABLE.getVal())
                .append(Helper.ContentVars.BASE.getVal());

        JavaFileObject pkgInfoFile = null;

        if (type.equals("PACKAGE")) {
            srcContent = template.replace(replaceStr, "package testpkg;")
                        .replace("#ClassName", className);

            String pkgInfoName = TESTPKG+"."+"package-info";
            pkgInfoContent = Helper.ContentVars.REPEATABLEANNO.getVal()
                             + "package " + TESTPKG + ";"
                             + annoData;
            pkgInfoFile = getFile(pkgInfoName, pkgInfoContent);
        } else {
            template = template.replace(replaceStr, Helper.ContentVars.REPEATABLEANNO.getVal())
                       .replace("#ClassName", className);
            srcContent = annoData + template;
        }

        JavaFileObject srcFile = getFile(className, srcContent);

        Iterable<? extends JavaFileObject> files = null;
        if (pkgInfoFile != null) {
            files = Arrays.asList(pkgInfoFile,srcFile);
        }
        else {
            files = Arrays.asList(srcFile);
        }
        return files;
    }

    private void error(String msg, String... contents) throws Exception {
        System.out.println("error: " + msg);
        errors++;
        if (contents.length == 1) {
            System.out.println("Contents = " + contents[0]);
        }
        // Test exits as soon as it gets a failure
        if (exitMode) throw new Exception();
    }
}
