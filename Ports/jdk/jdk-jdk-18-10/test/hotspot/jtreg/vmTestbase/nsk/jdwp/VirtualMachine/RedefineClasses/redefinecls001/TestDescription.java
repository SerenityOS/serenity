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
 * @summary converted from VM Testbase nsk/jdwp/VirtualMachine/RedefineClasses/redefinecls001.
 * VM Testbase keywords: [quick, jpda, jdwp, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: VirtualMachine
 *         command: RedefineClasses
 *     Test checks that debugee accept the command packet and
 *     replies with correct reply packet. Also test checks that
 *     after class redefinition invocation of static and object
 *     methods resuts in invocation of redefined methods, and
 *     no constructors are invoked.
 *     Test consists of two compoments:
 *         debugger: redefinecls001
 *         debuggee: redefinecls001a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     It also loads bytecode of redefined class from *.klass file.
 *     Next, debugger waits for tested classes are loaded, and breakpoint
 *     before class redefinition is reached.
 *     Then, debugger creates command packet for VirtualMachine.RedefineClasses
 *     command for the found classID and loaded bytecode, writes this packet
 *     to the transport channel, and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and checks if no reply data returned in the packet.
 *     Then, debugger resumes debuggee to allow methods of redefined class
 *     to be invoked and waits for second breakpoint is reached.
 *     Upon their invocation redefined methods puts proper value into
 *     the special static fields of debuggee class. The debugger then
 *     checks these fields to verify if the redefined methods were invoked
 *     and no constructors were invoked.
 *     Finally, debugger disconnects debuggee, waits for it exits
 *     and exits too with the proper exit code.
 * COMMENTS
 *     First positional argument for the test should be path to the test
 *     work directory where loaded *.klass file should be located.
 *     Test was updated according to rfe:
 *     4691123 TEST: some jdi tests contain precompiled .klass files undes SCCS.
 *     redefinecl001b.ja was moved into newclass directory and renamed
 *     to redefinecl001b.java.
 *     The precompiled class file is created during test base build process.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build ExecDriver
 * @build nsk.jdwp.VirtualMachine.RedefineClasses.redefinecls001a
 *        nsk.jdwp.VirtualMachine.RedefineClasses.redefinecls001b
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass
 * @run main/othervm
 *      nsk.jdwp.VirtualMachine.RedefineClasses.redefinecls001
 *      .
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

