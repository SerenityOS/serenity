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
 * @summary converted from VM Testbase nsk/jdwp/VirtualMachine/HoldEvents/holdevents002.
 * VM Testbase keywords: [jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: VirtualMachine
 *         command: HoldEvents
 *         event kind: BREAKPOINT
 *     Test checks that no requested BREAKPOINT events received from
 *     debuggee after holding events.
 *     Test consists of two compoments:
 *         debugger: holdevents002
 *         debuggee: holdevents002a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Next, debugger waits for tested class loaded and requests an
 *     BREAKPOINT event for some location into tested class.
 *     Then, debugger uses tesed command VirtualMachine.HoldEvents to
 *     hold events into debuggee and resumes debuggee to allow it to
 *     reach the breakpoint. Debugger waits for WAITTIME timeout for
 *     an event packet. If no event packet received, tests passes.
 *     If requested BREAKPOINT or any other event received? test failed.
 *     Finally, debugger disconnectes debuggee, waits for it exit
 *     and exits too with proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.VirtualMachine.HoldEvents.holdevents002a
 * @run main/othervm/timeout=420
 *      nsk.jdwp.VirtualMachine.HoldEvents.holdevents002
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

