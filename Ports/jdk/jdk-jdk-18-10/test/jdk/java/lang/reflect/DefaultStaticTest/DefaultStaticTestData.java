/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * Test Data used for testing default/static method
 *
 * @author Yong Lu
 */

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.collections.Lists;

import static helper.Mod.*;
import static helper.Declared.*;
import helper.Mod;
import helper.Declared;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@MethodDesc(name = "defaultMethod", retval = "TestIF1.defaultMethod", mod = DEFAULT, declared = YES)
interface TestIF1 {

    default String defaultMethod() {
        return "TestIF1.defaultMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF1.defaultMethod", mod = DEFAULT, declared = NO)
class TestClass1 implements TestIF1 {
}

@MethodDesc(name = "staticMethod", retval = "TestIF2.staticMethod", mod = STATIC, declared = YES)
interface TestIF2 {

    static String staticMethod() {
        return "TestIF2.staticMethod";
    }
}

@MethodDesc(name = "method", retval = "TestIF2.staticMethod", mod = REGULAR, declared = YES)
class TestClass2 implements TestIF2 {

    public String method() {
        return TestIF2.staticMethod();
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF3.defaultMethod", mod = DEFAULT, declared = YES)
@MethodDesc(name = "method", retval = "", mod = ABSTRACT, declared = YES)
interface TestIF3 {

    String method();

    default String defaultMethod() {
        return "TestIF3.defaultMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF3.defaultMethod", mod = DEFAULT, declared = NO)
@MethodDesc(name = "method", retval = "TestClass3.method", mod = REGULAR, declared = YES)
class TestClass3 implements TestIF3 {

    public String method() {
        return "TestClass3.method";
    }
}

@MethodDesc(name = "staticMethod", retval = "TestIF4.staticMethod", mod = STATIC, declared = YES)
@MethodDesc(name = "method", retval = "", mod = ABSTRACT, declared = YES)
interface TestIF4 {

    String method();

    static String staticMethod() {
        return "TestIF4.staticMethod";
    }
}

@MethodDesc(name = "method", retval = "TestClass4.method", mod = REGULAR, declared = YES)
class TestClass4 implements TestIF4 {

    public String method() {
        return "TestClass4.method";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF5.defaultMethod", mod = DEFAULT, declared = YES)
@MethodDesc(name = "staticMethod", retval = "TestIF5.staticMethod", mod = STATIC, declared = YES)
interface TestIF5 {

    default String defaultMethod() {
        return "TestIF5.defaultMethod";
    }

    static String staticMethod() {
        return "TestIF5.staticMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF5.defaultMethod", mod = DEFAULT, declared = NO)
class TestClass5 implements TestIF5 {
}

@MethodDesc(name = "defaultMethod", retval = "TestIF6.defaultMethod", mod = DEFAULT, declared = YES)
@MethodDesc(name = "staticMethod", retval = "TestIF6.staticMethod", mod = STATIC, declared = YES)
@MethodDesc(name = "method", retval = "", mod = ABSTRACT, declared = YES)
interface TestIF6 {

    String method();

    default String defaultMethod() {
        return "TestIF6.defaultMethod";
    }

    static String staticMethod() {
        return "TestIF6.staticMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF6.defaultMethod", mod = DEFAULT, declared = NO)
@MethodDesc(name = "method", retval = "TestClass6.method", mod = REGULAR, declared = YES)
class TestClass6 implements TestIF6 {

    public String method() {
        return "TestClass6.method";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF7.TestClass7", mod = DEFAULT, declared = YES)
interface TestIF7<T> {

    default T defaultMethod(T t) {
        return t;
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF7.TestClass7", mod = DEFAULT, declared = NO)
class TestClass7<T> implements TestIF7<T> {
}

@MethodDesc(name = "defaultMethod", retval = "TestIF8.TestClass8", mod = DEFAULT, declared = YES)
interface TestIF8<E> {

    default <E> E defaultMethod(E e) {
        return e;
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF8.TestClass8", mod = DEFAULT, declared = NO)
class TestClass8<T> implements TestIF8<T> {
}

@MethodDesc(name = "defaultMethod", retval = "TestIF9.defaultMethod", mod = DEFAULT, declared = YES)
interface TestIF9 extends TestIF1 {

    default String defaultMethod() {
        return "TestIF9.defaultMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF9.defaultMethod", mod = DEFAULT, declared = NO)
class TestClass9 implements TestIF9 {
}

@MethodDesc(name = "defaultMethod", retval = "TestIF9.defaultMethod", mod = DEFAULT, declared = NO)
@MethodDesc(name = "method", retval = "TestIF9.defaultMethod", mod = REGULAR, declared = YES)
class TestClass91 implements TestIF9, TestIF1 {

    public String method() {
        return defaultMethod();
    }
}

@MethodDesc(name = "staticMethod", retval = "TestIF10.staticMethod", mod = STATIC, declared = YES)
interface TestIF10 extends TestIF2 {

    static String staticMethod() {

        return "TestIF10.staticMethod";
    }
}

@MethodDesc(name = "staticMethod", retval = "TestIF11.staticMethod", mod = STATIC, declared = YES)
@MethodDesc(name = "defaultMethod", retval = "TestIF1.defaultMethod", mod = DEFAULT, declared = NO)
interface TestIF11 extends TestIF1 {

    static String staticMethod() {
        return "TestIF11.staticMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF1.defaultMethod", mod = DEFAULT, declared = NO)
class TestClass11 implements TestIF11 {
}

@MethodDesc(name = "defaultMethod", retval = "TestIF12.defaultMethod", mod = DEFAULT, declared = YES)
interface TestIF12 extends TestIF2 {

    default String defaultMethod() {
        return "TestIF12.defaultMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF12.defaultMethod", mod = DEFAULT, declared = NO)
class TestClass12 implements TestIF12 {
}

//Diamond Case
@MethodDesc(name = "defaultMethod", retval = "TestIF1.defaultMethod", mod = DEFAULT, declared = NO)
interface TestIF1A extends TestIF1 {
}

@MethodDesc(name = "defaultMethod", retval = "TestIF1.defaultMethod", mod = DEFAULT, declared = NO)
interface TestIF1B extends TestIF1 {
}

@MethodDesc(name = "defaultMethod", retval = "TestIF1.defaultMethod", mod = DEFAULT, declared = NO)
class TestClass13 implements TestIF1A, TestIF1B {
}

//Diamond Override Case
@MethodDesc(name = "defaultMethod", retval = "TestIF1C.defaultMethod", mod = DEFAULT, declared = YES)
interface TestIF1C extends TestIF1 {

    default String defaultMethod() {
        return "TestIF1C.defaultMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF1D.defaultMethod", mod = DEFAULT, declared = YES)
interface TestIF1D extends TestIF1 {

    default String defaultMethod() {
        return "TestIF1D.defaultMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestClass14.defaultMethod", mod = REGULAR, declared = YES)
class TestClass14 implements TestIF1C, TestIF1D {

    public String defaultMethod() {
        return "TestClass14.defaultMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "", mod = ABSTRACT, declared = YES)
interface TestIF15 extends TestIF1 {

    String defaultMethod();
}

@MethodDesc(name = "defaultMethod", retval = "TestClass15.defaultMethod", mod = REGULAR, declared = YES)
class TestClass15 implements TestIF15 {

    public String defaultMethod() {
        return "TestClass15.defaultMethod";
    }
}

interface FuncInterface<T> {

    String test(T t);
}

@MethodDesc(name = "defaultMethod", retval = "TestIF16.defaultMethod", mod = DEFAULT, declared = YES)
interface TestIF16 {

    default String defaultMethod() {
        FuncInterface<Object> fi = o -> o.toString();
        Object o = "TestIF16.defaultMethod";
        return fi.test(o);
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF16.defaultMethod", mod = DEFAULT, declared = NO)
class TestClass16 implements TestIF16 {
}

@MethodDesc(name = "defaultMethod", retval = "TestIF17.defaultMethod", mod = DEFAULT, declared = YES)
@MethodDesc(name = "staticMethod", retval = "TestIF17.staticMethod", mod = STATIC, declared = YES)
interface TestIF17 {

    default String defaultMethod() {
        return staticMethod().replace("staticMethod", "defaultMethod");
    }

    public static String staticMethod() {
        return "TestIF17.staticMethod";
    }
}

@MethodDesc(name = "defaultMethod", retval = "TestIF17.defaultMethod", mod = DEFAULT, declared = NO)
class TestClass17 implements TestIF17 {
}


@MethodDesc(name = "defaultMethod", retval = "TestIF17.defaultMethod", mod = DEFAULT, declared = NO)
class TestClass18 extends TestClass17 {
}


@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MethodDescs.class)
@interface MethodDesc {
    String name();
    String retval();
    Mod mod();
    Declared declared();
}

@Retention(RetentionPolicy.RUNTIME)
@interface MethodDescs {
    MethodDesc[] value();
}

//Diamond Case for static method
@MethodDesc(name = "staticMethod", retval = "TestIF2A.staticMethod", mod = STATIC, declared = YES)
interface TestIF2A extends TestIF2 {
    static String staticMethod() {
        return "TestIF2A.staticMethod";
    }
}

@MethodDesc(name = "method", retval = "", mod = ABSTRACT, declared = YES)
interface TestIF2B extends TestIF2 {
    String method();
}

@MethodDesc(name = "method", retval = "", mod = ABSTRACT, declared = YES)
interface TestIF18 extends TestIF10, TestIF2A {
    String method();
}

@MethodDesc(name = "method", retval = "", mod = ABSTRACT, declared = NO)
@MethodDesc(name = "defaultMethod", retval = "TestIF12.defaultMethod", mod = DEFAULT, declared = NO)
interface TestIF19 extends TestIF12, TestIF2B {
}

@MethodDesc(name = "staticMethod", retval = "TestIF20.staticMethod", mod = STATIC, declared = YES)
@MethodDesc(name = "defaultMethod", retval = "TestIF12.defaultMethod", mod = DEFAULT, declared = NO)
interface TestIF20 extends TestIF12, TestIF2A {
    static String staticMethod() {
        return "TestIF20.staticMethod";
    }
}

@MethodDesc(name = "method", retval = "", mod = ABSTRACT, declared = NO)
interface TestIF21 extends TestIF2A, TestIF2B {
}

public class DefaultStaticTestData {

    /**
     * Test data for DefaultStaticInvokeTest The format of inner array is: First
     * data is the name of the class under test Second data used in test as the
     * arguments used for the method call.
     */
    @DataProvider
    static Object[][] testClasses() {
        return new Object[][]{
            {"TestClass1", null},
            {"TestClass2", null},
            {"TestClass3", null},
            {"TestClass4", null},
            {"TestClass5", null},
            {"TestClass6", null},
            {"TestClass7", "TestIF7.TestClass7"},
            {"TestClass8", "TestIF8.TestClass8"},
            {"TestClass9", null},
            {"TestClass91", null},
            {"TestClass11", null},
            {"TestClass12", null},
            {"TestClass13", null},
            {"TestClass14", null},
            {"TestClass15", null},
            {"TestClass16", null},
            {"TestClass17", null},
            {"TestClass18", null},
        };
    }

    /**
     * Test data for DefaultStaticInvokeTest The format of inner array is: First
     * data is the name of the interface under test Second data used in test as
     * the arguments used for the method call.
     */
    @DataProvider
    static Object[][] testInterfaces() {
        return new Object[][]{
            {"TestIF1", null},
            {"TestIF2", null},
            {"TestIF2A", null},
            {"TestIF2B", null},
            {"TestIF3", null},
            {"TestIF4", null},
            {"TestIF5", null},
            {"TestIF6", null},
            {"TestIF7", "TestIF7.TestClass7"},
            {"TestIF8", "TestIF8.TestClass8"},
            {"TestIF9", null},
            {"TestIF10", null},
            {"TestIF11", null},
            {"TestIF12", null},
            {"TestIF1A", null},
            {"TestIF1B", null},
            {"TestIF1C", null},
            {"TestIF1D", null},
            {"TestIF15", null},
            {"TestIF16", null},
            {"TestIF17", null},
            {"TestIF18", null},
            {"TestIF19", null},
            {"TestIF20", null},
            {"TestIF21", null},
        };
    }

    @DataProvider
    static Object[][] testCasesAll() {
        List<Object[]> result = Lists.newArrayList();
        result.addAll(Arrays.asList(testClasses()));
        result.addAll(Arrays.asList(testInterfaces()));
        return result.toArray(new Object[result.size()][]);
    }
}
