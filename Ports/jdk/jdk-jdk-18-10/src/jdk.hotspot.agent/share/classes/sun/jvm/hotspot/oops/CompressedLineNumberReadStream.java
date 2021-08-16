/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.oops;

import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.debugger.*;

public class CompressedLineNumberReadStream extends CompressedReadStream {
  /** Equivalent to CompressedLineNumberReadStream(buffer, 0) */
  public CompressedLineNumberReadStream(Address buffer) {
    this(buffer, 0);
  }

  public CompressedLineNumberReadStream(Address buffer, int position) {
    super(buffer, position);
  }

  /** Read (bci, line number) pair from stream. Returns false at end-of-stream. */
  public boolean readPair() {
    int next = readByte() & 0xFF;
    // Check for terminator
    if (next == 0) return false;
    if (next == 0xFF) {
      // Escape character, regular compression used
      bci  += readSignedInt();
      line += readSignedInt();
    } else {
      // Single byte compression used
      bci  += next >> 3;
      line += next & 0x7;
    }
    return true;
  }

  public int bci()  { return bci;  }
  public int line() { return line; }

  //--------------------------------------------------------------------------------
  // Internals only below this point
  //

  private int bci;
  private int line;
}
