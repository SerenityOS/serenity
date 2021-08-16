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
 * @summary converted from VM Testbase nsk/jdwp/VirtualMachine/ReleaseEvents/releaseevents001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: VirtualMachine
 *         command: ReleaseEvents
 *     Test checks that debugee accept the command packet and
 *     replies with correct reply packet.
 *     Test consists of two compoments:
 *         debugger: releaseevents001
 *         debuggee: releaseevents001a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with synchronization signals.
 *     Before testing JDWP command debugger holds debuggee's events.
 *     Then, debugger creates command packet for VirtualMachine.ReleaseEvents
 *     command with no out data, writes packet to the transport channel,
 *     and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and checks that the received reply packet contains no data.
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 * COMMENTS
 *     Test fixed according to test bug:
 *     4954298 TEST_BUG: typo in nsk/jdwp/VirtualMachine/ReleaseEvents/<*> tests
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.VirtualMachine.ReleaseEvents.releaseevents001a
 * @run main/othervm
 *      nsk.jdwp.VirtualMachine.ReleaseEvents.releaseevents001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

