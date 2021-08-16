/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * <pre>
 *  Result:  {@value pkg1.Class1#TEST_7_PASSES}
 *  Result:  {@value pkg2.Class3#TEST_12_PASSES}
 * </pre>
 */
public class Class2 {

    /**
     * <pre>
     * Result:  {@value pkg1.Class1#TEST_8_PASSES}
     * Result:  {@value pkg2.Class3#TEST_13_PASSES}
     * </pre>
     */
    public int field;

    /**
     * <pre>
     * Result:  {@value pkg1.Class1#TEST_9_PASSES}
     * Result:  {@value pkg2.Class3#TEST_14_PASSES}
     * </pre>
     */
    public Class2() {}

    /**
     * <pre>
     * Result:  {@value pkg1.Class1#TEST_10_PASSES}
     * Result:  {@value pkg2.Class3#TEST_15_PASSES}
     * </pre>
     */
    public void method() {}

    /**
     * <pre>
     * Result:  {@value pkg1.Class1#TEST_11_PASSES}
     * Result:  {@value pkg2.Class3#TEST_16_PASSES}
     * </pre>
     */
    public class NestedClass{}
}
