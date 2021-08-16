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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/isSuspended/issuspended004.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ThreadReference.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ThreadReference.isSuspended()
 *     complies with its spec:
 *     public boolean isSuspended()
 *     Determines whether the thread has been suspended by the the debugger.
 *     Returns: true if the thread is currently suspended; false otherwise.
 *     Throws: ObjectCollectedException -
 *             if this object has been garbage collected.
 *     when a ThreadReference object has not been garbage collected,
 *     hence ObjectCollectedException is not expected to be thrown.
 *     The test checks up the following particular case:
 *     - a debuggee with second thread started up and
 *     locked up at a synchronized block,
 *     - suspending the thread with VirtualMachine.suspend() after
 *     making debugger's EventQueue empty.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ThreadReference.isSuspended.issuspended004;
 *     the debuggee program - nsk.jdi.ThreadReference.isSuspended.issuspended004a.
 *     Using nsk.jdi.share classes,
 *     the debugger gets the debuggee running on another JavaVM,
 *     creates the object debuggee.VM,
 *     establishes a pipe with the debuggee program, and then
 *     send to the programm commands, to which the debuggee replies
 *     via the pipe. Upon getting reply,
 *     the debugger calls corresponding debuggee.VM methods to get
 *     needed data and compares the data got to the data expected.
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.isSuspended.issuspended004
 *        nsk.jdi.ThreadReference.isSuspended.issuspended004a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.isSuspended.issuspended004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

