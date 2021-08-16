/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.module;

import java.io.Closeable;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import jdk.internal.loader.Resource;
import jdk.internal.access.JavaLangModuleAccess;
import jdk.internal.access.SharedSecrets;
import sun.net.www.ParseUtil;


/**
 * Provides support for patching modules, mostly the boot layer.
 */

public final class ModulePatcher {

    private static final JavaLangModuleAccess JLMA
        = SharedSecrets.getJavaLangModuleAccess();

    // module name -> sequence of patches (directories or JAR files)
    private final Map<String, List<Path>> map;

    /**
     * Initialize the module patcher with the given map. The map key is
     * the module name, the value is a list of path strings.
     */
    public ModulePatcher(Map<String, List<String>> input) {
        if (input.isEmpty()) {
            this.map = Map.of();
        } else {
            Map<String, List<Path>> map = new HashMap<>();
            for (Map.Entry<String, List<String>> e : input.entrySet()) {
                String mn = e.getKey();
                List<Path> paths = e.getValue().stream()
                        .map(Paths::get)
                        .toList();
                map.put(mn, paths);
            }
            this.map = map;
        }
    }

    /**
     * Returns a module reference that interposes on the given module if
     * needed. If there are no patches for the given module then the module
     * reference is simply returned. Otherwise the patches for the module
     * are scanned (to find any new packages) and a new module reference is
     * returned.
     *
     * @throws UncheckedIOException if an I/O error is detected
     */
    public ModuleReference patchIfNeeded(ModuleReference mref) {
        // if there are no patches for the module then nothing to do
        ModuleDescriptor descriptor = mref.descriptor();
        String mn = descriptor.name();
        List<Path> paths = map.get(mn);
        if (paths == null)
            return mref;

        // Scan the JAR file or directory tree to get the set of packages.
        // For automatic modules then packages that do not contain class files
        // must be ignored.
        Set<String> packages = new HashSet<>();
        boolean isAutomatic = descriptor.isAutomatic();
        try {
            for (Path file : paths) {
                if (Files.isRegularFile(file)) {

                    // JAR file - do not open as a multi-release JAR as this
                    // is not supported by the boot class loader
                    try (JarFile jf = new JarFile(file.toString())) {
                        jf.stream()
                          .filter(e -> !e.isDirectory()
                                  && (!isAutomatic || e.getName().endsWith(".class")))
                          .map(e -> toPackageName(file, e))
                          .filter(Checks::isPackageName)
                          .forEach(packages::add);
                    }

                } else if (Files.isDirectory(file)) {

                    // exploded directory without following sym links
                    Path top = file;
                    Files.find(top, Integer.MAX_VALUE,
                               ((path, attrs) -> attrs.isRegularFile()))
                            .filter(path -> (!isAutomatic
                                    || path.toString().endsWith(".class"))
                                    && !isHidden(path))
                            .map(path -> toPackageName(top, path))
                            .filter(Checks::isPackageName)
                            .forEach(packages::add);

                }
            }

        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        // if there are new packages then we need a new ModuleDescriptor
        packages.removeAll(descriptor.packages());
        if (!packages.isEmpty()) {
            Builder builder = JLMA.newModuleBuilder(descriptor.name(),
                                                    /*strict*/ descriptor.isAutomatic(),
                                                    descriptor.modifiers());
            if (!descriptor.isAutomatic()) {
                descriptor.requires().forEach(builder::requires);
                descriptor.exports().forEach(builder::exports);
                descriptor.opens().forEach(builder::opens);
                descriptor.uses().forEach(builder::uses);
            }
            descriptor.provides().forEach(builder::provides);

            descriptor.version().ifPresent(builder::version);
            descriptor.mainClass().ifPresent(builder::mainClass);

            // original + new packages
            builder.packages(descriptor.packages());
            builder.packages(packages);

            descriptor = builder.build();
        }

        // return a module reference to the patched module
        URI location = mref.location().orElse(null);

        ModuleTarget target = null;
        ModuleHashes recordedHashes = null;
        ModuleHashes.HashSupplier hasher = null;
        ModuleResolution mres = null;
        if (mref instanceof ModuleReferenceImpl) {
            ModuleReferenceImpl impl = (ModuleReferenceImpl)mref;
            target = impl.moduleTarget();
            recordedHashes = impl.recordedHashes();
            hasher = impl.hasher();
            mres = impl.moduleResolution();
        }

        return new ModuleReferenceImpl(descriptor,
                                       location,
                                       () -> new PatchedModuleReader(paths, mref),
                                       this,
                                       target,
                                       recordedHashes,
                                       hasher,
                                       mres);

    }

    /**
     * Returns true is this module patcher has patches.
     */
    public boolean hasPatches() {
        return !map.isEmpty();
    }

    /*
     * Returns the names of the patched modules.
     */
    Set<String> patchedModules() {
        return map.keySet();
    }

    /**
     * A ModuleReader that reads resources from a patched module.
     *
     * This class is public so as to expose the findResource method to the
     * built-in class loaders and avoid locating the resource twice during
     * class loading (once to locate the resource, the second to gets the
     * URL for the CodeSource).
     */
    public static class PatchedModuleReader implements ModuleReader {
        private final List<ResourceFinder> finders;
        private final ModuleReference mref;
        private final URL delegateCodeSourceURL;
        private volatile ModuleReader delegate;

        /**
         * Creates the ModuleReader to reads resources in a patched module.
         */
        PatchedModuleReader(List<Path> patches, ModuleReference mref) {
            List<ResourceFinder> finders = new ArrayList<>();
            boolean initialized = false;
            try {
                for (Path file : patches) {
                    if (Files.isRegularFile(file)) {
                        finders.add(new JarResourceFinder(file));
                    } else {
                        finders.add(new ExplodedResourceFinder(file));
                    }
                }
                initialized = true;
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            } finally {
                // close all ResourceFinder in the event of an error
                if (!initialized) closeAll(finders);
            }

            this.finders = finders;
            this.mref = mref;
            this.delegateCodeSourceURL = codeSourceURL(mref);
        }

        /**
         * Closes all resource finders.
         */
        private static void closeAll(List<ResourceFinder> finders) {
            for (ResourceFinder finder : finders) {
                try { finder.close(); } catch (IOException ioe) { }
            }
        }

        /**
         * Returns the code source URL for the given module.
         */
        private static URL codeSourceURL(ModuleReference mref) {
            try {
                Optional<URI> ouri = mref.location();
                if (ouri.isPresent())
                    return ouri.get().toURL();
            } catch (MalformedURLException e) { }
            return null;
        }

        /**
         * Returns the ModuleReader to delegate to when the resource is not
         * found in a patch location.
         */
        private ModuleReader delegate() throws IOException {
            ModuleReader r = delegate;
            if (r == null) {
                synchronized (this) {
                    r = delegate;
                    if (r == null) {
                        delegate = r = mref.open();
                    }
                }
            }
            return r;
        }

        /**
         * Finds a resources in the patch locations. Returns null if not found
         * or the name is "module-info.class" as that cannot be overridden.
         */
        private Resource findResourceInPatch(String name) throws IOException {
            if (!name.equals("module-info.class")) {
                for (ResourceFinder finder : finders) {
                    Resource r = finder.find(name);
                    if (r != null)
                        return r;
                }
            }
            return null;
        }

        /**
         * Finds a resource of the given name in the patched module.
         */
        public Resource findResource(String name) throws IOException {

            // patch locations
            Resource r = findResourceInPatch(name);
            if (r != null)
                return r;

            // original module
            ByteBuffer bb = delegate().read(name).orElse(null);
            if (bb == null)
                return null;

            return new Resource() {
                private <T> T shouldNotGetHere(Class<T> type) {
                    throw new InternalError("should not get here");
                }
                @Override
                public String getName() {
                    return shouldNotGetHere(String.class);
                }
                @Override
                public URL getURL() {
                    return shouldNotGetHere(URL.class);
                }
                @Override
                public URL getCodeSourceURL() {
                    return delegateCodeSourceURL;
                }
                @Override
                public ByteBuffer getByteBuffer() throws IOException {
                    return bb;
                }
                @Override
                public InputStream getInputStream() throws IOException {
                    return shouldNotGetHere(InputStream.class);
                }
                @Override
                public int getContentLength() throws IOException {
                    return shouldNotGetHere(int.class);
                }
            };
        }

        @Override
        public Optional<URI> find(String name) throws IOException {
            Resource r = findResourceInPatch(name);
            if (r != null) {
                URI uri = URI.create(r.getURL().toString());
                return Optional.of(uri);
            } else {
                return delegate().find(name);
            }
        }

        @Override
        public Optional<InputStream> open(String name) throws IOException {
            Resource r = findResourceInPatch(name);
            if (r != null) {
                return Optional.of(r.getInputStream());
            } else {
                return delegate().open(name);
            }
        }

        @Override
        public Optional<ByteBuffer> read(String name) throws IOException {
            Resource r = findResourceInPatch(name);
            if (r != null) {
                ByteBuffer bb = r.getByteBuffer();
                assert !bb.isDirect();
                return Optional.of(bb);
            } else {
                return delegate().read(name);
            }
        }

        @Override
        public void release(ByteBuffer bb) {
            if (bb.isDirect()) {
                try {
                    delegate().release(bb);
                } catch (IOException ioe) {
                    throw new InternalError(ioe);
                }
            }
        }

        @Override
        public Stream<String> list() throws IOException {
            Stream<String> s = delegate().list();
            for (ResourceFinder finder : finders) {
                s = Stream.concat(s, finder.list());
            }
            return s.distinct();
        }

        @Override
        public void close() throws IOException {
            closeAll(finders);
            delegate().close();
        }
    }


    /**
     * A resource finder that find resources in a patch location.
     */
    private static interface ResourceFinder extends Closeable {
        Resource find(String name) throws IOException;
        Stream<String> list() throws IOException;
    }


    /**
     * A ResourceFinder that finds resources in a JAR file.
     */
    private static class JarResourceFinder implements ResourceFinder {
        private final JarFile jf;
        private final URL csURL;

        JarResourceFinder(Path path) throws IOException {
            this.jf = new JarFile(path.toString());
            this.csURL = path.toUri().toURL();
        }

        @Override
        public void close() throws IOException {
            jf.close();
        }

        @Override
        public Resource find(String name) throws IOException {
            JarEntry entry = jf.getJarEntry(name);
            if (entry == null)
                return null;

            return new Resource() {
                @Override
                public String getName() {
                    return name;
                }
                @Override
                public URL getURL() {
                    String encodedPath = ParseUtil.encodePath(name, false);
                    try {
                        return new URL("jar:" + csURL + "!/" + encodedPath);
                    } catch (MalformedURLException e) {
                        return null;
                    }
                }
                @Override
                public URL getCodeSourceURL() {
                    return csURL;
                }
                @Override
                public ByteBuffer getByteBuffer() throws IOException {
                    byte[] bytes = getInputStream().readAllBytes();
                    return ByteBuffer.wrap(bytes);
                }
                @Override
                public InputStream getInputStream() throws IOException {
                    return jf.getInputStream(entry);
                }
                @Override
                public int getContentLength() throws IOException {
                    long size = entry.getSize();
                    return (size > Integer.MAX_VALUE) ? -1 : (int) size;
                }
            };
        }

        @Override
        public Stream<String> list() throws IOException {
            return jf.stream().map(JarEntry::getName);
        }
    }


    /**
     * A ResourceFinder that finds resources on the file system.
     */
    private static class ExplodedResourceFinder implements ResourceFinder {
        private final Path dir;

        ExplodedResourceFinder(Path dir) {
            this.dir = dir;
        }

        @Override
        public void close() { }

        @Override
        public Resource find(String name) throws IOException {
            Path file = Resources.toFilePath(dir, name);
            if (file != null) {
                return newResource(name, dir, file);
            } else {
                return null;
            }
        }

        private Resource newResource(String name, Path top, Path file) {
            return new Resource() {
                @Override
                public String getName() {
                    return name;
                }
                @Override
                public URL getURL() {
                    try {
                        return file.toUri().toURL();
                    } catch (IOException | IOError e) {
                        return null;
                    }
                }
                @Override
                public URL getCodeSourceURL() {
                    try {
                        return top.toUri().toURL();
                    } catch (IOException | IOError e) {
                        return null;
                    }
                }
                @Override
                public ByteBuffer getByteBuffer() throws IOException {
                    return ByteBuffer.wrap(Files.readAllBytes(file));
                }
                @Override
                public InputStream getInputStream() throws IOException {
                    return Files.newInputStream(file);
                }
                @Override
                public int getContentLength() throws IOException {
                    long size = Files.size(file);
                    return (size > Integer.MAX_VALUE) ? -1 : (int)size;
                }
            };
        }

        @Override
        public Stream<String> list() throws IOException {
            return Files.walk(dir, Integer.MAX_VALUE)
                        .map(f -> Resources.toResourceName(dir, f))
                        .filter(s -> !s.isEmpty());
        }
    }


    /**
     * Derives a package name from the file path of an entry in an exploded patch
     */
    private static String toPackageName(Path top, Path file) {
        Path entry = top.relativize(file);
        Path parent = entry.getParent();
        if (parent == null) {
            return warnIfModuleInfo(top, entry.toString());
        } else {
            return parent.toString().replace(File.separatorChar, '.');
        }
    }

    /**
     * Returns true if the given file exists and is a hidden file
     */
    private boolean isHidden(Path file) {
        try {
            return Files.isHidden(file);
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Derives a package name from the name of an entry in a JAR file.
     */
    private static String toPackageName(Path file, JarEntry entry) {
        String name = entry.getName();
        int index = name.lastIndexOf("/");
        if (index == -1) {
            return warnIfModuleInfo(file, name);
        } else {
            return name.substring(0, index).replace('/', '.');
        }
    }

    private static String warnIfModuleInfo(Path file, String e) {
        if (e.equals("module-info.class"))
            System.err.println("WARNING: " + e + " ignored in patch: " + file);
        return "";
    }
}
