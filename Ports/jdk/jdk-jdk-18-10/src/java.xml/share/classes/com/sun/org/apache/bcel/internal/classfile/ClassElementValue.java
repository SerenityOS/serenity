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
public class ClassElementValue extends ElementValue
{
    // For primitive types and string type, this points to the value entry in
    // the cpool
    // For 'class' this points to the class entry in the cpool
    private final int idx;

    public ClassElementValue(final int type, final int idx, final ConstantPool cpool)
    {
        super(type, cpool);
        this.idx = idx;
    }

    public int getIndex()
    {
        return idx;
    }

    public String getClassString()
    {
        final ConstantUtf8 c = (ConstantUtf8) super.getConstantPool().getConstant(idx,
                Const.CONSTANT_Utf8);
        return c.getBytes();
    }

    @Override
    public String stringifyValue()
    {
        final ConstantUtf8 cu8 = (ConstantUtf8) super.getConstantPool().getConstant(idx,
                Const.CONSTANT_Utf8);
        return cu8.getBytes();
    }

    @Override
    public void dump(final DataOutputStream dos) throws IOException
    {
        dos.writeByte(super.getType()); // u1 kind of value
        dos.writeShort(idx);
    }
}
