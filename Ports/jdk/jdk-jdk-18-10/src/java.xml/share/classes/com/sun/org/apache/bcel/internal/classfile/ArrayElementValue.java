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

/**
 * @since 6.0
 */
public class ArrayElementValue extends ElementValue
{
    // For array types, this is the array
    private final ElementValue[] elementValues;

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < elementValues.length; i++)
        {
            sb.append(elementValues[i]);
            if ((i + 1) < elementValues.length) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public ArrayElementValue(final int type, final ElementValue[] datums, final ConstantPool cpool)
    {
        super(type, cpool);
        if (type != ARRAY) {
            throw new IllegalArgumentException(
                    "Only element values of type array can be built with this ctor - type specified: " + type);
        }
        this.elementValues = datums;
    }

    @Override
    public void dump(final DataOutputStream dos) throws IOException
    {
        dos.writeByte(super.getType()); // u1 type of value (ARRAY == '[')
        dos.writeShort(elementValues.length);
        for (final ElementValue evalue : elementValues) {
            evalue.dump(dos);
        }
    }

    @Override
    public String stringifyValue()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < elementValues.length; i++)
        {
            sb.append(elementValues[i].stringifyValue());
            if ((i + 1) < elementValues.length) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public ElementValue[] getElementValuesArray()
    {
        return elementValues;
    }

    public int getElementValuesArraySize()
    {
        return elementValues.length;
    }
}
