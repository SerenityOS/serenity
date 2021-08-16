/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.loader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReference;
import java.lang.module.ModuleReader;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.VM;
import jdk.internal.module.ModulePatcher.PatchedModuleReader;
import jdk.internal.module.Resources;
import jdk.internal.vm.annotation.Stable;
import sun.security.util.LazyCodeSourcePermissionCollection;


/**
 * The platform or application class loader. Resources loaded from modules
 * defined to the boot class loader are also loaded via an instance of this
 * ClassLoader type.
 *
 * <p> This ClassLoader supports loading of classes and resources from modules.
 * Modules are defined to the ClassLoader by invoking the {@link #loadModule}
 * method. Defining a module to this ClassLoader has the effect of making the
 * types in the module visible. </p>
 *
 * <p> This ClassLoader also supports loading of classes and resources from a
 * class path of URLs that are specified to the ClassLoader at construction
 * time. The class path may expand at runtime (the Class-Path attribute in JAR
 * files or via instrumentation agents). </p>
 *
 * <p> The delegation model used by this ClassLoader differs to the regular
 * delegation model. When requested to load a class then this ClassLoader first
 * maps the class name to its package name. If there is a module defined to a
 * BuiltinClassLoader containing this package then the class loader delegates
 * directly to that class loader. If there isn't a module containing the
 * package then it delegates the search to the parent class loader and if not
 * found in the parent then it searches the class path. The main difference
 * between this and the usual delegation model is that it allows the platform
 * class loader to delegate to the application class loader, important with
 * upgraded modules defined to the platform class loader.
 */

public class BuiltinClassLoader
    extends SecureClassLoader
{
    static {
        if (!ClassLoader.registerAsParallelCapable())
            throw new InternalError("Unable to register as parallel capable");
    }

    // parent ClassLoader
    private final BuiltinClassLoader parent;

    // the URL class path, or null if there is no class path
    private @Stable URLClassPath ucp;

    /**
     * A module defined/loaded by a built-in class loader.
     *
     * A LoadedModule encapsulates a ModuleReference along with its CodeSource
     * URL to avoid needing to create this URL when defining classes.
     */
    private static class LoadedModule {
        private final BuiltinClassLoader loader;
        private final ModuleReference mref;
        private final URI uri;                      // may be null
        private @Stable URL codeSourceURL;          // may be null

        LoadedModule(BuiltinClassLoader loader, ModuleReference mref) {
            URL url = null;
            this.uri = mref.location().orElse(null);

            // for non-jrt schemes we need to resolve the codeSourceURL
            // eagerly during bootstrap since the handler might be
            // overridden
            if (uri != null && !"jrt".equals(uri.getScheme())) {
                url = createURL(uri);
            }
            this.loader = loader;
            this.mref = mref;
            this.codeSourceURL = url;
        }

        BuiltinClassLoader loader() { return loader; }
        ModuleReference mref() { return mref; }
        String name() { return mref.descriptor().name(); }

        URL codeSourceURL() {
            URL url = codeSourceURL;
            if (url == null && uri != null) {
                codeSourceURL = url = createURL(uri);
            }
            return url;
        }

        private URL createURL(URI uri) {
            URL url = null;
            try {
                url = uri.toURL();
            } catch (MalformedURLException | IllegalArgumentException e) {
            }
            return url;
        }
    }

    // maps package name to loaded module for modules in the boot layer
    private static final Map<String, LoadedModule> packageToModule;
    static {
        ArchivedClassLoaders archivedClassLoaders = ArchivedClassLoaders.get();
        if (archivedClassLoaders != null) {
            @SuppressWarnings("unchecked")
            Map<String, LoadedModule> map
                = (Map<String, LoadedModule>) archivedClassLoaders.packageToModule();
            packageToModule = map;
        } else {
            packageToModule = new ConcurrentHashMap<>(1024);
        }
    }

    /**
     * Invoked by ArchivedClassLoaders to archive the package-to-module map.
     */
    static Map<String, ?> packageToModule() {
        return packageToModule;
    }

    // maps a module name to a module reference
    private final Map<String, ModuleReference> nameToModule;

    // maps a module reference to a module reader
    private final Map<ModuleReference, ModuleReader> moduleToReader;

    // cache of resource name -> list of URLs.
    // used only for resources that are not in module packages
    private volatile SoftReference<Map<String, List<URL>>> resourceCache;

    /**
     * Create a new instance.
     */
    BuiltinClassLoader(String name, BuiltinClassLoader parent, URLClassPath ucp) {
        // ensure getParent() returns null when the parent is the boot loader
        super(name, parent == null || parent == ClassLoaders.bootLoader() ? null : parent);

        this.parent = parent;
        this.ucp = ucp;

        this.nameToModule = new ConcurrentHashMap<>(32);
        this.moduleToReader = new ConcurrentHashMap<>();
    }

    /**
     * Appends to the given file path to the class path.
     */
    void appendClassPath(String path) {
        // assert ucp != null;
        ucp.addFile(path);
    }

    /**
     * Sets the class path, called to reset the class path during -Xshare:dump
     */
    void setClassPath(URLClassPath ucp) {
        this.ucp = ucp;
    }

    /**
     * Returns {@code true} if there is a class path associated with this
     * class loader.
     */
    boolean hasClassPath() {
        return ucp != null;
    }

    /**
     * Register a module this class loader. This has the effect of making the
     * types in the module visible.
     */
    public void loadModule(ModuleReference mref) {
        ModuleDescriptor descriptor = mref.descriptor();
        String mn = descriptor.name();
        if (nameToModule.putIfAbsent(mn, mref) != null) {
            throw new InternalError(mn + " already defined to this loader");
        }

        LoadedModule loadedModule = new LoadedModule(this, mref);
        for (String pn : descriptor.packages()) {
            LoadedModule other = packageToModule.putIfAbsent(pn, loadedModule);
            if (other != null) {
                throw new InternalError(pn + " in modules " + mn + " and "
                                        + other.name());
            }
        }

        // clear resources cache if VM is already initialized
        if (resourceCache != null && VM.isModuleSystemInited()) {
            resourceCache = null;
        }
    }

    /**
     * Returns the {@code ModuleReference} for the named module defined to
     * this class loader; or {@code null} if not defined.
     *
     * @param name The name of the module to find
     */
    protected ModuleReference findModule(String name) {
        return nameToModule.get(name);
    }


    // -- finding resources

    /**
     * Returns a URL to a resource of the given name in a module defined to
     * this class loader.
     */
    @Override
    public URL findResource(String mn, String name) throws IOException {
        URL url = null;

        if (mn != null) {
            // find in module
            ModuleReference mref = nameToModule.get(mn);
            if (mref != null) {
                url = findResource(mref, name);
            }
        } else {
            // find on class path
            url = findResourceOnClassPath(name);
        }

        return checkURL(url);  // check access before returning
    }

    /**
     * Returns an input stream to a resource of the given name in a module
     * defined to this class loader.
     */
    @SuppressWarnings("removal")
    public InputStream findResourceAsStream(String mn, String name)
        throws IOException
    {
        // Need URL to resource when running with a security manager so that
        // the right permission check is done.
        if (System.getSecurityManager() != null || mn == null) {
            URL url = findResource(mn, name);
            return (url != null) ? url.openStream() : null;
        }

        // find in module defined to this loader, no security manager
        ModuleReference mref = nameToModule.get(mn);
        if (mref != null) {
            return moduleReaderFor(mref).open(name).orElse(null);
        } else {
            return null;
        }
    }

    /**
     * Finds a resource with the given name in the modules defined to this
     * class loader or its class path.
     */
    @Override
    public URL findResource(String name) {
        String pn = Resources.toPackageName(name);
        LoadedModule module = packageToModule.get(pn);
        if (module != null) {

            // resource is in a package of a module defined to this loader
            if (module.loader() == this) {
                URL url;
                try {
                    url = findResource(module.name(), name); // checks URL
                } catch (IOException ioe) {
                    return null;
                }
                if (url != null
                    && (name.endsWith(".class")
                        || url.toString().endsWith("/")
                        || isOpen(module.mref(), pn))) {
                    return url;
                }
            }

        } else {

            // not in a module package but may be in module defined to this loader
            try {
                List<URL> urls = findMiscResource(name);
                if (!urls.isEmpty()) {
                    URL url = urls.get(0);
                    if (url != null) {
                        return checkURL(url); // check access before returning
                    }
                }
            } catch (IOException ioe) {
                return null;
            }

        }

        // search class path
        URL url = findResourceOnClassPath(name);
        return checkURL(url);
    }

    /**
     * Returns an enumeration of URL objects to all the resources with the
     * given name in modules defined to this class loader or on the class
     * path of this loader.
     */
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        List<URL> checked = new ArrayList<>();  // list of checked URLs

        String pn = Resources.toPackageName(name);
        LoadedModule module = packageToModule.get(pn);
        if (module != null) {

            // resource is in a package of a module defined to this loader
            if (module.loader() == this) {
                URL url = findResource(module.name(), name); // checks URL
                if (url != null
                    && (name.endsWith(".class")
                        || url.toString().endsWith("/")
                        || isOpen(module.mref(), pn))) {
                    checked.add(url);
                }
            }

        } else {
            // not in a package of a module defined to this loader
            for (URL url : findMiscResource(name)) {
                url = checkURL(url);
                if (url != null) {
                    checked.add(url);
                }
            }
        }

        // class path (not checked)
        Enumeration<URL> e = findResourcesOnClassPath(name);

        // concat the checked URLs and the (not checked) class path
        return new Enumeration<>() {
            final Iterator<URL> iterator = checked.iterator();
            URL next;
            private boolean hasNext() {
                if (next != null) {
                    return true;
                } else if (iterator.hasNext()) {
                    next = iterator.next();
                    return true;
                } else {
                    // need to check each URL
                    while (e.hasMoreElements() && next == null) {
                        next = checkURL(e.nextElement());
                    }
                    return next != null;
                }
            }
            @Override
            public boolean hasMoreElements() {
                return hasNext();
            }
            @Override
            public URL nextElement() {
                if (hasNext()) {
                    URL result = next;
                    next = null;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };

    }

    /**
     * Returns the list of URLs to a "miscellaneous" resource in modules
     * defined to this loader. A miscellaneous resource is not in a module
     * package, e.g. META-INF/services/p.S.
     *
     * The cache used by this method avoids repeated searching of all modules.
     */
    @SuppressWarnings("removal")
    private List<URL> findMiscResource(String name) throws IOException {
        SoftReference<Map<String, List<URL>>> ref = this.resourceCache;
        Map<String, List<URL>> map = (ref != null) ? ref.get() : null;
        if (map == null) {
            // only cache resources after VM is fully initialized
            if (VM.isModuleSystemInited()) {
                map = new ConcurrentHashMap<>();
                this.resourceCache = new SoftReference<>(map);
            }
        } else {
            List<URL> urls = map.get(name);
            if (urls != null)
                return urls;
        }

        // search all modules for the resource
        List<URL> urls;
        try {
            urls = AccessController.doPrivileged(
                new PrivilegedExceptionAction<>() {
                    @Override
                    public List<URL> run() throws IOException {
                        List<URL> result = null;
                        for (ModuleReference mref : nameToModule.values()) {
                            URI u = moduleReaderFor(mref).find(name).orElse(null);
                            if (u != null) {
                                try {
                                    if (result == null)
                                        result = new ArrayList<>();
                                    result.add(u.toURL());
                                } catch (MalformedURLException |
                                         IllegalArgumentException e) {
                                }
                            }
                        }
                        return (result != null) ? result : Collections.emptyList();
                    }
                });
        } catch (PrivilegedActionException pae) {
            throw (IOException) pae.getCause();
        }

        // only cache resources after VM is fully initialized
        if (map != null) {
            map.putIfAbsent(name, urls);
        }

        return urls;
    }

    /**
     * Returns the URL to a resource in a module or {@code null} if not found.
     */
    @SuppressWarnings("removal")
    private URL findResource(ModuleReference mref, String name) throws IOException {
        URI u;
        if (System.getSecurityManager() == null) {
            u = moduleReaderFor(mref).find(name).orElse(null);
        } else {
            try {
                u = AccessController.doPrivileged(new PrivilegedExceptionAction<> () {
                    @Override
                    public URI run() throws IOException {
                        return moduleReaderFor(mref).find(name).orElse(null);
                    }
                });
            } catch (PrivilegedActionException pae) {
                throw (IOException) pae.getCause();
            }
        }
        if (u != null) {
            try {
                return u.toURL();
            } catch (MalformedURLException | IllegalArgumentException e) { }
        }
        return null;
    }

    /**
     * Returns the URL to a resource in a module. Returns {@code null} if not found
     * or an I/O error occurs.
     */
    private URL findResourceOrNull(ModuleReference mref, String name) {
        try {
            return findResource(mref, name);
        } catch (IOException ignore) {
            return null;
        }
    }

    /**
     * Returns a URL to a resource on the class path.
     */
    @SuppressWarnings("removal")
    private URL findResourceOnClassPath(String name) {
        if (hasClassPath()) {
            if (System.getSecurityManager() == null) {
                return ucp.findResource(name, false);
            } else {
                PrivilegedAction<URL> pa = () -> ucp.findResource(name, false);
                return AccessController.doPrivileged(pa);
            }
        } else {
            // no class path
            return null;
        }
    }

    /**
     * Returns the URLs of all resources of the given name on the class path.
     */
    @SuppressWarnings("removal")
    private Enumeration<URL> findResourcesOnClassPath(String name) {
        if (hasClassPath()) {
            if (System.getSecurityManager() == null) {
                return ucp.findResources(name, false);
            } else {
                PrivilegedAction<Enumeration<URL>> pa;
                pa = () -> ucp.findResources(name, false);
                return AccessController.doPrivileged(pa);
            }
        } else {
            // no class path
            return Collections.emptyEnumeration();
        }
    }

    // -- finding/loading classes

    /**
     * Finds the class with the specified binary name.
     */
    @Override
    protected Class<?> findClass(String cn) throws ClassNotFoundException {
        // no class loading until VM is fully initialized
        if (!VM.isModuleSystemInited())
            throw new ClassNotFoundException(cn);

        // find the candidate module for this class
        LoadedModule loadedModule = findLoadedModule(cn);

        Class<?> c = null;
        if (loadedModule != null) {

            // attempt to load class in module defined to this loader
            if (loadedModule.loader() == this) {
                c = findClassInModuleOrNull(loadedModule, cn);
            }

        } else {

            // search class path
            if (hasClassPath()) {
                c = findClassOnClassPathOrNull(cn);
            }

        }

        // not found
        if (c == null)
            throw new ClassNotFoundException(cn);

        return c;
    }

    /**
     * Finds the class with the specified binary name in a module.
     * This method returns {@code null} if the class cannot be found
     * or not defined in the specified module.
     */
    @Override
    protected Class<?> findClass(String mn, String cn) {
        if (mn != null) {
            // find the candidate module for this class
            LoadedModule loadedModule = findLoadedModule(mn, cn);
            if (loadedModule == null) {
                return null;
            }

            // attempt to load class in module defined to this loader
            assert loadedModule.loader() == this;
            return findClassInModuleOrNull(loadedModule, cn);
        }

        // search class path
        if (hasClassPath()) {
            return findClassOnClassPathOrNull(cn);
        }

        return null;
    }

    /**
     * Loads the class with the specified binary name.
     */
    @Override
    protected Class<?> loadClass(String cn, boolean resolve)
        throws ClassNotFoundException
    {
        Class<?> c = loadClassOrNull(cn, resolve);
        if (c == null)
            throw new ClassNotFoundException(cn);
        return c;
    }

    /**
     * A variation of {@code loadClass} to load a class with the specified
     * binary name. This method returns {@code null} when the class is not
     * found.
     */
    protected Class<?> loadClassOrNull(String cn, boolean resolve) {
        synchronized (getClassLoadingLock(cn)) {
            // check if already loaded
            Class<?> c = findLoadedClass(cn);

            if (c == null) {

                // find the candidate module for this class
                LoadedModule loadedModule = findLoadedModule(cn);
                if (loadedModule != null) {

                    // package is in a module
                    BuiltinClassLoader loader = loadedModule.loader();
                    if (loader == this) {
                        if (VM.isModuleSystemInited()) {
                            c = findClassInModuleOrNull(loadedModule, cn);
                        }
                    } else {
                        // delegate to the other loader
                        c = loader.loadClassOrNull(cn);
                    }

                } else {

                    // check parent
                    if (parent != null) {
                        c = parent.loadClassOrNull(cn);
                    }

                    // check class path
                    if (c == null && hasClassPath() && VM.isModuleSystemInited()) {
                        c = findClassOnClassPathOrNull(cn);
                    }
                }

            }

            if (resolve && c != null)
                resolveClass(c);

            return c;
        }
    }

    /**
     * A variation of {@code loadClass} to load a class with the specified
     * binary name. This method returns {@code null} when the class is not
     * found.
     */
    protected final Class<?> loadClassOrNull(String cn) {
        return loadClassOrNull(cn, false);
    }

    /**
     * Finds the candidate loaded module for the given class name.
     * Returns {@code null} if none of the modules defined to this
     * class loader contain the API package for the class.
     */
    private LoadedModule findLoadedModule(String cn) {
        int pos = cn.lastIndexOf('.');
        if (pos < 0)
            return null; // unnamed package

        String pn = cn.substring(0, pos);
        return packageToModule.get(pn);
    }

    /**
     * Finds the candidate loaded module for the given class name
     * in the named module.  Returns {@code null} if the named module
     * is not defined to this class loader or does not contain
     * the API package for the class.
     */
    private LoadedModule findLoadedModule(String mn, String cn) {
        LoadedModule loadedModule = findLoadedModule(cn);
        if (loadedModule != null && mn.equals(loadedModule.name())) {
            return loadedModule;
        } else {
            return null;
        }
    }

    /**
     * Finds the class with the specified binary name if in a module
     * defined to this ClassLoader.
     *
     * @return the resulting Class or {@code null} if not found
     */
    @SuppressWarnings("removal")
    private Class<?> findClassInModuleOrNull(LoadedModule loadedModule, String cn) {
        if (System.getSecurityManager() == null) {
            return defineClass(cn, loadedModule);
        } else {
            PrivilegedAction<Class<?>> pa = () -> defineClass(cn, loadedModule);
            return AccessController.doPrivileged(pa);
        }
    }

    /**
     * Finds the class with the specified binary name on the class path.
     *
     * @return the resulting Class or {@code null} if not found
     */
    @SuppressWarnings("removal")
    private Class<?> findClassOnClassPathOrNull(String cn) {
        String path = cn.replace('.', '/').concat(".class");
        if (System.getSecurityManager() == null) {
            Resource res = ucp.getResource(path, false);
            if (res != null) {
                try {
                    return defineClass(cn, res);
                } catch (IOException ioe) {
                    // TBD on how I/O errors should be propagated
                }
            }
            return null;
        } else {
            // avoid use of lambda here
            PrivilegedAction<Class<?>> pa = new PrivilegedAction<>() {
                public Class<?> run() {
                    Resource res = ucp.getResource(path, false);
                    if (res != null) {
                        try {
                            return defineClass(cn, res);
                        } catch (IOException ioe) {
                            // TBD on how I/O errors should be propagated
                        }
                    }
                    return null;
                }
            };
            return AccessController.doPrivileged(pa);
        }
    }

    /**
     * Defines the given binary class name to the VM, loading the class
     * bytes from the given module.
     *
     * @return the resulting Class or {@code null} if an I/O error occurs
     */
    private Class<?> defineClass(String cn, LoadedModule loadedModule) {
        ModuleReference mref = loadedModule.mref();
        ModuleReader reader = moduleReaderFor(mref);

        try {
            ByteBuffer bb = null;
            URL csURL = null;

            // locate class file, special handling for patched modules to
            // avoid locating the resource twice
            String rn = cn.replace('.', '/').concat(".class");
            if (reader instanceof PatchedModuleReader) {
                Resource r = ((PatchedModuleReader)reader).findResource(rn);
                if (r != null) {
                    bb = r.getByteBuffer();
                    csURL = r.getCodeSourceURL();
                }
            } else {
                bb = reader.read(rn).orElse(null);
                csURL = loadedModule.codeSourceURL();
            }

            if (bb == null) {
                // class not found
                return null;
            }

            CodeSource cs = new CodeSource(csURL, (CodeSigner[]) null);
            try {
                // define class to VM
                return defineClass(cn, bb, cs);

            } finally {
                reader.release(bb);
            }

        } catch (IOException ioe) {
            // TBD on how I/O errors should be propagated
            return null;
        }
    }

    /**
     * Defines the given binary class name to the VM, loading the class
     * bytes via the given Resource object.
     *
     * @return the resulting Class
     * @throws IOException if reading the resource fails
     * @throws SecurityException if there is a sealing violation (JAR spec)
     */
    private Class<?> defineClass(String cn, Resource res) throws IOException {
        URL url = res.getCodeSourceURL();

        // if class is in a named package then ensure that the package is defined
        int pos = cn.lastIndexOf('.');
        if (pos != -1) {
            String pn = cn.substring(0, pos);
            Manifest man = res.getManifest();
            defineOrCheckPackage(pn, man, url);
        }

        // defines the class to the runtime
        ByteBuffer bb = res.getByteBuffer();
        if (bb != null) {
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            return defineClass(cn, bb, cs);
        } else {
            byte[] b = res.getBytes();
            CodeSigner[] signers = res.getCodeSigners();
            CodeSource cs = new CodeSource(url, signers);
            return defineClass(cn, b, 0, b.length, cs);
        }
    }


    // -- packages

    /**
     * Defines a package in this ClassLoader. If the package is already defined
     * then its sealing needs to be checked if sealed by the legacy sealing
     * mechanism.
     *
     * @throws SecurityException if there is a sealing violation (JAR spec)
     */
    protected Package defineOrCheckPackage(String pn, Manifest man, URL url) {
        Package pkg = getAndVerifyPackage(pn, man, url);
        if (pkg == null) {
            try {
                if (man != null) {
                    pkg = definePackage(pn, man, url);
                } else {
                    pkg = definePackage(pn, null, null, null, null, null, null, null);
                }
            } catch (IllegalArgumentException iae) {
                // defined by another thread so need to re-verify
                pkg = getAndVerifyPackage(pn, man, url);
                if (pkg == null)
                    throw new InternalError("Cannot find package: " + pn);
            }
        }
        return pkg;
    }

    /**
     * Gets the Package with the specified package name. If defined
     * then verifies it against the manifest and code source.
     *
     * @throws SecurityException if there is a sealing violation (JAR spec)
     */
    private Package getAndVerifyPackage(String pn, Manifest man, URL url) {
        Package pkg = getDefinedPackage(pn);
        if (pkg != null) {
            if (pkg.isSealed()) {
                if (!pkg.isSealed(url)) {
                    throw new SecurityException(
                        "sealing violation: package " + pn + " is sealed");
                }
            } else {
                // can't seal package if already defined without sealing
                if ((man != null) && isSealed(pn, man)) {
                    throw new SecurityException(
                        "sealing violation: can't seal package " + pn +
                        ": already defined");
                }
            }
        }
        return pkg;
    }

    /**
     * Defines a new package in this ClassLoader. The attributes in the specified
     * Manifest are used to get the package version and sealing information.
     *
     * @throws IllegalArgumentException if the package name duplicates an
     *      existing package either in this class loader or one of its ancestors
     * @throws SecurityException if the package name is untrusted in the manifest
     */
    private Package definePackage(String pn, Manifest man, URL url) {
        String specTitle = null;
        String specVersion = null;
        String specVendor = null;
        String implTitle = null;
        String implVersion = null;
        String implVendor = null;
        String sealed = null;
        URL sealBase = null;

        if (man != null) {
            Attributes attr = SharedSecrets.javaUtilJarAccess()
                    .getTrustedAttributes(man, pn.replace('.', '/').concat("/"));
            if (attr != null) {
                specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
                specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
                specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                sealed = attr.getValue(Attributes.Name.SEALED);
            }

            attr = man.getMainAttributes();
            if (attr != null) {
                if (specTitle == null)
                    specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
                if (specVersion == null)
                    specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
                if (specVendor == null)
                    specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                if (implTitle == null)
                    implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                if (implVersion == null)
                    implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                if (implVendor == null)
                    implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                if (sealed == null)
                    sealed = attr.getValue(Attributes.Name.SEALED);
            }

            // package is sealed
            if ("true".equalsIgnoreCase(sealed))
                sealBase = url;
        }
        return definePackage(pn,
                specTitle,
                specVersion,
                specVendor,
                implTitle,
                implVersion,
                implVendor,
                sealBase);
    }

    /**
     * Returns {@code true} if the specified package name is sealed according to
     * the given manifest.
     *
     * @throws SecurityException if the package name is untrusted in the manifest
     */
    private boolean isSealed(String pn, Manifest man) {
        Attributes attr = SharedSecrets.javaUtilJarAccess()
                .getTrustedAttributes(man, pn.replace('.', '/').concat("/"));
        String sealed = null;
        if (attr != null)
            sealed = attr.getValue(Attributes.Name.SEALED);
        if (sealed == null && (attr = man.getMainAttributes()) != null)
            sealed = attr.getValue(Attributes.Name.SEALED);
        return "true".equalsIgnoreCase(sealed);
    }

    // -- permissions

    /**
     * Returns the permissions for the given CodeSource.
     */
    @Override
    protected PermissionCollection getPermissions(CodeSource cs) {
        return new LazyCodeSourcePermissionCollection(super.getPermissions(cs), cs);
    }

    // -- miscellaneous supporting methods

    /**
     * Returns the ModuleReader for the given module, creating it if needed.
     */
    private ModuleReader moduleReaderFor(ModuleReference mref) {
        ModuleReader reader = moduleToReader.get(mref);
        if (reader == null) {
            // avoid method reference during startup
            Function<ModuleReference, ModuleReader> create = new Function<>() {
                public ModuleReader apply(ModuleReference moduleReference) {
                    try {
                        return mref.open();
                    } catch (IOException e) {
                        // Return a null module reader to avoid a future class
                        // load attempting to open the module again.
                        return new NullModuleReader();
                    }
                }
            };
            reader = moduleToReader.computeIfAbsent(mref, create);
        }
        return reader;
    }

    /**
     * A ModuleReader that doesn't read any resources.
     */
    private static class NullModuleReader implements ModuleReader {
        @Override
        public Optional<URI> find(String name) {
            return Optional.empty();
        }
        @Override
        public Stream<String> list() {
            return Stream.empty();
        }
        @Override
        public void close() {
            throw new InternalError("Should not get here");
        }
    };

    /**
     * Returns true if the given module opens the given package
     * unconditionally.
     *
     * @implNote This method currently iterates over each of the open
     * packages. This will be replaced once the ModuleDescriptor.Opens
     * API is updated.
     */
    private boolean isOpen(ModuleReference mref, String pn) {
        ModuleDescriptor descriptor = mref.descriptor();
        if (descriptor.isOpen() || descriptor.isAutomatic())
            return true;
        for (ModuleDescriptor.Opens opens : descriptor.opens()) {
            String source = opens.source();
            if (!opens.isQualified() && source.equals(pn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks access to the given URL. We use URLClassPath for consistent
     * checking with java.net.URLClassLoader.
     */
    private static URL checkURL(URL url) {
        return URLClassPath.checkURL(url);
    }

    // Called from VM only, during -Xshare:dump
    private void resetArchivedStates() {
        ucp = null;
    }
}
