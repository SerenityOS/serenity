/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;

import java.nio.file.spi.FileSystemProvider;
import java.net.URI;

/**
 * This class consists exclusively of static methods that return a {@link Path}
 * by converting a path string or {@link URI}.
 *
 * @apiNote
 * It is recommended to obtain a {@code Path} via the {@code Path.of} methods
 * instead of via the {@code get} methods defined in this class as this class
 * may be deprecated in a future release.
 *
 * @since 1.7
 * @see Path
 */

public final class Paths {
    private Paths() { }

    /**
     * Converts a path string, or a sequence of strings that when joined form
     * a path string, to a {@code Path}.
     *
     * @implSpec
     * This method simply invokes {@link Path#of(String,String...)
     * Path.of(String, String...)} with the given parameters.
     *
     * @param   first
     *          the path string or initial part of the path string
     * @param   more
     *          additional strings to be joined to form the path string
     *
     * @return  the resulting {@code Path}
     *
     * @throws  InvalidPathException
     *          if the path string cannot be converted to a {@code Path}
     *
     * @see FileSystem#getPath
     * @see Path#of(String,String...)
     */
    public static Path get(String first, String... more) {
        return Path.of(first, more);
    }

    /**
     * Converts the given URI to a {@link Path} object.
     *
     * @implSpec
     * This method simply invokes {@link Path#of(URI) Path.of(URI)} with the
     * given parameter.
     *
     * @param   uri
     *          the URI to convert
     *
     * @return  the resulting {@code Path}
     *
     * @throws  IllegalArgumentException
     *          if preconditions on the {@code uri} parameter do not hold. The
     *          format of the URI is provider specific.
     * @throws  FileSystemNotFoundException
     *          The file system, identified by the URI, does not exist and
     *          cannot be created automatically, or the provider identified by
     *          the URI's scheme component is not installed
     * @throws  SecurityException
     *          if a security manager is installed and it denies an unspecified
     *          permission to access the file system
     *
     * @see Path#of(URI)
     */
    public static Path get(URI uri) {
        return Path.of(uri);
    }
}
