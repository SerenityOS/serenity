/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary JDI test for hidden classes
 *
 * @library /vmTestbase
 *          /test/lib
 * @modules java.base/jdk.internal.misc:+open
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @build nsk.jdi.HiddenClass.events.*
 *
 * @run main/othervm
 *      nsk.jdi.HiddenClass.events.events001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                       -XX:+WhiteBoxAPI ${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.HiddenClass.events;

import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;

import com.sun.jdi.request.EventRequest;

import nsk.jdi.HiddenClass.events.DebuggerBase;
import nsk.jdi.HiddenClass.events.EventHandler;

import nsk.share.Log;
import nsk.share.jdi.ArgumentHandler;

// This class is the test debugger
public class events001 extends DebuggerBase {
    static final String PACKAGE_NAME  = "nsk.jdi.HiddenClass.events";
    static final String DEBUGGEE_NAME = PACKAGE_NAME + ".events001a";
    static final String CHECKED_CLASS = PACKAGE_NAME + ".HiddenClass";
    static final String HC_FILTER = CHECKED_CLASS + "/0x*";

    private events001(ArgumentHandler argHandler) {
        super(argHandler);
    }

    public static void main (String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);

        events001 debugger = new events001(argHandler);
        System.exit(debugger.run(argHandler) + JCK_STATUS_BASE);
    }

    public int run(ArgumentHandler argHandler) {
        boolean testFailed = false;
        EventRequest breakpointRequest = null;
        EventRequest classPrepareRequest = null;
        EventRequest classUnloadRequest = null;
        EventRequest modWatchpointRequest = null;
        launchDebuggee(argHandler, DEBUGGEE_NAME);

        try {
            EventHandler eventHandler = EventHandler.createAndStart(this);

            // sync with debuggee
            readyCmdSync();

            // request a ClassPrepare event for the hidden class "HiddenClass/0x*"
            classPrepareRequest = enableClassPrepareRequest(HC_FILTER);

            // request a ClassUnload event for the hidden class "HiddenClass/0x*"
            classUnloadRequest = enableClassUnloadRequest(HC_FILTER);

            // sync with debuggee
            runCmdSync();
            readyCmdSync();

            // There is a latency in getting events from the debuggee
            // on the debugger side over the wire protocol, so we may
            // need to wait for ClassPrepareEvent to be posted.
            ReferenceType hcRefType = eventHandler.waitAndGetHCRefType();

            /* Hidden class has to be prepared at this point. */

            // request a Breakpoint event in the hidden class method "hcMethod"
            Method method = findMethod(hcRefType, "hcMethod");
            breakpointRequest = enableBreakpointRequest(method);

            // request a ModificationWatchpoint event on the hidden class field "hcField"
            Field field = findField(hcRefType, "hcField");
            modWatchpointRequest = enableModificationWatchpointRequest(field, HC_FILTER);

            // sync with debuggee
            runCmdSync();
            doneCmdSync();

            eventHandler.waitForCompleteness();
            testFailed |= eventHandler.failedStatus();
        } catch (Throwable t) {
            log.complain("FAIL: " + t.getMessage());
            t.printStackTrace(log.getOutStream());
            testFailed = true;
        } finally {
            // disable event requests
            disableRequest(breakpointRequest, "BreakpointRequest");
            disableRequest(classPrepareRequest, "ClassPrepareRequest");
            disableRequest(classUnloadRequest, "ClassUnloadRequest");
            disableRequest(modWatchpointRequest, "ModificationWatchpointRequest");

            // sync with debuggee
            quitCmdSync();
            testFailed |= shutdownDebuggee();
        }
        // check test results
        if (testFailed) {
            log.complain("# TEST FAILED");
            return FAILED;
        }
        log.display("# TEST PASSED");
        return PASSED;
    }
}
