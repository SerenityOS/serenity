/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jvmti/IterateThroughHeap/abort.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * This test exercises JVMTI function IterateOverHeap().
 * Test checks that if one of available callbacks returned JVMTI_VISIT_ABORT value,
 * then iteration will be stopped and no more objects will be reported.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:Abort=-waittime=5 nsk.jvmti.IterateThroughHeap.abort.Abort
 */

package nsk.jvmti.IterateThroughHeap.abort;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class Abort extends DebugeeClass {

    static {
        loadLibrary("Abort");
    }

    public static void main(String args[]) {
        String[] argv = JVMTITest.commonInit(args);
        System.exit(new Abort().runTest(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    protected Log log = null;
    protected ArgumentHandler argHandler = null;
    protected int status = Consts.TEST_PASSED;

    public int runTest(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        log.display("Verifying JVMTI_ABORT.");
        status = checkStatus(status);
        return status;
    }

}
