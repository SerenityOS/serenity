/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4486154 4495729
 * @summary The FileChannel file locking
 */

import java.io.*;
import java.nio.channels.*;
import java.nio.*;

/**
 * Testing FileChannel's locking methods.
 */

public class TryLock {

    public static void main(String[] args) throws Exception {
        test1(true, true);
        test1(false, true);
        test1(true, false);
        test1(false, false);

        test2(true, true);
        test2(false, true);
        test2(true, false);
        test2(false, false);

        test3(true, true);
        test3(false, true);
        test3(true, false);
        test3(false, false);
    }

    public static void test1(boolean shared, boolean trylock) throws Exception {
        File testFile = File.createTempFile("test1", null);
        FileInputStream fis = new FileInputStream(testFile);
        FileChannel fc = fis.getChannel();
        FileLock fl = null;
        try {
            if (trylock)
                fl = fc.tryLock(0, fc.size(), shared);
            else
                fl = fc.lock(0, fc.size(), shared);
            if (!shared)
                throw new RuntimeException("No exception thrown for test1");
        } catch (NonWritableChannelException e) {
            if (shared)
                throw new RuntimeException("Exception thrown for wrong case test1");
        } finally {
            if (fl != null)
                fl.release();
            fc.close();
            testFile.delete();
        }
    }

    public static void test2(boolean shared, boolean trylock) throws Exception {
        File testFile = File.createTempFile("test2", null);
        FileOutputStream fis = new FileOutputStream(testFile);
        FileChannel fc = fis.getChannel();
        FileLock fl = null;
        try {
            if (trylock)
                fl = fc.tryLock(0, fc.size(), shared);
            else
                fl = fc.lock(0, fc.size(), shared);
            if (shared)
                throw new RuntimeException("No exception thrown for test2");
        } catch (NonReadableChannelException e) {
            if (!shared)
                throw new RuntimeException("Exception thrown incorrectly for test2");
        } finally {
            if (fl != null)
                fl.release();
            fc.close();
            testFile.delete();
        }
    }

    public static void test3(boolean shared, boolean trylock) throws Exception {
        File testFile = File.createTempFile("test3", null);
        RandomAccessFile fis = new RandomAccessFile(testFile, "rw");
        FileChannel fc = fis.getChannel();
        try {
            FileLock fl = null;
            if (trylock)
                fl = fc.tryLock(0, fc.size(), shared);
            else
                fl = fc.lock(0, fc.size(), shared);
            fl.release();
        } finally {
            fc.close();
            testFile.delete();
        }
    }
}
