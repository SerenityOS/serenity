/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jstatd;

import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.net.MalformedURLException;
import sun.jvmstat.monitor.remote.*;

/**
 * Application providing remote access to the jvmstat instrumentation
 * exported by local Java Virtual Machine processes. Remote access is
 * provided through an RMI interface.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class Jstatd {

    private static Registry registry;
    private static int port = -1;
    private static boolean startRegistry = true;
    private static RemoteHost remoteHost;

    private static void printUsage() {
        System.err.println("usage: jstatd [-nr] [-p port] [-r rmiport] [-n rminame]\n" +
                           "       jstatd -?|-h|--help");
    }

    static void bind(String name, RemoteHost remoteHost)
                throws RemoteException, MalformedURLException, Exception {

        try {
            Naming.rebind(name, remoteHost);
        } catch (java.rmi.ConnectException e) {
            /*
             * either the registry is not running or we cannot contact it.
             * start an internal registry if requested.
             */
            if (startRegistry && registry == null) {
                int localport = (port < 0) ? Registry.REGISTRY_PORT : port;
                registry = LocateRegistry.createRegistry(localport);
                bind(name, remoteHost);
            } else {
                throw e;
            }
        }
    }

    @SuppressWarnings({"removal","deprecation"}) // Use of RMISecurityManager
    public static void main(String[] args) {
        String rminame = null;
        int rmiPort = 0;
        int argc = 0;

        for ( ; (argc < args.length) && (args[argc].startsWith("-")); argc++) {
            String arg = args[argc];

            if (arg.compareTo("-?") == 0 ||
                arg.compareTo("-h") == 0 ||
                arg.compareTo("--help") == 0) {
                printUsage();
                System.exit(0);
            } else if (arg.compareTo("-nr") == 0) {
                startRegistry = false;
            } else if (arg.startsWith("-p")) {
                if (arg.compareTo("-p") != 0) {
                    port = Integer.parseInt(arg.substring(2));
                } else {
                  argc++;
                  if (argc >= args.length) {
                      printUsage();
                      System.exit(1);
                  }
                  port = Integer.parseInt(args[argc]);
                }
            } else if (arg.startsWith("-r")) {
                if (arg.compareTo("-r") != 0) {
                    rmiPort = Integer.parseInt(arg.substring(2));
                } else {
                    argc++;
                    if (argc >= args.length) {
                        printUsage();
                        System.exit(1);
                    }
                    rmiPort = Integer.parseInt(args[argc]);
                }
            } else if (arg.startsWith("-n")) {
                if (arg.compareTo("-n") != 0) {
                    rminame = arg.substring(2);
                } else {
                    argc++;
                    if (argc >= args.length) {
                        printUsage();
                        System.exit(1);
                    }
                    rminame = args[argc];
                }
            } else {
                printUsage();
                System.exit(1);
            }
        }

        if (argc < args.length) {
            printUsage();
            System.exit(1);
        }

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

        StringBuilder name = new StringBuilder();

        if (port >= 0) {
            name.append("//:").append(port);
        }

        if (rminame == null) {
            rminame = "JStatRemoteHost";
        }

        name.append("/").append(rminame);

        try {
            // use 1.5.0 dynamically generated subs.
            System.setProperty("java.rmi.server.ignoreSubClasses", "true");
            remoteHost = new RemoteHostImpl(rmiPort);
            RemoteHost stub = (RemoteHost) UnicastRemoteObject.exportObject(
                    remoteHost, rmiPort);
            bind(name.toString(), stub);
            System.out.println("jstatd started (bound to " + name.toString() + ")");
            System.out.flush();
        } catch (MalformedURLException e) {
            if (rminame != null) {
                System.out.println("Bad RMI server name: " + rminame);
            } else {
                System.out.println("Bad RMI URL: " + name);
            }
            e.printStackTrace(System.out);
            System.exit(1);
        } catch (java.rmi.ConnectException e) {
            // could not attach to or create a registry
            System.out.println("Could not contact RMI registry");
            e.printStackTrace(System.out);
            System.exit(1);
        } catch (RemoteException e) {
            System.out.println("Could not bind " + name + " to RMI Registry");
            e.printStackTrace(System.out);
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Could not create remote object");
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
