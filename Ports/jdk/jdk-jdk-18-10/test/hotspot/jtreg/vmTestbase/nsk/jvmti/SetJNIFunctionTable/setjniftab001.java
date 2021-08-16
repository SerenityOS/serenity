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

package nsk.jvmti.SetJNIFunctionTable;

import java.io.*;
import java.util.*;
import nsk.share.*;

/**
 * The test exercises the JVMTI function SetJNIFunctionTable().
 * It checks the following spec assertion:<br>
 * <code>Set the JNI function table in all current and future JNI
 * environments. As a result, all future JNI calls are directed to the
 * specified functions.</code>
 * <p>
 * The test works as follows. Upon redirection of the JNI function
 * MonitorEnter(), the interception is verified:
 * <li>inside a main thread (checking current JNI environment)
 * <li>inside new threads attached after the redirection (checking future
 * JNI environments)
 * <li>inside the main thread after detaching and attaching back
 * (checking future JNI environments)<br>
 * Finally, the original JNI function table is restored and verified
 * inside the main thread and new threads.
 */
public class setjniftab001 {
    setjniftab001a _setjniftab001a = new setjniftab001a();

    static {
        try {
            System.loadLibrary("setjniftab001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"setjniftab001\" library");
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
        return new setjniftab001().runIt(args, out);
    }

    private int runIt(String args[], PrintStream out) {
        return check();
    }
}

/**
 * Tested class used for monitor operations
 */
class setjniftab001a {}
