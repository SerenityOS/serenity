/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.remote.internal.rmi;

import java.io.ObjectInputFilter;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

/**
 * <p>Unpublished interface controlling how the RMI Connector Server
 * exports objects.  The RMIServerImpl object and each
 * RMIConnectionImpl object are exported using the exporter.  The
 * default exporter calls {@link
 * UnicastRemoteObject#exportObject(Remote, int,
 * RMIClientSocketFactory, RMIServerSocketFactory)} to export objects
 * and {@link UnicastRemoteObject#unexportObject(Remote, boolean)} to
 * unexport them.  A replacement exporter can be specified via the
 * {@link #EXPORTER_ATTRIBUTE} property in the environment Map passed
 * to the RMI connector server.</p>
 */
public interface RMIExporter {
    public static final String EXPORTER_ATTRIBUTE =
        "com.sun.jmx.remote.rmi.exporter";

    public Remote exportObject(Remote obj,
                               int port,
                               RMIClientSocketFactory csf,
                               RMIServerSocketFactory ssf,
                               ObjectInputFilter filter)
            throws RemoteException;

    public boolean unexportObject(Remote obj, boolean force)
            throws NoSuchObjectException;
}
