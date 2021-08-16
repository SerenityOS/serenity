/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.zip.CRC32;

/**
 * Checksum provides methods for calculating a CRC32 value for a
 * transitions table.
 *
 * @since 1.4
 */
public class Checksum extends CRC32
{
    /**
     * Updates the CRC32 value from each byte of the given int
     * value. The bytes are used in the big endian order.
     * @param val the int value
     */
    public void update(int val) {
        byte[] b = new byte[4];
        b[0] = (byte)((val >>> 24) & 0xff);
        b[1] = (byte)((val >>> 16) & 0xff);
        b[2] = (byte)((val >>> 8) & 0xff);
        b[3] = (byte)(val & 0xff);
        update(b);
    }

    /**
     * Updates the CRC32 value from each byte of the given long
     * value. The bytes are used in the big endian order.
     * @param val the long value
     */
    void update(long val) {
        byte[] b = new byte[8];
        b[0] = (byte)((val >>> 56) & 0xff);
        b[1] = (byte)((val >>> 48) & 0xff);
        b[2] = (byte)((val >>> 40) & 0xff);
        b[3] = (byte)((val >>> 32) & 0xff);
        b[4] = (byte)((val >>> 24) & 0xff);
        b[5] = (byte)((val >>> 16) & 0xff);
        b[6] = (byte)((val >>> 8) & 0xff);
        b[7] = (byte)(val & 0xff);
        update(b);
    }
}
