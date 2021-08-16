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

package sun.jvmstat.monitor.remote;

import sun.jvmstat.monitor.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.IOException;

/**
 * Remote Interface for discovering and attaching to remote
 * monitorable Java Virtual Machines.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public interface RemoteHost extends Remote {

    /**
     * Remote method to attach to a remote HotSpot Java Virtual Machine
     * identified by <code>vmid</code>.
     *
     * @param vmid The identifier for the target virtual machine.
     * @return RemoteVm - A remote object for accessing the remote Java
     *                    Virtual Machine.
     *
     * @throws MonitorException Thrown when any other error is encountered
     *                          while communicating with the target virtual
     *                          machine.
     * @throws RemoteException
     *
     */
    RemoteVm attachVm(int vmid, String mode) throws RemoteException,
                                                    MonitorException;

    /**
     * Remote method to detach from a remote HotSpot Java Virtual Machine
     * identified by <code>vmid</code>.
     *
     * @param rvm The remote object for the target Java Virtual
     *            Machine.
     *
     * @throws MonitorException Thrown when any other error is encountered
     *                          while communicating with the target virtual
     *                          machine.
     * @throws RemoteException
     */
    void detachVm(RemoteVm rvm) throws RemoteException, MonitorException;

    /**
     * Get a list of Local Virtual Machine Identifiers for the active
     * Java Virtual Machine the remote system. A Local Virtual Machine
     * Identifier is also known as an <em>lvmid</em>.
     *
     * @return int[] - A array of <em>lvmid</em>s.
     * @throws MonitorException Thrown when any other error is encountered
     *                          while communicating with the target virtual
     *                          machine.
     * @throws RemoteException
     */
    int[] activeVms() throws RemoteException, MonitorException;
}
