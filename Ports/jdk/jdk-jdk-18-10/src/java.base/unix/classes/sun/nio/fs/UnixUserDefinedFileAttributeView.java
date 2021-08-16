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

import java.lang.ref.Reference;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.*;
import jdk.internal.misc.Unsafe;

import static sun.nio.fs.UnixConstants.*;
import static sun.nio.fs.UnixNativeDispatcher.*;

/**
 * Unix implementation of UserDefinedFileAttributeView using extended attributes.
 */
abstract class UnixUserDefinedFileAttributeView
    extends AbstractUserDefinedFileAttributeView
{
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    // namespace for extended user attributes
    private static final String USER_NAMESPACE = "user.";

    private static final int MIN_LISTXATTR_BUF_SIZE = 1024;
    private static final int MAX_LISTXATTR_BUF_SIZE = 32 * 1024;

    private final UnixPath file;
    private final boolean followLinks;

    UnixUserDefinedFileAttributeView(UnixPath file, boolean followLinks) {
        this.file = file;
        this.followLinks = followLinks;
    }

    private byte[] nameAsBytes(UnixPath file, String name) throws IOException {
        if (name == null)
            throw new NullPointerException("'name' is null");
        name = USER_NAMESPACE + name;
        byte[] bytes = Util.toBytes(name);
        if (bytes.length > maxNameLength()) {
            throw new FileSystemException(file.getPathForExceptionMessage(),
                null, "'" + name + "' is too big");
        }
        return bytes;
    }

    /**
     * @return the maximum supported length of xattr names (in bytes, including namespace)
     */
    protected abstract int maxNameLength();

    // Parses buffer as array of NULL-terminated C strings.
    private static List<String> asList(long address, int size) {
        List<String> list = new ArrayList<>();
        int start = 0;
        int pos = 0;
        while (pos < size) {
            if (unsafe.getByte(address + pos) == 0) {
                int len = pos - start;
                byte[] value = new byte[len];
                unsafe.copyMemory(null, address+start, value,
                    Unsafe.ARRAY_BYTE_BASE_OFFSET, len);
                String s = Util.toString(value);
                list.add(s);
                start = pos + 1;
            }
            pos++;
        }
        return list;
    }

    // runs flistxattr, increases buffer size up to MAX_LISTXATTR_BUF_SIZE if required
    private static List<String> list(int fd, int bufSize) throws UnixException {
        try {
            try (NativeBuffer buffer = NativeBuffers.getNativeBuffer(bufSize)) {
                int n = flistxattr(fd, buffer.address(), bufSize);
                return asList(buffer.address(), n);
            } // release buffer before recursion
        } catch (UnixException x) {
            if (x.errno() == ERANGE && bufSize < MAX_LISTXATTR_BUF_SIZE) {
                return list(fd, bufSize * 2); // try larger buffer size:
            } else {
                throw x;
            }
        }
    }

    @SuppressWarnings("removal")
    @Override
    public List<String> list() throws IOException  {
        if (System.getSecurityManager() != null)
            checkAccess(file.getPathForPermissionCheck(), true, false);

        int fd = -1;
        try {
            fd = file.openForAttributeAccess(followLinks);
        } catch (UnixException x) {
            x.rethrowAsIOException(file);
        }
        try {
            List<String> attrNames = list(fd, MIN_LISTXATTR_BUF_SIZE);
            return attrNames.stream()
                    .filter(s -> s.startsWith(USER_NAMESPACE))
                    .map(s -> s.substring(USER_NAMESPACE.length()))
                    .toList();
        } catch (UnixException x) {
            throw new FileSystemException(file.getPathForExceptionMessage(),
                null, "Unable to get list of extended attributes: " +
                x.getMessage());
        } finally {
            close(fd);
        }
    }

    @SuppressWarnings("removal")
    @Override
    public int size(String name) throws IOException  {
        if (System.getSecurityManager() != null)
            checkAccess(file.getPathForPermissionCheck(), true, false);

        int fd = -1;
        try {
            fd = file.openForAttributeAccess(followLinks);
        } catch (UnixException x) {
            x.rethrowAsIOException(file);
        }
        try {
            // fgetxattr returns size if called with size==0
            return fgetxattr(fd, nameAsBytes(file,name), 0L, 0);
        } catch (UnixException x) {
            throw new FileSystemException(file.getPathForExceptionMessage(),
                null, "Unable to get size of extended attribute '" + name +
                "': " + x.getMessage());
        } finally {
            close(fd);
        }
    }

    @SuppressWarnings("removal")
    @Override
    public int read(String name, ByteBuffer dst) throws IOException {
        if (System.getSecurityManager() != null)
            checkAccess(file.getPathForPermissionCheck(), true, false);

        if (dst.isReadOnly())
            throw new IllegalArgumentException("Read-only buffer");
        int pos = dst.position();
        int lim = dst.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        if (dst instanceof sun.nio.ch.DirectBuffer buf) {
            try {
                long address = buf.address() + pos;
                int n = read(name, address, rem);
                dst.position(pos + n);
                return n;
            } finally {
                Reference.reachabilityFence(buf);
            }
        } else {
            try (NativeBuffer nb = NativeBuffers.getNativeBuffer(rem)) {
                long address = nb.address();
                int n = read(name, address, rem);

                // copy from buffer into backing array
                int off = dst.arrayOffset() + pos + Unsafe.ARRAY_BYTE_BASE_OFFSET;
                unsafe.copyMemory(null, address, dst.array(), off, n);
                dst.position(pos + n);

                return n;
            }
        }
    }

    private int read(String name, long address, int rem) throws IOException {
        int fd = -1;
        try {
            fd = file.openForAttributeAccess(followLinks);
        } catch (UnixException x) {
            x.rethrowAsIOException(file);
        }
        try {
            int n = fgetxattr(fd, nameAsBytes(file, name), address, rem);

            // if remaining is zero then fgetxattr returns the size
            if (rem == 0) {
                if (n > 0)
                    throw new UnixException(ERANGE);
                return 0;
            }
            return n;
        } catch (UnixException x) {
            String msg = (x.errno() == ERANGE) ?
                    "Insufficient space in buffer" : x.getMessage();
            throw new FileSystemException(file.getPathForExceptionMessage(),
                    null, "Error reading extended attribute '" + name + "': " + msg);
        } finally {
            close(fd);
        }
    }

    @SuppressWarnings("removal")
    @Override
    public int write(String name, ByteBuffer src) throws IOException {
        if (System.getSecurityManager() != null)
            checkAccess(file.getPathForPermissionCheck(), false, true);

        int pos = src.position();
        int lim = src.limit();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        if (src instanceof sun.nio.ch.DirectBuffer buf) {
            try {
                long address = buf.address() + pos;
                write(name, address, rem);
                src.position(pos + rem);
                return rem;
            } finally {
                Reference.reachabilityFence(buf);
            }
        } else {
            try (NativeBuffer nb = NativeBuffers.getNativeBuffer(rem)) {
                long address = nb.address();

                if (src.hasArray()) {
                    // copy from backing array into buffer
                    int off = src.arrayOffset() + pos + Unsafe.ARRAY_BYTE_BASE_OFFSET;
                    unsafe.copyMemory(src.array(), off, null, address, rem);
                } else {
                    // backing array not accessible so transfer via temporary array
                    byte[] tmp = new byte[rem];
                    src.get(tmp);
                    src.position(pos);  // reset position as write may fail
                    unsafe.copyMemory(tmp, Unsafe.ARRAY_BYTE_BASE_OFFSET, null,
                            address, rem);
                }

                write(name, address, rem);
                src.position(pos + rem);
                return rem;
            }
        }
    }

    private void write(String name, long address, int rem) throws IOException {
        int fd = -1;
        try {
            fd = file.openForAttributeAccess(followLinks);
        } catch (UnixException x) {
            x.rethrowAsIOException(file);
        }
        try {
            fsetxattr(fd, nameAsBytes(file,name), address, rem);
        } catch (UnixException x) {
            throw new FileSystemException(file.getPathForExceptionMessage(),
                    null, "Error writing extended attribute '" + name + "': " +
                    x.getMessage());
        } finally {
            close(fd);
        }
    }

    @SuppressWarnings("removal")
    @Override
    public void delete(String name) throws IOException {
        if (System.getSecurityManager() != null)
            checkAccess(file.getPathForPermissionCheck(), false, true);

        int fd = -1;
        try {
            fd = file.openForAttributeAccess(followLinks);
        } catch (UnixException x) {
            x.rethrowAsIOException(file);
        }
        try {
            fremovexattr(fd, nameAsBytes(file,name));
        } catch (UnixException x) {
            throw new FileSystemException(file.getPathForExceptionMessage(),
                null, "Unable to delete extended attribute '" + name + "': " + x.getMessage());
        } finally {
            close(fd);
        }
    }

    /**
     * Used by copyTo/moveTo to copy extended attributes from source to target.
     *
     * @param   ofd
     *          file descriptor for source file
     * @param   nfd
     *          file descriptor for target file
     */
    static void copyExtendedAttributes(int ofd, int nfd) {
        try {
            List<String> attrNames = list(ofd, MIN_LISTXATTR_BUF_SIZE);
            for (String name : attrNames) {
                try {
                    copyExtendedAttribute(ofd, Util.toBytes(name), nfd);
                } catch(UnixException ignore){
                    // ignore
                }
            }
        } catch (UnixException e) {
            // unable to get list of attributes
            return;
        }
    }

    private static void copyExtendedAttribute(int ofd, byte[] name, int nfd)
        throws UnixException
    {
        int size = fgetxattr(ofd, name, 0L, 0);
        NativeBuffer buffer = NativeBuffers.getNativeBuffer(size);
        try {
            long address = buffer.address();
            size = fgetxattr(ofd, name, address, size);
            fsetxattr(nfd, name, address, size);
        } finally {
            buffer.release();
        }
    }
}