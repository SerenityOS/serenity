/*
 * Copyright (c) 2017, Red Hat, Inc. All rights reserved.
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
 * @bug 8188151
 * @summary assert failure with 2 handlers at same bci
 * @run main/othervm -XX:-BackgroundCompilation -XX:CompileOnly=TestC1ExceptionHandlersSameBCI::test1 -XX:CompileOnly=TestC1ExceptionHandlersSameBCI::test2 -XX:CompileCommand=dontinline,TestC1ExceptionHandlersSameBCI::not_inline1 -XX:CompileCommand=dontinline,TestC1ExceptionHandlersSameBCI::not_inline2 TestC1ExceptionHandlersSameBCI
 *
 */

public class TestC1ExceptionHandlersSameBCI {
    static class Ex1 extends Exception {

    }
    static class Ex2 extends Exception {

    }

    static void not_inline1() throws Ex1, Ex2 {

    }

    static void not_inline2(int v) {

    }

    static void test1() throws Ex1, Ex2 {
        int i = 0;
        try {
            not_inline1();
            i = 1;
            not_inline1();
        } catch (Ex1|Ex2 ex) {
            not_inline2(i);
        }
    }

    static void test2() {
        int i = 0;
        try {
            test1();
            i = 1;
            test1();
        } catch (Ex1|Ex2 ex) {
            not_inline2(i);
        }
    }

    static public void main(String[] args) {
        for (int i = 0; i < 5000; i++) {
            test2();
        }
    }
}
