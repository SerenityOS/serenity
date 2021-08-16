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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;
import java.util.*;
import java.security.AccessControlException;
import java.security.Permission;
import java.rmi.server.RMIClassLoader;
import java.security.PrivilegedAction;

/**
 * MarshalInputStream is an extension of ObjectInputStream.  When resolving
 * a class, it reads an object from the stream written by a corresponding
 * MarshalOutputStream.  If the class to be resolved is not available
 * locally, from the first class loader on the execution stack, or from the
 * context class loader of the current thread, it will attempt to load the
 * class from the location annotated by the sending MarshalOutputStream.
 * This location object must be a string representing a path of URLs.
 *
 * A new MarshalInputStream should be created to deserialize remote objects or
 * graphs containing remote objects.  Objects are created from the stream
 * using the ObjectInputStream.readObject method.
 *
 * @author      Peter Jones
 */
public class MarshalInputStream extends ObjectInputStream {

    /**
     * Value of "java.rmi.server.useCodebaseOnly" property,
     * as cached at class initialization time.
     *
     * The default value is true. That is, the value is true
     * if the property is absent or is not equal to "false".
     * The value is only false when the property is present
     * and is equal to "false".
     */
    @SuppressWarnings("removal")
    private static final boolean useCodebaseOnlyProperty =
        ! java.security.AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(
                "java.rmi.server.useCodebaseOnly", "true"))
            .equalsIgnoreCase("false");

    /** table to hold sun classes to which access is explicitly permitted */
    protected static Map<String, Class<?>> permittedSunClasses
        = new HashMap<>(3);

    /** if true, don't try superclass first in resolveClass() */
    private boolean skipDefaultResolveClass = false;

    /** callbacks to make when done() called: maps Object to Runnable */
    private final Map<Object, Runnable> doneCallbacks
        = new HashMap<>(3);

    /**
     * if true, load classes (if not available locally) only from the
     * URL specified by the "java.rmi.server.codebase" property.
     */
    private boolean useCodebaseOnly = useCodebaseOnlyProperty;

    /*
     * During parameter unmarshalling RMI needs to explicitly permit
     * access to sun.* stub classes
     */
    static {
        try {
            String registry = "sun.rmi.registry.RegistryImpl_Stub";
            permittedSunClasses.put(registry, Class.forName(registry));
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError("Missing system class: " +
                                           e.getMessage());
        }
    }

    /**
     * Create a new MarshalInputStream object.
     */
    public MarshalInputStream(InputStream in)
        throws IOException, StreamCorruptedException
    {
        super(in);
                    }

    /**
     * Returns a callback previously registered via the setDoneCallback
     * method with given key, or null if no callback has yet been registered
     * with that key.
     */
    public Runnable getDoneCallback(Object key) {
        return doneCallbacks.get(key);                 // not thread-safe
    }

    /**
     * Registers a callback to make when this stream's done() method is
     * invoked, along with a key for retrieving the same callback instance
     * subsequently from the getDoneCallback method.
     */
    public void setDoneCallback(Object key, Runnable callback) {
        //assert(!doneCallbacks.contains(key));
        doneCallbacks.put(key, callback);               // not thread-safe
    }

    /**
     * Indicates that the user of this MarshalInputStream is done reading
     * objects from it, so all callbacks registered with the setDoneCallback
     * method should now be (synchronously) executed.  When this method
     * returns, there are no more callbacks registered.
     *
     * This method is implicitly invoked by close() before it delegates to
     * the superclass's close method.
     */
    public void done() {
        Iterator<Runnable> iter = doneCallbacks.values().iterator();
        while (iter.hasNext()) {                        // not thread-safe
            Runnable callback = iter.next();
            callback.run();
        }
        doneCallbacks.clear();
    }

    /**
     * Closes this stream, implicitly invoking done() first.
     */
    public void close() throws IOException {
        done();
        super.close();
    }

    /**
     * resolveClass is extended to acquire (if present) the location
     * from which to load the specified class.
     * It will find, load, and return the class.
     */
    protected Class<?> resolveClass(ObjectStreamClass classDesc)
        throws IOException, ClassNotFoundException
    {
        /*
         * Always read annotation written by MarshalOutputStream
         * describing where to load class from.
         */
        Object annotation = readLocation();

        String className = classDesc.getName();

        /*
         * Unless we were told to skip this consideration, choose the
         * "default loader" to simulate the default ObjectInputStream
         * resolveClass mechanism (that is, choose the first non-platform
         * loader on the execution stack) to maximize the likelihood of
         * type compatibility with calling code.  (This consideration
         * is skipped during server parameter unmarshalling using the 1.2
         * stub protocol, because there would never be a non-null class
         * loader on the stack in that situation anyway.)
         */
        ClassLoader defaultLoader =
            skipDefaultResolveClass ? null : latestUserDefinedLoader();

        /*
         * If the "java.rmi.server.useCodebaseOnly" property was true or
         * useCodebaseOnly() was called or the annotation is not a String,
         * load from the local loader using the "java.rmi.server.codebase"
         * URL.  Otherwise, load from a loader using the codebase URL in
         * the annotation.
         */
        String codebase = null;
        if (!useCodebaseOnly && annotation instanceof String) {
            codebase = (String) annotation;
        }

        try {
            return RMIClassLoader.loadClass(codebase, className,
                                            defaultLoader);
        } catch (@SuppressWarnings("removal") AccessControlException e) {
            return checkSunClass(className, e);
        } catch (ClassNotFoundException e) {
            /*
             * Fix for 4442373: delegate to ObjectInputStream.resolveClass()
             * to resolve primitive classes.
             */
            try {
                if (Character.isLowerCase(className.charAt(0)) &&
                    className.indexOf('.') == -1)
                {
                    return super.resolveClass(classDesc);
                }
            } catch (ClassNotFoundException e2) {
            }
            throw e;
        }
    }

    /**
     * resolveProxyClass is extended to acquire (if present) the location
     * to determine the class loader to define the proxy class in.
     */
    protected Class<?> resolveProxyClass(String[] interfaces)
        throws IOException, ClassNotFoundException
    {
        /*
         * Always read annotation written by MarshalOutputStream.
         */
        Object annotation = readLocation();

        ClassLoader defaultLoader =
            skipDefaultResolveClass ? null : latestUserDefinedLoader();

        String codebase = null;
        if (!useCodebaseOnly && annotation instanceof String) {
            codebase = (String) annotation;
        }

        return RMIClassLoader.loadProxyClass(codebase, interfaces,
                                             defaultLoader);
    }

    /*
     * Returns the first non-platform class loader up the execution stack,
     * or platform class loader if only code from the platform class loader or null
     * is on the stack.
     */
    private static ClassLoader latestUserDefinedLoader() {
        return jdk.internal.misc.VM.latestUserDefinedLoader();
    }

    /**
     * Fix for 4179055: Need to assist resolving sun stubs; resolve
     * class locally if it is a "permitted" sun class
     */
    @SuppressWarnings("removal")
    private Class<?> checkSunClass(String className, AccessControlException e)
        throws AccessControlException
    {
        // ensure that we are giving out a stub for the correct reason
        Permission perm = e.getPermission();
        String name = null;
        if (perm != null) {
            name = perm.getName();
        }

        Class<?> resolvedClass = permittedSunClasses.get(className);

        // if class not permitted, throw the SecurityException
        if ((name == null) ||
            (resolvedClass == null) ||
            ((!name.equals("accessClassInPackage.sun.rmi.server")) &&
            (!name.equals("accessClassInPackage.sun.rmi.registry"))))
        {
            throw e;
        }

        return resolvedClass;
    }

    /**
     * Return the location for the class in the stream.  This method can
     * be overridden by subclasses that store this annotation somewhere
     * else than as the next object in the stream, as is done by this class.
     */
    protected Object readLocation()
        throws IOException, ClassNotFoundException
    {
        return readObject();
    }

    /**
     * Set a flag to indicate that the superclass's default resolveClass()
     * implementation should not be invoked by our resolveClass().
     */
    void skipDefaultResolveClass() {
        skipDefaultResolveClass = true;
    }

    /**
     * Disable code downloading except from the URL specified by the
     * "java.rmi.server.codebase" property.
     */
    void useCodebaseOnly() {
        useCodebaseOnly = true;
    }
}
