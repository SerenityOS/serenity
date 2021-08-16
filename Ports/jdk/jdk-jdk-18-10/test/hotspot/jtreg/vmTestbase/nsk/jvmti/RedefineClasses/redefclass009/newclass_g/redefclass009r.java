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
 * This is the new version of a redefined class
 */
public class redefclass009r {
// dummy constructor
/* fix for 4762721: see 8.8.7 Default Constructor from JLS, 2nd ed.:
   "If the class is declared public, then the default constructor
   is implicitly given the access modifier public (6.6);" */
    public redefclass009r() {
        int constr_i = 2;
        long constr_l = 3487687L;
        double constr_d = 4589.34D;
        float constr_f = 6708.05F;
        char constr_c = 'a';

        return;
    }

    public int checkIt(PrintStream out, boolean DEBUG_MODE) {
        if (DEBUG_MODE)
            out.println("NEW redefclass009r: inside the checkIt()");
        return 73;
    }

// dummy methods are below
    static double statMethod(int stat_x, int stat_y, int stat_z) {
        double stat_j = 5.0D;

        for (int stat_i=10; stat_i>stat_z; stat_i--) {
            stat_j += stat_x*stat_y;
        }
        return stat_j;
    }

    final void finMethod(char fin_c, long fin_i, int fin_j, long fin_k) {
        long fin_l = 44444L;

        do {
            fin_j -= fin_k*(fin_l+fin_i);
            fin_i = fin_j+fin_k;
            if (fin_i == 123456789L)
                break;
        } while (fin_i != 123456789L);
        float fin_f = fin_i;

        return;
    }
}
