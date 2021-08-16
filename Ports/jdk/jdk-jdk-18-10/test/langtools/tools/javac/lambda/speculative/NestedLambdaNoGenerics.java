/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
  * @bug 8078093
  * @summary Exponential performance regression Java 8 compiler compared to Java 7 compiler
  * @compile NestedLambdaNoGenerics.java
  */
import java.util.concurrent.Callable;

class NestedLambdaNoGenerics {
    void test() {
        m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null,
                () -> m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null,
                () -> m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null,
                () -> m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null,
                () -> m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null, () -> m(null,
                (Callable<String>)null)))))))))))))))))))))))))))))));
    }
    static class A0 { }
    static class A1 { }
    static class A2 { }
    static class A3 { }
    static class A4 { }
    String m(A0 t, Callable<A0> ct) { return ""; }
    String m(A1 t, Callable<A1> ct) { return ""; }
    String m(A2 t, Callable<A2> ct) { return ""; }
    String m(A3 t, Callable<A3> ct) { return ""; }
    String m(A4 t, Callable<A4> ct) { return ""; }
    String m(Object o, Callable<String> co) { return ""; }
}
