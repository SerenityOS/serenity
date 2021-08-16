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
 * This class is derived from the abstract {@link Constant}
 * and represents a reference to a invoke dynamic.
 *
 * @see     Constant
 * @see  <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4.10">
 * The CONSTANT_InvokeDynamic_info Structure in The Java Virtual Machine Specification</a>
 * @since 6.0
 */
public final class ConstantInvokeDynamic extends ConstantCP {

    /**
     * Initialize from another object.
     */
    public ConstantInvokeDynamic(final ConstantInvokeDynamic c) {
        this(c.getBootstrapMethodAttrIndex(), c.getNameAndTypeIndex());
    }


    /**
     * Initialize instance from file data.
     *
     * @param file Input stream
     * @throws IOException
     */
    ConstantInvokeDynamic(final DataInput file) throws IOException {
        this(file.readShort(), file.readShort());
    }


    public ConstantInvokeDynamic(final int bootstrap_method_attr_index, final int name_and_type_index) {
        super(Const.CONSTANT_InvokeDynamic, bootstrap_method_attr_index, name_and_type_index);
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
        v.visitConstantInvokeDynamic(this);
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
