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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/frames_ii/frames_ii001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ThreadReference.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ThreadReference.frames(int,int)
 *     complies with its spec:
 *     public java.util.List frames(int start, int length)
 *                       throws IncompatibleThreadStateException
 *     Returns a List containing a range of StackFrame mirrors from
 *     the thread's current call stack. The thread must be suspended
 *     (normally through an interruption to the VM) to get this information, and
 *     it is only valid until the thread is resumed again.
 *     Parameters: start  - the index of the first frame
 *                 length - the number of frames to retrieve
 *     Returns: a List of StackFrame with the current frame first
 *              followed by each caller's frame.
 *     Throws: IncompatibleThreadStateException -
 *             if the thread is not suspended in the target VM
 *             IndexOutOfBoundsException -
 *             if the specified range is not within the range
 *             from 0 to frameCount() - 1.
 *             ObjectCollectedException -
 *             if this object has been garbage collected.
 *     when a ThreadReference object has not been garbage collected,
 *     hence ObjectCollectedException is not expected to be thrown.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ThreadReference.frames_ii.frames_ii001;
 *     the debuggee program - nsk.jdi.ThreadReference.frames_ii.frames_ii001a.
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
 * New Merlin specification contains the following conditions to throw
 * IndexOutOfBoundsException:
 *     if the specified range is not within the range of stack frame indicies.
 *     That is, the exception is thrown if any of the following are true:
 *              start < 0
 *              start >= frameCount()
 *              length < 0
 *              (start+length) > frameCount()
 * To comply with the conditions,
 * all checks on "length < 0" in the test were corrected
 * to catch the specified Exception.
 * If the Exception is not thrown,
 * the test returnes the value FAILED.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.frames_ii.frames_ii001
 *        nsk.jdi.ThreadReference.frames_ii.frames_ii001a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.frames_ii.frames_ii001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

