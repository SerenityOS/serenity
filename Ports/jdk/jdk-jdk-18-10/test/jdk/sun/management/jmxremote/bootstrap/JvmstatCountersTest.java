/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4981215
 * @summary Tests that the jvmstat counters published by the out-of-the-box
 *          management agent for the JMX connection details are correct.
 * @author Luis-Miguel Alventosa
 *
 * @modules java.management
 *          jdk.attach
 *          jdk.management.agent/jdk.internal.agent
 *
 * @run clean JvmstatCountersTest
 * @run build JvmstatCountersTest
 * @run main/othervm/timeout=600 -XX:+UsePerfData JvmstatCountersTest 1
 * @run main/othervm/timeout=600 -XX:+UsePerfData -Dcom.sun.management.jmxremote JvmstatCountersTest 2
 * @run main/othervm/timeout=600 -XX:+UsePerfData -Dcom.sun.management.jmxremote.port=0 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false JvmstatCountersTest 3
 * @run main/othervm/timeout=600 -XX:+UsePerfData -Djdk.attach.allowAttachSelf JvmstatCountersTest 4
 */

import java.io.*;
import java.lang.management.*;
import java.util.*;
import javax.management.*;
import javax.management.remote.*;
import com.sun.tools.attach.*;
import jdk.internal.agent.ConnectorAddressLink;

public class JvmstatCountersTest {

    public static void checkAddress(String address) throws IOException {
        System.out.println("Address = " + address);
        JMXServiceURL url = new JMXServiceURL(address);
        JMXConnector jmxc = JMXConnectorFactory.connect(url);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        System.out.println("MBean Count = " + mbsc.getMBeanCount());
    }

    public static void checkKey(Map<String, String> data, int index,
            String key, String expectedValue) throws Exception {
        String counter = "sun.management.JMXConnectorServer." + index + "." + key;
        if (!data.containsKey(counter)) {
            System.out.println("Test FAILED! Missing counter " + counter);
            throw new IllegalArgumentException("Test case failed");
        }
        String value = data.get(counter);
        if (key.equals("remoteAddress")) {
            checkAddress(value);
        } else if (!expectedValue.equals(value)) {
            System.out.println("Test FAILED! Invalid counter " +
                    counter + "=" + value);
            throw new IllegalArgumentException("Test case failed");
        }
        System.out.println("OK: " + counter + "=" + value);
    }

    public static void main(String args[]) throws Exception {
        String localAddress = ConnectorAddressLink.importFrom(0);
        Map<String, String> remoteData = ConnectorAddressLink.importRemoteFrom(0);
        final int testCase = Integer.parseInt(args[0]);
        switch (testCase) {
            case 1:
                if (localAddress == null && remoteData.isEmpty()) {
                    System.out.println("Test PASSED! The OOTB management " +
                            "agent didn't publish any jvmstat counter.");
                } else {
                    System.out.println("Test FAILED! The OOTB management " +
                            "agent unexpectedly published jvmstat counters.");
                    throw new IllegalArgumentException("Test case 1 failed");
                }
                break;
            case 2:
                if (localAddress == null) {
                    System.out.println("Test FAILED! The OOTB management " +
                            "agent didn't publish the local connector.");
                    throw new IllegalArgumentException("Test case 2 failed");
                }
                checkAddress(localAddress);
                if (!remoteData.isEmpty()) {
                    System.out.println("Test FAILED! The OOTB management " +
                            "agent shouldn't publish the remote connector.");
                    throw new IllegalArgumentException("Test case 2 failed");
                }
                System.out.println("Test PASSED! The OOTB management " +
                        "agent only publishes the local connector through " +
                        "a jvmstat counter.");
                break;
            case 3:
                if (localAddress == null) {
                    System.out.println("Test FAILED! The OOTB management " +
                            "agent didn't publish the local connector.");
                    throw new IllegalArgumentException("Test case 3 failed");
                }
                checkAddress(localAddress);
                if (remoteData.isEmpty()) {
                    System.out.println("Test FAILED! The OOTB management " +
                            "agent didnn't publish the remote connector.");
                    throw new IllegalArgumentException("Test case 3 failed");
                }
                for (String key : remoteData.keySet()) {
                    if (!isKeyAcceptable(key)) {
                        System.out.println("Test FAILED! The OOTB management " +
                                "agent shouldn't publish anything which isn't " +
                                "related to the remote connector (" + key + ").");
                        throw new IllegalArgumentException("Test case 3 failed");
                    }
                }
                checkKey(remoteData, 0, "remoteAddress", null);
                checkKey(remoteData, 0, "authenticate", "false");
                checkKey(remoteData, 0, "ssl", "false");
                checkKey(remoteData, 0, "sslRegistry", "false");
                checkKey(remoteData, 0, "sslNeedClientAuth", "false");
                System.out.println("Test PASSED! The OOTB management " +
                        "agent publishes both the local and remote " +
                        "connector info through jvmstat counters.");
                break;
            case 4:
                if (localAddress != null || !remoteData.isEmpty()) {
                    System.out.println("Test FAILED! The OOTB management " +
                            "agent unexpectedly published jvmstat counters.");
                    throw new IllegalArgumentException("Test case 4 failed");
                }
                RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
                String name = rt.getName();
                System.out.println("name = " + name);
                String vmid = name.substring(0, name.indexOf("@"));
                System.out.println("vmid = " + vmid);
                VirtualMachine vm = VirtualMachine.attach(vmid);
                Properties p = new Properties();
                p.put("com.sun.management.jmxremote.port", "0");
                p.put("com.sun.management.jmxremote.authenticate", "false");
                p.put("com.sun.management.jmxremote.ssl", "false");
                vm.startManagementAgent(p);
                vm.startLocalManagementAgent();
                vm.detach();
                String localAddress2 = ConnectorAddressLink.importFrom(0);
                if (localAddress2 == null) {
                    System.out.println("Test FAILED! The OOTB management " +
                            "agent didn't publish the local connector.");
                    throw new IllegalArgumentException("Test case 4 failed");
                }
                checkAddress(localAddress2);
                Map<String, String> remoteData2 = ConnectorAddressLink.importRemoteFrom(0);
                if (remoteData2.isEmpty()) {
                    System.out.println("Test FAILED! The OOTB management " +
                            "agent didnn't publish the remote connector.");
                    throw new IllegalArgumentException("Test case 4 failed");
                }
                for (String key : remoteData2.keySet()) {
                    if (!isKeyAcceptable(key)) {
                        System.out.println("Test FAILED! The OOTB management " +
                                "agent shouldn't publish anything which isn't " +
                                "related to the remote connector (" + key + ").");
                        throw new IllegalArgumentException("Test case 4 failed");
                    }
                }
                checkKey(remoteData2, 0, "remoteAddress", null);
                checkKey(remoteData2, 0, "authenticate", "false");
                checkKey(remoteData2, 0, "ssl", "false");
                checkKey(remoteData2, 0, "sslRegistry", "false");
                checkKey(remoteData2, 0, "sslNeedClientAuth", "false");
                System.out.println("Test PASSED! The OOTB management agent " +
                        "publishes both the local and remote connector " +
                        "info through jvmstat counters when the agent is " +
                        "loaded through the Attach API.");
        }
        System.out.println("Bye! Bye!");
    }

    private static boolean isKeyAcceptable(String key) {
        return key.startsWith("sun.management.JMXConnectorServer.0.") ||
               key.startsWith("sun.management.JMXConnectorServer.remote.enabled");
    }
}
