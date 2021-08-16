/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.ServiceLoader;

import com.sun.jdi.JDIPermission;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.connect.spi.Connection;
import com.sun.jdi.connect.spi.TransportService;

/* Public for use by com.sun.jdi.Bootstrap */
public class VirtualMachineManagerImpl implements VirtualMachineManagerService {
    private List<Connector> connectors = new ArrayList<>();
    private LaunchingConnector defaultConnector = null;
    private List<VirtualMachine> targets = new ArrayList<>();
    private final ThreadGroup mainGroupForJDI;
    private ResourceBundle messages = null;
    private int vmSequenceNumber = 0;
    private static final int majorVersion = Runtime.version().feature();
    private static final int minorVersion = 0;

    private static final Object lock = new Object();
    private static VirtualMachineManagerImpl vmm;

    public static VirtualMachineManager virtualMachineManager() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            JDIPermission vmmPermission =
                new JDIPermission("virtualMachineManager");
            sm.checkPermission(vmmPermission);
        }
        synchronized (lock) {
            if (vmm == null) {
                vmm = new VirtualMachineManagerImpl();
            }
        }
        return vmm;
    }

    protected VirtualMachineManagerImpl() {

        /*
         * Create a top-level thread group
         */
        ThreadGroup top = Thread.currentThread().getThreadGroup();
        ThreadGroup parent = null;
        while ((parent = top.getParent()) != null) {
            top = parent;
        }
        mainGroupForJDI = new ThreadGroup(top, "JDI main");

        /*
         * Load the connectors
         */
        ServiceLoader<Connector> connectorLoader =
            ServiceLoader.load(Connector.class, Connector.class.getClassLoader());

        Iterator<Connector> connectors = connectorLoader.iterator();

        while (connectors.hasNext()) {
            Connector connector;

            try {
                connector = connectors.next();
            } catch (ThreadDeath x) {
                throw x;
            } catch (Exception x) {
                System.err.println(x);
                continue;
            } catch (Error x) {
                System.err.println(x);
                continue;
            }

            addConnector(connector);
        }

        /*
         * Load any transport services and encapsulate them with
         * an attaching and listening connector.
         */
        ServiceLoader<TransportService> transportLoader =
            ServiceLoader.load(TransportService.class,
                               TransportService.class.getClassLoader());

        Iterator<TransportService> transportServices =
            transportLoader.iterator();

        while (transportServices.hasNext()) {
            TransportService transportService;

            try {
                transportService = transportServices.next();
            } catch (ThreadDeath x) {
                throw x;
            } catch (Exception x) {
                System.err.println(x);
                continue;
            } catch (Error x) {
                System.err.println(x);
                continue;
            }

            addConnector(GenericAttachingConnector.create(transportService));
            addConnector(GenericListeningConnector.create(transportService));
        }

        // no connectors found
        if (allConnectors().size() == 0) {
            throw new Error("no Connectors loaded");
        }

        // Set the default launcher. In order to be compatible
        // 1.2/1.3/1.4 we try to make the default launcher
        // "com.sun.jdi.CommandLineLaunch". If this connector
        // isn't found then we arbitarly pick the first connector.
        //
        boolean found = false;
        List<LaunchingConnector> launchers = launchingConnectors();
        for (LaunchingConnector lc: launchers) {
            if (lc.name().equals("com.sun.jdi.CommandLineLaunch")) {
                setDefaultConnector(lc);
                found = true;
                break;
            }
        }
        if (!found && launchers.size() > 0) {
            setDefaultConnector(launchers.get(0));
        }
    }

    public LaunchingConnector defaultConnector() {
        if (defaultConnector == null) {
            throw new Error("no default LaunchingConnector");
        }
        return defaultConnector;
    }

    public void setDefaultConnector(LaunchingConnector connector) {
        defaultConnector = connector;
    }

    public List<LaunchingConnector> launchingConnectors() {
        List<LaunchingConnector> launchingConnectors = new ArrayList<>(connectors.size());
        for (Connector connector: connectors) {
            if (connector instanceof LaunchingConnector) {
                launchingConnectors.add((LaunchingConnector)connector);
            }
        }
        return Collections.unmodifiableList(launchingConnectors);
    }

    public List<AttachingConnector> attachingConnectors() {
        List<AttachingConnector> attachingConnectors = new ArrayList<>(connectors.size());
        for (Connector connector: connectors) {
            if (connector instanceof AttachingConnector) {
                attachingConnectors.add((AttachingConnector)connector);
            }
        }
        return Collections.unmodifiableList(attachingConnectors);
    }

    public List<ListeningConnector> listeningConnectors() {
        List<ListeningConnector> listeningConnectors = new ArrayList<>(connectors.size());
        for (Connector connector: connectors) {
            if (connector instanceof ListeningConnector) {
                listeningConnectors.add((ListeningConnector)connector);
            }
        }
        return Collections.unmodifiableList(listeningConnectors);
    }

    public List<Connector> allConnectors() {
        return Collections.unmodifiableList(connectors);
    }

    public List<VirtualMachine> connectedVirtualMachines() {
        return Collections.unmodifiableList(targets);
    }

    public void addConnector(Connector connector) {
        connectors.add(connector);
    }

    public void removeConnector(Connector connector) {
        connectors.remove(connector);
    }

    public synchronized VirtualMachine createVirtualMachine(
                                        Connection connection,
                                        Process process) throws IOException {

        if (!connection.isOpen()) {
            throw new IllegalStateException("connection is not open");
        }

        VirtualMachine vm;
        try {
            vm = new VirtualMachineImpl(this, connection, process,
                                                   ++vmSequenceNumber);
        } catch (VMDisconnectedException e) {
            throw new IOException(e.getMessage());
        }
        targets.add(vm);
        return vm;
    }

    public VirtualMachine createVirtualMachine(Connection connection) throws IOException {
        return createVirtualMachine(connection, null);
    }

    public void addVirtualMachine(VirtualMachine vm) {
        targets.add(vm);
    }

    void disposeVirtualMachine(VirtualMachine vm) {
        targets.remove(vm);
    }

    public int majorInterfaceVersion() {
        return majorVersion;
    }

    public int minorInterfaceVersion() {
        return minorVersion;
    }

    ThreadGroup mainGroupForJDI() {
        return mainGroupForJDI;
    }

    String getString(String key) {
        if (messages == null) {
            messages = ResourceBundle.getBundle("com.sun.tools.jdi.resources.jdi");
        }
        return messages.getString(key);
    }
}
