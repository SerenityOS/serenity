/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.remote.rmi;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * <p>RMI object used to establish connections to an RMI connector.
 * There is one Remote object implementing this interface for each RMI
 * connector.</p>
 *
 * <p>User code does not usually refer to this interface.  It is
 * specified as part of the public API so that different
 * implementations of that API will interoperate.</p>
 *
 * @since 1.5
 */
public interface RMIServer extends Remote {
    /**
     * <p>The version of the RMI Connector Protocol understood by this
     * connector server.  This is a string with the following format:</p>
     *
     * <pre>
     * <em>protocol-version</em> <em>implementation-name</em>
     * </pre>
     *
     * <p>The <code><em>protocol-version</em></code> is a series of
     * two or more non-negative integers separated by periods
     * (<code>.</code>).  An implementation of the version described
     * by this documentation must use the string <code>1.0</code>
     * here.</p>
     *
     * <p>After the protocol version there must be a space, followed
     * by the implementation name.  The format of the implementation
     * name is unspecified.  It is recommended that it include an
     * implementation version number.  An implementation can use an
     * empty string as its implementation name, for example for
     * security reasons.</p>
     *
     * @return a string with the format described here.
     *
     * @exception RemoteException if there is a communication
     * exception during the remote method call.
     */
    public String getVersion() throws RemoteException;

    /**
     * <p>Makes a new connection through this RMI connector.  Each
     * remote client calls this method to obtain a new RMI object
     * representing its connection.</p>
     *
     * @param credentials this object specifies the user-defined credentials
     * to be passed in to the server in order to authenticate the user before
     * creating the <code>RMIConnection</code>.  Can be null.
     *
     * @return the newly-created connection object.
     *
     * @exception IOException if the new client object cannot be
     * created or exported, or if there is a communication exception
     * during the remote method call.
     *
     * @exception SecurityException if the given credentials do not
     * allow the server to authenticate the caller successfully.
     */
    public RMIConnection newClient(Object credentials) throws IOException;
}
