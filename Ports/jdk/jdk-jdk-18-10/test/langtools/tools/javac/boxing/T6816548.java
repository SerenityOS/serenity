/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6816548
 * @summary Uninitialized register when performing casting + auto(un)boxing
 * @author  mcimadamore
 */
public class T6816548 {

    public static void main(String[] args) {
        testInt();
        testShort();
        testByte();
        testChar();
    }

    public static void testInt() {
        final int fi = 0;
        Byte b = fi;
        Short s = fi;
        Character c = fi;
    }

    public static void testShort() {
        final short fs = 0;
        Byte b = fs;
        Short s = fs;
        Character c = fs;
    }

    public static void testByte() {
        final byte fb = 0;
        Byte b = fb;
        Short s = fb;
        Character c = fb;
    }

    public static void testChar() {
        final char fc = '0';
        Byte b = fc;
        Short s = fc;
        Character c = fc;
    }
}
