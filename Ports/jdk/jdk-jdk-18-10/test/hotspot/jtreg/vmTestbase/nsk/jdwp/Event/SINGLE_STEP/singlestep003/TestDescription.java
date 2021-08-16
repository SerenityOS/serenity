/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jdwp/Event/SINGLE_STEP/singlestep003.
 * VM Testbase keywords: [quick, jpda, jdwp, exclude]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: Event
 *         command: Composite
 *         command set: EventRequest
 *         command: Set, Clear
 *         event kind: SINGLE_STEP
 *     Test checks that
 *         1) debuggee successfully creates SINGLE_STEP event request
 *            for particular threadID with attributes:
 *            depth=OUT, size=LINE
 *         2) expected SINGLE_STEP event is received when the thread
 *            in debuggee steps to the next line after breakpoint
 *         3) debuggee successfully removes event request
 *     Test consists of two compoments:
 *         debugger: singlestep003
 *         debuggee: singlestep003a
 *     First, debugger uses nsk.share support classes to launch debuggee,
 *     and obtains Transport object, that represents JDWP transport channel.
 *     Next, debugger waits for tested class loaded and gets methodID
 *     and location for expected single step event out of invoked method.
 *     Then debugger waits for breakpoint reached with some thread
 *     and makes request for SINGLE_STEP event for this threadID.
 *     When event is received debugger checks if the received event is for
 *     expected request, thread, and location and has correct attributes.
 *     Then debugger removes event request.
 *     Finally, debugger disconnects debuggee, waits for it exited
 *     and exits too with proper exit code.
 * COMMENTS
 *     Test was fixed due to test bug:
 *     4797978 TEST_BUG: potential race condition in a number of JDWP tests
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.Event.SINGLE_STEP.singlestep003a
 * @run main/othervm
 *      nsk.jdwp.Event.SINGLE_STEP.singlestep003
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

