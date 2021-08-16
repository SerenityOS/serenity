/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/ownedMonitorsAndFrames/ownedMonitorsAndFrames009.
 * VM Testbase keywords: [jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *
 *     Test checks that acquired ReentrantLock, ReentrantReadWriteLock.ReadLock and ReentrantReadWriteLock.WriteLock
 *     are not returned by ThreadReference,ownedMonitorsAndFrames.
 *
 *     Debuggee creates test thread wich acquires ReentrantLock, ReentrantReadWriteLock.ReadLock and
 *     ReentrantReadWriteLock.WriteLock.
 *
 *     Debugger obtains ThreadReference for debugee's test thread and checks that ownedMonitorsAndFrames() returns
 *     empty list.
 *     Then, debugger forces test thread release all locks and checks again that ownedMonitorsAndFrames() returns empty list.
 *
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames009.ownedMonitorsAndFrames009
 *        nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames009.ownedMonitorsAndFrames009a
 * @run main/othervm/native
 *      nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames009.ownedMonitorsAndFrames009
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames009;

import java.io.PrintStream;
import java.util.ArrayList;
import com.sun.jdi.ThreadReference;
import nsk.share.Consts;
import nsk.share.jdi.OwnedMonitorsDebugger;

public class ownedMonitorsAndFrames009 extends OwnedMonitorsDebugger {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ownedMonitorsAndFrames009().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return ownedMonitorsAndFrames009a.class.getName();
    }

    private void test(ThreadReference thread) {
        thread.suspend();

        try {
            // compare result of ownedMonitorsAndFrames() with empty list
            compare(thread.ownedMonitorsAndFrames(), new ArrayList<DebugMonitorInfo>());
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

        thread.resume();
    }

    public void doTest() {
        ThreadReference thread = debuggee.threadByName(ownedMonitorsAndFrames009a.testThreadName);

        test(thread);

        pipe.println(ownedMonitorsAndFrames009a.COMMAND_RELEASE_ALL_LOCKS);

        if (!isDebuggeeReady())
            return;

        test(thread);

        pipe.println(ownedMonitorsAndFrames009a.COMMAND_STOP_TEST_THREAD);

        if (!isDebuggeeReady())
            return;
    }
}
