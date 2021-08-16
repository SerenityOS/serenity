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

package nsk.jvmti.GetLocalVariableTable;

import java.io.*;

import nsk.share.*;

/**
 * This test checks that the JVMTI function <code>GetLocalVariableTable()</code>
 * returns local variable information properly.<br>
 * The test creates a dummy instance of tested class 'localtab004a' which
 * must be compiled with debugging info. Then an agent part verifies
 * the local variable table for the following methods of localtab004a:
 * <li>constructor
 * <li>static method statMethod()
 * <li>instance method finMethod()
 */
public class localtab004 {
    static {
        try {
            System.loadLibrary("localtab004");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"localtab004\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native int check(localtab004a _localtab004a);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new localtab004().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        localtab004a _localtab004a = new localtab004a();

        return check(_localtab004a);
    }
}

/**
 * Dummy class used only for testing local variable information in an agent.
 */
class localtab004a {
    localtab004a() {
        int constr_i = 2;
        long constr_l = 3487687L;
        double constr_d = 4589.34D;
        float constr_f = 6708.05F;
        char constr_c = 'a';

        return;
    }

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
    }
}
