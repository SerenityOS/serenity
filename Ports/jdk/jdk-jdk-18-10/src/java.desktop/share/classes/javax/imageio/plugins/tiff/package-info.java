/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Public classes used by the built-in TIFF plug-ins.
 * <p>
 * This package contains classes supporting the built-in TIFF reader and writer
 * plug-ins. Classes are provided for simplifying interaction with metadata,
 * including Exif metadata common in digital photography, and an extension of
 * {@link javax.imageio.ImageReadParam} which permits specifying which metadata
 * tags are allowed to be read. For more information about the operation of the
 * built-in TIFF plug-ins, see the
 * <a HREF="../../metadata/doc-files/tiff_metadata.html">TIFF metadata format
 * specification and usage notes</a>.
 *
 * @since 9
 */
package javax.imageio.plugins.tiff;
