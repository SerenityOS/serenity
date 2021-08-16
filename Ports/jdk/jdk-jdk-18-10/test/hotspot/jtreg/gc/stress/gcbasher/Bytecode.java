/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package gc.stress.gcbasher;

class Bytecode {
    public static final int IINC               = 132;
    public static final int TABLESWITCH        = 170;
    public static final int LOOKUPSWITCH       = 171;
    public static final int GETSTATIC          = 178;
    public static final int PUTSTATIC          = 179;
    public static final int GETFIELD           = 180;
    public static final int PUTFIELD           = 181;
    public static final int INVOKEVIRTUAL      = 182;
    public static final int INVOKESPECIAL      = 183;
    public static final int INVOKESTATIC       = 184;
    public static final int INVOKEINTERFACE    = 185;
    public static final int NEW                = 187;
    public static final int ANEWARRAY          = 189;
    public static final int CHECKCAST          = 192;
    public static final int INSTANCEOF         = 193;
    public static final int MULTIANEWARRAY     = 197;
    public static final int WIDE               = 196;

    private static final int lengths[] = {
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        2,
        3,
        2,
        3,
        3,
        2,
        2,
        2,
        2,
        2,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        2,
        2,
        2,
        2,
        2,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        3,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        1,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        2,
        99,
        99,
        1,
        1,
        1,
        1,
        1,
        1,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        5,
        5,
        3,
        2,
        3,
        1,
        1,
        3,
        3,
        1,
        1,
        0,
        4,
        3,
        3,
        5,
        5,
        1
    };

    public static int getLength(int bc) throws IllegalArgumentException {
        if ((bc < 0) || (bc >= lengths.length)) {
            throw new IllegalArgumentException("Unknown bytecode " + bc);
        }
        return lengths[bc];
    }
}
