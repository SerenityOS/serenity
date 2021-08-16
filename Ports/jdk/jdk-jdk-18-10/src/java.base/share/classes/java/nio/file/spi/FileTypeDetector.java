/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.spi;

import java.nio.file.Path;
import java.io.IOException;

/**
 * A file type detector for probing a file to guess its file type.
 *
 * <p> A file type detector is a concrete implementation of this class, has a
 * zero-argument constructor, and implements the abstract methods specified
 * below.
 *
 * <p> The means by which a file type detector determines the file type is
 * highly implementation specific. A simple implementation might examine the
 * <em>file extension</em> (a convention used in some platforms) and map it to
 * a file type. In other cases, the file type may be stored as a file <a
 * href="../attribute/package-summary.html"> attribute</a> or the bytes in a
 * file may be examined to guess its file type.
 *
 * @see java.nio.file.Files#probeContentType(Path)
 *
 * @since 1.7
 */

public abstract class FileTypeDetector {

    private static Void checkPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new RuntimePermission("fileTypeDetector"));
        return null;
    }
    private FileTypeDetector(Void ignore) { }

    /**
     * Initializes a new instance of this class.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link RuntimePermission}{@code ("fileTypeDetector")}
     */
    protected FileTypeDetector() {
        this(checkPermission());
    }

    /**
     * Probes the given file to guess its content type.
     *
     * <p> The means by which this method determines the file type is highly
     * implementation specific. It may simply examine the file name, it may use
     * a file <a href="../attribute/package-summary.html">attribute</a>,
     * or it may examines bytes in the file.
     *
     * <p> The probe result is the string form of the value of a
     * Multipurpose Internet Mail Extension (MIME) content type as
     * defined by <a href="http://www.ietf.org/rfc/rfc2045.txt"><i>RFC&nbsp;2045:
     * Multipurpose Internet Mail Extensions (MIME) Part One: Format of Internet
     * Message Bodies</i></a>. The string must be parsable according to the
     * grammar in the RFC 2045.
     *
     * @param   path
     *          the path to the file to probe
     *
     * @return  The content type or {@code null} if the file type is not
     *          recognized
     *
     * @throws  IOException
     *          An I/O error occurs
     * @throws  SecurityException
     *          If the implementation requires to access the file, and a
     *          security manager is installed, and it denies an unspecified
     *          permission required by a file system provider implementation.
     *          If the file reference is associated with the default file system
     *          provider then the {@link SecurityManager#checkRead(String)} method
     *          is invoked to check read access to the file.
     *
     * @see java.nio.file.Files#probeContentType
     */
    public abstract String probeContentType(Path path)
        throws IOException;
}
