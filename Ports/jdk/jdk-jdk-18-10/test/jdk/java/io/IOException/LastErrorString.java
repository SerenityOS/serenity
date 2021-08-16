/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4167937
   @ignore 7042603 - Test truncates system files when run as root
   @summary Test code paths that use the JVM_LastErrorString procedure
 */

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;


public class LastErrorString {

    static String UNWRITEABLE_DIR;
    static String UNREADABLE_FILE;
    static String READABLE_FILE;
    static String WRITEABLE_FILE;
    static String INVALID_PATH;

    static {
        if (File.separatorChar == '/') {
            UNWRITEABLE_DIR = "/etc/dfs";
            UNREADABLE_FILE = "/etc/shadow";
        } else if (File.separatorChar == '\\') {
            UNREADABLE_FILE = "c:/pagefile.sys";
            UNWRITEABLE_DIR = "z:/fooBAR/baz/GORP";
        } else {
            throw new RuntimeException("What kind of system is this?");
        }
        File d = new File(System.getProperty("test.src", "."));
        READABLE_FILE = new File(d, "LastErrorString.java").getPath();
        WRITEABLE_FILE = "x.LastErrorString";
        String s = "foo/";
        for (;;) {
            s = s + s;
            if (s.length() > 8192) break;
        }
        s += "bar";
        INVALID_PATH = s;
    }


    abstract static class Test {

        String name;

        public Test(String name) {
            this.name = name;
        }

        public abstract void run() throws IOException;

        public void go() throws IOException {
            try {
                this.run();
            } catch (IOException x) {
                System.err.println(name);
                System.err.println("  " + x);
                return;
            }
            System.err.println("WARNING: No exception for " + name);
        }

    }

    abstract static class ClosedFISTest extends Test {

        FileInputStream in;

        public ClosedFISTest(String name) {
            super("FileInputStream." + name);
        }

        public void go() throws IOException {
            this.in = new FileInputStream(READABLE_FILE);
            this.in.close();
            super.go();
        }

    }

    abstract static class ClosedFOSTest extends Test {

        FileOutputStream out;

        public ClosedFOSTest(String name) {
            super("FileOutputStream." + name);
        }

        public void go() throws IOException {
            this.out = new FileOutputStream(WRITEABLE_FILE);
            this.out.close();
            super.go();
        }

    }

    abstract static class ClosedRAFTest extends Test {

        RandomAccessFile raf;

        public ClosedRAFTest(String name) {
            super("RandomAccessFile." + name);
        }

        public void go() throws IOException {
            this.raf = new RandomAccessFile(WRITEABLE_FILE, "rw");
            this.raf.close();
            super.go();
        }

    }

    abstract static class ReadOnlyRAFTest extends Test {

        RandomAccessFile raf;

        public ReadOnlyRAFTest(String name) {
            super("RandomAccessFile." + name);
        }

        public void go() throws IOException {
            this.raf = new RandomAccessFile(READABLE_FILE, "r");
            super.go();
            this.raf.close();
        }

    }


    static void go() throws Exception {

        new Test("File.createNewFile") {
            public void run() throws IOException {
                new File(UNWRITEABLE_DIR, "foo").createNewFile();
            }}.go();

        new Test("File.getCanonicalpath") {
            public void run() throws IOException {
                new File(INVALID_PATH).getCanonicalPath();
            }}.go();

        new Test("FileInputStream(file)") {
            public void run() throws IOException {
                new FileInputStream(UNREADABLE_FILE);
            }}.go();

        new Test("FileInputStream(dir)") {
            public void run() throws IOException {
                new FileInputStream(".");
            }}.go();

        new ClosedFISTest("read()") {
            public void run() throws IOException {
                in.read();
            }}.go();

        new ClosedFISTest("read(byte[])") {
            public void run() throws IOException {
                byte[] b = new byte[10];
                in.read(b);
            }}.go();

        new ClosedFISTest("skip") {
            public void run() throws IOException {
                in.skip(10);
            }}.go();

        new ClosedFISTest("available") {
            public void run() throws IOException {
                in.available();
            }}.go();

        new Test("FileOutputStream") {
            public void run() throws IOException {
                new FileOutputStream(UNREADABLE_FILE);
            }}.go();

        new ClosedFOSTest("write()") {
            public void run() throws IOException {
                out.write(10);
            }}.go();

        new ClosedFOSTest("write(byte[])") {
            public void run() throws IOException {
                out.write(new byte[] { 1, 2, 3 });
            }}.go();

        new Test("RandomAccessFile") {
            public void run() throws IOException {
                new RandomAccessFile(UNREADABLE_FILE, "r");
            }}.go();

        new ClosedRAFTest("getFilePointer") {
            public void run() throws IOException {
                raf.getFilePointer();
            }}.go();

        new ClosedRAFTest("length") {
            public void run() throws IOException {
                raf.length();
            }}.go();

        new ClosedRAFTest("seek") {
            public void run() throws IOException {
                raf.seek(20);
            }}.go();

        new ClosedRAFTest("setLength") {
            public void run() throws IOException {
                raf.setLength(0);
            }}.go();

        new ClosedRAFTest("readShort") {
            public void run() throws IOException {
                raf.readShort();
            }}.go();

        new ClosedRAFTest("readInt") {
            public void run() throws IOException {
                raf.readInt();
            }}.go();

        new ReadOnlyRAFTest("writeShort") {
            public void run() throws IOException {
                raf.writeShort(10);
            }}.go();

        new ReadOnlyRAFTest("getFilePointer") {
            public void run() throws IOException {
                raf.writeInt(10);
            }}.go();

    }


    public static void main(String[] args) throws Exception {
        go();
    }

}
