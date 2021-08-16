/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Class to run a registry whose VM can be told to exit remotely; using
 * a registry (in a sub-process) in this fashion makes tests more robust under
 * windows where Process.destroy() seems not to be 100% reliable.
 */
public class RegistryRunner extends UnicastRemoteObject
    implements RemoteExiter
{
    private static final String PORT_LABEL_START = "RegistryRunner.port.start:";
    private static final String PORT_LABEL_END = ":RegistryRunner.port.end";

    protected static Registry registry = null;
    protected static RemoteExiter exiter = null;

    public RegistryRunner() throws RemoteException {
    }

    /**
     * Ask the registry to exit instead of forcing it do so; this
     * works better on windows...
     */
    public void exit() throws RemoteException {
        // REMIND: create a thread to do this to avoid
        // a remote exception?
        System.err.println("received call to exit");
        System.exit(0);
    }

    /**
     * Request that the registry process exit and handle
     * related exceptions.
     */
    public static void requestExit(int port) {

        try {
            RemoteExiter e =
                (RemoteExiter)
                Naming.lookup("rmi://localhost:" +
                              port +
                              "/RemoteExiter");
            try {
                e.exit();
            } catch (RemoteException re) {
            }
            e = null;

        } catch (java.net.MalformedURLException mfue) {
            // will not happen
        } catch (NotBoundException nbe) {
            TestLibrary.bomb("exiter not bound?", nbe);
        } catch (RemoteException re) {
            TestLibrary.bomb("remote exception trying to exit",
                             re);
        }
    }

    public static int getRegistryPort(String output) {
        int idxStart = output.indexOf(PORT_LABEL_START);
        int idxEnd = output.indexOf(PORT_LABEL_END);
        if (idxStart == -1 || idxEnd == -1) {
            return -1;
        }
        idxStart = idxStart+PORT_LABEL_START.length();
        String portStr = output.substring(idxStart, idxEnd);
        int port = Integer.valueOf(portStr);
        System.err.println("registry is running at port: " + port);
        return port;
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

            // create a registry
            registry = LocateRegistry.createRegistry(port);
            if (port == 0) {
                port = TestLibrary.getRegistryPort(registry);
            }

            // create a remote object to tell this VM to exit
            exiter = new RegistryRunner();
            Naming.rebind("rmi://localhost:" + port +
                          "/RemoteExiter", exiter);

            return port;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        return -1;
    }

    /**
     * RegistryVM.start() will filter the output of registry subprocess,
     * when valid port is detected, RegistryVM.start() returns.
     * So, for subclass, it's important to call this method after registry
     * is initialized and necessary remote objects have been bound.
     *
     * @param port the port on which registry accepts requests
     */
    protected static void notify(int port) {
        // this output is important for RegistryVM to get the port
        // where rmiregistry is serving
        System.out.println(PORT_LABEL_START + port + PORT_LABEL_END);
        System.out.flush();
    }

    public static void main(String[] args) {
        int port = init(args);
        notify(port);
    }
}
