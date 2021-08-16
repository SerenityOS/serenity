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

package java.nio.file;

import java.io.IOException;

/**
 * Thrown when a file system operation fails on one or two files. This class is
 * the general class for file system exceptions.
 *
 * @since 1.7
 */

public class FileSystemException
    extends IOException
{
    @java.io.Serial
    static final long serialVersionUID = -3055425747967319812L;

    /**
     *  String identifying the file or {@code null} if not known.
     */
    private final String file;

    /**
     *  String identifying the other file or {@code null} if there isn't
     *  another file or if not known.
     */
    private final String other;

    /**
     * Constructs an instance of this class. This constructor should be used
     * when an operation involving one file fails and there isn't any additional
     * information to explain the reason.
     *
     * @param   file
     *          a string identifying the file or {@code null} if not known.
     */
    public FileSystemException(String file) {
        super((String)null);
        this.file = file;
        this.other = null;
    }

    /**
     * Constructs an instance of this class. This constructor should be used
     * when an operation involving two files fails, or there is additional
     * information to explain the reason.
     *
     * @param   file
     *          a string identifying the file or {@code null} if not known.
     * @param   other
     *          a string identifying the other file or {@code null} if there
     *          isn't another file or if not known
     * @param   reason
     *          a reason message with additional information or {@code null}
     */
    public FileSystemException(String file, String other, String reason) {
        super(reason);
        this.file = file;
        this.other = other;
    }

    /**
     * Returns the file used to create this exception.
     *
     * @return  the file (can be {@code null})
     */
    public String getFile() {
        return file;
    }

    /**
     * Returns the other file used to create this exception.
     *
     * @return  the other file (can be {@code null})
     */
    public String getOtherFile() {
        return other;
    }

    /**
     * Returns the string explaining why the file system operation failed.
     *
     * @return  the string explaining why the file system operation failed
     */
    public String getReason() {
        return super.getMessage();
    }

    /**
     * Returns the detail message string.
     */
    @Override
    public String getMessage() {
        if (file == null && other == null)
            return getReason();
        StringBuilder sb = new StringBuilder();
        if (file != null)
            sb.append(file);
        if (other != null) {
            sb.append(" -> ");
            sb.append(other);
        }
        if (getReason() != null) {
            sb.append(": ");
            sb.append(getReason());
        }
        return sb.toString();
    }
}
