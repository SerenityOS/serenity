/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.tools.FileObject;

import com.sun.tools.javac.file.RelativePath.RelativeDirectory;
import com.sun.tools.javac.util.Context;

/**
 * A package-oriented index into the jrt: filesystem.
 */
public class JRTIndex {
    /** Get a shared instance of the cache. */
    private static JRTIndex sharedInstance;
    public synchronized static JRTIndex getSharedInstance() {
        if (sharedInstance == null) {
            try {
                sharedInstance = new JRTIndex();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return sharedInstance;
    }

    /** Get a context-specific instance of a cache. */
    public static JRTIndex instance(Context context) {
        try {
            JRTIndex instance = context.get(JRTIndex.class);
            if (instance == null)
                context.put(JRTIndex.class, instance = new JRTIndex());
            return instance;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static boolean isAvailable() {
        try {
            FileSystems.getFileSystem(URI.create("jrt:/"));
            return true;
        } catch (ProviderNotFoundException | FileSystemNotFoundException e) {
            return false;
        }
    }


    /**
     * The jrt: file system.
     */
    private final FileSystem jrtfs;

    /**
     * A lazily evaluated set of entries about the contents of the jrt: file system.
     */
    private final Map<RelativeDirectory, SoftReference<Entry>> entries;

    /**
     * An entry provides cached info about a specific package directory within jrt:.
     */
    class Entry {
        /**
         * The regular files for this package.
         * For now, assume just one instance of each file across all modules.
         */
        final Map<String, Path> files;

        /**
         * The set of subdirectories in jrt: for this package.
         */
        final Set<RelativeDirectory> subdirs;

        /**
         * The info that used to be in ct.sym for classes in this package.
         */
        final CtSym ctSym;

        private Entry(Map<String, Path> files, Set<RelativeDirectory> subdirs, CtSym ctSym) {
            this.files = files;
            this.subdirs = subdirs;
            this.ctSym = ctSym;
        }
    }

    /**
     * The info that used to be in ct.sym for classes in a package.
     */
    public static class CtSym {
        /**
         * The classes in this package are internal and not visible.
         */
        public final boolean hidden;
        /**
         * The classes in this package are proprietary and will generate a warning.
         */
        public final boolean proprietary;
        /**
         * The minimum profile in which classes in this package are available.
         */
        public final String minProfile;

        CtSym(boolean hidden, boolean proprietary, String minProfile) {
            this.hidden = hidden;
            this.proprietary = proprietary;
            this.minProfile = minProfile;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("CtSym[");
            boolean needSep = false;
            if (hidden) {
                sb.append("hidden");
                needSep = true;
            }
            if (proprietary) {
                if (needSep) sb.append(",");
                sb.append("proprietary");
                needSep = true;
            }
            if (minProfile != null) {
                if (needSep) sb.append(",");
                sb.append(minProfile);
            }
            sb.append("]");
            return sb.toString();
        }

        static final CtSym EMPTY = new CtSym(false, false, null);
    }

    /**
     * Create and initialize the index.
     */
    private JRTIndex() throws IOException {
        jrtfs = FileSystems.getFileSystem(URI.create("jrt:/"));
        entries = new HashMap<>();
    }

    public CtSym getCtSym(CharSequence packageName) throws IOException {
        return getEntry(RelativeDirectory.forPackage(packageName)).ctSym;
    }

    synchronized Entry getEntry(RelativeDirectory rd) throws IOException {
        SoftReference<Entry> ref = entries.get(rd);
        Entry e = (ref == null) ? null : ref.get();
        if (e == null) {
            Map<String, Path> files = new LinkedHashMap<>();
            Set<RelativeDirectory> subdirs = new LinkedHashSet<>();
            Path dir;
            if (rd.path.isEmpty()) {
                dir = jrtfs.getPath("/modules");
            } else {
                Path pkgs = jrtfs.getPath("/packages");
                dir = pkgs.resolve(rd.getPath().replaceAll("/$", "").replace("/", "."));
            }
            if (Files.exists(dir)) {
                try (DirectoryStream<Path> modules = Files.newDirectoryStream(dir)) {
                    for (Path module: modules) {
                        if (Files.isSymbolicLink(module))
                            module = Files.readSymbolicLink(module);
                        Path p = rd.resolveAgainst(module);
                        if (!Files.exists(p))
                            continue;
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {
                            for (Path entry: stream) {
                                String name = entry.getFileName().toString();
                                if (Files.isRegularFile(entry)) {
                                    // TODO: consider issue of files with same name in different modules
                                    files.put(name, entry);
                                } else if (Files.isDirectory(entry)) {
                                    subdirs.add(new RelativeDirectory(rd, name));
                                }
                            }
                        }
                    }
                }
            }
            e = new Entry(Collections.unmodifiableMap(files),
                    Collections.unmodifiableSet(subdirs),
                    getCtInfo(rd));
            entries.put(rd, new SoftReference<>(e));
        }
        return e;
    }

    public boolean isInJRT(FileObject fo) {
        if (fo instanceof PathFileObject pathFileObject) {
            Path path = pathFileObject.getPath();
            return (path.getFileSystem() == jrtfs);
        } else {
            return false;
        }
    }

    private CtSym getCtInfo(RelativeDirectory dir) {
        if (dir.path.isEmpty())
            return CtSym.EMPTY;
        // It's a side-effect of the default build rules that ct.properties
        // ends up as a resource bundle.
        if (ctBundle == null) {
            final String bundleName = "com.sun.tools.javac.resources.ct";
            ctBundle = ResourceBundle.getBundle(bundleName);
        }
        try {
            String attrs = ctBundle.getString(dir.path.replace('/', '.') + '*');
            boolean hidden = false;
            boolean proprietary = false;
            String minProfile = null;
            for (String attr: attrs.split(" +", 0)) {
                switch (attr) {
                    case "hidden":
                        hidden = true;
                        break;
                    case "proprietary":
                        proprietary = true;
                        break;
                    default:
                        minProfile = attr;
                }
            }
            return new CtSym(hidden, proprietary, minProfile);
        } catch (MissingResourceException e) {
            return CtSym.EMPTY;
        }

    }

    private ResourceBundle ctBundle;
}
