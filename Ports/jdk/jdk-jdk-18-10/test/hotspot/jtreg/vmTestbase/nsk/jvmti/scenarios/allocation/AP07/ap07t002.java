/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.allocation.AP07;

import java.io.*;
import java.lang.reflect.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ap07t002 extends DebugeeClass {
    /* number of interations to provoke garbage collecting */
    final static long IGNORE_TAG = 10l;

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ap07t002().runThis(argv, out);
    }

    private ap07t002 right;
    private ap07t002 left;

    private native void setTag(ap07t002 target, long tag);

    /* scaffold objects */
    static ArgumentHandler argHandler = null;
    static Log log = null;
    static long timeout = 0;
    int status = Consts.TEST_PASSED;

    private int runThis(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        // build right branch
        ap07t002 rightLoc = new ap07t002();
        rightLoc.left     = new ap07t002();
        rightLoc.right    = new ap07t002();
        setTag(rightLoc,       IGNORE_TAG);
        setTag(rightLoc.left,  IGNORE_TAG + 1l);
        setTag(rightLoc.right, IGNORE_TAG + 2l);

        // build left branch
        ap07t002 leftLoc  = new ap07t002();
        leftLoc.left      = new ap07t002();
        leftLoc.right     = new ap07t002();
        setTag(leftLoc,       1l);
        setTag(leftLoc.left,  2l);
        setTag(leftLoc.right, 3l);

        status = checkStatus(status);
        return status;
    }
}
