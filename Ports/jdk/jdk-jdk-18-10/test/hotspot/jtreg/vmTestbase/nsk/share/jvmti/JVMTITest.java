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
package nsk.share.jvmti;

import java.util.*;
import nsk.share.Log;
import nsk.share.TestBug;
import nsk.share.aod.*;

public class JVMTITest {

    public static final String dynamicAttachFlag = "-dynamicAgentAttach";

    public static String[] commonInit(String[] args) {
        List<String> stdArgs = new ArrayList<String>();

        for (String arg : args) {
            if (arg.equals(dynamicAttachFlag)) {
                attachAgent(args);
                break;
            } else {
                stdArgs.add(arg);
            }
        }

        return stdArgs.toArray(new String[stdArgs.size()]);
    }

    static void attachAgent(String[] args) {
        final String agentPrefix = "-agentlib:";

        String jdkPath = null;

        List<AgentInformation> agents = new ArrayList<AgentInformation>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith(agentPrefix)) {
                String agentString = args[i].substring(agentPrefix.length());

                String agentLibName;
                String agentOpts;

                int index = agentString.indexOf('=');
                if (index < 0) {
                    agentLibName = agentString;
                    agentOpts = null;
                } else {
                    agentLibName = agentString.substring(0, index);
                    agentOpts = agentString.substring(index + 1);
                }

                AgentInformation agentInfo = new AgentInformation(false, agentLibName, agentOpts, false);
                agents.add(agentInfo);
            } else if (args[i].equals("-jdk")) {
                jdkPath = args[i + 1];
                i++;
            }
        }

        if (jdkPath == null) {
            throw new TestBug("Dynamic attach mode error: JDK isn't specified");
        }

        if (agents.size() == 0) {
            throw new TestBug("Dynamic attach mode error: agents to attach aren't specified");
        }

        AgentsAttacher attacher = new AgentsAttacher(Utils.findCurrentVMIdUsingJPS(jdkPath),
                agents,
                new Log(System.out, true));
        attacher.attachAgents();
    }
}
