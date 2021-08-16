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
 * @summary converted from VM Testbase nsk/jdwp/ReferenceType/SourceFile/srcfile001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ReferenceType
 *         command: SourceFile
 *     Test checks that debugee accept command and replies
 *     with correct reply packet. Also test checks that
 *     returned source file name for the requested class
 *     is equal to the expected one.
 *     First, test launches debuggee VM using support classes
 *     and connects to it.
 *     Then test queries debugee VM for ReferenceTypeID for
 *     debugee class.
 *     Then test sends SourceFile command with received ReferenceTypeID
 *     as an command argument and waits for a reply packet.
 *     Then test checks if the received reply packet has proper
 *     structure and extracted source file name is equal to
 *     the expected name of the file with debugee class.
 *     After JDWP command has been tested, test sends debugee VM
 *     signal to quit, waits for debugee VM exits and exits too
 *     with a proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.ReferenceType.SourceFile.srcfile001a
 * @run main/othervm
 *      nsk.jdwp.ReferenceType.SourceFile.srcfile001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

