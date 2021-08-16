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
 * @summary converted from VM Testbase nsk/jdwp/Event/VM_DEATH/vmdeath002.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: Event
 *         command: Composite
 *         event kind: VM_DEATH
 *     Test checks that resquested VM_DEATH event is received upon
 *     debuggee VM exit either together with automatically generated
 *     VM_DEATH event or in a singleton event packet.
 *     Test consists of two compoments:
 *         debugger: vmdeath002
 *         debuggee: vmdeath002a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Debugger waits for debuggee started and makes an request for VM_DEATH
 *     event. Then, it resumes debuggee to allow it to exit.
 *     Next, debugger waits for an event packet and check if the received
 *     event contains only VM_DEATH events either requested one or automatically
 *     generated one. Received event should have expected requestID and
 *     correct attributes.
 *     Finally, debugger disconnects debuggee, waits for it exited
 *     and exits too with proper exit code.
 * COMMENTS
 *     Test was fixed due to test bug:
 *     4797978 TEST_BUG: potential race condition in a number of JDWP tests
 *     Test was fixed due to test bug:
 *     4863716 TEST_BUG: misprint in JDWP test vmdeath002
 *     Test was fixed due to test bug:
 *     4973268 TEST_BUG: nsk/jdwp/Event/VM_DEATH/vmdeath002 needs to be updated
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.Event.VM_DEATH.vmdeath002a
 * @run main/othervm
 *      nsk.jdwp.Event.VM_DEATH.vmdeath002
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

