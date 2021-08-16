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

package sun.jvm.hotspot.memory;

import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** A very simple data structure representing a contigous region of
    address space. */

public class MemRegion implements Cloneable {
  private Address start;
  private long byteSize;

  private static AddressField  startField;
  private static CIntegerField wordSizeField;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) {
    Type type = db.lookupType("MemRegion");

    startField    = type.getAddressField("_start");
    wordSizeField = type.getCIntegerField("_word_size");
  }

  public MemRegion() {
  }

  /** This constructor takes a "MemRegion*" in the target process */
  public MemRegion(Address memRegionAddr) {
    this(startField.getValue(memRegionAddr),
         wordSizeField.getValue(memRegionAddr));
  }

  public MemRegion(Address start, long wordSize) {
    setStart(start);
    setWordSize(wordSize);
  }

  public MemRegion(Address start, Address limit) {
    setStart(start);
    byteSize = limit.minus(start);
  }

  public Object clone() {
    return new MemRegion(start, byteSize);
  }

  public MemRegion copy() {
    return (MemRegion) clone();
  }

  public MemRegion intersection(MemRegion mr2) {
    MemRegion res = new MemRegion();
    if (AddressOps.gt(mr2.start(), start())) {
      res.setStart(mr2.start());
    } else {
      res.setStart(start());
    }
    Address resEnd;
    Address end = end();
    Address mr2End = mr2.end();
    if (AddressOps.lt(end, mr2End)) {
      resEnd = end;
    } else {
      resEnd = mr2End;
    }
    if (AddressOps.lt(resEnd, res.start())) {
      res.setStart(null);
      res.setWordSize(0);
    } else {
      res.setEnd(resEnd);
    }
    return res;
  }

  public MemRegion union(MemRegion mr2) {
    MemRegion res = new MemRegion();
    if (AddressOps.lt(mr2.start(), start())) {
      res.setStart(mr2.start());
    } else {
      res.setStart(start());
    }
    Address resEnd;
    Address end = end();
    Address mr2End = mr2.end();
    if (AddressOps.gt(end, mr2End)) {
      resEnd = end;
    } else {
      resEnd = mr2End;
    }
    res.setEnd(resEnd);
    return res;
  }

  public Address start() {
    return start;
  }

  public OopHandle startAsOopHandle() {
    return start().addOffsetToAsOopHandle(0);
  }

  public Address end() {
    return start.addOffsetTo(byteSize);
  }

  public OopHandle endAsOopHandle() {
    return end().addOffsetToAsOopHandle(0);
  }

  public void setStart(Address start) {
    this.start = start;
  }

  public void setEnd(Address end) {
    byteSize = end.minus(start);
  }

  public void setWordSize(long wordSize) {
    byteSize = VM.getVM().getAddressSize() * wordSize;
  }

  public boolean contains(MemRegion mr2) {
    return AddressOps.lte(start, mr2.start) && AddressOps.gte(end(), mr2.end());
  }

  public boolean contains(Address addr) {
    return AddressOps.gte(addr, start()) && AddressOps.lt(addr, end());
  }

  public long byteSize() {
    return byteSize;
  }

  public long wordSize() {
    return byteSize / VM.getVM().getAddressSize();
  }
}
