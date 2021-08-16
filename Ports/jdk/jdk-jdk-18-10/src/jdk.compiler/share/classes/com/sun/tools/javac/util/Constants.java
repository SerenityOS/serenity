/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import com.sun.tools.javac.code.Type;

/**
 * Utilities for operating on constant values.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class Constants {

    /**
     * Converts a constant in internal representation (in which
     * boolean, char, byte, short, and int are each represented by an
     * Integer) into standard representation.  Other values (including
     * null) are returned unchanged.
     */
    public static Object decode(Object value, Type type) {
        if (value instanceof Integer intVal) {
            int i = intVal;
            switch (type.getTag()) {
            case BOOLEAN:  return i != 0;
            case CHAR:     return (char) i;
            case BYTE:     return (byte) i;
            case SHORT:    return (short) i;
            }
        }
        return value;
    }

    /**
     * Returns a string representation of a constant value (given in
     * internal representation), quoted and formatted as in Java source.
     */
    public static String format(Object value, Type type) {
        value = decode(value, type);
        switch (type.getTag()) {
        case BYTE:      return formatByte((Byte) value);
        case LONG:      return formatLong((Long) value);
        case FLOAT:     return formatFloat((Float) value);
        case DOUBLE:    return formatDouble((Double) value);
        case CHAR:      return formatChar((Character) value);
        }
        if (value instanceof String str)
            return formatString(str);
        return value + "";
    }

    /**
     * Returns a string representation of a constant value (given in
     * standard wrapped representation), quoted and formatted as in
     * Java source.
     */
    public static String format(Object value) {
        if (value instanceof Byte byteVal)      return formatByte(byteVal);
        if (value instanceof Short shortVal)     return formatShort(shortVal);
        if (value instanceof Long longVal)      return formatLong(longVal);
        if (value instanceof Float floatVal)     return formatFloat(floatVal);
        if (value instanceof Double doubleVal)    return formatDouble(doubleVal);
        if (value instanceof Character charVal) return formatChar(charVal);
        if (value instanceof String strVal)    return formatString(strVal);
        if (value instanceof Integer ||
            value instanceof Boolean)   return value.toString();
        else
            throw new IllegalArgumentException("Argument is not a primitive type or a string; it " +
                                               ((value == null) ?
                                                "is a null value." :
                                                "has class " +
                                                value.getClass().getName()) + "." );
    }

    private static String formatByte(byte b) {
        return String.format("(byte)0x%02x", b);
    }

    private static String formatShort(short s) {
        return String.format("(short)%d", s);
    }

    private static String formatLong(long lng) {
        return lng + "L";
    }

    private static String formatFloat(float f) {
        if (Float.isNaN(f))
            return "0.0f/0.0f";
        else if (Float.isInfinite(f))
            return (f < 0) ? "-1.0f/0.0f" : "1.0f/0.0f";
        else
            return f + "f";
    }

    private static String formatDouble(double d) {
        if (Double.isNaN(d))
            return "0.0/0.0";
        else if (Double.isInfinite(d))
            return (d < 0) ? "-1.0/0.0" : "1.0/0.0";
        else
            return d + "";
    }

    private static String formatChar(char c) {
        return '\'' + Convert.quote(c) + '\'';
    }

    private static String formatString(String s) {
        return '"' + Convert.quote(s) + '"';
    }
}
