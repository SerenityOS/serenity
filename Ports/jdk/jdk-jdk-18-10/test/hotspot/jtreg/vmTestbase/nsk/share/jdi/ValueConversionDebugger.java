/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi;

import java.lang.reflect.*;
import nsk.share.*;
import nsk.share.jpda.ConversionUtils;
import com.sun.jdi.*;

/*
 * Class contains several common methods used by tests checking that values are
 * correctly converted as a result of JDI interface work (e.g. when method
 * 'ObjectReference.setValue(Field, Value)' is called)
 */
public class ValueConversionDebugger extends TestDebuggerType2 {

    protected static enum ValueType {
        BYTE,
        CHAR,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE
    }

    /*
     * short aliases for ValueType members
     */
    protected static ValueType BYTE = ValueType.BYTE;
    protected static ValueType CHAR = ValueType.CHAR;
    protected static ValueType SHORT = ValueType.SHORT;
    protected static ValueType INT = ValueType.INT;
    protected static ValueType LONG = ValueType.LONG;
    protected static ValueType FLOAT = ValueType.FLOAT;
    protected static ValueType DOUBLE = ValueType.DOUBLE;

    /*
     * Is information lost when given PrimitiveValue converted to the
     * primitive type representing by the destType
     */
    public static boolean informationLoss(PrimitiveValue value, Class destType) {
        /*
         * Use reflection here to avoid large nested switches
         * (construct method name, method is located in the nsk.share.jpda.ConversionUtils)
         */
        String methodNameToCall = "informationLoss";

        Object param = null;

        if (value instanceof ByteValue) {
            methodNameToCall += "ByteTo";
            param = Byte.valueOf(value.byteValue());
        } else if (value instanceof ShortValue) {
            methodNameToCall += "ShortTo";
            param = Short.valueOf(value.shortValue());
        } else if (value instanceof CharValue) {
            methodNameToCall += "CharTo";
            param = Character.valueOf(value.charValue());
        } else if (value instanceof IntegerValue) {
            methodNameToCall += "IntTo";
            param = Integer.valueOf(value.intValue());
        } else if (value instanceof LongValue) {
            methodNameToCall += "LongTo";
            param = Long.valueOf(value.longValue());
        } else if (value instanceof FloatValue) {
            methodNameToCall += "FloatTo";
            param = Float.valueOf(value.floatValue());
        } else if (value instanceof DoubleValue) {
            methodNameToCall += "DoubleTo";
            param = Double.valueOf(value.doubleValue());
        } else
            throw new IllegalArgumentException("Illegal PrimitiveValue: " + value);

        if (!destType.isPrimitive())
            throw new IllegalArgumentException("Illegal destType: " + destType + ", should be primitive type");

        if (destType == Byte.TYPE) {
            methodNameToCall += "Byte";
        } else if (destType == Short.TYPE) {
            methodNameToCall += "Short";
        } else if (destType == Character.TYPE) {
            methodNameToCall += "Char";
        } else if (destType == Integer.TYPE) {
            methodNameToCall += "Int";
        } else if (destType == Long.TYPE) {
            methodNameToCall += "Long";
        } else if (destType == Float.TYPE) {
            methodNameToCall += "Float";
        } else if (destType == Double.TYPE) {
            methodNameToCall += "Double";
        } else
            throw new IllegalArgumentException("Illegal destType: " + destType + ", should be primitive type");

        java.lang.reflect.Method method;
        try {
            method = ConversionUtils.class.getMethod(methodNameToCall, param.getClass());
        } catch (NoSuchMethodException e) {
            throw new Failure("Unexpected exception: " + e, e);
        }

        try {
            return (Boolean)method.invoke(null, new Object[]{param});
        } catch (IllegalAccessException e) {
            throw new Failure("Unexpected exception: " + e, e);
        } catch (InvocationTargetException e) {
            throw new Failure("Unexpected exception: " + e, e);
        }
    }

    /*
     * Is given PrimitiveValue can be converted to the primitive type represented by the
     * destType without information loss
     */
    public static boolean isValidConversion(PrimitiveValue value, Class destType) {
        return !informationLoss(value, destType);
    }

    /*
     * Method is used in subclasses for creation of tested values
     * (reflection is used to simplify coding)
     */
    protected PrimitiveValue createValue(Object arr, int arrayIndex) {
        PrimitiveValue value;

        if (arr instanceof byte[]) {
            value = debuggee.VM().mirrorOf(Array.getByte(arr,arrayIndex));
        } else if (arr instanceof char[]) {
            value = debuggee.VM().mirrorOf(Array.getChar(arr,arrayIndex));
        } else if (arr instanceof double[]) {
            value = debuggee.VM().mirrorOf(Array.getDouble(arr,arrayIndex));
        } else if (arr instanceof float[]) {
            value = debuggee.VM().mirrorOf(Array.getFloat(arr,arrayIndex));
        } else if (arr instanceof int[]) {
            value = debuggee.VM().mirrorOf(Array.getInt(arr,arrayIndex));
        } else if (arr instanceof long[]) {
            value = debuggee.VM().mirrorOf(Array.getLong(arr,arrayIndex));
        } else if (arr instanceof short[]) {
            value = debuggee.VM().mirrorOf(Array.getShort(arr,arrayIndex));
        } else {
            setSuccess(false);
            throw new TestBug("Unexpected object was passed in the 'createValue': " + arr);
        }

        return value;
    }

    /*
     * used by subclasses for debug output
     * (modified in the method 'isValidConversion')
     */
    protected String lastConversion;

    /*
     * Is given PrimitiveValue can be converted to the primitive type represented by the given type
     * without information loss
     */
    protected boolean isValidConversion(ValueType type, PrimitiveValue value) {
        com.sun.jdi.Type fromType = value.type();

        boolean ret = false;
        lastConversion = " conversion from "
                            + value + "(" + fromType + ")" + " to ";
        switch (type) {
        case BYTE:
                byte b = value.byteValue();
                ret = isValidConversion(value, Byte.TYPE);
                lastConversion += b + "(byte)";
                break;
        case CHAR:
                char c = value.charValue();
                ret = isValidConversion(value, Character.TYPE);
                lastConversion += Integer.toHexString(c) + "(char)";
                break;
        case DOUBLE:
                double d = value.doubleValue();
                ret = isValidConversion(value, Double.TYPE);
                lastConversion += d + "(double)";
                break;
        case FLOAT:
                float f = value.floatValue();
                ret = isValidConversion(value, Float.TYPE);
                lastConversion += f + "(float)";
                break;
        case INT:
                int i = value.intValue();
                ret = isValidConversion(value, Integer.TYPE);
                lastConversion += i + "(int)";
                break;
        case LONG:
                long j = value.longValue();
                ret = isValidConversion(value, Long.TYPE);
                lastConversion += j + "(long)";
                break;
        case SHORT:
                short s = value.shortValue();
                ret = isValidConversion(value, Short.TYPE);
                lastConversion += s + "(short)";
                break;
        default:
            throw new IllegalArgumentException("Invalid type: " + type);
        }
        return ret;
    }

    /*
     * Used in subclasses to check that given PrimitiveValue was correctly converted as a result
     * of JDI interface work (retValue - conversion result)
     * (
     *  example:
     *          test assigns DoubleValue = 1.5 (value) to the byte Field (retValue - ByteValue = 1),
     *          in this case we should check that value.byteValue() == retValue.byteValue()
     * )
     */
    protected void checkValueConversion(PrimitiveValue value, PrimitiveValue retValue) {
        boolean res;

        if (retValue instanceof ByteValue) {
            res = value.byteValue() != retValue.byteValue();
        } else if (retValue instanceof ShortValue) {
            res = value.shortValue() != retValue.shortValue();
        } else if (retValue instanceof CharValue) {
            res = value.charValue() != retValue.charValue();
        } else if (retValue instanceof IntegerValue) {
            res = value.intValue() != retValue.intValue();
        } else if (retValue instanceof LongValue) {
            res = value.longValue() != retValue.longValue();
        } else if (retValue instanceof FloatValue) {
            res = value.floatValue() != retValue.floatValue();
        } else if (retValue instanceof DoubleValue) {
            res = value.doubleValue() != retValue.doubleValue();
        } else {
            throw new TestBug("Invalid value type in the 'checkValueConversion': " + retValue.type().name());
        }

        if (res) {
            setSuccess(false);
            complain("Conversion error");
            complain("From type: " + value.type().name() + ", to type: " + retValue.type().name());
            complain(retValue + " != " + value);
            display("");
        }
    }
}
