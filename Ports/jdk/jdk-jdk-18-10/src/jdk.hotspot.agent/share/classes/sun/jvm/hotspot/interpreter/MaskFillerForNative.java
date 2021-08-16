/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;

/** Helper class for computing oop maps for native methods */

class MaskFillerForNative extends NativeSignatureIterator {
  MaskFillerForNative(Method method, BitMap mask, int maskSize) {
    super(method);
    this.mask = mask;
    this.size = maskSize;
  }

  public void passInt()    { /* ignore */ }
  public void passLong()   { /* ignore */ }
  public void passFloat()  { /* ignore */ }
  public void passDouble() { /* ignore */ }
  public void passObject() { mask.atPut(offset(), true); }

  public void generate() {
    super.iterate();
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //
  private BitMap mask;
  private int    size;
}
