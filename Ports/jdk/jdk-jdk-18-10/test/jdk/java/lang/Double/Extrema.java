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
 * @bug 4408489 4826652
 * @summary Testing values of Double.{MIN_VALUE, MIN_NORMAL, MAX_VALUE}
 * @author Joseph D. Darcy
 */

public class Extrema {
    public static void main(String[] args) throws Exception {
        if (Double.MIN_VALUE != Double.longBitsToDouble(0x1L))
            throw new RuntimeException("Double.MIN_VALUE is not equal "+
                                       "to longBitsToDouble(0x1L).");

        if (Double.MIN_NORMAL != Double.longBitsToDouble(0x0010000000000000L))
            throw new RuntimeException("Double.MIN_NORMAL is not equal "+
                                       "to longBitsToDouble(0x0010000000000000L).");

        if (Double.MAX_VALUE != Double.longBitsToDouble(0x7fefffffffffffffL))
            throw new RuntimeException("Double.MAX_VALUE is not equal "+
                                       "to longBitsToDouble(0x7fefffffffffffffL).");
    }
}
