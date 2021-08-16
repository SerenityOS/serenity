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
 * @summary converted from VM Testbase nsk/aod/VirtualMachine/VirtualMachine08.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     This test checks methods VirtualMachine.loadAgentPath(String agent) and
 *     VirtualMachine.loadAgentPath(String agent, String options).
 *     Test checks following cases:
 *         - it is possible to pass options to agent using loadAgentPath(String agent, String options)
 *         - it is possible to specify null options (in this case zero-length is passed to the Agent_OnAttach)
 *         - if Agent_OnAttach returns error code loadAgentPath throws AgentInitializationException and
 *         AgentInitializationException.returnValue() returns this error code
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.share.aod.TargetApplicationWaitingAgents
 * @run main/othervm/native
 *      -XX:+UsePerfData
 *      nsk.aod.VirtualMachine.VirtualMachine07.VirtualMachine07
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.share.aod.TargetApplicationWaitingAgents
 *      -na ${test.nativepath}/VirtualMachine07agent00,${test.nativepath}/VirtualMachine07agent01,${test.nativepath}/VirtualMachine07agent02,${test.nativepath}/VirtualMachine07agent03
 *      -testedMethod loadAgentPath
 *      -arch ${os.family}-${os.simpleArch}
 */

