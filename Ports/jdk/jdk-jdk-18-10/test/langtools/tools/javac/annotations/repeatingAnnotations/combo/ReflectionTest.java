/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8001457 8027477 8163113
 * @author   sogoel
 * @summary  Reflection api tests
 * @modules jdk.compiler
 * @build    Helper
 * @compile  expectedFiles/ExpectedBase.java expectedFiles/ExpectedContainer.java
 * @run main ReflectionTest
 */
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import expectedFiles.ExpectedBase;
import expectedFiles.ExpectedContainer;
import java.util.Iterator;
import java.util.regex.Pattern;

/*
 * Objective:
 * Test the following 6 methods from java.lang.reflect.AnnotatedElement:
 * - getAnnotation(Class<T>)
 * - getAnnotations()
 * - getDeclaredAnnotations()
 * - getDeclaredAnnotation(Class<T>)  // new method in JDK8
 * - getAnnotationsByType(Class<T>)         // new method in JDK8
 * - getDeclaredAnnotationsByType(Class<T>) // new method in JDK8
 * for multiple test cases, for example, BasicNonRepeatable case, BasicRepeatable case
 * for each of the src types - class, method, field, package
 *
 * This test uses following three enums:
 * 1. TestCase - Defines the ExpectedBase/ExpectedContainer values for each testCase
 *             - getTestFiles() - Creates list of JavaFileObjects for the primary
 *                                src types (class, method, field, package)
 *             - Each testCase is a new scenario with a combination of @Repeatable
 *               relationship present/absent in conjunction with @Inherited.
 *               For eg: BasicNonRepeatable_Legacy - It is a pre-JDK8 way of writing a single
 *                       annotation on a given srcType( class, method, field, package)
 *                       BasicRepeatable - It is a JDK8 way of writing repeating annotations
 *                       on a given srcType with a @Repeatable relationship
 *                       defined between Foo and FooContainer.
 *
 * 2. SrcType - Defines templates used in creation of test src
 *            - Defines getExpectedBase() and getExpectedContainer() for primary src types
 * 3. TestMethod - Defines getActualAnnoBase(), getActualAnnoContainer(), getExpectedAnnoBase(),
 *                 and getExpectedAnnoContainer() for each of the 6 methods that are being tested
 *                 in java.lang.reflect.AnnotatedElement
 *
 * Test execution flow:
 * - Loop over each of the src types and each test cases
 * - Creates test src for each flow, compile it, load the class object
 * - Run all 6 methods on this class object
 * - Get expected and actual annotations for each object and compare them.
 * - Increment the error counter if the annotations don't match.
 *
 * The test fails if the number of errors is greater than 0.
 */
public class ReflectionTest {

    static int errors = 0;
    // Variables used in creating test src for a given testcase/testSrcType
    static final String TESTPKG = "testpkg";
    static final String TESTMETHOD = "testMethod";
    static final String TESTFIELD = "testField";
    static final String PKGINFONAME = TESTPKG + ".package-info";
    static final String SUPERCLASS = "SuperClass";
    static final String TESTINTERFACE = "TestInterface";
    /*
     *  Set it to true to get more debug information
     */
    static final boolean DEBUG = false;
    static boolean CHECKORDERING;

    public static void main(String args[]) throws Exception {
        ReflectionTest test = new ReflectionTest();
        test.runTest();
    }

    public void runTest() throws Exception {

        ClassLoader parentClassLoader = getLoader();
        String className = "";
        Iterable<? extends JavaFileObject> files = null;

        for (SrcType srcType : SrcType.getSrcTypes()) {
            for (TestCase testCase : TestCase.values()) {
                className = testCase + "_" + srcType;
                debugPrint("*****************************************");
                System.out.println("Running Test for ClassName: " + className);

                // @Inherited only applicable for class, exclude cases for
                // package, method, field
                if (testCase.name().contains("Inherited")
                        && (srcType != SrcType.CLASS)) {
                    continue;
                }

                // Get list of JavaFileObjects to be compiled
                files = testCase.getTestFiles(srcType, className);
                if (srcType == SrcType.PACKAGE) {
                    className = TESTPKG + "." + className;
                }
                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

                // Compile the list of JavaFileObjects
                try {
                    Helper.compileCode(diagnostics, files);
                } catch (Exception ex) {
                    printTestSrc(files);
                    throw new RuntimeException(
                            "Exception when compiling class " + className, ex);
                }

                // Get Class object for the compiled class
                Class<?> c = loadClass(className, parentClassLoader, Helper.destDir);
                if (c != null) {
                    // For the loaded class object, compare expected and actual annotation values
                    // for each of the methods under test from java.lang.reflect.AnnotatedElement


                    // Ignoring following test cases since for now they are
                    // failing with ordering issues.
                    // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
                    // fail with ordering issues
                    List<String> orderingTestFailures = Arrays.asList(
                            "SingleOnSuperContainerOnSub_Inherited_Legacy",
                            "SingleOnSuperContainerAndSingleOnSub_Inherited_Legacy",
                            "ContainerAndSingleOnSuperSingleOnSub_Inherited_Legacy",
                            "SingleAnnoWithContainer",
                            "SingleOnSuperContainerAndSingleOnSub_Inherited",
                            "RepeatableOnSuperSingleOnSub_Inherited",
                            "SingleOnSuperRepeatableOnSub_Inherited",
                            "ContainerOnSuperSingleOnSub_Inherited",
                            "SingleOnSuperContainerOnSub_Inherited",
                            "ContainerAndSingleOnSuperSingleOnSub_Inherited");
                    if (orderingTestFailures.contains(testCase.toString())) {
                        CHECKORDERING = false;
                    } else
                        CHECKORDERING = true;

                    checkAnnoValues(srcType, c);
                } else {
                    error("Could not load className = " + c);
                }
            }
        }

        if (getNumErrors() > 0) {
            System.err.println("Test failed with " + getNumErrors() + " errors");
            throw new RuntimeException();
        }
    }

    /*
     *  Each test case in this enum has the following:
     *  - Define each test case with its @ExpectedBase and @ExpectedContainer annotations
     *  - Override getTestFiles() that creates list of JavaFileObjects for each case
     *    based on the src type.
     */
    enum TestCase {
        BasicNonRepeatable_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\"}, "
                + "getDeclAnnoVal = \"@Foo(0)\", "
                + "getAnnosArgs = {\"@Foo(0)\"}, "
                + "getDeclAnnosArgs = {\"@Foo(0)\"}) ",
        "@ExpectedContainer") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String testSrc = "";
                String pkgInfoContents = "";
                String contents = "";

                JavaFileObject pkgFileObj = null;
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = new StringBuilder();
                anno = Helper.ContentVars.BASEANNO.getVal();
                commonStmts.append(Helper.ContentVars.IMPORTEXPECTED.getVal())
                        .append(Helper.ContentVars.IMPORTSTMTS.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.BASE.getVal());
                switch (srcType) {
                    case PACKAGE:
                        /*
                        Sample package-info.java
                        @ExpectedBase
                        @ExpectedContainer
                        @Foo(0)
                        package testpkg;

                        @Retention(RetentionPolicy.RUNTIME)
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        Sample testSrc:
                        package testpkg;
                        class A {}
                         */
                        testSrc = srcType.getTemplate().replace("#CN", className);
                        contents = testSrc;
                        srcFileObj = Helper.getFile(className, contents);

                        replaceVal = expectedVals + "\n" + anno;
                        pkgInfoContents = SrcType.PKGINFO.getTemplate()
                                .replace("#REPLACE1", replaceVal)
                                .replace("#REPLACE2", commonStmts);
                        pkgFileObj = Helper.getFile(PKGINFONAME, pkgInfoContents);

                        files = Arrays.asList(pkgFileObj, srcFileObj);
                        break;
                    default:
                        // class, method, field
                    /*
                        Sample testSrc for class
                        @Retention(RetentionPolicy.RUNTIME)
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @ExpectedBase
                        @ExpectedContainer
                        @Foo(0)
                        class A {}
                         */
                        replaceVal = expectedVals + anno;
                        testSrc = srcType.getTemplate().replace("#CN", className)
                                .replace("#REPLACE", replaceVal);
                        contents = commonStmts + testSrc;
                        srcFileObj = Helper.getFile(className, contents);
                        files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        SingleAnnoInherited_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\", "
                + "getAnnotationsVals = {\"@Foo(0)\", \"ExpectedBase\", \"ExpectedContainer\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\"}, "
                + "getDeclAnnoVal = \"NULL\", "
                + "getAnnosArgs = {\"@Foo(0)\"}, "
                + "getDeclAnnosArgs = {})",
        "@ExpectedContainer") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = new StringBuilder();

                /*
                Sample testSrc:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Foo(0)
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                class SubClass extends SuperClass {}
                 */

                // @Inherited only works for classes, no switch cases for
                // method, field, package
                anno = Helper.ContentVars.BASEANNO.getVal();
                commonStmts.append(Helper.ContentVars.IMPORTEXPECTED.getVal())
                        .append(Helper.ContentVars.IMPORTSTMTS.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.INHERITED.getVal())
                        .append(Helper.ContentVars.BASE.getVal());

                if (srcType == SrcType.CLASS) {
                    // Contents for SuperClass
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS).replace("#REPLACE", replaceVal);

                    // Contents for SubClass that extends SuperClass
                    replaceVal = expectedVals;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className).replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        InheritedAnnoOnInterface_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"NULL\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\"},"
                + "getDeclAnnoVal = \"NULL\"," + "getAnnosArgs = {},"
                + "getDeclAnnosArgs = {})",
        "@ExpectedContainer") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = new StringBuilder();

                /*
                Sample test src:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Foo(0)
                interface TestInterface { }

                @ExpectedBase
                @ExpectedContainer
                class A implements TestInterface {}
                 */
                anno = Helper.ContentVars.BASEANNO.getVal();
                commonStmts.append(Helper.ContentVars.IMPORTEXPECTED.getVal())
                        .append(Helper.ContentVars.IMPORTSTMTS.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.INHERITED.getVal())
                        .append(Helper.ContentVars.BASE.getVal());

                if (srcType == SrcType.CLASS) {
                    // Contents for TestInterface
                    replaceVal = commonStmts + "\n" + anno;
                    String interfaceContents = SrcType.INTERFACE.getTemplate()
                            .replace("#IN", TESTINTERFACE)
                            .replace("#REPLACE", replaceVal);

                    // Contents for class implementing TestInterface
                    replaceVal = expectedVals;
                    String classContents = SrcType.INTERFACEIMPL.getTemplate()
                            .replace("#CN", className).replace("#IN", TESTINTERFACE)
                            .replace("#REPLACE", replaceVal);

                    contents = interfaceContents + classContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        AnnoOnSuperAndSubClass_Inherited_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(2)\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(2)\"}, "
                + // override every annotation on superClass
                "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(2)\"}, "
                + // ignores inherited annotations
                "getDeclAnnoVal = \"@Foo(2)\", " // ignores inherited
                + "getAnnosArgs = {\"@Foo(2)\"}, "
                + "getDeclAnnosArgs = { \"@Foo(2)\" })", // ignores inherited
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"NULL\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(2)\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(2)\"}, "
                + // ignores inherited annotations
                "getDeclAnnoVal = \"NULL\", " + // ignores inherited
                "getAnnosArgs = {}, " + "getDeclAnnosArgs = {})") { // ignores inherited

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = new StringBuilder();

                /*
                Sample test src
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Inherited
                @interface FooContainer {
                Foo[] value();
                }

                @Foo(1)
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                @Foo(2)
                class SubClass extends SuperClass {}
                 */
                // @Inherited only works for classes, no switch cases for
                // method, field, package
                commonStmts.append(Helper.ContentVars.IMPORTEXPECTED.getVal())
                        .append(Helper.ContentVars.IMPORTSTMTS.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.INHERITED.getVal())
                        .append(Helper.ContentVars.BASE.getVal())
                        .append(Helper.ContentVars.INHERITED.getVal())
                        .append(Helper.ContentVars.CONTAINER.getVal());

                if (srcType == SrcType.CLASS) {
                    // Contents for SuperClass
                    anno = "@Foo(1)";
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS).replace("#REPLACE", replaceVal);

                    // Contents for SubClass that extends SuperClass
                    anno = "@Foo(2)";
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className).replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        BasicContainer_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"NULL\","
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnoVal = \"NULL\", " + "getAnnosArgs = {}, "
                + "getDeclAnnosArgs = {} )",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnoVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"} )") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String testSrc = "";
                String pkgInfoContents = "";
                String contents = "";

                JavaFileObject pkgFileObj = null;
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = new StringBuilder();

                anno = Helper.ContentVars.LEGACYCONTAINER.getVal();
                commonStmts.append(Helper.ContentVars.IMPORTEXPECTED.getVal())
                        .append(Helper.ContentVars.IMPORTSTMTS.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.BASE.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.CONTAINER.getVal());
                switch (srcType) {
                    case PACKAGE:
                        /*
                        Sample package-info.java
                        @ExpectedBase
                        @ExpectedContainer
                        @FooContainer(value = {@Foo(1), @Foo(2)})
                        package testpkg;

                        @Retention(RetentionPolicy.RUNTIME)
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @Retention(RetentionPolicy.RUNTIME)
                        @interface FooContainer {
                        Foo[] value();
                        }

                        Sample testSrc:
                        package testpkg;
                        class A {}
                         */
                        testSrc = srcType.getTemplate().replace("#CN", className);
                        contents = testSrc;
                        srcFileObj = Helper.getFile(className, contents);

                        replaceVal = "\n" + expectedVals + "\n" + anno;
                        pkgInfoContents = SrcType.PKGINFO.getTemplate()
                                .replace("#REPLACE1", replaceVal)
                                .replace("#REPLACE2", commonStmts);
                        pkgFileObj = Helper.getFile(PKGINFONAME, pkgInfoContents);
                        files = Arrays.asList(pkgFileObj, srcFileObj);
                        break;
                    default:
                        /*
                        Sample testSrc for class:
                        @Retention(RetentionPolicy.RUNTIME)
                        @Inherited
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @Retention(RetentionPolicy.RUNTIME)
                        @Inherited
                        @interface FooContainer {
                        Foo[] value();
                        }

                        @ExpectedBase
                        @ExpectedContainer
                        @FooContainer(value = {@Foo(1), @Foo(2)})
                        class A {}
                         */
                        replaceVal = expectedVals + anno;
                        testSrc = srcType.getTemplate().replace("#CN", className)
                                .replace("#REPLACE", replaceVal);
                        contents = commonStmts + testSrc;
                        srcFileObj = Helper.getFile(className, contents);
                        files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        SingleAndContainerOnSuper_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\","
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnoVal = \"@Foo(0)\", "
                + "getAnnosArgs = {\"@Foo(0)\"}, "
                + "getDeclAnnosArgs = {\"@Foo(0)\"} )",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnoVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"} )") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String testSrc = "";
                String pkgInfoContents = "";
                String contents = "";

                JavaFileObject pkgFileObj = null;
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = new StringBuilder();

                anno = Helper.ContentVars.BASEANNO.getVal() +
                       Helper.ContentVars.LEGACYCONTAINER.getVal();
                commonStmts.append(Helper.ContentVars.IMPORTEXPECTED.getVal())
                        .append(Helper.ContentVars.IMPORTSTMTS.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.BASE.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.CONTAINER.getVal());
                switch (srcType) {
                    case PACKAGE:
                        /*
                        Sample package-info.java
                        @ExpectedBase
                        @ExpectedContainer
                        @Foo(0)
                        @FooContainer(value = {@Foo(1), @Foo(2)})
                        package testpkg;

                        @Retention(RetentionPolicy.RUNTIME)
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @Retention(RetentionPolicy.RUNTIME)
                        @interface FooContainer {
                        Foo[] value();
                        }

                        Sample testSrc:
                        package testpkg;
                        class A {}
                         */
                        testSrc = srcType.getTemplate().replace("#CN", className);
                        contents = testSrc;

                        srcFileObj = Helper.getFile(className, contents);

                        replaceVal = "\n" + expectedVals + "\n" + anno;
                        pkgInfoContents = SrcType.PKGINFO.getTemplate()
                                .replace("#REPLACE1", replaceVal)
                                .replace("#REPLACE2", commonStmts);
                        pkgFileObj = Helper.getFile(PKGINFONAME, pkgInfoContents);
                        files = Arrays.asList(pkgFileObj, srcFileObj);
                        break;
                    default:
                        /*
                        Sample testSrc for class:
                        @Retention(RetentionPolicy.RUNTIME)
                        @Inherited
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @Retention(RetentionPolicy.RUNTIME)
                        @Inherited
                        @interface FooContainer {
                        Foo[] value();
                        }

                        @ExpectedBase
                        @ExpectedContainer
                        @Foo(0)
                        @FooContainer(value = {@Foo(1), @Foo(2)})
                        class A {}
                         */
                        replaceVal = expectedVals + anno;
                        testSrc = srcType.getTemplate().replace("#CN", className)
                                .replace("#REPLACE", replaceVal);
                        contents = commonStmts + testSrc;

                        srcFileObj = Helper.getFile(className, contents);
                        files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        BasicContainer_Inherited_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"NULL\","
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\"}, "
                + "getDeclAnnoVal = \"NULL\", "
                + "getAnnosArgs = {}, "
                + "getDeclAnnosArgs = {} )",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\"}, "
                + "getDeclAnnoVal = \"NULL\", "
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosArgs = {} )") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(false);

                /*
                Sample testSrc:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface FooContainer {
                Foo[] value();
                }

                @FooContainer(value = {@Foo(1), @Foo(2)})
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                class SubClass extends SuperClass {}
                 */
                // @Inherited only works for classes, no switch cases for
                // method, field, package

                if (srcType == SrcType.CLASS) {
                    // Contents for SuperClass
                    anno = Helper.ContentVars.LEGACYCONTAINER.getVal();
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    // Contents for SubClass that extends SuperClass
                    replaceVal = expectedVals;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        ContainerOnSuperSingleOnSub_Inherited_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(0)\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\"},"
                + "getDeclAnnoVal = \"@Foo(0)\","
                + "getAnnosArgs = {\"@Foo(0)\"},"
                + "getDeclAnnosArgs = {\"@Foo(0)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(0)\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\"},"
                + "getDeclAnnoVal = \"NULL\","
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosArgs = {})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(false);

                /*
                Sample testSrc:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface FooContainer {
                Foo[] value();
                }

                @FooContainer(value = {@Foo(1), @Foo(2)})
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                @Foo(0)
                class SubClass extends SuperClass {}
                 */
                // @Inherited only works for classes, no switch cases for
                // method, field, package

                if (srcType == SrcType.CLASS) {
                    // Contents for SuperClass
                    anno = Helper.ContentVars.LEGACYCONTAINER.getVal();
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    // Contents for SubClass that extends SuperClass
                    anno = Helper.ContentVars.BASEANNO.getVal();
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
        // fail with ordering issues
        ContainerAndSingleOnSuperSingleOnSub_Inherited_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(0)\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\"},"
                + "getDeclAnnoVal = \"@Foo(0)\","
                + "getAnnosArgs = {\"@Foo(0)\"},"
                + "getDeclAnnosArgs = {\"@Foo(0)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(0)\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\"},"
                + "getDeclAnnoVal = \"NULL\","
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosArgs = {})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(false);

                /*
                Sample testSrc:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface FooContainer {
                Foo[] value();
                }

                @FooContainer(value = {@Foo(1), @Foo(2)}) @Foo(3)
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                @Foo(0)
                class SubClass extends SuperClass {}
                 */
                // @Inherited only works for classes, no switch cases for
                // method, field, package

                if (srcType == SrcType.CLASS) {
                    // Contents for SuperClass
                    anno = Helper.ContentVars.LEGACYCONTAINER.getVal()
                            + "@Foo(3)";
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    // Contents for SubClass that extends SuperClass
                    anno = Helper.ContentVars.BASEANNO.getVal();
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className).replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
        // fail with ordering issues
        SingleOnSuperContainerOnSub_Inherited_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnoVal = \"NULL\","
                + "getAnnosArgs = {\"@Foo(0)\"},"
                + "getDeclAnnosArgs = {})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnoVal = \"@FooContainer({@Foo(1), @Foo(2)})\","
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";

                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(false);

                /*
                Sample testSrc:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface FooContainer {
                Foo[] value();
                }

                @Foo(0)
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                @FooContainer(value = {@Foo(1), @Foo(2)})
                class SubClass extends SuperClass {}
                 */

                if (srcType == SrcType.CLASS) {
                    //Contents for SuperClass
                    anno = Helper.ContentVars.BASEANNO.getVal();
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    //Contents for SubClass that extends SuperClass
                    anno = Helper.ContentVars.LEGACYCONTAINER.getVal();
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className).replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
        // fail with ordering issues
        SingleOnSuperContainerAndSingleOnSub_Inherited_Legacy(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(3)\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(3)\"}, "
                + "getDeclAnnosVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(3)\"},"
                + "getDeclAnnoVal = \"@Foo(3)\","
                + "getAnnosArgs = {\"@Foo(3)\"},"
                + "getDeclAnnosArgs = {\"@Foo(3)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(3)\"}, "
                + "getDeclAnnosVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(3)\"},"
                + "getDeclAnnoVal = \"@FooContainer({@Foo(1), @Foo(2)})\","
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";

                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(false);

                /*
                Sample testSrc:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface FooContainer {
                Foo[] value();
                }

                @Foo(0)
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                @FooContainer(value = {@Foo(1), @Foo(2)}) @Foo(3)
                class SubClass extends SuperClass {}
                 */

                if (srcType == SrcType.CLASS) {
                    //Contents for SuperClass
                    anno = Helper.ContentVars.BASEANNO.getVal();
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    //Contents for SubClass that extends SuperClass
                    anno = Helper.ContentVars.LEGACYCONTAINER.getVal()
                            + "@Foo(3)";
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className).replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        BasicRepeatable(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"NULL\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\" }, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnoVal = \"NULL\","
                + "getAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\"},"
                + "getDeclAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\","
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnoVal = \"@FooContainer({@Foo(1), @Foo(2)})\","
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"} )") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String testSrc = "";
                String pkgInfoContents = "";
                String contents = "";

                JavaFileObject pkgFileObj = null;
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = new StringBuilder();

                anno = Helper.ContentVars.REPEATABLEANNO.getVal();
                commonStmts.append(Helper.ContentVars.IMPORTEXPECTED.getVal())
                        .append(Helper.ContentVars.IMPORTSTMTS.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.REPEATABLE.getVal())
                        .append(Helper.ContentVars.BASE.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.CONTAINER.getVal());
                switch (srcType) {
                    case PACKAGE:
                        /*
                        Sample package-info.java
                        @ExpectedBase
                        @ExpectedContainer
                        @Foo(1) @Foo(2)
                        package testpkg;

                        @Retention(RetentionPolicy.RUNTIME)
                        @Repeatable(FooContainer.class)
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @Retention(RetentionPolicy.RUNTIME)
                        @interface FooContainer {
                        Foo[] value();
                        }

                        Sample testSrc:
                        package testpkg;
                        class A {}
                         */
                        testSrc = srcType.getTemplate().replace("#CN", className);
                        contents = testSrc;
                        srcFileObj = Helper.getFile(className, contents);

                        replaceVal = expectedVals + "\n" + anno;
                        pkgInfoContents = SrcType.PKGINFO.getTemplate()
                                .replace("#REPLACE1", replaceVal)
                                .replace("#REPLACE2", commonStmts);
                        pkgFileObj = Helper.getFile(PKGINFONAME, pkgInfoContents);
                        files = Arrays.asList(pkgFileObj, srcFileObj);
                        break;
                    default:
                        /*
                        Sample testSrc for class:
                        @Retention(RetentionPolicy.RUNTIME)
                        @Repeatable(FooContainer.class)
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @Retention(RetentionPolicy.RUNTIME)
                        @interface FooContainer {
                        Foo[] value();
                        }

                        @ExpectedBase
                        @ExpectedContainer
                        @Foo(1) @Foo(2)
                        class A { }
                         */
                        replaceVal = expectedVals + anno;
                        testSrc = srcType.getTemplate().replace("#CN", className)
                                .replace("#REPLACE", replaceVal);
                        contents = commonStmts + testSrc;
                        srcFileObj = Helper.getFile(className, contents);
                        files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        BasicContainerRepeatable(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"NULL\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnoVal = \"NULL\","
                + "getAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\"},"
                + "getDeclAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\","
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnoVal = \"@FooContainer({@Foo(1), @Foo(2)})\","
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"} )") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String testSrc = "";
                String pkgInfoContents = "";
                String contents = "";

                JavaFileObject pkgFileObj = null;
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = new StringBuilder();

                anno = Helper.ContentVars.LEGACYCONTAINER.getVal();
                commonStmts.append(Helper.ContentVars.IMPORTEXPECTED.getVal())
                        .append(Helper.ContentVars.IMPORTSTMTS.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.REPEATABLE.getVal())
                        .append(Helper.ContentVars.BASE.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.CONTAINER.getVal());
                switch (srcType) {
                    case PACKAGE:
                        /*
                        Sample package-info.java
                        @ExpectedBase
                        @ExpectedContainer
                        @FooContainer(value = {@Foo(1), @Foo(2)})
                        package testpkg;

                        @Retention(RetentionPolicy.RUNTIME)
                        @Repeatable(FooContainer.class)
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @Retention(RetentionPolicy.RUNTIME)
                        @interface FooContainer {
                        Foo[] value();
                        }

                        Sample testSrc:
                        package testpkg;
                        class A {}
                         */
                        testSrc = srcType.getTemplate().replace("#CN", className);
                        contents = testSrc;
                        srcFileObj = Helper.getFile(className, contents);

                        replaceVal = expectedVals + "\n" + anno;
                        pkgInfoContents = SrcType.PKGINFO.getTemplate()
                                .replace("#REPLACE1", replaceVal)
                                .replace("#REPLACE2", commonStmts);
                        pkgFileObj = Helper.getFile(PKGINFONAME, pkgInfoContents);
                        files = Arrays.asList(pkgFileObj, srcFileObj);
                        break;
                    default:
                        /*
                        Sample testSrc for class:
                        @Retention(RetentionPolicy.RUNTIME)
                        @Repeatable(FooContainer.class)
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @Retention(RetentionPolicy.RUNTIME)
                        @interface FooContainer {
                        Foo[] value();
                        }

                        @ExpectedBase
                        @ExpectedContainer
                        @FooContainer(value = {@Foo(1), @Foo(2)})
                        class A { }
                         */
                        replaceVal = expectedVals + anno;
                        testSrc = srcType.getTemplate().replace("#CN", className)
                                .replace("#REPLACE", replaceVal);
                        contents = commonStmts + testSrc;
                        srcFileObj = Helper.getFile(className, contents);
                        files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        BasicContainerRepeatable_Inherited(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"NULL\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\"}, "
                + "getDeclAnnoVal = \"NULL\", "
                + "getAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\"}, "
                + "getDeclAnnosArgs = {})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = { \"ExpectedBase\", \"ExpectedContainer\"}, "
                + "getDeclAnnoVal = \"NULL\", "
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosArgs = {})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(true);
                /*
                Sample testSrc:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @Repeatable(FooContainer.class)
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface FooContainer {
                Foo[] value();
                }

                @FooContainer(value = {@Foo(1), @Foo(2)})
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                class SubClass extends SuperClass { }
                 */
                // @Inherited only works for classes, no switch cases for
                // method, field, package
                anno = Helper.ContentVars.LEGACYCONTAINER.getVal();

                if (srcType == SrcType.CLASS) {
                    // Contents for SuperClass
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    // Contents for SubClass that extends SuperClass
                    replaceVal = expectedVals;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        RepeatableAnnoInherited(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"NULL\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\"}, "
                + // ignores inherited annotations
                "getDeclAnnoVal = \"NULL\", "
                + // ignores inherited
                "getAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\"}, "
                + "getDeclAnnosArgs = {})", // ignores inherited
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = { \"ExpectedBase\", \"ExpectedContainer\"}, "
                + // ignores inherited annotations
                "getDeclAnnoVal = \"NULL\", "
                + // ignores inherited
                "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosArgs = {})") { // ignores inherited

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(true);
                /*
                Sample testSrc:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @Repeatable(FooContainer.class)
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface FooContainer {
                Foo[] value();
                }

                @Foo(1) @Foo(2)
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                class SubClass extends SuperClass { }
                 */
                // @Inherited only works for classes, no switch cases for
                // method, field, package
                anno = Helper.ContentVars.REPEATABLEANNO.getVal();

                if (srcType == SrcType.CLASS) {
                    // Contents for SuperClass
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    // Contents for SubClass that extends SuperClass
                    replaceVal = expectedVals;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
        // fail with ordering issues
        SingleAnnoWithContainer(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnoVal = \"@Foo(0)\","
                + "getAnnosArgs = {\"@Foo(0)\", \"@Foo(1)\", \"@Foo(2)\"},"
                + "getDeclAnnosArgs = {\"@Foo(0)\", \"@Foo(1)\",\"@Foo(2)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnoVal = \"@FooContainer({@Foo(1), @Foo(2)})\","
                + "getDeclAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String testSrc = "";
                String pkgInfoContents = "";
                String contents = "";

                JavaFileObject pkgFileObj = null;
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = new StringBuilder();

                anno = Helper.ContentVars.BASEANNO.getVal() + " "
                        + Helper.ContentVars.LEGACYCONTAINER.getVal();
                commonStmts.append(Helper.ContentVars.IMPORTEXPECTED.getVal())
                        .append(Helper.ContentVars.IMPORTSTMTS.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.REPEATABLE.getVal())
                        .append(Helper.ContentVars.BASE.getVal())
                        .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
                        .append(Helper.ContentVars.CONTAINER.getVal());
                switch (srcType) {
                    case PACKAGE:
                        /*
                        Sample package-info.java
                        @ExpectedBase
                        @ExpectedContainer
                        @Foo(0) @FooContainer(value = {@Foo(1), @Foo(2)})
                        package testpkg;

                        @Retention(RetentionPolicy.RUNTIME)
                        @Repeatable(FooContainer.class)
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @Retention(RetentionPolicy.RUNTIME)
                        @interface FooContainer {
                        Foo[] value();
                        }

                        Sample testSrc:
                        package testpkg;
                        class A {}
                         */
                        testSrc = srcType.getTemplate().replace("#CN", className);
                        contents = testSrc;
                        srcFileObj = Helper.getFile(className, contents);

                        replaceVal = expectedVals + "\n" + anno;
                        pkgInfoContents = SrcType.PKGINFO.getTemplate()
                                .replace("#REPLACE1", replaceVal)
                                .replace("#REPLACE2", commonStmts);
                        pkgFileObj = Helper.getFile(PKGINFONAME, pkgInfoContents);
                        files = Arrays.asList(pkgFileObj, srcFileObj);
                        break;
                    default:
                        /*
                        Sample testSrc:
                        @Retention(RetentionPolicy.RUNTIME)
                        @Inherited
                        @Repeatable(FooContainer.class)
                        @interface Foo {int value() default Integer.MAX_VALUE;}

                        @Retention(RetentionPolicy.RUNTIME)
                        @Inherited
                        @interface FooContainer {
                        Foo[] value();
                        }

                        @ExpectedBase
                        @ExpectedContainer
                        @Foo(0) @FooContainer(value = {@Foo(1), @Foo(2)})
                        class A { }
                         */
                        replaceVal = expectedVals + anno;
                        testSrc = srcType.getTemplate()
                                .replace("#CN", className)
                                .replace("#REPLACE", replaceVal);
                        contents = commonStmts + testSrc;
                        srcFileObj = Helper.getFile(className, contents);
                        files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        AnnoOnSuperAndSubClass_Inherited(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(1)\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(1)\" }, "
                + // override every annotation on superClass
                "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(1)\"}, "
                + // ignores inherited annotations
                "getDeclAnnoVal = \"@Foo(1)\", " // ignores inherited
                + "getAnnosArgs = {\"@Foo(1)\"}, "
                + "getDeclAnnosArgs = { \"@Foo(1)\" })", // ignores inherited
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"NULL\", "
                + "getAnnotationsVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(1)\" }, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(1)\"}, "
                + // ignores inherited annotations
                "getDeclAnnoVal = \"NULL\", " + // ignores inherited
                "getAnnosArgs = {}, " + "getDeclAnnosArgs = {})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(true);

                /*
                Sample testSrc:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @Repeatable(FooContainer.class)
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface FooContainer {
                Foo[] value();
                }

                @Foo(0)
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                @Foo(1)
                class SubClass extends SuperClass { }
                 */
                // @Inherited only works for classes, no switch cases for
                // method, field, package

                if (srcType == SrcType.CLASS) {
                    // Contents for SuperClass
                    anno = Helper.ContentVars.BASEANNO.getVal();
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    // Contents for SubClass that extends SuperClass
                    replaceVal = expectedVals + "\n" + "@Foo(1)";
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
        // fail with ordering issues
        RepeatableOnSuperSingleOnSub_Inherited(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(3)\", "
                + "getAnnotationsVals = {"
                + "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(3)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + //override every annotation on superClass
                "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(3)\"}, "
                + // ignores inherited annotations
                "getDeclAnnoVal = \"@Foo(3)\", " // ignores inherited
                + "getAnnosArgs = {\"@Foo(3)\"}, "
                + "getDeclAnnosArgs = { \"@Foo(3)\" })", // ignores inherited
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                + "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(3)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(3)\"}, "
                + // ignores inherited annotations
                "getDeclAnnoVal = \"NULL\", "
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosArgs = {}) // ignores inherited ") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(true);

                /*
                 Sample testSrc:
                 @Retention(RetentionPolicy.RUNTIME)
                 @Inherited
                 @Repeatable(FooContainer.class)
                 @interface Foo {int value() default Integer.MAX_VALUE;}

                 @Retention(RetentionPolicy.RUNTIME)
                 @Inherited
                 @interface FooContainer {
                 Foo[] value();
                 }

                 @Foo(1) @Foo(2)
                 class SuperClass { }

                 @ExpectedBase
                 @ExpectedContainer
                 @Foo(3)
                 class SubClass extends SuperClass { }
                 */
                //@Inherited only works for classes, no switch cases for method, field, package
                if (srcType == SrcType.CLASS) {
                    //Contents for SuperClass
                    anno = Helper.ContentVars.REPEATABLEANNO.getVal();
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    //Contents for SubClass that extends SuperClass
                    anno = "@Foo(3)";
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);
                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
        // fail with ordering issues
        SingleOnSuperRepeatableOnSub_Inherited(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\", "
                + "getAnnotationsVals = {"
                + "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + //override every annotation on superClass
                "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + // ignores inherited annotations
                "getDeclAnnoVal = \"NULL\","// ignores inherited
                + "getAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\"}, "
                + "getDeclAnnosArgs = { \"@Foo(1)\", \"@Foo(2)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                + "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + // ignores inherited annotations
                "getDeclAnnoVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "// ignores inherited
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(true);

                /*
                 Sample testSrc:
                 @Retention(RetentionPolicy.RUNTIME)
                 @Inherited
                 @Repeatable(FooContainer.class)
                 @interface Foo {int value() default Integer.MAX_VALUE;}

                 @Retention(RetentionPolicy.RUNTIME)
                 @Inherited
                 @interface FooContainer {
                 Foo[] value();
                 }

                 @Foo(0)
                 class SuperClass { }

                 @ExpectedBase
                 @ExpectedContainer
                 @Foo(1) @Foo(2)
                 class SubClass extends SuperClass { }
                 */
                //@Inherited only works for classes, no switch cases for method, field, package
                if (srcType == SrcType.CLASS) {
                    //Contents for SuperClass
                    anno = Helper.ContentVars.BASEANNO.getVal();
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    //Contents for SubClass that extends SuperClass
                    anno = Helper.ContentVars.REPEATABLEANNO.getVal();
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
        // fail with ordering issues
        ContainerOnSuperSingleOnSub_Inherited(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\", "
                + "getAnnotationsVals = {"
                + "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\"},"
                + "getDeclAnnoVal = \"@Foo(0)\","
                + "getAnnosArgs = {\"@Foo(0)\"},"
                + "getDeclAnnosArgs = {\"@Foo(0)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                + "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\"},"
                + "getDeclAnnoVal = \"NULL\","
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosArgs = {})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(true);

                /*
                 Sample testSrc:
                 @Retention(RetentionPolicy.RUNTIME)
                 @Inherited
                 @Repeatable(FooContainer.class)
                 @interface Foo {int value() default Integer.MAX_VALUE;}

                 @Retention(RetentionPolicy.RUNTIME)
                 @Inherited
                 @interface FooContainer {
                 Foo[] value();
                 }

                 @FooContainer(value = {@Foo(1), @Foo(2)})
                 class SuperClass { }

                 @ExpectedBase
                 @ExpectedContainer
                 @Foo(0)
                 class SubClass extends SuperClass { }
                 */
                //@Inherited only works for classes, no switch cases for method, field, package
                if (srcType == SrcType.CLASS) {
                    //Contents for SuperClass
                    anno = Helper.ContentVars.LEGACYCONTAINER.getVal();
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    //Contents for SubClass that extends SuperClass
                    anno = Helper.ContentVars.BASEANNO.getVal();
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
        // fail with ordering issues
        SingleOnSuperContainerOnSub_Inherited(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\", "
                + "getAnnotationsVals = {"
                + "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnoVal = \"NULL\","
                + "getAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\"},"
                + "getDeclAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                + "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnoVal = \"@FooContainer({@Foo(1), @Foo(2)})\","
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                     String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(true);

                /*
                 Sample testSrc:
                 @Retention(RetentionPolicy.RUNTIME)
                 @Inherited
                 @Repeatable(FooContainer.class)
                 @interface Foo {int value() default Integer.MAX_VALUE;}

                 @Retention(RetentionPolicy.RUNTIME)
                 @Inherited
                 @interface FooContainer {
                 Foo[] value();
                 }

                 @Foo(0)
                 class SuperClass { }

                 @ExpectedBase
                 @ExpectedContainer
                 @FooContainer(value = {@Foo(1), @Foo(2)})
                 class SubClass extends SuperClass { }
                 */
                //@Inherited only works for classes, no switch cases for method, field, package
                if (srcType == SrcType.CLASS) {
                    //Contents for SuperClass
                    anno = Helper.ContentVars.BASEANNO.getVal();
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    //Contents for SubClass that extends SuperClass
                    anno = Helper.ContentVars.LEGACYCONTAINER.getVal();
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
        // fail with ordering issues
        SingleOnSuperContainerAndSingleOnSub_Inherited(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(3)\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(3)\"}, "
                + "getDeclAnnosVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(3)\"},"
                + "getDeclAnnoVal = \"@Foo(3)\","
                + "getAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\", \"@Foo(3)\"},"
                + "getDeclAnnosArgs = {\"@Foo(1)\", \"@Foo(2)\", \"@Foo(3)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(3)\"}, "
                + "getDeclAnnosVals = {"
                +       "\"ExpectedBase\", \"ExpectedContainer\", \"@FooContainer({@Foo(1), @Foo(2)})\", \"@Foo(3)\"},"
                + "getDeclAnnoVal = \"@FooContainer({@Foo(1), @Foo(2)})\","
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;
                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(true);

                /*
                Sample testSrc:
                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @interface Foo {int value() default Integer.MAX_VALUE;}

                @Retention(RetentionPolicy.RUNTIME)
                @Inherited
                @Repeatable(FooContainer.class)
                @interface FooContainer {
                Foo[] value();
                }

                @Foo(0)
                class SuperClass { }

                @ExpectedBase
                @ExpectedContainer
                @FooContainer(value = {@Foo(1), @Foo(2)}) @Foo(3)
                class SubClass extends SuperClass {}
                 */

                if (srcType == SrcType.CLASS) {
                    //Contents for SuperClass
                    anno = Helper.ContentVars.BASEANNO.getVal();
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    //Contents for SubClass that extends SuperClass
                    anno = Helper.ContentVars.LEGACYCONTAINER.getVal()
                            + "@Foo(3)";
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        },
        // @ignore 8025924: Several test cases in repeatingAnnotations/combo/ReflectionTest
        // fail with ordering issues
        ContainerAndSingleOnSuperSingleOnSub_Inherited(
        "@ExpectedBase(value=Foo.class, "
                + "getAnnotationVal = \"@Foo(0)\", "
                + "getAnnotationsVals = {"
                + "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\"},"
                + "getDeclAnnoVal = \"@Foo(0)\","
                + "getAnnosArgs = {\"@Foo(0)\"},"
                + "getDeclAnnosArgs = {\"@Foo(0)\"})",
        "@ExpectedContainer(value=FooContainer.class, "
                + "getAnnotationVal = \"@FooContainer({@Foo(1), @Foo(2)})\", "
                + "getAnnotationsVals = {"
                + "\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\", \"@FooContainer({@Foo(1), @Foo(2)})\"}, "
                + "getDeclAnnosVals = {\"ExpectedBase\", \"ExpectedContainer\", \"@Foo(0)\"},"
                + "getDeclAnnoVal = \"NULL\","
                + "getAnnosArgs = {\"@FooContainer({@Foo(1), @Foo(2)})\"},"
                + "getDeclAnnosArgs = {})") {

            @Override
            public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                    String className) {
                String anno = "";
                String replaceVal = "";
                String contents = "";
                JavaFileObject srcFileObj = null;
                Iterable<? extends JavaFileObject> files = null;

                String expectedVals = "\n" + getExpectedBase() + "\n"
                        + getExpectedContainer() + "\n";
                StringBuilder commonStmts = getCommonStmts(true);

                /*
                 Sample testSrc:
                 @Retention(RetentionPolicy.RUNTIME)
                 @Inherited
                 @Repeatable(FooContainer.class)
                 @interface Foo {int value() default Integer.MAX_VALUE;}

                 @Retention(RetentionPolicy.RUNTIME)
                 @Inherited
                 @interface FooContainer {
                 Foo[] value();
                 }

                 @FooContainer(value = {@Foo(1), @Foo(2)})
                 @Foo(3)
                 class SuperClass { }

                 @ExpectedBase
                 @ExpectedContainer
                 @Foo(0)
                 class SubClass extends SuperClass { }
                 */

                //@Inherited only works for classes, no switch cases for method, field, package
                if (srcType == SrcType.CLASS) {
                    //Contents for SuperClass
                    anno = Helper.ContentVars.LEGACYCONTAINER.getVal()
                            + "@Foo(3)" ;
                    replaceVal = commonStmts + "\n" + anno;
                    String superClassContents = srcType.getTemplate()
                            .replace("#CN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    //Contents for SubClass that extends SuperClass
                    anno = Helper.ContentVars.BASEANNO.getVal();
                    replaceVal = expectedVals + "\n" + anno;
                    String subClassContents = SrcType.CLASSEXTENDS.getTemplate()
                            .replace("#CN", className)
                            .replace("#SN", SUPERCLASS)
                            .replace("#REPLACE", replaceVal);

                    contents = superClassContents + subClassContents;
                    srcFileObj = Helper.getFile(className, contents);
                    files = Arrays.asList(srcFileObj);
                }
                return files;
            }
        };

         private String expectedBase, expectedContainer;

         private TestCase(String expectedBase, String expectedContainer) {
             this.expectedBase = expectedBase;
             this.expectedContainer = expectedContainer;
         }

         public String getExpectedBase() {
             return expectedBase;
         }

         public String getExpectedContainer() {
             return expectedContainer;
         }

         // Each enum element should override this method
         public Iterable<? extends JavaFileObject> getTestFiles(SrcType srcType,
                 String className) {
             return null;
         }
    }

    /*
     * Each srctype has its template defined used for test src generation
     * Primary src types: class, method, field, package define
     *                    getExpectedBase() and getExpectedContainer()
     */
    enum SrcType {

        CLASS("\n#REPLACE\nclass #CN { } ") {

            @Override
            public ExpectedBase getExpectedBase(Class<?> c) {
                return c.getAnnotation(ExpectedBase.class);
            }

            @Override
            public ExpectedContainer getExpectedContainer(Class<?> c) {
                return c.getAnnotation(ExpectedContainer.class);
            }
        },
        METHOD("class #CN  {\n" + "   " + "#REPLACE\n" + "   void "
        + TESTMETHOD + "() {} \n" + "}\n") {

            @Override
            public ExpectedBase getExpectedBase(Class<?> c) {
                ExpectedBase ret = null;
                try {
                    ret = c.getDeclaredMethod(TESTMETHOD).getAnnotation(
                            ExpectedBase.class);
                } catch (NoSuchMethodException nme) {
                    error("Could not get " + TESTMETHOD + " for className "
                            + c.getName() + " Exception:\n" + nme);
                }
                return ret;
            }

            @Override
            public ExpectedContainer getExpectedContainer(Class<?> c) {
                ExpectedContainer ret = null;
                try {
                    ret = c.getDeclaredMethod(TESTMETHOD).getAnnotation(
                            ExpectedContainer.class);
                } catch (NoSuchMethodException nme) {
                    error("Could not get " + TESTMETHOD + " for className "
                            + c.getName() + " Exception:\n" + nme);
                }
                return ret;

            }
        },
        FIELD("class #CN  {\n" + "   " + "#REPLACE\n" + "   int " + TESTFIELD
        + " = 0; \n" + "}\n") {

            @Override
            public ExpectedBase getExpectedBase(Class<?> c) {
                ExpectedBase ret = null;
                try {
                    ret = c.getDeclaredField(TESTFIELD).getAnnotation(
                            ExpectedBase.class);
                } catch (NoSuchFieldException nme) {
                    error("Could not get " + TESTFIELD + " for className "
                            + c.getName() + " Exception:\n" + nme);
                }
                return ret;
            }

            @Override
            public ExpectedContainer getExpectedContainer(Class<?> c) {
                ExpectedContainer ret = null;
                try {
                    ret = c.getDeclaredField(TESTFIELD).getAnnotation(
                            ExpectedContainer.class);
                } catch (NoSuchFieldException nme) {
                    error("Could not get " + TESTFIELD + " for className "
                            + c.getName() + " Exception:\n" + nme);
                }
                return ret;

            }
        },
        PACKAGE("package " + TESTPKG + "; \n" + "class #CN {}") {

            @Override
            public ExpectedBase getExpectedBase(Class<?> c) {
                return c.getPackage().getAnnotation(ExpectedBase.class);
            }

            @Override
            public ExpectedContainer getExpectedContainer(Class<?> c) {
                return c.getPackage().getAnnotation(ExpectedContainer.class);
            }
        },
        PKGINFO("#REPLACE1\npackage " + TESTPKG + "; \n" + "#REPLACE2"),
        INTERFACE("#REPLACE\ninterface #IN { } "),
        INTERFACEIMPL("#REPLACE\nclass #CN implements #IN {}"),
        CLASSEXTENDS("#REPLACE\nclass #CN extends #SN {}");
        String template;

        private SrcType(String template) {
            this.template = template;
        }

        public String getTemplate() {
            return template;
        }

        // Elements should override
        public ExpectedBase getExpectedBase(Class<?> c) {
            return null;
        }

        public ExpectedContainer getExpectedContainer(Class<?> c) {
            return null;
        }

        /*
         * Returns only primary src types ;
         */
        public static SrcType[] getSrcTypes() {
            return new SrcType[]{CLASS, PACKAGE, METHOD, FIELD};
        }
    }

    /*
     * Each enum constant is one of the 6 methods from AnnotatedElement interface
     * that needs to be tested.
     * Each enum constant overrides these 4 methods:
     * - getActualAnnoBase(SrcType srcType, Class<?> c)
     * - getActualAnnoContainer(SrcType srcType, Class<?> c)
     * - getExpectedAnnoBase(SrcType srcType, Class<?> c)
     * - getExpectedAnnoContainer(SrcType srcType, Class<?> c)
     */
    enum TestMethod {

        GET_ANNO("getAnnotation") {

            @Override
            public Annotation[] getActualAnnoBase(SrcType srcType, Class<?> c) {
                Annotation[] actualAnno = new Annotation[1];
                switch (srcType) {
                    case CLASS:
                        actualAnno[0] = c.getAnnotation(srcType.getExpectedBase(c).value());
                        break;
                    case PACKAGE:
                        actualAnno[0] = c.getPackage().getAnnotation(
                                srcType.getExpectedBase(c).value());
                        break;
                    case METHOD:
                        try {
                            actualAnno[0] = c.getDeclaredMethod(TESTMETHOD).getAnnotation(
                                    srcType.getExpectedBase(c).value());
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualAnno[0] = c.getDeclaredField(TESTFIELD).getAnnotation(
                                    srcType.getExpectedBase(c).value());
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualAnno;
            }

            @Override
            public Annotation[] getActualAnnoContainer(SrcType srcType,
                    Class<?> c) {
                Annotation[] actualAnno = new Annotation[1];
                switch (srcType) {
                    case CLASS:
                        actualAnno[0] = c.getAnnotation(srcType.getExpectedContainer(c).value());
                        break;
                    case PACKAGE:
                        actualAnno[0] = c.getPackage().getAnnotation(
                                srcType.getExpectedContainer(c).value());
                        break;
                    case METHOD:
                        try {
                            actualAnno[0] = c.getDeclaredMethod(TESTMETHOD).getAnnotation(
                                    srcType.getExpectedContainer(c).value());
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualAnno[0] = c.getDeclaredField(TESTFIELD).getAnnotation(
                                    srcType.getExpectedContainer(c).value());
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualAnno;
            }

            @Override
            public String[] getExpectedAnnoBase(SrcType srcType, Class<?> c) {
                String[] expAnno = {srcType.getExpectedBase(c).getAnnotationVal()};
                return expAnno;
            }

            @Override
            public String[] getExpectedAnnoContainer(SrcType srcType, Class<?> c) {
                String[] expAnno = {srcType.getExpectedContainer(c).getAnnotationVal()};
                return expAnno;
            }
        },
        GET_ANNOS("getAnnotations") {

            @Override
            public Annotation[] getActualAnnoBase(SrcType srcType, Class<?> c) {
                Annotation[] actualAnnos = null;
                switch (srcType) {
                    case CLASS:
                        actualAnnos = c.getAnnotations();
                        break;
                    case PACKAGE:
                        actualAnnos = c.getPackage().getAnnotations();
                        break;
                    case METHOD:
                        try {
                            actualAnnos = c.getDeclaredMethod(TESTMETHOD).getAnnotations();
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualAnnos = c.getDeclaredField(TESTFIELD).getAnnotations();
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualAnnos;
            }

            @Override
            public Annotation[] getActualAnnoContainer(SrcType srcType,
                    Class<?> c) {
                Annotation[] actualAnnos = null;
                switch (srcType) {
                    case CLASS:
                        actualAnnos = c.getAnnotations();
                        break;
                    case PACKAGE:
                        actualAnnos = c.getPackage().getAnnotations();
                        break;
                    case METHOD:
                        try {
                            actualAnnos = c.getDeclaredMethod(TESTMETHOD).getAnnotations();
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualAnnos = c.getDeclaredField(TESTFIELD).getAnnotations();
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualAnnos;
            }

            @Override
            public String[] getExpectedAnnoBase(SrcType srcType, Class<?> c) {
                return srcType.getExpectedBase(c).getAnnotationsVals();
            }

            @Override
            public String[] getExpectedAnnoContainer(SrcType srcType, Class<?> c) {
                return srcType.getExpectedContainer(c).getAnnotationsVals();
            }
        },
        GET_DECL_ANNOS("getDeclaredAnnotations") {

            @Override
            public Annotation[] getActualAnnoBase(SrcType srcType, Class<?> c) {
                Annotation[] actualDeclAnnos = null;
                switch (srcType) {
                    case CLASS:
                        actualDeclAnnos = c.getDeclaredAnnotations();
                        break;
                    case PACKAGE:
                        actualDeclAnnos = c.getPackage().getDeclaredAnnotations();
                        break;
                    case METHOD:
                        try {
                            actualDeclAnnos = c.getDeclaredMethod(TESTMETHOD)
                                    .getDeclaredAnnotations();
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualDeclAnnos = c.getDeclaredField(TESTFIELD)
                                    .getDeclaredAnnotations();
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualDeclAnnos;
            }

            @Override
            public Annotation[] getActualAnnoContainer(SrcType srcType,
                    Class<?> c) {
                Annotation[] actualDeclAnnos = null;
                switch (srcType) {
                    case CLASS:
                        actualDeclAnnos = c.getDeclaredAnnotations();
                        break;
                    case PACKAGE:
                        actualDeclAnnos = c.getPackage().getDeclaredAnnotations();
                        break;
                    case METHOD:
                        try {
                            actualDeclAnnos = c.getDeclaredMethod(TESTMETHOD)
                                    .getDeclaredAnnotations();
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualDeclAnnos = c.getDeclaredField(TESTFIELD)
                                    .getDeclaredAnnotations();
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualDeclAnnos;
            }

            @Override
            public String[] getExpectedAnnoBase(SrcType srcType, Class<?> c) {
                return srcType.getExpectedBase(c).getDeclAnnosVals();
            }

            @Override
            public String[] getExpectedAnnoContainer(SrcType srcType, Class<?> c) {
                return srcType.getExpectedContainer(c).getDeclAnnosVals();
            }
        },
        GET_DECL_ANNO("getDeclaredAnnotation") {

            @Override
            public Annotation[] getActualAnnoBase(SrcType srcType, Class<?> c) {
                Annotation[] actualDeclAnno = new Annotation[1];
                switch (srcType) {
                    case CLASS:
                        actualDeclAnno[0] = c.getDeclaredAnnotation(
                                srcType.getExpectedBase(c).value());
                        break;
                    case PACKAGE:
                        actualDeclAnno[0] = c.getPackage().getDeclaredAnnotation(
                                srcType.getExpectedBase(c).value());
                        break;
                    case METHOD:
                        try {
                            actualDeclAnno[0] = c.getDeclaredMethod(TESTMETHOD)
                                    .getDeclaredAnnotation(
                                        srcType.getExpectedBase(c).value());
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualDeclAnno[0] = c.getDeclaredField(TESTFIELD)
                                    .getDeclaredAnnotation(
                                        srcType.getExpectedBase(c).value());
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualDeclAnno;
            }

            @Override
            public Annotation[] getActualAnnoContainer(SrcType srcType,
                    Class<?> c) {
                Annotation[] actualDeclAnno = new Annotation[1];
                switch (srcType) {
                    case CLASS:
                        actualDeclAnno[0] = c.getDeclaredAnnotation(
                                srcType.getExpectedContainer(c).value());
                        break;
                    case PACKAGE:
                        actualDeclAnno[0] = c.getPackage().getDeclaredAnnotation(
                                srcType.getExpectedContainer(c).value());
                        break;
                    case METHOD:
                        try {
                            actualDeclAnno[0] = c.getDeclaredMethod(TESTMETHOD)
                                    .getDeclaredAnnotation(
                                        srcType.getExpectedContainer(c).value());
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualDeclAnno[0] = c.getDeclaredField(TESTFIELD)
                                    .getDeclaredAnnotation(
                                        srcType.getExpectedContainer(c).value());
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualDeclAnno;
            }

            @Override
            public String[] getExpectedAnnoBase(SrcType srcType, Class<?> c) {
                String[] expAnno = {srcType.getExpectedBase(c).getDeclAnnoVal()};
                return expAnno;
            }

            @Override
            public String[] getExpectedAnnoContainer(SrcType srcType, Class<?> c) {
                String[] expAnno = {srcType.getExpectedContainer(c).getDeclAnnoVal()};
                return expAnno;
            }
        }, // new
        GET_ANNOS_ARG("getAnnotationsArg") {

            @Override
            public Annotation[] getActualAnnoBase(SrcType srcType, Class<?> c) {
                Annotation[] actualAnnoArgs = null;
                switch (srcType) {
                    case CLASS:
                        actualAnnoArgs = c.getAnnotationsByType(srcType.getExpectedBase(c).value());
                        break;
                    case PACKAGE:
                        actualAnnoArgs = c.getPackage().getAnnotationsByType(
                                srcType.getExpectedBase(c).value());
                        break;
                    case METHOD:
                        try {
                            actualAnnoArgs = c.getDeclaredMethod(TESTMETHOD)
                                    .getAnnotationsByType(
                                        srcType.getExpectedBase(c).value());
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualAnnoArgs = c.getDeclaredField(TESTFIELD)
                                    .getAnnotationsByType(
                                        srcType.getExpectedBase(c).value());
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualAnnoArgs;
            }

            @Override
            public Annotation[] getActualAnnoContainer(SrcType srcType,
                    Class<?> c) {
                Annotation[] actualAnnoArgs = null;
                switch (srcType) {
                    case CLASS:
                        actualAnnoArgs = c.getAnnotationsByType(srcType.getExpectedContainer(c).value());
                        break;
                    case PACKAGE:
                        actualAnnoArgs = c.getPackage().getAnnotationsByType(
                                srcType.getExpectedContainer(c).value());
                        break;
                    case METHOD:
                        try {
                            actualAnnoArgs = c.getDeclaredMethod(TESTMETHOD)
                                    .getAnnotationsByType(
                                        srcType.getExpectedContainer(c).value());
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualAnnoArgs = c.getDeclaredField(TESTFIELD)
                                    .getAnnotationsByType(
                                        srcType.getExpectedContainer(c).value());
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualAnnoArgs;
            }

            @Override
            public String[] getExpectedAnnoBase(SrcType srcType, Class<?> c) {
                return srcType.getExpectedBase(c).getAnnosArgs();
            }

            @Override
            public String[] getExpectedAnnoContainer(SrcType srcType, Class<?> c) {
                return srcType.getExpectedContainer(c).getAnnosArgs();
            }
        }, // new
        GET_DECL_ANNOS_ARG("getDeclAnnosArg") {

            @Override
            public Annotation[] getActualAnnoBase(SrcType srcType, Class<?> c) {
                Annotation[] actualDeclAnnosArgs = null;
                switch (srcType) {
                    case CLASS:
                        actualDeclAnnosArgs = c.getDeclaredAnnotationsByType(
                                srcType.getExpectedBase(c).value());
                        break;
                    case PACKAGE:
                        actualDeclAnnosArgs = c.getPackage().getDeclaredAnnotationsByType(
                                srcType.getExpectedBase(c).value());
                        break;
                    case METHOD:
                        try {
                            actualDeclAnnosArgs = c.getDeclaredMethod(TESTMETHOD)
                                    .getDeclaredAnnotationsByType(
                                        srcType.getExpectedBase(c).value());
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualDeclAnnosArgs = c.getDeclaredField(TESTFIELD)
                                    .getDeclaredAnnotationsByType(
                                        srcType.getExpectedBase(c).value());
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualDeclAnnosArgs;
            }

            @Override
            public Annotation[] getActualAnnoContainer(SrcType srcType,
                    Class<?> c) {
                Annotation[] actualDeclAnnosArgs = null;
                switch (srcType) {
                    case CLASS:
                        actualDeclAnnosArgs = c.getDeclaredAnnotationsByType(
                                srcType.getExpectedContainer(c).value());
                        break;
                    case PACKAGE:
                        actualDeclAnnosArgs = c.getPackage().getDeclaredAnnotationsByType(
                                srcType.getExpectedContainer(c).value());
                        break;
                    case METHOD:
                        try {
                            actualDeclAnnosArgs = c.getDeclaredMethod(TESTMETHOD)
                                    .getDeclaredAnnotationsByType(
                                        srcType.getExpectedContainer(c).value());
                        } catch (NoSuchMethodException nme) {
                            error("Could not get " + TESTMETHOD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nme);
                        }
                        break;
                    case FIELD:
                        try {
                            actualDeclAnnosArgs = c.getDeclaredField(TESTFIELD)
                                   .getDeclaredAnnotationsByType(
                                        srcType.getExpectedContainer(c).value());
                        } catch (NoSuchFieldException nfe) {
                            error("Could not get " + TESTFIELD
                                    + " for className = " + c.getName()
                                    + "Exception = " + nfe);
                        }
                        break;
                }
                return actualDeclAnnosArgs;
            }

            @Override
            public String[] getExpectedAnnoBase(SrcType srcType, Class<?> c) {
                return srcType.getExpectedBase(c).getDeclAnnosArgs();
            }

            @Override
            public String[] getExpectedAnnoContainer(SrcType srcType, Class<?> c) {
                return srcType.getExpectedContainer(c).getDeclAnnosArgs();
            }
        }; // new
        String name;

        private TestMethod(String name) {
            this.name = name;
        }

        public Annotation[] getActualAnnoBase(SrcType srcType, Class<?> c) {
            return null;
        }

        public Annotation[] getActualAnnoContainer(SrcType srcType, Class<?> c) {
            return null;
        }

        public String[] getExpectedAnnoBase(SrcType srcType, Class<?> c) {
            return null;
        }

        public String[] getExpectedAnnoContainer(SrcType srcType, Class<?> c) {
            return null;
        }
    }

    /*
     * For a given srcType and class object, compare expectedBase and actualBase
     * annotations as well as expectedContainer and actualContainer annotations.
     *
     * Return true if both comparisons are true else return false.
     *
     */
    protected static void checkAnnoValues(SrcType srcType, Class<?> c) {

        // Load @ExpectedBase and @ExpectedContainer
        ExpectedBase eb = srcType.getExpectedBase(c);
        ExpectedContainer ec = srcType.getExpectedContainer(c);
        if (eb == null) {
            error("Did not find ExpectedBase Annotation, Test will exit");
            throw new RuntimeException();
        }
        if (ec == null) {
            error("Did not find ExpectedContainer Annotation, Test will exit");
            throw new RuntimeException();
        }

        for (TestMethod testMethod : TestMethod.values()) {
            debugPrint("---------------------------------------------");
            debugPrint("Test method = " + testMethod);
            boolean isBasePass = true;
            boolean isConPass = true;
            // ExpectedBase = Annotation, no annotation is defined, skip comparison
            if (!eb.value().getSimpleName().equalsIgnoreCase("Annotation")) {
                isBasePass = compareAnnotations(
                        testMethod.getActualAnnoBase(srcType, c),
                        testMethod.getExpectedAnnoBase(srcType, c));
            }

            // ExpectedContainer = Annotation, no annotation is defined, skip comparison
            if (!ec.value().getSimpleName().equalsIgnoreCase("Annotation")) {
                isConPass = compareAnnotations(
                        testMethod.getActualAnnoContainer(srcType, c),
                        testMethod.getExpectedAnnoContainer(srcType, c));
            }
            if (isBasePass && isConPass) {
                debugPrint("Testcase passed for " + testMethod +
                        " for className = " + c.getName());
            } else {
                debugPrint("Testcase failed for " + testMethod +
                        " for className = " + c.getName());
            }
        }
    }

    // Annotation comparison: Length should be same and all expected values
    // should be present in actualAnno[].
    private static boolean compareAnnotations(Annotation[] actualAnnos,
            String[] expectedAnnos) {
        boolean compOrder = false;

        // Length is different
        if (actualAnnos.length != expectedAnnos.length) {
            error("Length not same, Actual length = " + actualAnnos.length
                    + " Expected length = " + expectedAnnos.length);
            printArrContents(actualAnnos);
            printArrContents(expectedAnnos);
            return false;
        } else {
            int i = 0;
            // Length is same and 0
            if (actualAnnos.length == 0) {
                // Expected/actual lengths already checked for
                // equality; no more checks do to
                return true;
            }
            // Expected annotation should be NULL
            if (actualAnnos[0] == null) {
                if (expectedAnnos[0].equals("NULL")) {
                    debugPrint("Arr values are NULL as expected");
                    return true;
                } else {
                    error("Array values are not NULL");
                    printArrContents(actualAnnos);
                    printArrContents(expectedAnnos);
                    return false;
                }
            }
            // Lengths are same, compare array contents
            String[] actualArr = new String[actualAnnos.length];
            for (Annotation a : actualAnnos) {
                if (a.annotationType().getSimpleName().contains("Expected"))
                actualArr[i++] = a.annotationType().getSimpleName();
                 else if (a.annotationType().getName().contains(TESTPKG)) {
                    String replaced = a.toString().replaceAll(Pattern.quote("testpkg."),"");
                    actualArr[i++] = replaced;
                } else
                    actualArr[i++] = a.toString();
            }
            List<String> actualList = new ArrayList<String>(Arrays.asList(actualArr));
            List<String> expectedList = new ArrayList<String>(Arrays.asList(expectedAnnos));
            if (!actualList.containsAll(expectedList)) {
                error("Array values are not same");
                printArrContents(actualAnnos);
                printArrContents(expectedAnnos);
                return false;
            } else {
                debugPrint("Arr values are same as expected");
                if (CHECKORDERING) {
                    debugPrint("Checking if annotation ordering is as expected..");
                    compOrder = compareOrdering(actualList, expectedList);
                    if (compOrder)
                        debugPrint("Arr values ordering is as expected");
                    else
                        error("Arr values ordering is not as expected! actual values: "
                            + actualList + " expected values: " + expectedList);
                } else
                    compOrder = true;
            }
        }
        return compOrder;
    }

    // Annotation ordering comparison
    private static boolean compareOrdering(List<String> actualList, List<String> expectedList) {
        boolean order = true;
        // Discarding Expected* annotations before comparison of ordering
        actualList = iterateList(actualList);
        expectedList = iterateList(expectedList);
        // Length is different
        if (actualList.size() != expectedList.size()) {
            error("Length not same, Actual list length = " + actualList.size()
                    + " Expected list length = " + expectedList.size());
            return false;
        } else {
            if (actualList.isEmpty() && expectedList.isEmpty()) {
        return true;
    }
            boolean tmp = true;
            for (int i = 0; i < actualList.size(); i++) {
                // Checking ordering
                if (order) {
                    if (!actualList.get(i).equals(expectedList.get(i))) {
                        tmp = false;
                        debugPrint("Odering is false");
                        debugPrint("actualList values: " + actualList
                                + " expectedList values: " + expectedList);
                    }
                }
            }
            order = tmp;
        }
        return order;
    }

    private static List<String> iterateList(List<String> list) {
        Iterator<String> iter = list.iterator();
        while (iter.hasNext()) {
            String anno = iter.next();
            if (anno.contains("Expected")) {
                iter.remove();
            }
        }
        return list;
    }

    private static void printArrContents(Annotation[] actualAnnos) {
        System.out.print("Actual Arr Values: ");
        for (Annotation a : actualAnnos) {
            if (a != null && a.annotationType() != null) {
                System.out.print("[" + a.toString() + "]");
            } else {
                System.out.println("[null]");
            }
        }
        System.out.println();
    }

    private static void printArrContents(String[] expectedAnnos) {
        System.out.print("Expected Arr Values: ");
        for (String s : expectedAnnos) {
            System.out.print("[" + s + "]");
        }
        System.out.println();
    }

    private ClassLoader getLoader() {
        return getClass().getClassLoader();
    }

    private static Class<?> loadClass(String className, ClassLoader parentClassLoader, File... destDirs) {
        try {
            List<URL> list = new ArrayList<>();
            for (File f : destDirs) {
                list.add(new URL("file:" + f.toString().replace("\\", "/") + "/"));
            }
            return Class.forName(className, true, new URLClassLoader(
                    list.toArray(new URL[list.size()]), parentClassLoader));
        } catch (ClassNotFoundException | MalformedURLException e) {
            throw new RuntimeException("Error loading class " + className, e);
        }
    }

    private static void printTestSrc(Iterable<? extends JavaFileObject> files) {
        for (JavaFileObject f : files) {
            System.out.println("Test file " + f.getName() + ":");
            try {
                System.out.println("" + f.getCharContent(true));
            } catch (IOException e) {
                throw new RuntimeException(
                        "Exception when printing test src contents for class " +
                                f.getName(), e);
            }
        }

    }

    public static StringBuilder getCommonStmts(boolean isRepeatable) {
        StringBuilder sb = new StringBuilder();

        sb.append(Helper.ContentVars.IMPORTEXPECTED.getVal())
          .append(Helper.ContentVars.IMPORTSTMTS.getVal())
          .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
          .append(Helper.ContentVars.INHERITED.getVal());
        if(isRepeatable) {
            sb.append(Helper.ContentVars.REPEATABLE.getVal());
        }
        sb.append(Helper.ContentVars.BASE.getVal())
          .append(Helper.ContentVars.RETENTIONRUNTIME.getVal())
          .append(Helper.ContentVars.INHERITED.getVal())
          .append(Helper.ContentVars.CONTAINER.getVal());
        return sb;
    }

    private static int getNumErrors() {
        return errors;
    }

    private static void error(String msg) {
        System.out.println("error: " + msg);
        errors++;
    }

    private static void debugPrint(String string) {
        if(DEBUG)
            System.out.println(string);
    }
}
