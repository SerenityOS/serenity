import java.io.*;

class InstallSDE {
    static final boolean verbose = true;
    static final String nameSDE = "SourceDebugExtension";

    byte[] orig;
    byte[] sdeAttr;
    byte[] gen;

    int origPos = 0;
    int genPos = 0;

    int sdeIndex;

    public static void main(String[] args) throws IOException {
        if (args.length == 2) {
            install(new File(args[0]), new File(args[1]));
        } else if (args.length == 3) {
            install(new File(args[0]), new File(args[1]), new File(args[2]));
        } else {
            abort("Usage: <command> <input class file> " +
                               "<attribute file> <output class file name>\n" +
                  "<command> <input/output class file> <attribute file>");
        }
    }

    static void install(File inClassFile, File attrFile, File outClassFile)
                                                            throws IOException {
        new InstallSDE(inClassFile, attrFile, outClassFile);
    }

    static void install(File inOutClassFile, File attrFile) throws IOException {
        File tmpFile = new File(inOutClassFile.getPath() + "tmp-out");
        File tmpInOutClassFile = new File(inOutClassFile.getPath() + "tmp-in");

        // Workaround delayed file deletes on Windows using a tmp file name
        if (!inOutClassFile.renameTo(tmpInOutClassFile)) {
            throw new IOException("tmp copy of inOutClassFile failed");
        }

        new InstallSDE(tmpInOutClassFile, attrFile, tmpFile);

        if (!tmpInOutClassFile.delete()) {
            throw new IOException("tmpInOutClassFile.delete() failed");
        }
        if (!tmpFile.renameTo(inOutClassFile)) {
            throw new IOException("tmpFile.renameTo(inOutClassFile) failed");
        }
    }

    static void abort(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    InstallSDE(File inClassFile, File attrFile, File outClassFile) throws IOException {
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
        addSDE();

        // write result
        FileOutputStream outStream = new FileOutputStream(outClassFile);
        outStream.write(gen, 0, genPos);
        outStream.close();
    }

    byte[] readWhole(File input) throws IOException {
        FileInputStream inStream = new FileInputStream(input);
        int len = (int)input.length();
        byte[] bytes = new byte[len];
        if (inStream.read(bytes, 0, len) != len) {
            abort("expected size: " + len);
        }
        inStream.close();
        return bytes;
    }

    void addSDE() throws UnsupportedEncodingException {
        int i;
        copy(4 + 2 + 2); // magic min/maj version
        int constantPoolCountPos = genPos;
        int constantPoolCount = readU2();
        writeU2(constantPoolCount);
        // copy old constant pool return index of SDE symbol, if found
        sdeIndex = copyConstantPool(constantPoolCount);
        if (sdeIndex < 0) {
            // if "SourceDebugExtension" symbol not there add it
            writeUtf8ForSDE();

            // increment the countantPoolCount
            sdeIndex = constantPoolCount;
            ++constantPoolCount;
            randomAccessWriteU2(constantPoolCountPos, constantPoolCount);

            if (verbose) {
                System.out.println("SourceDebugExtension not found, installed at: " +
                                   sdeIndex);
            }
        } else {
            if (verbose) {
                System.out.println("SourceDebugExtension found at: " +
                                   sdeIndex);
            }
        }
        copy(2 + 2 + 2);  // access, this, super
        int interfaceCount = readU2();
        writeU2(interfaceCount);
        if (verbose) {
            System.out.println("interfaceCount: " + interfaceCount);
        }
        copy(interfaceCount * 2);
        copyMembers(); // fields
        copyMembers(); // methods
        int attrCountPos = genPos;
        int attrCount = readU2();
        writeU2(attrCount);
        if (verbose) {
            System.out.println("class attrCount: " + attrCount);
        }
        // copy the class attributes, return true if SDE attr found (not copied)
        if (!copyAttrs(attrCount)) {
            // we will be adding SDE and it isn't already counted
            ++attrCount;
            randomAccessWriteU2(attrCountPos, attrCount);
            if (verbose) {
                System.out.println("class attrCount incremented");
            }
        }
        writeAttrForSDE(sdeIndex);
    }

    void copyMembers() {
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
            copyAttrs(attrCount);
        }
    }

    boolean copyAttrs(int attrCount) {
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
                writeU2(nameIndex);  // name
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

    void writeAttrForSDE(int index) {
        writeU2(index);
        writeU4(sdeAttr.length);
        for (int i = 0; i < sdeAttr.length; ++i) {
            writeU1(sdeAttr[i]);
        }
    }

    void randomAccessWriteU2(int pos, int val) {
        int savePos = genPos;
        genPos = pos;
        writeU2(val);
        genPos = savePos;
    }

    int readU1() {
        return ((int)orig[origPos++]) & 0xFF;
    }

    int readU2() {
         int res = readU1();
        return (res << 8) + readU1();
    }

    int readU4() {
        int res = readU2();
        return (res << 16) + readU2();
    }

    void writeU1(int val) {
        gen[genPos++] = (byte)val;
    }

    void writeU2(int val) {
        writeU1(val >> 8);
        writeU1(val & 0xFF);
    }

    void writeU4(int val) {
        writeU2(val >> 16);
        writeU2(val & 0xFFFF);
    }

    void copy(int count) {
        for (int i = 0; i < count; ++i) {
            gen[genPos++] = orig[origPos++];
        }
    }

    byte[] readBytes(int count) {
        byte[] bytes = new byte[count];
        for (int i = 0; i < count; ++i) {
            bytes[i] = orig[origPos++];
        }
        return bytes;
    }

    void writeBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; ++i) {
            gen[genPos++] = bytes[i];
        }
    }

    int copyConstantPool(int constantPoolCount) throws UnsupportedEncodingException {
        int sdeIndex = -1;
        // copy const pool index zero not in class file
        for (int i = 1; i < constantPoolCount; ++i) {
            int tag = readU1();
            writeU1(tag);
            switch (tag) {
                case 7:  // Class
                case 8:  // String
                    copy(2);
                    break;
                case 9:  // Field
                case 10: // Method
                case 11: // InterfaceMethod
                case 3:  // Integer
                case 4:  // Float
                case 12: // NameAndType
                case 18: // InvokeDynamic
                    copy(4);
                    break;
                case 5:  // Long
                case 6:  // Double
                    copy(8);
                    break;
                case 15: // MethodHandle
                    copy(3);
                    break;
                case 1:  // Utf8
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

    void writeUtf8ForSDE() {
        int len = nameSDE.length();
        writeU1(1); // Utf8 tag
        writeU2(len);
        for (int i = 0; i < len; ++i) {
            writeU1(nameSDE.charAt(i));
        }
    }
}
