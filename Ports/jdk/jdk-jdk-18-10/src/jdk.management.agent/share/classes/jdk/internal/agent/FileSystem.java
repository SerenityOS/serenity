/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.agent;

import java.io.File;
import java.io.IOException;

/*
 * Utility class to support file system operations
 *
 * @since 1.5
 */
public abstract class FileSystem {

    private static final Object lock = new Object();
    private static FileSystem fs;

    protected FileSystem() { }

    /**
     * Opens the file system
     */
    public static FileSystem open() {
        synchronized (lock) {
            if (fs == null) {
                fs = new FileSystemImpl();
            }
            return fs;
        }
    }

    /**
     * Tells whether or not the specified file is located on a
     * file system that supports file security or not.
     *
     * @throws  IOException     if an I/O error occurs.
     */
    public abstract boolean supportsFileSecurity(File f) throws IOException;

    /**
     * Tell whether or not the specified file is accessible
     * by anything other than the file owner.
     *
     * @throws  IOException     if an I/O error occurs.
     *
     * @throws  UnsupportedOperationException
     *          If file is located on a file system that doesn't support
     *          file security.
     */
    public abstract boolean isAccessUserOnly(File f) throws IOException;
}
