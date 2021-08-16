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
 * @summary converted from VM Testbase nsk/jdi/EventRequest/setEnabled/setenabled001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     EventRequest.
 *     The test checks up that a result of the method
 *     com.sun.jdi.EventRequest.setEnabled()
 *     complies with its spec:
 *     public void setEnabled(boolean val)
 *      Enables or disables this event request.
 *      While this event request is disabled, the event request will be ignored and
 *      the target VM will not be stopped if any of its threads reaches the event request.
 *      Disabled event requests still exist, and are included in event request lists
 *      such as EventRequestManager.breakpointRequests().
 *      Parameters: val - true if the event request is to be enabled; false otherwise.
 *      Throws: InvalidRequestStateException -
 *              if this request has been deleted.
 *              IllegalThreadStateException -
 *              if this is a StepRequest, val is true, and
 *              the thread named in the request has died.
 *     The test checks up on the following assertion:
 *         Enables or disables this event request.
 *     The cases to test include all sub-types of EventRequest.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.EventRequest.setEnabled.setenabled001;
 *     the debuggee program - nsk.jdi.EventRequest.setEnabled.setenabled001a.
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
 *        Modified due to fix of the bug:
 *        4915497 TEST_BUG: wrong synchronization in several JDI tests
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventRequest.setEnabled.setenabled001
 *        nsk.jdi.EventRequest.setEnabled.setenabled001a
 * @run main/othervm
 *      nsk.jdi.EventRequest.setEnabled.setenabled001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

