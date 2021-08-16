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

package nsk.jvmti.RedefineClasses;

import java.io.PrintStream;

/**
 * This is the old version of a redefined class
 */
public class redefclass008r {
    public int checkIt(PrintStream out, boolean DEBUG_MODE) {
        int i = 234;

        if (DEBUG_MODE)
            out.println("OLD redefclass008r: inside the checkIt()");
        return 19;
    }

// dummy methods are below
    static int statMethod(int x, int y, int z) {
        int j = 5;

        for (int i=10; i>z; i--) {
            j += x*y;
        }
        return j;
    }

    final void finMethod(long i, int j, long k) {
        long l = 44444L;

        do {
            j -= k*(l+i);
            i = j+k;
            if (i == 123456789L)
                break;
        } while (i != 123456789L);
        return;
    }
}
