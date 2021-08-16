/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * An Archive backed by a directory.
 */
public class DirArchive implements Archive {

    /**
     * A File located in a Directory.
     */
    private class FileEntry extends Archive.Entry {

        private final long size;
        private final Path path;

        FileEntry(Path path, String name) {
            super(DirArchive.this, getPathName(path), name,
                  Archive.Entry.EntryType.CLASS_OR_RESOURCE);
            this.path = path;
            try {
                size = Files.size(path);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        /**
         * Returns the number of bytes of this file.
         */
        @Override
        public long size() {
            return size;
        }

        @Override
        public InputStream stream() throws IOException {
            InputStream stream = Files.newInputStream(path);
            open.add(stream);
            return stream;
        }
    }

    private final Path dirPath;
    private final String moduleName;
    private final List<InputStream> open = new ArrayList<>();
    private final int chop;
    private final Consumer<String> log;
    private static final Consumer<String> noopConsumer = (String t) -> {
    };

    public DirArchive(Path dirPath, String moduleName) {
        this(dirPath, moduleName, noopConsumer);
    }

    public DirArchive(Path dirPath, String moduleName, Consumer<String> log) {
        Objects.requireNonNull(dirPath);
        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException(dirPath + " is not a directory");
        }
        chop = dirPath.toString().length() + 1;
        this.moduleName = Objects.requireNonNull(moduleName);
        this.dirPath = dirPath;
        this.log = log;
    }

    @Override
    public String moduleName() {
        return moduleName;
    }

    @Override
    public Path getPath() {
        return dirPath;
    }

    @Override
    public Stream<Entry> entries() {
        try {
            return Files.walk(dirPath).map(this::toEntry).filter(n -> n != null);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Archive.Entry toEntry(Path p) {
        if (Files.isDirectory(p)) {
            return null;
        }
        String name = getPathName(p).substring(chop);
        log.accept(moduleName + "/" + name);
        return new FileEntry(p, name);
    }

    @Override
    public void close() throws IOException {
        IOException e = null;
        for (InputStream stream : open) {
            try {
                stream.close();
            } catch (IOException ex) {
                if (e == null) {
                    e = ex;
                } else {
                    e.addSuppressed(ex);
                }
            }
        }
        if (e != null) {
            throw e;
        }
    }

    @Override
    public void open() throws IOException {
        // NOOP
    }

    private static String getPathName(Path path) {
        return path.toString().replace(File.separatorChar, '/');
    }

    @Override
    public int hashCode() {
        return Objects.hash(dirPath, moduleName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DirArchive) {
            DirArchive other = (DirArchive)obj;
            return Objects.equals(dirPath, other.dirPath) &&
                   Objects.equals(moduleName, other.moduleName);
        }

        return false;
    }
}
