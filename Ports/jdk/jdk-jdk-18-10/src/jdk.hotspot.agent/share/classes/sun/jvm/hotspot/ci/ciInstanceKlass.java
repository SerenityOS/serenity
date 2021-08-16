/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.ci;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.memory.SystemDictionary;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.TypeDataBase;
import sun.jvm.hotspot.types.WrongTypeException;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class ciInstanceKlass extends ciKlass {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type      = db.lookupType("ciInstanceKlass");
    initStateField = new CIntField(type.getCIntegerField("_init_state"), 0);
    isSharedField = new CIntField(type.getCIntegerField("_is_shared"), 0);
    CLASS_STATE_LINKED = db.lookupIntConstant("InstanceKlass::linked").intValue();
    CLASS_STATE_FULLY_INITIALIZED = db.lookupIntConstant("InstanceKlass::fully_initialized").intValue();
  }

  private static CIntField initStateField;
  private static CIntField isSharedField;
  private static int CLASS_STATE_LINKED;
  private static int CLASS_STATE_FULLY_INITIALIZED;

  public ciInstanceKlass(Address addr) {
    super(addr);
  }

  public int initState() {
    int initState = (int)initStateField.getValue(getAddress());
    if (isShared() && initState < CLASS_STATE_LINKED) {
      InstanceKlass ik = (InstanceKlass)getMetadata();
      initState = ik.getInitStateAsInt();
    }
    return initState;
  }

  public boolean isShared() {
    return isSharedField.getValue(getAddress()) != 0;
  }

  public boolean isLinked() {
    return initState() >= CLASS_STATE_LINKED;
  }

  public boolean isInitialized() {
    return initState() == CLASS_STATE_FULLY_INITIALIZED;
  }

  public void dumpReplayData(PrintStream out) {
    InstanceKlass ik = (InstanceKlass)getMetadata();
    ConstantPool cp = ik.getConstants();

    // Try to record related loaded classes
    Klass sub = ik.getSubklassKlass();
    while (sub != null) {
      if (sub instanceof InstanceKlass) {
        out.println("instanceKlass " + sub.getName().asString());
      }
      sub = sub.getNextSiblingKlass();
    }

    final int length = (int) cp.getLength();
    out.print("ciInstanceKlass " + name() + " " + (isLinked() ? 1 : 0) + " " + (isInitialized() ? 1 : 0) + " " + length);
    for (int index = 1; index < length; index++) {
      out.print(" " + cp.getTags().at(index));
    }
    out.println();
    if (isInitialized()) {
      Field[] staticFields = ik.getStaticFields();
      for (int i = 0; i < staticFields.length; i++) {
        Field f = staticFields[i];
        Oop mirror = ik.getJavaMirror();
        if (f.isFinal() && !f.hasInitialValue()) {
          out.print("staticfield " + name() + " " +
                    OopUtilities.escapeString(f.getID().getName()) + " " +
                    f.getFieldType().getSignature().asString() + " ");
          if (f instanceof ByteField) {
            ByteField bf = (ByteField)f;
            out.println(bf.getValue(mirror));
          } else if (f instanceof BooleanField) {
            BooleanField bf = (BooleanField)f;
            out.println(bf.getValue(mirror) ? 1 : 0);
          } else if (f instanceof ShortField) {
            ShortField bf = (ShortField)f;
            out.println(bf.getValue(mirror));
          } else if (f instanceof CharField) {
            CharField bf = (CharField)f;
            out.println(bf.getValue(mirror) & 0xffff);
          } else if (f instanceof IntField) {
            IntField bf = (IntField)f;
            out.println(bf.getValue(mirror));
          } else  if (f instanceof LongField) {
            LongField bf = (LongField)f;
            out.println(bf.getValue(mirror));
          } else if (f instanceof FloatField) {
            FloatField bf = (FloatField)f;
            out.println(Float.floatToRawIntBits(bf.getValue(mirror)));
          } else if (f instanceof DoubleField) {
            DoubleField bf = (DoubleField)f;
            out.println(Double.doubleToRawLongBits(bf.getValue(mirror)));
          } else if (f instanceof OopField) {
            OopField bf = (OopField)f;
            Oop value = bf.getValue(mirror);
            if (value == null) {
              out.println("null");
            } else if (value.isInstance()) {
              Instance inst = (Instance)value;
              if (inst.isA(SystemDictionary.getStringKlass())) {
                out.println("\"" + OopUtilities.stringOopToEscapedString(inst) + "\"");
              } else {
                out.println(inst.getKlass().getName().asString());
              }
            } else if (value.isObjArray()) {
              ObjArray oa = (ObjArray)value;
              Klass ek = (ObjArrayKlass)oa.getKlass();
              out.println(oa.getLength() + " " + ek.getName().asString());
            } else if (value.isTypeArray()) {
              TypeArray ta = (TypeArray)value;
              out.println(ta.getLength());
            } else {
              out.println(value);
            }
          }
        }
      }
    }
  }
}
