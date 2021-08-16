/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.nio.channels.SeekableByteChannel;
import java.net.URI;
import java.util.*;
import java.io.*;

/**
 * A "pass through" file system implementation that passes through, or delegates,
 * everything to the default file system.
 */

class PassThroughFileSystem extends FileSystem {
    private final FileSystemProvider provider;
    private final FileSystem delegate;

    PassThroughFileSystem(FileSystemProvider provider, FileSystem delegate) {
        this.provider = provider;
        this.delegate = delegate;
    }

    /**
     * Creates a new "pass through" file system. Useful for test environments
     * where the provider might not be deployed.
     */
    static FileSystem create() throws IOException {
        FileSystemProvider provider = new PassThroughProvider();
        Map<String,?> env = Collections.emptyMap();
        URI uri = URI.create("pass:///");
        return provider.newFileSystem(uri, env);
    }

    static Path unwrap(Path wrapper) {
        if (wrapper == null)
            throw new NullPointerException();
        if (!(wrapper instanceof PassThroughPath))
            throw new ProviderMismatchException();
        return ((PassThroughPath)wrapper).delegate;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public String getSeparator() {
        return delegate.getSeparator();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        final Iterable<Path> roots = delegate.getRootDirectories();
        return new Iterable<Path>() {
            @Override
            public Iterator<Path> iterator() {
                final Iterator<Path> itr = roots.iterator();
                return new Iterator<Path>() {
                    @Override
                    public boolean hasNext() {
                        return itr.hasNext();
                    }
                    @Override
                    public Path next() {
                        return new PassThroughPath(delegate, itr.next());
                    }
                    @Override
                    public void remove() {
                        itr.remove();
                    }
                };
            }
        };
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        // assume that unwrapped objects aren't exposed
        return delegate.getFileStores();
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        // assume that unwrapped objects aren't exposed
        return delegate.supportedFileAttributeViews();
    }

    @Override
    public Path getPath(String first, String... more) {
        return new PassThroughPath(this, delegate.getPath(first, more));
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        final PathMatcher matcher = delegate.getPathMatcher(syntaxAndPattern);
        return new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                return matcher.matches(unwrap(path));
            }
        };
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        // assume that unwrapped objects aren't exposed
        return delegate.getUserPrincipalLookupService();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        // to keep it simple
        throw new UnsupportedOperationException();
    }

    static class PassThroughProvider extends FileSystemProvider {
        private static final String SCHEME = "pass";
        private static volatile PassThroughFileSystem delegate;

        public PassThroughProvider() { }

        @Override
        public String getScheme() {
            return SCHEME;
        }

        private void checkScheme(URI uri) {
            if (!uri.getScheme().equalsIgnoreCase(SCHEME))
                throw new IllegalArgumentException();
        }

        private void checkUri(URI uri) {
            checkScheme(uri);
            if (!uri.getSchemeSpecificPart().equals("///"))
                throw new IllegalArgumentException();
        }

        @Override
        public FileSystem newFileSystem(URI uri, Map<String,?> env)
            throws IOException
        {
            checkUri(uri);
            synchronized (PassThroughProvider.class) {
                if (delegate != null)
                    throw new FileSystemAlreadyExistsException();
                PassThroughFileSystem result =
                    new PassThroughFileSystem(this, FileSystems.getDefault());
                delegate = result;
                return result;
            }
        }

        @Override
        public FileSystem getFileSystem(URI uri) {
            checkUri(uri);
            FileSystem result = delegate;
            if (result == null)
                throw new FileSystemNotFoundException();
            return result;
        }

        @Override
        public Path getPath(URI uri) {
            checkScheme(uri);
            if (delegate == null)
                throw new FileSystemNotFoundException();
            uri = URI.create(delegate.provider().getScheme() + ":" +
                             uri.getSchemeSpecificPart());
            return new PassThroughPath(delegate, delegate.provider().getPath(uri));
        }

        @Override
        public void setAttribute(Path file, String attribute, Object value, LinkOption... options)
            throws IOException
        {
            Files.setAttribute(unwrap(file), attribute, value, options);
        }

        @Override
        public Map<String,Object> readAttributes(Path file, String attributes, LinkOption... options)
            throws IOException
        {
            return Files.readAttributes(unwrap(file), attributes, options);
        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(Path file,
                                                                    Class<V> type,
                                                                    LinkOption... options)
        {
            return Files.getFileAttributeView(unwrap(file), type, options);
        }

        @Override
        public <A extends BasicFileAttributes> A readAttributes(Path file,
                                                                Class<A> type,
                                                                LinkOption... options)
            throws IOException
        {
            return Files.readAttributes(unwrap(file), type, options);
        }

        @Override
        public void delete(Path file) throws IOException {
            Files.delete(unwrap(file));
        }

        @Override
        public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs)
            throws IOException
        {
            Files.createSymbolicLink(unwrap(link), unwrap(target), attrs);
        }

        @Override
        public void createLink(Path link, Path existing) throws IOException {
            Files.createLink(unwrap(link), unwrap(existing));
        }

        @Override
        public Path readSymbolicLink(Path link) throws IOException {
            Path target = Files.readSymbolicLink(unwrap(link));
            return new PassThroughPath(delegate, target);
        }


        @Override
        public void copy(Path source, Path target, CopyOption... options) throws IOException {
            Files.copy(unwrap(source), unwrap(target), options);
        }

        @Override
        public void move(Path source, Path target, CopyOption... options) throws IOException {
            Files.move(unwrap(source), unwrap(target), options);
        }

        private DirectoryStream<Path> wrap(final DirectoryStream<Path> stream) {
            return new DirectoryStream<Path>() {
                @Override
                public Iterator<Path> iterator() {
                    final Iterator<Path> itr = stream.iterator();
                    return new Iterator<Path>() {
                        @Override
                        public boolean hasNext() {
                            return itr.hasNext();
                        }
                        @Override
                        public Path next() {
                            return new PassThroughPath(delegate, itr.next());
                        }
                        @Override
                        public void remove() {
                            itr.remove();
                        }
                    };
                }
                @Override
                public void close() throws IOException {
                    stream.close();
                }
            };
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter)
            throws IOException
        {
            return wrap(Files.newDirectoryStream(unwrap(dir), filter));
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs)
            throws IOException
        {
            Files.createDirectory(unwrap(dir), attrs);
        }

        @Override
        public SeekableByteChannel newByteChannel(Path file,
                                                  Set<? extends OpenOption> options,
                                                  FileAttribute<?>... attrs)
            throws IOException
        {
            return Files.newByteChannel(unwrap(file), options, attrs);
        }


        @Override
        public boolean isHidden(Path file) throws IOException {
            return Files.isHidden(unwrap(file));
        }

        @Override
        public FileStore getFileStore(Path file) throws IOException {
            return Files.getFileStore(unwrap(file));
        }

        @Override
        public boolean isSameFile(Path file, Path other) throws IOException {
            return Files.isSameFile(unwrap(file), unwrap(other));
        }

        @Override
        public void checkAccess(Path file, AccessMode... modes)
            throws IOException
        {
            // hack
            if (modes.length == 0) {
                if (Files.exists(unwrap(file)))
                    return;
                else
                    throw new NoSuchFileException(file.toString());
            }
            throw new RuntimeException("not implemented yet");
        }
    }

    static class PassThroughPath implements Path {
        private final FileSystem fs;
        private final Path delegate;

        PassThroughPath(FileSystem fs, Path delegate) {
            this.fs = fs;
            this.delegate = delegate;
        }

        private Path wrap(Path path) {
            return (path != null) ? new PassThroughPath(fs, path) : null;
        }

        @Override
        public FileSystem getFileSystem() {
            return fs;
        }

        @Override
        public boolean isAbsolute() {
            return delegate.isAbsolute();
        }

        @Override
        public Path getRoot() {
            return wrap(delegate.getRoot());
        }

        @Override
        public Path getParent() {
            return wrap(delegate.getParent());
        }

        @Override
        public int getNameCount() {
            return delegate.getNameCount();
        }

        @Override
        public Path getFileName() {
            return wrap(delegate.getFileName());
        }

        @Override
        public Path getName(int index) {
            return wrap(delegate.getName(index));
        }

        @Override
        public Path subpath(int beginIndex, int endIndex) {
            return wrap(delegate.subpath(beginIndex, endIndex));
        }

        @Override
        public boolean startsWith(Path other) {
            return delegate.startsWith(unwrap(other));
        }

        @Override
        public boolean startsWith(String other) {
            return delegate.startsWith(other);
        }

        @Override
        public boolean endsWith(Path other) {
            return delegate.endsWith(unwrap(other));
        }

        @Override
        public boolean endsWith(String other) {
            return delegate.endsWith(other);
        }

        @Override
        public Path normalize() {
            return wrap(delegate.normalize());
        }

        @Override
        public Path resolve(Path other) {
            return wrap(delegate.resolve(unwrap(other)));
        }

        @Override
        public Path resolve(String other) {
            return wrap(delegate.resolve(other));
        }

        @Override
        public Path resolveSibling(Path other) {
            return wrap(delegate.resolveSibling(unwrap(other)));
        }

        @Override
        public Path resolveSibling(String other) {
            return wrap(delegate.resolveSibling(other));
        }

        @Override
        public Path relativize(Path other) {
            return wrap(delegate.relativize(unwrap(other)));
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof PassThroughPath))
                return false;
            return delegate.equals(unwrap((PassThroughPath)other));
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public URI toUri() {
            String ssp = delegate.toUri().getSchemeSpecificPart();
            return URI.create(fs.provider().getScheme() + ":" + ssp);
        }

        @Override
        public Path toAbsolutePath() {
            return wrap(delegate.toAbsolutePath());
        }

        @Override
        public Path toRealPath(LinkOption... options) throws IOException {
            return wrap(delegate.toRealPath(options));
        }

        @Override
        public File toFile() {
            return delegate.toFile();
        }

        @Override
        public Iterator<Path> iterator() {
            final Iterator<Path> itr = delegate.iterator();
            return new Iterator<Path>() {
                @Override
                public boolean hasNext() {
                    return itr.hasNext();
                }
                @Override
                public Path next() {
                    return wrap(itr.next());
                }
                @Override
                public void remove() {
                    itr.remove();
                }
            };
        }

        @Override
        public int compareTo(Path other) {
            return delegate.compareTo(unwrap(other));
        }

        @Override
        public WatchKey register(WatchService watcher,
                                      WatchEvent.Kind<?>[] events,
                                      WatchEvent.Modifier... modifiers)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public  WatchKey register(WatchService watcher,
                                      WatchEvent.Kind<?>... events)
        {
            throw new UnsupportedOperationException();
        }
    }
}
