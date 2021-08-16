/*
 * Copyright (c) 1996, 2001, Oracle and/or its affiliates. All rights reserved.
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
package java.rmi.registry;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * <code>Registry</code> is a remote interface to a simple remote
 * object registry that provides methods for storing and retrieving
 * remote object references bound with arbitrary string names.  The
 * <code>bind</code>, <code>unbind</code>, and <code>rebind</code>
 * methods are used to alter the name bindings in the registry, and
 * the <code>lookup</code> and <code>list</code> methods are used to
 * query the current name bindings.
 *
 * <p>In its typical usage, a <code>Registry</code> enables RMI client
 * bootstrapping: it provides a simple means for a client to obtain an
 * initial reference to a remote object.  Therefore, a registry's
 * remote object implementation is typically exported with a
 * well-known address, such as with a well-known {@link
 * java.rmi.server.ObjID#REGISTRY_ID ObjID} and TCP port number
 * (default is {@link #REGISTRY_PORT 1099}).
 *
 * <p>The {@link LocateRegistry} class provides a programmatic API for
 * constructing a bootstrap reference to a <code>Registry</code> at a
 * remote address (see the static <code>getRegistry</code> methods)
 * and for creating and exporting a <code>Registry</code> in the
 * current VM on a particular local address (see the static
 * <code>createRegistry</code> methods).
 *
 * <p>A <code>Registry</code> implementation may choose to restrict
 * access to some or all of its methods (for example, methods that
 * mutate the registry's bindings may be restricted to calls
 * originating from the local host).  If a <code>Registry</code>
 * method chooses to deny access for a given invocation, its
 * implementation may throw {@link java.rmi.AccessException}, which
 * (because it extends {@link java.rmi.RemoteException}) will be
 * wrapped in a {@link java.rmi.ServerException} when caught by a
 * remote client.
 *
 * <p>The names used for bindings in a <code>Registry</code> are pure
 * strings, not parsed.  A service which stores its remote reference
 * in a <code>Registry</code> may wish to use a package name as a
 * prefix in the name binding to reduce the likelihood of name
 * collisions in the registry.
 *
 * @author      Ann Wollrath
 * @author      Peter Jones
 * @since       1.1
 * @see         LocateRegistry
 */
public interface Registry extends Remote {

    /** Well known port for registry. */
    public static final int REGISTRY_PORT = 1099;

    /**
     * Returns the remote reference bound to the specified
     * <code>name</code> in this registry.
     *
     * @param   name the name for the remote reference to look up
     *
     * @return  a reference to a remote object
     *
     * @throws  NotBoundException if <code>name</code> is not currently bound
     *
     * @throws  RemoteException if remote communication with the
     * registry failed; if exception is a <code>ServerException</code>
     * containing an <code>AccessException</code>, then the registry
     * denies the caller access to perform this operation
     *
     * @throws  AccessException if this registry is local and it denies
     * the caller access to perform this operation
     *
     * @throws  NullPointerException if <code>name</code> is <code>null</code>
     */
    public Remote lookup(String name)
        throws RemoteException, NotBoundException, AccessException;

    /**
     * Binds a remote reference to the specified <code>name</code> in
     * this registry.
     *
     * @param   name the name to associate with the remote reference
     * @param   obj a reference to a remote object (usually a stub)
     *
     * @throws  AlreadyBoundException if <code>name</code> is already bound
     *
     * @throws  RemoteException if remote communication with the
     * registry failed; if exception is a <code>ServerException</code>
     * containing an <code>AccessException</code>, then the registry
     * denies the caller access to perform this operation (if
     * originating from a non-local host, for example)
     *
     * @throws  AccessException if this registry is local and it denies
     * the caller access to perform this operation
     *
     * @throws  NullPointerException if <code>name</code> is
     * <code>null</code>, or if <code>obj</code> is <code>null</code>
     */
    public void bind(String name, Remote obj)
        throws RemoteException, AlreadyBoundException, AccessException;

    /**
     * Removes the binding for the specified <code>name</code> in
     * this registry.
     *
     * @param   name the name of the binding to remove
     *
     * @throws  NotBoundException if <code>name</code> is not currently bound
     *
     * @throws  RemoteException if remote communication with the
     * registry failed; if exception is a <code>ServerException</code>
     * containing an <code>AccessException</code>, then the registry
     * denies the caller access to perform this operation (if
     * originating from a non-local host, for example)
     *
     * @throws  AccessException if this registry is local and it denies
     * the caller access to perform this operation
     *
     * @throws  NullPointerException if <code>name</code> is <code>null</code>
     */
    public void unbind(String name)
        throws RemoteException, NotBoundException, AccessException;

    /**
     * Replaces the binding for the specified <code>name</code> in
     * this registry with the supplied remote reference.  If there is
     * an existing binding for the specified <code>name</code>, it is
     * discarded.
     *
     * @param   name the name to associate with the remote reference
     * @param   obj a reference to a remote object (usually a stub)
     *
     * @throws  RemoteException if remote communication with the
     * registry failed; if exception is a <code>ServerException</code>
     * containing an <code>AccessException</code>, then the registry
     * denies the caller access to perform this operation (if
     * originating from a non-local host, for example)
     *
     * @throws  AccessException if this registry is local and it denies
     * the caller access to perform this operation
     *
     * @throws  NullPointerException if <code>name</code> is
     * <code>null</code>, or if <code>obj</code> is <code>null</code>
     */
    public void rebind(String name, Remote obj)
        throws RemoteException, AccessException;

    /**
     * Returns an array of the names bound in this registry.  The
     * array will contain a snapshot of the names bound in this
     * registry at the time of the given invocation of this method.
     *
     * @return  an array of the names bound in this registry
     *
     * @throws  RemoteException if remote communication with the
     * registry failed; if exception is a <code>ServerException</code>
     * containing an <code>AccessException</code>, then the registry
     * denies the caller access to perform this operation
     *
     * @throws  AccessException if this registry is local and it denies
     * the caller access to perform this operation
     */
    public String[] list() throws RemoteException, AccessException;
}
