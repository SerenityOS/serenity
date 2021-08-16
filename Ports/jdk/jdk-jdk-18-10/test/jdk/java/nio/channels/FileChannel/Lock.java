/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4429043 4493595 6332756 6709457 7146506
 * @summary Test FileChannel file locking
 */

import java.io.*;
import java.nio.channels.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * Testing FileChannel's lock method.
 */
public class Lock {

    public static void main(String[] args) throws Exception {
        if (args.length == 2) {
            attemptLock(args[1], args[0].equals("2"));
            return;
        } else if (args.length != 0) {
            throw new RuntimeException("Wrong number of parameters.");
        }
        File blah = File.createTempFile("blah", null);
        blah.deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(blah, "rw");
        raf.write(1);
        raf.close();
        test1(blah, "1");
        test1(blah, "2");
        test2(blah, true);
        test2(blah, false);
        test3(blah);
        test4(blah);
    }

    /**
     * Test mutual locking with other process
     */
    static void test1(File blah, String str) throws Exception {
        try (RandomAccessFile fis = new RandomAccessFile(blah, "rw")) {
            FileChannel fc = fis.getChannel();
            FileLock lock = null;

            // grab the lock
            if (str.equals("1")) {
                lock = fc.lock(0, 10, false);
                if (lock == null)
                    throw new RuntimeException("Lock should not return null");
                try {
                    fc.lock(5, 10, false);
                    throw new RuntimeException("Overlapping locks allowed");
                } catch (OverlappingFileLockException e) {} // correct result
            }

            // execute the tamperer
            String command = System.getProperty("java.home") +
                File.separator + "bin" + File.separator + "java";
            String testClasses = System.getProperty("test.classes");
            if (testClasses != null)
                command += " -cp " + testClasses;
            command += " Lock " + str + " " + blah;
            Process p = Runtime.getRuntime().exec(command);

            // evaluate System.out of child process
            String s;
            boolean hasOutput = false;
            InputStreamReader isr;
            isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            while ((s = br.readLine()) != null) {
                // only throw on Unix as windows over NFS fails...
                if ((File.separatorChar == '/') && !s.equals("good")) {
                    throw new RuntimeException("Failed: " + s);
                }
                hasOutput = true;
            }

            // evaluate System.err in case of System.out of child process
            // was empty
            if (!hasOutput) {
                isr = new InputStreamReader(p.getErrorStream());
                br = new BufferedReader(isr);
                if ((s = br.readLine()) != null) {
                    System.err.println("Error output:");
                    System.err.println(s);
                    while ((s = br.readLine()) != null) {
                        System.err.println(s);
                    }
                }
                throw new RuntimeException("Failed, no output");
            }

            // clean up, check multiple releases
            if (lock != null) {
                lock.release();
                lock.release();
            }
        }
    }

    /**
     * Basic test for FileChannel.lock() and FileChannel.tryLock()
     */
    static void test2(File blah, boolean b) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(blah, "rw")) {
            FileChannel channel = raf.getChannel();
            FileLock lock;
            if (b)
                lock = channel.lock();
            else
                lock = channel.tryLock();
            lock.release();
        }
    }

    /**
     * Test that overlapping file locking is not possible when using different
     * FileChannel objects to the same file path
     */
    static void test3(File blah) throws Exception {
        try (RandomAccessFile raf1 = new RandomAccessFile(blah, "rw");
             RandomAccessFile raf2 = new RandomAccessFile(blah, "rw"))
        {
            FileChannel fc1 = raf1.getChannel();
            FileChannel fc2 = raf2.getChannel();

            // lock via one channel, and then attempt to lock the same file
            // using a second channel
            FileLock fl1 = fc1.lock();
            try {
                fc2.tryLock();
                throw new RuntimeException("Overlapping locks allowed");
            } catch (OverlappingFileLockException x) {}
            try {
                fc2.lock();
                throw new RuntimeException("Overlapping locks allowed");
            } catch (OverlappingFileLockException x) {}

            // release lock and the attempt to lock with the second channel
            // should succeed.
            fl1.release();
            fc2.lock();
            try {
                fc1.lock();
                throw new RuntimeException("Overlapping locks allowed");
            } catch (OverlappingFileLockException x) {}
        }
    }

    /**
     * Test file locking when file is opened for append
     */
    static void test4(File blah) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(blah, true)) {
            FileChannel fc = fos.getChannel();
            fc.tryLock().release();
            fc.tryLock(0L, 1L, false).release();
            fc.lock().release();
            fc.lock(0L, 1L, false).release();
        }
        try (FileChannel fc = FileChannel.open(blah.toPath(), APPEND)) {
            fc.tryLock().release();
            fc.tryLock(0L, 1L, false).release();
            fc.lock().release();
            fc.lock(0L, 1L, false).release();
        }
    }

    /**
     * Utility method to be run in secondary process which tries to acquire a
     * lock on a FileChannel
     */
    static void attemptLock(String fileName,
                            boolean expectsLock) throws Exception
    {
        File f = new File(fileName);
        try (RandomAccessFile raf = new RandomAccessFile(f, "rw")) {
            FileChannel fc = raf.getChannel();
            if (fc.tryLock(10, 10, false) == null) {
                System.out.println("bad: Failed to grab adjacent lock");
            }
            if (fc.tryLock(0, 10, false) == null) {
                if (expectsLock)
                    System.out.println("bad");
                else
                    System.out.println("good");
            } else {
                if (expectsLock)
                    System.out.println("good");
                else
                    System.out.println("bad");
            }
        }
    }
}
