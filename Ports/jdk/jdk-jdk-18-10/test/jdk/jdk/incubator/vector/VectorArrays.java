/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import jdk.incubator.vector.*;

public class VectorArrays {
    static boolean equals(byte[] a, byte[] b) {
        if (a == b)
            return true;
        if (a == null || b == null)
            return false;

        int length = a.length;
        if (b.length != length)
            return false;

        return mismatch(a, b) < 0;
    }

    static int compare(byte[] a, byte[] b) {
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int i = mismatch(a, b);
        if (i >= 0) {
            return Byte.compare(a[i], b[i]);
        }

        return a.length - b.length;

    }

    static int mismatch(byte[] a, byte[] b) {
        VectorSpecies<Byte> species = ByteVector.SPECIES_256;
        return mismatch(a, b, species);
    }

    static int mismatch(byte[] a, byte[] b, VectorSpecies<Byte> species) {
        int length = Math.min(a.length, b.length);
        if (a == b)
            return -1;

        int i = 0;
        // @@@ Method on species to truncate the length?
        for (; i < (length & ~(species.length() - 1)); i += species.length()) {
            Vector<Byte> va = ByteVector.fromArray(species, a, i);
            Vector<Byte> vb = ByteVector.fromArray(species, b, i);
            VectorMask<Byte> m = va.compare(VectorOperators.NE, vb);
            // @@@ count number of leading zeros with explicit method
            if (m.anyTrue()) {
                break; // mismatch found
            }
        }

        // @@@ Can use a shape of half the bit size
        //     or a mask
        for (; i < length; i++) {
            if (a[i] != b[i])
                return i;
        }

        return (a.length != b.length) ? length : -1;
    }
}
