/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4272800 4274208 4392010
 * @summary Test debugger operations in finalize() methods
 * @author Gordon Hirsch  (modified for HotSpot by tbell & rfield)
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g FinalizerTest.java
 *
 * @run driver FinalizerTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


/*
 * Debuggee which exercises a finalize() method. There's no guarantee
 * that this will work, but we need some way of attempting to test
 * the debugging of finalizers.
 * @author Gordon Hirsch  (modified for HotSpot by tbell & rfield)
 */
class FinalizerTarg {
    static String lockit = "lock";
    static boolean finalizerRun = false;
    static class BigObject {
        String name;
        byte[] foo = new byte[300000];

        public BigObject (String _name) {
            super();
            this.name = _name;
        }

        protected void finalize() throws Throwable {
            /*
             * JLS 2nd Ed. section 12.6 "Finalization of Class Instances" "[...]
             * invoke the finalize method for its superclass, [...] usually good
             * practice [...]"
             */
            super.finalize();
            //Thread.dumpStack();
            finalizerRun = true;
        }
    }

    static void waitForAFinalizer() {
        String s = Integer.toString(1);
        BigObject b = new BigObject (s);
        b = null; // Drop the object, creating garbage...
        System.gc();
        System.runFinalization();

        // Now, we have to make sure the finalizer
        // gets run.  We will keep allocating more
        // and more memory with the idea that eventually,
        // the memory occupied by the BigObject will get reclaimed
        // and the finalizer will be run.
        List holdAlot = new ArrayList();
        for (int chunk=10000000; chunk > 10000; chunk = chunk / 2) {
            if (finalizerRun) {
                return;
            }
            try {
                while(true) {
                    holdAlot.add(new byte[chunk]);
                    System.err.println("Allocated " + chunk);
                }
            }
            catch ( Throwable thrown ) {  // OutOfMemoryError
                System.gc();
            }
            System.runFinalization();
        }
        return;  // not reached
    }

    public static void main(String[] args) throws Exception {
        /*
         * Spin in waitForAFinalizer() while waiting for
         * another thread to run the finalizer on one of the
         * BigObjects ...
         */
        waitForAFinalizer();
    }
}
///// End of debuggee


public class FinalizerTest extends TestScaffold {

    public static void main(String args[])
        throws Exception {
        new FinalizerTest (args).startTests();
    }

    public FinalizerTest (String args[]) {
        super(args);
    }

    protected void runTests() throws Exception {
        try {
            BreakpointEvent event0 = startToMain("FinalizerTarg");

            BreakpointEvent event1 = resumeTo("FinalizerTarg$BigObject",
                                              "finalize", "()V");

            println("Breakpoint at " +
                    event1.location().method().name() + ":" +
                    event1.location().lineNumber() + " (" +
                    event1.location().codeIndex() + ")");

            /*
             * Record information about the current location
             */
            List frames = event1.thread().frames();
            List methodStack = new ArrayList(frames.size());
            Iterator iter = frames.iterator();
            while (iter.hasNext()) {
                StackFrame frame = (StackFrame) iter.next();
                methodStack.add(frame.location().declaringType().name() +
                                "." + frame.location().method().name());
            }
            println("Try a stepOverLine()...");
            StepEvent stepEvent = stepOverLine(event1.thread());

            println("Step Complete at " +
                               stepEvent.location().method().name() + ":" +
                               stepEvent.location().lineNumber() + " (" +
                               stepEvent.location().codeIndex() + ")");

            /*
             * Compare current location with recorded location
             */
            if (stepEvent.thread().frameCount() != methodStack.size()) {
                throw new Exception("Stack depths do not match: original=" +
                                    methodStack.size() +
                                    ", current=" +
                                    stepEvent.thread().frameCount());
            }
            iter = stepEvent.thread().frames().iterator();
            Iterator iter2 = methodStack.iterator();
            while (iter.hasNext()) {
                StackFrame frame = (StackFrame) iter.next();
                String name = (String) iter2.next();
                String currentName = frame.location().declaringType().name() +
                "." + frame.location().method().name();
                if (!name.equals(currentName)) {
                    throw new Exception("Stacks do not match at: original=" +
                                         name + ", current=" + currentName);

                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            testFailed = true;
        } finally {
            // Allow application to complete and shut down
            listenUntilVMDisconnect();
        }
        if (!testFailed) {
            println("FinalizerTest: passed");
        } else {
            throw new Exception("FinalizerTest: failed");
        }
    }
}
