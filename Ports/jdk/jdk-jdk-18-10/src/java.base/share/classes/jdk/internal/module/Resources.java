/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.module;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A helper class to support working with resources in modules. Also provides
 * support for translating resource names to file paths.
 */
public final class Resources {
    private Resources() { }

    /**
     * Return true if a resource can be encapsulated. Resource with names
     * ending in ".class" or "/" cannot be encapsulated. Resource names
     * that map to a legal package name can be encapsulated.
     */
    public static boolean canEncapsulate(String name) {
        int len = name.length();
        if (len > 6 && name.endsWith(".class")) {
            return false;
        } else {
            return Checks.isPackageName(toPackageName(name));
        }
    }

    /**
     * Derive a <em>package name</em> for a resource. The package name
     * returned by this method may not be a legal package name. This method
     * returns null if the resource name ends with a "/" (a directory)
     * or the resource name does not contain a "/".
     */
    public static String toPackageName(String name) {
        int index = name.lastIndexOf('/');
        if (index == -1 || index == name.length()-1) {
            return "";
        } else {
            return name.substring(0, index).replace('/', '.');
        }
    }

    /**
     * Returns a resource name corresponding to the relative file path
     * between {@code dir} and {@code file}. If the file is a directory
     * then the name will end with a  "/", except the top-level directory
     * where the empty string is returned.
     */
    public static String toResourceName(Path dir, Path file) {
        String s = dir.relativize(file)
                      .toString()
                      .replace(File.separatorChar, '/');
        if (!s.isEmpty() && Files.isDirectory(file))
            s += "/";
        return s;
    }

    /**
     * Returns a file path to a resource in a file tree. If the resource
     * name has a trailing "/" then the file path will locate a directory.
     * Returns {@code null} if the resource does not map to a file in the
     * tree file.
     */
    public static Path toFilePath(Path dir, String name) throws IOException {
        boolean expectDirectory = name.endsWith("/");
        if (expectDirectory) {
            name = name.substring(0, name.length() - 1);  // drop trailing "/"
        }
        Path path = toSafeFilePath(dir.getFileSystem(), name);
        if (path != null) {
            Path file = dir.resolve(path);
            try {
                BasicFileAttributes attrs;
                attrs = Files.readAttributes(file, BasicFileAttributes.class);
                if (attrs.isDirectory()
                    || (!attrs.isDirectory() && !expectDirectory))
                    return file;
            } catch (NoSuchFileException ignore) { }
        }
        return null;
    }

    /**
     * Map a resource name to a "safe" file path. Returns {@code null} if
     * the resource name cannot be converted into a "safe" file path.
     *
     * Resource names with empty elements, or elements that are "." or ".."
     * are rejected, as are resource names that translates to a file path
     * with a root component.
     */
    private static Path toSafeFilePath(FileSystem fs, String name) {
        // scan elements of resource name
        int next;
        int off = 0;
        while ((next = name.indexOf('/', off)) != -1) {
            int len = next - off;
            if (!mayTranslate(name, off, len)) {
                return null;
            }
            off = next + 1;
        }
        int rem = name.length() - off;
        if (!mayTranslate(name, off, rem)) {
            return null;
        }

        // convert to file path
        Path path;
        if (File.separatorChar == '/') {
            path = fs.getPath(name);
        } else {
            // not allowed to embed file separators
            if (name.contains(File.separator))
                return null;
            path = fs.getPath(name.replace('/', File.separatorChar));
        }

        // file path not allowed to have root component
        return (path.getRoot() == null) ? path : null;
    }

    /**
     * Returns {@code true} if the element in a resource name is a candidate
     * to translate to the element of a file path.
     */
    private static boolean mayTranslate(String name, int off, int len) {
        if (len <= 2) {
            if (len == 0)
                return false;
            boolean starsWithDot = (name.charAt(off) == '.');
            if (len == 1 && starsWithDot)
                return false;
            if (len == 2 && starsWithDot && (name.charAt(off+1) == '.'))
                return false;
        }
        return true;
    }

}
