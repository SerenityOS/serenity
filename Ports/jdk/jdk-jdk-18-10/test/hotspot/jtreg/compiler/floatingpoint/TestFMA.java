/*
 * Copyright (c) 2016, Red Hat, Inc. All rights reserved.
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
 * @bug 8162338
 * @summary intrinsify fused mac operations
 * @run main/othervm -XX:-BackgroundCompilation -XX:-BackgroundCompilation -XX:-UseOnStackReplacement TestFMA
 *
 */

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// Test all fused mac instructions that can be generated
public class TestFMA {

    @Test(args = {5.0F, 10.0F, 7.0F}, res = 57.0F)
    static float test1(float a, float b, float c) {
        return Math.fma(a, b, c);
    }

    @Test(args = {5.0D, 10.0D, 7.0D}, res = 57.0D)
    static double test2(double a, double b, double c) {
        return Math.fma(a, b, c);
    }

    @Test(args = {5.0F, 10.0F, 7.0F}, res = -43.0F)
    static float test3(float a, float b, float c) {
        return Math.fma(-a, b, c);
    }

    @Test(args = {5.0D, 10.0D, 7.0D}, res = -43.0D)
    static double test4(double a, double b, double c) {
        return Math.fma(-a, b, c);
    }

    @Test(args = {5.0F, 10.0F, 7.0F}, res = -43.0F)
    static float test5(float a, float b, float c) {
        return Math.fma(a, -b, c);
    }

    @Test(args = {5.0D, 10.0D, 7.0D}, res = -43.0D)
    static double test6(double a, double b, double c) {
        return Math.fma(a, -b, c);
    }

    @Test(args = {5.0F, 10.0F, 7.0F}, res = -57.0F)
    static float test7(float a, float b, float c) {
        return Math.fma(-a, b, -c);
    }

    @Test(args = {5.0D, 10.0D, 7.0D}, res = -57.0D)
    static double test8(double a, double b, double c) {
        return Math.fma(-a, b, -c);
    }

    @Test(args = {5.0F, 10.0F, 7.0F}, res = -57.0F)
    static float test9(float a, float b, float c) {
        return Math.fma(a, -b, -c);
    }

    @Test(args = {5.0D, 10.0D, 7.0D}, res = -57.0D)
    static double test10(double a, double b, double c) {
        return Math.fma(a, -b, -c);
    }

    @Test(args = {5.0F, 10.0F, 7.0F}, res = 43.0F)
    static float test11(float a, float b, float c) {
        return Math.fma(a, b, -c);
    }

    @Test(args = {5.0D, 10.0D, 7.0D}, res = 43.0D)
    static double test12(double a, double b, double c) {
        return Math.fma(a, b, -c);
    }

    static public void main(String[] args) throws Exception {
        TestFMA t = new TestFMA();
        for (Method m : t.getClass().getDeclaredMethods()) {
            if (m.getName().matches("test[0-9]+?")) {
                t.doTest(m);
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Test {
        double[] args();
        double res();
    }

    void doTest(Method m) throws Exception {
        String name = m.getName();
        System.out.println("Testing " + name);
        Class retType = m.getReturnType();
        Test test = m.getAnnotation(Test.class);
        double[] args = test.args();
        double expected = test.res();

        for (int i = 0; i < 20000; i++) {
            if (retType == double.class) {
                Object res = m.invoke(null, (double)args[0], (double)args[1], (double)args[2]);
                if ((double)res != expected) {
                    throw new RuntimeException(name + " failed : " + (double)res + " != " + expected);
                }
            } else {
                Object res = m.invoke(null, (float)args[0], (float)args[1], (float)args[2]);
                if ((float)res != (float)expected) {
                    throw new RuntimeException(name + " failed : " + (float)res + " != " + (float)expected);
                }
            }
        }
    }
}
