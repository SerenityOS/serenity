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
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

// A ConstantPool is an oop containing class constants
// as described in the class file

public class ConstantPool extends Metadata implements ClassConstants {
  private class CPSlot {
    private Address ptr;

    CPSlot(Address ptr) {
      this.ptr = ptr;
    }

    public Symbol getSymbol() {
      // (Lowest bit == 1) -> this is an pseudo string.
      return Symbol.create(ptr.andWithMask(~1));
    }
  }
  private class CPKlassSlot {
    private int name_index;
    private int resolved_klass_index;
    private static final int temp_resolved_klass_index = 0xffff;

    public CPKlassSlot(int n, int rk) {
      name_index = n;
      resolved_klass_index = rk;
    }
    public int getNameIndex() {
      return name_index;
    }
    public int getResolvedKlassIndex() {
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(resolved_klass_index != temp_resolved_klass_index, "constant pool merging was incomplete");
      }
      return resolved_klass_index;
    }
  }

  // Used for debugging this code
  private static final boolean DEBUG = false;

  protected void debugMessage(String message) {
    System.out.println(message);
  }

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type   = db.lookupType("ConstantPool");
    tags        = type.getAddressField("_tags");
    operands    = type.getAddressField("_operands");
    cache       = type.getAddressField("_cache");
    poolHolder  = new MetadataField(type.getAddressField("_pool_holder"), 0);
    length      = new CIntField(type.getCIntegerField("_length"), 0);
    resolved_klasses = type.getAddressField("_resolved_klasses");
    majorVersion         = new CIntField(type.getCIntegerField("_major_version"), 0);
    minorVersion         = new CIntField(type.getCIntegerField("_minor_version"), 0);
    sourceFileNameIndex  = new CIntField(type.getCIntegerField("_source_file_name_index"), 0);
    genericSignatureIndex = new CIntField(type.getCIntegerField("_generic_signature_index"), 0);
    headerSize  = type.getSize();
    elementSize = 0;
    // fetch constants:
    INDY_BSM_OFFSET = db.lookupIntConstant("ConstantPool::_indy_bsm_offset").intValue();
    INDY_ARGC_OFFSET = db.lookupIntConstant("ConstantPool::_indy_argc_offset").intValue();
    INDY_ARGV_OFFSET = db.lookupIntConstant("ConstantPool::_indy_argv_offset").intValue();
  }

  public ConstantPool(Address addr) {
    super(addr);
  }

  public boolean isConstantPool()      { return true; }

  private static AddressField tags;
  private static AddressField operands;
  private static AddressField cache;
  private static AddressField resolved_klasses;
  private static MetadataField poolHolder;
  private static CIntField length; // number of elements in oop
  private static CIntField majorVersion;
  private static CIntField minorVersion;
  private static CIntField genericSignatureIndex;
  private static CIntField sourceFileNameIndex;

  private static long headerSize;
  private static long elementSize;

  private static int INDY_BSM_OFFSET;
  private static int INDY_ARGC_OFFSET;
  private static int INDY_ARGV_OFFSET;

  public U1Array           getTags()       { return new U1Array(tags.getValue(getAddress())); }
  public U2Array           getOperands()   { return new U2Array(operands.getValue(getAddress())); }
  public ConstantPoolCache getCache()      {
    Address addr = cache.getValue(getAddress());
    return (ConstantPoolCache) VMObjectFactory.newObject(ConstantPoolCache.class, addr);
  }
  public InstanceKlass     getPoolHolder() { return (InstanceKlass)poolHolder.getValue(this); }
  public int               getLength()     { return (int)length.getValue(getAddress()); }
  public Oop               getResolvedReferences() {
    return getCache().getResolvedReferences();
  }
  public long      majorVersion()           { return majorVersion.getValue(this); }
  public long      minorVersion()           { return minorVersion.getValue(this); }

  public Symbol    getGenericSignature()    {
    long index = genericSignatureIndex.getValue(this);
    if (index != 0) {
      return getSymbolAt(index);
    } else {
      return null;
    }
  }

  public Symbol    getSourceFileName()      { return getSymbolAt(sourceFileNameIndex.getValue(this)); }

  public KlassArray        getResolvedKlasses() {
    return new KlassArray(resolved_klasses.getValue(getAddress()));
  }

  public U2Array referenceMap() {
    return getCache().referenceMap();
  }

  public int objectToCPIndex(int index) {
    return referenceMap().at(index);
  }

  private long getElementSize() {
    if (elementSize !=0 ) {
      return elementSize;
    } else {
      elementSize = VM.getVM().getOopSize();
    }
    return elementSize;
  }

  private long indexOffset(long index) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(index >= 0 && index < getLength(),  "invalid cp index " + index + " " + getLength());
    }
    return (index * getElementSize()) + headerSize;
  }

  public ConstantTag getTagAt(long index) {
    return new ConstantTag((byte)getTags().at((int) index));
  }

  public CPSlot getSlotAt(long index) {
    return new CPSlot(getAddressAtRaw(index));
  }

  public CPKlassSlot getKlassSlotAt(long index) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(getTagAt(index).isUnresolvedKlass() || getTagAt(index).isKlass(), "Corrupted constant pool");
    }
    int value = getIntAt(index);
    int name_index = extractHighShortFromInt(value);
    int resolved_klass_index = extractLowShortFromInt(value);
    return new CPKlassSlot(name_index, resolved_klass_index);
  }

  public Address getAddressAtRaw(long index) {
    return getAddress().getAddressAt(indexOffset(index));
  }

  public Symbol getSymbolAt(long index) {
    return Symbol.create(getAddressAtRaw(index));
  }

  public int getIntAt(long index){
    return getAddress().getJIntAt(indexOffset(index));
  }

  public float getFloatAt(long index){
    return getAddress().getJFloatAt(indexOffset(index));
  }

  public long getLongAt(long index) {
    int oneHalf = getAddress().getJIntAt(indexOffset(index + 1));
    int otherHalf   = getAddress().getJIntAt(indexOffset(index));
    // buildLongFromIntsPD accepts higher address value, lower address value
    // in that order.
    return VM.getVM().buildLongFromIntsPD(oneHalf, otherHalf);
  }

  public double getDoubleAt(long index) {
    return Double.longBitsToDouble(getLongAt(index));
  }

  public int getFieldOrMethodAt(int which) {
    if (DEBUG) {
      System.err.print("ConstantPool.getFieldOrMethodAt(" + which + "): new index = ");
    }
    int i = -1;
    ConstantPoolCache cache = getCache();
    if (cache == null) {
      i = which;
    } else {
      // change byte-ordering and go via cache
      i = cache.getEntryAt(0xFFFF & which).getConstantPoolIndex();
    }
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(getTagAt(i).isFieldOrMethod(), "Corrupted constant pool");
    }
    if (DEBUG) {
      System.err.println(i);
    }
    int res = getIntAt(i);
    if (DEBUG) {
      System.err.println("ConstantPool.getFieldOrMethodAt(" + i + "): result = " + res);
    }
    return res;
  }

  public int[] getNameAndTypeAt(int which) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(getTagAt(which).isNameAndType(), "Corrupted constant pool: " + which + " " + getTagAt(which));
    }
    int i = getIntAt(which);
    if (DEBUG) {
      System.err.println("ConstantPool.getNameAndTypeAt(" + which + "): result = " + i);
    }
    return new int[] { extractLowShortFromInt(i), extractHighShortFromInt(i) };
  }

  public Symbol getNameRefAt(int which) {
    return implGetNameRefAt(which, false);
  }

  public Symbol uncachedGetNameRefAt(int which) {
    return implGetNameRefAt(which, true);
  }

  private Symbol implGetNameRefAt(int which, boolean uncached) {
    int signatureIndex = getNameRefIndexAt(implNameAndTypeRefIndexAt(which, uncached));
    return getSymbolAt(signatureIndex);
  }

  public Symbol getSignatureRefAt(int which) {
    return implGetSignatureRefAt(which, false);
  }

  public Symbol uncachedGetSignatureRefAt(int which) {
    return implGetSignatureRefAt(which, true);
  }

  private Symbol implGetSignatureRefAt(int which, boolean uncached) {
    int signatureIndex = getSignatureRefIndexAt(implNameAndTypeRefIndexAt(which, uncached));
    return getSymbolAt(signatureIndex);
  }

  public static boolean isInvokedynamicIndex(int i) { return (i < 0); }

  public static int  decodeInvokedynamicIndex(int i) { Assert.that(isInvokedynamicIndex(i),  ""); return ~i; }

  // The invokedynamic points at a CP cache entry.  This entry points back
  // at the original CP entry (CONSTANT_InvokeDynamic) and also (via f2) at an entry
  // in the resolved_references array (which provides the appendix argument).
  public int invokedynamicCPCacheIndex(int index) {
    Assert.that(isInvokedynamicIndex(index), "should be a invokedynamic index");
    return decodeInvokedynamicIndex(index);
  }

  ConstantPoolCacheEntry invokedynamicCPCacheEntryAt(int index) {
    // decode index that invokedynamic points to.
    int cpCacheIndex = invokedynamicCPCacheIndex(index);
    return getCache().getEntryAt(cpCacheIndex);
  }

  private int implNameAndTypeRefIndexAt(int which, boolean uncached) {
    int i = which;
    if (!uncached && getCache() != null) {
      if (isInvokedynamicIndex(which)) {
        // Invokedynamic index is index into resolved_references
        int poolIndex = invokedynamicCPCacheEntryAt(which).getConstantPoolIndex();
        poolIndex = invokeDynamicNameAndTypeRefIndexAt(poolIndex);
        Assert.that(getTagAt(poolIndex).isNameAndType(), "");
        return poolIndex;
      }
      // change byte-ordering and go via cache
      i = remapInstructionOperandFromCache(which);
    } else {
      if (getTagAt(which).isInvokeDynamic() || getTagAt(which).isDynamicConstant()) {
        int poolIndex = invokeDynamicNameAndTypeRefIndexAt(which);
        Assert.that(getTagAt(poolIndex).isNameAndType(), "");
        return poolIndex;
      }
    }
    // assert(tag_at(i).is_field_or_method(), "Corrupted constant pool");
    // assert(!tag_at(i).is_invoke_dynamic(), "Must be handled above");
    int refIndex = getIntAt(i);
    return extractHighShortFromInt(refIndex);
  }

  private int remapInstructionOperandFromCache(int operand) {
    int cpc_index = operand;
    // DEBUG_ONLY(cpc_index -= CPCACHE_INDEX_TAG);
    // assert((int)(u2)cpc_index == cpc_index, "clean u2");
    int member_index = getCache().getEntryAt(cpc_index).getConstantPoolIndex();
    return member_index;
  }

  public int invokeDynamicNameAndTypeRefIndexAt(int which) {
    // assert(tag_at(which).is_invoke_dynamic(), "Corrupted constant pool");
    return extractHighShortFromInt(getIntAt(which));
  }

  // returns null, if not resolved.
  public Klass getKlassAt(int which) {
    if( ! getTagAt(which).isKlass()) return null;
    int resolved_klass_index = getKlassSlotAt(which).getResolvedKlassIndex();
    KlassArray resolved_klasses = getResolvedKlasses();
    return resolved_klasses.getAt(resolved_klass_index);
  }

  public Symbol getKlassNameAt(int which) {
    int name_index = getKlassSlotAt(which).getNameIndex();
    return getSymbolAt(name_index);
  }

  public Symbol getUnresolvedStringAt(int which) {
    return getSlotAt(which).getSymbol();
  }

  // returns null, if not resolved.
  public Klass getFieldOrMethodKlassRefAt(int which) {
    int refIndex = getFieldOrMethodAt(which);
    int klassIndex = extractLowShortFromInt(refIndex);
    return getKlassAt(klassIndex);
  }

  // returns null, if not resolved.
  public Method getMethodRefAt(int which) {
    Klass klass = getFieldOrMethodKlassRefAt(which);
    if (klass == null) return null;
    Symbol name = getNameRefAt(which);
    Symbol sig  = getSignatureRefAt(which);
    // Consider the super class for arrays. (java.lang.Object)
    if (klass.isArrayKlass()) {
       klass = klass.getJavaSuper();
    }
    return ((InstanceKlass)klass).findMethod(name.asString(), sig.asString());
  }

  // returns null, if not resolved.
  public Field getFieldRefAt(int which) {
    InstanceKlass klass = (InstanceKlass)getFieldOrMethodKlassRefAt(which);
    if (klass == null) return null;
    Symbol name = getNameRefAt(which);
    Symbol sig  = getSignatureRefAt(which);
    return klass.findField(name.asString(), sig.asString());
  }

  public int getNameAndTypeRefIndexAt(int index) {
    return implNameAndTypeRefIndexAt(index, false);
  }

  /** Lookup for entries consisting of (name_index, signature_index) */
  public int getNameRefIndexAt(int index) {
    int[] refIndex = getNameAndTypeAt(index);
    if (DEBUG) {
      System.err.println("ConstantPool.getNameRefIndexAt(" + index + "): refIndex = " + refIndex[0]+"/"+refIndex[1]);
    }
    int i = refIndex[0];
    if (DEBUG) {
      System.err.println("ConstantPool.getNameRefIndexAt(" + index + "): result = " + i);
    }
    return i;
  }

  /** Lookup for entries consisting of (name_index, signature_index) */
  public int getSignatureRefIndexAt(int index) {
    int[] refIndex = getNameAndTypeAt(index);
    if (DEBUG) {
      System.err.println("ConstantPool.getSignatureRefIndexAt(" + index + "): refIndex = " + refIndex[0]+"/"+refIndex[1]);
    }
    int i = refIndex[1];
    if (DEBUG) {
      System.err.println("ConstantPool.getSignatureRefIndexAt(" + index + "): result = " + i);
    }
    return i;
  }

  /** Lookup for MethodHandle entries. */
  public int getMethodHandleIndexAt(int i) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(getTagAt(i).isMethodHandle(), "Corrupted constant pool");
    }
    int res = extractHighShortFromInt(getIntAt(i));
    if (DEBUG) {
      System.err.println("ConstantPool.getMethodHandleIndexAt(" + i + "): result = " + res);
    }
    return res;
  }

  /** Lookup for MethodHandle entries. */
  public int getMethodHandleRefKindAt(int i) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(getTagAt(i).isMethodHandle(), "Corrupted constant pool");
    }
    int res = extractLowShortFromInt(getIntAt(i));
    if (DEBUG) {
      System.err.println("ConstantPool.getMethodHandleRefKindAt(" + i + "): result = " + res);
    }
    return res;
  }

  /** Lookup for MethodType entries. */
  public int getMethodTypeIndexAt(int i) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(getTagAt(i).isMethodType(), "Corrupted constant pool");
    }
    int res = getIntAt(i);
    if (DEBUG) {
      System.err.println("ConstantPool.getMethodHandleTypeAt(" + i + "): result = " + res);
    }
    return res;
  }

  /** Lookup for multi-operand (InvokeDynamic, Dynamic) entries. */
  public short[] getBootstrapSpecifierAt(int i) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(getTagAt(i).isInvokeDynamic() || getTagAt(i).isDynamicConstant(), "Corrupted constant pool");
    }
    int bsmSpec = extractLowShortFromInt(this.getIntAt(i));
    U2Array operands = getOperands();
    if (operands == null)  return null;  // safety first
    int basePos = VM.getVM().buildIntFromShorts(operands.at(bsmSpec * 2 + 0),
                                                operands.at(bsmSpec * 2 + 1));
    int argv = basePos + INDY_ARGV_OFFSET;
    int argc = operands.at(basePos + INDY_ARGC_OFFSET);
    int endPos = argv + argc;
    short[] values = new short[endPos - basePos];
    for (int j = 0; j < values.length; j++) {
        values[j] = operands.at(basePos+j);
    }
    return values;
  }

  final private static String[] nameForTag = new String[] {
  };

  private String nameForTag(int tag) {
    switch (tag) {
    case JVM_CONSTANT_Utf8:               return "JVM_CONSTANT_Utf8";
    case JVM_CONSTANT_Unicode:            return "JVM_CONSTANT_Unicode";
    case JVM_CONSTANT_Integer:            return "JVM_CONSTANT_Integer";
    case JVM_CONSTANT_Float:              return "JVM_CONSTANT_Float";
    case JVM_CONSTANT_Long:               return "JVM_CONSTANT_Long";
    case JVM_CONSTANT_Double:             return "JVM_CONSTANT_Double";
    case JVM_CONSTANT_Class:              return "JVM_CONSTANT_Class";
    case JVM_CONSTANT_String:             return "JVM_CONSTANT_String";
    case JVM_CONSTANT_Fieldref:           return "JVM_CONSTANT_Fieldref";
    case JVM_CONSTANT_Methodref:          return "JVM_CONSTANT_Methodref";
    case JVM_CONSTANT_InterfaceMethodref: return "JVM_CONSTANT_InterfaceMethodref";
    case JVM_CONSTANT_NameAndType:        return "JVM_CONSTANT_NameAndType";
    case JVM_CONSTANT_MethodHandle:       return "JVM_CONSTANT_MethodHandle";
    case JVM_CONSTANT_MethodType:         return "JVM_CONSTANT_MethodType";
    case JVM_CONSTANT_Dynamic:            return "JVM_CONSTANT_Dynamic";
    case JVM_CONSTANT_InvokeDynamic:      return "JVM_CONSTANT_InvokeDynamic";
    case JVM_CONSTANT_Invalid:            return "JVM_CONSTANT_Invalid";
    case JVM_CONSTANT_UnresolvedClass:    return "JVM_CONSTANT_UnresolvedClass";
    case JVM_CONSTANT_ClassIndex:         return "JVM_CONSTANT_ClassIndex";
    case JVM_CONSTANT_StringIndex:        return "JVM_CONSTANT_StringIndex";
    case JVM_CONSTANT_UnresolvedClassInError:    return "JVM_CONSTANT_UnresolvedClassInError";
    case JVM_CONSTANT_MethodHandleInError:return "JVM_CONSTANT_MethodHandleInError";
    case JVM_CONSTANT_MethodTypeInError:  return "JVM_CONSTANT_MethodTypeInError";
    }
    throw new InternalError("Unknown tag: " + tag);
  }

  public void iterateFields(MetadataVisitor visitor) {
    super.iterateFields(visitor);
    visitor.doMetadata(poolHolder, true);

      final int length = (int) getLength();
      // zero'th pool entry is always invalid. ignore it.
      for (int index = 1; index < length; index++) {
      int ctag = (int) getTags().at((int) index);
        switch (ctag) {
        case JVM_CONSTANT_ClassIndex:
        case JVM_CONSTANT_StringIndex:
        case JVM_CONSTANT_Integer:
          visitor.doInt(new IntField(new NamedFieldIdentifier(nameForTag(ctag)), indexOffset(index), true), true);
          break;

        case JVM_CONSTANT_Float:
          visitor.doFloat(new FloatField(new NamedFieldIdentifier(nameForTag(ctag)), indexOffset(index), true), true);
          break;

        case JVM_CONSTANT_Long:
          visitor.doLong(new LongField(new NamedFieldIdentifier(nameForTag(ctag)), indexOffset(index), true), true);
          // long entries occupy two slots
          index++;
          break;

        case JVM_CONSTANT_Double:
          visitor.doDouble(new DoubleField(new NamedFieldIdentifier(nameForTag(ctag)), indexOffset(index), true), true);
          // double entries occupy two slots
          index++;
          break;

        case JVM_CONSTANT_UnresolvedClassInError:
        case JVM_CONSTANT_UnresolvedClass:
        case JVM_CONSTANT_Class:
        case JVM_CONSTANT_Utf8:
          visitor.doOop(new OopField(new NamedFieldIdentifier(nameForTag(ctag)), indexOffset(index), true), true);
          break;

        case JVM_CONSTANT_Fieldref:
        case JVM_CONSTANT_Methodref:
        case JVM_CONSTANT_InterfaceMethodref:
        case JVM_CONSTANT_NameAndType:
        case JVM_CONSTANT_MethodHandle:
        case JVM_CONSTANT_MethodType:
        case JVM_CONSTANT_Dynamic:
        case JVM_CONSTANT_InvokeDynamic:
          visitor.doInt(new IntField(new NamedFieldIdentifier(nameForTag(ctag)), indexOffset(index), true), true);
          break;
        }
      }
    }

  public void writeBytes(OutputStream os) throws IOException {
          // Map between any modified UTF-8 and it's constant pool index.
          Map<String, Short> utf8ToIndex = new HashMap<>();
      DataOutputStream dos = new DataOutputStream(os);
      U1Array tags = getTags();
      int len = (int)getLength();
      int ci = 0; // constant pool index

      // collect all modified UTF-8 Strings from Constant Pool

      for (ci = 1; ci < len; ci++) {
          int cpConstType = tags.at(ci);
          if(cpConstType == JVM_CONSTANT_Utf8) {
              Symbol sym = getSymbolAt(ci);
              utf8ToIndex.put(sym.asString(), (short) ci);
          }
          else if(cpConstType == JVM_CONSTANT_Long ||
                  cpConstType == JVM_CONSTANT_Double) {
              ci++;
          }
      }


      for(ci = 1; ci < len; ci++) {
          int cpConstType = tags.at(ci);
          // write cp_info
          // write constant type
          switch(cpConstType) {
              case JVM_CONSTANT_Utf8: {
                  dos.writeByte(cpConstType);
                  Symbol sym = getSymbolAt(ci);
                  dos.writeShort((short)sym.getLength());
                  dos.write(sym.asByteArray());
                  if (DEBUG) debugMessage("CP[" + ci + "] = modified UTF-8 " + sym.asString());
                  break;
              }

              case JVM_CONSTANT_Unicode:
                  throw new IllegalArgumentException("Unicode constant!");

              case JVM_CONSTANT_Integer:
                  dos.writeByte(cpConstType);
                  dos.writeInt(getIntAt(ci));
                  if (DEBUG) debugMessage("CP[" + ci + "] = int " + getIntAt(ci));
                  break;

              case JVM_CONSTANT_Float:
                  dos.writeByte(cpConstType);
                  dos.writeFloat(getFloatAt(ci));
                  if (DEBUG) debugMessage("CP[" + ci + "] = float " + getFloatAt(ci));
                  break;

              case JVM_CONSTANT_Long: {
                  dos.writeByte(cpConstType);
                  long l = getLongAt(ci);
                  // long entries occupy two pool entries
                  ci++;
                  dos.writeLong(l);
                  break;
              }

              case JVM_CONSTANT_Double:
                  dos.writeByte(cpConstType);
                  dos.writeDouble(getDoubleAt(ci));
                  // double entries occupy two pool entries
                  ci++;
                  break;

              case JVM_CONSTANT_Class: {
                  dos.writeByte(cpConstType);
                  // Klass already resolved. ConstantPool constains Klass*.
                  Klass refKls = (Klass)Metadata.instantiateWrapperFor(getAddressAtRaw(ci));
                  String klassName = refKls.getName().asString();
                  Short s = (Short) utf8ToIndex.get(klassName);
                  dos.writeShort(s.shortValue());
                  if (DEBUG) debugMessage("CP[" + ci  + "] = class " + s);
                  break;
              }

              // case JVM_CONSTANT_ClassIndex:
              case JVM_CONSTANT_UnresolvedClassInError:
              case JVM_CONSTANT_UnresolvedClass: {
                  dos.writeByte(JVM_CONSTANT_Class);
                  String klassName = getSymbolAt(ci).asString();
                  Short s = (Short) utf8ToIndex.get(klassName);
                  dos.writeShort(s.shortValue());
                  if (DEBUG) debugMessage("CP[" + ci + "] = class " + s);
                  break;
              }

              case JVM_CONSTANT_String: {
                  dos.writeByte(cpConstType);
                  String str = getUnresolvedStringAt(ci).asString();
                  Short s = (Short) utf8ToIndex.get(str);
                  dos.writeShort(s.shortValue());
                  if (DEBUG) debugMessage("CP[" + ci + "] = string " + s);
                  break;
              }

              // all external, internal method/field references
              case JVM_CONSTANT_Fieldref:
              case JVM_CONSTANT_Methodref:
              case JVM_CONSTANT_InterfaceMethodref: {
                  dos.writeByte(cpConstType);
                  int value = getIntAt(ci);
                  short klassIndex = (short) extractLowShortFromInt(value);
                  short nameAndTypeIndex = (short) extractHighShortFromInt(value);
                  dos.writeShort(klassIndex);
                  dos.writeShort(nameAndTypeIndex);
                  if (DEBUG) debugMessage("CP[" + ci + "] = ref klass = " +
                                          klassIndex + ", N&T = " + nameAndTypeIndex);
                  break;
              }

              case JVM_CONSTANT_NameAndType: {
                  dos.writeByte(cpConstType);
                  int value = getIntAt(ci);
                  short nameIndex = (short) extractLowShortFromInt(value);
                  short signatureIndex = (short) extractHighShortFromInt(value);
                  dos.writeShort(nameIndex);
                  dos.writeShort(signatureIndex);
                  if (DEBUG) debugMessage("CP[" + ci + "] = N&T name = " + nameIndex
                                          + ", type = " + signatureIndex);
                  break;
              }

              case JVM_CONSTANT_MethodHandle: {
                  dos.writeByte(cpConstType);
                  int value = getIntAt(ci);
                  byte refKind = (byte) extractLowShortFromInt(value);
                  short memberIndex = (short) extractHighShortFromInt(value);
                  dos.writeByte(refKind);
                  dos.writeShort(memberIndex);
                  if (DEBUG) debugMessage("CP[" + ci + "] = MH kind = " +
                                          refKind + ", mem = " + memberIndex);
                  break;
              }

              case JVM_CONSTANT_MethodType: {
                  dos.writeByte(cpConstType);
                  int value = getIntAt(ci);
                  short refIndex = (short) value;
                  dos.writeShort(refIndex);
                  if (DEBUG) debugMessage("CP[" + ci + "] = MT index = " + refIndex);
                  break;
              }

              case JVM_CONSTANT_InvokeDynamic: {
                  dos.writeByte(cpConstType);
                  int value = getIntAt(ci);
                  short bsmIndex = (short) extractLowShortFromInt(value);
                  short nameAndTypeIndex = (short) extractHighShortFromInt(value);
                  dos.writeShort(bsmIndex);
                  dos.writeShort(nameAndTypeIndex);
                  if (DEBUG) debugMessage("CP[" + ci + "] = INDY bsm = " +
                                          bsmIndex + ", N&T = " + nameAndTypeIndex);
                  break;
              }

              default:
                  throw new InternalError("Unknown tag: " + cpConstType);
          } // switch
      }
      dos.flush();
      return;
  }

  public void printValueOn(PrintStream tty) {
    tty.print("ConstantPool for " + getPoolHolder().getName().asString());
  }

  public long getSize() {
    return alignSize(headerSize + getLength());
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static int extractHighShortFromInt(int val) {
    // must stay in sync with ConstantPool::name_and_type_at_put, method_at_put, etc.
    return (val >> 16) & 0xFFFF;
  }

  private static int extractLowShortFromInt(int val) {
    // must stay in sync with ConstantPool::name_and_type_at_put, method_at_put, etc.
    return val & 0xFFFF;
  }
}
