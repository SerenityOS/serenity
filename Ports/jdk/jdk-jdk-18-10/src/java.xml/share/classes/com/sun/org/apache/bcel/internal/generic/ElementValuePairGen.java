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

package com.sun.org.apache.bcel.internal.generic;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.classfile.ConstantUtf8;
import com.sun.org.apache.bcel.internal.classfile.ElementValue;
import com.sun.org.apache.bcel.internal.classfile.ElementValuePair;

/**
 * @since 6.0
 */
public class ElementValuePairGen
{
    private int nameIdx;

    private final ElementValueGen value;

    private final ConstantPoolGen constantPoolGen;

    public ElementValuePairGen(final ElementValuePair nvp, final ConstantPoolGen cpool,
            final boolean copyPoolEntries)
    {
        this.constantPoolGen = cpool;
        // J5ASSERT:
        // Could assert nvp.getNameString() points to the same thing as
        // constantPoolGen.getConstant(nvp.getNameIndex())
        // if
        // (!nvp.getNameString().equals(((ConstantUtf8)constantPoolGen.getConstant(nvp.getNameIndex())).getBytes()))
        // {
        // throw new IllegalArgumentException("envp buggered");
        // }
        if (copyPoolEntries)
        {
            nameIdx = cpool.addUtf8(nvp.getNameString());
        }
        else
        {
            nameIdx = nvp.getNameIndex();
        }
        value = ElementValueGen.copy(nvp.getValue(), cpool, copyPoolEntries);
    }

    /**
     * Retrieve an immutable version of this ElementNameValuePairGen
     */
    public ElementValuePair getElementNameValuePair()
    {
        final ElementValue immutableValue = value.getElementValue();
        return new ElementValuePair(nameIdx, immutableValue, constantPoolGen
                .getConstantPool());
    }

    protected ElementValuePairGen(final int idx, final ElementValueGen value,
            final ConstantPoolGen cpool)
    {
        this.nameIdx = idx;
        this.value = value;
        this.constantPoolGen = cpool;
    }

    public ElementValuePairGen(final String name, final ElementValueGen value,
            final ConstantPoolGen cpool)
    {
        this.nameIdx = cpool.addUtf8(name);
        this.value = value;
        this.constantPoolGen = cpool;
    }

    protected void dump(final DataOutputStream dos) throws IOException
    {
        dos.writeShort(nameIdx); // u2 name of the element
        value.dump(dos);
    }

    public int getNameIndex()
    {
        return nameIdx;
    }

    public final String getNameString()
    {
        // ConstantString cu8 = (ConstantString)constantPoolGen.getConstant(nameIdx);
        return ((ConstantUtf8) constantPoolGen.getConstant(nameIdx)).getBytes();
    }

    public final ElementValueGen getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return "ElementValuePair:[" + getNameString() + "="
                + value.stringifyValue() + "]";
    }
}
