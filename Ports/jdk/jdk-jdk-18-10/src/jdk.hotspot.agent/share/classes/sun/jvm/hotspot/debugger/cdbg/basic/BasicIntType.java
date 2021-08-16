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

package sun.jvm.hotspot.debugger.cdbg.basic;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;

public class BasicIntType extends BasicType implements IntType {
  private boolean unsigned;

  public BasicIntType(String name, int size, boolean unsigned) {
    this(name, size, unsigned, 0);
  }

  protected BasicIntType(String name, int size, boolean unsigned, int cvAttributes) {
    super(name, size, cvAttributes);
    this.unsigned = unsigned;
  }

  public IntType asInt()      { return this; }

  public int     getIntSize() { return getSize(); }
  public boolean isUnsigned() { return unsigned; }

  public void iterateObject(Address a, ObjectVisitor v, FieldIdentifier f) {
    v.doInt(f, a.getCIntegerAt(0, getSize(), isUnsigned()));
  }

  protected Type createCVVariant(int cvAttributes) {
    return new BasicIntType(getName(), getSize(), isUnsigned(), cvAttributes);
  }

  public void visit(TypeVisitor v) {
    v.doIntType(this);
  }
}
