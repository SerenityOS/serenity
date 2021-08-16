/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardCopyOption.*;

/**
 * Base class for Path implementation of jrt file systems.
 *
 * @implNote This class needs to maintain JDK 8 source compatibility.
 *
 * It is used internally in the JDK to implement jimage/jrtfs access,
 * but also compiled and delivered as part of the jrtfs.jar to support access
 * to the jimage file provided by the shipped JDK by tools running on JDK 8.
 */
final class JrtPath implements Path {

    final JrtFileSystem jrtfs;
    private final String path;
    private volatile int[] offsets;

    JrtPath(JrtFileSystem jrtfs, String path) {
        this.jrtfs = jrtfs;
        this.path = normalize(path);
        this.resolved = null;
    }

    JrtPath(JrtFileSystem jrtfs, String path, boolean normalized) {
        this.jrtfs = jrtfs;
        this.path = normalized ? path : normalize(path);
        this.resolved = null;
    }

    final String getName() {
        return path;
    }

    @Override
    public final JrtPath getRoot() {
        if (this.isAbsolute()) {
            return jrtfs.getRootPath();
        } else {
            return null;
        }
    }

    @Override
    public final JrtPath getFileName() {
        if (path.isEmpty())
            return this;
        if (path.length() == 1 && path.charAt(0) == '/')
            return null;
        int off = path.lastIndexOf('/');
        if (off == -1)
            return this;
        return new JrtPath(jrtfs, path.substring(off + 1), true);
    }

    @Override
    public final JrtPath getParent() {
        initOffsets();
        int count = offsets.length;
        if (count == 0) {     // no elements so no parent
            return null;
        }
        int off = offsets[count - 1] - 1;
        if (off <= 0) {       // parent is root only (may be null)
            return getRoot();
        }
        return new JrtPath(jrtfs, path.substring(0, off));
    }

    @Override
    public final int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    @Override
    public final JrtPath getName(int index) {
        initOffsets();
        if (index < 0 || index >= offsets.length) {
            throw new IllegalArgumentException("index: " +
                index + ", offsets length: " + offsets.length);
        }
        int begin = offsets[index];
        int end;
        if (index == (offsets.length - 1)) {
            end = path.length();
        } else {
            end = offsets[index + 1];
        }
        return new JrtPath(jrtfs, path.substring(begin, end));
    }

    @Override
    public final JrtPath subpath(int beginIndex, int endIndex) {
        initOffsets();
        if (beginIndex < 0 || endIndex > offsets.length ||
            beginIndex >= endIndex) {
            throw new IllegalArgumentException(
                "beginIndex: " + beginIndex + ", endIndex: " + endIndex +
                ", offsets length: " + offsets.length);
        }
        // starting/ending offsets
        int begin = offsets[beginIndex];
        int end;
        if (endIndex == offsets.length) {
            end = path.length();
        } else {
            end = offsets[endIndex];
        }
        return new JrtPath(jrtfs, path.substring(begin, end));
    }

    @Override
    public final JrtPath toRealPath(LinkOption... options) throws IOException {
        return jrtfs.toRealPath(this, options);
    }

    @Override
    public final JrtPath toAbsolutePath() {
        if (isAbsolute())
            return this;
        return new JrtPath(jrtfs, "/" + path, true);
    }

    @Override
    public final URI toUri() {
        String p = toAbsolutePath().path;
        if (!p.startsWith("/modules") || p.contains("..")) {
            throw new IOError(new RuntimeException(p + " cannot be represented as URI"));
        }

        p = p.substring("/modules".length());
        if (p.isEmpty()) {
            p = "/";
        }
        return toUri(p);
    }

    private boolean equalsNameAt(JrtPath other, int index) {
        int mbegin = offsets[index];
        int mlen;
        if (index == (offsets.length - 1)) {
            mlen = path.length() - mbegin;
        } else {
            mlen = offsets[index + 1] - mbegin - 1;
        }
        int obegin = other.offsets[index];
        int olen;
        if (index == (other.offsets.length - 1)) {
            olen = other.path.length() - obegin;
        } else {
            olen = other.offsets[index + 1] - obegin - 1;
        }
        if (mlen != olen) {
            return false;
        }
        int n = 0;
        while (n < mlen) {
            if (path.charAt(mbegin + n) != other.path.charAt(obegin + n)) {
                return false;
            }
            n++;
        }
        return true;
    }

    @Override
    public final JrtPath relativize(Path other) {
        final JrtPath o = checkPath(other);
        if (o.equals(this)) {
            return new JrtPath(jrtfs, "", true);
        }
        if (path.isEmpty()) {
            return o;
        }
        if (jrtfs != o.jrtfs || isAbsolute() != o.isAbsolute()) {
            throw new IllegalArgumentException(
                "Incorrect filesystem or path: " + other);
        }
        final String tp = this.path;
        final String op = o.path;
        if (op.startsWith(tp)) {    // fast path
            int off = tp.length();
            if (op.charAt(off - 1) == '/')
                return new JrtPath(jrtfs, op.substring(off), true);
            if (op.charAt(off) == '/')
                return new JrtPath(jrtfs, op.substring(off + 1), true);
        }
        int mc = this.getNameCount();
        int oc = o.getNameCount();
        int n = Math.min(mc, oc);
        int i = 0;
        while (i < n) {
            if (!equalsNameAt(o, i)) {
                break;
            }
            i++;
        }
        int dotdots = mc - i;
        int len = dotdots * 3 - 1;
        if (i < oc) {
            len += (o.path.length() - o.offsets[i] + 1);
        }
        StringBuilder sb  = new StringBuilder(len);
        while (dotdots > 0) {
            sb.append("..");
            if (sb.length() < len) {  // no tailing slash at the end
                sb.append('/');
            }
            dotdots--;
        }
        if (i < oc) {
            sb.append(o.path, o.offsets[i], o.path.length());
        }
        return new JrtPath(jrtfs, sb.toString(), true);
    }

    @Override
    public JrtFileSystem getFileSystem() {
        return jrtfs;
    }

    @Override
    public final boolean isAbsolute() {
        return !path.isEmpty() && path.charAt(0) == '/';
    }

    @Override
    public final JrtPath resolve(Path other) {
        final JrtPath o = checkPath(other);
        if (this.path.isEmpty() || o.isAbsolute()) {
            return o;
        }
        if (o.path.isEmpty()) {
            return this;
        }
        StringBuilder sb = new StringBuilder(path.length() + o.path.length() + 1);
        sb.append(path);
        if (path.charAt(path.length() - 1) != '/')
            sb.append('/');
        sb.append(o.path);
        return new JrtPath(jrtfs, sb.toString(), true);
    }

    @Override
    public final Path resolveSibling(Path other) {
        Objects.requireNonNull(other, "other");
        Path parent = getParent();
        return (parent == null) ? other : parent.resolve(other);
    }

    @Override
    public final boolean startsWith(Path other) {
        if (!(Objects.requireNonNull(other) instanceof JrtPath))
            return false;
        final JrtPath o = (JrtPath)other;
        final String tp = this.path;
        final String op = o.path;
        if (isAbsolute() != o.isAbsolute() || !tp.startsWith(op)) {
            return false;
        }
        int off = op.length();
        if (off == 0) {
            return tp.isEmpty();
        }
        // check match is on name boundary
        return tp.length() == off || tp.charAt(off) == '/' ||
               off == 0 || op.charAt(off - 1) == '/';
    }

    @Override
    public final boolean endsWith(Path other) {
        if (!(Objects.requireNonNull(other) instanceof JrtPath))
            return false;
        final JrtPath o = (JrtPath)other;
        final JrtPath t = this;
        int olast = o.path.length() - 1;
        if (olast > 0 && o.path.charAt(olast) == '/') {
            olast--;
        }
        int last = t.path.length() - 1;
        if (last > 0 && t.path.charAt(last) == '/') {
            last--;
        }
        if (olast == -1) {  // o.path.length == 0
            return last == -1;
        }
        if ((o.isAbsolute() && (!t.isAbsolute() || olast != last))
            || last < olast) {
            return false;
        }
        for (; olast >= 0; olast--, last--) {
            if (o.path.charAt(olast) != t.path.charAt(last)) {
                return false;
            }
        }
        return o.path.charAt(olast + 1) == '/' ||
               last == -1 || t.path.charAt(last) == '/';
    }

    @Override
    public final JrtPath resolve(String other) {
        return resolve(getFileSystem().getPath(other));
    }

    @Override
    public final Path resolveSibling(String other) {
        return resolveSibling(getFileSystem().getPath(other));
    }

    @Override
    public final boolean startsWith(String other) {
        return startsWith(getFileSystem().getPath(other));
    }

    @Override
    public final boolean endsWith(String other) {
        return endsWith(getFileSystem().getPath(other));
    }

    @Override
    public final JrtPath normalize() {
        String res = getResolved();
        if (res == path) {  // no change
            return this;
        }
        return new JrtPath(jrtfs, res, true);
    }

    private JrtPath checkPath(Path path) {
        Objects.requireNonNull(path);
        if (!(path instanceof JrtPath))
            throw new ProviderMismatchException("path class: " +
                path.getClass());
        return (JrtPath) path;
    }

    // create offset list if not already created
    private void initOffsets() {
        if (this.offsets == null) {
            int len = path.length();
            // count names
            int count = 0;
            int off = 0;
            while (off < len) {
                char c = path.charAt(off++);
                if (c != '/') {
                    count++;
                    off = path.indexOf('/', off);
                    if (off == -1)
                        break;
                }
            }
            // populate offsets
            int[] offsets = new int[count];
            count = 0;
            off = 0;
            while (off < len) {
                char c = path.charAt(off);
                if (c == '/') {
                    off++;
                } else {
                    offsets[count++] = off++;
                    off = path.indexOf('/', off);
                    if (off == -1)
                        break;
                }
            }
            this.offsets = offsets;
        }
    }

    private volatile String resolved;

    final String getResolvedPath() {
        String r = resolved;
        if (r == null) {
            if (isAbsolute()) {
                r = getResolved();
            } else {
                r = toAbsolutePath().getResolvedPath();
            }
            resolved = r;
        }
        return r;
    }

    // removes redundant slashs, replace "\" to separator "/"
    // and check for invalid characters
    private static String normalize(String path) {
        int len = path.length();
        if (len == 0) {
            return path;
        }
        char prevC = 0;
        for (int i = 0; i < len; i++) {
            char c = path.charAt(i);
            if (c == '\\' || c == '\u0000') {
                return normalize(path, i);
            }
            if (c == '/' && prevC == '/') {
                return normalize(path, i - 1);
            }
            prevC = c;
        }
        if (prevC == '/' && len > 1) {
            return path.substring(0, len - 1);
        }
        return path;
    }

    private static String normalize(String path, int off) {
        int len = path.length();
        StringBuilder to = new StringBuilder(len);
        to.append(path, 0, off);
        char prevC = 0;
        while (off < len) {
            char c = path.charAt(off++);
            if (c == '\\') {
                c = '/';
            }
            if (c == '/' && prevC == '/') {
                continue;
            }
            if (c == '\u0000') {
                throw new InvalidPathException(path,
                        "Path: NUL character not allowed");
            }
            to.append(c);
            prevC = c;
        }
        len = to.length();
        if (len > 1 && to.charAt(len - 1) == '/') {
            to.deleteCharAt(len - 1);
        }
        return to.toString();
    }

    // Remove DotSlash(./) and resolve DotDot (..) components
    private String getResolved() {
        int length = path.length();
        if (length == 0 || (path.indexOf("./") == -1 && path.charAt(length - 1) != '.')) {
            return path;
        } else {
            return resolvePath();
        }
    }

    private String resolvePath() {
        int length = path.length();
        char[] to = new char[length];
        int nc = getNameCount();
        int[] lastM = new int[nc];
        int lastMOff = -1;
        int m = 0;
        for (int i = 0; i < nc; i++) {
            int n = offsets[i];
            int len = (i == offsets.length - 1) ? length - n
                                                : offsets[i + 1] - n - 1;
            if (len == 1 && path.charAt(n) == '.') {
                if (m == 0 && path.charAt(0) == '/')   // absolute path
                    to[m++] = '/';
                continue;
            }
            if (len == 2 && path.charAt(n) == '.' && path.charAt(n + 1) == '.') {
                if (lastMOff >= 0) {
                    m = lastM[lastMOff--];    // retreat
                    continue;
                }
                if (path.charAt(0) == '/') {  // "/../xyz" skip
                    if (m == 0)
                        to[m++] = '/';
                } else {                      // "../xyz" -> "../xyz"
                    if (m != 0 && to[m-1] != '/')
                        to[m++] = '/';
                    while (len-- > 0)
                        to[m++] = path.charAt(n++);
                }
                continue;
            }
            if (m == 0 && path.charAt(0) == '/' ||   // absolute path
                m != 0 && to[m-1] != '/') {   // not the first name
                to[m++] = '/';
            }
            lastM[++lastMOff] = m;
            while (len-- > 0)
                to[m++] = path.charAt(n++);
        }
        if (m > 1 && to[m - 1] == '/')
            m--;
        return (m == to.length) ? new String(to) : new String(to, 0, m);
    }

    @Override
    public final String toString() {
        return path;
    }

    @Override
    public final int hashCode() {
        return path.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof JrtPath &&
               this.path.equals(((JrtPath) obj).path);
    }

    @Override
    public final int compareTo(Path other) {
        final JrtPath o = checkPath(other);
        return path.compareTo(o.path);
    }

    @Override
    public final WatchKey register(
            WatchService watcher,
            WatchEvent.Kind<?>[] events,
            WatchEvent.Modifier... modifiers) {
        Objects.requireNonNull(watcher, "watcher");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(modifiers, "modifiers");
        throw new UnsupportedOperationException();
    }

    @Override
    public final WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) {
        return register(watcher, events, new WatchEvent.Modifier[0]);
    }

    @Override
    public final File toFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Iterator<Path> iterator() {
        return new Iterator<Path>() {
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

    // Helpers for JrtFileSystemProvider and JrtFileSystem

    final JrtPath readSymbolicLink() throws IOException {
        if (!jrtfs.isLink(this)) {
            throw new IOException("not a symbolic link");
        }
        return jrtfs.resolveLink(this);
    }

    final boolean isHidden() {
        return false;
    }

    final void createDirectory(FileAttribute<?>... attrs)
            throws IOException {
        jrtfs.createDirectory(this, attrs);
    }

    final InputStream newInputStream(OpenOption... options) throws IOException {
        if (options.length > 0) {
            for (OpenOption opt : options) {
                if (opt != READ) {
                    throw new UnsupportedOperationException("'" + opt + "' not allowed");
                }
            }
        }
        return jrtfs.newInputStream(this);
    }

    final DirectoryStream<Path> newDirectoryStream(Filter<? super Path> filter)
            throws IOException {
        return new JrtDirectoryStream(this, filter);
    }

    final void delete() throws IOException {
        jrtfs.deleteFile(this, true);
    }

    final void deleteIfExists() throws IOException {
        jrtfs.deleteFile(this, false);
    }

    final JrtFileAttributes getAttributes(LinkOption... options) throws IOException {
        JrtFileAttributes zfas = jrtfs.getFileAttributes(this, options);
        if (zfas == null) {
            throw new NoSuchFileException(toString());
        }
        return zfas;
    }

    final void setAttribute(String attribute, Object value, LinkOption... options)
            throws IOException {
        JrtFileAttributeView.setAttribute(this, attribute, value);
    }

    final Map<String, Object> readAttributes(String attributes, LinkOption... options)
            throws IOException {
        return JrtFileAttributeView.readAttributes(this, attributes, options);
    }

    final void setTimes(FileTime mtime, FileTime atime, FileTime ctime)
            throws IOException {
        jrtfs.setTimes(this, mtime, atime, ctime);
    }

    final FileStore getFileStore() throws IOException {
        // each JrtFileSystem only has one root (as requested for now)
        if (exists()) {
            return jrtfs.getFileStore(this);
        }
        throw new NoSuchFileException(path);
    }

    final boolean isSameFile(Path other) throws IOException {
        if (this == other || this.equals(other)) {
            return true;
        }
        if (other == null || this.getFileSystem() != other.getFileSystem()) {
            return false;
        }
        this.checkAccess();
        JrtPath o = (JrtPath) other;
        o.checkAccess();
        return this.getResolvedPath().equals(o.getResolvedPath()) ||
               jrtfs.isSameFile(this, o);
    }

    final SeekableByteChannel newByteChannel(Set<? extends OpenOption> options,
                                             FileAttribute<?>... attrs)
            throws IOException
    {
        return jrtfs.newByteChannel(this, options, attrs);
    }

    final FileChannel newFileChannel(Set<? extends OpenOption> options,
            FileAttribute<?>... attrs)
            throws IOException {
        return jrtfs.newFileChannel(this, options, attrs);
    }

    final void checkAccess(AccessMode... modes) throws IOException {
        if (modes.length == 0) {    // check if the path exists
            jrtfs.checkNode(this);  // no need to follow link. the "link" node
                                    // is built from real node under "/module"
        } else {
            boolean w = false;
            for (AccessMode mode : modes) {
                switch (mode) {
                    case READ:
                        break;
                    case WRITE:
                        w = true;
                        break;
                    case EXECUTE:
                        throw new AccessDeniedException(toString());
                    default:
                        throw new UnsupportedOperationException();
                }
            }
            jrtfs.checkNode(this);
            if (w && jrtfs.isReadOnly()) {
                throw new AccessDeniedException(toString());
            }
        }
    }

    final boolean exists() {
        try {
            return jrtfs.exists(this);
        } catch (IOException x) {}
        return false;
    }

    final OutputStream newOutputStream(OpenOption... options) throws IOException {
        if (options.length == 0) {
            return jrtfs.newOutputStream(this, CREATE_NEW, WRITE);
        }
        return jrtfs.newOutputStream(this, options);
    }

    final void move(JrtPath target, CopyOption... options) throws IOException {
        if (this.jrtfs == target.jrtfs) {
            jrtfs.copyFile(true, this, target, options);
        } else {
            copyToTarget(target, options);
            delete();
        }
    }

    final void copy(JrtPath target, CopyOption... options) throws IOException {
        if (this.jrtfs == target.jrtfs) {
            jrtfs.copyFile(false, this, target, options);
        } else {
            copyToTarget(target, options);
        }
    }

    private void copyToTarget(JrtPath target, CopyOption... options)
            throws IOException {
        boolean replaceExisting = false;
        boolean copyAttrs = false;
        for (CopyOption opt : options) {
            if (opt == REPLACE_EXISTING) {
                replaceExisting = true;
            } else if (opt == COPY_ATTRIBUTES) {
                copyAttrs = true;
            }
        }
        // attributes of source file
        BasicFileAttributes jrtfas = getAttributes();
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
        if (exists) {
            throw new FileAlreadyExistsException(target.toString());
        }
        if (jrtfas.isDirectory()) {
            // create directory or file
            target.createDirectory();
        } else {
            try (InputStream is = jrtfs.newInputStream(this);
                 OutputStream os = target.newOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    os.write(buf, 0, n);
                }
            }
        }
        if (copyAttrs) {
            BasicFileAttributeView view =
                Files.getFileAttributeView(target, BasicFileAttributeView.class);
            try {
                view.setTimes(jrtfas.lastModifiedTime(),
                              jrtfas.lastAccessTime(),
                              jrtfas.creationTime());
            } catch (IOException x) {
                try {
                    target.delete();  // rollback?
                } catch (IOException ignore) {}
                throw x;
            }
        }
    }

    // adopted from sun.nio.fs.UnixUriUtils
    private static URI toUri(String str) {
        char[] path = str.toCharArray();
        assert path[0] == '/';
        StringBuilder sb = new StringBuilder();
        sb.append(path[0]);
        for (int i = 1; i < path.length; i++) {
            char c = (char)(path[i] & 0xff);
            if (match(c, L_PATH, H_PATH)) {
                sb.append(c);
            } else {
                sb.append('%');
                sb.append(hexDigits[(c >> 4) & 0x0f]);
                sb.append(hexDigits[(c) & 0x0f]);
            }
        }

        try {
            return new URI("jrt:" + sb.toString());
        } catch (URISyntaxException x) {
            throw new AssertionError(x);  // should not happen
        }
    }

    // The following is copied from java.net.URI

    // Compute the low-order mask for the characters in the given string
    private static long lowMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c < 64)
                m |= (1L << c);
        }
        return m;
    }

    // Compute the high-order mask for the characters in the given string
    private static long highMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if ((c >= 64) && (c < 128))
                m |= (1L << (c - 64));
        }
        return m;
    }

    // Compute a low-order mask for the characters
    // between first and last, inclusive
    private static long lowMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 63), 0);
        int l = Math.max(Math.min(last, 63), 0);
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Compute a high-order mask for the characters
    // between first and last, inclusive
    private static long highMask(char first, char last) {
        long m = 0;
        int f = Math.max(Math.min(first, 127), 64) - 64;
        int l = Math.max(Math.min(last, 127), 64) - 64;
        for (int i = f; i <= l; i++)
            m |= 1L << i;
        return m;
    }

    // Tell whether the given character is permitted by the given mask pair
    private static boolean match(char c, long lowMask, long highMask) {
        if (c < 64)
            return ((1L << c) & lowMask) != 0;
        if (c < 128)
            return ((1L << (c - 64)) & highMask) != 0;
        return false;
    }

    // digit    = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
    //            "8" | "9"
    private static final long L_DIGIT = lowMask('0', '9');
    private static final long H_DIGIT = 0L;

    // upalpha  = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" |
    //            "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" |
    //            "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z"
    private static final long L_UPALPHA = 0L;
    private static final long H_UPALPHA = highMask('A', 'Z');

    // lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" |
    //            "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" |
    //            "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
    private static final long L_LOWALPHA = 0L;
    private static final long H_LOWALPHA = highMask('a', 'z');

    // alpha         = lowalpha | upalpha
    private static final long L_ALPHA = L_LOWALPHA | L_UPALPHA;
    private static final long H_ALPHA = H_LOWALPHA | H_UPALPHA;

    // alphanum      = alpha | digit
    private static final long L_ALPHANUM = L_DIGIT | L_ALPHA;
    private static final long H_ALPHANUM = H_DIGIT | H_ALPHA;

    // mark          = "-" | "_" | "." | "!" | "~" | "*" | "'" |
    //                 "(" | ")"
    private static final long L_MARK = lowMask("-_.!~*'()");
    private static final long H_MARK = highMask("-_.!~*'()");

    // unreserved    = alphanum | mark
    private static final long L_UNRESERVED = L_ALPHANUM | L_MARK;
    private static final long H_UNRESERVED = H_ALPHANUM | H_MARK;

    // pchar         = unreserved | escaped |
    //                 ":" | "@" | "&" | "=" | "+" | "$" | ","
    private static final long L_PCHAR
        = L_UNRESERVED | lowMask(":@&=+$,");
    private static final long H_PCHAR
        = H_UNRESERVED | highMask(":@&=+$,");

   // All valid path characters
   private static final long L_PATH = L_PCHAR | lowMask(";/");
   private static final long H_PATH = H_PCHAR | highMask(";/");

   private static final char[] hexDigits = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
}
