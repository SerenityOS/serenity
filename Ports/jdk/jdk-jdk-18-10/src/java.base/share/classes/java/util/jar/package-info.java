/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Provides classes for reading and writing the JAR (Java ARchive)
 * file format, which is based on the standard ZIP file format with an
 * optional manifest file.  The manifest stores meta-information about
 * the JAR file contents and is also used for signing JAR files.
 *
 * <h2>Package Specification</h2>
 *
 * The {@code java.util.jar} package is based on the following
 * specifications:
 *
 * <ul>
 *   <li><b>Info-ZIP file format</b> - The JAR format is based on the Info-ZIP
 *       file format. See
 *       <a href="../zip/package-summary.html#package-description">java.util.zip
 *       package description.</a> <p>
 *       In JAR files, all file names must be encoded in the UTF-8 encoding.
 *   <li><a href="{@docRoot}/../specs/jar/jar.html">
 *       Manifest and Signature Specification</a> - The manifest format specification.
 * </ul>
 *
 * @since 1.2
 */
package java.util.jar;
