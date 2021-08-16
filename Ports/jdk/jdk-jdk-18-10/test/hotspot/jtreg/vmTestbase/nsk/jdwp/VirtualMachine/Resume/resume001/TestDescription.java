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
 * @summary converted from VM Testbase nsk/jdwp/VirtualMachine/Resume/resume001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: VirtualMachine
 *         command: Resume
 *     Test checks that debugee accept command and replies
 *     with correct reply packet. Also test checks that
 *     debugee is actually resumed by the command.
 *     First, test launches debuggee VM using support classes
 *     and connects to it. Debugee is launched in the suspend
 *     mode by default.
 *     Then test sends Resume command and waits for a
 *     reply packet.
 *     When reply is received test checks if the reply packet
 *     has proper structure. Then test waits for a signal
 *     from the resumed debuggee. If signal has not received
 *     after specified by WAITTIME timeout, test complains
 *     about an error.
 *     Finally, test sends debugee VM signal to quit, waits
 *     for debugee VM exits and exits too with a proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.VirtualMachine.Resume.resume001a
 * @run main/othervm
 *      nsk.jdwp.VirtualMachine.Resume.resume001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

