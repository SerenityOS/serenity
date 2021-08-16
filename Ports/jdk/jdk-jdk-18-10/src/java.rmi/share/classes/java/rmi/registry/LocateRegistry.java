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

package java.rmi.registry;

import java.rmi.RemoteException;
import java.rmi.server.ObjID;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;
import sun.rmi.registry.RegistryImpl;
import sun.rmi.server.UnicastRef2;
import sun.rmi.server.UnicastRef;
import sun.rmi.server.Util;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;

/**
 * <code>LocateRegistry</code> is used to obtain a reference to a bootstrap
 * remote object registry on a particular host (including the local host), or
 * to create a remote object registry that accepts calls on a specific port.
 *
 * <p> Note that a <code>getRegistry</code> call does not actually make a
 * connection to the remote host.  It simply creates a local reference to
 * the remote registry and will succeed even if no registry is running on
 * the remote host.  Therefore, a subsequent method invocation to a remote
 * registry returned as a result of this method may fail.
 *
 * @author  Ann Wollrath
 * @author  Peter Jones
 * @since   1.1
 * @see     java.rmi.registry.Registry
 */
public final class LocateRegistry {

    /**
     * Private constructor to disable public construction.
     */
    private LocateRegistry() {}

    /**
     * Returns a reference to the remote object <code>Registry</code> for
     * the local host on the default registry port of 1099.
     *
     * @return reference (a stub) to the remote object registry
     * @throws RemoteException if the reference could not be created
     * @since 1.1
     */
    public static Registry getRegistry()
        throws RemoteException
    {
        return getRegistry(null, Registry.REGISTRY_PORT);
    }

    /**
     * Returns a reference to the remote object <code>Registry</code> for
     * the local host on the specified <code>port</code>.
     *
     * @param port port on which the registry accepts requests
     * @return reference (a stub) to the remote object registry
     * @throws RemoteException if the reference could not be created
     * @since 1.1
     */
    public static Registry getRegistry(int port)
        throws RemoteException
    {
        return getRegistry(null, port);
    }

    /**
     * Returns a reference to the remote object <code>Registry</code> on the
     * specified <code>host</code> on the default registry port of 1099.  If
     * <code>host</code> is <code>null</code>, the local host is used.
     *
     * @param host host for the remote registry
     * @return reference (a stub) to the remote object registry
     * @throws RemoteException if the reference could not be created
     * @since 1.1
     */
    public static Registry getRegistry(String host)
        throws RemoteException
    {
        return getRegistry(host, Registry.REGISTRY_PORT);
    }

    /**
     * Returns a reference to the remote object <code>Registry</code> on the
     * specified <code>host</code> and <code>port</code>. If <code>host</code>
     * is <code>null</code>, the local host is used.
     *
     * @param host host for the remote registry
     * @param port port on which the registry accepts requests
     * @return reference (a stub) to the remote object registry
     * @throws RemoteException if the reference could not be created
     * @since 1.1
     */
    public static Registry getRegistry(String host, int port)
        throws RemoteException
    {
        return getRegistry(host, port, null);
    }

    /**
     * Returns a locally created remote reference to the remote object
     * <code>Registry</code> on the specified <code>host</code> and
     * <code>port</code>.  Communication with this remote registry will
     * use the supplied <code>RMIClientSocketFactory</code> <code>csf</code>
     * to create <code>Socket</code> connections to the registry on the
     * remote <code>host</code> and <code>port</code>.
     *
     * @param host host for the remote registry
     * @param port port on which the registry accepts requests
     * @param csf  client-side <code>Socket</code> factory used to
     *      make connections to the registry.  If <code>csf</code>
     *      is null, then the default client-side <code>Socket</code>
     *      factory will be used in the registry stub.
     * @return reference (a stub) to the remote registry
     * @throws RemoteException if the reference could not be created
     * @since 1.2
     */
    public static Registry getRegistry(String host, int port,
                                       RMIClientSocketFactory csf)
        throws RemoteException
    {
        Registry registry = null;

        if (port <= 0)
            port = Registry.REGISTRY_PORT;

        if (host == null || host.length() == 0) {
            // If host is blank (as returned by "file:" URL in 1.0.2 used in
            // java.rmi.Naming), try to convert to real local host name so
            // that the RegistryImpl's checkAccess will not fail.
            try {
                host = java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                // If that failed, at least try "" (localhost) anyway...
                host = "";
            }
        }

        /*
         * Create a proxy for the registry with the given host, port, and
         * client socket factory.  If the supplied client socket factory is
         * null, then the ref type is a UnicastRef, otherwise the ref type
         * is a UnicastRef2.  If the property
         * java.rmi.server.ignoreStubClasses is true, then the proxy
         * returned is an instance of a dynamic proxy class that implements
         * the Registry interface; otherwise the proxy returned is an
         * instance of the pregenerated stub class for RegistryImpl.
         **/
        LiveRef liveRef =
            new LiveRef(new ObjID(ObjID.REGISTRY_ID),
                        new TCPEndpoint(host, port, csf, null),
                        false);
        RemoteRef ref =
            (csf == null) ? new UnicastRef(liveRef) : new UnicastRef2(liveRef);

        return (Registry) Util.createProxy(RegistryImpl.class, ref, false);
    }

    /**
     * Creates and exports a <code>Registry</code> instance on the local
     * host that accepts requests on the specified <code>port</code>.
     *
     * <p>The <code>Registry</code> instance is exported as if the static
     * {@link UnicastRemoteObject#exportObject(Remote,int)
     * UnicastRemoteObject.exportObject} method is invoked, passing the
     * <code>Registry</code> instance and the specified <code>port</code> as
     * arguments, except that the <code>Registry</code> instance is
     * exported with a well-known object identifier, an {@link ObjID}
     * instance constructed with the value {@link ObjID#REGISTRY_ID}.
     *
     * @param port the port on which the registry accepts requests
     * @return the registry
     * @throws RemoteException if the registry could not be exported
     * @since 1.1
     **/
    public static Registry createRegistry(int port) throws RemoteException {
        return new RegistryImpl(port);
    }

    /**
     * Creates and exports a <code>Registry</code> instance on the local
     * host that uses custom socket factories for communication with that
     * instance.  The registry that is created listens for incoming
     * requests on the given <code>port</code> using a
     * <code>ServerSocket</code> created from the supplied
     * <code>RMIServerSocketFactory</code>.
     *
     * <p>The <code>Registry</code> instance is exported as if
     * the static {@link
     * UnicastRemoteObject#exportObject(Remote,int,RMIClientSocketFactory,RMIServerSocketFactory)
     * UnicastRemoteObject.exportObject} method is invoked, passing the
     * <code>Registry</code> instance, the specified <code>port</code>, the
     * specified <code>RMIClientSocketFactory</code>, and the specified
     * <code>RMIServerSocketFactory</code> as arguments, except that the
     * <code>Registry</code> instance is exported with a well-known object
     * identifier, an {@link ObjID} instance constructed with the value
     * {@link ObjID#REGISTRY_ID}.
     *
     * @param port port on which the registry accepts requests
     * @param csf  client-side <code>Socket</code> factory used to
     *      make connections to the registry
     * @param ssf  server-side <code>ServerSocket</code> factory
     *      used to accept connections to the registry
     * @return the registry
     * @throws RemoteException if the registry could not be exported
     * @since 1.2
     **/
    public static Registry createRegistry(int port,
                                          RMIClientSocketFactory csf,
                                          RMIServerSocketFactory ssf)
        throws RemoteException
    {
        return new RegistryImpl(port, csf, ssf);
    }
}
