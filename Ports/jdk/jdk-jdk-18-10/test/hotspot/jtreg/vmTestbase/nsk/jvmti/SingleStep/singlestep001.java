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

package nsk.jvmti.SingleStep;

import java.io.*;

import nsk.share.*;
import nsk.share.jvmti.*;

/**
 * This test exercises the JVMTI event <code>SingleStep</code>.
 * <br>It verifies that this event can be enabled and disabled
 * during program execution.<br>
 * The test works as follows. Breakpoint is set at special method
 * <code>bpMethod()</code>. Upon reaching the breakpoint, agent
 * enables <code>SingleStep</code> event generation. All the received
 * events are counted. When the method <code>bpMethod()</code> is
 * leaved and accordingly, the program returns to the calling method
 * <code>runThis()</code>, the agent disables the event generation.<br>
 * At least one <code>SingleStep</code> event must be received for
 * the each methods mentioned above. Also after disabling the event
 * no more event must be received.
 */
public class singlestep001 {
    static {
        try {
            System.loadLibrary("singlestep001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"singlestep001\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native int check();

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new singlestep001().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argHandler);
        Thread.currentThread().setName("singlestep001Thr");

        log.display("\nReaching a breakpoint method ...\n");
        bpMethod();
        log.display("The breakpoint method leaved ...");

        return check();
    }

    /**
     * dummy method used only to reach breakpoint set in the agent
     */
    private void bpMethod() {
        int dummyVar = 0;
    }
}
