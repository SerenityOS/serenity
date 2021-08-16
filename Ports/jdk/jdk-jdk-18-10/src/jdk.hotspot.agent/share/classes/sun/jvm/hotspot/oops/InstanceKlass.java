/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.classfile.ClassLoaderData;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

// An InstanceKlass is the VM level representation of a Java class.

public class InstanceKlass extends Klass {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  // field offset constants
  private static int ACCESS_FLAGS_OFFSET;
  private static int NAME_INDEX_OFFSET;
  private static int SIGNATURE_INDEX_OFFSET;
  private static int INITVAL_INDEX_OFFSET;
  private static int LOW_OFFSET;
  private static int HIGH_OFFSET;
  private static int FIELD_SLOTS;
  private static short FIELDINFO_TAG_SIZE;
  private static short FIELDINFO_TAG_OFFSET;

  // ClassState constants
  private static int CLASS_STATE_ALLOCATED;
  private static int CLASS_STATE_LOADED;
  private static int CLASS_STATE_LINKED;
  private static int CLASS_STATE_BEING_INITIALIZED;
  private static int CLASS_STATE_FULLY_INITIALIZED;
  private static int CLASS_STATE_INITIALIZATION_ERROR;

  // _misc_flags constants
  private static int MISC_REWRITTEN;
  private static int MISC_HAS_NONSTATIC_FIELDS;
  private static int MISC_SHOULD_VERIFY_CLASS;
  private static int MISC_IS_CONTENDED;
  private static int MISC_HAS_NONSTATIC_CONCRETE_METHODS;
  private static int MISC_DECLARES_NONSTATIC_CONCRETE_METHODS;
  private static int MISC_HAS_BEEN_REDEFINED;
  private static int MISC_IS_SCRATCH_CLASS;
  private static int MISC_IS_SHARED_BOOT_CLASS;
  private static int MISC_IS_SHARED_PLATFORM_CLASS;
  private static int MISC_IS_SHARED_APP_CLASS;

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type            = db.lookupType("InstanceKlass");
    arrayKlasses         = new MetadataField(type.getAddressField("_array_klasses"), 0);
    methods              = type.getAddressField("_methods");
    defaultMethods       = type.getAddressField("_default_methods");
    methodOrdering       = type.getAddressField("_method_ordering");
    localInterfaces      = type.getAddressField("_local_interfaces");
    transitiveInterfaces = type.getAddressField("_transitive_interfaces");
    fields               = type.getAddressField("_fields");
    javaFieldsCount      = new CIntField(type.getCIntegerField("_java_fields_count"), 0);
    constants            = new MetadataField(type.getAddressField("_constants"), 0);
    sourceDebugExtension = type.getAddressField("_source_debug_extension");
    innerClasses         = type.getAddressField("_inner_classes");
    nonstaticFieldSize   = new CIntField(type.getCIntegerField("_nonstatic_field_size"), 0);
    staticFieldSize      = new CIntField(type.getCIntegerField("_static_field_size"), 0);
    staticOopFieldCount  = new CIntField(type.getCIntegerField("_static_oop_field_count"), 0);
    nonstaticOopMapSize  = new CIntField(type.getCIntegerField("_nonstatic_oop_map_size"), 0);
    isMarkedDependent    = new CIntField(type.getCIntegerField("_is_marked_dependent"), 0);
    initState            = new CIntField(type.getCIntegerField("_init_state"), 0);
    itableLen            = new CIntField(type.getCIntegerField("_itable_len"), 0);
    if (VM.getVM().isJvmtiSupported()) {
      breakpoints        = type.getAddressField("_breakpoints");
    }
    miscFlags            = new CIntField(type.getCIntegerField("_misc_flags"), 0);
    headerSize           = type.getSize();

    // read field offset constants
    ACCESS_FLAGS_OFFSET            = db.lookupIntConstant("FieldInfo::access_flags_offset").intValue();
    NAME_INDEX_OFFSET              = db.lookupIntConstant("FieldInfo::name_index_offset").intValue();
    SIGNATURE_INDEX_OFFSET         = db.lookupIntConstant("FieldInfo::signature_index_offset").intValue();
    INITVAL_INDEX_OFFSET           = db.lookupIntConstant("FieldInfo::initval_index_offset").intValue();
    LOW_OFFSET                     = db.lookupIntConstant("FieldInfo::low_packed_offset").intValue();
    HIGH_OFFSET                    = db.lookupIntConstant("FieldInfo::high_packed_offset").intValue();
    FIELD_SLOTS                    = db.lookupIntConstant("FieldInfo::field_slots").intValue();
    FIELDINFO_TAG_SIZE             = db.lookupIntConstant("FIELDINFO_TAG_SIZE").shortValue();
    FIELDINFO_TAG_OFFSET           = db.lookupIntConstant("FIELDINFO_TAG_OFFSET").shortValue();

    // read ClassState constants
    CLASS_STATE_ALLOCATED = db.lookupIntConstant("InstanceKlass::allocated").intValue();
    CLASS_STATE_LOADED = db.lookupIntConstant("InstanceKlass::loaded").intValue();
    CLASS_STATE_LINKED = db.lookupIntConstant("InstanceKlass::linked").intValue();
    CLASS_STATE_BEING_INITIALIZED = db.lookupIntConstant("InstanceKlass::being_initialized").intValue();
    CLASS_STATE_FULLY_INITIALIZED = db.lookupIntConstant("InstanceKlass::fully_initialized").intValue();
    CLASS_STATE_INITIALIZATION_ERROR = db.lookupIntConstant("InstanceKlass::initialization_error").intValue();

    MISC_REWRITTEN                    = db.lookupIntConstant("InstanceKlass::_misc_rewritten").intValue();
    MISC_HAS_NONSTATIC_FIELDS         = db.lookupIntConstant("InstanceKlass::_misc_has_nonstatic_fields").intValue();
    MISC_SHOULD_VERIFY_CLASS          = db.lookupIntConstant("InstanceKlass::_misc_should_verify_class").intValue();
    MISC_IS_CONTENDED                 = db.lookupIntConstant("InstanceKlass::_misc_is_contended").intValue();
    MISC_HAS_NONSTATIC_CONCRETE_METHODS      = db.lookupIntConstant("InstanceKlass::_misc_has_nonstatic_concrete_methods").intValue();
    MISC_DECLARES_NONSTATIC_CONCRETE_METHODS = db.lookupIntConstant("InstanceKlass::_misc_declares_nonstatic_concrete_methods").intValue();
    MISC_HAS_BEEN_REDEFINED           = db.lookupIntConstant("InstanceKlass::_misc_has_been_redefined").intValue();
    MISC_IS_SCRATCH_CLASS             = db.lookupIntConstant("InstanceKlass::_misc_is_scratch_class").intValue();
    MISC_IS_SHARED_BOOT_CLASS         = db.lookupIntConstant("InstanceKlass::_misc_is_shared_boot_class").intValue();
    MISC_IS_SHARED_PLATFORM_CLASS     = db.lookupIntConstant("InstanceKlass::_misc_is_shared_platform_class").intValue();
    MISC_IS_SHARED_APP_CLASS          = db.lookupIntConstant("InstanceKlass::_misc_is_shared_app_class").intValue();
  }

  public InstanceKlass(Address addr) {
    super(addr);

    // If the class hasn't yet reached the "loaded" init state, then don't go any further
    // or we'll run into problems trying to look at fields that are not yet setup.
    // Attempted lookups of this InstanceKlass via ClassLoaderDataGraph, ClassLoaderData,
    // and Dictionary will all refuse to return it. The main purpose of allowing this
    // InstanceKlass to initialize is so ClassLoaderData.getKlasses() will succeed, allowing
    // ClassLoaderData.classesDo() to iterate over all Klasses (skipping those that are
    // not yet fully loaded).
    if (!isLoaded()) {
        return;
    }

    if (getJavaFieldsCount() != getAllFieldsCount()) {
      // Exercise the injected field logic
      for (int i = getJavaFieldsCount(); i < getAllFieldsCount(); i++) {
        getFieldName(i);
        getFieldSignature(i);
      }
    }
  }

  private static MetadataField arrayKlasses;
  private static AddressField  methods;
  private static AddressField  defaultMethods;
  private static AddressField  methodOrdering;
  private static AddressField  localInterfaces;
  private static AddressField  transitiveInterfaces;
  private static AddressField fields;
  private static CIntField javaFieldsCount;
  private static MetadataField constants;
  private static AddressField  sourceDebugExtension;
  private static AddressField  innerClasses;
  private static CIntField nonstaticFieldSize;
  private static CIntField staticFieldSize;
  private static CIntField staticOopFieldCount;
  private static CIntField nonstaticOopMapSize;
  private static CIntField isMarkedDependent;
  private static CIntField initState;
  private static CIntField itableLen;
  private static AddressField breakpoints;
  private static CIntField miscFlags;

  // type safe enum for ClassState from instanceKlass.hpp
  public static class ClassState {
     public static final ClassState ALLOCATED    = new ClassState("allocated");
     public static final ClassState LOADED       = new ClassState("loaded");
     public static final ClassState LINKED       = new ClassState("linked");
     public static final ClassState BEING_INITIALIZED      = new ClassState("beingInitialized");
     public static final ClassState FULLY_INITIALIZED    = new ClassState("fullyInitialized");
     public static final ClassState INITIALIZATION_ERROR = new ClassState("initializationError");

     private ClassState(String value) {
        this.value = value;
     }

     public String toString() {
        return value;
     }

     private String value;
  }

  public int  getInitStateAsInt() { return (int) initState.getValue(this); }
  public ClassState getInitState() {
     int state = getInitStateAsInt();
     if (state == CLASS_STATE_ALLOCATED) {
        return ClassState.ALLOCATED;
     } else if (state == CLASS_STATE_LOADED) {
        return ClassState.LOADED;
     } else if (state == CLASS_STATE_LINKED) {
        return ClassState.LINKED;
     } else if (state == CLASS_STATE_BEING_INITIALIZED) {
        return ClassState.BEING_INITIALIZED;
     } else if (state == CLASS_STATE_FULLY_INITIALIZED) {
        return ClassState.FULLY_INITIALIZED;
     } else if (state == CLASS_STATE_INITIALIZATION_ERROR) {
        return ClassState.INITIALIZATION_ERROR;
     } else {
        throw new RuntimeException("should not reach here");
     }
  }

  // initialization state quaries
  public boolean isLoaded() {
     return getInitStateAsInt() >= CLASS_STATE_LOADED;
  }

  public boolean isLinked() {
     return getInitStateAsInt() >= CLASS_STATE_LINKED;
  }

  public boolean isInitialized() {
     return getInitStateAsInt() == CLASS_STATE_FULLY_INITIALIZED;
  }

  public boolean isNotInitialized() {
     return getInitStateAsInt() < CLASS_STATE_BEING_INITIALIZED;
  }

  public boolean isBeingInitialized() {
     return getInitStateAsInt() == CLASS_STATE_BEING_INITIALIZED;
  }

  public boolean isInErrorState() {
     return getInitStateAsInt() == CLASS_STATE_INITIALIZATION_ERROR;
  }

  public int getClassStatus() {
     int result = 0;
     if (isLinked()) {
        result |= JVMDIClassStatus.VERIFIED | JVMDIClassStatus.PREPARED;
     }

     if (isInitialized()) {
        if (Assert.ASSERTS_ENABLED) {
           Assert.that(isLinked(), "Class status is not consistent");
        }
        result |= JVMDIClassStatus.INITIALIZED;
     }

     if (isInErrorState()) {
        result |= JVMDIClassStatus.ERROR;
     }
     return result;
  }

  // Byteside of the header
  private static long headerSize;

  public long getObjectSize(Oop object) {
    return getSizeHelper() * VM.getVM().getAddressSize();
  }

  public long getSize() { // in number of bytes
    long wordLength = VM.getVM().getBytesPerWord();
    long size = getHeaderSize() +
                (getVtableLen() +
                 getItableLen() +
                 getNonstaticOopMapSize()) * wordLength;
    if (isInterface()) {
      size += wordLength;
    }
    return alignSize(size);
  }

  private int getMiscFlags() {
    return (int) miscFlags.getValue(this);
  }

  public static long getHeaderSize() { return headerSize; }

  public short getFieldAccessFlags(int index) {
    return getFields().at(index * FIELD_SLOTS + ACCESS_FLAGS_OFFSET);
  }

  public short getFieldNameIndex(int index) {
    if (index >= getJavaFieldsCount()) throw new IndexOutOfBoundsException("not a Java field;");
    return getFields().at(index * FIELD_SLOTS + NAME_INDEX_OFFSET);
  }

  public Symbol getFieldName(int index) {
    int nameIndex = getFields().at(index * FIELD_SLOTS + NAME_INDEX_OFFSET);
    if (index < getJavaFieldsCount()) {
      return getConstants().getSymbolAt(nameIndex);
    } else {
      return vmSymbols.symbolAt(nameIndex);
    }
  }

  public short getFieldSignatureIndex(int index) {
    if (index >= getJavaFieldsCount()) throw new IndexOutOfBoundsException("not a Java field;");
    return getFields().at(index * FIELD_SLOTS + SIGNATURE_INDEX_OFFSET);
  }

  public Symbol getFieldSignature(int index) {
    int signatureIndex = getFields().at(index * FIELD_SLOTS + SIGNATURE_INDEX_OFFSET);
    if (index < getJavaFieldsCount()) {
      return getConstants().getSymbolAt(signatureIndex);
    } else {
      return vmSymbols.symbolAt(signatureIndex);
    }
  }

  public short getFieldGenericSignatureIndex(int index) {
    // int len = getFields().length();
    int allFieldsCount = getAllFieldsCount();
    int generic_signature_slot = allFieldsCount * FIELD_SLOTS;
    for (int i = 0; i < allFieldsCount; i++) {
      short flags = getFieldAccessFlags(i);
      AccessFlags access = new AccessFlags(flags);
      if (i == index) {
        if (access.fieldHasGenericSignature()) {
           return getFields().at(generic_signature_slot);
        } else {
          return 0;
        }
      } else {
        if (access.fieldHasGenericSignature()) {
          generic_signature_slot ++;
        }
      }
    }
    return 0;
  }

  public Symbol getFieldGenericSignature(int index) {
    short genericSignatureIndex = getFieldGenericSignatureIndex(index);
    if (genericSignatureIndex != 0)  {
      return getConstants().getSymbolAt(genericSignatureIndex);
    }
    return null;
  }

  public short getFieldInitialValueIndex(int index) {
    if (index >= getJavaFieldsCount()) throw new IndexOutOfBoundsException("not a Java field;");
    return getFields().at(index * FIELD_SLOTS + INITVAL_INDEX_OFFSET);
  }

  public int getFieldOffset(int index) {
    U2Array fields = getFields();
    short lo = fields.at(index * FIELD_SLOTS + LOW_OFFSET);
    short hi = fields.at(index * FIELD_SLOTS + HIGH_OFFSET);
    if ((lo & FIELDINFO_TAG_OFFSET) == FIELDINFO_TAG_OFFSET) {
      return VM.getVM().buildIntFromShorts(lo, hi) >> FIELDINFO_TAG_SIZE;
    }
    throw new RuntimeException("should not reach here");
  }

  // Accessors for declared fields
  public Klass     getArrayKlasses()        { return (Klass)        arrayKlasses.getValue(this); }
  public MethodArray  getMethods()              { return new MethodArray(methods.getValue(getAddress())); }

  public MethodArray  getDefaultMethods() {
    if (defaultMethods != null) {
      Address addr = defaultMethods.getValue(getAddress());
      if ((addr != null) && (addr.getAddressAt(0) != null)) {
        return new MethodArray(addr);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  public KlassArray   getLocalInterfaces()      { return new KlassArray(localInterfaces.getValue(getAddress())); }
  public KlassArray   getTransitiveInterfaces() { return new KlassArray(transitiveInterfaces.getValue(getAddress())); }
  public int       getJavaFieldsCount()     { return                (int) javaFieldsCount.getValue(this); }
  public int       getAllFieldsCount()      {
    int len = getFields().length();
    int allFieldsCount = 0;
    for (; allFieldsCount*FIELD_SLOTS < len; allFieldsCount++) {
      short flags = getFieldAccessFlags(allFieldsCount);
      AccessFlags access = new AccessFlags(flags);
      if (access.fieldHasGenericSignature()) {
        len --;
      }
    }
    return allFieldsCount;
  }
  public ConstantPool getConstants()        { return (ConstantPool) constants.getValue(this); }
  public Symbol    getSourceFileName()      { return                getConstants().getSourceFileName(); }
  public String    getSourceDebugExtension(){ return                CStringUtilities.getString(sourceDebugExtension.getValue(getAddress())); }
  public long      getNonstaticFieldSize()  { return                nonstaticFieldSize.getValue(this); }
  public long      getStaticOopFieldCount() { return                staticOopFieldCount.getValue(this); }
  public long      getNonstaticOopMapSize() { return                nonstaticOopMapSize.getValue(this); }
  public boolean   getIsMarkedDependent()   { return                isMarkedDependent.getValue(this) != 0; }
  public long      getItableLen()           { return                itableLen.getValue(this); }
  public long      majorVersion()           { return                getConstants().majorVersion(); }
  public long      minorVersion()           { return                getConstants().minorVersion(); }
  public Symbol    getGenericSignature()    { return                getConstants().getGenericSignature(); }

  // "size helper" == instance size in words
  public long getSizeHelper() {
    int lh = getLayoutHelper();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(lh > 0, "layout helper initialized for instance class");
    }
    return lh / VM.getVM().getAddressSize();
  }

  // same as enum InnerClassAttributeOffset in VM code.
  private static class InnerClassAttributeOffset {
    // from JVM spec. "InnerClasses" attribute
    public static int innerClassInnerClassInfoOffset;
    public static int innerClassOuterClassInfoOffset;
    public static int innerClassInnerNameOffset;
    public static int innerClassAccessFlagsOffset;
    public static int innerClassNextOffset;
    static {
      VM.registerVMInitializedObserver(new Observer() {
          public void update(Observable o, Object data) {
              initialize(VM.getVM().getTypeDataBase());
          }
      });
    }

    private static synchronized void initialize(TypeDataBase db) {
      innerClassInnerClassInfoOffset = db.lookupIntConstant(
          "InstanceKlass::inner_class_inner_class_info_offset").intValue();
      innerClassOuterClassInfoOffset = db.lookupIntConstant(
          "InstanceKlass::inner_class_outer_class_info_offset").intValue();
      innerClassInnerNameOffset = db.lookupIntConstant(
          "InstanceKlass::inner_class_inner_name_offset").intValue();
      innerClassAccessFlagsOffset = db.lookupIntConstant(
          "InstanceKlass::inner_class_access_flags_offset").intValue();
      innerClassNextOffset = db.lookupIntConstant(
          "InstanceKlass::inner_class_next_offset").intValue();
    }
  }

  private static class EnclosingMethodAttributeOffset {
    public static int enclosingMethodAttributeSize;
    static {
      VM.registerVMInitializedObserver(new Observer() {
          public void update(Observable o, Object data) {
              initialize(VM.getVM().getTypeDataBase());
          }
      });
    }
    private static synchronized void initialize(TypeDataBase db) {
      enclosingMethodAttributeSize = db.lookupIntConstant("InstanceKlass::enclosing_method_attribute_size").intValue();
    }
  }

  // refer to compute_modifier_flags in VM code.
  public long computeModifierFlags() {
    long access = getAccessFlags();
    // But check if it happens to be member class.
    U2Array innerClassList = getInnerClasses();
    int length = (innerClassList == null)? 0 : (int) innerClassList.length();
    if (length > 0) {
       if (Assert.ASSERTS_ENABLED) {
          Assert.that(length % InnerClassAttributeOffset.innerClassNextOffset == 0 ||
                      length % InnerClassAttributeOffset.innerClassNextOffset == EnclosingMethodAttributeOffset.enclosingMethodAttributeSize,
                      "just checking");
       }
       for (int i = 0; i < length; i += InnerClassAttributeOffset.innerClassNextOffset) {
          if (i == length - EnclosingMethodAttributeOffset.enclosingMethodAttributeSize) {
              break;
          }
          int ioff = innerClassList.at(i +
                         InnerClassAttributeOffset.innerClassInnerClassInfoOffset);
          // 'ioff' can be zero.
          // refer to JVM spec. section 4.7.5.
          if (ioff != 0) {
             // only look at classes that are already loaded
             // since we are looking for the flags for our self.
             Symbol name = getConstants().getKlassNameAt(ioff);

             if (name.equals(getName())) {
                // This is really a member class
                access = innerClassList.at(i +
                        InnerClassAttributeOffset.innerClassAccessFlagsOffset);
                break;
             }
          }
       } // for inner classes
    }

    // Remember to strip ACC_SUPER bit
    return (access & (~JVM_ACC_SUPER)) & JVM_ACC_WRITTEN_FLAGS;
  }


  // whether given Symbol is name of an inner/nested Klass of this Klass?
  // anonymous and local classes are excluded.
  public boolean isInnerClassName(Symbol sym) {
    return isInInnerClasses(sym, false);
  }

  // whether given Symbol is name of an inner/nested Klass of this Klass?
  // anonymous classes excluded, but local classes are included.
  public boolean isInnerOrLocalClassName(Symbol sym) {
    return isInInnerClasses(sym, true);
  }

  private boolean isInInnerClasses(Symbol sym, boolean includeLocals) {
    U2Array innerClassList = getInnerClasses();
    int length = ( innerClassList == null)? 0 : (int) innerClassList.length();
    if (length > 0) {
       if (Assert.ASSERTS_ENABLED) {
         Assert.that(length % InnerClassAttributeOffset.innerClassNextOffset == 0 ||
                     length % InnerClassAttributeOffset.innerClassNextOffset == EnclosingMethodAttributeOffset.enclosingMethodAttributeSize,
                     "just checking");
       }
       for (int i = 0; i < length; i += InnerClassAttributeOffset.innerClassNextOffset) {
         if (i == length - EnclosingMethodAttributeOffset.enclosingMethodAttributeSize) {
             break;
         }
         int ioff = innerClassList.at(i +
                        InnerClassAttributeOffset.innerClassInnerClassInfoOffset);
         // 'ioff' can be zero.
         // refer to JVM spec. section 4.7.5.
         if (ioff != 0) {
            Symbol innerName = getConstants().getKlassNameAt(ioff);
            Symbol myname = getName();
            int ooff = innerClassList.at(i +
                        InnerClassAttributeOffset.innerClassOuterClassInfoOffset);
            // for anonymous classes inner_name_index of InnerClasses
            // attribute is zero.
            int innerNameIndex = innerClassList.at(i +
                        InnerClassAttributeOffset.innerClassInnerNameOffset);
            // if this is not a member (anonymous, local etc.), 'ooff' will be zero
            // refer to JVM spec. section 4.7.5.
            if (ooff == 0) {
               if (includeLocals) {
                  // does it looks like my local class?
                  if (innerName.equals(sym) &&
                     innerName.asString().startsWith(myname.asString())) {
                     // exclude anonymous classes.
                     return (innerNameIndex != 0);
                  }
               }
            } else {
               Symbol outerName = getConstants().getKlassNameAt(ooff);

               // include only if current class is outer class.
               if (outerName.equals(myname) && innerName.equals(sym)) {
                  return true;
               }
           }
         }
       } // for inner classes
       return false;
    } else {
       return false;
    }
  }

  public boolean implementsInterface(Klass k) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(k.isInterface(), "should not reach here");
    }
    KlassArray interfaces =  getTransitiveInterfaces();
    final int len = interfaces.length();
    for (int i = 0; i < len; i++) {
      if (interfaces.getAt(i).equals(k)) return true;
    }
    return false;
  }

  boolean computeSubtypeOf(Klass k) {
    if (k.isInterface()) {
      return implementsInterface(k);
    } else {
      return super.computeSubtypeOf(k);
    }
  }

  public void printValueOn(PrintStream tty) {
    tty.print("InstanceKlass for " + getName().asString());
  }

  public void iterateFields(MetadataVisitor visitor) {
    super.iterateFields(visitor);
    visitor.doMetadata(arrayKlasses, true);
    // visitor.doOop(methods, true);
    // visitor.doOop(localInterfaces, true);
    // visitor.doOop(transitiveInterfaces, true);
      visitor.doCInt(nonstaticFieldSize, true);
      visitor.doCInt(staticFieldSize, true);
      visitor.doCInt(staticOopFieldCount, true);
      visitor.doCInt(nonstaticOopMapSize, true);
      visitor.doCInt(isMarkedDependent, true);
      visitor.doCInt(initState, true);
      visitor.doCInt(itableLen, true);
    }

  /*
   *  Visit the static fields of this InstanceKlass with the obj of
   *  the visitor set to the oop holding the fields, which is
   *  currently the java mirror.
   */
  public void iterateStaticFields(OopVisitor visitor) {
    visitor.setObj(getJavaMirror());
    visitor.prologue();
    iterateStaticFieldsInternal(visitor);
    visitor.epilogue();

  }

  void iterateStaticFieldsInternal(OopVisitor visitor) {
    int length = getJavaFieldsCount();
    for (int index = 0; index < length; index++) {
      short accessFlags    = getFieldAccessFlags(index);
      FieldType   type   = new FieldType(getFieldSignature(index));
      AccessFlags access = new AccessFlags(accessFlags);
      if (access.isStatic()) {
        visitField(visitor, type, index);
      }
    }
  }

  public Klass getJavaSuper() {
    return getSuper();
  }

  public static class StaticField {
    public AccessFlags flags;
    public Field field;

    StaticField(Field field, AccessFlags flags) {
      this.field = field;
      this.flags = flags;
    }
  }

  public Field[] getStaticFields() {
    U2Array fields = getFields();
    int length = getJavaFieldsCount();
    ArrayList<Field> result = new ArrayList<>();
    for (int index = 0; index < length; index++) {
      Field f = newField(index);
      if (f.isStatic()) {
        result.add(f);
      }
    }
    return result.toArray(new Field[result.size()]);
  }

  public void iterateNonStaticFields(OopVisitor visitor, Oop obj) {
    if (getSuper() != null) {
      ((InstanceKlass) getSuper()).iterateNonStaticFields(visitor, obj);
    }
    int length = getJavaFieldsCount();
    for (int index = 0; index < length; index++) {
      short accessFlags    = getFieldAccessFlags(index);
      FieldType   type   = new FieldType(getFieldSignature(index));
      AccessFlags access = new AccessFlags(accessFlags);
      if (!access.isStatic()) {
        visitField(visitor, type, index);
      }
    }
  }

  /** Field access by name. */
  public Field findLocalField(String name, String sig) {
    int length = getJavaFieldsCount();
    for (int i = 0; i < length; i++) {
      Symbol f_name = getFieldName(i);
      Symbol f_sig  = getFieldSignature(i);
      if (f_name.equals(name) && f_sig.equals(sig)) {
        return newField(i);
      }
    }

    return null;
  }

  /** Find field in direct superinterfaces. */
  public Field findInterfaceField(String name, String sig) {
    KlassArray interfaces = getLocalInterfaces();
    int n = interfaces.length();
    for (int i = 0; i < n; i++) {
      InstanceKlass intf1 = (InstanceKlass) interfaces.getAt(i);
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(intf1.isInterface(), "just checking type");
     }
      // search for field in current interface
      Field f = intf1.findLocalField(name, sig);
      if (f != null) {
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(f.getAccessFlagsObj().isStatic(), "interface field must be static");
        }
        return f;
      }
      // search for field in direct superinterfaces
      f = intf1.findInterfaceField(name, sig);
      if (f != null) return f;
    }
    // otherwise field lookup fails
    return null;
  }

  /** Find field according to JVM spec 5.4.3.2, returns the klass in
      which the field is defined. */
  public Field findField(String name, String sig) {
    // search order according to newest JVM spec (5.4.3.2, p.167).
    // 1) search for field in current klass
    Field f = findLocalField(name, sig);
    if (f != null) return f;

    // 2) search for field recursively in direct superinterfaces
    f = findInterfaceField(name, sig);
    if (f != null) return f;

    // 3) apply field lookup recursively if superclass exists
    InstanceKlass supr = (InstanceKlass) getSuper();
    if (supr != null) return supr.findField(name, sig);

    // 4) otherwise field lookup fails
    return null;
  }

  /** Find field according to JVM spec 5.4.3.2, returns the klass in
      which the field is defined (retained only for backward
      compatibility with jdbx) */
  public Field findFieldDbg(String name, String sig) {
    return findField(name, sig);
  }

  /** Get field by its index in the fields array. Only designed for
      use in a debugging system. */
  public Field getFieldByIndex(int fieldIndex) {
    return newField(fieldIndex);
  }


    /** Return a List of SA Fields for the fields declared in this class.
        Inherited fields are not included.
        Return an empty list if there are no fields declared in this class.
        Only designed for use in a debugging system. */
    public List<Field> getImmediateFields() {
        // A list of Fields for each field declared in this class/interface,
        // not including inherited fields.
        int length = getJavaFieldsCount();
        List<Field> immediateFields = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            immediateFields.add(getFieldByIndex(index));
        }

        return immediateFields;
    }

    /** Return a List of SA Fields for all the java fields in this class,
        including all inherited fields.  This includes hidden
        fields.  Thus the returned list can contain fields with
        the same name.
        Return an empty list if there are no fields.
        Only designed for use in a debugging system. */
    public List<Field> getAllFields() {
        // Contains a Field for each field in this class, including immediate
        // fields and inherited fields.
        List<Field> allFields = getImmediateFields();

        // transitiveInterfaces contains all interfaces implemented
        // by this class and its superclass chain with no duplicates.

        KlassArray interfaces = getTransitiveInterfaces();
        int n = interfaces.length();
        for (int i = 0; i < n; i++) {
            InstanceKlass intf1 = (InstanceKlass) interfaces.getAt(i);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(intf1.isInterface(), "just checking type");
            }
            allFields.addAll(intf1.getImmediateFields());
        }

        // Get all fields in the superclass, recursively.  But, don't
        // include fields in interfaces implemented by superclasses;
        // we already have all those.
        if (!isInterface()) {
            InstanceKlass supr;
            if  ( (supr = (InstanceKlass) getSuper()) != null) {
                allFields.addAll(supr.getImmediateFields());
            }
        }

        return allFields;
    }


    /** Return a List of SA Methods declared directly in this class/interface.
        Return an empty list if there are none, or if this isn't a class/
        interface.
    */
    public List<Method> getImmediateMethods() {
      // Contains a Method for each method declared in this class/interface
      // not including inherited methods.

      MethodArray methods = getMethods();
      int length = methods.length();
      Method[] tmp = new Method[length];

      IntArray methodOrdering = getMethodOrdering();
      if (methodOrdering.length() != length) {
         // no ordering info present
         for (int index = 0; index < length; index++) {
            tmp[index] = methods.at(index);
         }
      } else {
         for (int index = 0; index < length; index++) {
            int originalIndex = methodOrdering.at(index);
            tmp[originalIndex] = methods.at(index);
         }
      }

      return Arrays.asList(tmp);
    }

    /** Return a List containing an SA InstanceKlass for each
        interface named in this class's 'implements' clause.
    */
    public List<Klass> getDirectImplementedInterfaces() {
        // Contains an InstanceKlass for each interface in this classes
        // 'implements' clause.

        KlassArray interfaces = getLocalInterfaces();
        int length = interfaces.length();
        List<Klass> directImplementedInterfaces = new ArrayList<>(length);

        for (int index = 0; index < length; index ++) {
            directImplementedInterfaces.add(interfaces.getAt(index));
        }

        return directImplementedInterfaces;
    }

  public Klass arrayKlassImpl(boolean orNull, int n) {
    // FIXME: in reflective system this would need to change to
    // actually allocate
    if (getArrayKlasses() == null) { return null; }
    ObjArrayKlass oak = (ObjArrayKlass) getArrayKlasses();
    if (orNull) {
      return oak.arrayKlassOrNull(n);
    }
    return oak.arrayKlass(n);
  }

  public Klass arrayKlassImpl(boolean orNull) {
    return arrayKlassImpl(orNull, 1);
  }

  public String signature() {
     return "L" + super.signature() + ";";
  }

  /** Find method in vtable. */
  public Method findMethod(String name, String sig) {
    return findMethod(getMethods(), name, sig);
  }

  /** Breakpoint support (see methods on Method* for details) */
  public BreakpointInfo getBreakpoints() {
    if (!VM.getVM().isJvmtiSupported()) {
      return null;
    }
    Address addr = getAddress().getAddressAt(breakpoints.getOffset());
    return (BreakpointInfo) VMObjectFactory.newObject(BreakpointInfo.class, addr);
  }

  public IntArray  getMethodOrdering() {
    Address addr = getAddress().getAddressAt(methodOrdering.getOffset());
    return (IntArray) VMObjectFactory.newObject(IntArray.class, addr);
  }

  public U2Array getFields() {
    Address addr = getAddress().getAddressAt(fields.getOffset());
    return (U2Array) VMObjectFactory.newObject(U2Array.class, addr);
  }

  public U2Array getInnerClasses() {
    Address addr = getAddress().getAddressAt(innerClasses.getOffset());
    return (U2Array) VMObjectFactory.newObject(U2Array.class, addr);
  }


  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void visitField(OopVisitor visitor, FieldType type, int index) {
    Field f = newField(index);
    if (type.isOop()) {
      visitor.doOop((OopField) f, false);
      return;
    }
    if (type.isByte()) {
      visitor.doByte((ByteField) f, false);
      return;
    }
    if (type.isChar()) {
      visitor.doChar((CharField) f, false);
      return;
    }
    if (type.isDouble()) {
      visitor.doDouble((DoubleField) f, false);
      return;
    }
    if (type.isFloat()) {
      visitor.doFloat((FloatField) f, false);
      return;
    }
    if (type.isInt()) {
      visitor.doInt((IntField) f, false);
      return;
    }
    if (type.isLong()) {
      visitor.doLong((LongField) f, false);
      return;
    }
    if (type.isShort()) {
      visitor.doShort((ShortField) f, false);
      return;
    }
    if (type.isBoolean()) {
      visitor.doBoolean((BooleanField) f, false);
      return;
    }
  }

  // Creates new field from index in fields TypeArray
  private Field newField(int index) {
    FieldType type = new FieldType(getFieldSignature(index));
    if (type.isOop()) {
     if (VM.getVM().isCompressedOopsEnabled()) {
        return new NarrowOopField(this, index);
     } else {
        return new OopField(this, index);
     }
    }
    if (type.isByte()) {
      return new ByteField(this, index);
    }
    if (type.isChar()) {
      return new CharField(this, index);
    }
    if (type.isDouble()) {
      return new DoubleField(this, index);
    }
    if (type.isFloat()) {
      return new FloatField(this, index);
    }
    if (type.isInt()) {
      return new IntField(this, index);
    }
    if (type.isLong()) {
      return new LongField(this, index);
    }
    if (type.isShort()) {
      return new ShortField(this, index);
    }
    if (type.isBoolean()) {
      return new BooleanField(this, index);
    }
    throw new RuntimeException("Illegal field type at index " + index);
  }

  private static Method findMethod(MethodArray methods, String name, String signature) {
    int index = linearSearch(methods, name, signature);
    if (index != -1) {
      return methods.at(index);
    } else {
      return null;
    }
  }

  private static int linearSearch(MethodArray methods, String name, String signature) {
    int len = (int) methods.length();
    for (int index = 0; index < len; index++) {
      Method m = methods.at(index);
      if (m.getSignature().equals(signature) && m.getName().equals(name)) {
        return index;
      }
    }
    return -1;
  }

  public void dumpReplayData(PrintStream out) {
    ConstantPool cp = getConstants();

    // Try to record related loaded classes
    Klass sub = getSubklassKlass();
    while (sub != null) {
        if (sub instanceof InstanceKlass) {
            out.println("instanceKlass " + sub.getName().asString());
        }
        sub = sub.getNextSiblingKlass();
    }

    final int length = (int) cp.getLength();
    out.print("ciInstanceKlass " + getName().asString() + " " + (isLinked() ? 1 : 0) + " " + (isInitialized() ? 1 : 0) + " " + length);
    for (int index = 1; index < length; index++) {
      out.print(" " + cp.getTags().at(index));
    }
    out.println();
    if (isInitialized()) {
      Field[] staticFields = getStaticFields();
      for (int i = 0; i < staticFields.length; i++) {
        Field f = staticFields[i];
        Oop mirror = getJavaMirror();
        if (f.isFinal() && !f.hasInitialValue()) {
          out.print("staticfield " + getName().asString() + " " +
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
