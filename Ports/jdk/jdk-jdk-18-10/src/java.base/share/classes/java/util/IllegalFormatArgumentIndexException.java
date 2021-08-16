/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * Unchecked exception thrown when the argument index is not within the valid
 * range of supported argument index values. If an index value isn't
 * representable by an {@code int} type, then the value
 * {@code Integer.MIN_VALUE} will be used in the exception.
 *
 * @since 16
 */
class IllegalFormatArgumentIndexException extends IllegalFormatException {

    @java.io.Serial
    private static final long serialVersionUID = 4191767811181838112L;

    private final int illegalIndex;

    /**
     * Constructs an instance of this class with the specified argument index
     * @param index The value of a corresponding illegal argument index.
     */
    IllegalFormatArgumentIndexException(int index) {
        illegalIndex = index;
    }

    /**
     * Gets the value of the illegal index.
     * Returns {@code Integer.MIN_VALUE} if the illegal index is not
     * representable by an {@code int}.
     * @return the illegal index value
     */
    int getIndex() {
        return illegalIndex;
    }

    @Override
    public String getMessage() {
        int index = getIndex();

        if (index == Integer.MIN_VALUE) {
           return "Format argument index: (not representable as int)";
        }

        return String.format("Illegal format argument index = %d", getIndex());
    }

}
