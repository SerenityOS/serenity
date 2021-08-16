/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005042
 * @summary Check behavior of Method.isDefault
 * @author Joseph D. Darcy
 */

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

public class IsDefaultTest {
    public static void main(String... argv) throws Exception {
        int failures = 0;
        int visitationCount = 0;

        List<Class<?>> classList = new ArrayList<>();
        classList.add(TestType1.class);
        classList.add(TestType2.class);
        classList.add(TestType3.class);
        classList.add(TestType4.class);
        classList.add(TestType2.nestedTestType2.class);
        classList.add(TestType5.class);
        classList.add(TestType5.nestedTestType5.class);
        classList.add(TestType6.class);
        classList.add(TestType6.nestedTestType6.class);
        classList.add(TestType7.class);

        for(Class<?> clazz: classList) {
            for(Method method: clazz.getDeclaredMethods()) {
                ExpectedIsDefault expectedIsDefault = method.getAnnotation(ExpectedIsDefault.class);
                if (expectedIsDefault != null) {
                    visitationCount++;
                    boolean expected = expectedIsDefault.value();
                    boolean actual   = method.isDefault();

                    if (actual != expected) {
                        failures++;
                        System.err.printf("ERROR: On %s expected isDefault of ''%s''; got ''%s''.\n",
                                          method.toString(), expected, actual);
                    }
                }
            }
        }

        if (visitationCount == 0) {
            System.err.println("Test failed because no methods checked.");
            throw new RuntimeException();
        }

        if (failures > 0) {
            System.err.println("Test failed.");
            throw new RuntimeException();
        }
    }
}

interface TestType1 {
    @ExpectedIsDefault(false)
    void foo();

    @ExpectedIsDefault(true)
    default void bar() {}; // Default method

    @ExpectedIsDefault(true)
    default void bar(int i) {}; // Default method

    @ExpectedIsDefault(true)
    default void bar(String i) {}; // Default method
}

class TestType2 {
    @ExpectedIsDefault(false)
    void bar() {};

    interface nestedTestType2 {
        @ExpectedIsDefault(true)
        default void nestedBar() {};
   }
}

class TestType3 implements TestType1 {
    @ExpectedIsDefault(false)
    public void foo(){}

    @ExpectedIsDefault(false)
    @Override
    public void bar() {};

    @ExpectedIsDefault(false)
    @Override
    public void bar(int i) {};
}

@interface TestType4 {
    @ExpectedIsDefault(false)
    String value();

    @ExpectedIsDefault(false)
    String anotherValue() default "";
}

interface TestType5 {
    @ExpectedIsDefault(false)
    abstract void aFoo();

    @ExpectedIsDefault(false)
    static void sFoo() {};

    @ExpectedIsDefault(true)
    public default void pBar() {};

    @ExpectedIsDefault(true)
    public default String sBar() {return "";};

    interface nestedTestType5{
        @ExpectedIsDefault(false)
        void nestedFoo();

        @ExpectedIsDefault(true)
        default void nestedBar() {};
    }
}

class TestType6{
    interface nestedTestType6 {
        @ExpectedIsDefault(true)
        default void nestedBar() {};

        @ExpectedIsDefault(false)
        void nestedFoo();
   }

    @ExpectedIsDefault(false)
    void foo(nestedTestType6 n) {}
}

class TestType7 implements TestType6.nestedTestType6 {

    @ExpectedIsDefault(false)
    public void nestedFoo() {}

    @ExpectedIsDefault(false)
    @Override
    public void nestedBar() {};
}

@Retention(RetentionPolicy.RUNTIME)
@interface ExpectedIsDefault {
    boolean value();
}
