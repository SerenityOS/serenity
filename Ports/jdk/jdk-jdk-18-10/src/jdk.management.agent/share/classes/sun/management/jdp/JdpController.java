/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.management.jdp;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * JdpController is responsible to create and manage a broadcast loop.
 *
 * <p> Other part of code has no access to broadcast loop and have to use
 * provided static methods
 * {@link #startDiscoveryService(InetAddress,int,String,String) startDiscoveryService}
 * and {@link #stopDiscoveryService() stopDiscoveryService}
 * <p>{@link #startDiscoveryService(InetAddress,int,String,String) startDiscoveryService} could be called multiple
 * times as it stops the running service if it is necessary.
 * Call to {@link #stopDiscoveryService() stopDiscoveryService}
 * ignored if service isn't run.
 *
 *
 * <p> System properties below could be used to control broadcast loop behavior.
 * Property below have to be set explicitly in command line. It's not possible to
 * set it in management.config file.  Careless changes of these properties could
 * lead to security or network issues.
 * <ul>
 *     <li>com.sun.management.jdp.ttl         - set ttl for broadcast packet</li>
 *     <li>com.sun.management.jdp.pause       - set broadcast interval in seconds</li>
 *     <li>com.sun.management.jdp.source_addr - an address of interface to use for broadcast</li>
 * </ul>
 *
 * <p>null parameters values are filtered out on {@link JdpPacketWriter} level and
 * corresponding keys are not placed to packet.
 */
public final class JdpController {

    private static class JDPControllerRunner implements Runnable {

        private final JdpJmxPacket packet;
        private final JdpBroadcaster bcast;
        private final int pause;
        private volatile boolean shutdown = false;

        private JDPControllerRunner(JdpBroadcaster bcast, JdpJmxPacket packet, int pause) {
            this.bcast = bcast;
            this.packet = packet;
            this.pause = pause;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    bcast.sendPacket(packet);
                    try {
                        Thread.sleep(this.pause);
                    } catch (InterruptedException e) {
                        // pass
                    }
                }

            } catch (IOException e) {
              // pass;
            }

            // It's not possible to re-use controller,
            // nevertheless reset shutdown variable
            try {
                stop();
                bcast.shutdown();
            } catch (IOException ex) {
                // pass - ignore IOException during shutdown
            }
        }

        public void stop() {
            shutdown = true;
        }
    }
    private static JDPControllerRunner controller = null;

    private JdpController(){
        // Don't allow to instantiate this class.
    }

    // Utility to handle optional system properties
    // Parse an integer from string or return default if provided string is null
    private static int getInteger(String val, int dflt, String msg) throws JdpException {
        try {
            return (val == null) ? dflt : Integer.parseInt(val);
        } catch (NumberFormatException ex) {
            throw new JdpException(msg);
        }
    }

    // Parse an inet address from string or return default if provided string is null
    private static InetAddress getInetAddress(String val, InetAddress dflt, String msg) throws JdpException {
        try {
            return (val == null) ? dflt : InetAddress.getByName(val);
        } catch (UnknownHostException ex) {
            throw new JdpException(msg);
        }
    }

    // Get the process id of the current running Java process
    private static Long getProcessId() {
        try {
            // Get the current process id
            return ProcessHandle.current().pid();
        } catch(UnsupportedOperationException ex) {
            return null;
        }
    }


    /**
     * Starts discovery service
     *
     * @param address - multicast group address
     * @param port - udp port to use
     * @param instanceName - name of running JVM instance
     * @param url - JMX service url
     * @throws IOException
     */
    public static synchronized void startDiscoveryService(InetAddress address, int port, String instanceName, String url)
            throws IOException, JdpException {

        // Limit packet to local subnet by default
        int ttl = getInteger(
                System.getProperty("com.sun.management.jdp.ttl"), 1,
                "Invalid jdp packet ttl");

        // Broadcast once a 5 seconds by default
        int pause = getInteger(
                System.getProperty("com.sun.management.jdp.pause"), 5,
                "Invalid jdp pause");

        // Converting seconds to milliseconds
        pause = pause * 1000;

        // Allow OS to choose broadcast source
        InetAddress sourceAddress = getInetAddress(
                System.getProperty("com.sun.management.jdp.source_addr"), null,
                "Invalid source address provided");

        // Generate session id
        UUID id = UUID.randomUUID();

        JdpJmxPacket packet = new JdpJmxPacket(id, url);

        // Don't broadcast whole command line for security reason.
        // Strip everything after first space
        String javaCommand = System.getProperty("sun.java.command");
        if (javaCommand != null) {
            String[] arr = javaCommand.split(" ", 2);
            packet.setMainClass(arr[0]);
        }

        // Put optional explicit java instance name to packet, if user doesn't specify
        // it the key is skipped. PacketWriter is responsible to skip keys having null value.
        packet.setInstanceName(instanceName);

        // Set rmi server hostname if it explicitly specified by user with
        // java.rmi.server.hostname
        String rmiHostname = System.getProperty("java.rmi.server.hostname");
        packet.setRmiHostname(rmiHostname);

        // Set broadcast interval
        packet.setBroadcastInterval(Integer.toString(pause));

        // Set process id
        Long pid = getProcessId();
        if (pid != null) {
           packet.setProcessId(pid.toString());
        }

        JdpBroadcaster bcast = new JdpBroadcaster(address, sourceAddress, port, ttl);

        // Stop discovery service if it's already running
        stopDiscoveryService();

        controller = new JDPControllerRunner(bcast, packet, pause);

        Thread t = new Thread(null, controller, "JDP broadcaster", 0, false);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Stop running discovery service,
     * it's safe to attempt to stop not started service
     */
    public static synchronized void stopDiscoveryService() {
        if ( controller != null ){
             controller.stop();
             controller = null;
        }
    }
}
