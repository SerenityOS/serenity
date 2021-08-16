/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.nio.zipfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * @author Xueming Shen, Rajendra Gutupalli,Jaya Hangal
 */
final class ZipPath implements Path {

    private final ZipFileSystem zfs;
    private final byte[] path;
    private volatile int[] offsets;
    private int hashcode = 0;  // cached hashcode (created lazily)

    ZipPath(ZipFileSystem zfs, byte[] path) {
        this(zfs, path, false);
    }

    ZipPath(ZipFileSystem zfs, byte[] path, boolean normalized) {
        this.zfs = zfs;
        if (normalized) {
            this.path = path;
        } else {
            if (zfs.zc.isUTF8()) {
                this.path = normalize(path);
            } else {    // see normalize(String);
                this.path = normalize(zfs.getString(path));
            }
        }
    }

    ZipPath(ZipFileSystem zfs, String path) {
        this.zfs = zfs;
        this.path = normalize(path);
    }

    @Override
    public ZipPath getRoot() {
        if (this.isAbsolute())
            return zfs.getRootDir();
        else
            return null;
    }

    @Override
    public ZipPath getFileName() {
        int off = path.length;
        if (off == 0 || off == 1 && path[0] == '/')
            return null;
        while (--off >= 0 && path[off] != '/') {}
        if (off < 0)
            return this;
        off++;
        byte[] result = new byte[path.length - off];
        System.arraycopy(path, off, result, 0, result.length);
        return new ZipPath(getFileSystem(), result, true);
    }

    @Override
    public ZipPath getParent() {
        int off = path.length;
        if (off == 0 || off == 1 && path[0] == '/')
            return null;
        while (--off >= 0 && path[off] != '/') {}
        if (off <= 0)
            return getRoot();
        byte[] result = new byte[off];
        System.arraycopy(path, 0, result, 0, off);
        return new ZipPath(getFileSystem(), result, true);
    }

    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    @Override
    public ZipPath getName(int index) {
        initOffsets();
        if (index < 0 || index >= offsets.length)
            throw new IllegalArgumentException();
        int begin = offsets[index];
        int len;
        if (index == (offsets.length-1))
            len = path.length - begin;
        else
            len = offsets[index+1] - begin - 1;
        // construct result
        byte[] result = new byte[len];
        System.arraycopy(path, begin, result, 0, len);
        return new ZipPath(zfs, result);
    }

    @Override
    public ZipPath subpath(int beginIndex, int endIndex) {
        initOffsets();
        if (beginIndex < 0 ||
            beginIndex >=  offsets.length ||
            endIndex > offsets.length ||
            beginIndex >= endIndex)
            throw new IllegalArgumentException();

        // starting offset and length
        int begin = offsets[beginIndex];
        int len;
        if (endIndex == offsets.length)
            len = path.length - begin;
        else
            len = offsets[endIndex] - begin - 1;
        // construct result
        byte[] result = new byte[len];
        System.arraycopy(path, begin, result, 0, len);
        return new ZipPath(zfs, result);
    }

    @Override
    public ZipPath toRealPath(LinkOption... options) throws IOException {
        ZipPath realPath;
        byte[] resolved = getResolvedPath();
        // resolved is always absolute and normalized
        if (resolved == path) {
            realPath = this;
        } else {
            realPath = new ZipPath(zfs, resolved, true);
            realPath.resolved = resolved;
        }
        realPath.checkAccess();
        return realPath;
    }

    boolean isHidden() {
        return false;
    }

    @Override
    public ZipPath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        } else {
            // add '/' before the existing path
            byte[] tmp = new byte[path.length + 1];
            System.arraycopy(path, 0, tmp, 1, path.length);
            tmp[0] = '/';
            return new ZipPath(zfs, tmp, true);  // normalized
        }
    }

    @Override
    public URI toUri() {
        try {
            return new URI("jar",
                           decodeUri(zfs.getZipFile().toUri().toString()) +
                           "!" +
                           zfs.getString(toAbsolutePath().path),
                           null);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private boolean equalsNameAt(ZipPath other, int index) {
        int mbegin = offsets[index];
        int mlen;
        if (index == (offsets.length-1))
            mlen = path.length - mbegin;
        else
            mlen = offsets[index + 1] - mbegin - 1;
        int obegin = other.offsets[index];
        int olen;
        if (index == (other.offsets.length - 1))
            olen = other.path.length - obegin;
        else
            olen = other.offsets[index + 1] - obegin - 1;
        if (mlen != olen)
            return false;
        int n = 0;
        while(n < mlen) {
            if (path[mbegin + n] != other.path[obegin + n])
                return false;
            n++;
        }
        return true;
    }

    @Override
    public Path relativize(Path other) {
        final ZipPath o = checkPath(other);
        if (o.equals(this))
            return new ZipPath(zfs, new byte[0], true);
        if (this.path.length == 0)
            return o;
        if (this.zfs != o.zfs || this.isAbsolute() != o.isAbsolute())
            throw new IllegalArgumentException();
        if (this.path.length == 1 && this.path[0] == '/')
            return new ZipPath(zfs,
                               Arrays.copyOfRange(o.path, 1, o.path.length),
                               true);
        int mc = this.getNameCount();
        int oc = o.getNameCount();
        int n = Math.min(mc, oc);
        int i = 0;
        while (i < n) {
            if (!equalsNameAt(o, i))
                break;
            i++;
        }
        int dotdots = mc - i;
        int len = dotdots * 3 - 1;
        if (i < oc)
            len += (o.path.length - o.offsets[i] + 1);
        byte[] result = new byte[len];

        int pos = 0;
        while (dotdots > 0) {
            result[pos++] = (byte)'.';
            result[pos++] = (byte)'.';
            if (pos < len)       // no tailing slash at the end
                result[pos++] = (byte)'/';
            dotdots--;
        }
        if (i < oc)
            System.arraycopy(o.path, o.offsets[i],
                             result, pos,
                             o.path.length - o.offsets[i]);
        return new ZipPath(zfs, result);
    }

    @Override
    public ZipFileSystem getFileSystem() {
        return zfs;
    }

    @Override
    public boolean isAbsolute() {
        return path.length > 0 && path[0] == '/';
    }

    @Override
    public ZipPath resolve(Path other) {
        ZipPath o = checkPath(other);
        if (o.path.length == 0)
            return this;
        if (o.isAbsolute() || this.path.length == 0)
            return o;
        return resolve(o.path);
    }

    // opath is normalized, just concat
    private ZipPath resolve(byte[] opath) {
        byte[] resolved;
        byte[] tpath = this.path;
        int tlen = tpath.length;
        int olen = opath.length;
        if (path[tlen - 1] == '/') {
            resolved = new byte[tlen + olen];
            System.arraycopy(tpath, 0, resolved, 0, tlen);
            System.arraycopy(opath, 0, resolved, tlen, olen);
        } else {
            resolved = new byte[tlen + 1 + olen];
            System.arraycopy(tpath, 0, resolved, 0, tlen);
            resolved[tlen] = '/';
            System.arraycopy(opath, 0, resolved, tlen + 1, olen);
        }
        return new ZipPath(zfs, resolved, true);
    }

    @Override
    public Path resolveSibling(Path other) {
        Objects.requireNonNull(other, "other");
        Path parent = getParent();
        return (parent == null) ? other : parent.resolve(other);
    }

    @Override
    public boolean startsWith(Path other) {
        Objects.requireNonNull(other, "other");
        if (!(other instanceof ZipPath))
            return false;
        final ZipPath o = (ZipPath)other;
        if (o.isAbsolute() != this.isAbsolute() ||
            o.path.length > this.path.length)
            return false;
        int olast = o.path.length;
        for (int i = 0; i < olast; i++) {
            if (o.path[i] != this.path[i])
                return false;
        }
        olast--;
        return o.path.length == this.path.length ||
               o.path[olast] == '/' ||
               this.path[olast + 1] == '/';
    }

    @Override
    public boolean endsWith(Path other) {
        Objects.requireNonNull(other, "other");
        if (!(other instanceof ZipPath))
            return false;
        final ZipPath o = (ZipPath)other;
        int olast = o.path.length - 1;
        if (olast > 0 && o.path[olast] == '/')
            olast--;
        int last = this.path.length - 1;
        if (last > 0 && this.path[last] == '/')
            last--;
        if (olast == -1)    // o.path.length == 0
            return last == -1;
        if ((o.isAbsolute() &&(!this.isAbsolute() || olast != last)) ||
            (last < olast))
            return false;
        for (; olast >= 0; olast--, last--) {
            if (o.path[olast] != this.path[last])
                return false;
        }
        return o.path[olast + 1] == '/' ||
               last == -1 || this.path[last] == '/';
    }

    @Override
    public ZipPath resolve(String other) {
        byte[] opath = normalize(other);
        if (opath.length == 0)
            return this;
        if (opath[0] == '/' || this.path.length == 0)
            return new ZipPath(zfs, opath, true);
        return resolve(opath);
    }

    @Override
    public final Path resolveSibling(String other) {
        return resolveSibling(zfs.getPath(other));
    }

    @Override
    public final boolean startsWith(String other) {
        return startsWith(zfs.getPath(other));
    }

    @Override
    public final boolean endsWith(String other) {
        return endsWith(zfs.getPath(other));
    }

    @Override
    public Path normalize() {
        byte[] resolved = getResolved();
        if (resolved == path)    // no change
            return this;
        return new ZipPath(zfs, resolved, true);
    }

    private ZipPath checkPath(Path path) {
        Objects.requireNonNull(path, "path");
        if (!(path instanceof ZipPath))
            throw new ProviderMismatchException();
        return (ZipPath) path;
    }

    // create offset list if not already created
    private void initOffsets() {
        if (offsets == null) {
            int count, index;
            // count names
            count = 0;
            index = 0;
            if (path.length == 0) {
                // empty path has one name
                count = 1;
            } else {
                while (index < path.length) {
                    byte c = path[index++];
                    if (c != '/') {
                        count++;
                        while (index < path.length && path[index] != '/')
                             index++;
                    }
                }
            }
            // populate offsets
            int[] result = new int[count];
            count = 0;
            index = 0;
            while (index < path.length) {
                byte c = path[index];
                if (c == '/') {
                    index++;
                } else {
                    result[count++] = index++;
                    while (index < path.length && path[index] != '/')
                        index++;
                }
            }
            synchronized (this) {
                if (offsets == null)
                    offsets = result;
            }
        }
    }

    // resolved path for locating zip entry inside the zip file,
    // the result path does not contain ./ and .. components
    private volatile byte[] resolved = null;
    byte[] getResolvedPath() {
        byte[] r = resolved;
        if (r == null) {
            if (isAbsolute())
                r = getResolved();
            else
                r = toAbsolutePath().getResolvedPath();
            resolved = r;
        }
        return resolved;
    }

    // removes redundant slashs, replace "\" to zip separator "/"
    // and check for invalid characters
    private byte[] normalize(byte[] path) {
        int len = path.length;
        if (len == 0)
            return path;
        byte prevC = 0;
        for (int i = 0; i < len; i++) {
            byte c = path[i];
            if (c == '\\' || c == '\u0000')
                return normalize(path, i);
            if (c == (byte)'/' && prevC == '/')
                return normalize(path, i - 1);
            prevC = c;
        }
        if (len > 1 && prevC == '/') {
            return Arrays.copyOf(path, len - 1);
        }
        return path;
    }

    private byte[] normalize(byte[] path, int off) {
        byte[] to = new byte[path.length];
        int n = 0;
        while (n < off) {
            to[n] = path[n];
            n++;
        }
        int m = n;
        byte prevC = 0;
        while (n < path.length) {
            byte c = path[n++];
            if (c == (byte)'\\')
                c = (byte)'/';
            if (c == (byte)'/' && prevC == (byte)'/')
                continue;
            if (c == '\u0000')
                throw new InvalidPathException(zfs.getString(path),
                                               "Path: nul character not allowed");
            to[m++] = c;
            prevC = c;
        }
        if (m > 1 && to[m - 1] == '/')
            m--;
        return (m == to.length)? to : Arrays.copyOf(to, m);
    }

    // if zfs is NOT in utf8, normalize the path as "String"
    // to avoid incorrectly normalizing byte '0x5c' (as '\')
    // to '/'.
    private byte[] normalize(String path) {
        if (zfs.zc.isUTF8())
            return normalize(zfs.getBytes(path));
        int len = path.length();
        if (len == 0)
            return new byte[0];
        char prevC = 0;
        for (int i = 0; i < len; i++) {
            char c = path.charAt(i);
            if (c == '\\' || c == '\u0000')
                return normalize(path, i, len);
            if (c == '/' && prevC == '/')
                return normalize(path, i - 1, len);
            prevC = c;
        }
        if (len > 1 && prevC == '/')
            path = path.substring(0, len - 1);
        return zfs.getBytes(path);
    }

    private byte[] normalize(String path, int off, int len) {
        StringBuilder to = new StringBuilder(len);
        to.append(path, 0, off);
        char prevC = 0;
        while (off < len) {
            char c = path.charAt(off++);
            if (c == '\\')
                c = '/';
            if (c == '/' && prevC == '/')
                continue;
            if (c == '\u0000')
                throw new InvalidPathException(path,
                                               "Path: nul character not allowed");
            to.append(c);
            prevC = c;
        }
        len = to.length();
        if (len > 1 && prevC == '/')
            to.delete(len -1, len);
        return zfs.getBytes(to.toString());
    }

    // Remove DotSlash(./) and resolve DotDot (..) components
    private byte[] getResolved() {
        for (int i = 0; i < path.length; i++) {
            if (path[i] == (byte)'.' &&
                (i + 1 == path.length || path[i + 1] == '/')) {
                return resolve0();
            }
        }
        return path;
    }

    // TBD: performance, avoid initOffsets
    private byte[] resolve0() {
        byte[] to = new byte[path.length];
        int nc = getNameCount();
        int[] lastM = new int[nc];
        int lastMOff = -1;
        int m = 0;
        for (int i = 0; i < nc; i++) {
            int n = offsets[i];
            int len = (i == offsets.length - 1)?
                      (path.length - n):(offsets[i + 1] - n - 1);
            if (len == 1 && path[n] == (byte)'.') {
                if (m == 0 && path[0] == '/')   // absolute path
                    to[m++] = '/';
                continue;
            }
            if (len == 2 && path[n] == '.' && path[n + 1] == '.') {
                if (lastMOff >= 0) {
                    m = lastM[lastMOff--];  // retreat
                    continue;
                }
                if (path[0] == '/') {  // "/../xyz" skip
                    if (m == 0)
                        to[m++] = '/';
                } else {               // "../xyz" -> "../xyz"
                    if (m != 0 && to[m-1] != '/')
                        to[m++] = '/';
                    while (len-- > 0)
                        to[m++] = path[n++];
                }
                continue;
            }
            if (m == 0 && path[0] == '/' ||   // absolute path
                m != 0 && to[m-1] != '/') {   // not the first name
                to[m++] = '/';
            }
            lastM[++lastMOff] = m;
            while (len-- > 0)
                to[m++] = path[n++];
        }
        if (m > 1 && to[m - 1] == '/')
            m--;
        return (m == to.length)? to : Arrays.copyOf(to, m);
    }

    @Override
    public String toString() {
        return zfs.getString(path);
    }

    @Override
    public int hashCode() {
        int h = hashcode;
        if (h == 0)
            hashcode = h = Arrays.hashCode(path);
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ZipPath &&
               this.zfs == ((ZipPath) obj).zfs &&
               compareTo((Path) obj) == 0;
    }

    @Override
    public int compareTo(Path other) {
        final ZipPath o = checkPath(other);
        int len1 = this.path.length;
        int len2 = o.path.length;

        int n = Math.min(len1, len2);

        int k = 0;
        while (k < n) {
            int c1 = this.path[k] & 0xff;
            int c2 = o.path[k] & 0xff;
            if (c1 != c2)
                return c1 - c2;
            k++;
        }
        return len1 - len2;
    }

    public WatchKey register(
            WatchService watcher,
            WatchEvent.Kind<?>[] events,
            WatchEvent.Modifier... modifiers) {
        if (watcher == null || events == null || modifiers == null) {
            throw new NullPointerException();
        }
        // watcher must be associated with a different provider
        throw new ProviderMismatchException();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) {
        return register(watcher, events, new WatchEvent.Modifier[0]);
    }

    @Override
    public final File toFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Path> iterator() {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return (i < getNameCount());
            }

            @Override
            public Path next() {
                if (i < getNameCount()) {
                    Path result = getName(i);
                    i++;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new ReadOnlyFileSystemException();
            }
        };
    }

    /////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked") // Cast to V
    <V extends FileAttributeView> V getFileAttributeView(Class<V> type) {
        if (type == null)
            throw new NullPointerException();
        if (type == BasicFileAttributeView.class)
            return (V)new ZipFileAttributeView(this, false);
        if (type == ZipFileAttributeView.class)
            return (V)new ZipFileAttributeView(this, true);
        if (zfs.supportPosix) {
            if (type == PosixFileAttributeView.class)
                return (V)new ZipPosixFileAttributeView(this, false);
            if (type == FileOwnerAttributeView.class)
                return (V)new ZipPosixFileAttributeView(this,true);
        }
        throw new UnsupportedOperationException("view <" + type + "> is not supported");
    }

    private ZipFileAttributeView getFileAttributeView(String type) {
        if (type == null)
            throw new NullPointerException();
        if ("basic".equals(type))
            return new ZipFileAttributeView(this, false);
        if ("zip".equals(type))
            return new ZipFileAttributeView(this, true);
        if (zfs.supportPosix) {
            if ("posix".equals(type))
                return new ZipPosixFileAttributeView(this, false);
            if ("owner".equals(type))
                return new ZipPosixFileAttributeView(this, true);
        }
        throw new UnsupportedOperationException("view <" + type + "> is not supported");
    }

    void createDirectory(FileAttribute<?>... attrs)
        throws IOException
    {
        zfs.createDirectory(getResolvedPath(), attrs);
    }

    InputStream newInputStream(OpenOption... options) throws IOException
    {
        if (options.length > 0) {
            for (OpenOption opt : options) {
                if (opt != READ)
                    throw new UnsupportedOperationException("'" + opt + "' not allowed");
            }
        }
        return zfs.newInputStream(getResolvedPath());
    }

    DirectoryStream<Path> newDirectoryStream(Filter<? super Path> filter)
        throws IOException
    {
        return new ZipDirectoryStream(this, filter);
    }

    void delete() throws IOException {
        zfs.deleteFile(getResolvedPath(), true);
    }

    private void deleteIfExists() throws IOException {
        zfs.deleteFile(getResolvedPath(), false);
    }

    ZipFileAttributes readAttributes() throws IOException {
        ZipFileAttributes zfas = zfs.getFileAttributes(getResolvedPath());
        if (zfas == null)
            throw new NoSuchFileException(toString());
        return zfas;
    }

    @SuppressWarnings("unchecked") // Cast to A
    <A extends BasicFileAttributes> A readAttributes(Class<A> type) throws IOException {
        // unconditionally support BasicFileAttributes and ZipFileAttributes
        if (type == BasicFileAttributes.class || type == ZipFileAttributes.class) {
            return (A)readAttributes();
        }

        // support PosixFileAttributes when activated
        if (type == PosixFileAttributes.class && zfs.supportPosix) {
            return (A)readAttributes();
        }

        throw new UnsupportedOperationException("Attributes of type " +
            type.getName() + " not supported");
    }

    void setAttribute(String attribute, Object value, LinkOption... options)
        throws IOException
    {
        String type;
        String attr;
        int colonPos = attribute.indexOf(':');
        if (colonPos == -1) {
            type = "basic";
            attr = attribute;
        } else {
            type = attribute.substring(0, colonPos++);
            attr = attribute.substring(colonPos);
        }
        getFileAttributeView(type).setAttribute(attr, value);
    }

    void setTimes(FileTime mtime, FileTime atime, FileTime ctime)
        throws IOException
    {
        zfs.setTimes(getResolvedPath(), mtime, atime, ctime);
    }

    void setOwner(UserPrincipal owner) throws IOException {
        zfs.setOwner(getResolvedPath(), owner);
    }

    void setPermissions(Set<PosixFilePermission> perms)
        throws IOException
    {
        zfs.setPermissions(getResolvedPath(), perms);
    }

    void setGroup(GroupPrincipal group) throws IOException {
        zfs.setGroup(getResolvedPath(), group);
    }

    Map<String, Object> readAttributes(String attributes, LinkOption... options)
        throws IOException
    {
        String view;
        String attrs;
        int colonPos = attributes.indexOf(':');
        if (colonPos == -1) {
            view = "basic";
            attrs = attributes;
        } else {
            view = attributes.substring(0, colonPos++);
            attrs = attributes.substring(colonPos);
        }
        return getFileAttributeView(view).readAttributes(attrs);
    }

    FileStore getFileStore() throws IOException {
        // each ZipFileSystem only has one root (as requested for now)
        if (exists())
            return zfs.getFileStore(this);
        throw new NoSuchFileException(zfs.getString(path));
    }

    boolean isSameFile(Path other) throws IOException {
        if (this.equals(other))
            return true;
        if (other == null ||
            this.getFileSystem() != other.getFileSystem())
            return false;
        this.checkAccess();
        ((ZipPath)other).checkAccess();
        return Arrays.equals(this.getResolvedPath(),
                             ((ZipPath)other).getResolvedPath());
    }

    SeekableByteChannel newByteChannel(Set<? extends OpenOption> options,
                                       FileAttribute<?>... attrs)
        throws IOException
    {
        return zfs.newByteChannel(getResolvedPath(), options, attrs);
    }


    FileChannel newFileChannel(Set<? extends OpenOption> options,
                               FileAttribute<?>... attrs)
        throws IOException
    {
        return zfs.newFileChannel(getResolvedPath(), options, attrs);
    }

    void checkAccess(AccessMode... modes) throws IOException {
        boolean w = false;
        boolean x = false;
        for (AccessMode mode : modes) {
            switch (mode) {
                case READ:
                    break;
                case WRITE:
                    w = true;
                    break;
                case EXECUTE:
                    x = true;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
        zfs.checkAccess(getResolvedPath());
        if ((w && zfs.isReadOnly()) || x) {
            throw new AccessDeniedException(toString());
        }
    }

    private boolean exists() {
        return zfs.exists(getResolvedPath());
    }

    OutputStream newOutputStream(OpenOption... options) throws IOException
    {
        if (options.length == 0)
            return zfs.newOutputStream(getResolvedPath(),
                                       CREATE, TRUNCATE_EXISTING, WRITE);
        return zfs.newOutputStream(getResolvedPath(), options);
    }

    void move(ZipPath target, CopyOption... options)
        throws IOException
    {
        if (Files.isSameFile(this.zfs.getZipFile(), target.zfs.getZipFile()))
        {
            zfs.copyFile(true,
                         getResolvedPath(), target.getResolvedPath(),
                         options);
        } else {
            copyToTarget(target, options);
            delete();
        }
    }

    void copy(ZipPath target, CopyOption... options)
        throws IOException
    {
        if (Files.isSameFile(this.zfs.getZipFile(), target.zfs.getZipFile()))
            zfs.copyFile(false,
                         getResolvedPath(), target.getResolvedPath(),
                         options);
        else
            copyToTarget(target, options);
    }

    private void copyToTarget(ZipPath target, CopyOption... options)
        throws IOException
    {
        boolean replaceExisting = false;
        boolean copyAttrs = false;
        for (CopyOption opt : options) {
            if (opt == REPLACE_EXISTING)
                replaceExisting = true;
            else if (opt == COPY_ATTRIBUTES)
                copyAttrs = true;
        }
        // attributes of source file
        ZipFileAttributes zfas = readAttributes();
        // check if target exists
        boolean exists;
        if (replaceExisting) {
            try {
                target.deleteIfExists();
                exists = false;
            } catch (DirectoryNotEmptyException x) {
                exists = true;
            }
        } else {
            exists = target.exists();
        }
        if (exists)
            throw new FileAlreadyExistsException(target.toString());

        if (zfas.isDirectory()) {
            // create directory or file
            target.createDirectory();
        } else {
            try (InputStream is = zfs.newInputStream(getResolvedPath());
                 OutputStream os = target.newOutputStream())
            {
                is.transferTo(os);
            }
        }
        if (copyAttrs) {
            ZipFileAttributeView view =
                target.getFileAttributeView(ZipFileAttributeView.class);
            try {
                view.setTimes(zfas.lastModifiedTime(),
                              zfas.lastAccessTime(),
                              zfas.creationTime());
                // copy permissions
                view.setPermissions(zfas.storedPermissions().orElse(null));
            } catch (IOException x) {
                // rollback?
                try {
                    target.delete();
                } catch (IOException ignore) { }
                throw x;
            }
        }
    }

    private static int decode(char c) {
        if ((c >= '0') && (c <= '9'))
            return c - '0';
        if ((c >= 'a') && (c <= 'f'))
            return c - 'a' + 10;
        if ((c >= 'A') && (c <= 'F'))
            return c - 'A' + 10;
        assert false;
        return -1;
    }

    // to avoid double escape
    private static String decodeUri(String s) {
        if (s == null)
            return null;
        int n = s.length();
        if (n == 0)
            return s;
        if (s.indexOf('%') < 0)
            return s;

        StringBuilder sb = new StringBuilder(n);
        byte[] bb = new byte[n];
        boolean betweenBrackets = false;

        for (int i = 0; i < n;) {
            char c = s.charAt(i);
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = false;
            }
            if (c != '%' || betweenBrackets ) {
                sb.append(c);
                i++;
                continue;
            }
            int nb = 0;
            while (c == '%') {
                assert (n - i >= 2);
                bb[nb++] = (byte)(((decode(s.charAt(++i)) & 0xf) << 4) |
                                  (decode(s.charAt(++i)) & 0xf));
                if (++i >= n) {
                    break;
                }
                c = s.charAt(i);
            }
            sb.append(new String(bb, 0, nb, UTF_8));
        }
        return sb.toString();
    }
}
