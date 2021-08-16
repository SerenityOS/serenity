/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.code;

import sun.jvm.hotspot.debugger.*;

public class CompressedReadStream extends CompressedStream {
  /** Equivalent to CompressedReadStream(buffer, 0) */
  public CompressedReadStream(Address buffer) {
    this(buffer, 0);
  }

  public CompressedReadStream(Address buffer, int position) {
    super(buffer, position);
  }

  public boolean readBoolean() {
    return (read() != 0);
  }

  public byte readByte() {
    return (byte) read();
  }

  public char readChar() {
    return (char) readInt();
  }

  public short readShort() {
    return (short) readSignedInt();
  }

  public int readSignedInt() {
    return decodeSign(readInt());
  }

  public int readInt() {
    int b0 = read();
    if (b0 < L) {
      return b0;
    } else {
      return readIntMb(b0);
    }
  }


  public float readFloat() {
    return Float.intBitsToFloat(reverseInt(readInt()));
  }

  public double readDouble() {
    int rh = readInt();
    int rl = readInt();
    int h = reverseInt(rh);
    int l = reverseInt(rl);
    return Double.longBitsToDouble(((long)h << 32) | ((long)l & 0x00000000FFFFFFFFL));
  }

  public long readLong() {
    long low = readSignedInt() & 0x00000000FFFFFFFFL;
    long high = readSignedInt();
    return (high << 32) | low;
  }

  //--------------------------------------------------------------------------------
  // Internals only below this point
  //


  // This encoding, called UNSIGNED5, is taken from J2SE Pack200.
  // It assumes that most values have lots of leading zeroes.
  // Very small values, in the range [0..191], code in one byte.
  // Any 32-bit value (including negatives) can be coded, in
  // up to five bytes.  The grammar is:
  //    low_byte  = [0..191]
  //    high_byte = [192..255]
  //    any_byte  = low_byte | high_byte
  //    coding = low_byte
  //           | high_byte low_byte
  //           | high_byte high_byte low_byte
  //           | high_byte high_byte high_byte low_byte
  //           | high_byte high_byte high_byte high_byte any_byte
  // Each high_byte contributes six bits of payload.
  // The encoding is one-to-one (except for integer overflow)
  // and easy to parse and unparse.

  private int readIntMb(int b0) {
    int pos = position - 1;
    int sum = b0;
    // must collect more bytes: b[1]...b[4]
    int lg_H_i = lg_H;
    for (int i = 0; ;) {
      int b_i = read(pos + (++i));
      sum += b_i << lg_H_i; // sum += b[i]*(64**i)
      if (b_i < L || i == MAX_i) {
        setPosition(pos+i+1);
        return sum;
      }
      lg_H_i += lg_H;
    }
  }

  private short read(int index) {
    return (short) buffer.getCIntegerAt(index, 1, true);
  }

  /** Reads an unsigned byte, but returns it as a short */
  private short read() {
    short retval = (short) buffer.getCIntegerAt(position, 1, true);
    ++position;
    return retval;
  }
}
