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
 * @summary converted from VM Testbase nsk/jdi/EventRequest/setSuspendPolicy/setsuspendpolicy001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     EventRequest.
 *     The test checks up that a result of the method
 *     com.sun.jdi.EventRequest.setSuspendPolicy()
 *     complies with its spec:
 *     public void setSuspendPolicy(int policy)
 *      Determines the threads to suspend when the requested event occurs in
 *      the target VM.
 *      Use SUSPEND_ALL to suspend all threads in the target VM (the default).
 *      Use SUSPEND_EVENT_THREAD to suspend only the thread which generated the event.
 *      Use SUSPEND_NONE to suspend no threads.
 *      Thread suspensions through events have the same functionality as
 *      explicitly requested suspensions.
 *      See ThreadReference.suspend() and VirtualMachine.suspend() for details.
 *      Throws: InvalidRequestStateException -
 *              if this request is currently enabled or has been deleted.
 *              Suspend policy may only be set in disabled requests.
 *     The test checks up on following assertions:
 *       - a value of policy is equal to one assigned by last method setSuspendPolicy(int);
 *       - InvalidRequestStateException is thrown if this request is currently enabled;
 *       - InvalidRequestStateException is thrown if this request has been deleted.
 *     The cases to check include Requests of all sub-types.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.EventRequest.setSuspendPolicy.setsuspendpolicy001;
 *     the debuggee program - nsk.jdi.EventRequest.setSuspendPolicy.setsuspendpolicy001a.
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
 * @build nsk.jdi.EventRequest.setSuspendPolicy.setsuspendpolicy001
 *        nsk.jdi.EventRequest.setSuspendPolicy.setsuspendpolicy001a
 * @run main/othervm
 *      nsk.jdi.EventRequest.setSuspendPolicy.setsuspendpolicy001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

