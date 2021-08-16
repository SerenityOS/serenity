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

package nsk.jdi.ClassObjectReference.toString;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;

/**
 * The debugger application of the test.
 */
public class tostring001 {

    //------------------------------------------------------- immutable common fields

    final static String SIGNAL_READY = "ready";
    final static String SIGNAL_GO    = "go";
    final static String SIGNAL_QUIT  = "quit";

    private static int waitTime;
    private static int exitStatus;
    private static ArgumentHandler     argHandler;
    private static Log                 log;
    private static Debugee             debuggee;
    private static ReferenceType       debuggeeClass;

    //------------------------------------------------------- mutable common fields

    private final static String prefix = "nsk.jdi.ClassObjectReference.toString.";
    private final static String className = "tostring001";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    /** debuggee's methods for check **/
    private final static String checkedClasses[] = {
        "java.lang.Boolean"  ,
        "java.lang.Byte"     ,
        "java.lang.Character",
        "java.lang.Double"   ,
        "java.lang.Float"    ,
        "java.lang.Integer"  ,
        "java.lang.Long"     ,
        "java.lang.Short"    ,
        "java.lang.String"   ,
        "java.lang.Object"   ,

        debuggeeName + "$innerClass",
        debuggeeName + "$innerInterf",
        prefix + "tostring001aClass",
        prefix + "tostring001aInterf"
                                                   };

    //------------------------------------------------------- immutable common methods

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    private static void display(String msg) {
        log.display("debugger > " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        debuggee = Debugee.prepareDebugee(argHandler, log, debuggeeName);

        debuggeeClass = debuggee.classByName(debuggeeName);
        if ( debuggeeClass == null ) {
            complain("Class '" + debuggeeName + "' not found.");
            exitStatus = Consts.TEST_FAILED;
        }

        execTest();

        debuggee.quit();

        return exitStatus;
    }

    //------------------------------------------------------ mutable common method

    private static void execTest() {

        BreakpointRequest brkp = debuggee.setBreakpoint(debuggeeClass,
                                                    tostring001a.brkpMethodName,
                                                    tostring001a.brkpLineNumber);
        debuggee.resume();

        debuggee.sendSignal(SIGNAL_GO);
        Event event = null;

        // waiting the breakpoint event
        try {
            event = debuggee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new Failure("unexpected InterruptedException while waiting for Breakpoint event");
        }
        if (!(event instanceof BreakpointEvent)) {
            debuggee.resume();
            throw new Failure("BreakpointEvent didn't arrive");
        }

        ThreadReference thread = ((BreakpointEvent)event).thread();
        List params = new Vector();
        ClassType testedClass = (ClassType)debuggeeClass;

        display("Checking toString() method for ClassObjectReferences of debuggee's fields...");
        String brackets[] = {"", "[]", "[][]"};

        for (int i=0; i < checkedClasses.length; i++) {

            String basicName = checkedClasses[i];

            for (int dim = 0; dim<3; dim++) {

                String className = basicName + brackets[dim];
                ReferenceType refType = debuggee.classByName(className);
                if (refType == null) {
                    complain("Could NOT FIND class: " + className);
                    exitStatus = Consts.TEST_FAILED;
                    continue;
                }

                try {
                    ClassObjectReference classObjRef = refType.classObject();
                    String str = classObjRef.toString();
                    if (str == null) {
                        complain("toString() returns null for ClassObjectReferences of debugges's field: " + className);
                        exitStatus = Consts.TEST_FAILED;
                    } else if (str.length() == 0) {
                        complain("toString() returns empty string for ClassObjectReferences of debugges's field: " + className);
                        exitStatus = Consts.TEST_FAILED;
                    } else {
                        display("toString() method returns for " + className + " : " + str);
                    }
                } catch(Exception e) {
                    complain("unexpected " + e + " while taking ClassObjectReferences of debugges's field: " + className);
                    exitStatus = Consts.TEST_FAILED;
                }
            }
        }

        display("Checking completed!");
        debuggee.resume();
    }

    //--------------------------------------------------------- test specific methods

}
//--------------------------------------------------------- test specific classes
