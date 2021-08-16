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
 *   "If a "verifier" detects that a class, though well formed, contains an internal
 *    inconsistency or security problem, <code>VerifyError</code> will be thrown."
 *
 * The test consists of the following files:                                    <br>
 *     <code>redefineclasses030.java</code>             - debugger              <br>
 *     <code>redefineclasses030a.java</code>            - debuggee              <br>
 *     <code>redefineclasses030b.java</code>            - an initial redefined class  <br>
 *     <code>newclassXX/redefineclasses030b.jasm</code> - redefining debuggee's class <br>
 *
 * This test performs the following cases:                          <br>
 *    1. newclass01 - reducing stack size                           <br>
 *    2. newclass02 - pushing a local variable with wrong index     <br>
 * The test checks two different cases for suspended debugee and not
 * suspended one.
 * When <code>canRedefineClasses()</code> is <code>false</code>, the test is
 * considered as passed and completes it's execution.
 */

public class redefineclasses030 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";
    public final static String EXPECTED_STRING = "!!!Expected exception ";

    private final static String prefix = "nsk.jdi.VirtualMachine.redefineClasses.";
    private final static String debuggerName = prefix + "redefineclasses030";
    private final static String debugeeName = debuggerName + "a";
    public  final static String testedClassName = debuggerName + "b";

    private final static String[][] subDirs = {
            {"newclass01","reducing stack size"},
            {"newclass02","pushing a local variable with wrong index"}
    };

    private final static String classFileName =
                    testedClassName.replace('.',File.separatorChar)
                    + ".class";
    private final static String oldClassFile
            = ClassFileFinder.findClassFile(testedClassName, Utils.TEST_CLASS_PATH)
                             .toString();

    private static final String dashes = "--------------------------";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
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

        redefineclasses030 thisTest = new redefineclasses030();

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

        testedClass = debugee.classByName(testedClassName);
        display(typePrompt(testedClass) + "\t:" + testedClass.name());

        display("\nTEST BEGINS");
        display("===========");

        debugee.VM().suspend();

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
            mapBytes = mapClassToBytes(testDir + File.separator + subDirs [i][0] + File.separator + classFileName);
            try {
                debugee.VM().redefineClasses(mapBytes);
                exitStatus = Consts.TEST_FAILED;
                if (!alreadyComplained) {
                    alreadyComplained = true;
                    complain("***" + dashes);
                    complain("***" + statDebugee);
                    complain("***" + dashes);
                }
                complain("***VerifyError is not thrown");
                complain("***\twhile " + subDirs[i][1]);

                display(">return to the previous state...");
                mapBytes = mapClassToBytes(oldClassFile);
                try {
                    debugee.VM().redefineClasses(mapBytes);
                } catch (Exception e) {
                    throw new TestBug(UNEXPECTED_STRING + e);
                }
            } catch (VerifyError e) {
                display(EXPECTED_STRING + e);
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

    public static String toHexString(long b, int length) {
        return Right(Long.toHexString(b), length).replace(' ', '0').toUpperCase();
    }

    private static String Right(String source, int length) {

        if (length <= 0)
            return "";

        if (length <= source.length())
            return source.substring(source.length() - length, source.length());
        else
            return PadL(source, length);
    }

    private static String PadL(String source, int length, String what) {

        if (length <= 0)
            return "";

        if (source.length() > length)
            return PadL("", length, "*");

        while (source.length() < length)
            source = what + source;

        return source;
    }


    private static String PadL(String source, int length) {
        return PadL(source, length, " ");
    }


}
