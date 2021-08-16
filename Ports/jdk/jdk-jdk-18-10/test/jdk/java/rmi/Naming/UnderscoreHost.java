 /*
  * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
  * @bug 5083594
  * @summary Ensure that Naming.java correctly parses host names with '_' in
  * them.
  * @author Vinod Johnson
  *
  * @library ../testlibrary
  * @modules java.rmi/sun.rmi.registry
  *          java.rmi/sun.rmi.server
  *          java.rmi/sun.rmi.transport
  *          java.rmi/sun.rmi.transport.tcp
  * @build TestLibrary UnderscoreHost_Stub
  * @run main/othervm UnderscoreHost
 */

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;

public class UnderscoreHost extends UnicastRemoteObject implements Remote {
    private static final String HOSTNAME = "foo_bar";
    private static final String NAME = "name";
    /*
     * The socket factory captures the host name of the parsed URL, and
     * then connects to the local host.
     */
    private static class HostVerifyingSocketFactory extends RMISocketFactory {
        String host;

        public synchronized Socket createSocket(String host, int port)
            throws IOException  {
            if (this.host == null) {
                // Only set it the first time, subsequent DGC dirty calls
                // will be local host
                this.host = host;
            }
            return new Socket("localhost", port);
        }
        public ServerSocket createServerSocket(int port) throws IOException {
            return new ServerSocket(port);
        }
    }

    public UnderscoreHost() throws RemoteException {};

    public static void main(String args[]) {
        UnderscoreHost t = null;
        try {
            HostVerifyingSocketFactory hvf = new HostVerifyingSocketFactory();
            RMISocketFactory.setSocketFactory(hvf);
            Registry r = TestLibrary.createRegistryOnEphemeralPort();
            int port = TestLibrary.getRegistryPort(r);
            t = new UnderscoreHost();
            r.rebind(NAME, t);
            Naming.lookup("rmi://" + HOSTNAME +
                          ":" + port + "/" + NAME);
            /*
             * This test is coded to pass whether java.net.URI obeys
             * RFC 2396 or RFC 3986 (see 5085902, 6394131, etc.).
             *
             * If java.net.URI obeys RFC 3986, so host names may
             * contain underscores, then the Naming.lookup invocation
             * should succeed-- but the host actually connected to
             * must equal HOSTNAME.
             */
            if (!hvf.host.equals(HOSTNAME)) {
                throw new RuntimeException(
                    "java.rmi.Naming Parsing error:" +
                    hvf.host + ":" + HOSTNAME);
            }
        } catch (MalformedURLException e) {
            /*
             * If java.net.URI obeys RFC 2396, so host names must not
             * contain underscores, then the Naming.lookup invocation
             * should throw MalformedURLException-- so this is OK.
             */
        } catch (IOException ioe) {
            TestLibrary.bomb(ioe);
        } catch (java.rmi.NotBoundException nbe) {
            TestLibrary.bomb(nbe);
        } finally {
            TestLibrary.unexport(t);
        }

    }
}
