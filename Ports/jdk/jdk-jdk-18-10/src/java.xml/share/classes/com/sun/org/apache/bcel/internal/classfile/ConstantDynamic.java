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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataInput;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * This class is derived from the abstract {@link Constant}
 * and represents a reference to a dynamically computed constant.
 *
 * @see     Constant
 * @see  <a href="https://bugs.openjdk.java.net/secure/attachment/74618/constant-dynamic.html">
 * Change request for JEP 309</a>
 * @since 6.3
 */
public final class ConstantDynamic extends ConstantCP {

    /**
     * Initialize from another object.
     */
    public ConstantDynamic(final ConstantDynamic c) {
        this(c.getBootstrapMethodAttrIndex(), c.getNameAndTypeIndex());
    }


    /**
     * Initialize instance from file data.
     *
     * @param file Input stream
     * @throws IOException
     */
    ConstantDynamic(final DataInput file) throws IOException {
        this(file.readShort(), file.readShort());
    }


    public ConstantDynamic(final int bootstrap_method_attr_index, final int name_and_type_index) {
        super(Const.CONSTANT_Dynamic, bootstrap_method_attr_index, name_and_type_index);
    }


    /**
     * Called by objects that are traversing the nodes of the tree implicitly
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitConstantDynamic(this);
    }

    /**
     * @return Reference (index) to bootstrap method this constant refers to.
     *
     * Note that this method is a functional duplicate of getClassIndex
     * for use by ConstantInvokeDynamic.
     * @since 6.0
     */
    public int getBootstrapMethodAttrIndex() {
        return super.getClassIndex();  // AKA bootstrap_method_attr_index
    }

    /**
     * @return String representation
     */
    @Override
    public String toString() {
        return super.toString().replace("class_index", "bootstrap_method_attr_index");
    }
}
