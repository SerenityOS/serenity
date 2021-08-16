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

package sun.jvm.hotspot.oops;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

// An ObjArray is an array containing oops

public class ObjArray extends Array {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type   = db.lookupType("objArrayOopDesc");
    elementSize = VM.getVM().getHeapOopSize();
  }

  ObjArray(OopHandle handle, ObjectHeap heap) {
    super(handle, heap);
  }

  public boolean isObjArray()          { return true; }

  private static long elementSize;

  public OopHandle getOopHandleAt(long index) {
    long offset = baseOffsetInBytes(BasicType.T_OBJECT) + (index * elementSize);
    if (VM.getVM().isCompressedOopsEnabled()) {
      return getHandle().getCompOopHandleAt(offset);
    } else {
      return getHandle().getOopHandleAt(offset);
    }
  }

  public Oop getObjAt(long index) {
      return getHeap().newOop(getOopHandleAt(index));
  }

  public void printValueOn(PrintStream tty) {
    tty.print("ObjArray");
  }

  public void iterateFields(OopVisitor visitor, boolean doVMFields) {
    super.iterateFields(visitor, doVMFields);
    int length = (int) getLength();
    long baseOffset = baseOffsetInBytes(BasicType.T_OBJECT);
    for (int index = 0; index < length; index++) {
      long offset = baseOffset + (index * elementSize);
      OopField field;
      if (VM.getVM().isCompressedOopsEnabled()) {
        field = new NarrowOopField(new IndexableFieldIdentifier(index), offset, false);
      } else {
        field = new OopField(new IndexableFieldIdentifier(index), offset, false);
      }
      visitor.doOop(field, false);
    }
  }
}
