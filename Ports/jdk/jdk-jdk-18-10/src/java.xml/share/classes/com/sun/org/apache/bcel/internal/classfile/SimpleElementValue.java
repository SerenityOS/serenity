/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * @since 6.0
 */
public class SimpleElementValue extends ElementValue
{
    private int index;

    public SimpleElementValue(final int type, final int index, final ConstantPool cpool)
    {
        super(type, cpool);
        this.index = index;
    }

    /**
     * @return Value entry index in the cpool
     */
    public int getIndex()
    {
        return index;
    }

    public void setIndex(final int index)
    {
        this.index = index;
    }

    public String getValueString()
    {
        if (super.getType() != STRING) {
            throw new IllegalStateException(
                    "Dont call getValueString() on a non STRING ElementValue");
        }
        final ConstantUtf8 c = (ConstantUtf8) super.getConstantPool().getConstant(getIndex(),
                Const.CONSTANT_Utf8);
        return c.getBytes();
    }

    public int getValueInt()
    {
        if (super.getType() != PRIMITIVE_INT) {
            throw new IllegalStateException(
                    "Dont call getValueString() on a non STRING ElementValue");
        }
        final ConstantInteger c = (ConstantInteger) super.getConstantPool().getConstant(getIndex(),
                Const.CONSTANT_Integer);
        return c.getBytes();
    }

    public byte getValueByte()
    {
        if (super.getType() != PRIMITIVE_BYTE) {
            throw new IllegalStateException(
                    "Dont call getValueByte() on a non BYTE ElementValue");
        }
        final ConstantInteger c = (ConstantInteger) super.getConstantPool().getConstant(getIndex(),
                Const.CONSTANT_Integer);
        return (byte) c.getBytes();
    }

    public char getValueChar()
    {
        if (super.getType() != PRIMITIVE_CHAR) {
            throw new IllegalStateException(
                    "Dont call getValueChar() on a non CHAR ElementValue");
        }
        final ConstantInteger c = (ConstantInteger) super.getConstantPool().getConstant(getIndex(),
                Const.CONSTANT_Integer);
        return (char) c.getBytes();
    }

    public long getValueLong()
    {
        if (super.getType() != PRIMITIVE_LONG) {
            throw new IllegalStateException(
                    "Dont call getValueLong() on a non LONG ElementValue");
        }
        final ConstantLong j = (ConstantLong) super.getConstantPool().getConstant(getIndex());
        return j.getBytes();
    }

    public float getValueFloat()
    {
        if (super.getType() != PRIMITIVE_FLOAT) {
            throw new IllegalStateException(
                    "Dont call getValueFloat() on a non FLOAT ElementValue");
        }
        final ConstantFloat f = (ConstantFloat) super.getConstantPool().getConstant(getIndex());
        return f.getBytes();
    }

    public double getValueDouble()
    {
        if (super.getType() != PRIMITIVE_DOUBLE) {
            throw new IllegalStateException(
                    "Dont call getValueDouble() on a non DOUBLE ElementValue");
        }
        final ConstantDouble d = (ConstantDouble) super.getConstantPool().getConstant(getIndex());
        return d.getBytes();
    }

    public boolean getValueBoolean()
    {
        if (super.getType() != PRIMITIVE_BOOLEAN) {
            throw new IllegalStateException(
                    "Dont call getValueBoolean() on a non BOOLEAN ElementValue");
        }
        final ConstantInteger bo = (ConstantInteger) super.getConstantPool().getConstant(getIndex());
        return bo.getBytes() != 0;
    }

    public short getValueShort()
    {
        if (super.getType() != PRIMITIVE_SHORT) {
            throw new IllegalStateException(
                    "Dont call getValueShort() on a non SHORT ElementValue");
        }
        final ConstantInteger s = (ConstantInteger) super.getConstantPool().getConstant(getIndex());
        return (short) s.getBytes();
    }

    @Override
    public String toString()
    {
        return stringifyValue();
    }

    // Whatever kind of value it is, return it as a string
    @Override
    public String stringifyValue()
    {
        final ConstantPool cpool = super.getConstantPool();
        final int _type = super.getType();
        switch (_type)
        {
        case PRIMITIVE_INT:
            final ConstantInteger c = (ConstantInteger) cpool.getConstant(getIndex(),
                    Const.CONSTANT_Integer);
            return Integer.toString(c.getBytes());
        case PRIMITIVE_LONG:
            final ConstantLong j = (ConstantLong) cpool.getConstant(getIndex(),
                    Const.CONSTANT_Long);
            return Long.toString(j.getBytes());
        case PRIMITIVE_DOUBLE:
            final ConstantDouble d = (ConstantDouble) cpool.getConstant(getIndex(),
                    Const.CONSTANT_Double);
            return Double.toString(d.getBytes());
        case PRIMITIVE_FLOAT:
            final ConstantFloat f = (ConstantFloat) cpool.getConstant(getIndex(),
                    Const.CONSTANT_Float);
            return Float.toString(f.getBytes());
        case PRIMITIVE_SHORT:
            final ConstantInteger s = (ConstantInteger) cpool.getConstant(getIndex(),
                    Const.CONSTANT_Integer);
            return Integer.toString(s.getBytes());
        case PRIMITIVE_BYTE:
            final ConstantInteger b = (ConstantInteger) cpool.getConstant(getIndex(),
                    Const.CONSTANT_Integer);
            return Integer.toString(b.getBytes());
        case PRIMITIVE_CHAR:
            final ConstantInteger ch = (ConstantInteger) cpool.getConstant(
                    getIndex(), Const.CONSTANT_Integer);
            return String.valueOf((char)ch.getBytes());
        case PRIMITIVE_BOOLEAN:
            final ConstantInteger bo = (ConstantInteger) cpool.getConstant(
                    getIndex(), Const.CONSTANT_Integer);
            if (bo.getBytes() == 0) {
                return "false";
            }
            return "true";
        case STRING:
            final ConstantUtf8 cu8 = (ConstantUtf8) cpool.getConstant(getIndex(),
                    Const.CONSTANT_Utf8);
            return cu8.getBytes();
        default:
            throw new IllegalStateException("SimpleElementValue class does not know how to stringify type " + _type);
        }
    }

    @Override
    public void dump(final DataOutputStream dos) throws IOException
    {
        final int _type = super.getType();
        dos.writeByte(_type); // u1 kind of value
        switch (_type)
        {
        case PRIMITIVE_INT:
        case PRIMITIVE_BYTE:
        case PRIMITIVE_CHAR:
        case PRIMITIVE_FLOAT:
        case PRIMITIVE_LONG:
        case PRIMITIVE_BOOLEAN:
        case PRIMITIVE_SHORT:
        case PRIMITIVE_DOUBLE:
        case STRING:
            dos.writeShort(getIndex());
            break;
        default:
            throw new IllegalStateException("SimpleElementValue doesnt know how to write out type " + _type);
        }
    }
}
