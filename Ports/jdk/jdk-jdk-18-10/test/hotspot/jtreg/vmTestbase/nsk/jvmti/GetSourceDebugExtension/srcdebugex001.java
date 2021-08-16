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

package nsk.jvmti.GetSourceDebugExtension;

import java.io.*;

/**
 * This test checks that the SourceDebugExtension attribute can be
 * obtained by the JVMTI function <code>GetSourceDebugExtension()</code>,
 * otherwise the function should return the error
 * <code>JVMTI_ERROR_ABSENT_INFORMATION</code>.
 */
public class srcdebugex001 {
    static final int JCK_STATUS_BASE = 95;

    static boolean DEBUG_MODE = false;
    private PrintStream out;

    static {
        try {
            System.loadLibrary("srcdebugex001");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load srcdebugex001 library");
            System.err.println("java.library.path:" +
                System.getProperty("java.library.path"));
            throw e;
        }
    }

    native static int getSrcDebugX(boolean vrb);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new srcdebugex001().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {
        this.out = out;
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].equals("-v")) // verbose mode
                DEBUG_MODE = true;
        }

        if (DEBUG_MODE)
            out.println("Going to get the debug extension information...");
        return getSrcDebugX(DEBUG_MODE);
    }
}
