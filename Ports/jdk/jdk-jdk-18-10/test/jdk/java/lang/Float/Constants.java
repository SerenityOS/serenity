/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @compile Constants.java
 * @bug 4397405 4826652
 * @summary Testing constant-ness of Float.{MIN_VALUE, MAX_VALUE}, etc.
 * @author Joseph D. Darcy
 */

public class Constants {
    /*
     * This compile-only test is to make sure that the primitive
     * public static final fields in java.lang.Float are "constant
     * expressions" as defined by "The Java Language Specification,
     * 2nd edition" section 15.28; a different test checks the values
     * of those fields.
     */
    public static void main(String[] args) throws Exception {
        int i = 0;
        switch (i) {
        case (int)Float.NaN:                    // 0
            System.out.println("Float.NaN is a constant!");
            break;
        case (int)Float.MIN_VALUE + 1:          // 0 + 1
            System.out.println("Float.MIN_VALUE is a constant!");
            break;
        case (int)Float.MIN_NORMAL + 2:         // 0 + 2
            System.out.println("Float.MIN_NORMAL is a constant!");
            break;
        case Float.MIN_EXPONENT:                // -126
            System.out.println("Float.MIN_EXPONENT is a constant!");
            break;
        case Float.MAX_EXPONENT:                // 127
            System.out.println("Float.MAX_EXPONENT is a constant!");
            break;
        case (int)Float.MAX_VALUE - 1:          // Integer.MAX_VALUE - 1
            System.out.println("Float.MAX_VALUE is a constant!");
            break;
        case (int)Float.POSITIVE_INFINITY:      // Integer.MAX_VALUE
            System.out.println("Float.POSITIVE_INFINITY is a constant!");
            break;
        case (int)Float.NEGATIVE_INFINITY:      // Integer.MIN_VALUE
            System.out.println("Float.NEGATIVE_INFINITY is a constant!");
            break;
        }
    }
}
