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
 * @summary converted from VM Testbase nsk/jdi/EventRequestManager/createStepRequest/crstepreq003.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *   The test checks whether an implementation of
 *       EventRequestManager.createStepRequest (ThreadReference thread, int size, int depth)
 *   method complies with its spec.
 *   The test checks the following assertions:
 *       A size value of StepRequest.STEP_LINE will generate a step event each time
 *       the source line changes. A depth value of StepRequest.STEP_INTO will generate
 *       step events in any called methods. A depth value of StepRequest.STEP_OVER
 *       restricts step events to the current frame or caller frames. A depth value
 *       of StepRequest.STEP_OUT restricts step events to caller frames only.
 *   The debugger class - nsk.jdi.EventRequestManager.createStepRequest.crstepreq003;
 *   the debuggee class - nsk.jdi.EventRequestManager.createStepRequest.crstepreq003a;
 *   The test works as follows. At first, preliminary phase:
 *    - the debugger connects to the debuggee using nsk.jdi.share classes;
 *    - the debugger waits for the VMStartEvent and requested ClassPrepareEvent for
 *      debuggee class.
 *   At second, test specific phase, each action is performed for every test case:
 *    - the debugger waits for requested BreakpointEvent which is set on a line
 *      of debuggee's breakInThread() method at the beginning of synchronized block;
 *    - the debuggee starts additional thread with 'thread1' name and it gets
 *      suspended at breakpoint;
 *    - After getting BreakpointEvent, the debugger sets StepRequest for thread1 and
 *      resumes debuggee;
 *    - after getting StepEvent, the debugger checks its location by comparing with
 *      expected line number value.
 *   There are three cases in the test:
 *    - step request is created with STEP_LINE size and STEP_INTO depth,
 *    - step request is created with STEP_LINE size and STEP_OVER depth,
 *    - step request is created with STEP_LINE size and STEP_OUT depth.
 *   In case of error the test produces the return value 97 and a corresponding
 *   error message(s). Otherwise, the test is passed and produces the return
 *   value 95 and no message.
 * COMMENTS:
 *   This test is developed basing on analysis of 4628003 bug. The test is aimed
 *   to check to the case when debuggee's thread in step request does not wait
 *   for lock in syncronized block.
 *   Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - remove waiting for VMStartEvent after launching debuggee VM
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventRequestManager.createStepRequest.crstepreq003
 *        nsk.jdi.EventRequestManager.createStepRequest.crstepreq003a
 * @run main/othervm
 *      nsk.jdi.EventRequestManager.createStepRequest.crstepreq003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

