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
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An Archive of all content, classes, resources, configuration files, and
 * other, for a module.
 */
public interface Archive {

    /**
     * Entry is contained in an Archive
     */
    public abstract class Entry {

        public static enum EntryType {
            MODULE_NAME,
            CLASS_OR_RESOURCE,
            CONFIG,
            NATIVE_LIB,
            NATIVE_CMD,
            HEADER_FILE,
            LEGAL_NOTICE,
            MAN_PAGE,
            SERVICE;
        }

        private final String name;
        private final EntryType type;
        private final Archive archive;
        private final String path;

        /**
         * Constructs an entry of the given archive
         * @param archive archive
         * @param path
         * @param name an entry name that does not contain the module name
         * @param type
         */
        public Entry(Archive archive, String path, String name, EntryType type) {
            this.archive = Objects.requireNonNull(archive);
            this.path = Objects.requireNonNull(path);
            this.name = Objects.requireNonNull(name);
            this.type = Objects.requireNonNull(type);
        }

        public final Archive archive() {
            return archive;
        }

        public final EntryType type() {
            return type;
        }

        /**
         * Returns the name of this entry.
         */
        public final String name() {
            return name;
        }

        /**
         * Returns the name representing a ResourcePoolEntry in the form of:
         *    /$MODULE/$ENTRY_NAME
         */
        public final String getResourcePoolEntryName() {
            return "/" + archive.moduleName() + "/" + name;
        }

        @Override
        public String toString() {
            return "type " + type.name() + " path " + path;
        }

        /*
         * Returns the number of uncompressed bytes for this entry.
         */
        public abstract long size();

        public abstract InputStream stream() throws IOException;
    }

    /*
     * The module name.
     */
    String moduleName();

    /*
     * Returns the path to this module's content
     */
    Path getPath();

    /*
     * Stream of Entry.
     * The stream of entries needs to be closed after use
     * since it might cover lazy I/O based resources.
     * So callers need to use a try-with-resources block.
     */
    Stream<Entry> entries();

    /*
     * Open the archive
     */
    void open() throws IOException;

    /*
     * Close the archive
     */
    void close() throws IOException;
}
