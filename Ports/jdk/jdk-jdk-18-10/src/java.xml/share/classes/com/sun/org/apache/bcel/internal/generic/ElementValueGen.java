/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.classfile.AnnotationElementValue;
import com.sun.org.apache.bcel.internal.classfile.AnnotationEntry;
import com.sun.org.apache.bcel.internal.classfile.ArrayElementValue;
import com.sun.org.apache.bcel.internal.classfile.ClassElementValue;
import com.sun.org.apache.bcel.internal.classfile.ElementValue;
import com.sun.org.apache.bcel.internal.classfile.EnumElementValue;
import com.sun.org.apache.bcel.internal.classfile.SimpleElementValue;

/**
 * @since 6.0
 * @LastModified: May 2021
 */
public abstract class ElementValueGen
{
    private final int type;
    private final ConstantPoolGen cpGen;

    protected ElementValueGen(final int type, final ConstantPoolGen cpGen)
    {
        this.type = type;
        this.cpGen = cpGen;
    }

    /**
     * Subtypes return an immutable variant of the ElementValueGen
     */
    public abstract ElementValue getElementValue();

    public int getElementValueType()
    {
        return type;
    }

    public abstract String stringifyValue();

    public abstract void dump(DataOutputStream dos) throws IOException;

    public static final int STRING = 's';

    public static final int ENUM_CONSTANT = 'e';

    public static final int CLASS = 'c';

    public static final int ANNOTATION = '@';

    public static final int ARRAY = '[';

    public static final int PRIMITIVE_INT = 'I';

    public static final int PRIMITIVE_BYTE = 'B';

    public static final int PRIMITIVE_CHAR = 'C';

    public static final int PRIMITIVE_DOUBLE = 'D';

    public static final int PRIMITIVE_FLOAT = 'F';

    public static final int PRIMITIVE_LONG = 'J';

    public static final int PRIMITIVE_SHORT = 'S';

    public static final int PRIMITIVE_BOOLEAN = 'Z';

    public static ElementValueGen readElementValue(final DataInput dis,
            final ConstantPoolGen cpGen) throws IOException
    {
        final int type = dis.readUnsignedByte();
        switch (type)
        {
        case 'B': // byte
            return new SimpleElementValueGen(PRIMITIVE_BYTE, dis
                    .readUnsignedShort(), cpGen);
        case 'C': // char
            return new SimpleElementValueGen(PRIMITIVE_CHAR, dis
                    .readUnsignedShort(), cpGen);
        case 'D': // double
            return new SimpleElementValueGen(PRIMITIVE_DOUBLE, dis
                    .readUnsignedShort(), cpGen);
        case 'F': // float
            return new SimpleElementValueGen(PRIMITIVE_FLOAT, dis
                    .readUnsignedShort(), cpGen);
        case 'I': // int
            return new SimpleElementValueGen(PRIMITIVE_INT, dis
                    .readUnsignedShort(), cpGen);
        case 'J': // long
            return new SimpleElementValueGen(PRIMITIVE_LONG, dis
                    .readUnsignedShort(), cpGen);
        case 'S': // short
            return new SimpleElementValueGen(PRIMITIVE_SHORT, dis
                    .readUnsignedShort(), cpGen);
        case 'Z': // boolean
            return new SimpleElementValueGen(PRIMITIVE_BOOLEAN, dis
                    .readUnsignedShort(), cpGen);
        case 's': // String
            return new SimpleElementValueGen(STRING, dis.readUnsignedShort(),
                    cpGen);
        case 'e': // Enum constant
            return new EnumElementValueGen(dis.readUnsignedShort(), dis
                    .readUnsignedShort(), cpGen);
        case 'c': // Class
            return new ClassElementValueGen(dis.readUnsignedShort(), cpGen);
        case '@': // Annotation
            // TODO: isRuntimeVisible ??????????
            // FIXME
            return new AnnotationElementValueGen(ANNOTATION,
                    new AnnotationEntryGen(AnnotationEntry.read(dis, cpGen
                            .getConstantPool(), true), cpGen, false), cpGen);
        case '[': // Array
            final int numArrayVals = dis.readUnsignedShort();
            final ElementValue[] evalues = new ElementValue[numArrayVals];
            for (int j = 0; j < numArrayVals; j++)
            {
                evalues[j] = ElementValue.readElementValue(dis, cpGen
                        .getConstantPool());
            }
            return new ArrayElementValueGen(ARRAY, evalues, cpGen);
        default:
            throw new IllegalArgumentException("Unexpected element value kind in annotation: " + type);
        }
    }

    protected ConstantPoolGen getConstantPool()
    {
        return cpGen;
    }

    /**
     * Creates an (modifiable) ElementValueGen copy of an (immutable)
     * ElementValue - constant pool is assumed correct.
     */
    public static ElementValueGen copy(final ElementValue value,
            final ConstantPoolGen cpool, final boolean copyPoolEntries)
    {
        switch (value.getElementValueType())
        {
        case 'B': // byte
        case 'C': // char
        case 'D': // double
        case 'F': // float
        case 'I': // int
        case 'J': // long
        case 'S': // short
        case 'Z': // boolean
        case 's': // String
            return new SimpleElementValueGen((SimpleElementValue) value, cpool,
                    copyPoolEntries);
        case 'e': // Enum constant
            return new EnumElementValueGen((EnumElementValue) value, cpool,
                    copyPoolEntries);
        case '@': // Annotation
            return new AnnotationElementValueGen(
                    (AnnotationElementValue) value, cpool, copyPoolEntries);
        case '[': // Array
            return new ArrayElementValueGen((ArrayElementValue) value, cpool,
                    copyPoolEntries);
        case 'c': // Class
            return new ClassElementValueGen((ClassElementValue) value, cpool,
                    copyPoolEntries);
        default:
            throw new UnsupportedOperationException("Not implemented yet! (" + value.getElementValueType() + ")");
        }
    }
}
