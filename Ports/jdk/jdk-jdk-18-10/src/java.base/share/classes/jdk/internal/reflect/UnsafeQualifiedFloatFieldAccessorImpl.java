/*
 * Copyright (c) 2004, 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;

class UnsafeQualifiedFloatFieldAccessorImpl
    extends UnsafeQualifiedFieldAccessorImpl
{
    UnsafeQualifiedFloatFieldAccessorImpl(Field field, boolean isReadOnly) {
        super(field, isReadOnly);
    }

    public Object get(Object obj) throws IllegalArgumentException {
        return Float.valueOf(getFloat(obj));
    }

    public boolean getBoolean(Object obj) throws IllegalArgumentException {
        throw newGetBooleanIllegalArgumentException();
    }

    public byte getByte(Object obj) throws IllegalArgumentException {
        throw newGetByteIllegalArgumentException();
    }

    public char getChar(Object obj) throws IllegalArgumentException {
        throw newGetCharIllegalArgumentException();
    }

    public short getShort(Object obj) throws IllegalArgumentException {
        throw newGetShortIllegalArgumentException();
    }

    public int getInt(Object obj) throws IllegalArgumentException {
        throw newGetIntIllegalArgumentException();
    }

    public long getLong(Object obj) throws IllegalArgumentException {
        throw newGetLongIllegalArgumentException();
    }

    public float getFloat(Object obj) throws IllegalArgumentException {
        ensureObj(obj);
        return unsafe.getFloatVolatile(obj, fieldOffset);
    }

    public double getDouble(Object obj) throws IllegalArgumentException {
        return getFloat(obj);
    }

    public void set(Object obj, Object value)
        throws IllegalArgumentException, IllegalAccessException
    {
        ensureObj(obj);
        if (isReadOnly) {
            throwFinalFieldIllegalAccessException(value);
        }
        if (value == null) {
            throwSetIllegalArgumentException(value);
        }
        if (value instanceof Byte) {
            unsafe.putFloatVolatile(obj, fieldOffset, ((Byte) value).byteValue());
            return;
        }
        if (value instanceof Short) {
            unsafe.putFloatVolatile(obj, fieldOffset, ((Short) value).shortValue());
            return;
        }
        if (value instanceof Character) {
            unsafe.putFloatVolatile(obj, fieldOffset, ((Character) value).charValue());
            return;
        }
        if (value instanceof Integer) {
            unsafe.putFloatVolatile(obj, fieldOffset, ((Integer) value).intValue());
            return;
        }
        if (value instanceof Long) {
            unsafe.putFloatVolatile(obj, fieldOffset, ((Long) value).longValue());
            return;
        }
        if (value instanceof Float) {
            unsafe.putFloatVolatile(obj, fieldOffset, ((Float) value).floatValue());
            return;
        }
        throwSetIllegalArgumentException(value);
    }

    public void setBoolean(Object obj, boolean z)
        throws IllegalArgumentException, IllegalAccessException
    {
        throwSetIllegalArgumentException(z);
    }

    public void setByte(Object obj, byte b)
        throws IllegalArgumentException, IllegalAccessException
    {
        setFloat(obj, b);
    }

    public void setChar(Object obj, char c)
        throws IllegalArgumentException, IllegalAccessException
    {
        setFloat(obj, c);
    }

    public void setShort(Object obj, short s)
        throws IllegalArgumentException, IllegalAccessException
    {
        setFloat(obj, s);
    }

    public void setInt(Object obj, int i)
        throws IllegalArgumentException, IllegalAccessException
    {
        setFloat(obj, i);
    }

    public void setLong(Object obj, long l)
        throws IllegalArgumentException, IllegalAccessException
    {
        setFloat(obj, l);
    }

    public void setFloat(Object obj, float f)
        throws IllegalArgumentException, IllegalAccessException
    {
        ensureObj(obj);
        if (isReadOnly) {
            throwFinalFieldIllegalAccessException(f);
        }
        unsafe.putFloatVolatile(obj, fieldOffset, f);
    }

    public void setDouble(Object obj, double d)
        throws IllegalArgumentException, IllegalAccessException
    {
        throwSetIllegalArgumentException(d);
    }
}
