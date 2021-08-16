/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi.server;

import java.rmi.*;

/**
 * A ServerRef represents the server-side handle for a remote object
 * implementation.
 *
 * @author  Ann Wollrath
 * @since   1.1
 * @deprecated No replacement. This interface is unused and is obsolete.
 */
@Deprecated
public interface ServerRef extends RemoteRef {

    /** indicate compatibility with JDK 1.1.x version of class.
     *
     * @deprecated A {@code serialVersionUID} field in an interface is
     * ineffectual. Do not use; no replacement.
     */
    @Deprecated
    @SuppressWarnings("serial")
    static final long serialVersionUID = -4557750989390278438L;

    /**
     * Creates a client stub object for the supplied Remote object.
     * If the call completes successfully, the remote object should
     * be able to accept incoming calls from clients.
     * @param obj the remote object implementation
     * @param data information necessary to export the object
     * @return the stub for the remote object
     * @throws RemoteException if an exception occurs attempting
     * to export the object (e.g., stub class could not be found)
     * @since 1.1
     */
    RemoteStub exportObject(Remote obj, Object data)
        throws RemoteException;

    /**
     * Returns the hostname of the current client.  When called from a
     * thread actively handling a remote method invocation the
     * hostname of the client is returned.
     * @return the client's host name
     * @throws ServerNotActiveException if called outside of servicing
     * a remote method invocation
     * @since 1.1
     */
    String getClientHost() throws ServerNotActiveException;
}
