/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.nio.channels.SeekableByteChannel;
import java.net.URI;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TestProvider extends FileSystemProvider {

    private final FileSystemProvider defaultProvider;
    private final TestFileSystem theFileSystem;

    public TestProvider(FileSystemProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
        FileSystem fs = defaultProvider.getFileSystem(URI.create("file:/"));
        this.theFileSystem = new TestFileSystem(fs, this);
    }

    FileSystemProvider defaultProvider() {
        return defaultProvider;
    }

    @Override
    public String getScheme() {
        return "file";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String,?> env) throws IOException {
        return defaultProvider.newFileSystem(uri, env);
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        return theFileSystem;
    }

    @Override
    public Path getPath(URI uri) {
        Path path = defaultProvider.getPath(uri);
        return theFileSystem.wrap(path);
    }

    @Override
    public void setAttribute(Path file, String attribute, Object value,
                             LinkOption... options)
        throws IOException
    {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Map<String,Object> readAttributes(Path file, String attributes,
                                             LinkOption... options)
        throws IOException
    {
        Path delegate = theFileSystem.unwrap(file);
        return defaultProvider.readAttributes(delegate, attributes, options);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path file,
                                                            Class<A> type,
                                                            LinkOption... options)
        throws IOException
    {
        Path delegate = theFileSystem.unwrap(file);
        return defaultProvider.readAttributes(delegate, type, options);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path file,
                                                                Class<V> type,
                                                                LinkOption... options)
    {
        Path delegate = theFileSystem.unwrap(file);
        return defaultProvider.getFileAttributeView(delegate, type, options);
    }

    @Override
    public void delete(Path file) throws IOException {
        Path delegate = theFileSystem.unwrap(file);
        defaultProvider.delete(delegate);
    }

    @Override
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs)
        throws IOException
    {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void createLink(Path link, Path existing) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        Path delegate = theFileSystem.unwrap(link);
        Path target = defaultProvider.readSymbolicLink(delegate);
        return theFileSystem.wrap(target);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options)
        throws IOException
    {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void move(Path source, Path target, CopyOption... options)
        throws IOException
    {
        throw new RuntimeException("not implemented");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir,
                                                    DirectoryStream.Filter<? super Path> filter)
        throws IOException
    {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs)
        throws IOException
    {
        Path delegate = theFileSystem.unwrap(dir);
        defaultProvider.createDirectory(delegate, attrs);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path file,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs)
        throws IOException
    {
        Path delegate = theFileSystem.unwrap(file);
        return defaultProvider.newByteChannel(delegate, options, attrs);
    }

    @Override
    public boolean isHidden(Path file) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public FileStore getFileStore(Path file) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean isSameFile(Path file, Path other) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void checkAccess(Path file, AccessMode... modes)
        throws IOException
    {
        throw new RuntimeException("not implemented");
    }

    static class TestFileSystem extends FileSystem {
        private final FileSystem delegate;
        private final TestProvider provider;

        TestFileSystem(FileSystem delegate, TestProvider provider) {
            this.delegate = delegate;
            this.provider = provider;
        }

        Path wrap(Path path) {
            return (path != null) ? new TestPath(this, path) : null;
        }

        Path unwrap(Path wrapper) {
            if (wrapper == null)
                throw new NullPointerException();
            if (!(wrapper instanceof TestPath))
                throw new ProviderMismatchException();
            return ((TestPath)wrapper).unwrap();
        }

        @Override
        public FileSystemProvider provider() {
            return provider;
        }

        @Override
        public void close() throws IOException {
            throw new RuntimeException("not implemented");
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public String getSeparator() {
            return delegate.getSeparator();
        }

        @Override
        public Iterable<Path> getRootDirectories() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public Iterable<FileStore> getFileStores() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public Set<String> supportedFileAttributeViews() {
            return delegate.supportedFileAttributeViews();
        }

        @Override
        public Path getPath(String first, String... more) {
            Path path = delegate.getPath(first, more);
            return wrap(path);
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
            throw new UnsupportedOperationException();
        }
    }

    static class TestPath implements Path {
        private final TestFileSystem fs;
        private final Path delegate;

        TestPath(TestFileSystem fs, Path delegate) {
            this.fs = fs;
            this.delegate = delegate;
        }

        Path unwrap() {
            return delegate;
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
            return fs.wrap(delegate.getRoot());
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
        public Path getFileName() {
            return fs.wrap(delegate.getFileName());
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
            return delegate.startsWith(fs.unwrap(other));
        }

        @Override
        public boolean startsWith(String other) {
            return delegate.startsWith(other);
        }

        @Override
        public boolean endsWith(Path other) {
            return delegate.endsWith(fs.unwrap(other));
        }

        @Override
        public boolean endsWith(String other) {
            return delegate.endsWith(other);
        }

        @Override
        public Path normalize() {
            return fs.wrap(delegate.normalize());
        }

        @Override
        public Path resolve(Path other) {
            return fs.wrap(delegate.resolve(fs.unwrap(other)));
        }

        @Override
        public Path resolve(String other) {
            return fs.wrap(delegate.resolve(other));
        }

        @Override
        public Path resolveSibling(Path other) {
            return fs.wrap(delegate.resolveSibling(fs.unwrap(other)));
        }

        @Override
        public Path resolveSibling(String other) {
            return fs.wrap(delegate.resolveSibling(other));
        }

        @Override
        public Path relativize(Path other) {
            return fs.wrap(delegate.relativize(fs.unwrap(other)));
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof TestPath))
                return false;
            return delegate.equals(fs.unwrap((TestPath) other));
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
            return fs.wrap(delegate.toAbsolutePath());
        }

        @Override
        public Path toRealPath(LinkOption... options) throws IOException {
            return fs.wrap(delegate.toRealPath(options));
        }

        @Override
        public File toFile() {
            return new File(toString());
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
