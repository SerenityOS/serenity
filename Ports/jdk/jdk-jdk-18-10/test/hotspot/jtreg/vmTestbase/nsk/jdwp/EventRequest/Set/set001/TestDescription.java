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
 * @summary converted from VM Testbase nsk/jdwp/EventRequest/Set/set001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: EventRequest
 *         command: Set
 *         event kind: BREAKPOINT
 *     Test checks that debuggee accept tested command and replies
 *     with correct reply packet. Also test checks that requested
 *     breakpoint events occurs for created request.
 *     Test consists of two compoments:
 *         debugger: set001
 *         debuggee: set001a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Next, debugger waits for tested class loaded and constructs
 *     location for breakpoint request.
 *     Then, debugger creates command packet for command EventRequest.Set
 *     for this breakpoint location, sends it to debuggee and waits for
 *     reply packet. When reply packed is received, debugger extracts
 *     requestID and checks if it is not null.
 *     Then, debugger resumes debuggee and waits for expected BREAKPOINT
 *     event. If received event is not BREAKPOINT event, debugger
 *     complains an error.
 *     Finally, debugger disconnectes debuggee, waits for it exits
 *     and exits too with proper exit code.
 * COMMENTS
 *     Test was fixed due to test bug:
 *     4797978 TEST_BUG: potential race condition in a number of JDWP tests
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.EventRequest.Set.set001a
 * @run main/othervm
 *      nsk.jdwp.EventRequest.Set.set001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

