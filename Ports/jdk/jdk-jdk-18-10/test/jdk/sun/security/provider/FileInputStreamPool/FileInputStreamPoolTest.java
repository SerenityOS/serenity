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

/**
 * @test
 * @bug 8047769
 * @modules java.base/java.io:open
 *          java.base/java.lang.ref:open
 *          java.base/sun.security.provider:open
 * @summary SecureRandom should be more frugal with file descriptors
 */

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.HashSet;

public class FileInputStreamPoolTest {

    static final byte[] bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};

    static FilterInputStream testCaching(File file) throws IOException {
        InputStream in1 = TestProxy.FileInputStreamPool_getInputStream(file);
        InputStream in2 = TestProxy.FileInputStreamPool_getInputStream(file);
        assertTrue(in1 == in2,
            "1st InputStream: " + in1 +
                " is not same as 2nd: " + in2);

        byte[] readBytes = new byte[bytes.length];
        int nread = in1.read(readBytes);
        assertTrue(bytes.length == nread,
            "short read: " + nread +
                " bytes of expected: " + bytes.length);
        assertTrue(Arrays.equals(readBytes, bytes),
            "readBytes: " + Arrays.toString(readBytes) +
                " not equal to expected: " + Arrays.toString(bytes));

       return (FilterInputStream)in1;
    }

    static void assertTrue(boolean test, String message) {
        if (!test) {
            throw new AssertionError(message);
        }
    }

    static void processReferences(FilterInputStream in1) throws InterruptedException, IOException {
        FileInputStream fis = TestProxy.FilterInputStream_getInField(in1);
        FileDescriptor fd = fis.getFD();
        System.out.printf("fis: %s, fd: %s%n", fis, fd);
        // Prepare to wait for FD to be reclaimed
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        HashSet<Reference<?>> pending = new HashSet<>();
        pending.add(new WeakReference<>(in1, queue));
        pending.add(new WeakReference<>(fis, queue));
        pending.add(new WeakReference<>(fd, queue));

        Reference<?> r;
        while (((r = queue.remove(10L)) != null)
                || !pending.isEmpty()) {
            System.out.printf("r: %s, pending: %d%n", r, pending.size());
            if (r != null) {
                pending.remove(r);
            } else {
                fd = null;
                fis = null;
                in1 = null;
                System.gc();  // attempt to reclaim the FD
            }
            Thread.sleep(10L);
        }
        Reference.reachabilityFence(fd);
        Reference.reachabilityFence(fis);
        Reference.reachabilityFence(in1);
    }

    public static void main(String[] args) throws Exception {
        // 1st create temporary file
        File file = File.createTempFile("test", ".dat");
        try (AutoCloseable acf = () -> {
            // On Windows, failure to delete file is probably a consequence
            // of the file still being opened - so the test should fail.
            assertTrue(file.delete(),
                "Can't delete: " + file + " (is it still open?)");
        }) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(bytes);
            }

            // test caching 1st time

            processReferences(testCaching(file));

            // test caching 2nd time - this should only succeed if the stream
            // is re-opened as a consequence of cleared WeakReference

            processReferences(testCaching(file));
        }
    }

    /**
     * A proxy for (package)private static methods:
     *   sun.security.provider.FileInputStreamPool.getInputStream
     *   java.lang.ref.Reference.waitForReferenceProcessing
     */
    static class TestProxy {
        private static final Method getInputStreamMethod;
        private static final Method waitForReferenceProcessingMethod;
        private static final Field inField;

        static {
            try {
                Class<?> fileInputStreamPoolClass =
                    Class.forName("sun.security.provider.FileInputStreamPool");
                getInputStreamMethod =
                    fileInputStreamPoolClass.getDeclaredMethod(
                        "getInputStream", File.class);
                getInputStreamMethod.setAccessible(true);

                waitForReferenceProcessingMethod =
                    Reference.class.getDeclaredMethod("waitForReferenceProcessing");
                waitForReferenceProcessingMethod.setAccessible(true);

                inField = FilterInputStream.class.getDeclaredField("in");
                inField.setAccessible(true);
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        static InputStream FileInputStreamPool_getInputStream(File file)
            throws IOException {
            try {
                return (InputStream) getInputStreamMethod.invoke(null, file);
            } catch (InvocationTargetException e) {
                Throwable te = e.getTargetException();
                if (te instanceof IOException) {
                    throw (IOException) te;
                } else if (te instanceof RuntimeException) {
                    throw (RuntimeException) te;
                } else if (te instanceof Error) {
                    throw (Error) te;
                } else {
                    throw new UndeclaredThrowableException(te);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        static boolean Reference_waitForReferenceProcessing() {
            try {
                return (boolean) waitForReferenceProcessingMethod.invoke(null);
            } catch (InvocationTargetException e) {
                Throwable te = e.getTargetException();
                if (te instanceof InterruptedException) {
                    return true;
                } else if (te instanceof RuntimeException) {
                    throw (RuntimeException) te;
                } else if (te instanceof Error) {
                    throw (Error) te;
                } else {
                    throw new UndeclaredThrowableException(te);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        static FileInputStream FilterInputStream_getInField(FilterInputStream fis) {
            try {
                return (FileInputStream) inField.get(fis);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
