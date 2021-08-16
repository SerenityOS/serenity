/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A package of the Java Image I/O API dealing with low-level I/O from files and
 * streams.
 * <p>
 * The {@code ImageInputStream} interface unifies streaming and file-based
 * operations. An abstract base class, {@code ImageInputStreamImpl} is provided
 * to simplify writing a new {@code ImageInputStream} class. Concrete
 * implementation classes ({@code FileImageInputStream},
 * {@code FileCacheImageInputStream}, and {@code MemoryCacheImageInputStream})
 * are provided that allow input to come from a {@code File} or
 * {@code InputStream} with or without the use of a temporary cache file.
 * <p>
 * The {@code ImageOutputStream} interface performs an analogous function for
 * output. An abstract base class, {@code ImageOutputStreamImpl} is provided,
 * along with concrete implementation classes ({@code FileImageOutputStream},
 * {@code FileCacheImageOutputStream}, and {@code MemoryCacheImageOutputStream})
 * are provided that allow output to go to a {@code File} or
 * {@code OutputStream} with or without the use of a temporary cache file.
 * <p>
 * The {@code IIOByteBuffer} class provides an alternative way to perform reads
 * of sequences of bytes that reduces the amount of internal data copying.
 *
 * @since 1.4
 */
package javax.imageio.stream;
