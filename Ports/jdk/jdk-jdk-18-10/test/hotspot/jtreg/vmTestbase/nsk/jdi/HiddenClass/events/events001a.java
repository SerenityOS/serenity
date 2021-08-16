/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.HiddenClass.events;

import nsk.jdi.HiddenClass.events.DebuggeeBase;
import nsk.jdi.HiddenClass.events.HiddenClass;
import nsk.jdi.HiddenClass.events.HCInterf;
import nsk.share.jdi.ArgumentHandler;
import sun.hotspot.WhiteBox;

/* Debuggee class. */
class events001a extends DebuggeeBase {
    private final WhiteBox WB = WhiteBox.getWhiteBox();

    private events001a(ArgumentHandler argHandler) {
        super(argHandler);
    }

    public static void main(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        events001a testApp = new events001a(argHandler);
        int status = testApp.runIt();
        System.exit(DebuggerBase.JCK_STATUS_BASE + status);
    }

    public int runIt() {
        int status = DebuggerBase.PASSED;
        logMsg("\nDebuggee: started");

        try {
            testHiddenClass();
        } catch (Throwable t) {
            status = DebuggerBase.FAILED;
            logMsg("FAIL: Throwable was caught in debuggee main: " + t);
        }
        logMsg("\nDebuggee: finished");
        return status;
    }

    public void testHiddenClass() throws Exception {
        syncWithDebugger();

        // Define a hidden class.
        Class<?> hc = HiddenClass.defineHiddenClass();
        logMsg("Debuggee: defined a hidden class: " + hc.getName());

        // It is impossible to use a hidden class name to define a variable,
        // so we use the interface which the tested hidden class implements.
        HCInterf hcObj = (HCInterf)hc.newInstance();
        logMsg("Debuggee: created an instance of a hidden class: " + hc.getName());

        syncWithDebugger();

        // Invoke a hidden class method.
        logMsg("Debuggee: invoking a method of a hidden class: " + hc.getName());
        hcObj.hcMethod();

        // Provoke a hidden class unload event.
        // One more hidden class has to be defined to provoke class unload.
        // The first hidden class can not be unloaded. There are some JDI Field's
        // or Method's associated with the hidden class which keep it alive.
        logMsg("Debuggee: started provoking class unload events");
        Class<?> hcForUnload = HiddenClass.defineHiddenClass();
        hcForUnload = null;
        WB.fullGC(); // force full GC with WB to get hidden class unloaded
        logMsg("Debuggee: finished provoking class unload events");

        quitSyncWithDebugger();
    }
}
