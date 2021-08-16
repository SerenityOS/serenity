/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8220793
 * @summary Unit test for updating access and modification times of symlinks
 * @requires (os.family == "linux" | os.family == "mac" | os.family == "windows")
 * @library ..
 * @build SymlinkTime
 * @run main/othervm SymlinkTime
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

public class SymlinkTime {
    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        if (!TestUtil.supportsLinks(dir)) {
            System.out.println("Links not supported: skipping test");
            return;
        }

        try {
            // Create file and symbolic link to it
            final Path file = dir.resolve("file");
            final Path link = dir.resolve("link");
            Files.createFile(file);
            try {
                // Delay creating the link to get different time attributes
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException ignored) {
            }
            Files.createSymbolicLink(link, file);

            // Save file modification and access times
            BasicFileAttributeView view = Files.getFileAttributeView(link,
                BasicFileAttributeView.class);
            BasicFileAttributes attr = view.readAttributes();
            printTimes("Original file times", attr);
            FileTime fileModTime = attr.lastModifiedTime();
            FileTime fileAccTime = attr.lastAccessTime();

            // Read link modification and access times
            view = Files.getFileAttributeView(link,
                BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            attr = view.readAttributes();
            printTimes("Original link times", attr);

            // Set new base time and offset increment
            long base = 1000000000000L; // 2001-09-09T01:46:40Z
            long delta = 1000*60L;

            // Set new link modification and access times
            FileTime linkModTime = FileTime.fromMillis(base + delta);
            FileTime linkAccTime = FileTime.fromMillis(base + 2L*delta);
            view.setTimes(linkModTime, linkAccTime, null);

            // Verify link modification and access times updated correctly
            view = Files.getFileAttributeView(link,
                BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            attr = view.readAttributes();
            printTimes("Updated link times", attr);
            check("Link", attr, linkModTime, linkAccTime);

            // Verify file modification and access times unchanged
            view = Files.getFileAttributeView(file,
                BasicFileAttributeView.class);
            attr = view.readAttributes();
            printTimes("File times", attr);
            check("File", attr, fileModTime, fileAccTime);
        } finally {
            TestUtil.removeAll(dir);
        }
    }

    private static void check(String pathType, BasicFileAttributes attr,
        FileTime modTimeExpected, FileTime accTimeExpected) {
        if (!attr.lastModifiedTime().equals(modTimeExpected) ||
            !attr.lastAccessTime().equals(accTimeExpected)) {
            String message = String.format(
                "%s - modification time: expected %s, actual %s;%n" +
                "access time: expected %s, actual %s.%n", pathType,
                modTimeExpected, attr.lastModifiedTime(),
                accTimeExpected, attr.lastAccessTime());
            throw new RuntimeException(message);
        }
    }

    private static void printTimes(String label, BasicFileAttributes attr) {
        System.out.format
            ("%s%ncreation:     %s%nmodification: %s%naccess:       %s%n%n",
            label, attr.creationTime(), attr.lastModifiedTime(),
            attr.lastAccessTime());
    }
}
