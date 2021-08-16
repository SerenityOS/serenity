/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */

package jdk.internal.reflect;

import java.lang.reflect.*;
import java.util.Set;

/** Provides reflective access to the constant pools of classes.
    Currently this is needed to provide reflective access to annotations
    but may be used by other internal subsystems in the future. */

public class ConstantPool {
  // Number of entries in this constant pool (= maximum valid constant pool index)
  public int      getSize()                      { return getSize0            (constantPoolOop);        }
  public Class<?> getClassAt         (int index) { return getClassAt0         (constantPoolOop, index); }
  public Class<?> getClassAtIfLoaded (int index) { return getClassAtIfLoaded0 (constantPoolOop, index); }
  // Returns a class reference index for a method or a field.
  public int getClassRefIndexAt(int index) {
      return getClassRefIndexAt0(constantPoolOop, index);
  }
  // Returns either a Method or Constructor.
  // Static initializers are returned as Method objects.
  public Member   getMethodAt        (int index) { return getMethodAt0        (constantPoolOop, index); }
  public Member   getMethodAtIfLoaded(int index) { return getMethodAtIfLoaded0(constantPoolOop, index); }
  public Field    getFieldAt         (int index) { return getFieldAt0         (constantPoolOop, index); }
  public Field    getFieldAtIfLoaded (int index) { return getFieldAtIfLoaded0 (constantPoolOop, index); }
  // Fetches the class name, member (field, method or interface
  // method) name, and type descriptor as an array of three Strings
  public String[] getMemberRefInfoAt (int index) { return getMemberRefInfoAt0 (constantPoolOop, index); }
  // Returns a name and type reference index for a method, a field or an invokedynamic.
  public int getNameAndTypeRefIndexAt(int index) {
      return getNameAndTypeRefIndexAt0(constantPoolOop, index);
  }
  // Fetches the name and type from name_and_type index as an array of two Strings
  public String[] getNameAndTypeRefInfoAt(int index) {
      return getNameAndTypeRefInfoAt0(constantPoolOop, index);
  }
  public int      getIntAt           (int index) { return getIntAt0           (constantPoolOop, index); }
  public long     getLongAt          (int index) { return getLongAt0          (constantPoolOop, index); }
  public float    getFloatAt         (int index) { return getFloatAt0         (constantPoolOop, index); }
  public double   getDoubleAt        (int index) { return getDoubleAt0        (constantPoolOop, index); }
  public String   getStringAt        (int index) { return getStringAt0        (constantPoolOop, index); }
  public String   getUTF8At          (int index) { return getUTF8At0          (constantPoolOop, index); }
  public Tag getTagAt(int index) {
      return Tag.valueOf(getTagAt0(constantPoolOop, index));
  }

  public static enum Tag {
      UTF8(1),
      INTEGER(3),
      FLOAT(4),
      LONG(5),
      DOUBLE(6),
      CLASS(7),
      STRING(8),
      FIELDREF(9),
      METHODREF(10),
      INTERFACEMETHODREF(11),
      NAMEANDTYPE(12),
      METHODHANDLE(15),
      METHODTYPE(16),
      INVOKEDYNAMIC(18),
      INVALID(0);

      private final int tagCode;

      private Tag(int tagCode) {
          this.tagCode = tagCode;
      }

      private static Tag valueOf(byte v) {
          for (Tag tag : Tag.values()) {
              if (tag.tagCode == v) {
                  return tag;
              }
          }
          throw new IllegalArgumentException("Unknown constant pool tag code " + v);
      }
   }
  //---------------------------------------------------------------------------
  // Internals only below this point
  //

  static {
      Reflection.registerFieldsToFilter(ConstantPool.class, Set.of("constantPoolOop"));
  }

  // HotSpot-internal constant pool object (set by the VM, name known to the VM)
  private Object constantPoolOop;

  private native int      getSize0            (Object constantPoolOop);
  private native Class<?> getClassAt0         (Object constantPoolOop, int index);
  private native Class<?> getClassAtIfLoaded0 (Object constantPoolOop, int index);
  private native int      getClassRefIndexAt0 (Object constantPoolOop, int index);
  private native Member   getMethodAt0        (Object constantPoolOop, int index);
  private native Member   getMethodAtIfLoaded0(Object constantPoolOop, int index);
  private native Field    getFieldAt0         (Object constantPoolOop, int index);
  private native Field    getFieldAtIfLoaded0 (Object constantPoolOop, int index);
  private native String[] getMemberRefInfoAt0 (Object constantPoolOop, int index);
  private native int      getNameAndTypeRefIndexAt0(Object constantPoolOop, int index);
  private native String[] getNameAndTypeRefInfoAt0(Object constantPoolOop, int index);
  private native int      getIntAt0           (Object constantPoolOop, int index);
  private native long     getLongAt0          (Object constantPoolOop, int index);
  private native float    getFloatAt0         (Object constantPoolOop, int index);
  private native double   getDoubleAt0        (Object constantPoolOop, int index);
  private native String   getStringAt0        (Object constantPoolOop, int index);
  private native String   getUTF8At0          (Object constantPoolOop, int index);
  private native byte     getTagAt0           (Object constantPoolOop, int index);
}
