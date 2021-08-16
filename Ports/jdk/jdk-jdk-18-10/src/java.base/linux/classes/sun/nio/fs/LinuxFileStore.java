/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Linux implementation of FileStore
 */

class LinuxFileStore
    extends UnixFileStore
{
    // used when checking if extended attributes are enabled or not
    private volatile boolean xattrChecked;
    private volatile boolean xattrEnabled;

    LinuxFileStore(UnixPath file) throws IOException {
        super(file);
    }

    LinuxFileStore(UnixFileSystem fs, UnixMountEntry entry) throws IOException {
        super(fs, entry);
    }

    /**
     * Finds, and returns, the mount entry for the file system where the file
     * resides.
     */
    @Override
    UnixMountEntry findMountEntry() throws IOException {
        LinuxFileSystem fs = (LinuxFileSystem)file().getFileSystem();

        // step 1: get realpath
        UnixPath path = null;
        try {
            byte[] rp = UnixNativeDispatcher.realpath(file());
            path = new UnixPath(fs, rp);
        } catch (UnixException x) {
            x.rethrowAsIOException(file());
        }

        // step 2: find mount point
        List<UnixMountEntry> procMountsEntries =
            fs.getMountEntries("/proc/mounts");
        UnixPath parent = path.getParent();
        while (parent != null) {
            UnixFileAttributes attrs = null;
            try {
                attrs = UnixFileAttributes.get(parent, true);
            } catch (UnixException x) {
                x.rethrowAsIOException(parent);
            }
            if (attrs.dev() != dev()) {
                // step 3: lookup mounted file systems (use /proc/mounts to
                // ensure we find the file system even when not in /etc/mtab)
                byte[] dir = path.asByteArray();
                for (UnixMountEntry entry : procMountsEntries) {
                    if (Arrays.equals(dir, entry.dir()))
                        return entry;
                }
            }
            path = parent;
            parent = parent.getParent();
        }

        // step 3: lookup mounted file systems (use /proc/mounts to
        // ensure we find the file system even when not in /etc/mtab)
        byte[] dir = path.asByteArray();
        for (UnixMountEntry entry : procMountsEntries) {
            if (Arrays.equals(dir, entry.dir()))
                return entry;
        }

        throw new IOException("Mount point not found");
    }

    // get kernel version as a three element array {major, minor, micro}
    private static int[] getKernelVersion() {
        Pattern pattern = Pattern.compile("\\D+");
        String[] matches = pattern.split(System.getProperty("os.version"));
        int[] majorMinorMicro = new int[3];
        int length = Math.min(matches.length, majorMinorMicro.length);
        for (int i = 0; i < length; i++) {
            majorMinorMicro[i] = Integer.valueOf(matches[i]);
        }
        return majorMinorMicro;
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        // support DosFileAttributeView and UserDefinedAttributeView if extended
        // attributes enabled
        if (type == DosFileAttributeView.class ||
            type == UserDefinedFileAttributeView.class)
        {
            // lookup fstypes.properties
            FeatureStatus status = checkIfFeaturePresent("user_xattr");
            if (status == FeatureStatus.PRESENT)
                return true;
            if (status == FeatureStatus.NOT_PRESENT)
                return false;

            // if file system is mounted with user_xattr option then assume
            // extended attributes are enabled
            if ((entry().hasOption("user_xattr")))
                return true;

            // check for explicit disabling of extended attributes
            if (entry().hasOption("nouser_xattr")) {
                return false;
            }

            // user_xattr option not present but we special-case ext4 as we
            // know that extended attributes are enabled by default for
            // kernel version >= 2.6.39
            if (entry().fstype().equals("ext4")) {
                if (!xattrChecked) {
                    // check kernel version
                    int[] kernelVersion = getKernelVersion();
                    xattrEnabled = kernelVersion[0] > 2 ||
                        (kernelVersion[0] == 2 && kernelVersion[1] > 6) ||
                        (kernelVersion[0] == 2 && kernelVersion[1] == 6 &&
                            kernelVersion[2] >= 39);
                    xattrChecked = true;
                }
                return xattrEnabled;
            }

            // not ext4 so probe mount point
            if (!xattrChecked) {
                UnixPath dir = new UnixPath(file().getFileSystem(), entry().dir());
                xattrEnabled = isExtendedAttributesEnabled(dir);
                xattrChecked = true;
            }
            return xattrEnabled;
        }
        // POSIX attributes not supported on FAT
        if (type == PosixFileAttributeView.class && entry().fstype().equals("vfat"))
            return false;
        return super.supportsFileAttributeView(type);
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        if (name.equals("dos"))
            return supportsFileAttributeView(DosFileAttributeView.class);
        if (name.equals("user"))
            return supportsFileAttributeView(UserDefinedFileAttributeView.class);
        return super.supportsFileAttributeView(name);
    }
}
