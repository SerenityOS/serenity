/*
 * Copyright (c) 1997, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4082816
 * @summary In jdk1.2beta1 this causes a NullPointerException in the compiler.
 *          That is bad.  This text is part of the test for yet another bug
 *          which was hidden by the regression.
 * @author turnidge
 *
 * @compile InnerMemberRegression.java
 */

class InnerMemberRegression {
    void  test() {
        class Z {
            class Y {
                void test() {
                }
            }
            Y y = new Y();
            int m=100;

            void test() {
                y.test();
            }
        }  //end of class Z
        Z z = new Z();
        z.test();
    }

    public  static  void  main(String[]  s) {
        InnerMemberRegression x  =  new InnerMemberRegression();
        x.test();
    }
}
