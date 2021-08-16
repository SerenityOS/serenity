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
 * @summary converted from VM Testbase nsk/aod/VirtualMachine/VirtualMachine07.
 * VM Testbase keywords: [feature_282, jdk]
 * VM Testbase readme:
 * Description :
 *     Test checks work of Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework.
 *     This test checks methods VirtualMachine.loadAgentLibrary(String agent) and
 *     VirtualMachine.loadAgentLibrary(String agent, String options).
 *     Test checks following cases:
 *         - it is possible to pass options to agent using loadAgentLibrary(String agent, String options)
 *         - it is possible to specify null options (in this case zero-length is passed to the Agent_OnAttach)
 *         - if Agent_OnAttach returns error code loadAgentLibrary throws AgentInitializationException and
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
 *      -na VirtualMachine07agent00,VirtualMachine07agent01,VirtualMachine07agent02,VirtualMachine07agent03
 *      -testedMethod loadAgentLibrary
 */

package nsk.aod.VirtualMachine.VirtualMachine07;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.VirtualMachine;
import nsk.share.TestBug;
import nsk.share.aod.AODRunnerArgParser;
import nsk.share.aod.AODTestRunner;
import nsk.share.aod.AgentInformation;
import nsk.share.test.TestUtils;

import java.util.List;

/*
 * Test is written to test following methods:
 *      - VirtualMachine.loadAgentLibrary
 *      - VirtualMachine.loadAgentPath
 *
 *(method to test is specified via command line parameter 'testedMethod')
 */
public class VirtualMachine07 extends AODTestRunner {
    private static class ArgParser extends AODRunnerArgParser {
        private static final String TESTED_METHOD_OPT = "testedMethod";

        ArgParser(String[] args) {
            super(args);
        }

        protected boolean checkOption(String option, String value) {
            if (super.checkOption(option, value))
                return true;

            if (option.equals(TESTED_METHOD_OPT)) {
                if ("loadAgentLibrary".equals(value) || "loadAgentPath".equals(value)) {
                    return true;
                } else {
                    throw new TestBug("Unexpected value of '" + TESTED_METHOD_OPT + "': " + value);
                }
            }

            return false;
        }

        protected void checkOptions() {
            super.checkOptions();

            // if test loadAgentPath parameter arch is needed
            if (!testLoadAgentLibrary()) {
                if (!options.containsKey("arch")) {
                    throw new TestBug("Option 'arch' wasn't specified");
                }
            }
        }

        boolean testLoadAgentLibrary() {
            return options.getProperty(TESTED_METHOD_OPT).equals("loadAgentLibrary");
        }
    }

    public VirtualMachine07(String[] args) {
        super(args);
    }

    /*
     * When test method loadAgentPath platform specific agent name should be
     * created (lib<agent>.so for unix, lib<agent>.dylib for macosx and
     * <agent>.dll for windows)
     */
    protected String expandAgentPath(String path) {
        int index = path.lastIndexOf('/');
        if (index < 0)
            throw new TestBug("Unexpected agent library name format");

        String dir = path.substring(0, index);
        String libName = path.substring(index + 1);

        if (argParser.getArch().startsWith("windows")) {
            return dir + "/" + libName + ".dll";
        } else if (argParser.getArch().startsWith("mac")) {
            return dir + "/" + "lib" + libName + ".dylib";
        } else {
            return dir + "/" + "lib" + libName + ".so";
        }
    }

    protected AODRunnerArgParser createArgParser(String[] args) {
        return new ArgParser(args);
    }

    protected void loadAgent(VirtualMachine vm, String agent) throws Throwable {
        boolean testLoadAgentLibrary = ((ArgParser) argParser).testLoadAgentLibrary();

        if (testLoadAgentLibrary) {
            log.display("Test method VirtualMachine.loadAgentLibrary");
        } else {
            log.display("Test method VirtualMachine.loadAgentPath");
        }

        if (testLoadAgentLibrary) {
            log.display("Loading '" + agent + "'");
            vm.loadAgentLibrary(agent);
        } else {
            String expandedName = (agent == null ? null : expandAgentPath(agent));
            log.display("Loading '" + expandedName + "'");
            vm.loadAgentPath(expandedName);
        }
    }

    protected void loadAgent(VirtualMachine vm, String agent, String options) throws Throwable {
        boolean testLoadAgentLibrary = ((ArgParser) argParser).testLoadAgentLibrary();

        if (testLoadAgentLibrary) {
            log.display("Test method VirtualMachine.loadAgentLibrary");
        } else {
            log.display("Test method VirtualMachine.loadAgentPath");
        }

        if (testLoadAgentLibrary) {
            log.display("Loading '" + agent + "'");
            vm.loadAgentLibrary(agent, options);
        } else {
            String expandedName = (agent == null ? null : expandAgentPath(agent));
            log.display("Loading '" + expandedName + "'");
            vm.loadAgentPath(expandedName, options);
        }
    }

    public void doTestActions(String targetVMId) throws Throwable {
        // check that all required parameters were passed to the test
        List<AgentInformation> agents = argParser.getAgents();
        if (agents.size() != 4) {
            throw new TestBug("Test requires 4 agents, actually " + agents.size() + " were specified");
        }
        for (AgentInformation agent : agents) {
            if (agent.jarAgent) {
                throw new TestBug("Non native agent was specified");
            }
        }

        VirtualMachine vm = VirtualMachine.attach(targetVMId);

        try {
            AgentInformation agent;

            agent = agents.get(0);
            loadAgent(vm, agent.pathToAgent);

            agent = agents.get(1);
            loadAgent(vm, agent.pathToAgent, null);

            agent = agents.get(2);
            loadAgent(vm, agent.pathToAgent, "VirtualMachine_TestOptions");

            agent = agents.get(3);
            log.display("Loading '" + agent.pathToAgent + "' (this agent fails to initialize)");
            try {
                loadAgent(vm, agent.pathToAgent);

                TestUtils.testFailed("Expected AgentInitializationException wasn't thrown");
            } catch (AgentInitializationException e) {
                log.display("Expected AgentInitializationException was caught");
                log.display("AgentInitializationException.returnValue(): " + e.returnValue());
                TestUtils.assertEquals(e.returnValue(), 10,
                        "AgentInitializationException.returnValue() returns unexpected value: " + e.returnValue() + ", expected value is 10");
            }
        } finally {
            vm.detach();
        }
    }

    public static void main(String[] args) {
        new VirtualMachine07(args).runTest();
    }
}
