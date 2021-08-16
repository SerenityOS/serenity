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
 * @summary converted from VM Testbase nsk/jdwp/Method/LineTable/linetable001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: Method
 *         command: LineTable
 *     Test checks that debugee accept the command packet and
 *     replies with correct reply packet.
 *     Also test checks that code index and number for returned lines
 *     have expected values, namely:
 *         1) start code index is strongly less than
 *            end code index
 *     and for each line:
 *         2) line code index is between start and end code index
 *         3) line code index for previous line is strongly less
 *            then for next line
 *         4) line number is between start and end line number
 *         5) line number for previous line is no greater
 *            then for next line
 *         6) number of next line follows directly to the number
 *            of previous line
 *     Test consists of two compoments:
 *         debugger: linetable001
 *         debuggee: linetable001a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with synchronization signals.
 *     Next, debugger obtains classID and methodID for the tested class
 *     and method from debuggee.
 *     Then, debugger creates command packet for Method.LineTable command
 *     with the found classID and methodID as arguments, writes packet to
 *     the transport channel, and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and extracts code index and number for each lines. Also test
 *     checks above mentioned assertions.
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.Method.LineTable.linetable001a
 * @run main/othervm
 *      nsk.jdwp.Method.LineTable.linetable001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

