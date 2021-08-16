/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.rmi.registry;


import java.util.Hashtable;
import java.util.Properties;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.naming.*;
import javax.naming.spi.NamingManager;


/**
 * A RegistryContext is a context representing a remote RMI registry.
 *
 * @author Scott Seligman
 */


public class RegistryContext implements Context, Referenceable {

    private Hashtable<String, Object> environment;
    private Registry registry;
    private String host;
    private int port;
    private static final NameParser nameParser = new AtomicNameParser();
    private static final String SOCKET_FACTORY = "com.sun.jndi.rmi.factory.socket";
    /**
     * Determines whether classes may be loaded from an arbitrary URL code base.
     */
    static final boolean trustURLCodebase;
    static {
        // System property to control whether classes may be loaded from an
        // arbitrary URL codebase
        PrivilegedAction<String> act = () -> System.getProperty(
            "com.sun.jndi.rmi.object.trustURLCodebase", "false");
        @SuppressWarnings("removal")
        String trust = AccessController.doPrivileged(act);
        trustURLCodebase = "true".equalsIgnoreCase(trust);
    }

    Reference reference = null; // ref used to create this context, if any

    // Environment property that, if set, indicates that a security
    // manager should be installed (if none is already in place).
    public static final String SECURITY_MGR =
            "java.naming.rmi.security.manager";

    /**
     * Returns a context for the registry at a given host and port.
     * If "host" is null, uses default host.
     * If "port" is non-positive, uses default port.
     * Cloning of "env" is handled by caller; see comments within
     * RegistryContextFactory.getObjectInstance(), for example.
     */
    @SuppressWarnings("unchecked")
    public RegistryContext(String host, int port, Hashtable<?, ?> env)
            throws NamingException
    {
        environment = (env == null)
                      ? new Hashtable<String, Object>(5)
                      : (Hashtable<String, Object>) env;
        if (environment.get(SECURITY_MGR) != null) {
            installSecurityMgr();
        }

        // chop off '[' and ']' in an IPv6 literal address
        if ((host != null) && (host.charAt(0) == '[')) {
            host = host.substring(1, host.length() - 1);
        }

        RMIClientSocketFactory socketFactory =
                (RMIClientSocketFactory) environment.get(SOCKET_FACTORY);
        registry = getRegistry(host, port, socketFactory);
        this.host = host;
        this.port = port;
    }

    /**
     * Returns a clone of a registry context.  The context's private state
     * is independent of the original's (so closing one context, for example,
     * won't close the other).
     */
    // %%% Alternatively, this could be done with a clone() method.
    @SuppressWarnings("unchecked") // clone()
    RegistryContext(RegistryContext ctx) {
        environment = (Hashtable<String, Object>)ctx.environment.clone();
        registry = ctx.registry;
        host = ctx.host;
        port = ctx.port;
        reference = ctx.reference;
    }

    @SuppressWarnings("deprecation")
    protected void finalize() {
        close();
    }

    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty()) {
            return (new RegistryContext(this));
        }
        Remote obj;
        try {
            obj = registry.lookup(name.get(0));
        } catch (NotBoundException e) {
            throw (new NameNotFoundException(name.get(0)));
        } catch (RemoteException e) {
            throw (NamingException)wrapRemoteException(e).fillInStackTrace();
        }
        return (decodeObject(obj, name.getPrefix(1)));
    }

    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    /**
     * If the object to be bound is both Remote and Referenceable, binds the
     * object itself, not its Reference.
     */
    public void bind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw (new InvalidNameException(
                    "RegistryContext: Cannot bind empty name"));
        }
        try {
            registry.bind(name.get(0), encodeObject(obj, name.getPrefix(1)));
        } catch (AlreadyBoundException e) {
            NamingException ne = new NameAlreadyBoundException(name.get(0));
            ne.setRootCause(e);
            throw ne;
        } catch (RemoteException e) {
            throw (NamingException)wrapRemoteException(e).fillInStackTrace();
        }
    }

    public void bind(String name, Object obj) throws NamingException {
        bind(new CompositeName(name), obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw (new InvalidNameException(
                    "RegistryContext: Cannot rebind empty name"));
        }
        try {
            registry.rebind(name.get(0), encodeObject(obj, name.getPrefix(1)));
        } catch (RemoteException e) {
            throw (NamingException)wrapRemoteException(e).fillInStackTrace();
        }
    }

    public void rebind(String name, Object obj) throws NamingException {
        rebind(new CompositeName(name), obj);
    }

    public void unbind(Name name) throws NamingException {
        if (name.isEmpty()) {
            throw (new InvalidNameException(
                    "RegistryContext: Cannot unbind empty name"));
        }
        try {
            registry.unbind(name.get(0));
        } catch (NotBoundException e) {
            // method is idempotent
        } catch (RemoteException e) {
            throw (NamingException)wrapRemoteException(e).fillInStackTrace();
        }
    }

    public void unbind(String name) throws NamingException {
        unbind(new CompositeName(name));
    }

    /**
     * Rename is implemented by this sequence of operations:
     * lookup, bind, unbind.  The sequence is not performed atomically.
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        bind(newName, lookup(oldName));
        unbind(oldName);
    }

    public void rename(String name, String newName) throws NamingException {
        rename(new CompositeName(name), new CompositeName(newName));
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws
            NamingException {
        if (!name.isEmpty()) {
            throw (new InvalidNameException(
                    "RegistryContext: can only list \"\""));
        }
        try {
            String[] names = registry.list();
            return (new NameClassPairEnumeration(names));
        } catch (RemoteException e) {
            throw (NamingException)wrapRemoteException(e).fillInStackTrace();
        }
    }

    public NamingEnumeration<NameClassPair> list(String name) throws
            NamingException {
        return list(new CompositeName(name));
    }

    public NamingEnumeration<Binding> listBindings(Name name)
            throws NamingException
    {
        if (!name.isEmpty()) {
            throw (new InvalidNameException(
                    "RegistryContext: can only list \"\""));
        }
        try {
            String[] names = registry.list();
            return (new BindingEnumeration(this, names));
        } catch (RemoteException e) {
            throw (NamingException)wrapRemoteException(e).fillInStackTrace();
        }
    }

    public NamingEnumeration<Binding> listBindings(String name) throws
            NamingException {
        return listBindings(new CompositeName(name));
    }

    public void destroySubcontext(Name name) throws NamingException {
        throw (new OperationNotSupportedException());
    }

    public void destroySubcontext(String name) throws NamingException {
        throw (new OperationNotSupportedException());
    }

    public Context createSubcontext(Name name) throws NamingException {
        throw (new OperationNotSupportedException());
    }

    public Context createSubcontext(String name) throws NamingException {
        throw (new OperationNotSupportedException());
    }

    public Object lookupLink(Name name) throws NamingException {
        return lookup(name);
    }

    public Object lookupLink(String name) throws NamingException {
        return lookup(name);
    }

    public NameParser getNameParser(Name name) throws NamingException {
        return nameParser;
    }

    public NameParser getNameParser(String name) throws NamingException {
        return nameParser;
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        Name result = (Name)prefix.clone();
        return result.addAll(name);
    }

    public String composeName(String name, String prefix)
            throws NamingException
    {
        return composeName(new CompositeName(name),
                           new CompositeName(prefix)).toString();
    }

    public Object removeFromEnvironment(String propName)
            throws NamingException
    {
        return environment.remove(propName);
    }

    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException
    {
        if (propName.equals(SECURITY_MGR)) {
            installSecurityMgr();
        }
        return environment.put(propName, propVal);
    }

    @SuppressWarnings("unchecked") // clone()
    public Hashtable<String, Object> getEnvironment() throws NamingException {
        return (Hashtable<String, Object>)environment.clone();
    }

    public void close() {
        environment = null;
        registry = null;
        // &&& If we were caching registry connections, we would probably
        // uncache this one now.
    }

    public String getNameInNamespace() {
        return ""; // Registry has an empty name
    }

    /**
     * Returns an RMI registry reference for this context.
     *<p>
     * If this context was created from a reference, that reference is
     * returned.  Otherwise, an exception is thrown if the registry's
     * host is "localhost" or the default (null).  Although this could
     * possibly make for a valid reference, it's far more likely to be
     * an easily made error.
     *
     * @see RegistryContextFactory
     */
    public Reference getReference() throws NamingException {
        if (reference != null) {
            return (Reference)reference.clone();  // %%% clone the addrs too?
        }
        if (host == null || host.equals("localhost")) {
            throw (new ConfigurationException(
                    "Cannot create a reference for an RMI registry whose " +
                    "host was unspecified or specified as \"localhost\""));
        }
        String url = "rmi://";

        // Enclose IPv6 literal address in '[' and ']'
        url = (host.indexOf(':') > -1) ? url + "[" + host + "]" :
                                         url + host;
        if (port > 0) {
            url += ":" + Integer.toString(port);
        }
        RefAddr addr = new StringRefAddr(RegistryContextFactory.ADDRESS_TYPE,
                                         url);
        return (new Reference(RegistryContext.class.getName(),
                              addr,
                              RegistryContextFactory.class.getName(),
                              null));
    }


    /**
     * Wrap a RemoteException inside a NamingException.
     */
    public static NamingException wrapRemoteException(RemoteException re) {

        NamingException ne;

        if (re instanceof ConnectException) {
            ne = new ServiceUnavailableException();

        } else if (re instanceof AccessException) {
            ne = new NoPermissionException();

        } else if (re instanceof StubNotFoundException ||
                   re instanceof UnknownHostException) {
            ne = new ConfigurationException();

        } else if (re instanceof ExportException ||
                   re instanceof ConnectIOException ||
                   re instanceof MarshalException ||
                   re instanceof UnmarshalException ||
                   re instanceof NoSuchObjectException) {
            ne = new CommunicationException();

        } else if (re instanceof ServerException &&
                   re.detail instanceof RemoteException) {
            ne = wrapRemoteException((RemoteException)re.detail);

        } else {
            ne = new NamingException();
        }
        ne.setRootCause(re);
        return ne;
    }

    /**
     * Returns the registry at a given host, port and socket factory.
     * If "host" is null, uses default host.
     * If "port" is non-positive, uses default port.
     * If "socketFactory" is null, uses the default socket.
     */
    private static Registry getRegistry(String host, int port,
                RMIClientSocketFactory socketFactory)
            throws NamingException
    {
        // %%% We could cache registry connections here.  The transport layer
        // may already reuse connections.
        try {
            if (socketFactory == null) {
                return LocateRegistry.getRegistry(host, port);
            } else {
                return LocateRegistry.getRegistry(host, port, socketFactory);
            }
        } catch (RemoteException e) {
            throw (NamingException)wrapRemoteException(e).fillInStackTrace();
        }
    }

    /**
     * Attempts to install a security manager if none is currently in
     * place.
     */
    @SuppressWarnings("removal")
    private static void installSecurityMgr() {

        try {
            System.setSecurityManager(new SecurityManager());
        } catch (Exception e) {
        }
    }

    /**
     * Encodes an object prior to binding it in the registry.  First,
     * NamingManager.getStateToBind() is invoked.  If the resulting
     * object is Remote, it is returned.  If it is a Reference or
     * Referenceable, the reference is wrapped in a Remote object.
     * Otherwise, an exception is thrown.
     *
     * @param name      The object's name relative to this context.
     */
    private Remote encodeObject(Object obj, Name name)
            throws NamingException, RemoteException
    {
        obj = NamingManager.getStateToBind(obj, name, this, environment);

        if (obj instanceof Remote) {
            return (Remote)obj;
        }
        if (obj instanceof Reference) {
            return (new ReferenceWrapper((Reference)obj));
        }
        if (obj instanceof Referenceable) {
            return (new ReferenceWrapper(((Referenceable)obj).getReference()));
        }
        throw (new IllegalArgumentException(
                "RegistryContext: " +
                "object to bind must be Remote, Reference, or Referenceable"));
    }

    /**
     * Decodes an object that has been retrieved from the registry.
     * First, if the object is a RemoteReference, the Reference is
     * unwrapped.  Then, NamingManager.getObjectInstance() is invoked.
     *
     * @param name      The object's name relative to this context.
     */
    private Object decodeObject(Remote r, Name name) throws NamingException {
        try {
            Object obj = (r instanceof RemoteReference)
                        ? ((RemoteReference)r).getReference()
                        : (Object)r;

            /*
             * Classes may only be loaded from an arbitrary URL codebase when
             * the system property com.sun.jndi.rmi.object.trustURLCodebase
             * has been set to "true".
             */

            // Use reference if possible
            Reference ref = null;
            if (obj instanceof Reference) {
                ref = (Reference) obj;
            } else if (obj instanceof Referenceable) {
                ref = ((Referenceable)(obj)).getReference();
            }

            if (ref != null && ref.getFactoryClassLocation() != null &&
                !trustURLCodebase) {
                throw new ConfigurationException(
                    "The object factory is untrusted. Set the system property" +
                    " 'com.sun.jndi.rmi.object.trustURLCodebase' to 'true'.");
            }
            return NamingManager.getObjectInstance(obj, name, this,
                                                   environment);
        } catch (NamingException e) {
            throw e;
        } catch (RemoteException e) {
            throw (NamingException)
                wrapRemoteException(e).fillInStackTrace();
        } catch (Exception e) {
            NamingException ne = new NamingException();
            ne.setRootCause(e);
            throw ne;
        }
    }

}


/**
 * A name parser for case-sensitive atomic names.
 */
class AtomicNameParser implements NameParser {
    private static final Properties syntax = new Properties();

    public Name parse(String name) throws NamingException {
        return (new CompoundName(name, syntax));
    }
}


/**
 * An enumeration of name / class-name pairs.
 */
class NameClassPairEnumeration implements NamingEnumeration<NameClassPair> {
    private final String[] names;
    private int nextName;       // index into "names"

    NameClassPairEnumeration(String[] names) {
        this.names = names;
        nextName = 0;
    }

    public boolean hasMore() {
        return (nextName < names.length);
    }

    public NameClassPair next() throws NamingException {
        if (!hasMore()) {
            throw (new java.util.NoSuchElementException());
        }
        // Convert name to a one-element composite name, so embedded
        // meta-characters are properly escaped.
        String name = names[nextName++];
        Name cname = (new CompositeName()).add(name);
        NameClassPair ncp = new NameClassPair(cname.toString(),
                                            "java.lang.Object");
        ncp.setNameInNamespace(name);
        return ncp;
    }

    public boolean hasMoreElements() {
        return hasMore();
    }

    public NameClassPair nextElement() {
        try {
            return next();
        } catch (NamingException e) {   // should never happen
            throw (new java.util.NoSuchElementException(
                    "javax.naming.NamingException was thrown"));
        }
    }

    public void close() {
        nextName = names.length;
    }
}


/**
 * An enumeration of Bindings.
 *
 * The actual registry lookups are performed when next() is called.  It would
 * be nicer to defer this until the object (or its class name) is actually
 * requested.  The problem with that approach is that Binding.getObject()
 * cannot throw NamingException.
 */
class BindingEnumeration implements NamingEnumeration<Binding> {
    private RegistryContext ctx;
    private final String[] names;
    private int nextName;       // index into "names"

    BindingEnumeration(RegistryContext ctx, String[] names) {
        // Clone ctx in case someone closes it before we're through.
        this.ctx = new RegistryContext(ctx);
        this.names = names;
        nextName = 0;
    }

    @SuppressWarnings("deprecation")
    protected void finalize() {
        ctx.close();
    }

    public boolean hasMore() {
        if (nextName >= names.length) {
            ctx.close();
        }
        return (nextName < names.length);
    }

    public Binding next() throws NamingException {
        if (!hasMore()) {
            throw (new java.util.NoSuchElementException());
        }
        // Convert name to a one-element composite name, so embedded
        // meta-characters are properly escaped.
        String name = names[nextName++];
        Name cname = (new CompositeName()).add(name);

        Object obj = ctx.lookup(cname);
        String cnameStr = cname.toString();
        Binding binding = new Binding(cnameStr, obj);
        binding.setNameInNamespace(cnameStr);
        return binding;
    }

    public boolean hasMoreElements() {
        return hasMore();
    }

    public Binding nextElement() {
        try {
            return next();
        } catch (NamingException e) {
            throw (new java.util.NoSuchElementException(
                    "javax.naming.NamingException was thrown"));
        }
    }

    @SuppressWarnings("deprecation")
    public void close () {
        finalize();
    }
}
