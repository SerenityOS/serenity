/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A {@code FileSystem} that helps testing by trigger exception throwing based on filenames.
 */
class FaultyFileSystem extends FileSystem {
    final Path root;
    final boolean removeRootAfterClose;
    final FileSystem delegate;
    boolean isOpen;

    FaultyFileSystem(Path root) throws IOException {
        if (root == null) {
            root = Files.createTempDirectory("faultyFS");
            removeRootAfterClose = true;
        } else {
            if (! Files.isDirectory(root)) {
                throw new IllegalArgumentException("must be a directory.");
            }
            removeRootAfterClose = false;
        }
        this.root = root;
        delegate = root.getFileSystem();
        isOpen = true;
    }

    private static Path unwrap(Path p) {
        return PassThroughFileSystem.unwrap(p);
    }

    Path getRoot() {
        return new PassThroughFileSystem.PassThroughPath(this, root);
    }

    @Override
    public void close() throws IOException {
        if (isOpen) {
            if (removeRootAfterClose) {
                TestUtil.removeAll(root);
            }
            isOpen = false;
        }
    }

    @Override
    public FileSystemProvider provider() {
        return FaultyFSProvider.getInstance();
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public String getSeparator() {
        return delegate.getSeparator();
    }

    private <T> Iterable<T> SoleIterable(final T element) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private T soleElement = element;

                    @Override
                    public boolean hasNext() {
                        return soleElement != null;
                    }

                    @Override
                    public T next() {
                        try {
                            return soleElement;
                        } finally {
                            soleElement = null;
                        }
                    }
                };
            }
        };
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return SoleIterable(getRoot());
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        FileStore store;
        try {
            store = Files.getFileStore(root);
        } catch (IOException ioe) {
            store = null;
        }
        return SoleIterable(store);
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        // assume that unwrapped objects aren't exposed
        return delegate.supportedFileAttributeViews();
    }

    @Override
    public Path getPath(String first, String... more) {
        return new PassThroughFileSystem.PassThroughPath(this, delegate.getPath(first, more));
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

    static class FaultyException extends IOException {
        FaultyException() {
            super("fault triggered.");
        }
    }

    static class FaultyFSProvider extends FileSystemProvider {
        private static final String SCHEME = "faulty";
        private static volatile FaultyFileSystem delegate;
        private static FaultyFSProvider INSTANCE = new FaultyFSProvider();
        private boolean enabled;

        private FaultyFSProvider() {}

        public static FaultyFSProvider getInstance() {
            return INSTANCE;
        }

        public void setFaultyMode(boolean enable) {
            enabled = enable;
        }

        private void triggerEx(String filename, String... names) throws IOException {
            if (! enabled) {
                return;
            }

            if (filename.equals("SecurityException")) {
                throw new SecurityException("FaultyFS", new FaultyException());
            }

            if (filename.equals("IOException")) {
                throw new FaultyException();
            }

            for (String name: names) {
                if (name.equals(filename)) {
                    throw new FaultyException();
                }
            }
        }

        private void triggerEx(Path path, String... names) throws IOException {
            triggerEx(path.getFileName().toString(), names);
        }

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
        public FileSystem newFileSystem(Path fakeRoot, Map<String,?> env)
            throws IOException
        {
            if (env != null && env.keySet().contains("IOException")) {
                triggerEx("IOException");
            }

            synchronized (FaultyFSProvider.class) {
                if (delegate != null && delegate.isOpen())
                    throw new FileSystemAlreadyExistsException();
                FaultyFileSystem result = new FaultyFileSystem(fakeRoot);
                delegate = result;
                return result;
            }
        }

        @Override
        public FileSystem newFileSystem(URI uri, Map<String,?> env)
            throws IOException
        {
            if (env != null && env.keySet().contains("IOException")) {
                triggerEx("IOException");
            }

            checkUri(uri);
            synchronized (FaultyFSProvider.class) {
                if (delegate != null && delegate.isOpen())
                    throw new FileSystemAlreadyExistsException();
                FaultyFileSystem result = new FaultyFileSystem(null);
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

            // only allow absolute path
            String path = uri.getSchemeSpecificPart();
            if (! path.startsWith("///")) {
                throw new IllegalArgumentException();
            }
            return new PassThroughFileSystem.PassThroughPath(delegate, delegate.root.resolve(path.substring(3)));
        }

        @Override
        public void setAttribute(Path file, String attribute, Object value, LinkOption... options)
            throws IOException
        {
            triggerEx(file, "setAttribute");
            Files.setAttribute(unwrap(file), attribute, value, options);
        }

        @Override
        public Map<String,Object> readAttributes(Path file, String attributes, LinkOption... options)
            throws IOException
        {
            triggerEx(file, "readAttributes");
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
            triggerEx(file, "readAttributes");
            return Files.readAttributes(unwrap(file), type, options);
        }

        @Override
        public void delete(Path file) throws IOException {
            triggerEx(file, "delete");
            Files.delete(unwrap(file));
        }

        @Override
        public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs)
            throws IOException
        {
            triggerEx(target, "createSymbolicLink");
            Files.createSymbolicLink(unwrap(link), unwrap(target), attrs);
        }

        @Override
        public void createLink(Path link, Path existing) throws IOException {
            triggerEx(existing, "createLink");
            Files.createLink(unwrap(link), unwrap(existing));
        }

        @Override
        public Path readSymbolicLink(Path link) throws IOException {
            Path target = Files.readSymbolicLink(unwrap(link));
            triggerEx(target, "readSymbolicLink");
            return new PassThroughFileSystem.PassThroughPath(delegate, target);
        }


        @Override
        public void copy(Path source, Path target, CopyOption... options) throws IOException {
            triggerEx(source, "copy");
            Files.copy(unwrap(source), unwrap(target), options);
        }

        @Override
        public void move(Path source, Path target, CopyOption... options) throws IOException {
            triggerEx(source, "move");
            Files.move(unwrap(source), unwrap(target), options);
        }

        private DirectoryStream<Path> wrap(final DirectoryStream<Path> stream) {
            return new DirectoryStream<Path>() {
                @Override
                public Iterator<Path> iterator() {
                    final Iterator<Path> itr = stream.iterator();
                    return new Iterator<Path>() {
                        private Path next = null;
                        @Override
                        public boolean hasNext() {
                            if (next == null) {
                                if (itr.hasNext()) {
                                    next = itr.next();
                                } else {
                                    return false;
                                }
                            }
                            if (next != null) {
                                try {
                                    triggerEx(next, "DirectoryIteratorException");
                                } catch (IOException ioe) {
                                    throw new DirectoryIteratorException(ioe);
                                } catch (SecurityException se) {
                                    // ??? Does DS throw SecurityException during iteration?
                                    next = null;
                                    return hasNext();
                                }
                            }
                            return (next != null);
                        }
                        @Override
                        public Path next() {
                            try {
                                if (next != null || hasNext()) {
                                    return new PassThroughFileSystem.PassThroughPath(delegate, next);
                                } else {
                                    throw new NoSuchElementException();
                                }
                            } finally {
                                next = null;
                            }
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
            triggerEx(dir, "newDirectoryStream");
            return wrap(Files.newDirectoryStream(unwrap(dir), filter));
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs)
            throws IOException
        {
            triggerEx(dir, "createDirectory");
            Files.createDirectory(unwrap(dir), attrs);
        }

        @Override
        public SeekableByteChannel newByteChannel(Path file,
                                                  Set<? extends OpenOption> options,
                                                  FileAttribute<?>... attrs)
            throws IOException
        {
            triggerEx(file, "newByteChannel");
            return Files.newByteChannel(unwrap(file), options, attrs);
        }


        @Override
        public boolean isHidden(Path file) throws IOException {
            triggerEx(file, "isHidden");
            return Files.isHidden(unwrap(file));
        }

        @Override
        public FileStore getFileStore(Path file) throws IOException {
            triggerEx(file, "getFileStore");
            return Files.getFileStore(unwrap(file));
        }

        @Override
        public boolean isSameFile(Path file, Path other) throws IOException {
            triggerEx(file, "isSameFile");
            return Files.isSameFile(unwrap(file), unwrap(other));
        }

        @Override
        public void checkAccess(Path file, AccessMode... modes)
            throws IOException
        {
            triggerEx(file, "checkAccess");
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
}
