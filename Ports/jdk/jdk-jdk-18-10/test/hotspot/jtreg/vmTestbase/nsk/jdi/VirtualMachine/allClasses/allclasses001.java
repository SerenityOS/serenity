/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.VirtualMachine.allClasses;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * VirtualMachine.                                              <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.VirtualMachine.allClasses()</code>         <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test check up that new class and interface types <BR>
 * are in returned List only after corresponding objects<BR>
 * have been created within the debuggee.               <BR>
 * The sequence of the cases for testing is as follows: <BR>
 * <BR>
 *      Objects# == 0, Classes# == 0, Interfaces# == 0  <BR>
 * hence, the returned List must contain no mirrors of  <BR>
 * tested reference types;                              <BR>
 *      Objects# == 1, Classes# == 1, Interfaces# == 0  <BR>
 * the returned list must contain only one expected     <BR>
 * object mirroring a tested Class type;                <BR>
 *      Objects# == 2, Classes# == 2, Interfaces# == 1  <BR>
 * the returned list must contain three expected objects<BR>
 * mirroring two Class and one Interface types;         <BR>
 *      Objects# == 3, Classes# == 2, Interfaces# == 1, <BR>
 * the returned list must contain three expected objects<BR>
 * mirroring two Class and one Interface types;         <BR>
 * repetirion of previous check after one more object   <BR>
 * has been created;                                    <BR>
 *      Objects# == 4, Classes# == 3, Interfaces# == 1, <BR>
 * the returned list must contain four expected objects <BR>
 * mirroring three Class and one Interface types;       <BR>
 * <BR>
 */

public class allclasses001 extends JDIBase {

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new allclasses001().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return exitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.VirtualMachine.allClasses.allclasses001a";

    private String namePrefix =
        "nsk.jdi.VirtualMachine.allClasses.";

    //====================================================== test program

    static final int returnCode0 = 0;
    static final int returnCode1 = 1;

    private int runThis (String argv[], PrintStream out) {

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        waitTime        = argsHandler.getWaitTime() * 60000;

        try {
            log2("launching a debuggee :");
            log2("       " + debuggeeName);
            if (argsHandler.verbose()) {
                debuggee = binder.bindToDebugee(debuggeeName + " -vbs");
            } else {
                debuggee = binder.bindToDebugee(debuggeeName);
            }
            if (debuggee == null) {
                log3("ERROR: no debuggee launched");
                return FAILED;
            }
            log2("debuggee launched");
        } catch ( Exception e ) {
            log3("ERROR: Exception : " + e);
            log2("       test cancelled");
            return FAILED;
        }

        debuggee.redirectOutput(logHandler);

        vm = debuggee.VM();

        eventQueue = vm.eventQueue();
        if (eventQueue == null) {
            log3("ERROR: eventQueue == null : TEST ABORTED");
            vm.exit(PASS_BASE);
            return FAILED;
        }

        log2("invocation of the method runTest()");
        switch (runTest()) {

            case 0 :  log2("test phase has finished normally");
                      log2("   waiting for the debuggee to finish ...");
                      debuggee.waitFor();

                      log2("......getting the debuggee's exit status");
                      int status = debuggee.getStatus();
                      if (status != PASS_BASE) {
                          log3("ERROR: debuggee returned UNEXPECTED exit status: " +
                              status + " != PASS_BASE");
                          testExitCode = FAILED;
                      } else {
                          log2("......debuggee returned expected exit status: " +
                              status + " == PASS_BASE");
                      }
                      break;

            default : log3("ERROR: runTest() returned unexpected value");

            case 1 :  log3("test phase has not finished normally: debuggee is still alive");
                      log2("......forcing: vm.exit();");
                      testExitCode = FAILED;
                      try {
                          vm.exit(PASS_BASE);
                      } catch ( Exception e ) {
                          log3("ERROR: Exception : e");
                      }
                      break;

            case 2 :  log3("test cancelled due to VMDisconnectedException");
                      log2("......trying: vm.process().destroy();");
                      testExitCode = FAILED;
                      try {
                          Process vmProcess = vm.process();
                          if (vmProcess != null) {
                              vmProcess.destroy();
                          }
                      } catch ( Exception e ) {
                          log3("ERROR: Exception : e");
                      }
                      break;
            }

        return testExitCode;
    }


   /*
    * Return value: 0 - normal end of the test
    *               1 - ubnormal end of the test
    *               2 - VMDisconnectedException while test phase
    */

    private int runTest() {

        try {
            testRun();

            log2("waiting for VMDeathEvent");
            getEventSet();
            if (eventIterator.nextEvent() instanceof VMDeathEvent)
                return 0;

            log3("ERROR: last event is not the VMDeathEvent");
            return 1;
        } catch ( VMDisconnectedException e ) {
            log3("ERROR: VMDisconnectedException : " + e);
            return 2;
        } catch ( Exception e ) {
            log3("ERROR: Exception : " + e);
            return 1;
        }

    }

    private void testRun()
                 throws JDITestRuntimeException, Exception {

        eventRManager = vm.eventRequestManager();

        ClassPrepareRequest cpRequest = eventRManager.createClassPrepareRequest();
        cpRequest.setSuspendPolicy( EventRequest.SUSPEND_EVENT_THREAD);
        cpRequest.addClassFilter(debuggeeName);

        cpRequest.enable();
        vm.resume();
        getEventSet();
        cpRequest.disable();

        ClassPrepareEvent event = (ClassPrepareEvent) eventIterator.next();
        debuggeeClass = event.referenceType();

        if (!debuggeeClass.name().equals(debuggeeName))
           throw new JDITestRuntimeException("** Unexpected ClassName for ClassPrepareEvent **");

        log2("      received: ClassPrepareEvent for debuggeeClass");

        String bPointMethod = "methodForCommunication";
        String lineForComm  = "lineForComm";
        BreakpointRequest bpRequest;

        try {
            bpRequest = settingBreakpoint(debuggee.threadByNameOrThrow("main"),
                                          debuggeeClass,
                                          bPointMethod, lineForComm, "zero");
        } catch ( Exception e ) {
            throw e;
        }
        bpRequest.enable();

    //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        for (int i = 0; ; i++) {

            vm.resume();
            breakpointForCommunication();

            int instruction = ((IntegerValue) (
                               debuggeeClass.getValue(
                               debuggeeClass.fieldByName("instruction")))).value();

            if (instruction == 0) {
                vm.resume();
                break;
            }

            log1("  new check: # " + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            int expresult = returnCode0;

            String name[] = { "Class1ForCheck",
                              "Class2ForCheck",
                              "Class3ForCheck",
                              "InterfaceForCheck" };


            switch (i) {

                case 0:
                        log2("...... check case: Objects# ==0, Classes# ==0, Interfaces# ==0,");
                        if (refTypeYes(name[0]) != 0) {
                            log3("ERROR: Class in the List: " + name[0]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[1]) != 0) {
                            log3("ERROR: Class in the List: " + name[1]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[2]) != 0) {
                            log3("ERROR: Class in the List: " + name[2]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[3]) != 0) {
                            log3("ERROR: Interface in the List: " + name[3]);
                            expresult = returnCode1;
                        }
                        break;



                case 1:
                        log2("...... check case: Objects# ==1, Classes# ==1, Interfaces# ==0,");
                        if (refTypeYes(name[0]) == 0) {
                            log3("ERROR: Class not in the List: " + name[0]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[1]) != 0) {
                            log3("ERROR: Class in the List: " + name[1]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[2]) != 0) {
                            log3("ERROR: Class in the List: " + name[2]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[3]) != 0) {
                            log3("ERROR: Interface in the List: " + name[3]);
                            expresult = returnCode1;
                        }
                        break;


                case 2:
                        log2("...... check case: Objects# ==2, Classes# ==2, Interfaces# ==1,");
                        if (refTypeYes(name[0]) == 0) {
                            log3("ERROR: Class not in the List: " + name[0]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[1]) == 0) {
                            log3("ERROR: Class not in the List: " + name[1]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[2]) != 0) {
                            log3("ERROR: Class in the List: " + name[2]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[3]) == 0) {
                            log3("ERROR: Interface not in the List: " + name[3]);
                            expresult = returnCode1;
                        }
                        break;


                case 3:
                        log2("...... check case: Objects# ==3, Classes# ==2, Interfaces# ==1,");
                        if (refTypeYes(name[0]) == 0) {
                            log3("ERROR: Class not in the List: " + name[0]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[1]) == 0) {
                            log3("ERROR: Class not in the List: " + name[1]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[2]) != 0) {
                            log3("ERROR: Class in the List: " + name[2]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[3]) == 0) {
                            log3("ERROR: Interface not in the List: " + name[3]);
                            expresult = returnCode1;
                        }
                        break;


                case 4:
                        log2("...... check case: Objects# ==4, Classes# ==3, Interfaces# ==1,");
                        if (refTypeYes(name[0]) == 0) {
                            log3("ERROR: Class not in the List: " + name[0]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[1]) == 0) {
                            log3("ERROR: Class not in the List: " + name[1]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[2]) == 0) {
                            log3("ERROR: Class not in the List: " + name[2]);
                            expresult = returnCode1;
                        }
                        if (refTypeYes(name[3]) == 0) {
                            log3("ERROR: Interface not in the List: " + name[3]);
                            expresult = returnCode1;
                        }
                        break;


                default:
                        break ;
            }

            if (expresult != returnCode0)
                testExitCode = FAILED;
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods

    private int refTypeYes ( String name ) {

        List vmClasses = vm.allClasses();
        ListIterator li = vmClasses.listIterator();

        for ( int i = 0; li.hasNext(); i++) {
            ReferenceType obj = (ReferenceType) li.next();
            if ( obj.name().equals(namePrefix + name) )
                return 1;
        }
        return 0;
    }

}
