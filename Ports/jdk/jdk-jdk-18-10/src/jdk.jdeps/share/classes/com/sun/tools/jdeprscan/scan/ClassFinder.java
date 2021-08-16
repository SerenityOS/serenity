/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdeprscan.scan;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * A simple search path for classes.
 */
public class ClassFinder {
    final List<PathEntry> list = new ArrayList<>();
    final boolean verbose;

    public ClassFinder(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Adds a directory to this finder's search path, ignoring errors.
     *
     * @param dirName the directory to add
     */
    public void addDir(String dirName) {
        Path dir = Paths.get(dirName);

        if (Files.isDirectory(dir)) {
            list.add(new DirPathEntry(dir));
        }
    }

    /**
     * Adds a jar file to this finder's search path, ignoring errors.
     *
     * @param jarName the jar file name to add
     */
    public void addJar(String jarName) {
        try {
            list.add(new JarPathEntry(new JarFile(jarName)));
        } catch (IOException ignore) { }
    }

    /**
     * Adds the JRT filesystem to this finder's search path.
     */
    public void addJrt() {
        list.add(new JrtPathEntry());
    }

    /**
     * Searches the class path for a class with the given name,
     * returning a ClassFile for it. Returns null if not found.
     *
     * @param className the class to search for
     * @return a ClassFile instance, or null if not found
     */
    public ClassFile find(String className) {
        for (PathEntry pe : list) {
            ClassFile cf = pe.find(className);
            if (cf != null) {
                return cf;
            }
        }
        return null;
    }

    /**
     * An entry in this finder's class path.
     */
    interface PathEntry {
        /**
         * Returns a ClassFile instance corresponding to this name,
         * or null if it's not present in this entry.
         *
         * @param className the class to search for
         * @return a ClassFile instance, or null if not found
         */
        ClassFile find(String className);
    }

    /**
     * An entry that represents a jar file.
     */
    class JarPathEntry implements PathEntry {
        final JarFile jarFile;

        JarPathEntry(JarFile jf) {
            jarFile = jf;
        }

        @Override
        public ClassFile find(String className) {
            JarEntry entry = jarFile.getJarEntry(className + ".class");
            if (entry == null) {
                return null;
            }
            try {
                return ClassFile.read(jarFile.getInputStream(entry));
            } catch (IOException | ConstantPoolException ex) {
                if (verbose) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }

    /**
     * An entry that represents a directory containing a class hierarchy.
     */
    class DirPathEntry implements PathEntry {
        final Path dir;

        DirPathEntry(Path dir) {
            this.dir = dir;
        }

        @Override
        public ClassFile find(String className) {
            Path classFileName = dir.resolve(className + ".class");
            try {
                return ClassFile.read(classFileName);
            } catch (NoSuchFileException nsfe) {
                // not found, return silently
            } catch (IOException | ConstantPoolException ex) {
                if (verbose) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }

    /**
     * An entry that represents the JRT filesystem in the running image.
     *
     * JRT filesystem structure is:
     *     /packages/<dotted-pkgname>/<modlink>
     * where modlink is a symbolic link to /modules/<modname> which is
     * the top of the usual package-class hierarchy
     */
    class JrtPathEntry implements PathEntry {
        final FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));

        @Override
        public ClassFile find(String className) {
            int end = className.lastIndexOf('/');
            if (end < 0) {
                return null;
            }
            String pkg = "/packages/" + className.substring(0, end)
                                                 .replace('/', '.');
            try (Stream<Path> mods = Files.list(fs.getPath(pkg))) {
                Optional<Path> opath =
                    mods.map(path -> path.resolve(className + ".class"))
                        .filter(Files::exists)
                        .findFirst();
                if (opath.isPresent()) {
                    return ClassFile.read(opath.get());
                } else {
                    return null;
                }
            } catch (NoSuchFileException nsfe) {
                // not found, return silently
            } catch (IOException | ConstantPoolException ex) {
                if (verbose) {
                    ex.printStackTrace();
                }
            }
            return null;
        }
    }
}
