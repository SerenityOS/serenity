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
 * @summary converted from VM Testbase nsk/jdwp/StackFrame/PopFrames/popframes001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: StackFrame
 *         command: PopFrames
 *     Test checks that debugee accept the command packet and
 *     replies with correct reply packet. Also test checks that
 *     after current frame is popped the tested method is invoked
 *     second time.
 *     Test consists of two compoments:
 *         debugger: popframes001
 *         debuggee: popframes001a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Next, debugger waits for tested class loaded, requests methodID
 *     for tested method, sets breakpoint and wait for breakpoint reached.
 *     After breakpoint event is received with a thread, debugger gets top
 *     stack frame for this thread.
 *     Then, debugger creates command packet for StackFrame.PopFrames
 *     command with the found threadID and frameID, writes this packet
 *     to the transport channel, and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and checks if no reply data returned in the packet.
 *     Then, checks if the tested method will be invoked second time.
 *     To do so, it resumes debuggee and waits for BREAKPOINT event
 *     will be received again. If expected BREAKPOINT event is not received,
 *     debugger complains an error.
 *     Finally, debugger disconnects debuggee, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.StackFrame.PopFrames.popframes001a
 * @run main/othervm
 *      nsk.jdwp.StackFrame.PopFrames.popframes001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

