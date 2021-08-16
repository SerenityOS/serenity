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

import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.oops.Method;

public class DebugInfoReadStream extends CompressedReadStream {
  private NMethod code;
  private int InvocationEntryBCI;
  private List<ObjectValue> objectPool;

  public DebugInfoReadStream(NMethod code, int offset) {
    super(code.scopesDataBegin(), offset);
    InvocationEntryBCI = VM.getVM().getInvocationEntryBCI();
    this.code = code;
    this.objectPool = null;
  }

  public DebugInfoReadStream(NMethod code, int offset, List<ObjectValue> objectPool) {
    super(code.scopesDataBegin(), offset);
    InvocationEntryBCI = VM.getVM().getInvocationEntryBCI();
    this.code = code;
    this.objectPool = objectPool;
  }

  public OopHandle readOopHandle() {
    return code.getOopAt(readInt());
  }

  public Method readMethod() {
    return code.getMethodAt(readInt());
  }

  ScopeValue readObjectValue() {
    int id = readInt();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(objectPool != null, "object pool does not exist");
      for (Iterator itr = objectPool.iterator(); itr.hasNext();) {
        ObjectValue ov = (ObjectValue) itr.next();
        Assert.that(ov.id() != id, "should not be read twice");
      }
    }
    ObjectValue result = new ObjectValue(id);
    // Cache the object since an object field could reference it.
    objectPool.add(result);
    result.readObject(this);
    return result;
  }

  ScopeValue getCachedObject() {
    int id = readInt();
    Assert.that(objectPool != null, "object pool does not exist");
    for (Iterator itr = objectPool.iterator(); itr.hasNext();) {
      ObjectValue ov = (ObjectValue) itr.next();
      if (ov.id() == id) {
        return ov;
      }
    }
    Assert.that(false, "should not reach here");
    return null;
  }

  public int readBCI() {
    return readInt() + InvocationEntryBCI;
  }
}
