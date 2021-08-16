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

package nsk.jvmti.scenarios.allocation.AP03;

import java.io.*;
import java.lang.reflect.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ap03t001 extends DebugeeClass {
    /* number of interations to provoke garbage collecting */
    final static int GC_TRIES = 1;

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ap03t001().runThis(argv, out);
    }

    private static ap03t001 catcher = null;
    private static Wicket wicket = new Wicket();

    private native void setTag(long tag);

    /* scaffold objects */
    static ArgumentHandler argHandler = null;
    static Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    private int runThis(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        ap03t001 holder = new ap03t001();
        log.display("Set tag for tested object");
        holder.setTag(1L);
        // make an object collectable
        holder = null;

        // provoke GC and finalization
        for (int i= 0; i < GC_TRIES; i++) {
            System.gc();
            Runtime.getRuntime().runFinalization();
        }

        wicket.waitFor(timeout);
        log.display("Sync point: GC and Finalization have been called");

        if (catcher == null) {
            log.complain("Tested object has not been catched during finalization");
        }

        status = checkStatus(status);
        return status;
    }

    protected void finalize() throws Throwable {
        log.display("finalize() has been invoked");
        // restore strong reference to finalized object
        catcher = this;
        wicket.unlock();
    }
}
