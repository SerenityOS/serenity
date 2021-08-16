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
 * @summary converted from VM Testbase nsk/jdi/EventRequestManager/createStepRequest/crstepreq005.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *   The test checks whether an implementation of
 *       EventRequestManager.createStepRequest (ThreadReference thread, int size, int depth)
 *   method complies with its spec.
 *   The test checks the following assertions:
 *       A size value of StepRequest.STEP_LINE will generate a step event each time
 *       the source line changes...
 *       A depth value of StepRequest.STEP_OVER restricts step events to the current
 *       frame or caller frames.
 *   The debugger class - nsk.jdi.EventRequestManager.createStepRequest.crstepreq005;
 *   the debuggee class - nsk.jdi.EventRequestManager.createStepRequest.crstepreq005a;
 *   The test works as follows. At first, preliminary phase:
 *    - the debugger connects to the debuggee using nsk.jdi.share classes;
 *    - the debugger waits for the VMStartEvent and requested ClassPrepareEvent for
 *      debuggee class.
 *   At second, test specific phase, each action is performed for every test case:
 *    - the debugger waits for requested BreakpointEvent which is set on a line
 *      of debuggee's methodForCommunication() method. This method is invoked in
 *      additional 'thread1' started in debuggee. First breakpoint allows the debugger
 *      to obtain ThreadReference mirror of debuggee's 'thread1';
 *    - after getting first BreakpointEvent, the debugger sets second BreakpointRequest
 *      to suspend 'thread1 at the right location before setting step request;
 *    - after getting second BreakpointEvent, the debugger sets StepRequest
 *      with STEP_LINE size and STEP_OVER depth, resumes the debuggee and
 *      waits for two consecutive StepEvents;
 *    - upon getting StepEvents, the debugger checks their locations by comparing with
 *      expected line number values.
 *   In case of error the test produces the return value 97 and a corresponding
 *   error message(s). Otherwise, the test is passed and produces the return
 *   value 95 and no message.
 * COMMENTS:
 *   4767945 TEST_BUG: crstepreq005 test fails in Mantis
 *   Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - remove waiting for VMStartEvent after launching debuggee VM
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventRequestManager.createStepRequest.crstepreq005
 *        nsk.jdi.EventRequestManager.createStepRequest.crstepreq005a
 * @run main/othervm
 *      nsk.jdi.EventRequestManager.createStepRequest.crstepreq005
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

