/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4467564
 * @summary Test the popping of frames in an asynchronous context
 *          (that is, when suspended by the debugger at random points)
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g PopAsynchronousTest.java
 * @run driver PopAsynchronousTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class PopAsynchronousTarg {
    static final int N = 30;
    int fibonacci(int n) {
        if (n <= 2) {
            return 1;
        } else {
            return fibonacci(n-1) + fibonacci(n-2);
        }
    }
    void report(int n, int result) {
        System.out.println("fibonacci(" + n + ") = " + result);
    }
    public static void main(String[] args){
        int n = N;
        System.out.println("Howdy!");
        PopAsynchronousTarg pat = new PopAsynchronousTarg();
        pat.report(n, pat.fibonacci(n));
        System.out.println("Goodbye from PopAsynchronousTarg!");
    }
}

    /********** test program **********/

public class PopAsynchronousTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    int result = -1;
    boolean harassTarget = true;
    Object harassLock = new Object();

    PopAsynchronousTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new PopAsynchronousTest(args).startTests();
    }



    /********** event handlers **********/


    public void breakpointReached(BreakpointEvent event) {
        harassTarget = false;
        synchronized(harassLock) {
            try {
                StackFrame frame = event.thread().frame(0);
                LocalVariable lv = frame.visibleVariableByName("result");
                IntegerValue resultV = (IntegerValue)frame.getValue(lv);
                result = resultV.value();
            } catch (Exception exc) {
                exc.printStackTrace(System.err);
                failure("TEST FAILURE: exception " + exc);
            }
        }
    }

    /********** test assist **********/


    class HarassThread extends Thread {
        public void run() {
            int harassCount = 0;
            try {
                int prev = 0;
                int delayTime = 1;

                synchronized(harassLock) {
                    while (harassTarget && (harassCount < 10)) {
                        boolean backoff = true;
                        mainThread.suspend();
                        StackFrame top = mainThread.frame(0);
                        Method meth = top.location().method();
                        String methName = meth.name();
                        if (methName.equals("fibonacci")) {
                            LocalVariable lv = top.visibleVariableByName("n");
                            IntegerValue nV = (IntegerValue)top.getValue(lv);
                            int n = nV.value();
                            if (n != prev) {
                                backoff = false;
                                StackFrame popThis = top;
                                Iterator it = mainThread.frames().iterator();

                                /* pop lowest fibonacci frame */
                                while (it.hasNext()) {
                                    StackFrame frame = (StackFrame)it.next();
                                    if (!frame.location().method().name().equals("fibonacci")) {
                                        break;
                                    }
                                    popThis = frame;
                                }
                                println("popping fibonacci(" + n + ")");
                                mainThread.popFrames(popThis);
                                ++harassCount;
                                prev = n;
                            } else {
                                println("ignoring another fibonacci(" + n + ")");
                            }
                        } else {
                            println("ignoring " + methName);
                        }
                        if (backoff) {
                            delayTime *= 2;
                        } else {
                            delayTime /= 2;
                            if (delayTime < harassCount) {
                                delayTime = harassCount;
                            }
                        }
                        mainThread.resume();
                        println("Delaying for " + delayTime + "ms");
                        Thread.sleep(delayTime);
                    }
                }
            } catch (Exception exc) {
                exc.printStackTrace(System.err);
                failure("TEST FAILURE: exception " + exc);
            }
            println("Harassment complete, count = " + harassCount);
        }
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("PopAsynchronousTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();

        /*
         * Set event requests
         */
        List meths = targetClass.methodsByName("report");
        Location loc = ((Method)(meths.get(0))).location();
        BreakpointRequest request = erm.createBreakpointRequest(loc);
        request.enable();

        /*
         * start popping wildly away
         */
        (new HarassThread()).start();

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * check result
         */
        int correct = (new PopAsynchronousTarg()).
            fibonacci(PopAsynchronousTarg.N);
        if (result == correct) {
            println("Got expected result: " + result);
        } else {
            failure("FAIL: expected result: " + correct +
                    ", got: " + result);
        }

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("PopAsynchronousTest: passed");
        } else {
            throw new Exception("PopAsynchronousTest: failed");
        }
    }
}
