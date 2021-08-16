/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

/**
 * @deprecated class_test1 passes.
 */
@Deprecated(forRemoval=true)
public class TestClass {

    /**
     * @deprecated class_test2 passes. This is the second sentence of deprecated description for a field.
     */
    public int field;

    /**
     * @deprecated class_test3 passes. This is the second sentence of deprecated description for a constructor.
     */
    @Deprecated(forRemoval=true)
    public TestClass() {}

    /**
     * @deprecated class_test4 passes. Overloaded constructor.
     */
    @Deprecated(forRemoval=true)
    public TestClass(String s) {}

    /**
     * @deprecated class_test5 passes. This is the second sentence of deprecated description for a method.
     */
    public void method() {}

    /**
     * @deprecated class_test6 passes. Overloaded method 1.
     */
    public void overloadedMethod(String s) {}

    /**
     * @deprecated class_test7 passes. Overloaded method 2.
     */
    public void overloadedMethod(int i) {}
}
