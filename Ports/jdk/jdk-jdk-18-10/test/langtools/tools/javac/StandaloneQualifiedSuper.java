/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4147520
 * @summary Qualified 'super' must further qualify a member -- it cannot stand alone.
 *
 * @run compile/fail StandaloneQualifiedSuper.java
 */

public class StandaloneQualifiedSuper {

    public class AS { }

    public class BS { }

    public class CS { }

    public class A extends AS {
        A() { super(); }
        public class B extends BS {
            B() { super(); }
            public class C extends CS {
                C() { super(); }
                void test() {
                    // '<class>.super' must qualify another field or
                    // method -- it cannot stand alone.
                    System.out.println(B.super);  // ERROR
                }
            }
        }
    }
}
