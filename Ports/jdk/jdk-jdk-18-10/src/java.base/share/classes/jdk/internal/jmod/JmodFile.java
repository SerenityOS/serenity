/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.jmod;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Helper class to read JMOD file
 */
public class JmodFile implements AutoCloseable {
    // jmod magic number and version number
    private static final int JMOD_MAJOR_VERSION = 0x01;
    private static final int JMOD_MINOR_VERSION = 0x00;
    private static final byte[] JMOD_MAGIC_NUMBER = {
        0x4A, 0x4D, /* JM */
        JMOD_MAJOR_VERSION, JMOD_MINOR_VERSION, /* version 1.0 */
    };

    public static void checkMagic(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            // validate the header
            byte[] magic = in.readNBytes(4);
            if (magic.length != 4) {
                throw new IOException("Invalid JMOD file: " + file);
            }
            if (magic[0] != JMOD_MAGIC_NUMBER[0] ||
                magic[1] != JMOD_MAGIC_NUMBER[1]) {
                throw new IOException("Invalid JMOD file: " + file.toString());
            }
            if (magic[2] > JMOD_MAJOR_VERSION ||
                (magic[2] == JMOD_MAJOR_VERSION && magic[3] > JMOD_MINOR_VERSION)) {
                throw new IOException("Unsupported jmod version: " +
                    magic[2] + "." + magic[3] + " in " + file.toString());
            }
        }
    }

    /**
     * JMOD sections
     */
    public static enum Section {
        CLASSES("classes"),
        CONFIG("conf"),
        HEADER_FILES("include"),
        LEGAL_NOTICES("legal"),
        MAN_PAGES("man"),
        NATIVE_LIBS("lib"),
        NATIVE_CMDS("bin");

        private final String jmodDir;
        private Section(String jmodDir) {
            this.jmodDir = jmodDir;
        }

        /**
         * Returns the directory name in the JMOD file corresponding to
         * this section
         */
        public String jmodDir() { return jmodDir; }
    }

    /**
     * JMOD file entry.
     *
     * Each entry corresponds to a ZipEntry whose name is:
     *   Section::jmodDir + '/' + name
     */
    public static class Entry {
        private final ZipEntry zipEntry;
        private final Section section;
        private final String name;

        private Entry(ZipEntry e) {
            String name = e.getName();
            int i = name.indexOf('/');
            if (i <= 1) {
                throw new RuntimeException("invalid jmod entry: " + name);
            }

            this.zipEntry = e;
            this.section = section(name.substring(0, i));
            this.name = name.substring(i+1);
        }

        /**
         * Returns the section of this entry.
         */
        public Section section() {
            return section;
        }

        /**
         * Returns the name of this entry.
         */
        public String name() {
            return name;
        }

        /**
         * Returns true if the entry is a directory in the JMOD file.
         */
        public boolean isDirectory() {
            return zipEntry.isDirectory();
        }

        /**
         * Returns the size of this entry.
         */
        public long size() {
            return zipEntry.getSize();
        }

        public ZipEntry zipEntry() {
            return zipEntry;
        }

        @Override
        public String toString() {
            return section.jmodDir() + "/" + name;
        }

        /*
         * A map from the jmodDir name to Section
         */
        static final Map<String, Section> NAME_TO_SECTION =
            Arrays.stream(Section.values())
                  .collect(Collectors.toMap(Section::jmodDir, Function.identity()));

        static Section section(String name) {
            if (!NAME_TO_SECTION.containsKey(name)) {
                throw new IllegalArgumentException("invalid section: " + name);

            }
            return NAME_TO_SECTION.get(name);
        }

    }

    private final Path file;
    private final ZipFile zipfile;

    /**
     * Constructs a {@code JmodFile} from a given path.
     */
    public JmodFile(Path file) throws IOException {
        checkMagic(file);
        this.file = file;
        this.zipfile = new ZipFile(file.toFile());
    }

    public static void writeMagicNumber(OutputStream os) throws IOException {
        os.write(JMOD_MAGIC_NUMBER);
    }

    /**
     * Returns the {@code Entry} for a resource in a JMOD file section
     * or {@code null} if not found.
     */
    public Entry getEntry(Section section, String name) {
        String entry = section.jmodDir() + "/" + name;
        ZipEntry ze = zipfile.getEntry(entry);
        return (ze != null) ? new Entry(ze) : null;
    }

    /**
     * Opens an {@code InputStream} for reading the named entry of the given
     * section in this JMOD file.
     *
     * @throws IOException if the named entry is not found, or I/O error
     *         occurs when reading it
     */
    public InputStream getInputStream(Section section, String name)
        throws IOException
    {
        String entry = section.jmodDir() + "/" + name;
        ZipEntry e = zipfile.getEntry(entry);
        if (e == null) {
            throw new IOException(name + " not found: " + file);
        }
        return zipfile.getInputStream(e);
    }

    /**
     * Opens an {@code InputStream} for reading an entry in the JMOD file.
     *
     * @throws IOException if an I/O error occurs
     */
    public InputStream getInputStream(Entry entry) throws IOException {
        return zipfile.getInputStream(entry.zipEntry());
    }

    /**
     * Returns a stream of entries in this JMOD file.
     */
    public Stream<Entry> stream() {
        return zipfile.stream()
                      .map(Entry::new);
    }

    @Override
    public void close() throws IOException {
        if (zipfile != null) {
            zipfile.close();
        }
    }
}
