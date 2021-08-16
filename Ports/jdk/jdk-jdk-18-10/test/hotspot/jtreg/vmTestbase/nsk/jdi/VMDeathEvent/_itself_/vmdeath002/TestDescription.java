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
 * @summary converted from VM Testbase nsk/jdi/VMDeathEvent/_itself_/vmdeath002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     VMDeathEvent.
 *     The test checks up that the intefrace com.sun.jdi.VMDeathEvent
 *     complies with its spec:
 *     public interface VMDeathEvent
 *     extends Event
 *     Notification of target VM termination. This event occurs
 *     if the target VM terminates before the VM disconnects (VMDisconnectEvent).
 *     Thus, this event will NOT occur if external forces terminate the connection
 *     (e.g. a crash) or if the
 *     connection is intentionally terminated with VirtualMachine.dispose()
 *     On VM termination, a single unsolicited VMDeathEvent will always be sent
 *     with a suspend policy of SUSPEND_NONE.
 *     Additional VMDeathEvents will be sent in the same event set if
 *     they are requested with a VMDeathRequest.
 *     The VM is still intact and can be queried at the point this event was initiated
 *     but immediately thereafter it is not considered intact and cannot be queried.
 *     Note: If the enclosing EventSet has a suspend policy other than
 *     SUSPEND_ALL the initiating point may be long past.
 *     All VMDeathEvents will be in a single EventSet,
 *     no other events will be in the event set.
 *     A resume must occur to continue execution
 *     after any event set which performs suspensions -
 *     in this case to allow proper shutdown.
 *     The test checks that
 *     this event occurs
 *     if the target VM terminates before the VM disconnects
 *     as result of calling VirtualMachine.exit(int).
 *     The test works as follows:
 *     The debugger program - nsk.jdi.VMDeathEvent._itself_.vmdeath002;
 *     the debuggee program - nsk.jdi.VMDeathEvent._itself_.vmdeath002a.
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
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *       - waiting for VMStartEvent removed because it is handled now in
 *         binder.bindToDebugee() method
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VMDeathEvent._itself_.vmdeath002
 *        nsk.jdi.VMDeathEvent._itself_.vmdeath002a
 * @run main/othervm
 *      nsk.jdi.VMDeathEvent._itself_.vmdeath002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

