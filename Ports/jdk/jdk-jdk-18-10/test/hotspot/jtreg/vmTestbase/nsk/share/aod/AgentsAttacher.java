/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.aod;

import nsk.share.*;
import java.io.IOException;
import java.util.*;
import com.sun.tools.attach.*;

/*
 * This class loads java and native agents in the running VM using Attach API
 * (API from package com.sun.tools.attach).
 */
public class AgentsAttacher {

    protected String targetVMId;

    protected List<AgentInformation> agents;

    protected Log log;

    public AgentsAttacher(String targetVMId, List<AgentInformation> agents, Log log) {
        this.targetVMId = targetVMId;
        this.agents = agents;
        this.log = log;
    }

    public void attachAgents() {
        VirtualMachine vm = null;

        try {
            log.display("Trying to get VirtualMachine object");
            vm = VirtualMachine.attach(targetVMId);
        } catch (AttachNotSupportedException e) {
            log.complain("Unexpected AttachNotSupportedException during VirtualMachine.attach: " + e);
            e.printStackTrace(log.getOutStream());
        } catch (IOException e) {
            log.complain("Unexpected IOException during VirtualMachine.attach: " + e);
            e.printStackTrace(log.getOutStream());
        }

        if (vm == null) {
            failed("Unable to create VirtualMachine object");
        }

        log.display("VirtualMachine was created: " + vm);

        try {
            for (AgentInformation agentInfo : agents) {
                tryToLoadAgent(vm, agentInfo.pathToAgent, agentInfo.agentOptions, agentInfo.jarAgent);
            }
        } finally {
            try {
                log.display("Detaching from the VM '" + vm + "'");
                vm.detach();
            } catch (IOException e) {
                failed("Unexpected IOException during detaching: " + e, e);
            }
        }
    }

    protected void tryToLoadAgent(VirtualMachine vm, String agent, String agentOptions, boolean jarAgent) {
        boolean agentLoaded = false;

        Throwable failureCause = null;

        try {
            if (jarAgent) {
                log.display("Trying to load jar agent: '" + agent + "' (agent options: '" + agentOptions + "')");
                vm.loadAgent(agent, agentOptions);
            } else {
                log.display("Trying to load native agent: '" + agent + "' (agent options: '" + agentOptions + "')");
                vm.loadAgentLibrary(agent, agentOptions);
            }
            log.display("Agent was loaded");
            agentLoaded = true;
        } catch (AgentLoadException e) {
            failureCause = e;
            log.complain("Unexpected AgentLoadException during agent loading: " + e);
            if (jarAgent) {
                log.complain("(probably the agent does not exist, or cannot be started in the manner specified in "
                        + "the java.lang.instrument specification)");
            } else {
                log.complain("(probably agent library does not exist, or cannot be loaded for another reason)");
            }
            e.printStackTrace(log.getOutStream());
        } catch (AgentInitializationException e) {
            failureCause = e;
            log.complain("Unexpected AgentInitializationException during agent loading: " + e);
            if (jarAgent) {
                log.complain("(agentmain have thrown an exception)");
            } else {
                log.complain("Agent_OnAttach function returned an error: " + e.returnValue());
            }
            e.printStackTrace(log.getOutStream());
        } catch (IOException e) {
            failureCause = e;
            log.complain("Unexpected IOException during agent loading: " + e);
            e.printStackTrace(log.getOutStream());
        }

        if (!agentLoaded) {
            if (failureCause != null)
                failed("Couldn't attach agent to the target application", failureCause);
            else
                failed("Couldn't attach agent to the target application");
        }
    }

    private void failed(String errorMessage) {
        throw new Failure(errorMessage);
    }

    private void failed(String errorMessage, Throwable t) {
        throw new Failure(errorMessage, t);
    }
}
