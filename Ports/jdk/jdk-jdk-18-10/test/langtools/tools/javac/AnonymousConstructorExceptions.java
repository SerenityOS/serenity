/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4337978
 * @summary Verify that anonymous classes inheriting from classes whose constructors
 * are declared to throw exceptions can be succesfully compiled.
 * @author William Maddox
 *
 * @compile AnonymousConstructorExceptions.java
 */

class AnonymousConstructorExceptions {

    static class A1 {
        A1() throws RuntimeException {}
    }

    static class A2 {
        A2() throws Error {}
    }

    static class A3 {
        A3() throws Exception {}
    }

    void test1() {
        A1 bar = new A1() { };
    }

    void test2() {
        A2 bar = new A2() { };
    }

    void test3() throws Exception {
        A3 bar = new A3() { };
    }

}
