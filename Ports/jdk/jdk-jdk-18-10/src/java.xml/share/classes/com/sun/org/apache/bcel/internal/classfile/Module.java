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
 * This class is derived from <em>Attribute</em> and represents the list of
 * modules required, exported, opened or provided by a module.
 * There may be at most one Module attribute in a ClassFile structure.
 *
 * @see   Attribute
 * @since 6.4.0
 */
public final class Module extends Attribute {

    private final int moduleNameIndex;
    private final int moduleFlags;
    private final int moduleVersionIndex;

    private ModuleRequires[] requiresTable;
    private ModuleExports[] exportsTable;
    private ModuleOpens[] opensTable;
    private final int usesCount;
    private final int[] usesIndex;
    private ModuleProvides[] providesTable;

    /**
     * Construct object from input stream.
     * @param name_index Index in constant pool
     * @param length Content length in bytes
     * @param input Input stream
     * @param constant_pool Array of constants
     * @throws IOException
     */
    Module(final int name_index, final int length, final DataInput input, final ConstantPool constant_pool) throws IOException {
        super(Const.ATTR_MODULE, name_index, length, constant_pool);

        moduleNameIndex = input.readUnsignedShort();
        moduleFlags = input.readUnsignedShort();
        moduleVersionIndex = input.readUnsignedShort();

        final int requires_count = input.readUnsignedShort();
        requiresTable = new ModuleRequires[requires_count];
        for (int i = 0; i < requires_count; i++) {
            requiresTable[i] = new ModuleRequires(input);
        }

        final int exports_count = input.readUnsignedShort();
        exportsTable = new ModuleExports[exports_count];
        for (int i = 0; i < exports_count; i++) {
            exportsTable[i] = new ModuleExports(input);
        }

        final int opens_count = input.readUnsignedShort();
        opensTable = new ModuleOpens[opens_count];
        for (int i = 0; i < opens_count; i++) {
            opensTable[i] = new ModuleOpens(input);
        }

        usesCount = input.readUnsignedShort();
        usesIndex = new int[usesCount];
        for (int i = 0; i < usesCount; i++) {
            usesIndex[i] = input.readUnsignedShort();
        }

        final int provides_count = input.readUnsignedShort();
        providesTable = new ModuleProvides[provides_count];
        for (int i = 0; i < provides_count; i++) {
            providesTable[i] = new ModuleProvides(input);
        }
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
        v.visitModule(this);
    }

    // TODO add more getters and setters?

    /**
     * @return table of required modules
     * @see ModuleRequires
     */
    public ModuleRequires[] getRequiresTable() {
        return requiresTable;
    }


    /**
     * @return table of exported interfaces
     * @see ModuleExports
     */
    public ModuleExports[] getExportsTable() {
        return exportsTable;
    }


    /**
     * @return table of provided interfaces
     * @see ModuleOpens
     */
    public ModuleOpens[] getOpensTable() {
        return opensTable;
    }


    /**
     * @return table of provided interfaces
     * @see ModuleProvides
     */
    public ModuleProvides[] getProvidesTable() {
        return providesTable;
    }


    /**
     * Dump Module attribute to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    @Override
    public void dump( final DataOutputStream file ) throws IOException {
        super.dump(file);

        file.writeShort(moduleNameIndex);
        file.writeShort(moduleFlags);
        file.writeShort(moduleVersionIndex);

        file.writeShort(requiresTable.length);
        for (final ModuleRequires entry : requiresTable) {
            entry.dump(file);
        }

        file.writeShort(exportsTable.length);
        for (final ModuleExports entry : exportsTable) {
            entry.dump(file);
        }

        file.writeShort(opensTable.length);
        for (final ModuleOpens entry : opensTable) {
            entry.dump(file);
        }

        file.writeShort(usesIndex.length);
        for (final int entry : usesIndex) {
            file.writeShort(entry);
        }

        file.writeShort(providesTable.length);
        for (final ModuleProvides entry : providesTable) {
            entry.dump(file);
        }
    }


    /**
     * @return String representation, i.e., a list of packages.
     */
    @Override
    public String toString() {
        final ConstantPool cp = super.getConstantPool();
        final StringBuilder buf = new StringBuilder();
        buf.append("Module:\n");
        buf.append("  name:    ") .append(cp.getConstantString(moduleNameIndex, Const.CONSTANT_Module).replace('/', '.')).append("\n");
        buf.append("  flags:   ") .append(String.format("%04x", moduleFlags)).append("\n");
        final String version = moduleVersionIndex == 0 ? "0" : cp.getConstantString(moduleVersionIndex, Const.CONSTANT_Utf8);
        buf.append("  version: ") .append(version).append("\n");

        buf.append("  requires(").append(requiresTable.length).append("):\n");
        for (final ModuleRequires module : requiresTable) {
            buf.append("    ").append(module.toString(cp)).append("\n");
        }

        buf.append("  exports(").append(exportsTable.length).append("):\n");
        for (final ModuleExports module : exportsTable) {
            buf.append("    ").append(module.toString(cp)).append("\n");
        }

        buf.append("  opens(").append(opensTable.length).append("):\n");
        for (final ModuleOpens module : opensTable) {
            buf.append("    ").append(module.toString(cp)).append("\n");
        }

        buf.append("  uses(").append(usesIndex.length).append("):\n");
        for (final int index : usesIndex) {
            final String class_name = cp.getConstantString(index, Const.CONSTANT_Class);
            buf.append("    ").append(Utility.compactClassName(class_name, false)).append("\n");
        }

        buf.append("  provides(").append(providesTable.length).append("):\n");
        for (final ModuleProvides module : providesTable) {
            buf.append("    ").append(module.toString(cp)).append("\n");
        }

        return buf.substring(0, buf.length()-1); // remove the last newline
    }


    /**
     * @return deep copy of this attribute
     */
    @Override
    public Attribute copy( final ConstantPool _constant_pool ) {
        final Module c = (Module) clone();

        c.requiresTable = new ModuleRequires[requiresTable.length];
        for (int i = 0; i < requiresTable.length; i++) {
            c.requiresTable[i] = requiresTable[i].copy();
        }

        c.exportsTable = new ModuleExports[exportsTable.length];
        for (int i = 0; i < exportsTable.length; i++) {
            c.exportsTable[i] = exportsTable[i].copy();
        }

        c.opensTable = new ModuleOpens[opensTable.length];
        for (int i = 0; i < opensTable.length; i++) {
            c.opensTable[i] = opensTable[i].copy();
        }

        c.providesTable = new ModuleProvides[providesTable.length];
        for (int i = 0; i < providesTable.length; i++) {
            c.providesTable[i] = providesTable[i].copy();
        }

        c.setConstantPool(_constant_pool);
        return c;
    }
}
