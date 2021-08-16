/*
 * Copyright (c) 2001, 2007, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.utilities;

import sun.jvm.hotspot.debugger.*;

/** Manages a bitmap of the specified bit size */
public class BitMap {
  public BitMap(int sizeInBits) {
    this.size = sizeInBits;
    int nofWords = sizeInWords();
    data = new int[nofWords];
  }

  public int size() {
    return size;
  }

  // Accessors
  public boolean at(int offset) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(offset>=0 && offset < size(), "BitMap index out of bounds");
    }
    return Bits.isSetNthBit(wordFor(offset), offset % bitsPerWord);
  }

  public void atPut(int offset, boolean value) {
    int index = indexFor(offset);
    int pos   = offset % bitsPerWord;
    if (value) {
      data[index] = Bits.setNthBit(data[index], pos);
    } else {
      data[index] = Bits.clearNthBit(data[index], pos);
    }
  }

  public void set_size(int value) {
    size = value;
  }

  public void set_map(Address addr) {
    for (int i=0; i<sizeInWords(); i++) {
      data[i] =  (int) addr.getCIntegerAt(0, bytesPerWord, true);
      addr = addr.addOffsetTo(bytesPerWord);
    }

  }

  public void clear() {
    for (int i = 0; i < sizeInWords(); i++) {
      data[i] = Bits.NoBits;
    }
  }

  public void iterate(BitMapClosure blk) {
    for (int index = 0; index < sizeInWords(); index++) {
      int rest = data[index];
      for (int offset = index * bitsPerWord; rest != Bits.NoBits; offset++) {
        if (rest % 2 == 1) {
          if (offset < size()) {
            blk.doBit(offset);
          } else {
            return; // Passed end of map
          }
        }
        rest = rest >>> 1;
      }
    }
  }

  /** Sets this bitmap to the logical union of it and the
      argument. Both bitmaps must be the same size. Returns true if a
      change was caused in this bitmap. */
  public boolean setUnion(BitMap other) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(size() == other.size(), "must have same size");
    }
    boolean changed = false;
    for (int index = 0; index < sizeInWords(); index++) {
      int temp = data[index] | other.data[index];
      changed = changed || (temp != data[index]);
      data[index] = temp;
    }
    return changed;
  }

  /** Sets this bitmap to the logical intersection of it and the
      argument. Both bitmaps must be the same size. */
  public void setIntersection(BitMap other) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(size() == other.size(), "must have same size");
    }
    for (int index = 0; index < sizeInWords(); index++) {
      data[index] = data[index] & (other.data[index]);
    }
  }

  /** Sets this bitmap to the contents of the argument. Both bitmaps
      must be the same size. */
  public void setFrom(BitMap other) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(size() == other.size(), "must have same size");
    }
    for (int index = 0; index < sizeInWords(); index++) {
      data[index] = other.data[index];
    }
  }

  /** Sets this bitmap to the logical difference between it and the
      argument; that is, any bits that are set in the argument are
      cleared in this bitmap. Both bitmaps must be the same size.
      Returns true if a change was caused in this bitmap. */
  public boolean setDifference(BitMap other) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(size() == other.size(), "must have same size");
    }
    boolean changed = false;
    for (int index = 0; index < sizeInWords(); index++) {
      int temp = data[index] & ~(other.data[index]);
      changed = changed || (temp != data[index]);
      data[index] = temp;
    }
    return changed;
  }

  /** Both bitmaps must be the same size. */
  public boolean isSame(BitMap other) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(size() == other.size(), "must have same size");
    }
    for (int index = 0; index < sizeInWords(); index++) {
      if (data[index] != (other.data[index])) return false;
    }
    return true;
  }

  public int getNextOneOffset(int l_offset, int r_offset) {
    if (l_offset == r_offset) {
      return l_offset;
    }

    int index = indexFor(l_offset);
    int r_index = indexFor(r_offset);
    int res_offset = l_offset;

    int pos = bitInWord(res_offset);
    int res = data[index] >> pos;

    if (res != 0) {
      // find the position of the 1-bit
      for (; (res & 1) == 0; res_offset++) {
        res = res >> 1;
      }
      return res_offset;
    }
    // skip over all word length 0-bit runs
    for (index++; index < r_index; index++) {
      res = data[index];
      if (res != 0) {
        // found a 1, return the offset
        for (res_offset = index * bitsPerWord; (res & 1) == 0; res_offset++) {
          res = res >> 1;
        }
        return res_offset;
      }
    }
    return r_offset;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //
  private int   size; // in bits
  private int[] data;
  private static final int bitsPerWord = 32;
  private static final int bytesPerWord = 4;

  private int sizeInWords() {
    return (size() + bitsPerWord - 1) / bitsPerWord;
  }

  private int indexFor(int offset) {
    return offset / bitsPerWord;
  }

  private int wordFor(int offset) {
    return data[offset / bitsPerWord];
  }

  private int bitInWord(int offset) {
    return offset & (bitsPerWord - 1);
  }
}
