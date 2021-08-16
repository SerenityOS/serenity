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
 * This class is derived from <em>Attribute</em> and represents the list of packages that are exported or opened by the Module attribute.
 * There may be at most one ModulePackages attribute in a ClassFile structure.
 *
 * @see     Attribute
 */
public final class ModulePackages extends Attribute {

    private int[] packageIndexTable;


    /**
     * Initialize from another object. Note that both objects use the same
     * references (shallow copy). Use copy() for a physical copy.
     */
    public ModulePackages(final ModulePackages c) {
        this(c.getNameIndex(), c.getLength(), c.getPackageIndexTable(), c.getConstantPool());
    }


    /**
     * @param nameIndex Index in constant pool
     * @param length Content length in bytes
     * @param packageIndexTable Table of indices in constant pool
     * @param constantPool Array of constants
     */
    public ModulePackages(final int nameIndex, final int length, final int[] packageIndexTable,
            final ConstantPool constantPool) {
        super(Const.ATTR_MODULE_PACKAGES, nameIndex, length, constantPool);
        this.packageIndexTable = packageIndexTable != null ? packageIndexTable : new int[0];
    }


    /**
     * Construct object from input stream.
     * @param name_index Index in constant pool
     * @param length Content length in bytes
     * @param input Input stream
     * @param constant_pool Array of constants
     * @throws IOException
     */
    ModulePackages(final int name_index, final int length, final DataInput input, final ConstantPool constant_pool) throws IOException {
        this(name_index, length, (int[]) null, constant_pool);
        final int number_of_packages = input.readUnsignedShort();
        packageIndexTable = new int[number_of_packages];
        for (int i = 0; i < number_of_packages; i++) {
            packageIndexTable[i] = input.readUnsignedShort();
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
        v.visitModulePackages(this);
    }


    /**
     * Dump ModulePackages attribute to file stream in binary format.
     *
     * @param file Output file stream
     * @throws IOException
     */
    @Override
    public void dump( final DataOutputStream file ) throws IOException {
        super.dump(file);
        file.writeShort(packageIndexTable.length);
        for (final int index : packageIndexTable) {
            file.writeShort(index);
        }
    }


    /**
     * @return array of indices into constant pool of package names.
     */
    public int[] getPackageIndexTable() {
        return packageIndexTable;
    }


    /**
     * @return Length of package table.
     */
    public int getNumberOfPackages() {
        return packageIndexTable == null ? 0 : packageIndexTable.length;
    }


    /**
     * @return string array of package names
     */
    public String[] getPackageNames() {
        final String[] names = new String[packageIndexTable.length];
        for (int i = 0; i < packageIndexTable.length; i++) {
            names[i] = super.getConstantPool().getConstantString(packageIndexTable[i],
                    Const.CONSTANT_Package).replace('/', '.');
        }
        return names;
    }


    /**
     * @param packageIndexTable the list of package indexes
     * Also redefines number_of_packages according to table length.
     */
    public void setPackageIndexTable( final int[] packageIndexTable ) {
        this.packageIndexTable = packageIndexTable != null ? packageIndexTable : new int[0];
    }


    /**
     * @return String representation, i.e., a list of packages.
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("ModulePackages(");
        buf.append(packageIndexTable.length);
        buf.append("):\n");
        for (final int index : packageIndexTable) {
            final String package_name = super.getConstantPool().getConstantString(index, Const.CONSTANT_Package);
            buf.append("  ").append(Utility.compactClassName(package_name, false)).append("\n");
        }
        return buf.substring(0, buf.length()-1); // remove the last newline
    }


    /**
     * @return deep copy of this attribute
     */
    @Override
    public Attribute copy( final ConstantPool _constant_pool ) {
        final ModulePackages c = (ModulePackages) clone();
        if (packageIndexTable != null) {
            c.packageIndexTable = new int[packageIndexTable.length];
            System.arraycopy(packageIndexTable, 0, c.packageIndexTable, 0,
                    packageIndexTable.length);
        }
        c.setConstantPool(_constant_pool);
        return c;
    }
}
