/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.test.lib.util.FileUtils;

/*
 * @test
 * @bug 8166253
 * @summary Verify that OverlappingFileLockException is thrown when expected.
 * @library .. /test/lib
 * @build jdk.test.lib.util.FileUtils
 * @run main/othervm FileLockGC
 */
public class FileLockGC {
    public enum TestType {
        NO_GC_NO_RELEASE(true),
        // A hypothetical 'GC_THEN_RELEASE' case is infeasible
        RELEASE(false),
        RELEASE_THEN_GC(false),
        GC(true);

        private final boolean exceptionExpected;

        TestType(boolean exceptionExpected) {
            this.exceptionExpected = exceptionExpected;
        }

        boolean exceptionExpected() {
            return exceptionExpected;
        }
    }

    public static void main(String[] args) throws Exception {
        final File f = new File(System.getProperty("test.dir", ".")
            + File.separator + "junk.txt");
        final Path p = f.toPath();
        int failures = 0;

        for (TestType t : TestType.values()) {
            try {
                if (!testFileLockGC(f, t)) {
                    failures++;
                }
            } finally {
                FileUtils.deleteFileIfExistsWithRetry(p);
            }
        }

        if (failures != 0) {
            throw new RuntimeException("Test had " + failures + " failure(s)");
        }
    }

    private static boolean testFileLockGC(File f, TestType type)
        throws InterruptedException, IOException {
        System.out.printf("Test %s starting%n", type.toString());

        final RandomAccessFile raf1 = new RandomAccessFile(f, "rw");

        FileLock lock1 = raf1.getChannel().tryLock();
        WeakReference<FileLock> ref1 = new WeakReference(lock1);

        switch (type) {
            case GC:
                lock1 = null;
                System.gc();
                break;
            case RELEASE:
                lock1.release();
                break;
            case RELEASE_THEN_GC:
                lock1.release();
                lock1 = null;
                System.gc();
                break;
            default: // NO_GC_NO_RELEASE
                // lock1 is neither collected nor released
                break;
        }

        final RandomAccessFile raf2 = new RandomAccessFile(f, "rw");

        boolean success = true;
        FileLock lock2 = null;
        try {
            lock2 = raf2.getChannel().tryLock();
            if (type.exceptionExpected()) {
                System.err.printf
                    ("No expected OverlappingFileLockException for test %s%n",
                    type.toString());
                success = false;
            }
        } catch (OverlappingFileLockException ofe) {
            if (!type.exceptionExpected()) {
                System.err.printf
                    ("Unexpected OverlappingFileLockException for test %s%n",
                    type.toString());
                success = false;
            }
        } finally {
            if (lock1 != null) {
                lock1.release();
            }
            if (lock2 != null) {
                lock2.release();
            }
            raf2.close();
            raf1.close();
            System.out.printf("Test %s finished%n", type.toString());
        }

        return success;
    }
}
