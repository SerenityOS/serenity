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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/dispose/dispose004.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     VirtualMachine.
 *     The test checks up that a result of the method
 *     com.sun.jdi.VirtualMachine.dispose()
 *     complies with its spec:
 *     public void dispose()
 *     Invalidates this virtual machine mirror.
 *     The communication channel to the target VM is closed, and
 *     the target VM prepares to accept another subsequent connection from
 *     this debugger or another debugger, including the following tasks:
 *       All event requests are cancelled.
 *       All threads suspended by suspend() or by ThreadReference.suspend()
 *         are resumed as many times as necessary for them to run.
 *       Garbage collection is re-enabled in all cases where
 *         it was disabled through ObjectReference.disableCollection().
 *     Any current method invocations executing in the target VM are continued
 *     after the disconnection. Upon completion of any such method invocation,
 *     the invoking thread continues from the location where
 *     it was originally stopped.
 *     Resources originating in this VirtualMachine (ObjectReferences,
 *     ReferenceTypes, etc.) will become invalid.
 *     The test checks up that after call to VirtualMachine.dispose(),
 *     debuggee's tested thread suspended by the debuggee with the method
 *     Thread.suspend(), is resumed and runs.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.VirtualMachine.dispose.dispose004;
 *     the debuggee program - nsk.jdi.VirtualMachine.dispose.dispose004a.
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
 * @build nsk.jdi.VirtualMachine.dispose.dispose004
 *        nsk.jdi.VirtualMachine.dispose.dispose004a
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.dispose.dispose004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

