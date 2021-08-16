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

package nsk.jvmti.scenarios.allocation.AP06;

import java.io.*;
import java.lang.reflect.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ap06t001 extends DebugeeClass {
    /* number of interations to provoke garbage collecting */
    final static int GC_TRIES = 1;

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ap06t001().runThis(argv, out);
    }

    static Wicket startWicket = new Wicket();
    static Wicket endWicket = new Wicket();
    private static ap06t001Thread thread = null;

    /* scaffold objects */
    static ArgumentHandler argHandler = null;
    static Log log = null;
    static long timeout = 0;
    int status = Consts.TEST_PASSED;

    private int runThis(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        thread = new ap06t001Thread();
        thread.setTag();
        thread.start();
        startWicket.waitFor();

        status = checkStatus(status);

        endWicket.unlock();
        if (thread.isAlive()) {
            try {
                thread.join(timeout);
            } catch (InterruptedException e) {
                // OK
            }
        }
        return status;
    }
}

class ap06t001Thread extends Thread {
    public void run() {
        ap06t001.log.display("Checked thread started.");
        ap06t001.startWicket.unlock();

        ap06t001.endWicket.waitFor(ap06t001.timeout);
    }

    public native void setTag();
}
