/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

// generated from vm/mlvm/meth/share/PrimitiveTypeConverter.jmpp

package vm.mlvm.meth.share;

public class PrimitiveTypeConverter {

    /** Unbox, cast and box */
    public static Object cast(Object fromValue, Class<?> toType) {

        Class<?> fromType = fromValue.getClass();

        if ( Byte.class.equals(fromType) && toType.equals(byte.class) )
            return fromValue;

        if ( Byte.class.equals(fromType) && toType.equals(short.class) )
            return Short.valueOf((short) ((Byte) fromValue).byteValue());

        if ( Byte.class.equals(fromType) && toType.equals(char.class) )
            return Character.valueOf((char) ((Byte) fromValue).byteValue());

        if ( Byte.class.equals(fromType) && toType.equals(int.class) )
            return Integer.valueOf((int) ((Byte) fromValue).byteValue());

        if ( Byte.class.equals(fromType) && toType.equals(long.class) )
            return Long.valueOf((long) ((Byte) fromValue).byteValue());

        if ( Byte.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Byte) fromValue).byteValue());

        if ( Byte.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Byte) fromValue).byteValue());

        if ( Short.class.equals(fromType) && toType.equals(byte.class) )
            return Byte.valueOf((byte) ((Short) fromValue).shortValue());

        if ( Short.class.equals(fromType) && toType.equals(short.class) )
            return fromValue;

        if ( Short.class.equals(fromType) && toType.equals(char.class) )
            return Character.valueOf((char) ((Short) fromValue).shortValue());

        if ( Short.class.equals(fromType) && toType.equals(int.class) )
            return Integer.valueOf((int) ((Short) fromValue).shortValue());

        if ( Short.class.equals(fromType) && toType.equals(long.class) )
            return Long.valueOf((long) ((Short) fromValue).shortValue());

        if ( Short.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Short) fromValue).shortValue());

        if ( Short.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Short) fromValue).shortValue());

        if ( Character.class.equals(fromType) && toType.equals(byte.class) )
            return Byte.valueOf((byte) ((Character) fromValue).charValue());

        if ( Character.class.equals(fromType) && toType.equals(short.class) )
            return Short.valueOf((short) ((Character) fromValue).charValue());

        if ( Character.class.equals(fromType) && toType.equals(char.class) )
            return fromValue;

        if ( Character.class.equals(fromType) && toType.equals(int.class) )
            return Integer.valueOf((int) ((Character) fromValue).charValue());

        if ( Character.class.equals(fromType) && toType.equals(long.class) )
            return Long.valueOf((long) ((Character) fromValue).charValue());

        if ( Character.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Character) fromValue).charValue());

        if ( Character.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Character) fromValue).charValue());

        if ( Integer.class.equals(fromType) && toType.equals(byte.class) )
            return Byte.valueOf((byte) ((Integer) fromValue).intValue());

        if ( Integer.class.equals(fromType) && toType.equals(short.class) )
            return Short.valueOf((short) ((Integer) fromValue).intValue());

        if ( Integer.class.equals(fromType) && toType.equals(char.class) )
            return Character.valueOf((char) ((Integer) fromValue).intValue());

        if ( Integer.class.equals(fromType) && toType.equals(int.class) )
            return fromValue;

        if ( Integer.class.equals(fromType) && toType.equals(long.class) )
            return Long.valueOf((long) ((Integer) fromValue).intValue());

        if ( Integer.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Integer) fromValue).intValue());

        if ( Integer.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Integer) fromValue).intValue());

        if ( Long.class.equals(fromType) && toType.equals(byte.class) )
            return Byte.valueOf((byte) ((Long) fromValue).longValue());

        if ( Long.class.equals(fromType) && toType.equals(short.class) )
            return Short.valueOf((short) ((Long) fromValue).longValue());

        if ( Long.class.equals(fromType) && toType.equals(char.class) )
            return Character.valueOf((char) ((Long) fromValue).longValue());

        if ( Long.class.equals(fromType) && toType.equals(int.class) )
            return Integer.valueOf((int) ((Long) fromValue).longValue());

        if ( Long.class.equals(fromType) && toType.equals(long.class) )
            return fromValue;

        if ( Long.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Long) fromValue).longValue());

        if ( Long.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Long) fromValue).longValue());

        if ( Float.class.equals(fromType) && toType.equals(byte.class) )
            return Byte.valueOf((byte) ((Float) fromValue).floatValue());

        if ( Float.class.equals(fromType) && toType.equals(short.class) )
            return Short.valueOf((short) ((Float) fromValue).floatValue());

        if ( Float.class.equals(fromType) && toType.equals(char.class) )
            return Character.valueOf((char) ((Float) fromValue).floatValue());

        if ( Float.class.equals(fromType) && toType.equals(int.class) )
            return Integer.valueOf((int) ((Float) fromValue).floatValue());

        if ( Float.class.equals(fromType) && toType.equals(long.class) )
            return Long.valueOf((long) ((Float) fromValue).floatValue());

        if ( Float.class.equals(fromType) && toType.equals(float.class) )
            return fromValue;

        if ( Float.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Float) fromValue).floatValue());

        if ( Double.class.equals(fromType) && toType.equals(byte.class) )
            return Byte.valueOf((byte) ((Double) fromValue).doubleValue());

        if ( Double.class.equals(fromType) && toType.equals(short.class) )
            return Short.valueOf((short) ((Double) fromValue).doubleValue());

        if ( Double.class.equals(fromType) && toType.equals(char.class) )
            return Character.valueOf((char) ((Double) fromValue).doubleValue());

        if ( Double.class.equals(fromType) && toType.equals(int.class) )
            return Integer.valueOf((int) ((Double) fromValue).doubleValue());

        if ( Double.class.equals(fromType) && toType.equals(long.class) )
            return Long.valueOf((long) ((Double) fromValue).doubleValue());

        if ( Double.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Double) fromValue).doubleValue());

        if ( Double.class.equals(fromType) && toType.equals(double.class) )
            return fromValue;

        throw new IllegalArgumentException("Can't cast [" + fromValue + "] to " + toType);
    }

    /** Unbox, do primitive widening conversion (JLS 5.1.2) and box */
    public static Object convert(Object fromValue, Class<?> toType) {
        Class<?> fromType = fromValue.getClass();

        if ( Byte.class.equals(fromType) && toType.equals(byte.class) )
            return fromValue;

        if ( Short.class.equals(fromType) && toType.equals(short.class) )
            return fromValue;

        if ( Byte.class.equals(fromType) && toType.equals(short.class) )
            return Short.valueOf((short) ((Byte) fromValue).byteValue());

        if ( Integer.class.equals(fromType) && toType.equals(int.class) )
            return fromValue;

        if ( Byte.class.equals(fromType) && toType.equals(int.class) )
            return Integer.valueOf((int) ((Byte) fromValue).byteValue());

        if ( Short.class.equals(fromType) && toType.equals(int.class) )
            return Integer.valueOf((int) ((Short) fromValue).shortValue());

        if ( Character.class.equals(fromType) && toType.equals(int.class) )
            return Integer.valueOf((int) ((Character) fromValue).charValue());

        if ( Long.class.equals(fromType) && toType.equals(long.class) )
            return fromValue;

        if ( Byte.class.equals(fromType) && toType.equals(long.class) )
            return Long.valueOf((long) ((Byte) fromValue).byteValue());

        if ( Short.class.equals(fromType) && toType.equals(long.class) )
            return Long.valueOf((long) ((Short) fromValue).shortValue());

        if ( Character.class.equals(fromType) && toType.equals(long.class) )
            return Long.valueOf((long) ((Character) fromValue).charValue());

        if ( Integer.class.equals(fromType) && toType.equals(long.class) )
            return Long.valueOf((long) ((Integer) fromValue).intValue());

        if ( Float.class.equals(fromType) && toType.equals(float.class) )
            return fromValue;

        if ( Byte.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Byte) fromValue).byteValue());

        if ( Short.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Short) fromValue).shortValue());

        if ( Character.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Character) fromValue).charValue());

        if ( Integer.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Integer) fromValue).intValue());

        if ( Long.class.equals(fromType) && toType.equals(float.class) )
            return Float.valueOf((float) ((Long) fromValue).longValue());

        if ( Double.class.equals(fromType) && toType.equals(double.class) )
            return fromValue;

        if ( Byte.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Byte) fromValue).byteValue());

        if ( Short.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Short) fromValue).shortValue());

        if ( Character.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Character) fromValue).charValue());

        if ( Integer.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Integer) fromValue).intValue());

        if ( Long.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Long) fromValue).longValue());

        if ( Float.class.equals(fromType) && toType.equals(double.class) )
            return Double.valueOf((double) ((Float) fromValue).floatValue());

        throw new IllegalArgumentException("Can't convert [" + fromValue + "] to " + toType);
    }
}
