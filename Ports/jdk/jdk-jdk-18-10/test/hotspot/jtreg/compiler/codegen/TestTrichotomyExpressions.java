/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 8210215
 * @summary Test that C2 correctly optimizes trichotomy expressions.
 * @library /test/lib
 * @run main/othervm/timeout=240 -XX:-TieredCompilation -Xbatch
 *                   -XX:CompileCommand=dontinline,compiler.codegen.TestTrichotomyExpressions::test*
 *                   compiler.codegen.TestTrichotomyExpressions
 */

package compiler.codegen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Random;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

enum Operation { SMALLER, SMALLER_EQUAL, EQUAL, GREATER_EQUAL, GREATER, ALWAYS_FALSE }

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Test {
    Operation op();
}

public class TestTrichotomyExpressions {

    public static int compare1(int a, int b) {
        return (a < b) ? -1 : (a == b) ? 0 : 1;
    }

    public static int compare2(int a, int b) {
        return (a < b) ? -1 : (a <= b) ? 0 : 1;
    }

    public static int compare3(int a, int b) {
        return (a < b) ? -1 : (a > b) ? 1 : 0;
    }

    public static int compare4(int a, int b) {
        return (a < b) ? -1 : (a != b) ? 1 : 0;
    }

    public static int compare5(int a, int b) {
        return (a > b) ? 1 : (a < b) ? -1 : 0;
    }

    public static int compare6(int a, int b) {
        return (a > b) ? 1 : (a == b) ? 0 : -1;
    }

    public static int compare7(int a, int b) {
        return (a > b) ? 1 : (a >= b) ? 0 : -1;
    }

    public static int compare8(int a, int b) {
        return (a > b) ? 1 : (a != b) ? -1 : 0;
    }

    public static int compare9(int a, int b) {
        return (a == b) ? 0 : (a < b) ? -1 : 1;
    }

    public static int compare10(int a, int b) {
        return (a == b) ? 0 : (a <= b) ? -1 : 1;
    }

    public static int compare11(int a, int b) {
        return (a == b) ? 0 : (a > b) ? 1 : -1;
    }

    public static int compare12(int a, int b) {
        return (a == b) ? 0 : (a >= b) ? 1 : -1;
    }

    public static int compare13(int a, int b) {
        return (a <= b) ? ((a == b) ? 0 : -1) : 1;
    }

    public static int compare14(int a, int b) {
        return (a <= b) ? ((a < b) ? -1 : 0) : 1;
    }

    public static int compare15(int a, int b) {
        return (a <= b) ? ((a >= b) ? 0 : -1) : 1;
    }

    public static int compare16(int a, int b) {
        return (a <= b) ? ((a != b) ? -1 : 0) : 1;
    }

    public static int compare17(int a, int b) {
        return (a >= b) ? ((a <= b) ? 0 : 1) : -1;
    }

    public static int compare18(int a, int b) {
        return (a >= b) ? ((a == b) ? 0 : 1) : -1;
    }

    public static int compare19(int a, int b) {
        return (a >= b) ? ((a > b) ? 1 : 0) : -1;
    }

    public static int compare20(int a, int b) {
        return (a >= b) ? ((a != b) ? 1 : 0) : -1;
    }

    public static int compare21(int a, int b) {
        return (a != b) ? ((a < b) ? -1 : 1) : 0;
    }

    public static int compare22(int a, int b) {
        return (a != b) ? ((a <= b) ? -1 : 1) : 0;
    }

    public static int compare23(int a, int b) {
        return (a != b) ? ((a > b) ? 1 : -1) : 0;
    }

    public static int compare24(int a, int b) {
        return (a != b) ? ((a >= b) ? 1 : -1) : 0;
    }

    public static int compare25(int a, int b) {
        return (a < b) ? -1 : (b == a) ? 0 : 1;
    }

    public static int compare26(int a, int b) {
        return (a < b) ? -1 : (b >= a) ? 0 : 1;
    }

    public static int compare27(int a, int b) {
        return (a < b) ? -1 : (b < a) ? 1 : 0;
    }

    public static int compare28(int a, int b) {
        return (a < b) ? -1 : (b != a) ? 1 : 0;
    }

    public static int compare29(int a, int b) {
        return (a > b) ? 1 : (b > a) ? -1 : 0;
    }

    public static int compare30(int a, int b) {
        return (a > b) ? 1 : (b == a) ? 0 : -1;
    }

    public static int compare31(int a, int b) {
        return (a > b) ? 1 : (b <= a) ? 0 : -1;
    }

    public static int compare32(int a, int b) {
        return (a > b) ? 1 : (b != a) ? -1 : 0;
    }

    public static int compare33(int a, int b) {
        return (a == b) ? 0 : (b > a) ? -1 : 1;
    }

    public static int compare34(int a, int b) {
        return (a == b) ? 0 : (b >= a) ? -1 : 1;
    }

    public static int compare35(int a, int b) {
        return (a == b) ? 0 : (b < a) ? 1 : -1;
    }

    public static int compare36(int a, int b) {
        return (a == b) ? 0 : (b <= a) ? 1 : -1;
    }

    public static int compare37(int a, int b) {
        return (a <= b) ? ((b == a) ? 0 : -1) : 1;
    }

    public static int compare38(int a, int b) {
        return (a <= b) ? ((b > a) ? -1 : 0) : 1;
    }

    public static int compare39(int a, int b) {
        return (a <= b) ? ((b <= a) ? 0 : -1) : 1;
    }

    public static int compare40(int a, int b) {
        return (a <= b) ? ((b != a) ? -1 : 0) : 1;
    }

    public static int compare41(int a, int b) {
        return (a >= b) ? ((b >= a) ? 0 : 1) : -1;
    }

    public static int compare42(int a, int b) {
        return (a >= b) ? ((b == a) ? 0 : 1) : -1;
    }

    public static int compare43(int a, int b) {
        return (a >= b) ? ((b < a) ? 1 : 0) : -1;
    }

    public static int compare44(int a, int b) {
        return (a >= b) ? ((b != a) ? 1 : 0) : -1;
    }

    public static int compare45(int a, int b) {
        return (a != b) ? ((b > a) ? -1 : 1) : 0;
    }

    public static int compare46(int a, int b) {
        return (a != b) ? ((b >= a) ? -1 : 1) : 0;
    }

    public static int compare47(int a, int b) {
        return (a != b) ? ((b < a) ? 1 : -1) : 0;
    }

    public static int compare48(int a, int b) {
        return (a != b) ? ((b <= a) ? 1 : -1) : 0;
    }


    public static int compareAlwaysFalse1(int a, int b) {
        return (a >= b) ? 1 : (a > b) ? 2 : -1;
    }

    public static int compareAlwaysFalse2(int a, int b) {
        return (a <= b) ? 1 : (a < b) ? 2 : -1;
    }

    public static int compareAlwaysFalse3(int a, int b) {
        return (a == b) ? 1 : (a == b) ? 2 : -1;
    }

    public static int compareAlwaysFalse4(int a, int b) {
        return (a != b) ? 1 : (a < b) ? 2 : -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller1(int a, int b) {
        return compare1(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller2(int a, int b) {
        return compare1(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller3(int a, int b) {
        return compare1(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller4(int a, int b) {
        return compare2(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller5(int a, int b) {
        return compare2(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller6(int a, int b) {
        return compare2(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller7(int a, int b) {
        return compare3(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller8(int a, int b) {
        return compare3(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller9(int a, int b) {
        return compare3(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller10(int a, int b) {
        return compare4(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller11(int a, int b) {
        return compare4(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller12(int a, int b) {
        return compare4(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller13(int a, int b) {
        return compare5(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller14(int a, int b) {
        return compare5(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller15(int a, int b) {
        return compare5(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller16(int a, int b) {
        return compare6(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller17(int a, int b) {
        return compare6(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller18(int a, int b) {
        return compare6(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller19(int a, int b) {
        return compare7(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller20(int a, int b) {
        return compare7(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller21(int a, int b) {
        return compare7(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller22(int a, int b) {
        return compare8(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller23(int a, int b) {
        return compare8(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller24(int a, int b) {
        return compare8(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller25(int a, int b) {
        return compare9(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller26(int a, int b) {
        return compare9(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller27(int a, int b) {
        return compare9(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller28(int a, int b) {
        return compare10(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller29(int a, int b) {
        return compare10(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller30(int a, int b) {
        return compare10(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller31(int a, int b) {
        return compare11(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller32(int a, int b) {
        return compare11(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller33(int a, int b) {
        return compare11(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller34(int a, int b) {
        return compare12(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller35(int a, int b) {
        return compare12(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller36(int a, int b) {
        return compare12(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller37(int a, int b) {
        return compare13(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller38(int a, int b) {
        return compare13(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller39(int a, int b) {
        return compare13(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller40(int a, int b) {
        return compare14(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller41(int a, int b) {
        return compare14(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller42(int a, int b) {
        return compare14(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller43(int a, int b) {
        return compare15(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller44(int a, int b) {
        return compare15(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller45(int a, int b) {
        return compare15(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller46(int a, int b) {
        return compare16(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller47(int a, int b) {
        return compare16(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller48(int a, int b) {
        return compare16(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller49(int a, int b) {
        return compare17(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller50(int a, int b) {
        return compare17(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller51(int a, int b) {
        return compare17(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller52(int a, int b) {
        return compare18(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller53(int a, int b) {
        return compare18(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller54(int a, int b) {
        return compare18(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller55(int a, int b) {
        return compare19(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller56(int a, int b) {
        return compare19(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller57(int a, int b) {
        return compare19(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller58(int a, int b) {
        return compare20(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller59(int a, int b) {
        return compare20(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller60(int a, int b) {
        return compare20(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller61(int a, int b) {
        return compare21(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller62(int a, int b) {
        return compare21(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller63(int a, int b) {
        return compare21(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller64(int a, int b) {
        return compare22(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller65(int a, int b) {
        return compare22(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller66(int a, int b) {
        return compare22(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller67(int a, int b) {
        return compare23(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller68(int a, int b) {
        return compare23(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller69(int a, int b) {
        return compare23(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller70(int a, int b) {
        return compare24(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller71(int a, int b) {
        return compare24(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller72(int a, int b) {
        return compare24(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller73(int a, int b) {
        return compare25(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller74(int a, int b) {
        return compare25(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller75(int a, int b) {
        return compare25(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller76(int a, int b) {
        return compare26(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller77(int a, int b) {
        return compare26(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller78(int a, int b) {
        return compare26(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller79(int a, int b) {
        return compare27(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller80(int a, int b) {
        return compare27(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller81(int a, int b) {
        return compare27(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller82(int a, int b) {
        return compare28(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller83(int a, int b) {
        return compare28(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller84(int a, int b) {
        return compare28(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller85(int a, int b) {
        return compare29(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller86(int a, int b) {
        return compare29(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller87(int a, int b) {
        return compare29(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller88(int a, int b) {
        return compare30(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller89(int a, int b) {
        return compare30(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller90(int a, int b) {
        return compare30(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller91(int a, int b) {
        return compare31(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller92(int a, int b) {
        return compare31(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller93(int a, int b) {
        return compare31(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller94(int a, int b) {
        return compare32(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller95(int a, int b) {
        return compare32(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller96(int a, int b) {
        return compare32(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller97(int a, int b) {
        return compare33(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller98(int a, int b) {
        return compare33(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller99(int a, int b) {
        return compare33(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller100(int a, int b) {
        return compare34(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller101(int a, int b) {
        return compare34(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller102(int a, int b) {
        return compare34(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller103(int a, int b) {
        return compare35(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller104(int a, int b) {
        return compare35(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller105(int a, int b) {
        return compare35(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller106(int a, int b) {
        return compare36(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller107(int a, int b) {
        return compare36(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller108(int a, int b) {
        return compare36(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller109(int a, int b) {
        return compare37(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller110(int a, int b) {
        return compare37(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller111(int a, int b) {
        return compare37(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller112(int a, int b) {
        return compare38(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller113(int a, int b) {
        return compare38(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller114(int a, int b) {
        return compare38(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller115(int a, int b) {
        return compare39(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller116(int a, int b) {
        return compare39(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller117(int a, int b) {
        return compare39(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller118(int a, int b) {
        return compare40(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller119(int a, int b) {
        return compare40(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller120(int a, int b) {
        return compare40(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller121(int a, int b) {
        return compare41(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller122(int a, int b) {
        return compare41(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller123(int a, int b) {
        return compare41(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller124(int a, int b) {
        return compare42(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller125(int a, int b) {
        return compare42(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller126(int a, int b) {
        return compare42(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller127(int a, int b) {
        return compare43(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller128(int a, int b) {
        return compare43(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller129(int a, int b) {
        return compare43(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller130(int a, int b) {
        return compare44(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller131(int a, int b) {
        return compare44(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller132(int a, int b) {
        return compare44(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller133(int a, int b) {
        return compare45(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller134(int a, int b) {
        return compare45(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller135(int a, int b) {
        return compare45(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller136(int a, int b) {
        return compare46(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller137(int a, int b) {
        return compare46(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller138(int a, int b) {
        return compare46(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller139(int a, int b) {
        return compare47(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller140(int a, int b) {
        return compare47(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller141(int a, int b) {
        return compare47(a, b) <= -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller142(int a, int b) {
        return compare48(a, b) == -1;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller143(int a, int b) {
        return compare48(a, b) < 0;
    }

    @Test(op = Operation.SMALLER)
    public static boolean testSmaller144(int a, int b) {
        return compare48(a, b) <= -1;
    }


    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual1(int a, int b) {
        return compare1(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual2(int a, int b) {
        return compare2(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual3(int a, int b) {
        return compare3(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual4(int a, int b) {
        return compare4(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual5(int a, int b) {
        return compare5(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual6(int a, int b) {
        return compare6(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual7(int a, int b) {
        return compare7(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual8(int a, int b) {
        return compare8(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual9(int a, int b) {
        return compare9(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual10(int a, int b) {
        return compare10(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual11(int a, int b) {
        return compare11(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual12(int a, int b) {
        return compare12(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual13(int a, int b) {
        return compare13(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual14(int a, int b) {
        return compare14(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual15(int a, int b) {
        return compare15(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual16(int a, int b) {
        return compare16(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual17(int a, int b) {
        return compare17(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual18(int a, int b) {
        return compare18(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual19(int a, int b) {
        return compare19(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual20(int a, int b) {
        return compare20(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual21(int a, int b) {
        return compare21(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual22(int a, int b) {
        return compare22(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual23(int a, int b) {
        return compare23(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual24(int a, int b) {
        return compare24(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual25(int a, int b) {
        return compare2(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual26(int a, int b) {
        return compare26(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual27(int a, int b) {
        return compare27(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual28(int a, int b) {
        return compare28(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual29(int a, int b) {
        return compare29(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual30(int a, int b) {
        return compare30(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual31(int a, int b) {
        return compare31(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual32(int a, int b) {
        return compare32(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual33(int a, int b) {
        return compare33(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual34(int a, int b) {
        return compare34(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual35(int a, int b) {
        return compare35(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual36(int a, int b) {
        return compare36(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual37(int a, int b) {
        return compare37(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual38(int a, int b) {
        return compare38(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual39(int a, int b) {
        return compare39(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual40(int a, int b) {
        return compare40(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual41(int a, int b) {
        return compare41(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual42(int a, int b) {
        return compare42(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual43(int a, int b) {
        return compare43(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual44(int a, int b) {
        return compare44(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual45(int a, int b) {
        return compare45(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual46(int a, int b) {
        return compare46(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual47(int a, int b) {
        return compare47(a, b) <= 0;
    }

    @Test(op = Operation.SMALLER_EQUAL)
    public static boolean testSmallerEqual48(int a, int b) {
        return compare48(a, b) <= 0;
    }


    @Test(op = Operation.EQUAL)
    public static boolean testEqual1(int a, int b) {
        return compare1(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual2(int a, int b) {
        return compare2(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual3(int a, int b) {
        return compare3(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual4(int a, int b) {
        return compare4(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual5(int a, int b) {
        return compare5(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual6(int a, int b) {
        return compare6(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual7(int a, int b) {
        return compare7(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual8(int a, int b) {
        return compare8(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual9(int a, int b) {
        return compare9(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual10(int a, int b) {
        return compare10(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual11(int a, int b) {
        return compare11(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual12(int a, int b) {
        return compare12(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual13(int a, int b) {
        return compare13(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual14(int a, int b) {
        return compare14(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual15(int a, int b) {
        return compare15(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual16(int a, int b) {
        return compare16(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual17(int a, int b) {
        return compare17(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual18(int a, int b) {
        return compare18(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual19(int a, int b) {
        return compare19(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual20(int a, int b) {
        return compare20(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual21(int a, int b) {
        return compare21(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual22(int a, int b) {
        return compare22(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual23(int a, int b) {
        return compare23(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual24(int a, int b) {
        return compare24(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual25(int a, int b) {
        return compare25(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual26(int a, int b) {
        return compare26(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual27(int a, int b) {
        return compare27(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual28(int a, int b) {
        return compare28(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual29(int a, int b) {
        return compare29(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual30(int a, int b) {
        return compare30(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual31(int a, int b) {
        return compare31(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual32(int a, int b) {
        return compare32(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual33(int a, int b) {
        return compare33(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual34(int a, int b) {
        return compare34(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual35(int a, int b) {
        return compare35(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual36(int a, int b) {
        return compare36(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual37(int a, int b) {
        return compare37(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual38(int a, int b) {
        return compare38(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual39(int a, int b) {
        return compare39(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual40(int a, int b) {
        return compare40(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual41(int a, int b) {
        return compare41(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual42(int a, int b) {
        return compare42(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual43(int a, int b) {
        return compare43(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual44(int a, int b) {
        return compare44(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual45(int a, int b) {
        return compare45(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual46(int a, int b) {
        return compare46(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual47(int a, int b) {
        return compare47(a, b) == 0;
    }

    @Test(op = Operation.EQUAL)
    public static boolean testEqual48(int a, int b) {
        return compare48(a, b) == 0;
    }


    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual1(int a, int b) {
        return compare1(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual2(int a, int b) {
        return compare2(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual3(int a, int b) {
        return compare3(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual4(int a, int b) {
        return compare4(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual5(int a, int b) {
        return compare5(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual6(int a, int b) {
        return compare6(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual7(int a, int b) {
        return compare7(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual8(int a, int b) {
        return compare8(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual9(int a, int b) {
        return compare9(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual10(int a, int b) {
        return compare10(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual11(int a, int b) {
        return compare11(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual12(int a, int b) {
        return compare12(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual13(int a, int b) {
        return compare13(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual14(int a, int b) {
        return compare14(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual15(int a, int b) {
        return compare15(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual16(int a, int b) {
        return compare16(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual17(int a, int b) {
        return compare17(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual18(int a, int b) {
        return compare18(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual19(int a, int b) {
        return compare19(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual20(int a, int b) {
        return compare20(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual21(int a, int b) {
        return compare21(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual22(int a, int b) {
        return compare22(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual23(int a, int b) {
        return compare23(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual24(int a, int b) {
        return compare24(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual25(int a, int b) {
        return compare25(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual26(int a, int b) {
        return compare26(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual27(int a, int b) {
        return compare27(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual28(int a, int b) {
        return compare28(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual29(int a, int b) {
        return compare29(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual30(int a, int b) {
        return compare30(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual31(int a, int b) {
        return compare31(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual32(int a, int b) {
        return compare32(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual33(int a, int b) {
        return compare33(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual34(int a, int b) {
        return compare34(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual35(int a, int b) {
        return compare35(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual36(int a, int b) {
        return compare36(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual37(int a, int b) {
        return compare37(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual38(int a, int b) {
        return compare38(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual39(int a, int b) {
        return compare39(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual40(int a, int b) {
        return compare40(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual41(int a, int b) {
        return compare41(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual42(int a, int b) {
        return compare42(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual43(int a, int b) {
        return compare43(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual44(int a, int b) {
        return compare44(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual45(int a, int b) {
        return compare45(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual46(int a, int b) {
        return compare46(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual47(int a, int b) {
        return compare47(a, b) >= 0;
    }

    @Test(op = Operation.GREATER_EQUAL)
    public static boolean testGreaterEqual48(int a, int b) {
        return compare48(a, b) >= 0;
    }


    @Test(op = Operation.GREATER)
    public static boolean testGreater1(int a, int b) {
        return compare1(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater2(int a, int b) {
        return compare1(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater3(int a, int b) {
        return compare1(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater4(int a, int b) {
        return compare2(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater5(int a, int b) {
        return compare2(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater6(int a, int b) {
        return compare2(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater7(int a, int b) {
        return compare3(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater8(int a, int b) {
        return compare3(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater9(int a, int b) {
        return compare3(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater10(int a, int b) {
        return compare4(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater11(int a, int b) {
        return compare4(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater12(int a, int b) {
        return compare4(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater13(int a, int b) {
        return compare5(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater14(int a, int b) {
        return compare5(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater15(int a, int b) {
        return compare5(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater16(int a, int b) {
        return compare6(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater17(int a, int b) {
        return compare6(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater18(int a, int b) {
        return compare6(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater19(int a, int b) {
        return compare7(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater20(int a, int b) {
        return compare7(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater21(int a, int b) {
        return compare7(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater22(int a, int b) {
        return compare8(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater23(int a, int b) {
        return compare8(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater24(int a, int b) {
        return compare8(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater25(int a, int b) {
        return compare9(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater26(int a, int b) {
        return compare9(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater27(int a, int b) {
        return compare9(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater28(int a, int b) {
        return compare10(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater29(int a, int b) {
        return compare10(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater30(int a, int b) {
        return compare10(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater31(int a, int b) {
        return compare11(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater32(int a, int b) {
        return compare11(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater33(int a, int b) {
        return compare11(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater34(int a, int b) {
        return compare12(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater35(int a, int b) {
        return compare12(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater36(int a, int b) {
        return compare12(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater37(int a, int b) {
        return compare13(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater38(int a, int b) {
        return compare13(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater39(int a, int b) {
        return compare13(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater40(int a, int b) {
        return compare14(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater41(int a, int b) {
        return compare14(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater42(int a, int b) {
        return compare14(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater43(int a, int b) {
        return compare15(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater44(int a, int b) {
        return compare15(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater45(int a, int b) {
        return compare15(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater46(int a, int b) {
        return compare16(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater47(int a, int b) {
        return compare16(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater48(int a, int b) {
        return compare16(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater49(int a, int b) {
        return compare17(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater50(int a, int b) {
        return compare17(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater51(int a, int b) {
        return compare17(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater52(int a, int b) {
        return compare18(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater53(int a, int b) {
        return compare18(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater54(int a, int b) {
        return compare18(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater55(int a, int b) {
        return compare19(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater56(int a, int b) {
        return compare19(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater57(int a, int b) {
        return compare19(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater58(int a, int b) {
        return compare20(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater59(int a, int b) {
        return compare20(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater60(int a, int b) {
        return compare20(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater61(int a, int b) {
        return compare21(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater62(int a, int b) {
        return compare21(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater63(int a, int b) {
        return compare21(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater64(int a, int b) {
        return compare22(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater65(int a, int b) {
        return compare22(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater66(int a, int b) {
        return compare22(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater67(int a, int b) {
        return compare23(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater68(int a, int b) {
        return compare23(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater69(int a, int b) {
        return compare23(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater70(int a, int b) {
        return compare24(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater71(int a, int b) {
        return compare24(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater72(int a, int b) {
        return compare24(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater73(int a, int b) {
        return compare25(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater74(int a, int b) {
        return compare25(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater75(int a, int b) {
        return compare25(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater76(int a, int b) {
        return compare26(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater77(int a, int b) {
        return compare26(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater78(int a, int b) {
        return compare26(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater79(int a, int b) {
        return compare27(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater80(int a, int b) {
        return compare27(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater81(int a, int b) {
        return compare27(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater82(int a, int b) {
        return compare28(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater83(int a, int b) {
        return compare28(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater84(int a, int b) {
        return compare28(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater85(int a, int b) {
        return compare29(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater86(int a, int b) {
        return compare29(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater87(int a, int b) {
        return compare29(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater88(int a, int b) {
        return compare30(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater89(int a, int b) {
        return compare30(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater90(int a, int b) {
        return compare30(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater91(int a, int b) {
        return compare31(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater92(int a, int b) {
        return compare31(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater93(int a, int b) {
        return compare31(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater94(int a, int b) {
        return compare32(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater95(int a, int b) {
        return compare32(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater96(int a, int b) {
        return compare32(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater97(int a, int b) {
        return compare33(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater98(int a, int b) {
        return compare33(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater99(int a, int b) {
        return compare33(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater100(int a, int b) {
        return compare34(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater101(int a, int b) {
        return compare34(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater102(int a, int b) {
        return compare34(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater103(int a, int b) {
        return compare35(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater104(int a, int b) {
        return compare35(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater105(int a, int b) {
        return compare35(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater106(int a, int b) {
        return compare36(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater107(int a, int b) {
        return compare36(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater108(int a, int b) {
        return compare36(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater109(int a, int b) {
        return compare37(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater110(int a, int b) {
        return compare37(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater111(int a, int b) {
        return compare37(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater112(int a, int b) {
        return compare38(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater113(int a, int b) {
        return compare38(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater114(int a, int b) {
        return compare38(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater115(int a, int b) {
        return compare39(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater116(int a, int b) {
        return compare39(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater117(int a, int b) {
        return compare39(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater118(int a, int b) {
        return compare40(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater119(int a, int b) {
        return compare40(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater120(int a, int b) {
        return compare40(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater121(int a, int b) {
        return compare41(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater122(int a, int b) {
        return compare41(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater123(int a, int b) {
        return compare41(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater124(int a, int b) {
        return compare42(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater125(int a, int b) {
        return compare42(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater126(int a, int b) {
        return compare42(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater127(int a, int b) {
        return compare43(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater128(int a, int b) {
        return compare43(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater129(int a, int b) {
        return compare43(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater130(int a, int b) {
        return compare44(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater131(int a, int b) {
        return compare44(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater132(int a, int b) {
        return compare44(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater133(int a, int b) {
        return compare45(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater134(int a, int b) {
        return compare45(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater135(int a, int b) {
        return compare45(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater136(int a, int b) {
        return compare46(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater137(int a, int b) {
        return compare46(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater138(int a, int b) {
        return compare46(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater139(int a, int b) {
        return compare47(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater140(int a, int b) {
        return compare47(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater141(int a, int b) {
        return compare47(a, b) >= 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater142(int a, int b) {
        return compare48(a, b) == 1;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater143(int a, int b) {
        return compare48(a, b) > 0;
    }

    @Test(op = Operation.GREATER)
    public static boolean testGreater144(int a, int b) {
        return compare48(a, b) >= 1;
    }


    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse1(int a, int b) {
        return compareAlwaysFalse1(a, b) == 2;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse2(int a, int b) {
        return compareAlwaysFalse1(a, b) > 1;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse3(int a, int b) {
        return compareAlwaysFalse1(a, b) >= 2;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse4(int a, int b) {
        return compareAlwaysFalse2(a, b) == 2;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse5(int a, int b) {
        return compareAlwaysFalse2(a, b) > 1;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse6(int a, int b) {
        return compareAlwaysFalse2(a, b) >= 2;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse7(int a, int b) {
        return compareAlwaysFalse3(a, b) == 2;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse8(int a, int b) {
        return compareAlwaysFalse3(a, b) > 1;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse9(int a, int b) {
        return compareAlwaysFalse3(a, b) >= 2;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse10(int a, int b) {
        return compareAlwaysFalse4(a, b) == 2;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse11(int a, int b) {
        return compareAlwaysFalse4(a, b) > 1;
    }

    @Test(op = Operation.ALWAYS_FALSE)
    public static boolean testAlwaysFalse12(int a, int b) {
        return compareAlwaysFalse4(a, b) >= 2;
    }

    public static void main(String[] args) throws Exception {
        Random rand = Utils.getRandomInstance();
        for (int i = 0; i < 20_000; ++i) {
            int low = rand.nextInt();
            int high = rand.nextInt();
            if (low == high) {
                --low;
            }
            if (low > high) {
                int tmp = low;
                low = high;
                high = tmp;
            }
            for (Method m : TestTrichotomyExpressions.class.getMethods()) {
                if (m.isAnnotationPresent(Test.class)) {
                    Operation op = m.getAnnotation(Test.class).op();
                    boolean result = (boolean)m.invoke(null, low, low);
                    Asserts.assertEquals(result, (op == Operation.EQUAL || op == Operation.SMALLER_EQUAL || op == Operation.GREATER_EQUAL) ? true : false, m + " failed");
                    result = (boolean)m.invoke(null, low, high);
                    Asserts.assertEquals(result, (op == Operation.SMALLER || op == Operation.SMALLER_EQUAL) ? true : false, m + " failed");
                    result = (boolean)m.invoke(null, high, low);
                    Asserts.assertEquals(result, (op == Operation.GREATER || op == Operation.GREATER_EQUAL) ? true : false, m + " failed");
                }
            }
        }
    }
}
