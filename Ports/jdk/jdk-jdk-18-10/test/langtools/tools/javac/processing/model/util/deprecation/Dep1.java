/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * This class is used as input to TestDeprecation.java.
 */
@ExpectedDeprecation(false)
public class Dep1 {
    /**
     * @deprecated
     */
    @ExpectedDeprecation(true)
    public void method1() {}

    @Deprecated
    @ExpectedDeprecation(true)
    public void method2() {}


    // Parameters can't be deprecated.
    public void method3(/**
                         * TODO re-deprecate once bug 6404756 is fixed.
                         * @ deprecated
                         */
                        @ExpectedDeprecation(false)
                        Object method3_param0) {}

    public void method4(// @Deprecated -- TODO uncomment once bug 6404756 is fixed.
                        @ExpectedDeprecation(false)
                        Object method4_param0, Object method4_param1) {}

    @ExpectedDeprecation(false)
    public void methodn() {}

    @ExpectedDeprecation(false)
    private Object field0;

    @Deprecated
    @ExpectedDeprecation(true)
    private Object field1;


    /**
     * @deprecated
     */
    @ExpectedDeprecation(true)
    private Object field3;

    @ExpectedDeprecation(true)
    @Deprecated
    class NestedClass {}
}
