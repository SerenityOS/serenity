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

/*
 * Class contains information about dynamically attached agent
 */
public class AgentInformation {

    // counters used for unique agent names generation

    private static int jarAgentsCounter;

    private static int nativeAgentsCounter;

    public boolean jarAgent;

    public String pathToAgent;

    public String agentOptions;

    public AgentInformation(boolean jarAgent, String pathToAgent, String options, boolean addAgentNameOption) {
        this.jarAgent = jarAgent;
        this.pathToAgent = pathToAgent;
        this.agentOptions = options;

        // add to agent options additional parameter - agent name (it used by nsk.share.aod framework)

        String name;

        if (jarAgent)
            name = "JarAgent-" + jarAgentsCounter++;
        else
            name = "NativeAgent-" + nativeAgentsCounter++;

        if (addAgentNameOption) {
            if (this.agentOptions == null) {
                this.agentOptions = "-agentName=" + name;
            } else {
                this.agentOptions += " -agentName=" + name;
            }
        }
    }

    public AgentInformation(boolean jarAgent, String pathToAgent, String options) {
        this(jarAgent, pathToAgent, options, true);
    }
}
