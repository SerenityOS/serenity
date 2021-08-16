/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8147615
 * @summary Test whether an unreferenced FileChannel is actually cleaned
 * @requires (os.family == "linux") | (os.family == "mac") | (os.family == "aix")
 * @library /test/lib
 * @build jdk.test.lib.util.FileUtils CleanerTest
 * @modules java.management java.base/sun.nio.ch:+open
 * @run main/othervm CleanerTest
 */

import com.sun.management.UnixOperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.PhantomReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

import jdk.test.lib.util.FileUtils;

import sun.nio.ch.FileChannelImpl;

public class CleanerTest {
    public static void main(String[] args) throws Throwable {
        OperatingSystemMXBean mxBean =
            ManagementFactory.getOperatingSystemMXBean();
        UnixOperatingSystemMXBean unixMxBean = null;
        if (mxBean instanceof UnixOperatingSystemMXBean) {
            unixMxBean = (UnixOperatingSystemMXBean)mxBean;
        } else {
            System.out.println("Non-Unix system: skipping test.");
            return;
        }

        FileUtils.listFileDescriptors(System.out);
        long fdCount0 = unixMxBean.getOpenFileDescriptorCount();

        Path path = Paths.get(System.getProperty("test.dir", "."), "junk");
        try {
            FileChannel fc = FileChannel.open(path, StandardOpenOption.CREATE,
                StandardOpenOption.READ, StandardOpenOption.WRITE);

            // Prepare to wait for Channel, FD and Cleaner to be reclaimed
            ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
            HashSet<Reference<?>> pending = new HashSet<>();

            Reference<Object> fcRef = new PhantomReference<>(fc, refQueue);
            pending.add(fcRef);

            Field fdField = FileChannelImpl.class.getDeclaredField("fd");
            fdField.setAccessible(true);
            Object fd = fdField.get(fc);        // get the fd from the channel
            WeakReference<Object> fdWeak = new WeakReference<>(fd, refQueue);
            pending.add(fdWeak);

            Field closerField = FileChannelImpl.class.getDeclaredField("closer");
            closerField.setAccessible(true);
            Object closer = closerField.get(fc);
            System.out.printf("  cleanup: %s, fd: %s, cf: %s%n", fc, fd, closer);

            if (closer != null) {
                WeakReference<Object> closerWeak = new WeakReference<>(closer, refQueue);
                pending.add(closerWeak);
                System.out.printf("    closerWeak: %s%n", closerWeak);
            }

            // Wait for all of the objects being tracked to be reclaimed;
            // The test will timeout if they are not reclaimed within the jtreg timeout
            Reference<?> r;
            while (((r = refQueue.remove(1000L)) != null)
                    || !pending.isEmpty()) {
                System.out.printf("    r: %s, pending: %d%n", r, pending.size());
                if (r != null) {
                    pending.remove(r);
                } else {
                    fc = null;
                    fd = null;
                    closer = null;
                    System.gc();  // attempt to reclaim them
                }
            }

            Reference.reachabilityFence(fc);
            Reference.reachabilityFence(fd);
            Reference.reachabilityFence(closer);

            long fdCount = unixMxBean.getOpenFileDescriptorCount();
            if (fdCount != fdCount0) {
                // Add debugging info about file descriptor changes
                System.out.printf("initial count of open file descriptors: %d%n", fdCount0);
                System.out.printf("final count of open file descriptors: %d%n", fdCount);
                FileUtils.listFileDescriptors(System.out);
            }
        } finally {
            Files.delete(path);
        }
    }
}
