/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6374379
 * @library /test/lib
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 * @run main ReadLongZipFileName
 * @summary Verify that we can read zip file names > 255 chars long
 */

import java.io.*;
import java.util.jar.*;
import java.util.Stack;
import jdk.test.lib.util.FileUtils;

public class ReadLongZipFileName {
    private static String entryName = "testFile.txt";;

    public static void realMain(String args[]) {
        String longDirName = "abcdefghijklmnopqrstuvwx"; // 24 chars.
        String jarFileName = "areallylargejarfilename.jar";    // 27 chars.
        File file = null;
        File myJarFile = null;
        int currentFileLength = 0;
        int minRequiredLength = 600; // long enough to definitely fail.
        Stack<File> directories = new Stack<File>();

        String filename = "." + File.separator;
        try {
            // Create a directory structure long enough that the filename will
            // put us over the minRequiredLength.
            do {
                filename = filename + longDirName + File.separator;
                file = new File(filename);
                file.mkdir();
                currentFileLength = file.getCanonicalPath().length();
                directories.push(file);
            } while (currentFileLength < (minRequiredLength - jarFileName.length()));

            // Create a new Jar file: use jar instead of zip to make sure long
            // names work for both zip and jar subclass.
            filename = filename + jarFileName;
            JarOutputStream out = new JarOutputStream(
                new BufferedOutputStream(
                    new FileOutputStream(filename.toString())));
            out.putNextEntry(new JarEntry(entryName));
            out.write(1);
            out.close();
            myJarFile = new File(filename.toString());
            currentFileLength = myJarFile.getCanonicalPath().length();
            if (!myJarFile.exists()) {
                fail("Jar file does not exist.");
            }
        } catch (IOException e) {
            unexpected(e, "Problem creating the Jar file.");
        }

        try {
            JarFile readJarFile = new JarFile(myJarFile);
            JarEntry je = readJarFile.getJarEntry(entryName);
            check(je != null);
            DataInputStream dis = new DataInputStream(
                readJarFile.getInputStream(je));
            byte val = dis.readByte();
            check(val == 1);
            try {
                dis.readByte();
                fail("Read past expected EOF");
            } catch (IOException e) {
                pass();
            }
            readJarFile.close();
            pass("Opened Jar file for reading with a name " + currentFileLength
                 + " characters long");
        } catch (IOException e) {
            unexpected(e, "Test failed - problem reading the Jar file back in.");
        }

        if (myJarFile != null) {
            check(myJarFile.delete());
        }

        while (! directories.empty()) {
            File f = directories.pop();
            try {
                FileUtils.deleteFileWithRetry(f.toPath());
            } catch (IOException e) {
                unexpected(e, "Fail to clean up directory, " + f);
                break;
            }
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void pass(String msg) {System.out.println(msg); passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void unexpected(Throwable t, String msg) {
        System.out.println(msg); failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
