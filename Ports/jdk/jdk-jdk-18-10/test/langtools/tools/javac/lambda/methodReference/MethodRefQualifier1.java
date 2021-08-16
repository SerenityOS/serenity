/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048121
 * @summary javac complex method references: revamp and simplify
 */

public class MethodRefQualifier1 {

    interface SAM {
       void m();
    }

    static int count = 0;

    static void assertTrue(boolean cond, String msg) {
        if (!cond)
            throw new AssertionError(msg);
    }

    MethodRefQualifier1 check() {
        count++;
        return this;
    }

    void ido(Object... args) { }

    public static void main(String[] args) {
       new MethodRefQualifier1().test();
    }

    void test() {
       count = 0;
       SAM s = check()::ido;
       assertTrue(count == 1, "creation: unexpected: " + count);
       count = 0;
       s.m();
       assertTrue(count == 0, "evaluation: unexpected: " + count);
    }
}
