/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.rmi.registry;

import java.io.ObjectInputFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.net.*;
import java.rmi.*;
import java.rmi.server.ObjID;
import java.rmi.server.ServerNotActiveException;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Policy;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.text.MessageFormat;

import jdk.internal.access.SharedSecrets;
import sun.rmi.runtime.Log;
import sun.rmi.server.UnicastRef;
import sun.rmi.server.UnicastServerRef;
import sun.rmi.server.UnicastServerRef2;
import sun.rmi.transport.LiveRef;

/**
 * A "registry" exists on every node that allows RMI connections to
 * servers on that node.  The registry on a particular node contains a
 * transient database that maps names to remote objects.  When the
 * node boots, the registry database is empty.  The names stored in the
 * registry are pure and are not parsed.  A service storing itself in
 * the registry may want to prefix its name of the service by a package
 * name (although not required), to reduce name collisions in the
 * registry.
 *
 * The LocateRegistry class is used to obtain registry for different hosts.
 * <p>
 * The default RegistryImpl exported restricts access to clients on the local host
 * for the methods {@link #bind}, {@link #rebind}, {@link #unbind} by checking
 * the client host in the skeleton.
 *
 * @see java.rmi.registry.LocateRegistry
 */
public class RegistryImpl extends java.rmi.server.RemoteServer
        implements Registry
{

    /* indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = 4666870661827494597L;
    private Hashtable<String, Remote> bindings
        = new Hashtable<>(101);
    private static Hashtable<InetAddress, InetAddress> allowedAccessCache
        = new Hashtable<>(3);
    private static RegistryImpl registry;
    private static ObjID id = new ObjID(ObjID.REGISTRY_ID);

    private static ResourceBundle resources = null;

    /**
     * Property name of the RMI Registry serial filter to augment
     * the built-in list of allowed types.
     * Setting the property in the {@code conf/security/java.security} file
     * will enable the augmented filter.
     */
    private static final String REGISTRY_FILTER_PROPNAME = "sun.rmi.registry.registryFilter";

    /** Registry max depth of remote invocations. **/
    private static final int REGISTRY_MAX_DEPTH = 20;

    /** Registry maximum array size in remote invocations. **/
    private static final int REGISTRY_MAX_ARRAY_SIZE = 1_000_000;

    /**
     * The registryFilter created from the value of the {@code "sun.rmi.registry.registryFilter"}
     * property.
     */
    @SuppressWarnings("removal")
    private static final ObjectInputFilter registryFilter =
            AccessController.doPrivileged((PrivilegedAction<ObjectInputFilter>)RegistryImpl::initRegistryFilter);

    /**
     * Initialize the registryFilter from the security properties or system property; if any
     * @return an ObjectInputFilter, or null
     */
    @SuppressWarnings("deprecation")
    private static ObjectInputFilter initRegistryFilter() {
        ObjectInputFilter filter = null;
        String props = System.getProperty(REGISTRY_FILTER_PROPNAME);
        if (props == null) {
            props = Security.getProperty(REGISTRY_FILTER_PROPNAME);
        }
        if (props != null) {
            filter = SharedSecrets.getJavaObjectInputFilterAccess().createFilter2(props);
            Log regLog = Log.getLog("sun.rmi.registry", "registry", -1);
            if (regLog.isLoggable(Log.BRIEF)) {
                regLog.log(Log.BRIEF, "registryFilter = " + filter);
            }
        }
        return filter;
    }

    /**
     * Construct a new RegistryImpl on the specified port with the
     * given custom socket factory pair.
     */
    public RegistryImpl(int port,
                        RMIClientSocketFactory csf,
                        RMIServerSocketFactory ssf)
        throws RemoteException
    {
        this(port, csf, ssf, RegistryImpl::registryFilter);
    }


    /**
     * Construct a new RegistryImpl on the specified port with the
     * given custom socket factory pair and ObjectInputFilter.
     */
    @SuppressWarnings("removal")
    public RegistryImpl(int port,
                        RMIClientSocketFactory csf,
                        RMIServerSocketFactory ssf,
                        ObjectInputFilter serialFilter)
        throws RemoteException
    {
        if (port == Registry.REGISTRY_PORT && System.getSecurityManager() != null) {
            // grant permission for default port only.
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                    public Void run() throws RemoteException {
                        LiveRef lref = new LiveRef(id, port, csf, ssf);
                        setup(new UnicastServerRef2(lref, serialFilter));
                        return null;
                    }
                }, null, new SocketPermission("localhost:"+port, "listen,accept"));
            } catch (PrivilegedActionException pae) {
                throw (RemoteException)pae.getException();
            }
        } else {
            LiveRef lref = new LiveRef(id, port, csf, ssf);
            setup(new UnicastServerRef2(lref, serialFilter));
        }
    }

    /**
     * Construct a new RegistryImpl on the specified port.
     */
    @SuppressWarnings("removal")
    public RegistryImpl(int port)
        throws RemoteException
    {
        if (port == Registry.REGISTRY_PORT && System.getSecurityManager() != null) {
            // grant permission for default port only.
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                    public Void run() throws RemoteException {
                        LiveRef lref = new LiveRef(id, port);
                        setup(new UnicastServerRef(lref, RegistryImpl::registryFilter));
                        return null;
                    }
                }, null, new SocketPermission("localhost:"+port, "listen,accept"));
            } catch (PrivilegedActionException pae) {
                throw (RemoteException)pae.getException();
            }
        } else {
            LiveRef lref = new LiveRef(id, port);
            setup(new UnicastServerRef(lref, RegistryImpl::registryFilter));
        }
    }

    /*
     * Create the export the object using the parameter
     * <code>uref</code>
     */
    private void setup(UnicastServerRef uref)
        throws RemoteException
    {
        /* Server ref must be created and assigned before remote
         * object 'this' can be exported.
         */
        ref = uref;
        uref.exportObject(this, null, true);
    }

    /**
     * Returns the remote object for specified name in the registry.
     * @exception RemoteException If remote operation failed.
     * @exception NotBoundException If name is not currently bound.
     */
    public Remote lookup(String name)
        throws RemoteException, NotBoundException
    {
        synchronized (bindings) {
            Remote obj = bindings.get(name);
            if (obj == null)
                throw new NotBoundException(name);
            return obj;
        }
    }

    /**
     * Binds the name to the specified remote object.
     * @exception RemoteException If remote operation failed.
     * @exception AlreadyBoundException If name is already bound.
     */
    public void bind(String name, Remote obj)
        throws RemoteException, AlreadyBoundException, AccessException
    {
        // The access check preventing remote access is done in the skeleton
        // and is not applicable to local access.
        synchronized (bindings) {
            Remote curr = bindings.get(name);
            if (curr != null)
                throw new AlreadyBoundException(name);
            bindings.put(name, obj);
        }
    }

    /**
     * Unbind the name.
     * @exception RemoteException If remote operation failed.
     * @exception NotBoundException If name is not currently bound.
     */
    public void unbind(String name)
        throws RemoteException, NotBoundException, AccessException
    {
        // The access check preventing remote access is done in the skeleton
        // and is not applicable to local access.
        synchronized (bindings) {
            Remote obj = bindings.get(name);
            if (obj == null)
                throw new NotBoundException(name);
            bindings.remove(name);
        }
    }

    /**
     * Rebind the name to a new object, replaces any existing binding.
     * @exception RemoteException If remote operation failed.
     */
    public void rebind(String name, Remote obj)
        throws RemoteException, AccessException
    {
        // The access check preventing remote access is done in the skeleton
        // and is not applicable to local access.
        bindings.put(name, obj);
    }

    /**
     * Returns an enumeration of the names in the registry.
     * @exception RemoteException If remote operation failed.
     */
    public String[] list()
        throws RemoteException
    {
        String[] names;
        synchronized (bindings) {
            int i = bindings.size();
            names = new String[i];
            Enumeration<String> enum_ = bindings.keys();
            while ((--i) >= 0)
                names[i] = enum_.nextElement();
        }
        return names;
    }

    /**
     * Check that the caller has access to perform indicated operation.
     * The client must be on same the same host as this server.
     */
    @SuppressWarnings("removal")
    public static void checkAccess(String op) throws AccessException {

        try {
            /*
             * Get client host that this registry operation was made from.
             */
            final String clientHostName = getClientHost();
            InetAddress clientHost;

            try {
                clientHost = java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedExceptionAction<InetAddress>() {
                        public InetAddress run()
                            throws java.net.UnknownHostException
                        {
                            return InetAddress.getByName(clientHostName);
                        }
                    });
            } catch (PrivilegedActionException pae) {
                throw (java.net.UnknownHostException) pae.getException();
            }

            // if client not yet seen, make sure client allowed access
            if (allowedAccessCache.get(clientHost) == null) {

                if (clientHost.isAnyLocalAddress()) {
                    throw new AccessException(
                        op + " disallowed; origin unknown");
                }

                try {
                    final InetAddress finalClientHost = clientHost;

                    java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedExceptionAction<Void>() {
                            public Void run() throws java.io.IOException {
                                /*
                                 * if a ServerSocket can be bound to the client's
                                 * address then that address must be local
                                 */
                                (new ServerSocket(0, 10, finalClientHost)).close();
                                allowedAccessCache.put(finalClientHost,
                                                       finalClientHost);
                                return null;
                            }
                    });
                } catch (PrivilegedActionException pae) {
                    // must have been an IOException

                    throw new AccessException(
                        op + " disallowed; origin " +
                        clientHost + " is non-local host");
                }
            }
        } catch (ServerNotActiveException ex) {
            /*
             * Local call from this VM: allow access.
             */
        } catch (java.net.UnknownHostException ex) {
            throw new AccessException(op + " disallowed; origin is unknown host");
        }
    }

    public static ObjID getID() {
        return id;
    }

    /**
     * Retrieves text resources from the locale-specific properties file.
     */
    private static String getTextResource(String key) {
        if (resources == null) {
            try {
                resources = ResourceBundle.getBundle(
                    "sun.rmi.registry.resources.rmiregistry");
            } catch (MissingResourceException mre) {
            }
            if (resources == null) {
                // throwing an Error is a bit extreme, methinks
                return ("[missing resource file: " + key + "]");
            }
        }

        String val = null;
        try {
            val = resources.getString(key);
        } catch (MissingResourceException mre) {
        }

        if (val == null) {
            return ("[missing resource: " + key + "]");
        } else {
            return (val);
        }
    }

    /**
     * Convert class path specification into an array of file URLs.
     *
     * The path of the file is converted to a URI then into URL
     * form so that reserved characters can safely appear in the path.
     */
    private static URL[] pathToURLs(String path) {
        List<URL> paths = new ArrayList<>();
        for (String entry: path.split(File.pathSeparator)) {
            Path p = Paths.get(entry);
            try {
                p = p.toRealPath();
            } catch (IOException x) {
                p = p.toAbsolutePath();
            }
            try {
                paths.add(p.toUri().toURL());
            } catch (MalformedURLException e) {
                //ignore / skip entry
            }
        }
        return paths.toArray(new URL[0]);
    }

    /**
     * ObjectInputFilter to filter Registry input objects.
     * The list of acceptable classes is limited to classes normally
     * stored in a registry.
     *
     * @param filterInfo access to the class, array length, etc.
     * @return  {@link ObjectInputFilter.Status#ALLOWED} if allowed,
     *          {@link ObjectInputFilter.Status#REJECTED} if rejected,
     *          otherwise {@link ObjectInputFilter.Status#UNDECIDED}
     */
    @SuppressWarnings("removal")
    private static ObjectInputFilter.Status registryFilter(ObjectInputFilter.FilterInfo filterInfo) {
        if (registryFilter != null) {
            ObjectInputFilter.Status status = registryFilter.checkInput(filterInfo);
            if (status != ObjectInputFilter.Status.UNDECIDED) {
                // The Registry filter can override the built-in allow-list
                return status;
            }
        }

        if (filterInfo.depth() > REGISTRY_MAX_DEPTH) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> clazz = filterInfo.serialClass();
        if (clazz != null) {
            if (clazz.isArray()) {
                // Arrays are REJECTED only if they exceed the limit
                return (filterInfo.arrayLength() >= 0 && filterInfo.arrayLength() > REGISTRY_MAX_ARRAY_SIZE)
                    ? ObjectInputFilter.Status.REJECTED
                    : ObjectInputFilter.Status.UNDECIDED;
            }
            if (String.class == clazz
                    || java.lang.Number.class.isAssignableFrom(clazz)
                    || Remote.class.isAssignableFrom(clazz)
                    || java.lang.reflect.Proxy.class.isAssignableFrom(clazz)
                    || UnicastRef.class.isAssignableFrom(clazz)
                    || RMIClientSocketFactory.class.isAssignableFrom(clazz)
                    || RMIServerSocketFactory.class.isAssignableFrom(clazz)
                    || java.rmi.server.UID.class.isAssignableFrom(clazz)) {
                return ObjectInputFilter.Status.ALLOWED;
            } else {
                return ObjectInputFilter.Status.REJECTED;
            }
        }
        return ObjectInputFilter.Status.UNDECIDED;
    }

    /**
     * Return a new RegistryImpl on the requested port and export it to serve
     * registry requests. A classloader is initialized from the system property
     * "env.class.path" and a security manager is set unless one is already set.
     * <p>
     * The returned Registry is fully functional within the current process and
     * is usable for internal and testing purposes.
     *
     * @param regPort port on which the rmiregistry accepts requests;
     *                if 0, an implementation specific port is assigned
     * @return a RegistryImpl instance
     * @exception RemoteException If remote operation failed.
     * @since 9
     */
    @SuppressWarnings("removal")
    public static RegistryImpl createRegistry(int regPort) throws RemoteException {
        // Create and install the security manager if one is not installed
        // already.
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        /*
         * Fix bugid 4147561: When JDK tools are executed, the value of
         * the CLASSPATH environment variable for the shell in which they
         * were invoked is no longer incorporated into the application
         * class path; CLASSPATH's only effect is to be the value of the
         * system property "env.class.path".  To preserve the previous
         * (JDK1.1 and JDK1.2beta3) behavior of this tool, however, its
         * CLASSPATH should still be considered when resolving classes
         * being unmarshalled.  To effect this old behavior, a class
         * loader that loads from the file path specified in the
         * "env.class.path" property is created and set to be the context
         * class loader before the remote object is exported.
         */
        String envcp = System.getProperty("env.class.path");
        if (envcp == null) {
            envcp = ".";            // preserve old default behavior
        }
        URL[] urls = pathToURLs(envcp);
        ClassLoader cl = new URLClassLoader(urls);

        /*
         * Fix bugid 4242317: Classes defined by this class loader should
         * be annotated with the value of the "java.rmi.server.codebase"
         * property, not the "file:" URLs for the CLASSPATH elements.
         */
        sun.rmi.server.LoaderHandler.registerCodebaseLoader(cl);

        Thread.currentThread().setContextClassLoader(cl);

        RegistryImpl registryImpl = null;
        try {
            registryImpl = AccessController.doPrivileged(
                new PrivilegedExceptionAction<RegistryImpl>() {
                    public RegistryImpl run() throws RemoteException {
                        return new RegistryImpl(regPort);
                    }
                }, getAccessControlContext(regPort));
        } catch (PrivilegedActionException ex) {
            throw (RemoteException) ex.getException();
        }

        return registryImpl;
    }

    /**
     * Main program to start a registry. <br>
     * The port number can be specified on the command line.
     */
    public static void main(String args[])
    {
        try {
            final int regPort = (args.length >= 1) ? Integer.parseInt(args[0])
                                                   : Registry.REGISTRY_PORT;

            registry = createRegistry(regPort);

            // prevent registry from exiting
            while (true) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                }
            }
        } catch (NumberFormatException e) {
            System.err.println(MessageFormat.format(
                getTextResource("rmiregistry.port.badnumber"),
                args[0] ));
            System.err.println(MessageFormat.format(
                getTextResource("rmiregistry.usage"),
                "rmiregistry" ));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    /**
     * Generates an AccessControlContext with minimal permissions.
     * The approach used here is taken from the similar method
     * getAccessControlContext() in the sun.applet.AppletPanel class.
     */
    @SuppressWarnings("removal")
    private static AccessControlContext getAccessControlContext(int port) {
        // begin with permissions granted to all code in current policy
        PermissionCollection perms = AccessController.doPrivileged(
            new java.security.PrivilegedAction<PermissionCollection>() {
                public PermissionCollection run() {
                    CodeSource codesource = new CodeSource(null,
                        (java.security.cert.Certificate[]) null);
                    Policy p = java.security.Policy.getPolicy();
                    if (p != null) {
                        return p.getPermissions(codesource);
                    } else {
                        return new Permissions();
                    }
                }
            });

        /*
         * Anyone can connect to the registry and the registry can connect
         * to and possibly download stubs from anywhere. Downloaded stubs and
         * related classes themselves are more tightly limited by RMI.
         */
        perms.add(new SocketPermission("*", "connect,accept"));
        perms.add(new SocketPermission("localhost:"+port, "listen,accept"));

        perms.add(new RuntimePermission("accessClassInPackage.sun.jvmstat.*"));
        perms.add(new RuntimePermission("accessClassInPackage.sun.jvm.hotspot.*"));

        perms.add(new FilePermission("<<ALL FILES>>", "read"));

        /*
         * Create an AccessControlContext that consists of a single
         * protection domain with only the permissions calculated above.
         */
        ProtectionDomain pd = new ProtectionDomain(
            new CodeSource(null,
                (java.security.cert.Certificate[]) null), perms);
        return new AccessControlContext(new ProtectionDomain[] { pd });
    }
}
