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

public class BytecodeTableswitch extends Bytecode {
  BytecodeTableswitch(Method method, int bci) {
    super(method, bci);
  }


  // Attributes
  public int  defaultOffset()     { return javaSignedWordAt(alignedOffset(1 + 0*jintSize)); }
  public int  lowKey()            { return javaSignedWordAt(alignedOffset(1 + 1*jintSize)); }
  public int  highKey()           { return javaSignedWordAt(alignedOffset(1 + 2*jintSize)); }
  public int  length()            { return highKey()-lowKey()+1; }
  public int  destOffsetAt(int i) {
    int x2 = alignedOffset(1 + (3 + i)*jintSize);
    int val = javaSignedWordAt(x2);
    return javaSignedWordAt(alignedOffset(1 + (3 + i)*jintSize));
  }

  public void verify() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(isValid(), "check tableswitch");
    }
  }

  public boolean isValid() {
    boolean result = javaCode() == Bytecodes._tableswitch;
    if (result == false) return false;
    int lo = lowKey();
    int hi = highKey();
    if (hi < lo) // incorrect hi/lo values in tableswitch
       return false;

    int i  = hi - lo - 1 ;
    while (i-- > 0) {
      // no special check needed
    }
    return true;
  }

  public static BytecodeTableswitch at(Method method, int bci) {
    BytecodeTableswitch b = new BytecodeTableswitch(method, bci);
    if (Assert.ASSERTS_ENABLED) {
      b.verify();
    }
    return b;
  }

  /** Like at, but returns null if the BCI is not at tableswitch  */
  public static BytecodeTableswitch atCheck(Method method, int bci) {
    BytecodeTableswitch b = new BytecodeTableswitch(method, bci);
    return (b.isValid() ? b : null);
  }

  public static BytecodeTableswitch at(BytecodeStream bcs) {
    return new BytecodeTableswitch(bcs.method(), bcs.bci());
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("tableswitch");
    buf.append(spaces);
    buf.append("default: ");
    buf.append(bci() + defaultOffset());
    buf.append(comma);
    int lo = lowKey();
    int hi = highKey();
    int i  = hi - lo - 1 ;
    while (i-- > 0) {
       buf.append("case ");
       buf.append(lo + i);
       buf.append(':');
       buf.append(bci() + destOffsetAt(i));
       buf.append(comma);
    }
    return buf.toString();
  }
}
