/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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
 * @test
 * @bug 8214892
 * @summary Test that the onjcmd option of the jdwp agent works.
 *
 * @author Ralf Schmelter
 *
 * @library /test/lib
 * @run compile --add-exports java.base/jdk.internal.vm=ALL-UNNAMED -g OnJcmdTest.java
 * @run main/othervm --add-exports java.base/jdk.internal.vm=ALL-UNNAMED -agentlib:jdwp=transport=dt_socket,address=localhost:0,onjcmd=y,server=y OnJcmdTest
 */

import java.lang.reflect.Method;
import java.util.Properties;

import jdk.internal.vm.VMSupport;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class OnJcmdTest {

    private static String getListenerAddress() throws Exception {
        Properties props = VMSupport.getAgentProperties();
        return props.getProperty("sun.jdwp.listenerAddress", null);
    }

    public static void main(String[] args) throws Throwable {
        // First check if we get the expected errors.
        OutputAnalyzer output = ProcessTools.executeTestJvm(
                "-agentlib:jdwp=transport=dt_socket,address=any,onjcmd=y");
        output.shouldContain("Can only use onjcmd with server=y");
        output.shouldHaveExitValue(1);

        output = ProcessTools.executeTestJvm(
                "-agentlib:jdwp=transport=dt_socket,address=any,onjcmd=y,onthrow=a,launch=a");
        output.shouldContain("Cannot combine onjcmd and launch suboptions");
        output.shouldHaveExitValue(1);

        // Make sure debugging is not yet started.
        String prop = getListenerAddress();

        if (prop != null) {
            throw new RuntimeException("Listener address was set to " + prop);
        }

        // Now start it (test that it is OK to do this more than once).
        for (int i = 0; i < 3; ++i) {
            String jcmd = JDKToolFinder.getJDKTool("jcmd");
            output = ProcessTools.executeProcess(jcmd,
                    Long.toString(ProcessTools.getProcessId()),
                    "VM.start_java_debugging");

            String exp_str = i == 0 ? "Debugging has been started." :
                                      "Debugging is already active.";
            output.shouldContain(exp_str);
            output.shouldContain("Transport : dt_socket");
            output.shouldHaveExitValue(0);
        }

        // Now the property should be set, as the jdwp agent waits for a
        // connection.
        long t1 = System.currentTimeMillis();
        long t2 = t1;

        while(t2 - t1 < 4000) {
            prop = getListenerAddress();

            if (prop != null) {
                if (prop.equals("localhost:0")) {
                    throw new RuntimeException("Port was not expanded");
                } else if (!prop.startsWith("dt_socket:")) {
                    throw new RuntimeException("Invalid transport prop " + prop);
                }

                return;
            }

            Thread.sleep(50);
            t2 = System.currentTimeMillis();
        }

        throw new RuntimeException("Debugging backend didn't start");
    }
}
