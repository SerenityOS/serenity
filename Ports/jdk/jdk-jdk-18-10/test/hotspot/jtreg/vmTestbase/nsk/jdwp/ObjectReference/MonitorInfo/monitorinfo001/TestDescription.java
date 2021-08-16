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
 * @summary converted from VM Testbase nsk/jdwp/ObjectReference/MonitorInfo/monitorinfo001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ObjectReference
 *         command: MonitorInfo
 *     Test checks that debugee accept the command packet and
 *     replies with correct reply packet.
 *     Also test checks that expected threadIDs returned either for
 *     monitor owner and waiter threads.
 *     Test consists of two compoments:
 *         debugger: monitorinfo001
 *         debuggee: monitorinfo001a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with synchronization signals.
 *     Debuggee created oan object of the tested class as value of the class
 *     static field and starts several threads, which are waiting for this
 *     object monitor, and one thread, which ownes the object monitor.
 *     Then debuggee sends signal READY to the debugger.
 *     Debugger obtains from debuggee classID for tested class and objectID
 *     as the value of the class static field. Also debugger obtains threadIDs
 *     for all tested thread into debuggee.
 *     Debugger suspends debuggee (and all threads into it) before sending
 *     tested JDWP command.
 *     Then, debugger creates command packet for ObjectReference.MonitorInfo
 *     command with the found objectID as an argument, writes packet to
 *     the transport channel, and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and extracts threadIDs of thread waiting and owning the object monitor.
 *     Debugger checks that extracted threadIDs are equals to the expected ones..
 *     Finally, debugger resumes debuggee, sends it signal to quit, waits
 *     for it exits and exits too with the proper exit code.
 * COMMENTS
 *     For JDK 1.4.0-beta3 (build 1.4.0-beta3-b84) and earlier this test passed
 *     because target VM does not support VM capability: canGetMonitorInfo
 *     Test was fixed due to test bug:
 *     4864492 TEST_BUG: potential race in JDWP test monitorinfo001
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.ObjectReference.MonitorInfo.monitorinfo001a
 * @run main/othervm
 *      nsk.jdwp.ObjectReference.MonitorInfo.monitorinfo001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

