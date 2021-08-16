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
import java.lang.module.ModuleReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.module.Modules;
import jdk.internal.module.ServicesCatalog;
import jdk.internal.util.StaticProperty;

/**
 * Find resources and packages in modules defined to the boot class loader or
 * resources and packages on the "boot class path" specified via -Xbootclasspath/a.
 */

public class BootLoader {
    private BootLoader() { }

    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

    // The unnamed module for the boot loader
    private static final Module UNNAMED_MODULE;
    private static final String JAVA_HOME = StaticProperty.javaHome();

    static {
        JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
        UNNAMED_MODULE = jla.defineUnnamedModule(null);
        jla.addEnableNativeAccess(UNNAMED_MODULE);
        setBootLoaderUnnamedModule0(UNNAMED_MODULE);
    }

    // ClassLoaderValue map for the boot class loader
    private static final ConcurrentHashMap<?, ?> CLASS_LOADER_VALUE_MAP
        = new ConcurrentHashMap<>();

    // native libraries loaded by the boot class loader
    private static final NativeLibraries NATIVE_LIBS
        = NativeLibraries.jniNativeLibraries(null);

    /**
     * Returns the unnamed module for the boot loader.
     */
    public static Module getUnnamedModule() {
        return UNNAMED_MODULE;
    }

    /**
     * Returns the ServiceCatalog for modules defined to the boot class loader.
     */
    public static ServicesCatalog getServicesCatalog() {
        return ServicesCatalog.getServicesCatalog(ClassLoaders.bootLoader());
    }

    /**
     * Returns the ClassLoaderValue map for the boot class loader.
     */
    public static ConcurrentHashMap<?, ?> getClassLoaderValueMap() {
        return CLASS_LOADER_VALUE_MAP;
    }

    /**
     * Returns NativeLibraries for the boot class loader.
     */
    public static NativeLibraries getNativeLibraries() {
        return NATIVE_LIBS;
    }

    /**
     * Returns {@code true} if there is a class path associated with the
     * BootLoader.
     */
    public static boolean hasClassPath() {
        return ClassLoaders.bootLoader().hasClassPath();
    }

    /**
     * Registers a module with this class loader so that its classes
     * (and resources) become visible via this class loader.
     */
    public static void loadModule(ModuleReference mref) {
        ClassLoaders.bootLoader().loadModule(mref);
    }

    /**
     * Loads the Class object with the given name defined to the boot loader.
     */
    public static Class<?> loadClassOrNull(String name) {
        return JLA.findBootstrapClassOrNull(name);
    }

    /**
     * Loads the Class object with the given name in the given module
     * defined to the boot loader. Returns {@code null} if not found.
     */
    public static Class<?> loadClass(Module module, String name) {
        Class<?> c = loadClassOrNull(name);
        if (c != null && c.getModule() == module) {
            return c;
        } else {
            return null;
        }
    }

    /**
     * Loads a native library from the system library path.
     */
    @SuppressWarnings("removal")
    public static void loadLibrary(String name) {
        if (System.getSecurityManager() == null) {
            BootLoader.getNativeLibraries().loadLibrary(name);
        } else {
            AccessController.doPrivileged(new java.security.PrivilegedAction<>() {
                public Void run() {
                    BootLoader.getNativeLibraries().loadLibrary(name);
                    return null;
                }
            });
        }
    }

    /**
     * Returns a URL to a resource in a module defined to the boot loader.
     */
    public static URL findResource(String mn, String name) throws IOException {
        return ClassLoaders.bootLoader().findResource(mn, name);
    }

    /**
     * Returns an input stream to a resource in a module defined to the
     * boot loader.
     */
    public static InputStream findResourceAsStream(String mn, String name)
        throws IOException
    {
        return ClassLoaders.bootLoader().findResourceAsStream(mn, name);
    }

    /**
     * Returns the URL to the given resource in any of the modules
     * defined to the boot loader and the boot class path.
     */
    public static URL findResource(String name) {
        return ClassLoaders.bootLoader().findResource(name);
    }

    /**
     * Returns an Iterator to iterate over the resources of the given name
     * in any of the modules defined to the boot loader.
     */
    public static Enumeration<URL> findResources(String name) throws IOException {
        return ClassLoaders.bootLoader().findResources(name);
    }

    /**
     * Define a package for the given class to the boot loader, if not already
     * defined.
     */
    public static Package definePackage(Class<?> c) {
        return getDefinedPackage(c.getPackageName());
    }

    /**
     * Returns the Package of the given name defined to the boot loader or null
     * if the package has not been defined.
     */
    public static Package getDefinedPackage(String pn) {
        Package pkg = ClassLoaders.bootLoader().getDefinedPackage(pn);
        if (pkg == null) {
            String location = getSystemPackageLocation(pn.replace('.', '/'));
            if (location != null) {
                pkg = PackageHelper.definePackage(pn.intern(), location);
            }
        }
        return pkg;
    }

    /**
     * Returns a stream of the packages defined to the boot loader.
     */
    public static Stream<Package> packages() {
        return Arrays.stream(getSystemPackageNames())
                     .map(name -> getDefinedPackage(name.replace('/', '.')));
    }

    /**
     * Helper class to define {@code Package} objects for packages in modules
     * defined to the boot loader.
     */
    static class PackageHelper {
        private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

        /**
         * Define the {@code Package} with the given name. The specified
         * location is a jrt URL to a named module in the run-time image,
         * a file URL to a module in an exploded run-time image, or a file
         * path to an entry on the boot class path (java agent Boot-Class-Path
         * or -Xbootclasspath/a.
         *
         * <p> If the given location is a JAR file containing a manifest,
         * the defined Package contains the versioning information from
         * the manifest, if present.
         *
         * @param name     package name
         * @param location location where the package is (jrt URL or file URL
         *                 for a named module in the run-time or exploded image;
         *                 a file path for a package from -Xbootclasspath/a)
         */
        static Package definePackage(String name, String location) {
            Module module = findModule(location);
            if (module != null) {
                // named module from runtime image or exploded module
                if (name.isEmpty())
                    throw new InternalError("empty package in " + location);
                return JLA.definePackage(ClassLoaders.bootLoader(), name, module);
            }

            // package in unnamed module (-Xbootclasspath/a)
            URL url = toFileURL(location);
            Manifest man = url != null ? getManifest(location) : null;

            return ClassLoaders.bootLoader().defineOrCheckPackage(name, man, url);
        }

        /**
         * Finds the module at the given location defined to the boot loader.
         * The module is either in runtime image or exploded image.
         * Otherwise this method returns null.
         */
        private static Module findModule(String location) {
            String mn = null;
            if (location.startsWith("jrt:/")) {
                // named module in runtime image ("jrt:/".length() == 5)
                mn = location.substring(5, location.length());
            } else if (location.startsWith("file:/")) {
                // named module in exploded image
                Path path = Path.of(URI.create(location));
                Path modulesDir = Path.of(JAVA_HOME, "modules");
                if (path.startsWith(modulesDir)) {
                    mn = path.getFileName().toString();
                }
            }

            // return the Module object for the module name. The Module may
            // in the boot layer or a child layer for the case that the module
            // is loaded into a running VM
            if (mn != null) {
                String name = mn;
                return Modules.findLoadedModule(mn)
                    .orElseThrow(() -> new InternalError(name + " not loaded"));
            } else {
                return null;
            }
        }

        /**
         * Returns URL if the given location is a regular file path.
         */
        @SuppressWarnings("removal")
        private static URL toFileURL(String location) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                public URL run() {
                    Path path = Path.of(location);
                    if (Files.isRegularFile(path)) {
                        try {
                            return path.toUri().toURL();
                        } catch (MalformedURLException e) {}
                    }
                    return null;
                }
            });
        }

        /**
         * Returns the Manifest if the given location is a JAR file
         * containing a manifest.
         */
        @SuppressWarnings("removal")
        private static Manifest getManifest(String location) {
            return AccessController.doPrivileged(new PrivilegedAction<>() {
                public Manifest run() {
                    Path jar = Path.of(location);
                    try (InputStream in = Files.newInputStream(jar);
                         JarInputStream jis = new JarInputStream(in, false)) {
                        return jis.getManifest();
                    } catch (IOException e) {
                        return null;
                    }
                }
            });
        }
    }

    /**
     * Returns an array of the binary name of the packages defined by
     * the boot loader, in VM internal form (forward slashes instead of dot).
     */
    private static native String[] getSystemPackageNames();

    /**
     * Returns the location of the package of the given name, if
     * defined by the boot loader; otherwise {@code null} is returned.
     *
     * The location may be a module from the runtime image or exploded image,
     * or from the boot class append path (i.e. -Xbootclasspath/a or
     * BOOT-CLASS-PATH attribute specified in java agent).
     */
    private static native String getSystemPackageLocation(String name);
    private static native void setBootLoaderUnnamedModule0(Module module);
}
