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
import java.nio.channels.SeekableByteChannel;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

import static sun.nio.fs.UnixNativeDispatcher.*;
import static sun.nio.fs.UnixConstants.*;

/**
 * Unix implementation of SecureDirectoryStream.
 */

class UnixSecureDirectoryStream
    implements SecureDirectoryStream<Path>
{
    private final UnixDirectoryStream ds;
    private final int dfd;

    UnixSecureDirectoryStream(UnixPath dir,
                              long dp,
                              int dfd,
                              DirectoryStream.Filter<? super Path> filter)
    {
        this.ds = new UnixDirectoryStream(dir, dp, filter);
        this.dfd = dfd;
    }

    @Override
    public void close()
        throws IOException
    {
        ds.writeLock().lock();
        try {
            if (ds.closeImpl()) {
                UnixNativeDispatcher.close(dfd);
            }
        } finally {
            ds.writeLock().unlock();
        }
    }

    @Override
    public Iterator<Path> iterator() {
        return ds.iterator(this);
    }

    private UnixPath getName(Path obj) {
        if (obj == null)
            throw new NullPointerException();
        if (!(obj instanceof UnixPath))
            throw new ProviderMismatchException();
        return (UnixPath)obj;
    }

    /**
     * Opens sub-directory in this directory
     */
    @Override
    public SecureDirectoryStream<Path> newDirectoryStream(Path obj,
                                                          LinkOption... options)
        throws IOException
    {
        UnixPath file = getName(obj);
        UnixPath child = ds.directory().resolve(file);
        boolean followLinks = Util.followLinks(options);

        // permission check using name resolved against original path of directory
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            child.checkRead();
        }

        ds.readLock().lock();
        try {
            if (!ds.isOpen())
                throw new ClosedDirectoryStreamException();

            // open directory and create new secure directory stream
            int newdfd1 = -1;
            int newdfd2 = -1;
            long ptr = 0L;
            try {
                int flags = O_RDONLY;
                if (!followLinks)
                    flags |= O_NOFOLLOW;
                newdfd1 = openat(dfd, file.asByteArray(), flags , 0);
                newdfd2 = dup(newdfd1);
                ptr = fdopendir(newdfd1);
            } catch (UnixException x) {
                if (newdfd1 != -1)
                    UnixNativeDispatcher.close(newdfd1);
                if (newdfd2 != -1)
                    UnixNativeDispatcher.close(newdfd2);
                if (x.errno() == UnixConstants.ENOTDIR)
                    throw new NotDirectoryException(file.toString());
                x.rethrowAsIOException(file);
            }
            return new UnixSecureDirectoryStream(child, ptr, newdfd2, null);
        } finally {
            ds.readLock().unlock();
        }
    }

    /**
     * Opens file in this directory
     */
    @Override
    public SeekableByteChannel newByteChannel(Path obj,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs)
        throws IOException
    {
        UnixPath file = getName(obj);

        int mode = UnixFileModeAttribute
            .toUnixMode(UnixFileModeAttribute.ALL_READWRITE, attrs);

        // path for permission check
        String pathToCheck = ds.directory().resolve(file).getPathForPermissionCheck();

        ds.readLock().lock();
        try {
            if (!ds.isOpen())
                throw new ClosedDirectoryStreamException();
            try {
                return UnixChannelFactory.newFileChannel(dfd, file, pathToCheck, options, mode);
            } catch (UnixException x) {
                x.rethrowAsIOException(file);
                return null; // keep compiler happy
            }
        } finally {
            ds.readLock().unlock();
        }
    }

    /**
     * Deletes file/directory in this directory. Works in a race-free manner
     * when invoked with flags.
     */
    private void implDelete(Path obj, boolean haveFlags, int flags)
        throws IOException
    {
        UnixPath file = getName(obj);

        // permission check using name resolved against original path of directory
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            ds.directory().resolve(file).checkDelete();
        }

        ds.readLock().lock();
        try {
            if (!ds.isOpen())
                throw new ClosedDirectoryStreamException();

            if (!haveFlags) {
                // need file attribute to know if file is directory. This creates
                // a race in that the file may be replaced by a directory or a
                // directory replaced by a file between the time we query the
                // file type and unlink it.
                UnixFileAttributes attrs = null;
                try {
                    attrs = UnixFileAttributes.get(dfd, file, false);
                } catch (UnixException x) {
                    x.rethrowAsIOException(file);
                }
                flags = (attrs.isDirectory()) ? AT_REMOVEDIR : 0;
            }

            try {
                unlinkat(dfd, file.asByteArray(), flags);
            } catch (UnixException x) {
                if ((flags & AT_REMOVEDIR) != 0) {
                    if (x.errno() == EEXIST || x.errno() == ENOTEMPTY) {
                        throw new DirectoryNotEmptyException(null);
                    }
                }
                x.rethrowAsIOException(file);
            }
        } finally {
            ds.readLock().unlock();
        }
    }

    @Override
    public void deleteFile(Path file) throws IOException {
        implDelete(file, true, 0);
    }

    @Override
    public void deleteDirectory(Path dir) throws IOException {
        implDelete(dir, true, AT_REMOVEDIR);
    }

    /**
     * Rename/move file in this directory to another (open) directory
     */
    @Override
    public void move(Path fromObj, SecureDirectoryStream<Path> dir, Path toObj)
        throws IOException
    {
        UnixPath from = getName(fromObj);
        UnixPath to = getName(toObj);
        if (dir == null)
            throw new NullPointerException();
        if (!(dir instanceof UnixSecureDirectoryStream))
            throw new ProviderMismatchException();
        UnixSecureDirectoryStream that = (UnixSecureDirectoryStream)dir;

        // permission check
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            this.ds.directory().resolve(from).checkWrite();
            that.ds.directory().resolve(to).checkWrite();
        }

        // lock ordering doesn't matter
        this.ds.readLock().lock();
        try {
            that.ds.readLock().lock();
            try {
                if (!this.ds.isOpen() || !that.ds.isOpen())
                    throw new ClosedDirectoryStreamException();
                try {
                    renameat(this.dfd, from.asByteArray(), that.dfd, to.asByteArray());
                } catch (UnixException x) {
                    if (x.errno() == EXDEV) {
                        throw new AtomicMoveNotSupportedException(
                            from.toString(), to.toString(), x.errorString());
                    }
                    x.rethrowAsIOException(from, to);
                }
            } finally {
                that.ds.readLock().unlock();
            }
        } finally {
            this.ds.readLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private <V extends FileAttributeView> V getFileAttributeViewImpl(UnixPath file,
                                                                     Class<V> type,
                                                                     boolean followLinks)
    {
        if (type == null)
            throw new NullPointerException();
        Class<?> c = type;
        if (c == BasicFileAttributeView.class) {
            return (V) new BasicFileAttributeViewImpl(file, followLinks);
        }
        if (c == PosixFileAttributeView.class || c == FileOwnerAttributeView.class) {
            return (V) new PosixFileAttributeViewImpl(file, followLinks);
        }
        // TBD - should also support AclFileAttributeView
        return (V) null;
    }

    /**
     * Returns file attribute view bound to this directory
     */
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Class<V> type) {
        return getFileAttributeViewImpl(null, type, false);
    }

    /**
     * Returns file attribute view bound to dfd/filename.
     */
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path obj,
                                                                Class<V> type,
                                                                LinkOption... options)
    {
        UnixPath file = getName(obj);
        boolean followLinks = Util.followLinks(options);
        return getFileAttributeViewImpl(file, type, followLinks);
    }

    /**
     * A BasicFileAttributeView implementation that using a dfd/name pair.
     */
    private class BasicFileAttributeViewImpl
        implements BasicFileAttributeView
    {
        final UnixPath file;
        final boolean followLinks;

        BasicFileAttributeViewImpl(UnixPath file, boolean followLinks)
        {
            this.file = file;
            this.followLinks = followLinks;
        }

        int open() throws IOException {
            int oflags = O_RDONLY;
            if (!followLinks)
                oflags |= O_NOFOLLOW;
            try {
                return openat(dfd, file.asByteArray(), oflags, 0);
            } catch (UnixException x) {
                x.rethrowAsIOException(file);
                return -1; // keep compiler happy
            }
        }

        private void checkWriteAccess() {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                if (file == null) {
                    ds.directory().checkWrite();
                } else {
                    ds.directory().resolve(file).checkWrite();
                }
            }
        }

        @Override
        public String name() {
            return "basic";
        }

        @Override
        public BasicFileAttributes readAttributes() throws IOException {
            ds.readLock().lock();
            try {
                if (!ds.isOpen())
                    throw new ClosedDirectoryStreamException();

                @SuppressWarnings("removal")
                SecurityManager sm = System.getSecurityManager();
                if (sm != null) {
                    if (file == null) {
                        ds.directory().checkRead();
                    } else {
                        ds.directory().resolve(file).checkRead();
                    }
                }
                try {
                     UnixFileAttributes attrs = (file == null) ?
                         UnixFileAttributes.get(dfd) :
                         UnixFileAttributes.get(dfd, file, followLinks);

                     // SECURITY: must return as BasicFileAttribute
                     return attrs.asBasicFileAttributes();
                } catch (UnixException x) {
                    x.rethrowAsIOException(file);
                    return null;    // keep compiler happy
                }
            } finally {
                ds.readLock().unlock();
            }
        }

        @Override
        public void setTimes(FileTime lastModifiedTime,
                             FileTime lastAccessTime,
                             FileTime createTime) // ignore
            throws IOException
        {
            checkWriteAccess();

            ds.readLock().lock();
            try {
                if (!ds.isOpen())
                    throw new ClosedDirectoryStreamException();

                int fd = (file == null) ? dfd : open();
                try {
                    // if not changing both attributes then need existing attributes
                    if (lastModifiedTime == null || lastAccessTime == null) {
                        try {
                            UnixFileAttributes attrs = UnixFileAttributes.get(fd);
                            if (lastModifiedTime == null)
                                lastModifiedTime = attrs.lastModifiedTime();
                            if (lastAccessTime == null)
                                lastAccessTime = attrs.lastAccessTime();
                        } catch (UnixException x) {
                            x.rethrowAsIOException(file);
                        }
                    }
                    // update times
                    try {
                        futimes(fd,
                                lastAccessTime.to(TimeUnit.MICROSECONDS),
                                lastModifiedTime.to(TimeUnit.MICROSECONDS));
                    } catch (UnixException x) {
                        x.rethrowAsIOException(file);
                    }
                } finally {
                    if (file != null)
                        UnixNativeDispatcher.close(fd);
                }
            } finally {
                ds.readLock().unlock();
            }
        }
    }

    /**
     * A PosixFileAttributeView implementation that using a dfd/name pair.
     */
    private class PosixFileAttributeViewImpl
        extends BasicFileAttributeViewImpl implements PosixFileAttributeView
    {
        PosixFileAttributeViewImpl(UnixPath file, boolean followLinks) {
            super(file, followLinks);
        }

        private void checkWriteAndUserAccess() {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                super.checkWriteAccess();
                sm.checkPermission(new RuntimePermission("accessUserInformation"));
            }
        }

        @Override
        public String name() {
            return "posix";
        }

        @Override
        public PosixFileAttributes readAttributes() throws IOException {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                if (file == null)
                    ds.directory().checkRead();
                else
                    ds.directory().resolve(file).checkRead();
                sm.checkPermission(new RuntimePermission("accessUserInformation"));
            }

            ds.readLock().lock();
            try {
                if (!ds.isOpen())
                    throw new ClosedDirectoryStreamException();

                try {
                     UnixFileAttributes attrs = (file == null) ?
                         UnixFileAttributes.get(dfd) :
                         UnixFileAttributes.get(dfd, file, followLinks);
                     return attrs;
                } catch (UnixException x) {
                    x.rethrowAsIOException(file);
                    return null;    // keep compiler happy
                }
            } finally {
                ds.readLock().unlock();
            }
        }

        @Override
        public void setPermissions(Set<PosixFilePermission> perms)
            throws IOException
        {
            // permission check
            checkWriteAndUserAccess();

            ds.readLock().lock();
            try {
                if (!ds.isOpen())
                    throw new ClosedDirectoryStreamException();

                int fd = (file == null) ? dfd : open();
                try {
                    fchmod(fd, UnixFileModeAttribute.toUnixMode(perms));
                } catch (UnixException x) {
                    x.rethrowAsIOException(file);
                } finally {
                    if (file != null && fd >= 0)
                        UnixNativeDispatcher.close(fd);
                }
            } finally {
                ds.readLock().unlock();
            }
        }

        private void setOwners(int uid, int gid) throws IOException {
            // permission check
            checkWriteAndUserAccess();

            ds.readLock().lock();
            try {
                if (!ds.isOpen())
                    throw new ClosedDirectoryStreamException();

                int fd = (file == null) ? dfd : open();
                try {
                    fchown(fd, uid, gid);
                } catch (UnixException x) {
                    x.rethrowAsIOException(file);
                } finally {
                    if (file != null && fd >= 0)
                        UnixNativeDispatcher.close(fd);
                }
            } finally {
                ds.readLock().unlock();
            }
        }

        @Override
        public UserPrincipal getOwner() throws IOException {
            return readAttributes().owner();
        }

        @Override
        public void setOwner(UserPrincipal owner)
            throws IOException
        {
            if (!(owner instanceof UnixUserPrincipals.User))
                throw new ProviderMismatchException();
            if (owner instanceof UnixUserPrincipals.Group)
                throw new IOException("'owner' parameter can't be a group");
            int uid = ((UnixUserPrincipals.User)owner).uid();
            setOwners(uid, -1);
        }

        @Override
        public void setGroup(GroupPrincipal group)
            throws IOException
        {
            if (!(group instanceof UnixUserPrincipals.Group))
                throw new ProviderMismatchException();
            int gid = ((UnixUserPrincipals.Group)group).gid();
            setOwners(-1, gid);
        }
    }
}
