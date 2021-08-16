/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.*;

/* @test
 * @bug 8246129
 * @summary JDK add metadata to zip files with entries timestamped at the
 *          lower bound of the DOS time epoch, i.e., 1980-01-01T00:00:00Z
 * @run testng/othervm ZipEntryTimeBounds
 */
public class ZipEntryTimeBounds {

    @Test
    public void testFilesWithEntryAtLowerTimeBoundAreEqual() throws Exception {

        // Ensure that entries that end up being exactly at the start of the
        // DOS epoch, java.util.zip.ZipEntry.DOSTIME_BEFORE_1980, or
        // 1980-01-01 00:00:00 are written without any extra timestamp metadata
        // being written

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-01"));
        File f1 = createTempFile();
        makeZip(f1, new GregorianCalendar(1980, Calendar.JANUARY, 1, 0, 0, 0).getTimeInMillis());

        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        File f2 = createTempFile();
        makeZip(f2, new GregorianCalendar(1980, Calendar.JANUARY, 1, 0, 0, 0).getTimeInMillis());
        assertEquals(Files.mismatch(f1.toPath(), f2.toPath()), -1L);

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+01"));
        File f3 = createTempFile();
        makeZip(f3, new GregorianCalendar(1980, Calendar.JANUARY, 1, 0, 0, 0).getTimeInMillis());
        assertEquals(Files.mismatch(f1.toPath(), f3.toPath()), -1L);

        // Check that the milliseconds part of the time is exactly preserved
        assertEquals(new ZipFile(f1).getEntry("entry.txt").getTime() % 60000, 0);
        assertEquals(new ZipFile(f2).getEntry("entry.txt").getTime() % 60000, 0);
        assertEquals(new ZipFile(f3).getEntry("entry.txt").getTime() % 60000, 0);
    }

    @Test
    public void testFilesWithEntryAfterLowerTimeBoundAreEqual() throws Exception {
        // Ensure files written using different timezone with entries set
        // shortly after 1980-01-01 00:00:00 produce exactly identical files
        // without metadata

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-01"));
        File f1 = createTempFile();
        makeZip(f1, new GregorianCalendar(1980, Calendar.JANUARY, 1, 0, 0, 1).getTimeInMillis());

        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        File f2 = createTempFile();
        makeZip(f2, new GregorianCalendar(1980, Calendar.JANUARY, 1, 0, 0, 1).getTimeInMillis());
        assertEquals(Files.mismatch(f1.toPath(), f2.toPath()), -1L);

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+01"));
        File f3 = createTempFile();
        makeZip(f3, new GregorianCalendar(1980, Calendar.JANUARY, 1, 0, 0, 1).getTimeInMillis() + 999);
        assertEquals(Files.mismatch(f1.toPath(), f3.toPath()), -1L);

        // Check that the seconds part of the time is lossily preserved,
        // rounding down to the previous 2s step since epoch
        assertEquals(new ZipFile(f1).getEntry("entry.txt").getTime() % 60000, 0);
        assertEquals(new ZipFile(f2).getEntry("entry.txt").getTime() % 60000, 0);
        assertEquals(new ZipFile(f3).getEntry("entry.txt").getTime() % 60000, 0);

        File f4 = createTempFile();
        makeZip(f4, new GregorianCalendar(1980, Calendar.JANUARY, 1, 0, 0, 2).getTimeInMillis());
        assertEquals(new ZipFile(f4).getEntry("entry.txt").getTime() % 60000, 2000);
    }

    @Test
    public void testFilesWithEntryBeforeLowerTimeBoundAreNotEqual() throws Exception {
        // Files written using different timezone with entries set shortly
        // before 1980-01-01 00:00:00 will produce files which add timestamp
        // metadata that make the files turn up not equal

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-01"));
        File f1 = createTempFile();
        makeZip(f1, new GregorianCalendar(1979, Calendar.DECEMBER, 31, 23, 59, 59).getTimeInMillis());

        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        File f2 = createTempFile();
        makeZip(f2, new GregorianCalendar(1979, Calendar.DECEMBER, 31, 23, 59, 59).getTimeInMillis());
        assertNotEquals(Files.mismatch(f1.toPath(), f2.toPath()), -1L);

        TimeZone.setDefault(TimeZone.getTimeZone("GMT+01"));
        File f3 = createTempFile();
        makeZip(f3, new GregorianCalendar(1979, Calendar.DECEMBER, 31, 23, 59, 59).getTimeInMillis() + 500);
        assertNotEquals(Files.mismatch(f1.toPath(), f3.toPath()), -1L);

        // Check that the time is preserved at second precision, no rounding
        // to 2s
        assertEquals(new ZipFile(f1).getEntry("entry.txt").getTime() % 60000, 59000);
        assertEquals(new ZipFile(f2).getEntry("entry.txt").getTime() % 60000, 59000);
        // Milliseconds are discarded even when storing entries with extended
        // time metadata
        assertEquals(new ZipFile(f3).getEntry("entry.txt").getTime() % 60000, 59000);
    }

    private static void makeZip(File f, long time) throws Exception {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
            ZipEntry e = new ZipEntry("entry.txt");
            e.setTime(time);
            out.putNextEntry(e);
            out.write(new byte[] { 0, 1, 2, 3 });
            out.closeEntry();
        }
    }

    private static File createTempFile() throws IOException {
        File file = File.createTempFile("out", "zip");
        file.deleteOnExit();
        return file;
    }
}
