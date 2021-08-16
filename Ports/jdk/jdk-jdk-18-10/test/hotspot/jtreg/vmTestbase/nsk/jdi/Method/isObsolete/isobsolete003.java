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
package nsk.jdi.Method.isObsolete;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import java.io.*;
import java.util.*;

/**
 */
public class isobsolete003 {

    private final static String prefix = "nsk.jdi.Method.isObsolete";
    private final static String className = ".isobsolete003";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";
    private final static int brkpMainLineNumber = 48;
    private final static int brkpFooLineNumber = 33;

    private static int waitTime;
    private static int exitStatus;
    private static ArgumentHandler     argHandler;
    private static Log                 log;
    private static Debugee             debuggee;
    private static VirtualMachine      vm;
    private static ReferenceType       debuggeeClass;

    private static EventRequestManager eventRManager;
    private static EventSet            eventSet;
    private static EventIterator       eventIterator;

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        try {

            Binder binder = new Binder(argHandler, log);
            debuggee = binder.bindToDebugee(debuggeeName);
            debuggee.redirectStderr(log, "debuggee > ");
            debuggee.createIOPipe();
            eventRManager = debuggee.getEventRequestManager();

            vm = debuggee.VM();
            eventRManager = vm.eventRequestManager();

            waitForDebuggeeClassPrepared();

            if (vm.canRedefineClasses()) {

                execTest();

                debuggee.resume();
                getEventSet();
                if (eventIterator.nextEvent() instanceof VMDeathEvent) {
                    display("Waiting for the debuggee's finish...");
                    debuggee.waitFor();

                    display("Getting the debuggee's exit status.");
                    int status = debuggee.getStatus();
                    if (status != (Consts.TEST_PASSED + Consts.JCK_STATUS_BASE)) {
                        complain("Debuggee returned UNEXPECTED exit status: " + status);
                        exitStatus = Consts.TEST_FAILED;
                    }
                } else {
                    throw new TestBug("Last event is not the VMDeathEvent");
                }

            } else {
                display("vm.canRedefineClasses() == false : test is cancelled");
                vm.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
            }

        } catch (VMDisconnectedException e) {
            exitStatus = Consts.TEST_FAILED;
            complain("The test cancelled due to VMDisconnectedException.");
            e.printStackTrace(out);
            display("Trying: vm.process().destroy();");
            if (vm != null) {
                Process vmProcess = vm.process();
                if (vmProcess != null) {
                    vmProcess.destroy();
                }
            }

        } catch (Exception e) {
            exitStatus = Consts.TEST_FAILED;
            complain("Unexpected Exception: " + e.getMessage());
            e.printStackTrace(out);
            complain("The test has not finished normally. Forcing: vm.exit().");
            if (vm != null) {
                vm.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
            }
            debuggee.resume();
            getEventSet();
        }

        return exitStatus;
    }


    private static void execTest() {

        ThreadReference mainThread = debuggee.threadByName("main");

        // Set first breakpoint to have isobsolete003b class loaded.
        BreakpointRequest bpRequest = debuggee.makeBreakpoint(debuggeeClass, "main", brkpMainLineNumber);
        bpRequest.addThreadFilter(mainThread);
        bpRequest.addCountFilter(1);
        bpRequest.enable();

        waitForEvent(bpRequest);
        bpRequest.disable();

        // At this point isobsolete003b class should be loaded in debuggee.
        String redefName = prefix + ".isobsolete003b";

        ReferenceType redefClass = debuggee.classByName(redefName);
        if (redefClass == null) {
            throw new TestBug(redefName + "is not found in debuggee.");
        }

        String methodName = "foo";
        Method method = (Method) redefClass.methodsByName(methodName).get(0);

        // save some values for check in future
        Method oldMethod = method;
        Location oldLocation = method.location();
        long oldCodeIndex = oldLocation.codeIndex();
        String oldRetTypeName = method.returnTypeName();
        int oldHashCode = method.hashCode();

        // Set new breakpoint to have isobsolete003b.foo() method on stack before redefinition.
        bpRequest = debuggee.makeBreakpoint(redefClass, methodName, brkpFooLineNumber);
        bpRequest.addThreadFilter(mainThread);
        bpRequest.addCountFilter(1);
        bpRequest.enable();

        waitForEvent(bpRequest);
        bpRequest.disable();

        display("requested BreakpointEvent for foo() method received;");
        try {
            if (!mainThread.frame(0).location().method().equals(method)) {
                throw new TestBug("foo() method is not on the top of the main thread stack");
            }
        } catch (IncompatibleThreadStateException e) {
            throw new Failure("Unexpected IncompatibleThreadStateException while comparing mainThread.frame(0).location().method(): " + e.getMessage());
        }

        display("Making redefineClasses(mapClassToBytes()).");
        vm.redefineClasses(mapClassToBytes());

        // Check isObsolete after redefinition
        try {
            method = mainThread.frame(0).location().method();
        } catch (IncompatibleThreadStateException e) {
            throw new Failure("Unexpected IncompatibleThreadStateException while getting mainThread.frame(0).location().method(): " + e.getMessage());
        }
        if (!method.isObsolete()) {
            complain("method.isObsolete() == true for foo() method after redefineClasses()");
            exitStatus = Consts.TEST_FAILED;
        } else {
            // Do other checks for obsolete method.

            if (method.equals(oldMethod)) {
                complain("equals(oldMethod) returned true for obsolete method.");
                exitStatus = Consts.TEST_FAILED;
            }

            List l = null;
            Location loc = null;
            try {
                l = method.allLineLocations();
                if (l.size() > 0) {
                    complain("allLineLocations() returned a list with non-zero size for obsolete method." +
                        "Number of Locations :" + l.size());
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (AbsentInformationException e) {
                // it is expected
            }

            try {
                l = method.allLineLocations(vm.getDefaultStratum(), null);
                if (l.size() > 0) {
                    complain("allLineLocations(vm.getDefaultStratum(), null) returned a list with non-zero size for obsolete method." +
                        "Number of Locations :" + l.size());
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (AbsentInformationException e) {
                // it is expected
            }

            try {
                l = method.locationsOfLine(1);
                if (l.size() > 0) {
                    complain("locationsOfLine(1) returned a list with non-zero size for obsolete method." +
                        "Number of Locations :" + l.size());
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (AbsentInformationException e) {
                // it is expected
            }

            try {
                l = method.locationsOfLine(vm.getDefaultStratum(), null, 1);
                if (l.size() > 0) {
                    complain("locationsOfLine(vm.getDefaultStratum(), null, 1) returned a list with non-zero size for obsolete method." +
                        "Number of Locations :" + l.size());
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (AbsentInformationException e) {
                // it is expected
            }

            try {
                l = method.arguments();
                if (l.size() > 0) {
                    complain("arguments() returned a list with non-zero size for obsolete method." +
                        "Size of list  :" + l.size());
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (AbsentInformationException e) {
                // it is expected
            }

            l = method.argumentTypeNames();
            if (l.size() > 0) {
                complain("argumentTypeNames() returned a list with non-zero size for obsolete method." +
                    "Size of list :" + l.size());
                exitStatus = Consts.TEST_FAILED;
            }

            try {
                l = method.argumentTypes();
                if (l.size() > 0) {
                    complain("argumentsTypes() returned a list with non-zero size for obsolete method." +
                        "Size of list  :" + l.size());
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (ClassNotLoadedException e) {
                // it is expected
            }

            try {
                l = method.variables();
                if (l.size() > 0) {
                    complain("variables() returned a list with non-zero size for obsolete method." +
                        "Size of list  :" + l.size());
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (AbsentInformationException e) {
                // it is expected
            }

            try {
                l = method.variablesByName("dummyInt");
                if (l.size() > 0) {
                    complain("variablesByName(oldVar.name()) returned a list with non-zero size for obsolete method." +
                        "Size of list  :" + l.size());
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (AbsentInformationException e) {
                // it is expected
            }

            byte[] b = method.bytecodes();
            if (b.length > 0) {
                complain("bytecodes() returned an array with non-zero length for obsolete method." +
                    "Number of bytes :" + b.length);
                exitStatus = Consts.TEST_FAILED;
            }

            loc = method.location();
            if (loc != null && loc == oldLocation) {
                complain("location() returned old location for obsolete method.");
                exitStatus = Consts.TEST_FAILED;
            }

            loc = method.locationOfCodeIndex(oldCodeIndex);
            if (loc != null) {
                complain("locationOfCodeIndex(oldCodeIndex) returned not-null location for obsolete method.");
                exitStatus = Consts.TEST_FAILED;
            }

            String rtName = method.returnTypeName();
            if (rtName.equals(oldRetTypeName)) {
                complain("returnTypeName() returned an old string for obsolete method: " + rtName);
                exitStatus = Consts.TEST_FAILED;
            }

            try {
                Type rType = method.returnType();
                if (rType != null) {
                    complain("returnType() returned not-null Type for obsolete method: " + rType.name());
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (ClassNotLoadedException e) {
                // it is expected
            }

            int hashCode = method.hashCode();
            if (hashCode == oldHashCode) {
                complain("hashCode() returned old value for obsolete method: " + hashCode);
                exitStatus = Consts.TEST_FAILED;
            }
        }
    }

    private static void display(String msg) {
        log.display("debugger > " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }

    /**
     * Returns Map object for redefinition.
     *
     */
    private static Map<? extends com.sun.jdi.ReferenceType,byte[]> mapClassToBytes() {
        String[] args = argHandler.getArguments();
        if (args.length <= 0) {
            throw new Failure("mapClassToBytes(): Test arguments are not found.");
        }

        String testDir = args[0];
        display("Test current dir = " + testDir);

        String filePrefix = File.separator + "nsk"
                          + File.separator + "jdi"
                          + File.separator + "Method"
                          + File.separator + "isObsolete";

        String fileToRedefineName    = testDir +
                                       File.separator + "newclass" + filePrefix
                                       + File.separator + "isobsolete003b.class";

        display("fileToRedefineName : " + fileToRedefineName);

        byte[] arrayToRedefine;
        try {
            File fileToRedefine    = new File(fileToRedefineName);
            if (!fileToRedefine.exists()) {
                throw new Failure("mapClassToBytes(): fileToRedefine does not exist");
            }

            FileInputStream inputFile = new FileInputStream(fileToRedefine);
            arrayToRedefine = new byte [(int) fileToRedefine.length()];
            inputFile.read(arrayToRedefine);
            inputFile.close();

        } catch (IOException e) {
            complain("unexpected IOException: " + e);
            throw new Failure(e);
        }

        String testClassName = prefix + ".isobsolete003b";
        ReferenceType testClass = debuggee.classByName(testClassName);

        HashMap<com.sun.jdi.ReferenceType,byte[]> mapForClass = new HashMap<com.sun.jdi.ReferenceType,byte[]>();
        mapForClass.put(testClass, arrayToRedefine);

        return mapForClass;
    }

    private static Event waitForEvent (EventRequest evRequest) {

        vm.resume();
        Event resultEvent = null;
        try {
            eventSet = null;
            eventIterator = null;
            eventSet = vm.eventQueue().remove(waitTime);
            if (eventSet == null) {
                throw new Failure("TIMEOUT while waiting for an event");
            }
            eventIterator = eventSet.eventIterator();
            while (eventIterator.hasNext()) {
                 Event curEvent = eventIterator.nextEvent();
                 if (curEvent instanceof VMDisconnectEvent) {
                     throw new Failure("Unexpected VMDisconnectEvent received.");
                 } else if (curEvent.request().equals(evRequest)) {
                     display("Requested event received.");
                     resultEvent = curEvent;
                     break;
                 } else {
                     throw new TestBug("Unexpected Event received: " + curEvent.toString());
                 }
            }
        } catch (Exception e) {
            throw new Failure("Unexpected exception while waiting for an event: " + e);
        }
        return resultEvent;
    }

    private static void getEventSet() {
        try {
            eventSet = vm.eventQueue().remove(waitTime);
            if (eventSet == null) {
                throw new Failure("TIMEOUT while waiting for an event");
            }
            eventIterator = eventSet.eventIterator();
        } catch (Exception e) {
            throw new Failure("getEventSet(): Unexpected exception while waiting for an event: " + e);
        }
    }

    private static void waitForDebuggeeClassPrepared () {
        display("Creating request for ClassPrepareEvent for debuggee.");
        ClassPrepareRequest cpRequest = eventRManager.createClassPrepareRequest();
        cpRequest.addClassFilter(debuggeeName);
        cpRequest.addCountFilter(1);
        cpRequest.enable();

        ClassPrepareEvent event = (ClassPrepareEvent) waitForEvent(cpRequest);
        cpRequest.disable();

        debuggeeClass = event.referenceType();
        if (!debuggeeClass.name().equals(debuggeeName))
           throw new Failure("Unexpected class name for ClassPrepareEvent : " + debuggeeClass.name());
    }
}
