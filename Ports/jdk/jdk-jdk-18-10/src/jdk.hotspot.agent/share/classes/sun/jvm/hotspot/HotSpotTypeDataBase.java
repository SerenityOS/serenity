/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.types.basic.*;
import sun.jvm.hotspot.utilities.*;

/** <P> This is the cross-platform TypeDataBase used by the Oop
    hierarchy. The decision was made to make this cross-platform by
    having the VM export the necessary symbols via a built-in table;
    see src/share/vm/runtime/vmStructs.[ch]pp for more details. </P>

    <P> <B>WARNING</B>: clients should refer to this class through the
    TypeDataBase interface and not directly to the HotSpotTypeDataBase
    type. </P>

    <P> NOTE: since we are fetching the sizes of the Java primitive types
 */

public class HotSpotTypeDataBase extends BasicTypeDataBase {
  private Debugger symbolLookup;
  private String[] jvmLibNames;
  private static final int UNINITIALIZED_SIZE = -1;
  private static final int C_INT8_SIZE  = 1;
  private static final int C_INT32_SIZE = 4;
  private static final int C_INT64_SIZE = 8;
  private static int pointerSize = UNINITIALIZED_SIZE;
  // Counter to ensure read loops terminate:
  private static final int MAX_DUPLICATE_DEFINITIONS = 100;
  private int duplicateDefCount = 0;

  private static final boolean DEBUG;
  static {
    DEBUG = System.getProperty("sun.jvm.hotspot.HotSpotTypeDataBase.DEBUG")
            != null;
  }

  /** <P> This requires a SymbolLookup mechanism as well as the
      MachineDescription. Note that we do not need a NameMangler since
      we use the vmStructs mechanism to avoid looking up C++
      symbols. </P>

      <P> NOTE that it is guaranteed that this constructor will not
      attempt to fetch any Java values from the remote process, only C
      integers and addresses. This is required because we are fetching
      the sizes of the Java primitive types from the remote process,
      implying that attempting to fetch them before their sizes are
      known is illegal. </P>

      <P> Throws NoSuchSymbolException if a problem occurred while
      looking up one of the bootstrapping symbols related to the
      VMStructs table in the remote VM; this may indicate that the
      remote process is not actually a HotSpot VM. </P>
  */
  public HotSpotTypeDataBase(MachineDescription machDesc,
                             VtblAccess vtblAccess,
                             Debugger symbolLookup,
                             String[] jvmLibNames) throws NoSuchSymbolException {
    super(machDesc, vtblAccess);
    this.symbolLookup = symbolLookup;
    this.jvmLibNames = jvmLibNames;

    readVMTypes();
    initializePrimitiveTypes();
    readVMStructs();
    readVMIntConstants();
    readVMLongConstants();
    readExternalDefinitions();
  }

  public Type lookupType(String cTypeName, boolean throwException) {
    Type fieldType = super.lookupType(cTypeName, false);
    if (fieldType == null && cTypeName.startsWith("const ")) {
      fieldType = (BasicType)lookupType(cTypeName.substring(6), false);
    }
    if (fieldType == null && cTypeName.endsWith(" const")) {
        fieldType = (BasicType)lookupType(cTypeName.substring(0, cTypeName.length() - 6), false);
    }
    if (fieldType == null) {
      if (cTypeName.startsWith("GrowableArray<") && cTypeName.endsWith(">")) {
        String ttype = cTypeName.substring("GrowableArray<".length(),
                                            cTypeName.length() - 1);
        Type templateType = lookupType(ttype, false);
        if (templateType == null && typeNameIsPointerType(ttype)) {
          templateType = recursiveCreateBasicPointerType(ttype);
        }
        if (templateType == null) {
          lookupOrFail(ttype);
        }

        BasicType basicTargetType = createBasicType(cTypeName, false, false, false);

        // transfer fields from GrowableArrayBase to template instance
        BasicType generic = lookupOrFail("GrowableArrayBase");
        BasicType specific = lookupOrFail("GrowableArray<int>");
        basicTargetType.setSize(specific.getSize());
        Iterator fields = generic.getFields();
        while (fields.hasNext()) {
          Field f = (Field)fields.next();
          basicTargetType.addField(internalCreateField(basicTargetType, f.getName(),
                                                       f.getType(), f.isStatic(),
                                                       f.getOffset(), null));
        }
        fieldType = basicTargetType;
      }
    }
    if (fieldType == null && typeNameIsPointerType(cTypeName)) {
      fieldType = recursiveCreateBasicPointerType(cTypeName);
    }
    if (fieldType == null && throwException) {
      super.lookupType(cTypeName, true);
    }
    return fieldType;
  }

  private void readVMTypes() {
    // Get the variables we need in order to traverse the VMTypeEntry[]
    long typeEntryTypeNameOffset;
    long typeEntrySuperclassNameOffset;
    long typeEntryIsOopTypeOffset;
    long typeEntryIsIntegerTypeOffset;
    long typeEntryIsUnsignedOffset;
    long typeEntrySizeOffset;
    long typeEntryArrayStride;

    // Fetch the address of the VMTypeEntry*. We get this symbol first
    // and try to use it to make sure that symbol lookup is working.
    Address entryAddr = lookupInProcess("gHotSpotVMTypes");
    //    System.err.println("gHotSpotVMTypes address = " + entryAddr);
    // Dereference this once to get the pointer to the first VMTypeEntry
    //    dumpMemory(entryAddr, 80);
    entryAddr = entryAddr.getAddressAt(0);

    if (entryAddr == null) {
      throw new RuntimeException("gHotSpotVMTypes was not initialized properly in the remote process; can not continue");
    }

    typeEntryTypeNameOffset       = getLongValueFromProcess("gHotSpotVMTypeEntryTypeNameOffset");
    typeEntrySuperclassNameOffset = getLongValueFromProcess("gHotSpotVMTypeEntrySuperclassNameOffset");
    typeEntryIsOopTypeOffset      = getLongValueFromProcess("gHotSpotVMTypeEntryIsOopTypeOffset");
    typeEntryIsIntegerTypeOffset  = getLongValueFromProcess("gHotSpotVMTypeEntryIsIntegerTypeOffset");
    typeEntryIsUnsignedOffset     = getLongValueFromProcess("gHotSpotVMTypeEntryIsUnsignedOffset");
    typeEntrySizeOffset           = getLongValueFromProcess("gHotSpotVMTypeEntrySizeOffset");
    typeEntryArrayStride          = getLongValueFromProcess("gHotSpotVMTypeEntryArrayStride");

    if (typeEntryArrayStride == 0L) {
      throw new RuntimeException("zero stride: cannot read types.");
    }

    // Start iterating down it until we find an entry with no name
    Address typeNameAddr = null;
    do {
      // Fetch the type name first
      typeNameAddr = entryAddr.getAddressAt(typeEntryTypeNameOffset);
      if (typeNameAddr != null) {
        String typeName = CStringUtilities.getString(typeNameAddr);

        String superclassName = null;
        Address superclassNameAddr = entryAddr.getAddressAt(typeEntrySuperclassNameOffset);
        if (superclassNameAddr != null) {
          superclassName = CStringUtilities.getString(superclassNameAddr);
        }

        boolean isOopType     = (entryAddr.getCIntegerAt(typeEntryIsOopTypeOffset, C_INT32_SIZE, false) != 0);
        boolean isIntegerType = (entryAddr.getCIntegerAt(typeEntryIsIntegerTypeOffset, C_INT32_SIZE, false) != 0);
        boolean isUnsigned    = (entryAddr.getCIntegerAt(typeEntryIsUnsignedOffset, C_INT32_SIZE, false) != 0);
        long size             = entryAddr.getCIntegerAt(typeEntrySizeOffset, C_INT64_SIZE, true);

        createType(typeName, superclassName, isOopType, isIntegerType, isUnsigned, size);
        if (pointerSize == UNINITIALIZED_SIZE && typeName.equals("void*")) {
          pointerSize = (int)size;
        }
      }

      entryAddr = entryAddr.addOffsetTo(typeEntryArrayStride);
    } while (typeNameAddr != null && duplicateDefCount < MAX_DUPLICATE_DEFINITIONS);

    if (duplicateDefCount >= MAX_DUPLICATE_DEFINITIONS) {
      throw new RuntimeException("too many duplicate definitions");
    }
  }

  private void initializePrimitiveTypes() {
    // Look up the needed primitive types by name...they had better be present
    setJBooleanType(lookupPrimitiveType("jboolean"));
    setJByteType   (lookupPrimitiveType("jbyte"));
    setJCharType   (lookupPrimitiveType("jchar"));
    setJDoubleType (lookupPrimitiveType("jdouble"));
    setJFloatType  (lookupPrimitiveType("jfloat"));
    setJIntType    (lookupPrimitiveType("jint"));
    setJLongType   (lookupPrimitiveType("jlong"));
    setJShortType  (lookupPrimitiveType("jshort"));

    // Indicate that these are the Java primitive types
    ((BasicType) getJBooleanType()).setIsJavaPrimitiveType(true);
    ((BasicType) getJByteType()).setIsJavaPrimitiveType(true);
    ((BasicType) getJCharType()).setIsJavaPrimitiveType(true);
    ((BasicType) getJDoubleType()).setIsJavaPrimitiveType(true);
    ((BasicType) getJFloatType()).setIsJavaPrimitiveType(true);
    ((BasicType) getJIntType()).setIsJavaPrimitiveType(true);
    ((BasicType) getJLongType()).setIsJavaPrimitiveType(true);
    ((BasicType) getJShortType()).setIsJavaPrimitiveType(true);
  }

  private Type lookupPrimitiveType(String typeName) {
    Type type = lookupType(typeName, false);
    if (type == null) {
      throw new RuntimeException("Error initializing the HotSpotDataBase: could not find the primitive type \"" +
                                 typeName + "\" in the remote VM's VMStructs table. This type is required in " +
                                 "order to determine the size of Java primitive types. Can not continue.");
    }
    return type;
  }

  private void readExternalDefinitions() {
    String file = System.getProperty("sun.jvm.hotspot.typedb");
    if (file != null) {
      System.out.println("Reading " + file);
      BufferedReader in = null;
      try {
        StreamTokenizer t = new StreamTokenizer(in = new BufferedReader(new InputStreamReader(new FileInputStream(file))));
        t.resetSyntax();
        t.wordChars('\u0000','\uFFFF');
        t.whitespaceChars(' ', ' ');
        t.whitespaceChars('\n', '\n');
        t.whitespaceChars('\r', '\r');
        t.quoteChar('\"');
        t.eolIsSignificant(true);
        while (t.nextToken() != StreamTokenizer.TT_EOF) {
          if (t.ttype == StreamTokenizer.TT_EOL) {
            continue;
          }

          if (t.sval.equals("field")) {
            t.nextToken();
            BasicType containingType = (BasicType)lookupType(t.sval);
            t.nextToken();
            String fieldName = t.sval;

            // The field's Type must already be in the database -- no exceptions
            t.nextToken();
            Type fieldType = lookupType(t.sval);
            t.nextToken();
            boolean isStatic = Boolean.valueOf(t.sval).booleanValue();
            t.nextToken();
            long offset = Long.parseLong(t.sval);
            t.nextToken();
            Address staticAddress = null;
            if (isStatic) {
              throw new InternalError("static fields not supported");
            }

            // check to see if the field already exists
            Iterator i = containingType.getFields();
            boolean defined = false;
            while (i.hasNext()) {
              Field f = (Field) i.next();
              if (f.getName().equals(fieldName)) {
                if (f.isStatic() != isStatic) {
                  throw new RuntimeException("static/nonstatic mismatch: " + fieldName);
                }
                if (!isStatic) {
                  if (f.getOffset() != offset) {
                    throw new RuntimeException("bad redefinition of field offset: " + fieldName);
                  }
                } else {
                  if (!f.getStaticFieldAddress().equals(staticAddress)) {
                    throw new RuntimeException("bad redefinition of field location: " + fieldName);
                  }
                }
                if (f.getType() != fieldType) {
                  System.out.println(fieldType);
                  System.out.println(f.getType());
                  throw new RuntimeException("bad redefinition of field type: " + fieldName);
                }
                defined = true;
                break;
              }
            }

            if (!defined) {
              // Create field by type
              createField(containingType,
                          fieldName, fieldType,
                          isStatic,
                          offset,
                          staticAddress);
            }
          } else if (t.sval.equals("type")) {
            t.nextToken();
            String typeName = t.sval;
            t.nextToken();
            String superclassName = t.sval;
            if (superclassName.equals("null")) {
              superclassName = null;
            }
            t.nextToken();
            boolean isOop = Boolean.valueOf(t.sval).booleanValue();
            t.nextToken();
            boolean isInteger = Boolean.valueOf(t.sval).booleanValue();
            t.nextToken();
            boolean isUnsigned = Boolean.valueOf(t.sval).booleanValue();
            t.nextToken();
            long size = Long.parseLong(t.sval);

            BasicType type = null;
            try {
              type = (BasicType)lookupType(typeName);
            } catch (RuntimeException e) {
            }
            if (type != null) {
              if (type.isOopType() != isOop) {
                throw new RuntimeException("oop mismatch in type definition: " + typeName);
              }
              if (type.isCIntegerType() != isInteger) {
                throw new RuntimeException("integer type mismatch in type definition: " + typeName);
              }
              if (type.isCIntegerType() && (((CIntegerType)type).isUnsigned()) != isUnsigned) {
                throw new RuntimeException("unsigned mismatch in type definition: " + typeName);
              }
              if (type.getSuperclass() == null) {
                if (superclassName != null) {
                  if (type.getSize() == -1) {
                    type.setSuperclass(lookupType(superclassName));
                  } else {
                    throw new RuntimeException("unexpected superclass in type definition: " + typeName);
                  }
                }
              } else {
                if (superclassName == null) {
                  throw new RuntimeException("missing superclass in type definition: " + typeName);
                }
                if (!type.getSuperclass().getName().equals(superclassName)) {
                  throw new RuntimeException("incorrect superclass in type definition: " + typeName);
                }
              }
              if (type.getSize() != size) {
                if (type.getSize() == -1 || type.getSize() == 0) {
                  type.setSize(size);
                } else {
                  throw new RuntimeException("size mismatch in type definition: " + typeName + ": " + type.getSize() + " != " + size);
                }
              }
            }

            if (lookupType(typeName, false) == null) {
              // Create type
              createType(typeName, superclassName, isOop, isInteger, isUnsigned, size);
            }
          } else {
            throw new InternalError("\"" + t.sval + "\"");
          }
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      } finally {
        try {
          in.close();
        } catch (Exception e) {
        }
      }
    }
  }

  private void readVMStructs() {
    // Get the variables we need in order to traverse the VMStructEntry[]
    long structEntryTypeNameOffset;
    long structEntryFieldNameOffset;
    long structEntryTypeStringOffset;
    long structEntryIsStaticOffset;
    long structEntryOffsetOffset;
    long structEntryAddressOffset;
    long structEntryArrayStride;

    structEntryTypeNameOffset     = getLongValueFromProcess("gHotSpotVMStructEntryTypeNameOffset");
    structEntryFieldNameOffset    = getLongValueFromProcess("gHotSpotVMStructEntryFieldNameOffset");
    structEntryTypeStringOffset   = getLongValueFromProcess("gHotSpotVMStructEntryTypeStringOffset");
    structEntryIsStaticOffset     = getLongValueFromProcess("gHotSpotVMStructEntryIsStaticOffset");
    structEntryOffsetOffset       = getLongValueFromProcess("gHotSpotVMStructEntryOffsetOffset");
    structEntryAddressOffset      = getLongValueFromProcess("gHotSpotVMStructEntryAddressOffset");
    structEntryArrayStride        = getLongValueFromProcess("gHotSpotVMStructEntryArrayStride");

    if (structEntryArrayStride == 0L) {
      throw new RuntimeException("zero stride: cannot read types.");
    }

    // Fetch the address of the VMStructEntry*
    Address entryAddr = lookupInProcess("gHotSpotVMStructs");
    // Dereference this once to get the pointer to the first VMStructEntry
    entryAddr = entryAddr.getAddressAt(0);
    if (entryAddr == null) {
      throw new RuntimeException("gHotSpotVMStructs was not initialized properly in the remote process; can not continue");
    }

    // Start iterating down it until we find an entry with no name
    Address fieldNameAddr = null;
    String typeName = null;
    String fieldName = null;
    String typeString = null;
    boolean isStatic = false;
    long offset = 0;
    Address staticFieldAddr = null;
    long size = 0;
    long index = 0;
    String opaqueName = "<opaque>";
    lookupOrCreateClass(opaqueName, false, false, false);

    do {
      // Fetch the field name first
      fieldNameAddr = entryAddr.getAddressAt(structEntryFieldNameOffset);
      if (fieldNameAddr != null) {
        fieldName = CStringUtilities.getString(fieldNameAddr);

        // Now the rest of the names. Keep in mind that the type name
        // may be NULL, indicating that the type is opaque.
        Address addr = entryAddr.getAddressAt(structEntryTypeNameOffset);
        if (addr == null) {
          throw new RuntimeException("gHotSpotVMStructs unexpectedly had a NULL type name at index " + index);
        }
        typeName = CStringUtilities.getString(addr);

        addr = entryAddr.getAddressAt(structEntryTypeStringOffset);
        if (addr == null) {
          typeString = opaqueName;
        } else {
          typeString = CStringUtilities.getString(addr);
        }

        isStatic = !(entryAddr.getCIntegerAt(structEntryIsStaticOffset, C_INT32_SIZE, false) == 0);
        if (isStatic) {
          staticFieldAddr = entryAddr.getAddressAt(structEntryAddressOffset);
          offset = 0;
        } else {
          offset = entryAddr.getCIntegerAt(structEntryOffsetOffset, C_INT64_SIZE, true);
          staticFieldAddr = null;
        }

        // The containing Type must already be in the database -- no exceptions
        BasicType containingType = lookupOrFail(typeName);

        // The field's Type must already be in the database -- no exceptions
        BasicType fieldType = (BasicType)lookupType(typeString);

        // Create field by type
        createField(containingType, fieldName, fieldType,
                    isStatic, offset, staticFieldAddr);
      }

      ++index;
      entryAddr = entryAddr.addOffsetTo(structEntryArrayStride);
    } while (fieldNameAddr != null);
  }

  private void readVMIntConstants() {
    // Get the variables we need in order to traverse the VMIntConstantEntry[]
    long intConstantEntryNameOffset;
    long intConstantEntryValueOffset;
    long intConstantEntryArrayStride;

    intConstantEntryNameOffset  = getLongValueFromProcess("gHotSpotVMIntConstantEntryNameOffset");
    intConstantEntryValueOffset = getLongValueFromProcess("gHotSpotVMIntConstantEntryValueOffset");
    intConstantEntryArrayStride = getLongValueFromProcess("gHotSpotVMIntConstantEntryArrayStride");

    if (intConstantEntryArrayStride == 0L) {
      throw new RuntimeException("zero stride: cannot read types.");
    }


    // Fetch the address of the VMIntConstantEntry*
    Address entryAddr = lookupInProcess("gHotSpotVMIntConstants");
    // Dereference this once to get the pointer to the first VMIntConstantEntry
    entryAddr = entryAddr.getAddressAt(0);
    if (entryAddr == null) {
      throw new RuntimeException("gHotSpotVMIntConstants was not initialized properly in the remote process; can not continue");
    }

    // Start iterating down it until we find an entry with no name
    Address nameAddr = null;
    do {
      // Fetch the type name first
      nameAddr = entryAddr.getAddressAt(intConstantEntryNameOffset);
      if (nameAddr != null) {
        String name = CStringUtilities.getString(nameAddr);
        int value = (int) entryAddr.getCIntegerAt(intConstantEntryValueOffset, C_INT32_SIZE, false);

        // Be a little resilient
        Integer oldValue = lookupIntConstant(name, false);
        if (oldValue == null) {
          addIntConstant(name, value);
        } else {
          if (oldValue.intValue() != value) {
            throw new RuntimeException("Error: the integer constant \"" + name +
                                       "\" had its value redefined (old was " + oldValue +
                                       ", new is " + value + ". Aborting.");
          } else {
            System.err.println("Warning: the int constant \"" + name + "\" (declared in the remote VM in VMStructs::localHotSpotVMIntConstants) " +
                               "had its value declared as " + value + " twice. Continuing.");
            duplicateDefCount++;
          }
        }
      }

      entryAddr = entryAddr.addOffsetTo(intConstantEntryArrayStride);
    } while (nameAddr != null && duplicateDefCount < MAX_DUPLICATE_DEFINITIONS);

    if (duplicateDefCount >= MAX_DUPLICATE_DEFINITIONS) {
      throw new RuntimeException("too many duplicate definitions");
    }
  }

  private void readVMLongConstants() {
    // Get the variables we need in order to traverse the VMLongConstantEntry[]
    long longConstantEntryNameOffset;
    long longConstantEntryValueOffset;
    long longConstantEntryArrayStride;

    longConstantEntryNameOffset  = getLongValueFromProcess("gHotSpotVMLongConstantEntryNameOffset");
    longConstantEntryValueOffset = getLongValueFromProcess("gHotSpotVMLongConstantEntryValueOffset");
    longConstantEntryArrayStride = getLongValueFromProcess("gHotSpotVMLongConstantEntryArrayStride");

    if (longConstantEntryArrayStride == 0L) {
      throw new RuntimeException("zero stride: cannot read types.");
    }

    // Fetch the address of the VMLongConstantEntry*
    Address entryAddr = lookupInProcess("gHotSpotVMLongConstants");
    // Dereference this once to get the pointer to the first VMLongConstantEntry
    entryAddr = entryAddr.getAddressAt(0);
    if (entryAddr == null) {
      throw new RuntimeException("gHotSpotVMLongConstants was not initialized properly in the remote process; can not continue");
    }

    // Start iterating down it until we find an entry with no name
    Address nameAddr = null;
    do {
      // Fetch the type name first
      nameAddr = entryAddr.getAddressAt(longConstantEntryNameOffset);
      if (nameAddr != null) {
        String name = CStringUtilities.getString(nameAddr);
        long value = entryAddr.getCIntegerAt(longConstantEntryValueOffset, C_INT64_SIZE, true);

        // Be a little resilient
        Long oldValue = lookupLongConstant(name, false);
        if (oldValue == null) {
          addLongConstant(name, value);
        } else {
          if (oldValue.longValue() != value) {
            throw new RuntimeException("Error: the long constant \"" + name +
                                       "\" had its value redefined (old was " + oldValue +
                                       ", new is " + value + ". Aborting.");
          } else {
            System.err.println("Warning: the long constant \"" + name + "\" (declared in the remote VM in VMStructs::localHotSpotVMLongConstants) " +
                               "had its value declared as " + value + " twice. Continuing.");
            duplicateDefCount++;
          }
        }
      }

      entryAddr = entryAddr.addOffsetTo(longConstantEntryArrayStride);
    } while (nameAddr != null && duplicateDefCount < MAX_DUPLICATE_DEFINITIONS);

    if (duplicateDefCount >= MAX_DUPLICATE_DEFINITIONS) {
      throw new RuntimeException("too many duplicate definitions.");
    }
  }

  private BasicType lookupOrFail(String typeName) {
    BasicType type = (BasicType) lookupType(typeName, false);
    if (type == null) {
      throw new RuntimeException("Type \"" + typeName + "\", referenced in VMStructs::localHotSpotVMStructs in the remote VM, " +
                                 "was not present in the remote VMStructs::localHotSpotVMTypes table (should have been caught " +
                                 "in the debug build of that VM). Can not continue.");
    }
    return type;
  }

  private long getLongValueFromProcess(String symbol) {
    return lookupInProcess(symbol).getCIntegerAt(0, C_INT64_SIZE, true);
  }

  private Address lookupInProcess(String symbol) throws NoSuchSymbolException {
    // FIXME: abstract away the loadobject name
    for (int i = 0; i < jvmLibNames.length; i++) {
      Address addr = symbolLookup.lookup(jvmLibNames[i], symbol);
      if (addr != null) {
        return addr;
      }
    }
    String errStr = "(";
    for (int i = 0; i < jvmLibNames.length; i++) {
      errStr += jvmLibNames[i];
      if (i < jvmLibNames.length - 1) {
        errStr += ", ";
      }
    }
    errStr += ")";
    throw new NoSuchSymbolException(symbol,
                                    "Could not find symbol \"" + symbol +
                                    "\" in any of the known library names " +
                                    errStr);
  }

  private BasicType lookupOrCreateClass(String typeName, boolean isOopType,
                                        boolean isIntegerType, boolean isUnsigned) {
    BasicType type = (BasicType) lookupType(typeName, false);
    if (type == null) {
      // Create a new type
      type = createBasicType(typeName, isOopType, isIntegerType, isUnsigned);
    }
    return type;
  }

  /** Creates a new BasicType, initializes its size to -1 so we can
      test to ensure that all types' sizes are initialized by VMTypes,
      and adds it to the database. Takes care of initializing integer
      and oop types properly. */
  private BasicType createBasicType(String typeName, boolean isOopType,
                                    boolean isIntegerType, boolean isUnsigned) {

    BasicType type = null;

    if (isIntegerType) {
      type = new BasicCIntegerType(this, typeName, isUnsigned);
    } else {
      if (typeNameIsPointerType(typeName)) {
        type = recursiveCreateBasicPointerType(typeName);
      } else {
        type = new BasicType(this, typeName);
      }

      if (isOopType) {
        type.setIsOopType(true);
      }
    }

    type.setSize(UNINITIALIZED_SIZE);
    addType(type);
    return type;
  }

  /** Recursively creates a PointerType from the string representation
      of the type's name. Note that this currently needs some
      workarounds due to incomplete information in the VMStructs
      database. */
  private BasicPointerType recursiveCreateBasicPointerType(String typeName) {
    BasicPointerType result = (BasicPointerType)super.lookupType(typeName, false);
    if (result != null) {
      return result;
    }
    String targetTypeName = typeName.substring(0, typeName.lastIndexOf('*')).trim();
    Type targetType = null;
    if (typeNameIsPointerType(targetTypeName)) {
      targetType = lookupType(targetTypeName, false);
      if (targetType == null) {
        targetType = recursiveCreateBasicPointerType(targetTypeName);
      }
    } else {
      targetType = lookupType(targetTypeName, false);
      if (targetType == null) {
        // Workaround for missing C integer types in database.
        // Also looks like we can't throw an exception for other
        // missing target types because there are some in old
        // VMStructs tables that didn't have the target type declared.
        // For this case, we create basic types that never get filled
        // in.

        if (targetTypeName.equals("char") ||
            targetTypeName.equals("const char")) {
          // We don't have a representation of const-ness of C types in the SA
          BasicType basicTargetType = createBasicType(targetTypeName, false, true, false);
          basicTargetType.setSize(1);
          targetType = basicTargetType;
        } else if (targetTypeName.equals("u_char")) {
          BasicType basicTargetType = createBasicType(targetTypeName, false, true, true);
          basicTargetType.setSize(1);
          targetType = basicTargetType;
        } else {
          if (DEBUG) {
            System.err.println("WARNING: missing target type \"" + targetTypeName + "\" for pointer type \"" + typeName + "\"");
          }
          targetType = createBasicType(targetTypeName, false, false, false);
        }
      }
    }
    result = new BasicPointerType(this, typeName, targetType);
    if (pointerSize == UNINITIALIZED_SIZE && !typeName.equals("void*")) {
      // void* must be declared early so that other pointer types can use that to set their size.
      throw new InternalError("void* type hasn't been seen when parsing " + typeName);
    }
    result.setSize(pointerSize);
    addType(result);
    return result;
  }

  private boolean typeNameIsPointerType(String typeName) {
    int i = typeName.length() - 1;
    while (i >= 0 && Character.isWhitespace(typeName.charAt(i))) {
      --i;
    }
    if (i >= 0 && typeName.charAt(i) == '*') {
      return true;
    }
    return false;
  }

    public void createType(String typeName, String superclassName,
                           boolean isOopType, boolean isIntegerType,
                           boolean isUnsigned, long size) {
        // See whether we have a superclass
        BasicType superclass = null;
        if (superclassName != null) {
            // Fetch or create it (FIXME: would get oop types wrong if
            // they had a hierarchy; consider using lookupOrFail)
            superclass = lookupOrCreateClass(superclassName, false, false, false);
        }

        // Lookup or create the current type
        BasicType curType = lookupOrCreateClass(typeName, isOopType, isIntegerType, isUnsigned);
        // Set superclass and/or ensure it's correct
        if (superclass != null) {
            if (curType.getSuperclass() == null) {
                // Set the superclass in the current type
                curType.setSuperclass(superclass);
            }

            if (curType.getSuperclass() != superclass) {
                throw new RuntimeException("Error: the type \"" + typeName + "\" (declared in the remote VM in VMStructs::localHotSpotVMTypes) " +
                                           "had its superclass redefined (old was " + curType.getSuperclass().getName() + ", new is " +
                                           superclass.getName() + ").");
            }
        }

        // Classes are created with a size of UNINITIALIZED_SIZE.
        // Set size if necessary.
        if (curType.getSize() == UNINITIALIZED_SIZE || curType.getSize() == 0) {
            curType.setSize(size);
        } else {
            if (curType.getSize() != size) {
                throw new RuntimeException("Error: the type \"" + typeName + "\" (declared in the remote VM in VMStructs::localHotSpotVMTypes) " +
                                           "had its size redefined (old was " + curType.getSize() + ", new is " + size + ").");
            }

            if (!typeNameIsPointerType(typeName)) {
                System.err.println("Warning: the type \"" + typeName + "\" (declared in the remote VM in VMStructs::localHotSpotVMTypes) " +
                                   "had its size declared as " + size + " twice. Continuing.");
                duplicateDefCount++;
            }
        }

    }

    /** "Virtual constructor" for fields based on type */
    public void createField(BasicType containingType,
                            String name, Type type, boolean isStatic,
                            long offset, Address staticFieldAddress) {
        // Add field to containing type
        containingType.addField(internalCreateField(containingType, name, type, isStatic, offset, staticFieldAddress));
    }

    Field internalCreateField(BasicType containingType,
                              String name, Type type, boolean isStatic,
                              long offset, Address staticFieldAddress) {
    // "Virtual constructor" based on type
    if (type.isOopType()) {
      return new BasicOopField(this, containingType, name, type,
                               isStatic, offset, staticFieldAddress);
    }

    if (type instanceof CIntegerType) {
      return new BasicCIntegerField(this, containingType, name, type,
                                    isStatic, offset, staticFieldAddress);
    }

    if (type.equals(getJBooleanType())) {
      return new BasicJBooleanField(this, containingType, name, type,
                                    isStatic, offset, staticFieldAddress);
    }

    if (type.equals(getJByteType())) {
      return new BasicJByteField(this, containingType, name, type,
                                 isStatic, offset, staticFieldAddress);
    }

    if (type.equals(getJCharType())) {
      return new BasicJCharField(this, containingType, name, type,
                                 isStatic, offset, staticFieldAddress);
    }

    if (type.equals(getJDoubleType())) {
      return new BasicJDoubleField(this, containingType, name, type,
                                   isStatic, offset, staticFieldAddress);
    }

    if (type.equals(getJFloatType())) {
      return new BasicJFloatField(this, containingType, name, type,
                                  isStatic, offset, staticFieldAddress);
    }

    if (type.equals(getJIntType())) {
      return new BasicJIntField(this, containingType, name, type,
                                isStatic, offset, staticFieldAddress);
    }

    if (type.equals(getJLongType())) {
      return new BasicJLongField(this, containingType, name, type,
                                 isStatic, offset, staticFieldAddress);
    }

    if (type.equals(getJShortType())) {
      return new BasicJShortField(this, containingType, name, type,
                                  isStatic, offset, staticFieldAddress);
    }

    // Unknown ("opaque") type. Instantiate ordinary Field.
    return new BasicField(this, containingType, name, type,
                          isStatic, offset, staticFieldAddress);
  }

  // For debugging
  private void dumpMemory(Address addr, int len) {
    int i = 0;
    while (i < len) {
      System.err.print(addr.addOffsetTo(i) + ":");
      for (int j = 0; j < 8 && i < len; i++, j++) {
        String s = Long.toHexString(addr.getCIntegerAt(i, 1, true));
        System.err.print(" 0x");
        for (int k = 0; k < 2 - s.length(); k++) {
          System.err.print("0");
        }
        System.err.print(s);
      }
      System.err.println();
    }
  }
}
