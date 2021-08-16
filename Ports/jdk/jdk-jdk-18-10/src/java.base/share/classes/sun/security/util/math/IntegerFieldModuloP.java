/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util.math;

import java.math.BigInteger;

/**
 * An interface for the field of integers modulo a prime number. An
 * implementation of this interface can be used to get properties of the
 * field and to produce field elements of type ImmutableIntegerModuloP from
 * other objects and representations of field elements.
 */

public interface IntegerFieldModuloP {

    /**
     * Get the size of the field as a BigInteger. This size is equal to the
     * prime modulus used to construct the field.
     *
     * @return the size of the field.
     */
    BigInteger getSize();

    /**
     * Get the additive identity element 0
     *
     * @return the additive identity element
     */
    ImmutableIntegerModuloP get0();

    /**
     * Get the multiplicative identity element 1
     *
     * @return the multiplicative identity element
     */
    ImmutableIntegerModuloP get1();

    /**
     * Get the field element equivalent to the supplied BigInteger value. The
     * supplied value may be negative or larger than the modulus that defines
     * the field.
     *
     * @param v a BigInteger value
     * @return the field element corresponding to v
     */
    ImmutableIntegerModuloP getElement(BigInteger v);

    /**
     * Get a "small" value according to this implementation. This value may
     * be used in optimized forms of some operations to avoid unnecessary
     * calculations. For example, multiplication is much faster when it is
     * known that one of the numbers fits within a single limb.
     *
     * The definition of "small", and the range of accepted values, is
     * implementation-specific.
     *
     * @param v the small integer value
     * @throws IllegalArgumentException when the value is not small
     */
    SmallValue getSmallValue(int v);

    /**
     * Get a field element from a little-endian unsigned integer stored in an
     * array. The entire array will be used, and the supplied value may be
     * larger than the modulus that defines the field. The array will not be
     * modified.
     *
     * @param v an array containing a little-endian unsigned integer
     * @return the field element corresponding to v
     */
    default ImmutableIntegerModuloP getElement(byte[] v) {
        return getElement(v, 0, v.length, (byte) 0);
    }

    /**
     * Get a field element from a little-endian unsigned integer stored at the
     * specified position in an array. The supplied value may be
     * larger than the modulus that defines the field. This method also takes
     * a byte which is interpreted as an additional high-order byte of the
     * number. The array will not be modified.
     *
     * @param v an array containing a little-endian unsigned integer
     * @param offset the starting position of the integer
     * @param length the number of bytes to read
     * @param highByte the high-order byte of the number
     * @return the field element corresponding to the bytes at the specified
     *     position
     */
    ImmutableIntegerModuloP getElement(byte[] v, int offset, int length,
                                       byte highByte);
}

