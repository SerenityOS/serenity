/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

/**
 * @test
 * @bug 4313887 8062949 8191872
 * @library ..
 * @run testng SetLastModifiedTime
 * @summary Unit test for Files.setLastModifiedTime
 */

public class SetLastModifiedTime {

    static Path testDir;

    @BeforeClass
    void createTestDirectory() throws Exception {
        testDir = TestUtil.createTemporaryDirectory();
    }

    @AfterClass
    void removeTestDirectory() throws Exception {
        TestUtil.removeAll(testDir);
    }

    /**
     * Exercise Files.setLastModifiedTime on the given file
     */
    void test(Path path) throws Exception {
        FileTime now = Files.getLastModifiedTime(path);
        FileTime zero = FileTime.fromMillis(0L);

        Path result = Files.setLastModifiedTime(path, zero);
        assertTrue(result == path);
        assertEquals(Files.getLastModifiedTime(path), zero);

        result = Files.setLastModifiedTime(path, now);
        assertTrue(result == path);
        assertEquals(Files.getLastModifiedTime(path), now);
    }

    @Test
    public void testRegularFile() throws Exception {
        Path file = Files.createFile(testDir.resolve("file"));
        test(file);
    }

    @Test
    public void testDirectory() throws Exception {
        Path dir = Files.createDirectory(testDir.resolve("dir"));
        test(dir);
    }

    @Test
    public void testSymbolicLink() throws Exception {
        if (TestUtil.supportsLinks(testDir)) {
            Path target = Files.createFile(testDir.resolve("target"));
            Path link = testDir.resolve("link");
            Files.createSymbolicLink(link, target);
            test(link);
        }
    }

    @Test
    public void testNulls() throws Exception {
        Path path = Paths.get("foo");
        FileTime zero = FileTime.fromMillis(0L);

        try {
            Files.setLastModifiedTime(null, zero);
            assertTrue(false);
        } catch (NullPointerException expected) { }

        try {
            Files.setLastModifiedTime(path, null);
            assertTrue(false);
        } catch (NullPointerException expected) { }

        try {
            Files.setLastModifiedTime(null, null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
    }

    @Test
    public void testCompare() throws Exception {
        Path path = Files.createFile(testDir.resolve("path"));
        long timeMillis = 1512520600195L;
        FileTime fileTime = FileTime.fromMillis(timeMillis);
        Files.setLastModifiedTime(path, fileTime);
        File file = path.toFile();
        long ioTime = file.lastModified();
        long nioTime = Files.getLastModifiedTime(path).toMillis();
        assertTrue(ioTime == timeMillis || ioTime == 1000*(timeMillis/1000),
            "File.lastModified() not in {time, 1000*(time/1000)}");
        assertEquals(nioTime, ioTime,
            "File.lastModified() != Files.getLastModifiedTime().toMillis()");
    }
}

