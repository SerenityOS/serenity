/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */
package jdk.vm.ci.meta;

/**
 * Represents a constant (boxed) value, such as an integer, floating point number, or object
 * reference, within the compiler and across the compiler/runtime interface. Exports a set of
 * {@code JavaConstant} instances that represent frequently used constant values, such as
 * {@link #NULL_POINTER}.
 */
public interface JavaConstant extends Constant, JavaValue {
    /*
     * Using a larger cache for integers leads to only a slight increase in cache hit ratio which is
     * not enough to justify the impact on startup time.
     */
    JavaConstant NULL_POINTER = new NullConstant();
    PrimitiveConstant INT_MINUS_1 = new PrimitiveConstant(JavaKind.Int, -1);
    PrimitiveConstant INT_0 = new PrimitiveConstant(JavaKind.Int, 0);
    PrimitiveConstant INT_1 = new PrimitiveConstant(JavaKind.Int, 1);
    PrimitiveConstant INT_2 = new PrimitiveConstant(JavaKind.Int, 2);
    PrimitiveConstant LONG_0 = new PrimitiveConstant(JavaKind.Long, 0L);
    PrimitiveConstant LONG_1 = new PrimitiveConstant(JavaKind.Long, 1L);
    PrimitiveConstant FLOAT_0 = new PrimitiveConstant(JavaKind.Float, Float.floatToRawIntBits(0.0F));
    PrimitiveConstant FLOAT_1 = new PrimitiveConstant(JavaKind.Float, Float.floatToRawIntBits(1.0F));
    PrimitiveConstant DOUBLE_0 = new PrimitiveConstant(JavaKind.Double, Double.doubleToRawLongBits(0.0D));
    PrimitiveConstant DOUBLE_1 = new PrimitiveConstant(JavaKind.Double, Double.doubleToRawLongBits(1.0D));
    PrimitiveConstant TRUE = new PrimitiveConstant(JavaKind.Boolean, 1L);
    PrimitiveConstant FALSE = new PrimitiveConstant(JavaKind.Boolean, 0L);
    PrimitiveConstant ILLEGAL = new PrimitiveConstant(JavaKind.Illegal, 0);

    /**
     * Returns the Java kind of this constant.
     */
    JavaKind getJavaKind();

    /**
     * Checks whether this constant is null.
     *
     * @return {@code true} if this constant is the null constant
     */
    boolean isNull();

    static boolean isNull(Constant c) {
        if (c instanceof JavaConstant) {
            return ((JavaConstant) c).isNull();
        } else {
            return false;
        }
    }

    /**
     * Checks whether this constant is non-null.
     *
     * @return {@code true} if this constant is a primitive, or an object constant that is not null
     */
    default boolean isNonNull() {
        return !isNull();
    }

    /**
     * Checks whether this constant is the default value for its kind (null, 0, 0.0, false).
     *
     * @return {@code true} if this constant is the default value for its kind
     */
    @Override
    boolean isDefaultForKind();

    /**
     * Returns the value of this constant as a boxed Java value.
     *
     * @return the value of this constant
     */
    Object asBoxedPrimitive();

    /**
     * Returns the primitive int value this constant represents. The constant must have a
     * {@link JavaKind#getStackKind()} of {@link JavaKind#Int}.
     *
     * @return the constant value
     */
    int asInt();

    /**
     * Returns the primitive boolean value this constant represents. The constant must have kind
     * {@link JavaKind#Boolean}.
     *
     * @return the constant value
     */
    boolean asBoolean();

    /**
     * Returns the primitive long value this constant represents. The constant must have kind
     * {@link JavaKind#Long}, a {@link JavaKind#getStackKind()} of {@link JavaKind#Int}.
     *
     * @return the constant value
     */
    long asLong();

    /**
     * Returns the primitive float value this constant represents. The constant must have kind
     * {@link JavaKind#Float}.
     *
     * @return the constant value
     */
    float asFloat();

    /**
     * Returns the primitive double value this constant represents. The constant must have kind
     * {@link JavaKind#Double}.
     *
     * @return the constant value
     */
    double asDouble();

    @Override
    default String toValueString() {
        if (getJavaKind() == JavaKind.Illegal) {
            return "illegal";
        } else {
            return getJavaKind().format(asBoxedPrimitive());
        }
    }

    static String toString(JavaConstant constant) {
        if (constant.getJavaKind() == JavaKind.Illegal) {
            return "illegal";
        } else {
            return constant.getJavaKind().getJavaName() + "[" + constant.toValueString() + "]";
        }
    }

    /**
     * Creates a boxed double constant.
     *
     * @param d the double value to box
     * @return a boxed copy of {@code value}
     */
    static PrimitiveConstant forDouble(double d) {
        if (Double.compare(0.0D, d) == 0) {
            return DOUBLE_0;
        }
        if (Double.compare(d, 1.0D) == 0) {
            return DOUBLE_1;
        }
        return new PrimitiveConstant(JavaKind.Double, Double.doubleToRawLongBits(d));
    }

    /**
     * Creates a boxed float constant.
     *
     * @param f the float value to box
     * @return a boxed copy of {@code value}
     */
    static PrimitiveConstant forFloat(float f) {
        if (Float.compare(f, 0.0F) == 0) {
            return FLOAT_0;
        }
        if (Float.compare(f, 1.0F) == 0) {
            return FLOAT_1;
        }
        return new PrimitiveConstant(JavaKind.Float, Float.floatToRawIntBits(f));
    }

    /**
     * Creates a boxed long constant.
     *
     * @param i the long value to box
     * @return a boxed copy of {@code value}
     */
    static PrimitiveConstant forLong(long i) {
        if (i == 0) {
            return LONG_0;
        } else if (i == 1) {
            return LONG_1;
        } else {
            return new PrimitiveConstant(JavaKind.Long, i);
        }
    }

    /**
     * Creates a boxed integer constant.
     *
     * @param i the integer value to box
     * @return a boxed copy of {@code value}
     */
    static PrimitiveConstant forInt(int i) {
        switch (i) {
            case -1:
                return INT_MINUS_1;
            case 0:
                return INT_0;
            case 1:
                return INT_1;
            case 2:
                return INT_2;
            default:
                return new PrimitiveConstant(JavaKind.Int, i);
        }
    }

    /**
     * Creates a boxed byte constant.
     *
     * @param i the byte value to box
     * @return a boxed copy of {@code value}
     */
    static PrimitiveConstant forByte(byte i) {
        return new PrimitiveConstant(JavaKind.Byte, i);
    }

    /**
     * Creates a boxed boolean constant.
     *
     * @param i the boolean value to box
     * @return a boxed copy of {@code value}
     */
    static PrimitiveConstant forBoolean(boolean i) {
        return i ? TRUE : FALSE;
    }

    /**
     * Creates a boxed char constant.
     *
     * @param i the char value to box
     * @return a boxed copy of {@code value}
     */
    static PrimitiveConstant forChar(char i) {
        return new PrimitiveConstant(JavaKind.Char, i);
    }

    /**
     * Creates a boxed short constant.
     *
     * @param i the short value to box
     * @return a boxed copy of {@code value}
     */
    static PrimitiveConstant forShort(short i) {
        return new PrimitiveConstant(JavaKind.Short, i);
    }

    /**
     * Creates a {@link JavaConstant} from a primitive integer of a certain kind.
     */
    static PrimitiveConstant forIntegerKind(JavaKind kind, long i) {
        switch (kind) {
            case Boolean:
                return forBoolean(i != 0);
            case Byte:
                return forByte((byte) i);
            case Short:
                return forShort((short) i);
            case Char:
                return forChar((char) i);
            case Int:
                return forInt((int) i);
            case Long:
                return forLong(i);
            default:
                throw new IllegalArgumentException("not an integer kind: " + kind);
        }
    }

    /**
     * Creates a {@link JavaConstant} from a primitive integer of a certain width.
     */
    static PrimitiveConstant forPrimitiveInt(int bits, long i) {
        assert bits <= 64;
        switch (bits) {
            case 1:
                return forBoolean(i != 0);
            case 8:
                return forByte((byte) i);
            case 16:
                return forShort((short) i);
            case 32:
                return forInt((int) i);
            case 64:
                return forLong(i);
            default:
                throw new IllegalArgumentException("unsupported integer width: " + bits);
        }
    }

    static PrimitiveConstant forPrimitive(JavaKind kind, long rawValue) {
        switch (kind) {
            case Boolean:
                return JavaConstant.forBoolean(rawValue != 0);
            case Byte:
                return JavaConstant.forByte((byte) rawValue);
            case Char:
                return JavaConstant.forChar((char) rawValue);
            case Short:
                return JavaConstant.forShort((short) rawValue);
            case Int:
                return JavaConstant.forInt((int) rawValue);
            case Long:
                return JavaConstant.forLong(rawValue);
            case Float:
                return JavaConstant.forFloat(Float.intBitsToFloat((int) rawValue));
            case Double:
                return JavaConstant.forDouble(Double.longBitsToDouble(rawValue));
            default:
                throw new IllegalArgumentException("Unsupported kind: " + kind);
        }
    }

    /**
     * Creates a boxed constant for the given boxed primitive value.
     *
     * @param value the Java boxed value
     * @return the primitive constant holding the {@code value}
     */
    static PrimitiveConstant forBoxedPrimitive(Object value) {
        if (value instanceof Boolean) {
            return forBoolean((Boolean) value);
        } else if (value instanceof Byte) {
            return forByte((Byte) value);
        } else if (value instanceof Character) {
            return forChar((Character) value);
        } else if (value instanceof Short) {
            return forShort((Short) value);
        } else if (value instanceof Integer) {
            return forInt((Integer) value);
        } else if (value instanceof Long) {
            return forLong((Long) value);
        } else if (value instanceof Float) {
            return forFloat((Float) value);
        } else if (value instanceof Double) {
            return forDouble((Double) value);
        } else {
            return null;
        }
    }

    static PrimitiveConstant forIllegal() {
        return JavaConstant.ILLEGAL;
    }

    /**
     * Returns a constant with the default value for the given kind.
     */
    static JavaConstant defaultForKind(JavaKind kind) {
        switch (kind) {
            case Boolean:
                return FALSE;
            case Byte:
                return forByte((byte) 0);
            case Char:
                return forChar((char) 0);
            case Short:
                return forShort((short) 0);
            case Int:
                return INT_0;
            case Double:
                return DOUBLE_0;
            case Float:
                return FLOAT_0;
            case Long:
                return LONG_0;
            case Object:
                return NULL_POINTER;
            default:
                throw new IllegalArgumentException(kind.toString());
        }
    }
}
