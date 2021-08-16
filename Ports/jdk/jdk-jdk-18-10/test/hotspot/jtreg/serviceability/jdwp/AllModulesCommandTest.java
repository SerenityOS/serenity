/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.Set;
import java.util.HashSet;
import static jdk.test.lib.Asserts.assertTrue;

/**
 * @test
 * @summary Tests the modules-related JDWP commands
 * @library /test/lib
 * @modules jdk.jdwp.agent
 * @modules java.base/jdk.internal.misc
 * @requires vm.jvmti
 * @compile AllModulesCommandTestDebuggee.java
 * @run main/othervm AllModulesCommandTest
 */
public class AllModulesCommandTest implements DebuggeeLauncher.Listener {

    private DebuggeeLauncher launcher;
    private JdwpChannel channel;
    private CountDownLatch jdwpLatch = new CountDownLatch(1);
    private Set<String> jdwpModuleNames = new HashSet<>();
    private Set<String> javaModuleNames = new HashSet<>();

    public static void main(String[] args) throws Throwable {
        new AllModulesCommandTest().doTest();
    }

    private void doTest() throws Throwable {
        launcher = new DebuggeeLauncher(this);
        launcher.launchDebuggee();
        // Await till the debuggee sends all the necessary modules info to check against
        // then start the JDWP session
        jdwpLatch.await();
        doJdwp();
    }

    @Override
    public void onDebuggeeModuleInfo(String modName) {
        // The debuggee has sent out info about a loaded module
        javaModuleNames.add(modName);
    }

    @Override
    public void onDebuggeeSendingCompleted() {
        // The debuggee has completed sending all the info
        // We can start the JDWP session
        jdwpLatch.countDown();
    }

    @Override
    public void onDebuggeeError(String message) {
        System.err.println("Debuggee error: '" + message + "'");
        System.exit(1);
    }

    private void doJdwp() throws Exception {
        try {
            // Establish JDWP socket connection
            channel = new JdwpChannel();
            channel.connect(launcher.getJdwpPort());
            // Send out ALLMODULES JDWP command
            // and verify the reply
            JdwpAllModulesReply reply = new JdwpAllModulesCmd().send(channel);
            assertReply(reply);
            for (int i = 0; i < reply.getModulesCount(); ++i) {
                long modId = reply.getModuleId(i);
                // For each module reported by JDWP get its name using the JDWP NAME command
                // and store the reply
                String modName = getModuleName(modId);
                System.out.println("i=" + i + ", modId=" + modId + ", modName=" + modName);
                if (modName != null) { // JDWP reports unnamed modules, ignore them
                    jdwpModuleNames.add(modName);
                }
                // Assert the CLASSLOADER commands
                assertClassLoader(modId, modName);
            }

            System.out.println("Module names reported by JDWP: " + Arrays.toString(jdwpModuleNames.toArray()));
            System.out.println("Module names reported by Java: " + Arrays.toString(javaModuleNames.toArray()));

            // Modules reported by the JDWP should be the same as reported by the Java API
            if (!jdwpModuleNames.equals(javaModuleNames)) {
                throw new RuntimeException("Modules info reported by Java API differs from that reported by JDWP.");
            } else {
                System.out.println("Test passed!");
            }

        } finally {
            launcher.terminateDebuggee();
            try {
                new JdwpExitCmd(0).send(channel);
                channel.disconnect();
            } catch (Exception x) {
            }
        }
    }

    private String getModuleName(long modId) throws IOException {
        JdwpModNameReply reply = new JdwpModNameCmd(modId).send(channel);
        assertReply(reply);
        return reply.getModuleName();
    }

    private void assertReply(JdwpReply reply) {
        // Simple assert for any JDWP reply
        if (reply.getErrorCode() != 0) {
            throw new RuntimeException("Unexpected reply error code " + reply.getErrorCode() + " for reply " + reply);
        }
    }

    private void assertClassLoader(long modId, String modName) throws IOException {
        // Verify that the module classloader id is valid
        JdwpClassLoaderReply reply = new JdwpClassLoaderCmd(modId).send(channel);
        assertReply(reply);
        long moduleClassLoader = reply.getClassLoaderId();
        assertTrue(moduleClassLoader >= 0, "bad classloader refId " + moduleClassLoader + " for module '" + modName + "', moduleId=" + modId);

        String clsModName = getModuleName(modId);
        if ("java.base".equals(clsModName)) {
            // For the java.base module, because there will be some loaded classes, we can verify
            // that some of the loaded classes do report the java.base module as the module they belong to
            assertGetModule(moduleClassLoader, modId);
        }
    }

    private void assertGetModule(long moduleClassLoader, long modId) throws IOException {
        // Get all the visible classes for the module classloader
        JdwpVisibleClassesReply visibleClasses = new JdwpVisibleClassesCmd(moduleClassLoader).send(channel);
        assertReply(visibleClasses);

        boolean moduleFound = false;
        for (long clsId : visibleClasses.getVisibleClasses()) {
            // For each visible class get the module the class belongs to
            JdwpModuleReply modReply = new JdwpModuleCmd(clsId).send(channel);
            assertReply(modReply);
            long clsModId = modReply.getModuleId();

            // At least one of the visible classes should belong to our module
            if (modId == clsModId) {
                moduleFound = true;
                break;
            }
        }
        assertTrue(moduleFound, "None of the visible classes for the classloader of the module " + getModuleName(modId) + " reports the module as its own");
    }

}
