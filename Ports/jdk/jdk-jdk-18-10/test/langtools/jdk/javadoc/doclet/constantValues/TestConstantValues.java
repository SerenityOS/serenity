/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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

public class TestConstantValues {

    /**
     * Test 1 passes ({@value}).
     */
    public static final int TEST1PASSES = 500000;


    /**
     * Test 2 passes ({@value}).
     */
    public static final String TEST2PASSES = "My String";

    // all of if not most are in the JDK sources, it is
    // crucial we catch any discrepancies now.
    public static final byte BYTE_MAX_VALUE = 127;
    public static final byte BYTE_MIN_VALUE = -127;

    public static final char CHAR_MAX_VALUE = 65535;

    public static final double DOUBLE_MAX_VALUE = 1.7976931348623157E308;
    public static final double DOUBLE_MIN_VALUE = 4.9E-324;

    public static final float MAX_FLOAT_VALUE = 3.4028234663852886E38f;
    public static final float MIN_FLOAT_VALUE = 1.401298464324817E-45f;

    public static final boolean HELLO = true;
    public static final boolean GOODBYE = false;

    public static final int INT_MAX_VALUE = 2147483647;
    public static final int INT_MIN_VALUE = -2147483647;

    public static final long LONG_MAX_LONG_VALUE = 9223372036854775807L;
    public static final long LONG_MIN_LONG_VALUE = -9223372036854775808L;

    public static final short SHORT_MAX_VALUE = 32767;
    public static final short SHORT_MIN_VALUE = -32767;
}
