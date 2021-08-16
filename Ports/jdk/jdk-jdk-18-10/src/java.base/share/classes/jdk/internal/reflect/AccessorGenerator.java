/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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
import jdk.internal.misc.Unsafe;

/** Shared functionality for all accessor generators */

class AccessorGenerator implements ClassFileConstants {
    static final Unsafe unsafe = Unsafe.getUnsafe();

    // Constants because there's no way to say "short integer constant",
    // i.e., "1S"
    protected static final short S0 = (short) 0;
    protected static final short S1 = (short) 1;
    protected static final short S2 = (short) 2;
    protected static final short S3 = (short) 3;
    protected static final short S4 = (short) 4;
    protected static final short S5 = (short) 5;
    protected static final short S6 = (short) 6;

    // Instance variables for shared functionality between
    // FieldAccessorGenerator and MethodAccessorGenerator
    protected ClassFileAssembler asm;
    protected int   modifiers;
    protected short thisClass;
    protected short superClass;
    protected short targetClass;
    // Common constant pool entries to FieldAccessor and MethodAccessor
    protected short throwableClass;
    protected short classCastClass;
    protected short nullPointerClass;
    protected short illegalArgumentClass;
    protected short invocationTargetClass;
    protected short initIdx;
    protected short initNameAndTypeIdx;
    protected short initStringNameAndTypeIdx;
    protected short nullPointerCtorIdx;
    protected short illegalArgumentCtorIdx;
    protected short illegalArgumentStringCtorIdx;
    protected short invocationTargetCtorIdx;
    protected short superCtorIdx;
    protected short objectClass;
    protected short toStringIdx;
    protected short codeIdx;
    protected short exceptionsIdx;
    // Boxing
    protected short valueOfIdx;
    protected short booleanIdx;
    protected short booleanBoxIdx;
    protected short booleanUnboxIdx;
    protected short byteIdx;
    protected short byteBoxIdx;
    protected short byteUnboxIdx;
    protected short characterIdx;
    protected short characterBoxIdx;
    protected short characterUnboxIdx;
    protected short doubleIdx;
    protected short doubleBoxIdx;
    protected short doubleUnboxIdx;
    protected short floatIdx;
    protected short floatBoxIdx;
    protected short floatUnboxIdx;
    protected short integerIdx;
    protected short integerBoxIdx;
    protected short integerUnboxIdx;
    protected short longIdx;
    protected short longBoxIdx;
    protected short longUnboxIdx;
    protected short shortIdx;
    protected short shortBoxIdx;
    protected short shortUnboxIdx;

    protected final short NUM_COMMON_CPOOL_ENTRIES = (short) 30;
    protected final short NUM_BOXING_CPOOL_ENTRIES = (short) 73;

    // Requires that superClass has been set up
    protected void emitCommonConstantPoolEntries() {
        // +   [UTF-8] "java/lang/Throwable"
        // +   [CONSTANT_Class_info] for above
        // +   [UTF-8] "java/lang/ClassCastException"
        // +   [CONSTANT_Class_info] for above
        // +   [UTF-8] "java/lang/NullPointerException"
        // +   [CONSTANT_Class_info] for above
        // +   [UTF-8] "java/lang/IllegalArgumentException"
        // +   [CONSTANT_Class_info] for above
        // +   [UTF-8] "java/lang/InvocationTargetException"
        // +   [CONSTANT_Class_info] for above
        // +   [UTF-8] "<init>"
        // +   [UTF-8] "()V"
        // +   [CONSTANT_NameAndType_info] for above
        // +   [CONSTANT_Methodref_info] for NullPointerException's constructor
        // +   [CONSTANT_Methodref_info] for IllegalArgumentException's constructor
        // +   [UTF-8] "(Ljava/lang/String;)V"
        // +   [CONSTANT_NameAndType_info] for "<init>(Ljava/lang/String;)V"
        // +   [CONSTANT_Methodref_info] for IllegalArgumentException's constructor taking a String
        // +   [UTF-8] "(Ljava/lang/Throwable;)V"
        // +   [CONSTANT_NameAndType_info] for "<init>(Ljava/lang/Throwable;)V"
        // +   [CONSTANT_Methodref_info] for InvocationTargetException's constructor
        // +   [CONSTANT_Methodref_info] for "super()"
        // +   [UTF-8] "java/lang/Object"
        // +   [CONSTANT_Class_info] for above
        // +   [UTF-8] "toString"
        // +   [UTF-8] "()Ljava/lang/String;"
        // +   [CONSTANT_NameAndType_info] for "toString()Ljava/lang/String;"
        // +   [CONSTANT_Methodref_info] for Object's toString method
        // +   [UTF-8] "Code"
        // +   [UTF-8] "Exceptions"
        asm.emitConstantPoolUTF8("java/lang/Throwable");
        asm.emitConstantPoolClass(asm.cpi());
        throwableClass = asm.cpi();
        asm.emitConstantPoolUTF8("java/lang/ClassCastException");
        asm.emitConstantPoolClass(asm.cpi());
        classCastClass = asm.cpi();
        asm.emitConstantPoolUTF8("java/lang/NullPointerException");
        asm.emitConstantPoolClass(asm.cpi());
        nullPointerClass = asm.cpi();
        asm.emitConstantPoolUTF8("java/lang/IllegalArgumentException");
        asm.emitConstantPoolClass(asm.cpi());
        illegalArgumentClass = asm.cpi();
        asm.emitConstantPoolUTF8("java/lang/reflect/InvocationTargetException");
        asm.emitConstantPoolClass(asm.cpi());
        invocationTargetClass = asm.cpi();
        asm.emitConstantPoolUTF8("<init>");
        initIdx = asm.cpi();
        asm.emitConstantPoolUTF8("()V");
        asm.emitConstantPoolNameAndType(initIdx, asm.cpi());
        initNameAndTypeIdx = asm.cpi();
        asm.emitConstantPoolMethodref(nullPointerClass, initNameAndTypeIdx);
        nullPointerCtorIdx = asm.cpi();
        asm.emitConstantPoolMethodref(illegalArgumentClass, initNameAndTypeIdx);
        illegalArgumentCtorIdx = asm.cpi();
        asm.emitConstantPoolUTF8("(Ljava/lang/String;)V");
        asm.emitConstantPoolNameAndType(initIdx, asm.cpi());
        initStringNameAndTypeIdx = asm.cpi();
        asm.emitConstantPoolMethodref(illegalArgumentClass, initStringNameAndTypeIdx);
        illegalArgumentStringCtorIdx = asm.cpi();
        asm.emitConstantPoolUTF8("(Ljava/lang/Throwable;)V");
        asm.emitConstantPoolNameAndType(initIdx, asm.cpi());
        asm.emitConstantPoolMethodref(invocationTargetClass, asm.cpi());
        invocationTargetCtorIdx = asm.cpi();
        asm.emitConstantPoolMethodref(superClass, initNameAndTypeIdx);
        superCtorIdx = asm.cpi();
        asm.emitConstantPoolUTF8("java/lang/Object");
        asm.emitConstantPoolClass(asm.cpi());
        objectClass = asm.cpi();
        asm.emitConstantPoolUTF8("toString");
        asm.emitConstantPoolUTF8("()Ljava/lang/String;");
        asm.emitConstantPoolNameAndType(sub(asm.cpi(), S1), asm.cpi());
        asm.emitConstantPoolMethodref(objectClass, asm.cpi());
        toStringIdx = asm.cpi();
        asm.emitConstantPoolUTF8("Code");
        codeIdx = asm.cpi();
        asm.emitConstantPoolUTF8("Exceptions");
        exceptionsIdx = asm.cpi();
    }

    /** Constant pool entries required to be able to box/unbox primitive
        types. Note that we don't emit these if we don't need them. */
    protected void emitBoxingContantPoolEntries() {
        //  *  [UTF-8] "valueOf"
        //  *  [UTF-8] "java/lang/Boolean"
        //  *  [CONSTANT_Class_info] for above
        //  *  [UTF-8] "(Z)Ljava/lang/Boolean;"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "booleanValue"
        //  *  [UTF-8] "()Z"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "java/lang/Byte"
        //  *  [CONSTANT_Class_info] for above
        //  *  [UTF-8] "(B)Ljava/lang/Byte;"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "byteValue"
        //  *  [UTF-8] "()B"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "java/lang/Character"
        //  *  [CONSTANT_Class_info] for above
        //  *  [UTF-8] "(C)Ljava/lang/Character;"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "charValue"
        //  *  [UTF-8] "()C"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "java/lang/Double"
        //  *  [CONSTANT_Class_info] for above
        //  *  [UTF-8] "(D)Ljava/lang/Double;"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "doubleValue"
        //  *  [UTF-8] "()D"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "java/lang/Float"
        //  *  [CONSTANT_Class_info] for above
        //  *  [UTF-8] "(F)Ljava/lang/Float;"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "floatValue"
        //  *  [UTF-8] "()F"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "java/lang/Integer"
        //  *  [CONSTANT_Class_info] for above
        //  *  [UTF-8] "(I)Ljava/lang/Integer;"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "intValue"
        //  *  [UTF-8] "()I"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "java/lang/Long"
        //  *  [CONSTANT_Class_info] for above
        //  *  [UTF-8] "(J)Ljava/lang/Long;"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "longValue"
        //  *  [UTF-8] "()J"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "java/lang/Short"
        //  *  [CONSTANT_Class_info] for above
        //  *  [UTF-8] "(S)Ljava/lang/Short;"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above
        //  *  [UTF-8] "shortValue"
        //  *  [UTF-8] "()S"
        //  *  [CONSTANT_NameAndType_info] for above
        //  *  [CONSTANT_Methodref_info] for above

        // valueOf-method name
        asm.emitConstantPoolUTF8("valueOf");
        valueOfIdx = asm.cpi();

        // Boolean
        asm.emitConstantPoolUTF8("java/lang/Boolean");
        asm.emitConstantPoolClass(asm.cpi());
        booleanIdx = asm.cpi();
        asm.emitConstantPoolUTF8("(Z)Ljava/lang/Boolean;");
        asm.emitConstantPoolNameAndType(valueOfIdx, asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S2), asm.cpi());
        booleanBoxIdx = asm.cpi();
        asm.emitConstantPoolUTF8("booleanValue");
        asm.emitConstantPoolUTF8("()Z");
        asm.emitConstantPoolNameAndType(sub(asm.cpi(), S1), asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S6), asm.cpi());
        booleanUnboxIdx = asm.cpi();

        // Byte
        asm.emitConstantPoolUTF8("java/lang/Byte");
        asm.emitConstantPoolClass(asm.cpi());
        byteIdx = asm.cpi();
        asm.emitConstantPoolUTF8("(B)Ljava/lang/Byte;");
        asm.emitConstantPoolNameAndType(valueOfIdx, asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S2), asm.cpi());
        byteBoxIdx = asm.cpi();
        asm.emitConstantPoolUTF8("byteValue");
        asm.emitConstantPoolUTF8("()B");
        asm.emitConstantPoolNameAndType(sub(asm.cpi(), S1), asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S6), asm.cpi());
        byteUnboxIdx = asm.cpi();

        // Character
        asm.emitConstantPoolUTF8("java/lang/Character");
        asm.emitConstantPoolClass(asm.cpi());
        characterIdx = asm.cpi();
        asm.emitConstantPoolUTF8("(C)Ljava/lang/Character;");
        asm.emitConstantPoolNameAndType(valueOfIdx, asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S2), asm.cpi());
        characterBoxIdx = asm.cpi();
        asm.emitConstantPoolUTF8("charValue");
        asm.emitConstantPoolUTF8("()C");
        asm.emitConstantPoolNameAndType(sub(asm.cpi(), S1), asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S6), asm.cpi());
        characterUnboxIdx = asm.cpi();

        // Double
        asm.emitConstantPoolUTF8("java/lang/Double");
        asm.emitConstantPoolClass(asm.cpi());
        doubleIdx = asm.cpi();
        asm.emitConstantPoolUTF8("(D)Ljava/lang/Double;");
        asm.emitConstantPoolNameAndType(valueOfIdx, asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S2), asm.cpi());
        doubleBoxIdx = asm.cpi();
        asm.emitConstantPoolUTF8("doubleValue");
        asm.emitConstantPoolUTF8("()D");
        asm.emitConstantPoolNameAndType(sub(asm.cpi(), S1), asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S6), asm.cpi());
        doubleUnboxIdx = asm.cpi();

        // Float
        asm.emitConstantPoolUTF8("java/lang/Float");
        asm.emitConstantPoolClass(asm.cpi());
        floatIdx = asm.cpi();
        asm.emitConstantPoolUTF8("(F)Ljava/lang/Float;");
        asm.emitConstantPoolNameAndType(valueOfIdx, asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S2), asm.cpi());
        floatBoxIdx = asm.cpi();
        asm.emitConstantPoolUTF8("floatValue");
        asm.emitConstantPoolUTF8("()F");
        asm.emitConstantPoolNameAndType(sub(asm.cpi(), S1), asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S6), asm.cpi());
        floatUnboxIdx = asm.cpi();

        // Integer
        asm.emitConstantPoolUTF8("java/lang/Integer");
        asm.emitConstantPoolClass(asm.cpi());
        integerIdx = asm.cpi();
        asm.emitConstantPoolUTF8("(I)Ljava/lang/Integer;");
        asm.emitConstantPoolNameAndType(valueOfIdx, asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S2), asm.cpi());
        integerBoxIdx = asm.cpi();
        asm.emitConstantPoolUTF8("intValue");
        asm.emitConstantPoolUTF8("()I");
        asm.emitConstantPoolNameAndType(sub(asm.cpi(), S1), asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S6), asm.cpi());
        integerUnboxIdx = asm.cpi();

        // Long
        asm.emitConstantPoolUTF8("java/lang/Long");
        asm.emitConstantPoolClass(asm.cpi());
        longIdx = asm.cpi();
        asm.emitConstantPoolUTF8("(J)Ljava/lang/Long;");
        asm.emitConstantPoolNameAndType(valueOfIdx, asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S2), asm.cpi());
        longBoxIdx = asm.cpi();
        asm.emitConstantPoolUTF8("longValue");
        asm.emitConstantPoolUTF8("()J");
        asm.emitConstantPoolNameAndType(sub(asm.cpi(), S1), asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S6), asm.cpi());
        longUnboxIdx = asm.cpi();

        // Short
        asm.emitConstantPoolUTF8("java/lang/Short");
        asm.emitConstantPoolClass(asm.cpi());
        shortIdx = asm.cpi();
        asm.emitConstantPoolUTF8("(S)Ljava/lang/Short;");
        asm.emitConstantPoolNameAndType(valueOfIdx, asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S2), asm.cpi());
        shortBoxIdx = asm.cpi();
        asm.emitConstantPoolUTF8("shortValue");
        asm.emitConstantPoolUTF8("()S");
        asm.emitConstantPoolNameAndType(sub(asm.cpi(), S1), asm.cpi());
        asm.emitConstantPoolMethodref(sub(asm.cpi(), S6), asm.cpi());
        shortUnboxIdx = asm.cpi();
    }

    // Necessary because of Java's annoying promotion rules
    protected static short add(short s1, short s2) {
        return (short) (s1 + s2);
    }

    protected static short sub(short s1, short s2) {
        return (short) (s1 - s2);
    }

    protected boolean isStatic() {
        return Modifier.isStatic(modifiers);
    }

    protected boolean isPrivate() {
        return Modifier.isPrivate(modifiers);
    }

    /** Returns class name in "internal" form (i.e., '/' separators
        instead of '.') */
    protected static String getClassName
        (Class<?> c, boolean addPrefixAndSuffixForNonPrimitiveTypes)
    {
        if (c.isPrimitive()) {
            if (c == Boolean.TYPE) {
                return "Z";
            } else if (c == Byte.TYPE) {
                return "B";
            } else if (c == Character.TYPE) {
                return "C";
            } else if (c == Double.TYPE) {
                return "D";
            } else if (c == Float.TYPE) {
                return "F";
            } else if (c == Integer.TYPE) {
                return "I";
            } else if (c == Long.TYPE) {
                return "J";
            } else if (c == Short.TYPE) {
                return "S";
            } else if (c == Void.TYPE) {
                return "V";
            }
            throw new InternalError("Should have found primitive type");
        } else if (c.isArray()) {
            return "[" + getClassName(c.getComponentType(), true);
        } else {
            if (addPrefixAndSuffixForNonPrimitiveTypes) {
                return internalize("L" + c.getName() + ";");
            } else {
                return internalize(c.getName());
            }
        }
    }

    private static String internalize(String className) {
        return className.replace('.', '/');
    }

    protected void emitConstructor() {
        // Generate code into fresh code buffer
        ClassFileAssembler cb = new ClassFileAssembler();
        // 0 incoming arguments
        cb.setMaxLocals(1);
        cb.opc_aload_0();
        cb.opc_invokespecial(superCtorIdx, 0, 0);
        cb.opc_return();

        // Emit method
        emitMethod(initIdx, cb.getMaxLocals(), cb, null, null);
    }

    // The descriptor's index in the constant pool must be (1 +
    // nameIdx). "numArgs" must indicate ALL arguments, including the
    // implicit "this" argument; double and long arguments each count
    // as 2 in this count. The code buffer must NOT contain the code
    // length. The exception table may be null, but if non-null must
    // NOT contain the exception table's length. The checked exception
    // indices may be null.
    protected void emitMethod(short nameIdx,
                              int numArgs,
                              ClassFileAssembler code,
                              ClassFileAssembler exceptionTable,
                              short[] checkedExceptionIndices)
    {
        int codeLen = code.getLength();
        int excLen  = 0;
        if (exceptionTable != null) {
            excLen = exceptionTable.getLength();
            if ((excLen % 8) != 0) {
                throw new IllegalArgumentException("Illegal exception table");
            }
        }
        int attrLen = 12 + codeLen + excLen;
        excLen = excLen / 8; // No-op if no exception table

        asm.emitShort(ACC_PUBLIC);
        asm.emitShort(nameIdx);
        asm.emitShort(add(nameIdx, S1));
        if (checkedExceptionIndices == null) {
            // Code attribute only
            asm.emitShort(S1);
        } else {
            // Code and Exceptions attributes
            asm.emitShort(S2);
        }
        // Code attribute
        asm.emitShort(codeIdx);
        asm.emitInt(attrLen);
        asm.emitShort(code.getMaxStack());
        asm.emitShort((short) Math.max(numArgs, code.getMaxLocals()));
        asm.emitInt(codeLen);
        asm.append(code);
        asm.emitShort((short) excLen);
        if (exceptionTable != null) {
            asm.append(exceptionTable);
        }
        asm.emitShort(S0); // No additional attributes for Code attribute
        if (checkedExceptionIndices != null) {
            // Exceptions attribute
            asm.emitShort(exceptionsIdx);
            asm.emitInt(2 + 2 * checkedExceptionIndices.length);
            asm.emitShort((short) checkedExceptionIndices.length);
            for (int i = 0; i < checkedExceptionIndices.length; i++) {
                asm.emitShort(checkedExceptionIndices[i]);
            }
        }
    }

    protected short indexForPrimitiveType(Class<?> type) {
        if (type == Boolean.TYPE) {
            return booleanIdx;
        } else if (type == Byte.TYPE) {
            return byteIdx;
        } else if (type == Character.TYPE) {
            return characterIdx;
        } else if (type == Double.TYPE) {
            return doubleIdx;
        } else if (type == Float.TYPE) {
            return floatIdx;
        } else if (type == Integer.TYPE) {
            return integerIdx;
        } else if (type == Long.TYPE) {
            return longIdx;
        } else if (type == Short.TYPE) {
            return shortIdx;
        }
        throw new InternalError("Should have found primitive type");
    }

    protected short boxingMethodForPrimitiveType(Class<?> type) {
        if (type == Boolean.TYPE) {
            return booleanBoxIdx;
        } else if (type == Byte.TYPE) {
            return byteBoxIdx;
        } else if (type == Character.TYPE) {
            return characterBoxIdx;
        } else if (type == Double.TYPE) {
            return doubleBoxIdx;
        } else if (type == Float.TYPE) {
            return floatBoxIdx;
        } else if (type == Integer.TYPE) {
            return integerBoxIdx;
        } else if (type == Long.TYPE) {
            return longBoxIdx;
        } else if (type == Short.TYPE) {
            return shortBoxIdx;
        }
        throw new InternalError("Should have found primitive type");
    }

    /** Returns true for widening or identity conversions for primitive
        types only */
    protected static boolean canWidenTo(Class<?> type, Class<?> otherType) {
        if (!type.isPrimitive()) {
            return false;
        }

        // Widening conversions (from JVM spec):
        //  byte to short, int, long, float, or double
        //  short to int, long, float, or double
        //  char to int, long, float, or double
        //  int to long, float, or double
        //  long to float or double
        //  float to double

        if (type == Boolean.TYPE) {
            if (otherType == Boolean.TYPE) {
                return true;
            }
        } else if (type == Byte.TYPE) {
            if (   otherType == Byte.TYPE
                   || otherType == Short.TYPE
                   || otherType == Integer.TYPE
                   || otherType == Long.TYPE
                   || otherType == Float.TYPE
                   || otherType == Double.TYPE) {
                return true;
            }
        } else if (type == Short.TYPE) {
            if (   otherType == Short.TYPE
                   || otherType == Integer.TYPE
                   || otherType == Long.TYPE
                   || otherType == Float.TYPE
                   || otherType == Double.TYPE) {
                return true;
            }
        } else if (type == Character.TYPE) {
            if (   otherType == Character.TYPE
                   || otherType == Integer.TYPE
                   || otherType == Long.TYPE
                   || otherType == Float.TYPE
                   || otherType == Double.TYPE) {
                return true;
            }
        } else if (type == Integer.TYPE) {
            if (   otherType == Integer.TYPE
                   || otherType == Long.TYPE
                   || otherType == Float.TYPE
                   || otherType == Double.TYPE) {
                return true;
            }
        } else if (type == Long.TYPE) {
            if (   otherType == Long.TYPE
                   || otherType == Float.TYPE
                   || otherType == Double.TYPE) {
                return true;
            }
        } else if (type == Float.TYPE) {
            if (   otherType == Float.TYPE
                   || otherType == Double.TYPE) {
                return true;
            }
        } else if (type == Double.TYPE) {
            if (otherType == Double.TYPE) {
                return true;
            }
        }

        return false;
    }

    /** Emits the widening bytecode for the given primitive conversion
        (or none if the identity conversion). Requires that a primitive
        conversion exists; i.e., canWidenTo must have already been
        called and returned true. */
    protected static void emitWideningBytecodeForPrimitiveConversion
        (ClassFileAssembler cb,
         Class<?> fromType,
         Class<?> toType)
    {
        // Note that widening conversions for integral types (i.e., "b2s",
        // "s2i") are no-ops since values on the Java stack are
        // sign-extended.

        // Widening conversions (from JVM spec):
        //  byte to short, int, long, float, or double
        //  short to int, long, float, or double
        //  char to int, long, float, or double
        //  int to long, float, or double
        //  long to float or double
        //  float to double

        if (   fromType == Byte.TYPE
               || fromType == Short.TYPE
               || fromType == Character.TYPE
               || fromType == Integer.TYPE) {
            if (toType == Long.TYPE) {
                cb.opc_i2l();
            } else if (toType == Float.TYPE) {
                cb.opc_i2f();
            } else if (toType == Double.TYPE) {
                cb.opc_i2d();
            }
        } else if (fromType == Long.TYPE) {
            if (toType == Float.TYPE) {
                cb.opc_l2f();
            } else if (toType == Double.TYPE) {
                cb.opc_l2d();
            }
        } else if (fromType == Float.TYPE) {
            if (toType == Double.TYPE) {
                cb.opc_f2d();
            }
        }

        // Otherwise, was identity or no-op conversion. Fall through.
    }

    protected short unboxingMethodForPrimitiveType(Class<?> primType) {
        if (primType == Boolean.TYPE) {
            return booleanUnboxIdx;
        } else if (primType == Byte.TYPE) {
            return byteUnboxIdx;
        } else if (primType == Character.TYPE) {
            return characterUnboxIdx;
        } else if (primType == Short.TYPE) {
            return shortUnboxIdx;
        } else if (primType == Integer.TYPE) {
            return integerUnboxIdx;
        } else if (primType == Long.TYPE) {
            return longUnboxIdx;
        } else if (primType == Float.TYPE) {
            return floatUnboxIdx;
        } else if (primType == Double.TYPE) {
            return doubleUnboxIdx;
        }
        throw new InternalError("Illegal primitive type " + primType.getName());
    }

    protected static final Class<?>[] primitiveTypes = new Class<?>[] {
        Boolean.TYPE,
        Byte.TYPE,
        Character.TYPE,
        Short.TYPE,
        Integer.TYPE,
        Long.TYPE,
        Float.TYPE,
        Double.TYPE
    };

    /** We don't consider "Void" to be a primitive type */
    protected static boolean isPrimitive(Class<?> c) {
        return (c.isPrimitive() && c != Void.TYPE);
    }

    protected int typeSizeInStackSlots(Class<?> c) {
        if (c == Void.TYPE) {
            return 0;
        }
        if (c == Long.TYPE || c == Double.TYPE) {
            return 2;
        }
        return 1;
    }

    private ClassFileAssembler illegalArgumentCodeBuffer;
    protected ClassFileAssembler illegalArgumentCodeBuffer() {
        if (illegalArgumentCodeBuffer == null) {
            illegalArgumentCodeBuffer = new ClassFileAssembler();
            illegalArgumentCodeBuffer.opc_new(illegalArgumentClass);
            illegalArgumentCodeBuffer.opc_dup();
            illegalArgumentCodeBuffer.opc_invokespecial(illegalArgumentCtorIdx, 0, 0);
            illegalArgumentCodeBuffer.opc_athrow();
        }

        return illegalArgumentCodeBuffer;
    }
}
