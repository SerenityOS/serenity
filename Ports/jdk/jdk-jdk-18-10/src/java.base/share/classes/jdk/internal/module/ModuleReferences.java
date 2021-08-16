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

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import jdk.internal.jmod.JmodFile;
import jdk.internal.module.ModuleHashes.HashSupplier;
import sun.net.www.ParseUtil;


/**
 * A factory for creating ModuleReference implementations where the modules are
 * packaged as modular JAR file, JMOD files or where the modules are exploded
 * on the file system.
 */

class ModuleReferences {
    private ModuleReferences() { }

    /**
     * Creates a ModuleReference to a possibly-patched module
     */
    private static ModuleReference newModule(ModuleInfo.Attributes attrs,
                                             URI uri,
                                             Supplier<ModuleReader> supplier,
                                             ModulePatcher patcher,
                                             HashSupplier hasher) {
        ModuleReference mref = new ModuleReferenceImpl(attrs.descriptor(),
                                                       uri,
                                                       supplier,
                                                       null,
                                                       attrs.target(),
                                                       attrs.recordedHashes(),
                                                       hasher,
                                                       attrs.moduleResolution());
        if (patcher != null)
            mref = patcher.patchIfNeeded(mref);

        return mref;
    }

    /**
     * Creates a ModuleReference to a possibly-patched module in a modular JAR.
     */
    static ModuleReference newJarModule(ModuleInfo.Attributes attrs,
                                        ModulePatcher patcher,
                                        Path file) {
        URI uri = file.toUri();
        Supplier<ModuleReader> supplier = () -> new JarModuleReader(file, uri);
        HashSupplier hasher = (a) -> ModuleHashes.computeHash(supplier, a);
        return newModule(attrs, uri, supplier, patcher, hasher);
    }

    /**
     * Creates a ModuleReference to a module in a JMOD file.
     */
    static ModuleReference newJModModule(ModuleInfo.Attributes attrs, Path file) {
        URI uri = file.toUri();
        Supplier<ModuleReader> supplier = () -> new JModModuleReader(file, uri);
        HashSupplier hasher = (a) -> ModuleHashes.computeHash(supplier, a);
        return newModule(attrs, uri, supplier, null, hasher);
    }

    /**
     * Creates a ModuleReference to a possibly-patched exploded module.
     */
    static ModuleReference newExplodedModule(ModuleInfo.Attributes attrs,
                                             ModulePatcher patcher,
                                             Path dir) {
        Supplier<ModuleReader> supplier = () -> new ExplodedModuleReader(dir);
        return newModule(attrs, dir.toUri(), supplier, patcher, null);
    }


    /**
     * A base module reader that encapsulates machinery required to close the
     * module reader safely.
     */
    static abstract class SafeCloseModuleReader implements ModuleReader {

        // RW lock to support safe close
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final Lock readLock = lock.readLock();
        private final Lock writeLock = lock.writeLock();
        private boolean closed;

        SafeCloseModuleReader() { }

        /**
         * Returns a URL to  resource. This method is invoked by the find
         * method to do the actual work of finding the resource.
         */
        abstract Optional<URI> implFind(String name) throws IOException;

        /**
         * Returns an input stream for reading a resource. This method is
         * invoked by the open method to do the actual work of opening
         * an input stream to the resource.
         */
        abstract Optional<InputStream> implOpen(String name) throws IOException;

        /**
         * Returns a stream of the names of resources in the module. This
         * method is invoked by the list method to do the actual work of
         * creating the stream.
         */
        abstract Stream<String> implList() throws IOException;

        /**
         * Closes the module reader. This method is invoked by close to do the
         * actual work of closing the module reader.
         */
        abstract void implClose() throws IOException;

        @Override
        public final Optional<URI> find(String name) throws IOException {
            readLock.lock();
            try {
                if (!closed) {
                    return implFind(name);
                } else {
                    throw new IOException("ModuleReader is closed");
                }
            } finally {
                readLock.unlock();
            }
        }


        @Override
        public final Optional<InputStream> open(String name) throws IOException {
            readLock.lock();
            try {
                if (!closed) {
                    return implOpen(name);
                } else {
                    throw new IOException("ModuleReader is closed");
                }
            } finally {
                readLock.unlock();
            }
        }

        @Override
        public final Stream<String> list() throws IOException {
            readLock.lock();
            try {
                if (!closed) {
                    return implList();
                } else {
                    throw new IOException("ModuleReader is closed");
                }
            } finally {
                readLock.unlock();
            }
        }

        @Override
        public final void close() throws IOException {
            writeLock.lock();
            try {
                if (!closed) {
                    closed = true;
                    implClose();
                }
            } finally {
                writeLock.unlock();
            }
        }
    }


    /**
     * A ModuleReader for a modular JAR file.
     */
    static class JarModuleReader extends SafeCloseModuleReader {
        private final JarFile jf;
        private final URI uri;

        static JarFile newJarFile(Path path) {
            try {
                return new JarFile(new File(path.toString()),
                                   true,                       // verify
                                   ZipFile.OPEN_READ,
                                   JarFile.runtimeVersion());
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }

        JarModuleReader(Path path, URI uri) {
            this.jf = newJarFile(path);
            this.uri = uri;
        }

        private JarEntry getEntry(String name) {
            return jf.getJarEntry(Objects.requireNonNull(name));
        }

        @Override
        Optional<URI> implFind(String name) throws IOException {
            JarEntry je = getEntry(name);
            if (je != null) {
                if (jf.isMultiRelease())
                    name = je.getRealName();
                if (je.isDirectory() && !name.endsWith("/"))
                    name += "/";
                String encodedPath = ParseUtil.encodePath(name, false);
                String uris = "jar:" + uri + "!/" + encodedPath;
                return Optional.of(URI.create(uris));
            } else {
                return Optional.empty();
            }
        }

        @Override
        Optional<InputStream> implOpen(String name) throws IOException {
            JarEntry je = getEntry(name);
            if (je != null) {
                return Optional.of(jf.getInputStream(je));
            } else {
                return Optional.empty();
            }
        }

        @Override
        Stream<String> implList() throws IOException {
            // take snapshot to avoid async close
            List<String> names = jf.versionedStream()
                    .map(JarEntry::getName)
                    .toList();
            return names.stream();
        }

        @Override
        void implClose() throws IOException {
            jf.close();
        }
    }


    /**
     * A ModuleReader for a JMOD file.
     */
    static class JModModuleReader extends SafeCloseModuleReader {
        private final JmodFile jf;
        private final URI uri;

        static JmodFile newJmodFile(Path path) {
            try {
                return new JmodFile(path);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }

        JModModuleReader(Path path, URI uri) {
            this.jf = newJmodFile(path);
            this.uri = uri;
        }

        private JmodFile.Entry getEntry(String name) {
            Objects.requireNonNull(name);
            return jf.getEntry(JmodFile.Section.CLASSES, name);
        }

        @Override
        Optional<URI> implFind(String name) {
            JmodFile.Entry je = getEntry(name);
            if (je != null) {
                if (je.isDirectory() && !name.endsWith("/"))
                    name += "/";
                String encodedPath = ParseUtil.encodePath(name, false);
                String uris = "jmod:" + uri + "!/" + encodedPath;
                return Optional.of(URI.create(uris));
            } else {
                return Optional.empty();
            }
        }

        @Override
        Optional<InputStream> implOpen(String name) throws IOException {
            JmodFile.Entry je = getEntry(name);
            if (je != null) {
                return Optional.of(jf.getInputStream(je));
            } else {
                return Optional.empty();
            }
        }

        @Override
        Stream<String> implList() throws IOException {
            // take snapshot to avoid async close
            List<String> names = jf.stream()
                    .filter(e -> e.section() == JmodFile.Section.CLASSES)
                    .map(JmodFile.Entry::name)
                    .toList();
            return names.stream();
        }

        @Override
        void implClose() throws IOException {
            jf.close();
        }
    }


    /**
     * A ModuleReader for an exploded module.
     */
    static class ExplodedModuleReader implements ModuleReader {
        private final Path dir;
        private volatile boolean closed;

        ExplodedModuleReader(Path dir) {
            this.dir = dir;

            // when running with a security manager then check that the caller
            // has access to the directory.
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                boolean unused = Files.isDirectory(dir);
            }
        }

        /**
         * Throws IOException if the module reader is closed;
         */
        private void ensureOpen() throws IOException {
            if (closed) throw new IOException("ModuleReader is closed");
        }

        @Override
        public Optional<URI> find(String name) throws IOException {
            ensureOpen();
            Path path = Resources.toFilePath(dir, name);
            if (path != null) {
                try {
                    return Optional.of(path.toUri());
                } catch (IOError e) {
                    throw (IOException) e.getCause();
                }
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Optional<InputStream> open(String name) throws IOException {
            ensureOpen();
            Path path = Resources.toFilePath(dir, name);
            if (path != null) {
                return Optional.of(Files.newInputStream(path));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Optional<ByteBuffer> read(String name) throws IOException {
            ensureOpen();
            Path path = Resources.toFilePath(dir, name);
            if (path != null) {
                return Optional.of(ByteBuffer.wrap(Files.readAllBytes(path)));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Stream<String> list() throws IOException {
            ensureOpen();
            return Files.walk(dir, Integer.MAX_VALUE)
                        .map(f -> Resources.toResourceName(dir, f))
                        .filter(s -> s.length() > 0);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

}
