/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package jdk.tools.jlink.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import jdk.tools.jlink.internal.Archive.Entry.EntryType;

/**
 * An Archive backed by a jar file.
 */
public abstract class JarArchive implements Archive {

    /**
     * An entry located in a jar file.
     */
    public class JarEntry extends Entry {

        private final long size;
        private final ZipEntry entry;
        private final ZipFile file;

        JarEntry(String path, String name, EntryType type, ZipFile file, ZipEntry entry) {
            super(JarArchive.this, path, name, type);
            this.entry = Objects.requireNonNull(entry);
            this.file = Objects.requireNonNull(file);
            size = entry.getSize();
        }

        /**
         * Returns the number of uncompressed bytes for this entry.
         */
        @Override
        public long size() {
            return size;
        }

        @Override
        public InputStream stream() throws IOException {
            return file.getInputStream(entry);
        }
    }

    private final Path file;
    private final String moduleName;
    private final Runtime.Version version;
    // currently processed JarFile
    private JarFile jarFile;

    protected JarArchive(String mn, Path file, Runtime.Version version) {
        Objects.requireNonNull(mn);
        Objects.requireNonNull(file);
        this.moduleName = mn;
        this.file = file;
        this.version = Objects.requireNonNull(version);
    }

    @Override
    public String moduleName() {
        return moduleName;
    }

    @Override
    public Path getPath() {
        return file;
    }

    @Override
    public Stream<Entry> entries() {
        try {
            if (jarFile == null) {
                open();
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return jarFile.versionedStream()
                .filter(je -> !je.isDirectory())
                .map(this::toEntry);
    }

    abstract EntryType toEntryType(String entryName);

    abstract String getFileName(String entryName);

    abstract Entry toEntry(ZipEntry ze);

    @Override
    public void close() throws IOException {
        if (jarFile != null) {
            jarFile.close();
        }
    }

    @Override
    public void open() throws IOException {
        if (jarFile != null) {
            jarFile.close();
        }
        jarFile = new JarFile(file.toFile(), true, ZipFile.OPEN_READ, version);
    }

    protected JarFile getJarFile() {
        return jarFile;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, moduleName, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JarArchive) {
            JarArchive other = (JarArchive)obj;
            return Objects.equals(file, other.file) &&
                   Objects.equals(moduleName, other.moduleName) &&
                   Objects.equals(version, other.version);
        }

        return false;
    }
}
