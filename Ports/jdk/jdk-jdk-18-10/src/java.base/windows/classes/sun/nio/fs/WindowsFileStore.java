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

import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.io.IOException;
import java.util.Locale;

import static sun.nio.fs.WindowsConstants.*;
import static sun.nio.fs.WindowsNativeDispatcher.*;

/**
 * Windows implementation of FileStore.
 */

class WindowsFileStore
    extends FileStore
{
    private final String root;
    private final VolumeInformation volInfo;
    private final int volType;
    private final String displayName;   // returned by toString

    private int hashCode;

    private WindowsFileStore(String root) throws WindowsException {
        assert root.charAt(root.length()-1) == '\\';
        this.root = root;
        this.volInfo = GetVolumeInformation(root);
        this.volType = GetDriveType(root);

        // file store "display name" is the volume name if available
        String vol = volInfo.volumeName();
        if (!vol.isEmpty()) {
            this.displayName = vol;
        } else {
            // TBD - should we map all types? Does this need to be localized?
            this.displayName = (volType == DRIVE_REMOVABLE) ? "Removable Disk" : "";
        }
    }

    static WindowsFileStore create(String root, boolean ignoreNotReady)
        throws IOException
    {
        try {
            return new WindowsFileStore(root);
        } catch (WindowsException x) {
            if (ignoreNotReady && x.lastError() == ERROR_NOT_READY)
                return null;
            x.rethrowAsIOException(root);
            return null; // keep compiler happy
        }
    }

    static WindowsFileStore create(WindowsPath file) throws IOException {
        try {
            // if the file is a link then GetVolumePathName returns the
            // volume that the link is on so we need to call it with the
            // final target
            String target = WindowsLinkSupport.getFinalPath(file, true);
            try {
                return createFromPath(target);
            } catch (WindowsException e) {
                // GetVolumePathName might return the following error codes
                // when the drives were created using `subst`.
                // Try expanding the path again in such cases.
                if (e.lastError() != ERROR_DIR_NOT_ROOT &&
                    e.lastError() != ERROR_INVALID_PARAMETER &&
                    e.lastError() != ERROR_DIRECTORY)
                    throw e;
                target = WindowsLinkSupport.getFinalPath(file);
                if (target == null)
                    throw new FileSystemException(file.getPathForExceptionMessage(),
                            null, "Couldn't resolve path");
                return createFromPath(target);
            }
        } catch (WindowsException x) {
            x.rethrowAsIOException(file);
            return null; // keep compiler happy
        }
    }

    private static WindowsFileStore createFromPath(String target) throws WindowsException {
        String root = GetVolumePathName(target);
        return new WindowsFileStore(root);
    }

    VolumeInformation volumeInformation() {
        return volInfo;
    }

    int volumeType() {
        return volType;
    }

    @Override
    public String name() {
        return volInfo.volumeName();   // "SYSTEM", "DVD-RW", ...
    }

    @Override
    public String type() {
        return volInfo.fileSystemName();  // "FAT", "NTFS", ...
    }

    @Override
    public boolean isReadOnly() {
        return ((volInfo.flags() & FILE_READ_ONLY_VOLUME) != 0);
    }

    // read the free space info
    private DiskFreeSpace readDiskFreeSpaceEx() throws IOException {
        try {
            return GetDiskFreeSpaceEx(root);
        } catch (WindowsException x) {
            x.rethrowAsIOException(root);
            return null;
        }
    }

    private DiskFreeSpace readDiskFreeSpace() throws IOException {
        try {
            return GetDiskFreeSpace(root);
        } catch (WindowsException x) {
            x.rethrowAsIOException(root);
            return null;
        }
    }

    @Override
    public long getTotalSpace() throws IOException {
        long space = readDiskFreeSpaceEx().totalNumberOfBytes();
        return space >= 0 ? space : Long.MAX_VALUE;
    }

    @Override
    public long getUsableSpace() throws IOException {
        long space = readDiskFreeSpaceEx().freeBytesAvailable();
        return space >= 0 ? space : Long.MAX_VALUE;
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        long space = readDiskFreeSpaceEx().freeBytesAvailable();
        return space >= 0 ? space : Long.MAX_VALUE;
    }

    @Override
    public long getBlockSize() throws IOException {
        return readDiskFreeSpace().bytesPerSector();
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        if (type == null)
            throw new NullPointerException();
        return (V) null;
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        // standard
        if (attribute.equals("totalSpace"))
            return getTotalSpace();
        if (attribute.equals("usableSpace"))
            return getUsableSpace();
        if (attribute.equals("unallocatedSpace"))
            return getUnallocatedSpace();
        if (attribute.equals("bytesPerSector"))
            return getBlockSize();
        // windows specific for testing purposes
        if (attribute.equals("volume:vsn"))
            return volInfo.volumeSerialNumber();
        if (attribute.equals("volume:isRemovable"))
            return volType == DRIVE_REMOVABLE;
        if (attribute.equals("volume:isCdrom"))
            return volType == DRIVE_CDROM;
        throw new UnsupportedOperationException("'" + attribute + "' not recognized");
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        if (type == null)
            throw new NullPointerException();
        if (type == BasicFileAttributeView.class || type == DosFileAttributeView.class)
            return true;
        if (type == AclFileAttributeView.class || type == FileOwnerAttributeView.class)
            return ((volInfo.flags() & FILE_PERSISTENT_ACLS) != 0);
        if (type == UserDefinedFileAttributeView.class)
            return ((volInfo.flags() & FILE_NAMED_STREAMS) != 0);
        return false;
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        if (name.equals("basic") || name.equals("dos"))
            return true;
        if (name.equals("acl"))
            return supportsFileAttributeView(AclFileAttributeView.class);
        if (name.equals("owner"))
            return supportsFileAttributeView(FileOwnerAttributeView.class);
        if (name.equals("user"))
            return supportsFileAttributeView(UserDefinedFileAttributeView.class);
        return false;
    }

    @Override
    public boolean equals(Object ob) {
        if (ob == this)
            return true;
        if (ob instanceof WindowsFileStore other) {
            if (root.equals(other.root))
                return true;
            if (volType == DRIVE_FIXED && other.volumeType() == DRIVE_FIXED)
                return root.equalsIgnoreCase(other.root);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hc = hashCode;
        if (hc == 0) {
            hc = (volType == DRIVE_FIXED) ?
                root.toLowerCase(Locale.ROOT).hashCode() : root.hashCode();
            hashCode = hc;
        }
        return hc;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(displayName);
        if (sb.length() > 0)
            sb.append(" ");
        sb.append("(");
        // drop trailing slash
        sb.append(root.subSequence(0, root.length()-1));
        sb.append(")");
        return sb.toString();
    }
}
