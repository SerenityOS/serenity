/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ThreadReference._bounds_;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 * Test checks up ThreadReference methods for the following cases:  <br>
 *      - <code>getValue(null)</code>                               <br>
 *      - <code>getValues(null)</code>                              <br>
 *      - <code>getValues(list with size = 0)</code>                <br>
 *      - <code>setValue(null, null)</code>                         <br>
 *      - <code>setValue(field, null)</code>                        <br>
 *      - <code>visibleVariableByName(null)</code>                  <br>
 *      - <code>visibleVariableByName("")</code>                    <br>
 * <code>NullPointerException</code> is expected for every test case
 * except for the three last.
 */

public class bounds001 {

    private final static String prefix = "nsk.jdi.ThreadReference._bounds_.";
    private final static String debuggerName = prefix + "bounds001";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;

    private int[] frameParam = {-1, Integer.MAX_VALUE, -1, -1};

    private static void display(String msg) {
        log.display(msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        bounds001 thisTest = new bounds001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        thisTest.execTest();
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        debugee.VM().suspend();
        ThreadReference thread = debugee.threadByName("main");

        display("\nTEST BEGINS");
        display("===========");

        display("stop(null)");
        try {
            thread.stop(null);
            complain("InvalidTypeException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch(InvalidTypeException e) {
            display("!!!expected InvalidTypeException");
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("popFrames(null)");
        try {
            thread.popFrames(null);
            complain("NullPointerException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        int frameCount = 0;
        try {
            frameCount = thread.frameCount();
        } catch (IncompatibleThreadStateException e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        frameParam[2] = frameCount;
        frameParam[3] = frameParam[2] + 1;
        display("frame count: " + frameParam[2]);
        display("---------------");

        for (int i = 0; i < frameParam.length; i++) {
            display("frame(" + frameParam[i] + ")");
            try {
                thread.frame(frameParam[i]);
                if (frameParam[i] < 0 || frameParam[i] >= frameCount) {
                    complain("IndexOutOfBoundsException is not thrown");
                    exitStatus = Consts.TEST_FAILED;
                } else {
                    display("OK");
                }
            } catch(IndexOutOfBoundsException e) {
                if (frameParam[i] < 0 || frameParam[i] >= frameCount) {
                    display("!!!expected IndexOutOfBoundsException");
                } else {
                    complain("Unexpected " + e);
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch(Exception e) {
                complain("Unexpected " + e);
                exitStatus = Consts.TEST_FAILED;
            }
            display("");
        }

        for (int i = 0; i < frameParam.length; i++) {
            for (int j = 0; j < frameParam.length; j++) {
                display("frames(" + frameParam[i] + ", " + frameParam[j] + ")");
                try {
                    thread.frames(frameParam[i], frameParam[j]);
                    if (frameParam[i] < 0 ||
                            frameParam[j] < 0 ||
                            frameParam[i] >= frameCount ||
                            frameParam[i] + frameParam[j] > frameCount) {
                        complain("IndexOutOfBoundsException is not thrown");
                        exitStatus = Consts.TEST_FAILED;
                    } else {
                        display("OK");
                    }
                } catch(IndexOutOfBoundsException e) {
                    if (frameParam[i] < 0 ||
                            frameParam[j] < 0 ||
                            frameParam[i] >= frameCount ||
                            frameParam[i] + frameParam[j] > frameCount) {
                        display("!!!expected IndexOutOfBoundsException");
                    } else {
                        complain("Unexpected " + e);
                        exitStatus = Consts.TEST_FAILED;
                    }
                } catch(Exception e) {
                    complain("Unexpected " + e);
                    exitStatus = Consts.TEST_FAILED;
                }
                display("");
            }
        }
        display("");

        display("=============");
        display("TEST FINISHES\n");

        debugee.resume();
        debugee.quit();
    }
}
