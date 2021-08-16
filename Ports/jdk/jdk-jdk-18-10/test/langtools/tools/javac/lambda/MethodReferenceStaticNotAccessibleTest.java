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
 * @bug 8136809
 * @summary Javac fails compiling Collectors.reducing with method reference combiner
 * @compile MethodReferenceStaticNotAccessibleTest.java
 */

import java.util.function.BinaryOperator;

class MethodReferenceStaticNotAccessibleTest_Foo {
    MethodReferenceStaticNotAccessibleTest_Foo m(MethodReferenceStaticNotAccessibleTest_Foo foo) { return null; }
    private static void m(MethodReferenceStaticNotAccessibleTest_Foo foo1, MethodReferenceStaticNotAccessibleTest_Foo foo2) {}
}

public class MethodReferenceStaticNotAccessibleTest {
    <T> void m(T t, BinaryOperator<T> binop) {}

    void test(MethodReferenceStaticNotAccessibleTest_Foo foo) {
        m(foo, MethodReferenceStaticNotAccessibleTest_Foo::m);
    }
}
