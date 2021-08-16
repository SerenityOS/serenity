/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.tools.JavaFileObject;

/**
 * Used to represent a platform-neutral path within a platform-specific
 * container, such as a directory or zip file.
 * Internally, the file separator is always '/'.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public abstract class RelativePath implements Comparable<RelativePath> {
    /**
     * @param p must use '/' as an internal separator
     */
    protected RelativePath(String p) {
        path = p;
    }

    public abstract RelativeDirectory dirname();

    public abstract String basename();

    public Path resolveAgainst(Path directory) throws /*unchecked*/ InvalidPathException {
        String sep = directory.getFileSystem().getSeparator();
        return directory.resolve(path.replace("/", sep));
    }

    public Path resolveAgainst(FileSystem fs) throws /*unchecked*/ InvalidPathException {
        String sep = fs.getSeparator();
        Path root = fs.getRootDirectories().iterator().next();
        return root.resolve(path.replace("/", sep));
    }

    @Override
    public int compareTo(RelativePath other) {
        return path.compareTo(other.path);
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof RelativePath relativePath) && path.equals(relativePath.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return "RelPath[" + path + "]";
    }

    public String getPath() {
        return path;
    }

    protected final String path;

    /**
     * Used to represent a platform-neutral subdirectory within a platform-specific
     * container, such as a directory or zip file.
     * Internally, the file separator is always '/', and if the path is not empty,
     * it always ends in a '/' as well.
     */
    public static class RelativeDirectory extends RelativePath {

        static RelativeDirectory forPackage(CharSequence packageName) {
            return new RelativeDirectory(packageName.toString().replace('.', '/'));
        }

        /**
         * @param p must use '/' as an internal separator
         */
        public RelativeDirectory(String p) {
            super(p.length() == 0 || p.endsWith("/") ? p : p + "/");
        }

        /**
         * @param p must use '/' as an internal separator
         */
        public RelativeDirectory(RelativeDirectory d, String p) {
            this(d.path + p);
        }

        @Override
        public RelativeDirectory dirname() {
            int l = path.length();
            if (l == 0)
                return this;
            int sep = path.lastIndexOf('/', l - 2);
            return new RelativeDirectory(path.substring(0, sep + 1));
        }

        @Override
        public String basename() {
            int l = path.length();
            if (l == 0)
                return path;
            int sep = path.lastIndexOf('/', l - 2);
            return path.substring(sep + 1, l - 1);
        }

        /**
         * Return true if this subdirectory "contains" the other path.
         * A subdirectory path does not contain itself.
         **/
        boolean contains(RelativePath other) {
            return other.path.length() > path.length() && other.path.startsWith(path);
        }

        @Override
        public String toString() {
            return "RelativeDirectory[" + path + "]";
        }
    }

    /**
     * Used to represent a platform-neutral file within a platform-specific
     * container, such as a directory or zip file.
     * Internally, the file separator is always '/'. It never ends in '/'.
     */
    public static class RelativeFile extends RelativePath {
        static RelativeFile forClass(CharSequence className, JavaFileObject.Kind kind) {
            return new RelativeFile(className.toString().replace('.', '/') + kind.extension);
        }

        public RelativeFile(String p) {
            super(p);
            if (p.endsWith("/"))
                throw new IllegalArgumentException(p);
        }

        /**
         * @param p must use '/' as an internal separator
         */
        public RelativeFile(RelativeDirectory d, String p) {
            this(d.path + p);
        }

        RelativeFile(RelativeDirectory d, RelativePath p) {
            this(d, p.path);
        }

        @Override
        public RelativeDirectory dirname() {
            int sep = path.lastIndexOf('/');
            return new RelativeDirectory(path.substring(0, sep + 1));
        }

        @Override
        public String basename() {
            int sep = path.lastIndexOf('/');
            return path.substring(sep + 1);
        }

        ZipEntry getZipEntry(ZipFile zip) {
            return zip.getEntry(path);
        }

        @Override
        public String toString() {
            return "RelativeFile[" + path + "]";
        }

    }

}
