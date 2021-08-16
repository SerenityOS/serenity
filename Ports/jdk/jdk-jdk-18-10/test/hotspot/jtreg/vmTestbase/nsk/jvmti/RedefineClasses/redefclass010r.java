/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

// THIS TEST IS LINE NUMBER SENSITIVE

package nsk.jvmti.RedefineClasses;

import java.io.PrintStream;

/**
 * This is the old version of a redefined class with some empty
 * commented out lines
 */
public class redefclass010r { // redefclass010.c::orig_ln[0][0]
    public int checkIt(PrintStream out, boolean DEBUG_MODE) {
//
//
//
//
        if (DEBUG_MODE)                                              // redefclass010.c::orig_ln[1][0]
            out.println("OLD redefclass010r: inside the checkIt()"); // redefclass010.c::orig_ln[1]10]
//
        return 19;                                                   // redefclass010.c::orig_ln[1][2]
    }

// dummy methods are below
    static double statMethod(int x, int y, int z) {
        double j = 5.0D;         // redefclass010.c::orig_ln[3][0]

        for (int i=10; i>z; i--) // redefclass010.c::orig_ln[3][1] // redefclass010.c::orig_ln[3][3]
            j += x*y;            // redefclass010.c::orig_ln[3][2]
        return j;                // redefclass010.c::orig_ln[3][4]
    }

    final void finMethod(char c, long i, int j, long k) {} // redefclass010.c::orig_ln[2][0]
}
