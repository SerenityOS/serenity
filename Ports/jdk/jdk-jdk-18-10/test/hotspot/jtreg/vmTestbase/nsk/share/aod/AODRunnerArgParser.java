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

package nsk.share.aod;

import nsk.share.ArgumentParser;
import nsk.share.TestBug;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AODRunnerArgParser extends ArgumentParser {
    private static final String JAR_AGENT_PARAM = "ja";
    private static final String NATIVE_AGENT_PARAM = "na";
    private static final String TARGET_APP_PARAM = "target";
    private static final String JAVA_OPTS_PARAM = "javaOpts";
    private static final String TESTED_JDK_PARAM = "jdk";
    private static final Set<String> SUPPORTED_OPTIONS = Set.of(
            JAR_AGENT_PARAM,
            NATIVE_AGENT_PARAM,
            TARGET_APP_PARAM,
            JAVA_OPTS_PARAM,
            TESTED_JDK_PARAM);

    private List<AgentInformation> agents;

    public AODRunnerArgParser(String[] args) {
        super(args);
    }

    protected boolean checkOption(String option, String value) {
        if (super.checkOption(option, value)) {
            return true;
        }

        if (!SUPPORTED_OPTIONS.contains(option)) {
            return false;
        }

        if (option.equals(JAR_AGENT_PARAM)) {
            addAgentInfo(true, value);
        }

        if (option.equals(NATIVE_AGENT_PARAM)) {
            addAgentInfo(false, value);
        }

        return true;
    }

    protected void checkOptions() {
        if (agents == null) {
            agents = new ArrayList<>();
        }
    }

    private void addAgentInfo(boolean jarAgent, String unsplittedAgentsString) {
        if (agents == null) {
            agents = new ArrayList<>();
        }

        String[] agentStrings;

        if (unsplittedAgentsString.contains(",")) {
            agentStrings = unsplittedAgentsString.split(",");
        } else {
            agentStrings = new String[]{ unsplittedAgentsString };
        }

        for (String agentString : agentStrings) {
            int index = agentString.indexOf('=');

            if (index > 0) {
                String pathToAgent = agentString.substring(0, index);
                String options = agentString.substring(index + 1);
                agents.add(new AgentInformation(jarAgent, pathToAgent, options));
            } else {
                agents.add(new AgentInformation(jarAgent, agentString, null));
            }
        }
    }

    public String getTargetApp() {
        if (!options.containsKey(TARGET_APP_PARAM)) {
            throw new TestBug("Target application isn't specified");
        }

        return options.getProperty(TARGET_APP_PARAM);
    }

    public String getTestedJDK() {
        if (!options.containsKey(TESTED_JDK_PARAM)) {
            throw new TestBug("Tested JDK isn't specified");
        }

        return options.getProperty(TESTED_JDK_PARAM);
    }

    public String getJavaOpts() {
        var value = options.getProperty(JAVA_OPTS_PARAM, "");
        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.trim();
    }

    public List<AgentInformation> getAgents() {
        return agents;
    }
}
