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
 */

package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.Const;

/**
 * This class represents an entry in the requires table of the Module attribute.
 * Each entry describes a module on which the parent module depends.
 *
 * @see   Module
 * @since 6.4.0
 */
public final class ModuleRequires implements Cloneable, Node {

    private final int requiresIndex;  // points to CONSTANT_Module_info
    private final int requiresFlags;
    private final int requiresVersionIndex;  // either 0 or points to CONSTANT_Utf8_info


    /**
     * Construct object from file stream.
     *
     * @param file Input stream
     * @throws IOException if an I/O Exception occurs in readUnsignedShort
     */
    ModuleRequires(final DataInput file) throws IOException {
        requiresIndex = file.readUnsignedShort();
        requiresFlags = file.readUnsignedShort();
        requiresVersionIndex = file.readUnsignedShort();
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
        v.visitModuleRequires(this);
    }

    // TODO add more getters and setters?

    /**
     * Dump table entry to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException if an I/O Exception occurs in writeShort
     */
    public void dump( final DataOutputStream file ) throws IOException {
        file.writeShort(requiresIndex);
        file.writeShort(requiresFlags);
        file.writeShort(requiresVersionIndex);
    }


    /**
     * @return String representation
     */
    @Override
    public String toString() {
        return "requires(" + requiresIndex + ", " + String.format("%04x", requiresFlags) + ", " + requiresVersionIndex + ")";
    }


    /**
     * @return Resolved string representation
     */
    public String toString( final ConstantPool constant_pool ) {
        final StringBuilder buf = new StringBuilder();
        final String module_name = constant_pool.constantToString(requiresIndex, Const.CONSTANT_Module);
        buf.append(Utility.compactClassName(module_name, false));
        buf.append(", ").append(String.format("%04x", requiresFlags));
        final String version = requiresVersionIndex == 0 ? "0" : constant_pool.getConstantString(requiresVersionIndex, Const.CONSTANT_Utf8);
        buf.append(", ").append(version);
        return buf.toString();
    }


    /**
     * @return deep copy of this object
     */
    public ModuleRequires copy() {
        try {
            return (ModuleRequires) clone();
        } catch (final CloneNotSupportedException e) {
            // TODO should this throw?
        }
        return null;
    }
}
