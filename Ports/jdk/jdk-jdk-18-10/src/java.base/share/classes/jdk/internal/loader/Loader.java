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

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import jdk.internal.access.SharedSecrets;
import jdk.internal.module.Resources;

/**
 * A class loader that loads classes and resources from a collection of
 * modules, or from a single module where the class loader is a member
 * of a pool of class loaders.
 *
 * <p> The delegation model used by this ClassLoader differs to the regular
 * delegation model. When requested to load a class then this ClassLoader first
 * maps the class name to its package name. If there a module defined to the
 * Loader containing the package then the class loader attempts to load from
 * that module. If the package is instead defined to a module in a "remote"
 * ClassLoader then this class loader delegates directly to that class loader.
 * The map of package name to remote class loader is created based on the
 * modules read by modules defined to this class loader. If the package is not
 * local or remote then this class loader will delegate to the parent class
 * loader. This allows automatic modules (for example) to link to types in the
 * unnamed module of the parent class loader.
 *
 * @see ModuleLayer#defineModulesWithOneLoader
 * @see ModuleLayer#defineModulesWithManyLoaders
 */

public final class Loader extends SecureClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    // the pool this loader is a member of; can be null
    private final LoaderPool pool;

    // parent ClassLoader, can be null
    private final ClassLoader parent;

    // maps a module name to a module reference
    private final Map<String, ModuleReference> nameToModule;

    // maps package name to a module loaded by this class loader
    private final Map<String, LoadedModule> localPackageToModule;

    // maps package name to a remote class loader, populated post initialization
    private final Map<String, ClassLoader> remotePackageToLoader
        = new ConcurrentHashMap<>();

    // maps a module reference to a module reader, populated lazily
    private final Map<ModuleReference, ModuleReader> moduleToReader
        = new ConcurrentHashMap<>();

    // ACC used when loading classes and resources
    @SuppressWarnings("removal")
    private final AccessControlContext acc;

    /**
     * A module defined/loaded to a {@code Loader}.
     */
    private static class LoadedModule {
        private final ModuleReference mref;
        private final URL url;          // may be null
        private final CodeSource cs;

        LoadedModule(ModuleReference mref) {
            URL url = null;
            if (mref.location().isPresent()) {
                try {
                    url = mref.location().get().toURL();
                } catch (MalformedURLException | IllegalArgumentException e) { }
            }
            this.mref = mref;
            this.url = url;
            this.cs = new CodeSource(url, (CodeSigner[]) null);
        }

        ModuleReference mref() { return mref; }
        String name() { return mref.descriptor().name(); }
        URL location() { return url; }
        CodeSource codeSource() { return cs; }
    }


    /**
     * Creates a {@code Loader} in a loader pool that loads classes/resources
     * from one module.
     */
    @SuppressWarnings("removal")
    public Loader(ResolvedModule resolvedModule,
                  LoaderPool pool,
                  ClassLoader parent)
    {
        super("Loader-" + resolvedModule.name(), parent);

        this.pool = pool;
        this.parent = parent;

        ModuleReference mref = resolvedModule.reference();
        ModuleDescriptor descriptor = mref.descriptor();
        String mn = descriptor.name();
        this.nameToModule = Map.of(mn, mref);

        Map<String, LoadedModule> localPackageToModule = new HashMap<>();
        LoadedModule lm = new LoadedModule(mref);
        descriptor.packages().forEach(pn -> localPackageToModule.put(pn, lm));
        this.localPackageToModule = localPackageToModule;

        this.acc = AccessController.getContext();
    }

    /**
     * Creates a {@code Loader} that loads classes/resources from a collection
     * of modules.
     *
     * @throws IllegalArgumentException
     *         If two or more modules have the same package
     */
    @SuppressWarnings("removal")
    public Loader(Collection<ResolvedModule> modules, ClassLoader parent) {
        super(parent);

        this.pool = null;
        this.parent = parent;

        Map<String, ModuleReference> nameToModule = new HashMap<>();
        Map<String, LoadedModule> localPackageToModule = new HashMap<>();
        for (ResolvedModule resolvedModule : modules) {
            ModuleReference mref = resolvedModule.reference();
            ModuleDescriptor descriptor = mref.descriptor();
            nameToModule.put(descriptor.name(), mref);
            descriptor.packages().forEach(pn -> {
                LoadedModule lm = new LoadedModule(mref);
                if (localPackageToModule.put(pn, lm) != null)
                    throw new IllegalArgumentException("Package "
                        + pn + " in more than one module");
            });
        }
        this.nameToModule = nameToModule;
        this.localPackageToModule = localPackageToModule;

        this.acc = AccessController.getContext();
    }

    /**
     * Completes initialization of this Loader. This method populates
     * remotePackageToLoader with the packages of the remote modules, where
     * "remote modules" are the modules read by modules defined to this loader.
     *
     * @param cf the Configuration containing at least modules to be defined to
     *           this class loader
     *
     * @param parentModuleLayers the parent ModuleLayers
     */
    public Loader initRemotePackageMap(Configuration cf,
                                       List<ModuleLayer> parentModuleLayers)
    {
        for (String name : nameToModule.keySet()) {
            ResolvedModule resolvedModule = cf.findModule(name).get();
            assert resolvedModule.configuration() == cf;

            for (ResolvedModule other : resolvedModule.reads()) {
                String mn = other.name();
                ClassLoader loader;

                if (other.configuration() == cf) {

                    // The module reads another module in the newly created
                    // layer. If all modules are defined to the same class
                    // loader then the packages are local.
                    if (pool == null) {
                        assert nameToModule.containsKey(mn);
                        continue;
                    }

                    loader = pool.loaderFor(mn);
                    assert loader != null;

                } else {

                    // find the layer for the target module
                    ModuleLayer layer = parentModuleLayers.stream()
                        .map(parent -> findModuleLayer(parent, other.configuration()))
                        .flatMap(Optional::stream)
                        .findAny()
                        .orElseThrow(() ->
                            new InternalError("Unable to find parent layer"));

                    // find the class loader for the module
                    // For now we use the platform loader for modules defined to the
                    // boot loader
                    assert layer.findModule(mn).isPresent();
                    loader = layer.findLoader(mn);
                    if (loader == null)
                        loader = ClassLoaders.platformClassLoader();
                }

                // find the packages that are exported to the target module
                ModuleDescriptor descriptor = other.reference().descriptor();
                if (descriptor.isAutomatic()) {
                    ClassLoader l = loader;
                    descriptor.packages().forEach(pn -> remotePackage(pn, l));
                } else {
                    String target = resolvedModule.name();
                    for (ModuleDescriptor.Exports e : descriptor.exports()) {
                        boolean delegate;
                        if (e.isQualified()) {
                            // qualified export in same configuration
                            delegate = (other.configuration() == cf)
                                    && e.targets().contains(target);
                        } else {
                            // unqualified
                            delegate = true;
                        }

                        if (delegate) {
                            remotePackage(e.source(), loader);
                        }
                    }
                }
            }

        }

        return this;
    }

    /**
     * Adds to remotePackageToLoader so that an attempt to load a class in
     * the package delegates to the given class loader.
     *
     * @throws IllegalStateException
     *         if the package is already mapped to a different class loader
     */
    private void remotePackage(String pn, ClassLoader loader) {
        ClassLoader l = remotePackageToLoader.putIfAbsent(pn, loader);
        if (l != null && l != loader) {
            throw new IllegalStateException("Package "
                + pn + " cannot be imported from multiple loaders");
        }
    }


    /**
     * Find the layer corresponding to the given configuration in the tree
     * of layers rooted at the given parent.
     */
    private Optional<ModuleLayer> findModuleLayer(ModuleLayer parent, Configuration cf) {
        return SharedSecrets.getJavaLangAccess().layers(parent)
                .filter(l -> l.configuration() == cf)
                .findAny();
    }


    /**
     * Returns the loader pool that this loader is in or {@code null} if this
     * loader is not in a loader pool.
     */
    public LoaderPool pool() {
        return pool;
    }


    // -- resources --

    /**
     * Returns a URL to a resource of the given name in a module defined to
     * this class loader.
     */
    @SuppressWarnings("removal")
    @Override
    protected URL findResource(String mn, String name) throws IOException {
        ModuleReference mref = (mn != null) ? nameToModule.get(mn) : null;
        if (mref == null)
            return null;   // not defined to this class loader

        // locate resource
        URL url = null;
        try {
            url = AccessController.doPrivileged(
                new PrivilegedExceptionAction<URL>() {
                    @Override
                    public URL run() throws IOException {
                        Optional<URI> ouri = moduleReaderFor(mref).find(name);
                        if (ouri.isPresent()) {
                            try {
                                return ouri.get().toURL();
                            } catch (MalformedURLException |
                                     IllegalArgumentException e) { }
                        }
                        return null;
                    }
                });
        } catch (PrivilegedActionException pae) {
            throw (IOException) pae.getCause();
        }

        // check access with permissions restricted by ACC
        if (url != null && System.getSecurityManager() != null) {
            try {
                URL urlToCheck = url;
                url = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<URL>() {
                        @Override
                        public URL run() throws IOException {
                            return URLClassPath.checkURL(urlToCheck);
                        }
                    }, acc);
            } catch (PrivilegedActionException pae) {
                url = null;
            }
        }

        return url;
    }

    @Override
    public URL findResource(String name) {
        String pn = Resources.toPackageName(name);
        LoadedModule module = localPackageToModule.get(pn);

        if (module != null) {
            try {
                URL url = findResource(module.name(), name);
                if (url != null
                    && (name.endsWith(".class")
                        || url.toString().endsWith("/")
                        || isOpen(module.mref(), pn))) {
                    return url;
                }
            } catch (IOException ioe) {
                // ignore
            }

        } else {
            for (ModuleReference mref : nameToModule.values()) {
                try {
                    URL url = findResource(mref.descriptor().name(), name);
                    if (url != null) return url;
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }

        return null;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        return Collections.enumeration(findResourcesAsList(name));
    }

    @Override
    public URL getResource(String name) {
        Objects.requireNonNull(name);

        // this loader
        URL url = findResource(name);
        if (url == null) {
            // parent loader
            if (parent != null) {
                url = parent.getResource(name);
            } else {
                url = BootLoader.findResource(name);
            }
        }
        return url;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Objects.requireNonNull(name);

        // this loader
        List<URL> urls = findResourcesAsList(name);

        // parent loader
        Enumeration<URL> e;
        if (parent != null) {
            e = parent.getResources(name);
        } else {
            e = BootLoader.findResources(name);
        }

        // concat the URLs with the URLs returned by the parent
        return new Enumeration<>() {
            final Iterator<URL> iterator = urls.iterator();
            @Override
            public boolean hasMoreElements() {
                return (iterator.hasNext() || e.hasMoreElements());
            }
            @Override
            public URL nextElement() {
                if (iterator.hasNext()) {
                    return iterator.next();
                } else {
                    return e.nextElement();
                }
            }
        };
    }

    /**
     * Finds the resources with the given name in this class loader.
     */
    private List<URL> findResourcesAsList(String name) throws IOException {
        String pn = Resources.toPackageName(name);
        LoadedModule module = localPackageToModule.get(pn);
        if (module != null) {
            URL url = findResource(module.name(), name);
            if (url != null
                    && (name.endsWith(".class")
                    || url.toString().endsWith("/")
                    || isOpen(module.mref(), pn))) {
                return List.of(url);
            } else {
                return Collections.emptyList();
            }
        } else {
            List<URL> urls = new ArrayList<>();
            for (ModuleReference mref : nameToModule.values()) {
                URL url = findResource(mref.descriptor().name(), name);
                if (url != null) {
                    urls.add(url);
                }
            }
            return urls;
        }
    }


    // -- finding/loading classes

    /**
     * Finds the class with the specified binary name.
     */
    @Override
    protected Class<?> findClass(String cn) throws ClassNotFoundException {
        Class<?> c = null;
        LoadedModule loadedModule = findLoadedModule(cn);
        if (loadedModule != null)
            c = findClassInModuleOrNull(loadedModule, cn);
        if (c == null)
            throw new ClassNotFoundException(cn);
        return c;
    }

    /**
     * Finds the class with the specified binary name in the given module.
     * This method returns {@code null} if the class cannot be found.
     */
    @Override
    protected Class<?> findClass(String mn, String cn) {
        Class<?> c = null;
        LoadedModule loadedModule = findLoadedModule(cn);
        if (loadedModule != null && loadedModule.name().equals(mn))
            c = findClassInModuleOrNull(loadedModule, cn);
        return c;
    }

    /**
     * Loads the class with the specified binary name.
     */
    @Override
    protected Class<?> loadClass(String cn, boolean resolve)
        throws ClassNotFoundException
    {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            String pn = packageName(cn);
            if (!pn.isEmpty()) {
                sm.checkPackageAccess(pn);
            }
        }

        synchronized (getClassLoadingLock(cn)) {
            // check if already loaded
            Class<?> c = findLoadedClass(cn);

            if (c == null) {

                LoadedModule loadedModule = findLoadedModule(cn);

                if (loadedModule != null) {

                    // class is in module defined to this class loader
                    c = findClassInModuleOrNull(loadedModule, cn);

                } else {

                    // type in another module or visible via the parent loader
                    String pn = packageName(cn);
                    ClassLoader loader = remotePackageToLoader.get(pn);
                    if (loader == null) {
                        // type not in a module read by any of the modules
                        // defined to this loader, so delegate to parent
                        // class loader
                        loader = parent;
                    }
                    if (loader == null) {
                        c = BootLoader.loadClassOrNull(cn);
                    } else {
                        c = loader.loadClass(cn);
                    }

                }
            }

            if (c == null)
                throw new ClassNotFoundException(cn);

            if (resolve)
                resolveClass(c);

            return c;
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
        PrivilegedAction<Class<?>> pa = () -> defineClass(cn, loadedModule);
        return AccessController.doPrivileged(pa, acc);
    }

    /**
     * Defines the given binary class name to the VM, loading the class
     * bytes from the given module.
     *
     * @return the resulting Class or {@code null} if an I/O error occurs
     */
    private Class<?> defineClass(String cn, LoadedModule loadedModule) {
        ModuleReader reader = moduleReaderFor(loadedModule.mref());

        try {
            // read class file
            String rn = cn.replace('.', '/').concat(".class");
            ByteBuffer bb = reader.read(rn).orElse(null);
            if (bb == null) {
                // class not found
                return null;
            }

            try {
                return defineClass(cn, bb, loadedModule.codeSource());
            } finally {
                reader.release(bb);
            }

        } catch (IOException ioe) {
            // TBD on how I/O errors should be propagated
            return null;
        }
    }


    // -- permissions

    /**
     * Returns the permissions for the given CodeSource.
     */
    @Override
    protected PermissionCollection getPermissions(CodeSource cs) {
        PermissionCollection perms = super.getPermissions(cs);

        URL url = cs.getLocation();
        if (url == null)
            return perms;

        // add the permission to access the resource
        try {
            Permission p = url.openConnection().getPermission();
            if (p != null) {
                // for directories then need recursive access
                if (p instanceof FilePermission) {
                    String path = p.getName();
                    if (path.endsWith(File.separator)) {
                        path += "-";
                        p = new FilePermission(path, "read");
                    }
                }
                perms.add(p);
            }
        } catch (IOException ioe) { }

        return perms;
    }


    // -- miscellaneous supporting methods

    /**
     * Find the candidate module for the given class name.
     * Returns {@code null} if none of the modules defined to this
     * class loader contain the API package for the class.
     */
    private LoadedModule findLoadedModule(String cn) {
        String pn = packageName(cn);
        return pn.isEmpty() ? null : localPackageToModule.get(pn);
    }

    /**
     * Returns the package name for the given class name
     */
    private String packageName(String cn) {
        int pos = cn.lastIndexOf('.');
        return (pos < 0) ? "" : cn.substring(0, pos);
    }


    /**
     * Returns the ModuleReader for the given module.
     */
    private ModuleReader moduleReaderFor(ModuleReference mref) {
        return moduleToReader.computeIfAbsent(mref, m -> createModuleReader(mref));
    }

    /**
     * Creates a ModuleReader for the given module.
     */
    private ModuleReader createModuleReader(ModuleReference mref) {
        try {
            return mref.open();
        } catch (IOException e) {
            // Return a null module reader to avoid a future class load
            // attempting to open the module again.
            return new NullModuleReader();
        }
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
    }

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
}
