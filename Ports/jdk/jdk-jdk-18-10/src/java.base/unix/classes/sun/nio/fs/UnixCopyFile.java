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

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.LinkPermission;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static sun.nio.fs.UnixNativeDispatcher.*;
import static sun.nio.fs.UnixConstants.*;


/**
 * Unix implementation of Path#copyTo and Path#moveTo methods.
 */

class UnixCopyFile {
    private UnixCopyFile() {  }

    // The flags that control how a file is copied or moved
    private static class Flags {
        boolean replaceExisting;
        boolean atomicMove;
        boolean followLinks;
        boolean interruptible;

        // the attributes to copy
        boolean copyBasicAttributes;
        boolean copyPosixAttributes;
        boolean copyNonPosixAttributes;

        // flags that indicate if we should fail if attributes cannot be copied
        boolean failIfUnableToCopyBasic;
        boolean failIfUnableToCopyPosix;
        boolean failIfUnableToCopyNonPosix;

        static Flags fromCopyOptions(CopyOption... options) {
            Flags flags = new Flags();
            flags.followLinks = true;
            for (CopyOption option: options) {
                if (option == StandardCopyOption.REPLACE_EXISTING) {
                    flags.replaceExisting = true;
                    continue;
                }
                if (option == LinkOption.NOFOLLOW_LINKS) {
                    flags.followLinks = false;
                    continue;
                }
                if (option == StandardCopyOption.COPY_ATTRIBUTES) {
                    // copy all attributes but only fail if basic attributes
                    // cannot be copied
                    flags.copyBasicAttributes = true;
                    flags.copyPosixAttributes = true;
                    flags.copyNonPosixAttributes = true;
                    flags.failIfUnableToCopyBasic = true;
                    continue;
                }
                if (ExtendedOptions.INTERRUPTIBLE.matches(option)) {
                    flags.interruptible = true;
                    continue;
                }
                if (option == null)
                    throw new NullPointerException();
                throw new UnsupportedOperationException("Unsupported copy option");
            }
            return flags;
        }

        static Flags fromMoveOptions(CopyOption... options) {
            Flags flags = new Flags();
            for (CopyOption option: options) {
                if (option == StandardCopyOption.ATOMIC_MOVE) {
                    flags.atomicMove = true;
                    continue;
                }
                if (option == StandardCopyOption.REPLACE_EXISTING) {
                    flags.replaceExisting = true;
                    continue;
                }
                if (option == LinkOption.NOFOLLOW_LINKS) {
                    // ignore
                    continue;
                }
                if (option == null)
                    throw new NullPointerException();
                throw new UnsupportedOperationException("Unsupported copy option");
            }

            // a move requires that all attributes be copied but only fail if
            // the basic attributes cannot be copied
            flags.copyBasicAttributes = true;
            flags.copyPosixAttributes = true;
            flags.copyNonPosixAttributes = true;
            flags.failIfUnableToCopyBasic = true;
            return flags;
        }
    }

    // copy directory from source to target
    private static void copyDirectory(UnixPath source,
                                      UnixFileAttributes attrs,
                                      UnixPath target,
                                      Flags flags)
        throws IOException
    {
        try {
            mkdir(target, attrs.mode());
        } catch (UnixException x) {
            x.rethrowAsIOException(target);
        }

        // no attributes to copy
        if (!flags.copyBasicAttributes &&
            !flags.copyPosixAttributes &&
            !flags.copyNonPosixAttributes) return;

        // open target directory if possible (this can fail when copying a
        // directory for which we don't have read access).
        int dfd = -1;
        try {
            dfd = open(target, O_RDONLY, 0);
        } catch (UnixException x) {
            // access to target directory required to copy named attributes
            if (flags.copyNonPosixAttributes && flags.failIfUnableToCopyNonPosix) {
                try { rmdir(target); } catch (UnixException ignore) { }
                x.rethrowAsIOException(target);
            }
        }

        boolean done = false;
        try {
            // copy owner/group/permissions
            if (flags.copyPosixAttributes){
                try {
                    if (dfd >= 0) {
                        fchown(dfd, attrs.uid(), attrs.gid());
                        fchmod(dfd, attrs.mode());
                    } else {
                        chown(target, attrs.uid(), attrs.gid());
                        chmod(target, attrs.mode());
                    }
                } catch (UnixException x) {
                    // unable to set owner/group
                    if (flags.failIfUnableToCopyPosix)
                        x.rethrowAsIOException(target);
                }
            }
            // copy other attributes
            if (flags.copyNonPosixAttributes && (dfd >= 0)) {
                int sfd = -1;
                try {
                    sfd = open(source, O_RDONLY, 0);
                } catch (UnixException x) {
                    if (flags.failIfUnableToCopyNonPosix)
                        x.rethrowAsIOException(source);
                }
                if (sfd >= 0) {
                    source.getFileSystem().copyNonPosixAttributes(sfd, dfd);
                    close(sfd);
                }
            }
            // copy time stamps last
            if (flags.copyBasicAttributes) {
                try {
                    if (dfd >= 0 && futimesSupported()) {
                        futimes(dfd,
                                attrs.lastAccessTime().to(TimeUnit.MICROSECONDS),
                                attrs.lastModifiedTime().to(TimeUnit.MICROSECONDS));
                    } else {
                        utimes(target,
                               attrs.lastAccessTime().to(TimeUnit.MICROSECONDS),
                               attrs.lastModifiedTime().to(TimeUnit.MICROSECONDS));
                    }
                } catch (UnixException x) {
                    // unable to set times
                    if (flags.failIfUnableToCopyBasic)
                        x.rethrowAsIOException(target);
                }
            }
            done = true;
        } finally {
            if (dfd >= 0)
                close(dfd);
            if (!done) {
                // rollback
                try { rmdir(target); } catch (UnixException ignore) { }
            }
        }
    }

    // copy regular file from source to target
    private static void copyFile(UnixPath source,
                                 UnixFileAttributes attrs,
                                 UnixPath  target,
                                 Flags flags,
                                 long addressToPollForCancel)
        throws IOException
    {
        int fi = -1;
        try {
            fi = open(source, O_RDONLY, 0);
        } catch (UnixException x) {
            x.rethrowAsIOException(source);
        }

        try {
            // open new file
            int fo = -1;
            try {
                fo = open(target,
                           (O_WRONLY |
                            O_CREAT |
                            O_EXCL),
                           attrs.mode());
            } catch (UnixException x) {
                x.rethrowAsIOException(target);
            }

            // set to true when file and attributes copied
            boolean complete = false;
            try {
                // transfer bytes to target file
                try {
                    transfer(fo, fi, addressToPollForCancel);
                } catch (UnixException x) {
                    x.rethrowAsIOException(source, target);
                }
                // copy owner/permissions
                if (flags.copyPosixAttributes) {
                    try {
                        fchown(fo, attrs.uid(), attrs.gid());
                        fchmod(fo, attrs.mode());
                    } catch (UnixException x) {
                        if (flags.failIfUnableToCopyPosix)
                            x.rethrowAsIOException(target);
                    }
                }
                // copy non POSIX attributes (depends on file system)
                if (flags.copyNonPosixAttributes) {
                    source.getFileSystem().copyNonPosixAttributes(fi, fo);
                }
                // copy time attributes
                if (flags.copyBasicAttributes) {
                    try {
                        if (futimesSupported()) {
                            futimes(fo,
                                    attrs.lastAccessTime().to(TimeUnit.MICROSECONDS),
                                    attrs.lastModifiedTime().to(TimeUnit.MICROSECONDS));
                        } else {
                            utimes(target,
                                   attrs.lastAccessTime().to(TimeUnit.MICROSECONDS),
                                   attrs.lastModifiedTime().to(TimeUnit.MICROSECONDS));
                        }
                    } catch (UnixException x) {
                        if (flags.failIfUnableToCopyBasic)
                            x.rethrowAsIOException(target);
                    }
                }
                complete = true;
            } finally {
                close(fo);

                // copy of file or attributes failed so rollback
                if (!complete) {
                    try {
                        unlink(target);
                    } catch (UnixException ignore) { }
                }
            }
        } finally {
            close(fi);
        }
    }

    // copy symbolic link from source to target
    private static void copyLink(UnixPath source,
                                 UnixFileAttributes attrs,
                                 UnixPath  target,
                                 Flags flags)
        throws IOException
    {
        byte[] linktarget = null;
        try {
            linktarget = readlink(source);
        } catch (UnixException x) {
            x.rethrowAsIOException(source);
        }
        try {
            symlink(linktarget, target);

            if (flags.copyPosixAttributes) {
                try {
                    lchown(target, attrs.uid(), attrs.gid());
                } catch (UnixException x) {
                    // ignore since link attributes not required to be copied
                }
            }
        } catch (UnixException x) {
            x.rethrowAsIOException(target);
        }
    }

    // copy special file from source to target
    private static void copySpecial(UnixPath source,
                                    UnixFileAttributes attrs,
                                    UnixPath  target,
                                    Flags flags)
        throws IOException
    {
        try {
            mknod(target, attrs.mode(), attrs.rdev());
        } catch (UnixException x) {
            x.rethrowAsIOException(target);
        }
        boolean done = false;
        try {
            if (flags.copyPosixAttributes) {
                try {
                    chown(target, attrs.uid(), attrs.gid());
                    chmod(target, attrs.mode());
                } catch (UnixException x) {
                    if (flags.failIfUnableToCopyPosix)
                        x.rethrowAsIOException(target);
                }
            }
            if (flags.copyBasicAttributes) {
                try {
                    utimes(target,
                           attrs.lastAccessTime().to(TimeUnit.MICROSECONDS),
                           attrs.lastModifiedTime().to(TimeUnit.MICROSECONDS));
                } catch (UnixException x) {
                    if (flags.failIfUnableToCopyBasic)
                        x.rethrowAsIOException(target);
                }
            }
            done = true;
        } finally {
            if (!done) {
                try { unlink(target); } catch (UnixException ignore) { }
            }
        }
    }

    // throw a DirectoryNotEmpty exception if appropriate
    static void ensureEmptyDir(UnixPath dir) throws IOException {
        try {
            long ptr = opendir(dir);
            try (UnixDirectoryStream stream =
                new UnixDirectoryStream(dir, ptr, e -> true)) {
                if (stream.iterator().hasNext()) {
                    throw new DirectoryNotEmptyException(
                        dir.getPathForExceptionMessage());
                }
            }
        } catch (UnixException e) {
            e.rethrowAsIOException(dir);
        }
    }

    // move file from source to target
    static void move(UnixPath source, UnixPath target, CopyOption... options)
        throws IOException
    {
        // permission check
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            source.checkWrite();
            target.checkWrite();
        }

        // translate options into flags
        Flags flags = Flags.fromMoveOptions(options);

        // handle atomic rename case
        if (flags.atomicMove) {
            try {
                rename(source, target);
            } catch (UnixException x) {
                if (x.errno() == EXDEV) {
                    throw new AtomicMoveNotSupportedException(
                        source.getPathForExceptionMessage(),
                        target.getPathForExceptionMessage(),
                        x.errorString());
                }
                x.rethrowAsIOException(source, target);
            }
            return;
        }

        // move using rename or copy+delete
        UnixFileAttributes sourceAttrs = null;
        UnixFileAttributes targetAttrs = null;

        // get attributes of source file (don't follow links)
        try {
            sourceAttrs = UnixFileAttributes.get(source, false);
        } catch (UnixException x) {
            x.rethrowAsIOException(source);
        }

        // get attributes of target file (don't follow links)
        try {
            targetAttrs = UnixFileAttributes.get(target, false);
        } catch (UnixException x) {
            // ignore
        }
        boolean targetExists = (targetAttrs != null);

        // if the target exists:
        // 1. check if source and target are the same file
        // 2. throw exception if REPLACE_EXISTING option is not set
        // 3. delete target if REPLACE_EXISTING option set
        if (targetExists) {
            if (sourceAttrs.isSameFile(targetAttrs))
                return;  // nothing to do as files are identical
            if (!flags.replaceExisting) {
                throw new FileAlreadyExistsException(
                    target.getPathForExceptionMessage());
            }

            // attempt to delete target
            try {
                if (targetAttrs.isDirectory()) {
                    rmdir(target);
                } else {
                    unlink(target);
                }
            } catch (UnixException x) {
                // target is non-empty directory that can't be replaced.
                if (targetAttrs.isDirectory() &&
                   (x.errno() == EEXIST || x.errno() == ENOTEMPTY))
                {
                    throw new DirectoryNotEmptyException(
                        target.getPathForExceptionMessage());
                }
                x.rethrowAsIOException(target);
            }
        }

        // first try rename
        try {
            rename(source, target);
            return;
        } catch (UnixException x) {
            if (x.errno() != EXDEV && x.errno() != EISDIR) {
                x.rethrowAsIOException(source, target);
            }
        }

        // copy source to target
        if (sourceAttrs.isDirectory()) {
            ensureEmptyDir(source);
            copyDirectory(source, sourceAttrs, target, flags);
        } else {
            if (sourceAttrs.isSymbolicLink()) {
                copyLink(source, sourceAttrs, target, flags);
            } else {
                if (sourceAttrs.isDevice()) {
                    copySpecial(source, sourceAttrs, target, flags);
                } else {
                    copyFile(source, sourceAttrs, target, flags, 0L);
                }
            }
        }

        // delete source
        try {
            if (sourceAttrs.isDirectory()) {
                rmdir(source);
            } else {
                unlink(source);
            }
        } catch (UnixException x) {
            // file was copied but unable to unlink the source file so attempt
            // to remove the target and throw a reasonable exception
            try {
                if (sourceAttrs.isDirectory()) {
                    rmdir(target);
                } else {
                    unlink(target);
                }
            } catch (UnixException ignore) { }

            if (sourceAttrs.isDirectory() &&
                (x.errno() == EEXIST || x.errno() == ENOTEMPTY))
            {
                throw new DirectoryNotEmptyException(
                    source.getPathForExceptionMessage());
            }
            x.rethrowAsIOException(source);
        }
    }

    // copy file from source to target
    static void copy(final UnixPath source,
                     final UnixPath target,
                     CopyOption... options) throws IOException
    {
        // permission checks
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            source.checkRead();
            target.checkWrite();
        }

        // translate options into flags
        final Flags flags = Flags.fromCopyOptions(options);

        UnixFileAttributes sourceAttrs = null;
        UnixFileAttributes targetAttrs = null;

        // get attributes of source file
        try {
            sourceAttrs = UnixFileAttributes.get(source, flags.followLinks);
        } catch (UnixException x) {
            x.rethrowAsIOException(source);
        }

        // if source file is symbolic link then we must check LinkPermission
        if (sm != null && sourceAttrs.isSymbolicLink()) {
            sm.checkPermission(new LinkPermission("symbolic"));
        }

        // get attributes of target file (don't follow links)
        try {
            targetAttrs = UnixFileAttributes.get(target, false);
        } catch (UnixException x) {
            // ignore
        }
        boolean targetExists = (targetAttrs != null);

        // if the target exists:
        // 1. check if source and target are the same file
        // 2. throw exception if REPLACE_EXISTING option is not set
        // 3. try to unlink the target
        if (targetExists) {
            if (sourceAttrs.isSameFile(targetAttrs))
                return;  // nothing to do as files are identical
            if (!flags.replaceExisting)
                throw new FileAlreadyExistsException(
                    target.getPathForExceptionMessage());
            try {
                if (targetAttrs.isDirectory()) {
                    rmdir(target);
                } else {
                    unlink(target);
                }
            } catch (UnixException x) {
                // target is non-empty directory that can't be replaced.
                if (targetAttrs.isDirectory() &&
                   (x.errno() == EEXIST || x.errno() == ENOTEMPTY))
                {
                    throw new DirectoryNotEmptyException(
                        target.getPathForExceptionMessage());
                }
                x.rethrowAsIOException(target);
            }
        }

        // do the copy
        if (sourceAttrs.isDirectory()) {
            copyDirectory(source, sourceAttrs, target, flags);
            return;
        }
        if (sourceAttrs.isSymbolicLink()) {
            copyLink(source, sourceAttrs, target, flags);
            return;
        }
        if (!flags.interruptible) {
            // non-interruptible file copy
            copyFile(source, sourceAttrs, target, flags, 0L);
            return;
        }

        // interruptible file copy
        final UnixFileAttributes attrsToCopy = sourceAttrs;
        Cancellable copyTask = new Cancellable() {
            @Override public void implRun() throws IOException {
                copyFile(source, attrsToCopy, target, flags,
                    addressToPollForCancel());
            }
        };
        try {
            Cancellable.runInterruptibly(copyTask);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof IOException)
                throw (IOException)t;
            throw new IOException(t);
        }
    }

    // -- native methods --

    static native void transfer(int dst, int src, long addressToPollForCancel)
        throws UnixException;

    static {
        jdk.internal.loader.BootLoader.loadLibrary("nio");
    }

}
