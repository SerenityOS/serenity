/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class ConstMethod extends Metadata {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  // anon-enum constants for _flags.
  private static int HAS_LINENUMBER_TABLE;
  private static int HAS_CHECKED_EXCEPTIONS;
  private static int HAS_LOCALVARIABLE_TABLE;
  private static int HAS_EXCEPTION_TABLE;
  private static int HAS_GENERIC_SIGNATURE;
  private static int HAS_METHOD_ANNOTATIONS;
  private static int HAS_PARAMETER_ANNOTATIONS;
  private static int HAS_METHOD_PARAMETERS;
  private static int HAS_DEFAULT_ANNOTATIONS;
  private static int HAS_TYPE_ANNOTATIONS;

  private static final int sizeofShort = 2;

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type                  = db.lookupType("ConstMethod");
    constants                  = new MetadataField(type.getAddressField("_constants"), 0);
    constMethodSize            = new CIntField(type.getCIntegerField("_constMethod_size"), 0);
    flags                      = new CIntField(type.getCIntegerField("_flags"), 0);

    // enum constants for flags
    HAS_LINENUMBER_TABLE      = db.lookupIntConstant("ConstMethod::_has_linenumber_table").intValue();
    HAS_CHECKED_EXCEPTIONS     = db.lookupIntConstant("ConstMethod::_has_checked_exceptions").intValue();
    HAS_LOCALVARIABLE_TABLE   = db.lookupIntConstant("ConstMethod::_has_localvariable_table").intValue();
    HAS_EXCEPTION_TABLE       = db.lookupIntConstant("ConstMethod::_has_exception_table").intValue();
    HAS_GENERIC_SIGNATURE     = db.lookupIntConstant("ConstMethod::_has_generic_signature").intValue();
    HAS_METHOD_ANNOTATIONS    = db.lookupIntConstant("ConstMethod::_has_method_annotations").intValue();
    HAS_PARAMETER_ANNOTATIONS = db.lookupIntConstant("ConstMethod::_has_parameter_annotations").intValue();
    HAS_METHOD_PARAMETERS = db.lookupIntConstant("ConstMethod::_has_method_parameters").intValue();
    HAS_DEFAULT_ANNOTATIONS   = db.lookupIntConstant("ConstMethod::_has_default_annotations").intValue();
    HAS_TYPE_ANNOTATIONS      = db.lookupIntConstant("ConstMethod::_has_type_annotations").intValue();

    // Size of Java bytecodes allocated immediately after ConstMethod*.
    codeSize                   = new CIntField(type.getCIntegerField("_code_size"), 0);
    nameIndex                  = new CIntField(type.getCIntegerField("_name_index"), 0);
    signatureIndex             = new CIntField(type.getCIntegerField("_signature_index"), 0);
    idnum                      = new CIntField(type.getCIntegerField("_method_idnum"), 0);
    maxStack                   = new CIntField(type.getCIntegerField("_max_stack"), 0);
    maxLocals                  = new CIntField(type.getCIntegerField("_max_locals"), 0);
    sizeOfParameters           = new CIntField(type.getCIntegerField("_size_of_parameters"), 0);

    // start of byte code
    bytecodeOffset = type.getSize();

    type                       = db.lookupType("MethodParametersElement");
    methodParametersElementSize = type.getSize();

    type                       = db.lookupType("CheckedExceptionElement");
    checkedExceptionElementSize = type.getSize();

    type                       = db.lookupType("LocalVariableTableElement");
    localVariableTableElementSize = type.getSize();

    type                       = db.lookupType("ExceptionTableElement");
    exceptionTableElementSize = type.getSize();
  }

  public ConstMethod(Address addr) {
    super(addr);
  }

  // Fields
  private static MetadataField constants;
  private static CIntField constMethodSize;
  private static CIntField flags;
  private static CIntField codeSize;
  private static CIntField nameIndex;
  private static CIntField signatureIndex;
  private static CIntField idnum;
  private static CIntField maxStack;
  private static CIntField maxLocals;
  private static CIntField sizeOfParameters;

  // start of bytecode
  private static long bytecodeOffset;
  private static long methodParametersElementSize;
  private static long checkedExceptionElementSize;
  private static long localVariableTableElementSize;
  private static long exceptionTableElementSize;

  public Method getMethod() {
    InstanceKlass ik = (InstanceKlass)getConstants().getPoolHolder();
    MethodArray methods = ik.getMethods();
    return methods.at((int)getIdNum());
  }

  // Accessors for declared fields
  public ConstantPool getConstants() {
    return (ConstantPool) constants.getValue(this);
  }

  public long getConstMethodSize() {
    return constMethodSize.getValue(this);
  }

  public long getFlags() {
    return flags.getValue(this);
  }

  public long getCodeSize() {
    return codeSize.getValue(this);
  }

  public long getNameIndex() {
    return nameIndex.getValue(this);
  }

  public long getSignatureIndex() {
    return signatureIndex.getValue(this);
  }

  public long getGenericSignatureIndex() {
    if (hasGenericSignature()) {
      return getAddress().getCIntegerAt(offsetOfGenericSignatureIndex(), 2, true);
    } else {
      return 0;
    }
  }

  public long getIdNum() {
    return idnum.getValue(this);
  }

  public long getMaxStack() {
    return maxStack.getValue(this);
  }

  public long getMaxLocals() {
    return maxLocals.getValue(this);
  }

  public long getSizeOfParameters() {
    return sizeOfParameters.getValue(this);
  }

  public Symbol getName() {
    return getMethod().getName();
  }

  public Symbol getSignature() {
    return getMethod().getSignature();
  }

  public Symbol getGenericSignature() {
    return getMethod().getGenericSignature();
  }

  // bytecode accessors

  /** See if address is in the Method's bytecodes */
  public boolean isAddressInMethod(Address bcp) {
    Address bytecodeStart = getAddress().addOffsetTo(bytecodeOffset);
    Address bytecodeEnd = bytecodeStart.addOffsetTo(getCodeSize() - 1);
    if (bcp.greaterThanOrEqual(bytecodeStart) && bcp.lessThanOrEqual(bytecodeEnd)) {
      return true;
    } else {
      return false;
    }
  }

  /** Get a bytecode or breakpoint at the given bci */
  public int getBytecodeOrBPAt(int bci) {
    return getAddress().getJByteAt(bytecodeOffset + bci) & 0xFF;
  }

  public byte getBytecodeByteArg(int bci) {
    return (byte) getBytecodeOrBPAt(bci);
  }

  /** Fetches a 16-bit big-endian ("Java ordered") value from the
      bytecode stream */
  public short getBytecodeShortArg(int bci) {
    int hi = getBytecodeOrBPAt(bci);
    int lo = getBytecodeOrBPAt(bci + 1);
    return (short) ((hi << 8) | lo);
  }

  /** Fetches a 16-bit native ordered value from the
      bytecode stream */
  public short getNativeShortArg(int bci) {
    int hi = getBytecodeOrBPAt(bci);
    int lo = getBytecodeOrBPAt(bci + 1);
    if (VM.getVM().isBigEndian()) {
        return (short) ((hi << 8) | lo);
    } else {
        return (short) ((lo << 8) | hi);
    }
  }

  /** Fetches a 32-bit big-endian ("Java ordered") value from the
      bytecode stream */
  public int getBytecodeIntArg(int bci) {
    int b4 = getBytecodeOrBPAt(bci);
    int b3 = getBytecodeOrBPAt(bci + 1);
    int b2 = getBytecodeOrBPAt(bci + 2);
    int b1 = getBytecodeOrBPAt(bci + 3);

    return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
  }

  /** Fetches a 32-bit native ordered value from the
      bytecode stream */
  public int getNativeIntArg(int bci) {
    int b4 = getBytecodeOrBPAt(bci);
    int b3 = getBytecodeOrBPAt(bci + 1);
    int b2 = getBytecodeOrBPAt(bci + 2);
    int b1 = getBytecodeOrBPAt(bci + 3);

    if (VM.getVM().isBigEndian()) {
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    } else {
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }
  }

  public byte[] getByteCode() {
     byte[] bc = new byte[ (int) getCodeSize() ];
     for( int i=0; i < bc.length; i++ )
     {
        long offs = bytecodeOffset + i;
        bc[i] = getAddress().getJByteAt( offs );
     }
     return bc;
  }

  public long getSize() {
    return getConstMethodSize();
  }

  public void printValueOn(PrintStream tty) {
    tty.print("ConstMethod " + getName().asString() + getSignature().asString() + "@" + getAddress());
  }

  public void iterateFields(MetadataVisitor visitor) {
    visitor.doMetadata(constants, true);
      visitor.doCInt(constMethodSize, true);
      visitor.doCInt(flags, true);
      visitor.doCInt(codeSize, true);
      visitor.doCInt(nameIndex, true);
      visitor.doCInt(signatureIndex, true);
      visitor.doCInt(codeSize, true);
      visitor.doCInt(maxStack, true);
      visitor.doCInt(maxLocals, true);
      visitor.doCInt(sizeOfParameters, true);
    }

  // Accessors

  public boolean hasLineNumberTable() {
    return (getFlags() & HAS_LINENUMBER_TABLE) != 0;
  }

  public int getLineNumberFromBCI(int bci) {
    if (!VM.getVM().isCore()) {
      if (bci == DebugInformationRecorder.SYNCHRONIZATION_ENTRY_BCI) bci = 0;
    }

    if (isNative()) {
      return -1;
    }

    if (Assert.ASSERTS_ENABLED) {
        Assert.that(0 <= bci && bci < getCodeSize(),
                    "illegal bci(" + bci + ") codeSize(" + getCodeSize() + ")");
    }
    int bestBCI  =  0;
    int bestLine = -1;
    if (hasLineNumberTable()) {
      // The line numbers are a short array of 2-tuples [start_pc, line_number].
      // Not necessarily sorted and not necessarily one-to-one.
      CompressedLineNumberReadStream stream =
        new CompressedLineNumberReadStream(getAddress(), (int) offsetOfCompressedLineNumberTable());
      while (stream.readPair()) {
        if (stream.bci() == bci) {
          // perfect match
          return stream.line();
        } else {
          // update best_bci/line
          if (stream.bci() < bci && stream.bci() >= bestBCI) {
            bestBCI  = stream.bci();
            bestLine = stream.line();
          }
        }
      }
    }
    return bestLine;
  }

  public LineNumberTableElement[] getLineNumberTable() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(hasLineNumberTable(),
                  "should only be called if table is present");
    }
    int len = getLineNumberTableLength();
    CompressedLineNumberReadStream stream =
      new CompressedLineNumberReadStream(getAddress(), (int) offsetOfCompressedLineNumberTable());
    LineNumberTableElement[] ret = new LineNumberTableElement[len];

    for (int idx = 0; idx < len; idx++) {
      stream.readPair();
      ret[idx] = new LineNumberTableElement(stream.bci(), stream.line());
    }
    return ret;
  }

  public boolean hasLocalVariableTable() {
    return (getFlags() & HAS_LOCALVARIABLE_TABLE) != 0;
  }

  public Symbol getLocalVariableName(int bci, int slot) {
    return getMethod().getLocalVariableName(bci, slot);
  }

  /** Should only be called if table is present */
  public LocalVariableTableElement[] getLocalVariableTable() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(hasLocalVariableTable(), "should only be called if table is present");
    }
    LocalVariableTableElement[] ret = new LocalVariableTableElement[getLocalVariableTableLength()];
    long offset = offsetOfLocalVariableTable();
    for (int i = 0; i < ret.length; i++) {
      ret[i] = new LocalVariableTableElement(getAddress(), offset);
      offset += localVariableTableElementSize;
    }
    return ret;
  }

  public boolean hasExceptionTable() {
    return (getFlags() & HAS_EXCEPTION_TABLE) != 0;
  }

  public ExceptionTableElement[] getExceptionTable() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(hasExceptionTable(), "should only be called if table is present");
    }
    ExceptionTableElement[] ret = new ExceptionTableElement[getExceptionTableLength()];
    long offset = offsetOfExceptionTable();
    for (int i = 0; i < ret.length; i++) {
      ret[i] = new ExceptionTableElement(getAddress(), offset);
      offset += exceptionTableElementSize;
    }
    return ret;
  }

  public boolean hasCheckedExceptions() {
    return (getFlags() & HAS_CHECKED_EXCEPTIONS) != 0;
  }

  public CheckedExceptionElement[] getCheckedExceptions() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(hasCheckedExceptions(), "should only be called if table is present");
    }
    CheckedExceptionElement[] ret = new CheckedExceptionElement[getCheckedExceptionsLength()];
    long offset = offsetOfCheckedExceptions();
    for (int i = 0; i < ret.length; i++) {
      ret[i] = new CheckedExceptionElement(getAddress(), offset);
      offset += checkedExceptionElementSize;
    }
    return ret;
  }

  private boolean hasMethodParameters() {
    return (getFlags() & HAS_METHOD_PARAMETERS) != 0;
  }

  private boolean hasGenericSignature() {
    return (getFlags() & HAS_GENERIC_SIGNATURE) != 0;
  }

  private boolean hasMethodAnnotations() {
    return (getFlags() & HAS_METHOD_ANNOTATIONS) != 0;
  }

  private boolean hasParameterAnnotations() {
    return (getFlags() & HAS_PARAMETER_ANNOTATIONS) != 0;
  }

  private boolean hasDefaultAnnotations() {
    return (getFlags() & HAS_DEFAULT_ANNOTATIONS) != 0;
  }

  private boolean hasTypeAnnotations() {
    return (getFlags() & HAS_TYPE_ANNOTATIONS) != 0;
  }


  //---------------------------------------------------------------------------
  // Internals only below this point
  //

  private boolean isNative() {
    return getMethod().isNative();
  }

  // Offset of end of code
  private long offsetOfCodeEnd() {
    return bytecodeOffset + getCodeSize();
  }

  // Offset of start of compressed line number table (see method.hpp)
  private long offsetOfCompressedLineNumberTable() {
    return offsetOfCodeEnd() + (isNative() ? 2 * VM.getVM().getAddressSize() : 0);
  }

  // Offset of last short in Method* before annotations, if present
  private long offsetOfLastU2Element() {
    int offset = 0;
    if (hasMethodAnnotations()) offset++;
    if (hasParameterAnnotations()) offset++;
    if (hasTypeAnnotations()) offset++;
    if (hasDefaultAnnotations()) offset++;
    long wordSize = VM.getVM().getObjectHeap().getOopSize();
    return (getSize() * wordSize) - (offset * wordSize) - sizeofShort;
  }

  // Offset of the generic signature index
  private long offsetOfGenericSignatureIndex() {
    return offsetOfLastU2Element();
  }

  private long offsetOfMethodParametersLength() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(hasMethodParameters(), "should only be called if table is present");
    }
    return hasGenericSignature() ? offsetOfLastU2Element() - sizeofShort :
                                   offsetOfLastU2Element();
  }

  private int getMethodParametersLength() {
      if (hasMethodParameters())
          return (int) getAddress().getCIntegerAt(offsetOfMethodParametersLength(), 2, true);
      else
          return 0;
  }

  // Offset of start of checked exceptions
  private long offsetOfMethodParameters() {
    long offset = offsetOfMethodParametersLength();
    long length = getMethodParametersLength();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(length > 0, "should only be called if method parameter information is present");
    }
    offset -= length * methodParametersElementSize;
    return offset;
  }

  private long offsetOfCheckedExceptionsLength() {
    if (hasMethodParameters())
      return offsetOfMethodParameters() - sizeofShort;
    else {
      return hasGenericSignature() ? offsetOfLastU2Element() - sizeofShort :
                                     offsetOfLastU2Element();
    }
  }

  private int getCheckedExceptionsLength() {
    if (hasCheckedExceptions()) {
      return (int) getAddress().getCIntegerAt(offsetOfCheckedExceptionsLength(), 2, true);
    } else {
      return 0;
    }
  }

  // Offset of start of checked exceptions
  private long offsetOfCheckedExceptions() {
    long offset = offsetOfCheckedExceptionsLength();
    long length = getCheckedExceptionsLength();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(length > 0, "should only be called if table is present");
    }
    offset -= length * checkedExceptionElementSize;
    return offset;
  }

  private int getLineNumberTableLength() {
    int len = 0;
    if (hasLineNumberTable()) {
      CompressedLineNumberReadStream stream =
        new CompressedLineNumberReadStream(getAddress(), (int) offsetOfCompressedLineNumberTable());
      while (stream.readPair()) {
        len += 1;
      }
    }
    return len;
  }

  private int getLocalVariableTableLength() {
    if (hasLocalVariableTable()) {
      return (int) getAddress().getCIntegerAt(offsetOfLocalVariableTableLength(), 2, true);
    } else {
      return 0;
    }
  }

  // Offset of local variable table length
  private long offsetOfLocalVariableTableLength() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(hasLocalVariableTable(), "should only be called if table is present");
    }

    if (hasExceptionTable()) {
      return offsetOfExceptionTable() - sizeofShort;
    } else if (hasCheckedExceptions()) {
      return offsetOfCheckedExceptions() - sizeofShort;
    } else if (hasMethodParameters()) {
      return offsetOfMethodParameters() - sizeofShort;
    } else {
      return hasGenericSignature() ? offsetOfLastU2Element() - sizeofShort :
                                     offsetOfLastU2Element();
    }
  }

  private long offsetOfLocalVariableTable() {
    long offset = offsetOfLocalVariableTableLength();
    long length = getLocalVariableTableLength();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(length > 0, "should only be called if table is present");
    }
    offset -= length * localVariableTableElementSize;
    return offset;
  }

  private int getExceptionTableLength() {
    if (hasExceptionTable()) {
      return (int) getAddress().getCIntegerAt(offsetOfExceptionTableLength(), 2, true);
    } else {
      return 0;
    }
  }

  private long offsetOfExceptionTableLength() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(hasExceptionTable(), "should only be called if table is present");
    }
    if (hasCheckedExceptions()) {
      return offsetOfCheckedExceptions() - sizeofShort;
    } else if (hasMethodParameters()) {
      return offsetOfMethodParameters() - sizeofShort;
    } else {
      return hasGenericSignature() ? offsetOfLastU2Element() - sizeofShort :
                                     offsetOfLastU2Element();
    }
  }

  private long offsetOfExceptionTable() {
    long offset = offsetOfExceptionTableLength();
    long length = getExceptionTableLength();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(length > 0, "should only be called if table is present");
    }
    offset -= length * exceptionTableElementSize;
    return offset;
  }

}
