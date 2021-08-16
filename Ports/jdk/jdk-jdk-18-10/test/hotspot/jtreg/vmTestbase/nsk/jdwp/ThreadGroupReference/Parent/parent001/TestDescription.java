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
 * @summary converted from VM Testbase nsk/jdwp/ThreadGroupReference/Parent/parent001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ThreadGroupReference
 *         command: Parent
 *     Test checks that debugee accepts command and replies
 *     with correct reply packet. Also test check that returned
 *     parent ThreadGroupID for top-level group is equal to zero.
 *     First, test launches debuggee VM using support classes
 *     and connects to it.
 *     Then test queries debugee VM for ThreadGroupReferenceIDs
 *     of all top level thread groups.
 *     Then test sends Parent command for each ThreadGroupReferenceID
 *     and waits for a reply packet.
 *     Test checks if the each received reply packet has proper
 *     structure and extract childs info of the queried thread group.
 *     Also test check that extracted parent ThreadGroupID for
 *     top-level group is equal to zero.
 *     After JDWP command has been tested, test sends debugee VM
 *     signal to quit, waits for debugee exits and exits too
 *     with a proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.ThreadGroupReference.Parent.parent001a
 * @run main/othervm
 *      nsk.jdwp.ThreadGroupReference.Parent.parent001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

