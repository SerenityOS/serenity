/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SecureZipFSProvider extends FileSystemProvider {
    private final ConcurrentHashMap<FileSystem, SecureZipFS> map =
            new ConcurrentHashMap<>();
    private final FileSystemProvider defaultProvider;

    public SecureZipFSProvider(FileSystemProvider provider) {
        defaultProvider = provider;
    }

    @Override
    public String getScheme() {
        return "jar";
    }

    public FileSystem newFileSystem(FileSystem fs) {
        return map.computeIfAbsent(fs, (sfs) ->
                new SecureZipFS(this, fs));
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env)
            throws IOException {
        FileSystem fs = defaultProvider.newFileSystem(uri, env);
        return map.computeIfAbsent(fs, (sfs) ->
                new SecureZipFS(this, fs)
        );
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        return map.get(defaultProvider.getFileSystem(uri));
    }

    @Override
    public Path getPath(URI uri) {
        Path p = defaultProvider.getPath(uri);
        return map.get(defaultProvider.getFileSystem(uri)).wrap(p);
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options)
            throws IOException {
        Path p = toTestPath(path).unwrap();

        // Added permission checks before opening the file
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("customPermission"));
            sm.checkRead(p.toString());
        }
        return defaultProvider.newInputStream(p, options);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs)
            throws IOException {
        Path p = toTestPath(path).unwrap();
        return defaultProvider.newByteChannel(p, options, attrs);
    }

    @Override
    public FileChannel newFileChannel(Path path,
                                      Set<? extends OpenOption> options,
                                      FileAttribute<?>... attrs)
            throws IOException {
        Path p = toTestPath(path).unwrap();
        return defaultProvider.newFileChannel(p, options, attrs);
    }


    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir,
                                                    DirectoryStream.Filter<? super Path> filter) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs)
            throws IOException {
        Path p = toTestPath(dir).unwrap();
        defaultProvider.createDirectory(p, attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        Path p = toTestPath(path).unwrap();
        defaultProvider.delete(p);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options)
            throws IOException {
        Path sp = toTestPath(source).unwrap();
        Path tp = toTestPath(target).unwrap();
        defaultProvider.copy(sp, tp, options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options)
            throws IOException {
        Path sp = toTestPath(source).unwrap();
        Path tp = toTestPath(target).unwrap();
        defaultProvider.move(sp, tp, options);
    }

    @Override
    public boolean isSameFile(Path path, Path path2)
            throws IOException {
        Path p = toTestPath(path).unwrap();
        Path p2 = toTestPath(path2).unwrap();
        return defaultProvider.isSameFile(p, p2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        Path p = toTestPath(path).unwrap();
        return defaultProvider.isHidden(p);
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        Path p = toTestPath(path).unwrap();
        return defaultProvider.getFileStore(p);
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        Path p = toTestPath(path).unwrap();
        defaultProvider.checkAccess(p, modes);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path,
                                                                Class<V> type,
                                                                LinkOption... options) {
        Path p = toTestPath(path).unwrap();
        return defaultProvider.getFileAttributeView(p, type, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path,
                                                            Class<A> type,
                                                            LinkOption... options)
            throws IOException {
        Path p = toTestPath(path).unwrap();
        return defaultProvider.readAttributes(p, type, options);
    }

    @Override
    public Map<String, Object> readAttributes(Path path,
                                              String attributes,
                                              LinkOption... options)
            throws IOException {
        Path p = toTestPath(path).unwrap();
        return defaultProvider.readAttributes(p, attributes, options);
    }

    @Override
    public void setAttribute(Path path, String attribute,
                             Object value, LinkOption... options)
            throws IOException {
        Path p = toTestPath(path).unwrap();
        defaultProvider.setAttribute(p, attribute, options);
    }

    // Checks that the given file is a TestPath
    static TestPath toTestPath(Path obj) {
        if (obj == null)
            throw new NullPointerException();
        if (!(obj instanceof TestPath))
            throw new ProviderMismatchException();
        return (TestPath) obj;
    }

    static class SecureZipFS extends FileSystem {
        private final SecureZipFSProvider provider;
        private final FileSystem delegate;

        public SecureZipFS(SecureZipFSProvider provider, FileSystem delegate) {
            this.provider = provider;
            this.delegate = delegate;
        }

        Path wrap(Path path) {
            return (path != null) ? new TestPath(this, path) : null;
        }

        Path unwrap(Path wrapper) {
            if (wrapper == null)
                throw new NullPointerException();
            if (!(wrapper instanceof TestPath))
                throw new ProviderMismatchException();
            return ((TestPath) wrapper).unwrap();
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
            return delegate.getRootDirectories();
        }

        @Override
        public Iterable<FileStore> getFileStores() {
            return delegate.getFileStores();
        }

        @Override
        public Set<String> supportedFileAttributeViews() {
            return delegate.supportedFileAttributeViews();
        }

        @Override
        public Path getPath(String first, String... more) {
            return wrap(delegate.getPath(first, more));
        }

        @Override
        public PathMatcher getPathMatcher(String syntaxAndPattern) {
            return delegate.getPathMatcher(syntaxAndPattern);
        }

        @Override
        public UserPrincipalLookupService getUserPrincipalLookupService() {
            return delegate.getUserPrincipalLookupService();
        }

        @Override
        public WatchService newWatchService() throws IOException {
            return delegate.newWatchService();
        }
    }

    static class TestPath implements Path {
        private final SecureZipFS fs;
        private final Path delegate;

        TestPath(SecureZipFS fs, Path delegate) {
            this.fs = fs;
            this.delegate = delegate;
        }

        Path unwrap() {
            return delegate;
        }

        @Override
        public SecureZipFS getFileSystem() {
            return fs;
        }

        @Override
        public boolean isAbsolute() {
            return delegate.isAbsolute();
        }

        @Override
        public Path getRoot() {
            return fs.wrap(delegate.getRoot());
        }

        @Override
        public Path getFileName() {
            return fs.wrap(delegate.getFileName());
        }

        @Override
        public Path getParent() {
            return fs.wrap(delegate.getParent());
        }

        @Override
        public int getNameCount() {
            return delegate.getNameCount();
        }

        @Override
        public Path getName(int index) {
            return fs.wrap(delegate.getName(index));
        }

        @Override
        public Path subpath(int beginIndex, int endIndex) {
            return fs.wrap(delegate.subpath(beginIndex, endIndex));
        }

        @Override
        public boolean startsWith(Path other) {
            return delegate.startsWith(other);
        }

        @Override
        public boolean endsWith(Path other) {
            return delegate.endsWith(other);
        }

        @Override
        public Path normalize() {
            return fs.wrap(delegate.normalize());
        }

        @Override
        public Path resolve(Path other) {
            return fs.wrap(delegate.resolve(fs.wrap(other)));
        }

        @Override
        public Path relativize(Path other) {
            return fs.wrap(delegate.relativize(fs.wrap(other)));
        }

        @Override
        public URI toUri() {
            String ssp = delegate.toUri().getSchemeSpecificPart();
            return URI.create(fs.provider().getScheme() + ":" + ssp);
        }

        @Override
        public Path toAbsolutePath() {
            return fs.wrap(delegate.toAbsolutePath());
        }

        @Override
        public Path toRealPath(LinkOption... options) throws IOException {
            return fs.wrap(delegate.toRealPath(options));
        }

        @Override
        public WatchKey register(WatchService watcher,
                                 WatchEvent.Kind<?>[] events,
                                 WatchEvent.Modifier... modifiers)
                throws IOException {
            return delegate.register(watcher, events, modifiers);
        }

        @Override
        public Iterator<Path> iterator() {
            final Iterator<Path> itr = delegate.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return itr.hasNext();
                }

                @Override
                public Path next() {
                    return fs.wrap(itr.next());
                }

                @Override
                public void remove() {
                    itr.remove();
                }
            };
        }

        @Override
        public int compareTo(Path other) {
            return delegate.compareTo(fs.unwrap(other));
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TestPath && delegate.equals(fs.unwrap((TestPath) other));
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
