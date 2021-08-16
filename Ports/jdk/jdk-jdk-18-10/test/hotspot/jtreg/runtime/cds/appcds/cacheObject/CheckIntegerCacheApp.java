/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import sun.hotspot.WhiteBox;

//
// Help test archived box cache consistency.
//
// Takes two arguments:
// 0: the expected maximum value expected to be archived
// 1: if the values are expected to be retrieved from the archive or not
//    (only applies to IntegerCache; other caches should always be mapped
//    from archive)
//
public class CheckIntegerCacheApp {
    static WhiteBox wb;

    public static void main(String[] args) throws Exception {
        wb = WhiteBox.getWhiteBox();

        if (!wb.areOpenArchiveHeapObjectsMapped()) {
            System.out.println("This may happen during normal operation. Test Skipped.");
            return;
        }

        if (args.length != 2) {
            throw new RuntimeException(
                    "FAILED. Incorrect argument length: " + args.length);
        }

        boolean archivedExpected = Boolean.parseBoolean(args[1]);

        // Base JLS compliance check
        for (int i = -128; i <= 127; i++) {
            if (Integer.valueOf(i) != Integer.valueOf(i)) {
                throw new RuntimeException(
                        "FAILED. All values in range [-128, 127] should be interned in cache: " + i);
            }
            if (Byte.valueOf((byte)i) != Byte.valueOf((byte)i)) {
                throw new RuntimeException(
                        "FAILED. All Byte values in range [-128, 127] should be interned in cache: " + (byte)i);
            }
            if (Short.valueOf((short)i) != Short.valueOf((short)i)) {
                throw new RuntimeException(
                        "FAILED. All Short values in range [-128, 127] should be interned in cache: " + (byte)i);
            }
            if (Long.valueOf(i) != Long.valueOf(i)) {
                throw new RuntimeException(
                        "FAILED. All Long values in range [-128, 127] should be interned in cache: " + i);
            }
            checkArchivedAsExpected(archivedExpected, Integer.valueOf(i));
            checkArchivedAsExpected(true, Byte.valueOf((byte)i));
            checkArchivedAsExpected(true, Short.valueOf((short)i));
            checkArchivedAsExpected(true, Long.valueOf(i));

            // Character cache only values 0 through 127
            if (i >= 0) {
                if (Character.valueOf((char)i) != Character.valueOf((char)i)) {
                    throw new RuntimeException(
                            "FAILED. All Character values in range [0, 127] should be interned in cache: " + i);
                }
                checkArchivedAsExpected(true, Character.valueOf((char)i));
            }
        }

        int high = Integer.parseInt(args[0]);
        if (Integer.valueOf(high) != Integer.valueOf(high)) {
            throw new RuntimeException(
                    "FAILED. Value expected to be retrieved from cache: " + high);
        }
        checkArchivedAsExpected(archivedExpected, Integer.valueOf(high));

        if (Integer.valueOf(high + 1) == Integer.valueOf(high + 1)) {
            throw new RuntimeException(
                    "FAILED. Value not expected to be retrieved from cache: " + high);
        }
        checkArchivedAsExpected(false, Integer.valueOf(high + 1));
        checkArchivedAsExpected(false, Short.valueOf((short)128));
        checkArchivedAsExpected(false, Long.valueOf(128));
        checkArchivedAsExpected(false, Character.valueOf((char)128));
    }

    private static void checkArchivedAsExpected(boolean archivedExpected, Object value) {
        if (archivedExpected) {
            if (!wb.isShared(value)) {
                throw new RuntimeException(
                        "FAILED. Value expected to be archived: " + value +
                        " of type " + value.getClass().getName());
            }
        } else {
            if (wb.isShared(value)) {
                throw new RuntimeException(
                        "FAILED. Value not expected to be archived: " + value +
                        " of type " + value.getClass().getName());
            }
        }
    }
}
