/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jdi/StackFrame/getArgumentValues/getArgumentValues001.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that method 'StackFrame.getArgumentValues()' returns the values of all arguments in this frame.
 *         Test calls 'StackFrame.getArgumentValues()' for following methods:
 *                 - methods receiving as argument single argument of primitive type
 *                 - method receiving as argument Object
 *                 - method receiving as argument String
 *                 - method without arguments
 *                 - method receiving all primitive types and Object as arguments
 *                 - method receiving arrays of all primitive types and Object array as arguments
 *                 - method receiving multidimensional arrays of all primitive types and Object multidimensional array as arguments
 *                 - method with single arument, method changes argument several times
 *                 - method with several arguments, arguments are changed in loop many times
 *                 - static method receiving all primitive types and Object as arguments
 *                 - static method with single arument, method changes argument several times
 *                 - static method with several arguments, arguments are changed in loop many times
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StackFrame.getArgumentValues.getArgumentValues001.getArgumentValues001
 *        nsk.jdi.StackFrame.getArgumentValues.getArgumentValues001.getArgumentValues001a
 *
 * @comment make sure getArgumentValues001a is compiled with full debug info
 * @clean nsk.jdi.StackFrame.getArgumentValues.getArgumentValues001.getArgumentValues001a
 * @compile -g:lines,source,vars getArgumentValues001a.java
 *
 * @run main/othervm
 *      nsk.jdi.StackFrame.getArgumentValues.getArgumentValues001.getArgumentValues001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.StackFrame.getArgumentValues.getArgumentValues001;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.*;
import nsk.share.jdi.*;

/*
 * Test checks that method 'StackFrame.getArgumentValues()' returns the values of all arguments in this frame.
 *
 * Test checks method 'getArgumentValues()' using special class 'getArgumentValues001a.TestClass'. This class
 * contains methods with various arguments and contains static array 'testLocations' with information about
 * locations intended for breakpoints, before executing code at this locations debuggee saves values of current
 * method arguments at special static array 'TestClass.expectedArgumentValues'.
 * Debugger creates BreakpointRequests for test locations using infromation from 'TestClass.testLocations',
 * starts thread listening BreakpointEvents and forces debuggee to execute methods of 'getArgumentValues001a.TestClass'.
 * When BreakpointEvent is received debugger obtains StackFrame for current frame of thread which was stoped by breakpoint,
 * then debugger obtains expected argument values from static array 'TestClass.expectedArgumentValues' and compares
 * expected values with values returned by  method 'StackFrame.getArgumentValues()'.
 */
public class getArgumentValues001 extends TestDebuggerType2 {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new getArgumentValues001().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return getArgumentValues001a.class.getName();
    }

    private Value extractValue(Value value) {
        if (value == null)
            return value;

        String valueClassName = getArgumentValues001a.Value.class.getName();

        /* if value is instance of 'getArgumentValues001a.Value' extact primitive type value
         * (name of primitive type is stored in field 'name' and primitive type value is stored
         * in field with name "<TypeName>Value")
         */
        if (value.type().name().equals(valueClassName)) {
            ReferenceType valueClass = debuggee.classByName(valueClassName);

            Field typeNameField = valueClass.fieldByName("name");
            String typeName = ((StringReference)((ObjectReference)value).getValue(typeNameField)).value();

            Field valueField = valueClass.fieldByName(typeName + "Value");

            return ((ObjectReference)value).getValue(valueField);
        } else
            return value;
    }

    // print information about expected and actual argument values
    void printDebugInfo(List<Value> values, ArrayReference expectedValues) {
        log.display("Values:");
        int i = 0;
        for (Value value : values) {
            log.display("Value " + i + ": " + value);
            i++;
        }
        log.display("Expected values:");
        for (i = 0; i < expectedValues.length(); i++) {
            log.display("Value " + i + ": " + extractValue(expectedValues.getValue(i)));
        }
    }

    // information about BreakpointEvents generated by debuggee VM
    static class BreakpointData {

        public BreakpointData(BreakpointRequest request, int breakpointsNumber) {
            this.request = request;
            this.breakpointsNumber = breakpointsNumber;
        }

        // breakpoint request
        BreakpointRequest request;
        // how events are generated for request
        int breakpointsNumber;
    }

    private Location getLocation(ReferenceType referenceType, int lineNumber) throws AbsentInformationException {
        for (Location location : referenceType.allLineLocations()) {
            if (location.lineNumber() == lineNumber)
                return location;
        }

        throw new TestBug("Can't find location with lineNumber = " + lineNumber + " for class " + referenceType);
    }

    public void doTest() {
        List<BreakpointData> requests = new ArrayList<BreakpointData>();

        ReferenceType referenceType = debuggee.classByName(getArgumentValues001a.TestClass.class.getName());
        try {
            // array 'getArgumentValues001a.TestClass.testLocations' contains infromation about BreakpointEvents
            // which will be generated during test, create List of BreakpointData based on this information
            for (getArgumentValues001a.LocationData locationData : getArgumentValues001a.TestClass.testLocations) {
                BreakpointRequest request = debuggee.getEventRequestManager().createBreakpointRequest(getLocation(referenceType, locationData.lineNumber));
                request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                requests.add(new BreakpointData(request, locationData.breakpointsNumber));
                request.enable();
            }
        } catch (AbsentInformationException e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
            return;
        }

        boolean testThreadStarted = false;
        ThreadReference testThread = null;

        try {
            for (BreakpointData requestData : requests) {

                for (int i = 0; i < requestData.breakpointsNumber; i++) {
                    // start thread wich waits next event generated for given EventRequest
                    EventListenerThread listenerThread = new EventListenerThread(requestData.request);
                    listenerThread.start();
                    listenerThread.waitStartListen();

                    // if thread wasn't started start it
                    if (!testThreadStarted) {
                        pipe.println(getArgumentValues001a.COMMAND_START_TEST_THREAD);

                        if (!isDebuggeeReady())
                            return;

                        testThreadStarted = true;
                    } else
                        testThread.resume();

                    // wait for next BreakpointEvent
                    BreakpointEvent event = (BreakpointEvent)listenerThread.getEvent();

                    // expected argument values are stored in special static array 'expectedArgumentValues'
                    ArrayReference expectedArgValues = (ArrayReference)referenceType.getValue(referenceType.fieldByName("expectedArgumentValues"));

                    // get current frame
                    StackFrame frame = event.thread().frame(0);
                    List<Value> values = frame.getArgumentValues();
                    System.out.println("Total values: " + values.size());

                    if (expectedArgValues.length() != values.size()) {
                        setSuccess(false);
                        log.complain("Unexpected arguments number: " + values.size() + ", expected number is " + expectedArgValues.length());
                        printDebugInfo(values, expectedArgValues);
                        continue;
                    } else {
                        for (int j = 0; j < values.size(); j++) {
                            Value value = values.get(j);
                            // values for primitive types are wrapped in special class 'getArgumentValues001a.Value' and
                            // real value should be extracted before comparing
                            Value expectedValue = extractValue(expectedArgValues.getValue(j));

                            boolean success;

                            if (expectedValue == null) {
                                success = (value == null);
                            } else {
                                success = expectedValue.equals(value);
                            }

                            if (!success) {
                                setSuccess(false);
                                log.complain("Unexpected argument value: " + value + ", expected value: " + expectedValue);
                                printDebugInfo(values, expectedArgValues);
                                continue;
                            }
                        }
                    }

                    if (testThread == null)
                        testThread = event.thread();
                }

            }
        } catch (Throwable t) {
            setSuccess(false);
            log.complain("Unexpected exception: " + t);
            t.printStackTrace(log.getOutStream());
            return;
        }

        if (testThread != null)
            testThread.resume();

        pipe.println(getArgumentValues001a.COMMAND_STOP_TEST_THREAD);

        if (!isDebuggeeReady())
            return;
    }
}
