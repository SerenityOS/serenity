/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Utils;
import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 * The test against the method <code>com.sun.jdi.VirtualMachine.redefineClasses()</code>
 * and checks up the following assertion:                                       <br>
 *   "If <code>canUnrestrictedlyRedefineClasses()</code> is <code>false</code>,
 *    attempting to change method modifiers will throw
 *    <code>UnsupportedOperationException</code> exception"
 *
 * The test consists of the following files:                                    <br>
 *     <code>redefineclasses008.java</code>             - debugger              <br>
 *     <code>redefineclasses008a.java</code>            - initial debuggee      <br>
 *     <code>newclassXX/redefineclasses008a.java</code> - redefining debuggees  <br>
 *
 * Initial debugee-class has three dummy methods with different
 * kind of accessibility: public, protected and private (see COMMENTS into readme-file)
 * This test performs the following cases:                                <br>
 *    1.newclass01 - adding <static> modifiers to public method           <br>
 *    2.newclass02 - adding <static> modifiers to 3 methods               <br>
 *    3.newclass03 - adding <final> modifiers to public method            <br>
 *    4.newclass04 - adding <final> modifiers to 3 methods                <br>
 *    5.newclass05 - adding <synchronized> modifiers to public method     <br>
 *    6.newclass06 - adding <synchronized> modifiers to 3 methods         <br>
 * The test checks two different cases for suspended debugee and not
 * suspended one.
 * When <code>canRedefineClasses()</code> is <code>false</code>, the test is
 * considered as passed and completes it's execution.
 */

public class redefineclasses008 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";
    public final static String EXPECTED_STRING = "!!!Expected exception ";

    private final static String prefix = "nsk.jdi.VirtualMachine.redefineClasses.";
    private final static String debuggerName = prefix + "redefineclasses008";
    private final static String debugeeName = debuggerName + "a";

    private final static String[][] subDirs = {
            {"newclass01","adding <static> modifiers to public method"},
            {"newclass02","adding <static> modifiers to 3 methods"},
            {"newclass03","adding <final> modifiers to public method"},
            {"newclass04","adding <final> modifiers to 3 methods"},
            {"newclass05","adding <synchronized> modifiers to public method"},
            {"newclass06","adding <synchronized> modifiers to 3 methods"},
    };

    private final static String newClassFile = File.separator
                    + prefix.replace('.',File.separatorChar)
                    + "redefineclasses008a.class";
    private final static String oldClassFile
            = ClassFileFinder.findClassFile(debugeeName, Utils.TEST_CLASS_PATH)
                             .toString();
    private static final String dashes = "--------------------------";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String testDir;
    private static String statDebugee;

    private ClassType testedClass;

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

        redefineclasses008 thisTest = new redefineclasses008();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        testDir = argv[0];
        waitTime = argHandler.getWaitTime() * 60000;

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        try {
            thisTest.execTest();
        } catch (TestBug e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            debugee.resume();
            debugee.quit();
        }
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws TestBug {

        if (!debugee.VM().canRedefineClasses()) {
            debugee.sendSignal("continue");
            display("\n>>>canRedefineClasses() is false<<< test canceled.\n");
            return;
        }

        debugee.VM().suspend();
        testedClass = (ClassType)debugee.classByName(debugeeName);
        display("Tested class\t:" + testedClass.name());
        BreakpointRequest brkp = debugee.setBreakpoint(testedClass,
                                                    redefineclasses008a.brkpMethodName,
                                                    redefineclasses008a.brkpLineNumber);
        debugee.resume();
        debugee.sendSignal("continue");

        waitBreakpointEvent(brkp, waitTime);

        display("\nTEST BEGINS");
        display("===========");

        ThreadReference thrd = debugee.threadByName("main");
        if (thrd.isSuspended()) {
            statDebugee = "Debugee is suspended";
            display("\n\n<<<" + statDebugee + ">>>");
            display(dashes);
        }
        performCases();

        debugee.resume();

        thrd = debugee.threadByName("main");
        if (!thrd.isSuspended()) {
            statDebugee = "Debugee is not suspended";
            display("\n\n<<<" + statDebugee + ">>>");
            display("------------------------------");
        }
        performCases();

        display("\n=============");
        display("TEST FINISHES\n");
    }

    private void performCases() {
        Map<? extends com.sun.jdi.ReferenceType,byte[]> mapBytes;
        boolean alreadyComplained = false;
        for (int i = 0; i < subDirs.length; i++) {
            display("\n" + subDirs[i][1] + ">>>");
            mapBytes = mapClassToBytes(testDir + File.separator + subDirs [i][0] + newClassFile);
            try {
                debugee.VM().redefineClasses(mapBytes);
                if (!debugee.VM().canUnrestrictedlyRedefineClasses()) {
                    exitStatus = Consts.TEST_FAILED;
                    if (!alreadyComplained) {
                        alreadyComplained = true;
                        complain("***" + dashes);
                        complain("***" + statDebugee);
                        complain("***" + dashes);
                    }
                    complain("***UnsupportedOperationException is not thrown");
                    complain("***\twhile " + subDirs[i][1]);

                    display(">return to the previous state...");
                    mapBytes = mapClassToBytes(oldClassFile);
                    try {
                        debugee.VM().redefineClasses(mapBytes);
                    } catch (Exception e) {
                        throw new TestBug(UNEXPECTED_STRING + e);
                    }
                }
            } catch (UnsupportedOperationException e) {
                if (!debugee.VM().canUnrestrictedlyRedefineClasses()) {
                    display(EXPECTED_STRING + e);
                } else {
                    exitStatus = Consts.TEST_FAILED;
                    if (!alreadyComplained) {
                        alreadyComplained = true;
                        complain("***" + dashes);
                        complain("***" + statDebugee);
                        complain("***" + dashes);
                    }
                    complain(statDebugee + UNEXPECTED_STRING
                                + "UnsupportedOperationException");
                    complain("***\twhile " + subDirs[i][1]);
                }
            } catch (Exception e) {
                throw new TestBug(UNEXPECTED_STRING + e);
            }
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
            throw new TestBug(UNEXPECTED_STRING + e);
        }

        try {
            inputFile.read(arrayToRedefine);
            inputFile.close();
        } catch (IOException e) {
            throw new TestBug(UNEXPECTED_STRING + e);
        }
        HashMap<com.sun.jdi.ReferenceType,byte[]> mapForClass = new HashMap<com.sun.jdi.ReferenceType,byte[]>();
        mapForClass.put(testedClass, arrayToRedefine);
        return mapForClass;
    }

    private BreakpointEvent waitBreakpointEvent(BreakpointRequest brkp, long waitTime) {
        Event event;
        display(">waiting breakpoint event...");
        try {
            event = debugee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new TestBug(UNEXPECTED_STRING + e);
        }
        if (!(event instanceof BreakpointEvent )) {
            throw new TestBug("BreakpointEvent didn't arrive");
        }

        return (BreakpointEvent )event;
    }
}
