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
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

// Oop represents the superclass for all types of
// objects in the HotSpot object heap.

public class Oop {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type  = db.lookupType("oopDesc");
    mark       = new CIntField(type.getCIntegerField("_mark"), 0);
    klass      = new MetadataField(type.getAddressField("_metadata._klass"), 0);
    compressedKlass  = new NarrowKlassField(type.getAddressField("_metadata._compressed_klass"), 0);
    headerSize = type.getSize();
  }

  private OopHandle  handle;
  private ObjectHeap heap;

  Oop(OopHandle handle, ObjectHeap heap) {
    this.handle = handle;
    this.heap   = heap;
  }

  ObjectHeap getHeap()   { return heap; }

  /** Should not be used or needed by most clients outside this
      package; is needed, however, by {@link
      sun.jvm.hotspot.utilities.MarkBits}. */
  public OopHandle getHandle() { return handle; }

  private static long headerSize;
  public  static long getHeaderSize() { return headerSize; } // Header size in bytes.

  private static CIntField mark;
  private static MetadataField  klass;
  private static NarrowKlassField compressedKlass;

  // Accessors for declared fields
  public Mark  getMark()   { return new Mark(getHandle()); }
  public Klass getKlass() {
    if (VM.getVM().isCompressedKlassPointersEnabled()) {
      return (Klass)compressedKlass.getValue(getHandle());
    } else {
      return (Klass)klass.getValue(getHandle());
    }
  }

  public boolean isA(Klass k) {
    return getKlass().isSubtypeOf(k);
  }

  // Returns the byte size of this object
  public long getObjectSize() {
    Klass k = getKlass();
    // All other types should be overriding getObjectSize directly
    return ((InstanceKlass)k).getObjectSize(this);
  }

  // Type test operations
  public boolean isInstance()          { return false; }
  public boolean isInstanceRef()       { return false; }
  public boolean isArray()             { return false; }
  public boolean isObjArray()          { return false; }
  public boolean isTypeArray()         { return false; }
  public boolean isThread()            { return false; }

  // Align the object size.
  public static long alignObjectSize(long size) {
    return VM.getVM().alignUp(size, VM.getVM().getMinObjAlignmentInBytes());
  }

  // All vm's align longs, so pad out certain offsets.
  public static long alignObjectOffset(long offset) {
    return VM.getVM().alignUp(offset, VM.getVM().getBytesPerLong());
  }

  public boolean equals(Object obj) {
    if (obj != null && (obj instanceof Oop)) {
      return getHandle().equals(((Oop) obj).getHandle());
    }
    return false;
 }

  public int hashCode() { return getHandle().hashCode(); }

  /** Identity hash in the target VM */
  public long identityHash() {
    Mark mark = getMark();
    if (mark.isUnlocked() && (!mark.hasNoHash())) {
      return (int) mark.hash();
    } else if (mark.isMarked()) {
      return (int) mark.hash();
    } else {
      return slowIdentityHash();
    }
  }

  public long slowIdentityHash() {
    return VM.getVM().getObjectSynchronizer().identityHashValueFor(this);
  }

  public void iterate(OopVisitor visitor, boolean doVMFields) {
    visitor.setObj(this);
    visitor.prologue();
    iterateFields(visitor, doVMFields);
    visitor.epilogue();
  }

  void iterateFields(OopVisitor visitor, boolean doVMFields) {
    if (doVMFields) {
      visitor.doCInt(mark, true);
      if (VM.getVM().isCompressedKlassPointersEnabled()) {
        visitor.doMetadata(compressedKlass, true);
      } else {
        visitor.doMetadata(klass, true);
      }
    }
  }

  public void print()      { printOn(System.out); }
  public void printValue() { printValueOn(System.out); }
  public void printRaw()   { printRawOn(System.out); }

  public static void printOopValueOn(Oop obj, PrintStream tty) {
    if (obj == null) {
      tty.print("null");
    } else {
      obj.printValueOn(tty);
      tty.print(" @ " + VM.getVM().getUniverse().heap().oopAddressDescription(obj.getHandle()));
    }
  }

  public static void printOopAddressOn(Oop obj, PrintStream tty) {
    if (obj == null) {
      tty.print("null");
    } else {
      tty.print(obj.getHandle().toString());
    }
  }

  public void printOn(PrintStream tty) {
    OopPrinter printer = new OopPrinter(tty);
    iterate(printer, true);
  }

  public void printValueOn(PrintStream tty) {
    try {
      tty.print("Oop for " + getKlass().getName().asString());
    } catch (java.lang.NullPointerException e) {
      tty.print("Oop");
    }
  }

  public void printRawOn(PrintStream tty) {
    tty.print("Dumping raw memory for ");
    printValueOn(tty);
    tty.println();
    long size = getObjectSize() * 4;
    for (long i = 0; i < size; i += 4) {
      long memVal = getHandle().getCIntegerAt(i, 4, true);
      tty.println(Long.toHexString(memVal));
    }
  }

  public boolean verify() { return true;}

  public static Klass getKlassForOopHandle(OopHandle handle) {
    if (handle == null) {
      return null;
    }
    if (VM.getVM().isCompressedKlassPointersEnabled()) {
      return (Klass)Metadata.instantiateWrapperFor(handle.getCompKlassAddressAt(compressedKlass.getOffset()));
    } else {
      return (Klass)Metadata.instantiateWrapperFor(handle.getAddressAt(klass.getOffset()));
    }
  }
};
