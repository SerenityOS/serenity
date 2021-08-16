/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8181493 8231174
 * @summary Verify that nanosecond precision is maintained for file timestamps
 * @requires (os.family == "linux") | (os.family == "mac") | (os.family == "windows")
 * @modules java.base/sun.nio.fs:+open
 */

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SetTimesNanos {
    private static final boolean IS_WINDOWS =
        System.getProperty("os.name").startsWith("Windows");

    public static void main(String[] args) throws Exception {
        if (!IS_WINDOWS) {
            // Check whether futimens() system call is supported
            Class unixNativeDispatcherClass =
                Class.forName("sun.nio.fs.UnixNativeDispatcher");
            Method futimensSupported =
                unixNativeDispatcherClass.getDeclaredMethod("futimensSupported");
            futimensSupported.setAccessible(true);
            if (!(boolean)futimensSupported.invoke(null)) {
                System.err.println("futimens() not supported; skipping test");
                return;
            }
        }

        Path dirPath = Path.of("test");
        Path dir = Files.createDirectory(dirPath);
        FileStore store = Files.getFileStore(dir);
        System.out.format("FileStore: \"%s\" on %s (%s)%n",
            dir, store.name(), store.type());

        Set<String> testedTypes = IS_WINDOWS ?
            Set.of("NTFS") : Set.of("apfs", "ext4", "xfs", "zfs");
        if (!testedTypes.contains(store.type())) {
            System.err.format("%s not in %s; skipping test", store.type(), testedTypes);
            return;
        }

        testNanos(dir);

        Path file = Files.createFile(dir.resolve("test.dat"));
        testNanos(file);
    }

    private static void testNanos(Path path) throws IOException {
        // Set modification and access times
        // Time stamp = "2017-01-01 01:01:01.123456789";
        long timeNanos = 1_483_261_261L*1_000_000_000L + 123_456_789L;
        FileTime pathTime = FileTime.from(timeNanos, TimeUnit.NANOSECONDS);
        BasicFileAttributeView view =
            Files.getFileAttributeView(path, BasicFileAttributeView.class);
        view.setTimes(pathTime, pathTime, null);

        // Windows file time resolution is 100ns so truncate
        if (IS_WINDOWS) {
            timeNanos = 100L*(timeNanos/100L);
        }

        // Read attributes
        BasicFileAttributes attrs =
            Files.readAttributes(path, BasicFileAttributes.class);

        // Check timestamps
        String[] timeNames = new String[] {"modification", "access"};
        FileTime[] times = new FileTime[] {attrs.lastModifiedTime(),
            attrs.lastAccessTime()};
        for (int i = 0; i < timeNames.length; i++) {
            long nanos = times[i].to(TimeUnit.NANOSECONDS);
            if (nanos != timeNanos) {
                throw new RuntimeException("Expected " + timeNames[i] +
                    " timestamp to be '" + timeNanos + "', but was '" +
                    nanos + "'");
            }
        }
    }
}
