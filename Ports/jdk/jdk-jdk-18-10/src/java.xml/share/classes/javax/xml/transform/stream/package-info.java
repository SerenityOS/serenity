/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Provides stream and URI specific transformation classes.
 *
 * <p>
 * The {@link javax.xml.transform.stream.StreamSource} class
 * provides methods for specifying {@link java.io.InputStream} input,
 * {@link java.io.Reader} input, and URL input in the form of strings. Even
 * if an input stream or reader is specified as the source,
 * {@link javax.xml.transform.stream.StreamSource#setSystemId} should still
 * be called, so that the transformer can know from where it should resolve
 * relative URIs. The public identifier is always optional: if the application
 * writer includes one, it will be provided as part of the
 * {@link javax.xml.transform.SourceLocator} information.
 * <p>
 * The {@link javax.xml.transform.stream.StreamResult} class
 * provides methods for specifying {@link java.io.OutputStream},
 * {@link java.io.Writer}, or an output system ID, as the output of the
 * transformation result.
 * <p>
 * Normally streams should be used rather than readers or writers, for
 * both the Source and Result, since readers and writers already have the encoding
 * established to and from the internal Unicode format. However, there are times
 * when it is useful to write to a character stream, such as when using a
 * StringWriter in order to write to a String, or in the case of reading source
 * XML from a StringReader.
 *
 *
 * @since 1.5
 */

package javax.xml.transform.stream;
