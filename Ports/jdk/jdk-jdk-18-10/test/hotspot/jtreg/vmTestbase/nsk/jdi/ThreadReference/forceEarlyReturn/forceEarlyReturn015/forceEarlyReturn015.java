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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn015.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test provokes method 'ThreadReference.forceEarlyReturn()' throw 'InvalidStackFrameException'.
 *     Test scenario:
 *     Debugger enables ThreadStartRequest with suspend policy SUSPEND_ALL and starts thread listening JDI events.
 *     Then debugger forces debuggee VM to start new thread. When ThreadStartEvent is received debugger obtains
 *     ThreadReference through 'ThreadStartEvent.thread()' and call 'ThreadReference.forceEarlyReturn()', since
 *     just started thread has no frames yet 'ThreadReference.forceEarlyReturn()' should throw 'InvalidStackFrameException'.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn015.forceEarlyReturn015
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn015.forceEarlyReturn015a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn015.forceEarlyReturn015
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn015;

import java.io.PrintStream;

import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.Consts;
import nsk.share.jdi.ForceEarlyReturnDebugger;

public class forceEarlyReturn015 extends ForceEarlyReturnDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public String debuggeeClassName() {
        return forceEarlyReturn015a.class.getName();
    }

    public static int run(String argv[], PrintStream out) {
        return new forceEarlyReturn015().runIt(argv, out);
    }

    public void doTest() {
        ThreadStartRequest threadStartRequest = debuggee.getEventRequestManager().createThreadStartRequest();
        threadStartRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        threadStartRequest.enable();

        EventListenerThread eventListener = new EventListenerThread(threadStartRequest);
        eventListener.start();
        eventListener.waitStartListen();

        pipe.println(forceEarlyReturn015a.COMMAND_START_NEW_THREAD);

        ThreadStartEvent event = (ThreadStartEvent) eventListener.getEvent();

        try {
            // just started thread has no frames and InvalidStackFrameException
            // should be thrown
            event.thread().forceEarlyReturn(vm.mirrorOf(0));

            setSuccess(false);
            log.complain("Expected 'InvalidStackFrameException' was not thrown");
        } catch (InvalidStackFrameException e) {
            // expected exception
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

        threadStartRequest.disable();

        vm.resume();

        if (!isDebuggeeReady())
            return;
    }
}
