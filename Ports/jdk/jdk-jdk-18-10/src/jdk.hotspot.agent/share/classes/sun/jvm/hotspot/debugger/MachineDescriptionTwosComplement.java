/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger;

/** Base class for all twos-complement machine descriptions, which
    handles the cIntegerType{Min,Max}Value methods. */

public abstract class MachineDescriptionTwosComplement {

  /** Handles 1, 2, 4, and 8-byte signed integers */
  private static final long[] signedMinValues = {
    Byte.MIN_VALUE,
    Short.MIN_VALUE,
    Integer.MIN_VALUE,
    Long.MIN_VALUE
  };

  /** Handles 1, 2, 4, and 8-byte signed integers */
  private static final long[] signedMaxValues = {
    Byte.MAX_VALUE,
    Short.MAX_VALUE,
    Integer.MAX_VALUE,
    Long.MAX_VALUE
  };

  /** Handles 1, 2, and 4-byte unsigned integers properly, with a bug
      in the 8-byte unsigned integer's constant */
  private static final long[] unsignedMaxValues = {
    255L,
    65535L,
    4294967295L,
    -1L
  };

  public long cIntegerTypeMaxValue(long sizeInBytes, boolean isUnsigned) {
    if (isUnsigned) {
      // Would be nice to signal to the caller that 8-byte unsigned
      // integers are not supported properly, but it looks like doing
      // so at this level will cause problems above

      return tableLookup(sizeInBytes, unsignedMaxValues);
    } else {
      return tableLookup(sizeInBytes, signedMaxValues);
    }
  };

  public long cIntegerTypeMinValue(long sizeInBytes, boolean isUnsigned) {
    if (isUnsigned) {
      return 0;
    }

    return tableLookup(sizeInBytes, signedMinValues);
  }

  // Nearly all of the supported machines are not LP64 */
  public boolean isLP64() {
    return false;
  }

  private long tableLookup(long sizeInBytes, long[] table) {
    switch ((int) sizeInBytes) {
    case 1:
      return table[0];
    case 2:
      return table[1];
    case 4:
      return table[2];
    case 8:
      return table[3];
    default:
      throw new IllegalArgumentException("C integer type of " + sizeInBytes + " not supported");
    }
  }
}
