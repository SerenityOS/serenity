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
 * @summary converted from VM Testbase nsk/jdwp/Method/IsObsolete/isobsolete002.
 * VM Testbase keywords: [quick, jpda, jdwp, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: Method
 *         command: IsObsolete
 *     Test checks that debugee accept the command packet and
 *     replies with correct reply packet after class redefinition
 *     for redefined method being at that time with active stack
 *     frame.
 *     Test consists of two compoments:
 *         debugger: isobsolete002
 *         debuggee: isobsolete002a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     It waits for tested class is loaded, sets breakpoint on the tested
 *     method, and waits for breakpoint is reached.
 *     Then, it loads bytecode of redefined class from *.klass file and
 *     redefines the class. After class redefinition, debugger queries
 *     debiggi for the methodID of to level stack frame and checks this
 *     method.
 *     Debugger creates command packet with Method.IsObsolete command for
 *     the tested method, writes this packet to the transport channel,
 *     and waits for a reply packet. When reply packet is received,
 *     debugger parses the packet structure and checks if expected
 *     isObsolete value returned in the packet.
 *     Finally, debugger disconnects debuggee, waits for it exits
 *     and exits too with the proper exit code.
 * COMMENTS
 *     First positional argument for the test should be path to the test
 *     work directory where loaded *.klass file should be located.
 *         Test was fixed due to test bug:
 *         4514956 Method.isObsolete() returns false for redefined method
 *     Test was updated according to rfe:
 *     4691123 TEST: some jdi tests contain precompiled .klass files undes SCCS.
 *     isobsolete002b.ja was moved into newclass directory and renamed
 *     to isobsolete002b.java.
 *     The precompiled class file is created during test base build process.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build ExecDriver
 * @build nsk.jdwp.Method.IsObsolete.isobsolete002a
 *        nsk.jdwp.Method.IsObsolete.isobsolete002b
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass
 * @run main/othervm
 *      nsk.jdwp.Method.IsObsolete.isobsolete002
 *      .
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

