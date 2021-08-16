/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.jrtfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import jdk.internal.jimage.ImageReader.Node;
import static java.util.stream.Collectors.toList;

/**
 * jrt file system implementation built on System jimage files.
 *
 * @implNote This class needs to maintain JDK 8 source compatibility.
 *
 * It is used internally in the JDK to implement jimage/jrtfs access,
 * but also compiled and delivered as part of the jrtfs.jar to support access
 * to the jimage file provided by the shipped JDK by tools running on JDK 8.
 */
class JrtFileSystem extends FileSystem {

    private final JrtFileSystemProvider provider;
    private final JrtPath rootPath = new JrtPath(this, "/");
    private volatile boolean isOpen;
    private volatile boolean isClosable;
    private SystemImage image;

    JrtFileSystem(JrtFileSystemProvider provider, Map<String, ?> env)
            throws IOException
    {
        this.provider = provider;
        this.image = SystemImage.open();  // open image file
        this.isOpen = true;
        this.isClosable = env != null;
    }

    // FileSystem method implementations
    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void close() throws IOException {
        if (!isClosable)
            throw new UnsupportedOperationException();
        cleanup();
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton(getRootPath());
    }

    @Override
    public JrtPath getPath(String first, String... more) {
        if (more.length == 0) {
            return new JrtPath(this, first);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(first);
        for (String path : more) {
            if (!path.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(path);
            }
        }
        return new JrtPath(this, sb.toString());
    }

    @Override
    public final boolean isReadOnly() {
        return true;
    }

    @Override
    public final UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final WatchService newWatchService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Iterable<FileStore> getFileStores() {
        return Collections.singleton(getFileStore(getRootPath()));
    }

    private static final Set<String> supportedFileAttributeViews
            = Collections.unmodifiableSet(
                    new HashSet<String>(Arrays.asList("basic", "jrt")));

    @Override
    public final Set<String> supportedFileAttributeViews() {
        return supportedFileAttributeViews;
    }

    @Override
    public final String toString() {
        return "jrt:/";
    }

    @Override
    public final String getSeparator() {
        return "/";
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndInput) {
        int pos = syntaxAndInput.indexOf(':');
        if (pos <= 0 || pos == syntaxAndInput.length()) {
            throw new IllegalArgumentException("pos is " + pos);
        }
        String syntax = syntaxAndInput.substring(0, pos);
        String input = syntaxAndInput.substring(pos + 1);
        String expr;
        if (syntax.equalsIgnoreCase("glob")) {
            expr = JrtUtils.toRegexPattern(input);
        } else if (syntax.equalsIgnoreCase("regex")) {
            expr = input;
        } else {
                throw new UnsupportedOperationException("Syntax '" + syntax
                        + "' not recognized");
        }
        // return matcher
        final Pattern pattern = Pattern.compile(expr);
        return (Path path) -> pattern.matcher(path.toString()).matches();
    }

    JrtPath resolveLink(JrtPath path) throws IOException {
        Node node = checkNode(path);
        if (node.isLink()) {
            node = node.resolveLink();
            return new JrtPath(this, node.getName());  // TBD, normalized?
        }
        return path;
    }

    JrtFileAttributes getFileAttributes(JrtPath path, LinkOption... options)
            throws IOException {
        Node node = checkNode(path);
        if (node.isLink() && followLinks(options)) {
            return new JrtFileAttributes(node.resolveLink(true));
        }
        return new JrtFileAttributes(node);
    }

    /**
     * returns the list of child paths of the given directory "path"
     *
     * @param path name of the directory whose content is listed
     * @return iterator for child paths of the given directory path
     */
    Iterator<Path> iteratorOf(JrtPath path, DirectoryStream.Filter<? super Path> filter)
            throws IOException {
        Node node = checkNode(path).resolveLink(true);
        if (!node.isDirectory()) {
            throw new NotDirectoryException(path.getName());
        }
        if (filter == null) {
            return node.getChildren()
                       .stream()
                       .map(child -> (Path)(path.resolve(new JrtPath(this, child.getNameString()).getFileName())))
                       .iterator();
        }
        return node.getChildren()
                   .stream()
                   .map(child -> (Path)(path.resolve(new JrtPath(this, child.getNameString()).getFileName())))
                   .filter(p ->  { try { return filter.accept(p);
                                   } catch (IOException x) {}
                                   return false;
                                  })
                   .iterator();
    }

    // returns the content of the file resource specified by the path
    byte[] getFileContent(JrtPath path) throws IOException {
        Node node = checkNode(path);
        if (node.isDirectory()) {
            throw new FileSystemException(path + " is a directory");
        }
        //assert node.isResource() : "resource node expected here";
        return image.getResource(node);
    }

    /////////////// Implementation details below this point //////////

    // static utility methods
    static ReadOnlyFileSystemException readOnly() {
        return new ReadOnlyFileSystemException();
    }

    // do the supplied options imply that we have to chase symlinks?
    static boolean followLinks(LinkOption... options) {
        if (options != null) {
            for (LinkOption lo : options) {
                Objects.requireNonNull(lo);
                if (lo == LinkOption.NOFOLLOW_LINKS) {
                    return false;
                } else {
                    throw new AssertionError("should not reach here");
                }
            }
        }
        return true;
    }

    // check that the options passed are supported by (read-only) jrt file system
    static void checkOptions(Set<? extends OpenOption> options) {
        // check for options of null type and option is an intance of StandardOpenOption
        for (OpenOption option : options) {
            Objects.requireNonNull(option);
            if (!(option instanceof StandardOpenOption)) {
                throw new IllegalArgumentException(
                    "option class: " + option.getClass());
            }
        }
        if (options.contains(StandardOpenOption.WRITE) ||
            options.contains(StandardOpenOption.APPEND)) {
            throw readOnly();
        }
    }

    // clean up this file system - called from finalize and close
    synchronized void cleanup() throws IOException {
        if (isOpen) {
            isOpen = false;
            image.close();
            image = null;
        }
    }

    // These methods throw read only file system exception
    final void setTimes(JrtPath jrtPath, FileTime mtime, FileTime atime, FileTime ctime)
            throws IOException {
        throw readOnly();
    }

    // These methods throw read only file system exception
    final void createDirectory(JrtPath jrtPath, FileAttribute<?>... attrs) throws IOException {
        throw readOnly();
    }

    final void deleteFile(JrtPath jrtPath, boolean failIfNotExists)
            throws IOException {
        throw readOnly();
    }

    final OutputStream newOutputStream(JrtPath jrtPath, OpenOption... options)
            throws IOException {
        throw readOnly();
    }

    final void copyFile(boolean deletesrc, JrtPath srcPath, JrtPath dstPath, CopyOption... options)
            throws IOException {
        throw readOnly();
    }

    final FileChannel newFileChannel(JrtPath path,
            Set<? extends OpenOption> options,
            FileAttribute<?>... attrs)
            throws IOException {
        throw new UnsupportedOperationException("newFileChannel");
    }

    final InputStream newInputStream(JrtPath path) throws IOException {
        return new ByteArrayInputStream(getFileContent(path));
    }

    final SeekableByteChannel newByteChannel(JrtPath path,
            Set<? extends OpenOption> options,
            FileAttribute<?>... attrs)
            throws IOException {
        checkOptions(options);

        byte[] buf = getFileContent(path);
        final ReadableByteChannel rbc
                = Channels.newChannel(new ByteArrayInputStream(buf));
        final long size = buf.length;
        return new SeekableByteChannel() {
            long read = 0;

            @Override
            public boolean isOpen() {
                return rbc.isOpen();
            }

            @Override
            public long position() throws IOException {
                return read;
            }

            @Override
            public SeekableByteChannel position(long pos)
                    throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read(ByteBuffer dst) throws IOException {
                int n = rbc.read(dst);
                if (n > 0) {
                    read += n;
                }
                return n;
            }

            @Override
            public SeekableByteChannel truncate(long size)
                    throws IOException {
                throw new NonWritableChannelException();
            }

            @Override
            public int write(ByteBuffer src) throws IOException {
                throw new NonWritableChannelException();
            }

            @Override
            public long size() throws IOException {
                return size;
            }

            @Override
            public void close() throws IOException {
                rbc.close();
            }
        };
    }

    final JrtFileStore getFileStore(JrtPath path) {
        return new JrtFileStore(path);
    }

    final void ensureOpen() throws IOException {
        if (!isOpen()) {
            throw new ClosedFileSystemException();
        }
    }

    final JrtPath getRootPath() {
        return rootPath;
    }

    boolean isSameFile(JrtPath path1, JrtPath path2) throws IOException {
        return checkNode(path1) == checkNode(path2);
    }

    boolean isLink(JrtPath path) throws IOException {
        return checkNode(path).isLink();
    }

    boolean exists(JrtPath path) throws IOException {
        try {
            checkNode(path);
        } catch (NoSuchFileException exp) {
            return false;
        }
        return true;
    }

    boolean isDirectory(JrtPath path, boolean resolveLinks)
            throws IOException {
        Node node = checkNode(path);
        return resolveLinks && node.isLink()
                ? node.resolveLink(true).isDirectory()
                : node.isDirectory();
    }

    JrtPath toRealPath(JrtPath path, LinkOption... options)
            throws IOException {
        Node node = checkNode(path);
        if (followLinks(options) && node.isLink()) {
            node = node.resolveLink();
        }
        // image node holds the real/absolute path name
        return new JrtPath(this, node.getName(), true);
    }

    private Node lookup(String path) {
        try {
            return image.findNode(path);
        } catch (RuntimeException | IOException ex) {
            throw new InvalidPathException(path, ex.toString());
        }
    }

    private Node lookupSymbolic(String path) {
        int i = 1;
        while (i < path.length()) {
            i = path.indexOf('/', i);
            if (i == -1) {
                break;
            }
            String prefix = path.substring(0, i);
            Node node = lookup(prefix);
            if (node == null) {
                break;
            }
            if (node.isLink()) {
                Node link = node.resolveLink(true);
                // resolved symbolic path concatenated to the rest of the path
                String resPath = link.getName() + path.substring(i);
                node = lookup(resPath);
                return node != null ? node : lookupSymbolic(resPath);
            }
            i++;
        }
        return null;
    }

    Node checkNode(JrtPath path) throws IOException {
        ensureOpen();
        String p = path.getResolvedPath();
        Node node = lookup(p);
        if (node == null) {
            node = lookupSymbolic(p);
            if (node == null) {
                throw new NoSuchFileException(p);
            }
        }
        return node;
    }
}
