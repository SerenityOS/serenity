/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import com.sun.jdi.FloatValue;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;

public class FloatValueImpl extends PrimitiveValueImpl
                            implements FloatValue {
    private float value;

    FloatValueImpl(VirtualMachine aVm, float aValue) {
        super(aVm);
        value = aValue;
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof FloatValue)) {
            return (value == ((FloatValue)obj).value()) &&
                   super.equals(obj);
        } else {
            return false;
        }
    }

    public int hashCode() {
        /*
         * TO DO: Better hash code
         */
        return intValue();
    }

    public int compareTo(FloatValue obj) {
        float other = obj.value();
        if (value() < other) {
            return -1;
        } else if (value() == other) {
            return 0;
        } else {
            return 1;
        }
    }

    public Type type() {
        return vm.theFloatType();
    }

    public float value() {
        return value;
    }

    public boolean booleanValue() {
        return (value == 0.0 ? false : true);
    }

    public byte byteValue() {
        return (byte)value;
    }

    public char charValue() {
        return (char)value;
    }

    public short shortValue() {
        return (short)value;
    }

    public int intValue() {
        return (int)value;
    }

    public long longValue() {
        return (long)value;
    }

    public float floatValue() {
        return value;
    }

    public double doubleValue() {
        return value;
    }

    byte checkedByteValue() throws InvalidTypeException {
        if ((value > Byte.MAX_VALUE) || (value < Byte.MIN_VALUE)) {
            throw new InvalidTypeException("Can't convert " + value + " to byte");
        } else {
            return super.checkedByteValue();
        }
    }

    char checkedCharValue() throws InvalidTypeException {
        if ((value > Character.MAX_VALUE) || (value < Character.MIN_VALUE)) {
            throw new InvalidTypeException("Can't convert " + value + " to char");
        } else {
            return super.checkedCharValue();
        }
    }

    short checkedShortValue() throws InvalidTypeException {
        if ((value > Short.MAX_VALUE) || (value < Short.MIN_VALUE)) {
            throw new InvalidTypeException("Can't convert " + value + " to short");
        } else {
            return super.checkedShortValue();
        }
    }

    int checkedIntValue() throws InvalidTypeException {
        int intValue = (int)value;
        if (intValue != value) {
            throw new InvalidTypeException("Can't convert " + value + " to int");
        } else {
            return super.checkedIntValue();
        }
    }

    long checkedLongValue() throws InvalidTypeException {
        long longValue = (long)value;
        if (longValue != value) {
            throw new InvalidTypeException("Can't convert " + value + " to long");
        } else {
            return super.checkedLongValue();
        }
    }

    public String toString() {
        return "" + value;
    }

    byte typeValueKey() {
        return JDWP.Tag.FLOAT;
    }
}
