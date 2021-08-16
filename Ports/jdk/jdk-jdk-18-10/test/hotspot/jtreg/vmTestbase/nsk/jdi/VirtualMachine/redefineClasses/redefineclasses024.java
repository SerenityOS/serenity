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
 *   "If <code>canUnrestrictedlyRedefineClasses()</code> is <code>false</code> attempting
 *    to change a a class hierarchy will throw <code>UnsupportedOperationException</code>
 *    exception."
 *
 * The test consists of the following files:                                <br>
 *    redefineclasses024.java             - debugger                        <br>
 *    redefineclasses024a.java            - debuggee                        <br>
 *    redefineclasses024c.java            - a subsidiary class              <br>
 *    redefineclasses024i.java            - a subsidiary interface          <br>
 *    redefineclasses024b.java            - an initial redefined class      <br>
 *    newclassXX/redefineclasses024b.java - redefining debuggee's class     <br>
 *
 * This test performs the following cases:                              <br>
 *    1. newclass01 - implements an interface                           <br>
 *    2. newclass02 - extends a class                                   <br>
 *    3. newclass03 - extends a class, implements an interface          <br>
 *    4. newclass04 - extends <code>Object</code> class                 <br>
 * The last case assumes the exception will not be thrown.
 * The test checks two different cases for suspended debugee and not
 * suspended one.
 * When <code>canRedefineClasses()</code> is <code>false</code>, the test is
 * considered as passed and completes it's execution.
 */

public class redefineclasses024 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";
    public final static String EXPECTED_STRING = "!!!Expected exception ";

    private final static String prefix = "nsk.jdi.VirtualMachine.redefineClasses.";
    private final static String debuggerName = prefix + "redefineclasses024";
    private final static String debugeeName = debuggerName + "a";
    public  final static String testedClassName = debuggerName + "b";

    private final static String[][] subDirs = {
            {"newclass01","implements an interface"},
            {"newclass02","extends a class"},
            {"newclass03","extends a class, implements an interface"},
            {"newclass04","extends Object"}
    };

    private final static String newClassFile = File.separator
                    + testedClassName.replace('.',File.separatorChar)
                    + ".class";
    private final static String oldClassFile
            = ClassFileFinder.findClassFile(testedClassName, Utils.TEST_CLASS_PATH)
                             .toString();
    private static final String dashes = "--------------------------";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static String testDir;
    private static String statDebugee;

    private ReferenceType testedClass;

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

        redefineclasses024 thisTest = new redefineclasses024();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        testDir = argv[0];

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);
        waitTime = argHandler.getWaitTime() * 60000;

        try {
            thisTest.execTest();
        } catch (TestBug e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } catch (Exception e) {
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
            display("\n>>>canRedefineClasses() is false<<< test canceled.\n");
            return;
        }

        ClassPrepareRequest clpr = debugee.getEventRequestManager().createClassPrepareRequest();
        clpr.addClassFilter(testedClassName);
        clpr.enable();

        debugee.sendSignal("load_class");

        ClassPrepareEvent clpEvent = waitClassPrepareEvent(clpr, waitTime);

        testedClass = debugee.classByName(testedClassName);
        display(typePrompt(testedClass) + "\t:" + testedClass.name());

        display("\nTEST BEGINS");
        display("===========");

        displayVMStatus();
        performCases();

        debugee.resume();

        displayVMStatus();
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
                if ( i < 3 ) {
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
                } else {
                        display("REDEFINED");
                }
            } catch (UnsupportedOperationException e) {
                if ( i < 3 ) {
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

    private ClassPrepareEvent waitClassPrepareEvent(ClassPrepareRequest clpr, long waitTime) {
        Event event;
        display(">waiting class prepare event...");
        try {
            event = debugee.waitingEvent(clpr, waitTime);
        } catch (InterruptedException e) {
            throw new TestBug(UNEXPECTED_STRING + e);
        }
        if (!(event instanceof ClassPrepareEvent )) {
            throw new TestBug("ClassPrepareEvent didn't arrive");
        }

        return (ClassPrepareEvent )event;
    }

    private String typePrompt(ReferenceType refType) {
        String msg = "Tested ";
        if (refType instanceof InterfaceType) {
            msg += "interface";
        } else if (refType instanceof ClassType) {
            msg += "class";
        } else {
            msg += "type";
        }
        return msg;
    }

    void displayVMStatus() {
        ThreadReference thrd = debugee.threadByName("main");
        if (thrd.isSuspended()) {
            statDebugee = "Debugee is suspended";
            display("\n\n<<<" + statDebugee + ">>>");
            display(dashes);
        } else {
            statDebugee = "Debugee is not suspended";
            display("\n\n<<<" + statDebugee + ">>>");
            display("------------------------------");
        }
    }
}
