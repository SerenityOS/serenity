/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.net.BindException;
import java.util.Properties;
import java.util.function.Predicate;
import static org.testng.Assert.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import jdk.test.lib.process.ProcessTools;


/**
 * @test
 * @bug 8075926
 * @key intermittent
 * @summary Makes sure that the current management agent status is reflected
 *          in the related performance counters.
 *
 * @library /test/lib
 *
 * @build PortAllocator TestApp ManagementAgentJcmd
 * @run testng/othervm -XX:+UsePerfData JMXStatusPerfCountersTest
 */
public class JMXStatusPerfCountersTest {
    private final static String TEST_APP_NAME = "TestApp";

    private final static String REMOTE_STATUS_KEY = "sun.management.JMXConnectorServer.remote.enabled";

    private static ProcessBuilder testAppPb;
    private Process testApp;

    private ManagementAgentJcmd jcmd;

    @BeforeClass
    public static void setupClass() throws Exception {
        testAppPb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UsePerfData",
            "-cp", System.getProperty("test.class.path"),
            TEST_APP_NAME
        );
    }

    @BeforeTest
    public void setup() {
        jcmd = new ManagementAgentJcmd(TEST_APP_NAME, false);
    }

    @BeforeMethod
    public void startTestApp() throws Exception {
        testApp = ProcessTools.startProcess(
            TEST_APP_NAME, testAppPb,
            (Predicate<String>)l->l.trim().equals("main enter")
        );
    }

    @AfterMethod
    public void stopTestApp() throws Exception {
        testApp.getOutputStream().write(1);
        testApp.getOutputStream().flush();
        testApp.waitFor();
        testApp = null;
    }

    /**
     * The 'sun.management.JMXConnectorServer.remote.enabled' counter must not be
     * exported if the remote agent is not enabled.
     * @throws Exception
     */
    @Test
    public void testNotInitializedRemote() throws Exception {
        assertFalse(
            getCounters().containsKey(REMOTE_STATUS_KEY),
            "Unexpected occurrence of " + REMOTE_STATUS_KEY + " in perf counters"
        );
    }

    /**
     * After enabling the remote agent the 'sun.management.JMXConnectorServer.remote.enabled'
     * counter will be exported with value of '0' - corresponding to the actual
     * version of the associated remote connector perf counters.
     * @throws Exception
     */
    @Test
    public void testRemoteEnabled() throws Exception {
        while (true) {
            try {
                int[] ports = PortAllocator.allocatePorts(1);
                jcmd.start(
                    "jmxremote.port=" + ports[0],
                    "jmxremote.authenticate=false",
                    "jmxremote.ssl=false"
                );
                String v = getCounters().getProperty(REMOTE_STATUS_KEY);
                assertNotNull(v);
                assertEquals("0", v);
                return;
            } catch (BindException e) {
                System.out.println("Failed to allocate ports. Retrying ...");
            }
        }
    }

    /**
     * After disabling the remote agent the value of 'sun.management.JMXConnectorServer.remote.enabled'
     * counter will become '-1'.
     * @throws Exception
     */
    @Test
    public void testRemoteDisabled() throws Exception {
        while (true) {
            try {
                int[] ports = PortAllocator.allocatePorts(1);
                jcmd.start(
                    "jmxremote.port=" + ports[0],
                    "jmxremote.authenticate=false",
                    "jmxremote.ssl=false"
                );
                jcmd.stop();
                String v = getCounters().getProperty(REMOTE_STATUS_KEY);
                assertNotNull(v);
                assertEquals("-1", v);
                return;
            } catch (BindException e) {
                System.out.println("Failed to allocate ports. Retrying ...");
            }
        }
    }

    /**
     * Each subsequent re-enablement of the remote agent must keep the value of
     * 'sun.management.JMXConnectorServer.remote.enabled' counter in sync with
     * the actual version of the associated remote connector perf counters.
     * @throws Exception
     */
    @Test
    public void testRemoteReEnabled() throws Exception {
        while (true) {
            try {
                int[] ports = PortAllocator.allocatePorts(1);
                jcmd.start(
                    "jmxremote.port=" + ports[0],
                    "jmxremote.authenticate=false",
                    "jmxremote.ssl=false"
                );
                jcmd.stop();
                jcmd.start(
                    "jmxremote.port=" + ports[0],
                    "jmxremote.authenticate=false",
                    "jmxremote.ssl=false"
                );

                String v = getCounters().getProperty(REMOTE_STATUS_KEY);
                assertNotNull(v);
                assertEquals("1", v);
                return;
            } catch (BindException e) {
                System.out.println("Failed to allocate ports. Retrying ...");
            }
        }
    }

    private Properties getCounters() throws IOException, InterruptedException {
        return jcmd.perfCounters("sun\\.management\\.JMXConnectorServer\\..*");
    }
}
