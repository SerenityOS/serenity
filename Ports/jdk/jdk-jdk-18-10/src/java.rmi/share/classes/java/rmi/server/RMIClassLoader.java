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

package java.rmi.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * <code>RMIClassLoader</code> comprises static methods to support
 * dynamic class loading with RMI.  Included are methods for loading
 * classes from a network location (one or more URLs) and obtaining
 * the location from which an existing class should be loaded by
 * remote parties.  These methods are used by the RMI runtime when
 * marshalling and unmarshalling classes contained in the arguments
 * and return values of remote method calls, and they also may be
 * invoked directly by applications in order to mimic RMI's dynamic
 * class loading behavior.
 *
 * <p>The implementation of the following static methods
 *
 * <ul>
 *
 * <li>{@link #loadClass(URL,String)}
 * <li>{@link #loadClass(String,String)}
 * <li>{@link #loadClass(String,String,ClassLoader)}
 * <li>{@link #loadProxyClass(String,String[],ClassLoader)}
 * <li>{@link #getClassLoader(String)}
 * <li>{@link #getClassAnnotation(Class)}
 *
 * </ul>
 *
 * is provided by an instance of {@link RMIClassLoaderSpi}, the
 * service provider interface for those methods.  When one of the
 * methods is invoked, its behavior is to delegate to a corresponding
 * method on the service provider instance.  The details of how each
 * method delegates to the provider instance is described in the
 * documentation for each particular method.
 *
 * <p>The service provider instance is chosen as follows:
 *
 * <ul>
 *
 * <li>If the system property
 * {@systemProperty java.rmi.server.RMIClassLoaderSpi} is defined, then if
 * its value equals the string <code>"default"</code>, the provider
 * instance will be the value returned by an invocation of the {@link
 * #getDefaultProviderInstance()} method, and for any other value, if
 * a class named with the value of the property can be loaded by the
 * system class loader (see {@link ClassLoader#getSystemClassLoader})
 * and that class is assignable to {@link RMIClassLoaderSpi} and has a
 * public no-argument constructor, then that constructor will be
 * invoked to create the provider instance.  If the property is
 * defined but any other of those conditions are not true, then an
 * unspecified <code>Error</code> will be thrown to code that attempts
 * to use <code>RMIClassLoader</code>, indicating the failure to
 * obtain a provider instance.
 *
 * <li>If a resource named
 * <code>META-INF/services/java.rmi.server.RMIClassLoaderSpi</code> is
 * visible to the system class loader, then the contents of that
 * resource are interpreted as a provider-configuration file, and the
 * first class name specified in that file is used as the provider
 * class name.  If a class with that name can be loaded by the system
 * class loader and that class is assignable to {@link
 * RMIClassLoaderSpi} and has a public no-argument constructor, then
 * that constructor will be invoked to create the provider instance.
 * If the resource is found but a provider cannot be instantiated as
 * described, then an unspecified <code>Error</code> will be thrown to
 * code that attempts to use <code>RMIClassLoader</code>, indicating
 * the failure to obtain a provider instance.
 *
 * <li>Otherwise, the provider instance will be the value returned by
 * an invocation of the {@link #getDefaultProviderInstance()} method.
 *
 * </ul>
 *
 * @author      Ann Wollrath
 * @author      Peter Jones
 * @author      Laird Dornin
 * @see         RMIClassLoaderSpi
 * @since       1.1
 */
public class RMIClassLoader {

    /** "default" provider instance */
    private static final RMIClassLoaderSpi defaultProvider =
        newDefaultProviderInstance();

    /** provider instance */
    @SuppressWarnings("removal")
    private static final RMIClassLoaderSpi provider =
        AccessController.doPrivileged(
            new PrivilegedAction<RMIClassLoaderSpi>() {
                public RMIClassLoaderSpi run() { return initializeProvider(); }
            });

    /*
     * Disallow anyone from creating one of these.
     */
    private RMIClassLoader() {}

    /**
     * Loads the class with the specified <code>name</code>.
     *
     * <p>This method delegates to {@link #loadClass(String,String)},
     * passing <code>null</code> as the first argument and
     * <code>name</code> as the second argument.
     *
     * @param   name the name of the class to load
     *
     * @return  the <code>Class</code> object representing the loaded class
     *
     * @throws MalformedURLException if a provider-specific URL used
     * to load classes is invalid
     *
     * @throws  ClassNotFoundException if a definition for the class
     * could not be found at the codebase location
     *
     * @deprecated replaced by <code>loadClass(String,String)</code> method
     * @see #loadClass(String,String)
     */
    @Deprecated
    public static Class<?> loadClass(String name)
        throws MalformedURLException, ClassNotFoundException
    {
        return loadClass((String) null, name);
    }

    /**
     * Loads a class from a codebase URL.
     *
     * If <code>codebase</code> is <code>null</code>, then this method
     * will behave the same as {@link #loadClass(String,String)} with a
     * <code>null</code> <code>codebase</code> and the given class name.
     *
     * <p>This method delegates to the
     * {@link RMIClassLoaderSpi#loadClass(String,String,ClassLoader)}
     * method of the provider instance, passing the result of invoking
     * {@link URL#toString} on the given URL (or <code>null</code> if
     * <code>codebase</code> is null) as the first argument,
     * <code>name</code> as the second argument,
     * and <code>null</code> as the third argument.
     *
     * @param   codebase the URL to load the class from, or <code>null</code>
     *
     * @param   name the name of the class to load
     *
     * @return  the <code>Class</code> object representing the loaded class
     *
     * @throws MalformedURLException if <code>codebase</code> is
     * <code>null</code> and a provider-specific URL used
     * to load classes is invalid
     *
     * @throws  ClassNotFoundException if a definition for the class
     * could not be found at the specified URL
     */
    public static Class<?> loadClass(URL codebase, String name)
        throws MalformedURLException, ClassNotFoundException
    {
        return provider.loadClass(
            codebase != null ? codebase.toString() : null, name, null);
    }

    /**
     * Loads a class from a codebase URL path.
     *
     * <p>This method delegates to the
     * {@link RMIClassLoaderSpi#loadClass(String,String,ClassLoader)}
     * method of the provider instance, passing <code>codebase</code>
     * as the first argument, <code>name</code> as the second argument,
     * and <code>null</code> as the third argument.
     *
     * @param   codebase the list of URLs (separated by spaces) to load
     * the class from, or <code>null</code>
     *
     * @param   name the name of the class to load
     *
     * @return  the <code>Class</code> object representing the loaded class
     *
     * @throws MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or if
     * <code>codebase</code> is <code>null</code> and a provider-specific
     * URL used to load classes is invalid
     *
     * @throws  ClassNotFoundException if a definition for the class
     * could not be found at the specified location
     *
     * @since   1.2
     */
    public static Class<?> loadClass(String codebase, String name)
        throws MalformedURLException, ClassNotFoundException
    {
        return provider.loadClass(codebase, name, null);
    }

    /**
     * Loads a class from a codebase URL path, optionally using the
     * supplied loader.
     *
     * This method should be used when the caller would like to make
     * available to the provider implementation an additional contextual
     * class loader to consider, such as the loader of a caller on the
     * stack.  Typically, a provider implementation will attempt to
     * resolve the named class using the given <code>defaultLoader</code>,
     * if specified, before attempting to resolve the class from the
     * codebase URL path.
     *
     * <p>This method delegates to the
     * {@link RMIClassLoaderSpi#loadClass(String,String,ClassLoader)}
     * method of the provider instance, passing <code>codebase</code>
     * as the first argument, <code>name</code> as the second argument,
     * and <code>defaultLoader</code> as the third argument.
     *
     * @param   codebase the list of URLs (separated by spaces) to load
     * the class from, or <code>null</code>
     *
     * @param   name the name of the class to load
     *
     * @param   defaultLoader additional contextual class loader
     * to use, or <code>null</code>
     *
     * @return  the <code>Class</code> object representing the loaded class
     *
     * @throws MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or if
     * <code>codebase</code> is <code>null</code> and a provider-specific
     * URL used to load classes is invalid
     *
     * @throws  ClassNotFoundException if a definition for the class
     * could not be found at the specified location
     *
     * @since   1.4
     */
    public static Class<?> loadClass(String codebase, String name,
                                     ClassLoader defaultLoader)
        throws MalformedURLException, ClassNotFoundException
    {
        return provider.loadClass(codebase, name, defaultLoader);
    }

    /**
     * Loads a dynamic proxy class (see {@link java.lang.reflect.Proxy})
     * that implements a set of interfaces with the given names
     * from a codebase URL path.
     *
     * <p>The interfaces will be resolved similar to classes loaded via
     * the {@link #loadClass(String,String)} method using the given
     * <code>codebase</code>.
     *
     * <p>This method delegates to the
     * {@link RMIClassLoaderSpi#loadProxyClass(String,String[],ClassLoader)}
     * method of the provider instance, passing <code>codebase</code>
     * as the first argument, <code>interfaces</code> as the second argument,
     * and <code>defaultLoader</code> as the third argument.
     *
     * @param   codebase the list of URLs (space-separated) to load
     * classes from, or <code>null</code>
     *
     * @param   interfaces the names of the interfaces for the proxy class
     * to implement
     *
     * @param   defaultLoader additional contextual class loader
     * to use, or <code>null</code>
     *
     * @return  a dynamic proxy class that implements the named interfaces
     *
     * @throws  MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and a provider-specific
     * URL used to load classes is invalid
     *
     * @throws  ClassNotFoundException if a definition for one of
     * the named interfaces could not be found at the specified location,
     * or if creation of the dynamic proxy class failed (such as if
     * {@link java.lang.reflect.Proxy#getProxyClass(ClassLoader,Class[])}
     * would throw an <code>IllegalArgumentException</code> for the given
     * interface list)
     *
     * @since   1.4
     */
    public static Class<?> loadProxyClass(String codebase, String[] interfaces,
                                          ClassLoader defaultLoader)
        throws ClassNotFoundException, MalformedURLException
    {
        return provider.loadProxyClass(codebase, interfaces, defaultLoader);
    }

    /**
     * Returns a class loader that loads classes from the given codebase
     * URL path.
     *
     * <p>The class loader returned is the class loader that the
     * {@link #loadClass(String,String)} method would use to load classes
     * for the same <code>codebase</code> argument.
     *
     * <p>This method delegates to the
     * {@link RMIClassLoaderSpi#getClassLoader(String)} method
     * of the provider instance, passing <code>codebase</code> as the argument.
     *
     * <p>If there is a security manger, its <code>checkPermission</code>
     * method will be invoked with a
     * <code>RuntimePermission("getClassLoader")</code> permission;
     * this could result in a <code>SecurityException</code>.
     * The provider implementation of this method may also perform further
     * security checks to verify that the calling context has permission to
     * connect to all of the URLs in the codebase URL path.
     *
     * @param   codebase the list of URLs (space-separated) from which
     * the returned class loader will load classes from, or <code>null</code>
     *
     * @return a class loader that loads classes from the given codebase URL
     * path
     *
     * @throws  MalformedURLException if <code>codebase</code> is
     * non-<code>null</code> and contains an invalid URL, or
     * if <code>codebase</code> is <code>null</code> and a provider-specific
     * URL used to identify the class loader is invalid
     *
     * @throws  SecurityException if there is a security manager and the
     * invocation of its <code>checkPermission</code> method fails, or
     * if the caller does not have permission to connect to all of the
     * URLs in the codebase URL path
     *
     * @since   1.3
     */
    public static ClassLoader getClassLoader(String codebase)
        throws MalformedURLException, SecurityException
    {
        return provider.getClassLoader(codebase);
    }

    /**
     * Returns the annotation string (representing a location for
     * the class definition) that RMI will use to annotate the class
     * descriptor when marshalling objects of the given class.
     *
     * <p>This method delegates to the
     * {@link RMIClassLoaderSpi#getClassAnnotation(Class)} method
     * of the provider instance, passing <code>cl</code> as the argument.
     *
     * @param   cl the class to obtain the annotation for
     *
     * @return  a string to be used to annotate the given class when
     * it gets marshalled, or <code>null</code>
     *
     * @throws  NullPointerException if <code>cl</code> is <code>null</code>
     *
     * @since   1.2
     */
    /*
     * REMIND: Should we say that the returned class annotation will or
     * should be a (space-separated) list of URLs?
     */
    public static String getClassAnnotation(Class<?> cl) {
        return provider.getClassAnnotation(cl);
    }

    /**
     * Returns the canonical instance of the default provider
     * for the service provider interface {@link RMIClassLoaderSpi}.
     * If the system property <code>java.rmi.server.RMIClassLoaderSpi</code>
     * is not defined, then the <code>RMIClassLoader</code> static
     * methods
     *
     * <ul>
     *
     * <li>{@link #loadClass(URL,String)}
     * <li>{@link #loadClass(String,String)}
     * <li>{@link #loadClass(String,String,ClassLoader)}
     * <li>{@link #loadProxyClass(String,String[],ClassLoader)}
     * <li>{@link #getClassLoader(String)}
     * <li>{@link #getClassAnnotation(Class)}
     *
     * </ul>
     *
     * will use the canonical instance of the default provider
     * as the service provider instance.
     *
     * <p>If there is a security manager, its
     * <code>checkPermission</code> method will be invoked with a
     * <code>RuntimePermission("setFactory")</code> permission; this
     * could result in a <code>SecurityException</code>.
     *
     * <p>The default service provider instance implements
     * {@link RMIClassLoaderSpi} as follows:
     *
     * <blockquote>
     *
     * <p>The <b>{@link RMIClassLoaderSpi#getClassAnnotation(Class)
     * getClassAnnotation}</b> method returns a <code>String</code>
     * representing the codebase URL path that a remote party should
     * use to download the definition for the specified class.  The
     * format of the returned string is a path of URLs separated by
     * spaces.
     *
     * The codebase string returned depends on the defining class
     * loader of the specified class:
     *
     * <ul>
     *
     * <li><p>If the class loader is the system class loader (see
     * {@link ClassLoader#getSystemClassLoader}), a parent of the
     * system class loader such as the loader used for installed
     * extensions, or the bootstrap class loader (which may be
     * represented by <code>null</code>), then the value of the
     * {@systemProperty java.rmi.server.codebase} property (or possibly an
     * earlier cached value) is returned, or
     * <code>null</code> is returned if that property is not set.
     *
     * <li><p>Otherwise, if the class loader is an instance of
     * <code>URLClassLoader</code>, then the returned string is a
     * space-separated list of the external forms of the URLs returned
     * by invoking the <code>getURLs</code> methods of the loader.  If
     * the <code>URLClassLoader</code> was created by this provider to
     * service an invocation of its <code>loadClass</code> or
     * <code>loadProxyClass</code> methods, then no permissions are
     * required to get the associated codebase string.  If it is an
     * arbitrary other <code>URLClassLoader</code> instance, then if
     * there is a security manager, its <code>checkPermission</code>
     * method will be invoked once for each URL returned by the
     * <code>getURLs</code> method, with the permission returned by
     * invoking <code>openConnection().getPermission()</code> on each
     * URL; if any of those invocations throws a
     * <code>SecurityException</code> or an <code>IOException</code>,
     * then the value of the <code>java.rmi.server.codebase</code>
     * property (or possibly an earlier cached value) is returned, or
     * <code>null</code> is returned if that property is not set.
     *
     * <li><p>Finally, if the class loader is not an instance of
     * <code>URLClassLoader</code>, then the value of the
     * <code>java.rmi.server.codebase</code> property (or possibly an
     * earlier cached value) is returned, or
     * <code>null</code> is returned if that property is not set.
     *
     * </ul>
     *
     * <p>For the implementations of the methods described below,
     * which all take a <code>String</code> parameter named
     * <code>codebase</code> that is a space-separated list of URLs,
     * each invocation has an associated <i>codebase loader</i> that
     * is identified using the <code>codebase</code> argument in
     * conjunction with the current thread's context class loader (see
     * {@link Thread#getContextClassLoader()}).  When there is a
     * security manager, this provider maintains an internal table of
     * class loader instances (which are at least instances of {@link
     * java.net.URLClassLoader}) keyed by the pair of their parent
     * class loader and their codebase URL path (an ordered list of
     * URLs).  If the <code>codebase</code> argument is <code>null</code>,
     * the codebase URL path is the value of the system property
     * <code>java.rmi.server.codebase</code> or possibly an
     * earlier cached value.  For a given codebase URL path passed as the
     * <code>codebase</code> argument to an invocation of one of the
     * below methods in a given context, the codebase loader is the
     * loader in the table with the specified codebase URL path and
     * the current thread's context class loader as its parent.  If no
     * such loader exists, then one is created and added to the table.
     * The table does not maintain strong references to its contained
     * loaders, in order to allow them and their defined classes to be
     * garbage collected when not otherwise reachable.  In order to
     * prevent arbitrary untrusted code from being implicitly loaded
     * into a virtual machine with no security manager, if there is no
     * security manager set, the codebase loader is just the current
     * thread's context class loader (the supplied codebase URL path
     * is ignored, so remote class loading is disabled).
     *
     * <p>The <b>{@link RMIClassLoaderSpi#getClassLoader(String)
     * getClassLoader}</b> method returns the codebase loader for the
     * specified codebase URL path.  If there is a security manager,
     * then if the calling context does not have permission to connect
     * to all of the URLs in the codebase URL path, a
     * <code>SecurityException</code> will be thrown.
     *
     * <p>The <b>{@link
     * RMIClassLoaderSpi#loadClass(String,String,ClassLoader)
     * loadClass}</b> method attempts to load the class with the
     * specified name as follows:
     *
     * <blockquote>
     *
     * If the <code>defaultLoader</code> argument is
     * non-<code>null</code>, it first attempts to load the class with the
     * specified <code>name</code> using the
     * <code>defaultLoader</code>, such as by evaluating
     *
     * <pre>
     *     Class.forName(name, false, defaultLoader)
     * </pre>
     *
     * If the class is successfully loaded from the
     * <code>defaultLoader</code>, that class is returned.  If an
     * exception other than <code>ClassNotFoundException</code> is
     * thrown, that exception is thrown to the caller.
     *
     * <p>Next, the <code>loadClass</code> method attempts to load the
     * class with the specified <code>name</code> using the codebase
     * loader for the specified codebase URL path.
     * If there is a security manager, then the calling context
     * must have permission to connect to all of the URLs in the
     * codebase URL path; otherwise, the current thread's context
     * class loader will be used instead of the codebase loader.
     *
     * </blockquote>
     *
     * <p>The <b>{@link
     * RMIClassLoaderSpi#loadProxyClass(String,String[],ClassLoader)
     * loadProxyClass}</b> method attempts to return a dynamic proxy
     * class with the named interface as follows:
     *
     * <blockquote>
     *
     * <p>If the <code>defaultLoader</code> argument is
     * non-<code>null</code> and all of the named interfaces can be
     * resolved through that loader, then,
     *
     * <ul>
     *
     * <li>if all of the resolved interfaces are <code>public</code>,
     * then it first attempts to obtain a dynamic proxy class (using
     * {@link
     * java.lang.reflect.Proxy#getProxyClass(ClassLoader,Class[])
     * Proxy.getProxyClass}) for the resolved interfaces defined in
     * the codebase loader; if that attempt throws an
     * <code>IllegalArgumentException</code>, it then attempts to
     * obtain a dynamic proxy class for the resolved interfaces
     * defined in the <code>defaultLoader</code>.  If both attempts
     * throw <code>IllegalArgumentException</code>, then this method
     * throws a <code>ClassNotFoundException</code>.  If any other
     * exception is thrown, that exception is thrown to the caller.
     *
     * <li>if all of the non-<code>public</code> resolved interfaces
     * are defined in the same class loader, then it attempts to
     * obtain a dynamic proxy class for the resolved interfaces
     * defined in that loader.
     *
     * <li>otherwise, a <code>LinkageError</code> is thrown (because a
     * class that implements all of the specified interfaces cannot be
     * defined in any loader).
     *
     * </ul>
     *
     * <p>Otherwise, if all of the named interfaces can be resolved
     * through the codebase loader, then,
     *
     * <ul>
     *
     * <li>if all of the resolved interfaces are <code>public</code>,
     * then it attempts to obtain a dynamic proxy class for the
     * resolved interfaces in the codebase loader.  If the attempt
     * throws an <code>IllegalArgumentException</code>, then this
     * method throws a <code>ClassNotFoundException</code>.
     *
     * <li>if all of the non-<code>public</code> resolved interfaces
     * are defined in the same class loader, then it attempts to
     * obtain a dynamic proxy class for the resolved interfaces
     * defined in that loader.
     *
     * <li>otherwise, a <code>LinkageError</code> is thrown (because a
     * class that implements all of the specified interfaces cannot be
     * defined in any loader).
     *
     * </ul>
     *
     * <p>Otherwise, a <code>ClassNotFoundException</code> is thrown
     * for one of the named interfaces that could not be resolved.
     *
     * </blockquote>
     *
     * </blockquote>
     *
     * @return  the canonical instance of the default service provider
     *
     * @throws  SecurityException if there is a security manager and the
     * invocation of its <code>checkPermission</code> method fails
     *
     * @since   1.4
     */
    public static RMIClassLoaderSpi getDefaultProviderInstance() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("setFactory"));
        }
        return defaultProvider;
    }

    /**
     * Returns the security context of the given class loader.
     *
     * @param   loader a class loader from which to get the security context
     *
     * @return  the security context
     *
     * @deprecated no replacement.  As of the Java 2 platform v1.2, RMI no
     * longer uses this method to obtain a class loader's security context.
     * @see java.lang.SecurityManager#getSecurityContext()
     */
    @Deprecated
    public static Object getSecurityContext(ClassLoader loader)
    {
        return sun.rmi.server.LoaderHandler.getSecurityContext(loader);
    }

    /**
     * Creates an instance of the default provider class.
     */
    private static RMIClassLoaderSpi newDefaultProviderInstance() {
        return new RMIClassLoaderSpi() {
            public Class<?> loadClass(String codebase, String name,
                                      ClassLoader defaultLoader)
                throws MalformedURLException, ClassNotFoundException
            {
                return sun.rmi.server.LoaderHandler.loadClass(
                    codebase, name, defaultLoader);
            }

            public Class<?> loadProxyClass(String codebase,
                                           String[] interfaces,
                                           ClassLoader defaultLoader)
                throws MalformedURLException, ClassNotFoundException
            {
                return sun.rmi.server.LoaderHandler.loadProxyClass(
                    codebase, interfaces, defaultLoader);
            }

            public ClassLoader getClassLoader(String codebase)
                throws MalformedURLException
            {
                return sun.rmi.server.LoaderHandler.getClassLoader(codebase);
            }

            public String getClassAnnotation(Class<?> cl) {
                return sun.rmi.server.LoaderHandler.getClassAnnotation(cl);
            }
        };
    }

    /**
     * Chooses provider instance, following above documentation.
     *
     * This method assumes that it has been invoked in a privileged block.
     */
    private static RMIClassLoaderSpi initializeProvider() {
        /*
         * First check for the system property being set:
         */
        String providerClassName =
            System.getProperty("java.rmi.server.RMIClassLoaderSpi");

        if (providerClassName != null) {
            if (providerClassName.equals("default")) {
                return defaultProvider;
            }

            try {
                Class<? extends RMIClassLoaderSpi> providerClass =
                    Class.forName(providerClassName, false,
                                  ClassLoader.getSystemClassLoader())
                    .asSubclass(RMIClassLoaderSpi.class);
                @SuppressWarnings("deprecation")
                RMIClassLoaderSpi result = providerClass.newInstance();
                return result;

            } catch (ClassNotFoundException e) {
                throw new NoClassDefFoundError(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (InstantiationException e) {
                throw new InstantiationError(e.getMessage());
            } catch (ClassCastException e) {
                Error error = new LinkageError(
                    "provider class not assignable to RMIClassLoaderSpi");
                error.initCause(e);
                throw error;
            }
        }

        /*
         * Next look for a provider configuration file installed:
         */
        Iterator<RMIClassLoaderSpi> iter =
            ServiceLoader.load(RMIClassLoaderSpi.class,
                               ClassLoader.getSystemClassLoader()).iterator();
        if (iter.hasNext()) {
            try {
                return iter.next();
            } catch (ClassCastException e) {
                Error error = new LinkageError(
                    "provider class not assignable to RMIClassLoaderSpi");
                error.initCause(e);
                throw error;
            }
        }

        /*
         * Finally, return the canonical instance of the default provider.
         */
        return defaultProvider;
    }
}
