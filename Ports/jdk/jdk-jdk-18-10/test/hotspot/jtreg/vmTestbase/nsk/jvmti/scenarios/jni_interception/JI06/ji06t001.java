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

package nsk.jvmti.scenarios.jni_interception.JI06;

import java.io.*;
import java.util.*;
import nsk.share.*;

/**
 * The test exercises the JVMTI target area "JNI Function Interception".
 * It implements the following scenario:<br>
 * <code>Crash test. Let thread A owns the monitor of object O and thread B
 * calls JNI function MonitorEnter with the monitor, i.e. thread B is
 * blocked on entering in monitor. At this moment thread C tries to
 * redirect MonitorEnter function.</code>
 * <p>
 * The test works as follows. At first, a native thread named "owner"
 * enters a monitor. Then several native threads named "waitingThread"
 * are started trying to enter the monitor and blocked. Upon that thread
 * "redirector" intercepts the function MonitorEnter(). Finally, the
 * monitor is released by the "owner" and all waiting threads should
 * acquire it successfully by the MonitorEnter().
 */
public class ji06t001 {
    ji06t001a _ji06t001a = new ji06t001a();

    static {
        try {
            System.loadLibrary("ji06t001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"ji06t001\" library");
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
        return new ji06t001().runIt(args, out);
    }

    private int runIt(String args[], PrintStream out) {
        return check();
    }
}

/**
 * Tested class used for monitor operations
 */
class ji06t001a {}
