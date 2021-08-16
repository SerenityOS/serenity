/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.nio.charset.Charset;

/**
 * Writes text to character files using a default buffer size. Encoding from characters
 * to bytes uses either a specified {@linkplain java.nio.charset.Charset charset}
 * or the platform's
 * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
 *
 * <p>
 * Whether or not a file is available or may be created depends upon the
 * underlying platform.  Some platforms, in particular, allow a file to be
 * opened for writing by only one {@code FileWriter} (or other file-writing
 * object) at a time.  In such situations the constructors in this class
 * will fail if the file involved is already open.
 *
 * <p>
 * The {@code FileWriter} is meant for writing streams of characters. For writing
 * streams of raw bytes, consider using a {@code FileOutputStream}.
 *
 * @see OutputStreamWriter
 * @see FileOutputStream
 *
 * @author      Mark Reinhold
 * @since       1.1
 */

public class FileWriter extends OutputStreamWriter {

    /**
     * Constructs a {@code FileWriter} given a file name, using the platform's
     * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}
     *
     * @param fileName  String The system-dependent filename.
     * @throws IOException  if the named file exists but is a directory rather
     *                  than a regular file, does not exist but cannot be
     *                  created, or cannot be opened for any other reason
     */
    public FileWriter(String fileName) throws IOException {
        super(new FileOutputStream(fileName));
    }

    /**
     * Constructs a {@code FileWriter} given a file name and a boolean indicating
     * whether to append the data written, using the platform's
     * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
     *
     * @param fileName  String The system-dependent filename.
     * @param append    boolean if {@code true}, then data will be written
     *                  to the end of the file rather than the beginning.
     * @throws IOException  if the named file exists but is a directory rather
     *                  than a regular file, does not exist but cannot be
     *                  created, or cannot be opened for any other reason
     */
    public FileWriter(String fileName, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append));
    }

    /**
     * Constructs a {@code FileWriter} given the {@code File} to write,
     * using the platform's
     * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}
     *
     * @param file  the {@code File} to write.
     * @throws IOException  if the file exists but is a directory rather than
     *                  a regular file, does not exist but cannot be created,
     *                  or cannot be opened for any other reason
     */
    public FileWriter(File file) throws IOException {
        super(new FileOutputStream(file));
    }

    /**
     * Constructs a {@code FileWriter} given the {@code File} to write and
     * a boolean indicating whether to append the data written, using the platform's
     * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
     *
     * @param file  the {@code File} to write
     * @param     append    if {@code true}, then bytes will be written
     *                      to the end of the file rather than the beginning
     * @throws IOException  if the file exists but is a directory rather than
     *                  a regular file, does not exist but cannot be created,
     *                  or cannot be opened for any other reason
     * @since 1.4
     */
    public FileWriter(File file, boolean append) throws IOException {
        super(new FileOutputStream(file, append));
    }

    /**
     * Constructs a {@code FileWriter} given a file descriptor,
     * using the platform's
     * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
     *
     * @param fd  the {@code FileDescriptor} to write.
     */
    public FileWriter(FileDescriptor fd) {
        super(new FileOutputStream(fd));
    }


    /**
     * Constructs a {@code FileWriter} given a file name and
     * {@linkplain java.nio.charset.Charset charset}.
     *
     * @param fileName  the name of the file to write
     * @param charset the {@linkplain java.nio.charset.Charset charset}
     * @throws IOException  if the named file exists but is a directory rather
     *                  than a regular file, does not exist but cannot be
     *                  created, or cannot be opened for any other reason
     *
     * @since 11
     */
    public FileWriter(String fileName, Charset charset) throws IOException {
        super(new FileOutputStream(fileName), charset);
    }

    /**
     * Constructs a {@code FileWriter} given a file name,
     * {@linkplain java.nio.charset.Charset charset} and a boolean indicating
     * whether to append the data written.
     *
     * @param fileName  the name of the file to write
     * @param charset the {@linkplain java.nio.charset.Charset charset}
     * @param append    a boolean. If {@code true}, the writer will write the data
     *                  to the end of the file rather than the beginning.
     * @throws IOException  if the named file exists but is a directory rather
     *                  than a regular file, does not exist but cannot be
     *                  created, or cannot be opened for any other reason
     *
     * @since 11
     */
    public FileWriter(String fileName, Charset charset, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append), charset);
    }

    /**
     * Constructs a {@code FileWriter} given the {@code File} to write and
     * {@linkplain java.nio.charset.Charset charset}.
     *
     * @param file  the {@code File} to write
     * @param charset the {@linkplain java.nio.charset.Charset charset}
     * @throws IOException  if the file exists but is a directory rather than
     *                  a regular file, does not exist but cannot be created,
     *                  or cannot be opened for any other reason
     *
     * @since 11
     */
    public FileWriter(File file, Charset charset) throws IOException {
        super(new FileOutputStream(file), charset);
    }

    /**
     * Constructs a {@code FileWriter} given the {@code File} to write,
     * {@linkplain java.nio.charset.Charset charset} and a boolean indicating
     * whether to append the data written.
     *
     * @param file  the {@code File} to write
     * @param charset the {@linkplain java.nio.charset.Charset charset}
     * @param append    a boolean. If {@code true}, the writer will write the data
     *                  to the end of the file rather than the beginning.
     * @throws IOException  if the file exists but is a directory rather than
     *                  a regular file, does not exist but cannot be created,
     *                  or cannot be opened for any other reason
     * @since 11
     */
    public FileWriter(File file, Charset charset, boolean append) throws IOException {
        super(new FileOutputStream(file, append), charset);
    }
}
