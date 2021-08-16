/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Defines charsets, decoders, and encoders, for translating between
 * bytes and Unicode characters.
 *
 * <table class="striped" style="margin-left:2em; text-align:left">
 *     <caption style="display:none">Summary of charsets, decoders, and encoders in this package</caption>
 *  <thead>
 *  <tr><th scope="col">Class name</th>
 *      <th scope="col">Description
 *  </thead>
 *  <tbody>
 *   <tr><th scope="row">{@link java.nio.charset.Charset}</th>
 *       <td>A named mapping between characters and bytes</td></tr>
 *   <tr><th scope="row">{@link java.nio.charset.CharsetDecoder}</th>
 *       <td>Decodes bytes into characters</td></tr>
 *   <tr><th scope="row">{@link java.nio.charset.CharsetEncoder}</th>
 *       <td>Encodes characters into bytes</td></tr>
 *   <tr><th scope="row">{@link java.nio.charset.CoderResult}</th>
 *       <td>Describes coder results</td></tr>
 *   <tr><th scope="row">{@link java.nio.charset.CodingErrorAction}</th>
 *       <td>Describes actions to take when coding errors are detected</td></tr>
 * </tbody>
 * </table>
 *
 * <p> A <i>charset</i> is named mapping between sequences of
 * sixteen-bit Unicode characters and sequences of bytes, in the sense
 * defined in <a
 * href="http://www.ietf.org/rfc/rfc2278.txt"><i>RFC&nbsp;2278</i></a>.
 * A <i>decoder</i> is an engine which transforms bytes in a specific
 * charset into characters, and an <i>encoder</i> is an engine which
 * transforms characters into bytes.  Encoders and decoders operate on
 * byte and character buffers.  They are collectively referred to as
 * <i>coders</i>.
 *
 * <p> The {@link java.nio.charset.Charset} class defines methods for
 * creating coders for a given charset and for retrieving the various
 * names associated with a charset.  It also defines static methods
 * for testing whether a particular charset is supported, for locating
 * charset instances by name, and for constructing a map that contains
 * every charset for which support is available in the current Java
 * virtual machine.
 *
 * <p> Most users will not use these classes directly; instead they
 * will use the existing charset-related constructors and methods in
 * the {@link java.lang.String} class, together with the existing
 * {@link java.io.InputStreamReader} and {@link
 * java.io.OutputStreamWriter} classes, all of whose implementations
 * have been reworked to make use of the charset facilities defined in
 * this package.  A small number of changes have been made to the
 * {@link java.io.InputStreamReader} and {@link
 * java.io.OutputStreamWriter} classes in order to allow explicit
 * charset objects to be specified in the construction of instances of
 * those classes.
 *
 * <p> Support for new charsets can be made available via the
 * interface defined in the {@link
 * java.nio.charset.spi.CharsetProvider} class in the {@link
 * java.nio.charset.spi} package.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a
 * constructor or method in any class or interface in this package
 * will cause a {@link java.lang.NullPointerException
 * NullPointerException} to be thrown.
 *
 *
 * @since 1.4
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 */
package java.nio.charset;
