/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.util;

import java.io.IOException;

import jdk.javadoc.internal.doclets.toolkit.DocletException;


/**
 * Wraps an IOException and the filename to which it applies.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 *
 * @apiNote This exception should be thrown by a doclet when an IO exception occurs
 *  and the file is known that was in use when the exception occurred.
 */
public class DocFileIOException extends DocletException {
    /**
     * A hint for the type of operation being performed when the exception occurred.
     *
     * @apiNote This may be used as a hint when reporting a message to the end user.
     */
    public enum Mode {
        /** The file was being opened for reading, or being read when the exception occurred. */
        READ,
        /** The file was being opened for writing, or being written when the exception occurred. */
        WRITE
    }

    /**
     * The file that was in use when the exception occurred.
     */
    public final DocFile fileName;

    /**
     * The mode in which the file was being used when the exception occurred.
     */
    public final Mode mode;

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception to wrap an IO exception, the file which caused it, and the manner
     * in which the file was being used.
     *
     * @param fileName the file in use when the exception occurred
     * @param mode the manner in which the file was being used
     * @param cause the underlying exception
     */
    public DocFileIOException(DocFile fileName, Mode mode, IOException cause) {
        super(mode + ":" + fileName.getPath(), cause);
        this.fileName = fileName;
        this.mode = mode;
    }
}
