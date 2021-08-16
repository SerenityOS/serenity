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

import java.util.HashMap;
import java.util.Map;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.classfile.Constant;
import com.sun.org.apache.bcel.internal.classfile.ConstantCP;
import com.sun.org.apache.bcel.internal.classfile.ConstantClass;
import com.sun.org.apache.bcel.internal.classfile.ConstantDouble;
import com.sun.org.apache.bcel.internal.classfile.ConstantFieldref;
import com.sun.org.apache.bcel.internal.classfile.ConstantFloat;
import com.sun.org.apache.bcel.internal.classfile.ConstantInteger;
import com.sun.org.apache.bcel.internal.classfile.ConstantInterfaceMethodref;
import com.sun.org.apache.bcel.internal.classfile.ConstantInvokeDynamic;
import com.sun.org.apache.bcel.internal.classfile.ConstantLong;
import com.sun.org.apache.bcel.internal.classfile.ConstantMethodref;
import com.sun.org.apache.bcel.internal.classfile.ConstantNameAndType;
import com.sun.org.apache.bcel.internal.classfile.ConstantPool;
import com.sun.org.apache.bcel.internal.classfile.ConstantString;
import com.sun.org.apache.bcel.internal.classfile.ConstantUtf8;

/**
 * This class is used to build up a constant pool. The user adds
 * constants via `addXXX' methods, `addString', `addClass',
 * etc.. These methods return an index into the constant
 * pool. Finally, `getFinalConstantPool()' returns the constant pool
 * built up. Intermediate versions of the constant pool can be
 * obtained with `getConstantPool()'. A constant pool has capacity for
 * Constants.MAX_SHORT entries. Note that the first (0) is used by the
 * JVM and that Double and Long constants need two slots.
 *
 * @see Constant
 * @LastModified: May 2021
 */
public class ConstantPoolGen {

    private static final int DEFAULT_BUFFER_SIZE = 256;
    private int size;
    private Constant[] constants;
    private int index = 1; // First entry (0) used by JVM

    private static final String METHODREF_DELIM = ":";
    private static final String IMETHODREF_DELIM = "#";
    private static final String FIELDREF_DELIM = "&";
    private static final String NAT_DELIM = "%"; // Name and Type

    private static class Index {

        final int index;


        Index(final int i) {
            index = i;
        }
    }


    /**
     * Initialize with given array of constants.
     *
     * @param cs array of given constants, new ones will be appended
     */
    public ConstantPoolGen(final Constant[] cs) {
        final StringBuilder sb = new StringBuilder(DEFAULT_BUFFER_SIZE);

        size = Math.max(DEFAULT_BUFFER_SIZE, cs.length + 64);
        constants = new Constant[size];

        System.arraycopy(cs, 0, constants, 0, cs.length);
        if (cs.length > 0) {
            index = cs.length;
        }


        for (int i = 1; i < index; i++) {
            final Constant c = constants[i];
            if (c instanceof ConstantString) {
                final ConstantString s = (ConstantString) c;
                final ConstantUtf8 u8 = (ConstantUtf8) constants[s.getStringIndex()];
                final String key = u8.getBytes();
                if (!stringTable.containsKey(key)) {
                    stringTable.put(key, new Index(i));
                }
            } else if (c instanceof ConstantClass) {
                final ConstantClass s = (ConstantClass) c;
                final ConstantUtf8 u8 = (ConstantUtf8) constants[s.getNameIndex()];
                final String key = u8.getBytes();
                if (!classTable.containsKey(key)) {
                    classTable.put(key, new Index(i));
                }
            } else if (c instanceof ConstantNameAndType) {
                final ConstantNameAndType n = (ConstantNameAndType) c;
                final ConstantUtf8 u8 = (ConstantUtf8) constants[n.getNameIndex()];
                final ConstantUtf8 u8_2 = (ConstantUtf8) constants[n.getSignatureIndex()];

                sb.append(u8.getBytes());
                sb.append(NAT_DELIM);
                sb.append(u8_2.getBytes());
                final String key = sb.toString();
                sb.delete(0, sb.length());

                if (!natTable.containsKey(key)) {
                    natTable.put(key, new Index(i));
                }
            } else if (c instanceof ConstantUtf8) {
                final ConstantUtf8 u = (ConstantUtf8) c;
                final String key = u.getBytes();
                if (!utf8Table.containsKey(key)) {
                    utf8Table.put(key, new Index(i));
                }
            } else if (c instanceof ConstantCP) {
                final ConstantCP m = (ConstantCP) c;
                String class_name;
                ConstantUtf8 u8;

                if (c instanceof ConstantInvokeDynamic) {
                    class_name = Integer.toString(((ConstantInvokeDynamic) m).getBootstrapMethodAttrIndex());
                    // since name can't begin with digit, can  use
                    // METHODREF_DELIM with out fear of duplicates.
                } else {
                final ConstantClass clazz = (ConstantClass) constants[m.getClassIndex()];
                    u8 = (ConstantUtf8) constants[clazz.getNameIndex()];
                    class_name = u8.getBytes().replace('/', '.');
                }

                final ConstantNameAndType n = (ConstantNameAndType) constants[m.getNameAndTypeIndex()];
                u8 = (ConstantUtf8) constants[n.getNameIndex()];
                final String method_name = u8.getBytes();
                u8 = (ConstantUtf8) constants[n.getSignatureIndex()];
                final String signature = u8.getBytes();

                String delim = METHODREF_DELIM;
                if (c instanceof ConstantInterfaceMethodref) {
                    delim = IMETHODREF_DELIM;
                } else if (c instanceof ConstantFieldref) {
                    delim = FIELDREF_DELIM;
                }

                sb.append(class_name);
                sb.append(delim);
                sb.append(method_name);
                sb.append(delim);
                sb.append(signature);
                final String key = sb.toString();
                sb.delete(0, sb.length());

                if (!cpTable.containsKey(key)) {
                    cpTable.put(key, new Index(i));
                }
            } else if (c == null) { // entries may be null
                // nothing to do
            } else if (c instanceof ConstantInteger) {
                // nothing to do
            } else if (c instanceof ConstantLong) {
                // nothing to do
            } else if (c instanceof ConstantFloat) {
                // nothing to do
            } else if (c instanceof ConstantDouble) {
                // nothing to do
            } else if (c instanceof com.sun.org.apache.bcel.internal.classfile.ConstantMethodType) {
                // TODO should this be handled somehow?
            } else if (c instanceof com.sun.org.apache.bcel.internal.classfile.ConstantMethodHandle) {
                // TODO should this be handled somehow?
            } else if (c instanceof com.sun.org.apache.bcel.internal.classfile.ConstantModule) {
                // TODO should this be handled somehow?
            } else if (c instanceof com.sun.org.apache.bcel.internal.classfile.ConstantPackage) {
                // TODO should this be handled somehow?
            } else {
                assert false : "Unexpected constant type: " + c.getClass().getName();
            }
        }
    }


    /**
     * Initialize with given constant pool.
     */
    public ConstantPoolGen(final ConstantPool cp) {
        this(cp.getConstantPool());
    }


    /**
     * Create empty constant pool.
     */
    public ConstantPoolGen() {
        size = DEFAULT_BUFFER_SIZE;
        constants = new Constant[size];
    }


    /** Resize internal array of constants.
     */
    protected void adjustSize() {
        if (index + 3 >= size) {
            final Constant[] cs = constants;
            size *= 2;
            constants = new Constant[size];
            System.arraycopy(cs, 0, constants, 0, index);
        }
    }

    private final Map<String, Index> stringTable = new HashMap<>();


    /**
     * Look for ConstantString in ConstantPool containing String `str'.
     *
     * @param str String to search for
     * @return index on success, -1 otherwise
     */
    public int lookupString( final String str ) {
        final Index index = stringTable.get(str);
        return (index != null) ? index.index : -1;
    }


    /**
     * Add a new String constant to the ConstantPool, if it is not already in there.
     *
     * @param str String to add
     * @return index of entry
     */
    public int addString( final String str ) {
        int ret;
        if ((ret = lookupString(str)) != -1) {
            return ret; // Already in CP
        }
        final int utf8 = addUtf8(str);
        adjustSize();
        final ConstantString s = new ConstantString(utf8);
        ret = index;
        constants[index++] = s;
        if (!stringTable.containsKey(str)) {
            stringTable.put(str, new Index(ret));
        }
        return ret;
    }

    private final Map<String, Index> classTable = new HashMap<>();


    /**
     * Look for ConstantClass in ConstantPool named `str'.
     *
     * @param str String to search for
     * @return index on success, -1 otherwise
     */
    public int lookupClass( final String str ) {
        final Index index = classTable.get(str.replace('.', '/'));
        return (index != null) ? index.index : -1;
    }


    private int addClass_( final String clazz ) {
        int ret;
        if ((ret = lookupClass(clazz)) != -1) {
            return ret; // Already in CP
        }
        adjustSize();
        final ConstantClass c = new ConstantClass(addUtf8(clazz));
        ret = index;
        constants[index++] = c;
        if (!classTable.containsKey(clazz)) {
            classTable.put(clazz, new Index(ret));
        }
        return ret;
    }


    /**
     * Add a new Class reference to the ConstantPool, if it is not already in there.
     *
     * @param str Class to add
     * @return index of entry
     */
    public int addClass( final String str ) {
        return addClass_(str.replace('.', '/'));
    }


    /**
     * Add a new Class reference to the ConstantPool for a given type.
     *
     * @param type Class to add
     * @return index of entry
     */
    public int addClass( final ObjectType type ) {
        return addClass(type.getClassName());
    }


    /**
     * Add a reference to an array class (e.g. String[][]) as needed by MULTIANEWARRAY
     * instruction, e.g. to the ConstantPool.
     *
     * @param type type of array class
     * @return index of entry
     */
    public int addArrayClass( final ArrayType type ) {
        return addClass_(type.getSignature());
    }


    /**
     * Look for ConstantInteger in ConstantPool.
     *
     * @param n integer number to look for
     * @return index on success, -1 otherwise
     */
    public int lookupInteger( final int n ) {
        for (int i = 1; i < index; i++) {
            if (constants[i] instanceof ConstantInteger) {
                final ConstantInteger c = (ConstantInteger) constants[i];
                if (c.getBytes() == n) {
                    return i;
                }
            }
        }
        return -1;
    }


    /**
     * Add a new Integer constant to the ConstantPool, if it is not already in there.
     *
     * @param n integer number to add
     * @return index of entry
     */
    public int addInteger( final int n ) {
        int ret;
        if ((ret = lookupInteger(n)) != -1) {
            return ret; // Already in CP
        }
        adjustSize();
        ret = index;
        constants[index++] = new ConstantInteger(n);
        return ret;
    }


    /**
     * Look for ConstantFloat in ConstantPool.
     *
     * @param n Float number to look for
     * @return index on success, -1 otherwise
     */
    public int lookupFloat( final float n ) {
        final int bits = Float.floatToIntBits(n);
        for (int i = 1; i < index; i++) {
            if (constants[i] instanceof ConstantFloat) {
                final ConstantFloat c = (ConstantFloat) constants[i];
                if (Float.floatToIntBits(c.getBytes()) == bits) {
                    return i;
                }
            }
        }
        return -1;
    }


    /**
     * Add a new Float constant to the ConstantPool, if it is not already in there.
     *
     * @param n Float number to add
     * @return index of entry
     */
    public int addFloat( final float n ) {
        int ret;
        if ((ret = lookupFloat(n)) != -1) {
            return ret; // Already in CP
        }
        adjustSize();
        ret = index;
        constants[index++] = new ConstantFloat(n);
        return ret;
    }

    private final Map<String, Index> utf8Table = new HashMap<>();


    /**
     * Look for ConstantUtf8 in ConstantPool.
     *
     * @param n Utf8 string to look for
     * @return index on success, -1 otherwise
     */
    public int lookupUtf8( final String n ) {
        final Index index = utf8Table.get(n);
        return (index != null) ? index.index : -1;
    }


    /**
     * Add a new Utf8 constant to the ConstantPool, if it is not already in there.
     *
     * @param n Utf8 string to add
     * @return index of entry
     */
    public int addUtf8( final String n ) {
        int ret;
        if ((ret = lookupUtf8(n)) != -1) {
            return ret; // Already in CP
        }
        adjustSize();
        ret = index;
        constants[index++] = new ConstantUtf8(n);
        if (!utf8Table.containsKey(n)) {
            utf8Table.put(n, new Index(ret));
        }
        return ret;
    }


    /**
     * Look for ConstantLong in ConstantPool.
     *
     * @param n Long number to look for
     * @return index on success, -1 otherwise
     */
    public int lookupLong( final long n ) {
        for (int i = 1; i < index; i++) {
            if (constants[i] instanceof ConstantLong) {
                final ConstantLong c = (ConstantLong) constants[i];
                if (c.getBytes() == n) {
                    return i;
                }
            }
        }
        return -1;
    }


    /**
     * Add a new long constant to the ConstantPool, if it is not already in there.
     *
     * @param n Long number to add
     * @return index of entry
     */
    public int addLong( final long n ) {
        int ret;
        if ((ret = lookupLong(n)) != -1) {
            return ret; // Already in CP
        }
        adjustSize();
        ret = index;
        constants[index] = new ConstantLong(n);
        index += 2; // Wastes one entry according to spec
        return ret;
    }


    /**
     * Look for ConstantDouble in ConstantPool.
     *
     * @param n Double number to look for
     * @return index on success, -1 otherwise
     */
    public int lookupDouble( final double n ) {
        final long bits = Double.doubleToLongBits(n);
        for (int i = 1; i < index; i++) {
            if (constants[i] instanceof ConstantDouble) {
                final ConstantDouble c = (ConstantDouble) constants[i];
                if (Double.doubleToLongBits(c.getBytes()) == bits) {
                    return i;
                }
            }
        }
        return -1;
    }


    /**
     * Add a new double constant to the ConstantPool, if it is not already in there.
     *
     * @param n Double number to add
     * @return index of entry
     */
    public int addDouble( final double n ) {
        int ret;
        if ((ret = lookupDouble(n)) != -1) {
            return ret; // Already in CP
        }
        adjustSize();
        ret = index;
        constants[index] = new ConstantDouble(n);
        index += 2; // Wastes one entry according to spec
        return ret;
    }

    private final Map<String, Index> natTable = new HashMap<>();


    /**
     * Look for ConstantNameAndType in ConstantPool.
     *
     * @param name of variable/method
     * @param signature of variable/method
     * @return index on success, -1 otherwise
     */
    public int lookupNameAndType( final String name, final String signature ) {
        final Index _index = natTable.get(name + NAT_DELIM + signature);
        return (_index != null) ? _index.index : -1;
    }


    /**
     * Add a new NameAndType constant to the ConstantPool if it is not already
     * in there.
     *
     * @param name Name string to add
     * @param signature signature string to add
     * @return index of entry
     */
    public int addNameAndType( final String name, final String signature ) {
        int ret;
        int name_index;
        int signature_index;
        if ((ret = lookupNameAndType(name, signature)) != -1) {
            return ret; // Already in CP
        }
        adjustSize();
        name_index = addUtf8(name);
        signature_index = addUtf8(signature);
        ret = index;
        constants[index++] = new ConstantNameAndType(name_index, signature_index);
        final String key = name + NAT_DELIM + signature;
        if (!natTable.containsKey(key)) {
            natTable.put(key, new Index(ret));
        }
        return ret;
    }

    private final Map<String, Index> cpTable = new HashMap<>();


    /**
     * Look for ConstantMethodref in ConstantPool.
     *
     * @param class_name Where to find method
     * @param method_name Guess what
     * @param signature return and argument types
     * @return index on success, -1 otherwise
     */
    public int lookupMethodref( final String class_name, final String method_name, final String signature ) {
        final Index index = cpTable.get(class_name + METHODREF_DELIM + method_name
                + METHODREF_DELIM + signature);
        return (index != null) ? index.index : -1;
    }


    public int lookupMethodref( final MethodGen method ) {
        return lookupMethodref(method.getClassName(), method.getName(), method.getSignature());
    }


    /**
     * Add a new Methodref constant to the ConstantPool, if it is not already
     * in there.
     *
     * @param class_name class name string to add
     * @param method_name method name string to add
     * @param signature method signature string to add
     * @return index of entry
     */
    public int addMethodref( final String class_name, final String method_name, final String signature ) {
        int ret;
        int class_index;
        int name_and_type_index;
        if ((ret = lookupMethodref(class_name, method_name, signature)) != -1) {
            return ret; // Already in CP
        }
        adjustSize();
        name_and_type_index = addNameAndType(method_name, signature);
        class_index = addClass(class_name);
        ret = index;
        constants[index++] = new ConstantMethodref(class_index, name_and_type_index);
        final String key = class_name + METHODREF_DELIM + method_name + METHODREF_DELIM + signature;
        if (!cpTable.containsKey(key)) {
            cpTable.put(key, new Index(ret));
        }
        return ret;
    }


    public int addMethodref( final MethodGen method ) {
        return addMethodref(method.getClassName(), method.getName(), method.getSignature());
    }


    /**
     * Look for ConstantInterfaceMethodref in ConstantPool.
     *
     * @param class_name Where to find method
     * @param method_name Guess what
     * @param signature return and argument types
     * @return index on success, -1 otherwise
     */
    public int lookupInterfaceMethodref( final String class_name, final String method_name, final String signature ) {
        final Index index = cpTable.get(class_name + IMETHODREF_DELIM + method_name
                + IMETHODREF_DELIM + signature);
        return (index != null) ? index.index : -1;
    }


    public int lookupInterfaceMethodref( final MethodGen method ) {
        return lookupInterfaceMethodref(method.getClassName(), method.getName(), method
                .getSignature());
    }


    /**
     * Add a new InterfaceMethodref constant to the ConstantPool, if it is not already
     * in there.
     *
     * @param class_name class name string to add
     * @param method_name method name string to add
     * @param signature signature string to add
     * @return index of entry
     */
    public int addInterfaceMethodref( final String class_name, final String method_name, final String signature ) {
        int ret;
        int class_index;
        int name_and_type_index;
        if ((ret = lookupInterfaceMethodref(class_name, method_name, signature)) != -1) {
            return ret; // Already in CP
        }
        adjustSize();
        class_index = addClass(class_name);
        name_and_type_index = addNameAndType(method_name, signature);
        ret = index;
        constants[index++] = new ConstantInterfaceMethodref(class_index, name_and_type_index);
        final String key = class_name + IMETHODREF_DELIM + method_name + IMETHODREF_DELIM + signature;
        if (!cpTable.containsKey(key)) {
            cpTable.put(key, new Index(ret));
        }
        return ret;
    }


    public int addInterfaceMethodref( final MethodGen method ) {
        return addInterfaceMethodref(method.getClassName(), method.getName(), method.getSignature());
    }


    /**
     * Look for ConstantFieldref in ConstantPool.
     *
     * @param class_name Where to find method
     * @param field_name Guess what
     * @param signature return and argument types
     * @return index on success, -1 otherwise
     */
    public int lookupFieldref( final String class_name, final String field_name, final String signature ) {
        final Index index = cpTable.get(class_name + FIELDREF_DELIM + field_name
                + FIELDREF_DELIM + signature);
        return (index != null) ? index.index : -1;
    }


    /**
     * Add a new Fieldref constant to the ConstantPool, if it is not already
     * in there.
     *
     * @param class_name class name string to add
     * @param field_name field name string to add
     * @param signature signature string to add
     * @return index of entry
     */
    public int addFieldref( final String class_name, final String field_name, final String signature ) {
        int ret;
        int class_index;
        int name_and_type_index;
        if ((ret = lookupFieldref(class_name, field_name, signature)) != -1) {
            return ret; // Already in CP
        }
        adjustSize();
        class_index = addClass(class_name);
        name_and_type_index = addNameAndType(field_name, signature);
        ret = index;
        constants[index++] = new ConstantFieldref(class_index, name_and_type_index);
        final String key = class_name + FIELDREF_DELIM + field_name + FIELDREF_DELIM + signature;
        if (!cpTable.containsKey(key)) {
            cpTable.put(key, new Index(ret));
        }
        return ret;
    }


    /**
     * @param i index in constant pool
     * @return constant pool entry at index i
     */
    public Constant getConstant( final int i ) {
        return constants[i];
    }


    /**
     * Use with care!
     *
     * @param i index in constant pool
     * @param c new constant pool entry at index i
     */
    public void setConstant( final int i, final Constant c ) {
        constants[i] = c;
    }


    /**
     * @return intermediate constant pool
     */
    public ConstantPool getConstantPool() {
        return new ConstantPool(constants);
    }


    /**
     * @return current size of constant pool
     */
    public int getSize() {
        return index;
    }


    /**
     * @return constant pool with proper length
     */
    public ConstantPool getFinalConstantPool() {
        final Constant[] cs = new Constant[index];
        System.arraycopy(constants, 0, cs, 0, index);
        return new ConstantPool(cs);
    }


    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        for (int i = 1; i < index; i++) {
            buf.append(i).append(")").append(constants[i]).append("\n");
        }
        return buf.toString();
    }


    /** Import constant from another ConstantPool and return new index.
     */
    public int addConstant( final Constant c, final ConstantPoolGen cp ) {
        final Constant[] constants = cp.getConstantPool().getConstantPool();
        switch (c.getTag()) {
            case Const.CONSTANT_String: {
                final ConstantString s = (ConstantString) c;
                final ConstantUtf8 u8 = (ConstantUtf8) constants[s.getStringIndex()];
                return addString(u8.getBytes());
            }
            case Const.CONSTANT_Class: {
                final ConstantClass s = (ConstantClass) c;
                final ConstantUtf8 u8 = (ConstantUtf8) constants[s.getNameIndex()];
                return addClass(u8.getBytes());
            }
            case Const.CONSTANT_NameAndType: {
                final ConstantNameAndType n = (ConstantNameAndType) c;
                final ConstantUtf8 u8 = (ConstantUtf8) constants[n.getNameIndex()];
                final ConstantUtf8 u8_2 = (ConstantUtf8) constants[n.getSignatureIndex()];
                return addNameAndType(u8.getBytes(), u8_2.getBytes());
            }
            case Const.CONSTANT_Utf8:
                return addUtf8(((ConstantUtf8) c).getBytes());
            case Const.CONSTANT_Double:
                return addDouble(((ConstantDouble) c).getBytes());
            case Const.CONSTANT_Float:
                return addFloat(((ConstantFloat) c).getBytes());
            case Const.CONSTANT_Long:
                return addLong(((ConstantLong) c).getBytes());
            case Const.CONSTANT_Integer:
                return addInteger(((ConstantInteger) c).getBytes());
            case Const.CONSTANT_InterfaceMethodref:
            case Const.CONSTANT_Methodref:
            case Const.CONSTANT_Fieldref: {
                final ConstantCP m = (ConstantCP) c;
                final ConstantClass clazz = (ConstantClass) constants[m.getClassIndex()];
                final ConstantNameAndType n = (ConstantNameAndType) constants[m.getNameAndTypeIndex()];
                ConstantUtf8 u8 = (ConstantUtf8) constants[clazz.getNameIndex()];
                final String class_name = u8.getBytes().replace('/', '.');
                u8 = (ConstantUtf8) constants[n.getNameIndex()];
                final String name = u8.getBytes();
                u8 = (ConstantUtf8) constants[n.getSignatureIndex()];
                final String signature = u8.getBytes();
                switch (c.getTag()) {
                    case Const.CONSTANT_InterfaceMethodref:
                        return addInterfaceMethodref(class_name, name, signature);
                    case Const.CONSTANT_Methodref:
                        return addMethodref(class_name, name, signature);
                    case Const.CONSTANT_Fieldref:
                        return addFieldref(class_name, name, signature);
                    default: // Never reached
                        throw new IllegalArgumentException("Unknown constant type " + c);
                }
            }
            default: // Never reached
                throw new IllegalArgumentException("Unknown constant type " + c);
        }
    }
}
