/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import static java.util.zip.ZipFile.CENOFF;
import static java.util.zip.ZipFile.CENTIM;
import static java.util.zip.ZipFile.ENDHDR;
import static java.util.zip.ZipFile.ENDOFF;
import static java.util.zip.ZipFile.LOCTIM;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/* @test
 * @bug 8184940 8188869
 * @summary JDK 9 rejects zip files where the modified day or month is 0
 *          or otherwise represent an invalid date, such as 1980-02-30 24:60:60
 * @author Liam Miller-Cushon
 */
public class ZeroDate {

    public static void main(String[] args) throws Exception {
        // create a zip file, and read it in as a byte array
        Path path = Files.createTempFile("bad", ".zip");
        try (OutputStream os = Files.newOutputStream(path);
                ZipOutputStream zos = new ZipOutputStream(os)) {
            ZipEntry e = new ZipEntry("x");
            zos.putNextEntry(e);
            zos.write((int) 'x');
        }
        int len = (int) Files.size(path);
        byte[] data = new byte[len];
        try (InputStream is = Files.newInputStream(path)) {
            is.read(data);
        }
        Files.delete(path);

        // year, month, day are zero
        testDate(data.clone(), 0, LocalDate.of(1979, 11, 30).atStartOfDay());
        // only year is zero
        testDate(data.clone(), 0 << 25 | 4 << 21 | 5 << 16, LocalDate.of(1980, 4, 5).atStartOfDay());
        // month is greater than 12
        testDate(data.clone(), 0 << 25 | 13 << 21 | 1 << 16, LocalDate.of(1981, 1, 1).atStartOfDay());
        // 30th of February
        testDate(data.clone(), 0 << 25 | 2 << 21 | 30 << 16, LocalDate.of(1980, 3, 1).atStartOfDay());
        // 30th of February, 24:60:60
        testDate(data.clone(), 0 << 25 | 2 << 21 | 30 << 16 | 24 << 11 | 60 << 5 | 60 >> 1,
                LocalDateTime.of(1980, 3, 2, 1, 1, 0));
    }

    private static void testDate(byte[] data, int date, LocalDateTime expected) throws IOException {
        // set the datetime
        int endpos = data.length - ENDHDR;
        int cenpos = u16(data, endpos + ENDOFF);
        int locpos = u16(data, cenpos + CENOFF);
        writeU32(data, cenpos + CENTIM, date);
        writeU32(data, locpos + LOCTIM, date);

        // ensure that the archive is still readable, and the date is 1979-11-30
        Path path = Files.createTempFile("out", ".zip");
        try (OutputStream os = Files.newOutputStream(path)) {
            os.write(data);
        }
        try (ZipFile zf = new ZipFile(path.toFile())) {
            ZipEntry ze = zf.entries().nextElement();
            Instant actualInstant = ze.getLastModifiedTime().toInstant();
            Instant expectedInstant = expected.atZone(ZoneId.systemDefault()).toInstant();
            if (!actualInstant.equals(expectedInstant)) {
                throw new AssertionError(
                        String.format("actual: %s, expected: %s", actualInstant, expectedInstant));
            }
        } finally {
            Files.delete(path);
        }
    }

    static int u8(byte[] data, int offset) {
        return data[offset] & 0xff;
    }

    static int u16(byte[] data, int offset) {
        return u8(data, offset) + (u8(data, offset + 1) << 8);
    }

    private static void writeU32(byte[] data, int pos, int value) {
        data[pos] = (byte) (value & 0xff);
        data[pos + 1] = (byte) ((value >> 8) & 0xff);
        data[pos + 2] = (byte) ((value >> 16) & 0xff);
        data[pos + 3] = (byte) ((value >> 24) & 0xff);
    }
}
