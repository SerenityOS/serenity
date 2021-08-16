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
 * @summary converted from VM Testbase nsk/jdwp/ArrayReference/SetValues/setvalues001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ArrayReference
 *         command: SetValues
 *     Test checks that debugee accept the command packet and
 *     replies with correct reply packet. Also test checks that
 *     the new set values of array components are equal to the
 *     expected ones.
 *     Test consists of two compoments:
 *         debugger: setvalues001
 *         debuggee: setvalues001a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with synchronization messages.
 *     Next, debugger obtains from debuggee classID for the tested class and
 *     arrayID as the value of the class static field. Checked array in
 *     debuggee is filled with the initial integer values.
 *     Then, debugger creates command packet for SetValues command with the
 *     found arrayID, start index and number of components as arguments,
 *     and new integer values, writes packet to the transport channel
 *     and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and ensures there is no data in the packet. Also test send signal
 *     <RUN> to debuggee to check new array values. If new values are set
 *     correctly debuggee replies with signal DONE.
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.ArrayReference.SetValues.setvalues001a
 * @run main/othervm
 *      nsk.jdwp.ArrayReference.SetValues.setvalues001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

