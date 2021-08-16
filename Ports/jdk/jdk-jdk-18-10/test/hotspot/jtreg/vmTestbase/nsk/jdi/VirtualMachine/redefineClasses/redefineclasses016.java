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
 * and checks up the following assertion:       <br>
 *  "If <code>canUnrestrictedlyRedefineClasses()</code> is <code>false</code>,
 *  changing the schema(the fields) will throw <code>UnsupportedOperationException</code>
 *  exception"
 * On debugee part there is an interface that has three method declarations:<br>
 *       <code>static public final Object field001</code> <br>
 *       <code>       public final Object field002</code> <br>
 *       <code>              final Object field003</code> <br>
 *       <code>                    Object field004</code> <br>
 * JLSS 9.3 says "...every field declaration in the body of an interface is implicitly
 * <code>public</code>, <code>static</code> and <code>final</code>..."
 * The test tries to redefine this interface, manipulating modifiers of
 * these fields, and expects UnsupportedOperationException will not be thrown.
 *
 * The test consists of the following files:                                    <br>
 *    <code>redefineclasses016.java</code>             - debugger               <br>
 *    <code>redefineclasses016a.java</code>            - initial debuggee       <br>
 *    <code>newclassXX/redefineclasses016a.java</code> - redefining debuggees   <br>
 *
 * This test performs the following cases:                  <br>
 *    1. newclass01 - removing <code>static</code> modifiers           <br>
 *    2. newclass02 - adding <code>static</code> modifiers             <br>
 *    3. newclass03 - removing <code>final</code> modifiers            <br>
 *    4. newclass04 - adding <code>final</code> modifiers              <br>
 *    5. newclass05 - removing <code>public</code> modifiers           <br>
 *    6. newclass06 - adding <code>public</code> modifiers             <br>
 * The test checks two different cases for suspended debugee and not
 * suspended one.
 *
 * When <code>canRedefineClasses()</code> is <code>false</code>, the test is
 * considered as passed and completes it's execution.
 */

public class redefineclasses016 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";
    public final static String EXPECTED_STRING = "!!!Expected exception ";

    private final static String prefix = "nsk.jdi.VirtualMachine.redefineClasses.";
    private final static String debuggerName = prefix + "redefineclasses016";
    private final static String debugeeName = debuggerName + "a";
    private final static String testedClassName = debuggerName + "b";

    private final static String[][] subDirs = {
            {"newclass01","removing <static> modifiers"},
            {"newclass02","adding <static> modifiers"},
            {"newclass03","removing <final> modifiers"},
            {"newclass04","adding <final> modifiers"},
            {"newclass05","removing <public> modifiers"},
            {"newclass06","adding <public> modifiers"}
   };

    private final static String newClassFile = File.separator
                    + prefix.replace('.',File.separatorChar)
                    + "redefineclasses016b.class";

    private static final String dashes = "--------------------------";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static String testDir;
    private static String statDebugee;

    private ReferenceType debugeeClass, testedClass;

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

        redefineclasses016 thisTest = new redefineclasses016();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        testDir = argv[0];

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

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

        debugee.VM().suspend();
        debugeeClass = debugee.classByName(debugeeName);
        testedClass = debugee.classByName(testedClassName);
        display("Tested class\t:" + testedClass.name());

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
                display("REDEFINED");
            } catch (UnsupportedOperationException e) {
                exitStatus = Consts.TEST_FAILED;
                if (!alreadyComplained) {
                    alreadyComplained = true;
                    complain("***" + dashes);
                    complain("***" + statDebugee);
                    complain("***" + dashes);
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
}
