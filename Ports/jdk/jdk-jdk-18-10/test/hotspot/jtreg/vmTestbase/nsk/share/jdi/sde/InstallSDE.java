/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi.sde;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import nsk.share.Consts;

/*
 * Inserts in class file 'SourceDebugExtension' attribute based on input .SMAP file.
 */
public class InstallSDE {
    static final String nameSDE = "SourceDebugExtension";

    private byte[] orig;

    private byte[] sdeAttr;

    private byte[] gen;

    private int origPos = 0;

    private int genPos = 0;

    private int sdeIndex;

    public static void install(File inClassFile, File smapFile, File outClassFile, boolean verbose) throws IOException {
        new InstallSDE(inClassFile, smapFile, outClassFile, verbose);
    }

    public static void install(byte[] aOrig, byte[] aSdeAttr, File outClassFile, boolean verbose) throws IOException {
        new InstallSDE(aOrig, aSdeAttr, outClassFile, verbose);
    }

    public static void install(File inOutClassFile, File attrFile, boolean verbose) throws IOException {
        File tmpFile = new File(inOutClassFile.getPath() + "tmp-out");
        File tmpInOutClassFile = new File(inOutClassFile.getPath() + "tmp-in");

        // Workaround delayed file deletes on Windows using a tmp file name
        if (!inOutClassFile.renameTo(tmpInOutClassFile)) {
            throw new IOException("inOutClassFile.renameTo(tmpInOutClassFile) failed");
        }

        new InstallSDE(tmpInOutClassFile, attrFile, tmpFile, verbose);

        if (!tmpInOutClassFile.delete()) {
            throw new IOException("tmpInOutClassFile.delete() failed");
        }
        if (!tmpFile.renameTo(inOutClassFile)) {
            throw new IOException("tmpFile.renameTo(inOutClassFile) failed");
        }
    }

    private static void abort(String msg) {
        System.err.println(msg);
        System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
    }

    private InstallSDE(File inClassFile, File attrFile, File outClassFile, boolean verbose) throws IOException {
        if (!inClassFile.exists()) {
            abort("no such file: " + inClassFile);
        }
        if (!attrFile.exists()) {
            abort("no such file: " + attrFile);
        }

        // get the bytes
        orig = readWhole(inClassFile);
        sdeAttr = readWhole(attrFile);
        gen = new byte[orig.length + sdeAttr.length + 100];

        // do it
        addSDE(verbose);

        // write result
        FileOutputStream outStream = new FileOutputStream(outClassFile);
        outStream.write(gen, 0, genPos);
        outStream.close();
    }

    private InstallSDE(byte[] aOrig, byte[] aSdeAttr, File outClassFile, boolean verbose) throws IOException {
        orig = aOrig;
        sdeAttr = aSdeAttr;
        gen = new byte[orig.length + sdeAttr.length + 100];

        // do it
        addSDE(verbose);

        // write result
        FileOutputStream outStream = new FileOutputStream(outClassFile);
        outStream.write(gen, 0, genPos);
        outStream.close();
    }

    private byte[] readWhole(File input) throws IOException {
        FileInputStream inStream = new FileInputStream(input);
        try {
                return readWhole(inStream, (int) input.length());
        } finally {
                inStream.close();
        }
    }

    private byte[] readWhole(InputStream inStream, int len) throws IOException {
        byte[] bytes = new byte[len];

        if (inStream.read(bytes, 0, len) != len) {
            abort("expected size: " + len);
        }

        return bytes;
    }

    private void addSDE(boolean verbose) throws UnsupportedEncodingException {
        copy(4 + 2 + 2); // magic min/maj version
        int constantPoolCountPos = genPos;
        int constantPoolCount = readU2();
        writeU2(constantPoolCount);
        // copy old constant pool return index of SDE symbol, if found
        sdeIndex = copyConstantPool(constantPoolCount, verbose);
        if (sdeIndex < 0) {
            // if "SourceDebugExtension" symbol not there add it
            writeUtf8ForSDE();

            // increment the countantPoolCount
            sdeIndex = constantPoolCount;
            ++constantPoolCount;
            randomAccessWriteU2(constantPoolCountPos, constantPoolCount);

            if (verbose) {
                System.out.println("SourceDebugExtension not found, installed at: " + sdeIndex);
            }
        } else {
            if (verbose) {
                System.out.println("SourceDebugExtension found at: " + sdeIndex);
            }
        }
        copy(2 + 2 + 2); // access, this, super
        int interfaceCount = readU2();
        writeU2(interfaceCount);
        if (verbose) {
            System.out.println("interfaceCount: " + interfaceCount);
        }
        copy(interfaceCount * 2);
        copyMembers(verbose); // fields
        copyMembers(verbose); // methods
        int attrCountPos = genPos;
        int attrCount = readU2();
        writeU2(attrCount);
        if (verbose) {
            System.out.println("class attrCount: " + attrCount);
        }
        // copy the class attributes, return true if SDE attr found (not copied)
        if (!copyAttrs(attrCount, verbose)) {
            // we will be adding SDE and it isn't already counted
            ++attrCount;
            randomAccessWriteU2(attrCountPos, attrCount);
            if (verbose) {
                System.out.println("class attrCount incremented");
            }
        }
        writeAttrForSDE(sdeIndex);
    }

    private void copyMembers(boolean verbose) {
        int count = readU2();
        writeU2(count);
        if (verbose) {
            System.out.println("members count: " + count);
        }
        for (int i = 0; i < count; ++i) {
            copy(6); // access, name, descriptor
            int attrCount = readU2();
            writeU2(attrCount);
            if (verbose) {
                System.out.println("member attr count: " + attrCount);
            }
            copyAttrs(attrCount, verbose);
        }
    }

    private boolean copyAttrs(int attrCount, boolean verbose) {
        boolean sdeFound = false;
        for (int i = 0; i < attrCount; ++i) {
            int nameIndex = readU2();
            // don't write old SDE
            if (nameIndex == sdeIndex) {
                sdeFound = true;
                if (verbose) {
                    System.out.println("SDE attr found");
                }
            } else {
                writeU2(nameIndex); // name
                int len = readU4();
                writeU4(len);
                copy(len);
                if (verbose) {
                    System.out.println("attr len: " + len);
                }
            }
        }
        return sdeFound;
    }

    private void writeAttrForSDE(int index) {
        writeU2(index);
        writeU4(sdeAttr.length);
        for (int i = 0; i < sdeAttr.length; ++i) {
            writeU1(sdeAttr[i]);
        }
    }

    private void randomAccessWriteU2(int pos, int val) {
        int savePos = genPos;
        genPos = pos;
        writeU2(val);
        genPos = savePos;
    }

    private int readU1() {
        return ((int) orig[origPos++]) & 0xFF;
    }

    private int readU2() {
        int res = readU1();
        return (res << 8) + readU1();
    }

    private int readU4() {
        int res = readU2();
        return (res << 16) + readU2();
    }

    private void writeU1(int val) {
        gen[genPos++] = (byte) val;
    }

    private void writeU2(int val) {
        writeU1(val >> 8);
        writeU1(val & 0xFF);
    }

    private void writeU4(int val) {
        writeU2(val >> 16);
        writeU2(val & 0xFFFF);
    }

    private void copy(int count) {
        for (int i = 0; i < count; ++i) {
            gen[genPos++] = orig[origPos++];
        }
    }

    private byte[] readBytes(int count) {
        byte[] bytes = new byte[count];
        for (int i = 0; i < count; ++i) {
            bytes[i] = orig[origPos++];
        }
        return bytes;
    }

    private void writeBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; ++i) {
            gen[genPos++] = bytes[i];
        }
    }

    private int copyConstantPool(int constantPoolCount, boolean verbose) throws UnsupportedEncodingException {
        int sdeIndex = -1;
        // copy const pool index zero not in class file
        if ( verbose )
                System.out.println("cp count=" + constantPoolCount);
        for (int i = 1; i < constantPoolCount; ++i) {
            int tag = readU1();
            writeU1(tag);
            if ( verbose )
                System.out.println(i + ": tag=" + tag);
            switch (tag) {
            case 7: // Class
            case 8: // String
            case 16: // MethodType
                copy(2);
                break;
            case 15: // MethodHandle
                copy(3);
                break;
            case 9: // Field
            case 10: // Method
            case 11: // InterfaceMethod
            case 3: // Integer
            case 4: // Float
            case 12: // NameAndType
            case 17: // InvokeDynamic_17 (will go away)
            case 18: // InokeDynamic
                copy(4);
                break;
            case 5: // Long
            case 6: // Double
                copy(8);
                ++i;
                break;
            case 1: // Utf8
                int len = readU2();
                writeU2(len);
                byte[] utf8 = readBytes(len);
                String str = new String(utf8, "UTF-8");
                if (verbose) {
                    System.out.println(i + " read class attr -- '" + str + "'");
                }
                if (str.equals(nameSDE)) {
                    sdeIndex = i;
                }
                writeBytes(utf8);
                break;
            default:
                abort("unexpected tag: " + tag);
                break;
            }
        }
        return sdeIndex;
    }

    private void writeUtf8ForSDE() {
        int len = nameSDE.length();
        writeU1(1); // Utf8 tag
        writeU2(len);
        for (int i = 0; i < len; ++i) {
            writeU1(nameSDE.charAt(i));
        }
    }
}
