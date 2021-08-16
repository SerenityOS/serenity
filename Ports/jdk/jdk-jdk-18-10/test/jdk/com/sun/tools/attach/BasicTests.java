/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;

import jdk.test.lib.thread.ProcessThread;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/*
 * @test
 * @bug 6173612 6273707 6277253 6335921 6348630 6342019 6381757
 * @key intermittent
 * @summary Basic unit tests for the VM attach mechanism. This test will perform
 * a number of basic attach tests.
 *
 * @library /test/lib
 * @modules java.instrument
 *          jdk.attach
 *          jdk.jartool/sun.tools.jar
 *
 * @run build Agent BadAgent RedefineAgent Application RedefineDummy RunnerUtil
 * @run main BasicTests
 */
public class BasicTests {

    /*
     * The actual test is in the nested class TestMain.
     * The responsibility of this class is to:
     * 1. Build all needed jars.
     * 2. Start the Application class in a separate process.
     * 3. Find the pid and shutdown port of the running Application.
     * 4. Launches the tests in nested class TestMain that will attach to the Application.
     * 5. Shut down the Application.
     */
    public static void main(String args[]) throws Throwable {
        ProcessThread processThread = null;
        try {
            buildJars();
            processThread = RunnerUtil.startApplication();
            runTests(processThread.getPid());
        } catch (Throwable t) {
            System.out.println("TestBasic got unexpected exception: " + t);
            t.printStackTrace();
            throw t;
        } finally {
            // Make sure the Application process is stopped.
            RunnerUtil.stopApplication(processThread);
        }
    }

    /**
     * Runs the actual tests in nested class TestMain.
     * The reason for running the tests in a separate process
     * is that we need to modify the class path.
     */
    private static void runTests(long pid) throws Throwable {
        final String sep = File.separator;

        // Need to add jdk/lib/tools.jar to classpath.
        String classpath =
            System.getProperty("test.class.path", "");
        String testClassDir = System.getProperty("test.classes", "") + sep;

        // Argumenta : -classpath cp BasicTests$TestMain pid agent badagent redefineagent
        String[] args = {
            "-classpath",
            classpath,
            "BasicTests$TestMain",
            Long.toString(pid),
            testClassDir + "Agent.jar",
            testClassDir + "BadAgent.jar",
            testClassDir + "RedefineAgent.jar" };
        OutputAnalyzer output = ProcessTools.executeTestJvm(args);
        output.shouldHaveExitValue(0);
    }

    /**
     * Will build all jars needed by the tests.
     */
    private static void buildJars() throws Throwable {
        String[] jars = {"Agent", "BadAgent", "RedefineAgent", "Application" };
        for (String jar : jars) {
            buildJar(jar);
        }
    }

    /**
     * Will build a jar with the given name.
     * Class file and manifest must already exist.
     * @param jarName Name of the jar.
     */
    private static void buildJar(String jarName) throws Throwable {
        String testClasses = System.getProperty("test.classes", "?");
        String testSrc = System.getProperty("test.src", "?");
        String jar = String.format("%s/%s.jar", testClasses, jarName);
        String manifest = String.format("%s/%s.mf", testSrc, jarName.toLowerCase());
        String clazz = String.format("%s.class", jarName);

        // Arguments to the jar command has this format:
        // "-cfm TESTCLASSES/Agent.jar TESTSRC/agent.mf -C TESTCLASSES Agent.class"
        RunnerUtil.createJar("-cfm", jar, manifest, "-C", testClasses, clazz);
    }

    /**
     * This is the actual test. It will attach to the running Application
     * and perform a number of basic attach tests.
     */
    public static class TestMain {
        public static void main(String args[]) throws Exception {
            String pid = args[0];
            String agent = args[1];
            String badagent = args[2];
            String redefineagent = args[3];

            System.out.println(" - Attaching to application ...");
            VirtualMachine vm = VirtualMachine.attach(pid);

            // Test 1 - read the system properties from the target VM and
            // check that property is set
            System.out.println(" - Test: system properties in target VM");
            Properties props = vm.getSystemProperties();
            String value = props.getProperty("attach.test");
            if (value == null || !value.equals("true")) {
                throw new RuntimeException("attach.test property not set");
            }
            System.out.println(" - attach.test property set as expected");

            // Test 1a - read the agent properties from the target VM.
            // By default, the agent property contains "sun.java.command",
            // "sun.jvm.flags", and "sun.jvm.args".
            // Just sanity check - make sure not empty.
            System.out.println(" - Test: agent properties in target VM");
            props = vm.getAgentProperties();
            if (props == null || props.size() == 0) {
                throw new RuntimeException("Agent properties is empty");
            }
            System.out.println(" - agent properties non-empty as expected");

            // Test 2 - attempt to load an agent that does not exist
            System.out.println(" - Test: Load an agent that does not exist");
            try {
                vm.loadAgent("SilverBullet.jar");
            } catch (AgentLoadException x) {
                System.out.println(" - AgentLoadException thrown as expected!");
            }

            // Test 3 - load an "bad" agent (agentmain throws an exception)
            System.out.println(" - Test: Load a bad agent");
            System.out.println("INFO: This test will cause error messages "
                + "to appear in the application log about SilverBullet.jar "
                + "not being found and an agent failing to start.");
            try {
                vm.loadAgent(badagent);
                throw new RuntimeException(
                    "AgentInitializationException not thrown as expected!");
            } catch (AgentInitializationException x) {
                System.out.println(
                    " - AgentInitializationException thrown as expected!");
            }

            // Test 4 - detach from the VM and attempt a load (should throw IOE)
            System.out.println(" - Test: Detach from VM");
            System.out.println("INFO: This test will cause error messages "
                + "to appear in the application log about a BadAgent including "
                + "a RuntimeException and an InvocationTargetException.");
            vm.detach();
            try {
                vm.loadAgent(agent);
                throw new RuntimeException("loadAgent did not throw an exception!!");
            } catch (IOException ioe) {
                System.out.println(" - IOException as expected");
            }

            // Test 5 - functional "end-to-end" test.
            // Create a listener socket. Load Agent.jar into the target VM passing
            // it the port number of our listener. When agent loads it should connect
            // back to the tool.

            System.out.println(" - Re-attaching to application ...");
            vm = VirtualMachine.attach(pid);

            System.out.println(" - Test: End-to-end connection with agent");

            ServerSocket ss = new ServerSocket(0);
            int port = ss.getLocalPort();

            System.out.println(" - Loading Agent.jar into target VM ...");
            vm.loadAgent(agent, Integer.toString(port));

            System.out.println(" - Waiting for agent to connect back to tool ...");
            Socket s = ss.accept();
            System.out.println(" - Connected to agent.");

            // Test 5b - functional "end-to-end" test.
            // Now with an agent that does redefine.

            System.out.println(" - Re-attaching to application ...");
            vm = VirtualMachine.attach(pid);

            System.out.println(" - Test: End-to-end connection with RedefineAgent");

            ServerSocket ss2 = new ServerSocket(0);
            int port2 = ss2.getLocalPort();

            System.out.println(" - Loading RedefineAgent.jar into target VM ...");
            vm.loadAgent(redefineagent, Integer.toString(port2));

            System.out.println(" - Waiting for RedefineAgent to connect back to tool ...");
            Socket s2 = ss2.accept();
            System.out.println(" - Connected to RedefineAgent.");

            // Test 6 - list method should list the target VM
            System.out.println(" - Test: VirtualMachine.list");
            List<VirtualMachineDescriptor> l = VirtualMachine.list();
            boolean found = false;
            for (VirtualMachineDescriptor vmd: l) {
                if (vmd.id().equals(pid)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                System.out.println(" - " + pid + " found.");
            } else {
                throw new RuntimeException(pid + " not found in VM list");
            }

            // test 7 - basic hashCode/equals tests
            System.out.println(" - Test: hashCode/equals");

            VirtualMachine vm1 = VirtualMachine.attach(pid);
            VirtualMachine vm2 = VirtualMachine.attach(pid);
            if (!vm1.equals(vm2)) {
                throw new RuntimeException("virtual machines are not equal");
            }
            if (vm.hashCode() != vm.hashCode()) {
                throw new RuntimeException("virtual machine hashCodes not equal");
            }
            System.out.println(" - hashCode/equals okay");

            // ---
            System.out.println(" - Cleaning up...");
            s.close();
            ss.close();
            s2.close();
            ss2.close();
        }
    }
}
