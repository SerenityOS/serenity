/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *  check that lambda expression can appear in
 * @compile TargetType25.java
 */

class TargetType25 {

    interface F<A, B> {
        B f(A a);
    }

    <Z> void m1(F<String, Z> f) {  }
    <Z> void m2(F<String, F<String, Z>> f) {  }
    <Z> void m3(F<String, F<String, F<String, Z>>> f) {  }

    void testExprLambdaInMethodContext() {
        m1(s1 -> 1);
        m2(s1 -> s2 -> 1);
        m3(s1 -> s2 -> s3 -> 1);
    }

    void testExprLambdaInAssignmentContext() {
        F<String, Integer> fn1 = s1 -> 1;
        F<String, F<String, Integer>> fn2 = s1 -> s2 -> 1;
        F<String, F<String, F<String, Integer>>> fn3 = s1 -> s2 -> s3 -> 1;
    }

    void testStatementLambdaInMethodContext() {
        m1(s1 -> { return 1; });
        m2(s1 -> { return s2 -> { return 1; }; });
        m3(s1 -> { return s2 -> { return s3 -> { return 1; }; }; });
    }

    void testStatementLambdaInAssignmentContext() {
        F<String, Integer> fn1 = s1 -> { return 1; };
        F<String, F<String, Integer>> fn2 = s1 -> { return s2 -> { return 1; }; };
        F<String, F<String, F<String, Integer>>> fn3 = s1 -> { return s2 -> { return s3 -> { return 1; }; }; };
    }
}
