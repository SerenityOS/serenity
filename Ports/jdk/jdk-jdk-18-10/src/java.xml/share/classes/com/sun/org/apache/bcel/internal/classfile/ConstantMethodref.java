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
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * This class represents a constant pool reference to a method.
 *
 */
public final class ConstantMethodref extends ConstantCP {

    /**
     * Initialize from another object.
     */
    public ConstantMethodref(final ConstantMethodref c) {
        super(Const.CONSTANT_Methodref, c.getClassIndex(), c.getNameAndTypeIndex());
    }


    /**
     * Initialize instance from input data.
     *
     * @param input input stream
     * @throws IOException
     */
    ConstantMethodref(final DataInput input) throws IOException {
        super(Const.CONSTANT_Methodref, input);
    }


    /**
     * @param class_index Reference to the class containing the method
     * @param name_and_type_index and the method signature
     */
    public ConstantMethodref(final int class_index, final int name_and_type_index) {
        super(Const.CONSTANT_Methodref, class_index, name_and_type_index);
    }


    /**
     * Called by objects that are traversing the nodes of the tree implicitely
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitConstantMethodref(this);
    }
}
