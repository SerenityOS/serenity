/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Provides classes for reading and writing the standard ZIP and GZIP file
 * formats. Also includes classes for compressing and decompressing data using
 * the DEFLATE compression algorithm, which is used by the ZIP and GZIP file
 * formats. Additionally, there are utility classes for computing the CRC-32,
 * CRC-32C and Adler-32 checksums of arbitrary input streams.
 *
 * <h2>Package Specification</h2>
 *
 * <ul>
 *     <li><a href="http://www.info-zip.org/doc/appnote-19970311-iz.zip">
 *         Info-ZIP Application Note 970311</a> - a detailed description of
 *         the Info-ZIP format upon which the {@code java.util.zip} classes
 *         are based.
 *     <li><a id="zip64">An implementation may optionally support the
 *         ZIP64(tm) format extensions defined by the</a>
 *         <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">
 *         PKWARE ZIP File Format Specification</a>. The ZIP64(tm) format
 *         extensions are used to overcome the size limitations of the
 *         original ZIP format.
 *     <li><a id="lang_encoding">APPENDIX D of</a>
 *         <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">
 *         PKWARE ZIP File Format Specification</a> - Language Encoding Flag
 *         to encode ZIP entry filename and comment fields using UTF-8.
 *     <li><a href="http://www.ietf.org/rfc/rfc1950.txt">
 *         ZLIB Compressed Data Format Specification version 3.3</a>
 *         &nbsp;
 *         <a href="http://www.ietf.org/rfc/rfc1950.txt.pdf">(pdf)</a>
 *         (RFC 1950)
 *     <li><a href="http://www.ietf.org/rfc/rfc1951.txt">
 *         DEFLATE Compressed Data Format Specification version 1.3</a>
 *         &nbsp;
 *         <a href="http://www.ietf.org/rfc/rfc1951.txt.pdf">(pdf)</a>
 *         (RFC 1951)
 *     <li><a href="http://www.ietf.org/rfc/rfc1952.txt">
 *         GZIP file format specification version 4.3</a>
 *         &nbsp;
 *         <a href="http://www.ietf.org/rfc/rfc1952.txt.pdf">(pdf)</a>
 *         (RFC 1952)
 *     <li>CRC-32 checksum is described in RFC 1952 (above)
 *     <li>CRC-32C checksum is described in
 *         <a href="http://www.ietf.org/rfc/rfc3720.txt">Internet Small
 *         Computer Systems Interface (iSCSI)</a>
 *         &nbsp;
 *         <a href="http://www.ietf.org/rfc/rfc3720.txt.pdf">(pdf)</a>
 *         (RFC 3720)
 *     <li>Adler-32 checksum is described in RFC 1950 (above)
 * </ul>
 *
 * @since 1.1
 */
package java.util.zip;
