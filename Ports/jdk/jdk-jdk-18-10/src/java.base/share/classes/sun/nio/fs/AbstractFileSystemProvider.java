/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.io.IOException;
import java.util.Map;

/**
 * Base implementation class of FileSystemProvider
 */

public abstract class AbstractFileSystemProvider extends FileSystemProvider {
    protected AbstractFileSystemProvider() { }

    /**
     * Splits the given attribute name into the name of an attribute view and
     * the attribute. If the attribute view is not identified then it assumed
     * to be "basic".
     */
    private static String[] split(String attribute) {
        String[] s = new String[2];
        int pos = attribute.indexOf(':');
        if (pos == -1) {
            s[0] = "basic";
            s[1] = attribute;
        } else {
            s[0] = attribute.substring(0, pos++);
            s[1] = (pos == attribute.length()) ? "" : attribute.substring(pos);
        }
        return s;
    }

    /**
     * Gets a DynamicFileAttributeView by name. Returns {@code null} if the
     * view is not available.
     */
    abstract DynamicFileAttributeView getFileAttributeView(Path file,
                                                           String name,
                                                           LinkOption... options);

    @Override
    public final void setAttribute(Path file,
                                   String attribute,
                                   Object value,
                                   LinkOption... options)
        throws IOException
    {
        String[] s = split(attribute);
        if (s[0].isEmpty())
            throw new IllegalArgumentException(attribute);
        DynamicFileAttributeView view = getFileAttributeView(file, s[0], options);
        if (view == null)
            throw new UnsupportedOperationException("View '" + s[0] + "' not available");
        view.setAttribute(s[1], value);
    }

    @Override
    public final Map<String,Object> readAttributes(Path file, String attributes, LinkOption... options)
        throws IOException
    {
        String[] s = split(attributes);
        if (s[0].isEmpty())
            throw new IllegalArgumentException(attributes);
        DynamicFileAttributeView view = getFileAttributeView(file, s[0], options);
        if (view == null)
            throw new UnsupportedOperationException("View '" + s[0] + "' not available");
        return view.readAttributes(s[1].split(","));
    }

    /**
     * Deletes a file. The {@code failIfNotExists} parameters determines if an
     * {@code IOException} is thrown when the file does not exist.
     */
    abstract boolean implDelete(Path file, boolean failIfNotExists) throws IOException;

    @Override
    public final void delete(Path file) throws IOException {
        implDelete(file, true);
    }

    @Override
    public final boolean deleteIfExists(Path file) throws IOException {
        return implDelete(file, false);
    }

    /**
     * Tests whether a file is a directory.
     *
     * @return  {@code true} if the file is a directory; {@code false} if
     *          the file does not exist, is not a directory, or it cannot
     *          be determined if the file is a directory or not.
     */
    public boolean isDirectory(Path file) {
        try {
            return readAttributes(file, BasicFileAttributes.class).isDirectory();
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Tests whether a file is a regular file with opaque content.
     *
     * @return  {@code true} if the file is a regular file; {@code false} if
     *          the file does not exist, is not a regular file, or it
     *          cannot be determined if the file is a regular file or not.
     */
    public boolean isRegularFile(Path file) {
        try {
            return readAttributes(file, BasicFileAttributes.class).isRegularFile();
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Checks the existence of a file.
     *
     * @return  {@code true} if the file exists; {@code false} if the file does
     *          not exist or its existence cannot be determined.
     */
    public boolean exists(Path file) {
        try {
            checkAccess(file);
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Returns a path name as bytes for a Unix domain socket.
     * Different encodings may be used for these names on some platforms.
     * If path is empty, then an empty byte[] is returned.
     */
    public abstract byte[] getSunPathForSocketFile(Path path);
}
