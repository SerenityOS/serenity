/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * File system provider for jrt file systems. Conditionally creates jrt fs on
 * .jimage file or exploded modules directory of underlying JDK.
 *
 * @implNote This class needs to maintain JDK 8 source compatibility.
 *
 * It is used internally in the JDK to implement jimage/jrtfs access,
 * but also compiled and delivered as part of the jrtfs.jar to support access
 * to the jimage file provided by the shipped JDK by tools running on JDK 8.
 */
public final class JrtFileSystemProvider extends FileSystemProvider {

    private volatile FileSystem theFileSystem;

    public JrtFileSystemProvider() {
    }

    @Override
    public String getScheme() {
        return "jrt";
    }

    /**
     * Need RuntimePermission "accessSystemModules" to create or get jrt:/
     */
    private void checkPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            RuntimePermission perm = new RuntimePermission("accessSystemModules");
            sm.checkPermission(perm);
        }
    }

    private void checkUri(URI uri) {
        if (!uri.getScheme().equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("URI does not match this provider");
        }
        if (uri.getAuthority() != null) {
            throw new IllegalArgumentException("Authority component present");
        }
        if (uri.getPath() == null) {
            throw new IllegalArgumentException("Path component is undefined");
        }
        if (!uri.getPath().equals("/")) {
            throw new IllegalArgumentException("Path component should be '/'");
        }
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException("Query component present");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("Fragment component present");
        }
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env)
            throws IOException {
        Objects.requireNonNull(env);
        checkPermission();
        checkUri(uri);
        if (env.containsKey("java.home")) {
            return newFileSystem((String)env.get("java.home"), uri, env);
        } else {
            return new JrtFileSystem(this, env);
        }
    }

    private static final String JRT_FS_JAR = "jrt-fs.jar";
    private FileSystem newFileSystem(String targetHome, URI uri, Map<String, ?> env)
            throws IOException {
        Objects.requireNonNull(targetHome);
        Path jrtfs = FileSystems.getDefault().getPath(targetHome, "lib", JRT_FS_JAR);
        if (Files.notExists(jrtfs)) {
            throw new IOException(jrtfs.toString() + " not exist");
        }
        Map<String,?> newEnv = new HashMap<>(env);
        newEnv.remove("java.home");
        ClassLoader cl = newJrtFsLoader(jrtfs);
        try {
            Class<?> c = Class.forName(JrtFileSystemProvider.class.getName(), false, cl);
            @SuppressWarnings("deprecation")
            Object tmp = c.newInstance();
            return ((FileSystemProvider)tmp).newFileSystem(uri, newEnv);
        } catch (ClassNotFoundException |
                 IllegalAccessException |
                 InstantiationException e) {
            throw new IOException(e);
        }
    }

    private static class JrtFsLoader extends URLClassLoader {
        JrtFsLoader(URL[] urls) {
            super(urls);
        }
        @Override
        protected Class<?> loadClass(String cn, boolean resolve)
                throws ClassNotFoundException
        {
            Class<?> c = findLoadedClass(cn);
            if (c == null) {
                URL u = findResource(cn.replace('.', '/') + ".class");
                if (u != null) {
                    c = findClass(cn);
                } else {
                    return super.loadClass(cn, resolve);
                }
            }
            if (resolve)
                resolveClass(c);
            return c;
        }
    }

    @SuppressWarnings("removal")
    private static URLClassLoader newJrtFsLoader(Path jrtfs) {
        final URL url;
        try {
            url = jrtfs.toUri().toURL();
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException(mue);
        }

        final URL[] urls = new URL[] { url };
        return AccessController.doPrivileged(
                new PrivilegedAction<URLClassLoader>() {
                    @Override
                    public URLClassLoader run() {
                        return new JrtFsLoader(urls);
                    }
                }
        );
    }

    @Override
    public Path getPath(URI uri) {
        checkPermission();
        if (!uri.getScheme().equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("URI does not match this provider");
        }
        if (uri.getAuthority() != null) {
            throw new IllegalArgumentException("Authority component present");
        }
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException("Query component present");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("Fragment component present");
        }
        String path = uri.getPath();
        if (path == null || path.charAt(0) != '/' || path.contains("..")) {
            throw new IllegalArgumentException("Invalid path component");
        }

        return getTheFileSystem().getPath("/modules" + path);
    }

    private FileSystem getTheFileSystem() {
        checkPermission();
        FileSystem fs = this.theFileSystem;
        if (fs == null) {
            synchronized (this) {
                fs = this.theFileSystem;
                if (fs == null) {
                    try {
                        this.theFileSystem = fs = new JrtFileSystem(this, null);
                    } catch (IOException ioe) {
                        throw new InternalError(ioe);
                    }
                }
            }
        }
        return fs;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        checkPermission();
        checkUri(uri);
        return getTheFileSystem();
    }

    // Checks that the given file is a JrtPath
    static final JrtPath toJrtPath(Path path) {
        Objects.requireNonNull(path, "path");
        if (!(path instanceof JrtPath)) {
            throw new ProviderMismatchException();
        }
        return (JrtPath) path;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        toJrtPath(path).checkAccess(modes);
    }

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        return toJrtPath(link).readSymbolicLink();
    }

    @Override
    public void copy(Path src, Path target, CopyOption... options)
            throws IOException {
        toJrtPath(src).copy(toJrtPath(target), options);
    }

    @Override
    public void createDirectory(Path path, FileAttribute<?>... attrs)
            throws IOException {
        toJrtPath(path).createDirectory(attrs);
    }

    @Override
    public final void delete(Path path) throws IOException {
        toJrtPath(path).delete();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V
            getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return JrtFileAttributeView.get(toJrtPath(path), type, options);
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return toJrtPath(path).getFileStore();
    }

    @Override
    public boolean isHidden(Path path) {
        return toJrtPath(path).isHidden();
    }

    @Override
    public boolean isSameFile(Path path, Path other) throws IOException {
        return toJrtPath(path).isSameFile(other);
    }

    @Override
    public void move(Path src, Path target, CopyOption... options)
            throws IOException {
        toJrtPath(src).move(toJrtPath(target), options);
    }

    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path,
            Set<? extends OpenOption> options,
            ExecutorService exec,
            FileAttribute<?>... attrs)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
            Set<? extends OpenOption> options,
            FileAttribute<?>... attrs)
            throws IOException {
        return toJrtPath(path).newByteChannel(options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(
            Path path, Filter<? super Path> filter) throws IOException {
        return toJrtPath(path).newDirectoryStream(filter);
    }

    @Override
    public FileChannel newFileChannel(Path path,
            Set<? extends OpenOption> options,
            FileAttribute<?>... attrs)
            throws IOException {
        return toJrtPath(path).newFileChannel(options, attrs);
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options)
            throws IOException {
        return toJrtPath(path).newInputStream(options);
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options)
            throws IOException {
        return toJrtPath(path).newOutputStream(options);
    }

    @Override
    @SuppressWarnings("unchecked") // Cast to A
    public <A extends BasicFileAttributes> A
            readAttributes(Path path, Class<A> type, LinkOption... options)
            throws IOException {
        if (type == BasicFileAttributes.class || type == JrtFileAttributes.class) {
            return (A) toJrtPath(path).getAttributes(options);
        }
        return null;
    }

    @Override
    public Map<String, Object>
            readAttributes(Path path, String attribute, LinkOption... options)
            throws IOException {
        return toJrtPath(path).readAttributes(attribute, options);
    }

    @Override
    public void setAttribute(Path path, String attribute,
            Object value, LinkOption... options)
            throws IOException {
        toJrtPath(path).setAttribute(attribute, value, options);
    }
}
