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

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * represents an annotation that is represented in the class file and is
 * provided to the JVM.
 *
 * @since 6.0
 */
public class RuntimeVisibleAnnotations extends Annotations
{
    /**
     * @param name_index
     *            Index pointing to the name <em>Code</em>
     * @param length
     *            Content length in bytes
     * @param input
     *            Input stream
     * @param constant_pool
     *            Array of constants
     */
    public RuntimeVisibleAnnotations(final int name_index, final int length, final DataInput input, final ConstantPool constant_pool) throws IOException
    {
        super(Const.ATTR_RUNTIME_VISIBLE_ANNOTATIONS, name_index, length, input, constant_pool, true);
    }

    /**
     * @return deep copy of this attribute
     */
    @Override
    public Attribute copy(final ConstantPool constant_pool)
    {
        return (Attribute) clone();
    }

    @Override
    public final void dump(final DataOutputStream dos) throws IOException
    {
        super.dump(dos);
        writeAnnotations(dos);
    }
}
