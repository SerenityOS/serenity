/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6227936
 * @summary Wrong type of inherited method using specialized type parameter
 * @author  Peter von der Ah\u00e9
 * @compile T6227936.java
 */

public class T6227936 {
    static class A<T extends B<?>> {}

    abstract static class B<T extends C> {
        public abstract T getC();
    }

    static class C {}

    abstract static class A1<T extends B1<?>> extends A<T> {
        void test(T t) {
            C1 c1 = t.getC();
        }
    }

    abstract static class B1<T extends C1> extends B<T> {
    }

    abstract static class C1 extends C {
        abstract void someMethod();
    }
}
