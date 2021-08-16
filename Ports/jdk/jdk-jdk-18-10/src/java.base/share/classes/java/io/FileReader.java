/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Reads text from character files using a default buffer size. Decoding from bytes
 * to characters uses either a specified {@linkplain java.nio.charset.Charset charset}
 * or the platform's
 * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
 *
 * <p>
 * The {@code FileReader} is meant for reading streams of characters. For reading
 * streams of raw bytes, consider using a {@code FileInputStream}.
 *
 * @see InputStreamReader
 * @see FileInputStream
 *
 * @author      Mark Reinhold
 * @since       1.1
 */
public class FileReader extends InputStreamReader {

   /**
    * Creates a new {@code FileReader}, given the name of the file to read,
    * using the platform's
    * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
    *
    * @param      fileName the name of the file to read
    * @throws     FileNotFoundException  if the named file does not exist,
    *             is a directory rather than a regular file,
    *             or for some other reason cannot be opened for
    *             reading.
    */
    public FileReader(String fileName) throws FileNotFoundException {
        super(new FileInputStream(fileName));
    }

   /**
    * Creates a new {@code FileReader}, given the {@code File} to read,
    * using the platform's
    * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
    *
    * @param      file the {@code File} to read
    * @throws     FileNotFoundException  if the file does not exist,
    *             is a directory rather than a regular file,
    *             or for some other reason cannot be opened for
    *             reading.
    */
    public FileReader(File file) throws FileNotFoundException {
        super(new FileInputStream(file));
    }

   /**
    * Creates a new {@code FileReader}, given the {@code FileDescriptor} to read,
    * using the platform's
    * {@linkplain java.nio.charset.Charset#defaultCharset() default charset}.
    *
    * @param fd the {@code FileDescriptor} to read
    */
    public FileReader(FileDescriptor fd) {
        super(new FileInputStream(fd));
    }

   /**
    * Creates a new {@code FileReader}, given the name of the file to read
    * and the {@linkplain java.nio.charset.Charset charset}.
    *
    * @param      fileName the name of the file to read
    * @param      charset the {@linkplain java.nio.charset.Charset charset}
    * @throws     IOException  if the named file does not exist,
    *             is a directory rather than a regular file,
    *             or for some other reason cannot be opened for
    *             reading.
    *
    * @since 11
    */
    public FileReader(String fileName, Charset charset) throws IOException {
        super(new FileInputStream(fileName), charset);
    }

   /**
    * Creates a new {@code FileReader}, given the {@code File} to read and
    * the {@linkplain java.nio.charset.Charset charset}.
    *
    * @param      file the {@code File} to read
    * @param      charset the {@linkplain java.nio.charset.Charset charset}
    * @throws     IOException  if the file does not exist,
    *             is a directory rather than a regular file,
    *             or for some other reason cannot be opened for
    *             reading.
    *
    * @since 11
    */
    public FileReader(File file, Charset charset) throws IOException {
        super(new FileInputStream(file), charset);
    }
}
