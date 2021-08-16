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
import sun.jvm.hotspot.classfile.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class Klass extends Metadata implements ClassConstants {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  // anon-enum constants for _layout_helper.
  public static int LH_INSTANCE_SLOW_PATH_BIT;
  public static int LH_LOG2_ELEMENT_SIZE_SHIFT;
  public static int LH_ELEMENT_TYPE_SHIFT;
  public static int LH_HEADER_SIZE_SHIFT;
  public static int LH_ARRAY_TAG_SHIFT;
  public static int LH_ARRAY_TAG_TYPE_VALUE;
  public static int LH_ARRAY_TAG_OBJ_VALUE;

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type    = db.lookupType("Klass");
    javaMirrorFieldOffset = type.getField("_java_mirror").getOffset();
    superField   = new MetadataField(type.getAddressField("_super"), 0);
    layoutHelper = new IntField(type.getJIntField("_layout_helper"), 0);
    name         = type.getAddressField("_name");
    accessFlags  = new CIntField(type.getCIntegerField("_access_flags"), 0);
    try {
      traceIDField  = type.getField("_trace_id");
    } catch(Exception e) {
    }
    subklass     = new MetadataField(type.getAddressField("_subklass"), 0);
    nextSibling  = new MetadataField(type.getAddressField("_next_sibling"), 0);
    nextLink     = new MetadataField(type.getAddressField("_next_link"), 0);
    vtableLen    = new CIntField(type.getCIntegerField("_vtable_len"), 0);
    classLoaderData = type.getAddressField("_class_loader_data");

    LH_INSTANCE_SLOW_PATH_BIT  = db.lookupIntConstant("Klass::_lh_instance_slow_path_bit").intValue();
    LH_LOG2_ELEMENT_SIZE_SHIFT = db.lookupIntConstant("Klass::_lh_log2_element_size_shift").intValue();
    LH_ELEMENT_TYPE_SHIFT      = db.lookupIntConstant("Klass::_lh_element_type_shift").intValue();
    LH_HEADER_SIZE_SHIFT       = db.lookupIntConstant("Klass::_lh_header_size_shift").intValue();
    LH_ARRAY_TAG_SHIFT         = db.lookupIntConstant("Klass::_lh_array_tag_shift").intValue();
    LH_ARRAY_TAG_TYPE_VALUE    = db.lookupIntConstant("Klass::_lh_array_tag_type_value").intValue();
    LH_ARRAY_TAG_OBJ_VALUE     = db.lookupIntConstant("Klass::_lh_array_tag_obj_value").intValue();
  }


  public Klass(Address addr) {
    super(addr);
  }

  // jvmdi support - see also class_status in VM code
  public int getClassStatus() {
    return 0; // overridden in derived classes
  }

  public boolean isKlass()             { return true; }
  public boolean isArrayKlass()        { return false; }

  // Fields
  private static long javaMirrorFieldOffset;
  private static MetadataField  superField;
  private static IntField layoutHelper;
  private static AddressField  name;
  private static CIntField accessFlags;
  private static MetadataField  subklass;
  private static MetadataField  nextSibling;
  private static MetadataField  nextLink;
  private static sun.jvm.hotspot.types.Field traceIDField;
  private static CIntField vtableLen;
  private static AddressField classLoaderData;

  protected Symbol getSymbol(AddressField field) {
    return Symbol.create(addr.getAddressAt(field.getOffset()));
  }

  // Accessors for declared fields
  public Instance getJavaMirror() {
    Address addr = getAddress().addOffsetTo(javaMirrorFieldOffset);
    VMOopHandle vmOopHandle = VMObjectFactory.newObject(VMOopHandle.class, addr);
    return vmOopHandle.resolve();
  }
  public Klass    getSuper()            { return (Klass)    superField.getValue(this);   }
  public Klass    getJavaSuper()        { return null;  }
  public int      getLayoutHelper()     { return (int)           layoutHelper.getValue(this); }
  public Symbol   getName()             { return            getSymbol(name); }
  public long     getAccessFlags()      { return            accessFlags.getValue(this);  }
  // Convenience routine
  public AccessFlags getAccessFlagsObj(){ return new AccessFlags(getAccessFlags());      }
  public Klass    getSubklassKlass()    { return (Klass)    subklass.getValue(this);     }
  public Klass    getNextSiblingKlass() { return (Klass)    nextSibling.getValue(this);  }
  public Klass    getNextLinkKlass()    { return (Klass)    nextLink.getValue(this);  }
  public long     getVtableLen()        { return            vtableLen.getValue(this); }

  public ClassLoaderData getClassLoaderData() { return ClassLoaderData.instantiateWrapperFor(classLoaderData.getValue(getAddress())); }
  public Oop             getClassLoader()     { return   getClassLoaderData().getClassLoader(); }

  public long traceID() {
    if (traceIDField == null) return 0;
    return traceIDField.getJLong(addr);
  }

  // computed access flags - takes care of inner classes etc.
  // This is closer to actual source level than getAccessFlags() etc.
  public long computeModifierFlags() {
    return 0L; // Unless overridden, modifier_flags is 0.
  }

  // same as JVM_GetClassModifiers
  public final long getClassModifiers() {
    // unlike the VM counterpart we never have to deal with primitive type,
    // because we operator on Klass and not an instance of java.lang.Class.
    long flags = computeModifierFlags();
    if (isSuper()) {
       flags |= JVM_ACC_SUPER;
    }
    return flags;
  }

  // subclass check
  public boolean isSubclassOf(Klass k) {
    if (k != null) {
      Klass t = this;
      // Run up the super chain and check
      while (t != null) {
        if (t.equals(k)) return true;
        t = t.getSuper();
      }
    }
    return false;
  }

  // subtype check
  public boolean isSubtypeOf(Klass k) {
    return computeSubtypeOf(k);
  }

  boolean computeSubtypeOf(Klass k) {
    return isSubclassOf(k);
  }

  // Find LCA (Least Common Ancester) in class heirarchy
  public Klass lca( Klass k2 ) {
    Klass k1 = this;
    while ( true ) {
      if ( k1.isSubtypeOf(k2) ) return k2;
      if ( k2.isSubtypeOf(k1) ) return k1;
      k1 = k1.getSuper();
      k2 = k2.getSuper();
    }
  }

  public void printValueOn(PrintStream tty) {
    tty.print("Klass");
  }

  public void iterateFields(MetadataVisitor visitor) {
      // visitor.doOop(javaMirror, true);
    visitor.doMetadata(superField, true);
      visitor.doInt(layoutHelper, true);
      // visitor.doOop(name, true);
      visitor.doCInt(accessFlags, true);
    visitor.doMetadata(subklass, true);
    visitor.doMetadata(nextSibling, true);
    visitor.doCInt(vtableLen, true);
    }

  public long getObjectSize() {
    throw new RuntimeException("should not reach here");
  }

  /** Array class with specific rank */
  public Klass arrayKlass(int rank)       { return arrayKlassImpl(false, rank); }
  /** Array class with this klass as element type */
  public Klass arrayKlass()               { return arrayKlassImpl(false);       }
  /** These will return null instead of allocating on the heap */
  public Klass arrayKlassOrNull(int rank) { return arrayKlassImpl(true, rank);  }
  public Klass arrayKlassOrNull()         { return arrayKlassImpl(true);        }

  public Klass arrayKlassImpl(boolean orNull, int rank) {
    throw new RuntimeException("array_klass should be dispatched to InstanceKlass, ObjArrayKlass or TypeArrayKlass");
  }

  public Klass arrayKlassImpl(boolean orNull) {
    throw new RuntimeException("array_klass should be dispatched to InstanceKlass, ObjArrayKlass or TypeArrayKlass");
  }

  // This returns the name in the form java/lang/String which isn't really a signature
  // The subclasses override this to produce the correct form, eg
  //   Ljava/lang/String; For ArrayKlasses getName itself is the signature.
  public String signature() { return getName().asString(); }

  // Convenience routines
  public boolean isPublic()                 { return getAccessFlagsObj().isPublic(); }
  public boolean isFinal()                  { return getAccessFlagsObj().isFinal(); }
  public boolean isInterface()              { return getAccessFlagsObj().isInterface(); }
  public boolean isAbstract()               { return getAccessFlagsObj().isAbstract(); }
  public boolean isSuper()                  { return getAccessFlagsObj().isSuper(); }
  public boolean isSynthetic()              { return getAccessFlagsObj().isSynthetic(); }
  public boolean hasFinalizer()             { return getAccessFlagsObj().hasFinalizer(); }
  public boolean isCloneable()              { return getAccessFlagsObj().isCloneable(); }
  public boolean hasVanillaConstructor()    { return getAccessFlagsObj().hasVanillaConstructor(); }
  public boolean hasMirandaMethods ()       { return getAccessFlagsObj().hasMirandaMethods(); }
}
