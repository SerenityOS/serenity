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
 * @summary converted from VM Testbase nsk/jdi/EventRequestManager/_bounds_/requests001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the boundary value of the parameters.
 *     The test checks up the methods
 *     a) createStepRequest(ThreadReference, int, int)
 *            this method is invoked 3 times with different arguments:
 *              1. (null, StepRequest.STEP_LINE, StepRequest.STEP_OVER)
 *                 in this case NullPointerException is expected
 *              2. (thread, Integer.MAX_VALUE, StepRequest.STEP_OVER)
 *              3. (null, StepRequest.STEP_LINE, Integer.MAX_VALUE)
 *                 in 2, 3 cases no exceptions are expected
 *     b) createBreakpointRequest(Location)
 *     c) createAccessWatchpointRequest(Field)
 *     d) createModificationWatchpointRequest(Field)
 *     f) deleteEventRequest(EventRequest)
 *     g) deleteEventRequests(List)
 *    In b)-g) cases <code>NullPointerException</code> is expected.
 * COMMENTS:
 *     10.21.2002 fix for #4764615
 *     When calling
 *         createStepRequest(thread, Integer.MAX_VALUE,StepRequest.STEP_OVER) or
 *         createStepRequest(null, StepRequest.STEP_LINE,Integer.MAX_VALUE),
 *     throwing of IllegalArgumentException is expected.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventRequestManager._bounds_.requests001
 *        nsk.jdi.EventRequestManager._bounds_.requests001a
 * @run main/othervm
 *      nsk.jdi.EventRequestManager._bounds_.requests001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

