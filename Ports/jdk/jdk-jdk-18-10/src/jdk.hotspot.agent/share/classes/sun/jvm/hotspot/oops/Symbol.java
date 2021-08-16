/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

// A Symbol is a canonicalized string.
// All Symbols reside in global symbolTable.

public class Symbol extends VMObject {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type  = db.lookupType("Symbol");
    lengthField = type.getCIntegerField("_length");
    baseOffset = type.getField("_body").getOffset();
    idHashAndRefcount = type.getCIntegerField("_hash_and_refcount");
  }

  public static Symbol create(Address addr) {
    if (addr == null) {
      return null;
    }
    return new Symbol(addr);
  }

  Symbol(Address addr) {
    super(addr);
  }

  public boolean isSymbol()            { return true; }

  private static long baseOffset; // tells where the array part starts

  // Fields
  private static CIntegerField lengthField;
  // idHash is a short packed into the high bits of a 32-bit integer with refcount
  private static CIntegerField idHashAndRefcount;

  // Accessors for declared fields
  public long getLength() {
    return lengthField.getValue(this.addr);
  }

  public byte getByteAt(long index) {
    return addr.getJByteAt(baseOffset + index);
  }

  public boolean equals(byte[] modUTF8Chars) {
    int l = (int) getLength();
    if (l != modUTF8Chars.length) return false;
    while (l-- > 0) {
      if (modUTF8Chars[l] != getByteAt(l)) return false;
    }
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(l == -1, "we should be at the beginning");
    }
    return true;
  }

  public boolean equals(String string) {
    return asString().equals(string);
  }

  public byte[] asByteArray() {
    int length = (int) getLength();
    byte [] result = new byte [length];
    for (int index = 0; index < length; index++) {
      result[index] = getByteAt(index);
    }
    return result;
  }

  public String asString() {
    // Decode the byte array and return the string.
    try {
      return readModifiedUTF8(asByteArray());
    } catch(Exception e) {
      System.err.println(addr);
      e.printStackTrace();
      return null;
    }
  }

  public boolean startsWith(String str) {
    return asString().startsWith(str);
  }

  public void printValueOn(PrintStream tty) {
    tty.print("#" + asString());
  }

  /** Note: this comparison is used for vtable sorting only; it
      doesn't matter what order it defines, as long as it is a total,
      time-invariant order Since Symbol* are in C_HEAP, their
      relative order in memory never changes, so use address
      comparison for speed. */
  public long fastCompare(Symbol other) {
    return addr.minus(other.addr);
  }

  private static String readModifiedUTF8(byte[] buf) throws IOException {
    final int len = buf.length;
    byte[] tmp = new byte[len + 2];
    // write modified UTF-8 length as short in big endian
    tmp[0] = (byte) ((len >>> 8) & 0xFF);
    tmp[1] = (byte) ((len >>> 0) & 0xFF);
    // copy the data
    System.arraycopy(buf, 0, tmp, 2, len);
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(tmp));
    return dis.readUTF();
  }
}
