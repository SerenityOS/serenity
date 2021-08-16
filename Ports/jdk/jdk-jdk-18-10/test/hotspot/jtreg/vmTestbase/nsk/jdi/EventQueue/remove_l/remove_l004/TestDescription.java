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
 * @summary converted from VM Testbase nsk/jdi/EventQueue/remove_l/remove_l004.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     EventQueue.
 *     The test checks up that a result of the method
 *     com.sun.jdi.EventQueue.remove_l()
 *     complies with its spec:
 *     public EventSet remove(long timeout)
 *                 throws java.lang.InterruptedException
 *      Waits a specified time for the next available event.
 *      Parameters: timeout - Time in milliseconds to wait for the next event
 *      Returns: the next EventSet, or null if there is a timeout.
 *      Throws: java.lang.InterruptedException -
 *              if another thread has interrupted this thread.
 *              VMDisconnectedException -
 *              if the connection to the target VM is no longer available.
 *              Note this will always be preceded by a VMDisconnectEvent.
 *     The test checks two assertions:
 *     - Returns: null if there is a timeout.
 *     - Returns: the next EventSet if no timeout.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.EventQueue.remove_l.remove_l004;
 *     the debuggee program - nsk.jdi.EventQueue.remove_l.remove_l004a.
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
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventQueue.remove_l.remove_l004
 *        nsk.jdi.EventQueue.remove_l.remove_l004a
 * @run main/othervm
 *      nsk.jdi.EventQueue.remove_l.remove_l004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts} -Dtest.timeout.factor=${test.timeout.factor}"
 */

