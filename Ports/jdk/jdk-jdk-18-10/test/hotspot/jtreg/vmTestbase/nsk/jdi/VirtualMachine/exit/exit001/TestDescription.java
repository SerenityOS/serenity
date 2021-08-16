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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/exit/exit001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     VirtualMachine.
 *     The test checks up that a result of the method
 *     com.sun.jdi.VirtualMachine.exit()
 *     complies with its spec:
 *     public void exit(int exitCode)
 *     Causes the mirrored VM to terminate with the given error code.
 *     All resources associated with this VirtualMachine are freed.
 *     If the mirrored VM is remote, the communication channel to it will be closed.
 *     Resources originating in this VirtualMachine (ObjectReferences,
 *     ReferenceTypes, etc.) will become invalid.
 *     Threads running in the mirrored VM are abruptly terminated.
 *     A thread death exception is not thrown and finally blocks are not run.
 *     Parameters: exitCode - the exit code for the target VM.
 *     The test checks up that after call to VirtualMachine.exit(),
 *     the communication channel is closed, hence,
 *     the VirtualMachine object becomes invalid.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.VirtualMachine.exit.exit001;
 *     the debuggee program - nsk.jdi.VirtualMachine.exit.exit001a.
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
 * @build nsk.jdi.VirtualMachine.exit.exit001
 *        nsk.jdi.VirtualMachine.exit.exit001a
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.exit.exit001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

