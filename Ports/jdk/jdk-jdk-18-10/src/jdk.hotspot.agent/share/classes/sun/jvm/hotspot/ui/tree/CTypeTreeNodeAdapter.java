/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.ui.tree;

import java.util.*;
import sun.jvm.hotspot.oops.FieldIdentifier;
import sun.jvm.hotspot.oops.Oop;
import sun.jvm.hotspot.oops.UnknownOopException;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.CStringUtilities;

/** Encapsulates an arbitrary type value in a tree handled by SimpleTreeModel */

public class CTypeTreeNodeAdapter extends FieldTreeNodeAdapter {
  final private Address addr;
  final private Type type;
  private CTypeFieldIdentifier[] fields = null;

  private void collectFields(Type type, ArrayList<CTypeFieldIdentifier> list, boolean statics, boolean recurse) {
    Type supertype = type.getSuperclass();
    if (supertype != null && recurse) {
      collectFields(supertype, list, statics, recurse);
    }
    Iterator i = type.getFields();
    while (i.hasNext()) {
      Field f = (Field) i.next();
      if (f.isStatic() == statics) {
        list.add(new CTypeFieldIdentifier(type, f));
      }
    }
  }


  private CTypeFieldIdentifier[] getFields() {
    if (fields == null) {
      ArrayList<CTypeFieldIdentifier> f = new ArrayList<>();
      collectFields(type, f, false, true);
      fields = f.toArray(new CTypeFieldIdentifier[0]);
    }
    return fields;
  }

  static class CTypeFieldIdentifier extends FieldIdentifier {
    final private Field field;
    final private Type holder;

    CTypeFieldIdentifier(Type t, Field f) {
      holder = t;
      field = f;
    }

    public Field getField() {
      return field;
    }

    public String getName() {
      return field.getType().getName() + " " + holder.getName() + "::" + field.getName();
    }
  }


  public CTypeTreeNodeAdapter(Address a, Type t, FieldIdentifier id) {
    this(a, t, id, false);
  }

  public CTypeTreeNodeAdapter(Address a, Type t, FieldIdentifier id, boolean treeTableMode) {
    super(id, treeTableMode);
    type = t;
    addr = a;
  }

  public CTypeTreeNodeAdapter(Type t) {
    super(null, false);
    type = t;
    addr = null;
    ArrayList<CTypeFieldIdentifier> statics = new ArrayList<>();
    collectFields(type, statics, true, false);
    fields = statics.toArray(new CTypeFieldIdentifier[0]);
  }

  public CTypeTreeNodeAdapter(Iterator types) {
    super(null, false);
    addr = null;
    type = null;
    ArrayList<CTypeFieldIdentifier> statics = new ArrayList<>();
    while (types.hasNext()) {
      collectFields((Type)types.next(), statics, true, false);
    }
    fields = statics.toArray(new CTypeFieldIdentifier[0]);
  }

  public int getChildCount() {
    return getFields().length;
  }

  public SimpleTreeNode getChild(int index) {
    CTypeFieldIdentifier cf = getFields()[index];
    Field f = cf.getField();
    Type t = f.getType();
    try {
      if (t.isOopType()) {
        OopHandle handle;
        if (f.isStatic()) {
          handle = f.getOopHandle();
        } else {
          handle = f.getOopHandle(addr);
        }
        try {
          Oop oop = VM.getVM().getObjectHeap().newOop(handle);
          return new OopTreeNodeAdapter(oop, cf, getTreeTableMode());
        } catch (AddressException e) {
          return new BadAddressTreeNodeAdapter(handle,
                                           new CTypeFieldIdentifier(type, f),
                                           getTreeTableMode());
        } catch (UnknownOopException e) {
          return new BadAddressTreeNodeAdapter(handle,
                                           new CTypeFieldIdentifier(type, f),
                                           getTreeTableMode());
        }
      } else if (t.isCIntegerType()) {
        long value = 0;
        if (f.isStatic()) {
          value = f.getCInteger((CIntegerType)t);
        } else {
          value = f.getCInteger(addr, (CIntegerType)t);
        }
        return new LongTreeNodeAdapter(value, cf, getTreeTableMode());
      } else if (t.isJavaPrimitiveType()) {
        boolean isStatic = f.isStatic();
        if (f instanceof JByteField) {
          long value = isStatic? f.getJByte() : f.getJByte(addr);
          return new LongTreeNodeAdapter(value, cf, getTreeTableMode());
        } else if (f instanceof JShortField) {
          long value = isStatic? f.getJShort() : f.getJShort(addr);
          return new LongTreeNodeAdapter(value, cf, getTreeTableMode());
        } else if (f instanceof JIntField) {
          long value = isStatic? f.getJInt() : f.getJInt(addr);
          return new LongTreeNodeAdapter(value, cf, getTreeTableMode());
        } else if (f instanceof JLongField) {
          long value = isStatic? f.getJLong() : f.getJLong(addr);
          return new LongTreeNodeAdapter(value, cf, getTreeTableMode());
        } else if (f instanceof JCharField) {
          char value = isStatic? f.getJChar() : f.getJChar(addr);
          return new CharTreeNodeAdapter(value, cf, getTreeTableMode());
        } else if (f instanceof JBooleanField) {
          boolean value = isStatic? f.getJBoolean() : f.getJBoolean(addr);
          return new BooleanTreeNodeAdapter(value, cf, getTreeTableMode());
        } else if (f instanceof JFloatField) {
          float value = isStatic? f.getJFloat() : f.getJFloat(addr);
          return new DoubleTreeNodeAdapter(value, cf, getTreeTableMode());
        } else if (f instanceof JDoubleField) {
          double value = isStatic? f.getJDouble() : f.getJDouble(addr);
          return new DoubleTreeNodeAdapter(value, cf, getTreeTableMode());
        } else {
          throw new RuntimeException("unhandled type: " + t.getName());
        }
      } else if (t.isPointerType()) {
        Address ptr;
        if (f.isStatic()) {
          ptr = f.getAddress();
        } else {
          ptr = f.getAddress(addr);
        }

        if (t.isCStringType()) {
            return new CStringTreeNodeAdapter(CStringUtilities.getString(ptr), cf);
        }

        return new CTypeTreeNodeAdapter(ptr, ((PointerType) t).getTargetType(), cf, getTreeTableMode());
      } else {
        if (f.isStatic()) {
            return new CTypeTreeNodeAdapter(f.getStaticFieldAddress(), f.getType(),
                                        cf, getTreeTableMode());
        } else {
            return new CTypeTreeNodeAdapter(addr.addOffsetTo(f.getOffset()), f.getType(),
                                        cf, getTreeTableMode());
        }
      }
    } catch (AddressException e) {
      return new BadAddressTreeNodeAdapter(e.getAddress(),
                                           new CTypeFieldIdentifier(type, f),
                                           getTreeTableMode());
    }
  }

  public boolean isLeaf() {
    return getFields().length == 0;
  }

  public int getIndexOfChild(SimpleTreeNode child) {
    CTypeFieldIdentifier id = (CTypeFieldIdentifier)((FieldTreeNodeAdapter) child).getID();
    CTypeFieldIdentifier[] f = getFields();
    for (int i = 0; i < f.length; i++) {
      if (id == f[i]) {
        return i;
      }
    }
    return -1;
  }

  public String getValue() {
    if (type != null) {
      return type.getName() + " @ " + addr;
    } else {
      return "<statics>";
    }
  }
}
