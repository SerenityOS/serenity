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

package nsk.jvmti.scenarios.general_functions.GF01;

import java.io.*;
import java.util.*;
import nsk.share.*;

/**
 * The test exercises the JVMTI target area "General Functions".
 * It implements the following scenario:<br>
 * <code>Check that Hotspot provides "highly recommended" properties with
 * GetSystemProperties, GetSystemProperty in the OnLoad and Live phases:<pre>
 *
 *           java.vm.vendor
 *           java.vm.version
 *           java.vm.name
 *           java.vm.info
 *           java.library.path
 *           java.class.path</pre></code>
 * <br>
 * The tested JVMTI functions are verified thrice, in particular, inside
 * Agent_OnLoad() (i.e. during the OnLoad phase), VMInit callback and
 * VMDeath callback (the Live phase). All the highly recommended
 * properties mentioned above must be found.
 */
public class gf01t001 {
    static {
        try {
            System.loadLibrary("gf01t001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"gf01t001\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native int check();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        return new gf01t001().runIt(args, out);
    }

    private int runIt(String args[], PrintStream out) {
        return check();
    }
}
