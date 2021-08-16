/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.util.regex.*;
import sun.jvm.hotspot.debugger.DebuggerException;

public class RMIHelper {
    private static final boolean startRegistry;
    private static final Pattern CONNECT_PATTERN = Pattern.compile("^((?<serverid>.+?)@)?(?<host>.+?)(/(?<servername>.+))?$");
    private static final String DEFAULT_RMI_OBJECT_NAME = "SARemoteDebugger";
    private static int port;

    static {
        String tmp = System.getProperty("sun.jvm.hotspot.rmi.startRegistry");
        if (tmp != null && tmp.equals("false")) {
            startRegistry = false;
        } else {
            // by default, we attempt to start rmiregistry
            startRegistry = true;
        }

        port = Registry.REGISTRY_PORT;
        tmp = System.getProperty("sun.jvm.hotspot.rmi.port");
        if (tmp != null) {
            try {
                port = Integer.parseInt(tmp);
            } catch (NumberFormatException nfe) {
                System.err.println("invalid port supplied, assuming default");
            }
        }
    }

    public static void rebind(String serverID, String serverName, Remote object) throws DebuggerException {
        String name = getName(serverID, serverName);
        try {
            Naming.rebind(name, object);
        } catch (RemoteException re) {
            if (startRegistry) {
                // may be the user didn't start rmiregistry, try to start it
                try {
                    LocateRegistry.createRegistry(port);
                    Naming.rebind(name, object);
                } catch (Exception exp) {
                    throw new DebuggerException(exp);
                }
            } else {
                throw new DebuggerException(re);
            }
        } catch (Exception exp) {
            throw new DebuggerException(exp);
        }
    }

    public static void unbind(String serverID, String serverName) throws DebuggerException {
        String name = getName(serverID, serverName);
        try {
            Naming.unbind(name);
        } catch (Exception exp) {
            throw new DebuggerException(exp);
        }
    }

    public static Remote lookup(String connectionString) throws DebuggerException {
        // connectionString follows the pattern [serverid@]host[:port][/servername]
        // we have to transform this as //host[:port]/<servername>['_'<serverid>]
        Matcher matcher = CONNECT_PATTERN.matcher(connectionString);
        matcher.find();

        String serverNamePrefix = System.getProperty("sun.jvm.hotspot.rmi.serverNamePrefix");
        String rmiObjectName = matcher.group("servername");
        if (serverNamePrefix != null) {
            if (rmiObjectName == null) {
                System.err.println("WARNING: sun.jvm.hotspot.rmi.serverNamePrefix is deprecated. Please specify it in --connect.");
                rmiObjectName = serverNamePrefix;
            } else {
                throw new DebuggerException("Cannot set both sun.jvm.hotspot.rmi.serverNamePrefix and servername in --connect together");
            }
        }
        if (rmiObjectName == null) {
            rmiObjectName = DEFAULT_RMI_OBJECT_NAME;
        }
        StringBuilder nameBuf = new StringBuilder("//");
        nameBuf.append(matcher.group("host"));
        nameBuf.append('/');
        nameBuf.append(rmiObjectName);
        if (matcher.group("serverid") != null) {
            nameBuf.append('_');
            nameBuf.append(matcher.group("serverid"));
        }

        try {
            return Naming.lookup(nameBuf.toString());
        } catch (Exception exp) {
            throw new DebuggerException(exp);
        }
    }

    private static String getName(String serverID, String serverName) {
        String name = serverName;
        String serverNamePrefix = System.getProperty("sun.jvm.hotspot.rmi.serverNamePrefix");
        if (serverNamePrefix != null) {
            if (serverName == null) {
                System.err.println("WARNING: sun.jvm.hotspot.rmi.serverNamePrefix is deprecated. Please specify it with --servername.");
                name = serverNamePrefix;
            } else {
                throw new DebuggerException("Cannot set both sun.jvm.hotspot.rmi.serverNamePrefix and --servername together");
            }
        }
        if (name == null) {
            name = DEFAULT_RMI_OBJECT_NAME;
        }
        if (serverID != null) {
           name += "_" + serverID;
        }
        if (port != Registry.REGISTRY_PORT) {
           name = "//localhost:" + port + "/" + name;
        }
        return name;
    }
}
