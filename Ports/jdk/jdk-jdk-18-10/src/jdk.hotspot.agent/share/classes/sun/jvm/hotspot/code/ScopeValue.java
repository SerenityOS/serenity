/*
 * Copyright (c) 2000, 2009, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;

import sun.jvm.hotspot.utilities.*;

/** <P> Classes used for serializing debugging information. These
    abstractions are introducted to provide symmetric read and write
    operations. </P>

    <P>
    <UL>
    <LI> ScopeValue: describes the value of a variable/expression in a scope
    <UL>
    <LI> LocationValue: describes a value in a given location (in frame or register)
    <LI> ConstantValue: describes a constant
    </UL>
    </UL>
    </P> */

public abstract class ScopeValue {
  // Package private enumeration values (FIXME: read from target VM)
  static final int LOCATION_CODE        = 0;
  static final int CONSTANT_INT_CODE    = 1;
  static final int CONSTANT_OOP_CODE    = 2;
  static final int CONSTANT_LONG_CODE   = 3;
  static final int CONSTANT_DOUBLE_CODE = 4;
  static final int CONSTANT_OBJECT_CODE = 5;
  static final int CONSTANT_OBJECT_ID_CODE = 6;

  public boolean isLocation()       { return false; }
  public boolean isConstantInt()    { return false; }
  public boolean isConstantDouble() { return false; }
  public boolean isConstantLong()   { return false; }
  public boolean isConstantOop()    { return false; }
  public boolean isObject()         { return false; }

  public static ScopeValue readFrom(DebugInfoReadStream stream) {
    switch (stream.readInt()) {
    case LOCATION_CODE:
      return new LocationValue(stream);
    case CONSTANT_INT_CODE:
      return new ConstantIntValue(stream);
    case CONSTANT_OOP_CODE:
      return new ConstantOopReadValue(stream);
    case CONSTANT_LONG_CODE:
      return new ConstantLongValue(stream);
    case CONSTANT_DOUBLE_CODE:
      return new ConstantDoubleValue(stream);
    case CONSTANT_OBJECT_CODE:
      return stream.readObjectValue();
    case CONSTANT_OBJECT_ID_CODE:
      return stream.getCachedObject();
    default:
      Assert.that(false, "should not reach here");
      return null;
    }
  }

  public abstract void printOn(PrintStream tty);
}
