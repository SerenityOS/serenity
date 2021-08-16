/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6567415
 * @summary Test to ensure javac does not go into an infinite loop, while
 *               reading a classfile of a specific length.
 * @modules jdk.compiler/com.sun.tools.javac.jvm
 * @compile -XDignore.symbol.file T6567415.java
 * @run main T6567415
 * @author ksrini
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/*
 * this test compiles Bar.java into a classfile and enlarges the file to the
 * magic file length, then use this mutated file on the classpath to compile
 * Foo.java which references Bar.java and Ka-boom. QED.
 */
public class T6567415 {
    final static String TEST_FILE_NAME = "Bar";
    final static String TEST_JAVA = TEST_FILE_NAME + ".java";
    final static String TEST_CLASS = TEST_FILE_NAME + ".class";

    final static String TEST2_FILE_NAME = "Foo";
    final static String TEST2_JAVA = TEST2_FILE_NAME + ".java";

    /*
     * the following is the initial buffer length set in ClassReader.java
     * thus this value needs to change if ClassReader buf length changes.
     */

    final static int BAD_FILE_LENGTH =
            com.sun.tools.javac.jvm.ClassReader.INITIAL_BUFFER_SIZE;

    static void createClassFile() throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(TEST_JAVA);
            PrintStream ps = new PrintStream(fos);
            ps.println("public class " + TEST_FILE_NAME + " {}");
        } finally {
            fos.close();
        }
        String cmds[] = {TEST_JAVA};
        com.sun.tools.javac.Main.compile(cmds);
    }

    static void enlargeClassFile() throws IOException {
        File f = new File(TEST_CLASS);
        if (!f.exists()) {
            System.out.println("file not found: " + TEST_CLASS);
            System.exit(1);
        }
        File tfile = new File(f.getAbsolutePath() + ".tmp");
        f.renameTo(tfile);

        RandomAccessFile raf = null;
        FileChannel wfc = null;

        FileInputStream fis = null;
        FileChannel rfc = null;

        try {
            raf =  new RandomAccessFile(f, "rw");
            wfc = raf.getChannel();

            fis = new FileInputStream(tfile);
            rfc = fis.getChannel();

            ByteBuffer bb = MappedByteBuffer.allocate(BAD_FILE_LENGTH);
            rfc.read(bb);
            bb.rewind();
            wfc.write(bb);
            wfc.truncate(BAD_FILE_LENGTH);
        } finally {
            wfc.close();
            raf.close();
            rfc.close();
            fis.close();
        }
        System.out.println("file length = " + f.length());
    }

    static void createJavaFile() throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(TEST2_JAVA);
            PrintStream ps = new PrintStream(fos);
            ps.println("public class " + TEST2_FILE_NAME +
                    " {" + TEST_FILE_NAME + " b = new " +
                    TEST_FILE_NAME  + " ();}");
        } finally {
            fos.close();
        }
    }

    public static void main(String... args) throws Exception {
        createClassFile();
        enlargeClassFile();
        createJavaFile();
        Thread t = new Thread () {
            @Override
            public void run() {
                String cmds[] = {"-verbose", "-cp", ".", TEST2_JAVA};
                int ret = com.sun.tools.javac.Main.compile(cmds);
                System.out.println("test compilation returns: " + ret);
            }
        };
        t.start();
        t.join(1000*60);
        System.out.println(t.getState());
        if (t.isAlive()) {
            throw new RuntimeException("Error: compilation is looping");
        }
    }
}
