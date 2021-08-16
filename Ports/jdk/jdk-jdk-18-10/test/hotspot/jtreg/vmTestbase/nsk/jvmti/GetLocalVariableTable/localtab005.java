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
import java.util.*;

import nsk.share.*;

/**
 * This test checks that the JVMTI function <code>GetLocalVariableTable()</code>
 * returns local variable information properly including variables with generic
 * types.<br>
 * The test creates a dummy instance of tested class <code>localtab005a</code>
 * which must be compiled with debugging info. Then an agent part obtains the
 * local variable table for the following methods of localtab005a:
 * <li>constructor
 * <li>static method statMethod()
 * <li>instance method insMethod()<br>
 * Some local variables of these methods have generic types. The generic
 * signature should be returned properly for these variables, and NULL for
 * non-generic ones.
 */
public class localtab005 {
    static {
        try {
            System.loadLibrary("localtab005");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"localtab005\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native int check(localtab005a _localtab005a);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new localtab005().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        localtab005a _localtab005a = new localtab005a();

        return check(_localtab005a);
    }
}

/*
 * Dummy classes used only for verifying local variable information
 * in an agent.
 */

class localtab005b<L extends String> {
    localtab005b(String par) {}
}

class localtab005c<A, B extends Integer> {}

interface localtab005if<I> {}

class localtab005d<T> implements localtab005if<T> {}

class localtab005e {}

class localtab005f extends localtab005e implements localtab005if {}

class localtab005g<E extends localtab005e & localtab005if> {}

class localtab005a {
    localtab005a() {
        localtab005b<String> constr_b =
            new localtab005b<String>(new String("bang"));
        int constr_i = 2;
        localtab005c<Boolean, Integer> constr_c =
            new localtab005c<Boolean, Integer>();
        float constr_f = 6708.05F;
        char constr_ch = 'a';
        localtab005if<Object> constr_if =
            new localtab005d<Object>();
    }

    static double statMethod(int stat_x, int stat_y, int stat_z) {
        double stat_j = 5.0D;
        localtab005d<Byte> stat_d =
            new localtab005d<Byte>();

        return stat_j;
    }

    void insMethod(char ins_c, long ins_i, localtab005d<Object> ltab005d, long ins_k) {
        long ins_l = 44444L;
        localtab005g<localtab005f> ins_g =
            new localtab005g<localtab005f>();
    }
}
