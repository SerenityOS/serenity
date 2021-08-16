/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.interpreter;

import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.utilities.*;

public class BytecodeLookupswitch extends Bytecode {
  BytecodeLookupswitch(Method method, int bci) {
    super(method, bci);
  }

  // Attributes
  public int defaultOffset() { return javaSignedWordAt(alignedOffset(1 + 0*jintSize)); }
  public int numberOfPairs() { return javaSignedWordAt(alignedOffset(1 + 1*jintSize)); }
  public LookupswitchPair pairAt(int i) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(0 <= i && i < numberOfPairs(), "pair index out of bounds");
    }
    return new LookupswitchPair(method, bci + alignedOffset(1 + (1 + i)*2*jintSize));
  }

  public void verify() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(isValid(), "check lookupswitch");
    }
  }

  public boolean isValid() {
    boolean result = javaCode() == Bytecodes._lookupswitch;
    if (result == false) return false;
    int i = numberOfPairs() - 1;
    while (i-- > 0) {
      if(pairAt(i).match() > pairAt(i+1).match())
         return false; // unsorted lookup table
    }
    return true;
  }

  public static BytecodeLookupswitch at(Method method, int bci) {
    BytecodeLookupswitch b = new BytecodeLookupswitch(method, bci);
    if (Assert.ASSERTS_ENABLED) {
      b.verify();
    }
    return b;
  }

  /** Like at, but returns null if the BCI is not at lookupswitch  */
  public static BytecodeLookupswitch atCheck(Method method, int bci) {
    BytecodeLookupswitch b = new BytecodeLookupswitch(method, bci);
    return (b.isValid() ? b : null);
  }

  public static BytecodeLookupswitch at(BytecodeStream bcs) {
    return new BytecodeLookupswitch(bcs.method(), bcs.bci());
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("lookupswitch");
    buf.append(spaces);
    buf.append("default: ");
    buf.append(bci() + defaultOffset());
    buf.append(comma);
    int i = numberOfPairs() - 1;
    while (i-- > 0) {
      LookupswitchPair pair = pairAt(i);
      buf.append("case ");
      buf.append(pair.match());
      buf.append(':');
      buf.append(bci() + pair.offset());
      buf.append(comma);
    }

    return buf.toString();
  }
}
