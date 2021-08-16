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
 * The test checks up <code>com.sun.jdi.VirtualMachine.redefineClasses()</code>
 * in case when redefined class loaded by a custom loader.
 *
 * The test perform next steps:                                         <br>
 *     1. Debugee loads a tested class by the custom loader.            <br>
 *     2. When getting <code>ClassPrepareEvent</code>, debugger checks
 *        class loader of tested class to be the custom class loader.   <br>
 *     3. Debugger redefines the tested class and checks the redefinition
 *        to be successful.                                             <br>
 * The abose steps are performed for two cases, when the custom loader
 * extends <code>java.net.URLClassLoader</code> and when the custom loader
 * directly extends <code>java.lang.ClassLoader</code>.
 */

public class redefineclasses034 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";

    private final static String prefix = "nsk.jdi.VirtualMachine.redefineClasses.";
    private final static String debuggerName = prefix + "redefineclasses034";
    public final static String debugeeName = debuggerName + "a";
    public final static int TESTCASE_NUMBER = 2;
    public final static String [] testedClassName = {
                                    debuggerName + "b",
                                    debuggerName + "c"
    };
    private final static String [] clsLdrs = {
                debugeeName + "ClassLoaderB",
                debugeeName + "ClassLoaderC"
    };

    private static final String redefinedMethodName = "justMethod";

    private final static String PATH = "newclass" + File.separator
                    + prefix.replace('.',File.separatorChar);
    private final static String [] newClassFile = {
                PATH + "redefineclasses034b.class",
                PATH + "redefineclasses034c.class"
    };

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String classDir;

    private int expectedEventCount = 3;
    private int currentCase = -1;
    private ReferenceType testedClass, debugeeClass;

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

        redefineclasses034 thisTest = new redefineclasses034();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        classDir = argv[0];
        waitTime = argHandler.getWaitTime() * 60000;

        Binder binder = new Binder(argHandler, log);
        debugee = binder.bindToDebugee(debugeeName);
//        debugee.redirectStderr(log.getOutStream());

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
//        debugee.resume();

        EventSet eventSet = null;
        EventIterator eventIterator = null;
        Event event;
        long totalTime = waitTime;
        long tmp, begin = System.currentTimeMillis(),
             delta = 0;
        boolean exit = false;

        int eventCount = 0;
        EventRequestManager evm = debugee.getEventRequestManager();
        ClassPrepareRequest req = evm.createClassPrepareRequest();
        req.addClassFilter(debuggerName + "*");
        req.addClassExclusionFilter(debugeeName + "Class*");
        req.addClassExclusionFilter(debuggerName);
        req.enable();
        debugee.resume();

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
                    display(" event ===>>> " + event);

                    if (event instanceof ClassPrepareEvent) {
                        testedClass = ((ClassPrepareEvent )event).referenceType();// debugee.classByName(testedClassNameB);
                        String className = testedClass.name();
                        debugeeClass = debugee.classByName(debugeeName);
                        display(">>>>" + className);
                        if (className.compareTo(debugeeName) == 0) {
                            debugee.setBreakpoint(testedClass,
                                                    redefineclasses034a.brkpMethodName,
                                                    redefineclasses034a.brkpLineNumber);
                            display("");

                        } else if ( !checkCase(currentCase) ) {
                            complain("Unexpected event for \"" +  className
                                        + "\" class");
                            exitStatus = Consts.TEST_FAILED;
                        }

                        debugee.resume();
                        eventCount++;

                    } else if (event instanceof BreakpointEvent) {
                        currentCase++;
                        display("\ntestcase " + (currentCase + 1));
                        display("===========");
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

    private boolean checkCase(int index) {
        display("\nclasses info on debugger side:");
        display("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        display("testedClass\t:" + testedClassName[index]);

        String clsLdrName = testedClass.classLoader().referenceType().name();
        String clsLdrName4Debugee = debugeeClass.classLoader().referenceType().name();
        display("class loader\t:" + clsLdrName);
        display("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        display("debugeeClass\t:" + debugeeClass.name());
        display("class loader\t:" + clsLdrName4Debugee);
        display("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        if (clsLdrName.compareTo(clsLdrName4Debugee) == 0
                || clsLdrName.compareTo(clsLdrs[index]) != 0) {
            complain("wrong class loader \"" + clsLdrName
                        + "\"\n\tfor \"" + testedClassName[index] + "\"");
            exitStatus = Consts.TEST_FAILED;
        }

        List methods = testedClass.methodsByName(redefinedMethodName);
        Method mthd = (Method)methods.get(0);
        int varsBefore = localVars(mthd);

        display("\nredefining...");
        redefineDebugee(newClassFile[index]);

        methods = testedClass.methodsByName(redefinedMethodName);
        mthd = (Method)methods.get(0);
        int varsAfter = localVars(mthd);

        if (varsAfter == varsBefore) {
            complain("count of local variables is not changed!");
            exitStatus = Consts.TEST_FAILED;
        }
        display("");
        return exitStatus != Consts.TEST_FAILED;
    }

    private void redefineDebugee(String classFileName) {
        Map<? extends com.sun.jdi.ReferenceType,byte[]> mapBytes;
        boolean alreadyComplained = false;
        mapBytes = mapClassToBytes(classDir + File.separator + classFileName);
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
        mapForClass.put(testedClass, arrayToRedefine);
        return mapForClass;
    }

    private int localVars(Method mthd) {
        int res = 0;
        try {
            List locVars = mthd.variables();
            res = locVars.size();
            display("\ncount of local variables: " + res);

        } catch (AbsentInformationException e) {
            display("AbsentInformationException: no info");
        }
        return res;
    }
}
