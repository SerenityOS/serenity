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
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.jar.Manifest;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.VM;
import jdk.internal.module.ServicesCatalog;

/**
 * Creates and provides access to the built-in platform and application class
 * loaders. It also creates the class loader that is used to locate resources
 * in modules defined to the boot class loader.
 */

public class ClassLoaders {

    private ClassLoaders() { }

    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

    // the built-in class loaders
    private static final BootClassLoader BOOT_LOADER;
    private static final PlatformClassLoader PLATFORM_LOADER;
    private static final AppClassLoader APP_LOADER;

    // Sets the ServicesCatalog for the specified loader using archived objects.
    private static void setArchivedServicesCatalog(ClassLoader loader) {
        ServicesCatalog catalog = ArchivedClassLoaders.get().servicesCatalog(loader);
        ServicesCatalog.putServicesCatalog(loader, catalog);
    }

    // Creates the built-in class loaders.
    static {
        ArchivedClassLoaders archivedClassLoaders = ArchivedClassLoaders.get();
        if (archivedClassLoaders != null) {
            // assert VM.getSavedProperty("jdk.boot.class.path.append") == null
            BOOT_LOADER = (BootClassLoader) archivedClassLoaders.bootLoader();
            setArchivedServicesCatalog(BOOT_LOADER);
            PLATFORM_LOADER = (PlatformClassLoader) archivedClassLoaders.platformLoader();
            setArchivedServicesCatalog(PLATFORM_LOADER);
        } else {
            // -Xbootclasspath/a or -javaagent with Boot-Class-Path attribute
            String append = VM.getSavedProperty("jdk.boot.class.path.append");
            URLClassPath ucp = (append != null && !append.isEmpty())
                    ? new URLClassPath(append, true)
                    : null;
            BOOT_LOADER = new BootClassLoader(ucp);
            PLATFORM_LOADER = new PlatformClassLoader(BOOT_LOADER);
        }
        // A class path is required when no initial module is specified.
        // In this case the class path defaults to "", meaning the current
        // working directory.  When an initial module is specified, on the
        // contrary, we drop this historic interpretation of the empty
        // string and instead treat it as unspecified.
        String cp = System.getProperty("java.class.path");
        if (cp == null || cp.isEmpty()) {
            String initialModuleName = System.getProperty("jdk.module.main");
            cp = (initialModuleName == null) ? "" : null;
        }
        URLClassPath ucp = new URLClassPath(cp, false);
        if (archivedClassLoaders != null) {
            APP_LOADER = (AppClassLoader) archivedClassLoaders.appLoader();
            setArchivedServicesCatalog(APP_LOADER);
            APP_LOADER.setClassPath(ucp);
        } else {
            APP_LOADER = new AppClassLoader(PLATFORM_LOADER, ucp);
            ArchivedClassLoaders.archive();
        }
    }

    /**
     * Returns the class loader that is used to find resources in modules
     * defined to the boot class loader.
     *
     * @apiNote This method is not public, it should instead be used via
     * the BootLoader class that provides a restricted API to this class
     * loader.
     */
    static BuiltinClassLoader bootLoader() {
        return BOOT_LOADER;
    }

    /**
     * Returns the platform class loader.
     */
    public static ClassLoader platformClassLoader() {
        return PLATFORM_LOADER;
    }

    /**
     * Returns the application class loader.
     */
    public static ClassLoader appClassLoader() {
        return APP_LOADER;
    }

    /**
     * The class loader that is used to find resources in modules defined to
     * the boot class loader. It is not used for class loading.
     */
    private static class BootClassLoader extends BuiltinClassLoader {
        BootClassLoader(URLClassPath bcp) {
            super(null, null, bcp);
        }

        @Override
        protected Class<?> loadClassOrNull(String cn, boolean resolve) {
            return JLA.findBootstrapClassOrNull(cn);
        }
    };

    /**
     * The platform class loader, a unique type to make it easier to distinguish
     * from the application class loader.
     */
    private static class PlatformClassLoader extends BuiltinClassLoader {
        static {
            if (!ClassLoader.registerAsParallelCapable())
                throw new InternalError();
        }

        PlatformClassLoader(BootClassLoader parent) {
            super("platform", parent, null);
        }
    }

    /**
     * The application class loader that is a {@code BuiltinClassLoader} with
     * customizations to be compatible with long standing behavior.
     */
    private static class AppClassLoader extends BuiltinClassLoader {
        static {
            if (!ClassLoader.registerAsParallelCapable())
                throw new InternalError();
        }

        AppClassLoader(BuiltinClassLoader parent, URLClassPath ucp) {
            super("app", parent, ucp);
        }

        @Override
        protected Class<?> loadClass(String cn, boolean resolve)
            throws ClassNotFoundException
        {
            // for compatibility reasons, say where restricted package list has
            // been updated to list API packages in the unnamed module.
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                int i = cn.lastIndexOf('.');
                if (i != -1) {
                    sm.checkPackageAccess(cn.substring(0, i));
                }
            }

            return super.loadClass(cn, resolve);
        }

        @Override
        protected PermissionCollection getPermissions(CodeSource cs) {
            PermissionCollection perms = super.getPermissions(cs);
            perms.add(new RuntimePermission("exitVM"));
            return perms;
        }

        /**
         * Called by the VM to support dynamic additions to the class path
         *
         * @see java.lang.instrument.Instrumentation#appendToSystemClassLoaderSearch
         */
        void appendToClassPathForInstrumentation(String path) {
            appendClassPath(path);
        }

        /**
         * Called by the VM to support define package for AppCDS
         */
        protected Package defineOrCheckPackage(String pn, Manifest man, URL url) {
            return super.defineOrCheckPackage(pn, man, url);
        }

        /**
         * Called by the VM, during -Xshare:dump
         */
        private void resetArchivedStates() {
            setClassPath(null);
        }
    }

    /**
     * Attempts to convert the given string to a file URL.
     *
     * @apiNote This is called by the VM
     */
    @Deprecated
    private static URL toFileURL(String s) {
        try {
            // Use an intermediate File object to construct a URI/URL without
            // authority component as URLClassPath can't handle URLs with a UNC
            // server name in the authority component.
            return Path.of(s).toRealPath().toFile().toURI().toURL();
        } catch (InvalidPathException | IOException ignore) {
            // malformed path string or class path element does not exist
            return null;
        }
    }
}
