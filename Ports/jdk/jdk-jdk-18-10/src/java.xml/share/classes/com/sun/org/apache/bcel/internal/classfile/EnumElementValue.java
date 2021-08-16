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
public class EnumElementValue extends ElementValue
{
    // For enum types, these two indices point to the type and value
    private final int typeIdx;

    private final int valueIdx;

    public EnumElementValue(final int type, final int typeIdx, final int valueIdx,
            final ConstantPool cpool)
    {
        super(type, cpool);
        if (type != ENUM_CONSTANT) {
            throw new IllegalArgumentException(
                    "Only element values of type enum can be built with this ctor - type specified: " + type);
        }
        this.typeIdx = typeIdx;
        this.valueIdx = valueIdx;
    }

    @Override
    public void dump(final DataOutputStream dos) throws IOException
    {
        dos.writeByte(super.getType()); // u1 type of value (ENUM_CONSTANT == 'e')
        dos.writeShort(typeIdx); // u2
        dos.writeShort(valueIdx); // u2
    }

    @Override
    public String stringifyValue()
    {
        final ConstantUtf8 cu8 = (ConstantUtf8) super.getConstantPool().getConstant(valueIdx,
                Const.CONSTANT_Utf8);
        return cu8.getBytes();
    }

    public String getEnumTypeString()
    {
        final ConstantUtf8 cu8 = (ConstantUtf8) super.getConstantPool().getConstant(typeIdx,
                Const.CONSTANT_Utf8);
        return cu8.getBytes();// Utility.signatureToString(cu8.getBytes());
    }

    public String getEnumValueString()
    {
        final ConstantUtf8 cu8 = (ConstantUtf8) super.getConstantPool().getConstant(valueIdx,
                Const.CONSTANT_Utf8);
        return cu8.getBytes();
    }

    public int getValueIndex()
    {
        return valueIdx;
    }

    public int getTypeIndex()
    {
        return typeIdx;
    }
}
