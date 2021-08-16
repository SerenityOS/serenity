/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8206986
 * @summary Check definite (un)assignment for in switch expressions.
 * @compile ExpressionSwitchDA.java
 * @run main ExpressionSwitchDA
 */

public class ExpressionSwitchDA {
    public static void test1() {
        int i;
        int j = 0;
        switch (j) {
            case 0 : i=42; break;
            default: i=42;
        }
        System.out.println(i);
    }
    public static void test2(){
        int i;
        int j = 0;
        switch (j) {
            case 0  -> i=42;
            default -> i=42;
        }
        System.out.println(i);
    }
    public static void test3(){
        int i;
        int j = 0;
        switch (j) {
            case 0  -> { i=42; }
            default -> { i=42; }
        }
        System.out.println(i);
    }
    public static void test4(){
        int i;
        int j = 0;
        int k = switch (j) {
            case 0  -> i=42;
            default -> i=42;
        };
        System.out.println(i);
    }
    public static void test5(){
        int i;
        int j = 0;
        int k = switch (j) {
            case 0  -> { i=42; yield 42; }
            default -> i=42;
        };
        System.out.println(i);
    }
    public static void test6(){
        int i;
        int j = 0;
        int k = switch (j) {
            case 0  -> i=42;
            default -> { i=42; yield 42; }
        };
        System.out.println(i);
    }
    public static void test7(){
        int i;
        int j = 0;
        int k = switch (j) {
            case 0  -> { i=42; yield 42; }
            default -> { i=42; yield 42; }
        };
        System.out.println(i);
    }
    public static void test8() {
        int i;
        int j = 0;
        switch (j) {
            case 0 : i=42; break;
            default: throw new NullPointerException();
        }
        System.out.println(i);
    }
    public static void test9() {
        int i;
        int j = 0;
        switch (j) {
            case 0 -> i=42;
            default ->  throw new NullPointerException();
        }
        System.out.println(i);
    }
    public static void test10() {
        int i;
        int j = 0;
        switch (j) {
            case 0 -> { i=42; System.out.print(i);}
            default ->  throw new NullPointerException();
        }
        System.out.println(i);
    }
    public static void test11() {
        int i;
        int j = 0;
        switch (j) {
            case 0 -> { i=42; System.out.print(i);}
            default ->  { throw new NullPointerException(); }
        }
        System.out.println(i);
    }
    public static void test12() {
        int i;
        int j = 0;
        switch (j) {
            case 0 : i=42; break;
            default: return;
        }
        System.out.println(i);
    }
    public static void test13() {
        int i;
        int j = 0;
        switch (j) {
            case 0 -> i=42;
            default -> { return; }
        }
        System.out.println(i);
    }
    public static void test14() {
        int i;
        int j = 0;
        switch (j) {
            case 0 -> { i=42; }
            default -> { return; }
        }
        System.out.println(i);
    }
    public static void test15() {
        final int i;
        int j = 0;
        switch (j) {
            case 0 -> { i=42; }
            default -> { i=42; }
        }
        System.out.println(i);
    }
    public static void main(String[] args) {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
        test7();
        test8();
        test9();
        test10();
        test11();
        test12();
        test13();
        test14();
        test15();
    }
}
