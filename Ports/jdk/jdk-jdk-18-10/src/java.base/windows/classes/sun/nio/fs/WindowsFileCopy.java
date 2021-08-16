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
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

/**
 * Utility methods for copying and moving files.
 */

class WindowsFileCopy {
    private WindowsFileCopy() {
    }

    /**
     * Copy file from source to target
     */
    static void copy(final WindowsPath source,
                     final WindowsPath target,
                     CopyOption... options)
        throws IOException
    {
        // map options
        boolean replaceExisting = false;
        boolean copyAttributes = false;
        boolean followLinks = true;
        boolean interruptible = false;
        for (CopyOption option: options) {
            if (option == StandardCopyOption.REPLACE_EXISTING) {
                replaceExisting = true;
                continue;
            }
            if (option == LinkOption.NOFOLLOW_LINKS) {
                followLinks = false;
                continue;
            }
            if (option == StandardCopyOption.COPY_ATTRIBUTES) {
                copyAttributes = true;
                continue;
            }
            if (ExtendedOptions.INTERRUPTIBLE.matches(option)) {
                interruptible = true;
                continue;
            }
            if (option == null)
                throw new NullPointerException();
            throw new UnsupportedOperationException("Unsupported copy option");
        }

        // check permissions. If the source file is a symbolic link then
        // later we must also check LinkPermission
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            source.checkRead();
            target.checkWrite();
        }

        // get attributes of source file
        // attempt to get attributes of target file
        // if both files are the same there is nothing to do
        // if target exists and !replace then throw exception

        WindowsFileAttributes sourceAttrs = null;
        WindowsFileAttributes targetAttrs = null;

        long sourceHandle = 0L;
        try {
            sourceHandle = source.openForReadAttributeAccess(followLinks);
        } catch (WindowsException x) {
            x.rethrowAsIOException(source);
        }
        try {
            // source attributes
            try {
                sourceAttrs = WindowsFileAttributes.readAttributes(sourceHandle);
            } catch (WindowsException x) {
                x.rethrowAsIOException(source);
            }

            // open target (don't follow links)
            long targetHandle = 0L;
            try {
                targetHandle = target.openForReadAttributeAccess(false);
                try {
                    targetAttrs = WindowsFileAttributes.readAttributes(targetHandle);

                    // if both files are the same then nothing to do
                    if (WindowsFileAttributes.isSameFile(sourceAttrs, targetAttrs)) {
                        return;
                    }

                    // can't replace file
                    if (!replaceExisting) {
                        throw new FileAlreadyExistsException(
                            target.getPathForExceptionMessage());
                    }

                } finally {
                    CloseHandle(targetHandle);
                }
            } catch (WindowsException x) {
                // ignore
            }

        } finally {
            CloseHandle(sourceHandle);
        }

        // if source file is a symbolic link then we must check for LinkPermission
        if (sm != null && sourceAttrs.isSymbolicLink()) {
            sm.checkPermission(new LinkPermission("symbolic"));
        }

        // if source is a Unix domain socket, we don't want to copy it for various
        // reasons including consistency with Unix
        if (sourceAttrs.isUnixDomainSocket()) {
            throw new IOException("Can not copy socket file");
        }

        final String sourcePath = asWin32Path(source);
        final String targetPath = asWin32Path(target);

        // if target exists then delete it.
        if (targetAttrs != null) {
            try {
                if (targetAttrs.isDirectory() || targetAttrs.isDirectoryLink()) {
                    RemoveDirectory(targetPath);
                } else {
                    DeleteFile(targetPath);
                }
            } catch (WindowsException x) {
                if (targetAttrs.isDirectory()) {
                    // ERROR_ALREADY_EXISTS is returned when attempting to delete
                    // non-empty directory on SAMBA servers.
                    if (x.lastError() == ERROR_DIR_NOT_EMPTY ||
                        x.lastError() == ERROR_ALREADY_EXISTS)
                    {
                        throw new DirectoryNotEmptyException(
                            target.getPathForExceptionMessage());
                    }
                }
                x.rethrowAsIOException(target);
            }
        }

        // Use CopyFileEx if the file is not a directory or junction
        if (!sourceAttrs.isDirectory() && !sourceAttrs.isDirectoryLink()) {
            final int flags = (!followLinks) ? COPY_FILE_COPY_SYMLINK : 0;

            if (interruptible) {
                // interruptible copy
                Cancellable copyTask = new Cancellable() {
                    @Override
                    public int cancelValue() {
                        return 1;  // TRUE
                    }
                    @Override
                    public void implRun() throws IOException {
                        try {
                            CopyFileEx(sourcePath, targetPath, flags,
                                       addressToPollForCancel());
                        } catch (WindowsException x) {
                            x.rethrowAsIOException(source, target);
                        }
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
            } else {
                // non-interruptible copy
                try {
                    CopyFileEx(sourcePath, targetPath, flags, 0L);
                } catch (WindowsException x) {
                    x.rethrowAsIOException(source, target);
                }
            }
            if (copyAttributes) {
                // CopyFileEx does not copy security attributes
                try {
                    copySecurityAttributes(source, target, followLinks);
                } catch (IOException x) {
                    // ignore
                }
            }
            return;
        }

        // copy directory or directory junction
        try {
            if (sourceAttrs.isDirectory()) {
                CreateDirectory(targetPath, 0L);
            } else {
                String linkTarget = WindowsLinkSupport.readLink(source);
                int flags = SYMBOLIC_LINK_FLAG_DIRECTORY;
                CreateSymbolicLink(targetPath,
                                   WindowsPath.addPrefixIfNeeded(linkTarget),
                                   flags);
            }
        } catch (WindowsException x) {
            x.rethrowAsIOException(target);
        }
        if (copyAttributes) {
            // copy DOS/timestamps attributes
            WindowsFileAttributeViews.Dos view =
                WindowsFileAttributeViews.createDosView(target, false);
            try {
                view.setAttributes(sourceAttrs);
            } catch (IOException x) {
                if (sourceAttrs.isDirectory()) {
                    try {
                        RemoveDirectory(targetPath);
                    } catch (WindowsException ignore) { }
                }
            }

            // copy security attributes. If this fail it doesn't cause the move
            // to fail.
            try {
                copySecurityAttributes(source, target, followLinks);
            } catch (IOException ignore) { }
        }
    }

    // throw a DirectoryNotEmpty exception if not empty
    static void ensureEmptyDir(WindowsPath dir) throws IOException {
        try (WindowsDirectoryStream dirStream =
            new WindowsDirectoryStream(dir, (e) -> true)) {
            if (dirStream.iterator().hasNext()) {
                throw new DirectoryNotEmptyException(
                    dir.getPathForExceptionMessage());
            }
        }
    }

    /**
     * Move file from source to target
     */
    static void move(WindowsPath source, WindowsPath target, CopyOption... options)
        throws IOException
    {
        // map options
        boolean atomicMove = false;
        boolean replaceExisting = false;
        for (CopyOption option: options) {
            if (option == StandardCopyOption.ATOMIC_MOVE) {
                atomicMove = true;
                continue;
            }
            if (option == StandardCopyOption.REPLACE_EXISTING) {
                replaceExisting = true;
                continue;
            }
            if (option == LinkOption.NOFOLLOW_LINKS) {
                // ignore
                continue;
            }
            if (option == null) throw new NullPointerException();
            throw new UnsupportedOperationException("Unsupported copy option");
        }

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            source.checkWrite();
            target.checkWrite();
        }

        final String sourcePath = asWin32Path(source);
        final String targetPath = asWin32Path(target);

        // atomic case
        if (atomicMove) {
            try {
                MoveFileEx(sourcePath, targetPath, MOVEFILE_REPLACE_EXISTING);
            } catch (WindowsException x) {
                if (x.lastError() == ERROR_NOT_SAME_DEVICE) {
                    throw new AtomicMoveNotSupportedException(
                        source.getPathForExceptionMessage(),
                        target.getPathForExceptionMessage(),
                        x.errorString());
                }
                x.rethrowAsIOException(source, target);
            }
            return;
        }

        // get attributes of source file
        // attempt to get attributes of target file
        // if both files are the same there is nothing to do
        // if target exists and !replace then throw exception

        WindowsFileAttributes sourceAttrs = null;
        WindowsFileAttributes targetAttrs = null;

        long sourceHandle = 0L;
        try {
            sourceHandle = source.openForReadAttributeAccess(false);
        } catch (WindowsException x) {
            x.rethrowAsIOException(source);
        }
        try {
            // source attributes
            try {
                sourceAttrs = WindowsFileAttributes.readAttributes(sourceHandle);
            } catch (WindowsException x) {
                x.rethrowAsIOException(source);
            }

            // open target (don't follow links)
            long targetHandle = 0L;
            try {
                targetHandle = target.openForReadAttributeAccess(false);
                try {
                    targetAttrs = WindowsFileAttributes.readAttributes(targetHandle);

                    // if both files are the same then nothing to do
                    if (WindowsFileAttributes.isSameFile(sourceAttrs, targetAttrs)) {
                        return;
                    }

                    // can't replace file
                    if (!replaceExisting) {
                        throw new FileAlreadyExistsException(
                            target.getPathForExceptionMessage());
                    }

                } finally {
                    CloseHandle(targetHandle);
                }
            } catch (WindowsException x) {
                // ignore
            }

        } finally {
            CloseHandle(sourceHandle);
        }

        // if target exists then delete it.
        if (targetAttrs != null) {
            try {
                if (targetAttrs.isDirectory() || targetAttrs.isDirectoryLink()) {
                    RemoveDirectory(targetPath);
                } else {
                    DeleteFile(targetPath);
                }
            } catch (WindowsException x) {
                if (targetAttrs.isDirectory()) {
                    // ERROR_ALREADY_EXISTS is returned when attempting to delete
                    // non-empty directory on SAMBA servers.
                    if (x.lastError() == ERROR_DIR_NOT_EMPTY ||
                        x.lastError() == ERROR_ALREADY_EXISTS)
                    {
                        throw new DirectoryNotEmptyException(
                            target.getPathForExceptionMessage());
                    }
                }
                x.rethrowAsIOException(target);
            }
        }

        // first try MoveFileEx (no options). If target is on same volume then
        // all attributes (including security attributes) are preserved.
        try {
            MoveFileEx(sourcePath, targetPath, 0);
            return;
        } catch (WindowsException x) {
            if (x.lastError() != ERROR_NOT_SAME_DEVICE)
                x.rethrowAsIOException(source, target);
        }

        // target is on different volume so use MoveFileEx with copy option
        if (!sourceAttrs.isDirectory() && !sourceAttrs.isDirectoryLink()) {
            try {
                MoveFileEx(sourcePath, targetPath, MOVEFILE_COPY_ALLOWED);
            } catch (WindowsException x) {
                x.rethrowAsIOException(source, target);
            }
            // MoveFileEx does not copy security attributes when moving
            // across volumes.
            try {
                copySecurityAttributes(source, target, false);
            } catch (IOException x) {
                // ignore
            }
            return;
        }

        // moving directory or directory-link to another file system
        assert sourceAttrs.isDirectory() || sourceAttrs.isDirectoryLink();

        // create new directory or directory junction
        try {
            if (sourceAttrs.isDirectory()) {
                ensureEmptyDir(source);
                CreateDirectory(targetPath, 0L);
            } else {
                String linkTarget = WindowsLinkSupport.readLink(source);
                CreateSymbolicLink(targetPath,
                                   WindowsPath.addPrefixIfNeeded(linkTarget),
                                   SYMBOLIC_LINK_FLAG_DIRECTORY);
            }
        } catch (WindowsException x) {
            x.rethrowAsIOException(target);
        }

        // copy timestamps/DOS attributes
        WindowsFileAttributeViews.Dos view =
                WindowsFileAttributeViews.createDosView(target, false);
        try {
            view.setAttributes(sourceAttrs);
        } catch (IOException x) {
            // rollback
            try {
                RemoveDirectory(targetPath);
            } catch (WindowsException ignore) { }
            throw x;
        }

        // copy security attributes. If this fails it doesn't cause the move
        // to fail.
        try {
            copySecurityAttributes(source, target, false);
        } catch (IOException ignore) { }

        // delete source
        try {
            RemoveDirectory(sourcePath);
        } catch (WindowsException x) {
            // rollback
            try {
                RemoveDirectory(targetPath);
            } catch (WindowsException ignore) { }
            // ERROR_ALREADY_EXISTS is returned when attempting to delete
            // non-empty directory on SAMBA servers.
            if (x.lastError() == ERROR_DIR_NOT_EMPTY ||
                x.lastError() == ERROR_ALREADY_EXISTS)
            {
                throw new DirectoryNotEmptyException(
                    target.getPathForExceptionMessage());
            }
            x.rethrowAsIOException(source);
        }
    }


    private static String asWin32Path(WindowsPath path) throws IOException {
        try {
            return path.getPathForWin32Calls();
        } catch (WindowsException x) {
            x.rethrowAsIOException(path);
            return null;
        }
    }

    /**
     * Copy DACL/owner/group from source to target
     */
    private static void copySecurityAttributes(WindowsPath source,
                                               WindowsPath target,
                                               boolean followLinks)
        throws IOException
    {
        String path = WindowsLinkSupport.getFinalPath(source, followLinks);

        // may need SeRestorePrivilege to set file owner
        WindowsSecurity.Privilege priv =
            WindowsSecurity.enablePrivilege("SeRestorePrivilege");
        try {
            int request = (DACL_SECURITY_INFORMATION |
                OWNER_SECURITY_INFORMATION | GROUP_SECURITY_INFORMATION);
            NativeBuffer buffer =
                WindowsAclFileAttributeView.getFileSecurity(path, request);
            try {
                try {
                    SetFileSecurity(target.getPathForWin32Calls(), request,
                        buffer.address());
                } catch (WindowsException x) {
                    x.rethrowAsIOException(target);
                }
            } finally {
                buffer.release();
            }
        } finally {
            priv.drop();
        }
    }
}
