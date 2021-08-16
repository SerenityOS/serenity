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

package sun.jvm.hotspot.runtime;

import sun.jvm.hotspot.oops.*;

public abstract class SignatureInfo extends SignatureIterator {
  protected boolean hasIterated; // need this because iterate cannot be called in constructor (set is virtual!)
  protected int     size;
  protected int     type;        // BasicType

  protected void lazyIterate() {
    if (!hasIterated) {
      iterate();
      hasIterated = true;
    }
  }

  protected abstract void set(int size, int /*BasicType*/ type);

  public void doBool()                     { set(BasicTypeSize.getTBooleanSize(), BasicType.getTBoolean()); }
  public void doChar()                     { set(BasicTypeSize.getTCharSize(),    BasicType.getTChar());    }
  public void doFloat()                    { set(BasicTypeSize.getTFloatSize(),   BasicType.getTFloat());   }
  public void doDouble()                   { set(BasicTypeSize.getTDoubleSize(),  BasicType.getTDouble());  }
  public void doByte()                     { set(BasicTypeSize.getTByteSize(),    BasicType.getTByte());    }
  public void doShort()                    { set(BasicTypeSize.getTShortSize(),   BasicType.getTShort());   }
  public void doInt()                      { set(BasicTypeSize.getTIntSize(),     BasicType.getTInt());     }
  public void doLong()                     { set(BasicTypeSize.getTLongSize(),    BasicType.getTLong());    }
  public void doVoid()                     { set(BasicTypeSize.getTVoidSize(),    BasicType.getTVoid());    }
  public void doObject(int begin, int end) { set(BasicTypeSize.getTObjectSize(),  BasicType.getTObject());  }
  public void doArray(int begin, int end)  { set(BasicTypeSize.getTArraySize(),   BasicType.getTArray());   }

  public SignatureInfo(Symbol signature) {
    super(signature);

    type = BasicType.getTIllegal();
  }

  public int size() { lazyIterate(); return size; }
  public int type() { lazyIterate(); return type; }
}
