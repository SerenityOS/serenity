/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;

/**
 * Represents a primitive constant value, such as an integer or floating point number, within the
 * compiler and across the compiler/runtime interface.
 */
public class PrimitiveConstant implements JavaConstant, SerializableConstant {

    private final JavaKind kind;

    /**
     * The boxed primitive value as a {@code long}. For {@code float} and {@code double} values,
     * this value is the result of {@link Float#floatToRawIntBits(float)} and
     * {@link Double#doubleToRawLongBits(double)} respectively.
     */
    private final long primitive;

    protected PrimitiveConstant(JavaKind kind, long primitive) {
        this.primitive = primitive;
        this.kind = kind;

        assert kind.isPrimitive() || kind == JavaKind.Illegal;
    }

    static PrimitiveConstant forTypeChar(char kind, long i) {
        return JavaConstant.forIntegerKind(JavaKind.fromPrimitiveOrVoidTypeChar(kind), i);
    }

    @Override
    public JavaKind getJavaKind() {
        return kind;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isDefaultForKind() {
        return primitive == 0;
    }

    @Override
    public boolean asBoolean() {
        assert getJavaKind() == JavaKind.Boolean;
        return primitive != 0L;
    }

    @Override
    public int asInt() {
        assert getJavaKind().getStackKind() == JavaKind.Int : getJavaKind().getStackKind();
        return (int) primitive;
    }

    @Override
    public long asLong() {
        assert getJavaKind().isNumericInteger();
        return primitive;
    }

    @Override
    public float asFloat() {
        assert getJavaKind() == JavaKind.Float;
        return Float.intBitsToFloat((int) primitive);
    }

    @Override
    public double asDouble() {
        assert getJavaKind() == JavaKind.Double;
        return Double.longBitsToDouble(primitive);
    }

    @Override
    public Object asBoxedPrimitive() {
        switch (getJavaKind()) {
            case Byte:
                return Byte.valueOf((byte) primitive);
            case Boolean:
                return Boolean.valueOf(asBoolean());
            case Short:
                return Short.valueOf((short) primitive);
            case Char:
                return Character.valueOf((char) primitive);
            case Int:
                return Integer.valueOf(asInt());
            case Long:
                return Long.valueOf(asLong());
            case Float:
                return Float.valueOf(asFloat());
            case Double:
                return Double.valueOf(asDouble());
            default:
                throw new IllegalArgumentException("unexpected kind " + getJavaKind());
        }
    }

    @Override
    public int getSerializedSize() {
        return getJavaKind().getByteCount();
    }

    @Override
    public void serialize(ByteBuffer buffer) {
        switch (getJavaKind()) {
            case Byte:
            case Boolean:
                buffer.put((byte) primitive);
                break;
            case Short:
                buffer.putShort((short) primitive);
                break;
            case Char:
                buffer.putChar((char) primitive);
                break;
            case Int:
                buffer.putInt(asInt());
                break;
            case Long:
                buffer.putLong(asLong());
                break;
            case Float:
                buffer.putFloat(asFloat());
                break;
            case Double:
                buffer.putDouble(asDouble());
                break;
            default:
                throw new IllegalArgumentException("unexpected kind " + getJavaKind());
        }
    }

    @Override
    public int hashCode() {
        return (int) (primitive ^ (primitive >>> 32)) * (getJavaKind().ordinal() + 31);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PrimitiveConstant)) {
            return false;
        }
        PrimitiveConstant other = (PrimitiveConstant) o;
        return this.kind.equals(other.kind) && this.primitive == other.primitive;
    }

    @Override
    public String toString() {
        if (getJavaKind() == JavaKind.Illegal) {
            return "illegal";
        } else {
            return getJavaKind().getJavaName() + "[" + asBoxedPrimitive() + "|0x" + Long.toHexString(primitive) + "]";
        }
    }
}
