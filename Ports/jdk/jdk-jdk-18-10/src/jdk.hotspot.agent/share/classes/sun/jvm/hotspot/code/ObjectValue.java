/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.utilities.*;

/** An ObjectValue describes an object eliminated by escape analysis. */

public class ObjectValue extends ScopeValue {
  private int              id;
  private ScopeValue       klass;
  private List<ScopeValue> fieldsValue;

  // Field "boolean visited" is not implemented here since
  // it is used only a during debug info creation.

  public ObjectValue(int id) {
    this.id = id;
    klass   = null;
    fieldsValue = new ArrayList<>();
  }

  public boolean isObject() { return true; }
  public int id() { return id; }
  public ScopeValue getKlass() { return klass; }
  public List<ScopeValue> getFieldsValue() { return fieldsValue; }
  public ScopeValue getFieldAt(int i) { return (ScopeValue)fieldsValue.get(i); }
  public int fieldsSize() { return fieldsValue.size(); }

  // Field "value" is always NULL here since it is used
  // only during deoptimization of a compiled frame
  // pointing to reallocated object.
  public OopHandle getValue() { return null; }

  /** Serialization of debugging information */

  void readObject(DebugInfoReadStream stream) {
    klass = readFrom(stream);
    Assert.that(klass.isConstantOop(), "should be constant klass oop");
    int length = stream.readInt();
    for (int i = 0; i < length; i++) {
      ScopeValue val = readFrom(stream);
      fieldsValue.add(val);
    }
  }

  // Printing

  public void print() {
    printOn(System.out);
  }

  public void printOn(PrintStream tty) {
    tty.print("scalarObj[" + id + "]");
  }

  void printFieldsOn(PrintStream tty) {
    if (fieldsValue.size() > 0) {
      ((ScopeValue)fieldsValue.get(0)).printOn(tty);
    }
    for (int i = 1; i < fieldsValue.size(); i++) {
      tty.print(", ");
      ((ScopeValue)fieldsValue.get(i)).printOn(tty);
    }
  }

};
