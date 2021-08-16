/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

/** This interface provides the declarations for the accessor methods
    of java.lang.reflect.Field. Each Field object is configured with a
    (possibly dynamically-generated) class which implements this
    interface. */

public interface FieldAccessor {
    /** Matches specification in {@link java.lang.reflect.Field} */
    public Object get(Object obj) throws IllegalArgumentException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public boolean getBoolean(Object obj) throws IllegalArgumentException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public byte getByte(Object obj) throws IllegalArgumentException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public char getChar(Object obj) throws IllegalArgumentException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public short getShort(Object obj) throws IllegalArgumentException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public int getInt(Object obj) throws IllegalArgumentException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public long getLong(Object obj) throws IllegalArgumentException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public float getFloat(Object obj) throws IllegalArgumentException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public double getDouble(Object obj) throws IllegalArgumentException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public void set(Object obj, Object value)
        throws IllegalArgumentException, IllegalAccessException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public void setBoolean(Object obj, boolean z)
        throws IllegalArgumentException, IllegalAccessException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public void setByte(Object obj, byte b)
        throws IllegalArgumentException, IllegalAccessException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public void setChar(Object obj, char c)
        throws IllegalArgumentException, IllegalAccessException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public void setShort(Object obj, short s)
        throws IllegalArgumentException, IllegalAccessException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public void setInt(Object obj, int i)
        throws IllegalArgumentException, IllegalAccessException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public void setLong(Object obj, long l)
        throws IllegalArgumentException, IllegalAccessException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public void setFloat(Object obj, float f)
        throws IllegalArgumentException, IllegalAccessException;

    /** Matches specification in {@link java.lang.reflect.Field} */
    public void setDouble(Object obj, double d)
        throws IllegalArgumentException, IllegalAccessException;
}
