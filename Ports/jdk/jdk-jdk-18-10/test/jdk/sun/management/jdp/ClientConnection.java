/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class ClientConnection {

    public final String IANA_JDP_ADDRESS = "224.0.23.178";
    public final String IANA_JDP_PORT = "7095";
    public final String UNDEFINED_NAME = "TheVMwithNoName";

    public final int port;
    public final InetAddress address;
    public final int pauseInSeconds;
    public final String instanceName;

    public ClientConnection()
            throws UnknownHostException {

        String discoveryAddress = System.getProperty("com.sun.management.jdp.address", IANA_JDP_ADDRESS);
        address = InetAddress.getByName(discoveryAddress);

        String discoveryPort = System.getProperty("com.sun.management.jdp.port", IANA_JDP_PORT);
        port = Integer.parseInt(discoveryPort);

        String pause = System.getProperty("com.sun.management.jdp.pause", "1");
        pauseInSeconds = Integer.parseUnsignedInt(pause);

        instanceName = System.getProperty("com.sun.management.jdp.name", UNDEFINED_NAME);

    }

    public MulticastSocket connectWithTimeout(int msTimeOut) throws IOException {
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(address);
        socket.setSoTimeout(msTimeOut);
        return socket;
    }
}
