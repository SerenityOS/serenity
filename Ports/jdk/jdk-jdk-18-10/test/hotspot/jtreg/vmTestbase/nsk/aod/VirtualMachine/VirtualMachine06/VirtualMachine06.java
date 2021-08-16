/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/aod/VirtualMachine/VirtualMachine06.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     This test checks methods VirtualMachine.loadAgent(String agent) and
 *     VirtualMachine.loadAgent(String agent, String options).
 *     Test checks following cases:
 *         - it is possible to pass options to agent using loadAgent(String agent, String options)
 *         - it is possible to specify null options (in this case null is passed to the agentmain)
 *         - if agent throws exception from 'agentmain' VirtualMachine.loadAgent throws AgentInitializationException
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 *
 *
 * @comment compile VM06Agent0[0-3].java to current directory
 * @build nsk.aod.VirtualMachine.VirtualMachine06.VM06Agent00
 *        nsk.aod.VirtualMachine.VirtualMachine06.VM06Agent01
 *        nsk.aod.VirtualMachine.VirtualMachine06.VM06Agent02
 *        nsk.aod.VirtualMachine.VirtualMachine06.VM06Agent03
 * @run driver jdk.test.lib.helpers.ClassFileInstaller
 *      nsk.aod.VirtualMachine.VirtualMachine06.VM06Agent00
 *      nsk.aod.VirtualMachine.VirtualMachine06.VM06Agent01
 *      nsk.aod.VirtualMachine.VirtualMachine06.VM06Agent02
 *      nsk.aod.VirtualMachine.VirtualMachine06.VM06Agent03
 *
 * @comment create VM06Agent0[0-3].jar in current directory
 * @run driver ExecDriver --cmd
 *      ${test.jdk}/bin/jar
 *      -cmf ${test.src}/VM06Agent00.mf
 *      VM06Agent00.jar
 *      nsk/aod/VirtualMachine/VirtualMachine06/VM06Agent00.class
 * @run driver ExecDriver --cmd
 *      ${test.jdk}/bin/jar
 *      -cmf ${test.src}/VM06Agent01.mf
 *      VM06Agent01.jar
 *      nsk/aod/VirtualMachine/VirtualMachine06/VM06Agent01.class
 * @run driver ExecDriver --cmd
 *      ${test.jdk}/bin/jar
 *      -cmf ${test.src}/VM06Agent02.mf
 *      VM06Agent02.jar
 *      nsk/aod/VirtualMachine/VirtualMachine06/VM06Agent02.class
 * @run driver ExecDriver --cmd
 *      ${test.jdk}/bin/jar
 *      -cmf ${test.src}/VM06Agent03.mf
 *      VM06Agent03.jar
 *      nsk/aod/VirtualMachine/VirtualMachine06/VM06Agent03.class
 *
 *
 * @build nsk.share.aod.TargetApplicationWaitingAgents
 * @run main/othervm
 *      -XX:+UsePerfData
 *      nsk.aod.VirtualMachine.VirtualMachine06.VirtualMachine06
 *      -jdk ${test.jdk}
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.share.aod.TargetApplicationWaitingAgents
 *      -ja VM06Agent00.jar,VM06Agent01.jar,VM06Agent02.jar,VM06Agent03.jar
 */

package nsk.aod.VirtualMachine.VirtualMachine06;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.VirtualMachine;
import nsk.share.TestBug;
import nsk.share.aod.AODTestRunner;
import nsk.share.aod.AgentInformation;
import nsk.share.test.TestUtils;

import java.util.List;

/*
 * Test checks following methods:
 *      - VirtualMachine.loadAgent(String)
 *      - VirtualMachine.loadAgent(String, String)
 */
public class VirtualMachine06 extends AODTestRunner {

    public VirtualMachine06(String[] args) {
        super(args);
    }

    public void doTestActions(String targetVMId) throws Throwable {
        // check that all required parameters were passed to the test
        List<AgentInformation> agents = argParser.getAgents();
        if (agents.size() != 4) {
            throw new TestBug("Test requires 4 agents, actually " + agents.size() + " were specified");
        }
        for (AgentInformation agent : agents) {
            if (!agent.jarAgent) {
                throw new TestBug("Non JAR agent was specified");
            }
        }

        VirtualMachine vm = VirtualMachine.attach(targetVMId);

        try {
            AgentInformation agent;

            agent = agents.get(0);
            log.display("Loading '" + agent.pathToAgent + "'");
            // pass null options to agent
            vm.loadAgent(agent.pathToAgent);

            agent = agents.get(1);
            log.display("Loading '" + agent.pathToAgent + "'");
            // pass null options to agent
            vm.loadAgent(agent.pathToAgent, null);

            agent = agents.get(2);
            log.display("Loading '" + agent.pathToAgent + "'");
            // pass non-null options to agent
            vm.loadAgent(agent.pathToAgent, "VirtualMachine06_TestOptions");

            agent = agents.get(3);
            log.display("Loading '" + agent.pathToAgent + "' (this agent throws exception from agentmain)");
            try {
                // check agent throwing exception from agentmain
                vm.loadAgent(agent.pathToAgent);
                TestUtils.testFailed("Expected AgentInitializationException wasn't thrown");
            } catch (AgentInitializationException e) {
                // expected exception
            }
        } finally {
            vm.detach();
        }
    }

    public static void main(String[] args) {
        new VirtualMachine06(args).runTest();
    }
}
