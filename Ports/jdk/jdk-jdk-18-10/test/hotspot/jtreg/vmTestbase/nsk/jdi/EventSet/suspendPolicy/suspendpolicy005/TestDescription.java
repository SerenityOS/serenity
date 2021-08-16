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
 * @summary converted from VM Testbase nsk/jdi/EventSet/suspendPolicy/suspendpolicy005.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     EventSet.
 *     The test checks up that a result of the method
 *     com.sun.jdi.EventSet.suspendPolicy()
 *     complies with its spec:
 *     public int suspendPolicy()
 *      Returns the policy used to suspend threads in the target VM for this event set.
 *      This policy is selected from the suspend policies for each event's request;
 *      the target VM chooses the policy which suspends the most threads.
 *      The target VM suspends threads according to that policy and
 *      that policy is returned here.
 *      See EventRequest for the possible policy values.
 *      In rare cases, the suspend policy may differ from the requested value if
 *      a ClassPrepareEvent has occurred in a debugger system thread.
 *      See ClassPrepareEvent.thread() for details.
 *      Returns: the suspendPolicy which is either SUSPEND_ALL,
 *               SUSPEND_EVENT_THREAD or SUSPEND_NONE.
 *     The test checks that for an ExceptionEvent set,
 *     the method returns values corresponding to one which suspends the most threads.
 *     The cases to check include event sets containing one to three threads
 *     with all possible combinations:
 *     NONE, THREAD, ALL, NONE+THREAD, NONE+ALL, THREAD+ALL, NONE+THREAD+ALL
 *     The test works as follows:
 *     The debugger program - nsk.jdi.EventSet.suspendPolicy.suspendpolicy005;
 *     the debuggee program - nsk.jdi.EventSet.suspendPolicy.suspendpolicy005a.
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
 *     Fixed due to the bug 4528893.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventSet.suspendPolicy.suspendpolicy005
 *        nsk.jdi.EventSet.suspendPolicy.suspendpolicy005a
 * @run main/othervm
 *      nsk.jdi.EventSet.suspendPolicy.suspendpolicy005
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

