/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 */

/*
 * @test
 * @bug 6622260
 * @summary javap prints negative bytes incorrectly in hex
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;

public class T6622260 {
    public static void main(String[] args) throws Exception {
        new T6622260().run();
    }

    public void run() throws IOException {
        File javaFile = writeTestFile();
        File classFile = compileTestFile(javaFile);
        modifyClassFile(classFile);
        String output = javap(classFile);
        verify(output);
    }

    File writeTestFile() throws IOException {
        File f = new File("Test.java");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        out.println("@Deprecated class Test { int f; void m() { } }");
        out.close();
        return f;
    }

    File compileTestFile(File f) {
        int rc = com.sun.tools.javac.Main.compile(new String[] { f.getPath() });
        if (rc != 0)
            throw new Error("compilation failed. rc=" + rc);
        String path = f.getPath();
        return new File(path.substring(0, path.length() - 5) + ".class");
    }

    void modifyClassFile(File f) throws IOException {
        String newAttributeName = "NonstandardAttribute";
        byte[] newAttributeData = { 0, 1, 2, 127, (byte)128, (byte)129, (byte)254, (byte)255 };

        DataInputStream in = new DataInputStream(new FileInputStream(f));
        byte[] data = new byte[(int) f.length()];
        in.readFully(data);
        in.close();

        in = new DataInputStream(new ByteArrayInputStream(data));
        in.skipBytes(4); // magic
        in.skipBytes(2); // minor
        in.skipBytes(2); // minor

        int constantPoolPos = data.length - in.available();
        int constant_pool_count = skipConstantPool(in);

        int flagsPos = data.length - in.available();
        in.skipBytes(2); // access_flags
        in.skipBytes(2); // this_class
        in.skipBytes(2); // super_class

        int interfaces_count = in.readUnsignedShort();
        in.skipBytes(interfaces_count * 2);

        int field_count = in.readUnsignedShort();
        for (int i = 0; i < field_count; i++) {
            in.skipBytes(6); // access_flags, name_index, descriptor_index
            skipAttributes(in);
        }

        int method_count = in.readUnsignedShort();
        for (int i = 0; i < method_count; i++) {
            in.skipBytes(6); // access_flags, name_index, descriptor_index
            skipAttributes(in);
        }

        int classAttributesPos = data.length - in.available();
        int attributes_count = in.readUnsignedShort();

        f.renameTo(new File(f.getPath() + ".BAK"));
        DataOutputStream out = new DataOutputStream(new FileOutputStream(f));

        // copy head
        out.write(data, 0, constantPoolPos);

        // copy constant pool, adding in name of new attribute
        out.writeShort(constant_pool_count + 1);
        out.write(data, constantPoolPos + 2, flagsPos - constantPoolPos - 2);
        out.write(1); // CONSTANT_Utf8
        out.writeUTF(newAttributeName);

        // copy flags, class, superclass, interfaces, fields and methods
        out.write(data, flagsPos, classAttributesPos - flagsPos);

        // copy class attributes, adding in new attribute
        out.writeShort(attributes_count + 1);
        out.write(data, classAttributesPos + 2, data.length - classAttributesPos - 2);
        out.writeShort(constant_pool_count); // index of new attribute name
        out.writeInt(newAttributeData.length);
        out.write(newAttributeData);
        out.close();
    }

    int skipConstantPool(DataInputStream in) throws IOException {
        int constant_pool_count = in.readUnsignedShort();
        for (int i = 1; i < constant_pool_count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
            case  1: // CONSTANT_Utf8
                int length = in.readUnsignedShort();
                in.skipBytes(length); // bytes
                break;

            case  3: // CONSTANT_Integer
            case  4: // CONSTANT_Float
                in.skipBytes(4); // bytes
                break;

            case  5: // CONSTANT_Long
            case  6: // CONSTANT_Double
                in.skipBytes(8); // high_bytes, low_bytes
                break;

            case  7: // CONSTANT_Class
                in.skipBytes(2); // name_index
                break;

            case  8: // CONSTANT_String
                in.skipBytes(2); // string_index
                break;

            case  9: // CONSTANT_FieldRef
            case 10: // CONSTANT_Methodref
            case 11: // CONSTANT_InterfaceMethodref
                in.skipBytes(4); // class_index, name_and_type_index
                break;

            case 12: // CONSTANT_NameAndType
                in.skipBytes(4); // name_index, descriptor_index
                break;

            default:
                throw new Error("constant pool tag: " + tag);
            }
        }
        return constant_pool_count;
    }

    int skipAttributes(DataInputStream in) throws IOException {
        int attributes_count = in.readUnsignedShort();
        for (int i = 0; i < attributes_count; i++) {
            in.skipBytes(2); // attribute_name_index;
            int length = in.readInt();
            in.skipBytes(length); // info
        }
        return attributes_count;
    }

    String javap(File f) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(new String[] { "-v", f.getPath() }, out);
        if (rc != 0)
            throw new Error("javap failed. rc=" + rc);
        out.close();
        return sw.toString();
    }

    void verify(String output) {
        System.out.println(output);
        output = output.substring(output.indexOf("Test.java"));
        if (output.indexOf("-") >= 0)
            throw new Error("- found in output");
        if (output.indexOf("FFFFFF") >= 0)
            throw new Error("FFFFFF found in output");
    }
}
