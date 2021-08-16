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

package com.sun.tools.javac.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Objects;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.file.RelativePath.RelativeFile;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;


/**
 *  Implementation of JavaFileObject using java.nio.file API.
 *
 *  <p>PathFileObjects are, for the most part, straightforward wrappers around
 *  immutable absolute Path objects. Different subtypes are used to provide
 *  specialized implementations of "inferBinaryName" and "getName" that capture
 *  additional information available at the time the object is created.
 *
 *  <p>In general, {@link JavaFileManager#isSameFile} should be used to
 *  determine whether two file objects refer to the same file on disk.
 *  PathFileObject also supports the standard {@code equals} and {@code hashCode}
 *  methods, primarily for convenience when working with collections.
 *  All of these operations delegate to the equivalent operations on the
 *  underlying Path object.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class PathFileObject implements JavaFileObject {
    private static final FileSystem defaultFileSystem = FileSystems.getDefault();
    private static final boolean isMacOS = System.getProperty("os.name", "").contains("OS X");

    protected final BaseFileManager fileManager;
    protected final Path path;
    private boolean hasParents;

    /**
     * Create a PathFileObject for a file within a directory, such that the
     * binary name can be inferred from the relationship to an enclosing directory.
     *
     * The binary name is derived from {@code relativePath}.
     * The name is derived from the composition of {@code userPackageRootDir}
     * and {@code relativePath}.
     *
     * @param fileManager the file manager creating this file object
     * @param path the absolute path referred to by this file object
     * @param userPackageRootDir the path of the directory containing the
     *          root of the package hierarchy
     * @param relativePath the path of this file relative to {@code userPackageRootDir}
     */
    static PathFileObject forDirectoryPath(BaseFileManager fileManager, Path path,
            Path userPackageRootDir, RelativePath relativePath) {
        return new DirectoryFileObject(fileManager, path, userPackageRootDir, relativePath);
    }

    private static class DirectoryFileObject extends PathFileObject {
        private final Path userPackageRootDir;
        private final RelativePath relativePath;

        private DirectoryFileObject(BaseFileManager fileManager, Path path,
                Path userPackageRootDir, RelativePath relativePath) {
            super(fileManager, path);
            this.userPackageRootDir = Objects.requireNonNull(userPackageRootDir);
            this.relativePath = relativePath;
        }

        @Override @DefinedBy(Api.COMPILER)
        public String getName() {
            return relativePath.resolveAgainst(userPackageRootDir).toString();
        }

        @Override
        public String inferBinaryName(Iterable<? extends Path> paths) {
            return toBinaryName(relativePath);
        }

        @Override
        public String toString() {
            return "DirectoryFileObject[" + userPackageRootDir + ":" + relativePath.path + "]";
        }

        @Override
        PathFileObject getSibling(String baseName) {
            return new DirectoryFileObject(fileManager,
                    path.resolveSibling(baseName),
                    userPackageRootDir,
                    new RelativeFile(relativePath.dirname(), baseName)
            );
        }
    }

    /**
     * Create a PathFileObject for a file in a file system such as a jar file,
     * such that the binary name can be inferred from its position within the
     * file system.
     *
     * The binary name is derived from {@code path}.
     * The name is derived from the composition of {@code userJarPath}
     * and {@code path}.
     *
     * @param fileManager the file manager creating this file object
     * @param path the path referred to by this file object
     * @param userJarPath the path of the jar file containing the file system.
     * @return the file object
     */
    public static PathFileObject forJarPath(BaseFileManager fileManager,
            Path path, Path userJarPath) {
        return new JarFileObject(fileManager, path, userJarPath);
    }

    private static class JarFileObject extends PathFileObject {
        private final Path userJarPath;

        private JarFileObject(BaseFileManager fileManager, Path path, Path userJarPath) {
            super(fileManager, path);
            this.userJarPath = userJarPath;
        }

        @Override @DefinedBy(Api.COMPILER)
        public String getName() {
            // The use of ( ) to delimit the entry name is not ideal
            // but it does match earlier behavior
            return userJarPath + "(" + path + ")";
        }

        @Override
        public String inferBinaryName(Iterable<? extends Path> paths) {
            Path root = path.getFileSystem().getRootDirectories().iterator().next();
            return toBinaryName(root.relativize(path));
        }

        @Override @DefinedBy(Api.COMPILER)
        public URI toUri() {
            // Work around bug JDK-8134451:
            // path.toUri() returns double-encoded URIs, that cannot be opened by URLConnection
            return createJarUri(userJarPath, path.toString());
        }

        @Override
        public String toString() {
            return "JarFileObject[" + userJarPath + ":" + path + "]";
        }

        @Override
        PathFileObject getSibling(String baseName) {
            return new JarFileObject(fileManager,
                    path.resolveSibling(baseName),
                    userJarPath
            );
        }

        private static URI createJarUri(Path jarFile, String entryName) {
            URI jarURI = jarFile.toUri().normalize();
            String separator = entryName.startsWith("/") ? "!" : "!/";
            try {
                // The jar URI convention appears to be not to re-encode the jarURI
                return new URI("jar:" + jarURI + separator + entryName);
            } catch (URISyntaxException e) {
                throw new CannotCreateUriError(jarURI + separator + entryName, e);
            }
        }
    }

    /**
     * Create a PathFileObject for a file in a modular file system, such as jrt:,
     * such that the binary name can be inferred from its position within the
     * filesystem.
     *
     * The binary name is derived from {@code path}, ignoring the first two
     * elements of the name (which are "modules" and a module name).
     * The name is derived from {@code path}.
     *
     * @param fileManager the file manager creating this file object
     * @param path the path referred to by this file object
     * @return the file object
     */
    public static PathFileObject forJRTPath(BaseFileManager fileManager,
            final Path path) {
        return new JRTFileObject(fileManager, path);
    }

    private static class JRTFileObject extends PathFileObject {
        // private final Path javaHome;
        private JRTFileObject(BaseFileManager fileManager, Path path) {
            super(fileManager, path);
        }

        @Override @DefinedBy(Api.COMPILER)
        public String getName() {
            return path.toString();
        }

        @Override
        public String inferBinaryName(Iterable<? extends Path> paths) {
            // use subpath to ignore the leading /modules/MODULE-NAME
            return toBinaryName(path.subpath(2, path.getNameCount()));
        }

        @Override
        public String toString() {
            return "JRTFileObject[" + path + "]";
        }

        @Override
        PathFileObject getSibling(String baseName) {
            return new JRTFileObject(fileManager,
                    path.resolveSibling(baseName)
            );
        }
    }

    /**
     * Create a PathFileObject for a file whose binary name must be inferred
     * from its position on a search path.
     *
     * The binary name is inferred by finding an enclosing directory in
     * the sequence of paths associated with the location given to
     * {@link JavaFileManager#inferBinaryName).
     * The name is derived from {@code userPath}.
     *
     * @param fileManager the file manager creating this file object
     * @param path the path referred to by this file object
     * @param userPath the "user-friendly" name for this path.
     */
    static PathFileObject forSimplePath(BaseFileManager fileManager,
            Path path, Path userPath) {
        return new SimpleFileObject(fileManager, path, userPath);
    }

    private static class SimpleFileObject extends PathFileObject {
        private final Path userPath;
        private SimpleFileObject(BaseFileManager fileManager, Path path, Path userPath) {
            super(fileManager, path);
            this.userPath = userPath;
        }

        @Override @DefinedBy(Api.COMPILER)
        public String getName() {
            return userPath.toString();
        }

        @Override @DefinedBy(Api.COMPILER)
        public String getShortName() {
            return userPath.getFileName().toString();
        }

        @Override
        public String inferBinaryName(Iterable<? extends Path> paths) {
            Path absPath = path.toAbsolutePath();
            for (Path p: paths) {
                Path ap = p.toAbsolutePath();
                if (absPath.startsWith(ap)) {
                    try {
                        Path rp = ap.relativize(absPath);
                        if (rp != null) // maybe null if absPath same as ap
                            return toBinaryName(rp);
                    } catch (IllegalArgumentException e) {
                        // ignore this p if cannot relativize path to p
                    }
                }
            }
            return null;
        }

        @Override @DefinedBy(Api.COMPILER)
        public Kind getKind() {
            return BaseFileManager.getKind(userPath);
        }

        @Override @DefinedBy(Api.COMPILER)
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return isPathNameCompatible(userPath, simpleName, kind);
        }

        @Override @DefinedBy(Api.COMPILER)
        public URI toUri() {
            return userPath.toUri().normalize();
        }

        @Override
        PathFileObject getSibling(String baseName) {
            return new SimpleFileObject(fileManager,
                    path.resolveSibling(baseName),
                    userPath.resolveSibling(baseName)
            );
        }
    }

    /**
     * Create a PathFileObject, for a specified path, in the context of
     * a given file manager.
     *
     * In general, this path should be an
     * {@link Path#toAbsolutePath absolute path}, if not a
     * {@link Path#toRealPath} real path.
     * It will be used as the basis of {@code equals}, {@code hashCode}
     * and {@code isSameFile} methods on this file object.
     *
     * A PathFileObject should also have a "friendly name" per the
     * specification for {@link FileObject#getName}. The friendly name
     * is provided by the various subtypes of {@code PathFileObject}.
     *
     * @param fileManager the file manager creating this file object
     * @param path the path contained in this file object.
     */
    protected PathFileObject(BaseFileManager fileManager, Path path) {
        this.fileManager = Objects.requireNonNull(fileManager);
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("directories not supported");
        }
        this.path = path;
    }

    /**
     * See {@link JavacFileManager#inferBinaryName}.
     */
    abstract String inferBinaryName(Iterable<? extends Path> paths);

    /**
     * Return the file object for a sibling file with a given file name.
     * See {@link JavacFileManager#getFileForOutput} and
     * {@link JavacFileManager#getJavaFileForOutput}.
     */
    abstract PathFileObject getSibling(String basename);

    /**
     * Return the Path for this object.
     * @return the Path for this object.
     * @see StandardJavaFileManager#asPath
     */
    public Path getPath() {
        return path;
    }

    /**
     * The short name is used when generating raw diagnostics.
     * @return the last component of the path
     */
    public String getShortName() {
        return path.getFileName().toString();
    }

    @Override @DefinedBy(Api.COMPILER)
    public Kind getKind() {
        return BaseFileManager.getKind(path);
    }

    @Override @DefinedBy(Api.COMPILER)
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return isPathNameCompatible(path, simpleName, kind);
    }

    protected boolean isPathNameCompatible(Path p, String simpleName, Kind kind) {
        Objects.requireNonNull(simpleName);
        Objects.requireNonNull(kind);

        if (kind == Kind.OTHER && BaseFileManager.getKind(p) != kind) {
            return false;
        }

        String sn = simpleName + kind.extension;
        String pn = p.getFileName().toString();
        if (pn.equals(sn)) {
            return true;
        }

        if (p.getFileSystem() == defaultFileSystem) {
            if (isMacOS) {
                if (Normalizer.isNormalized(pn, Normalizer.Form.NFD)
                        && Normalizer.isNormalized(sn, Normalizer.Form.NFC)) {
                    // On Mac OS X it is quite possible to have the file name and the
                    // given simple name normalized in different ways.
                    // In that case we have to normalize file name to the
                    // Normal Form Composed (NFC).
                    String normName = Normalizer.normalize(pn, Normalizer.Form.NFC);
                    if (normName.equals(sn)) {
                        return true;
                    }
                }
            }

            if (pn.equalsIgnoreCase(sn)) {
                try {
                    // allow for Windows
                    return p.toRealPath(LinkOption.NOFOLLOW_LINKS).getFileName().toString().equals(sn);
                } catch (IOException e) {
                }
            }
        }

        return false;
    }

    @Override @DefinedBy(Api.COMPILER)
    public NestingKind getNestingKind() {
        return null;
    }

    @Override @DefinedBy(Api.COMPILER)
    public Modifier getAccessLevel() {
        return null;
    }

    @Override @DefinedBy(Api.COMPILER)
    public URI toUri() {
        return path.toUri();
    }

    @Override @DefinedBy(Api.COMPILER)
    public InputStream openInputStream() throws IOException {
        fileManager.updateLastUsedTime();
        return Files.newInputStream(path);
    }

    @Override @DefinedBy(Api.COMPILER)
    public OutputStream openOutputStream() throws IOException {
        fileManager.updateLastUsedTime();
        fileManager.flushCache(this);
        ensureParentDirectoriesExist();
        return Files.newOutputStream(path);
    }

    @Override @DefinedBy(Api.COMPILER)
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        CharsetDecoder decoder = fileManager.getDecoder(fileManager.getEncodingName(), ignoreEncodingErrors);
        return new InputStreamReader(openInputStream(), decoder);
    }

    @Override @DefinedBy(Api.COMPILER)
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        CharBuffer cb = fileManager.getCachedContent(this);
        if (cb == null) {
            try (InputStream in = openInputStream()) {
                ByteBuffer bb = fileManager.makeByteBuffer(in);
                JavaFileObject prev = fileManager.log.useSource(this);
                try {
                    cb = fileManager.decode(bb, ignoreEncodingErrors);
                } finally {
                    fileManager.log.useSource(prev);
                }
                fileManager.recycleByteBuffer(bb);
                if (!ignoreEncodingErrors) {
                    fileManager.cache(this, cb);
                }
            }
        }
        return cb;
    }

    @Override @DefinedBy(Api.COMPILER)
    public Writer openWriter() throws IOException {
        fileManager.updateLastUsedTime();
        fileManager.flushCache(this);
        ensureParentDirectoriesExist();
        return new OutputStreamWriter(Files.newOutputStream(path), fileManager.getEncodingName());
    }

    @Override @DefinedBy(Api.COMPILER)
    public long getLastModified() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override @DefinedBy(Api.COMPILER)
    public boolean delete() {
        try {
            Files.delete(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    boolean isSameFile(PathFileObject other) {
        // By construction, the "path" field should be canonical in all likely, supported scenarios.
        // (Any exceptions would involve the use of symlinks within a package hierarchy.)
        // Therefore, it is sufficient to check that the paths are .equals.
        return path.equals(other.path);
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof PathFileObject pathFileObject && path.equals(pathFileObject.path));
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + path + "]";
    }

    private void ensureParentDirectoriesExist() throws IOException {
        if (!hasParents) {
            Path parent = path.getParent();
            if (parent != null && !Files.isDirectory(parent)) {
                try {
                    Files.createDirectories(parent);
                } catch (IOException e) {
                    throw new IOException("could not create parent directories", e);
                }
            }
            hasParents = true;
        }
    }

    protected static String toBinaryName(RelativePath relativePath) {
        return toBinaryName(relativePath.path, "/");
    }

    protected static String toBinaryName(Path relativePath) {
        return toBinaryName(relativePath.toString(),
                relativePath.getFileSystem().getSeparator());
    }

    private static String toBinaryName(String relativePath, String sep) {
        return removeExtension(relativePath).replace(sep, ".");
    }

    private static String removeExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        return (lastDot == -1 ? fileName : fileName.substring(0, lastDot));
    }

    /**
     * Return the last component of a presumed hierarchical URI.
     * From the scheme specific part of the URI, it returns the substring
     * after the last "/" if any, or everything if no "/" is found.
     * @param fo the file object
     * @return the simple name of the file object
     */
    public static String getSimpleName(FileObject fo) {
        URI uri = fo.toUri();
        String s = uri.getSchemeSpecificPart();
        return s.substring(s.lastIndexOf("/") + 1); // safe when / not found

    }

    /** Used when URLSyntaxException is thrown unexpectedly during
     *  implementations of FileObject.toURI(). */
    public static class CannotCreateUriError extends Error {
        private static final long serialVersionUID = 9101708840997613546L;
        public CannotCreateUriError(String value, Throwable cause) {
            super(value, cause);
        }
    }
}
