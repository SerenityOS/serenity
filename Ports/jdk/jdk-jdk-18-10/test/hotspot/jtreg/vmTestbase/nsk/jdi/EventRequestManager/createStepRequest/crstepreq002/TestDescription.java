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
 * @summary converted from VM Testbase nsk/jdi/EventRequestManager/createStepRequest/crstepreq002.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     EventRequestManager.
 *     The test checks up that a result of the method
 *     com.sun.jdi.EventRequestManager.createStepRequest()
 *     complies with its spec:
 *     public StepRequest createStepRequest(ThreadReference thread, int size, int depth)
 *      Creates a new disabled StepRequest.
 *      The new event request is added to the list managed by this EventRequestManager.
 *      Use EventRequest.enable() to activate this event request.
 *      The returned request will control stepping only in the specified thread;
 *      all other threads will be unaffected.
 *      A sizevalue of StepRequest.STEP_MIN will generate a step event each time the code index changes.
 *      It represents the smallest step size available and often maps to the instruction level.
 *      A size value of StepRequest.STEP_LINE will generate a step event  each time the source line changes.
 *      A depth value of StepRequest.STEP_INTO will generate step events in any called methods.
 *      A depth value of StepRequest.STEP_OVER restricts step events to the current frame or caller frames.
 *      A depth value of StepRequest.STEP_OUT restricts step events to caller frames only.
 *      All depth restrictions are relative to the call stack immediately before the step takes place.
 *      Only one pending step request is allowed per thread.
 *      Parameters: thread - the thread in which to step
 *                  depth - the step depth
 *                  size - the step size
 *      Returns:    the created StepRequest
 *      Throws:     DuplicateRequestException -
 *                  if there is already a pending step request for the specified thread.
 *     The test checks up on the following assertions:
 *     - Creates a new disabled StepRequest.
 *     - STEP_MIN, STEP_LINE, STEP_INTO, STEP_OVER, STEP_OUT are valid arguments,
 *       that is, they don't throw IllegalArgumentException.
 *     - Values larger and lesser corresponding above are invalid arguments, that is,
 *       they do throw IllegalArgumentException.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.EventRequestManager.createStepRequest.crstepreq002;
 *     the debuggee program - nsk.jdi.EventRequestManager.createStepRequest.crstepreq002a.
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
 * @build nsk.jdi.EventRequestManager.createStepRequest.crstepreq002
 *        nsk.jdi.EventRequestManager.createStepRequest.crstepreq002a
 * @run main/othervm
 *      nsk.jdi.EventRequestManager.createStepRequest.crstepreq002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

