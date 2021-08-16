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
package jdk.internal.module;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jdk.internal.jimage.ImageLocation;
import jdk.internal.jimage.ImageReader;
import jdk.internal.jimage.ImageReaderFactory;
import jdk.internal.access.JavaNetUriAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.util.StaticProperty;
import jdk.internal.module.ModuleHashes.HashSupplier;

/**
 * The factory for SystemModules objects and for creating ModuleFinder objects
 * that find modules in the runtime image.
 *
 * This class supports initializing the module system when the runtime is an
 * images build, an exploded build, or an images build with java.base patched
 * by an exploded java.base. It also supports a testing mode that re-parses
 * the module-info.class resources in the run-time image.
 */

public final class SystemModuleFinders {
    private static final JavaNetUriAccess JNUA = SharedSecrets.getJavaNetUriAccess();

    private static final boolean USE_FAST_PATH;
    static {
        String value = System.getProperty("jdk.system.module.finder.disableFastPath");
        if (value == null) {
            USE_FAST_PATH = true;
        } else {
            USE_FAST_PATH = !value.isEmpty() && !Boolean.parseBoolean(value);
        }
    }

    // cached ModuleFinder returned from ofSystem
    private static volatile ModuleFinder cachedSystemModuleFinder;

    private SystemModuleFinders() { }

    /**
     * Returns the SystemModules object to reconstitute all modules. Returns
     * null if this is an exploded build or java.base is patched by an exploded
     * build.
     */
    static SystemModules allSystemModules() {
        if (USE_FAST_PATH) {
            return SystemModulesMap.allSystemModules();
        } else {
            return null;
        }
    }

    /**
     * Returns a SystemModules object to reconstitute the modules for the
     * given initial module. If the initial module is null then return the
     * SystemModules object to reconstitute the default modules.
     *
     * Return null if there is no SystemModules class for the initial module,
     * this is an exploded build, or java.base is patched by an exploded build.
     */
    static SystemModules systemModules(String initialModule) {
        if (USE_FAST_PATH) {
            if (initialModule == null) {
                return SystemModulesMap.defaultSystemModules();
            }

            String[] initialModules = SystemModulesMap.moduleNames();
            for (int i = 0; i < initialModules.length; i++) {
                String moduleName = initialModules[i];
                if (initialModule.equals(moduleName)) {
                    String cn = SystemModulesMap.classNames()[i];
                    try {
                        // one-arg Class.forName as java.base may not be defined
                        Constructor<?> ctor = Class.forName(cn).getConstructor();
                        return (SystemModules) ctor.newInstance();
                    } catch (Exception e) {
                        throw new InternalError(e);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a ModuleFinder that is backed by the given SystemModules object.
     *
     * @apiNote The returned ModuleFinder is thread safe.
     */
    static ModuleFinder of(SystemModules systemModules) {
        ModuleDescriptor[] descriptors = systemModules.moduleDescriptors();
        ModuleTarget[] targets = systemModules.moduleTargets();
        ModuleHashes[] recordedHashes = systemModules.moduleHashes();
        ModuleResolution[] moduleResolutions = systemModules.moduleResolutions();

        int moduleCount = descriptors.length;
        ModuleReference[] mrefs = new ModuleReference[moduleCount];
        @SuppressWarnings(value = {"rawtypes", "unchecked"})
        Map.Entry<String, ModuleReference>[] map
            = (Map.Entry<String, ModuleReference>[])new Map.Entry[moduleCount];

        Map<String, byte[]> nameToHash = generateNameToHash(recordedHashes);

        for (int i = 0; i < moduleCount; i++) {
            String name = descriptors[i].name();
            HashSupplier hashSupplier = hashSupplier(nameToHash, name);
            ModuleReference mref = toModuleReference(descriptors[i],
                                                     targets[i],
                                                     recordedHashes[i],
                                                     hashSupplier,
                                                     moduleResolutions[i]);
            mrefs[i] = mref;
            map[i] = Map.entry(name, mref);
        }

        return new SystemModuleFinder(mrefs, map);
    }

    /**
     * Returns the ModuleFinder to find all system modules. Supports both
     * images and exploded builds.
     *
     * @apiNote Used by ModuleFinder.ofSystem()
     */
    public static ModuleFinder ofSystem() {
        ModuleFinder finder = cachedSystemModuleFinder;
        if (finder != null) {
            return finder;
        }

        // probe to see if this is an images build
        String home = StaticProperty.javaHome();
        Path modules = Path.of(home, "lib", "modules");
        if (Files.isRegularFile(modules)) {
            if (USE_FAST_PATH) {
                SystemModules systemModules = allSystemModules();
                if (systemModules != null) {
                    finder = of(systemModules);
                }
            }

            // fall back to parsing the module-info.class files in image
            if (finder == null) {
                finder = ofModuleInfos();
            }

            cachedSystemModuleFinder = finder;
            return finder;

        }

        // exploded build (do not cache module finder)
        Path dir = Path.of(home, "modules");
        if (!Files.isDirectory(dir))
            throw new InternalError("Unable to detect the run-time image");
        ModuleFinder f = ModulePath.of(ModuleBootstrap.patcher(), dir);
        return new ModuleFinder() {
            @SuppressWarnings("removal")
            @Override
            public Optional<ModuleReference> find(String name) {
                PrivilegedAction<Optional<ModuleReference>> pa = () -> f.find(name);
                return AccessController.doPrivileged(pa);
            }
            @SuppressWarnings("removal")
            @Override
            public Set<ModuleReference> findAll() {
                PrivilegedAction<Set<ModuleReference>> pa = f::findAll;
                return AccessController.doPrivileged(pa);
            }
        };
    }

    /**
     * Parses the module-info.class of all module in the runtime image and
     * returns a ModuleFinder to find the modules.
     *
     * @apiNote The returned ModuleFinder is thread safe.
     */
    private static ModuleFinder ofModuleInfos() {
        // parse the module-info.class in every module
        Map<String, ModuleInfo.Attributes> nameToAttributes = new HashMap<>();
        Map<String, byte[]> nameToHash = new HashMap<>();
        ImageReader reader = SystemImage.reader();
        for (String mn : reader.getModuleNames()) {
            ImageLocation loc = reader.findLocation(mn, "module-info.class");
            ModuleInfo.Attributes attrs
                = ModuleInfo.read(reader.getResourceBuffer(loc), null);

            nameToAttributes.put(mn, attrs);
            ModuleHashes hashes = attrs.recordedHashes();
            if (hashes != null) {
                for (String name : hashes.names()) {
                    nameToHash.computeIfAbsent(name, k -> hashes.hashFor(name));
                }
            }
        }

        // create a ModuleReference for each module
        Set<ModuleReference> mrefs = new HashSet<>();
        Map<String, ModuleReference> nameToModule = new HashMap<>();
        for (Map.Entry<String, ModuleInfo.Attributes> e : nameToAttributes.entrySet()) {
            String mn = e.getKey();
            ModuleInfo.Attributes attrs = e.getValue();
            HashSupplier hashSupplier = hashSupplier(nameToHash, mn);
            ModuleReference mref = toModuleReference(attrs.descriptor(),
                                                     attrs.target(),
                                                     attrs.recordedHashes(),
                                                     hashSupplier,
                                                     attrs.moduleResolution());
            mrefs.add(mref);
            nameToModule.put(mn, mref);
        }

        return new SystemModuleFinder(mrefs, nameToModule);
    }

    /**
     * A ModuleFinder that finds module in an array or set of modules.
     */
    private static class SystemModuleFinder implements ModuleFinder {
        final Set<ModuleReference> mrefs;
        final Map<String, ModuleReference> nameToModule;

        SystemModuleFinder(ModuleReference[] array,
                           Map.Entry<String, ModuleReference>[] map) {
            this.mrefs = Set.of(array);
            this.nameToModule = Map.ofEntries(map);
        }

        SystemModuleFinder(Set<ModuleReference> mrefs,
                           Map<String, ModuleReference> nameToModule) {
            this.mrefs = Set.copyOf(mrefs);
            this.nameToModule = Map.copyOf(nameToModule);
        }

        @Override
        public Optional<ModuleReference> find(String name) {
            Objects.requireNonNull(name);
            return Optional.ofNullable(nameToModule.get(name));
        }

        @Override
        public Set<ModuleReference> findAll() {
            return mrefs;
        }
    }

    /**
     * Creates a ModuleReference to the system module.
     */
    static ModuleReference toModuleReference(ModuleDescriptor descriptor,
                                             ModuleTarget target,
                                             ModuleHashes recordedHashes,
                                             HashSupplier hasher,
                                             ModuleResolution mres) {
        String mn = descriptor.name();
        URI uri = JNUA.create("jrt", "/".concat(mn));

        Supplier<ModuleReader> readerSupplier = new Supplier<>() {
            @Override
            public ModuleReader get() {
                return new SystemModuleReader(mn, uri);
            }
        };

        ModuleReference mref = new ModuleReferenceImpl(descriptor,
                                                       uri,
                                                       readerSupplier,
                                                       null,
                                                       target,
                                                       recordedHashes,
                                                       hasher,
                                                       mres);

        // may need a reference to a patched module if --patch-module specified
        mref = ModuleBootstrap.patcher().patchIfNeeded(mref);

        return mref;
    }

    /**
     * Generates a map of module name to hash value.
     */
    static Map<String, byte[]> generateNameToHash(ModuleHashes[] recordedHashes) {
        Map<String, byte[]> nameToHash = null;

        boolean secondSeen = false;
        // record the hashes to build HashSupplier
        for (ModuleHashes mh : recordedHashes) {
            if (mh != null) {
                // if only one module contain ModuleHashes, use it
                if (nameToHash == null) {
                    nameToHash = mh.hashes();
                } else {
                    if (!secondSeen) {
                        nameToHash = new HashMap<>(nameToHash);
                        secondSeen = true;
                    }
                    nameToHash.putAll(mh.hashes());
                }
            }
        }
        return (nameToHash != null) ? nameToHash : Map.of();
    }

    /**
     * Returns a HashSupplier that returns the hash of the given module.
     */
    static HashSupplier hashSupplier(Map<String, byte[]> nameToHash, String name) {
        byte[] hash = nameToHash.get(name);
        if (hash != null) {
            // avoid lambda here
            return new HashSupplier() {
                @Override
                public byte[] generate(String algorithm) {
                    return hash;
                }
            };
        } else {
            return null;
        }
    }

    /**
     * Holder class for the ImageReader
     *
     * @apiNote This class must be loaded before a security manager is set.
     */
    private static class SystemImage {
        static final ImageReader READER = ImageReaderFactory.getImageReader();
        static ImageReader reader() {
            return READER;
        }
    }

    /**
     * A ModuleReader for reading resources from a module linked into the
     * run-time image.
     */
    private static class SystemModuleReader implements ModuleReader {
        private final String module;
        private volatile boolean closed;

        /**
         * If there is a security manager set then check permission to
         * connect to the run-time image.
         */
        private static void checkPermissionToConnect(URI uri) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                try {
                    URLConnection uc = uri.toURL().openConnection();
                    sm.checkPermission(uc.getPermission());
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            }
        }

        SystemModuleReader(String module, URI uri) {
            checkPermissionToConnect(uri);
            this.module = module;
        }

        /**
         * Returns the ImageLocation for the given resource, {@code null}
         * if not found.
         */
        private ImageLocation findImageLocation(String name) throws IOException {
            Objects.requireNonNull(name);
            if (closed)
                throw new IOException("ModuleReader is closed");
            ImageReader imageReader = SystemImage.reader();
            if (imageReader != null) {
                return imageReader.findLocation(module, name);
            } else {
                // not an images build
                return null;
            }
        }

        /**
         * Returns {@code true} if the given resource exists, {@code false}
         * if not found.
         */
        private boolean containsImageLocation(String name) throws IOException {
            Objects.requireNonNull(name);
            if (closed)
                throw new IOException("ModuleReader is closed");
            ImageReader imageReader = SystemImage.reader();
            if (imageReader != null) {
                return imageReader.verifyLocation(module, name);
            } else {
                // not an images build
                return false;
            }
        }

        @Override
        public Optional<URI> find(String name) throws IOException {
            if (containsImageLocation(name)) {
                URI u = JNUA.create("jrt", "/" + module + "/" + name);
                return Optional.of(u);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Optional<InputStream> open(String name) throws IOException {
            return read(name).map(this::toInputStream);
        }

        private InputStream toInputStream(ByteBuffer bb) { // ## -> ByteBuffer?
            try {
                int rem = bb.remaining();
                byte[] bytes = new byte[rem];
                bb.get(bytes);
                return new ByteArrayInputStream(bytes);
            } finally {
                release(bb);
            }
        }

        @Override
        public Optional<ByteBuffer> read(String name) throws IOException {
            ImageLocation location = findImageLocation(name);
            if (location != null) {
                return Optional.of(SystemImage.reader().getResourceBuffer(location));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public void release(ByteBuffer bb) {
            Objects.requireNonNull(bb);
            ImageReader.releaseByteBuffer(bb);
        }

        @Override
        public Stream<String> list() throws IOException {
            if (closed)
                throw new IOException("ModuleReader is closed");

            Spliterator<String> s = new ModuleContentSpliterator(module);
            return StreamSupport.stream(s, false);
        }

        @Override
        public void close() {
            // nothing else to do
            closed = true;
        }
    }

    /**
     * A Spliterator for traversing the resources of a module linked into the
     * run-time image.
     */
    private static class ModuleContentSpliterator implements Spliterator<String> {
        final String moduleRoot;
        final Deque<ImageReader.Node> stack;
        Iterator<ImageReader.Node> iterator;

        ModuleContentSpliterator(String module) throws IOException {
            moduleRoot = "/modules/" + module;
            stack = new ArrayDeque<>();

            // push the root node to the stack to get started
            ImageReader.Node dir = SystemImage.reader().findNode(moduleRoot);
            if (dir == null || !dir.isDirectory())
                throw new IOException(moduleRoot + " not a directory");
            stack.push(dir);
            iterator = Collections.emptyIterator();
        }

        /**
         * Returns the name of the next non-directory node or {@code null} if
         * there are no remaining nodes to visit.
         */
        private String next() throws IOException {
            for (;;) {
                while (iterator.hasNext()) {
                    ImageReader.Node node = iterator.next();
                    String name = node.getName();
                    if (node.isDirectory()) {
                        // build node
                        ImageReader.Node dir = SystemImage.reader().findNode(name);
                        assert dir.isDirectory();
                        stack.push(dir);
                    } else {
                        // strip /modules/$MODULE/ prefix
                        return name.substring(moduleRoot.length() + 1);
                    }
                }

                if (stack.isEmpty()) {
                    return null;
                } else {
                    ImageReader.Node dir = stack.poll();
                    assert dir.isDirectory();
                    iterator = dir.getChildren().iterator();
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            String next;
            try {
                next = next();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
            if (next != null) {
                action.accept(next);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Spliterator<String> trySplit() {
            return null;
        }

        @Override
        public int characteristics() {
            return Spliterator.DISTINCT + Spliterator.NONNULL + Spliterator.IMMUTABLE;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }
    }
}
