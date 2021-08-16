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

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.SocketPermission;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.rmi.server.LogStream;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import sun.reflect.misc.ReflectUtil;
import sun.rmi.runtime.Log;

/**
 * <code>LoaderHandler</code> provides the implementation of the static
 * methods of the <code>java.rmi.server.RMIClassLoader</code> class.
 *
 * @author      Ann Wollrath
 * @author      Peter Jones
 * @author      Laird Dornin
 */
@SuppressWarnings("deprecation")
public final class LoaderHandler {

    /** RMI class loader log level */
    @SuppressWarnings("removal")
    static final int logLevel = LogStream.parseLevel(
        java.security.AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("sun.rmi.loader.logLevel")));

    /* loader system log */
    static final Log loaderLog =
        Log.getLog("sun.rmi.loader", "loader", LoaderHandler.logLevel);

    /**
     * value of "java.rmi.server.codebase" property, as cached at class
     * initialization time.  It may contain malformed URLs.
     */
    private static String codebaseProperty = null;
    static {
        @SuppressWarnings("removal")
        String prop = java.security.AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("java.rmi.server.codebase"));
        if (prop != null && prop.trim().length() > 0) {
            codebaseProperty = prop;
        }
    }

    /** list of URLs represented by the codebase property, if valid */
    private static URL[] codebaseURLs = null;

    /** table of class loaders that use codebase property for annotation */
    private static final Map<ClassLoader, Void> codebaseLoaders =
        Collections.synchronizedMap(new IdentityHashMap<ClassLoader, Void>(5));
    static {
        for (ClassLoader codebaseLoader = ClassLoader.getSystemClassLoader();
             codebaseLoader != null;
             codebaseLoader = codebaseLoader.getParent())
        {
            codebaseLoaders.put(codebaseLoader, null);
        }
    }

    /**
     * table mapping codebase URL path and context class loader pairs
     * to class loader instances.  Entries hold class loaders with weak
     * references, so this table does not prevent loaders from being
     * garbage collected.
     */
    private static final HashMap<LoaderKey, LoaderEntry> loaderTable
        = new HashMap<>(5);

    /** reference queue for cleared class loader entries */
    private static final ReferenceQueue<Loader> refQueue
        = new ReferenceQueue<>();

    /*
     * Disallow anyone from creating one of these.
     */
    private LoaderHandler() {}

    /**
     * Returns an array of URLs initialized with the value of the
     * java.rmi.server.codebase property as the URL path.
     */
    private static synchronized URL[] getDefaultCodebaseURLs()
        throws MalformedURLException
    {
        /*
         * If it hasn't already been done, convert the codebase property
         * into an array of URLs; this may throw a MalformedURLException.
         */
        if (codebaseURLs == null) {
            if (codebaseProperty != null) {
                codebaseURLs = pathToURLs(codebaseProperty);
            } else {
                codebaseURLs = new URL[0];
            }
        }
        return codebaseURLs;
    }

    /**
     * Load a class from a network location (one or more URLs),
     * but first try to resolve the named class through the given
     * "default loader".
     */
    public static Class<?> loadClass(String codebase, String name,
                                     ClassLoader defaultLoader)
        throws MalformedURLException, ClassNotFoundException
    {
        if (loaderLog.isLoggable(Log.BRIEF)) {
            loaderLog.log(Log.BRIEF,
                "name = \"" + name + "\", " +
                "codebase = \"" + (codebase != null ? codebase : "") + "\"" +
                (defaultLoader != null ?
                 ", defaultLoader = " + defaultLoader : ""));
        }

        URL[] urls;
        if (codebase != null) {
            urls = pathToURLs(codebase);
        } else {
            urls = getDefaultCodebaseURLs();
        }

        if (defaultLoader != null) {
            try {
                Class<?> c = loadClassForName(name, false, defaultLoader);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE,
                        "class \"" + name + "\" found via defaultLoader, " +
                        "defined by " + c.getClassLoader());
                }
                return c;
            } catch (ClassNotFoundException e) {
            }
        }

        return loadClass(urls, name);
    }

    /**
     * Returns the class annotation (representing the location for
     * a class) that RMI will use to annotate the call stream when
     * marshalling objects of the given class.
     */
    public static String getClassAnnotation(Class<?> cl) {
        String name = cl.getName();

        /*
         * Class objects for arrays of primitive types never need an
         * annotation, because they never need to be (or can be) downloaded.
         *
         * REMIND: should we (not) be annotating classes that are in
         * "java.*" packages?
         */
        int nameLength = name.length();
        if (nameLength > 0 && name.charAt(0) == '[') {
            // skip past all '[' characters (see bugid 4211906)
            int i = 1;
            while (nameLength > i && name.charAt(i) == '[') {
                i++;
            }
            if (nameLength > i && name.charAt(i) != 'L') {
                return null;
            }
        }

        /*
         * Get the class's class loader.  If it is null, the system class
         * loader, an ancestor of the base class loader (such as the loader
         * for installed extensions), return the value of the
         * "java.rmi.server.codebase" property.
         */
        ClassLoader loader = cl.getClassLoader();
        if (loader == null || codebaseLoaders.containsKey(loader)) {
            return codebaseProperty;
        }

        /*
         * Get the codebase URL path for the class loader, if it supports
         * such a notion (i.e., if it is a URLClassLoader or subclass).
         */
        String annotation = null;
        if (loader instanceof Loader) {
            /*
             * If the class loader is one of our RMI class loaders, we have
             * already computed the class annotation string, and no
             * permissions are required to know the URLs.
             */
            annotation = ((Loader) loader).getClassAnnotation();

        } else if (loader instanceof URLClassLoader) {
            try {
                URL[] urls = ((URLClassLoader) loader).getURLs();
                if (urls != null) {
                    /*
                     * If the class loader is not one of our RMI class loaders,
                     * we must verify that the current access control context
                     * has permission to know all of these URLs.
                     */
                    @SuppressWarnings("removal")
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        Permissions perms = new Permissions();
                        for (int i = 0; i < urls.length; i++) {
                            Permission p =
                                urls[i].openConnection().getPermission();
                            if (p != null) {
                                if (!perms.implies(p)) {
                                    sm.checkPermission(p);
                                    perms.add(p);
                                }
                            }
                        }
                    }

                    annotation = urlsToPath(urls);
                }
            } catch (SecurityException | IOException e) {
                /*
                 * SecurityException: If access was denied to the knowledge of
                 * the class loader's URLs, fall back to the default behavior.
                 *
                 * IOException: This shouldn't happen, although it is declared
                 * to be thrown by openConnection() and getPermission().  If it
                 * does happen, forget about this class loader's URLs and
                 * fall back to the default behavior.
                 */
            }
        }

        if (annotation != null) {
            return annotation;
        } else {
            return codebaseProperty;    // REMIND: does this make sense??
        }
    }

    /**
     * Returns a classloader that loads classes from the given codebase URL
     * path.  The parent classloader of the returned classloader is the
     * context class loader.
     */
    public static ClassLoader getClassLoader(String codebase)
        throws MalformedURLException
    {
        ClassLoader parent = getRMIContextClassLoader();

        URL[] urls;
        if (codebase != null) {
            urls = pathToURLs(codebase);
        } else {
            urls = getDefaultCodebaseURLs();
        }

        /*
         * If there is a security manager, the current access control
         * context must have the "getClassLoader" RuntimePermission.
         */
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("getClassLoader"));
        } else {
            /*
             * But if no security manager is set, disable access to
             * RMI class loaders and simply return the parent loader.
             */
            return parent;
        }

        Loader loader = lookupLoader(urls, parent);

        /*
         * Verify that the caller has permission to access this loader.
         */
        if (loader != null) {
            loader.checkPermissions();
        }

        return loader;
    }

    /**
     * Return the security context of the given class loader.
     */
    public static Object getSecurityContext(ClassLoader loader) {
        /*
         * REMIND: This is a bogus JDK1.1-compatible implementation.
         * This method should never be called by application code anyway
         * (hence the deprecation), but should it do something different
         * and perhaps more useful, like return a String or a URL[]?
         */
        if (loader instanceof Loader) {
            URL[] urls = ((Loader) loader).getURLs();
            if (urls.length > 0) {
                return urls[0];
            }
        }
        return null;
    }

    /**
     * Register a class loader as one whose classes should always be
     * annotated with the value of the "java.rmi.server.codebase" property.
     */
    public static void registerCodebaseLoader(ClassLoader loader) {
        codebaseLoaders.put(loader, null);
    }

    /**
     * Load a class from the RMI class loader corresponding to the given
     * codebase URL path in the current execution context.
     */
    private static Class<?> loadClass(URL[] urls, String name)
        throws ClassNotFoundException
    {
        ClassLoader parent = getRMIContextClassLoader();
        if (loaderLog.isLoggable(Log.VERBOSE)) {
            loaderLog.log(Log.VERBOSE,
                "(thread context class loader: " + parent + ")");
        }

        /*
         * If no security manager is set, disable access to RMI class
         * loaders and simply delegate request to the parent loader
         * (see bugid 4140511).
         */
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            try {
                Class<?> c = Class.forName(name, false, parent);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE,
                        "class \"" + name + "\" found via " +
                        "thread context class loader " +
                        "(no security manager: codebase disabled), " +
                        "defined by " + c.getClassLoader());
                }
                return c;
            } catch (ClassNotFoundException e) {
                if (loaderLog.isLoggable(Log.BRIEF)) {
                    loaderLog.log(Log.BRIEF,
                        "class \"" + name + "\" not found via " +
                        "thread context class loader " +
                        "(no security manager: codebase disabled)", e);
                }
                throw new ClassNotFoundException(e.getMessage() +
                    " (no security manager: RMI class loader disabled)",
                    e.getException());
            }
        }

        /*
         * Get or create the RMI class loader for this codebase URL path
         * and parent class loader pair.
         */
        Loader loader = lookupLoader(urls, parent);

        try {
            if (loader != null) {
                /*
                 * Verify that the caller has permission to access this loader.
                 */
                loader.checkPermissions();
            }
        } catch (SecurityException e) {
            /*
             * If the current access control context does not have permission
             * to access all of the URLs in the codebase path, wrap the
             * resulting security exception in a ClassNotFoundException, so
             * the caller can handle this outcome just like any other class
             * loading failure (see bugid 4146529).
             */
            try {
                /*
                 * But first, check to see if the named class could have been
                 * resolved without the security-offending codebase anyway;
                 * if so, return successfully (see bugids 4191926 & 4349670).
                 */
                Class<?> c = loadClassForName(name, false, parent);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE,
                        "class \"" + name + "\" found via " +
                        "thread context class loader " +
                        "(access to codebase denied), " +
                        "defined by " + c.getClassLoader());
                }
                return c;
            } catch (ClassNotFoundException unimportant) {
                /*
                 * Presumably the security exception is the more important
                 * exception to report in this case.
                 */
                if (loaderLog.isLoggable(Log.BRIEF)) {
                    loaderLog.log(Log.BRIEF,
                        "class \"" + name + "\" not found via " +
                        "thread context class loader " +
                        "(access to codebase denied)", e);
                }
                throw new ClassNotFoundException(
                    "access to class loader denied", e);
            }
        }

        try {
            Class<?> c = loadClassForName(name, false, loader);
            if (loaderLog.isLoggable(Log.VERBOSE)) {
                loaderLog.log(Log.VERBOSE,
                    "class \"" + name + "\" " + "found via codebase, " +
                    "defined by " + c.getClassLoader());
            }
            return c;
        } catch (ClassNotFoundException e) {
            if (loaderLog.isLoggable(Log.BRIEF)) {
                loaderLog.log(Log.BRIEF,
                    "class \"" + name + "\" not found via codebase", e);
            }
            throw e;
        }
    }

    /**
     * Define and return a dynamic proxy class in a class loader with
     * URLs supplied in the given location.  The proxy class will
     * implement interface classes named by the given array of
     * interface names.
     */
    public static Class<?> loadProxyClass(String codebase, String[] interfaces,
                                          ClassLoader defaultLoader)
        throws MalformedURLException, ClassNotFoundException
    {
        if (loaderLog.isLoggable(Log.BRIEF)) {
            loaderLog.log(Log.BRIEF,
                "interfaces = " + Arrays.asList(interfaces) + ", " +
                "codebase = \"" + (codebase != null ? codebase : "") + "\"" +
                (defaultLoader != null ?
                 ", defaultLoader = " + defaultLoader : ""));
        }

        /*
         * This method uses a fairly complex algorithm to load the
         * proxy class and its interface classes in order to maximize
         * the likelihood that the proxy's codebase annotation will be
         * preserved.  The algorithm is (assuming that all of the
         * proxy interface classes are public):
         *
         * If the default loader is not null, try to load the proxy
         * interfaces through that loader. If the interfaces can be
         * loaded in that loader, try to define the proxy class in an
         * RMI class loader (child of the context class loader) before
         * trying to define the proxy in the default loader.  If the
         * attempt to define the proxy class succeeds, the codebase
         * annotation is preserved.  If the attempt fails, try to
         * define the proxy class in the default loader.
         *
         * If the interface classes can not be loaded from the default
         * loader or the default loader is null, try to load them from
         * the RMI class loader.  Then try to define the proxy class
         * in the RMI class loader.
         *
         * Additionally, if any of the proxy interface classes are not
         * public, all of the non-public interfaces must reside in the
         * same class loader or it will be impossible to define the
         * proxy class (an IllegalAccessError will be thrown).  An
         * attempt to load the interfaces from the default loader is
         * made.  If the attempt fails, a second attempt will be made
         * to load the interfaces from the RMI loader. If all of the
         * non-public interfaces classes do reside in the same class
         * loader, then we attempt to define the proxy class in the
         * class loader of the non-public interfaces.  No other
         * attempt to define the proxy class will be made.
         */
        ClassLoader parent = getRMIContextClassLoader();
        if (loaderLog.isLoggable(Log.VERBOSE)) {
            loaderLog.log(Log.VERBOSE,
                "(thread context class loader: " + parent + ")");
        }

        URL[] urls;
        if (codebase != null) {
            urls = pathToURLs(codebase);
        } else {
            urls = getDefaultCodebaseURLs();
        }

        /*
         * If no security manager is set, disable access to RMI class
         * loaders and use the would-de parent instead.
         */
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            try {
                Class<?> c = loadProxyClass(interfaces, defaultLoader, parent,
                                         false);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE,
                        "(no security manager: codebase disabled) " +
                        "proxy class defined by " + c.getClassLoader());
                }
                return c;
            } catch (ClassNotFoundException e) {
                if (loaderLog.isLoggable(Log.BRIEF)) {
                    loaderLog.log(Log.BRIEF,
                        "(no security manager: codebase disabled) " +
                        "proxy class resolution failed", e);
                }
                throw new ClassNotFoundException(e.getMessage() +
                    " (no security manager: RMI class loader disabled)",
                    e.getException());
            }
        }

        /*
         * Get or create the RMI class loader for this codebase URL path
         * and parent class loader pair.
         */
        Loader loader = lookupLoader(urls, parent);

        try {
            if (loader != null) {
                /*
                 * Verify that the caller has permission to access this loader.
                 */
                loader.checkPermissions();
            }
        } catch (SecurityException e) {
            /*
             * If the current access control context does not have permission
             * to access all of the URLs in the codebase path, wrap the
             * resulting security exception in a ClassNotFoundException, so
             * the caller can handle this outcome just like any other class
             * loading failure (see bugid 4146529).
             */
            try {
                /*
                 * But first, check to see if the proxy class could have been
                 * resolved without the security-offending codebase anyway;
                 * if so, return successfully (see bugids 4191926 & 4349670).
                 */
                Class<?> c = loadProxyClass(interfaces, defaultLoader, parent,
                                            false);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE,
                        "(access to codebase denied) " +
                        "proxy class defined by " + c.getClassLoader());
                }
                return c;
            } catch (ClassNotFoundException unimportant) {
                /*
                 * Presumably the security exception is the more important
                 * exception to report in this case.
                 */
                if (loaderLog.isLoggable(Log.BRIEF)) {
                    loaderLog.log(Log.BRIEF,
                        "(access to codebase denied) " +
                        "proxy class resolution failed", e);
                }
                throw new ClassNotFoundException(
                    "access to class loader denied", e);
            }
        }

        try {
            Class<?> c = loadProxyClass(interfaces, defaultLoader, loader, true);
            if (loaderLog.isLoggable(Log.VERBOSE)) {
                loaderLog.log(Log.VERBOSE,
                              "proxy class defined by " + c.getClassLoader());
            }
            return c;
        } catch (ClassNotFoundException e) {
            if (loaderLog.isLoggable(Log.BRIEF)) {
                loaderLog.log(Log.BRIEF,
                              "proxy class resolution failed", e);
            }
            throw e;
        }
    }

    /**
     * Define a proxy class in the default loader if appropriate.
     * Define the class in an RMI class loader otherwise.  The proxy
     * class will implement classes which are named in the supplied
     * interfaceNames.
     */
    private static Class<?> loadProxyClass(String[] interfaceNames,
                                           ClassLoader defaultLoader,
                                           ClassLoader codebaseLoader,
                                           boolean preferCodebase)
        throws ClassNotFoundException
    {
        ClassLoader proxyLoader = null;
        Class<?>[] classObjs = new Class<?>[interfaceNames.length];
        boolean[] nonpublic = { false };

      defaultLoaderCase:
        if (defaultLoader != null) {
            try {
                proxyLoader =
                    loadProxyInterfaces(interfaceNames, defaultLoader,
                                        classObjs, nonpublic);
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    ClassLoader[] definingLoaders =
                        new ClassLoader[classObjs.length];
                    for (int i = 0; i < definingLoaders.length; i++) {
                        definingLoaders[i] = classObjs[i].getClassLoader();
                    }
                    loaderLog.log(Log.VERBOSE,
                        "proxy interfaces found via defaultLoader, " +
                        "defined by " + Arrays.asList(definingLoaders));
                }
            } catch (ClassNotFoundException e) {
                break defaultLoaderCase;
            }
            if (!nonpublic[0]) {
                if (preferCodebase) {
                    try {
                        return Proxy.getProxyClass(codebaseLoader, classObjs);
                    } catch (IllegalArgumentException e) {
                    }
                }
                proxyLoader = defaultLoader;
            }
            return loadProxyClass(proxyLoader, classObjs);
        }

        nonpublic[0] = false;
        proxyLoader = loadProxyInterfaces(interfaceNames, codebaseLoader,
                                          classObjs, nonpublic);
        if (loaderLog.isLoggable(Log.VERBOSE)) {
            ClassLoader[] definingLoaders = new ClassLoader[classObjs.length];
            for (int i = 0; i < definingLoaders.length; i++) {
                definingLoaders[i] = classObjs[i].getClassLoader();
            }
            loaderLog.log(Log.VERBOSE,
                "proxy interfaces found via codebase, " +
                "defined by " + Arrays.asList(definingLoaders));
        }
        if (!nonpublic[0]) {
            proxyLoader = codebaseLoader;
        }
        return loadProxyClass(proxyLoader, classObjs);
    }

    /**
     * Define a proxy class in the given class loader.  The proxy
     * class will implement the given interfaces Classes.
     */
    private static Class<?> loadProxyClass(ClassLoader loader, Class<?>[] interfaces)
        throws ClassNotFoundException
    {
        try {
            return Proxy.getProxyClass(loader, interfaces);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(
                "error creating dynamic proxy class", e);
        }
    }

    /*
     * Load Class objects for the names in the interfaces array fron
     * the given class loader.
     *
     * We pass classObjs and nonpublic arrays to avoid needing a
     * multi-element return value.  nonpublic is an array to enable
     * the method to take a boolean argument by reference.
     *
     * nonpublic array is needed to signal when the return value of
     * this method should be used as the proxy class loader.  Because
     * null represents a valid class loader, that value is
     * insufficient to signal that the return value should not be used
     * as the proxy class loader.
     */
    private static ClassLoader loadProxyInterfaces(String[] interfaces,
                                                   ClassLoader loader,
                                                   Class<?>[] classObjs,
                                                   boolean[] nonpublic)
        throws ClassNotFoundException
    {
        /* loader of a non-public interface class */
        ClassLoader nonpublicLoader = null;

        for (int i = 0; i < interfaces.length; i++) {
            Class<?> cl =
                (classObjs[i] = loadClassForName(interfaces[i], false, loader));

            if (!Modifier.isPublic(cl.getModifiers())) {
                ClassLoader current = cl.getClassLoader();
                if (loaderLog.isLoggable(Log.VERBOSE)) {
                    loaderLog.log(Log.VERBOSE,
                        "non-public interface \"" + interfaces[i] +
                        "\" defined by " + current);
                }
                if (!nonpublic[0]) {
                    nonpublicLoader = current;
                    nonpublic[0] = true;
                } else if (current != nonpublicLoader) {
                    throw new IllegalAccessError(
                        "non-public interfaces defined in different " +
                        "class loaders");
                }
            }
        }
        return nonpublicLoader;
    }

    /**
     * Convert a string containing a space-separated list of URLs into a
     * corresponding array of URL objects, throwing a MalformedURLException
     * if any of the URLs are invalid.
     */
    private static URL[] pathToURLs(String path)
        throws MalformedURLException
    {
        synchronized (pathToURLsCache) {
            Object[] v = pathToURLsCache.get(path);
            if (v != null) {
                return ((URL[])v[0]);
            }
        }
        StringTokenizer st = new StringTokenizer(path); // divide by spaces
        URL[] urls = new URL[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            urls[i] = new URL(st.nextToken());
        }
        synchronized (pathToURLsCache) {
            pathToURLsCache.put(path,
                                new Object[] {urls, new SoftReference<String>(path)});
        }
        return urls;
    }

    /** map from weak(key=string) to [URL[], soft(key)] */
    private static final Map<String, Object[]> pathToURLsCache
        = new WeakHashMap<>(5);

    /**
     * Convert an array of URL objects into a corresponding string
     * containing a space-separated list of URLs.
     *
     * Note that if the array has zero elements, the return value is
     * null, not the empty string.
     */
    private static String urlsToPath(URL[] urls) {
        if (urls.length == 0) {
            return null;
        } else if (urls.length == 1) {
            return urls[0].toExternalForm();
        } else {
            StringBuilder path = new StringBuilder(urls[0].toExternalForm());
            for (int i = 1; i < urls.length; i++) {
                path.append(' ');
                path.append(urls[i].toExternalForm());
            }
            return path.toString();
        }
    }

    /**
     * Return the class loader to be used as the parent for an RMI class
     * loader used in the current execution context.
     */
    private static ClassLoader getRMIContextClassLoader() {
        /*
         * The current implementation simply uses the current thread's
         * context class loader.
         */
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Look up the RMI class loader for the given codebase URL path
     * and the given parent class loader.  A new class loader instance
     * will be created and returned if no match is found.
     */
    @SuppressWarnings("removal")
    private static Loader lookupLoader(final URL[] urls,
                                       final ClassLoader parent)
    {
        /*
         * If the requested codebase URL path is empty, the supplied
         * parent class loader will be sufficient.
         *
         * REMIND: To be conservative, this optimization is commented out
         * for now so that it does not open a security hole in the future
         * by providing untrusted code with direct access to the public
         * loadClass() method of a class loader instance that it cannot
         * get a reference to.  (It's an unlikely optimization anyway.)
         *
         * if (urls.length == 0) {
         *     return parent;
         * }
         */

        LoaderEntry entry;
        Loader loader;

        synchronized (LoaderHandler.class) {
            /*
             * Take this opportunity to remove from the table entries
             * whose weak references have been cleared.
             */
            while ((entry = (LoaderEntry) refQueue.poll()) != null) {
                if (!entry.removed) {   // ignore entries removed below
                    loaderTable.remove(entry.key);
                }
            }

            /*
             * Look up the codebase URL path and parent class loader pair
             * in the table of RMI class loaders.
             */
            LoaderKey key = new LoaderKey(urls, parent);
            entry = loaderTable.get(key);

            if (entry == null || (loader = entry.get()) == null) {
                /*
                 * If entry was in table but it's weak reference was cleared,
                 * remove it from the table and mark it as explicitly cleared,
                 * so that new matching entry that we put in the table will
                 * not be erroneously removed when this entry is processed
                 * from the weak reference queue.
                 */
                if (entry != null) {
                    loaderTable.remove(key);
                    entry.removed = true;
                }

                /*
                 * A matching loader was not found, so create a new class
                 * loader instance for the requested codebase URL path and
                 * parent class loader.  The instance is created within an
                 * access control context retricted to the permissions
                 * necessary to load classes from its codebase URL path.
                 */
                AccessControlContext acc = getLoaderAccessControlContext(urls);
                loader = java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<Loader>() {
                        public Loader run() {
                            return new Loader(urls, parent);
                        }
                    }, acc);

                /*
                 * Finally, create an entry to hold the new loader with a
                 * weak reference and store it in the table with the key.
                 */
                entry = new LoaderEntry(key, loader);
                loaderTable.put(key, entry);
            }
        }

        return loader;
    }

    /**
     * LoaderKey holds a codebase URL path and parent class loader pair
     * used to look up RMI class loader instances in its class loader cache.
     */
    private static class LoaderKey {

        private URL[] urls;

        private ClassLoader parent;

        private int hashValue;

        public LoaderKey(URL[] urls, ClassLoader parent) {
            this.urls = urls;
            this.parent = parent;

            if (parent != null) {
                hashValue = parent.hashCode();
            }
            for (int i = 0; i < urls.length; i++) {
                hashValue ^= urls[i].hashCode();
            }
        }

        public int hashCode() {
            return hashValue;
        }

        public boolean equals(Object obj) {
            if (obj instanceof LoaderKey) {
                LoaderKey other = (LoaderKey) obj;
                if (parent != other.parent) {
                    return false;
                }
                if (urls == other.urls) {
                    return true;
                }
                if (urls.length != other.urls.length) {
                    return false;
                }
                for (int i = 0; i < urls.length; i++) {
                    if (!urls[i].equals(other.urls[i])) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * LoaderEntry contains a weak reference to an RMIClassLoader.  The
     * weak reference is registered with the private static "refQueue"
     * queue.  The entry contains the codebase URL path and parent class
     * loader key for the loader so that the mapping can be removed from
     * the table efficiently when the weak reference is cleared.
     */
    private static class LoaderEntry extends WeakReference<Loader> {

        public LoaderKey key;

        /**
         * set to true if the entry has been removed from the table
         * because it has been replaced, so it should not be attempted
         * to be removed again
         */
        public boolean removed = false;

        public LoaderEntry(LoaderKey key, Loader loader) {
            super(loader, refQueue);
            this.key = key;
        }
    }

    /**
     * Return the access control context that a loader for the given
     * codebase URL path should execute with.
     */
    @SuppressWarnings("removal")
    private static AccessControlContext getLoaderAccessControlContext(
        URL[] urls)
    {
        /*
         * The approach used here is taken from the similar method
         * getAccessControlContext() in the sun.applet.AppletPanel class.
         */
        // begin with permissions granted to all code in current policy
        PermissionCollection perms =
            java.security.AccessController.doPrivileged(
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

        // createClassLoader permission needed to create loader in context
        perms.add(new RuntimePermission("createClassLoader"));

        // add permissions to read any "java.*" property
        perms.add(new java.util.PropertyPermission("java.*","read"));

        // add permissions reuiqred to load from codebase URL path
        addPermissionsForURLs(urls, perms, true);

        /*
         * Create an AccessControlContext that consists of a single
         * protection domain with only the permissions calculated above.
         */
        ProtectionDomain pd = new ProtectionDomain(
            new CodeSource((urls.length > 0 ? urls[0] : null),
                (java.security.cert.Certificate[]) null),
            perms);
        return new AccessControlContext(new ProtectionDomain[] { pd });
    }

    /**
     * Adds to the specified permission collection the permissions
     * necessary to load classes from a loader with the specified URL
     * path; if "forLoader" is true, also adds URL-specific
     * permissions necessary for the security context that such a
     * loader operates within, such as permissions necessary for
     * granting automatic permissions to classes defined by the
     * loader.  A given permission is only added to the collection if
     * it is not already implied by the collection.
     */
    private static void addPermissionsForURLs(URL[] urls,
                                             PermissionCollection perms,
                                             boolean forLoader)
    {
        for (int i = 0; i < urls.length; i++) {
            URL url = urls[i];
            try {
                URLConnection urlConnection = url.openConnection();
                Permission p = urlConnection.getPermission();
                if (p != null) {
                    if (p instanceof FilePermission) {
                        /*
                         * If the codebase is a file, the permission required
                         * to actually read classes from the codebase URL is
                         * the permission to read all files beneath the last
                         * directory in the file path, either because JAR
                         * files can refer to other JAR files in the same
                         * directory, or because permission to read a
                         * directory is not implied by permission to read the
                         * contents of a directory, which all that might be
                         * granted.
                         */
                        String path = p.getName();
                        int endIndex = path.lastIndexOf(File.separatorChar);
                        if (endIndex != -1) {
                            path = path.substring(0, endIndex+1);
                            if (path.endsWith(File.separator)) {
                                path += "-";
                            }
                            Permission p2 = new FilePermission(path, "read");
                            if (!perms.implies(p2)) {
                                perms.add(p2);
                            }
                            perms.add(new FilePermission(path, "read"));
                        } else {
                            /*
                             * No directory separator: use permission to
                             * read the file.
                             */
                            if (!perms.implies(p)) {
                                perms.add(p);
                            }
                        }
                    } else {
                        if (!perms.implies(p)) {
                            perms.add(p);
                        }

                        /*
                         * If the purpose of these permissions is to grant
                         * them to an instance of a URLClassLoader subclass,
                         * we must add permission to connect to and accept
                         * from the host of non-"file:" URLs, otherwise the
                         * getPermissions() method of URLClassLoader will
                         * throw a security exception.
                         */
                        if (forLoader) {
                            // get URL with meaningful host component
                            URL hostURL = url;
                            for (URLConnection conn = urlConnection;
                                 conn instanceof JarURLConnection;)
                            {
                                hostURL =
                                    ((JarURLConnection) conn).getJarFileURL();
                                conn = hostURL.openConnection();
                            }
                            String host = hostURL.getHost();
                            if (host != null &&
                                p.implies(new SocketPermission(host,
                                                               "resolve")))
                            {
                                Permission p2 =
                                    new SocketPermission(host,
                                                         "connect,accept");
                                if (!perms.implies(p2)) {
                                    perms.add(p2);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                /*
                 * This shouldn't happen, although it is declared to be
                 * thrown by openConnection() and getPermission().  If it
                 * does, don't bother granting or requiring any permissions
                 * for this URL.
                 */
            }
        }
    }

    /**
     * Loader is the actual class of the RMI class loaders created
     * by the RMIClassLoader static methods.
     */
    private static class Loader extends URLClassLoader {

        /** parent class loader, kept here as an optimization */
        private ClassLoader parent;

        /** string form of loader's codebase URL path, also an optimization */
        private String annotation;

        /** permissions required to access loader through public API */
        private Permissions permissions;

        private Loader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
            this.parent = parent;

            /*
             * Precompute the permissions required to access the loader.
             */
            permissions = new Permissions();
            addPermissionsForURLs(urls, permissions, false);

            /*
             * Caching the value of class annotation string here assumes
             * that the protected method addURL() is never called on this
             * class loader.
             */
            annotation = urlsToPath(urls);
        }

        /**
         * Return the string to be annotated with all classes loaded from
         * this class loader.
         */
        public String getClassAnnotation() {
            return annotation;
        }

        /**
         * Check that the current access control context has all of the
         * permissions necessary to load classes from this loader.
         */
        private void checkPermissions() {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {           // should never be null?
                Enumeration<Permission> enum_ = permissions.elements();
                while (enum_.hasMoreElements()) {
                    sm.checkPermission(enum_.nextElement());
                }
            }
        }

        /**
         * Return the permissions to be granted to code loaded from the
         * given code source.
         */
        protected PermissionCollection getPermissions(CodeSource codesource) {
            PermissionCollection perms = super.getPermissions(codesource);
            /*
             * Grant the same permissions that URLClassLoader would grant.
             */
            return perms;
        }

        /**
         * Return a string representation of this loader (useful for
         * debugging).
         */
        public String toString() {
            return super.toString() + "[\"" + annotation + "\"]";
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (parent == null) {
                ReflectUtil.checkPackageAccess(name);
            }
            return super.loadClass(name, resolve);
        }


    }

    private static Class<?> loadClassForName(String name,
                                              boolean initialize,
                                              ClassLoader loader)
            throws ClassNotFoundException
    {
        if (loader == null) {
            ReflectUtil.checkPackageAccess(name);
        }
        return Class.forName(name, initialize, loader);
    }

}
