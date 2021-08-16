/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * A JVM with JDP off should not send multicast JDP packets at all.
 * com.sun.management.jmxremote.autodiscovery=false should be respected.
 */

import java.net.SocketTimeoutException;
import java.util.Map;

public class JdpOffTestCase extends JdpTestCase {

    private boolean testPassed = false;

    public JdpOffTestCase(ClientConnection connection) {
        super(connection);
    }

    /**
     * Subclasses: JdpOnTestCase and JdpOffTestCase have different messages.
     */
    @Override
    protected String initialLogMessage() {
        return "Expecting NOT to receive any packets with jdp.name=" + connection.instanceName;
    }

    /**
     * The socket has not received anything, and this is the expected behavior.
     */
    @Override
    protected void onSocketTimeOut(SocketTimeoutException e) throws Exception {
        log.fine("No packages received. Test passed!");
        testPassed = true;
    }

    /**
     * The socket did not timeout and no valid JDP packets were received.
     */
    @Override
    protected void shutdown() throws Exception {
        log.fine("Test timed out. Test passed!");
        testPassed = true;
    }

    /**
     * This method is executed after a correct Jdp packet, coming from this VM has been received.
     *
     * @param payload A dictionary containing the data if the received Jdp packet.
     */
    @Override
    protected void packetFromThisVMReceived(Map<String, String> payload) throws Exception {
        String message = "Jdp packet from this VM received. This should not happen!";
        log.severe(message);
        throw new Exception(message);
    }


    /**
     * The test should stop after the socket has timed out. See onSocketTimeOut {@link}.
     */
    @Override
    protected boolean shouldContinue() {
        return !testPassed;
    }

    public static void main(String[] args) throws Exception {
        JdpTestCase client = new JdpOffTestCase(new ClientConnection());
        client.run();
    }

}
