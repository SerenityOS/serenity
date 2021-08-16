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

import com.sun.org.apache.bcel.internal.Const;

/**
 * Denotes array type, such as int[][]
 *
 */
public final class ArrayType extends ReferenceType {

    private int dimensions;
    private Type basicType;


    /**
     * Convenience constructor for array type, e.g. int[]
     *
     * @param type array type, e.g. T_INT
     */
    public ArrayType(final byte type, final int dimensions) {
        this(BasicType.getType(type), dimensions);
    }


    /**
     * Convenience constructor for reference array type, e.g. Object[]
     *
     * @param class_name complete name of class (java.lang.String, e.g.)
     */
    public ArrayType(final String class_name, final int dimensions) {
        this(ObjectType.getInstance(class_name), dimensions);
    }


    /**
     * Constructor for array of given type
     *
     * @param type type of array (may be an array itself)
     */
    public ArrayType(final Type type, final int dimensions) {
        super(Const.T_ARRAY, "<dummy>");
        if ((dimensions < 1) || (dimensions > Const.MAX_BYTE)) {
            throw new ClassGenException("Invalid number of dimensions: " + dimensions);
        }
        switch (type.getType()) {
            case Const.T_ARRAY:
                final ArrayType array = (ArrayType) type;
                this.dimensions = dimensions + array.dimensions;
                basicType = array.basicType;
                break;
            case Const.T_VOID:
                throw new ClassGenException("Invalid type: void[]");
            default: // Basic type or reference
                this.dimensions = dimensions;
                basicType = type;
                break;
        }
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < this.dimensions; i++) {
            buf.append('[');
        }
        buf.append(basicType.getSignature());
        super.setSignature(buf.toString());
    }


    /**
     * @return basic type of array, i.e., for int[][][] the basic type is int
     */
    public Type getBasicType() {
        return basicType;
    }


    /**
     * @return element type of array, i.e., for int[][][] the element type is int[][]
     */
    public Type getElementType() {
        if (dimensions == 1) {
            return basicType;
        }
        return new ArrayType(basicType, dimensions - 1);
    }


    /** @return number of dimensions of array
     */
    public int getDimensions() {
        return dimensions;
    }


    /** @return a hash code value for the object.
     */
    @Override
    public int hashCode() {
        return basicType.hashCode() ^ dimensions;
    }


    /** @return true if both type objects refer to the same array type.
     */
    @Override
    public boolean equals( final Object _type ) {
        if (_type instanceof ArrayType) {
            final ArrayType array = (ArrayType) _type;
            return (array.dimensions == dimensions) && array.basicType.equals(basicType);
        }
        return false;
    }
}
