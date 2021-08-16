/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package gc.stress.gcbasher;

class Decompiler {
    private ByteCursor cursor;
    private ClassInfo ci;

    public Decompiler(byte[] classData) {
        cursor = new ByteCursor(classData);

        int magicNumber = cursor.readInt();
        if (magicNumber != 0xCAFEBABE) {
            throw new IllegalArgumentException("Bad magic number " + magicNumber);
        }

        cursor.readUnsignedShort(); // Minor version
        cursor.readUnsignedShort(); // Major version

        ConstantPoolEntry[] constantPool = decodeConstantPool();

        cursor.readUnsignedShort(); // Access flags

        // this class index in constant pool;
        int classInfo = cursor.readUnsignedShort();
        int classInfoNameIndex = constantPool[classInfo].getNameIndex();
        ci = new ClassInfo(constantPool[classInfoNameIndex].getValue());

        cursor.readUnsignedShort(); // superclass

        int numInterfaces = cursor.readUnsignedShort();
        for (int i = 0; i < numInterfaces; i++) {
            cursor.readUnsignedShort(); // interface
        }

        decodeFields();
        MethodInfo[] methods = decodeMethods(constantPool);
        decodeMethodDependencies(methods, constantPool);
    }

    public ClassInfo getClassInfo() {
        return ci;
    }

    private boolean isDependency(String name, String className) {
        return !name.equals(className) && !name.startsWith("[");
    }

    private void addDependency(MethodInfo m, String name) {
        Dependency d = new Dependency(m.getName(), m.getDescriptor(), name);
        ci.addResolutionDep(d);
    }

    private String resolveName(ConstantPoolEntry[] constantPool, int cpi) {
        int nameIndex = constantPool[cpi].getNameIndex();
        return constantPool[nameIndex].getValue();
    }

    private void decodeMethodDependencies(MethodInfo[] methods, ConstantPoolEntry[] constantPool) {
        for (int i = 0; i < methods.length; i++) {
            MethodInfo m = methods[i];
            final int stopCheck = m.getCodeStart() + m.getCodeLength();

            int byteCodeIndex = m.getCodeStart();
            while (byteCodeIndex < stopCheck) {
                int bc = cursor.readUnsignedByteAt(byteCodeIndex);

                switch (bc) {
                    // These opcodes cause name resolution or initialization
                    // Their index bytes all point to a CONSTANT_Class (4.4.1)
                    case Bytecode.ANEWARRAY:
                    case Bytecode.CHECKCAST:
                    case Bytecode.INSTANCEOF:
                    case Bytecode.MULTIANEWARRAY:
                    case Bytecode.NEW: {
                        int cpi = cursor.readUnsignedShortAt(byteCodeIndex + 1);
                        String name = resolveName(constantPool, cpi);

                        if (isDependency(name, ci.getName())) {
                            addDependency(m, name);
                        }
                        break;
                    }

                    // These opcodes cause name resolution or initialization
                    // Their index bytes all point to a CONSTANT_Field/Methodref (4.4.2)
                    case Bytecode.GETFIELD:
                    case Bytecode.INVOKEINTERFACE:
                    case Bytecode.INVOKESPECIAL:
                    case Bytecode.INVOKEVIRTUAL:
                    case Bytecode.PUTFIELD:
                    case Bytecode.PUTSTATIC:
                    case Bytecode.GETSTATIC:
                    case Bytecode.INVOKESTATIC: {
                        int cpi = cursor.readUnsignedShortAt(byteCodeIndex + 1);
                        int classIndex = constantPool[cpi].getClassIndex();
                        String name = resolveName(constantPool, classIndex);

                        if (isDependency(name, ci.getName())) {
                            addDependency(m, name);
                        }
                        break;
                    }

                    case Bytecode.LOOKUPSWITCH: {
                        byteCodeIndex++;
                        int offset = byteCodeIndex - m.getCodeStart();
                        while (offset % 4 != 0) {
                            offset++;
                            byteCodeIndex++;
                        }

                        cursor.readIntAt(byteCodeIndex); // def
                        byteCodeIndex +=4;

                        int npairs = cursor.readIntAt(byteCodeIndex);
                        byteCodeIndex +=4;
                        byteCodeIndex += (8 * npairs);
                        continue;
                    }

                    case Bytecode.TABLESWITCH: {
                        byteCodeIndex++;
                        int offset = byteCodeIndex - m.getCodeStart();
                        while (offset % 4 != 0) {
                            offset++;
                            byteCodeIndex++;
                        }

                        cursor.readIntAt(byteCodeIndex); // def
                        byteCodeIndex +=4;

                        int low = cursor.readIntAt(byteCodeIndex);
                        byteCodeIndex +=4;
                        int high = cursor.readIntAt(byteCodeIndex);
                        byteCodeIndex +=4;
                        byteCodeIndex += (4 * (high - low + 1));
                        continue;
                    }

                    case Bytecode.WIDE: {
                        bc = cursor.readUnsignedByteAt(++byteCodeIndex);
                        if (bc == Bytecode.IINC) {
                            byteCodeIndex += 5;
                        } else {
                            byteCodeIndex += 3;
                        }
                        continue;
                    }
                }

                byteCodeIndex += Bytecode.getLength(bc);
            }

            if (byteCodeIndex - stopCheck > 1) {
                String err = "bad finish for method " + m.getName() +
                             "End + "  + (byteCodeIndex - stopCheck);
                throw new IllegalArgumentException(err);
            }
        }
    }

    private MethodInfo[] decodeMethods(ConstantPoolEntry[] constantPool) {
        MethodInfo[] methods = new MethodInfo[cursor.readUnsignedShort()];

        for (int i = 0; i < methods.length; i++) {
            cursor.readUnsignedShort(); // access flags

            String name = constantPool[cursor.readUnsignedShort()].getValue();
            String descriptor = constantPool[cursor.readUnsignedShort()].getValue();

            int codeLength = 0;
            int codeStart = 0;

            int numAttributes = cursor.readUnsignedShort(); // attributes count
            for (int j = 0; j < numAttributes; j++) {
                int type = cursor.readUnsignedShort(); // attrib nameIndex
                int aLen = cursor.readInt(); // attrib length

                if (constantPool[type].getValue().equals("Code")) {
                    cursor.readUnsignedShort(); // Max stack
                    cursor.readUnsignedShort(); // Max locals

                    codeLength = cursor.readInt();
                    codeStart = cursor.getOffset();

                    cursor.skipBytes(codeLength); // Need to skip the code bytes
                    cursor.skipBytes(cursor.readUnsignedShort() * 8); // Skip exception table

                    int numSubAttributes = cursor.readUnsignedShort();
                    for (int k = 0; k < numSubAttributes; k++) {
                        cursor.readUnsignedShort(); // sub name
                        cursor.skipBytes(cursor.readInt()); // sub attrib data
                    }
                } else {
                    cursor.skipBytes(aLen); // unknown attrib data
                }
            }

            methods[i] = new MethodInfo(name, descriptor, codeLength, codeStart);
        }

        return methods;
    }

    private void decodeFields() {
        // Looks like we dont need any field info, throw it away!
        int numFields = cursor.readUnsignedShort();

        for (int i = 0; i < numFields; i++) {
            cursor.readUnsignedShort(); // access flags
            cursor.readUnsignedShort(); // nameIndex
            cursor.readUnsignedShort(); // descriptorIndex

            int numAttributes = cursor.readUnsignedShort();
            for (int j = 0; j < numAttributes; j++) {
                cursor.readUnsignedShort(); // nameIndex
                int length = cursor.readInt();
                cursor.skipBytes(length); // data
            }
        }
    }

    private ConstantPoolEntry[] decodeConstantPool() {
        final int CONSTANT_Utf8 = 1;
        // final int CONSTANT_Unicode = 2;
        final int CONSTANT_Integer = 3;
        final int CONSTANT_Float = 4;
        final int CONSTANT_Long = 5;
        final int CONSTANT_Double = 6;
        final int CONSTANT_Class = 7;
        final int CONSTANT_String = 8;
        final int CONSTANT_Fieldref = 9;
        final int CONSTANT_Methodref = 10;
        final int CONSTANT_InterfaceMethodref = 11;
        final int CONSTANT_NameAndType = 12;
        final int CONSTANT_MethodHandle = 15;
        final int CONSTANT_MethodType = 16;
        final int CONSTANT_InvokeDynamic = 18;

        ConstantPoolEntry[] constantPool = new ConstantPoolEntry[cursor.readUnsignedShort()];

        // The constant pool starts at index 1
        for (int i = 1; i < constantPool.length; i++) {
            int type = cursor.readUnsignedByte();

            switch (type) {
                case CONSTANT_Class:
                    constantPool[i] = new ConstantPoolEntry(cursor.readUnsignedShort()); // name_index
                    break;

                case CONSTANT_Fieldref: case CONSTANT_Methodref: case CONSTANT_InterfaceMethodref:
                    constantPool[i] = new ConstantPoolEntry(cursor.readUnsignedShort()); // class_index
                    cursor.readUnsignedShort(); // name_and_type_index
                    break;

                case CONSTANT_String:
                    cursor.readUnsignedShort(); // string_index
                    break;

                case CONSTANT_Integer:
                    cursor.readInt(); // bytes
                    break;

                case CONSTANT_Float:
                    cursor.readInt(); // bytes
                    break;

                case CONSTANT_Long:
                    cursor.readInt(); // high_bytes
                    cursor.readInt(); // low_bytes
                    i++; // 8 byte constants use 2 constant pool slots.
                    break;

                case CONSTANT_Double:
                    cursor.readInt(); // high_bytes
                    cursor.readInt(); // low_bytes
                    i++; // 8 byte constants use 2 constant pool slots.
                    break;

                case CONSTANT_NameAndType:
                    constantPool[i] = new ConstantPoolEntry(cursor.readUnsignedShort()); // name_index
                    cursor.readUnsignedShort(); // descriptor_index
                    break;

                case CONSTANT_Utf8:
                    int length = cursor.readUnsignedShort(); // length
                    constantPool[i] = new ConstantPoolEntry(cursor.readUtf8(length)); // bytes[length]
                    break;

                case CONSTANT_MethodHandle:
                    cursor.readUnsignedByte(); // reference_kind
                    cursor.readUnsignedShort(); // reference_index
                    break;

                case CONSTANT_MethodType:
                    cursor.readUnsignedShort(); // descriptor_index
                    break;

                case CONSTANT_InvokeDynamic:
                    cursor.readUnsignedShort(); // bootstrap_method_attr_index
                    cursor.readUnsignedShort(); // name_and_type_index
                    break;

                default:
                    String err = "Unknown constant pool type " + String.valueOf(type) + "\n" +
                                 "CPE " + i + " of " + constantPool.length + "\n" +
                                 "Byte offset " + Integer.toHexString(cursor.getOffset());
                    throw new IllegalArgumentException(err);
            }
        }
        return constantPool;
    }
}
