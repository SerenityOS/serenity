/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 *  @test
 *  @bug 5013605
 *  @summary Localize log messages from the management agents
 *  @modules jdk.management.agent/jdk.internal.agent
 *
 *  @author Tim Bell
 */
import jdk.internal.agent.Agent;

public class AgentCheckTest {

    public static void main(String[] args){
        String [][] testStrings = {
            {"agent.err.error", "", ""},
            {"jmxremote.ConnectorBootstrap.starting", "", ""},
            {"jmxremote.ConnectorBootstrap.noAuthentication", "", ""},
            {"jmxremote.ConnectorBootstrap.ready", "Phony JMXServiceURL", ""},
            {"jmxremote.ConnectorBootstrap.password.readonly", "Phony passwordFileName", ""},
        };

        boolean pass = true;
        System.out.println("Start...");
        for (int ii = 0; ii < testStrings.length; ii++) {
            String key = testStrings[ii][0];
            String p1 = testStrings[ii][1];
            String p2 = testStrings[ii][2];
            String ss = Agent.getText(key, p1, p2);
            if (ss.startsWith("missing resource key")) {
                pass = false;
                System.out.println("    lookup failed for key = " + key);
            }
        }
        if (!pass) {
            throw new Error ("Resource lookup(s) failed; Test failed");
        }
        System.out.println("...Finished.");
    }
}
