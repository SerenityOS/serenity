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
import java.util.Arrays;

import com.sun.org.apache.bcel.internal.Const;

/**
 * This class represents a bootstrap method attribute, i.e., the bootstrap
 * method ref, the number of bootstrap arguments and an array of the
 * bootstrap arguments.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.23">
 * The class File Format : The BootstrapMethods Attribute</a>
 * @since 6.0
 */
public class BootstrapMethod implements Cloneable {

    /** Index of the CONSTANT_MethodHandle_info structure in the constant_pool table */
    private int bootstrapMethodRef;

    /** Array of references to the constant_pool table */
    private int[] bootstrapArguments;


    /**
     * Initialize from another object.
     */
    public BootstrapMethod(final BootstrapMethod c) {
        this(c.getBootstrapMethodRef(), c.getBootstrapArguments());
    }

    /**
     * Construct object from input stream.
     *
     * @param input Input stream
     * @throws IOException
     */
    BootstrapMethod(final DataInput input) throws IOException {
        this(input.readUnsignedShort(), input.readUnsignedShort());

        for (int i = 0; i < bootstrapArguments.length; i++) {
            bootstrapArguments[i] = input.readUnsignedShort();
        }
    }

    // helper method
    private BootstrapMethod(final int bootstrap_method_ref, final int num_bootstrap_arguments) {
        this(bootstrap_method_ref, new int[num_bootstrap_arguments]);
    }

    /**
     * @param bootstrapMethodRef int index into constant_pool of CONSTANT_MethodHandle
     * @param bootstrapArguments int[] indices into constant_pool of CONSTANT_[type]_info
     */
    public BootstrapMethod(final int bootstrapMethodRef, final int[] bootstrapArguments) {
        this.bootstrapMethodRef = bootstrapMethodRef;
        this.bootstrapArguments = bootstrapArguments;
    }

    /**
     * @return index into constant_pool of bootstrap_method
     */
    public int getBootstrapMethodRef() {
        return bootstrapMethodRef;
    }

    /**
     * @param bootstrapMethodRef int index into constant_pool of CONSTANT_MethodHandle
     */
    public void setBootstrapMethodRef(final int bootstrapMethodRef) {
        this.bootstrapMethodRef = bootstrapMethodRef;
    }

    /**
     * @return int[] of bootstrap_method indices into constant_pool of CONSTANT_[type]_info
     */
    public int[] getBootstrapArguments() {
        return bootstrapArguments;
    }

    /**
     * @return count of number of boostrap arguments
     */
    public int getNumBootstrapArguments() {
        return bootstrapArguments.length;
    }

    /**
     * @param bootstrapArguments int[] indices into constant_pool of CONSTANT_[type]_info
     */
    public void setBootstrapArguments(final int[] bootstrapArguments) {
        this.bootstrapArguments = bootstrapArguments;
    }

    /**
     * @return String representation.
     */
    @Override
    public final String toString() {
        return "BootstrapMethod(" + bootstrapMethodRef + ", " + bootstrapArguments.length + ", "
               + Arrays.toString(bootstrapArguments) + ")";
    }

    /**
     * @return Resolved string representation
     */
    public final String toString( final ConstantPool constantPool ) {
        final StringBuilder buf = new StringBuilder();
        String bootstrap_method_name;
        bootstrap_method_name = constantPool.constantToString(bootstrapMethodRef,
                Const.CONSTANT_MethodHandle);
        buf.append(Utility.compactClassName(bootstrap_method_name, false));
        final int num_bootstrap_arguments = bootstrapArguments.length;
        if (num_bootstrap_arguments > 0) {
            buf.append("\nMethod Arguments:");
            for (int i = 0; i < num_bootstrap_arguments; i++) {
                buf.append("\n  ").append(i).append(": ");
                buf.append(constantPool.constantToString(constantPool.getConstant(bootstrapArguments[i])));
            }
        }
        return buf.toString();
    }

    /**
     * Dump object to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    public final void dump(final DataOutputStream file) throws IOException {
        file.writeShort(bootstrapMethodRef);
        file.writeShort(bootstrapArguments.length);
        for (final int bootstrap_argument : bootstrapArguments) {
            file.writeShort(bootstrap_argument);
        }
    }

    /**
     * @return deep copy of this object
     */
    public BootstrapMethod copy() {
        try {
            return (BootstrapMethod) clone();
        } catch (final CloneNotSupportedException e) {
            // TODO should this throw?
        }
        return null;
    }
}
