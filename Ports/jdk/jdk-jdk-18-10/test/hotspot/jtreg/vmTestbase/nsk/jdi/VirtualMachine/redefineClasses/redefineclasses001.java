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

package nsk.jdi.VirtualMachine.redefineClasses;

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
 * <code>com.sun.jdi.VirtualMachine.redefineClasses()</code>    <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test checks up on the following assertion:               <BR>
 *    If any redefined methods have active stack frames,        <BR>
 *    those active frames continue to run the bytecodes of      <BR>
 *    the previous method.                                      <BR>
 *    The redefined methods will be used on new invokes.        <BR>
 * <BR>
 * The test has three phases and works as follows.              <BR>
 * <BR>
 * In first phase,                                              <BR>
 * upon launching debuggee's VM which will be suspended,                <BR>
 * a debugger waits for the VMStartEvent within a predefined            <BR>
 * time interval. If no the VMStartEvent received, the test is FAILED.  <BR>
 * Upon getting the VMStartEvent, it makes the request for debuggee's   <BR>
 * ClassPrepareEvent with SUSPEND_EVENT_THREAD, resumes the VM,         <BR>
 * and waits for the event within the predefined time interval.         <BR>
 * If no the ClassPrepareEvent received, the test is FAILED.            <BR>
 * Upon getting the ClassPrepareEvent,                                  <BR>
 * the debugger sets up the breakpoint with SUSPEND_EVENT_THREAD        <BR>
 * within debuggee's special methodForCommunication().                  <BR>
 * <BR>
 * In second phase to check the assetion,                               <BR>
 * the debugger and the debuggee perform the following.                 <BR>
 * - The debugger resumes the debuggee and waits for the BreakpointEvent.<BR>
 * - The debuggee creates new tested class object, objRF1, and invokes  <BR>
 *   the methodForCommunication to be suspended and                     <BR>
 *   to inform the debugger with the event.                             <BR>
 * - Upon getting the breakpointForCommunication Event,                 <BR>
 *   the debugger sets up the breakpoint bpRequest2 in the tested method<BR>
 *   redefineclasses001b.m2(), resumes the debuggee.                    <BR>
 * - The debuggee invokes objRF1.m1() method to be suspended            <BR>
 *   at the breakpoint in the method m2().                              <BR>
 * - Upon getting the breakpointInMethod Event,                         <BR>
 *   the debugger prepares mapClassToByte, invokes                      <BR>
 *         VirtualMachine.redefineClasses(mapClassToByte)               <BR>
 *   and resumes the debuggee.                                          <BR>
 * - The debuggee invokes the methodForCommunication to be suspended    <BR>
 *   once again and to inform the debugger with the event.              <BR>
 * - Upon getting seconf breakpointForCommunication Event,              <BR>
 *   the debugger sets up another breakpoint in the redefined method m2()<BR>
 *   and resumes the debuggee.                                          <BR>
 * - The debuggee invokes the method m1() to invoke m2() second time,   <BR>
 *   wher it will be suspended once again.                              <BR>
 * - Upon getting second breakpointInMethod Event,                      <BR>
 *   the debugger checks up the value of the class variable "i1"        <BR>
 *   which should be equal to the expected value.                       <BR>
 * <BR>
 * In third phase when at the end,                                      <BR>
 * the debuggee changes the value of the "instruction"                  <BR>
 * to inform the debugger of checks finished, and both end.             <BR>
 * <BR>
 */

public class redefineclasses001 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new redefineclasses001().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.VirtualMachine.redefineClasses.redefineclasses001a";

    //====================================================== test program

    private int runThis (String argv[], PrintStream out) {

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        waitTime        = argsHandler.getWaitTime() * 60000;

        try {
            log2("launching a debuggee :");
            log2("       " + debuggeeName);

            debuggee = binder.bindToDebugee(debuggeeName);

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


        if ( !vm.canRedefineClasses() ) {
            log2("......vm.canRedefineClasses() == false : test is cancelled");
            vm.resume();
            return;
        }


        String bPointMethod = "methodForCommunication";
        String lineForComm  = "lineForComm";
        BreakpointRequest bpRequest;

        bpRequest = settingBreakpoint(debuggee.threadByNameOrThrow("main"),
                                      debuggeeClass,
                                      bPointMethod, lineForComm, "zero");
        bpRequest.enable();

    //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        String bpClassName  = "nsk.jdi.VirtualMachine.redefineClasses.redefineclasses001b";
        String bpMethodName = "m2";
        String bpLineName   = "bpline";
        String varName      = "i1";
        int    varValue     = 2;

        BreakpointRequest bpRequest2 = null;
        BreakpointRequest bpRequest3 = null;
        ReferenceType     bpClass    = null;


        for (int i = 0; ; i++) {

            vm.resume();
            breakpointForCommunication();

            int instruction = ((IntegerValue)
                               (debuggeeClass.getValue(debuggeeClass.fieldByName("instruction")))).value();

            if (instruction == 0) {
                vm.resume();
                break;
            }

            log1(":::::: case: # " + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            switch (i) {
              case 0:
                  List          classes = vm.classesByName(bpClassName);
                  bpClass = (ReferenceType) classes.get(0);
                  bpRequest2 = settingBreakpoint(debuggee.threadByNameOrThrow("main"),
                                          bpClass,
                                          bpMethodName, bpLineName, "one");
                  bpRequest2.enable();

                  vm.resume();
                  breakpointInMethod("one");
                  bpRequest2.disable();

                  try {
                      log2("......vm.redefineClasses(mapClassToBytes());");
                      vm.redefineClasses(mapClassToBytes());
                  } catch ( Exception e ) {
                      log3("ERROR:  Exception: " + e);
                      testExitCode = FAILED;
                      throw e;
                  } catch ( Error e ) {
                      log3("ERROR:  Error: " + e);
                      testExitCode = FAILED;
                      throw e;
                  }
                  break;

              case 1:

                  bpRequest3 = settingBreakpoint(debuggee.threadByNameOrThrow("main"),
                                          bpClass,
                                          bpMethodName, bpLineName, "one");

                  bpRequest3.enable();

                  vm.resume();
                  breakpointInMethod("one");
                  bpRequest3.disable();

                  int i1 = ( (IntegerValue)
                      bpClass.getValue(bpClass.fieldByName(varName) ) ).value();

                  if (i1 != varValue) {
                      testExitCode = FAILED;
                      log3("ERROR: value of i1 != 2 (expected) but : " + i1);
                  }
                  break;

              default:
                  throw new JDITestRuntimeException ("** default case **");
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
// this is for debugging
// testExitCode = FAILED;

        vm.resume();
        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods

    private void breakpointInMethod(String number)
                 throws JDITestRuntimeException {

        Event event;

        getEventSet();
        event = eventIterator.nextEvent();

        if ( !(event instanceof BreakpointEvent) )
            throw new JDITestRuntimeException("** event IS NOT a breakpoint **");

        log2("---->: request().getProperty == " +
             event.request().getProperty("number"));

        if ( event.request().getProperty("number").equals(number) )
            return;

        throw new JDITestRuntimeException("** UNEXPECTED breakpoint **");
    }


    private Map<? extends com.sun.jdi.ReferenceType,byte[]> mapClassToBytes()
                throws JDITestRuntimeException {

        byte []arrayToRedefine;

//   -----------------------------
        log2("......getting: String[] args = argsHandler.getArguments();");
        String[] args = argsHandler.getArguments();
        if (args.length <= 0) {
            log3("ERROR: args.length <= 0");
            testExitCode = FAILED;
            throw new JDITestRuntimeException("** args.length <= 0 **");
        }
        String testDir = args[0];
        log2("...... testDir = " + testDir);


        String filePrefix = File.separator + "nsk"
                          + File.separator + "jdi"
                          + File.separator + "VirtualMachine"
                          + File.separator + "redefineClasses";

        String fileToRedefineName    = testDir
                                       + File.separator + "newclass"
                                       + filePrefix
                                       + File.separator + "redefineclasses001b.class";

        log2("...... fileToRedefineName = " + fileToRedefineName);
//   -----------------------------

        try {
            log2("......File fileToRedefine = new File(fileToRedefineName);");
            File fileToRedefine    = new File(fileToRedefineName);
            if (fileToRedefine.exists())
                log2("       fileToRedefine.exists()");
            else {
                log3("ERROR: fileToRedefine doesn't exist");
                testExitCode = FAILED;
            }

            if (testExitCode == FAILED)
                throw new JDITestRuntimeException("** testExitCode == FAILED **");

            int fileToRedefineLength = (int) fileToRedefine.length();
            log2("       fileToRedefineLength == " + fileToRedefineLength);

            FileInputStream inputFile = new FileInputStream(fileToRedefine);
            arrayToRedefine = new byte[fileToRedefineLength];
            inputFile.read(arrayToRedefine);
            inputFile.close();
        } catch ( IOException e ) {
            log3("ERROR: IOException: " + e);
            throw new JDITestRuntimeException("** IOException **");
        }

        String testClassName = "nsk.jdi.VirtualMachine.redefineClasses.redefineclasses001b";
        List classes = vm.classesByName(testClassName);
        ReferenceType testClass = (ReferenceType) classes.get(0);

        HashMap<com.sun.jdi.ReferenceType,byte[]> mapForClass = new HashMap<com.sun.jdi.ReferenceType,byte[]>();
        mapForClass.put(testClass, arrayToRedefine);

        return mapForClass;
    }

}
