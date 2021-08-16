/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.io.IOException;

/**
 * An object to which {@code char} sequences and values can be appended.  The
 * {@code Appendable} interface must be implemented by any class whose
 * instances are intended to receive formatted output from a {@link
 * java.util.Formatter}.
 *
 * <p> The characters to be appended should be valid Unicode characters as
 * described in <a href="Character.html#unicode">Unicode Character
 * Representation</a>.  Note that supplementary characters may be composed of
 * multiple 16-bit {@code char} values.
 *
 * <p> Appendables are not necessarily safe for multithreaded access.  Thread
 * safety is the responsibility of classes that extend and implement this
 * interface.
 *
 * <p> Since this interface may be implemented by existing classes
 * with different styles of error handling there is no guarantee that
 * errors will be propagated to the invoker.
 *
 * @since 1.5
 */
public interface Appendable {

    /**
     * Appends the specified character sequence to this {@code Appendable}.
     *
     * <p> Depending on which class implements the character sequence
     * {@code csq}, the entire sequence may not be appended.  For
     * instance, if {@code csq} is a {@link java.nio.CharBuffer} then
     * the subsequence to append is defined by the buffer's position and limit.
     *
     * @param  csq
     *         The character sequence to append.  If {@code csq} is
     *         {@code null}, then the four characters {@code "null"} are
     *         appended to this Appendable.
     *
     * @return  A reference to this {@code Appendable}
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    Appendable append(CharSequence csq) throws IOException;

    /**
     * Appends a subsequence of the specified character sequence to this
     * {@code Appendable}.
     *
     * <p> An invocation of this method of the form {@code out.append(csq, start, end)}
     * when {@code csq} is not {@code null}, behaves in
     * exactly the same way as the invocation
     *
     * <pre>
     *     out.append(csq.subSequence(start, end)) </pre>
     *
     * @param  csq
     *         The character sequence from which a subsequence will be
     *         appended.  If {@code csq} is {@code null}, then characters
     *         will be appended as if {@code csq} contained the four
     *         characters {@code "null"}.
     *
     * @param  start
     *         The index of the first character in the subsequence
     *
     * @param  end
     *         The index of the character following the last character in the
     *         subsequence
     *
     * @return  A reference to this {@code Appendable}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code start} or {@code end} are negative, {@code start}
     *          is greater than {@code end}, or {@code end} is greater than
     *          {@code csq.length()}
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    Appendable append(CharSequence csq, int start, int end) throws IOException;

    /**
     * Appends the specified character to this {@code Appendable}.
     *
     * @param  c
     *         The character to append
     *
     * @return  A reference to this {@code Appendable}
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    Appendable append(char c) throws IOException;
}
