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

import sun.nio.cs.UTF_8;

import jdk.internal.util.StaticProperty;

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.channels.*;
import java.util.*;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Base implementation of FileStore for Unix/like implementations.
 */

abstract class UnixFileStore
    extends FileStore
{
    // original path of file that identified file system
    private final UnixPath file;

    // device ID
    private final long dev;

    // entry in the mount tab
    private final UnixMountEntry entry;

    // return the device ID where the given file resides
    private static long devFor(UnixPath file) throws IOException {
        try {
            return UnixFileAttributes.get(file, true).dev();
        } catch (UnixException x) {
            x.rethrowAsIOException(file);
            return 0L;  // keep compiler happy
        }
    }

    UnixFileStore(UnixPath file) throws IOException {
        this.file = file;
        this.dev = devFor(file);
        this.entry = findMountEntry();
    }

    UnixFileStore(UnixFileSystem fs, UnixMountEntry entry) throws IOException {
        this.file = new UnixPath(fs, entry.dir());
        this.dev = (entry.dev() == 0L) ? devFor(this.file) : entry.dev();
        this.entry = entry;
    }

    /**
     * Find the mount entry for the file store
     */
    abstract UnixMountEntry findMountEntry() throws IOException;

    UnixPath file() {
        return file;
    }

    long dev() {
        return dev;
    }

    UnixMountEntry entry() {
        return entry;
    }

    @Override
    public String name() {
        return entry.name();
    }

    @Override
    public String type() {
        return entry.fstype();
    }

    @Override
    public boolean isReadOnly() {
        return entry.isReadOnly();
    }

    // uses statvfs to read the file system information
    private UnixFileStoreAttributes readAttributes() throws IOException {
        try {
            return UnixFileStoreAttributes.get(file);
        } catch (UnixException x) {
            x.rethrowAsIOException(file);
            return null;    // keep compile happy
        }
    }

    @Override
    public long getTotalSpace() throws IOException {
        UnixFileStoreAttributes attrs = readAttributes();
        try {
            return Math.multiplyExact(attrs.blockSize(), attrs.totalBlocks());
        } catch (ArithmeticException ignore) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public long getUsableSpace() throws IOException {
        UnixFileStoreAttributes attrs = readAttributes();
        try {
            return Math.multiplyExact(attrs.blockSize(), attrs.availableBlocks());
        } catch (ArithmeticException ignore) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        UnixFileStoreAttributes attrs = readAttributes();
        try {
            return Math.multiplyExact(attrs.blockSize(), attrs.freeBlocks());
        } catch (ArithmeticException ignore) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public long getBlockSize() throws IOException {
       UnixFileStoreAttributes attrs = readAttributes();
       return attrs.blockSize();
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> view)
    {
        if (view == null)
            throw new NullPointerException();
        return (V) null;
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        if (attribute.equals("totalSpace"))
            return getTotalSpace();
        if (attribute.equals("usableSpace"))
            return getUsableSpace();
        if (attribute.equals("unallocatedSpace"))
            return getUnallocatedSpace();
        throw new UnsupportedOperationException("'" + attribute + "' not recognized");
    }

    /**
     * Checks whether extended attributes are enabled on the file system where the given file resides.
     *
     * @param path A path pointing to an existing node, such as the file system's root
     * @return <code>true</code> if enabled, <code>false</code> if disabled or unable to determine
     */
    protected boolean isExtendedAttributesEnabled(UnixPath path) {
        if (!UnixNativeDispatcher.xattrSupported()) {
            // avoid I/O if native code doesn't support xattr
            return false;
        }

        int fd = -1;
        try {
            fd = path.openForAttributeAccess(false);

            // fgetxattr returns size if called with size==0
            byte[] name = Util.toBytes("user.java");
            UnixNativeDispatcher.fgetxattr(fd, name, 0L, 0);
            return true;
        } catch (UnixException e) {
            // attribute does not exist
            if (e.errno() == UnixConstants.XATTR_NOT_FOUND)
                return true;
        } finally {
            UnixNativeDispatcher.close(fd);
        }
        return false;
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        if (type == null)
            throw new NullPointerException();
        if (type == BasicFileAttributeView.class)
            return true;
        if (type == PosixFileAttributeView.class ||
            type == FileOwnerAttributeView.class)
        {
            // lookup fstypes.properties
            FeatureStatus status = checkIfFeaturePresent("posix");
            // assume supported if UNKNOWN
            return (status != FeatureStatus.NOT_PRESENT);
        }
        return false;
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        if (name.equals("basic") || name.equals("unix"))
            return true;
        if (name.equals("posix"))
            return supportsFileAttributeView(PosixFileAttributeView.class);
        if (name.equals("owner"))
            return supportsFileAttributeView(FileOwnerAttributeView.class);
        return false;
    }

    @Override
    public boolean equals(Object ob) {
        if (ob == this)
            return true;
        if (!(ob instanceof UnixFileStore))
            return false;
        UnixFileStore other = (UnixFileStore)ob;
        return (this.dev == other.dev) &&
               Arrays.equals(this.entry.dir(), other.entry.dir()) &&
               this.entry.name().equals(other.entry.name());
    }

    @Override
    public int hashCode() {
        return (int)(dev ^ (dev >>> 32)) ^ Arrays.hashCode(entry.dir());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(Util.toString(entry.dir()));
        sb.append(" (");
        sb.append(entry.name());
        sb.append(")");
        return sb.toString();
    }

    // -- fstypes.properties --

    private static final Object loadLock = new Object();
    private static volatile Properties props;

    enum FeatureStatus {
        PRESENT,
        NOT_PRESENT,
        UNKNOWN;
    }

    /**
     * Returns status to indicate if file system supports a given feature
     */
    @SuppressWarnings("removal")
    FeatureStatus checkIfFeaturePresent(String feature) {
        if (props == null) {
            synchronized (loadLock) {
                if (props == null) {
                    props = AccessController.doPrivileged(
                        new PrivilegedAction<>() {
                            @Override
                            public Properties run() {
                                return loadProperties();
                            }});
                }
            }
        }

        String value = props.getProperty(type());
        if (value != null) {
            String[] values = value.split("\\s");
            for (String s: values) {
                s = s.trim().toLowerCase();
                if (s.equals(feature)) {
                    return FeatureStatus.PRESENT;
                }
                if (s.startsWith("no")) {
                    s = s.substring(2);
                    if (s.equals(feature)) {
                        return FeatureStatus.NOT_PRESENT;
                    }
                }
            }
        }
        return FeatureStatus.UNKNOWN;
    }

    private static Properties loadProperties() {
        Properties result = new Properties();
        String fstypes = StaticProperty.javaHome() + "/lib/fstypes.properties";
        Path file = Path.of(fstypes);
        try {
            try (ReadableByteChannel rbc = Files.newByteChannel(file)) {
                result.load(Channels.newReader(rbc, UTF_8.INSTANCE));
            }
        } catch (IOException x) {
        }
        return result;
    }
}
