/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      7151010 8006547 8007766 8029017 8246774
 * @summary  Default test cases for running combinations for Target values
 * @modules jdk.compiler
 * @build    Helper
 * @run main TargetAnnoCombo
 */

import java.util.Set;
import java.util.List;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;

public class TargetAnnoCombo {

    static final String TESTPKG = "testpkg";

    // Set it to true to get more debug information including base and container
    // target sets for a given test case.
    static final boolean DEBUG = false;

    // Define constant target sets to be used for the combination of the target values.
    final static Set<ElementType> noSet = null;
    final static Set<ElementType> empty = EnumSet.noneOf(ElementType.class);

    // [TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE,
    // PACKAGE, TYPE_PARAMETER, TYPE_USE, RECORD_COMPONENT]
    final static Set<ElementType> allTargets = EnumSet.allOf(ElementType.class);

    // [TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE,
    // PACKAGE]
    final static Set<ElementType> jdk7 = EnumSet.range(TYPE, PACKAGE);

    // [TYPE_USE, TYPE_PARAMETER]
    final static Set<ElementType> jdk8 = EnumSet.range(TYPE_PARAMETER, TYPE_USE);

    // List of test cases to run. This list is created in generate().
    // To run a specific test cases add case number in @run main line.
    List<TestCase> testCases = new ArrayList<TestCase>();

    int errors = 0;

    // Identify test cases that fail.
    enum IgnoreKind {
        RUN,
        IGNORE
    };

    private class TestCase {

        private Set<ElementType> baseAnnotations;
        private Set<ElementType> containerAnnotations;
        private IgnoreKind ignore;
        java.util.List<String> options;

        public TestCase(Set<ElementType> baseAnnotations, Set<ElementType> containerAnnotations) {
            this(baseAnnotations, containerAnnotations, IgnoreKind.RUN, null);
        }

        public TestCase(Set<ElementType> baseAnnotations, Set<ElementType> containerAnnotations, List<String> options) {
            this(baseAnnotations, containerAnnotations, IgnoreKind.RUN, options);
        }

        public TestCase(Set<ElementType> baseAnnotations, Set<ElementType> containerAnnotations,
                        IgnoreKind ignoreKind, java.util.List<String> options) {
            this.baseAnnotations = baseAnnotations;
            this.containerAnnotations = containerAnnotations;
            this.ignore = ignoreKind;
            this.options = options;
        }

        public Set getBaseAnnotations() {
            return baseAnnotations;
        }

        public Set getContainerAnnotations() {
            return containerAnnotations;
        }

        public boolean isIgnored() {
            return ignore == IgnoreKind.IGNORE;
        }

        // Determine if a testCase should compile or not.
        private boolean isValidSubSet() {
            /*
             *  RULE 1: conAnnoTarget should be a subset of baseAnnoTarget
             *  RULE 2: For empty @Target ({}) - annotation cannot be applied anywhere
             *         - Empty sets for both is valid
             *         - Empty baseTarget set is invalid with non-empty conTarget set
             *         - Non-empty baseTarget set is valid with empty conTarget set
             *  RULE 3: For no @Target specified - annotation can be applied to any JDK 7 targets
             *         - No @Target for both is valid
             *         - No @Target for baseTarget set with @Target conTarget set is valid
             *         - @Target for baseTarget set with no @Target for conTarget is invalid
             */


            /* If baseAnno has no @Target, Foo can be either applied to @Target specified
             * for container annotation else will be applicable for all default targets
             * if no @Target is present for container annotation.
             * In both cases, the set will be a valid set with no @Target for base annotation
             */
            if (baseAnnotations == null) {
                if (containerAnnotations == null) {
                    return true;
                }
                return !(containerAnnotations.contains(TYPE_USE) ||
                         containerAnnotations.contains(TYPE_PARAMETER));
            }

            Set<ElementType> tempBaseSet = EnumSet.noneOf(ElementType.class);
            tempBaseSet.addAll(baseAnnotations);

            // If BaseAnno has TYPE, then ANNOTATION_TYPE is allowed by default.
            if (baseAnnotations.contains(TYPE)) {
                tempBaseSet.add(ANNOTATION_TYPE);
            }

            // If BaseAnno has TYPE_USE, then add the extra allowed types
            if (baseAnnotations.contains(TYPE_USE)) {
                tempBaseSet.add(ANNOTATION_TYPE);
                tempBaseSet.add(TYPE);
                tempBaseSet.add(TYPE_PARAMETER);
            }

            // If containerAnno has no @Target, only valid case if baseAnnoTarget has
            // all targets defined else invalid set.
            if (containerAnnotations == null) {
                return tempBaseSet.containsAll(jdk7);
            }

            // At this point, neither conAnnoTarget or baseAnnoTarget are null.
            if (containerAnnotations.isEmpty()) {
                return true;
            }

            // At this point, conAnnoTarget is non-empty.
            if (baseAnnotations.isEmpty()) {
                return false;
            }

            // At this point, neither conAnnoTarget or baseAnnoTarget are empty.
            return tempBaseSet.containsAll(containerAnnotations);
        }
    }

    public static void main(String args[]) throws Exception {
        TargetAnnoCombo tac = new TargetAnnoCombo();
        // Generates all test cases to be run.
        tac.generate();
        List<Integer> cases = new ArrayList<Integer>();
        for (int i = 0; i < args.length; i++) {
            cases.add(Integer.parseInt(args[i]));
        }
        if (cases.isEmpty()) {
            tac.run();
        } else {
            for (int index : cases) {
                tac.executeTestCase(tac.testCases.get(index), index);
            }
        }
    }

    // options to be passed if target RECORD_COMPONENT can't be considered
    List<String> source8 = List.of("-source", "8");

    private void generate() {
        // Adding test cases to run.
        testCases.addAll(Arrays.asList(
                // No base target against no container target.
    /*  0*/     new TestCase(noSet, noSet),
                // No base target against empty container target.
    /*  1*/     new TestCase(noSet, empty),
                // No base target against TYPE_USE only container target.
                new TestCase(noSet, less(jdk8, TYPE_PARAMETER), source8),
                // No base target against TYPE_PARAMETER only container target.
                new TestCase(noSet, less(jdk8, TYPE_USE), source8),
                // No base target against TYPE_USE + TYPE_PARAMETER only container target.
                new TestCase(noSet, jdk8, source8),
                // No base target against TYPE_USE + some selection of jdk7 targets.
                new TestCase(noSet,
                plus(EnumSet.range(TYPE, LOCAL_VARIABLE), TYPE_USE)),
                // No base target against TYPE_PARAMETER + some selection of jdk7 targets.
                new TestCase(noSet,
                plus(EnumSet.range(TYPE, LOCAL_VARIABLE), TYPE_PARAMETER)),
                // No base target against each jdk7 target alone as container target.
                new TestCase(noSet, plus(empty, TYPE)),
                new TestCase(noSet, plus(empty, PARAMETER)),
                new TestCase(noSet, plus(empty, PACKAGE)),
    /*  10*/    new TestCase(noSet, plus(empty, METHOD)),
                new TestCase(noSet, plus(empty, LOCAL_VARIABLE)),
                new TestCase(noSet, plus(empty, FIELD)),
                new TestCase(noSet, plus(empty, CONSTRUCTOR)),
                new TestCase(noSet, plus(empty, ANNOTATION_TYPE)),
                // Empty base target against no container target.
                new TestCase(empty, noSet),
                // Empty base target against empty container target.
                new TestCase(empty, empty),
                // Empty base target against any lone container target.
                new TestCase(empty, plus(empty, TYPE)),
                new TestCase(empty, plus(empty, PARAMETER)),
                new TestCase(empty, plus(empty, PACKAGE)),
    /*  20*/    new TestCase(empty, plus(empty, METHOD)),
                new TestCase(empty, plus(empty, LOCAL_VARIABLE)),
                new TestCase(empty, plus(empty, FIELD)),
                new TestCase(empty, plus(empty, CONSTRUCTOR)),
                new TestCase(empty, plus(empty, ANNOTATION_TYPE)),
                new TestCase(empty, less(jdk8, TYPE_USE), source8),
                new TestCase(empty, less(jdk8, TYPE_PARAMETER), source8),
                // No container target against all all-but one jdk7 targets.
                new TestCase(less(jdk7, TYPE), noSet, source8),
                new TestCase(less(jdk7, PARAMETER), noSet, source8),
                new TestCase(less(jdk7, PACKAGE), noSet, source8),
    /*  30*/    new TestCase(less(jdk7, METHOD), noSet, source8),
                new TestCase(less(jdk7, LOCAL_VARIABLE), noSet, source8),
                new TestCase(less(jdk7, FIELD), noSet, source8),
                new TestCase(less(jdk7, CONSTRUCTOR), noSet, source8),
                new TestCase(less(jdk7, ANNOTATION_TYPE), noSet, source8),
                // No container against all but TYPE and ANNOTATION_TYPE
                new TestCase(less(jdk7, TYPE, ANNOTATION_TYPE), noSet),
                // No container against jdk7 targets.
                new TestCase(jdk7, noSet, source8),
                // No container against jdk7 targets plus one or both of TYPE_USE, TYPE_PARAMETER
                new TestCase(plus(jdk7, TYPE_USE), noSet, source8),
                new TestCase(plus(jdk7, TYPE_PARAMETER), noSet, source8),
                new TestCase(allTargets, noSet, null),
                // Empty container target against any lone target.
    /*  40*/    new TestCase(plus(empty, TYPE), empty),
                new TestCase(plus(empty, PARAMETER), empty),
                new TestCase(plus(empty, PACKAGE), empty),
                new TestCase(plus(empty, METHOD), empty),
                new TestCase(plus(empty, LOCAL_VARIABLE), empty),
                new TestCase(plus(empty, FIELD), empty),
                new TestCase(plus(empty, CONSTRUCTOR), empty),
                new TestCase(plus(empty, ANNOTATION_TYPE), empty),
                new TestCase(plus(empty, TYPE_USE), empty),
                new TestCase(plus(empty, TYPE_PARAMETER), empty),
                // All base targets against all container targets.
    /*  50*/    new TestCase(allTargets, allTargets),
                // All base targets against all but one container targets.
                new TestCase(allTargets, less(allTargets, TYPE)),
                new TestCase(allTargets, less(allTargets, PARAMETER)),
                new TestCase(allTargets, less(allTargets, PACKAGE)),
                new TestCase(allTargets, less(allTargets, METHOD)),
                new TestCase(allTargets, less(allTargets, LOCAL_VARIABLE)),
                new TestCase(allTargets, less(allTargets, FIELD)),
                new TestCase(allTargets, less(allTargets, CONSTRUCTOR)),
                new TestCase(allTargets, less(allTargets, ANNOTATION_TYPE)),
                new TestCase(allTargets, less(allTargets, TYPE_USE)),
    /*  60*/    new TestCase(allTargets, less(allTargets, TYPE_PARAMETER)),
                // All container targets against all but one base targets.
                new TestCase(less(allTargets, TYPE), allTargets),
                new TestCase(less(allTargets, PARAMETER), allTargets),
                new TestCase(less(allTargets, PACKAGE), allTargets),
                new TestCase(less(allTargets, METHOD), allTargets),
                new TestCase(less(allTargets, LOCAL_VARIABLE), allTargets),
                new TestCase(less(allTargets, FIELD), allTargets),
                new TestCase(less(allTargets, CONSTRUCTOR), allTargets),
                new TestCase(less(allTargets, ANNOTATION_TYPE), allTargets),
                new TestCase(less(allTargets, TYPE_USE), allTargets),
    /*  70*/    new TestCase(less(allTargets, TYPE_PARAMETER), allTargets)));
        // Generates 100 test cases for any lone base target contained in Set
        // allTargets against any lone container target.
        for (ElementType b : allTargets) {
            for (ElementType c : allTargets) {
                testCases.add(new TestCase(plus(empty, b), plus(empty, c)));
            }
        }
    }

    void run() throws Exception {
        int testCtr = 0;
        for (TestCase tc : testCases) {
            if (!tc.isIgnored()) {
                executeTestCase(tc, testCases.indexOf(tc));
                testCtr++;
            }
        }
        System.out.println("Total tests run: " + testCtr);
        if (errors > 0) {
            throw new Exception(errors + " errors found");
        }
    }

    private void executeTestCase(TestCase testCase, int index) {
        debugPrint("Test case number = " + index);
        debugPrint(" => baseAnnoTarget = " + testCase.getBaseAnnotations());
        debugPrint(" => containerAnnoTarget = " + testCase.getContainerAnnotations());

        String className = "TC" + index;
        boolean shouldCompile = testCase.isValidSubSet();
        Iterable<? extends JavaFileObject> files = getFileList(className, testCase, shouldCompile);
        // Get result of compiling test src file(s).
        boolean result = getCompileResult(className, shouldCompile, files, testCase.options);
        // List test src code if test fails.
        if (!result) {
            System.out.println("FAIL: Test " + index);
            try {
                for (JavaFileObject f : files) {
                    System.out.println("File: " + f.getName() + "\n" + f.getCharContent(true));
                }
            } catch (IOException ioe) {
                System.out.println("Exception: " + ioe);
            }
        } else {
            debugPrint("PASS: Test " + index);
        }

    }

    // Create src code and corresponding JavaFileObjects.
    private Iterable<? extends JavaFileObject> getFileList(String className,
            TestCase testCase, boolean shouldCompile) {
        Set<ElementType> baseAnnoTarget = testCase.getBaseAnnotations();
        Set<ElementType> conAnnoTarget = testCase.getContainerAnnotations();
        String srcContent = "";
        String pkgInfoContent = "";
        String template = Helper.template;
        String baseTarget = "", conTarget = "";

        String target = Helper.ContentVars.TARGET.getVal();
        if (baseAnnoTarget != null) {
            String tmp = target.replace("#VAL", convertToString(baseAnnoTarget).toString());
            baseTarget = tmp.replace("[", "{").replace("]", "}");
        }
        if (conAnnoTarget != null) {
            String tmp = target.replace("#VAL", convertToString(conAnnoTarget).toString());
            conTarget = tmp.replace("[", "{").replace("]", "}");
        }

        String annoData = Helper.ContentVars.IMPORTSTMTS.getVal()
                + conTarget
                + Helper.ContentVars.CONTAINER.getVal()
                + baseTarget
                + Helper.ContentVars.REPEATABLE.getVal()
                + Helper.ContentVars.BASE.getVal();

        JavaFileObject pkgInfoFile = null;

        // If shouldCompile = true and no @Target is specified for container annotation,
        // then all 8 ElementType enum constants are applicable as targets for
        // container annotation.
        if (shouldCompile && conAnnoTarget == null) {
            Set<ElementType> copySet = EnumSet.noneOf(ElementType.class);
            copySet.addAll(jdk7);
            conAnnoTarget = copySet;
        }

        if (shouldCompile) {
            boolean isPkgCasePresent = conAnnoTarget.contains(PACKAGE);
            String repeatableAnno = Helper.ContentVars.BASEANNO.getVal()
                    + " " + Helper.ContentVars.BASEANNO.getVal();
            for (ElementType s : conAnnoTarget) {
                String replaceStr = "/*" + s.name() + "*/";
                if (s.name().equalsIgnoreCase("PACKAGE")) {
                    //Create packageInfo file.
                    String pkgInfoName = TESTPKG + "." + "package-info";
                    pkgInfoContent = repeatableAnno + "\npackage " + TESTPKG + ";" + annoData;
                    pkgInfoFile = Helper.getFile(pkgInfoName, pkgInfoContent);
                } else {
                    template = template.replace(replaceStr, repeatableAnno);
                    if (!isPkgCasePresent) {
                        srcContent = template.replace(
                                "/*ANNODATA*/", annoData).replace("#ClassName", className);
                    } else {
                        replaceStr = "/*PACKAGE*/";
                        String tmp = template.replace(replaceStr, "package " + TESTPKG + ";");
                        srcContent = tmp.replace("#ClassName", className);
                    }
                }
            }
        } else {
            // For invalid cases, compilation should fail at declaration site.
            template = "class #ClassName {}";
            srcContent = annoData + template.replace("#ClassName", className);
        }
        JavaFileObject srcFile = Helper.getFile(className, srcContent);
        Iterable<? extends JavaFileObject> files = null;
        if (pkgInfoFile != null) {
            files = Arrays.asList(pkgInfoFile, srcFile);
        } else {
            files = Arrays.asList(srcFile);
        }
        return files;
    }

    // Compile the test source file(s) and return test result.
    private boolean getCompileResult(String className, boolean shouldCompile,
            Iterable<? extends JavaFileObject> files, Iterable<String> options) {

        DiagnosticCollector<JavaFileObject> diagnostics =
                new DiagnosticCollector<JavaFileObject>();
        Helper.compileCode(diagnostics, files, options);
        // Test case pass or fail.
        boolean ok = false;
        String errMesg = "";
        int numDiags = diagnostics.getDiagnostics().size();
        if (numDiags == 0) {
            if (shouldCompile) {
                debugPrint("Test passed, compiled as expected.");
                ok = true;
            } else {
                errMesg = "Test failed, compiled unexpectedly.";
                ok = false;
            }
        } else {
            if (shouldCompile) {
                // did not compile.
                List<Diagnostic<? extends JavaFileObject>> allDiagnostics = diagnostics.getDiagnostics();
                if (allDiagnostics.stream().noneMatch(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR)) {
                    ok = true;
                } else {
                    errMesg = "Test failed, should have compiled successfully.";
                    ok = false;
                }
            } else {
                // Error in compilation as expected.
                String expectedErrKey = "compiler.err.invalid.repeatable."
                        + "annotation.incompatible.target";
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    if ((d.getKind() == Diagnostic.Kind.ERROR)
                            && d.getCode().contains(expectedErrKey)) {
                        // Error message as expected.
                        debugPrint("Error message as expected.");
                        ok = true;
                        break;
                    } else {
                        // error message is incorrect.
                        ok = false;
                    }
                }
                if (!ok) {
                    errMesg = "Incorrect error received when compiling "
                            + className + ", expected: " + expectedErrKey;
                }
            }
        }

        if (!ok) {
            error(errMesg);
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                System.out.println(" Diags: " + d);
            }
        }
        return ok;
    }

    private Set<ElementType> less(Set<ElementType> base, ElementType... sub) {
        Set<ElementType> res = EnumSet.noneOf(ElementType.class);
        res.addAll(base);
        for (ElementType t : sub) {
            res.remove(t);
        }
        return res;
    }

    private Set<ElementType> plus(Set<ElementType> base, ElementType... add) {
        Set<ElementType> res = EnumSet.noneOf(ElementType.class);
        res.addAll(base);
        for (ElementType t : add) {
            res.add(t);
        }
        return res;
    }

    // Iterate target set and add "ElementType." in front of every target type.
    private List<String> convertToString(Set<ElementType> annoTarget) {
        if (annoTarget == null) {
            return null;
        }
        List<String> annoTargets = new ArrayList<String>();
        for (ElementType e : annoTarget) {
            annoTargets.add("ElementType." + e.name());
        }
        return annoTargets;
    }

    private void debugPrint(String string) {
        if (DEBUG) {
            System.out.println(string);
        }
    }

    private void error(String msg) {
        System.out.println("ERROR: " + msg);
        errors++;
    }
}

