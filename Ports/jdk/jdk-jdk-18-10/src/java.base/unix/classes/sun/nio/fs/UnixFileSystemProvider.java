/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileTypeDetector;
import java.nio.channels.*;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.io.FilePermission;
import java.util.*;

import jdk.internal.util.StaticProperty;
import sun.nio.ch.ThreadPool;
import sun.security.util.SecurityConstants;
import static sun.nio.fs.UnixNativeDispatcher.*;
import static sun.nio.fs.UnixConstants.*;

/**
 * Base implementation of FileSystemProvider
 */

public abstract class UnixFileSystemProvider
    extends AbstractFileSystemProvider
{
    private static final byte[] EMPTY_PATH = new byte[0];
    private final UnixFileSystem theFileSystem;

    public UnixFileSystemProvider() {
        theFileSystem = newFileSystem(StaticProperty.userDir());
    }

    UnixFileSystem theFileSystem() {
        return theFileSystem;
    }

    /**
     * Constructs a new file system using the given default directory.
     */
    abstract UnixFileSystem newFileSystem(String dir);

    @Override
    public final String getScheme() {
        return "file";
    }

    private void checkUri(URI uri) {
        if (!uri.getScheme().equalsIgnoreCase(getScheme()))
            throw new IllegalArgumentException("URI does not match this provider");
        if (uri.getRawAuthority() != null)
            throw new IllegalArgumentException("Authority component present");
        String path = uri.getPath();
        if (path == null)
            throw new IllegalArgumentException("Path component is undefined");
        if (!path.equals("/"))
            throw new IllegalArgumentException("Path component should be '/'");
        if (uri.getRawQuery() != null)
            throw new IllegalArgumentException("Query component present");
        if (uri.getRawFragment() != null)
            throw new IllegalArgumentException("Fragment component present");
    }

    @Override
    public final FileSystem newFileSystem(URI uri, Map<String,?> env) {
        checkUri(uri);
        throw new FileSystemAlreadyExistsException();
    }

    @Override
    public final FileSystem getFileSystem(URI uri) {
        checkUri(uri);
        return theFileSystem;
    }

    @Override
    public Path getPath(URI uri) {
        return UnixUriUtils.fromUri(theFileSystem, uri);
    }

    UnixPath checkPath(Path obj) {
        if (obj == null)
            throw new NullPointerException();
        if (!(obj instanceof UnixPath))
            throw new ProviderMismatchException();
        return (UnixPath)obj;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V getFileAttributeView(Path obj,
                                                                Class<V> type,
                                                                LinkOption... options)
    {
        UnixPath file = UnixPath.toUnixPath(obj);
        boolean followLinks = Util.followLinks(options);
        if (type == BasicFileAttributeView.class)
            return (V) UnixFileAttributeViews.createBasicView(file, followLinks);
        if (type == PosixFileAttributeView.class)
            return (V) UnixFileAttributeViews.createPosixView(file, followLinks);
        if (type == FileOwnerAttributeView.class)
            return (V) UnixFileAttributeViews.createOwnerView(file, followLinks);
        if (type == null)
            throw new NullPointerException();
        return (V) null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends BasicFileAttributes> A readAttributes(Path file,
                                                               Class<A> type,
                                                               LinkOption... options)
        throws IOException
    {
        Class<? extends BasicFileAttributeView> view;
        if (type == BasicFileAttributes.class)
            view = BasicFileAttributeView.class;
        else if (type == PosixFileAttributes.class)
            view = PosixFileAttributeView.class;
        else if (type == null)
            throw new NullPointerException();
        else
            throw new UnsupportedOperationException();
        return (A) getFileAttributeView(file, view, options).readAttributes();
    }

    @Override
    protected DynamicFileAttributeView getFileAttributeView(Path obj,
                                                            String name,
                                                            LinkOption... options)
    {
        UnixPath file = UnixPath.toUnixPath(obj);
        boolean followLinks = Util.followLinks(options);
        if (name.equals("basic"))
            return UnixFileAttributeViews.createBasicView(file, followLinks);
        if (name.equals("posix"))
            return UnixFileAttributeViews.createPosixView(file, followLinks);
        if (name.equals("unix"))
            return UnixFileAttributeViews.createUnixView(file, followLinks);
        if (name.equals("owner"))
            return UnixFileAttributeViews.createOwnerView(file, followLinks);
        return null;
    }

    @Override
    public FileChannel newFileChannel(Path obj,
                                      Set<? extends OpenOption> options,
                                      FileAttribute<?>... attrs)
        throws IOException
    {
        UnixPath file = checkPath(obj);
        int mode = UnixFileModeAttribute
            .toUnixMode(UnixFileModeAttribute.ALL_READWRITE, attrs);
        try {
            return UnixChannelFactory.newFileChannel(file, options, mode);
        } catch (UnixException x) {
            x.rethrowAsIOException(file);
            return null;
        }
    }

    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path obj,
                                                              Set<? extends OpenOption> options,
                                                              ExecutorService executor,
                                                              FileAttribute<?>... attrs) throws IOException
    {
        UnixPath file = checkPath(obj);
        int mode = UnixFileModeAttribute
            .toUnixMode(UnixFileModeAttribute.ALL_READWRITE, attrs);
        ThreadPool pool = (executor == null) ? null : ThreadPool.wrap(executor, 0);
        try {
            return UnixChannelFactory
                .newAsynchronousFileChannel(file, options, mode, pool);
        } catch (UnixException x) {
            x.rethrowAsIOException(file);
            return null;
        }
    }


    @Override
    public SeekableByteChannel newByteChannel(Path obj,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs)
         throws IOException
    {
        UnixPath file = UnixPath.toUnixPath(obj);
        int mode = UnixFileModeAttribute
            .toUnixMode(UnixFileModeAttribute.ALL_READWRITE, attrs);
        try {
            return UnixChannelFactory.newFileChannel(file, options, mode);
        } catch (UnixException x) {
            x.rethrowAsIOException(file);
            return null;  // keep compiler happy
        }
    }

    @Override
    boolean implDelete(Path obj, boolean failIfNotExists) throws IOException {
        UnixPath file = UnixPath.toUnixPath(obj);
        file.checkDelete();

        // need file attributes to know if file is directory
        UnixFileAttributes attrs = null;
        try {
            attrs = UnixFileAttributes.get(file, false);
            if (attrs.isDirectory()) {
                rmdir(file);
            } else {
                unlink(file);
            }
            return true;
        } catch (UnixException x) {
            // no-op if file does not exist
            if (!failIfNotExists && x.errno() == ENOENT)
                return false;

            // DirectoryNotEmptyException if not empty
            if (attrs != null && attrs.isDirectory() &&
                (x.errno() == EEXIST || x.errno() == ENOTEMPTY))
                throw new DirectoryNotEmptyException(file.getPathForExceptionMessage());

            x.rethrowAsIOException(file);
            return false;
        }
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options)
        throws IOException
    {
        UnixCopyFile.copy(UnixPath.toUnixPath(source),
                          UnixPath.toUnixPath(target),
                          options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options)
        throws IOException
    {
        UnixCopyFile.move(UnixPath.toUnixPath(source),
                          UnixPath.toUnixPath(target),
                          options);
    }

    @Override
    public void checkAccess(Path obj, AccessMode... modes) throws IOException {
        UnixPath file = UnixPath.toUnixPath(obj);
        boolean e = false;
        boolean r = false;
        boolean w = false;
        boolean x = false;

        if (modes.length == 0) {
            e = true;
        } else {
            for (AccessMode mode: modes) {
                switch (mode) {
                    case READ : r = true; break;
                    case WRITE : w = true; break;
                    case EXECUTE : x = true; break;
                    default: throw new AssertionError("Should not get here");
                }
            }
        }

        int mode = 0;
        if (e || r) {
            file.checkRead();
            mode |= (r) ? R_OK : F_OK;
        }
        if (w) {
            file.checkWrite();
            mode |= W_OK;
        }
        if (x) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                // not cached
                sm.checkExec(file.getPathForPermissionCheck());
            }
            mode |= X_OK;
        }
        try {
            access(file, mode);
        } catch (UnixException exc) {
            exc.rethrowAsIOException(file);
        }
    }

    @Override
    public boolean isSameFile(Path obj1, Path obj2) throws IOException {
        UnixPath file1 = UnixPath.toUnixPath(obj1);
        if (file1.equals(obj2))
            return true;
        if (obj2 == null)
            throw new NullPointerException();
        if (!(obj2 instanceof UnixPath))
            return false;
        UnixPath file2 = (UnixPath)obj2;

        // check security manager access to both files
        file1.checkRead();
        file2.checkRead();

        UnixFileAttributes attrs1;
        UnixFileAttributes attrs2;
        try {
             attrs1 = UnixFileAttributes.get(file1, true);
        } catch (UnixException x) {
            x.rethrowAsIOException(file1);
            return false;    // keep compiler happy
        }
        try {
            attrs2 = UnixFileAttributes.get(file2, true);
        } catch (UnixException x) {
            x.rethrowAsIOException(file2);
            return false;    // keep compiler happy
        }
        return attrs1.isSameFile(attrs2);
    }

    @Override
    public boolean isHidden(Path obj) {
        UnixPath file = UnixPath.toUnixPath(obj);
        file.checkRead();
        UnixPath name = file.getFileName();
        if (name == null)
            return false;

        byte[] path;
        if (name.isEmpty()) { // corner case for empty paths
            path = name.getFileSystem().defaultDirectory();
        } else {
            path = name.asByteArray();
        }
        return path[0] == '.';
    }

    /**
     * Returns a FileStore to represent the file system where the given file
     * reside.
     */
    abstract FileStore getFileStore(UnixPath path) throws IOException;

    @Override
    public FileStore getFileStore(Path obj) throws IOException {
        UnixPath file = UnixPath.toUnixPath(obj);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("getFileStoreAttributes"));
            file.checkRead();
        }
        return getFileStore(file);
    }

    @Override
    public void createDirectory(Path obj, FileAttribute<?>... attrs)
        throws IOException
    {
        UnixPath dir = UnixPath.toUnixPath(obj);
        dir.checkWrite();

        int mode = UnixFileModeAttribute.toUnixMode(UnixFileModeAttribute.ALL_PERMISSIONS, attrs);
        try {
            mkdir(dir, mode);
        } catch (UnixException x) {
            if (x.errno() == EISDIR)
                throw new FileAlreadyExistsException(dir.toString());
            x.rethrowAsIOException(dir);
        }
    }


    @Override
    public DirectoryStream<Path> newDirectoryStream(Path obj, DirectoryStream.Filter<? super Path> filter)
        throws IOException
    {
        UnixPath dir = UnixPath.toUnixPath(obj);
        dir.checkRead();
        if (filter == null)
            throw new NullPointerException();

        // can't return SecureDirectoryStream on kernels that don't support openat
        // or O_NOFOLLOW
        if (!openatSupported() || O_NOFOLLOW == 0) {
            try {
                long ptr = opendir(dir);
                return new UnixDirectoryStream(dir, ptr, filter);
            } catch (UnixException x) {
                if (x.errno() == ENOTDIR)
                    throw new NotDirectoryException(dir.getPathForExceptionMessage());
                x.rethrowAsIOException(dir);
            }
        }

        // open directory and dup file descriptor for use by
        // opendir/readdir/closedir
        int dfd1 = -1;
        int dfd2 = -1;
        long dp = 0L;
        try {
            dfd1 = open(dir, O_RDONLY, 0);
            dfd2 = dup(dfd1);
            dp = fdopendir(dfd1);
        } catch (UnixException x) {
            if (dfd1 != -1)
                UnixNativeDispatcher.close(dfd1);
            if (dfd2 != -1)
                UnixNativeDispatcher.close(dfd2);
            if (x.errno() == UnixConstants.ENOTDIR)
                throw new NotDirectoryException(dir.getPathForExceptionMessage());
            x.rethrowAsIOException(dir);
        }
        return new UnixSecureDirectoryStream(dir, dp, dfd2, filter);
    }

    @Override
    public void createSymbolicLink(Path obj1, Path obj2, FileAttribute<?>... attrs)
        throws IOException
    {
        UnixPath link = UnixPath.toUnixPath(obj1);
        UnixPath target = UnixPath.toUnixPath(obj2);

        // no attributes supported when creating links
        if (attrs.length > 0) {
            UnixFileModeAttribute.toUnixMode(0, attrs);  // may throw NPE or UOE
            throw new UnsupportedOperationException("Initial file attributes" +
                "not supported when creating symbolic link");
        }

        // permission check
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new LinkPermission("symbolic"));
            link.checkWrite();
        }

        // create link
        try {
            symlink(target.asByteArray(), link);
        } catch (UnixException x) {
            x.rethrowAsIOException(link);
        }
    }

    @Override
    public void createLink(Path obj1, Path obj2) throws IOException {
        UnixPath link = UnixPath.toUnixPath(obj1);
        UnixPath existing = UnixPath.toUnixPath(obj2);

        // permission check
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new LinkPermission("hard"));
            link.checkWrite();
            existing.checkWrite();
        }
        try {
            link(existing, link);
        } catch (UnixException x) {
            x.rethrowAsIOException(link, existing);
        }
    }

    @Override
    public Path readSymbolicLink(Path obj1) throws IOException {
        UnixPath link = UnixPath.toUnixPath(obj1);
        // permission check
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            FilePermission perm = new FilePermission(link.getPathForPermissionCheck(),
                SecurityConstants.FILE_READLINK_ACTION);
            sm.checkPermission(perm);
        }
        try {
            byte[] target = readlink(link);
            return new UnixPath(link.getFileSystem(), target);
        } catch (UnixException x) {
           if (x.errno() == UnixConstants.EINVAL)
                throw new NotLinkException(link.getPathForExceptionMessage());
            x.rethrowAsIOException(link);
            return null;    // keep compiler happy
        }
    }

    @Override
    public final boolean isDirectory(Path obj) {
        UnixPath file = UnixPath.toUnixPath(obj);
        file.checkRead();
        int mode = UnixNativeDispatcher.stat(file);
        return ((mode & UnixConstants.S_IFMT) == UnixConstants.S_IFDIR);
    }

    @Override
    public final boolean isRegularFile(Path obj) {
        UnixPath file = UnixPath.toUnixPath(obj);
        file.checkRead();
        int mode = UnixNativeDispatcher.stat(file);
        return ((mode & UnixConstants.S_IFMT) == UnixConstants.S_IFREG);
    }

    @Override
    public final boolean exists(Path obj) {
        UnixPath file = UnixPath.toUnixPath(obj);
        file.checkRead();
        return UnixNativeDispatcher.exists(file);
    }

    /**
     * Returns a {@code FileTypeDetector} for this platform.
     */
    FileTypeDetector getFileTypeDetector() {
        return new AbstractFileTypeDetector() {
            @Override
            public String implProbeContentType(Path file) {
                return null;
            }
        };
    }

    /**
     * Returns a {@code FileTypeDetector} that chains the given array of file
     * type detectors. When the {@code implProbeContentType} method is invoked
     * then each of the detectors is invoked in turn, the result from the
     * first to detect the file type is returned.
     */
    final FileTypeDetector chain(final AbstractFileTypeDetector... detectors) {
        return new AbstractFileTypeDetector() {
            @Override
            protected String implProbeContentType(Path file) throws IOException {
                for (AbstractFileTypeDetector detector : detectors) {
                    String result = detector.implProbeContentType(file);
                    if (result != null && !result.isEmpty()) {
                        return result;
                    }
                }
                return null;
            }
        };
    }

    @Override
    public byte[] getSunPathForSocketFile(Path obj) {
        UnixPath file = UnixPath.toUnixPath(obj);
        if (file.isEmpty()) {
            return EMPTY_PATH;
        }
        return file.getByteArrayForSysCalls();
    }
}
