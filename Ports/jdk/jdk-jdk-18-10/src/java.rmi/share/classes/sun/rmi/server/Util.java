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
package sun.rmi.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.DataOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.StubNotFoundException;
import java.rmi.registry.Registry;
import java.rmi.server.LogStream;
import java.rmi.server.ObjID;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RemoteObjectInvocationHandler;
import java.rmi.server.RemoteRef;
import java.rmi.server.RemoteStub;
import java.rmi.server.Skeleton;
import java.rmi.server.SkeletonNotFoundException;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import sun.rmi.registry.RegistryImpl;
import sun.rmi.runtime.Log;
import sun.rmi.transport.LiveRef;
import sun.rmi.transport.tcp.TCPEndpoint;

/**
 * A utility class with static methods for creating stubs/proxies and
 * skeletons for remote objects.
 */
@SuppressWarnings("deprecation")
public final class Util {

    /** "server" package log level */
    @SuppressWarnings("removal")
    static final int logLevel = LogStream.parseLevel(
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("sun.rmi.server.logLevel")));

    /** server reference log */
    public static final Log serverRefLog =
        Log.getLog("sun.rmi.server.ref", "transport", Util.logLevel);

    /** cached value of property java.rmi.server.ignoreStubClasses */
    @SuppressWarnings("removal")
    private static final boolean ignoreStubClasses =
        AccessController.doPrivileged(
            (PrivilegedAction<Boolean>) () -> Boolean.getBoolean("java.rmi.server.ignoreStubClasses"));

    /** cache of  impl classes that have no corresponding stub class */
    private static final Map<Class<?>, Void> withoutStubs =
        Collections.synchronizedMap(new WeakHashMap<Class<?>, Void>(11));

    /** parameter types for stub constructor */
    private static final Class<?>[] stubConsParamTypes = { RemoteRef.class };

    private Util() {
    }

    /**
     * Returns a proxy for the specified implClass.
     *
     * If both of the following criteria is satisfied, a dynamic proxy for
     * the specified implClass is returned (otherwise a RemoteStub instance
     * for the specified implClass is returned):
     *
     *    a) either the property java.rmi.server.ignoreStubClasses is true or
     *       a pregenerated stub class does not exist for the impl class, and
     *    b) forceStubUse is false.
     *
     * If the above criteria are satisfied, this method constructs a
     * dynamic proxy instance (that implements the remote interfaces of
     * implClass) constructed with a RemoteObjectInvocationHandler instance
     * constructed with the clientRef.
     *
     * Otherwise, this method loads the pregenerated stub class (which
     * extends RemoteStub and implements the remote interfaces of
     * implClass) and constructs an instance of the pregenerated stub
     * class with the clientRef.
     *
     * @param implClass the class to obtain remote interfaces from
     * @param clientRef the remote ref to use in the invocation handler
     * @param forceStubUse if true, forces creation of a RemoteStub
     * @throws IllegalArgumentException if implClass implements illegal
     * remote interfaces
     * @throws StubNotFoundException if problem locating/creating stub or
     * creating the dynamic proxy instance
     **/
    @SuppressWarnings("removal")
    public static Remote createProxy(Class<?> implClass,
                                     RemoteRef clientRef,
                                     boolean forceStubUse)
        throws StubNotFoundException
    {
        Class<?> remoteClass;

        try {
            remoteClass = getRemoteClass(implClass);
        } catch (ClassNotFoundException ex ) {
            throw new StubNotFoundException(
                "object does not implement a remote interface: " +
                implClass.getName());
        }

        if (forceStubUse ||
            !(ignoreStubClasses || !stubClassExists(remoteClass)))
        {
            return createStub(remoteClass, clientRef);
        }

        final ClassLoader loader = implClass.getClassLoader();
        final Class<?>[] interfaces = getRemoteInterfaces(implClass);
        final InvocationHandler handler =
            new RemoteObjectInvocationHandler(clientRef);

        /* REMIND: private remote interfaces? */

        try {
            return AccessController.doPrivileged(new PrivilegedAction<Remote>() {
                public Remote run() {
                    return (Remote) Proxy.newProxyInstance(loader,
                                                           interfaces,
                                                           handler);
                }});
        } catch (IllegalArgumentException e) {
            throw new StubNotFoundException("unable to create proxy", e);
        }
    }

    /**
     * Returns true if a stub class for the given impl class can be loaded,
     * otherwise returns false.
     *
     * @param remoteClass the class to obtain remote interfaces from
     */
    private static boolean stubClassExists(Class<?> remoteClass) {
        if (!withoutStubs.containsKey(remoteClass)) {
            try {
                Class.forName(remoteClass.getName() + "_Stub",
                              false,
                              remoteClass.getClassLoader());
                return true;

            } catch (ClassNotFoundException cnfe) {
                withoutStubs.put(remoteClass, null);
            }
        }
        return false;
    }

    /*
     * Returns the class/superclass that implements the remote interface.
     * @throws ClassNotFoundException if no class is found to have a
     * remote interface
     */
    private static Class<?> getRemoteClass(Class<?> cl)
        throws ClassNotFoundException
    {
        while (cl != null) {
            Class<?>[] interfaces = cl.getInterfaces();
            for (int i = interfaces.length -1; i >= 0; i--) {
                if (Remote.class.isAssignableFrom(interfaces[i]))
                    return cl;          // this class implements remote object
            }
            cl = cl.getSuperclass();
        }
        throw new ClassNotFoundException(
                "class does not implement java.rmi.Remote");
    }

    /**
     * Returns an array containing the remote interfaces implemented
     * by the given class.
     *
     * @param   remoteClass the class to obtain remote interfaces from
     * @throws  IllegalArgumentException if remoteClass implements
     *          any illegal remote interfaces
     * @throws  NullPointerException if remoteClass is null
     */
    private static Class<?>[] getRemoteInterfaces(Class<?> remoteClass) {
        ArrayList<Class<?>> list = new ArrayList<>();
        getRemoteInterfaces(list, remoteClass);
        return list.toArray(new Class<?>[list.size()]);
    }

    /**
     * Fills the given array list with the remote interfaces implemented
     * by the given class.
     *
     * @throws  IllegalArgumentException if the specified class implements
     *          any illegal remote interfaces
     * @throws  NullPointerException if the specified class or list is null
     */
    private static void getRemoteInterfaces(ArrayList<Class<?>> list, Class<?> cl) {
        Class<?> superclass = cl.getSuperclass();
        if (superclass != null) {
            getRemoteInterfaces(list, superclass);
        }

        Class<?>[] interfaces = cl.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            Class<?> intf = interfaces[i];
            /*
             * If it is a remote interface (if it extends from
             * java.rmi.Remote) and is not already in the list,
             * then add the interface to the list.
             */
            if (Remote.class.isAssignableFrom(intf)) {
                if (!(list.contains(intf))) {
                    Method[] methods = intf.getMethods();
                    for (int j = 0; j < methods.length; j++) {
                        checkMethod(methods[j]);
                    }
                    list.add(intf);
                }
            }
        }
    }

    /**
     * Verifies that the supplied method has at least one declared exception
     * type that is RemoteException or one of its superclasses.  If not,
     * then this method throws IllegalArgumentException.
     *
     * @throws IllegalArgumentException if m is an illegal remote method
     */
    private static void checkMethod(Method m) {
        Class<?>[] ex = m.getExceptionTypes();
        for (int i = 0; i < ex.length; i++) {
            if (ex[i].isAssignableFrom(RemoteException.class))
                return;
        }
        throw new IllegalArgumentException(
            "illegal remote method encountered: " + m);
    }

    /**
     * Creates a RemoteStub instance for the specified class, constructed
     * with the specified RemoteRef.  The supplied class must be the most
     * derived class in the remote object's superclass chain that
     * implements a remote interface.  The stub class name is the name of
     * the specified remoteClass with the suffix "_Stub".  The loading of
     * the stub class is initiated from class loader of the specified class
     * (which may be the bootstrap class loader).
     **/
    private static RemoteStub createStub(Class<?> remoteClass, RemoteRef ref)
        throws StubNotFoundException
    {
        String stubname = remoteClass.getName() + "_Stub";

        /* Make sure to use the local stub loader for the stub classes.
         * When loaded by the local loader the load path can be
         * propagated to remote clients, by the MarshalOutputStream/InStream
         * pickle methods
         */
        try {
            Class<?> stubcl =
                Class.forName(stubname, false, remoteClass.getClassLoader());
            Constructor<?> cons = stubcl.getConstructor(stubConsParamTypes);
            return (RemoteStub) cons.newInstance(new Object[] { ref });

        } catch (ClassNotFoundException e) {
            throw new StubNotFoundException(
                "Stub class not found: " + stubname, e);
        } catch (NoSuchMethodException e) {
            throw new StubNotFoundException(
                "Stub class missing constructor: " + stubname, e);
        } catch (InstantiationException e) {
            throw new StubNotFoundException(
                "Can't create instance of stub class: " + stubname, e);
        } catch (IllegalAccessException e) {
            throw new StubNotFoundException(
                "Stub class constructor not public: " + stubname, e);
        } catch (InvocationTargetException e) {
            throw new StubNotFoundException(
                "Exception creating instance of stub class: " + stubname, e);
        } catch (ClassCastException e) {
            throw new StubNotFoundException(
                "Stub class not instance of RemoteStub: " + stubname, e);
        }
    }

    /**
     * Locate and return the Skeleton for the specified remote object
     */
    static Skeleton createSkeleton(Remote object)
        throws SkeletonNotFoundException
    {
        Class<?> cl;
        try {
            cl = getRemoteClass(object.getClass());
        } catch (ClassNotFoundException ex ) {
            throw new SkeletonNotFoundException(
                "object does not implement a remote interface: " +
                object.getClass().getName());
        }

        // now try to load the skeleton based ont he name of the class
        String skelname = cl.getName() + "_Skel";
        try {
            Class<?> skelcl = Class.forName(skelname, false, cl.getClassLoader());

            return (Skeleton)skelcl.newInstance();
        } catch (ClassNotFoundException ex) {
            throw new SkeletonNotFoundException("Skeleton class not found: " +
                                                skelname, ex);
        } catch (InstantiationException ex) {
            throw new SkeletonNotFoundException("Can't create skeleton: " +
                                                skelname, ex);
        } catch (IllegalAccessException ex) {
            throw new SkeletonNotFoundException("No public constructor: " +
                                                skelname, ex);
        } catch (ClassCastException ex) {
            throw new SkeletonNotFoundException(
                "Skeleton not of correct class: " + skelname, ex);
        }
    }

    /**
     * Compute the "method hash" of a remote method.  The method hash
     * is a long containing the first 64 bits of the SHA digest from
     * the UTF encoded string of the method name and descriptor.
     */
    public static long computeMethodHash(Method m) {
        long hash = 0;
        ByteArrayOutputStream sink = new ByteArrayOutputStream(127);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            DataOutputStream out = new DataOutputStream(
                new DigestOutputStream(sink, md));

            String s = getMethodNameAndDescriptor(m);
            if (serverRefLog.isLoggable(Log.VERBOSE)) {
                serverRefLog.log(Log.VERBOSE,
                    "string used for method hash: \"" + s + "\"");
            }
            out.writeUTF(s);

            // use only the first 64 bits of the digest for the hash
            out.flush();
            byte hasharray[] = md.digest();
            for (int i = 0; i < Math.min(8, hasharray.length); i++) {
                hash += ((long) (hasharray[i] & 0xFF)) << (i * 8);
            }
        } catch (IOException ignore) {
            /* can't happen, but be deterministic anyway. */
            hash = -1;
        } catch (NoSuchAlgorithmException complain) {
            throw new SecurityException(complain.getMessage());
        }
        return hash;
    }

    /**
     * Return a string consisting of the given method's name followed by
     * its "method descriptor", as appropriate for use in the computation
     * of the "method hash".
     *
     * See section 4.3.3 of The Java Virtual Machine Specification for
     * the definition of a "method descriptor".
     */
    private static String getMethodNameAndDescriptor(Method m) {
        StringBuilder desc = new StringBuilder(m.getName());
        desc.append('(');
        Class<?>[] paramTypes = m.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            desc.append(getTypeDescriptor(paramTypes[i]));
        }
        desc.append(')');
        Class<?> returnType = m.getReturnType();
        if (returnType == void.class) { // optimization: handle void here
            desc.append('V');
        } else {
            desc.append(getTypeDescriptor(returnType));
        }
        return desc.toString();
    }

    /**
     * Get the descriptor of a particular type, as appropriate for either
     * a parameter or return type in a method descriptor.
     */
    private static String getTypeDescriptor(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == int.class) {
                return "I";
            } else if (type == boolean.class) {
                return "Z";
            } else if (type == byte.class) {
                return "B";
            } else if (type == char.class) {
                return "C";
            } else if (type == short.class) {
                return "S";
            } else if (type == long.class) {
                return "J";
            } else if (type == float.class) {
                return "F";
            } else if (type == double.class) {
                return "D";
            } else if (type == void.class) {
                return "V";
            } else {
                throw new Error("unrecognized primitive type: " + type);
            }
        } else if (type.isArray()) {
            /*
             * According to JLS 20.3.2, the getName() method on Class does
             * return the VM type descriptor format for array classes (only);
             * using that should be quicker than the otherwise obvious code:
             *
             *     return "[" + getTypeDescriptor(type.getComponentType());
             */
            return type.getName().replace('.', '/');
        } else {
            return "L" + type.getName().replace('.', '/') + ";";
        }
    }

    /**
     * Returns the binary name of the given type without package
     * qualification.  Nested types are treated no differently from
     * top-level types, so for a nested type, the returned name will
     * still be qualified with the simple name of its enclosing
     * top-level type (and perhaps other enclosing types), the
     * separator will be '$', etc.
     **/
    public static String getUnqualifiedName(Class<?> c) {
        String binaryName = c.getName();
        return binaryName.substring(binaryName.lastIndexOf('.') + 1);
    }
}
