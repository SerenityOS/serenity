/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/** A BitMap implementing the BitMapInterface. */
public class BitMapSegmented implements BitMapInterface {
  private static final int SegmentSizeBits = 30;
  private static final int SegmentSize = 1 << (SegmentSizeBits - 1);

  public BitMapSegmented(long sizeInBits) {
    this.size = sizeInBits;

    if (sizeInBits == 0) {
      segmentBitMaps = new BitMap[0];
      return;
    }

    int lastSegmentSize = (int)(sizeInBits % SegmentSize);

    int segments = segmentIndex(sizeInBits - 1) + 1;
    int completeSegments = segments - ((lastSegmentSize != 0) ? 1 : 0);

    segmentBitMaps = new BitMap[segments];

    for (int i = 0; i < completeSegments; i++) {
      segmentBitMaps[i] = new BitMap(SegmentSize);
    }

    if (lastSegmentSize != 0) {
      segmentBitMaps[completeSegments] = new BitMap(lastSegmentSize);
    }
  }

  public long size() {
    return size;
  }

  // Accessors
  public boolean at(long offset) {
    assert offset < size;

    int segmentIndex = segmentIndex(offset);
    int segmentOffset = segmentOffset(offset);
    return segmentBitMaps[segmentIndex].at(segmentOffset);
  }

  public void atPut(long offset, boolean value) {
    assert offset < size;

    int segmentIndex = segmentIndex(offset);
    int segmentOffset = segmentOffset(offset);
    segmentBitMaps[segmentIndex].atPut(segmentOffset, value);
  }

  public void clear() {
    for (BitMap map : segmentBitMaps) {
      map.clear();
    }
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //
  private final long     size; // in bits
  private final BitMap[] segmentBitMaps;

  private int segmentIndex(long offset) {
    long longIndex = offset / SegmentSize;

    assert longIndex < Integer.MAX_VALUE;
    return (int)longIndex;
  }

  private int segmentOffset(long offset) {
    return (int)(offset % SegmentSize);
  }
}
