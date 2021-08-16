/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.tools.DocumentationTool;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import com.sun.tools.javac.util.Assert;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;

/**
 * Implementation of DocFileFactory using a {@link StandardJavaFileManager}.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 */
class StandardDocFileFactory extends DocFileFactory {
    private final StandardJavaFileManager fileManager;
    private Path destDir;

    public StandardDocFileFactory(BaseConfiguration configuration) {
        super(configuration);
        fileManager = (StandardJavaFileManager) configuration.getFileManager();
    }

    @Override
    public void setDestDir(String destDirName) throws SimpleDocletException {
        if (destDir != null)
            throw new AssertionError("destDir already initialized: " + destDir);

        if (!destDirName.isEmpty()
                || !fileManager.hasLocation(DocumentationTool.Location.DOCUMENTATION_OUTPUT)) {
            try {
                String dirName = destDirName.isEmpty() ? "." : destDirName;
                Path dir = Paths.get(dirName);
                fileManager.setLocationFromPaths(DocumentationTool.Location.DOCUMENTATION_OUTPUT, Arrays.asList(dir));
            } catch (IOException e) {
                // generic IOException from file manager, setting location, e.g. file not a directory
                String message = configuration.getDocResources().getText("doclet.error.initializing.dest.dir", e);
                throw new SimpleDocletException(message, e);
            }
        }

        destDir = fileManager.getLocationAsPaths(DocumentationTool.Location.DOCUMENTATION_OUTPUT).iterator().next();
    }

    private Path getDestDir() {
        Objects.requireNonNull(destDir, "destDir not initialized");
        return destDir;
    }

    @Override
    public DocFile createFileForDirectory(String file) {
        return new StandardDocFile(Paths.get(file));
    }

    @Override
    public DocFile createFileForInput(String file) {
        return new StandardDocFile(Paths.get(file));
    }

    @Override
    public DocFile createFileForInput(Path file) {
        return new StandardDocFile(file);
    }

    @Override
    public DocFile createFileForOutput(DocPath path) {
        return new StandardDocFile(DocumentationTool.Location.DOCUMENTATION_OUTPUT, path);
    }

    @Override
    Iterable<DocFile> list(Location location, DocPath path) {
        Location l = ((location == StandardLocation.SOURCE_PATH)
                && !fileManager.hasLocation(StandardLocation.SOURCE_PATH))
                ? StandardLocation.CLASS_PATH
                : location;

        Set<DocFile> files = new LinkedHashSet<>();
        for (Path f: fileManager.getLocationAsPaths(l)) {
            if (Files.isDirectory(f)) {
                f = f.resolve(path.getPath());
                if (Files.exists(f))
                    files.add(new StandardDocFile(f));
            }
        }
        return files;
    }

    private static Path newFile(Path dir, String path) {
        return (dir == null) ? Paths.get(path) : dir.resolve(path);
    }

    class StandardDocFile extends DocFile {
        private final Path file;

        /** Create a StandardDocFile for a given file. */
        StandardDocFile(Path file) {
            this.file = file;
        }

        /** Create a StandardDocFile for a given location and relative path. */
        StandardDocFile(Location location, DocPath path) {
            super(location, path);
            Assert.check(location == DocumentationTool.Location.DOCUMENTATION_OUTPUT);
            this.file = newFile(getDestDir(), path.getPath());
        }

        @Override
        public FileObject getFileObject()  {
            return getJavaFileObjectForInput(file);
        }

        /**
         * Open an input stream for the file.
         *
         * @throws DocFileIOException if there is a problem while opening stream
         */
        @Override
        public InputStream openInputStream() throws DocFileIOException {
            try {
                JavaFileObject fo = getJavaFileObjectForInput(file);
                return new BufferedInputStream(fo.openInputStream());
            } catch (IOException e) {
                throw new DocFileIOException(this, DocFileIOException.Mode.READ, e);
            }
        }

        /**
         * Open an output stream for the file.
         * The file must have been created with a location of
         * {@link DocumentationTool.Location#DOCUMENTATION_OUTPUT} and a corresponding relative path.
         *
         * @throws DocFileIOException if there is a problem while opening stream
         */
        @Override
        public OutputStream openOutputStream() throws DocFileIOException {
            if (location != DocumentationTool.Location.DOCUMENTATION_OUTPUT)
                throw new IllegalStateException();

            try {
                OutputStream out = getFileObjectForOutput(path).openOutputStream();
                return new BufferedOutputStream(out);
            } catch (IOException e) {
                throw new DocFileIOException(this, DocFileIOException.Mode.WRITE, e);
            }
        }

        /**
         * Open an writer for the file, using the encoding (if any) given in the
         * doclet configuration.
         * The file must have been created with a location of
         * {@link DocumentationTool.Location#DOCUMENTATION_OUTPUT} and a corresponding relative path.
         *
         * @throws DocFileIOException if there is a problem while opening stream
         * @throws UnsupportedEncodingException if the configured encoding is not supported
         */
        @Override
        public Writer openWriter() throws DocFileIOException, UnsupportedEncodingException {
            if (location != DocumentationTool.Location.DOCUMENTATION_OUTPUT)
                throw new IllegalStateException();

            try {
                OutputStream out = getFileObjectForOutput(path).openOutputStream();
                String docencoding = configuration.getOptions().docEncoding();
                return new BufferedWriter(new OutputStreamWriter(out, docencoding));
            } catch (IOException e) {
                throw new DocFileIOException(this, DocFileIOException.Mode.WRITE, e);
            }
        }

        /** Return true if the file can be read. */
        @Override
        public boolean canRead() {
            return Files.isReadable(file);
        }

        /** Return true if the file can be written. */
        @Override
        public boolean canWrite() {
            return Files.isWritable(file);
        }

        /** Return true if the file exists. */
        @Override
        public boolean exists() {
            return Files.exists(file);
        }

        /** Return the base name (last component) of the file name. */
        @Override
        public String getName() {
            return file.getFileName().toString();
        }

        /** Return the file system path for this file. */
        @Override
        public String getPath() {
            return file.toString();
        }

        /** Return true is file has an absolute path name. */
        @Override
        public boolean isAbsolute() {
            return file.isAbsolute();
        }

        /** Return true is file identifies a directory. */
        @Override
        public boolean isDirectory() {
            return Files.isDirectory(file);
        }

        /** Return true is file identifies a file. */
        @Override
        public boolean isFile() {
            return Files.isRegularFile(file);
        }

        /** Return true if this file is the same as another. */
        @Override
        public boolean isSameFile(DocFile other) {
            if (!(other instanceof StandardDocFile))
                return false;

            try {
                return Files.isSameFile(file, ((StandardDocFile) other).file);
            } catch (IOException e) {
                return false;
            }
        }

        /** If the file is a directory, list its contents. */
        @Override
        public Iterable<DocFile> list() throws DocFileIOException {
            List<DocFile> files = new ArrayList<>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(file)) {
                for (Path f: ds) {
                    files.add(new StandardDocFile(f));
                }
            } catch (IOException e) {
                throw new DocFileIOException(this, DocFileIOException.Mode.READ, e);
            }
            return files;
        }

        /** Create the file as a directory, including any parent directories. */
        @Override
        public boolean mkdirs() {
            try {
                Files.createDirectories(file);
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        /**
         * Derive a new file by resolving a relative path against this file.
         * The new file will inherit the configuration and location of this file
         * If this file has a path set, the new file will have a corresponding
         * new path.
         */
        @Override
        public DocFile resolve(DocPath p) {
            return resolve(p.getPath());
        }

        /**
         * Derive a new file by resolving a relative path against this file.
         * The new file will inherit the configuration and location of this file
         * If this file has a path set, the new file will have a corresponding
         * new path.
         */
        @Override
        public DocFile resolve(String p) {
            if (location == null && path == null) {
                return new StandardDocFile(file.resolve(p));
            } else {
                return new StandardDocFile(location, path.resolve(p));
            }
        }

        /**
         * Resolve a relative file against the given output location.
         * @param locn Currently, only
         * {@link DocumentationTool.Location#DOCUMENTATION_OUTPUT} is supported.
         */
        @Override
        public DocFile resolveAgainst(Location locn) {
            if (locn != DocumentationTool.Location.DOCUMENTATION_OUTPUT)
                throw new IllegalArgumentException();
            return new StandardDocFile(getDestDir().resolve(file));
        }

        /** Return a string to identify the contents of this object,
         * for debugging purposes.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("StandardDocFile[");
            if (location != null)
                sb.append("locn:").append(location).append(",");
            if (path != null)
                sb.append("path:").append(path.getPath()).append(",");
            sb.append("file:").append(file);
            sb.append("]");
            return sb.toString();
        }

        private JavaFileObject getJavaFileObjectForInput(Path file) {
            return fileManager.getJavaFileObjects(file).iterator().next();
        }

        private FileObject getFileObjectForOutput(DocPath path) throws IOException {
            // break the path into a package-part and the rest, by finding
            // the position of the last '/' before an invalid character for a
            // package name, such as the "." before an extension or the "-"
            // in filenames like package-summary.html, doc-files or src-html.
            String p = path.getPath();
            int lastSep = -1;
            for (int i = 0; i < p.length(); i++) {
                char ch = p.charAt(i);
                if (ch == '/') {
                    lastSep = i;
                } else if (i == lastSep + 1 && !Character.isJavaIdentifierStart(ch)
                        || !Character.isJavaIdentifierPart(ch)) {
                    break;
                }
            }
            String pkg = (lastSep == -1) ? "" : p.substring(0, lastSep);
            String rest = p.substring(lastSep + 1);
            return fileManager.getFileForOutput(location, pkg, rest, null);
        }
    }
}
