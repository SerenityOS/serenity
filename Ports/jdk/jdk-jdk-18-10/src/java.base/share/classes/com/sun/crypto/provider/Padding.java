/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.crypto.provider;

import javax.crypto.ShortBufferException;

/**
 * Padding interface.
 *
 * This interface is implemented by general-purpose padding schemes, such as
 * the one described in PKCS#5.
 *
 * @author Jan Luehe
 * @author Gigi Ankeny
 *
 *
 * @see PKCS5Padding
 */

interface Padding {

    /**
     * Adds the given number of padding bytes to the data input.
     * The value of the padding bytes is determined
     * by the specific padding mechanism that implements this
     * interface.
     *
     * @param in the input buffer with the data to pad
     * @param off the offset in <code>in</code> where the padding bytes
     *  are appended
     * @param len the number of padding bytes to add
     *
     * @exception ShortBufferException if <code>in</code> is too small to hold
     * the padding bytes
     */
    void padWithLen(byte[] in, int off, int len) throws ShortBufferException;

    /**
     * Returns the index where padding starts.
     *
     * <p>Given a buffer with data and their padding, this method returns the
     * index where the padding starts.
     *
     * @param in the buffer with the data and their padding
     * @param off the offset in <code>in</code> where the data starts
     * @param len the length of the data and their padding
     *
     * @return the index where the padding starts, or -1 if the input is
     * not properly padded
     */
    int unpad(byte[] in, int off, int len);

    /**
     * Determines how long the padding will be for a given input length.
     *
     * @param len the length of the data to pad
     *
     * @return the length of the padding
     */
    int padLength(int len);
}
