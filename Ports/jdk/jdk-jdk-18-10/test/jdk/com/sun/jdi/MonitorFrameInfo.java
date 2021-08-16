/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6230699
 * @summary Test ThreadReference.ownedMonitorsAndFrames()
 * @bug 6701700
 * @summary MonitorInfo objects aren't invalidated when the owning thread is resumed
 * @author Swamy Venkataramanappa
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g MonitorFrameInfo.java
 * @run driver MonitorFrameInfo
 */
import java.util.List;

import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.MonitorInfo;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;

    /********** target program **********/

class MonitorFrameInfoTarg {
    static void foo3() {
        System.out.println("executing foo3");

    }
    static void foo2() {
        Object l1 = new Object();
        synchronized(l1) {
            System.out.println("executing foo2 " + l1);
            foo3();
        }
    }
    static void foo1() {
        foo2();
    }
    public static void main(String[] args){
        System.out.println("Howdy!");
        Object l1 = new Object();
        synchronized(l1) {
            System.out.println("executing main" + l1);
            foo1();
        }
    }
}

    /********** test program **********/

public class MonitorFrameInfo extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    List<MonitorInfo> monitors;

    static int expectedCount = 2;
    static int[] expectedDepth = { 1, 3 };

    static String[] expectedNames = {"foo3", "foo2", "foo1", "main"};

    MonitorFrameInfo (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new MonitorFrameInfo(args).startTests();
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("MonitorFrameInfoTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();

        int initialSize = mainThread.frames().size();

        resumeTo("MonitorFrameInfoTarg", "foo3", "()V");

        if (!mainThread.frame(0).location().method().name()
                        .equals("foo3")) {
            failure("FAILED: frame failed");
        }

        if (mainThread.frames().size() != (initialSize + 3)) {
            failure("FAILED: frames size failed");
        }

        if (mainThread.frames().size() != mainThread.frameCount()) {
            failure("FAILED: frames size not equal to frameCount");
        }

        /* Test monitor frame info.
         */
        if (vm().canGetMonitorFrameInfo()) {
            System.out.println("Get monitors");
            monitors = mainThread.ownedMonitorsAndFrames();
            if (monitors.size() != expectedCount) {
                failure("monitors count is not equal to expected count");
            }
            MonitorInfo mon = null;
            for (int j=0; j < monitors.size(); j++) {
                mon  = (MonitorInfo)monitors.get(j);
                System.out.println("Monitor obj " + mon.monitor() + "depth =" +mon.stackDepth());
                if (mon.stackDepth() != expectedDepth[j]) {
                    failure("FAILED: monitor stack depth is not equal to expected depth");
                }
            }

            // The last gotten monInfo is in mon.   When we resume the thread,
            // it should become invalid.  We will step out of the top frame
            // so that the frame depth in this mon object will no longer be correct.
            // That is why the monInfo's have to become invalid when the thread is
            // resumed.
            stepOut(mainThread);
            boolean ok = false;
            try {
                System.out.println("*** Saved Monitor obj " + mon.monitor() + "depth =" +mon.stackDepth());
            } catch(InvalidStackFrameException ee) {
                // ok
                ok = true;
                System.out.println("Got expected InvalidStackFrameException after a resume");
            }
            if (!ok) {
                failure("FAILED: MonitorInfo object was not invalidated by a resume");
            }
        } else {
            System.out.println("can not get monitors frame info");
        }


        /*
         * resume until end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("MonitorFrameInfo: passed");
        } else {
            throw new Exception("MonitorFrameInfo: failed");
        }
    }
}
