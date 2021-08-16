/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.incubator.vector;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Stable;

import static jdk.internal.vm.vector.VectorSupport.*;
import static jdk.incubator.vector.VectorIntrinsics.*;

/**
 * Local type witness for primitive types int.class, etc.
 * It caches all sorts of goodies that we can't put on java.lang.Class.
 */
enum LaneType {
    FLOAT(float.class, Float.class, float[].class, 'F', 24, Float.SIZE, T_FLOAT),
    DOUBLE(double.class, Double.class, double[].class, 'F', 53, Double.SIZE, T_DOUBLE),
    BYTE(byte.class, Byte.class, byte[].class, 'I', -1, Byte.SIZE, T_BYTE),
    SHORT(short.class, Short.class, short[].class, 'I', -1, Short.SIZE, T_SHORT),
    INT(int.class, Integer.class, int[].class, 'I', -1, Integer.SIZE, T_INT),
    LONG(long.class, Long.class, long[].class, 'I', -1, Long.SIZE, T_LONG);

    LaneType(Class<?> elementType,
             Class<?> genericElementType,
             Class<?> arrayType,
             char elementKind,
             int elementPrecision,
             int elementSize,
             int basicType) {
        if (elementPrecision <= 0)
            elementPrecision += elementSize;
        this.elementType = elementType;
        this.genericElementType = genericElementType;
        this.arrayType = arrayType;
        this.elementKind = elementKind;
        this.elementPrecision = elementPrecision;
        this.elementSize = elementSize;
        this.elementSizeLog2 = Integer.numberOfTrailingZeros(elementSize);
        assert(elementSize == (1 << elementSizeLog2));
        this.switchKey = 1+ordinal();
        this.printName = elementType.getSimpleName();
        // Note: If we ever have non-standard lane sizes, such as
        // int:128 or int:4 or float:16, report the size in the
        // printName.  If we do unsigned or vector or bit lane types,
        // report that condition also.
        this.typeChar = printName.toUpperCase().charAt(0);
        assert("FDBSIL".indexOf(typeChar) == ordinal()) : this;
        // Same as in JVMS, org.objectweb.asm.Opcodes, etc.:
        this.basicType = basicType;
        assert(basicType ==
               ( (elementSizeLog2 - /*lg(Byte.SIZE)*/ 3)
                 | (elementKind == 'F' ? 4 : 8))) : this;
        assert("....zcFDBSILoav..".charAt(basicType) == typeChar);
    }

    final Class<?> elementType;
    final Class<?> arrayType;
    final Class<?> genericElementType;
    final int elementSize;
    final int elementSizeLog2;
    final int elementPrecision;
    final char elementKind; // 'I' or 'F'
    final int switchKey;  // 1+ordinal(), which is non-zero
    final String printName;
    final char typeChar; // one of "BSILFD"
    final int basicType;  // lg(size/8) | (kind=='F'?4:kind=='I'?8)

    private @Stable LaneType asIntegral;
    private @Stable LaneType asFloating;

    @Override
    public String toString() {
        return printName;
    }

    LaneType asIntegral() {
        return asIntegral.check();
    }

    LaneType asFloating() {
        if (asFloating == null) {
            throw badElementType(elementType, "either int or long, to reinterpret as float or double");
        }
        return asFloating;
    }

    /** Decode a class mirror for an element type into an enum. */
    @ForceInline
    static LaneType of(Class<?> elementType) {
        // The following two lines are expected to
        // constant fold in the JIT, if the argument
        // is constant and this method is inlined.
        int c0 = elementType.getName().charAt(0);
        LaneType type = ENUM_FROM_C0[c0 & C0_MASK];
        // This line can short-circuit if a valid
        // elementType constant was passed:
        if (type != null && type.elementType == elementType) {
            return type;
        }
        // Come here if something went wrong.
        return ofSlow(elementType);
    }
    private static LaneType ofSlow(Class<?> elementType) {
        for (LaneType type : ENUM_VALUES) {
            if (type.elementType == elementType) {
                return type;
            }
        }
        throw badElementType(elementType, "a primitive type such as byte.class with a known bit-size");
    }

    /**
     * Finds a LaneType for the given query-type.
     * The element type, box type, and array type are
     * all matched.  May be helpful for error messages.
     */
    /*package-private*/
    static LaneType forClassOrNull(Class<?> queryType) {
        for (LaneType type : ENUM_VALUES) {
            if (type.genericElementType == queryType) {
                return type;
            }
            if (type.elementType == queryType) {
                return type;
            }
            if (type.arrayType == queryType) {
                return type;
            }
        }
        return null;
    }

    LaneType check(Class<?> expectedType) {
        if (expectedType == this.elementType)
            return this;
        throw badElementType(this.elementType, expectedType);
    }

    private static
    RuntimeException badElementType(Class<?> elementType, Object expected) {
        String msg = String.format("Bad vector element type: %s (should be %s)",
                                   elementType, expected);
        if (expected instanceof Class) {
            // Failure mode for check().
            return new ClassCastException(msg);
        } else {
            // Failure mode for VectorSpecies.{of*,withLanes}.
            return new UnsupportedOperationException(msg);
        }
    }

    // Switch keys for local use.
    // We need these because switches keyed on enums
    // don't optimize properly; see JDK-8161245

    static final int
        SK_FLOAT    = 1,
        SK_DOUBLE   = 2,
        SK_BYTE     = 3,
        SK_SHORT    = 4,
        SK_INT      = 5,
        SK_LONG     = 6,
        SK_LIMIT    = 7;

    /*package-private*/
    @ForceInline
    static LaneType ofSwitchKey(int sk) {
        return ENUM_FROM_SK[sk].check();
    }

    /*package-private*/
    @ForceInline
    static LaneType ofBasicType(int bt) {
        return ENUM_FROM_BT[bt].check();
    }

    /*package-private*/
    @ForceInline final LaneType check() { return this; }

    // Constant-foldable tables mapping ordinals, switch keys,
    // and first characters of type names to enum values.

    @Stable private static final LaneType[] ENUM_VALUES;
    @Stable private static final LaneType[] ENUM_FROM_SK;
    @Stable private static final LaneType[] ENUM_FROM_C0;
    @Stable private static final LaneType[] ENUM_FROM_BT;
    private static final int C0_MASK = 0x0F, BT_MASK = 0x0F;
    static {
        LaneType[] values = values().clone();
        LaneType[] valuesByKey = new LaneType[1+values.length];
        LaneType[] valuesByC0  = new LaneType[C0_MASK+1];
        LaneType[] valuesByBT  = new LaneType[BT_MASK+1];
        for (int ord = 0; ord < values.length; ord++) {
            int key = 1+ord;
            LaneType value = values[ord];
            valuesByKey[key] = value;
            try {
                assert(LaneType.class
                       .getDeclaredField("SK_"+value.name())
                       .get(null).equals(key));
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex);
            }
            int c0 = value.elementType.getName().charAt(0);
            c0 &= C0_MASK;
            assert(valuesByC0[c0] == null);
            valuesByC0[c0] = value;
            assert(valuesByBT[value.basicType] == null);
            valuesByBT[value.basicType] = value;
            // set up asIntegral
            if (value.elementKind == 'I') {
                value.asIntegral = value;
            } else {
                for (LaneType v : values) {
                    if (v.elementKind == 'I' &&
                        v.elementSize == value.elementSize) {
                        value.asIntegral = v;
                        break;
                    }
                }
            }
            // set up asFloating
            if (value.elementKind == 'F') {
                value.asFloating = value;
            } else {
                for (LaneType v : values) {
                    if (v.elementKind == 'F' &&
                        v.elementSize == value.elementSize) {
                        value.asFloating = v;
                        break;
                    }
                }
            }
        }
        // Test the asIntegral/asFloating links:
        for (LaneType lt : values) {
            if (lt.elementKind == 'I') {
                assert(lt.asIntegral == lt);
                if (lt.asFloating == null) {
                    assert(lt.elementSize < Float.SIZE);
                } else {
                    assert(lt.asFloating.elementKind == 'F' &&
                           lt.asFloating.elementSize == lt.elementSize);
                }
            } else {
                assert(lt.elementKind == 'F');
                assert(lt.asFloating == lt);
                assert(lt.asIntegral.elementKind == 'I' &&
                       lt.asIntegral.elementSize == lt.elementSize);
            }
        }
        ENUM_VALUES = values;
        ENUM_FROM_SK = valuesByKey;
        ENUM_FROM_C0 = valuesByC0;
        ENUM_FROM_BT = valuesByBT;
    }

    static {
        assert(ofBasicType(T_FLOAT) == FLOAT);
        assert(ofBasicType(T_DOUBLE) == DOUBLE);
        assert(ofBasicType(T_BYTE) == BYTE);
        assert(ofBasicType(T_SHORT) == SHORT);
        assert(ofBasicType(T_INT) == INT);
        assert(ofBasicType(T_LONG) == LONG);
    }
}
