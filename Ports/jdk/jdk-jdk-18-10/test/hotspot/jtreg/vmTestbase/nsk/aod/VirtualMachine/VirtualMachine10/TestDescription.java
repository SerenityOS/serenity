/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/aod/VirtualMachine/VirtualMachine10.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     This test checks method VirtualMachine.loadAgentPath(String agent).
 *     Test checks following spec clause: "Agent_OnAttach function is invoked even if the agent library was loaded
 *     prior to invoking this method". In this test the same agent library first loaded via VM command line
 *     option 'agentlib:', then it is loaded using method 'System.loadLibrary' and than dynamically attached.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.aod.VirtualMachine.VirtualMachine09.VM09Target
 * @run main/othervm/native
 *      -XX:+UsePerfData
 *      nsk.aod.VirtualMachine.VirtualMachine09.VirtualMachine09
 *      -jdk ${test.jdk}
 *      -javaOpts="-agentlib:VirtualMachine09agent00 -XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.aod.VirtualMachine.VirtualMachine09.VM09Target
 *      -na ${test.nativepath}/VirtualMachine09agent00
 *      -testedMethod loadAgentPath
 *      -arch ${os.family}-${os.simpleArch}
 */

