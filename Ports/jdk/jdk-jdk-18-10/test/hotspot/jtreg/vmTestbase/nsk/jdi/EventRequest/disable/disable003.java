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

package nsk.jdi.EventRequest.disable;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 * The test checks up <code>com.sun.jdi.EventRequest.disable()</code> after
 * class file redefinition.
 *
 * The test performs as follow:
 * Debuggee has two classes. Debugger creates a <code>BreakpointRequest</code>
 * at the first class and waits a corresponding event. After getting
 * <code>BreakpointEvent</code>, debugger redefines the second class, tries to
 * disable the set breakpoint and checks status of the <code>BreakpointRequest</code>
 * to be disabled.
 */

public class disable003 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";

    private final static String prefix = "nsk.jdi.EventRequest.disable.";
    private final static String debuggerName = prefix + "disable003";
    public final static String debugeeName = debuggerName + "a";
    public final static String testedClassName = debuggerName + "b";

    private final static String classFileName = "disable003b.class";
    private final static String newClassFile = "newclass" + File.separator
                    + prefix.replace('.',File.separatorChar)
                    + classFileName;

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String classDir;

    public final static int expectedEventCount = 1;
    private ReferenceType debugeeClass;

    private static void display(String msg) {
        log.display(msg);
    }

    private static void complain(String msg) {
        log.complain(msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        disable003 thisTest = new disable003();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        classDir = argv[0];
        waitTime = argHandler.getWaitTime() * 60000;

        Binder binder = new Binder(argHandler, log);
        debugee = binder.bindToDebugee(debugeeName);
        debugee.redirectOutput(log);

        try {
            thisTest.execTest();
        } catch (Throwable e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            debugee.endDebugee();
        }
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws Failure {

        if (!debugee.VM().canRedefineClasses()) {
            display("\n>>>canRedefineClasses() is false<<< test canceled.\n");
            return;
        }

        display("\nTEST BEGINS");
        display("===========");

        EventSet eventSet = null;
        EventIterator eventIterator = null;
        Event event;
        long totalTime = waitTime;
        long tmp, begin = System.currentTimeMillis(),
             delta = 0;
        boolean exit = false;

        EventRequestManager evm = debugee.getEventRequestManager();
        ClassPrepareRequest req = evm.createClassPrepareRequest();
        req.addClassFilter(debugeeName);
        req.enable();
        debugee.resume();

        int eventCount = 0;
        while (totalTime > 0 && !exit) {
            if (eventIterator == null || !eventIterator.hasNext()) {
                try {
                    eventSet = debugee.VM().eventQueue().remove(totalTime);
                } catch (InterruptedException e) {
                    new Failure(e);
                }
                if (eventSet != null) {
                    eventIterator = eventSet.eventIterator();
                } else {
                    eventIterator = null;
                }
            }
            if (eventIterator != null) {
                while (eventIterator.hasNext()) {
                    event = eventIterator.nextEvent();
//                    display("\nevent ===>>> " + event);

                    if (event instanceof ClassPrepareEvent) {
                        display("\nevent ===>>> " + event);
                        prepareTestCases();
                        debugee.resume();

                    } else if (event instanceof BreakpointEvent) {
                        display("\nevent ===>>> " + event);
                        ClassType testedClass;
                        testedClass = (ClassType )debugee.classByName(testedClassName);

                        display("\nredefinition of the \""
                                    + testedClassName + "\" class");
                        redefineDebugee(testedClass, newClassFile);

                        eventCount++;
                        boolean isBrkpEnabled = event.request().isEnabled();
                        if (!isBrkpEnabled) {
                            complain("breakpoint was disabled after "
                                        + "the redefinition of an other class");
                            exitStatus = Consts.TEST_FAILED;
                        } else {
                            display("\nis breakpoint enabled?\t" + isBrkpEnabled);
                            display("\ndisabling the breakpoint request...");
                            event.request().disable();
                            if (event.request().isEnabled()) {
                                complain("breakpoint is not disabled");
                                exitStatus = Consts.TEST_FAILED;
                            }
                        }
                        debugee.resume();

                    } else if (event instanceof VMDeathEvent) {
                        exit = true;
                        break;
                    } else if (event instanceof VMDisconnectEvent) {
                        exit = true;
                        break;
                    } // if
                } // while
            } // if
            tmp = System.currentTimeMillis();
            delta = tmp - begin;
            totalTime -= delta;
                begin = tmp;
        }

        if (totalTime <= 0) {
            complain("out of wait time...");
            exitStatus = Consts.TEST_FAILED;
        }
        if (eventCount != expectedEventCount) {
            complain("expecting " + expectedEventCount
                        + " events, but "
                        + eventCount + " events arrived.");
            exitStatus = Consts.TEST_FAILED;
        }

        display("=============");
        display("TEST FINISHES\n");
    }

    private void redefineDebugee(ReferenceType refType, String classFileName) {
        Map<? extends com.sun.jdi.ReferenceType,byte[]> mapBytes;
        boolean alreadyComplained = false;
        mapBytes = mapClassToBytes(refType, classFileName);
        try {
            debugee.VM().redefineClasses(mapBytes);
        } catch (Exception e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
    }

    private Map<? extends com.sun.jdi.ReferenceType,byte[]> mapClassToBytes(ReferenceType refType, String fileName) {
        display("a new class-file\t:" + fileName);
        File fileToBeRedefined = new File(classDir + File.separator + fileName);
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
        mapForClass.put(refType, arrayToRedefine);
        return mapForClass;
    }

    private void prepareTestCases() {
        debugeeClass = debugee.classByName(debugeeName);
        display("debugeeClass\t\t:" + debugeeClass.name());
        display("setting breakpoint...");
        debugee.setBreakpoint(debugeeClass,
                                disable003a.brkpMethodName,
                                disable003a.brkpLineNumber);
    }
}
