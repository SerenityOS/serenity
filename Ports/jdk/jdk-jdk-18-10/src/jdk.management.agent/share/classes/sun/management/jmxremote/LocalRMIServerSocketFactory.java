/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

package sun.management.jmxremote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Enumeration;

/**
 * This RMI server socket factory creates server sockets that
 * will only accept connection requests from clients running
 * on the host where the RMI remote objects have been exported.
 */
public final class LocalRMIServerSocketFactory implements RMIServerSocketFactory {
    /**
     * Creates a server socket that only accepts connection requests from
     * clients running on the host where the RMI remote objects have been
     * exported.
     */
    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port) {
            @Override
            public Socket accept() throws IOException {
                final Socket socket = super.accept();
                final InetAddress remoteAddr = socket.getInetAddress();
                final String msg = "The server sockets created using the " +
                       "LocalRMIServerSocketFactory only accept connections " +
                       "from clients running on the host where the RMI " +
                       "remote objects have been exported.";

                if (remoteAddr == null) {
                    // Though unlikeky, the socket could be already
                    // closed... Send a more detailed message in
                    // this case. Also avoid throwing NullPointerExceptiion
                    //
                    String details = "";
                    if (socket.isClosed()) {
                        details = " Socket is closed.";
                    } else if (!socket.isConnected()) {
                        details = " Socket is not connected";
                    }
                    try {
                        socket.close();
                    } catch (Exception ok) {
                        // ok - this is just cleanup before throwing detailed
                        // exception.
                    }
                    throw new IOException(msg +
                            " Couldn't determine client address." +
                            details);
                } else if (remoteAddr.isLoopbackAddress()) {
                    // local address: accept the connection.
                    return socket;
                }
                // Retrieve all the network interfaces on this host.
                Enumeration<NetworkInterface> nis;
                try {
                    nis = NetworkInterface.getNetworkInterfaces();
                } catch (SocketException e) {
                    try {
                        socket.close();
                    } catch (IOException ioe) {
                        // Ignore...
                    }
                    throw new IOException(msg, e);
                }
                // Walk through the network interfaces to see
                // if any of them matches the client's address.
                // If true, then the client's address is local.
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = nis.nextElement();
                    Enumeration<InetAddress> addrs = ni.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress localAddr = addrs.nextElement();
                        if (localAddr.equals(remoteAddr)) {
                            return socket;
                        }
                    }
                }
                // The client's address is remote so refuse the connection.
                try {
                    socket.close();
                } catch (IOException ioe) {
                    // Ignore...
                }
                throw new IOException(msg);
            }
        };
    }

    /**
     * Two LocalRMIServerSocketFactory objects
     * are equal if they are of the same type.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof LocalRMIServerSocketFactory);
    }

    /**
     * Returns a hash code value for this LocalRMIServerSocketFactory.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
