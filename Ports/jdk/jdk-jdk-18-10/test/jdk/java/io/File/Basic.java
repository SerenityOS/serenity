/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4165666 4203706 4288670 4290024
 * @summary Basic heartbeat test for File methods that access the filesystem
 * @build Basic Util
 * @run main/othervm Basic
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.PrintStream;
import java.io.RandomAccessFile;


public class Basic {

    static PrintStream out = System.err;

    static File rwFile = new File("x.Basic.rw");
    static File bigFile = new File("x.Basic.big");
    static File roFile = new File("x.Basic.ro");
    static File thisDir = new File(".");
    static File dir = new File("x.Basic.dir");
    static File dir2 = new File("x.Basic.dir2");
    static byte bytes[] = new byte[] {1, 2, 3, 4, 5, 6};

    static void showBoolean(String what, boolean value) {
        out.println("  " + what + ": " + value);
    }

    static void showLong(String what, long value) {
        out.println("  " + what + ": " + value);
    }

    static void show(File f) throws Exception {
        out.println(f + ": ");
        showBoolean("exists", f.exists());
        showBoolean("isFile", f.isFile());
        showBoolean("isDirectory", f.isDirectory());
        showBoolean("canRead", f.canRead());
        showBoolean("canWrite", f.canWrite());
        showLong("lastModified", f.lastModified());
        showLong("length", f.length());
    }

    static void testFile(File f, boolean writeable, long length)
        throws Exception
    {
        if (!f.exists()) fail(f, "does not exist");
        if (!f.isFile()) fail(f, "is not a file");
        if (f.isDirectory()) fail(f, "is a directory");
        if (!f.canRead()) fail(f, "is not readable");
        if (!Util.isPrivileged() && f.canWrite() != writeable)
            fail(f, writeable ? "is not writeable" : "is writeable");
        if (f.length() != length) fail(f, "has wrong length");
    }

    static void fail(File f, String why) throws Exception {
        throw new Exception(f + " " + why);
    }

    static void setup() throws Exception {
        rwFile.delete();
        bigFile.delete();
        roFile.delete();
        thisDir.delete();
        dir.delete();
        dir2.delete();

        try (FileOutputStream fos = new FileOutputStream(rwFile)) {
            fos.write(bytes);
        }

        roFile.createNewFile();
        roFile.setReadOnly();
    }

    public static void main(String[] args) throws Exception {
        setup();

        show(rwFile);
        testFile(rwFile, true, bytes.length);
        rwFile.delete();
        if (rwFile.exists()) {
            fail(rwFile, "could not delete");
        }

        show(roFile);
        testFile(roFile, false, 0);

        show(thisDir);
        if (!thisDir.exists()) fail(thisDir, "does not exist");
        if (thisDir.isFile()) fail(thisDir, "is a file");
        if (!thisDir.isDirectory()) fail(thisDir, "is not a directory");
        if (!thisDir.canRead()) fail(thisDir, "is readable");
        if (!thisDir.canWrite()) fail(thisDir, "is writeable");
        String[] fs = thisDir.list();
        if (fs == null) fail(thisDir, "list() returned null");
        out.print("  [" + fs.length + "]");
        for (int i = 0; i < fs.length; i++) {
            out.print(" " + fs[i]);
        }
        out.println();
        if (fs.length == 0) fail(thisDir, "is empty");

        if (!dir.mkdir() || !dir.exists() || !dir.isDirectory()) {
            fail(dir, "could not create");
        }
        if (!dir.renameTo(dir2)) {
            fail(dir, "failed to rename");
        }
        if (dir.exists() || !dir2.exists() || !dir2.isDirectory()) {
            fail(dir, "not renamed");
        }

        System.err.println("NOTE: Large files not supported on this system");
    }

}
