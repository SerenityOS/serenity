/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.perfdata.monitor.protocol.rmi;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.rmi.RemoteException;
import sun.jvmstat.monitor.*;
import sun.jvmstat.monitor.event.*;
import sun.jvmstat.monitor.remote.*;

/**
 * Class for managing the RemoteMonitoredVm instances on a remote system.
 * <p>
 * This class is responsible for the mechanism that detects the active
 * HotSpot Java Virtual Machines on the remote host and possibly for a
 * specific user. The ability to detect all possible HotSpot Java Virtual
 * Machines on the remote host may be limited by the permissions of the
 * principal running the RMI server application on the remote host.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class RemoteVmManager {

    private RemoteHost remoteHost;
    private String user;

    /**
     * Creates a RemoteVmManager instance for the remote system.
     * <p>
     * Manages RemoteMonitordVm instances for which the principal
     * running the remote server has appropriate permissions.
     *
     * @param remoteHost the remote proxy object to the RMI server on
     *                   the remote system.
     */
    public RemoteVmManager(RemoteHost remoteHost) {
        this(remoteHost, null);
    }

    /**
     * Creates a RemoteVmManager instance for the given user.
     * <p>
     * Manages RemoteMonitoredVm instances for all remote Java Virtual
     * machines owned by the specified user on the remote system. The
     * RMI server on the remote system must have the appropriate permissions
     * to access the named users Java Virtual Machines.
     *
     * @param remoteHost the remote proxy object to the RMI server on
     *                   the remote system.
     * @param user the name of the user
     */
    public RemoteVmManager(RemoteHost remoteHost, String user) {
        this.user = user;
        this.remoteHost = remoteHost;
    }

    /**
     * Return the current set of monitorable Java Virtual Machines.
     * <p>
     * The set returned by this method depends on the user name passed
     * to the constructor. If no user name was specified, then this
     * method will return all candidate JVMs on the system. Otherwise,
     * only the JVMs for the given user will be returned. This assumes
     * that the RMI server process has the appropriate permissions to
     * access the target set of JVMs.
     *
     * @return Set - the Set of monitorable Java Virtual Machines
     */
    public Set<Integer> activeVms() throws MonitorException {
        int[] active = null;

        try {
            active = remoteHost.activeVms();

        } catch (RemoteException e) {
            throw new MonitorException("Error communicating with remote host: "
                                       + e.getMessage(), e);
        }

        Set<Integer> activeSet = new HashSet<Integer>(active.length);

        for (int i = 0; i < active.length; i++) {
            activeSet.add(active[i]);
        }

        return activeSet;
    }
}
