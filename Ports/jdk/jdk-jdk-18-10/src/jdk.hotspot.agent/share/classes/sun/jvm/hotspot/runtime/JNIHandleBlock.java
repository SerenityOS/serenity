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

package sun.jvm.hotspot.runtime;

import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** */
public class JNIHandleBlock extends VMObject {
  private static Field         handlesField;
  private static CIntegerField topField;
  private static AddressField  nextField;

  private static int           blockSizeInOops;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) {
    Type type = db.lookupType("JNIHandleBlock");

    handlesField = type.getField("_handles");
    topField = type.getCIntegerField("_top");
    nextField = type.getAddressField("_next");

    blockSizeInOops = db.lookupIntConstant("JNIHandleBlock::block_size_in_oops").intValue();
  }

  public JNIHandleBlock(Address addr) {
    super(addr);
  }

  public JNIHandleBlock next() {
    Address handleAddr = nextField.getValue(addr);
    if (handleAddr == null) {
      return null;
    }

    /* the next handle block is valid only if the current block is full */
    if (top() < blockSizeInOops) {
      return null;
    }
    return new JNIHandleBlock(handleAddr);
  }

  public int top() {
    return (int) topField.getValue(addr);
  }

  public void oopsDo(AddressVisitor visitor) {
    // Visit handles in this block
    for (int i = 0; i < top(); i++) {
      Address cur = getOopHandleAddress(i);
      if (cur != null) {
        visitor.visitAddress(cur);
      }
    }

    // Visit handles in subsequent blocks if necessary
    JNIHandleBlock n = next();
    if (n != null) {
      n.oopsDo(visitor);
    }
  }

  public OopHandle getOopHandle(int x) {
    Address oopAddr = getOopHandleAddress(x);
    if (oopAddr != null) {
      return oopAddr.getOopHandleAt(0);
    }
    return null;
  }

  /** Debugging routine only. Returns non-null JNIHandleBlock
      containing the JNI handle or null if this handle block and its
      successors did not contain it. */
  public JNIHandleBlock blockContainingHandle(Address jniHandle) {
    JNIHandleBlock cur = this;
    while (cur != null) {
      if (indexOfHandle(jniHandle) >= 0) {
        return cur;
      }
      cur = cur.next();
    }
    return null;
  }

  /** Debugging routine: returns the index (0..top() - 1) of the
      handle in this block, or -1 if the handle was not contained in
      this block. Does not search successor blocks. */
  public int indexOfHandle(Address jniHandle) {
    for (int i = 0; i < top(); i++) {
      Address addr = getOopHandleAddress(i);
      if (addr != null) {
        if (addr.equals(jniHandle)) {
          return i;
        }
      }
    }
    return -1;
  }

  public String toString() {
    Address handleBase = addr.addOffsetTo(handlesField.getOffset());
    Address handleEnd = addr.addOffsetTo(handlesField.getOffset() + top() * VM.getVM().getOopSize());
    return "JNIHandleBlock [" + handleBase + ", " + handleEnd + ")";
  }

  /** Only returns addresses of valid OopHandles */
  private Address getOopHandleAddress(int x) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(x < top(), "out of bounds");
    }

    Address oopAddr = addr.addOffsetTo(handlesField.getOffset() + x * VM.getVM().getOopSize());
    OopHandle handle = oopAddr.getOopHandleAt(0);
    if (VM.getVM().getUniverse().isInReserved(handle)) {
      /* the oop handle is valid only if it is not freed (i.e. reserved in heap) */
      return oopAddr;
    } else {
      return null;
    }
  }
}
