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

package nsk.jdi.VirtualMachine.redefineClasses;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 * The test checks up <code>com.sun.jdi.VirtualMachine.redefineClasses()</code>.
 * The test stops in <code>justMethod</code>, does a redefineClasses which deletes
 * a line in that method and then tries to get <code>StackFrame.thisObject()</code>.
 * When <code>canRedefineClasses()</code> is <code>false</code>, the test is
 * considered as passed and completes it's execution.
 */

public class redefineclasses032 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";

    private final static String prefix = "nsk.jdi.VirtualMachine.redefineClasses.";
    private final static String debuggerName = prefix + "redefineclasses032";
    private final static String debugeeName = debuggerName + "a";
    private final static String Name = debuggerName + "b";

    public final static String PERFORM_SGL = "perform";

    private final static String newClassFile = "newclass" + File.separator
                    + prefix.replace('.',File.separatorChar)
                    + "redefineclasses032a.class";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String testDir;

    private ClassType debugeeClass;

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

        redefineclasses032 thisTest = new redefineclasses032();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        testDir = argv[0];
        waitTime = argHandler.getWaitTime() * 60000;

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        try {
            thisTest.execTest();
        } catch (Failure e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            debugee.resume();
            debugee.quit();
        }
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws Failure {

        if (!debugee.VM().canRedefineClasses()) {
            display("\n>>>canRedefineClasses() is false<<< test canceled.\n");
            return;
        }

        debugeeClass = (ClassType)debugee.classByName(debugeeName);

        display("\nTEST BEGINS");
        display("===========");

        debugee.VM().suspend();

        display("Tested class\t:" + debugeeClass.name());

        ThreadReference thrd = debugee.threadByName("main");

        BreakpointRequest brkp = debugee.setBreakpoint(debugeeClass,
                                            redefineclasses032a.brkpMethodName,
                                            redefineclasses032a.brkpLineNumber);
        debugee.resume();
        debugee.sendSignal(PERFORM_SGL);

        waitBreakpointEvent(brkp, waitTime);

        display("\nredefining...");
        redefineDebugee();

        StackFrame stackFrame = null;
        ObjectReference obj = null;
        display("getting current frane...");
        try {
            stackFrame = thrd.frame(0);
        } catch (Exception e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }

        display("getting stackFrame.thisObject()...");
        try {
            obj = stackFrame.thisObject();
        } catch (Exception e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }

        if (obj != null) {
            exitStatus = Consts.TEST_FAILED;
            complain("stackFrame.thisObject() is not null for static method");
        }

        debugee.resume();

        display("\n=============");
        display("TEST FINISHES\n");
    }

    private void redefineDebugee() {
        Map<? extends com.sun.jdi.ReferenceType,byte[]> mapBytes;
        boolean alreadyComplained = false;
        mapBytes = mapClassToBytes(testDir + File.separator + newClassFile);
        try {
            debugee.VM().redefineClasses(mapBytes);
        } catch (Exception e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
    }

    private Map<? extends com.sun.jdi.ReferenceType,byte[]> mapClassToBytes(String fileName) {
        display("class-file\t:" + fileName);
        File fileToBeRedefined = new File(fileName);
        int fileToRedefineLength = (int )fileToBeRedefined.length();
        byte[] arrayToRedefine = new byte[fileToRedefineLength];

        FileInputStream inputFile;
        try {
            inputFile = new FileInputStream(fileToBeRedefined);
        } catch (FileNotFoundException e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }

        try {
            inputFile.read(arrayToRedefine);
            inputFile.close();
        } catch (IOException e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
        HashMap<com.sun.jdi.ReferenceType,byte[]> mapForClass = new HashMap<com.sun.jdi.ReferenceType,byte[]>();
        mapForClass.put(debugeeClass, arrayToRedefine);
        return mapForClass;
    }

    private BreakpointEvent waitBreakpointEvent(BreakpointRequest brkp, long waitTime) {
        Event event;
        try {
            event = debugee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
        if (!(event instanceof BreakpointEvent )) {
            throw new Failure("BreakpointEvent didn't arrive");
        }

        return (BreakpointEvent )event;
    }
}
