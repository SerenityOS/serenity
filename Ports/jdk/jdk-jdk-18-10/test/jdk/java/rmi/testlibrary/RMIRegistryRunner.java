/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**/

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

/**
 * Class to run a rmiregistry whose VM can be told to exit remotely;
 * Difference between this class and RegistryRunner is that this class
 * simulate rmiregistry closer than RegistryRunner.
 */
public class RMIRegistryRunner extends RegistryRunner
{
    public RMIRegistryRunner() throws RemoteException {
    }

    /**
     * port 0 means to use ephemeral port to start registry.
     *
     * @param args command line arguments passed in from main
     * @return the port number on which registry accepts requests
     */
    protected static int init(String[] args) {
        try {
            if (args.length == 0) {
                System.err.println("Usage: <port>");
                System.exit(0);
            }
            int port = -1;
            port = Integer.parseInt(args[0]);

            // call RegistryImpl.createRegistry to simulate rmiregistry.
            registry = sun.rmi.registry.RegistryImpl.createRegistry(port);
            if (port == 0) {
                port = TestLibrary.getRegistryPort(registry);
            }

            // create a remote object to tell this VM to exit
            exiter = new RMIRegistryRunner();
            Naming.rebind("rmi://localhost:" + port +
                          "/RemoteExiter", exiter);

            return port;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        return -1;
    }

    public static void main(String[] args) {
        int port = init(args);
        notify(port);
    }
}
