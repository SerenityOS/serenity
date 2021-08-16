/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package pkg1;

/**
 * Result:  {@value TEST_2_PASSES}
 */
public class Class1 {

    /**
     * Result:  {@value}
     */
    public static final String TEST_1_PASSES = "Test 1 passes";

    public static final String TEST_2_PASSES  = "Test 2 passes";
    public static final String TEST_3_PASSES  = "Test 3 passes";
    public static final String TEST_4_PASSES  = "Test 4 passes";
    public static final String TEST_5_PASSES  = "Test 5 passes";
    public static final String TEST_6_PASSES  = "Test 6 passes";
    public static final String TEST_7_PASSES  = "Test 7 passes";
    public static final String TEST_8_PASSES  = "Test 8 passes";
    public static final String TEST_9_PASSES  = "Test 9 passes";
    public static final String TEST_10_PASSES = "Test 10 passes";
    public static final String TEST_11_PASSES = "Test 11 passes";

    /**
     * Invalid (non-constant field): {@value}
     */
    public static String TEST_12_ERROR = "Test 12 generates an error message";

    /**
     * Invalid (null): {@value}
     */
    public static final String NULL = null;

    /**
     * Result:  {@value TEST_3_PASSES}
     */
    public int field;

    /**
     * Result:  {@value TEST_4_PASSES}
     */
    public Class1() {}

    /**
     * Result:  {@value TEST_5_PASSES}
     */
    public void method() {}

    /**
     * Result:  {@value TEST_12_ERROR}
     */
    public void invalidValueTag1() {}

    /**
     * Result:  {@value}
     */
    public void invalidValueTag2() {}

    /**
     * Result:  {@value NULL}
     */
    public void testNullConstant() {}

    /**
     * Result:  {@value pkg1.Class1#TEST_6_PASSES}
     */
    public class NestedClass{}
}
