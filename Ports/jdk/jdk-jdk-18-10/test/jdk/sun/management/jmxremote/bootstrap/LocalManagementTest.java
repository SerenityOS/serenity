/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

/**
 * @test
 * @bug 5016507 6173612 6319776 6342019 6484550 8004926
 * @summary Start a managed VM and test that a management tool can connect
 *          without connection or username/password details.
 *          TestManager will attempt a connection to the address obtained from
 *          both agent properties and jvmstat buffer.
 *
 * @library /test/lib
 * @modules java.management
 *          jdk.attach
 *          jdk.management.agent/jdk.internal.agent
 *
 * @build TestManager TestApplication
 * @run main/othervm/timeout=300 LocalManagementTest
 */
public class LocalManagementTest {
    private static final String TEST_CLASSPATH = System.getProperty("test.class.path");

    public static void main(String[] args) throws Exception {
        int failures = 0;
        for(Method m : LocalManagementTest.class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers()) &&
                m.getName().startsWith("test")) {
                m.setAccessible(true);
                try {
                    System.out.println(m.getName());
                    System.out.println("==========");
                    Boolean rslt = (Boolean)m.invoke(null);
                    if (!rslt) {
                        System.err.println(m.getName() + " failed");
                        failures++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    failures++;
                }
            }
        }
        if (failures > 0) {
            throw new Error("Test failed");
        }
    }

    @SuppressWarnings("unused")
    private static boolean test1() throws Exception {
        return doTest("1", "-Dcom.sun.management.jmxremote");
    }

    /**
     * no args (blank) - manager should attach and start agent
     */
    @SuppressWarnings("unused")
    private static boolean test3() throws Exception {
        return doTest("3", null);
    }

    /**
     * use DNS-only name service
     */
    @SuppressWarnings("unused")
    private static boolean test5() throws Exception {
        return doTest("5", "-Dsun.net.spi.namservice.provider.1=\"dns,sun\"");
    }

    private static boolean doTest(String testId, String arg) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("-XX:+UsePerfData");
        Collections.addAll(args, Utils.getTestJavaOpts());
        args.add("-cp");
        args.add(TEST_CLASSPATH);

        if (arg != null) {
            args.add(arg);
        }
        args.add("TestApplication");
        ProcessBuilder server = ProcessTools.createJavaProcessBuilder(
            args.toArray(new String[args.size()])
        );

        Process serverPrc = null, clientPrc = null;
        try {
            final AtomicReference<String> port = new AtomicReference<>();

            serverPrc = ProcessTools.startProcess(
                "TestApplication(" + testId + ")",
                server,
                (String line) -> {
                    if (line.startsWith("port:")) {
                         port.set(line.split("\\:")[1]);
                    } else if (line.startsWith("waiting")) {
                        return true;
                    }
                    return false;
                }
            );

            System.out.println("Attaching test manager:");
            System.out.println("=========================");
            System.out.println("  PID           : " + serverPrc.pid());
            System.out.println("  shutdown port : " + port.get());

            ProcessBuilder client = ProcessTools.createJavaProcessBuilder(
                "-cp",
                TEST_CLASSPATH,
                "--add-exports", "jdk.management.agent/jdk.internal.agent=ALL-UNNAMED",
                "TestManager",
                String.valueOf(serverPrc.pid()),
                port.get(),
                "true"
            );

            clientPrc = ProcessTools.startProcess(
                "TestManager",
                client,
                (String line) -> line.startsWith("Starting TestManager for PID")
            );

            int clientExitCode = clientPrc.waitFor();
            int serverExitCode = serverPrc.waitFor();
            return clientExitCode == 0 && serverExitCode == 0;
        } finally {
            if (clientPrc != null) {
                System.out.println("Stopping process " + clientPrc);
                clientPrc.destroy();
                clientPrc.waitFor();
            }
            if (serverPrc != null) {
                System.out.println("Stopping process " + serverPrc);
                serverPrc.destroy();
                serverPrc.waitFor();
            }
        }
    }
}
