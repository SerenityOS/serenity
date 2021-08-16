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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/suspend/suspend001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ThreadReference.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ThreadReference.suspend()
 *     complies with its spec:
 *     public void suspend()
 *     Suspends this thread. The thread can be resumed through resume() or
 *     resumed with other threads through VirtualMachine.resume().
 *     Unlike java.lang.Thread#suspend, suspends of both the virtual machine and
 *     individual threads are counted.
 *     Before a thread will run again, it must be resumed (through resume() or
 *     resume()) the same number of times it has been suspended.
 *     Suspending single threads with this method has the same dangers as
 *     java.lang.Thread.suspend(). If the suspended thread holds a monitor
 *     needed by another running thread, deadlock is possible
 *     in the target VM (at least until the suspended thread is resumed again).
 *     The suspended thread is guaranteed to remain suspended until resumed
 *     through one of the JDI resume methods mentioned above;
 *     the application in the target VM cannot resume the suspended thread
 *     through java.lang.Thread#resume.
 *     Throws: ObjectCollectedException -
 *             if this object has been garbage collected.
 *     when a ThreadReference object has not been garbage collected,
 *     hence ObjectCollectedException is not expected to be thrown.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ThreadReference.suspend.suspend001;
 *     the debuggee program - nsk.jdi.ThreadReference.suspend.suspend001a.
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
 * @build nsk.jdi.ThreadReference.suspend.suspend001
 *        nsk.jdi.ThreadReference.suspend.suspend001a
 * @run main/othervm/timeout=420
 *      nsk.jdi.ThreadReference.suspend.suspend001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

