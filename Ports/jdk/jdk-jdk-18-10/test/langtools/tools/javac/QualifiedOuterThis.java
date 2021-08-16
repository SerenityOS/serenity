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
 * @bug 4091327 4079087
 * @summary Verify resolution of qualified outer class 'this' references.
 * @author William Maddox (maddox) [shamelessly cribbed from bug report]
 *
 * @clean QualifiedOuterThis QualifiedOuterThis$Y QualifiedOuterThis$Y$Z
 * @compile QualifiedOuterThis.java
 * @run main QualifiedOuterThis
 */

public class QualifiedOuterThis {
    static StringBuffer sb = new StringBuffer();
    public String toString() { sb.append('X'); return "X"; }
    void test() {
        class Y {
            public String toString() { sb.append('Y'); return "Y"; }
            class Z {
                public String toString() { sb.append('Z'); return "Z"; }
                void test() {
                    System.out.println(this.toString());
                    System.out.println(Y.this.toString());
                    System.out.println(QualifiedOuterThis.this.toString());
                }
            }
            void test() {
                new Z().test();
            }
        }
        new Y().test();
    }
    public static void main(String[] s) throws Exception {
        QualifiedOuterThis x = new QualifiedOuterThis();
        x.test();  // Print Z Y X
        System.out.println(sb.toString());
        if (!sb.toString().equals("ZYX")) {
            throw new Exception("incorrect outer instance method called!");
        }
    }
}
