/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdeps;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Dependencies.ClassFileError;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * ClassFileReader reads ClassFile(s) of a given path that can be
 * a .class file, a directory, or a JAR file.
 */
public class ClassFileReader implements Closeable {
    /**
     * Returns a ClassFileReader instance of a given path.
     */
    public static ClassFileReader newInstance(Path path, Runtime.Version version) throws IOException {
        if (Files.notExists(path)) {
            throw new FileNotFoundException(path.toString());
        }

        if (Files.isDirectory(path)) {
            return new DirectoryReader(path);
        } else if (path.getFileName().toString().endsWith(".jar")) {
            return new JarFileReader(path, version);
        } else {
            return new ClassFileReader(path);
        }
    }

    /**
     * Returns a ClassFileReader instance of a given FileSystem and path.
     *
     * This method is used for reading classes from jrtfs.
     */
    public static ClassFileReader newInstance(FileSystem fs, Path path) throws IOException {
        return new DirectoryReader(fs, path);
    }

    protected final Path path;
    protected final String baseFileName;
    protected Set<String> entries; // binary names

    protected final List<String> skippedEntries = new ArrayList<>();
    protected ClassFileReader(Path path) {
        this.path = path;
        this.baseFileName = path.getFileName() != null
                                ? path.getFileName().toString()
                                : path.toString();
    }

    public String getFileName() {
        return baseFileName;
    }

    public List<String> skippedEntries() {
        return skippedEntries;
    }

    /**
     * Returns all entries in this archive.
     */
    public Set<String> entries() {
        Set<String> es = this.entries;
        if (es == null) {
            // lazily scan the entries
            this.entries = scan();
        }
        return this.entries;
    }

    /**
     * Returns the ClassFile matching the given binary name
     * or a fully-qualified class name.
     */
    public ClassFile getClassFile(String name) throws IOException {
        if (name.indexOf('.') > 0) {
            int i = name.lastIndexOf('.');
            String pathname = name.replace('.', File.separatorChar) + ".class";
            if (baseFileName.equals(pathname) ||
                    baseFileName.equals(pathname.substring(0, i) + "$" +
                                        pathname.substring(i+1, pathname.length()))) {
                return readClassFile(path);
            }
        } else {
            if (baseFileName.equals(name.replace('/', File.separatorChar) + ".class")) {
                return readClassFile(path);
            }
        }
        return null;
    }

    public Iterable<ClassFile> getClassFiles() throws IOException {
        return FileIterator::new;
    }

    protected ClassFile readClassFile(Path p) throws IOException {
        InputStream is = null;
        try {
            is = Files.newInputStream(p);
            return ClassFile.read(is);
        } catch (ConstantPoolException e) {
            throw new ClassFileError(e);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    protected Set<String> scan() {
        try {
            ClassFile cf = ClassFile.read(path);
            String name = cf.access_flags.is(AccessFlags.ACC_MODULE)
                ? "module-info" : cf.getName();
            return Collections.singleton(name);
        } catch (ConstantPoolException|IOException e) {
            throw new ClassFileError(e);
        }
    }

    static boolean isClass(Path file) {
        String fn = file.getFileName().toString();
        return fn.endsWith(".class");
    }

    @Override
    public void close() throws IOException {
    }

    class FileIterator implements Iterator<ClassFile> {
        int count;
        FileIterator() {
            this.count = 0;
        }
        public boolean hasNext() {
            return count == 0 && baseFileName.endsWith(".class");
        }

        public ClassFile next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            try {
                ClassFile cf = readClassFile(path);
                count++;
                return cf;
            } catch (IOException e) {
                throw new ClassFileError(e);
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public String toString() {
        return path.toString();
    }

    private static class DirectoryReader extends ClassFileReader {
        protected final String fsSep;
        DirectoryReader(Path path) throws IOException {
            this(FileSystems.getDefault(), path);
        }
        DirectoryReader(FileSystem fs, Path path) throws IOException {
            super(path);
            this.fsSep = fs.getSeparator();
        }

        protected Set<String> scan() {
            try (Stream<Path> stream = Files.walk(path, Integer.MAX_VALUE)) {
                return stream.filter(ClassFileReader::isClass)
                             .map(path::relativize)
                             .map(Path::toString)
                             .map(p -> p.replace(File.separatorChar, '/'))
                             .collect(Collectors.toSet());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public ClassFile getClassFile(String name) throws IOException {
            if (name.indexOf('.') > 0) {
                int i = name.lastIndexOf('.');
                String pathname = name.replace(".", fsSep) + ".class";
                Path p = path.resolve(pathname);
                if (Files.notExists(p)) {
                    p = path.resolve(pathname.substring(0, i) + "$" +
                            pathname.substring(i+1, pathname.length()));
                }
                if (Files.exists(p)) {
                    return readClassFile(p);
                }
            } else {
                Path p = path.resolve(name + ".class");
                if (Files.exists(p)) {
                    return readClassFile(p);
                }
            }
            return null;
        }

        public Iterable<ClassFile> getClassFiles() throws IOException {
            final Iterator<ClassFile> iter = new DirectoryIterator();
            return () -> iter;
        }

        class DirectoryIterator implements Iterator<ClassFile> {
            private final List<Path> entries;
            private int index = 0;
            DirectoryIterator() throws IOException {
                List<Path> paths = null;
                try (Stream<Path> stream = Files.walk(path, Integer.MAX_VALUE)) {
                    paths = stream.filter(ClassFileReader::isClass).toList();

                }
                this.entries = paths;
                this.index = 0;
            }

            public boolean hasNext() {
                return index != entries.size();
            }

            public ClassFile next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Path path = entries.get(index++);
                try {
                    return readClassFile(path);
                } catch (IOException e) {
                    throw new ClassFileError(e);
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }
    }

    static class JarFileReader extends ClassFileReader {
        private final JarFile jarfile;
        private final Runtime.Version version;

        JarFileReader(Path path, Runtime.Version version) throws IOException {
            this(path, openJarFile(path.toFile(), version), version);
        }

        JarFileReader(Path path, JarFile jf, Runtime.Version version) throws IOException {
            super(path);
            this.jarfile = jf;
            this.version = version;
        }

        @Override
        public void close() throws IOException {
            jarfile.close();
        }

        private static JarFile openJarFile(File f, Runtime.Version version)
                throws IOException {
            JarFile jf;
            if (version == null) {
                jf = new JarFile(f, false);
                if (jf.isMultiRelease()) {
                    throw new MultiReleaseException("err.multirelease.option.notfound", f.getName());
                }
            } else {
                jf = new JarFile(f, false, ZipFile.OPEN_READ, version);
            }
            return jf;
        }

        protected Set<String> scan() {
            try (JarFile jf = openJarFile(path.toFile(), version)) {
                return jf.versionedStream().map(JarEntry::getName)
                         .filter(n -> n.endsWith(".class"))
                         .collect(Collectors.toSet());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public ClassFile getClassFile(String name) throws IOException {
            if (name.indexOf('.') > 0) {
                int i = name.lastIndexOf('.');
                String entryName = name.replace('.', '/') + ".class";
                JarEntry e = jarfile.getJarEntry(entryName);
                if (e == null) {
                    e = jarfile.getJarEntry(entryName.substring(0, i) + "$"
                            + entryName.substring(i + 1, entryName.length()));
                }
                if (e != null) {
                    return readClassFile(jarfile, e);
                }
            } else {
                JarEntry e = jarfile.getJarEntry(name + ".class");
                if (e != null) {
                    return readClassFile(jarfile, e);
                }
            }
            return null;
        }

        protected ClassFile readClassFile(JarFile jarfile, JarEntry e) throws IOException {
            try (InputStream is = jarfile.getInputStream(e)) {
                ClassFile cf = ClassFile.read(is);
                if (jarfile.isMultiRelease()) {
                    VersionHelper.add(jarfile, e, cf);
                }
                return cf;
            } catch (ConstantPoolException ex) {
                throw new ClassFileError(ex);
            }
        }

        public Iterable<ClassFile> getClassFiles() throws IOException {
            final Iterator<ClassFile> iter = new JarFileIterator(this, jarfile);
            return () -> iter;
        }
    }

    class JarFileIterator implements Iterator<ClassFile> {
        protected final JarFileReader reader;
        protected Iterator<JarEntry> entries;
        protected JarFile jf;
        protected JarEntry nextEntry;
        protected ClassFile cf;
        JarFileIterator(JarFileReader reader) {
            this(reader, null);
        }
        JarFileIterator(JarFileReader reader, JarFile jarfile) {
            this.reader = reader;
            setJarFile(jarfile);
        }

        void setJarFile(JarFile jarfile) {
            if (jarfile == null) return;

            this.jf = jarfile;
            this.entries = jarfile.versionedStream().iterator();
            this.nextEntry = nextEntry();
        }

        public boolean hasNext() {
            if (nextEntry != null && cf != null) {
                return true;
            }
            while (nextEntry != null) {
                try {
                    cf = reader.readClassFile(jf, nextEntry);
                    return true;
                } catch (ClassFileError | IOException ex) {
                    skippedEntries.add(String.format("%s: %s (%s)",
                                                     ex.getMessage(),
                                                     nextEntry.getName(),
                                                     jf.getName()));
                }
                nextEntry = nextEntry();
            }
            return false;
        }

        public ClassFile next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            ClassFile classFile = cf;
            cf = null;
            nextEntry = nextEntry();
            return classFile;
        }

        protected JarEntry nextEntry() {
            while (entries.hasNext()) {
                JarEntry e = entries.next();
                String name = e.getName();
                if (name.endsWith(".class")) {
                    return e;
                }
            }
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    private static final String MODULE_INFO = "module-info.class";
}
