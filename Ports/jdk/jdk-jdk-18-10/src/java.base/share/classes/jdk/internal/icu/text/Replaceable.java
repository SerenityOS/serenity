/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 *******************************************************************************
 * (C) Copyright IBM Corp. 1996-2005 - All Rights Reserved                     *
 *                                                                             *
 * The original version of this source code and documentation is copyrighted   *
 * and owned by IBM, These materials are provided under terms of a License     *
 * Agreement between IBM and Sun. This technology is protected by multiple     *
 * US and International patents. This notice and attribution to IBM may not    *
 * to removed.                                                                 *
 *******************************************************************************
 */

package jdk.internal.icu.text;

/**
 * <code>Replaceable</code> is an interface representing a
 * string of characters that supports the replacement of a range of
 * itself with a new string of characters.  It is used by APIs that
 * change a piece of text while retaining metadata.  Metadata is data
 * other than the Unicode characters returned by char32At().  One
 * example of metadata is style attributes; another is an edit
 * history, marking each character with an author and revision number.
 *
 * <p>An implicit aspect of the <code>Replaceable</code> API is that
 * during a replace operation, new characters take on the metadata of
 * the old characters.  For example, if the string "the <b>bold</b>
 * font" has range (4, 8) replaced with "strong", then it becomes "the
 * <b>strong</b> font".
 *
 * <p><code>Replaceable</code> specifies ranges using a start
 * offset and a limit offset.  The range of characters thus specified
 * includes the characters at offset start..limit-1.  That is, the
 * start offset is inclusive, and the limit offset is exclusive.
 *
 * <p><code>Replaceable</code> also includes API to access characters
 * in the string: <code>length()</code>, <code>charAt()</code>,
 * <code>char32At()</code>, and <code>extractBetween()</code>.
 *
 * <p>For a subclass to support metadata, typical behavior of
 * <code>replace()</code> is the following:
 * <ul>
 *   <li>Set the metadata of the new text to the metadata of the first
 *   character replaced</li>
 *   <li>If no characters are replaced, use the metadata of the
 *   previous character</li>
 *   <li>If there is no previous character (i.e. start == 0), use the
 *   following character</li>
 *   <li>If there is no following character (i.e. the replaceable was
 *   empty), use default metadata</li>
 *   <li>If the code point U+FFFF is seen, it should be interpreted as
 *   a special marker having no metadata</li>
 * </ul>
 * If this is not the behavior, the subclass should document any differences.
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @stable ICU 2.0
 */
public interface Replaceable {
    /**
     * Returns the number of 16-bit code units in the text.
     * @return number of 16-bit code units in text
     * @stable ICU 2.0
     */
    int length();

    /**
     * Returns the 16-bit code unit at the given offset into the text.
     * @param offset an integer between 0 and <code>length()</code>-1
     * inclusive
     * @return 16-bit code unit of text at given offset
     * @stable ICU 2.0
     */
    char charAt(int offset);

    /**
     * Copies characters from this object into the destination
     * character array.  The first character to be copied is at index
     * <code>srcStart</code>; the last character to be copied is at
     * index <code>srcLimit-1</code> (thus the total number of
     * characters to be copied is <code>srcLimit-srcStart</code>). The
     * characters are copied into the subarray of <code>dst</code>
     * starting at index <code>dstStart</code> and ending at index
     * <code>dstStart + (srcLimit-srcStart) - 1</code>.
     *
     * @param srcStart the beginning index to copy, inclusive;
     *        {@code 0 <= start <= limit}.
     * @param srcLimit the ending index to copy, exclusive;
     *        {@code start <= limit <= length()}.
     * @param dst the destination array.
     * @param dstStart the start offset in the destination array.
     * @stable ICU 2.0
     */
    void getChars(int srcStart, int srcLimit, char dst[], int dstStart);
}
