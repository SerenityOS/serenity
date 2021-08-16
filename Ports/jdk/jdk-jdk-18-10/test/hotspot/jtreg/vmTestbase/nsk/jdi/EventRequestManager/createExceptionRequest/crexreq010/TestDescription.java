/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jdi/EventRequestManager/createExceptionRequest/crexreq010.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     EventRequestManager.
 *     The test checks up that a result of the method
 *     com.sun.jdi.EventRequestManager.createExceptionRequest()
 *     complies with its spec:
 *     public ExceptionRequest createExceptionRequest(ReferenceType refType,
 *                                                    boolean notifyCaught,
 *                                                    boolean notifyUncaught)
 *      Creates a new disabled ExceptionRequest.
 *      The new event request is added to the list managed by this EventRequestManager.
 *      Use EventRequest.enable() to activate this event request.
 *      A specific exception type and its subclasses can be selected for exception events.
 *      Caught exceptions, uncaught exceptions, or both can be selected.
 *      Note, however, that at the time an exception is thrown,
 *      it is not always possible to determine whether it is truly caught.
 *      See ExceptionEvent.catchLocation() for details.
 *      Parameters: refType - If non-null, specifies that exceptions
 *                            which are instances of refType will be reported.
 *                            Note: this will include instances of sub-types.
 *                            If null, all instances will be reported
 *                  notifyCaught   - If true, caught exceptions will be reported.
 *                  notifyUncaught - If true, uncaught exceptions will be reported.
 *      Returns: the created ExceptionRequest
 *     The test checks up on the following assertion:
 *       - If neither caught exception nor uncaught exception is selected,
 *         that is, both notifyCaught and notifyUncaught arguments are false,
 *         returned value is null.
 *     Note. In condition of no Exception for such testcase is defined in the spec,
 *           null is the only reasonable returned value.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.EventRequestManager.createExceptionRequest.crexreq010;
 *     the debuggee program - nsk.jdi.EventRequestManager.createExceptionRequest.crexreq010a.
 *     Using nsk.jdi.share classes,
 *     the debugger gets the debuggee running on another JavaVM,
 *     creates the object debuggee.VM, and waits for VMStartEvent.
 *     Upon getting the debuggee VM started,
 *     the debugger calls corresponding debuggee.VM methods to get
 *     needed data and to perform checks.
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *     Test fixed according to test bug:
 *     4615030 crexreq010: method createExceptionRequest() doesn't return expected
 *     - test now checks that createExceptionRequest() returns non-null object
 *       and no unspecified exception is thrown
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventRequestManager.createExceptionRequest.crexreq010
 *        nsk.jdi.EventRequestManager.createExceptionRequest.crexreq010a
 * @run main/othervm
 *      nsk.jdi.EventRequestManager.createExceptionRequest.crexreq010
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

