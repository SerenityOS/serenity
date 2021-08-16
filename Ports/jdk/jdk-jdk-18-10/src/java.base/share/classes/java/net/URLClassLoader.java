/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.io.Closeable;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import jdk.internal.loader.Resource;
import jdk.internal.loader.URLClassPath;
import jdk.internal.access.SharedSecrets;
import jdk.internal.perf.PerfCounter;
import sun.net.www.ParseUtil;
import sun.security.util.SecurityConstants;

/**
 * This class loader is used to load classes and resources from a search
 * path of URLs referring to both JAR files and directories. Any {@code jar:}
 * scheme URL (see {@link java.net.JarURLConnection}) is assumed to refer to a
 * JAR file.  Any {@code file:} scheme URL that ends with a '/' is assumed to
 * refer to a directory. Otherwise, the URL is assumed to refer to a JAR file
 * which will be opened as needed.
 * <p>
 * This class loader supports the loading of classes and resources from the
 * contents of a <a href="../util/jar/JarFile.html#multirelease">multi-release</a>
 * JAR file that is referred to by a given URL.
 * <p>
 * The AccessControlContext of the thread that created the instance of
 * URLClassLoader will be used when subsequently loading classes and
 * resources.
 * <p>
 * The classes that are loaded are by default granted permission only to
 * access the URLs specified when the URLClassLoader was created.
 *
 * @author  David Connelly
 * @since   1.2
 */
public class URLClassLoader extends SecureClassLoader implements Closeable {
    /* The search path for classes and resources */
    private final URLClassPath ucp;

    /* The context to be used when loading classes and resources */
    @SuppressWarnings("removal")
    private final AccessControlContext acc;

    /**
     * Constructs a new URLClassLoader for the given URLs. The URLs will be
     * searched in the order specified for classes and resources after first
     * searching in the specified parent class loader.  Any {@code jar:}
     * scheme URL is assumed to refer to a JAR file.  Any {@code file:} scheme
     * URL that ends with a '/' is assumed to refer to a directory.  Otherwise,
     * the URL is assumed to refer to a JAR file which will be downloaded and
     * opened as needed.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's {@code checkCreateClassLoader} method
     * to ensure creation of a class loader is allowed.
     *
     * @param      urls the URLs from which to load classes and resources
     * @param      parent the parent class loader for delegation
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkCreateClassLoader} method doesn't allow
     *             creation of a class loader.
     * @throws     NullPointerException if {@code urls} or any of its
     *             elements is {@code null}.
     * @see SecurityManager#checkCreateClassLoader
     */
    @SuppressWarnings("removal")
    public URLClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urls, acc);
    }

    URLClassLoader(String name, URL[] urls, ClassLoader parent,
                   @SuppressWarnings("removal") AccessControlContext acc) {
        super(name, parent);
        this.acc = acc;
        this.ucp = new URLClassPath(urls, acc);
    }

    /**
     * Constructs a new URLClassLoader for the specified URLs using the
     * default delegation parent {@code ClassLoader}. The URLs will
     * be searched in the order specified for classes and resources after
     * first searching in the parent class loader. Any URL that ends with
     * a '/' is assumed to refer to a directory. Otherwise, the URL is
     * assumed to refer to a JAR file which will be downloaded and opened
     * as needed.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's {@code checkCreateClassLoader} method
     * to ensure creation of a class loader is allowed.
     *
     * @param      urls the URLs from which to load classes and resources
     *
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkCreateClassLoader} method doesn't allow
     *             creation of a class loader.
     * @throws     NullPointerException if {@code urls} or any of its
     *             elements is {@code null}.
     * @see SecurityManager#checkCreateClassLoader
     */
    @SuppressWarnings("removal")
    public URLClassLoader(URL[] urls) {
        super();
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urls, acc);
    }

    URLClassLoader(URL[] urls, @SuppressWarnings("removal") AccessControlContext acc) {
        super();
        this.acc = acc;
        this.ucp = new URLClassPath(urls, acc);
    }

    /**
     * Constructs a new URLClassLoader for the specified URLs, parent
     * class loader, and URLStreamHandlerFactory. The parent argument
     * will be used as the parent class loader for delegation. The
     * factory argument will be used as the stream handler factory to
     * obtain protocol handlers when creating new jar URLs.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's {@code checkCreateClassLoader} method
     * to ensure creation of a class loader is allowed.
     *
     * @param  urls the URLs from which to load classes and resources
     * @param  parent the parent class loader for delegation
     * @param  factory the URLStreamHandlerFactory to use when creating URLs
     *
     * @throws SecurityException  if a security manager exists and its
     *         {@code checkCreateClassLoader} method doesn't allow
     *         creation of a class loader.
     * @throws NullPointerException if {@code urls} or any of its
     *         elements is {@code null}.
     * @see SecurityManager#checkCreateClassLoader
     */
    @SuppressWarnings("removal")
    public URLClassLoader(URL[] urls, ClassLoader parent,
                          URLStreamHandlerFactory factory) {
        super(parent);
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urls, factory, acc);
    }


    /**
     * Constructs a new named {@code URLClassLoader} for the specified URLs.
     * The URLs will be searched in the order specified for classes
     * and resources after first searching in the specified parent class loader.
     * Any URL that ends with a '/' is assumed to refer to a directory.
     * Otherwise, the URL is assumed to refer to a JAR file which will be
     * downloaded and opened as needed.
     *
     * @param  name class loader name; or {@code null} if not named
     * @param  urls the URLs from which to load classes and resources
     * @param  parent the parent class loader for delegation
     *
     * @throws IllegalArgumentException if the given name is empty.
     * @throws NullPointerException if {@code urls} or any of its
     *         elements is {@code null}.
     *
     * @throws SecurityException if a security manager exists and its
     *         {@link SecurityManager#checkCreateClassLoader()} method doesn't
     *         allow creation of a class loader.
     *
     * @since 9
     */
    @SuppressWarnings("removal")
    public URLClassLoader(String name,
                          URL[] urls,
                          ClassLoader parent) {
        super(name, parent);
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urls, acc);
    }

    /**
     * Constructs a new named {@code URLClassLoader} for the specified URLs,
     * parent class loader, and URLStreamHandlerFactory.
     * The parent argument will be used as the parent class loader for delegation.
     * The factory argument will be used as the stream handler factory to
     * obtain protocol handlers when creating new jar URLs.
     *
     * @param  name class loader name; or {@code null} if not named
     * @param  urls the URLs from which to load classes and resources
     * @param  parent the parent class loader for delegation
     * @param  factory the URLStreamHandlerFactory to use when creating URLs
     *
     * @throws IllegalArgumentException if the given name is empty.
     * @throws NullPointerException if {@code urls} or any of its
     *         elements is {@code null}.
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code checkCreateClassLoader} method doesn't allow
     *         creation of a class loader.
     *
     * @since 9
     */
    @SuppressWarnings("removal")
    public URLClassLoader(String name, URL[] urls, ClassLoader parent,
                          URLStreamHandlerFactory factory) {
        super(name, parent);
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urls, factory, acc);
    }

    /* A map (used as a set) to keep track of closeable local resources
     * (either JarFiles or FileInputStreams). We don't care about
     * Http resources since they don't need to be closed.
     *
     * If the resource is coming from a jar file
     * we keep a (weak) reference to the JarFile object which can
     * be closed if URLClassLoader.close() called. Due to jar file
     * caching there will typically be only one JarFile object
     * per underlying jar file.
     *
     * For file resources, which is probably a less common situation
     * we have to keep a weak reference to each stream.
     */

    private WeakHashMap<Closeable,Void>
        closeables = new WeakHashMap<>();

    /**
     * Returns an input stream for reading the specified resource.
     * If this loader is closed, then any resources opened by this method
     * will be closed.
     *
     * <p> The search order is described in the documentation for {@link
     * #getResource(String)}.  </p>
     *
     * @param  name
     *         The resource name
     *
     * @return  An input stream for reading the resource, or {@code null}
     *          if the resource could not be found
     *
     * @throws  NullPointerException If {@code name} is {@code null}
     *
     * @since  1.7
     */
    public InputStream getResourceAsStream(String name) {
        Objects.requireNonNull(name);
        URL url = getResource(name);
        try {
            if (url == null) {
                return null;
            }
            URLConnection urlc = url.openConnection();
            InputStream is = urlc.getInputStream();
            if (urlc instanceof JarURLConnection juc) {
                JarFile jar = juc.getJarFile();
                synchronized (closeables) {
                    if (!closeables.containsKey(jar)) {
                        closeables.put(jar, null);
                    }
                }
            } else if (urlc instanceof sun.net.www.protocol.file.FileURLConnection) {
                synchronized (closeables) {
                    closeables.put(is, null);
                }
            }
            return is;
        } catch (IOException e) {
            return null;
        }
    }

   /**
    * Closes this URLClassLoader, so that it can no longer be used to load
    * new classes or resources that are defined by this loader.
    * Classes and resources defined by any of this loader's parents in the
    * delegation hierarchy are still accessible. Also, any classes or resources
    * that are already loaded, are still accessible.
    * <p>
    * In the case of jar: and file: URLs, it also closes any files
    * that were opened by it. If another thread is loading a
    * class when the {@code close} method is invoked, then the result of
    * that load is undefined.
    * <p>
    * The method makes a best effort attempt to close all opened files,
    * by catching {@link IOException}s internally. Unchecked exceptions
    * and errors are not caught. Calling close on an already closed
    * loader has no effect.
    *
    * @throws    IOException if closing any file opened by this class loader
    * resulted in an IOException. Any such exceptions are caught internally.
    * If only one is caught, then it is re-thrown. If more than one exception
    * is caught, then the second and following exceptions are added
    * as suppressed exceptions of the first one caught, which is then re-thrown.
    *
    * @throws    SecurityException if a security manager is set, and it denies
    *   {@link RuntimePermission}{@code ("closeClassLoader")}
    *
    * @since 1.7
    */
    public void close() throws IOException {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("closeClassLoader"));
        }
        List<IOException> errors = ucp.closeLoaders();

        // now close any remaining streams.

        synchronized (closeables) {
            Set<Closeable> keys = closeables.keySet();
            for (Closeable c : keys) {
                try {
                    c.close();
                } catch (IOException ioex) {
                    errors.add(ioex);
                }
            }
            closeables.clear();
        }

        if (errors.isEmpty()) {
            return;
        }

        IOException firstex = errors.remove(0);

        // Suppress any remaining exceptions

        for (IOException error: errors) {
            firstex.addSuppressed(error);
        }
        throw firstex;
    }

    /**
     * Appends the specified URL to the list of URLs to search for
     * classes and resources.
     * <p>
     * If the URL specified is {@code null} or is already in the
     * list of URLs, or if this loader is closed, then invoking this
     * method has no effect.
     *
     * @param url the URL to be added to the search path of URLs
     */
    protected void addURL(URL url) {
        ucp.addURL(url);
    }

    /**
     * Returns the search path of URLs for loading classes and resources.
     * This includes the original list of URLs specified to the constructor,
     * along with any URLs subsequently appended by the addURL() method.
     * @return the search path of URLs for loading classes and resources.
     */
    public URL[] getURLs() {
        return ucp.getURLs();
    }

    /**
     * Finds and loads the class with the specified name from the URL search
     * path. Any URLs referring to JAR files are loaded and opened as needed
     * until the class is found.
     *
     * @param     name the name of the class
     * @return    the resulting class
     * @throws    ClassNotFoundException if the class could not be found,
     *            or if the loader is closed.
     * @throws    NullPointerException if {@code name} is {@code null}.
     */
    @SuppressWarnings("removal")
    protected Class<?> findClass(final String name)
        throws ClassNotFoundException
    {
        final Class<?> result;
        try {
            result = AccessController.doPrivileged(
                new PrivilegedExceptionAction<>() {
                    public Class<?> run() throws ClassNotFoundException {
                        String path = name.replace('.', '/').concat(".class");
                        Resource res = ucp.getResource(path, false);
                        if (res != null) {
                            try {
                                return defineClass(name, res);
                            } catch (IOException e) {
                                throw new ClassNotFoundException(name, e);
                            }
                        } else {
                            return null;
                        }
                    }
                }, acc);
        } catch (java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
        if (result == null) {
            throw new ClassNotFoundException(name);
        }
        return result;
    }

    /*
     * Retrieve the package using the specified package name.
     * If non-null, verify the package using the specified code
     * source and manifest.
     */
    private Package getAndVerifyPackage(String pkgname,
                                        Manifest man, URL url) {
        Package pkg = getDefinedPackage(pkgname);
        if (pkg != null) {
            // Package found, so check package sealing.
            if (pkg.isSealed()) {
                // Verify that code source URL is the same.
                if (!pkg.isSealed(url)) {
                    throw new SecurityException(
                        "sealing violation: package " + pkgname + " is sealed");
                }
            } else {
                // Make sure we are not attempting to seal the package
                // at this code source URL.
                if ((man != null) && isSealed(pkgname, man)) {
                    throw new SecurityException(
                        "sealing violation: can't seal package " + pkgname +
                        ": already loaded");
                }
            }
        }
        return pkg;
    }

    /*
     * Defines a Class using the class bytes obtained from the specified
     * Resource. The resulting Class must be resolved before it can be
     * used.
     */
    private Class<?> defineClass(String name, Resource res) throws IOException {
        long t0 = System.nanoTime();
        int i = name.lastIndexOf('.');
        URL url = res.getCodeSourceURL();
        if (i != -1) {
            String pkgname = name.substring(0, i);
            // Check if package already loaded.
            Manifest man = res.getManifest();
            if (getAndVerifyPackage(pkgname, man, url) == null) {
                try {
                    if (man != null) {
                        definePackage(pkgname, man, url);
                    } else {
                        definePackage(pkgname, null, null, null, null, null, null, null);
                    }
                } catch (IllegalArgumentException iae) {
                    // parallel-capable class loaders: re-verify in case of a
                    // race condition
                    if (getAndVerifyPackage(pkgname, man, url) == null) {
                        // Should never happen
                        throw new AssertionError("Cannot find package " +
                                                 pkgname);
                    }
                }
            }
        }
        // Now read the class bytes and define the class
        java.nio.ByteBuffer bb = res.getByteBuffer();
        if (bb != null) {
            // Use (direct) ByteBuffer:
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
            return defineClass(name, bb, cs);
        } else {
            byte[] b = res.getBytes();
            // must read certificates AFTER reading bytes.
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
            return defineClass(name, b, 0, b.length, cs);
        }
    }

    /**
     * Defines a new package by name in this {@code URLClassLoader}.
     * The attributes contained in the specified {@code Manifest}
     * will be used to obtain package version and sealing information.
     * For sealed packages, the additional URL specifies the code source URL
     * from which the package was loaded.
     *
     * @param name  the package name
     * @param man   the {@code Manifest} containing package version and sealing
     *              information
     * @param url   the code source url for the package, or null if none
     * @throws      IllegalArgumentException if the package name is
     *              already defined by this class loader
     * @return      the newly defined {@code Package} object
     *
     * @revised 9
     */
    protected Package definePackage(String name, Manifest man, URL url) {
        String specTitle = null, specVersion = null, specVendor = null;
        String implTitle = null, implVersion = null, implVendor = null;
        String sealed = null;
        URL sealBase = null;

        Attributes attr = SharedSecrets.javaUtilJarAccess()
                .getTrustedAttributes(man, name.replace('.', '/').concat("/"));
        if (attr != null) {
            specTitle   = attr.getValue(Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            specVendor  = attr.getValue(Name.SPECIFICATION_VENDOR);
            implTitle   = attr.getValue(Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            implVendor  = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            sealed      = attr.getValue(Name.SEALED);
        }
        attr = man.getMainAttributes();
        if (attr != null) {
            if (specTitle == null) {
                specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            }
            if (specVersion == null) {
                specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            }
            if (specVendor == null) {
                specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (implTitle == null) {
                implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (implVersion == null) {
                implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (implVendor == null) {
                implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (sealed == null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        if ("true".equalsIgnoreCase(sealed)) {
            sealBase = url;
        }
        return definePackage(name, specTitle, specVersion, specVendor,
                             implTitle, implVersion, implVendor, sealBase);
    }

    /*
     * Returns true if the specified package name is sealed according to the
     * given manifest.
     *
     * @throws SecurityException if the package name is untrusted in the manifest
     */
    private boolean isSealed(String name, Manifest man) {
        Attributes attr = SharedSecrets.javaUtilJarAccess()
                .getTrustedAttributes(man, name.replace('.', '/').concat("/"));
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Name.SEALED);
        }
        if (sealed == null) {
            if ((attr = man.getMainAttributes()) != null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    /**
     * Finds the resource with the specified name on the URL search path.
     *
     * @param name the name of the resource
     * @return a {@code URL} for the resource, or {@code null}
     * if the resource could not be found, or if the loader is closed.
     */
    public URL findResource(final String name) {
        /*
         * The same restriction to finding classes applies to resources
         */
        @SuppressWarnings("removal")
        URL url = AccessController.doPrivileged(
            new PrivilegedAction<>() {
                public URL run() {
                    return ucp.findResource(name, true);
                }
            }, acc);

        return url != null ? URLClassPath.checkURL(url) : null;
    }

    /**
     * Returns an Enumeration of URLs representing all of the resources
     * on the URL search path having the specified name.
     *
     * @param name the resource name
     * @throws    IOException if an I/O exception occurs
     * @return An {@code Enumeration} of {@code URL}s.
     *         If the loader is closed, the Enumeration contains no elements.
     */
    public Enumeration<URL> findResources(final String name)
        throws IOException
    {
        final Enumeration<URL> e = ucp.findResources(name, true);

        return new Enumeration<>() {
            private URL url = null;

            private boolean next() {
                if (url != null) {
                    return true;
                }
                do {
                    @SuppressWarnings("removal")
                    URL u = AccessController.doPrivileged(
                        new PrivilegedAction<>() {
                            public URL run() {
                                if (!e.hasMoreElements())
                                    return null;
                                return e.nextElement();
                            }
                        }, acc);
                    if (u == null)
                        break;
                    url = URLClassPath.checkURL(u);
                } while (url == null);
                return url != null;
            }

            public URL nextElement() {
                if (!next()) {
                    throw new NoSuchElementException();
                }
                URL u = url;
                url = null;
                return u;
            }

            public boolean hasMoreElements() {
                return next();
            }
        };
    }

    /**
     * Returns the permissions for the given codesource object.
     * The implementation of this method first calls super.getPermissions
     * and then adds permissions based on the URL of the codesource.
     * <p>
     * If the protocol of this URL is "jar", then the permission granted
     * is based on the permission that is required by the URL of the Jar
     * file.
     * <p>
     * If the protocol is "file" and there is an authority component, then
     * permission to connect to and accept connections from that authority
     * may be granted. If the protocol is "file"
     * and the path specifies a file, then permission to read that
     * file is granted. If protocol is "file" and the path is
     * a directory, permission is granted to read all files
     * and (recursively) all files and subdirectories contained in
     * that directory.
     * <p>
     * If the protocol is not "file", then permission
     * to connect to and accept connections from the URL's host is granted.
     * @param codesource the codesource
     * @throws    NullPointerException if {@code codesource} is {@code null}.
     * @return the permissions granted to the codesource
     */
    @SuppressWarnings("removal")
    protected PermissionCollection getPermissions(CodeSource codesource)
    {
        PermissionCollection perms = super.getPermissions(codesource);

        URL url = codesource.getLocation();

        Permission p;
        URLConnection urlConnection;

        try {
            urlConnection = url.openConnection();
            p = urlConnection.getPermission();
        } catch (java.io.IOException ioe) {
            p = null;
            urlConnection = null;
        }

        if (p instanceof FilePermission) {
            // if the permission has a separator char on the end,
            // it means the codebase is a directory, and we need
            // to add an additional permission to read recursively
            String path = p.getName();
            if (path.endsWith(File.separator)) {
                path += "-";
                p = new FilePermission(path, SecurityConstants.FILE_READ_ACTION);
            }
        } else if ((p == null) && (url.getProtocol().equals("file"))) {
            String path = url.getFile().replace('/', File.separatorChar);
            path = ParseUtil.decode(path);
            if (path.endsWith(File.separator))
                path += "-";
            p = new FilePermission(path, SecurityConstants.FILE_READ_ACTION);
        } else {
            /**
             * Not loading from a 'file:' URL so we want to give the class
             * permission to connect to and accept from the remote host
             * after we've made sure the host is the correct one and is valid.
             */
            URL locUrl = url;
            if (urlConnection instanceof JarURLConnection) {
                locUrl = ((JarURLConnection)urlConnection).getJarFileURL();
            }
            String host = locUrl.getHost();
            if (host != null && !host.isEmpty())
                p = new SocketPermission(host,
                                         SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION);
        }

        // make sure the person that created this class loader
        // would have this permission

        if (p != null) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                final Permission fp = p;
                AccessController.doPrivileged(new PrivilegedAction<>() {
                    public Void run() throws SecurityException {
                        sm.checkPermission(fp);
                        return null;
                    }
                }, acc);
            }
            perms.add(p);
        }
        return perms;
    }

    /**
     * Creates a new instance of URLClassLoader for the specified
     * URLs and parent class loader. If a security manager is
     * installed, the {@code loadClass} method of the URLClassLoader
     * returned by this method will invoke the
     * {@code SecurityManager.checkPackageAccess} method before
     * loading the class.
     *
     * @param urls the URLs to search for classes and resources
     * @param parent the parent class loader for delegation
     * @throws     NullPointerException if {@code urls} or any of its
     *             elements is {@code null}.
     * @return the resulting class loader
     */
    public static URLClassLoader newInstance(final URL[] urls,
                                             final ClassLoader parent) {
        // Save the caller's context
        @SuppressWarnings("removal")
        final AccessControlContext acc = AccessController.getContext();
        // Need a privileged block to create the class loader
        @SuppressWarnings("removal")
        URLClassLoader ucl = AccessController.doPrivileged(
            new PrivilegedAction<>() {
                public URLClassLoader run() {
                    return new FactoryURLClassLoader(null, urls, parent, acc);
                }
            });
        return ucl;
    }

    /**
     * Creates a new instance of URLClassLoader for the specified
     * URLs and default parent class loader. If a security manager is
     * installed, the {@code loadClass} method of the URLClassLoader
     * returned by this method will invoke the
     * {@code SecurityManager.checkPackageAccess} before
     * loading the class.
     *
     * @param urls the URLs to search for classes and resources
     * @throws     NullPointerException if {@code urls} or any of its
     *             elements is {@code null}.
     * @return the resulting class loader
     */
    public static URLClassLoader newInstance(final URL[] urls) {
        // Save the caller's context
        @SuppressWarnings("removal")
        final AccessControlContext acc = AccessController.getContext();
        // Need a privileged block to create the class loader
        @SuppressWarnings("removal")
        URLClassLoader ucl = AccessController.doPrivileged(
            new PrivilegedAction<>() {
                public URLClassLoader run() {
                    return new FactoryURLClassLoader(urls, acc);
                }
            });
        return ucl;
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}

final class FactoryURLClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    FactoryURLClassLoader(String name, URL[] urls, ClassLoader parent,
                          @SuppressWarnings("removal") AccessControlContext acc) {
        super(name, urls, parent, acc);
    }

    FactoryURLClassLoader(URL[] urls, @SuppressWarnings("removal") AccessControlContext acc) {
        super(urls, acc);
    }

    public final Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        // First check if we have permission to access the package. This
        // should go away once we've added support for exported packages.
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            int i = name.lastIndexOf('.');
            if (i != -1) {
                sm.checkPackageAccess(name.substring(0, i));
            }
        }
        return super.loadClass(name, resolve);
    }
}
