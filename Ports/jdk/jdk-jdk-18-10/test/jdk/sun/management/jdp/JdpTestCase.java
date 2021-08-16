/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * A JVM with JDP on should send multicast JDP packets regularly.
 * Look at JdpOnTestCase.java and JdpOffTestCase.java
 */


import sun.management.jdp.JdpJmxPacket;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class JdpTestCase {
    final Logger log = Logger.getLogger("sun.management.jdp");
    final int MAGIC = 0xC0FFEE42;                       // Jdp magic number.
    private static final int BUFFER_LENGTH = 64 * 1024;   // max UDP size, except for IPv6 jumbograms.
    private final int TIME_OUT_FACTOR = 10;             // Socket times out after 10 times the jdp pause.
    protected int timeOut;
    private long startTime;
    protected ClientConnection connection;

    public JdpTestCase(ClientConnection connection) {
        this.connection = connection;
        JdpTestUtil.enableConsoleLogging(log, Level.ALL);
    }

    public void run() throws Exception {
        log.fine("Test started.");
        log.fine("Listening for multicast packets at " + connection.address.getHostAddress()
                + ":" + String.valueOf(connection.port));
        log.fine(initialLogMessage());
        log.fine("Pause in between packets is: " + connection.pauseInSeconds + " seconds.");

        startTime = System.currentTimeMillis();
        timeOut = connection.pauseInSeconds * TIME_OUT_FACTOR;
        log.fine("Timeout set to " + String.valueOf(timeOut) + " seconds.");

        MulticastSocket socket = connection.connectWithTimeout(timeOut * 1000);

        byte[] buffer = new byte[BUFFER_LENGTH];
        DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);

        do {
            try {
                socket.receive(datagram);
                onReceived(extractUDPpayload(datagram));
            } catch (SocketTimeoutException e) {
                onSocketTimeOut(e);
            }

            if (!shouldContinue()) {
              break;
            }

            if (hasTestLivedLongEnough()) {
                shutdown();
            }

        } while (true);
        log.fine("Test ended successfully.");
    }

    /**
     * Subclasses: JdpOnTestCase and JdpOffTestCase have different messages.
     */
    protected abstract String initialLogMessage();


    /**
     * Executed when the socket receives a UDP packet.
     */
    private void onReceived(byte[] packet) throws Exception {
        if (isJDP(packet)) {
            Map<String, String> payload = checkStructure(packet);
            jdpPacketReceived(payload);
        } else {
            log.fine("Non JDP packet received, ignoring it.");
        }
    }

    /**
     * Determine whether the test should end.
     *
     * @return
     */
    abstract protected boolean shouldContinue();

    /**
     * This method is executed when the socket has not received any packet for timeOut seconds.
     */
    abstract protected void onSocketTimeOut(SocketTimeoutException e) throws Exception;

    /**
     * This method is executed after a correct Jdp packet has been received.
     *
     * @param payload A dictionary containing the data if the received Jdp packet.
     */
    private void jdpPacketReceived(Map<String, String> payload) throws Exception {
        final String instanceName = payload.get("INSTANCE_NAME");
        if (instanceName != null && instanceName.equals(connection.instanceName)) {
            packetFromThisVMReceived(payload);
        } else {
            packetFromOtherVMReceived(payload);
        }
    }

    /**
     * This method is executed after a correct Jdp packet, coming from this VM has been received.
     *
     * @param payload A dictionary containing the data if the received Jdp packet.
     */
    protected abstract void packetFromThisVMReceived(Map<String, String> payload) throws Exception;


    /**
     * This method is executed after a correct Jdp packet, coming from another VM has been received.
     *
     * @param payload A dictionary containing the data if the received Jdp packet.
     */
    protected void packetFromOtherVMReceived(Map<String, String> payload) {
        final String jdpName = payload.get("INSTANCE_NAME");
        log.fine("Ignoring JDP packet sent by other VM, jdp.name=" + jdpName);
    }


    /**
     * The test should stop if it has been 12 times the jdp.pause.
     * jdp.pause is how many seconds in between packets.
     * <p/>
     * This timeout (12 times)is slightly longer than the socket timeout (10 times) on purpose.
     * In the off test case, the socket should time out first.
     *
     * @return
     */
    protected boolean hasTestLivedLongEnough() {
        long now = System.currentTimeMillis();
        boolean haslivedLongEnough = (now - startTime) > (timeOut * 1.2 * 1000);
        return haslivedLongEnough;
    }

    /**
     * This exit condition arises when we receive UDP packets but they are not valid Jdp.
     */
    protected void shutdown() throws Exception {
        log.severe("Shutting down the test.");
        throw new Exception("Not enough JDP packets received before timeout!");
    }

    /**
     * Assert that this Jdp packet contains the required two keys.
     * <p/>
     * We expect zero packet corruption and thus fail on the first corrupted packet.
     * This might need revision.
     */
    protected Map<String, String> checkStructure(byte[] packet) throws UnsupportedEncodingException {
        Map<String, String> payload = JdpTestUtil.readPayload(packet);
        assertTrue(payload.size() >= 2, "JDP should have minimun 2 entries.");
        assertTrue(payload.get(JdpJmxPacket.UUID_KEY).length() > 0);
        assertTrue(payload.get(JdpJmxPacket.JMX_SERVICE_URL_KEY).length() > 0);
        return payload;
    }


    /**
     * Check if packet has correct JDP magic number.
     *
     * @param packet
     * @return
     * @throws IOException
     */
    private boolean isJDP(byte[] packet) throws IOException {
        int magic = JdpTestUtil.decode4ByteInt(packet, 0);
        return (magic == MAGIC);
    }

    private byte[] extractUDPpayload(DatagramPacket datagram) {
        byte[] data = Arrays.copyOf(datagram.getData(), datagram.getLength());
        return data;
    }

    /**
     * Hack until I find a way to use TestNG's assertions.
     */
    private void assertTrue(boolean assertion, String message) {
        if (assertion == false) {
            log.severe(message);
            assert (false);
        }
    }

    private void assertTrue(boolean assertion) {
        assertTrue(assertion, "");
    }

}
