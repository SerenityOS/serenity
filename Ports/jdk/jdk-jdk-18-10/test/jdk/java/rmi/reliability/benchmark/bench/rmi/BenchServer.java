/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package bench.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The RMI benchmark server is a simple compute-engine-like server which allows
 * client benchmarks to create/export and unexport objects off of the server,
 * or run arbitrary tasks.
 */
public interface BenchServer extends Remote {
    /**
     * Interface used for creating server-side remote objects.
     */
    public interface RemoteObjectFactory extends Serializable {
        Remote create() throws RemoteException;
    }

    /**
     * Interface used for server-side tasks.
     */
    public interface Task extends Serializable {
        Object execute() throws Exception;
    }

    /**
     * Uses the given remote object factory to create a new remote object on
     * the server side.
     */
    Remote create(RemoteObjectFactory factory) throws RemoteException;

    /**
     * Unexports the specified remote object.  Returns true if successful,
     * false otherwise.
     */
    boolean unexport(Remote obj, boolean force) throws RemoteException;

    /**
     * Execute given task.
     */
    Object execute(Task task) throws Exception;

    /**
     * Invoke the garbage collector.
     */
    void gc() throws RemoteException;

    /**
     * Terminate the server.
     */
    void terminate(int delay) throws RemoteException;
}
