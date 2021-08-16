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
 * Classes supporting the built-in JPEG plug-in.
 * <p>
 * This package contains some support classes for the built-in JPEG reader and
 * writer plug-ins. Classes are provided for representing quantization and
 * Huffman tables, and extensions of {@code ImageReadParam} and
 * {@code ImageWriteParam} are provided to supply tables during the reading and
 * writing process. For more information about the operation of the built-in
 * JPEG plug-ins, see the
 * <a href="../../metadata/doc-files/jpeg_metadata.html">JPEG metadata format
 * specification and usage notes</a>.
 *
 * @since 1.4
 */
package javax.imageio.plugins.jpeg;
