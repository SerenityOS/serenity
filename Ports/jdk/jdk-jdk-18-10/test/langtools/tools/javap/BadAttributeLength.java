/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8047072
 * @summary javap OOM on fuzzed classfile
 * @modules jdk.jdeps/com.sun.tools.javap
 * @run main BadAttributeLength
 */


import java.io.*;

public class BadAttributeLength {

    public static String source = "public class Test {\n" +
                                  "    public static void main(String[] args) {}\n" +
                                  "}";

    public static void main(String[] args) throws Exception {
        final File sourceFile = new File("Test.java");
        if (sourceFile.exists()) {
            if (!sourceFile.delete()) {
                throw new IOException("Can't override the Test.java file. " +
                        "Check permissions.");
            }
        }
        try (FileWriter fw = new FileWriter(sourceFile)) {
            fw.write(source);
        }

        final String[] javacOpts = {"Test.java"};

        if (com.sun.tools.javac.Main.compile(javacOpts) != 0) {
            throw new Exception("Can't compile embedded test.");
        }

        RandomAccessFile raf = new RandomAccessFile("Test.class", "rw");
        long attPos = getFirstAttributePos(raf);
        if (attPos < 0) {
            throw new Exception("The class file contains no attributes at all.");
        }
        raf.seek(attPos + 2); // Jump to the attribute length
        raf.writeInt(Integer.MAX_VALUE - 1);
        raf.close();

        String[] opts = { "-v", "Test.class" };
        StringWriter sw = new StringWriter();
        PrintWriter pout = new PrintWriter(sw);

        com.sun.tools.javap.Main.run(opts, pout);
        pout.flush();

        if (sw.getBuffer().indexOf("OutOfMemoryError") != -1) {
            throw new Exception("javap exited with OutOfMemoryError " +
                    "instead of giving the proper error message.");
        }
    }

    private static long getFirstAttributePos(RandomAccessFile cfile) throws Exception {
        cfile.seek(0);
        int v1, v2;
        v1 = cfile.readInt();
        // System.out.println("Magic: " + String.format("%X", v1));

        v1 = cfile.readUnsignedShort();
        v2 = cfile.readUnsignedShort();
        // System.out.println("Version: " + String.format("%d.%d", v1, v2));

        v1 = cfile.readUnsignedShort();
        // System.out.println("CPool size: " + v1);
        // Exhaust the constant pool
        for (; v1 > 1; v1--) {
            // System.out.print(".");
            byte tag = cfile.readByte();
            switch (tag) {
                case 7  : // Class
                case 8  : // String
                    // Data is 2 bytes long
                    cfile.skipBytes(2);
                    break;
                case 3  : // Integer
                case 4  : // Float
                case 9  : // FieldRef
                case 10 : // MethodRef
                case 11 : // InterfaceMethodRef
                case 12 : // Name and Type
                    // Data is 4 bytes long
                    cfile.skipBytes(4);
                    break;
                case 5  : // Long
                case 6  : // Double
                    // Data is 8 bytes long
                    cfile.skipBytes(8);
                    break;
                case 1  : // Utf8
                    v2 = cfile.readUnsignedShort(); // Read buffer size
                    cfile.skipBytes(v2); // Skip buffer
                    break;
                default :
                    throw new Exception("Unexpected tag in CPool: [" + tag + "] at "
                            + Long.toHexString(cfile.getFilePointer()));
            }
        }
        // System.out.println();

        cfile.skipBytes(6); // Access flags, this_class and super_class
        v1 = cfile.readUnsignedShort(); // Number of interfaces
        // System.out.println("Interfaces: " + v1);
        cfile.skipBytes(3 * v1); // Each interface_info record is 3 bytes long
        v1 = cfile.readUnsignedShort(); // Number of fields
        // System.out.println("Fields: " + v1);
        // Exhaust the fields table
        for (; v1 > 0; v1--) {
            // System.out.print(".");
            cfile.skipBytes(6); // Skip access_flags, name_index and descriptor_index
            v2 = cfile.readUnsignedShort(); // Field attributes count
            if (v2 > 0) {
                // This field has some attributes - suits our needs
                // System.out.println();
                return cfile.getFilePointer();
            }
        }
        // System.out.println();
        v1 = cfile.readUnsignedShort(); // Number of methods
        // System.out.println("Methods: " + v1);
        // Exhaust the methods table
        for (; v1 > 0; v1--) {
            // System.out.print(".");
            cfile.skipBytes(6); // Skip access_flags, name_index and descriptor_index
            v2 = cfile.readUnsignedShort(); // Method attributes count
            if (v2 > 0) {
                // This method got attributes - Ok with us,
                // return position of the first one
                // System.out.println();
                return cfile.getFilePointer();
            }
        }
        // System.out.println();
        // Class attributes section
        v1 = cfile.readUnsignedShort(); // Counts of attributes in class
        if (v1 > 0) {
            // Class has some attributes, return position of the first one
            return cfile.getFilePointer();
        }
        // Bummer! No attributes in the entire class file. Not fair!
        return -1L;
    }
}
